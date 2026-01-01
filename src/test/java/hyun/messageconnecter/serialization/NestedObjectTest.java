package hyun.messageconnecter.serialization;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.nested.NestedMessage;
import hyun.messageconnecter.fixture.tcp.nested.RequestHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 중첩 객체 직렬화/역직렬화 테스트
 */
class NestedObjectTest {

    private TcpMessageSerializer serializer;
    private TcpMessageDeserializer deserializer;

    @BeforeEach
    void setUp() {
        serializer = new TcpMessageSerializer();
        deserializer = new TcpMessageDeserializer();
    }

    @Test
    @DisplayName("중첩 객체 직렬화 테스트")
    void testNestedObjectSerialization() {
        // Given
        RequestHeader header = new RequestHeader();
        header.setMessageCode("REQ001");
        header.setTimestamp(LocalDateTime.of(2025, 1, 25, 14, 30, 45));
        header.setUserId("user123");

        NestedMessage message = new NestedMessage();
        message.setHeader(header);
        message.setBody("This is test body content");

        // When
        byte[] bytes = serializer.serialize(message);

        // Then
        assertThat(bytes).hasSize(300); // totalLength = 300

        String result = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Serialized: [" + result + "]");
        System.out.println("Length: " + bytes.length);

        // 헤더 검증 (0~100)
        String headerPart = result.substring(0, 100);
        assertThat(headerPart.substring(0, 10)).isEqualTo("REQ001    "); // messageCode
        assertThat(headerPart.substring(10, 24)).isEqualTo("20250125143045"); // timestamp
        assertThat(headerPart.substring(24, 44)).isEqualTo("user123             "); // userId

        // 바디 검증 (100~300)
        String bodyPart = result.substring(100, 300);
        assertThat(bodyPart).startsWith("This is test body content");
    }

    @Test
    @DisplayName("중첩 객체 역직렬화 테스트")
    void testNestedObjectDeserialization() {
        // Given
        RequestHeader header = new RequestHeader();
        header.setMessageCode("REQ002");
        header.setTimestamp(LocalDateTime.of(2025, 12, 31, 23, 59, 59));
        header.setUserId("admin");

        NestedMessage original = new NestedMessage();
        original.setHeader(header);
        original.setBody("Test body");

        byte[] bytes = serializer.serialize(original);

        // When
        NestedMessage deserialized = deserializer.deserialize(bytes, NestedMessage.class);

        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getHeader()).isNotNull();
        assertThat(deserialized.getHeader().getMessageCode()).isEqualTo("REQ002");
        assertThat(deserialized.getHeader().getTimestamp()).isEqualTo(LocalDateTime.of(2025, 12, 31, 23, 59, 59));
        assertThat(deserialized.getHeader().getUserId()).isEqualTo("admin");
        assertThat(deserialized.getBody()).isEqualTo("Test body");
    }

    @Test
    @DisplayName("중첩 객체 라운드트립 테스트")
    void testNestedObjectRoundtrip() {
        // Given
        RequestHeader header = new RequestHeader();
        header.setMessageCode("ROUND");
        header.setTimestamp(LocalDateTime.of(2025, 6, 15, 12, 0, 0));
        header.setUserId("tester");

        NestedMessage original = new NestedMessage();
        original.setHeader(header);
        original.setBody("Roundtrip test message");

        // When
        byte[] serialized = serializer.serialize(original);
        NestedMessage deserialized = deserializer.deserialize(serialized, NestedMessage.class);

        // Then
        assertThat(deserialized.getHeader().getMessageCode()).isEqualTo(original.getHeader().getMessageCode());
        assertThat(deserialized.getHeader().getTimestamp()).isEqualTo(original.getHeader().getTimestamp());
        assertThat(deserialized.getHeader().getUserId()).isEqualTo(original.getHeader().getUserId());
        assertThat(deserialized.getBody()).isEqualTo(original.getBody());
    }

    @Test
    @DisplayName("중첩 객체가 null인 경우 테스트")
    void testNestedObjectNullHandling() {
        // Given
        NestedMessage message = new NestedMessage();
        message.setHeader(null); // null 헤더
        message.setBody("Body only");

        // When
        byte[] bytes = serializer.serialize(message);
        NestedMessage deserialized = deserializer.deserialize(bytes, NestedMessage.class);

        // Then
        assertThat(bytes).hasSize(300);
        assertThat(deserialized.getHeader()).isNull(); // null로 복원됨
        assertThat(deserialized.getBody()).isEqualTo("Body only");
    }
}
