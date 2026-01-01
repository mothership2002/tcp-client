package hyun.messageconnecter;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;
import hyun.messageconnecter.enums.AutoCalculate;
import hyun.messageconnecter.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * TCP 메시지 역직렬화 엔진 (바이트 레벨)
 * 바이트 배열을 객체로 변환
 * <p>
 * 주요 개선사항:
 * - 문자열 길이가 아닌 바이트 길이 기준으로 처리
 * - 필드별 인코딩 지원
 * - 멀티바이트 문자 안전 처리
 */
public class TcpMessageDeserializer {

    private static final Logger log = LoggerFactory.getLogger(TcpMessageDeserializer.class);

    /**
     * 바이트 배열을 객체로 역직렬화
     * <p>
     * STX/ETX 검증 및 체크섬 검증 포함
     *
     * @param bytes 바이트 배열
     * @param clazz 대상 클래스
     * @return 역직렬화된 객체
     */
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        // @TcpMessage 어노테이션 확인
        TcpMessage tcpMessage = clazz.getAnnotation(TcpMessage.class);
        if (tcpMessage == null) {
            throw new IllegalArgumentException("@TcpMessage annotation is required on class: " + clazz.getName());
        }

        // === STX/ETX 검증 및 제거 ===
        byte[] messageBytes = validateAndStripFraming(bytes, tcpMessage);

        // === 체크섬 검증 ===
        validateChecksums(messageBytes, clazz, tcpMessage);

