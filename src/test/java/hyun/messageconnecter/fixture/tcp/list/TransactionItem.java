package hyun.messageconnecter.fixture.tcp.list;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.util.Objects;

/**
 * 리스트 테스트용 - 거래 아이템
 * 100바이트 고정
 */
@TcpMessage(totalLength = 100, charset = "UTF-8")
public class TransactionItem {

    @TcpField(order = 1, length = 20, align = Align.LEFT)
    private String accountNumber;

    @TcpField(order = 2, length = 30, align = Align.LEFT)
    private String customerName;

    @TcpField(order = 3, length = 15, align = Align.RIGHT, paddingChar = '0')
    private Long amount;

    @TcpField(order = 4, length = 1)
    private String transactionType; // D(Deposit) or W(Withdrawal)

    public TransactionItem() {
    }

    public TransactionItem(String accountNumber, String customerName, Long amount, String transactionType) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.amount = amount;
        this.transactionType = transactionType;
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
        TransactionItem that = (TransactionItem) o;
        return Objects.equals(accountNumber, that.accountNumber) &&
               Objects.equals(customerName, that.customerName) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(transactionType, that.transactionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber, customerName, amount, transactionType);
    }

    @Override
    public String toString() {
        return "TransactionItem{" +
               "accountNumber='" + accountNumber + '\'' +
               ", customerName='" + customerName + '\'' +
               ", amount=" + amount +
               ", transactionType='" + transactionType + '\'' +
               '}';
    }

    // 나머지 34바이트는 자동 패딩
}
