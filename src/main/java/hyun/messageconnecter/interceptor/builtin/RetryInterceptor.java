package hyun.messageconnecter.interceptor.builtin;

import hyun.messageconnecter.interceptor.ClientInterceptor;
import hyun.messageconnecter.interceptor.InterceptorChain;
import hyun.messageconnecter.interceptor.InterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 재시도 인터셉터
 * <p>
 * 활성화 조건:
 * - tcp.client.interceptor.retry.enabled=true
 * <p>
 * 설정:
 * - tcp.client.interceptor.retry.max-attempts: 최대 재시도 횟수 (기본값: 3)
 * - tcp.client.interceptor.retry.backoff-ms: 초기 백오프 시간 (기본값: 1000ms)
 * <p>
 * 재시도 정책:
 * - Exponential Backoff: 재시도마다 대기 시간이 증가 (backoff * attempt)
 * - 모든 예외에 대해 재시도 (특정 예외만 재시도하려면 커스터마이징 필요)
 * <p>
 * 사용 예시:
 * <pre>
 * # application.yml
 * tcp:
 *   client:
 *     interceptor:
 *       retry:
 *         enabled: true
 *         max-attempts: 3
 *         backoff-ms: 1000
 * </pre>
 */
@Component
@ConditionalOnProperty(
        name = "tcp.client.interceptor.retry.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RetryInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RetryInterceptor.class);

    private final int maxAttempts;
    private final long backoffMs;

    public RetryInterceptor(
            @Value("${tcp.client.interceptor.retry.max-attempts:3}") int maxAttempts,
            @Value("${tcp.client.interceptor.retry.backoff-ms:1000}") long backoffMs) {
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;

        log.info("RetryInterceptor initialized: maxAttempts={}, backoffMs={}",
                maxAttempts, backoffMs);
    }

    @Override
    public CompletableFuture<Object> intercept(InterceptorContext context, InterceptorChain chain) {
        return retryWithBackoff(context, chain, 1);
    }

    /**
     * 재시도 로직 (재귀적 구현)
     *
     * @param context 인터셉터 컨텍스트
     * @param chain   인터셉터 체인
     * @param attempt 현재 시도 횟수 (1부터 시작)
     * @return 응답 Future
     */
    private CompletableFuture<Object> retryWithBackoff(
            InterceptorContext context,
            InterceptorChain chain,
            int attempt) {

        return chain.proceed(context)
                .exceptionallyCompose(error -> {
                    if (attempt >= maxAttempts) {
                        // 최대 재시도 횟수 도달
                        log.error("Retry exhausted after {} attempts for method {}: {}",
                                maxAttempts,
                                context.getMethod().getName(),
                                error.getMessage());

                        return CompletableFuture.failedFuture(
                                new RetryExhaustedException(
                                        String.format("Failed after %d attempts: %s",
                                                maxAttempts,
                                                error.getMessage()),
                                        error
                                )
                        );
                    }

                    // 재시도 로깅
                    long sleepTime = backoffMs * attempt;
                    log.warn("Retry attempt {}/{} for method {} after {}ms: {}",
                            attempt,
                            maxAttempts,
                            context.getMethod().getName(),
                            sleepTime,
                            error.getMessage());

                    // Exponential backoff 후 재시도
                    return CompletableFuture
                            .supplyAsync(() -> null,
                                    CompletableFuture.delayedExecutor(
                                            sleepTime,
                                            java.util.concurrent.TimeUnit.MILLISECONDS))
                            .thenCompose(v -> retryWithBackoff(context, chain, attempt + 1));
                });
    }

    /**
     * 재시도 횟수 초과 예외
     */
    public static class RetryExhaustedException extends RuntimeException {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
