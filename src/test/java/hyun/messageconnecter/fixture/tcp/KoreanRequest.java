package hyun.messageconnecter.fixture.tcp;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.util.Objects;

/**
 * 한글 인코딩 테스트용 메시지
 * EUC-KR 인코딩을 사용하여 한글이 바이트 레벨에서 정확히 처리되는지 검증
 */
@TcpMessage(totalLength = 100, charset = "EUC-KR")
public class KoreanRequest {

    // 한글 1글자 = 2바이트 (EUC-KR)
    // "홍길동" = 6바이트
    @TcpField(order = 1, length = 20, align = Align.LEFT)
    private String name;

    // 영문 + 숫자
    @TcpField(order = 2, length = 15, align = Align.LEFT)
    private String accountNumber;

    // 한글 주소
    @TcpField(order = 3, length = 50, align = Align.LEFT)
    private String address;

    public KoreanRequest() {
    }

    public KoreanRequest(String name, String accountNumber, String address) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KoreanRequest that = (KoreanRequest) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(accountNumber, that.accountNumber) &&
               Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, accountNumber, address);
    }

    @Override
    public String toString() {
        return "KoreanRequest{" +
               "name='" + name + '\'' +
               ", accountNumber='" + accountNumber + '\'' +
               ", address='" + address + '\'' +
               '}';
    }

    // 나머지 15바이트는 자동 패딩
}
