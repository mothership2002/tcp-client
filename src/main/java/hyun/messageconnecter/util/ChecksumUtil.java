package hyun.messageconnecter.util;

import java.util.zip.CRC32;

/**
 * 체크섬 계산 유틸리티
 * 레거시 시스템에서 사용되는 CRC16, CRC32, XOR 체크섬 지원
 */
public class ChecksumUtil {

    /**
     * CRC16 체크섬 계산 (Polynomial: 0x8005, IBM/ANSI 표준)
     * 레거시 시스템에서 가장 널리 사용되는 CRC16 알고리즘
     *
     * @param data 체크섬을 계산할 바이트 배열
     * @return CRC16 체크섬 값 (0x0000 ~ 0xFFFF)
     */
    public static int calculateCRC16(byte[] data) {
        int crc = 0xFFFF; // 초기값
        int polynomial = 0x8005; // IBM/ANSI 표준 다항식

        for (byte b : data) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ polynomial;
                } else {
                    crc <<= 1;
                }
            }
        }

        return crc & 0xFFFF;
    }

    /**
     * CRC32 체크섬 계산 (Java 표준 라이브러리 사용)
     * IEEE 802.3 표준 (Polynomial: 0x04C11DB7)
     *
     * @param data 체크섬을 계산할 바이트 배열
     * @return CRC32 체크섬 값
     */
    public static long calculateCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    /**
     * XOR 체크섬 계산
     * 모든 바이트를 XOR 연산하여 간단한 체크섬 생성
     * 레거시 시스템에서 가볍게 사용되는 방식
     *
     * @param data 체크섬을 계산할 바이트 배열
     * @return XOR 체크섬 값 (0x00 ~ 0xFF)
     */
    public static int calculateXOR(byte[] data) {
        int xor = 0;
        for (byte b : data) {
            xor ^= (b & 0xFF);
        }
        return xor & 0xFF;
    }
}
