package hyun.messageconnecter.serialization;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.KoreanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 한글 인코딩 바이트 레벨 테스트
 * 멀티바이트 문자(한글)가 바이트 길이 기준으로 정확히 처리되는지 검증
 */
class KoreanEncodingTest {

    private TcpMessageSerializer serializer;
    private TcpMessageDeserializer deserializer;

    @BeforeEach
    void setUp() {
        serializer = new TcpMessageSerializer();
        deserializer = new TcpMessageDeserializer();
    }

    @Test
    @DisplayName("한글 EUC-KR 인코딩 바이트 길이 테스트")
    void testKoreanByteLength() {
        // Given
        String koreanName = "홍길동";
        byte[] bytes = koreanName.getBytes(Charset.forName("EUC-KR"));

        // Then - 한글 1글자 = 2바이트 (EUC-KR)
        assertThat(bytes.length).isEqualTo(6); // 3글자 * 2바이트
    }

    @Test
    @DisplayName("한글 포함 메시지 직렬화 - 바이트 길이 검증")
    void testKoreanMessageSerialization() {
        // Given
        KoreanRequest request = new KoreanRequest();
        request.setName("홍길동"); // 6바이트
        request.setAccountNumber("1234567890"); // 10바이트
        request.setAddress("서울특별시 강남구"); // 18바이트 (9글자 * 2)

        // When
        byte[] bytes = serializer.serialize(request);

        // Then
        assertThat(bytes.length).isEqualTo(100); // 전체 메시지 길이

        // 필드별 바이트 검증
        byte[] nameBytes = new byte[20];
        System.arraycopy(bytes, 0, nameBytes, 0, 20);
        String nameResult = new String(nameBytes, Charset.forName("EUC-KR")).trim();
        assertThat(nameResult).isEqualTo("홍길동");

        byte[] accountBytes = new byte[15];
        System.arraycopy(bytes, 20, accountBytes, 0, 15);
        String accountResult = new String(accountBytes, Charset.forName("EUC-KR")).trim();
        assertThat(accountResult).isEqualTo("1234567890");

        byte[] addressBytes = new byte[50];
        System.arraycopy(bytes, 35, addressBytes, 0, 50);
        String addressResult = new String(addressBytes, Charset.forName("EUC-KR")).trim();
        assertThat(addressResult).isEqualTo("서울특별시 강남구");
    }

    @Test
    @DisplayName("한글 포함 메시지 역직렬화 - 바이트 길이 검증")
    void testKoreanMessageDeserialization() {
        // Given
        KoreanRequest original = new KoreanRequest();
        original.setName("김철수");
        original.setAccountNumber("9876543210");
        original.setAddress("부산광역시 해운대구");

        // When - 직렬화 후 역직렬화
        byte[] bytes = serializer.serialize(original);
        KoreanRequest deserialized = deserializer.deserialize(bytes, KoreanRequest.class);

        // Then
        assertThat(deserialized.getName()).isEqualTo("김철수");
        assertThat(deserialized.getAccountNumber()).isEqualTo("9876543210");
        assertThat(deserialized.getAddress()).isEqualTo("부산광역시 해운대구");
    }

    @Test
    @DisplayName("한글 + 영문 혼합 메시지 테스트")
    void testMixedKoreanEnglishMessage() {
        // Given
        KoreanRequest request = new KoreanRequest();
        request.setName("홍길동ABC"); // 한글 3글자(6바이트) + 영문 3글자(3바이트) = 9바이트
        request.setAccountNumber("ACCT123");
        request.setAddress("서울 Gangnam 123");

        // When
        byte[] bytes = serializer.serialize(request);
        KoreanRequest deserialized = deserializer.deserialize(bytes, KoreanRequest.class);

        // Then
        assertThat(deserialized.getName()).isEqualTo("홍길동ABC");
        assertThat(deserialized.getAccountNumber()).isEqualTo("ACCT123");
        assertThat(deserialized.getAddress()).isEqualTo("서울 Gangnam 123");
    }

    @Test
    @DisplayName("긴 한글 텍스트 잘라내기 테스트 (TRUNCATE_SAFE)")
    void testLongKoreanTextTruncation() {
        // Given
        KoreanRequest request = new KoreanRequest();
        // 20바이트를 초과하는 긴 이름 (한글 15글자 = 30바이트)
        request.setName("홍길동김철수이영희박민수최지훈"); // 30바이트
        request.setAccountNumber("1234567890");
        request.setAddress("서울");

        // When
        byte[] bytes = serializer.serialize(request);

        // Then
        assertThat(bytes.length).isEqualTo(100);

        // name 필드는 20바이트로 안전하게 잘림
        byte[] nameBytes = new byte[20];
        System.arraycopy(bytes, 0, nameBytes, 0, 20);
        String nameResult = new String(nameBytes, Charset.forName("EUC-KR")).trim();

        // 10글자까지 들어갈 수 있음 (10글자 * 2바이트 = 20바이트)
        assertThat(nameResult.length()).isLessThanOrEqualTo(10);
        assertThat(nameResult).startsWith("홍길동");
    }

