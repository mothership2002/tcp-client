package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpClient;

import java.util.concurrent.CompletableFuture;

/**
 * 간단한 TCP 클라이언트 (테스트용)
 */
@TcpClient(
        host = "${test.tcp.host}",
        port = "${test.tcp.port}",
        connectionTimeout = "5000",
        charset = "UTF-8"
)
public interface SimpleTcpClient {
    CompletableFuture<SimpleResponse> send(SimpleRequest request);
}
