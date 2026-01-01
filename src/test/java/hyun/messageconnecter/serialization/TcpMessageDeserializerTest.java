package hyun.messageconnecter.serialization;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.fixture.tcp.BankTransactionResponse;
import hyun.messageconnecter.fixture.tcp.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TCP 메시지 역직렬화 엔진 테스트
 */
class TcpMessageDeserializerTest {

    private TcpMessageDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new TcpMessageDeserializer();
    }

    @Test
    @DisplayName("간단한 응답 메시지 역직렬화 테스트")
    void testSimpleResponseDeserialization() {
        // Given: "0000" + "Success             " + "      " (총 30바이트)
        String data = "0000Success                   ";
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        // When
        SimpleResponse response = deserializer.deserialize(bytes, SimpleResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResultCode()).isEqualTo("0000");
        assertThat(response.getResultMessage()).isEqualTo("Success");
    }

    @Test
    @DisplayName("은행 거래 응답 메시지 역직렬화 테스트")
    void testBankTransactionResponseDeserialization() {
        // Given
        // resultCode: "0000" (4바이트, LEFT)
        // resultMessage: "정상 처리되었습니다." + 패딩 (100바이트, LEFT)
        // transactionId: "TXN20250125001" + 패딩 (20바이트, LEFT)
        // balanceAfter: "000000001234567" (15바이트, RIGHT, 제로 패딩)
        // 나머지 11바이트 패딩

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s", "0000"));
        sb.append(String.format("%-100s", "Transaction completed"));
        sb.append(String.format("%-20s", "TXN20250125001"));
        sb.append("000000001234567");
        sb.append(String.format("%-11s", "")); // 나머지 패딩

        String data = sb.toString();
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        System.out.println("Data length: " + data.length());
        System.out.println("Data: [" + data + "]");

        // When
        BankTransactionResponse response = deserializer.deserialize(bytes, BankTransactionResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResultCode()).isEqualTo("0000");
        assertThat(response.getResultMessage()).isEqualTo("Transaction completed");
        assertThat(response.getTransactionId()).isEqualTo("TXN20250125001");
        assertThat(response.getBalanceAfter()).isEqualTo(1234567L);
    }

    @Test
    @DisplayName("빈 필드 처리 테스트")
    void testEmptyFieldHandling() {
        // Given: resultCode만 있고 나머지는 빈 값
        String data = String.format("%-30s", "E001");
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        // When
        SimpleResponse response = deserializer.deserialize(bytes, SimpleResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResultCode()).isEqualTo("E001");
        assertThat(response.getResultMessage()).isEmpty();
    }

    @Test
    @DisplayName("제로 패딩된 숫자 역직렬화 테스트")
    void testZeroPaddedNumberDeserialization() {
        // Given
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s", "0000"));
        sb.append(String.format("%-100s", "OK"));
        sb.append(String.format("%-20s", "TXN001"));
        sb.append("000000000000123"); // 123을 15바이트 제로 패딩
        sb.append(String.format("%-11s", ""));

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        // When
        BankTransactionResponse response = deserializer.deserialize(bytes, BankTransactionResponse.class);

        // Then
        assertThat(response.getBalanceAfter()).isEqualTo(123L);
    }

    @Test
    @DisplayName("@TcpMessage 어노테이션 없으면 예외 발생")
    void testMissingTcpMessageAnnotation() {
        // Given
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);

        // When & Then
        assertThatThrownBy(() -> deserializer.deserialize(bytes, Object.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@TcpMessage annotation is required");
    }

    @Test
    @DisplayName("데이터 길이 부족 시 빈 문자열로 처리")
    void testInsufficientDataLength() {
        // Given: 30바이트보다 짧은 데이터
        String data = "0000OK";
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        // When
        SimpleResponse response = deserializer.deserialize(bytes, SimpleResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResultCode()).isEqualTo("0000");
        assertThat(response.getResultMessage()).isEmpty(); // 데이터 부족 시 빈 문자열
    }

    @Test
    @DisplayName("우측 정렬 패딩 제거 테스트")
    void testRightAlignPaddingRemoval() {
        // Given: 숫자 필드는 우측 정렬이므로 좌측에 제로 패딩
        StringBuilder sb = new StringBuilder();
        sb.append("0000");
        sb.append(String.format("%-100s", "Success"));
        sb.append(String.format("%-20s", "TXN001"));
        sb.append("000000000999999"); // 15바이트, 좌측 제로 패딩
        sb.append(String.format("%-11s", ""));

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        // When
        BankTransactionResponse response = deserializer.deserialize(bytes, BankTransactionResponse.class);

        // Then
        assertThat(response.getBalanceAfter()).isEqualTo(999999L);
    }
}