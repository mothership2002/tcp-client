package hyun.messageconnecter;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;
import hyun.messageconnecter.enums.AutoCalculate;
import hyun.messageconnecter.enums.OverflowPolicy;
import hyun.messageconnecter.enums.Transform;
import hyun.messageconnecter.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * TCP 메시지 직렬화 엔진 (바이트 레벨)
 * 객체를 고정길이 바이트 배열로 변환
 * <p>
 * 주요 개선사항:
 * - 문자열 길이가 아닌 바이트 길이 기준으로 처리
 * - 필드별 인코딩 지원
 * - 멀티바이트 문자 안전 처리
 * - 오버플로우 정책 지원
 */
public class TcpMessageSerializer {

    private static final Logger log = LoggerFactory.getLogger(TcpMessageSerializer.class);

    /**
     * 자동 계산 필드 메타데이터
     */
    private record AutoCalculatedField(
            int position,
            int length,
            AutoCalculate type,
            String charset,
            Align align,
            char paddingChar) {
    }

    /**
     * 객체를 바이트 배열로 직렬화 (2-pass 처리)
     * <p>
     * Pass 1: 일반 필드 직렬화 (자동 계산 필드는 placeholder)
     * Pass 2: 자동 계산 필드 값 계산 및 삽입
     * <p>
     * STX/ETX가 설정된 경우 최종 메시지 앞뒤에 추가
     *
     * @param message 직렬화할 객체
     * @return 바이트 배열
     */
    public byte[] serialize(Object message) {
        Class<?> clazz = message.getClass();

        // @TcpMessage 어노테이션 확인
        TcpMessage tcpMessage = clazz.getAnnotation(TcpMessage.class);
        if (tcpMessage == null) {
            throw new IllegalArgumentException("@TcpMessage annotation is required on class: " + clazz.getName());
        }

        // 필드 수집 및 순서대로 정렬
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(TcpField.class))
                .sorted(Comparator.comparing(f -> f.getAnnotation(TcpField.class).order()))
                .toList();

        // === Pass 1: 일반 필드 직렬화 ===
        int totalLength = tcpMessage.totalLength();
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        List<AutoCalculatedField> autoFields = new ArrayList<>();

