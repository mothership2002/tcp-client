package hyun.messageconnecter.integration;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.autocal.LegacyMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 자동 계산 필드 통합 테스트
 * - MESSAGE_LENGTH 자동 계산
 * - CHECKSUM_CRC16 자동 계산
 * - 체크섬 검증
 */
class AutoCalculateIntegrationTest {

    private TcpMessageSerializer serializer = new TcpMessageSerializer();

    private TcpMessageDeserializer deserializer = new TcpMessageDeserializer();

    @Test
    void testMessageLengthAutoCalculate() {
        // Given: MESSAGE_LENGTH 자동 계산 필드가 있는 메시지
        LegacyMessage message = new LegacyMessage();
        message.setTransactionCode("DEPOSIT");
        message.setData("Test data for auto calculation");
        // messageLength 필드는 설정하지 않음 (자동 계산됨)

        // When: 직렬화
        byte[] bytes = serializer.serialize(message);

        // Then: 바이트 배열이 정확히 100바이트
        assertThat(bytes).hasSize(100);

        // Then: 역직렬화 시 messageLength가 자동으로 채워짐
        LegacyMessage deserialized = deserializer.deserialize(bytes, LegacyMessage.class);
        assertThat(deserialized.getMessageLength()).isEqualTo("0000000100");  // 10자리, 우측정렬, 0 패딩
    }

    @Test
    void testChecksumCRC16AutoCalculate() {
        // Given: CRC16 체크섬 자동 계산 필드가 있는 메시지
        LegacyMessage message = new LegacyMessage();
        message.setTransactionCode("WITHDRAW");
        message.setData("Test checksum calculation");
        // checksum 필드는 설정하지 않음 (자동 계산됨)

        // When: 직렬화
        byte[] bytes = serializer.serialize(message);

        // Then: 체크섬이 자동으로 계산되어 삽입됨
        LegacyMessage deserialized = deserializer.deserialize(bytes, LegacyMessage.class);
        assertThat(deserialized.getChecksum()).isNotEmpty();
        assertThat(deserialized.getChecksum()).hasSize(4);  // 16진수 4자리
        assertThat(deserialized.getChecksum()).matches("[0-9A-F]{4}");  // 16진수 형식
    }

    @Test
    void testChecksumRoundtrip() {
        // Given: 원본 메시지
        LegacyMessage original = new LegacyMessage();
        original.setTransactionCode("TRANSFER");
        original.setData("Roundtrip test for checksum validation");

        // When: 직렬화 후 역직렬화
        byte[] bytes = serializer.serialize(original);
        LegacyMessage deserialized = deserializer.deserialize(bytes, LegacyMessage.class);

        // Then: 역직렬화가 성공하고 데이터가 일치
        assertThat(deserialized.getTransactionCode()).isEqualTo("TRANSFER");
        assertThat(deserialized.getData()).startsWith("Roundtrip test");
        assertThat(deserialized.getChecksum()).isNotEmpty();
    }

    @Test
    void testChecksumValidationFailure() {
        // Given: 올바른 메시지
        LegacyMessage message = new LegacyMessage();
        message.setTransactionCode("QUERY");
        message.setData("Test checksum validation failure");
        byte[] bytes = serializer.serialize(message);

        // When: 체크섬 필드를 임의로 변경
        int checksumPosition = 96;  // 체크섬 필드 위치 (order 4, offset = 10 + 10 + 76 = 96)
        bytes[checksumPosition] = 'X';  // 첫 번째 체크섬 바이트 변경

        // Then: 역직렬화 시 체크섬 검증 실패
        assertThatThrownBy(() -> deserializer.deserialize(bytes, LegacyMessage.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Checksum validation failed");
    }

    @Test
    void testChecksumValidationWithDataCorruption() {
        // Given: 올바른 메시지
        LegacyMessage message = new LegacyMessage();
        message.setTransactionCode("UPDATE");
        message.setData("Test data corruption detection");
        byte[] bytes = serializer.serialize(message);

        // When: 데이터 필드를 임의로 변경 (체크섬은 그대로)
        int dataPosition = 20;  // 데이터 필드 중간
        bytes[dataPosition] = 'Z';  // 데이터 손상

        // Then: 역직렬화 시 체크섬 불일치로 검증 실패
        assertThatThrownBy(() -> deserializer.deserialize(bytes, LegacyMessage.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Checksum validation failed");
    }

    @Test
    void testBothAutoCalculateFields() {
        // Given: MESSAGE_LENGTH와 CHECKSUM 모두 자동 계산되는 메시지
        LegacyMessage message = new LegacyMessage();
        message.setTransactionCode("CREATE");
        message.setData("Both auto-calculate fields test");

        // When: 직렬화 후 역직렬화
        byte[] bytes = serializer.serialize(message);
        LegacyMessage deserialized = deserializer.deserialize(bytes, LegacyMessage.class);

        // Then: 두 필드 모두 자동으로 채워짐
        assertThat(deserialized.getMessageLength()).isEqualTo("0000000100");
        assertThat(deserialized.getChecksum()).isNotEmpty().hasSize(4);
        assertThat(deserialized.getTransactionCode()).isEqualTo("CREATE");
        assertThat(deserialized.getData()).startsWith("Both auto-calculate");
    }
}
