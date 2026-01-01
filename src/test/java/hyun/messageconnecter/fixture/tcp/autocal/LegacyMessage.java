package hyun.messageconnecter.fixture.tcp.autocal;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;
import hyun.messageconnecter.enums.AutoCalculate;

import java.util.Objects;

/**
 * 자동 계산 필드 테스트용 메시지
 * MESSAGE_LENGTH와 CHECKSUM_CRC16을 자동으로 계산
 */
@TcpMessage(totalLength = 100, charset = "UTF-8")
public class LegacyMessage {

    // 메시지 길이 자동 계산
    @TcpField(order = 1, length = 10, autoCalculate = AutoCalculate.MESSAGE_LENGTH, align = Align.RIGHT, paddingChar = '0')
    private String messageLength;

    // 거래 코드
    @TcpField(order = 2, length = 10)
    private String transactionCode;

    // 데이터
    @TcpField(order = 3, length = 76)
    private String data;

    // CRC16 체크섬 자동 계산
    @TcpField(order = 4, length = 4, autoCalculate = AutoCalculate.CHECKSUM_CRC16)
    private String checksum;

    public LegacyMessage() {
    }

    public String getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(String messageLength) {
        this.messageLength = messageLength;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegacyMessage that = (LegacyMessage) o;
        return Objects.equals(messageLength, that.messageLength) &&
               Objects.equals(transactionCode, that.transactionCode) &&
               Objects.equals(data, that.data) &&
               Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageLength, transactionCode, data, checksum);
    }

    @Override
    public String toString() {
        return "LegacyMessage{" +
               "messageLength='" + messageLength + '\'' +
               ", transactionCode='" + transactionCode + '\'' +
               ", data='" + data + '\'' +
               ", checksum='" + checksum + '\'' +
               '}';
    }
}