        for (Field field : fields) {
            TcpField tcpField = field.getAnnotation(TcpField.class);
            field.setAccessible(true);

            try {
                // 자동 계산 필드인지 확인
                if (tcpField.autoCalculate() != AutoCalculate.NONE) {
                    // placeholder로 채우고 나중에 계산
                    int position = buffer.position();
                    String charset = tcpField.charset().isEmpty() ? tcpMessage.charset() : tcpField.charset();
                    char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : tcpMessage.paddingChar();

                    autoFields.add(new AutoCalculatedField(
                            position,
                            tcpField.length(),
                            tcpField.autoCalculate(),
                            charset,
                            tcpField.align(),
                            paddingChar));

                    // placeholder (0x00으로 채움)
                    byte[] placeholder = new byte[tcpField.length()];
                    buffer.put(placeholder);

                    log.debug("Auto-calculated field at position {}: {} (length: {})",
                            position, tcpField.autoCalculate(), tcpField.length());
                } else {
                    // 일반 필드 직렬화
                    Object value = field.get(message);
                    byte[] fieldBytes = serializeField(value, tcpField, tcpMessage);
                    buffer.put(fieldBytes);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }

        // 나머지 공간을 패딩으로 채움
        int remaining = buffer.remaining();
        if (remaining > 0) {
            byte[] paddingBytes = createPaddingBytes(remaining, tcpMessage.paddingChar(), tcpMessage.charset());
            buffer.put(paddingBytes);
        }

        // 버퍼가 초과하면 에러
        if (buffer.position() > totalLength) {
            throw new IllegalStateException(
                    String.format("Message length %d exceeds total length %d", buffer.position(), totalLength));
        }

        byte[] messageBytes = buffer.array();

        // === Pass 2: 자동 계산 필드 값 삽입 ===
        for (AutoCalculatedField autoField : autoFields) {
            byte[] calculatedBytes = calculateFieldValue(autoField, messageBytes, totalLength);
            System.arraycopy(calculatedBytes, 0, messageBytes, autoField.position, autoField.length);
            log.debug("Inserted auto-calculated value at position {}: {} bytes",
                    autoField.position, calculatedBytes.length);
        }

        // === STX/ETX 추가 ===
        byte stx = tcpMessage.stx();
        byte etx = tcpMessage.etx();

        if (stx != 0x00 || etx != 0x00) {
            int finalLength = totalLength;
            if (stx != 0x00) finalLength++;
            if (etx != 0x00) finalLength++;

            ByteBuffer finalBuffer = ByteBuffer.allocate(finalLength);

            if (stx != 0x00) {
                finalBuffer.put(stx);
                log.debug("Added STX: 0x{}", String.format("%02X", stx));
            }

            finalBuffer.put(messageBytes);

            if (etx != 0x00) {
                finalBuffer.put(etx);
                log.debug("Added ETX: 0x{}", String.format("%02X", etx));
            }

            return finalBuffer.array();
        }

        return messageBytes;
    }

    /**
     * 자동 계산 필드 값 계산
     */
    private byte[] calculateFieldValue(AutoCalculatedField autoField, byte[] messageBytes, int totalLength) {
        String valueString = switch (autoField.type) {
            case MESSAGE_LENGTH -> String.valueOf(totalLength);
            case CHECKSUM_CRC16 -> {
                // CRC16 계산 (체크섬 필드 제외)
                byte[] dataForChecksum = excludeField(messageBytes, autoField.position, autoField.length);
                int crc16 = ChecksumUtil.calculateCRC16(dataForChecksum);
                yield String.format("%04X", crc16); // 16진수 4자리
            }
            case CHECKSUM_CRC32 -> {
                // CRC32 계산 (체크섬 필드 제외)
                byte[] dataForChecksum = excludeField(messageBytes, autoField.position, autoField.length);
                long crc32 = ChecksumUtil.calculateCRC32(dataForChecksum);
                yield String.format("%08X", crc32); // 16진수 8자리
            }
            case CHECKSUM_XOR -> {
                // XOR 계산 (체크섬 필드 제외)
                byte[] dataForChecksum = excludeField(messageBytes, autoField.position, autoField.length);
                int xor = ChecksumUtil.calculateXOR(dataForChecksum);
                yield String.format("%02X", xor); // 16진수 2자리
            }
            default -> throw new IllegalStateException("Unknown auto-calculate type: " + autoField.type);
        };

        // 문자열을 바이트로 변환하고 패딩 적용
        byte[] valueBytes = valueString.getBytes(Charset.forName(autoField.charset));
        return applyBytePadding(
                valueBytes,
                autoField.length,
                autoField.paddingChar,
                autoField.align,
                OverflowPolicy.TRUNCATE_SAFE,
                autoField.charset);
    }

    /**
     * 특정 필드를 제외한 바이트 배열 반환
     * 체크섬 계산 시 체크섬 필드 자신은 제외해야 함
     */
    private byte[] excludeField(byte[] messageBytes, int position, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length - length);

        // position 이전 데이터
        if (position > 0) {
            buffer.put(messageBytes, 0, position);
        }

        // position + length 이후 데이터
        int afterPosition = position + length;
        if (afterPosition < messageBytes.length) {
            buffer.put(messageBytes, afterPosition, messageBytes.length - afterPosition);
        }

        return buffer.array();
    }

    /**
     * 단일 필드를 바이트 배열로 직렬화
     */
    private byte[] serializeField(Object value, TcpField tcpField, TcpMessage tcpMessage) {
        int fieldLength = tcpField.length();
        String charset = tcpField.charset().isEmpty() ? tcpMessage.charset() : tcpField.charset();
        char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : tcpMessage.paddingChar();

        // 1. 리스트/배열 처리
        if (tcpField.maxSize() > 0) {
            return serializeList(value, tcpField, tcpMessage);
        }

        // 2. 중첩 객체 처리
        if (value != null && value.getClass().isAnnotationPresent(TcpMessage.class)) {
            return serializeNestedObject(value, tcpField, tcpMessage);
        }

        // 3. 일반 필드 처리 (기존 로직)
        // 값을 문자열로 변환
        String stringValue = convertToString(value, tcpField);

        // Transform 적용
        stringValue = applyTransform(stringValue, tcpField.transform());

        // 문자열을 바이트로 변환
        byte[] valueBytes = stringValue.getBytes(Charset.forName(charset));

        // 바이트 길이 처리 (패딩 또는 잘라내기)
        return applyBytePadding(valueBytes, fieldLength, paddingChar, tcpField.align(),
                tcpMessage.overflowPolicy(), charset);
    }

