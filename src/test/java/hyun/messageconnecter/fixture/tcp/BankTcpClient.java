package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpClient;

import java.util.concurrent.CompletableFuture;

/**
 * 은행 거래 TCP 클라이언트 (테스트용)
 */
@TcpClient(
        host = "${bank.tcp.host}",
        port = "${bank.tcp.port}",
        connectionTimeout = "5000",
        charset = "EUC-KR"
)
public interface BankTcpClient {
    CompletableFuture<BankTransactionResponse> send(BankTransactionRequest request);
}