    @Test
    @DisplayName("Roundtrip 테스트 - 직렬화 후 역직렬화가 원본과 동일")
    void testKoreanRoundtrip() {
        // Given
        KoreanRequest original = new KoreanRequest();
        original.setName("이순신");
        original.setAccountNumber("1111222233");
        original.setAddress("인천광역시 남동구 구월동");

        // When
        byte[] serialized = serializer.serialize(original);
        KoreanRequest deserialized = deserializer.deserialize(serialized, KoreanRequest.class);

        // Then
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("빈 한글 필드 테스트")
    void testEmptyKoreanField() {
        // Given
        KoreanRequest request = new KoreanRequest();
        request.setName(""); // 빈 문자열
        request.setAccountNumber("1234567890");
        request.setAddress("");

        // When
        byte[] bytes = serializer.serialize(request);
        KoreanRequest deserialized = deserializer.deserialize(bytes, KoreanRequest.class);

        // Then - 빈 문자열은 빈 문자열로 유지됨 (null이 아님)
        assertThat(deserialized.getName()).isEmpty();
        assertThat(deserialized.getAccountNumber()).isEqualTo("1234567890");
        assertThat(deserialized.getAddress()).isEmpty();
    }

    @Test
    @DisplayName("실제 사용 예제: 은행 거래 메시지 직렬화")
    void testRealWorldExample_BankTransaction() {
        // Given - 실제 은행 거래 시나리오
        KoreanRequest request = new KoreanRequest();
        request.setName("홍길동");
        request.setAccountNumber("110-123-456789");
        request.setAddress("서울특별시 강남구 테헤란로 123");

        System.out.println("\n=== 은행 거래 메시지 직렬화 예제 ===");
        System.out.println("입력:");
        System.out.println("  - 이름: " + request.getName());
        System.out.println("  - 계좌번호: " + request.getAccountNumber());
        System.out.println("  - 주소: " + request.getAddress());

        // When - 직렬화
        byte[] serialized = serializer.serialize(request);

        System.out.println("\n직렬화 결과:");
        System.out.println("  - 총 바이트 길이: " + serialized.length + " bytes");
        System.out.println("  - 예상 길이: 100 bytes (totalLength)");

        // 각 필드별 바이트 확인
        byte[] nameBytes = new byte[20];
        System.arraycopy(serialized, 0, nameBytes, 0, 20);
        System.out.println("  - name 필드 (0~19): [" + new String(nameBytes, Charset.forName("EUC-KR")) + "]");
        System.out.println("    → 바이트 길이: 20 (한글 3글자=6바이트 + 패딩 14바이트)");

        byte[] accountBytes = new byte[15];
        System.arraycopy(serialized, 20, accountBytes, 0, 15);
        System.out.println("  - accountNumber 필드 (20~34): [" + new String(accountBytes, Charset.forName("EUC-KR")) + "]");

        byte[] addressBytes = new byte[50];
        System.arraycopy(serialized, 35, addressBytes, 0, 50);
        System.out.println("  - address 필드 (35~84): [" + new String(addressBytes, Charset.forName("EUC-KR")).trim() + "]");
        System.out.println("    → 바이트 길이: 50 (한글 12글자=24바이트 + 영문/숫자/공백 + 패딩)");

        // 역직렬화
        KoreanRequest deserialized = deserializer.deserialize(serialized, KoreanRequest.class);

        System.out.println("\n역직렬화 결과:");
        System.out.println("  - 이름: " + deserialized.getName());
        System.out.println("  - 계좌번호: " + deserialized.getAccountNumber());
        System.out.println("  - 주소: " + deserialized.getAddress());

        // Then - 검증
        assertThat(serialized.length).isEqualTo(100);
        assertThat(deserialized.getName()).isEqualTo("홍길동");
        assertThat(deserialized.getAccountNumber()).isEqualTo("110-123-456789");
        assertThat(deserialized.getAddress()).isEqualTo("서울특별시 강남구 테헤란로 123");

    }

    @Test
    @DisplayName("실제 사용 예제: 바이트 길이 검증")
    void testRealWorldExample_ByteLengthValidation() {
        System.out.println("\n=== 바이트 길이 검증 예제 ===");

        // 예제 1: 한글만
        String koreanOnly = "홍길동";
        byte[] koreanBytes = koreanOnly.getBytes(Charset.forName("EUC-KR"));
        System.out.println("1. 한글만: \"" + koreanOnly + "\"");
        System.out.println("   - 문자 길이: " + koreanOnly.length() + " 글자");
        System.out.println("   - 바이트 길이: " + koreanBytes.length + " bytes (EUC-KR)");
        System.out.println("   - 계산: 3글자 * 2바이트/글자 = 6바이트");

        assertThat(koreanBytes.length).isEqualTo(6);

        // 예제 2: 한글 + 영문
        String mixed = "홍길동ABC";
        byte[] mixedBytes = mixed.getBytes(Charset.forName("EUC-KR"));
        System.out.println("\n2. 한글 + 영문: \"" + mixed + "\"");
        System.out.println("   - 문자 길이: " + mixed.length() + " 글자");
        System.out.println("   - 바이트 길이: " + mixedBytes.length + " bytes (EUC-KR)");
        System.out.println("   - 계산: 한글 3글자(6바이트) + 영문 3글자(3바이트) = 9바이트");

        assertThat(mixedBytes.length).isEqualTo(9);

        // 예제 3: 긴 주소
        String longAddress = "서울특별시 강남구 테헤란로 123번길 45-67";
        byte[] addressBytes = longAddress.getBytes(Charset.forName("EUC-KR"));
        System.out.println("\n3. 긴 주소: \"" + longAddress + "\"");
        System.out.println("   - 문자 길이: " + longAddress.length() + " 글자");
        System.out.println("   - 바이트 길이: " + addressBytes.length + " bytes (EUC-KR)");
        System.out.println("   - 한글 글자 수: " + longAddress.replaceAll("[^\\uAC00-\\uD7A3]", "").length());
        System.out.println("   - 영문/숫자/특수문자: " + longAddress.replaceAll("[\\uAC00-\\uD7A3]", "").length());

        // 예제 4: 50바이트 필드에 맞추기
        System.out.println("\n4. 50바이트 필드에 긴 주소 넣기:");
        if (addressBytes.length > 50) {
            System.out.println("   - 원본 바이트 길이: " + addressBytes.length + " bytes (50바이트 초과!)");
            System.out.println("   - TRUNCATE_SAFE 정책 적용 → 멀티바이트 안전하게 자름");
        }

    }

    @Test
    @DisplayName("실제 사용 예제: null vs 빈 문자열 구분")
    void testRealWorldExample_NullVsEmpty() {
        System.out.println("\n=== null vs 빈 문자열 구분 예제 ===");

        // Case 1: null 필드
        KoreanRequest request1 = new KoreanRequest();
        request1.setName(null);
        request1.setAccountNumber("123456");
        request1.setAddress("주소");

        byte[] bytes1 = serializer.serialize(request1);
        KoreanRequest result1 = deserializer.deserialize(bytes1, KoreanRequest.class);

        System.out.println("Case 1: null 필드");
        System.out.println("  입력: name = null");
        System.out.println("  출력: name = \"" + result1.getName() + "\" (빈 문자열)");
        assertThat(result1.getName()).isEmpty();

        // Case 2: 빈 문자열 필드
        KoreanRequest request2 = new KoreanRequest();
        request2.setName("");
        request2.setAccountNumber("123456");
        request2.setAddress("주소");

        byte[] bytes2 = serializer.serialize(request2);
        KoreanRequest result2 = deserializer.deserialize(bytes2, KoreanRequest.class);

        System.out.println("\nCase 2: 빈 문자열 필드");
        System.out.println("  입력: name = \"\" (빈 문자열)");
        System.out.println("  출력: name = \"" + result2.getName() + "\" (빈 문자열)");
        assertThat(result2.getName()).isEmpty();

        // Case 3: 공백만 있는 필드
        KoreanRequest request3 = new KoreanRequest();
        request3.setName("   ");
        request3.setAccountNumber("123456");
        request3.setAddress("주소");

        byte[] bytes3 = serializer.serialize(request3);
        KoreanRequest result3 = deserializer.deserialize(bytes3, KoreanRequest.class);

        System.out.println("\nCase 3: 공백만 있는 필드");
        System.out.println("  입력: name = \"   \" (공백 3개)");
        System.out.println("  출력: name = \"" + result3.getName() + "\" (패딩 제거 후 빈 문자열)");
        assertThat(result3.getName()).isEmpty();
    }
}
