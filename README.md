# TCP Client Spring Boot Starter

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Test Coverage](https://img.shields.io/badge/coverage-80%25-green.svg)]()
[![License](https://img.shields.io/badge/license-MIT-blue.svg)]()
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/spring--boot-4.x-green.svg)]()
[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)]()

**선언적 TCP 클라이언트 라이브러리** - 고정 길이 바이너리 프로토콜을 인터페이스 선언만으로 자동 처리

Spring Boot의 AutoConfiguration과 동적 프록시를 활용하여 레거시 시스템과의 TCP 통신을 간소화합니다.

> **왜 TCP 클라이언트인가?** Spring은 HTTP 통신을 위한 훌륭한 도구(RestClient, WebClient)를 제공하지만,
> 레거시 시스템의 고정 길이 바이너리 프로토콜을 위한 선언적 솔루션은 제공하지 않습니다.
> 이 라이브러리는 바이트 단위 직렬화/역직렬화, 체크섬 계산, STX/ETX 프레이밍 등을 자동화하여
> 레거시 TCP 통신을 Spring 스타일로 처리할 수 있게 합니다.

## v1.1.0 새로운 기능

-  **인터셉터 체인** - 로깅, 재시도, 메트릭 등 횡단 관심사 처리
-  **LoggingInterceptor** - 요청/응답 자동 로깅
-  **RetryInterceptor** - Exponential Backoff 재시도
-  **TransactionInterceptor** - 트랜잭션 ID 자동 생성 및 MDC 관리
-  **MetricsInterceptor** - Micrometer 메트릭 수집 (Prometheus, Grafana 연동)

---

## QuickStart (5분 안에 시작하기)

### 1. 의존성 추가

```gradle
// build.gradle
dependencies {
    implementation 'com.github.mothership2002:tcp-client:v1.1.1'
}
```

### 2. 클라이언트 인터페이스 정의

```java
package com.mycompany.client;

import hyun.messageconnecter.annotation.TcpClient;
import java.util.concurrent.CompletableFuture;

@TcpClient(
    host = "${bank.tcp.host}",
    port = "${bank.tcp.port}",
    connectionTimeout = "5000",
    charset = "EUC-KR"
)
public interface BankClient {
    CompletableFuture<TransactionResponse> send(TransactionRequest request);
}
```

### 3. 메시지 클래스 정의

```java
import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.annotation.AutoCalculate;
import hyun.messageconnecter.enums.Align;

@TcpMessage(totalLength = 100, charset = "EUC-KR")
public class TransactionRequest {
    @TcpField(order = 1, length = 10, autoCalculate = AutoCalculate.MESSAGE_LENGTH)
    private String messageLength;  // 자동 계산됨

    @TcpField(order = 2, length = 10)
    private String transactionCode;

    @TcpField(order = 3, length = 20)
    private String accountNumber;

    @TcpField(order = 4, length = 15, align = Align.RIGHT, paddingChar = '0')
    private Long amount;

    @TcpField(order = 5, length = 4, autoCalculate = AutoCalculate.CHECKSUM_CRC16)
    private String checksum;  // 자동 계산됨

    // Getters and setters omitted for brevity
}

@TcpMessage(totalLength = 150, charset = "EUC-KR")
public class TransactionResponse {
    @TcpField(order = 1, length = 10)
    private String messageLength;

    @TcpField(order = 2, length = 4)
    private String resultCode;

    @TcpField(order = 3, length = 100)
    private String resultMessage;

    @TcpField(order = 4, length = 4)
    private String checksum;
}
```

### 4. 활성화 및 사용

```java
@SpringBootApplication
@EnableTcpClient(basePackages = "com.mycompany.client")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Service
@RequiredArgsConstructor
public class BankService {
    private final BankClient bankClient;

    public void transfer(String accountNumber, Long amount) throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionCode("TRANSFER");
        request.setAccountNumber(accountNumber);
        request.setAmount(amount);
        // messageLength와 checksum은 자동 계산됨!

        TransactionResponse response = bankClient.send(request).get();

        if ("0000".equals(response.getResultCode())) {
            log.info("거래 성공: {}", response.getResultMessage());
        } else {
            log.error("거래 실패: {}", response.getResultMessage());
        }
    }
}
```

### 5. 설정 파일 (application.yml)

```yaml
bank:
  tcp:
    host: 192.168.1.100
    port: 9000
```

**끝! 이제 자동으로 직렬화/역직렬화되고, 체크섬도 자동 계산됩니다.** ✨

---

## 주요 기능

### 1. 고정 길이 프로토콜 지원

```java
@TcpField(order = 1, length = 10, align = Align.LEFT, paddingChar = ' ')
private String accountNumber;  // "1234567890"

@TcpField(order = 2, length = 15, align = Align.RIGHT, paddingChar = '0')
private Long amount;  // "000000050000"
```

