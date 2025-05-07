package com.y5neko.burpext.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtils {
    /**
     * 读取文件内容为字符串，统一换行符为 \n，使用 UTF-8 编码
     *
     * @param file 要读取的文件
     * @return 文件内容字符串
     * @throws IOException 读取异常
     */
    public static String readFileToString(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("文件不存在或不是一个普通文件: " + file.getAbsolutePath());
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n"); // 强制统一为 \n
            }
        }
        return sb.toString();
    }

    /**
     * 读取文件内容为字符串，统一换行符为 \n，使用 UTF-8 编码
     *
     * @param filePath 文件路径字符串
     * @return 文件内容字符串
     * @throws IOException 读取异常
     */
    public static String readFileToString(String filePath) throws IOException {
        return readFileToString(new File(filePath));
    }
}
