package hyun.messageconnecter.integration;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.client.tcp.TcpMessageClient;
import hyun.messageconnecter.e2e.MockTcpServer;
import hyun.messageconnecter.fixture.tcp.autocal.FramedMessage;
import hyun.messageconnecter.fixture.tcp.autocal.LegacyMessage;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TCP Client E2E 테스트
 * <p>
 * 실제 TCP 소켓 통신 환경에서 클라이언트 동작을 검증합니다.
 * <p>
 * 테스트 시나리오:
 * 1. 자동 계산 필드 메시지 송수신 (MESSAGE_LENGTH, CHECKSUM_CRC16)
 * 2. STX/ETX 프레이밍 메시지 송수신
 * 3. 라운드트립 테스트 (직렬화 → 전송 → 수신 → 역직렬화)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TcpClientE2ETest {

    private static final int TEST_PORT = 18888;
    private static final String TEST_HOST = "localhost";
    private static final int CONNECTION_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 5000;

    private final TcpMessageSerializer serializer = new TcpMessageSerializer();

    private final TcpMessageDeserializer deserializer = new TcpMessageDeserializer();

    private MockTcpServer mockServer100;  // LegacyMessage용 (100바이트)
    private MockTcpServer mockServer102;  // FramedMessage용 (102바이트)
    private TcpMessageClient tcpClient;
    private EventLoopGroup eventLoopGroup;
    private ExecutorService executor;

    @BeforeAll
    void startServers() throws Exception {
        // 100바이트 메시지용 서버 시작
        mockServer100 = new MockTcpServer(TEST_PORT, 100, null);
        mockServer100.start().get(5, TimeUnit.SECONDS);

        // 102바이트 메시지용 서버 시작 (다른 포트)
        mockServer102 = new MockTcpServer(TEST_PORT + 1, 102, null);
        mockServer102.start().get(5, TimeUnit.SECONDS);
    }

    @AfterAll
    void stopServers() {
        if (mockServer100 != null) {
            mockServer100.stop();
        }
        if (mockServer102 != null) {
            mockServer102.stop();
        }
    }

    @BeforeEach
    void setUp() {
        eventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        executor = Executors.newCachedThreadPool();
        tcpClient = new TcpMessageClient(
                TEST_HOST,
                TEST_PORT,
                CONNECTION_TIMEOUT,
                READ_TIMEOUT,
                "UTF-8",
                eventLoopGroup,
                executor,
                serializer,
                deserializer
        );

        // 각 테스트 전에 카운터 리셋
        mockServer100.reset();
        mockServer102.reset();
    }

    @AfterEach
    void tearDown() {
        if (tcpClient != null) {
            tcpClient.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    void testAutoCalculateFieldsE2E() throws Exception {
        // Given: Mock 서버에 응답 로직 설정
        mockServer100.setResponseProvider(requestBytes -> {
            // 요청 역직렬화
            LegacyMessage requestMsg = deserializer.deserialize(requestBytes, LegacyMessage.class);

            // 응답 메시지 생성 (실제 서버처럼 처리된 응답)
            // transactionCode는 10바이트 고정이므로 짧게 설정
            LegacyMessage responseMsg = new LegacyMessage();
            responseMsg.setTransactionCode("RESP_OK");  // 7자 (10바이트 내)
            responseMsg.setData("Processed: " + requestMsg.getData());
            // messageLength와 checksum은 자동 계산됨

            // 응답 직렬화
            return serializer.serialize(responseMsg);
        });

        // Given: 자동 계산 필드가 있는 요청 메시지
        LegacyMessage request = new LegacyMessage();
        request.setTransactionCode("DEPOSIT");
        request.setData("E2E test for auto-calculated fields");

        // When: TCP 클라이언트로 메시지 전송
        CompletableFuture<LegacyMessage> responseFuture = tcpClient.send(request, LegacyMessage.class);
        LegacyMessage response = responseFuture.get(READ_TIMEOUT, TimeUnit.MILLISECONDS);

        // Then: 응답이 정상적으로 수신됨
        assertThat(response).isNotNull();
        assertThat(response.getTransactionCode()).startsWith("RESP_OK");  // 서버가 변환한 값
        assertThat(response.getData()).startsWith("Processed: E2E test");  // 서버가 처리한 데이터

        // Then: 자동 계산 필드가 올바르게 처리됨
        assertThat(response.getMessageLength()).isEqualTo("0000000100");  // 100바이트
        assertThat(response.getChecksum()).isNotEmpty().hasSize(4);  // CRC16 체크섬

        // Then: 서버가 요청을 정확히 수신함
        assertThat(mockServer100.getLastReceivedMessage()).hasSize(100);
        assertThat(mockServer100.getRequestCount()).isEqualTo(1);
    }

    @Test
    void testSTXETXFramingE2E() throws Exception {
        // Given: Mock 서버에 응답 로직 설정
        mockServer102.setResponseProvider(requestBytes -> {
            // STX/ETX 검증
            assertThat(requestBytes[0]).isEqualTo((byte) 0x02);  // STX
            assertThat(requestBytes[requestBytes.length - 1]).isEqualTo((byte) 0x03);  // ETX

            // 요청 역직렬화
            FramedMessage requestMsg = deserializer.deserialize(requestBytes, FramedMessage.class);

            // 응답 메시지 생성 (실제 서버처럼 처리된 응답)
            FramedMessage responseMsg = new FramedMessage();
            responseMsg.setField1("Response to: " + requestMsg.getField1());
            responseMsg.setField2("Response to: " + requestMsg.getField2());

            // 응답 직렬화
            return serializer.serialize(responseMsg);
        });

        // Given: STX/ETX 프레이밍이 설정된 요청 메시지
        FramedMessage request = new FramedMessage();
        request.setField1("E2E framing test field 1");
        request.setField2("E2E framing test field 2");

        // When: TCP 클라이언트로 메시지 전송 (102바이트 서버 포트 사용)
        TcpMessageClient tcpClient102 = new TcpMessageClient(
                TEST_HOST,
                TEST_PORT + 1,  // 102바이트 서버 포트
                CONNECTION_TIMEOUT,
                READ_TIMEOUT,
                "UTF-8",
                eventLoopGroup,
                executor,
                serializer,
                deserializer
        );
        CompletableFuture<FramedMessage> responseFuture = tcpClient102.send(request, FramedMessage.class);
        FramedMessage response = responseFuture.get(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        tcpClient102.shutdown();

        // Then: 응답이 정상적으로 수신됨
        assertThat(response).isNotNull();
        assertThat(response.getField1()).startsWith("Response to: E2E framing test field 1");
        assertThat(response.getField2()).startsWith("Response to: E2E framing test field 2");

        // Then: 서버가 STX/ETX를 포함한 102바이트를 수신함
        assertThat(mockServer102.getLastReceivedMessage()).hasSize(102);
        assertThat(mockServer102.getLastReceivedMessage()[0]).isEqualTo((byte) 0x02);
        assertThat(mockServer102.getLastReceivedMessage()[101]).isEqualTo((byte) 0x03);
        assertThat(mockServer102.getRequestCount()).isEqualTo(1);
    }

    @Test
    void testChecksumValidationE2E() throws Exception {
        // Given: Mock 서버에 응답 로직 설정
        mockServer100.setResponseProvider(requestBytes -> {
            // 요청을 역직렬화하여 체크섬 검증 (deserializer가 자동으로 검증함)
            LegacyMessage requestMessage = deserializer.deserialize(requestBytes, LegacyMessage.class);

            // 새로운 응답 메시지 생성
            LegacyMessage responseMessage = new LegacyMessage();
            responseMessage.setTransactionCode("RESPONSE");
            responseMessage.setData("Checksum validation passed");

            // 응답 직렬화 (체크섬 자동 계산)
            return serializer.serialize(responseMessage);
        });

        // Given: 체크섬 자동 계산이 포함된 요청 메시지
        LegacyMessage request = new LegacyMessage();
        request.setTransactionCode("REQUEST");
        request.setData("Test checksum validation in E2E");

        // When: TCP 클라이언트로 메시지 전송
        CompletableFuture<LegacyMessage> responseFuture = tcpClient.send(request, LegacyMessage.class);
        LegacyMessage response = responseFuture.get(READ_TIMEOUT, TimeUnit.MILLISECONDS);

        // Then: 응답이 정상적으로 수신됨 (체크섬 검증 통과)
        assertThat(response).isNotNull();
        assertThat(response.getTransactionCode()).isEqualTo("RESPONSE");
        assertThat(response.getData()).startsWith("Checksum validation passed");
        assertThat(response.getChecksum()).isNotEmpty();
    }

    @Test
    void testMultipleRequestsE2E() throws Exception {
        // Given: Mock 서버에 응답 로직 설정
        mockServer100.setResponseProvider(requestBytes -> {
            // 요청 역직렬화
            LegacyMessage requestMsg = deserializer.deserialize(requestBytes, LegacyMessage.class);

            // 응답 메시지 생성 (실제 서버처럼 처리된 응답)
            // transactionCode는 10바이트 고정이므로 짧게 설정
            LegacyMessage responseMsg = new LegacyMessage();
            responseMsg.setTransactionCode(requestMsg.getTransactionCode() + "_OK");  // 예: "REQ_0_OK" = 8자
            responseMsg.setData("Server processed: " + requestMsg.getData());

            // 응답 직렬화
            return serializer.serialize(responseMsg);
        });

        // When: 여러 번 요청 전송
        for (int i = 0; i < 5; i++) {
            LegacyMessage request = new LegacyMessage();
            request.setTransactionCode("REQ_" + i);
            request.setData("Multiple request test " + i);

            CompletableFuture<LegacyMessage> responseFuture = tcpClient.send(request, LegacyMessage.class);
            LegacyMessage response = responseFuture.get(READ_TIMEOUT, TimeUnit.MILLISECONDS);

            // Then: 각 응답이 정상적으로 수신됨
            assertThat(response).isNotNull();
            assertThat(response.getTransactionCode()).startsWith("REQ_" + i + "_OK");  // 10바이트 제한
            assertThat(response.getData()).startsWith("Server processed: Multiple request test " + i);
        }

        // Then: 서버가 5개의 요청을 모두 처리함
        assertThat(mockServer100.getRequestCount()).isEqualTo(5);
    }

    @Test
    void testRoundtripConsistency() throws Exception {
        // Given: Mock 서버에 응답 로직 설정
        mockServer102.setResponseProvider(requestBytes -> {
            // 요청 역직렬화
            FramedMessage requestMessage = deserializer.deserialize(requestBytes, FramedMessage.class);

            // 새로운 응답 생성
            FramedMessage responseMessage = new FramedMessage();
            responseMessage.setField1("Response to: " + requestMessage.getField1());
            responseMessage.setField2("Response to: " + requestMessage.getField2());

            // 응답 직렬화
            return serializer.serialize(responseMessage);
        });

        // Given: 프레이밍된 요청 메시지
        FramedMessage request = new FramedMessage();
        request.setField1("Original field 1");
        request.setField2("Original field 2");

        // When: TCP 클라이언트로 메시지 전송 (102바이트 서버 포트 사용)
        TcpMessageClient tcpClient102 = new TcpMessageClient(
                TEST_HOST,
                TEST_PORT + 1,  // 102바이트 서버 포트
                CONNECTION_TIMEOUT,
                READ_TIMEOUT,
                "UTF-8",
                eventLoopGroup,
                executor,
                serializer,
                deserializer
        );
        CompletableFuture<FramedMessage> responseFuture = tcpClient102.send(request, FramedMessage.class);
        FramedMessage response = responseFuture.get(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        tcpClient102.shutdown();

        // Then: 서버가 요청을 역직렬화하고 새로운 응답을 생성함
        assertThat(response).isNotNull();
        assertThat(response.getField1()).startsWith("Response to: Original field 1");
        assertThat(response.getField2()).startsWith("Response to: Original field 2");
    }

    @Test
    void testConnectionTimeout() throws Exception {
        // Given: 서버가 시작되지 않은 상태 (연결 실패 시뮬레이션)
        TcpMessageClient failClient = new TcpMessageClient(
                TEST_HOST,
                19999,  // 존재하지 않는 포트
                500,  // 짧은 타임아웃
                5000,
                "UTF-8",
                eventLoopGroup,
                executor,
                serializer,
                deserializer
        );

        // When: 메시지 전송 시도
        LegacyMessage request = new LegacyMessage();
        request.setTransactionCode("TIMEOUT");
        request.setData("This should timeout");

        CompletableFuture<LegacyMessage> responseFuture = failClient.send(request, LegacyMessage.class);

        // Then: 타임아웃 또는 연결 실패 예외 발생
        assertThat(responseFuture)
                .failsWithin(2, TimeUnit.SECONDS)
                .withThrowableOfType(Exception.class);
    }
}
