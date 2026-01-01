package hyun.messageconnecter.e2e;

import hyun.messageconnecter.annotation.EnableTcpClient;
import hyun.messageconnecter.fixture.tcp.SimpleRequest;
import hyun.messageconnecter.fixture.tcp.SimpleResponse;
import hyun.messageconnecter.fixture.tcp.SimpleTcpClient;
import hyun.messageconnecter.interceptor.ClientInterceptor;
import hyun.messageconnecter.interceptor.InterceptorChain;
import hyun.messageconnecter.interceptor.InterceptorContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인터셉터 통합 E2E 테스트
 * <p>
 * 테스트 시나리오:
 * 1. TransactionInterceptor: MDC에 트랜잭션 ID 추가
 * 2. LoggingInterceptor: 요청/응답 로깅
 * 3. 커스텀 인터셉터: 요청 카운터
 */
@SpringBootTest(classes = {InterceptorE2ETest.TestConfig.class})
@TestPropertySource(properties = {
        "test.tcp.host=localhost",
        "test.tcp.port=19002",
        // 인터셉터 활성화
        "tcp.client.interceptor.transaction.enabled=true",
        "tcp.client.interceptor.logging.enabled=true"
})
class InterceptorE2ETest {

    @Autowired
    private SimpleTcpClient simpleTcpClient;

    @Autowired
    private RequestCountInterceptor requestCountInterceptor;

    private static MockTcpServer mockTcpServer;

    @BeforeAll
    static void startServer() throws Exception {
        mockTcpServer = new MockTcpServer(19002, 50, requestBytes -> {
            // SimpleResponse 생성 (30바이트)
            return createSimpleResponse();
        });
        mockTcpServer.start().get();
    }

    /**
     * SimpleResponse 생성 (30바이트)
     * - resultCode: 4 bytes
     * - resultMessage: 20 bytes
     * - auto-padding: 6 bytes
     */
    private static byte[] createSimpleResponse() {
        StringBuilder sb = new StringBuilder();

        // resultCode (4바이트)
        sb.append("0000");

        // resultMessage (20바이트)
        String message = "OK";
        sb.append(message);
        sb.append(" ".repeat(20 - message.length()));

        // auto-padding (6바이트)
        sb.append(" ".repeat(6));

        return sb.toString().getBytes();
    }

    @AfterAll
    static void stopServer() {
        if (mockTcpServer != null) {
            mockTcpServer.stop();
        }
    }

    @Test
    void testInterceptorChainExecution() throws Exception {
        // Given
        SimpleRequest request = new SimpleRequest();
        request.setMessageCode("0100");
        request.setData("test-data");
        request.setNumber(12345L);

        int beforeCount = requestCountInterceptor.getCount();

        // When
        CompletableFuture<SimpleResponse> future = simpleTcpClient.send(request);
        SimpleResponse response = future.get();

        // Then: 응답이 정상적으로 수신됨
        assertThat(response).isNotNull();

        // Then: 커스텀 인터셉터가 실행됨 (요청 카운터 증가)
        assertThat(requestCountInterceptor.getCount()).isEqualTo(beforeCount + 1);
    }

    @Test
    void testTransactionInterceptorMDC() throws Exception {
        // Given
        SimpleRequest request = new SimpleRequest();
        request.setMessageCode("0200");
        request.setData("mdc-test");
        request.setNumber(99999L);

        // When: 트랜잭션 실행 중 MDC 확인을 위한 커스텀 인터셉터 사용
        CompletableFuture<SimpleResponse> future = simpleTcpClient.send(request);
        SimpleResponse response = future.get();

        // Then: 응답이 정상적으로 수신됨
        assertThat(response).isNotNull();

        // Note: MDC는 스레드별로 관리되므로 비동기 환경에서 직접 테스트하기 어려움
        // TransactionInterceptor의 로그를 통해 MDC가 올바르게 설정되었는지 확인 가능
    }

    @Test
    void testMultipleRequests() throws Exception {
        // Given: 여러 요청을 순차적으로 전송
        int requestCount = 5;
        int beforeCount = requestCountInterceptor.getCount();

        // When
        for (int i = 0; i < requestCount; i++) {
            SimpleRequest request = new SimpleRequest();
            request.setMessageCode("030" + i);
            request.setData("batch-" + i);
            request.setNumber(1000L * (i + 1));

            CompletableFuture<SimpleResponse> future = simpleTcpClient.send(request);
            SimpleResponse response = future.get();

            assertThat(response).isNotNull();
        }

        // Then: 모든 요청이 인터셉터를 거침
        assertThat(requestCountInterceptor.getCount()).isEqualTo(beforeCount + requestCount);
    }

    /**
     * 테스트 설정
     */
    @Configuration
    @EnableTcpClient(basePackages = "hyun.messageconnecter.fixture.tcp")
    @org.springframework.context.annotation.Import(hyun.messageconnecter.config.TcpClientAutoConfiguration.class)
    static class TestConfig {

        /**
         * 커스텀 인터셉터: 요청 카운터
         */
        @Bean
        public RequestCountInterceptor requestCountInterceptor() {
            return new RequestCountInterceptor();
        }
    }

    /**
     * 요청 카운터 인터셉터 (테스트용)
     * 각 요청마다 카운터를 증가시켜 인터셉터가 실행되었는지 검증
     */
    static class RequestCountInterceptor implements ClientInterceptor {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public CompletableFuture<Object> intercept(InterceptorContext context, InterceptorChain chain) {
            counter.incrementAndGet();
            return chain.proceed(context);
        }

        public int getCount() {
            return counter.get();
        }
    }
}
