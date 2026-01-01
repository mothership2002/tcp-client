package hyun.messageconnecter.annotation;

import hyun.messageconnecter.enums.OverflowPolicy;
import hyun.messageconnecter.enums.PaddingPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TCP 메시지 도메인 클래스 마커
 * 전체 메시지의 길이, 인코딩, 패딩 정책 등을 지정
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TcpMessage {

    /**
     * 전체 메시지의 바이트 길이
     */
    int totalLength();

    /**
     * 패딩 문자 (기본: 공백)
     * 개별 필드의 paddingChar가 지정되지 않은 경우 사용됨
     */
    char paddingChar() default ' ';

    /**
     * 문자 인코딩 (기본: UTF-8)
     * EUC-KR, MS949 등 레거시 시스템 인코딩 지원
     */
    String charset() default "UTF-8";

    /**
     * 패딩 정책 (기본: 가변 길이)
     */
    PaddingPolicy paddingPolicy() default PaddingPolicy.TRAILING_PAD;

    /**
     * 필드 길이 초과 시 처리 정책 (기본: 안전한 잘라내기)
     * TRUNCATE: 단순 잘라냄 (멀티바이트 문자 중간에서 잘릴 수 있음)
     * TRUNCATE_SAFE: 멀티바이트 안전하게 잘라냄 (권장)
     * ERROR: 예외 발생
     */
    OverflowPolicy overflowPolicy() default OverflowPolicy.TRUNCATE_SAFE;

    /**
     * STX (Start of Text) 바이트 (선택)
     * 기본값: 0x00 (STX 없음)
     * <p>
     * 레거시 시스템에서 메시지 시작을 표시하는 제어 문자
     * 일반적으로 0x02를 사용
     */
    byte stx() default 0x00;

    /**
     * ETX (End of Text) 바이트 (선택)
     * 기본값: 0x00 (ETX 없음)
     * <p>
     * 레거시 시스템에서 메시지 종료를 표시하는 제어 문자
     * 일반적으로 0x03을 사용
     */
    byte etx() default 0x00;
}
