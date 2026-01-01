package hyun.messageconnecter.annotation;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.BankTcpClient;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 커스텀 어노테이션 기반 Bean 등록 단위 테스트
 * <p>
 * 목적: Spring 컨테이너가 @EnableTcpClient 어노테이션으로
 * FactoryBean과 동적 프록시를 제대로 생성하는지 검증
 * <p>
 * 검증 항목:
 * 1. @TcpClient 인터페이스가 Bean으로 등록되는가?
 * 2. 등록된 Bean이 JDK 동적 프록시인가?
 * 3. Singleton으로 동작하는가?
 * 4. 필요한 의존성들이 주입되는가?
 */
@SpringBootTest
@TestPropertySource(properties = {
        "bank.tcp.host=localhost",
        "bank.tcp.port=19000"
})
class ClientAnnotationBeanRegistrationTest {

    @SpringBootApplication
    @EnableTcpClient(basePackages = "hyun.messageconnecter.fixture.tcp")
    @Import({
            hyun.messageconnecter.config.TcpClientAutoConfiguration.class
    })
    static class TestApplication {
        // TcpClientAutoConfiguration이 자동으로 다음 Bean들을 생성:
        // - EventLoopGroup
        // - TcpMessageSerializer
        // - TcpMessageDeserializer
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private BankTcpClient bankTcpClient;

    @Test
    void testTcpClientBeanRegistration() {
        // Given: Spring 컨텍스트가 로드됨
        assertThat(applicationContext).isNotNull();

        // When: BankTcpClient Bean 조회
        boolean beanExists = applicationContext.containsBean("bankTcpClient");
        Object bean = applicationContext.getBean("bankTcpClient");

        // Then: Bean이 등록되어 있음
        assertThat(beanExists).isTrue();
        assertThat(bean).isNotNull();

        // Then: Autowired도 정상 작동
        assertThat(bankTcpClient).isNotNull();
        assertThat(bankTcpClient).isSameAs(bean);
    }

    @Test
    void testTcpClientBeanIsProxy() {
        // Given: BankTcpClient Bean이 등록됨
        assertThat(bankTcpClient).isNotNull();

        // When: Bean의 실제 클래스 확인
        Class<?> beanClass = bankTcpClient.getClass();

        // Then: JDK 동적 프록시로 생성됨
        assertThat(Proxy.isProxyClass(beanClass)).isTrue();

        // Then: 프록시가 BankTcpClient 인터페이스를 구현함
        assertThat(beanClass.getInterfaces()).contains(BankTcpClient.class);

        // Then: InvocationHandler가 설정되어 있음
        assertThat(Proxy.getInvocationHandler(bankTcpClient)).isNotNull();
    }

    @Test
    void testTcpClientBeanType() {
        // Given: BankTcpClient Bean이 등록됨
        assertThat(bankTcpClient).isNotNull();

        // When/Then: 타입 검증
        assertThat(bankTcpClient).isInstanceOf(BankTcpClient.class);

        // When/Then: ApplicationContext에서 타입으로 조회 가능
        BankTcpClient beanByType = applicationContext.getBean(BankTcpClient.class);
        assertThat(beanByType).isSameAs(bankTcpClient);
    }

    @Test
    void testTcpClientIsSingleton() {
        // Given: BankTcpClient Bean이 등록됨
        assertThat(bankTcpClient).isNotNull();

        // When: ApplicationContext에서 다시 조회
        BankTcpClient tcpClient2 = applicationContext.getBean(BankTcpClient.class);

        // Then: Singleton으로 동작함 (같은 인스턴스 반환)
        assertThat(tcpClient2).isSameAs(bankTcpClient);
    }

    @Test
    void testBeanNamingConvention() {
        // Given: Spring 컨텍스트가 로드됨
        assertThat(applicationContext).isNotNull();

        // When/Then: Bean 이름이 인터페이스 이름의 첫 글자를 소문자로 한 것
        // BankTcpClient → bankTcpClient
        assertThat(applicationContext.containsBean("bankTcpClient")).isTrue();
    }

    @Test
    void testRequiredDependenciesAreInjected() {
        // Given: FactoryBean이 의존하는 Bean들이 필요함
        assertThat(applicationContext).isNotNull();

        // When/Then: TcpClient가 필요로 하는 의존성들이 Bean으로 등록됨
        assertThat(applicationContext.getBean(TcpMessageSerializer.class)).isNotNull();
        assertThat(applicationContext.getBean(TcpMessageDeserializer.class)).isNotNull();
        assertThat(applicationContext.getBean(EventLoopGroup.class)).isNotNull();

        // When/Then: 클라이언트 Bean이 정상적으로 생성됨 (의존성 주입 성공)
        assertThat(bankTcpClient).isNotNull();
    }
}
