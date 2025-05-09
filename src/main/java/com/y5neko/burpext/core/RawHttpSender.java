package com.y5neko.burpext.core;

import com.y5neko.burpext.utils.CharsetUtils;
import okhttp3.*;
import org.brotli.dec.BrotliInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class RawHttpSender {
    // 单例 OkHttpClient（线程安全）
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    // 单例实例
    private static final RawHttpSender INSTANCE = new RawHttpSender();

    // 私有构造函数
    private RawHttpSender() {
    }

    // 获取单例
    public static RawHttpSender getInstance() {
        return INSTANCE;
    }

    /**
     * 主方法：发送原始HTTP请求包(默认仅返回响应体)
     * @param rawRequest 原始HTTP请求包
     * @return 响应字节流
     * @throws IOException IO异常
     */
    public byte[] sendRawRequest(String rawRequest) throws IOException {
        return sendRawRequest(rawRequest, false);
    }

    /**
     * 主方法：发送原始HTTP请求包
     * @param rawRequest 原始HTTP请求包
     * @param isWholeResponse 是否返回完整响应
     * @return 响应字节流
     * @throws IOException IO异常
     */
    public byte[] sendRawRequest(String rawRequest, boolean isWholeResponse) throws IOException {
        // 拆分请求
        String[] lines = rawRequest.split("\r?\n");

        String requestLine = lines[0];
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            throw new IllegalArgumentException("请求行格式不正确");
        }

        String method = parts[0];
        String path = parts[1];

        // Header 解析
        Map<String, String> headers = new LinkedHashMap<>();
        int i = 1;
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                i++; // 跳过空行
                break;
            }
            int index = line.indexOf(":");
            if (index > 0) {
                String name = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                headers.put(name, value);
            }
        }

        // 解析 body
        StringBuilder bodyBuilder = new StringBuilder();
        for (; i < lines.length; i++) {
            bodyBuilder.append(lines[i]);
            if (i != lines.length - 1) {
                bodyBuilder.append("\n");
            }
        }
        String body = bodyBuilder.toString();

        // 构造 URL
        String host = headers.get("Host");
        if (host == null) throw new IllegalArgumentException("缺少 Host 头");

        String scheme = host.contains("443") ? "https" : "http";
        if (headers.containsKey("X-Forwarded-Proto")) {
            scheme = headers.get("X-Forwarded-Proto");
        }

        URL url = new URL(scheme + "://" + host + path);

        // 构建 Request
        Request.Builder builder = new Request.Builder().url(url);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("Content-Length")) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        // 分情况构造请求体
        RequestBody requestBody = null;
        if (!method.equalsIgnoreCase("GET") && !body.isEmpty()) {
            String contentType = headers.getOrDefault("Content-Type", "application/x-www-form-urlencoded");
            requestBody = RequestBody.create(body, MediaType.parse(contentType));
        } else if (!method.equalsIgnoreCase("GET") && body.isEmpty()) {
            String contentType = headers.getOrDefault("Content-Type", "application/x-www-form-urlencoded");
            if (contentType.equalsIgnoreCase("application/x-www-form-urlencoded")) {
                requestBody = RequestBody.create("", MediaType.parse(contentType));
            } else if (contentType.equalsIgnoreCase("application/json")) {
                requestBody = RequestBody.create("{}", MediaType.parse(contentType));
            }
        }

        Request request = builder.method(method, requestBody).build();

        byte[] bytes = new byte[0];


        //old