### 2. 자동 계산 필드

```java
@TcpField(order = 1, length = 10, autoCalculate = AutoCalculate.MESSAGE_LENGTH)
private String messageLength;  // 전체 메시지 길이 자동 계산

@TcpField(order = 5, length = 4, autoCalculate = AutoCalculate.CHECKSUM_CRC16)
private String checksum;  // CRC16 체크섬 자동 계산

@TcpField(order = 5, length = 4, autoCalculate = AutoCalculate.CHECKSUM_CRC32)
private String checksum;  // CRC32 체크섬도 지원

@TcpField(order = 5, length = 4, autoCalculate = AutoCalculate.CHECKSUM_XOR)
private String checksum;  // XOR 체크섬도 지원
```

### 3. STX/ETX 프레이밍

```java
@Framing(stx = 0x02, etx = 0x03)
@TcpMessage(totalLength = 100, charset = "UTF-8")
public class FramedMessage {
    @TcpField(order = 1, length = 50)
    private String field1;

    @TcpField(order = 2, length = 50)
    private String field2;

    // Getters and setters omitted for brevity
}
// 전송: [STX][field1][field2][ETX]
```

### 4. 중첩 객체 및 리스트 지원

```java
@TcpMessage(totalLength = 500, charset = "UTF-8")
public class OrderRequest {
    @TcpField(order = 1, length = 20)
    private String orderId;

    // 중첩 객체
    @TcpField(order = 2, length = 100)
    private Customer customer;

    // 리스트 (최대 5개, 각 항목 50바이트)
    @TcpField(order = 3, maxSize = 5, itemLength = 50)
    private List<OrderItem> items;
}
```

### 5. 동기/비동기 모드

```java
public interface BankClient {
    // 비동기 모드
    CompletableFuture<Response> send(Request request);
}

// 동기 방식으로 사용
Response response = bankClient.send(request).get();
```

### 6. 다양한 데이터 타입 지원

```java
@TcpField(order = 1, length = 10)
private String stringField;

@TcpField(order = 2, length = 15, align = Align.RIGHT, paddingChar = '0')
private Long longField;

@TcpField(order = 3, length = 10, align = Align.RIGHT, paddingChar = '0')
private Integer intField;

@TcpField(order = 4, length = 8, format = "yyyyMMdd")
private LocalDate dateField;

@TcpField(order = 5, length = 6, format = "HHmmss")
private LocalTime timeField;
```

### 7. 인터셉터 (v1.1.0+) 

횡단 관심사(로깅, 메트릭, 재시도)를 인터셉터로 처리할 수 있습니다.

#### 내장 인터셉터

```yaml
# application.yml
tcp:
  client:
    interceptor:
      # 트랜잭션 ID 자동 생성 (기본 활성화)
      transaction:
        enabled: true

      # 요청/응답 로깅
      logging:
        enabled: true

      # 자동 재시도 (Exponential Backoff)
      retry:
        enabled: true
        max-attempts: 3
        backoff-ms: 1000

      # 메트릭 수집 (Micrometer 필요)
      metrics:
        enabled: true
```

#### 커스텀 인터셉터

```java
@Component
public class CustomInterceptor implements ClientInterceptor {
    @Override
    public CompletableFuture<Object> intercept(InterceptorContext context, InterceptorChain chain) {
        // 전처리
        log.info("Before: {}", context.getMethod().getName());

        return chain.proceed(context)
            .whenComplete((response, error) -> {
                // 후처리
                if (error != null) {
                    log.error("Error occurred", error);
                } else {
                    log.info("After: {}", response);
                }
            });
    }
}
```

**인터셉터 실행 순서**:
```
TransactionInterceptor → LoggingInterceptor → RetryInterceptor →
MetricsInterceptor → CustomInterceptor → 실제 TCP 호출
```

---

## 설치

### Gradle

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.mothership2002:tcp-client:v1.1.1'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.mothership2002</groupId>
    <artifactId>tcp-client</artifactId>
    <version>v1.1.1</version>
</dependency>
```

---

## 설정

### AutoConfiguration (자동 설정)

Spring Boot AutoConfiguration이 다음 Bean들을 자동으로 생성합니다:

```java
- EventLoopGroup (Netty)
- TcpMessageSerializer
- TcpMessageDeserializer
```

커스텀 설정이 필요하면 직접 Bean을 정의하면 자동 설정이 비활성화됩니다:

```java
@Configuration
public class CustomConfig {

    @Bean
    public EventLoopGroup eventLoopGroup() {
        // 커스텀 설정: 스레드 수 조정
        return new NioEventLoopGroup(10);
    }
}
```

### application.yml 설정

```yaml
# TCP 클라이언트 설정
bank:
  tcp:
    host: 192.168.1.100
    port: 9000

