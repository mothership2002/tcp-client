package hyun.messageconnecter.annotation;

import hyun.messageconnecter.registrar.TcpClientBeanDefinitionRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TCP 클라이언트 기능을 활성화합니다.
 * <p>
 * 이 어노테이션을 @Configuration 클래스 또는 @SpringBootApplication 클래스에 추가하면
 *
 * @TcpClient 어노테이션이 붙은 인터페이스를 자동으로 스캔하여 Bean으로 등록합니다.
 * <p>
 * 사용 예시:
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableTcpClient(basePackages = "com.mycompany.client")
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }
 * </pre>
 * <p>
 * basePackages를 지정하지 않으면 @EnableTcpClient가 선언된 클래스의 패키지를 기준으로 스캔합니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(TcpClientBeanDefinitionRegistrar.class)
public @interface EnableTcpClient {

    /**
     * @TcpClient 인터페이스를 스캔할 base package들
     * <p>
     * 지정하지 않으면 @EnableTcpClient가 선언된 클래스의 패키지와 하위 패키지를 스캔합니다.
     * <p>
     * 예: {"com.mycompany.client", "com.mycompany.integration"}
     */
    String[] basePackages() default {};

    /**
     * basePackages의 타입 안전 버전
     * <p>
     * 지정된 클래스들의 패키지를 base package로 사용합니다.
     */
    Class<?>[] basePackageClasses() default {};
}
