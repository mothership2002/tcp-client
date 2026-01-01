package hyun.messageconnecter.fixture.tcp.autocal;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;

import java.util.Objects;

/**
 * STX/ETX 프레이밍 테스트용 메시지
 */
@TcpMessage(
    totalLength = 100,
    charset = "UTF-8",
    stx = 0x02,  // Start of Text
    etx = 0x03   // End of Text
)
public class FramedMessage {

    @TcpField(order = 1, length = 50)
    private String field1;

    @TcpField(order = 2, length = 50)
    private String field2;

    public FramedMessage() {
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FramedMessage that = (FramedMessage) o;
        return Objects.equals(field1, that.field1) &&
               Objects.equals(field2, that.field2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2);
    }

    @Override
    public String toString() {
        return "FramedMessage{" +
               "field1='" + field1 + '\'' +
               ", field2='" + field2 + '\'' +
               '}';
    }
}
