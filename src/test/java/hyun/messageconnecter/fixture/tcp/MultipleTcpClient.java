package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpClient;

import java.util.concurrent.CompletableFuture;

/**
 * 여러 메시지 타입을 처리하는 TCP 클라이언트 (테스트용)
 *
 * 하나의 클라이언트 인터페이스에서 여러 요청/응답 쌍을 정의할 수 있습니다.
 * TcpMessageClient는 제네릭이 제거되어 동적으로 타입을 처리하므로,
 * 메서드별로 다른 요청/응답 타입을 사용할 수 있습니다.
 */
@TcpClient(
        host = "${bank.tcp.host}",
        port = "${bank.tcp.port}",
        connectionTimeout = "5000",
        charset = "EUC-KR"
)
public interface MultipleTcpClient {

    /**
     * 계좌 조회
     */
    CompletableFuture<BankTransactionResponse> getAccount(BankTransactionRequest request);

    /**
     * 입금
     */
    CompletableFuture<BankTransactionResponse> deposit(BankTransactionRequest request);

    /**
     * 출금
     */
    CompletableFuture<BankTransactionResponse> withdraw(BankTransactionRequest request);

    /**
     * 간단한 조회 (다른 응답 타입)
     */
    CompletableFuture<SimpleResponse> simpleQuery(SimpleRequest request);
}
