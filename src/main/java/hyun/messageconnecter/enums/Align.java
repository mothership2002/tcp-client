package hyun.messageconnecter.enums;

/**
 * 필드 정렬 방향
 */
public enum Align {
    /**
     * 좌측 정렬 (우측에 패딩 추가)
     * 예: "TEST      " (10바이트)
     */
    LEFT,

    /**
     * 우측 정렬 (좌측에 패딩 추가)
     * 예: "      TEST" (10바이트)
     */
    RIGHT
}
