package hyun.messageconnecter.enums;

/**
 * 필드 길이 초과 시 처리 정책
 */
public enum OverflowPolicy {
    /**
     * 길이 초과 시 단순 잘라냄
     * 멀티바이트 문자가 중간에 잘릴 수 있음
     */
    TRUNCATE,

    /**
     * 멀티바이트 문자를 안전하게 잘라냄
     * 불완전한 바이트 시퀀스를 방지 (권장)
     */
    TRUNCATE_SAFE,

    /**
     * 길이 초과 시 예외 발생
     * 엄격한 검증이 필요한 경우 사용
     */
    ERROR
}
