package hyun.messageconnecter.fixture.tcp.nested;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.util.Objects;

/**
 * 중첩 객체 테스트용 - 메인 메시지
 * 300바이트 고정 (헤더 100 + 바디 200)
 */
@TcpMessage(totalLength = 300, charset = "UTF-8")
public class NestedMessage {

    @TcpField(order = 1, length = 100)
    private RequestHeader header;

    @TcpField(order = 2, length = 200, align = Align.LEFT)
    private String body;

    public NestedMessage() {
    }

    public NestedMessage(RequestHeader header, String body) {
        this.header = header;
        this.body = body;
    }

    public RequestHeader getHeader() {
        return header;
    }

    public void setHeader(RequestHeader header) {
        this.header = header;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NestedMessage that = (NestedMessage) o;
        return Objects.equals(header, that.header) &&
               Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, body);
    }

    @Override
    public String toString() {
        return "NestedMessage{" +
               "header=" + header +
               ", body='" + body + '\'' +
               '}';
    }
}
