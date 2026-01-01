package hyun.messageconnecter.interceptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Chain of Responsibility 패턴을 구현한 인터셉터 체인
 */
public class InterceptorChain {

    private final List<ClientInterceptor> interceptors;
    private final Supplier<CompletableFuture<Object>> target;
    private int index = 0;

    public InterceptorChain(List<ClientInterceptor> interceptors, Supplier<CompletableFuture<Object>> target) {
        this.interceptors = interceptors;
        this.target = target;
    }

    /**
     * 다음 인터셉터 또는 실제 타겟 호출 실행
     *
     * @param context 인터셉터 컨텍스트
     * @return 비동기 응답
     */
    public CompletableFuture<Object> proceed(InterceptorContext context) {
        if (index >= interceptors.size()) {
            // 모든 인터셉터를 거쳤으면 실제 타겟 호출
            return target.get();
        }

        // 현재 인터셉터 실행
        ClientInterceptor interceptor = interceptors.get(index++);
        return interceptor.intercept(context, this);
    }
}
