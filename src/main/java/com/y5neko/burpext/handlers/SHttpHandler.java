package com.y5neko.burpext.handlers;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.ToolSource;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.logging.Logging;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.y5neko.burpext.utils.FileUtils;
import com.y5neko.burpext.utils.JudgeUtils;
import com.y5neko.burpext.detector.Detector;
import com.y5neko.burpext.detector.DigPm;
import com.y5neko.burpext.mode.BruteDetect;
import com.y5neko.burpext.mode.KeywordsDetect;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;
import static com.y5neko.burpext.core.Config.configFilePath;

public class SHttpHandler implements HttpHandler {
    private final Logging logging;

    // 缓存型线程池
//    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final ExecutorService executor = Executors.newFixedThreadPool(200);

    public SHttpHandler(Logging logging) {
        this.logging = logging;
    }

    /**
     * 处理请求
     * @param httpRequestToBeSent 请求对象
     * @return 处理后的请求对象
     */
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        // 获取请求的来源工具
        ToolSource toolSource = httpRequestToBeSent.toolSource();

        // 创建请求和响应的注释对象
        Annotations annotations = httpRequestToBeSent.annotations();

        // 获取请求参数
        List<ParsedHttpParameter> httpParametersUrl = httpRequestToBeSent.parameters(HttpParameterType.URL);
        List<ParsedHttpParameter> httpParametersBody = httpRequestToBeSent.parameters(HttpParameterType.BODY);
        List<ParsedHttpParameter> httpParametersJson = httpRequestToBeSent.parameters(HttpParameterType.JSON);

        // 有参数才处理，否则直接跳过
        if (!httpParametersUrl.isEmpty() || !httpParametersBody.isEmpty() || !httpParametersJson.isEmpty()) {
            System.out.println(httpParametersUrl);
            System.out.println(httpParametersBody);
            System.out.println(httpParametersJson);
            try {
                // 获取配置文件
                String configJson = FileUtils.readFileToString(configFilePath);
                JSONObject config = JSONObject.parseObject(configJson);

                // 检查是否启用了插件
                JSONArray enabledComponents = config.getJSONArray("enabledBurpComponents");
                boolean hasEnabledPlugin = enabledComponents.stream()
                        .filter(String.class::isInstance)
                        .map(Object::toString)
                        .anyMatch(s -> s.equalsIgnoreCase(String.valueOf(toolSource.toolType())));

                // 检查检测模式
                String detectionMode = config.getString("detectionMode");

                // 获取白名单规则
                String[] whiteListRules = config.getObject("whiteList", String[].class);

                // 获取检测方式
                JSONArray enabledDetectionMethods = config.getJSONArray("enabledDetectionMethods");

                // 检查是否启用了关键字检测
                boolean isKeywordsDetect = enabledDetectionMethods.contains("响应关键字");

                // 在函数顶部判断是否需要异步处理
                if (enabledComponents.contains("启用插件") && hasEnabledPlugin) {
                    executor.submit(() -> {
                        try {
                            if (JudgeUtils.isWhiteList(httpRequestToBeSent, whiteListRules)) {
                                Detector detector = null;
                                if (enabledDetectionMethods.contains("DigPm")) {
                                    detector = new DigPm();
                                }
                                detect(httpRequestToBeSent, detectionMode, detector, isKeywordsDetect, logging);
                            }
                        } catch (Exception e) {
                            logging.logToError("检测任务失败: " + e.getMessage());
                        }
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // 不处理本来的请求
        return continueWith(httpRequestToBeSent, annotations);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        return null;
    }

    /**
     * 检测请求<br><b>detector为null时默认仅进行响应关键字检测</b>
     * @param httpRequestToBeSent 请求对象
     * @param detectionMode 检测模式
     */
    private static void detect(HttpRequestToBeSent httpRequestToBeSent, String detectionMode, Detector detector, boolean isKeywordsDetect, Logging logging) {
        if (detectionMode.equals("暴力模式（谨慎开启）") && detector == null) {
            new BruteDetect(httpRequestToBeSent, detector, isKeywordsDetect, logging);
        }
        else if (detectionMode.equals("参数关键字") && detector == null) {
            new KeywordsDetect(httpRequestToBeSent, detector, isKeywordsDetect, logging);
        }
        else if (detectionMode.equals("暴力模式（谨慎开启）")) {
            new BruteDetect(httpRequestToBeSent, detector, isKeywordsDetect, logging);
        }
        else if (detectionMode.equals("参数关键字")) {
            new KeywordsDetect(httpRequestToBeSent, detector, isKeywordsDetect, logging);
        }
        else {
            logging.logToError("未知检测模式或未启用检测器，跳过处理");
        }
    }
}
