package hyun.messageconnecter.integration;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.autocal.FramedMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * STX/ETX 프레이밍 통합 테스트
 * - STX(0x02) 추가 확인
 * - ETX(0x03) 추가 확인
 * - STX/ETX 검증 실패 시나리오
 */
class FramingIntegrationTest {

    private TcpMessageSerializer serializer = new TcpMessageSerializer();

    private TcpMessageDeserializer deserializer = new TcpMessageDeserializer();

    @Test
    void testSTXETXFramingAdded() {
        // Given: STX=0x02, ETX=0x03이 설정된 메시지
        FramedMessage message = new FramedMessage();
        message.setField1("Test field 1");
        message.setField2("Test field 2");

        // When: 직렬화
        byte[] bytes = serializer.serialize(message);

        // Then: 총 길이는 100(메시지) + 1(STX) + 1(ETX) = 102바이트
        assertThat(bytes).hasSize(102);

        // Then: 첫 바이트는 STX (0x02)
        assertThat(bytes[0]).isEqualTo((byte) 0x02);

        // Then: 마지막 바이트는 ETX (0x03)
        assertThat(bytes[bytes.length - 1]).isEqualTo((byte) 0x03);
    }

    @Test
    void testSTXETXRoundtrip() {
        // Given: 원본 메시지
        FramedMessage original = new FramedMessage();
        original.setField1("Roundtrip test field 1");
        original.setField2("Roundtrip test field 2");

        // When: 직렬화 후 역직렬화
        byte[] bytes = serializer.serialize(original);
        FramedMessage deserialized = deserializer.deserialize(bytes, FramedMessage.class);

        // Then: 역직렬화 성공 및 데이터 일치
        assertThat(deserialized.getField1()).startsWith("Roundtrip test field 1");
        assertThat(deserialized.getField2()).startsWith("Roundtrip test field 2");
    }

    @Test
    void testSTXValidationFailure() {
        // Given: 올바른 메시지
        FramedMessage message = new FramedMessage();
        message.setField1("STX test");
        message.setField2("Validation test");
        byte[] bytes = serializer.serialize(message);

        // When: STX를 잘못된 값으로 변경
        bytes[0] = 0x01;  // 0x02가 아닌 0x01로 변경

        // Then: 역직렬화 시 STX 검증 실패
        assertThatThrownBy(() -> deserializer.deserialize(bytes, FramedMessage.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid STX")
            .hasMessageContaining("expected 0x02")
            .hasMessageContaining("got 0x01");
    }

    @Test
    void testETXValidationFailure() {
        // Given: 올바른 메시지
        FramedMessage message = new FramedMessage();
        message.setField1("ETX test");
        message.setField2("Validation test");
        byte[] bytes = serializer.serialize(message);

        // When: ETX를 잘못된 값으로 변경
        bytes[bytes.length - 1] = 0x04;  // 0x03이 아닌 0x04로 변경

        // Then: 역직렬화 시 ETX 검증 실패
        assertThatThrownBy(() -> deserializer.deserialize(bytes, FramedMessage.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid ETX")
            .hasMessageContaining("expected 0x03")
            .hasMessageContaining("got 0x04");
    }

    @Test
    void testMessageTooShortForSTX() {
        // Given: 빈 바이트 배열 (STX가 없음)
        byte[] bytes = new byte[0];

        // When/Then: 역직렬화 시 예외 발생
        assertThatThrownBy(() -> deserializer.deserialize(bytes, FramedMessage.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message too short to contain STX");
    }

    @Test
    void testMessageBodyExtraction() {
        // Given: 프레이밍된 메시지
        FramedMessage message = new FramedMessage();
        message.setField1("Body extraction test 1");
        message.setField2("Body extraction test 2");

        // When: 직렬화
        byte[] bytes = serializer.serialize(message);

        // Then: STX와 ETX를 제외한 메시지 본문은 100바이트
        // bytes[0] = STX, bytes[1..100] = 메시지, bytes[101] = ETX
        assertThat(bytes).hasSize(102);

        // 메시지 본문만 추출 (STX, ETX 제외)
        byte[] messageBody = new byte[100];
        System.arraycopy(bytes, 1, messageBody, 0, 100);
        assertThat(messageBody).hasSize(100);
    }
}
