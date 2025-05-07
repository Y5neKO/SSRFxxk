package com.y5neko.burpext.utils;

import burp.api.montoya.http.handler.HttpRequestToBeSent;

public class JudgeUtils {
    /**
     * 判断请求是否在白名单中
     * @param request 待判断的请求
     * @param whiteListRules 白名单规则数组
     * @return 是否在白名单中
     */
    public static boolean isWhiteList(HttpRequestToBeSent request, String[] whiteListRules) {
        String fullUrl = request.url();
        String host = request.httpService().host();
        String port = String.valueOf(request.httpService().port());

        if (whiteListRules == null || whiteListRules.length == 0) return true;

        for (String rule : whiteListRules) {
            // URL规则匹配（带协议）
            if (rule.startsWith("http://") || rule.startsWith("https://")) {
                if (RuleMatcherUtils.matchUrlRule(fullUrl, rule)) return true;
            }
            // Host:Port组合规则
            else if (rule.contains(":")) {
                if (RuleMatcherUtils.matchHostPortRule(host, port, rule)) return true;
            }
            // 纯Host规则
            else {
                if (RuleMatcherUtils.matchHostWithWildcard(host, rule)) return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为 POST 请求
     * @param httpRequestToBeSent 待判断的请求
     * @return 是否为 POST 请求
     */
    public static boolean isPost(HttpRequestToBeSent httpRequestToBeSent) {
        return httpRequestToBeSent.method().equalsIgnoreCase("POST");
    }

    /**
     * 判断是否为 JSON 请求
     * @param httpRequestToBeSent 待判断的请求
     * @return 是否为 JSON 请求
     */
    public static boolean isJsonRequest(HttpRequestToBeSent httpRequestToBeSent) {
        if (isPost(httpRequestToBeSent)) {
            return httpRequestToBeSent.headers().stream().anyMatch(header -> header.name().equalsIgnoreCase("Content-Type") && header.value().contains("application/json"));
        } else {
            return false;
        }
    }
}
