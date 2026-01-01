package hyun.messageconnecter.enums;

/**
 * 필드 자동 계산 타입
 * 레거시 시스템에서 자주 사용되는 자동 계산 필드를 지원합니다.
 */
public enum AutoCalculate {
    /**
     * 자동 계산 없음 (기본값)
     */
    NONE,

    /**
     * 전체 메시지 길이 자동 계산
     * @TcpMessage 의 totalLength 값을 필드에 설정
     * 주로 Integer 또는 String 타입 필드에 사용
     */
    MESSAGE_LENGTH,

    /**
     * CRC16 체크섬 자동 계산
     * 메시지 전체에 대한 CRC16 체크섬을 계산하여 설정
     * 주로 String 또는 Integer 타입 필드에 사용
     */
    CHECKSUM_CRC16,

    /**
     * CRC32 체크섬 자동 계산
     * 메시지 전체에 대한 CRC32 체크섬을 계산하여 설정
     * 주로 String 또는 Long 타입 필드에 사용
     */
    CHECKSUM_CRC32,

    /**
     * XOR 체크섬 자동 계산
     * 메시지 전체에 대한 XOR 체크섬을 계산하여 설정
     * 주로 String 또는 Integer 타입 필드에 사용
     */
    CHECKSUM_XOR
}
