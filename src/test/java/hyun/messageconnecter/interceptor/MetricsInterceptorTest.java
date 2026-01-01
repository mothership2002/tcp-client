package hyun.messageconnecter.interceptor;

import hyun.messageconnecter.interceptor.builtin.MetricsInterceptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MetricsInterceptor 단위 테스트
 * Micrometer 메트릭이 올바르게 수집되는지 검증
 */
class MetricsInterceptorTest {

    private MeterRegistry meterRegistry;
    private MetricsInterceptor metricsInterceptor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsInterceptor = new MetricsInterceptor(meterRegistry);
    }

    @Test
    void testSuccessMetricsRecorded() throws Exception {
        // Given
        Method method = this.getClass().getDeclaredMethod("testSuccessMetricsRecorded");
        InterceptorContext context = new InterceptorContext(
                "request",
                method,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        InterceptorChain chain = new InterceptorChain(
                List.of(metricsInterceptor),
                () -> CompletableFuture.completedFuture("response")
        );

        // When
        CompletableFuture<Object> result = chain.proceed(context);
        result.get();  // 대기

        // Then: Timer 메트릭 확인
        Timer timer = meterRegistry.find("tcp.client.request.duration")
                .tag("method", "testSuccessMetricsRecorded")
                .tag("status", "success")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS)).isGreaterThan(0);

        // Then: Counter 메트릭 확인
        Counter counter = meterRegistry.find("tcp.client.requests.total")
                .tag("method", "testSuccessMetricsRecorded")
                .tag("status", "success")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testErrorMetricsRecorded() throws Exception {
        // Given
        RuntimeException expectedException = new RuntimeException("Test exception");

        Method method = this.getClass().getDeclaredMethod("testErrorMetricsRecorded");
        InterceptorContext context = new InterceptorContext(
                "request",
                method,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        InterceptorChain chain = new InterceptorChain(
                List.of(metricsInterceptor),
                () -> CompletableFuture.failedFuture(expectedException)
        );

        // When
        CompletableFuture<Object> result = chain.proceed(context);

        try {
            result.get();  // 예외 발생 예상
        } catch (Exception ignored) {
            // 예외 무시
        }

        // Then: Timer 메트릭 확인 (에러도 시간 측정)
        Timer timer = meterRegistry.find("tcp.client.request.duration")
                .tag("method", "testErrorMetricsRecorded")
                .tag("status", "error")
                .tag("error", "RuntimeException")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);

        // Then: Counter 메트릭 확인 (에러 카운트)
        Counter counter = meterRegistry.find("tcp.client.requests.total")
                .tag("method", "testErrorMetricsRecorded")
                .tag("status", "error")
                .tag("error", "RuntimeException")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testMultipleRequestsMetrics() throws Exception {
        // Given
        Method method = this.getClass().getDeclaredMethod("testMultipleRequestsMetrics");
        InterceptorContext context = new InterceptorContext(
                "request",
                method,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        // When: 성공 3번, 실패 2번
        for (int i = 0; i < 3; i++) {
            InterceptorChain successChain = new InterceptorChain(
                    List.of(metricsInterceptor),
                    () -> CompletableFuture.completedFuture("response")
            );
            successChain.proceed(context).get();
        }

        for (int i = 0; i < 2; i++) {
            InterceptorChain failChain = new InterceptorChain(
                    List.of(metricsInterceptor),
                    () -> CompletableFuture.failedFuture(new RuntimeException("Error"))
            );
            try {
                failChain.proceed(context).get();
            } catch (Exception ignored) {
            }
        }

        // Then: 성공 카운트
        Counter successCounter = meterRegistry.find("tcp.client.requests.total")
                .tag("method", "testMultipleRequestsMetrics")
                .tag("status", "success")
                .counter();

        assertThat(successCounter).isNotNull();
        assertThat(successCounter.count()).isEqualTo(3.0);

        // Then: 실패 카운트
        Counter errorCounter = meterRegistry.find("tcp.client.requests.total")
                .tag("method", "testMultipleRequestsMetrics")
                .tag("status", "error")
                .counter();

        assertThat(errorCounter).isNotNull();
        assertThat(errorCounter.count()).isEqualTo(2.0);

        // Then: Timer도 5번 기록되었는지 확인
        Timer successTimer = meterRegistry.find("tcp.client.request.duration")
                .tag("method", "testMultipleRequestsMetrics")
                .tag("status", "success")
                .timer();

        Timer errorTimer = meterRegistry.find("tcp.client.request.duration")
                .tag("method", "testMultipleRequestsMetrics")
                .tag("status", "error")
                .timer();

        assertThat(successTimer).isNotNull();
        assertThat(successTimer.count()).isEqualTo(3);

        assertThat(errorTimer).isNotNull();
        assertThat(errorTimer.count()).isEqualTo(2);
    }

    @Test
    void testDifferentErrorTypes() throws Exception {
        // Given
        Method method = this.getClass().getDeclaredMethod("testDifferentErrorTypes");
        InterceptorContext context = new InterceptorContext(
                "request",
                method,
                String.class,
                InterceptorContext.ClientType.TCP
        );

        // When: 다양한 에러 타입
        InterceptorChain runtimeChain = new InterceptorChain(
                List.of(metricsInterceptor),
                () -> CompletableFuture.failedFuture(new RuntimeException("Runtime error"))
        );

        InterceptorChain illegalArgChain = new InterceptorChain(
                List.of(metricsInterceptor),
                () -> CompletableFuture.failedFuture(new IllegalArgumentException("Illegal arg"))
        );

        try {
            runtimeChain.proceed(context).get();
        } catch (Exception ignored) {
        }

        try {
            illegalArgChain.proceed(context).get();
        } catch (Exception ignored) {
        }

        // Then: RuntimeException 메트릭
        Counter runtimeCounter = meterRegistry.find("tcp.client.requests.total")
                .tag("method", "testDifferentErrorTypes")
                .tag("status", "error")
                .tag("error", "RuntimeException")
                .counter();

        assertThat(runtimeCounter).isNotNull();
        assertThat(runtimeCounter.count()).isEqualTo(1.0);

        // Then: IllegalArgumentException 메트릭
        Counter illegalArgCounter = meterRegistry.find("tcp.client.requests.total")
                .tag("method", "testDifferentErrorTypes")
                .tag("status", "error")
                .tag("error", "IllegalArgumentException")
                .counter();

        assertThat(illegalArgCounter).isNotNull();
        assertThat(illegalArgCounter.count()).isEqualTo(1.0);
    }
}