payment:
  tcp:
    host: 192.168.1.101
    port: 9001

# 인터셉터 설정 (v1.1.0+)
tcp:
  client:
    interceptor:
      transaction:
        enabled: true  # 트랜잭션 ID 자동 생성 (기본값)
      logging:
        enabled: true  # 요청/응답 로깅
      retry:
        enabled: false  # 재시도 (필요 시 활성화)
        max-attempts: 3
        backoff-ms: 1000
      metrics:
        enabled: true  # Micrometer 메트릭 수집 (기본값)

# Actuator 메트릭 엔드포인트 노출 (선택사항)
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus
  metrics:
    tags:
      application: ${spring.application.name}

# 로깅 설정
logging:
  level:
    hyun.messageconnecter: DEBUG
    # 인터셉터 상세 로그
    hyun.messageconnecter.interceptor: TRACE
```

---

## 실제 사용 예제

### 예제 1: 은행 계좌 이체

```java
// 1. 클라이언트 인터페이스
@TcpClient(
    host = "${bank.host}",
    port = "${bank.port}",
    connectionTimeout = "5000",
    readTimeout = "10000",
    charset = "EUC-KR"
)
public interface BankClient {
    CompletableFuture<TransferResponse> transfer(TransferRequest request);
    CompletableFuture<BalanceResponse> getBalance(BalanceRequest request);
}

// 2. 요청 메시지
@TcpMessage(totalLength = 129, charset = "EUC-KR")
public class TransferRequest {
    @TcpField(order = 1, length = 10, autoCalculate = AutoCalculate.MESSAGE_LENGTH)
    private String messageLength;

    @TcpField(order = 2, length = 10)
    private String transactionCode = "TRANSFER";

    @TcpField(order = 3, length = 20)
    private String fromAccount;

    @TcpField(order = 4, length = 20)
    private String toAccount;

    @TcpField(order = 5, length = 15, align = Align.RIGHT, paddingChar = '0')
    private Long amount;

    @TcpField(order = 6, length = 50)
    private String memo;

    @TcpField(order = 7, length = 4, autoCalculate = AutoCalculate.CHECKSUM_CRC16)
    private String checksum;

    // Getters and setters omitted for brevity
}

// 3. 응답 메시지
@TcpMessage(totalLength = 153, charset = "EUC-KR")
public class TransferResponse {
    @TcpField(order = 1, length = 10)
    private String messageLength;

    @TcpField(order = 2, length = 4)
    private String resultCode;

    @TcpField(order = 3, length = 100)
    private String resultMessage;

    @TcpField(order = 4, length = 20)
    private String transactionId;

    @TcpField(order = 5, length = 15, align = Align.RIGHT, paddingChar = '0')
    private Long balanceAfter;

    @TcpField(order = 6, length = 4)
    private String checksum;
}

// 4. 서비스 레이어
@Service
@RequiredArgsConstructor
@Slf4j
public class BankService {
    private final BankClient bankClient;

    @Transactional
    public CompletableFuture<TransferResult> transferMoney(
            String fromAccount,
            String toAccount,
            Long amount,
            String memo) {

        TransferRequest request = new TransferRequest();
        request.setFromAccount(fromAccount);
        request.setToAccount(toAccount);
        request.setAmount(amount);
        request.setMemo(memo);

        return bankClient.transfer(request)
            .thenApply(response -> {
                if ("0000".equals(response.getResultCode())) {
                    log.info("이체 성공: {} -> {} ({}원)",
                        fromAccount, toAccount, amount);
                    return TransferResult.success(response.getTransactionId());
                } else {
                    log.error("이체 실패: {}", response.getResultMessage());
                    return TransferResult.failure(response.getResultCode());
                }
            })
            .exceptionally(ex -> {
                log.error("통신 오류", ex);
                return TransferResult.error(ex.getMessage());
            });
    }
}
```

---

### 예제 2: 결제 승인 시스템

```java
// 1. 클라이언트 인터페이스
@TcpClient(
    host = "${payment.host}",
    port = "${payment.port}",
    connectionTimeout = "3000",
    readTimeout = "30000",  // 결제는 시간이 걸릴 수 있음
    charset = "UTF-8"
)
public interface PaymentClient {
    CompletableFuture<PaymentResponse> approve(PaymentRequest request);
    CompletableFuture<CancelResponse> cancel(CancelRequest request);
}

// 2. 결제 승인 요청
@Framing(stx = 0x02, etx = 0x03)  // STX/ETX 프레이밍
@TcpMessage(totalLength = 62, charset = "UTF-8")
public class PaymentRequest {
    @TcpField(order = 1, length = 10, autoCalculate = AutoCalculate.MESSAGE_LENGTH)
    private String messageLength;

    @TcpField(order = 2, length = 8, format = "yyyyMMdd")
    private LocalDate transactionDate;

