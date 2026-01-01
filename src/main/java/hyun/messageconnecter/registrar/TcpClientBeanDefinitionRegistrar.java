package hyun.messageconnecter.registrar;

import hyun.messageconnecter.annotation.EnableTcpClient;
import hyun.messageconnecter.annotation.TcpClient;
import hyun.messageconnecter.factory.TcpClientFactoryBean;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @EnableTcpClient 어노테이션을 통해 활성화되며,
 * @TcpClient 어노테이션이 붙은 인터페이스를 스캔하여
 * 동적 프록시를 생성하고 Spring Bean으로 등록합니다.
 */
public class TcpClientBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(TcpClientBeanDefinitionRegistrar.class);

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        // @EnableTcpClient 어노테이션 속성 읽기
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableTcpClient.class.getName()));

        if (attributes == null) {
            log.warn("@EnableTcpClient annotation not found. Skipping TCP client registration.");
            return;
        }

        // basePackages 결정
        String[] basePackages = getBasePackages(importingClassMetadata, attributes);

        log.info("Scanning for @TcpClient interfaces in packages: {}", Arrays.toString(basePackages));

        // ClassPath 스캐너 설정 (인터페이스도 스캔하도록 설정)
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                        // 인터페이스도 candidate로 인정
                        AnnotationMetadata metadata = beanDefinition.getMetadata();
                        return metadata.isInterface() || super.isCandidateComponent(beanDefinition);
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(TcpClient.class));

        // ResourceLoader 설정 (테스트 클래스패스 스캔을 위해 필요)
        if (this.resourceLoader != null) {
            scanner.setResourceLoader(this.resourceLoader);
        }

        // 지정된 패키지들에서 @TcpClient 스캔
        Set<BeanDefinition> candidates = new HashSet<>();
        for (String basePackage : basePackages) {
            try {
                Set<BeanDefinition> found = scanner.findCandidateComponents(basePackage);
                candidates.addAll(found);
                log.debug("Found {} @TcpClient interface(s) in package: {}", found.size(), basePackage);
            } catch (Exception e) {
                log.warn("Failed to scan package: {}", basePackage, e);
            }
        }

        log.info("Found total {} @TcpClient interfaces", candidates.size());

        for (BeanDefinition candidate : candidates) {
            try {
                String className = candidate.getBeanClassName();
                Class<?> interfaceClass = Class.forName(className);

                // 인터페이스 검증
                if (!interfaceClass.isInterface()) {
                    log.warn("@TcpClient annotation is only allowed on interfaces: {}", className);
                    continue;
                }

                // Bean 이름 생성 (인터페이스 이름의 첫 글자를 소문자로)
                String beanName = getBeanName(interfaceClass);

                // 이미 등록된 Bean이 있으면 스킵
                if (registry.containsBeanDefinition(beanName)) {
                    log.warn("Bean already registered: {}", beanName);
                    continue;
                }

                // 동적 프록시 생성 및 Bean 등록
                registerProxyBean(registry, interfaceClass, beanName);

                log.info("Registered TCP client bean: {} for interface {}", beanName, className);

            } catch (ClassNotFoundException e) {
                log.error("Failed to load class: {}", candidate.getBeanClassName(), e);
            }
        }
    }

    /**
     * FactoryBean을 등록하여 프록시 생성을 위임
     */
    private void registerProxyBean(BeanDefinitionRegistry registry, Class<?> interfaceClass, String beanName) {
        // TcpClientFactoryBean을 BeanDefinition으로 등록
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TcpClientFactoryBean.class);

        // interfaceClass 속성 설정
        builder.addPropertyValue("interfaceClass", interfaceClass);

        // Lazy 초기화 설정 (필요 시)
        builder.setLazyInit(false);

        // Bean 등록
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    /**
     * Bean 이름 생성 (인터페이스 이름의 첫 글자를 소문자로)
     */
    private String getBeanName(Class<?> interfaceClass) {
        String simpleName = interfaceClass.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * @EnableTcpClient 어노테이션에서 basePackages 추출
     * <p>
     * 우선순위:
     * 1. basePackages 속성
     * 2. basePackageClasses 속성
     * 3. @EnableTcpClient가 선언된 클래스의 패키지
     */
    private String[] getBasePackages(AnnotationMetadata metadata, AnnotationAttributes attributes) {
        List<String> basePackages = new ArrayList<>();

        // 1. basePackages 속성
        String[] packages = attributes.getStringArray("basePackages");
        if (packages.length > 0) {
            Collections.addAll(basePackages, packages);
        }

        // 2. basePackageClasses 속성
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
        for (Class<?> clazz : basePackageClasses) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        // 3. 둘 다 없으면 @EnableTcpClient가 선언된 클래스의 패키지 사용
        if (basePackages.isEmpty()) {
            String defaultPackage = ClassUtils.getPackageName(metadata.getClassName());
            if (StringUtils.hasText(defaultPackage)) {
                basePackages.add(defaultPackage);
                log.info("Using default base package from @EnableTcpClient location: {}", defaultPackage);
            } else {
                // 최악의 경우 전체 스캔 (성능 저하 경고)
                log.warn("No base packages specified. Scanning common package prefixes (performance impact).");
                basePackages.addAll(Arrays.asList("com", "org", "net", "io", "kr", "co"));
            }
        }

        return basePackages.toArray(new String[0]);
    }
}