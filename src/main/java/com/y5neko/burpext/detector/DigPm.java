package com.y5neko.burpext.detector;

import com.alibaba.fastjson2.JSONObject;
import com.y5neko.burpext.core.RawHttpSender;
import com.y5neko.burpext.utils.CharsetUtils;
import com.y5neko.burpext.utils.MiscUtils;

import java.io.IOException;
import java.net.InetAddress;

public class DigPm implements Detector{
    private String fullDomain;
    private String mainDomain;
    private String subDomain;
    private String token;
    private String result;

    public DigPm() throws IOException {
        getSubDomain();
    }

    public void getSubDomain() throws IOException {
        String request = "POST /get_sub_domain HTTP/1.1\n" +
                "Host: dig.pm\n" +
                "Content-type: application/x-www-form-urlencoded\n" +
                "\n" +
                "mainDomain=ipv6.1433.eu.org.";

        String response = CharsetUtils.bytesToString(RawHttpSender.getInstance().sendRawRequest(request));
        JSONObject responseObj = JSONObject.parseObject(response);
        fullDomain = responseObj.getString("fullDomain").substring(0, responseObj.getString("fullDomain").length() - 1);
        mainDomain = responseObj.getString("mainDomain").substring(0, responseObj.getString("mainDomain").length() - 1);
        subDomain = responseObj.getString("subDomain");
        token = responseObj.getString("token");
    }

    public String getResult() throws Exception {
        String request = "POST /get_results HTTP/1.1\n" +
                "Host: dig.pm\n" +
                "Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryJ7928Bk56C4daeQR\n" +
                "X-Recaptcha: just4test\n" +
                "\n" +
                "------WebKitFormBoundaryJ7928Bk56C4daeQR\n" +
                "Content-Disposition: form-data; name=\"mainDomain\"\n" +
                "\n" +
                mainDomain + ".\n" +
                "------WebKitFormBoundaryJ7928Bk56C4daeQR\n" +
                "Content-Disposition: form-data; name=\"token\"\n" +
                "\n" +
                token + "\n" +
                "------WebKitFormBoundaryJ7928Bk56C4daeQR\n" +
                "Content-Disposition: form-data; name=\"subDomain\"\n" +
                "\n" +
                subDomain + "\n" +
                "------WebKitFormBoundaryJ7928Bk56C4daeQR--\n";

        result = CharsetUtils.bytesToString(RawHttpSender.getInstance().sendRawRequest(request));
        return result;
    }

    public String getFullDomain() {
        return fullDomain;
    }

    public String getMainDomain() {
        return mainDomain;
    }

    public String getToken() {
        return token;
    }

    public static boolean checkDnslogStatus() throws Exception {
        try {
            DigPm detector = new DigPm();
            String testurl = "testy5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
            if (testurl.length() > 11) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 测试用
     * @param args 无参数
     * @throws IOException IO异常
     * @throws InterruptedException 线程异常
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        DigPm detector = new DigPm();

        String testurl = "testy5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
        InetAddress addr = InetAddress.getByName(testurl);
        System.out.println(addr.getHostAddress());

        Thread.sleep(3000);

        try {
            System.out.println(detector.getResult());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