//        try (Response response = CLIENT.newCall(request).execute()) {
//            if (response.body() == null) return "SSRFxxk: no response.".getBytes();
//
//            // 获取响应字节流
//            if (isWholeResponse) {
//                bytes = response.toString().getBytes();
//            } else {
//                bytes = response.body().bytes();
//            }
//
//            String encoding = response.header("Content-Encoding");
//            InputStream decodedStream = getInputStream(bytes, encoding);
//
//            bytes = decodedStream.readAllBytes();
//
//            System.out.println(Arrays.toString(bytes));
//            System.out.println("================================");
//            System.out.println(response.body());
//
//            return bytes;
//        } catch (Exception e) {
//            return "SSRFxxk: request faild.".getBytes();
//        }

        // new
        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.body() == null) return "SSRFxxk: no response.".getBytes();

            byte[] rawBody = response.body().bytes(); // 注意：只能读取一次
            String encoding = response.header("Content-Encoding");
            InputStream decodedStream = getInputStream(rawBody, encoding);
            byte[] decodedBody = decodedStream.readAllBytes();

            if (isWholeResponse) {

                // 状态行
                String setCookie = "";
                for (String cookie : response.headers("Set-Cookie")) {
                    setCookie = cookie;
                }

                String headerBuilder = "HTTP/1.1 " +
                        response.code() +
                        " " +
                        response.message() +
                        "\r\n" +
                        response.headers().toString().replaceAll("██", setCookie).replace("\n", "\r\n") +
                        "\r\n"; // 空行分隔头部和 body

                byte[] headerBytes = headerBuilder.getBytes();

                // 合并 header + body
                byte[] fullResponse = new byte[headerBytes.length + decodedBody.length];
                System.arraycopy(headerBytes, 0, fullResponse, 0, headerBytes.length);
                System.arraycopy(decodedBody, 0, fullResponse, headerBytes.length, decodedBody.length);
                return fullResponse;

            } else {
                return decodedBody;
            }
        } catch (Exception e) {
            return "SSRFxxk: request faild.".getBytes();
        }
    }

    /**
     * 根据 Content-Encoding 解码字节流
     * @param bytes 原始字节流
     * @param encoding Content-Encoding
     * @return 解码后的字节流
     * @throws IOException IO异常
     */
    @NotNull
    private static InputStream getInputStream(byte[] bytes, String encoding) throws IOException {
        InputStream rawStream = new ByteArrayInputStream(bytes);
        InputStream decodedStream = rawStream;

        if ("gzip".equalsIgnoreCase(encoding)) {
            decodedStream = new GZIPInputStream(rawStream);
        } else if ("br".equalsIgnoreCase(encoding)) {
            decodedStream = new BrotliInputStream(rawStream);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
            decodedStream = new InflaterInputStream(rawStream);
        }
        return decodedStream;
    }

    public static void main(String[] args) throws IOException {
//        String request = "POST / HTTP/1.1\n" +
//                "Host: www.4399.com\n" +
//                "sec-ch-ua-mobile: ?0\n" +
//                "Sec-Fetch-Mode: navigate\n" +
//                "Accept-Encoding: gzip, deflate, br, zstd\n" +
//                "Sec-Fetch-Site: http://Sec-Fetch-Sitey5nekogbd0e0l2xa.c7835d9815.ipv6.1433.eu.org\n" +
//                "Sec-Fetch-Site22: http://Sec-Fetch-Site22y5nekozwmfefbacg.c7835d9815.ipv6.1433.eu.org\n" +
//                "Sec-Fetch-Dest: document\n" +
//                "Upgrade-Insecure-Requests: 1\n" +
//                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\n" +
//                "Accept-Language: zh-CN,zh;q=0.9\n" +
//                "sec-ch-ua-platform: \"macOS\"\n" +
//                "Sec-Fetch-User: ?1\n" +
//                "sec-ch-ua: \"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"\n" +
//                "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36\n" +
//                "Content-Length: 68\n" +
//                "Accept: */*\n" +
//                "Accept-Language: en-US;q=0.9,en;q=0.8\n" +
//                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.60 Safari/537.36\n" +
//                "Connection: close\n" +
//                "Cache-Control: max-age=0\n" +
//                "\n" +
//                "url=http://urly5nekoxpvyqkg0r0.c7835d9815.ipv6.1433.eu.org&test=1235";


        String request = "POST /aichat/api/conversation?inputMethod=chat_search&k_signature=dtJWWYVkAq2RcZ8ljZjiflbxqjjVJiYggaXvYdwoy9hpMBpE%2FhtVE6hTUClpXyv0atapGonnIegh2mvU%2B6lLZzNGs61DNvSWmTtHacbi3qY%3D&k_timestamp=1746600203&word=%E4%BD%A0%E5%95%8A%E5%A5%BD%E5%95%8A HTTP/1.1\n" +
                "Host: chat-ws.baidu.com\n" +
                "Cookie: BIDUPSID=481C2E35C86BB9B9EA734B7A11321B49; PSTM=1744351784; BAIDUID=481C2E35C86BB9B9BBCF44081B912DCA:FG=1; H_PS_PSSID=60277_62327_62862_62969_63041_63045_63141_63126_63179; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; H_WISE_SIDS=60277_62327_62862_62969_63041_63045_63141_63126_63191\n" +
                "Content-Length: 341\n" +
                "Sec-Ch-Ua: \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"\n" +
                "Accept: text/event-stream\n" +
                "Content-Type: application/json\n" +
                "Sec-Ch-Ua-Mobile: ?0\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.60 Safari/537.36\n" +
                "Sec-Ch-Ua-Platform: \"macOS\"\n" +
                "Origin: https://www.baidu.com\n" +
                "Sec-Fetch-Site: same-site\n" +
                "Sec-Fetch-Mode: cors\n" +
                "Sec-Fetch-Dest: empty\n" +
                "Referer: https://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=1&rsv_idx=1&tn=baidu&wd=%E4%BD%A0%E5%95%8A%E5%A5%BD%E5%95%8A&fenlei=256&oq=asdasdasdadasdadsasasd&rsv_pq=c9436bcd0006009d&rsv_t=e3beZC6s%2FFwk4k67RcCprUq%2FqPtv4IqdAsp%2FtJQ9iH6FQ2nUrwFhj3YZ29M&rqlang=cn&rsv_dl=tb&rsv_enter=0&rsv_btype=t&rsv_sug3=70&rsv_sug1=4&rsv_sug7=101&rsv_sug2=0&inputT=4157&rsv_sug4=4387\n" +
                "Accept-Encoding: gzip, deflate, br\n" +
                "Accept-Language: zh-CN,zh;q=0.9\n" +
                "Priority: u=1, i\n" +
                "Connection: keep-alive\n" +
                "Accept-Encoding: gzip, deflate, br\n" +
                "Accept: */*\n" +
                "Accept-Language: en-US;q=0.9,en;q=0.8\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.60 Safari/537.36\n" +
                "Connection: close\n" +
                "Cache-Control: max-age=0\n" +
                "Accept-Encoding: gzip, deflate, br\n" +
                "Accept: */*\n" +
                "Accept-Language: en-US;q=0.9,en;q=0.8\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.60 Safari/537.36\n" +
                "Connection: close\n" +
                "Cache-Control: max-age=0\n" +
                "\n" +
                "{\"message\":{\"inputMethod\":\"chat_search\",\"isRebuild\":false,\"content\":{\"query\":\"你啊好啊\",\"qtype\":0},\"searchInfo\":{\"srcid\":\"279\",\"order\":\"1\",\"tplname\":\"wenda_generate\",\"strategy\":\"\",\"ori_lid\":\"\",\"re_rank\":\"\",\"lid\":\"15060897762294323258\",\"qid\":\"15060897762294323258\",\"viscate_qid\":\"\"},\"source\":\"ala_279\",\"from\":\"apage_dqa\",\"dqaRewrite\":{}}}";

        String response = CharsetUtils.bytesToString(RawHttpSender.getInstance().sendRawRequest(request, true));
        System.out.println(response);
    }
}
