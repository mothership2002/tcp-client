package hyun.messageconnecter.interceptor.builtin;

import hyun.messageconnecter.interceptor.ClientInterceptor;
import hyun.messageconnecter.interceptor.InterceptorChain;
import hyun.messageconnecter.interceptor.InterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 트랜잭션 ID 관리 인터셉터
 * <p>
 * 활성화 조건:
 * - tcp.client.interceptor.transaction.enabled=true (기본값: true)
 * <p>
 * 기능:
 * - 각 요청마다 고유한 트랜잭션 ID 생성
 * - MDC에 트랜잭션 ID 추가 (로그에 자동으로 포함됨)
 * - InterceptorContext에 트랜잭션 ID 속성 추가
 * <p>
 * MDC 키:
 * - transactionId: UUID 형태의 고유 ID
 * - clientMethod: 호출된 클라이언트 메서드명
 * <p>
 * 사용 예시:
 * <pre>
 * # application.yml
 * tcp:
 *   client:
 *     interceptor:
 *       transaction:
 *         enabled: true  # 기본값이므로 생략 가능
 * </pre>
 * <p>
 * Logback 설정 예시:
 * <pre>
 * {@code
 * <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [txId=%X{transactionId}] - %msg%n</pattern>
 * }
 * </pre>
 */
@Component
@ConditionalOnProperty(
        name = "tcp.client.interceptor.transaction.enabled",
        havingValue = "true",
        matchIfMissing = true  // 기본값 true (명시하지 않으면 활성화)
)
public class TransactionInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TransactionInterceptor.class);

    private static final String MDC_TRANSACTION_ID = "transactionId";
    private static final String MDC_CLIENT_METHOD = "clientMethod";
    private static final String ATTR_TRANSACTION_ID = "transactionId";

    @Override
    public CompletableFuture<Object> intercept(InterceptorContext context, InterceptorChain chain) {
        // 트랜잭션 ID 생성
        String transactionId = UUID.randomUUID().toString();
        String methodName = context.getMethod().getName();

        // MDC에 추가 (로그에 자동 포함)
        MDC.put(MDC_TRANSACTION_ID, transactionId);
        MDC.put(MDC_CLIENT_METHOD, methodName);

        // InterceptorContext에도 저장 (다른 인터셉터에서 사용 가능)
        context.setAttribute(ATTR_TRANSACTION_ID, transactionId);

        log.debug("Transaction started: id={}, method={}", transactionId, methodName);

        return chain.proceed(context)
                .whenComplete((response, error) -> {
                    // 요청 완료 후 MDC 정리 (메모리 누수 방지)
                    try {
                        if (error != null) {
                            log.debug("Transaction failed: id={}, method={}, error={}",
                                    transactionId,
                                    methodName,
                                    error.getMessage());
                        } else {
                            log.debug("Transaction completed: id={}, method={}",
                                    transactionId,
                                    methodName);
                        }
                    } finally {
                        MDC.remove(MDC_TRANSACTION_ID);
                        MDC.remove(MDC_CLIENT_METHOD);
                    }
                });
    }
}
