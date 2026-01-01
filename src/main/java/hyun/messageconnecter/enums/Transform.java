package hyun.messageconnecter.enums;

/**
 * 필드 값 변환 규칙
 */
public enum Transform {
    /**
     * 변환 없음 (기본값)
     */
    NONE,

    /**
     * camelCase → snake_case
     * 예: "userName" → "user_name"
     */
    CAMEL_TO_SNAKE,

    /**
     * camelCase → kebab-case
     * 예: "userName" → "user-name"
     */
    CAMEL_TO_KEBAB,

    /**
     * 대문자 변환
     * 예: "test" → "TEST"
     */
    UPPER,

    /**
     * 소문자 변환
     * 예: "TEST" → "test"
     */
    LOWER
}
