package hyun.messageconnecter.proxy;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.annotation.TcpClient;
import hyun.messageconnecter.client.tcp.TcpMessageClient;
import hyun.messageconnecter.interceptor.ClientInterceptor;
import hyun.messageconnecter.interceptor.InterceptorChain;
import hyun.messageconnecter.interceptor.InterceptorContext;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @TcpClient 어노테이션이 붙은 인터페이스의 동적 프록시 핸들러
 * <p>
 * 성능 최적화:
 * - 메서드별 응답 타입 및 동기/비동기 여부를 초기화 시 캐싱 (리플렉션 비용 제거)
 * - TcpMessageClient를 한 번만 생성하여 재사용
 * - 런타임 오버헤드 최소화 (Map 조회 O(1))
 * <p>
 * 동기/비동기 지원:
 * - CompletableFuture<T> 반환 → 비동기 모드
 * - T 반환 → 동기 모드 (내부적으로 .join() 호출)
 */
public class TcpClientInvocationHandler implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(TcpClientInvocationHandler.class);

    private final Class<?> interfaceClass;
    private final TcpMessageClient tcpClient;
    private final List<ClientInterceptor> interceptors;

    /**
     * 메서드별 메타데이터 캐시
     * Key: Method 객체
     * Value: 응답 타입 및 동기/비동기 정보
     * <p>
     * 초기화 시 한 번만 리플렉션으로 추출하고,
     * 런타임에는 O(1)로 조회만 수행
     */
    private final Map<Method, MethodMetadata> methodMetadataCache = new ConcurrentHashMap<>();

    /**
     * 메서드 메타데이터 (응답 타입, 동기/비동기)
     */
    private record MethodMetadata(Class<?> responseType, boolean isAsync) {
    }

    public TcpClientInvocationHandler(
            Class<?> interfaceClass,
            Environment environment,
            EventLoopGroup eventLoopGroup,
            ExecutorService executor,
            TcpMessageSerializer serializer,
            TcpMessageDeserializer deserializer,
            List<ClientInterceptor> interceptors) {
        this.interfaceClass = interfaceClass;
        this.interceptors = interceptors != null ? interceptors : List.of();

        TcpClient annotation = interfaceClass.getAnnotation(TcpClient.class);
        if (annotation == null) {
            throw new IllegalArgumentException("@TcpClient annotation is required on interface: " + interfaceClass.getName());
        }

        // 설정값 읽기 (SpEL 지원)
        String host = resolveProperty(annotation.host(), environment);
        int port = resolveIntProperty(annotation.port(), environment);
        int connectionTimeout = resolveIntProperty(annotation.connectionTimeout(), environment);
        int readTimeout = resolveIntProperty(annotation.readTimeout(), environment);
        String charset = resolveProperty(annotation.charset(), environment);

        log.debug("Initializing TcpMessageClient for {}:{} with charset {}", host, port, charset);

        // TcpMessageClient 한 번만 생성 (재사용)
        this.tcpClient = new TcpMessageClient(
                host,
                port,
                connectionTimeout,
                readTimeout,
                charset,
                eventLoopGroup,
                executor,
                serializer,
                deserializer
        );

        cacheResponseTypes();
    }

    /**
     * 인터페이스의 모든 메서드를 분석하여 메타데이터 캐싱
     * 앱 시작 시 한 번만 실행되므로 성능 영향 없음
     */
    private void cacheResponseTypes() {
        for (Method method : interfaceClass.getDeclaredMethods()) {
            MethodMetadata metadata = extractMethodMetadata(method);
            methodMetadataCache.put(method, metadata);
            log.debug("Cached method {}: responseType={}, async={}",
                    method.getName(),
                    metadata.responseType.getSimpleName(),
                    metadata.isAsync);
        }
        log.info("Cached {} method(s) for interface {}", methodMetadataCache.size(), interfaceClass.getSimpleName());
    }

    /**
     * 메서드에서 메타데이터 추출 (응답 타입, 동기/비동기)
     *
     * 지원 패턴:
     * 1. CompletableFuture<T> → 비동기 모드, 응답 타입 = T
     * 2. T → 동기 모드, 응답 타입 = T
     */
    private MethodMetadata extractMethodMetadata(Method method) {
        Type returnType = method.getGenericReturnType();
        Class<?> rawReturnType = method.getReturnType();

        // 1. CompletableFuture<T>인 경우 (비동기)
        if (rawReturnType == CompletableFuture.class) {
            if (!(returnType instanceof ParameterizedType parameterizedType)) {
                throw new IllegalArgumentException(
                        String.format("Method %s returns raw CompletableFuture without type parameter",
                                method.getName()));
            }

            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length != 1) {
                throw new IllegalArgumentException(
                        String.format("Method %s has invalid CompletableFuture type arguments", method.getName()));
            }

            Class<?> responseType = (Class<?>) typeArguments[0];
            return new MethodMetadata(responseType, true);
        }

        // 2. 일반 타입 T인 경우 (동기)
        // void는 허용하지 않음
        if (rawReturnType == void.class || rawReturnType == Void.class) {
            throw new IllegalArgumentException(
                    String.format("Method %s cannot return void. Use CompletableFuture<Void> for fire-and-forget.",
                            method.getName()));
        }

        return new MethodMetadata(rawReturnType, false);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object 메서드는 기본 처리
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 파라미터 검증 (단일 파라미터만 허용)
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException(
                    String.format("Method %s must have exactly one parameter", method.getName()));
        }

        Object request = args[0];

        MethodMetadata metadata = methodMetadataCache.get(method);
        if (metadata == null) {
            throw new IllegalStateException(
                    String.format("Metadata not cached for method %s", method.getName()));
        }

        log.debug("Invoking method {} with request type {} expecting response type {} (async={})",
                method.getName(),
                request.getClass().getSimpleName(),
                metadata.responseType.getSimpleName(),
                metadata.isAsync);

        // 인터셉터 컨텍스트 생성
        InterceptorContext context = new InterceptorContext(
                request,
                method,
                metadata.responseType,
                InterceptorContext.ClientType.TCP
        );

        // 인터셉터 체인 생성
        InterceptorChain chain = new InterceptorChain(
                interceptors,
                () -> (CompletableFuture<Object>) tcpClient.send(request, metadata.responseType)
        );

        // 인터셉터 체인 실행
        CompletableFuture<?> future = chain.proceed(context);

        // 비동기 모드: Future 그대로 반환
        if (metadata.isAsync) {
            return future;
        }

        // 동기 모드: 블로킹 대기 (.join() 호출)
        try {
            return future.join();
        } catch (CompletionException e) {
            // CompletionException을 unwrap하여 원본 예외를 던짐
            Throwable cause = e.getCause();
            log.error("Synchronous call failed for method {}: {}",
                    method.getName(),
                    cause != null ? cause.getMessage() : e.getMessage(),
                    cause != null ? cause : e);

            // 원본 예외가 RuntimeException이면 그대로 던짐
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            // 원본 예외가 checked exception이면 RuntimeException으로 감싸서 던짐
            if (cause != null) {
                throw new RuntimeException("Synchronous call failed: " + cause.getMessage(), cause);
            }
            // cause가 null이면 CompletionException 그대로 던짐
            throw e;
        }
    }

    /**
     * SpEL 표현식 또는 일반 문자열 해석
     * ${property.name} 형태를 지원합니다.
     */
    private String resolveProperty(String value, Environment environment) {
        if (value.startsWith("${") && value.endsWith("}")) {
            String propertyName = value.substring(2, value.length() - 1);
            String resolved = environment.getProperty(propertyName);
            if (resolved == null) {
                throw new IllegalArgumentException("Property not found: " + propertyName);
            }
            return resolved;
        }
        return value;
    }

    /**
     * 정수형 속성 해석
     * SpEL 표현식 또는 일반 문자열을 정수로 변환합니다.
     */
    private int resolveIntProperty(String value, Environment environment) {
        String resolved = resolveProperty(value, environment);
        try {
            return Integer.parseInt(resolved);
        } catch (NumberFormatException e) {
            // 문자열이 아닌 경우 직접 파싱 시도
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid integer value: " + value, e);
            }
        }
    }
}
