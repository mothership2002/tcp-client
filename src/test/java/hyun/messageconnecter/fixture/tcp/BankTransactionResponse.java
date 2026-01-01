package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.util.Objects;

/**
 * 은행 거래 응답 메시지 (테스트용)
 * 총 150바이트 고정 길이
 */
@TcpMessage(totalLength = 150, charset = "EUC-KR")
public class BankTransactionResponse {

    @TcpField(order = 1, length = 4, align = Align.LEFT)
    private String resultCode; // 0000: 성공, E001: 실패

    @TcpField(order = 2, length = 100, align = Align.LEFT)
    private String resultMessage;

    @TcpField(order = 3, length = 20, align = Align.LEFT)
    private String transactionId;

    @TcpField(order = 4, length = 15, align = Align.RIGHT, paddingChar = '0')
    private Long balanceAfter;

    public BankTransactionResponse() {
    }

    public BankTransactionResponse(String resultCode, String resultMessage, String transactionId, Long balanceAfter) {
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.transactionId = transactionId;
        this.balanceAfter = balanceAfter;
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankTransactionResponse that = (BankTransactionResponse) o;
        return Objects.equals(resultCode, that.resultCode) &&
               Objects.equals(resultMessage, that.resultMessage) &&
               Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(balanceAfter, that.balanceAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultCode, resultMessage, transactionId, balanceAfter);
    }

    @Override
    public String toString() {
        return "BankTransactionResponse{" +
               "resultCode='" + resultCode + '\'' +
               ", resultMessage='" + resultMessage + '\'' +
               ", transactionId='" + transactionId + '\'' +
               ", balanceAfter=" + balanceAfter +
               '}';
    }

    // 나머지 11바이트는 자동 패딩
}
