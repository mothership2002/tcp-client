package hyun.messageconnecter.interceptor.builtin;

import hyun.messageconnecter.interceptor.ClientInterceptor;
import hyun.messageconnecter.interceptor.InterceptorChain;
import hyun.messageconnecter.interceptor.InterceptorContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 메트릭 수집 인터셉터
 * <p>
 * Micrometer를 사용하여 TCP 클라이언트의 메트릭을 수집합니다.
 * <p>
 * 활성화 조건:
 * - Micrometer가 클래스패스에 존재
 * - tcp.client.interceptor.metrics.enabled=true (기본값: true)
 * <p>
 * 수집 메트릭:
 * 1. tcp.client.request.duration - 요청 처리 시간 (Timer)
 *    - Tags: method, status (success/error), error (에러 타입)
 * 2. tcp.client.requests.total - 요청 건수 (Counter)
 *    - Tags: method, status (success/error), error (에러 타입)
 * <p>
 * 사용 예시:
 * <pre>
 * # application.yml
 * tcp:
 *   client:
 *     interceptor:
 *       metrics:
 *         enabled: true  # 기본값이므로 생략 가능
 *
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: metrics, prometheus
 * </pre>
 * <p>
 * Prometheus 쿼리 예시:
 * <pre>
 * # 평균 응답 시간
 * rate(tcp_client_request_duration_seconds_sum[5m]) /
 * rate(tcp_client_request_duration_seconds_count[5m])
 *
 * # P99 레이턴시
 * histogram_quantile(0.99, rate(tcp_client_request_duration_seconds_bucket[5m]))
 *
 * # 성공률
 * sum(rate(tcp_client_requests_total{status="success"}[5m])) /
 * sum(rate(tcp_client_requests_total[5m]))
 * </pre>
 */
@Component
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(
        name = "tcp.client.interceptor.metrics.enabled",
        havingValue = "true",
        matchIfMissing = true  // 기본값 true (Micrometer 있으면 자동 활성화)
)
public class MetricsInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MetricsInterceptor.class);

    private final MeterRegistry meterRegistry;

    public MetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("MetricsInterceptor initialized with MeterRegistry: {}",
                meterRegistry.getClass().getSimpleName());
    }

    @Override
    public CompletableFuture<Object> intercept(InterceptorContext context, InterceptorChain chain) {
        String methodName = context.getMethod().getName();

        // Timer 시작
        Timer.Sample sample = Timer.start(meterRegistry);

        return chain.proceed(context)
                .whenComplete((response, error) -> {
                    if (error != null) {
                        // 에러 발생 시 메트릭
                        recordErrorMetrics(sample, methodName, error);
                    } else {
                        // 성공 시 메트릭
                        recordSuccessMetrics(sample, methodName);
                    }
                });
    }

    /**
     * 성공 메트릭 기록
     */
    private void recordSuccessMetrics(Timer.Sample sample, String methodName) {
        // 처리 시간 (Timer)
        sample.stop(Timer.builder("tcp.client.request.duration")
                .description("TCP client request duration")
                .tag("method", methodName)
                .tag("status", "success")
                .register(meterRegistry));

        // 요청 건수 (Counter)
        Counter.builder("tcp.client.requests.total")
                .description("Total TCP client requests")
                .tag("method", methodName)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();

        if (log.isTraceEnabled()) {
            log.trace("Metrics recorded for successful request: method={}", methodName);
        }
    }

    /**
     * 에러 메트릭 기록
     */
    private void recordErrorMetrics(Timer.Sample sample, String methodName, Throwable error) {
        String errorType = error.getClass().getSimpleName();

        // 처리 시간 (Timer) - 에러도 시간 측정
        sample.stop(Timer.builder("tcp.client.request.duration")
                .description("TCP client request duration")
                .tag("method", methodName)
                .tag("status", "error")
                .tag("error", errorType)
                .register(meterRegistry));

        // 요청 건수 (Counter) - 에러 카운트
        Counter.builder("tcp.client.requests.total")
                .description("Total TCP client requests")
                .tag("method", methodName)
                .tag("status", "error")
                .tag("error", errorType)
                .register(meterRegistry)
                .increment();

        if (log.isDebugEnabled()) {
            log.debug("Metrics recorded for failed request: method={}, error={}",
                    methodName, errorType);
        }
    }
}