        // 필드 수집 및 순서대로 정렬
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(TcpField.class))
                .sorted(Comparator.comparing(f -> f.getAnnotation(TcpField.class).order()))
                .toList();

        // 객체 생성
        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of class: " + clazz.getName(), e);
        }

        // 현재 읽기 위치 (바이트 오프셋)
        int position = 0;

        // 각 필드 역직렬화
        for (Field field : fields) {
            TcpField tcpField = field.getAnnotation(TcpField.class);
            field.setAccessible(true);

            int fieldLength = tcpField.length();

            // 데이터가 부족하면 빈 바이트 배열로 처리
            byte[] fieldBytes;
            if (position + fieldLength > messageBytes.length) {
                log.warn("Data length {} is less than expected position {}. Using empty bytes.",
                        messageBytes.length, position + fieldLength);
                fieldBytes = new byte[0];
            } else {
                fieldBytes = Arrays.copyOfRange(messageBytes, position, position + fieldLength);
            }

            position += fieldLength;

            // 필드 역직렬화 (중첩 객체, 리스트, 일반 필드)
            try {
                Object fieldValue = deserializeField(fieldBytes, field, tcpField, tcpMessage);
                field.set(instance, fieldValue);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set field: " + field.getName(), e);
            }
        }

        return instance;
    }

    /**
     * STX/ETX 검증 및 제거
     */
    private byte[] validateAndStripFraming(byte[] bytes, TcpMessage tcpMessage) {
        byte stx = tcpMessage.stx();
        byte etx = tcpMessage.etx();

        int offset = 0;
        int length = bytes.length;

        // STX 검증
        if (stx != 0x00) {
            if (bytes.length < 1) {
                throw new IllegalArgumentException("Message too short to contain STX");
            }
            if (bytes[0] != stx) {
                throw new IllegalArgumentException(
                        String.format("Invalid STX: expected 0x%02X but got 0x%02X", stx, bytes[0]));
            }
            offset = 1;
            length--;
            log.debug("STX validated: 0x{}", String.format("%02X", stx));
        }

        // ETX 검증
        if (etx != 0x00) {
            if (bytes.length < offset + 1) {
                throw new IllegalArgumentException("Message too short to contain ETX");
            }
            byte lastByte = bytes[bytes.length - 1];
            if (lastByte != etx) {
                throw new IllegalArgumentException(
                        String.format("Invalid ETX: expected 0x%02X but got 0x%02X", etx, lastByte));
            }
            length--;
            log.debug("ETX validated: 0x{}", String.format("%02X", etx));
        }

        // STX/ETX 제거된 메시지 반환
        if (offset == 0 && length == bytes.length) {
            return bytes; // 변경 없음
        }

        return Arrays.copyOfRange(bytes, offset, offset + length);
    }

    /**
     * 체크섬 검증
     */
    private void validateChecksums(byte[] messageBytes, Class<?> clazz, TcpMessage tcpMessage) {
        // 체크섬 필드 수집
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(TcpField.class))
                .sorted(Comparator.comparing(f -> f.getAnnotation(TcpField.class).order()))
                .toList();

        int position = 0;

        for (Field field : fields) {
            TcpField tcpField = field.getAnnotation(TcpField.class);
            field.setAccessible(true);

            int fieldLength = tcpField.length();
            AutoCalculate autoCalc = tcpField.autoCalculate();

            // 체크섬 필드인 경우 검증
            if (autoCalc == AutoCalculate.CHECKSUM_CRC16 ||
                    autoCalc == AutoCalculate.CHECKSUM_CRC32 ||
                    autoCalc == AutoCalculate.CHECKSUM_XOR) {

                if (position + fieldLength > messageBytes.length) {
                    log.warn("Not enough data to validate checksum at position {}", position);
                    return;
                }

                // 체크섬 필드 값 읽기
                byte[] checksumBytes = Arrays.copyOfRange(messageBytes, position, position + fieldLength);
                String charset = tcpField.charset().isEmpty() ? tcpMessage.charset() : tcpField.charset();
                String checksumValue = new String(checksumBytes, Charset.forName(charset)).trim();

                // 체크섬 계산 (체크섬 필드 제외)
                byte[] dataForChecksum = excludeField(messageBytes, position, fieldLength);
                String expectedChecksum = switch (autoCalc) {
                    case CHECKSUM_CRC16 -> {
                        int crc16 = ChecksumUtil.calculateCRC16(dataForChecksum);
                        yield String.format("%04X", crc16);
                    }
                    case CHECKSUM_CRC32 -> {
                        long crc32 = ChecksumUtil.calculateCRC32(dataForChecksum);
                        yield String.format("%08X", crc32);
                    }
                    case CHECKSUM_XOR -> {
                        int xor = ChecksumUtil.calculateXOR(dataForChecksum);
                        yield String.format("%02X", xor);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + autoCalc);
                };

                // 체크섬 비교
                if (!checksumValue.equals(expectedChecksum)) {
                    throw new IllegalArgumentException(
                            String.format("Checksum validation failed: expected %s but got %s",
                                    expectedChecksum, checksumValue));
                }

                log.debug("Checksum validated: {} = {}", autoCalc, checksumValue);
            }

            position += fieldLength;
        }
    }

    /**
     * 특정 필드를 제외한 바이트 배열 반환
     */
    private byte[] excludeField(byte[] messageBytes, int position, int length) {
        byte[] result = new byte[messageBytes.length - length];

        // position 이전 데이터
        if (position > 0) {
            System.arraycopy(messageBytes, 0, result, 0, position);
        }

        // position + length 이후 데이터
        int afterPosition = position + length;
        if (afterPosition < messageBytes.length) {
            System.arraycopy(messageBytes, afterPosition, result, position,
                    messageBytes.length - afterPosition);
        }

        return result;
    }

    /**
     * 필드 역직렬화 (중첩 객체, 리스트, 일반 타입 처리)
     */
    private Object deserializeField(byte[] fieldBytes, Field field, TcpField tcpField, TcpMessage parentMessage) {
        Class<?> fieldType = field.getType();

        // 1. 리스트/배열 처리
        if (tcpField.maxSize() > 0) {
            return deserializeList(fieldBytes, field, tcpField, parentMessage);
        }

        // 2. 중첩 객체 처리
        if (fieldType.isAnnotationPresent(TcpMessage.class)) {
            return deserializeNestedObject(fieldBytes, fieldType, tcpField, parentMessage);
        }

        // 3. 일반 타입 처리
        String charset = tcpField.charset().isEmpty() ? parentMessage.charset() : tcpField.charset();
        String fieldValue = new String(fieldBytes, Charset.forName(charset));

        // 패딩 제거 (단, 자동 계산 필드는 계산된 형식 그대로 유지)
        if (tcpField.autoCalculate() == AutoCalculate.NONE) {
            char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : parentMessage.paddingChar();
            fieldValue = removePadding(fieldValue, paddingChar, tcpField.align());
        }

        // 타입 변환
        return convertToType(fieldValue, fieldType, tcpField);
    }

    /**
     * 중첩 객체 역직렬화
     */
    private Object deserializeNestedObject(byte[] fieldBytes, Class<?> fieldType, TcpField tcpField, TcpMessage parentMessage) {
        // 모두 패딩인지 확인 (null 체크)
        if (isAllPadding(fieldBytes, tcpField, parentMessage)) {
            return null;
        }

        // 재귀적으로 역직렬화
        return deserialize(fieldBytes, fieldType);
    }

    /**
     * 리스트/배열 역직렬화
     */
    private Object deserializeList(byte[] fieldBytes, Field field, TcpField tcpField, TcpMessage parentMessage) {
        int maxSize = tcpField.maxSize();
        int itemLength = tcpField.itemLength();

        List<Object> items = new ArrayList<>();

        // 각 아이템 역직렬화
        for (int i = 0; i < maxSize; i++) {
            int start = i * itemLength;
            int end = start + itemLength;

            if (end > fieldBytes.length) {
                log.warn("List deserialization: not enough bytes for item {}", i);
                break;
            }

            byte[] itemBytes = Arrays.copyOfRange(fieldBytes, start, end);

            // 아이템이 모두 패딩인지 확인
            if (isAllPadding(itemBytes, tcpField, parentMessage)) {
                // 패딩만 있으면 리스트에 추가하지 않음 (빈 슬롯)
                continue;
            }

            // 리스트의 제네릭 타입 가져오기
            Class<?> itemType = getListItemType(field);

            Object item = deserializeListItem(itemBytes, itemType, tcpField, parentMessage);
            items.add(item);
        }

        return items;
    }

    /**
     * 리스트 아이템 역직렬화
     */
    private Object deserializeListItem(byte[] itemBytes, Class<?> itemType, TcpField tcpField, TcpMessage parentMessage) {
        // 아이템이 @TcpMessage를 가진 경우
        if (itemType.isAnnotationPresent(TcpMessage.class)) {
            return deserialize(itemBytes, itemType);
        }

        // 일반 타입 (String, Number 등)
        String charset = tcpField.charset().isEmpty() ? parentMessage.charset() : tcpField.charset();
        String itemValue = new String(itemBytes, Charset.forName(charset));

        char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : parentMessage.paddingChar();
        itemValue = removePadding(itemValue, paddingChar, tcpField.align());

        return convertToType(itemValue, itemType, tcpField);
    }

    /**
     * 모든 바이트가 패딩인지 확인
     */
    private boolean isAllPadding(byte[] bytes, TcpField tcpField, TcpMessage parentMessage) {
        if (bytes == null || bytes.length == 0) {
            return true;
        }

        String charset = tcpField.charset().isEmpty() ? parentMessage.charset() : tcpField.charset();
        char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : parentMessage.paddingChar();

        String value = new String(bytes, Charset.forName(charset));
        for (char c : value.toCharArray()) {
            if (c != paddingChar) {
                return false;
            }
        }
        return true;
    }

    /**
     * 리스트의 제네릭 타입 추출
     * 예: List<TransactionItem> → TransactionItem.class
     */
    private Class<?> getListItemType(Field field) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] typeArgs = paramType.getActualTypeArguments();

            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }

        // 제네릭 정보가 없으면 String.class 기본 반환
        log.warn("Could not determine generic type for field {}. Using String.class as default.", field.getName());
        return String.class;
    }

    /**
     * 패딩 제거
     */
    private String removePadding(String value, char paddingChar, Align align) {
        if (value.isEmpty()) {
            return value;
        }

        // LEFT 정렬이면 우측 패딩 제거
        if (align == Align.LEFT) {
            int end = value.length() - 1;
            while (end >= 0 && value.charAt(end) == paddingChar) {
                end--;
            }
            return value.substring(0, end + 1);
        }
        // RIGHT 정렬이면 좌측 패딩 제거
        else {
            int start = 0;
            // 최소 1자리는 남김 (모두 패딩인 경우 마지막 1자리 유지)
            while (start < value.length() - 1 && value.charAt(start) == paddingChar) {
                start++;
            }
            return value.substring(start);
        }
    }

    /**
     * 문자열을 대상 타입으로 변환
     */
    private Object convertToType(String value, Class<?> targetType, TcpField tcpField) {
        try {
            // String 타입 - 빈 문자열도 그대로 반환
            if (targetType == String.class) {
                return value;
            }

            // 빈 문자열이면 null 또는 기본값 반환 (숫자, 날짜 등)
            if (value.isEmpty()) {
                if (targetType.isPrimitive()) {
                    return getDefaultValue(targetType);
                }
                return null;
            }

            // Long 타입
            if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            }

            // Integer 타입
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            }

            // Double 타입
            if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            }

            // Float 타입
            if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(value);
            }

            // BigDecimal 타입
            if (targetType == BigDecimal.class) {
                return new BigDecimal(value);
            }

            // Boolean 타입 (Y/N, true/false, 1/0)
            if (targetType == Boolean.class || targetType == boolean.class) {
                return value.equalsIgnoreCase("Y")
                        || value.equalsIgnoreCase("true")
                        || value.equals("1");
            }

            // LocalDate 타입
            if (targetType == LocalDate.class) {
                String format = tcpField.format().isEmpty() ? "yyyyMMdd" : tcpField.format();
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(format));
            }

            // LocalDateTime 타입
            if (targetType == LocalDateTime.class) {
                String format = tcpField.format().isEmpty() ? "yyyyMMddHHmmss" : tcpField.format();
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(format));
            }

            // Enum 타입
            if (targetType.isEnum()) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
                return Enum.valueOf(enumType, value.trim());
            }

            throw new IllegalArgumentException("Unsupported type: " + targetType.getName());

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to convert value '%s' to type %s", value, targetType.getName()), e);
        }
    }

    /**
     * Primitive 타입의 기본값 반환
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\0';
        return null;
    }
}
