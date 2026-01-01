package hyun.messageconnecter.fixture.tcp.list;

import hyun.messageconnecter.annotation.TcpField;
import hyun.messageconnecter.annotation.TcpMessage;
import hyun.messageconnecter.enums.Align;

import java.util.List;
import java.util.Objects;

/**
 * 리스트 테스트용 - 배치 요청 메시지
 * 총 30100바이트 (헤더 100 + 거래 아이템 300개 × 100바이트)
 */
@TcpMessage(totalLength = 30100, charset = "UTF-8")
public class BatchRequest {

    @TcpField(order = 1, length = 100, align = Align.LEFT)
    private String batchId;

    @TcpField(order = 2, length = 30000, maxSize = 300, itemLength = 100)
    private List<TransactionItem> items;

    public BatchRequest() {
    }

    public BatchRequest(String batchId, List<TransactionItem> items) {
        this.batchId = batchId;
        this.items = items;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchRequest that = (BatchRequest) o;
        return Objects.equals(batchId, that.batchId) &&
               Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, items);
    }

    @Override
    public String toString() {
        return "BatchRequest{" +
               "batchId='" + batchId + '\'' +
               ", items=" + items +
               '}';
    }
}
