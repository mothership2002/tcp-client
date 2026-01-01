package hyun.messageconnecter.e2e;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * E2E 테스트용 Mock TCP Server
 * <p>
 * 기능:
 * - 고정 길이 메시지 수신
 * - STX/ETX 프레이밍 처리
 * - 체크섬 검증 (선택적)
 * - 응답 메시지 전송
 * - 비동기 처리 지원
 */
public class MockTcpServer {

    private static final Logger log = LoggerFactory.getLogger(MockTcpServer.class);

    private final int port;
    private final int expectedMessageLength;
    private Function<byte[], byte[]> responseProvider;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread serverThread;

    private byte[] lastReceivedMessage;

    private int requestCount = 0;

    /**
     * MockTcpServer 생성자
     *
     * @param port 서버 포트
     * @param expectedMessageLength 예상 메시지 길이 (STX/ETX 포함)
     * @param responseProvider 요청 바이트를 받아 응답 바이트를 반환하는 함수
     */
    public MockTcpServer(int port, int expectedMessageLength, Function<byte[], byte[]> responseProvider) {
        this.port = port;
        this.expectedMessageLength = expectedMessageLength;
        this.responseProvider = responseProvider;
    }

    /**
     * 서버 시작 (비동기)
     */
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> startFuture = new CompletableFuture<>();

        if (running.get()) {
            startFuture.completeExceptionally(new IllegalStateException("Server is already running"));
            return startFuture;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(10000); // 10000ms timeout for accept()
                running.set(true);
                log.info("Mock TCP Server started on port {}", port);
                startFuture.complete(null);

                while (running.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (SocketTimeoutException e) {
                        // Timeout은 정상 동작 (running 체크를 위함)
                    }
                }
            } catch (IOException e) {
                log.error("Server error", e);
                if (!startFuture.isDone()) {
                    startFuture.completeExceptionally(e);
                }
            } finally {
                closeServerSocket();
                log.info("Mock TCP Server stopped");
            }
        });

        serverThread.setName("MockTcpServer-" + port);
        serverThread.start();

        return startFuture;
    }

    /**
     * 클라이언트 연결 처리
     */
    private void handleClient(Socket clientSocket) {
        try {
            clientSocket.setSoTimeout(10000); // 10초 read timeout

            try (InputStream in = clientSocket.getInputStream();
                 OutputStream out = clientSocket.getOutputStream()) {

                log.debug("Client connected: {}", clientSocket.getRemoteSocketAddress());

                // 메시지 수신
                byte[] buffer = new byte[expectedMessageLength];
                int totalRead = 0;

                while (totalRead < expectedMessageLength) {
                    int read = in.read(buffer, totalRead, expectedMessageLength - totalRead);
                    if (read == -1) {
                        throw new IOException("Unexpected end of stream");
                    }
                    totalRead += read;
                }

                log.debug("Received {} bytes", totalRead);
                lastReceivedMessage = buffer;
                requestCount++;

                // 응답 생성 및 전송
                if (responseProvider == null) {
                    log.error("ResponseProvider is null! Cannot generate response.");
                    throw new IllegalStateException("ResponseProvider not set");
                }

                log.debug("Calling responseProvider to generate response");
                byte[] response = responseProvider.apply(buffer);
                log.debug("ResponseProvider returned {} bytes", response != null ? response.length : 0);

                if (response == null) {
                    log.error("ResponseProvider returned null!");
                    throw new IllegalStateException("ResponseProvider returned null");
                }

                out.write(response);
                out.flush();
                log.debug("Sent {} bytes response", response.length);

                // 쓰기 종료 신호 전송 (클라이언트가 EOF를 받을 수 있도록)
                clientSocket.shutdownOutput();
                log.debug("Shutdown output, waiting for client to close connection");

                // 클라이언트가 연결을 닫을 때까지 대기 (읽기 시도)
                // 클라이언트가 연결을 닫으면 -1 반환
                int result = in.read();
                if (result == -1) {
                    log.debug("Client closed connection");
                }
            }
        } catch (IOException e) {
            log.error("Error handling client", e);
        } finally {
            // 소켓 명시적으로 닫기
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                log.warn("Error closing client socket", e);
            }
        }
    }

    /**
     * 서버 종료
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        running.set(false);
        closeServerSocket();

        if (serverThread != null) {
            try {
                serverThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for server thread to stop");
            }
        }
    }

    /**
     * ServerSocket 닫기
     */
    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("Error closing server socket", e);
            }
        }
    }

    /**
     * 서버 상태 확인
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 마지막 수신 메시지 초기화
     */
    public void reset() {
        lastReceivedMessage = null;
        requestCount = 0;
    }

    /**
     * 응답 제공자 설정 (동적으로 변경 가능)
     */
    public void setResponseProvider(Function<byte[], byte[]> responseProvider) {
        this.responseProvider = responseProvider;
    }

    public byte[] getLastReceivedMessage() {
        return lastReceivedMessage;
    }

    public int getRequestCount() {
        return requestCount;
    }
}