    @TcpField(order = 3, length = 6, format = "HHmmss")
    private LocalTime transactionTime;

    @TcpField(order = 4, length = 16)
    private String cardNumber;

    @TcpField(order = 5, length = 4)
    private String expiryDate;  // YYMM

    @TcpField(order = 6, length = 12, align = Align.RIGHT, paddingChar = '0')
    private Long amount;

    @TcpField(order = 7, length = 2)
    private String installment;  // 00=일시불, 02=2개월...

    @TcpField(order = 8, length = 4, autoCalculate = AutoCalculate.CHECKSUM_CRC16)
    private String checksum;
}

// 3. 서비스 레이어
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentClient paymentClient;

    public CompletableFuture<PaymentResult> processPayment(Order order) {
        PaymentRequest request = new PaymentRequest();
        request.setTransactionDate(LocalDate.now());
        request.setTransactionTime(LocalTime.now());
        request.setCardNumber(order.getCardNumber());
        request.setExpiryDate(order.getExpiryDate());
        request.setAmount(order.getTotalAmount());
        request.setInstallment(order.getInstallment());

        return paymentClient.approve(request)
            .thenApply(response -> {
                if ("00".equals(response.getApprovalCode())) {
                    return PaymentResult.approved(
                        response.getApprovalNumber(),
                        response.getTransactionId()
                    );
                } else {
                    return PaymentResult.declined(response.getErrorMessage());
                }
            })
            .orTimeout(30, TimeUnit.SECONDS)  // 30초 타임아웃
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    return PaymentResult.timeout();
                }
                return PaymentResult.error(ex.getMessage());
            });
    }
}
```

---

## 테스트

### 단위 테스트

```java
@SpringBootTest
@EnableTcpClient(basePackages = "com.mycompany.client")
@Import({
    hyun.messageconnecter.config.TcpClientAutoConfiguration.class
})
class BankServiceTest {

    @Autowired
    private BankClient bankClient;

    @Test
    void testTransfer() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccount("1234567890");
        request.setToAccount("0987654321");
        request.setAmount(10000L);

        CompletableFuture<TransferResponse> future = bankClient.transfer(request);
        TransferResponse response = future.get();

        assertThat(response).isNotNull();
        assertThat(response.getResultCode()).isEqualTo("0000");
    }
}
```

---

## 문서

자세한 내용은 다음 문서를 참고하세요:

- **[사용 가이드](.docs/USAGE_EXAMPLES.md)** - 상세한 사용 예제
- **[설계 문서](.docs/DESIGN_DECISIONS.md)** - 아키텍처 설계 결정
- **[API 문서](.docs/PROJECT_OVERVIEW.md)** - 전체 API 레퍼런스
- **[트러블슈팅](.docs/E2E_TEST_TROUBLESHOOTING.md)** - 문제 해결 가이드
- **[로드맵](.docs/ROADMAP.md)** - 향후 개발 계획

---

### 메트릭 모니터링 (v1.1.0+)

Micrometer를 통해 실시간 메트릭을 수집할 수 있습니다:

**수집 메트릭**:
- `tcp.client.request.duration` - 요청 처리 시간 (Timer)
- `tcp.client.requests.total` - 요청 건수 (Counter)
- Tags: `method`, `status`, `error`

**Prometheus 쿼리 예시**:
```promql
# 평균 응답 시간
rate(tcp_client_request_duration_seconds_sum[5m]) /
rate(tcp_client_request_duration_seconds_count[5m])

# P99 레이턴시
histogram_quantile(0.99, rate(tcp_client_request_duration_seconds_bucket[5m]))

# 성공률
sum(rate(tcp_client_requests_total{status="success"}[5m])) /
sum(rate(tcp_client_requests_total[5m]))
```

**Grafana 대시보드**:
```json
{
  "panels": [
    {
      "title": "요청 처리 시간",
      "targets": [{
        "expr": "rate(tcp_client_request_duration_seconds_sum[5m]) / rate(tcp_client_request_duration_seconds_count[5m])"
      }]
    },
    {
      "title": "요청 건수 (TPS)",
      "targets": [{
        "expr": "sum(rate(tcp_client_requests_total[1m])) by (method)"
      }]
    }
  ]
}
```

---

##  기여

이슈 및 PR을 환영합니다!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

##  라이선스

MIT License - 자유롭게 사용하세요!

---

##  감사

이 프로젝트는 다음 기술을 사용합니다:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Netty](https://netty.io/)
- [Micrometer](https://micrometer.io/) (v1.1.0+)
- [AssertJ](https://assertj.github.io/doc/)

---

##  문의

- **Issues**: [GitHub Issues](https://github.com/mothership2002/tcp-client/issues)

---

**Made with using Vibe Coding (AI-Assisted Development)**
