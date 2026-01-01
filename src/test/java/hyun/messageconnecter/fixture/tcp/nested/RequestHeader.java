package hyun.messageconnecter.fixture.tcp.nested;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 중첩 객체 테스트용 - 헤더 객체
 * 100바이트 고정
 */
@TcpMessage(totalLength = 100, charset = "UTF-8")
public class RequestHeader {

    @TcpField(order = 1, length = 10, align = Align.LEFT)
    private String messageCode;

    @TcpField(order = 2, length = 14, format = "yyyyMMddHHmmss")
    private LocalDateTime timestamp;

    @TcpField(order = 3, length = 20, align = Align.LEFT)
    private String userId;

    public RequestHeader() {
    }

    public RequestHeader(String messageCode, LocalDateTime timestamp, String userId) {
        this.messageCode = messageCode;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestHeader that = (RequestHeader) o;
        return Objects.equals(messageCode, that.messageCode) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageCode, timestamp, userId);
    }

    @Override
    public String toString() {
        return "RequestHeader{" +
               "messageCode='" + messageCode + '\'' +
               ", timestamp=" + timestamp +
               ", userId='" + userId + '\'' +
               '}';
    }

    // 나머지 56바이트는 자동 패딩
}
