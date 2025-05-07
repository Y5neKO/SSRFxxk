package com.y5neko.burpext.utils;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class ResponseUtils {

    private final byte[] rawBytes;
    private final String decodedText;

    public ResponseUtils(Response response) throws Exception {
        ResponseBody body = response.body();
        if (body == null) {
            rawBytes = new byte[0];
            decodedText = "";
            return;
        }

        // 原始压缩内容（如 gzip/deflate）
        byte[] compressed = body.bytes();

        // 解压缩
        String encoding = response.header("Content-Encoding", "").toLowerCase();
        InputStream decodedStream = getDecodedStream(compressed, encoding);

        rawBytes = decodedStream.readAllBytes();
        Charset charset = extractCharsetFromContentType(response.header("Content-Type"));

        decodedText = new String(rawBytes, charset);
    }

    private static InputStream getDecodedStream(byte[] bytes, String encoding) throws Exception {
        switch (encoding) {
            case "gzip":
                return new GZIPInputStream(new ByteArrayInputStream(bytes));
            case "deflate":
                return new InflaterInputStream(new ByteArrayInputStream(bytes));
            default:
                return new ByteArrayInputStream(bytes); // no compression
        }
    }

    private static Charset extractCharsetFromContentType(String contentType) {
        if (contentType == null) return StandardCharsets.UTF_8;

        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim().toLowerCase();
            if (part.startsWith("charset=")) {
                try {
                    return Charset.forName(part.split("=", 2)[1].trim());
                } catch (Exception ignored) {}
            }
        }
        return StandardCharsets.UTF_8;
    }

    public byte[] getBytes() {
        return rawBytes;
    }

    public String getText() {
        return decodedText;
    }

    @Override
    public String toString() {
        return "[ResponseUtils] length=" + rawBytes.length + ", preview=" +
                decodedText.substring(0, Math.min(100, decodedText.length())).replaceAll("\n", "\\n");
    }
}
