package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpClient;

import java.util.concurrent.CompletableFuture;

/**
 * 동기/비동기 모드 테스트용 TCP 클라이언트
 * <p>
 * 사용 예시:
 * <pre>
 * // 동기 방식 - 즉시 결과 반환 (블로킹)
 * SimpleResponse response = client.sendSync(request);
 *
 * // 비동기 방식 - Future 반환 (논블로킹)
 * CompletableFuture<SimpleResponse> future = client.sendAsync(request);
 * SimpleResponse response = future.join();
 * </pre>
 */
@TcpClient(host = "${tcp.host:localhost}", port = "${tcp.port:8080}")
public interface SyncAsyncTcpClient {

    /**
     * 동기 방식 호출
     * 응답을 받을 때까지 블로킹됨
     */
    SimpleResponse sendSync(SimpleRequest request);

    /**
     * 비동기 방식 호출
     * 즉시 CompletableFuture 반환 (논블로킹)
     */
    CompletableFuture<SimpleResponse> sendAsync(SimpleRequest request);
}