    /**
     * 값을 문자열로 변환
     * <p>
     * 중요: null은 빈 문자열로 변환됩니다.
     * 역직렬화 시에도 빈 바이트는 빈 문자열로 변환되므로
     * null → serialize → deserialize → "" 가 됩니다.
     */
    private String convertToString(Object value, TcpField tcpField) {
        if (value == null) {
            return "";  // null은 빈 문자열로 변환
        }

        String stringValue;

        // 타입별 처리
        switch (value) {
            case LocalDate date -> {
                String format = tcpField.format().isEmpty() ? "yyyyMMdd" : tcpField.format();
                stringValue = date.format(DateTimeFormatter.ofPattern(format));
            }
            case LocalDateTime dateTime -> {
                String format = tcpField.format().isEmpty() ? "yyyyMMddHHmmss" : tcpField.format();
                stringValue = dateTime.format(DateTimeFormatter.ofPattern(format));
            }
            case BigDecimal decimal -> {
                if (!tcpField.format().isEmpty()) {
                    DecimalFormat df = new DecimalFormat(tcpField.format());
                    stringValue = df.format(decimal);
                } else {
                    stringValue = decimal.toPlainString();
                }
            }
            case Number number -> {
                // 숫자 포맷팅
                if (!tcpField.format().isEmpty()) {
                    DecimalFormat df = new DecimalFormat(tcpField.format());
                    stringValue = df.format(value);
                } else {
                    stringValue = value.toString();
                }
            }
            case Boolean b ->
                // Boolean은 Y/N으로 변환
                    stringValue = b ? "Y" : "N";
            default -> stringValue = value.toString();
        }

        return stringValue;
    }

    /**
     * Transform 적용
     */
    private String applyTransform(String value, Transform transform) {
        return switch (transform) {
            case CAMEL_TO_SNAKE -> value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            case CAMEL_TO_KEBAB -> value.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
            case UPPER -> value.toUpperCase();
            case LOWER -> value.toLowerCase();
            default -> value;
        };
    }

    /**
     * 바이트 레벨 패딩 적용
     *
     * @param valueBytes 원본 바이트 배열
     * @param targetLength 목표 바이트 길이
     * @param paddingChar 패딩 문자
     * @param align 정렬 방향
     * @param overflowPolicy 오버플로우 정책
     * @param charset 인코딩
     * @return 패딩이 적용된 바이트 배열
     */
    private byte[] applyBytePadding(byte[] valueBytes, int targetLength, char paddingChar,
            Align align, OverflowPolicy overflowPolicy, String charset) {

        int currentLength = valueBytes.length;

        // 길이가 같으면 그대로 반환
        if (currentLength == targetLength) {
            return valueBytes;
        }

        // 길이가 초과하는 경우
        if (currentLength > targetLength) {
            return handleOverflow(valueBytes, targetLength, overflowPolicy, charset);
        }

        // 길이가 부족한 경우 - 패딩 추가
        int paddingLength = targetLength - currentLength;
        byte[] paddingBytes = createPaddingBytes(paddingLength, paddingChar, charset);

        ByteBuffer buffer = ByteBuffer.allocate(targetLength);

        if (align == Align.LEFT) {
            // 좌측 정렬: 값 + 패딩
            buffer.put(valueBytes);
            buffer.put(paddingBytes);
        } else {
            // 우측 정렬: 패딩 + 값
            buffer.put(paddingBytes);
            buffer.put(valueBytes);
        }

        return buffer.array();
    }

    /**
     * 길이 초과 처리
     */
    private byte[] handleOverflow(byte[] valueBytes, int targetLength, OverflowPolicy policy, String charset) {
        return switch (policy) {
            case ERROR -> throw new IllegalStateException(
                    String.format("Field value length %d exceeds target length %d",
                            valueBytes.length, targetLength));
            case TRUNCATE -> {
                // 단순 잘라내기
                log.warn("Truncating {} bytes to {} bytes (may break multibyte characters)",
                        valueBytes.length, targetLength);
                yield Arrays.copyOf(valueBytes, targetLength);
            }
            default ->
                // 멀티바이트 안전 잘라내기
                    truncateSafe(valueBytes, targetLength, charset);
        };
    }

