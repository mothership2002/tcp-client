package hyun.messageconnecter.interceptor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InterceptorChain 단위 테스트
 * 인터셉터 체인이 올바른 순서로 실행되는지 검증
 */
class InterceptorChainTest {

    @Test
    void testInterceptorChainExecutionOrder() throws Exception {
        // Given: 실행 순서를 기록할 리스트
        List<String> executionOrder = new ArrayList<>();

        // 3개의 인터셉터 생성
        ClientInterceptor interceptor1 = (context, chain) -> {
            executionOrder.add("before-1");
            CompletableFuture<Object> result = chain.proceed(context);
            return result.thenApply(response -> {
                executionOrder.add("after-1");
                return response;
            });
        };

        ClientInterceptor interceptor2 = (context, chain) -> {
            executionOrder.add("before-2");
            CompletableFuture<Object> result = chain.proceed(context);
            return result.thenApply(response -> {
                executionOrder.add("after-2");
                return response;
            });
        };

        ClientInterceptor interceptor3 = (context, chain) -> {
            executionOrder.add("before-3");
            CompletableFuture<Object> result = chain.proceed(context);
            return result.thenApply(response -> {
                executionOrder.add("after-3");
                return response;
            });
        };

        List<ClientInterceptor> interceptors = List.of(interceptor1, interceptor2, interceptor3);

        // 타겟 호출 (실제 비즈니스 로직)
        CompletableFuture<Object> targetResult = CompletableFuture.completedFuture("target-response");

        // InterceptorContext 생성
        Method dummyMethod = this.getClass().getDeclaredMethod("testInterceptorChainExecutionOrder");
        InterceptorContext context = new InterceptorContext(
                "request",
                dummyMethod,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        // InterceptorChain 생성
        InterceptorChain chain = new InterceptorChain(interceptors, () -> {
            executionOrder.add("target");
            return targetResult;
        });

        // When: 체인 실행
        CompletableFuture<Object> result = chain.proceed(context);
        Object response = result.join();

        // Then: 응답 확인
        assertThat(response).isEqualTo("target-response");

        // Then: 실행 순서 확인
        // before 순서: 1 -> 2 -> 3 -> target
        // after 순서: 3 -> 2 -> 1 (역순)
        assertThat(executionOrder).containsExactly(
                "before-1",
                "before-2",
                "before-3",
                "target",
                "after-3",
                "after-2",
                "after-1"
        );
    }

    @Test
    void testEmptyInterceptorChain() throws Exception {
        // Given: 인터셉터가 없는 체인
        List<ClientInterceptor> interceptors = List.of();

        Method dummyMethod = this.getClass().getDeclaredMethod("testEmptyInterceptorChain");
        InterceptorContext context = new InterceptorContext(
                "request",
                dummyMethod,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        InterceptorChain chain = new InterceptorChain(interceptors, () ->
                CompletableFuture.completedFuture("direct-response"));

        // When: 체인 실행
        CompletableFuture<Object> result = chain.proceed(context);
        Object response = result.join();

        // Then: 타겟이 바로 호출됨
        assertThat(response).isEqualTo("direct-response");
    }

    @Test
    void testInterceptorCanModifyResponse() throws Exception {
        // Given: 응답을 수정하는 인터셉터
        ClientInterceptor modifyingInterceptor = (context, chain) -> {
            return chain.proceed(context).thenApply(response -> {
                // 응답을 대문자로 변환
                if (response instanceof String) {
                    return ((String) response).toUpperCase();
                }
                return response;
            });
        };

        List<ClientInterceptor> interceptors = List.of(modifyingInterceptor);

        Method dummyMethod = this.getClass().getDeclaredMethod("testInterceptorCanModifyResponse");
        InterceptorContext context = new InterceptorContext(
                "request",
                dummyMethod,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        InterceptorChain chain = new InterceptorChain(interceptors, () ->
                CompletableFuture.completedFuture("lowercase"));

        // When: 체인 실행
        CompletableFuture<Object> result = chain.proceed(context);
        Object response = result.join();

        // Then: 응답이 수정됨
        assertThat(response).isEqualTo("LOWERCASE");
    }

    @Test
    void testInterceptorCanAccessContext() throws Exception {
        // Given: 컨텍스트에 접근하는 인터셉터
        AtomicInteger requestCount = new AtomicInteger(0);

        ClientInterceptor contextAccessingInterceptor = (context, chain) -> {
            // 컨텍스트에서 정보 읽기
            assertThat(context.getRequest()).isEqualTo("test-request");
            assertThat(context.getClientType()).isEqualTo(InterceptorContext.ClientType.TCP);

            // 컨텍스트에 속성 추가
            context.setAttribute("requestId", "req-123");
            requestCount.incrementAndGet();

            return chain.proceed(context);
        };

        List<ClientInterceptor> interceptors = List.of(contextAccessingInterceptor);

        Method dummyMethod = this.getClass().getDeclaredMethod("testInterceptorCanAccessContext");
        InterceptorContext context = new InterceptorContext(
                "test-request",
                dummyMethod,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        InterceptorChain chain = new InterceptorChain(interceptors, () ->
                CompletableFuture.completedFuture("response"));

        // When: 체인 실행
        CompletableFuture<Object> result = chain.proceed(context);
        result.join();

        // Then: 인터셉터가 호출됨
        assertThat(requestCount.get()).isEqualTo(1);

        // Then: 컨텍스트에 속성이 추가됨
        assertThat(context.getAttribute("requestId")).isEqualTo("req-123");
    }

    @Test
    void testInterceptorExceptionPropagation() throws Exception {
        // Given: 예외를 던지는 인터셉터
        RuntimeException expectedException = new RuntimeException("Test exception");

        ClientInterceptor throwingInterceptor = (context, chain) -> {
            return CompletableFuture.failedFuture(expectedException);
        };

        List<ClientInterceptor> interceptors = List.of(throwingInterceptor);

        Method dummyMethod = this.getClass().getDeclaredMethod("testInterceptorExceptionPropagation");
        InterceptorContext context = new InterceptorContext(
                "request",
                dummyMethod,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        InterceptorChain chain = new InterceptorChain(interceptors, () ->
                CompletableFuture.completedFuture("should-not-reach"));

        // When: 체인 실행
        CompletableFuture<Object> result = chain.proceed(context);

        // Then: 예외가 전파됨
        assertThat(result)
                .failsWithin(java.time.Duration.ofSeconds(1))
                .withThrowableThat()
                .havingCause()
                .isEqualTo(expectedException);
    }
}
