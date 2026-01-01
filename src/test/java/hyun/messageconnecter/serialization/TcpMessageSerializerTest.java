package hyun.messageconnecter.serialization;

import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.BankTransactionRequest;
import hyun.messageconnecter.fixture.tcp.SimpleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TCP 메시지 직렬화 엔진 테스트
 */
class TcpMessageSerializerTest {

    private TcpMessageSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new TcpMessageSerializer();
    }

    @Test
    @DisplayName("간단한 요청 메시지 직렬화 테스트")
    void testSimpleRequestSerialization() {
        // Given
        SimpleRequest request = new SimpleRequest();
        request.setMessageCode("TEST");
        request.setData("HELLO");
        request.setNumber(123L);

        // When
        byte[] bytes = serializer.serialize(request);

        // Then
        String result = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Result: [" + result + "]");
        System.out.println("Length: " + result.length());

        // 전체 길이 검증
        assertThat(result).hasSize(50);

        // 필드별 검증
        assertThat(result.substring(0, 10)).isEqualTo("TEST      "); // messageCode: 10바이트, 좌측 정렬
        assertThat(result.substring(10, 30)).isEqualTo("HELLO               "); // data: 20바이트, 좌측 정렬
        assertThat(result.substring(30, 40)).isEqualTo("0000000123"); // number: 10바이트, 우측 정렬, 제로 패딩
        assertThat(result.substring(40, 50)).isEqualTo("          "); // 나머지 10바이트 패딩
    }

    @Test
    @DisplayName("은행 거래 요청 메시지 직렬화 테스트")
    void testBankTransactionRequestSerialization() {
        // Given
        BankTransactionRequest request = new BankTransactionRequest();
        request.setMessageCode("TRX001");
        request.setAccountNumber("1234567890");
        request.setCustomerName("hongGilDong"); // UPPER 변환됨
        request.setTransactionDate(LocalDate.of(2025, 1, 25));
        request.setAmount(100000L);
        request.setTransactionType("D");

        // When
        byte[] bytes = serializer.serialize(request);

        // Then
        // EUC-KR 인코딩이지만 영문/숫자만 있어서 UTF-8로 읽어도 동일
        String result = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Result: [" + result + "]");
        System.out.println("Length: " + result.length());

        // 전체 길이 검증
        assertThat(result.length()).isGreaterThanOrEqualTo(104); // 필드 합계

        // 필드별 검증
        assertThat(result.substring(0, 10)).isEqualTo("TRX001    "); // messageCode
        assertThat(result.substring(10, 30)).isEqualTo("1234567890          "); // accountNumber
        assertThat(result.substring(30, 80)).isEqualTo("HONGGILDONG                                       "); // customerName: UPPER 변환
        assertThat(result.substring(80, 88)).isEqualTo("20250125"); // transactionDate
        assertThat(result.substring(88, 103)).isEqualTo("000000000100000"); // amount
        assertThat(result.substring(103, 104)).isEqualTo("D"); // transactionType
    }

    @Test
    @DisplayName("null 값 처리 테스트")
    void testNullValueHandling() {
        // Given
        SimpleRequest request = new SimpleRequest();
        request.setMessageCode("TEST");
        // data와 number는 null

        // When
        byte[] bytes = serializer.serialize(request);

        // Then
        String result = new String(bytes, StandardCharsets.UTF_8);
        assertThat(result).hasSize(50);
        assertThat(result.substring(0, 10)).isEqualTo("TEST      ");
        assertThat(result.substring(10, 30)).isEqualTo("                    "); // null -> 빈 문자열 -> 패딩
    }

    @Test
    @DisplayName("길이 초과 시 잘림 테스트")
    void testValueTruncation() {
        // Given
        SimpleRequest request = new SimpleRequest();
        request.setMessageCode("VERYLONGMESSAGECODE"); // 10바이트 초과
        request.setData("HELLO");
        request.setNumber(123L);

        // When
        byte[] bytes = serializer.serialize(request);

        // Then
        String result = new String(bytes, StandardCharsets.UTF_8);
        assertThat(result.substring(0, 10)).isEqualTo("VERYLONG" + "ME"); // 10바이트로 잘림
    }

    @Test
    @DisplayName("@TcpMessage 어노테이션 없으면 예외 발생")
    void testMissingTcpMessageAnnotation() {
        // Given
        Object invalidObject = new Object();

        // When & Then
        assertThatThrownBy(() -> serializer.serialize(invalidObject))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@TcpMessage annotation is required");
    }

    @Test
    @DisplayName("날짜 포맷팅 테스트")
    void testDateFormatting() {
        // Given
        BankTransactionRequest request = new BankTransactionRequest();
        request.setMessageCode("TEST");
        request.setAccountNumber("1234567890");
        request.setCustomerName("test");
        request.setTransactionDate(LocalDate.of(2025, 12, 31));
        request.setAmount(0L);
        request.setTransactionType("D");

        // When
        byte[] bytes = serializer.serialize(request);
        String result = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(result.substring(80, 88)).isEqualTo("20251231"); // yyyyMMdd 포맷
    }

    @Test
    @DisplayName("Transform.UPPER 테스트")
    void testTransformUpper() {
        // Given
        BankTransactionRequest request = new BankTransactionRequest();
        request.setMessageCode("TEST");
        request.setAccountNumber("1234567890");
        request.setCustomerName("lowercase"); // UPPER 변환됨
        request.setTransactionDate(LocalDate.now());
        request.setAmount(0L);
        request.setTransactionType("D");

        // When
        byte[] bytes = serializer.serialize(request);
        String result = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(result.substring(30, 80)).startsWith("LOWERCASE");
    }
}
