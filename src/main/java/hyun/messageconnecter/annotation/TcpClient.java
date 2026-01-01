package hyun.messageconnecter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TCP 클라이언트 인터페이스 마커
 * 이 어노테이션이 붙은 인터페이스는 자동으로 프록시 구현체가 생성됨
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TcpClient {

    /**
     * TCP 서버 호스트
     * SpEL 표현식 지원: "${legacy.host}"
     */
    String host();

    /**
     * TCP 서버 포트
     * SpEL 표현식 지원: "${legacy.port}" 또는 직접 값
     */
    String port() default "8080";

    /**
     * 연결 타임아웃 (밀리초)
     * SpEL 표현식 지원: "${legacy.connection-timeout}" 또는 직접 값
     */
    String connectionTimeout() default "5000";

    /**
     * 읽기 타임아웃 (밀리초)
     * SpEL 표현식 지원: "${legacy.read-timeout}" 또는 직접 값
     */
    String readTimeout() default "10000";

    /**
     * 문자 인코딩 (기본: UTF-8)
     *
     * @TcpMessage 의 charset과 일치해야 함
     */
    String charset() default "UTF-8";
}
