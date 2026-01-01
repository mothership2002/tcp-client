package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.util.Objects;

/**
 * 간단한 TCP 응답 메시지 (테스트용)
 * 총 30바이트 고정 길이
 */
@TcpMessage(totalLength = 30, charset = "UTF-8")
public class SimpleResponse {

    @TcpField(order = 1, length = 4, align = Align.LEFT)
    private String resultCode;

    @TcpField(order = 2, length = 20, align = Align.LEFT)
    private String resultMessage;

    public SimpleResponse() {
    }

    public SimpleResponse(String resultCode, String resultMessage) {
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleResponse that = (SimpleResponse) o;
        return Objects.equals(resultCode, that.resultCode) &&
               Objects.equals(resultMessage, that.resultMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultCode, resultMessage);
    }

    @Override
    public String toString() {
        return "SimpleResponse{" +
               "resultCode='" + resultCode + '\'' +
               ", resultMessage='" + resultMessage + '\'' +
               '}';
    }

    // 나머지 6바이트는 자동 패딩
}
