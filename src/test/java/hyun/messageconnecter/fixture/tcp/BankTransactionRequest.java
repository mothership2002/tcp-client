package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;
import hyun.messageconnecter.enums.Transform;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 은행 거래 요청 메시지 (테스트용)
 * 레거시 은행 시스템과의 통신을 시뮬레이션
 * 총 200바이트 고정 길이
 */
@TcpMessage(totalLength = 200, charset = "EUC-KR")
public class BankTransactionRequest {

    @TcpField(order = 1, length = 10, align = Align.LEFT)
    private String messageCode;

    @TcpField(order = 2, length = 20, align = Align.LEFT)
    private String accountNumber;

    @TcpField(order = 3, length = 50, align = Align.LEFT, transform = Transform.UPPER)
    private String customerName;

    @TcpField(order = 4, length = 8, format = "yyyyMMdd")
    private LocalDate transactionDate;

    @TcpField(order = 5, length = 15, align = Align.RIGHT, paddingChar = '0')
    private Long amount;

    @TcpField(order = 6, length = 1)
    private String transactionType; // D(Deposit) or W(Withdrawal)

    public BankTransactionRequest() {
    }

    public BankTransactionRequest(String messageCode, String accountNumber, String customerName,
                                   LocalDate transactionDate, Long amount, String transactionType) {
        this.messageCode = messageCode;
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.transactionType = transactionType;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankTransactionRequest that = (BankTransactionRequest) o;
        return Objects.equals(messageCode, that.messageCode) &&
               Objects.equals(accountNumber, that.accountNumber) &&
               Objects.equals(customerName, that.customerName) &&
               Objects.equals(transactionDate, that.transactionDate) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(transactionType, that.transactionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageCode, accountNumber, customerName, transactionDate, amount, transactionType);
    }

    @Override
    public String toString() {
        return "BankTransactionRequest{" +
               "messageCode='" + messageCode + '\'' +
               ", accountNumber='" + accountNumber + '\'' +
               ", customerName='" + customerName + '\'' +
               ", transactionDate=" + transactionDate +
               ", amount=" + amount +
               ", transactionType='" + transactionType + '\'' +
               '}';
    }

    // 나머지 96바이트는 자동 패딩
}