    /**
     * 멀티바이트 문자를 안전하게 잘라냄
     * 불완전한 바이트 시퀀스를 방지
     */
    private byte[] truncateSafe(byte[] valueBytes, int targetLength, String charsetName) {
        if (valueBytes.length <= targetLength) {
            return valueBytes;
        }

        Charset charset = Charset.forName(charsetName);

        // 바이트를 문자열로 디코딩 후 다시 인코딩하면서 길이 조정
        String original = new String(valueBytes, charset);

        // 문자를 하나씩 제거하면서 바이트 길이가 목표 이하가 될 때까지 반복
        for (int charLength = original.length() - 1; charLength > 0; charLength--) {
            String truncated = original.substring(0, charLength);
            byte[] truncatedBytes = truncated.getBytes(charset);

            if (truncatedBytes.length <= targetLength) {
                // 나머지 공간을 패딩으로 채움
                if (truncatedBytes.length < targetLength) {
                    int paddingLength = targetLength - truncatedBytes.length;
                    byte[] result = Arrays.copyOf(truncatedBytes, targetLength);
                    // 패딩은 공백(0x20)으로 채움
                    Arrays.fill(result, truncatedBytes.length, targetLength, (byte) 0x20);
                    log.warn("Safe truncation: {} chars -> {} chars ({} bytes)",
                            original.length(), charLength, truncatedBytes.length);
                    return result;
                }
                return truncatedBytes;
            }
        }

        // 최악의 경우 빈 바이트 배열 반환 (모든 문자가 목표 길이를 초과)
        log.warn("Could not safely truncate. Returning empty bytes with padding.");
        return createPaddingBytes(targetLength, ' ', charsetName);
    }

    /**
     * 중첩 객체 직렬화
     * 필드 타입이 @TcpMessage를 가진 경우 재귀적으로 직렬화
     */
    private byte[] serializeNestedObject(Object value, TcpField tcpField, TcpMessage parentMessage) {
        if (value == null) {
            // null이면 필드 길이만큼 패딩으로 채움
            String charset = tcpField.charset().isEmpty() ? parentMessage.charset() : tcpField.charset();
            char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : parentMessage.paddingChar();
            return createPaddingBytes(tcpField.length(), paddingChar, charset);
        }

        TcpMessage nestedMessage = value.getClass().getAnnotation(TcpMessage.class);

        // 검증: 중첩 객체의 totalLength가 필드 length와 일치하는지 확인
        if (nestedMessage.totalLength() != tcpField.length()) {
            throw new IllegalArgumentException(
                    String.format("Nested object totalLength %d does not match field length %d",
                            nestedMessage.totalLength(), tcpField.length()));
        }

        // 재귀적으로 직렬화
        byte[] nestedBytes = serialize(value);

        log.debug("Serialized nested object: {} bytes", nestedBytes.length);
        return nestedBytes;
    }

