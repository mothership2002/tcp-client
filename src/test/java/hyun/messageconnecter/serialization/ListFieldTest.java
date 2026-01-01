package hyun.messageconnecter.serialization;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.list.BatchRequest;
import hyun.messageconnecter.fixture.tcp.list.TransactionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리스트 필드 직렬화/역직렬화 테스트
 */
class ListFieldTest {

    private TcpMessageSerializer serializer;
    private TcpMessageDeserializer deserializer;

    @BeforeEach
    void setUp() {
        serializer = new TcpMessageSerializer();
        deserializer = new TcpMessageDeserializer();
    }

    @Test
    @DisplayName("리스트 직렬화 테스트 - 3개 아이템")
    void testListSerialization() {
        // Given
        BatchRequest request = new BatchRequest();
        request.setBatchId("BATCH001");

        List<TransactionItem> items = new ArrayList<>();
        items.add(new TransactionItem("1234567890", "Hong Gil Dong", 100000L, "D"));
        items.add(new TransactionItem("0987654321", "Kim Chul Soo", 50000L, "W"));
        items.add(new TransactionItem("1111222233", "Lee Young Hee", 75000L, "D"));
        request.setItems(items);

        // When
        byte[] bytes = serializer.serialize(request);

        // Then
        assertThat(bytes).hasSize(30100); // totalLength

        String result = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("BatchId: [" + result.substring(0, 100) + "]");
        System.out.println("First Item: [" + result.substring(100, 200) + "]");
        System.out.println("Second Item: [" + result.substring(200, 300) + "]");
        System.out.println("Third Item: [" + result.substring(300, 400) + "]");

        // batchId 검증
        assertThat(result.substring(0, 8)).isEqualTo("BATCH001");

        // 첫 번째 아이템 검증 (100~200)
        String firstItem = result.substring(100, 200);
        assertThat(firstItem.substring(0, 20)).isEqualTo("1234567890          ");
        assertThat(firstItem.substring(20, 50)).isEqualTo("Hong Gil Dong                 ");
        assertThat(firstItem.substring(50, 65)).isEqualTo("000000000100000");
        assertThat(firstItem.substring(65, 66)).isEqualTo("D");

        // 두 번째 아이템 검증 (200~300)
        String secondItem = result.substring(200, 300);
        assertThat(secondItem.substring(0, 20)).isEqualTo("0987654321          ");

        // 세 번째 아이템 검증 (300~400)
        String thirdItem = result.substring(300, 400);
        assertThat(thirdItem.substring(0, 20)).isEqualTo("1111222233          ");
    }

    @Test
    @DisplayName("리스트 역직렬화 테스트")
    void testListDeserialization() {
        // Given
        BatchRequest original = new BatchRequest();
        original.setBatchId("BATCH002");

        List<TransactionItem> items = new ArrayList<>();
        items.add(new TransactionItem("ACC001", "User One", 1000L, "D"));
        items.add(new TransactionItem("ACC002", "User Two", 2000L, "W"));
        original.setItems(items);

        byte[] bytes = serializer.serialize(original);

        // When
        BatchRequest deserialized = deserializer.deserialize(bytes, BatchRequest.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getBatchId()).isEqualTo("BATCH002");
        assertThat(deserialized.getItems()).hasSize(2);

        TransactionItem item1 = deserialized.getItems().get(0);
        assertThat(item1.getAccountNumber()).isEqualTo("ACC001");
        assertThat(item1.getCustomerName()).isEqualTo("User One");
        assertThat(item1.getAmount()).isEqualTo(1000L);
        assertThat(item1.getTransactionType()).isEqualTo("D");

        TransactionItem item2 = deserialized.getItems().get(1);
        assertThat(item2.getAccountNumber()).isEqualTo("ACC002");
        assertThat(item2.getCustomerName()).isEqualTo("User Two");
        assertThat(item2.getAmount()).isEqualTo(2000L);
        assertThat(item2.getTransactionType()).isEqualTo("W");
    }

    @Test
    @DisplayName("리스트 라운드트립 테스트 - 300개 아이템")
    void testListRoundtripWithMaxSize() {
        // Given
        BatchRequest original = new BatchRequest();
        original.setBatchId("BATCH_MAX");

        // 300개 아이템 생성 (maxSize)
        List<TransactionItem> items = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            items.add(new TransactionItem(
                    "ACC" + String.format("%05d", i),
                    "User" + i,
                    (long) (i * 1000),
                    i % 2 == 0 ? "D" : "W"
            ));
        }
        original.setItems(items);

        // When
        byte[] serialized = serializer.serialize(original);
        BatchRequest deserialized = deserializer.deserialize(serialized, BatchRequest.class);

        // Then
        assertThat(serialized).hasSize(30100);
        assertThat(deserialized.getBatchId()).isEqualTo("BATCH_MAX");
        assertThat(deserialized.getItems()).hasSize(300);

        // 첫 번째와 마지막 아이템 검증
        TransactionItem first = deserialized.getItems().get(0);
        assertThat(first.getAccountNumber()).isEqualTo("ACC00000");
        assertThat(first.getCustomerName()).isEqualTo("User0");

        TransactionItem last = deserialized.getItems().get(299);
        assertThat(last.getAccountNumber()).isEqualTo("ACC00299");
        assertThat(last.getCustomerName()).isEqualTo("User299");
    }

    @Test
    @DisplayName("리스트가 비어있는 경우 테스트")
    void testEmptyListHandling() {
        // Given
        BatchRequest request = new BatchRequest();
        request.setBatchId("BATCH_EMPTY");
        request.setItems(new ArrayList<>()); // 빈 리스트

        // When
        byte[] bytes = serializer.serialize(request);
        BatchRequest deserialized = deserializer.deserialize(bytes, BatchRequest.class);

        // Then
        assertThat(bytes).hasSize(30100);
        assertThat(deserialized.getBatchId()).isEqualTo("BATCH_EMPTY");
        assertThat(deserialized.getItems()).isEmpty(); // 빈 리스트로 복원
    }

    @Test
    @DisplayName("리스트가 null인 경우 테스트")
    void testNullListHandling() {
        // Given
        BatchRequest request = new BatchRequest();
        request.setBatchId("BATCH_NULL");
        request.setItems(null); // null 리스트

        // When
        byte[] bytes = serializer.serialize(request);
        BatchRequest deserialized = deserializer.deserialize(bytes, BatchRequest.class);

        // Then
        assertThat(bytes).hasSize(30100);
        assertThat(deserialized.getBatchId()).isEqualTo("BATCH_NULL");
        assertThat(deserialized.getItems()).isEmpty(); // 빈 리스트로 복원 (패딩만 있음)
    }

    @Test
    @DisplayName("리스트 크기가 maxSize를 초과하는 경우 테스트")
    void testListSizeExceedsMaxSize() {
        // Given
        BatchRequest request = new BatchRequest();
        request.setBatchId("BATCH_OVERFLOW");

        // 350개 아이템 (maxSize=300 초과)
        List<TransactionItem> items = new ArrayList<>();
        for (int i = 0; i < 350; i++) {
            items.add(new TransactionItem("ACC" + i, "User" + i, (long) i, "D"));
        }
        request.setItems(items);

        // When
        byte[] bytes = serializer.serialize(request);
        BatchRequest deserialized = deserializer.deserialize(bytes, BatchRequest.class);

        // Then
        assertThat(bytes).hasSize(30100); // 여전히 고정 크기
        assertThat(deserialized.getItems()).hasSize(300); // maxSize만큼만 복원됨 (초과 아이템은 잘림)
    }
}
