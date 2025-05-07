// 新建文件 JSONUtils.java
package com.y5neko.burpext.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.function.Function;

public class JSONUtils {
    public static void replaceAllValues(JSONObject jsonObject, Function<String, String> payloadGenerator) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            String payload = payloadGenerator.apply(key);

            if (value instanceof JSONObject) {
                replaceAllValues((JSONObject) value, payloadGenerator);
            } else if (value instanceof JSONArray) {
                replaceAllValues((JSONArray) value, payloadGenerator);
            } else {
                jsonObject.put(key, payload);
            }
        }
    }

    public static void replaceAllValues(JSONArray jsonArray, Function<String, String> payloadGenerator) {
        for (int i = 0; i < jsonArray.size(); i++) {
            Object value = jsonArray.get(i);
            String payload = payloadGenerator.apply("array"); // 数组元素使用固定 key

            if (value instanceof JSONObject) {
                replaceAllValues((JSONObject) value, payloadGenerator);
            } else if (value instanceof JSONArray) {
                replaceAllValues((JSONArray) value, payloadGenerator);
            } else {
                jsonArray.set(i, payload);
            }
        }
    }

    /**
     * 递归替换所有的值
     * @param jsonObject JSON对象
     * @param suffix 域名后缀
     */
    public static void replaceAllJsonValues4Detect(JSONObject jsonObject, String suffix) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            // 创建一个检测的payload，用于匹配成功后的参数，存储
            String payload = "http://" + key + "y5neko" + MiscUtils.getRamdomString(10) + "." + suffix;

            if (value instanceof JSONObject) {
                // 递归处理子对象
                replaceAllJsonValues4Detect((JSONObject) value, suffix);
            } else if (value instanceof JSONArray) {
                // 处理数组
                replaceAllJsonArrayValues4Detect((JSONArray) value, suffix);
            } else {
                // 替换值
                jsonObject.put(key, payload);
            }
        }
    }

    /**
     * 递归替换数组中的值
     * @param jsonArray JSON数组
     * @param suffix 域名后缀
     */
    public static void replaceAllJsonArrayValues4Detect(JSONArray jsonArray, String suffix) {
        for (int i = 0; i < jsonArray.size(); i++) {
            Object value = jsonArray.get(i);

            String payload = "http://" + "arrayy5neko" + MiscUtils.getRamdomString(10) + "." + suffix;

            if (value instanceof JSONObject) {
                replaceAllJsonValues4Detect((JSONObject) value, suffix);
            } else if (value instanceof JSONArray) {
                replaceAllJsonArrayValues4Detect((JSONArray) value, suffix);
            } else {
                // 替换数组中的普通值
                jsonArray.set(i, payload);
            }
        }
    }

    /**
     * 递归替换所有的值
     * @param jsonObject JSON对象
     * @param suffix 域名后缀
     * @param paramsRules 规则列表
     * @param isDetected 是否检测到
     */
    public static void replaceAllJsonValues4DetectWithRules(JSONObject jsonObject, String suffix, JSONArray paramsRules, boolean isDetected) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            // 规则匹配检查
            boolean shouldReplace = RuleMatcherUtils.isKeyMatched(key, paramsRules);

            if (shouldReplace) {
                String payload = "http://" + key + "y5neko" + MiscUtils.getRamdomString(10) + "." + suffix;
                // 保留原值类型处理
                if (value instanceof JSONObject) {
                    replaceAllJsonValues4DetectWithRules((JSONObject) value, suffix, paramsRules, isDetected);
                    jsonObject.put(key, value); // 保留结构，仅处理嵌套
                } else if (value instanceof JSONArray) {
                    replaceAllJsonArrayValues4DetectWithRules((JSONArray) value, suffix, paramsRules, isDetected);
                    jsonObject.put(key, value);
                } else {
                    jsonObject.put(key, payload);
                    isDetected = true;
                }
            } else {
                // 不匹配时递归处理嵌套结构但不修改值
                if (value instanceof JSONObject) {
                    replaceAllJsonValues4DetectWithRules((JSONObject) value, suffix, paramsRules, isDetected);
                } else if (value instanceof JSONArray) {
                    replaceAllJsonArrayValues4DetectWithRules((JSONArray) value, suffix, paramsRules, isDetected);
                }
            }
        }
    }

    /**
     * 递归替换数组中的值
     * @param jsonArray JSON数组
     * @param suffix 域名后缀
     * @param paramsRules 规则列表
     * @param isDetected 是否检测到
     */
    public static void replaceAllJsonArrayValues4DetectWithRules(JSONArray jsonArray, String suffix, JSONArray paramsRules, boolean isDetected) {
        for (int i = 0; i < jsonArray.size(); i++) {
            Object value = jsonArray.get(i);

            // 数组元素处理逻辑（可根据需求调整）
            String payload = "http://arrayy5neko" + MiscUtils.getRamdomString(10) + "." + suffix;

            if (value instanceof JSONObject) {
                replaceAllJsonValues4DetectWithRules((JSONObject) value, suffix, paramsRules, isDetected);
            } else if (value instanceof JSONArray) {
                replaceAllJsonArrayValues4DetectWithRules((JSONArray) value, suffix, paramsRules, isDetected);
            } else {
                jsonArray.set(i, payload);
                isDetected = true;
            }
        }
    }
}