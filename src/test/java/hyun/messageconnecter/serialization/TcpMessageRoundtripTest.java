package hyun.messageconnecter.serialization;

import hyun.messageconnecter.TcpMessageDeserializer;
import hyun.messageconnecter.TcpMessageSerializer;
import hyun.messageconnecter.fixture.tcp.BankTransactionRequest;
import hyun.messageconnecter.fixture.tcp.BankTransactionResponse;
import hyun.messageconnecter.fixture.tcp.SimpleRequest;
import hyun.messageconnecter.fixture.tcp.SimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TCP 메시지 라운드트립 테스트
 * 직렬화 → 역직렬화 → 원본과 비교
 */
class TcpMessageRoundtripTest {

    private TcpMessageSerializer serializer;
    private TcpMessageDeserializer deserializer;

    @BeforeEach
    void setUp() {
        serializer = new TcpMessageSerializer();
        deserializer = new TcpMessageDeserializer();
    }

    @Test
    @DisplayName("SimpleRequest 라운드트립 테스트")
    void testSimpleRequestRoundtrip() {
        // Given
        SimpleRequest original = new SimpleRequest();
        original.setMessageCode("TEST");
        original.setData("HELLO");
        original.setNumber(123L);

        // When: 직렬화 → 역직렬화
        byte[] serialized = serializer.serialize(original);
        SimpleRequest deserialized = deserializer.deserialize(serialized, SimpleRequest.class);

        // Then: 원본과 동일
        assertThat(deserialized.getMessageCode()).isEqualTo(original.getMessageCode());
        assertThat(deserialized.getData()).isEqualTo(original.getData());
        assertThat(deserialized.getNumber()).isEqualTo(original.getNumber());
    }

    @Test
    @DisplayName("BankTransactionRequest 라운드트립 테스트")
    void testBankTransactionRequestRoundtrip() {
        // Given
        BankTransactionRequest original = new BankTransactionRequest();
        original.setMessageCode("TRX001");
        original.setAccountNumber("1234567890");
        original.setCustomerName("testUser"); // UPPER 변환됨
        original.setTransactionDate(LocalDate.of(2025, 1, 25));
        original.setAmount(100000L);
        original.setTransactionType("D");

        // When: 직렬화 → 역직렬화
        byte[] serialized = serializer.serialize(original);
        BankTransactionRequest deserialized = deserializer.deserialize(serialized, BankTransactionRequest.class);

        // Then: 원본과 동일 (Transform으로 인해 customerName은 대문자로 변환됨)
        assertThat(deserialized.getMessageCode()).isEqualTo(original.getMessageCode());
        assertThat(deserialized.getAccountNumber()).isEqualTo(original.getAccountNumber());
        assertThat(deserialized.getCustomerName()).isEqualToIgnoringCase(original.getCustomerName()); // UPPER 변환
        assertThat(deserialized.getTransactionDate()).isEqualTo(original.getTransactionDate());
        assertThat(deserialized.getAmount()).isEqualTo(original.getAmount());
        assertThat(deserialized.getTransactionType()).isEqualTo(original.getTransactionType());
    }

    @Test
    @DisplayName("SimpleResponse 라운드트립 테스트")
    void testSimpleResponseRoundtrip() {
        // Given
        SimpleResponse original = new SimpleResponse();
        original.setResultCode("0000");
        original.setResultMessage("Success");

        // When
        byte[] serialized = serializer.serialize(original);
        SimpleResponse deserialized = deserializer.deserialize(serialized, SimpleResponse.class);

        // Then
        assertThat(deserialized.getResultCode()).isEqualTo(original.getResultCode());
        assertThat(deserialized.getResultMessage()).isEqualTo(original.getResultMessage());
    }

    @Test
    @DisplayName("BankTransactionResponse 라운드트립 테스트")
    void testBankTransactionResponseRoundtrip() {
        // Given
        BankTransactionResponse original = new BankTransactionResponse();
        original.setResultCode("0000");
        original.setResultMessage("Transaction completed successfully");
        original.setTransactionId("TXN20250125001");
        original.setBalanceAfter(1234567L);

        // When
        byte[] serialized = serializer.serialize(original);
        BankTransactionResponse deserialized = deserializer.deserialize(serialized, BankTransactionResponse.class);

        // Then
        assertThat(deserialized.getResultCode()).isEqualTo(original.getResultCode());
        assertThat(deserialized.getResultMessage()).isEqualTo(original.getResultMessage());
        assertThat(deserialized.getTransactionId()).isEqualTo(original.getTransactionId());
        assertThat(deserialized.getBalanceAfter()).isEqualTo(original.getBalanceAfter());
    }

    @Test
    @DisplayName("null 값 라운드트립 테스트")
    void testNullValueRoundtrip() {
        // Given
        SimpleRequest original = new SimpleRequest();
        original.setMessageCode("TEST");
        // data와 number는 null

        // When
        byte[] serialized = serializer.serialize(original);
        SimpleRequest deserialized = deserializer.deserialize(serialized, SimpleRequest.class);

        // Then
        assertThat(deserialized.getMessageCode()).isEqualTo(original.getMessageCode());
        assertThat(deserialized.getData()).isNullOrEmpty();
        assertThat(deserialized.getNumber()).isEqualTo(0L);
    }

    @Test
    @DisplayName("제로값 라운드트립 테스트")
    void testZeroValueRoundtrip() {
        // Given
        SimpleRequest original = new SimpleRequest();
        original.setMessageCode("TEST");
        original.setData("DATA");
        original.setNumber(0L);

        // When
        byte[] serialized = serializer.serialize(original);
        SimpleRequest deserialized = deserializer.deserialize(serialized, SimpleRequest.class);

        // Then
        assertThat(deserialized.getNumber()).isEqualTo(0L);
    }

    @Test
    @DisplayName("날짜 라운드트립 테스트")
    void testDateRoundtrip() {
        // Given
        BankTransactionRequest original = new BankTransactionRequest();
        original.setMessageCode("TEST");
        original.setAccountNumber("1234567890");
        original.setCustomerName("test");
        original.setTransactionDate(LocalDate.of(2025, 12, 31));
        original.setAmount(0L);
        original.setTransactionType("D");

        // When
        byte[] serialized = serializer.serialize(original);
        BankTransactionRequest deserialized = deserializer.deserialize(serialized, BankTransactionRequest.class);

        // Then
        assertThat(deserialized.getTransactionDate()).isEqualTo(original.getTransactionDate());
    }

    @Test
    @DisplayName("큰 숫자 라운드트립 테스트")
    void testLargeNumberRoundtrip() {
        // Given
        BankTransactionRequest original = new BankTransactionRequest();
        original.setMessageCode("TEST");
        original.setAccountNumber("1234567890");
        original.setCustomerName("test");
        original.setTransactionDate(LocalDate.now());
        original.setAmount(999999999999999L); // 15자리 최대값
        original.setTransactionType("D");

        // When
        byte[] serialized = serializer.serialize(original);
        BankTransactionRequest deserialized = deserializer.deserialize(serialized, BankTransactionRequest.class);

        // Then
        assertThat(deserialized.getAmount()).isEqualTo(original.getAmount());
    }
}