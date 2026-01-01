package hyun.messageconnecter.factory;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.interceptor.ClientInterceptor;
import hyun.messageconnecter.proxy.TcpClientInvocationHandler;
import io.netty.channel.EventLoopGroup;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @TcpClient 인터페이스의 프록시를 생성하는 FactoryBean
 */
public class TcpClientFactoryBean implements FactoryBean<Object>, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(TcpClientFactoryBean.class);

    private Class<?> interfaceClass;
    private ApplicationContext applicationContext;

    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object getObject() {
        log.debug("Creating TCP client proxy for interface: {}", interfaceClass.getName());

        // ApplicationContext에서 필요한 Bean 가져오기
        Environment environment = applicationContext.getBean(Environment.class);
        EventLoopGroup eventLoopGroup = applicationContext.getBean(EventLoopGroup.class);
        ExecutorService executor = applicationContext.getBean("tcpClientExecutor", ExecutorService.class);
        TcpMessageSerializer serializer = applicationContext.getBean(TcpMessageSerializer.class);
        TcpMessageDeserializer deserializer = applicationContext.getBean(TcpMessageDeserializer.class);

        // 모든 ClientInterceptor Bean 수집
        Map<String, ClientInterceptor> interceptorBeans = applicationContext.getBeansOfType(ClientInterceptor.class);
        List<ClientInterceptor> interceptors = new ArrayList<>(interceptorBeans.values());
        log.debug("Found {} interceptor(s) for TCP client", interceptors.size());

        // InvocationHandler 생성
        TcpClientInvocationHandler handler = new TcpClientInvocationHandler(
                interfaceClass,
                environment,
                eventLoopGroup,
                executor,
                serializer,
                deserializer,
                interceptors
        );

        // JDK 동적 프록시 생성
        return Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                handler
        );
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
