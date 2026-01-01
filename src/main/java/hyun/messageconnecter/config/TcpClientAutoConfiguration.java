package hyun.messageconnecter.config;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP 클라이언트 Auto-Configuration
 * <p>
 * 필요한 기본 Bean들(Serializer, Deserializer, EventLoopGroup, ExecutorService)을 자동으로 생성합니다.
 * <p>
 * ⚠️ 중요: @TcpClient 스캔 및 등록은 @EnableTcpClient 어노테이션을 통해 활성화됩니다.
 * 사용자는 @SpringBootApplication 클래스에 @EnableTcpClient를 추가해야 합니다.
 * <p>
 * 예시:
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
 */
@Configuration
public class TcpClientAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TcpClientAutoConfiguration.class);

    public TcpClientAutoConfiguration() {
        log.info("TcpClientAutoConfiguration initialized - Base beans only");
        log.info("To enable @TcpClient scanning, add @EnableTcpClient to your @SpringBootApplication class");
    }

    /**
     * Netty EventLoopGroup Bean 생성
     * <p>
     * Netty 4.2+에서는 MultiThreadIoEventLoopGroup과 NioIoHandler를 사용하여 생성합니다.
     * 스레드 수를 지정하지 않으면 기본값(CPU 코어 수 * 2)이 사용됩니다.
     * <p>
     * 사용자가 직접 정의하지 않은 경우에만 생성됩니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public EventLoopGroup eventLoopGroup() {
        log.info("Creating default EventLoopGroup bean using MultiThreadIoEventLoopGroup with NioIoHandler");
        return new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    }

    /**
     * CompletableFuture 콜백 실행용 ExecutorService Bean 생성
     * <p>
     * CachedThreadPool을 사용하여 필요에 따라 스레드를 생성하고,
     * 60초 동안 유휴 상태인 스레드는 자동으로 종료됩니다.
     * <p>
     * 이를 통해 CompletableFuture 콜백이 Netty EventLoop 스레드를 블로킹하지 않도록 합니다.
     */
    @Bean(destroyMethod = "shutdown")
    @Primary
    @ConditionalOnMissingBean(name = "tcpClientExecutor")
    public ExecutorService tcpClientExecutor() {
        log.info("Creating ExecutorService bean for CompletableFuture callbacks");
        return Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("tcp-client-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * TCP 메시지 직렬화 엔진 Bean 생성
     */
    @Bean
    @ConditionalOnMissingBean
    public TcpMessageSerializer tcpMessageSerializer() {
        log.info("Creating TcpMessageSerializer bean");
        return new TcpMessageSerializer();
    }

    /**
     * TCP 메시지 역직렬화 엔진 Bean 생성
     */
    @Bean
    @ConditionalOnMissingBean
    public TcpMessageDeserializer tcpMessageDeserializer() {
        log.info("Creating TcpMessageDeserializer bean");
        return new TcpMessageDeserializer();
    }
}
