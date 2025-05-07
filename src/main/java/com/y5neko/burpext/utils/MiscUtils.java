package com.y5neko.burpext.utils;

import java.util.Base64;
import java.util.Random;

public class MiscUtils {
    /**
     * 获取随机字符串
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String getRamdomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(str.charAt(random.nextInt(str.length())));
        }
        return sb.toString();
    }

    /**
     * 字节码转base64
     * @param bytes 字节码
     * @return base64字符串
     */
    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * base64字符串转字节码
     * @param base64 base64字符串
     * @return 字节码
     */
    public static byte[] base64StringToBytes(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * base64字符串转字符串（自动识别编码）
     * @param base64 base64字符串
     * @return 字符串
     */
    public static String base64StringToStringAutoDetect(String base64) {
        byte[] bytes = base64StringToBytes(base64);
        return CharsetUtils.bytesToString(bytes);
    }
}
