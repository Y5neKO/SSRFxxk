package com.y5neko.burpext.mode;

import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.y5neko.burpext.core.RawHttpSender;
import com.y5neko.burpext.dao.LogDAO;
import com.y5neko.burpext.utils.*;
import com.y5neko.burpext.detector.Detector;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.y5neko.burpext.core.Config.configFilePath;

/**
 * 替换所有的参数为Payload
 */
public class BruteDetect {
    Detector detector;

    boolean hasVulnerability = false;

    List<String> vulParameters = new ArrayList<>();

    public BruteDetect(HttpRequestToBeSent httpRequestToBeSent, Detector detector, boolean isKeywordsDetect, Logging logging) {
        try {
            this.detector = detector;

            String configJson = FileUtils.readFileToString(configFilePath);
            JSONObject config = JSONObject.parseObject(configJson);
            // 获取请求头关键字和规则
            JSONArray headersRules = config.getJSONArray("headers");
            // 获取响应关键字和规则
            JSONArray responseRules = config.getJSONArray("response");

            // 定义一个对象接收修改后的请求
            HttpRequest modifiedRequest = httpRequestToBeSent.withDefaultHeaders();

            // ============================ 先处理请求头 =======================
            List<HttpHeader> headers = httpRequestToBeSent.headers();
            for (HttpHeader header : headers) {
                for (int i = 0; i < headersRules.size(); i++) {
                    JSONObject rule = headersRules.getJSONObject(i);
                    String ruleString = rule.getString("rule");
                    String keywordString = rule.getString("keyword");
                    // 判断匹配规则
                    switch (ruleString) {
                        case "包含":
                            if (header.name().contains(keywordString)) {
                                String payload = "http://" + header.name() + "y5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
                                modifiedRequest = modifiedRequest.withHeader(header.name(), payload);
                            }
                            break;
                        case "完全匹配":
                            if (header.name().equals(keywordString)) {
                                String payload = "http://" + header.name() + "y5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
                                modifiedRequest = modifiedRequest.withHeader(header.name(), payload);
                            }
                            break;
                        case "正则(包含)":
                            Pattern pattern = Pattern.compile(keywordString);
                            Matcher matcher = pattern.matcher(header.name());
                            if (matcher.find()) {
                                String payload = "http://" + header.name() + "y5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
                                modifiedRequest = modifiedRequest.withHeader(header.name(), payload);
                            }
                            break;
                        case "正则(完全匹配)":
                            if (header.name().matches(keywordString)) {
                                String payload = "http://" + header.name() + "y5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
                                modifiedRequest = modifiedRequest.withHeader(header.name(), payload);
                            }
                            break;
                    }
                }
            }
            // ============================ 暴力处理其它 ========================
            // GET参数
            List<ParsedHttpParameter> httpParametersUrl = modifiedRequest.parameters(HttpParameterType.URL);
            for (ParsedHttpParameter httpParameterUrl : httpParametersUrl) {
                // 创建一个检测的payload，用于匹配成功后的参数，存储
                String payload = "http://" + httpParameterUrl.name() + "y5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
                modifiedRequest = modifiedRequest.withParameter(HttpParameter.urlParameter(httpParameterUrl.name(), payload));
            }
            // POST参数
            List<ParsedHttpParameter> httpParametersBody = modifiedRequest.parameters(HttpParameterType.BODY);
            for (ParsedHttpParameter httpParameterBody : httpParametersBody) {
                // 创建一个检测的payload，用于匹配成功后的参数，存储
                String payload = "http://" + httpParameterBody.name() + "y5neko" + MiscUtils.getRamdomString(10) + "." + detector.getFullDomain();
                modifiedRequest = modifiedRequest.withParameter(HttpParameter.bodyParameter(httpParameterBody.name(), payload));
            }
            // JSON参数
            List<ParsedHttpParameter> httpParametersJson = modifiedRequest.parameters(HttpParameterType.JSON);
            // 有json参数时才进行json处理
            if (!httpParametersJson.isEmpty()) {
                try {
                    String jsonBody = modifiedRequest.bodyToString();
                    // 处理json参数
                    JSONObject jsonObject = JSON.parseObject(jsonBody);
                    JSONUtils.replaceAllJsonValues4Detect(jsonObject, detector.getFullDomain());
                    modifiedRequest = modifiedRequest.withBody(jsonObject.toJSONString());
                } catch (Exception e) {
                    logging.logToError("JSON解析错误，跳过处理");
                }
            }
            System.out.println(modifiedRequest);
            // 尝试发送请求，有些情况下服务器解析dnslog域名时会超时，直接忽略返回结果就行
            String result;
            try {
                result = MiscUtils.bytesToBase64(RawHttpSender.getInstance().sendRawRequest(modifiedRequest.toString(), true));
            } catch (Exception e) {
                result = MiscUtils.bytesToBase64(("timeout:" + e.getMessage()).getBytes(StandardCharsets.UTF_8));
            }
            Thread.sleep(5000);
            // =======================检测是否有结果===================
            // 创建一个新字符串暂时储存解base64后的字符串
            String decodedString = MiscUtils.base64StringToStringAutoDetect(result);

            // Dnslog
            String dnslogResult = detector.getResult();
            List<String> dnslogMatches = RuleMatcherUtils.paramsMatches(dnslogResult);
            // 响应关键字
            if (isKeywordsDetect && responseRules != null && !decodedString.equals("超时")) {
                for (int i = 0; i < responseRules.size(); i++) {
                    JSONObject ruleObj = responseRules.getJSONObject(i);
                    String ruleType = ruleObj.getString("rule");
                    String keyword = ruleObj.getString("keyword");
                    // 空值防御
                    if (ruleType == null || keyword == null) continue;
                    try {
                        boolean resultMatched = false;
                        if ("包含".equalsIgnoreCase(ruleType)) {
                            // 包含匹配
                            resultMatched = decodedString.contains(keyword);
                        }
                        else if ("完全匹配".equalsIgnoreCase(ruleType)) {
                            resultMatched = decodedString.equalsIgnoreCase(keyword);
                        }
                        else if ("正则(包含)".equalsIgnoreCase(ruleType)) {
                            // 正则匹配（部分匹配）
                            Pattern pattern = Pattern.compile(keyword);
                            resultMatched = pattern.matcher(decodedString).find();
                        }
                        else if ("正则(完全匹配)".equalsIgnoreCase(ruleType)) {
                            // 正则匹配（完全匹配）
                            resultMatched = decodedString.matches(keyword);
                        }
                        if (resultMatched) {
                            hasVulnerability = true;
                            break; // 匹配成功立即终止循环
                        }
                    } catch (PatternSyntaxException e) {
                        // 处理无效正则表达式
                        logging.logToError("无效正则表达式: " + keyword);
                    }
                }
            }
            if (dnslogResult.length() > 10) {
                hasVulnerability = true;
            }
            if (!dnslogMatches.isEmpty()) {
                vulParameters.addAll(dnslogMatches);
            }
            if (vulParameters.isEmpty() && hasVulnerability) {
                vulParameters.add("未识别到参数，请人工判断");
            }
            if (hasVulnerability) {
                logging.logToOutput("检测到漏洞，url为:\n" + httpRequestToBeSent.url() + "\n参数列表为:\n" + vulParameters);
                logging.logToOutput("============================");
            }
            // ======================= 写入日志 ============================
            LogDAO dao = LogDAO.getInstance(logging);
            LogDAO.LogEntry entry = new LogDAO.LogEntry(
                    httpRequestToBeSent.url(),
                    hasVulnerability,
                    vulParameters,
                    MiscUtils.bytesToBase64(modifiedRequest.toString().getBytes()),
                    result,
                    "暴力模式"
            );
            dao.insertLog(entry);
        } catch (Exception e) {
            logging.logToError(e);
        }
    }
}
