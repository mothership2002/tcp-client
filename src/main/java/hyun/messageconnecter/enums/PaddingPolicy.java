package hyun.messageconnecter.enums;

/**
 * 패딩 정책
 */
public enum PaddingPolicy {
    /**
     * 고정 길이 (엄격)
     * 필드 길이의 합이 totalLength와 정확히 일치해야 함
     */
    FIXED_LENGTH,

    /**
     * 가변 길이 (유연)
     * 필드 길이의 합이 totalLength보다 작아도 됨
     * 부족한 부분은 끝에 패딩 추가
     */
    TRAILING_PAD
}