    /**
     * 리스트/배열 직렬화
     * maxSize > 0인 경우 리스트로 간주하고 각 아이템을 직렬화
     */
    private byte[] serializeList(Object value, TcpField tcpField, TcpMessage parentMessage) {
        int maxSize = tcpField.maxSize();
        int itemLength = tcpField.itemLength();
        int fieldLength = tcpField.length();
        String charset = tcpField.charset().isEmpty() ? parentMessage.charset() : tcpField.charset();
        char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : parentMessage.paddingChar();

        // 검증: itemLength가 지정되어야 함
        if (itemLength <= 0) {
            throw new IllegalArgumentException("itemLength must be specified when maxSize > 0");
        }

        // 검증: length = maxSize × itemLength
        if (fieldLength != maxSize * itemLength) {
            throw new IllegalArgumentException(
                    String.format("Field length %d does not match maxSize %d × itemLength %d = %d",
                            fieldLength, maxSize, itemLength, maxSize * itemLength));
        }

        ByteBuffer buffer = ByteBuffer.allocate(fieldLength);

        if (value == null) {
            // null이면 전체를 패딩으로 채움
            byte[] paddingBytes = createPaddingBytes(fieldLength, paddingChar, charset);
            buffer.put(paddingBytes);
        } else {
            // Collection으로 변환
            Collection<?> items;
            if (value instanceof Collection) {
                items = (Collection<?>) value;
            } else if (value.getClass().isArray()) {
                // 배열을 리스트로 변환 (primitive 배열 지원)
                items = arrayToList(value);
            } else {
                throw new IllegalArgumentException("List field must be Collection or Array type");
            }

            int itemCount = 0;

            // 각 아이템 직렬화
            for (Object item : items) {
                if (itemCount >= maxSize) {
                    // 오버플로우: 경고 후 무시
                    log.warn("List size {} exceeds maxSize {}. Extra items will be ignored.",
                            items.size(), maxSize);
                    break;
                }

                byte[] itemBytes = serializeListItem(item, itemLength, tcpField, parentMessage);
                buffer.put(itemBytes);
                itemCount++;
            }

            // 나머지 공간을 패딩으로 채움
            int remaining = maxSize - itemCount;
            if (remaining > 0) {
                int remainingBytes = remaining * itemLength;
                byte[] paddingBytes = createPaddingBytes(remainingBytes, paddingChar, charset);
                buffer.put(paddingBytes);
                log.debug("List has {} items, filled {} empty slots with padding", itemCount, remaining);
            }
        }

        return buffer.array();
    }

    /**
     * 리스트의 단일 아이템 직렬화
     */
    private byte[] serializeListItem(Object item, int itemLength, TcpField tcpField, TcpMessage parentMessage) {
        String charset = tcpField.charset().isEmpty() ? parentMessage.charset() : tcpField.charset();
        char paddingChar = tcpField.paddingChar() != ' ' ? tcpField.paddingChar() : parentMessage.paddingChar();

        if (item == null) {
            // null 아이템은 패딩으로 채움
            return createPaddingBytes(itemLength, paddingChar, charset);
        }

        // 아이템이 @TcpMessage를 가진 경우
        if (item.getClass().isAnnotationPresent(TcpMessage.class)) {
            TcpMessage itemMessage = item.getClass().getAnnotation(TcpMessage.class);

            // 검증: 아이템의 totalLength가 itemLength와 일치하는지 확인
            if (itemMessage.totalLength() != itemLength) {
                throw new IllegalArgumentException(
                        String.format("List item totalLength %d does not match itemLength %d",
                                itemMessage.totalLength(), itemLength));
            }

            return serialize(item);
        }

        // 일반 타입 (String, Number 등)
        String stringValue = convertToString(item, tcpField);
        stringValue = applyTransform(stringValue, tcpField.transform());
        byte[] valueBytes = stringValue.getBytes(Charset.forName(charset));

        return applyBytePadding(valueBytes, itemLength, paddingChar, tcpField.align(),
                parentMessage.overflowPolicy(), charset);
    }

    /**
     * 패딩 바이트 배열 생성
     */
    private byte[] createPaddingBytes(int length, char paddingChar, String charset) {
        if (length <= 0) {
            return new byte[0];
        }

        // 패딩 문자를 바이트로 변환
        byte[] singleCharBytes = String.valueOf(paddingChar).getBytes(Charset.forName(charset));

        // 패딩 문자가 멀티바이트일 수 있으므로 처리
        if (singleCharBytes.length == 1) {
            // 단일 바이트 패딩 (일반적인 경우)
            byte[] padding = new byte[length];
            Arrays.fill(padding, singleCharBytes[0]);
            return padding;
        } else {
            // 멀티바이트 패딩 (드문 경우)
            ByteBuffer buffer = ByteBuffer.allocate(length);
            int written = 0;
            while (written + singleCharBytes.length <= length) {
                buffer.put(singleCharBytes);
                written += singleCharBytes.length;
            }
            // 나머지는 공백으로 채움
            while (buffer.hasRemaining()) {
                buffer.put((byte) 0x20);
            }
            return buffer.array();
        }
    }

    /**
     * 배열을 리스트로 변환 (primitive 배열 포함)
     *
     * @param array 배열 (Object[] 또는 primitive 배열)
     * @return 리스트
     */
    private List<?> arrayToList(Object array) {
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Input must be an array");
        }

        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            list.add(Array.get(array, i));  // primitive 자동 boxing
        }

        return list;
    }
}
