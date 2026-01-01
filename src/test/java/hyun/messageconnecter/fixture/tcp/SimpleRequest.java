package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.util.Objects;

/**
 * 간단한 TCP 요청 메시지 (테스트용)
 * 총 50바이트 고정 길이
 */
@TcpMessage(totalLength = 50, charset = "UTF-8")
public class SimpleRequest {

    @TcpField(order = 1, length = 10, align = Align.LEFT)
    private String messageCode;

    @TcpField(order = 2, length = 20, align = Align.LEFT)
    private String data;

    @TcpField(order = 3, length = 10, align = Align.RIGHT, paddingChar = '0')
    private Long number;

    public SimpleRequest() {
    }

    public SimpleRequest(String messageCode, String data, Long number) {
        this.messageCode = messageCode;
        this.data = data;
        this.number = number;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleRequest that = (SimpleRequest) o;
        return Objects.equals(messageCode, that.messageCode) &&
               Objects.equals(data, that.data) &&
               Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageCode, data, number);
    }

    @Override
    public String toString() {
        return "SimpleRequest{" +
               "messageCode='" + messageCode + '\'' +
               ", data='" + data + '\'' +
               ", number=" + number +
               '}';
    }

    // 나머지 10바이트는 자동 패딩
}
