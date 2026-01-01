package hyun.messageconnecter.interceptor.builtin;

import hyun.messageconnecter.interceptor.ClientInterceptor;
import hyun.messageconnecter.interceptor.InterceptorChain;
import hyun.messageconnecter.interceptor.InterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 요청/응답 로깅 인터셉터
 * <p>
 * 활성화 조건:
 * - tcp.client.interceptor.logging.enabled=true
 * <p>
 * 기능:
 * - 요청 정보 로깅 (메서드명, 요청 객체)
 * - 응답 정보 로깅 (응답 객체, 처리 시간)
 * - 에러 로깅 (예외 정보)
 * <p>
 * 사용 예시:
 * <pre>
 * # application.yml
 * tcp:
 *   client:
 *     interceptor:
 *       logging:
 *         enabled: true
 * </pre>
 */
@Component
@ConditionalOnProperty(
        name = "tcp.client.interceptor.logging.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class LoggingInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public CompletableFuture<Object> intercept(InterceptorContext context, InterceptorChain chain) {
        String methodName = context.getMethod().getName();
        Object request = context.getRequest();

        log.info("TCP Request: method={}, requestType={}",
                methodName,
                request.getClass().getSimpleName());

        if (log.isDebugEnabled()) {
            log.debug("TCP Request details: method={}, request={}",
                    methodName,
                    request);
        }

        long startTime = System.currentTimeMillis();

        return chain.proceed(context)
                .whenComplete((response, error) -> {
                    long duration = System.currentTimeMillis() - startTime;

                    if (error != null) {
                        // 에러 로깅
                        log.error("TCP Request failed: method={}, duration={}ms, error={}",
                                methodName,
                                duration,
                                error.getMessage(),
                                error);
                    } else {
                        // 성공 로깅
                        log.info("TCP Response: method={}, duration={}ms, responseType={}",
                                methodName,
                                duration,
                                response != null ? response.getClass().getSimpleName() : "null");

                        if (log.isDebugEnabled()) {
                            log.debug("TCP Response details: method={}, duration={}ms, response={}",
                                    methodName,
                                    duration,
                                    response);
                        }
                    }
                });
    }
}
