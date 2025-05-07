package com.y5neko.burpext.utils;

import org.mozilla.universalchardet.UniversalDetector;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CharsetUtils {
    /**
     * 自动识别字节数组编码并转换为字符串
     * @param bytes 原始字节数组
     * @param defaultCharset 猜测失败时的默认编码
     * @return 转换后的字符串
     */
    public static String bytesToString(byte[] bytes, Charset defaultCharset) {
        // 1. 优先通过BOM检测编码
        Charset bomCharset = detectCharsetByBOM(bytes);
        if (bomCharset != null) {
            return new String(bytes, bomCharset).replaceFirst("\uFEFF", "");
        }

        // 2. 使用juniversalchardet检测编码
        String detectedEncoding = detectCharsetByUniversalDetector(bytes);
        if (detectedEncoding != null) {
            try {
                return new String(bytes, detectedEncoding);
            } catch (Exception ignored) {
                // 如果检测失败，继续尝试备选方案
            }
        }

        // 3. 尝试常见编码
        Charset[] candidates = {
                StandardCharsets.UTF_8,
                Charset.forName("GBK"),
                StandardCharsets.ISO_8859_1,
                StandardCharsets.UTF_16
        };

        for (Charset charset : candidates) {
            try {
                return new String(bytes, charset);
            } catch (Exception ignored) {}
        }

        // 4. 最终回退到默认编码
        return new String(bytes, defaultCharset);
    }

    /**
     * 通过BOM检测编码
     */
    private static Charset detectCharsetByBOM(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte)0xEF
                && bytes[1] == (byte)0xBB
                && bytes[2] == (byte)0xBF) {
            return StandardCharsets.UTF_8;
        } else if (bytes.length >= 2
                && bytes[0] == (byte)0xFE
                && bytes[1] == (byte)0xFF) {
            return StandardCharsets.UTF_16BE;
        } else if (bytes.length >= 2
                && bytes[0] == (byte)0xFF
                && bytes[1] == (byte)0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        return null;
    }

    /**
     * 使用Mozilla的编码检测库
     */
    private static String detectCharsetByUniversalDetector(byte[] bytes) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();
        return encoding;
    }

    // 默认使用UTF-8的快捷方法
    public static String bytesToString(byte[] bytes) {
        return bytesToString(bytes, StandardCharsets.UTF_8);
    }
}
