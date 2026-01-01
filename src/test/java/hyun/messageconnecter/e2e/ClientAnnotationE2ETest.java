package hyun.messageconnecter.e2e;

import hyun.messageconnecter.annotation.EnableTcpClient;
import hyun.messageconnecter.fixture.tcp.BankTcpClient;
import hyun.messageconnecter.fixture.tcp.BankTransactionRequest;
import hyun.messageconnecter.fixture.tcp.BankTransactionResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TCP 클라이언트 어노테이션 기반 E2E 테스트
 * <p>
 * 목적: @EnableTcpClient 어노테이션으로
 * @TcpClient 인터페이스가 자동으로 Bean 등록되고 정상 동작하는지 검증
 * <p>
 * 테스트 대상:
 * - BankTcpClient (@TcpClient) - Bean 자동 등록 및 동작
 */
@SpringBootTest
@TestPropertySource(properties = {
        "bank.tcp.host=localhost",
        "bank.tcp.port=19000"
})
class ClientAnnotationE2ETest {

    /**
     * 테스트용 Spring Boot Application 클래스
     *
     * @EnableTcpClient로 커스텀 어노테이션 기반 Bean 스캐닝 활성화
     */
    @SpringBootApplication
    @EnableTcpClient(basePackages = "hyun.messageconnecter.fixture.tcp")
    @Import({
            hyun.messageconnecter.config.TcpClientAutoConfiguration.class
    })
    public static class TestApplication {
    }

    @Autowired
    private BankTcpClient bankTcpClient;  // @TcpClient 어노테이션으로 Bean 등록됨

    private static MockTcpServer mockTcpServer;

    @BeforeAll
    static void startTcpServer() throws Exception {
        // Bank TCP 서버 (200바이트 요청, 150바이트 응답)
        mockTcpServer = new MockTcpServer(19000, 200, requestBytes -> {
            // 간단한 응답 생성 (150바이트)
            return createBankResponse();
        });
        mockTcpServer.start().get();
    }

    @AfterAll
    static void stopTcpServer() {
        if (mockTcpServer != null) {
            mockTcpServer.stop();
        }
    }

    @Test
    void testTcpClientBeanAutoRegistration() throws Exception {
        // Given: @EnableTcpClient로 BankTcpClient Bean이 자동 등록됨
        assertThat(bankTcpClient).isNotNull();

        // When: TCP 요청 전송
        BankTransactionRequest request = new BankTransactionRequest();
        request.setTransactionType("D");
        request.setAccountNumber("1234567890");
        request.setAmount(50000L);
        request.setTransactionDate(LocalDate.now());

        BankTransactionResponse response = bankTcpClient.send(request).get();

        // Then: Bean이 정상 작동하고 응답 수신
        assertThat(response).isNotNull();
        assertThat(response.getResultCode()).isEqualTo("0000");
        assertThat(response.getResultMessage()).contains("successful");
    }

    /**
     * 간단한 Bank 응답 생성 (150바이트)
     *
     * 구조:
     * - resultCode: 4 bytes
     * - resultMessage: 100 bytes
     * - transactionId: 20 bytes
     * - balanceAfter: 15 bytes (RIGHT aligned, paddingChar = '0')
     * - auto-padding: 11 bytes (to reach 150 bytes total)
     */
    private static byte[] createBankResponse() {
        StringBuilder sb = new StringBuilder();

        // resultCode (4바이트)
        sb.append("0000");

        // resultMessage (100바이트)
        String message = "Transaction successful";
        sb.append(message);
        sb.append(" ".repeat(100 - message.length()));

        // transactionId (20바이트)
        String txnId = "TXN123456789";
        sb.append(txnId);
        sb.append(" ".repeat(20 - txnId.length()));

        // balanceAfter (15바이트) - RIGHT aligned with '0' padding
        String balance = String.format("%015d", 1000000L);
        sb.append(balance);

        // auto-padding (11바이트) to reach 150 total
        sb.append(" ".repeat(11));

        return sb.toString().getBytes();
    }
}
