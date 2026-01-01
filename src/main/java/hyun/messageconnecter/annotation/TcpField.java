package hyun.messageconnecter.annotation;

import hyun.messageconnecter.enums.Align;
import hyun.messageconnecter.enums.AutoCalculate;
import hyun.messageconnecter.enums.Transform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TCP 메시지 필드 메타데이터
 * 각 필드의 바이트 길이, 정렬, 변환 규칙 등을 지정
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TcpField {

    /**
     * 필드 순서 (1부터 시작)
     * 이 순서대로 직렬화됨
     */
    int order();

    /**
     * 필드의 바이트 길이
     */
    int length();

    /**
     * 정렬 방향 (기본: 좌측 정렬)
     */
    Align align() default Align.LEFT;

    /**
     * 패딩 문자 (기본: 공백)
     * align이 LEFT면 우측에, RIGHT면 좌측에 추가됨
     */
    char paddingChar() default ' ';

    /**
     * 값 변환 규칙 (기본: 변환 없음)
     */
    Transform transform() default Transform.NONE;

    /**
     * 날짜/숫자 포맷 (선택)
     * 날짜: "yyyyMMdd", "yyyyMMddHHmmss" 등
     * 숫자: "0.00" 등 (DecimalFormat 패턴)
     */
    String format() default "";

    /**
     * 필드별 문자 인코딩 (선택)
     * 기본값: "" (TcpMessage의 charset 사용)
     * 예: "EUC-KR", "UTF-8", "MS949"
     * 필드별로 다른 인코딩이 필요한 레거시 시스템 지원
     */
    String charset() default "";

    /**
     * 리스트/배열 최대 크기 (선택)
     * 기본값: 0 (리스트가 아님)
     * 0보다 크면 이 필드를 리스트로 간주하고, 각 아이템을 itemLength만큼 직렬화
     * 예: maxSize=300이면 최대 300개의 아이템
     */
    int maxSize() default 0;

    /**
     * 리스트/배열의 각 아이템 바이트 길이 (선택)
     * maxSize > 0일 때 필수
     * 각 아이템이 차지하는 고정 바이트 길이
     * 검증: length = maxSize × itemLength
     */
    int itemLength() default 0;

    /**
     * 자동 계산 타입 (선택)
     * 기본값: NONE (자동 계산 없음)
     * <p>
     * 레거시 시스템에서 자주 사용되는 자동 계산 필드:
     * - MESSAGE_LENGTH: 전체 메시지 길이 자동 설정
     * - CHECKSUM_CRC16: CRC16 체크섬 자동 계산
     * - CHECKSUM_CRC32: CRC32 체크섬 자동 계산
     * - CHECKSUM_XOR: XOR 체크섬 자동 계산
     */
    AutoCalculate autoCalculate() default AutoCalculate.NONE;
}
