package hyun.messageconnecter.interceptor;

import java.util.concurrent.CompletableFuture;

/**
 * TCP/HTTP 클라이언트 요청/응답 인터셉터
 * Chain of Responsibility 패턴으로 여러 인터셉터를 체인으로 연결
 */
public interface ClientInterceptor {

    /**
     * 인터셉터 로직 실행
     *
     * @param context 요청/응답 컨텍스트
     * @param chain 다음 인터셉터 또는 실제 호출로 이어지는 체인
     * @return 비동기 응답
     */
    CompletableFuture<Object> intercept(InterceptorContext context, InterceptorChain chain);
}
