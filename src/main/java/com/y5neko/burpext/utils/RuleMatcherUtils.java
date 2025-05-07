package com.y5neko.burpext.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RuleMatcherUtils {
    // 缓存正则表达式，避免重复编译
    private static final Map<String, Pattern> regexCache = new ConcurrentHashMap<>();

    /**
     * 匹配关键字
     * @param key 待匹配的关键字
     * @param paramsRules 关键字规则列表
     * @return 是否匹配
     */
    public static boolean isKeyMatched(String key, JSONArray paramsRules) {
        if (paramsRules == null || paramsRules.isEmpty()) return false;

        for (int i = 0; i < paramsRules.size(); i++) {
            JSONObject rule = paramsRules.getJSONObject(i);
            String ruleType = rule.getString("rule");
            String keyword = rule.getString("keyword");

            if (ruleType == null || keyword == null) continue;

            try {
                switch (ruleType) {
                    case "包含":
                        if (key.contains(keyword)) return true;
                        break;
                    case "完全匹配":
                        if (key.equals(keyword)) return true;
                        break;
                    case "正则(包含)":
                        Pattern pattern = regexCache.computeIfAbsent(keyword, Pattern::compile);
                        if (pattern.matcher(key).find()) return true;
                        break;
                    case "正则(完全匹配)":
                        Pattern fullPattern = regexCache.computeIfAbsent(keyword, k -> Pattern.compile("^" + k + "$"));
                        if (fullPattern.matcher(key).matches()) return true;
                        break;
                }
            } catch (PatternSyntaxException e) {
                System.err.println("Invalid regex pattern: " + keyword);
            }
        }
        return false;
    }

    static boolean matchUrlRule(String fullUrl, String rule) {
        // 处理通配符在结尾的情况（如 https://example.com/api/*）
        if (rule.endsWith("*")) {
            String prefix = rule.substring(0, rule.length() - 1);
            return fullUrl.startsWith(prefix);
        }
        return fullUrl.equalsIgnoreCase(rule);
    }

    static boolean matchHostPortRule(String host, String port, String rule) {
        // 拆分主机和端口部分
        String[] parts = rule.split(":", 2);
        if (parts.length != 2) return false;

        String ruleHost = parts[0];
        String rulePort = parts[1];

        // 先校验端口
        if (!rulePort.equals(port)) return false;

        // 再校验主机（支持通配符）
        return matchHostWithWildcard(host, ruleHost);
    }

    static boolean matchHostWithWildcard(String host, String rule) {
        // 处理包含通配符的情况
        if (rule.contains("*")) {
            String regex = "^" + rule.replace(".", "\\.")
                    .replace("*", ".*") + "$";
            return host.matches(regex);
        }
        // 精确匹配
        return host.equalsIgnoreCase(rule);
    }

    /**
     * 字符串匹配
     * @param str1 待匹配的字符串
     * @param str2OrPattern 匹配规则，支持正则表达式
     * @param matchType 匹配类型，0 为包含，1 为正则表达式
     * @return 是否匹配
     */
    public static boolean stringMatch(String str1, String str2OrPattern, int matchType) {
        switch (matchType) {
            case 0:
                return str1.contains(str2OrPattern);
            case 1:
                Pattern pattern = Pattern.compile(str2OrPattern);
                return pattern.matcher(str1).find();
            default:
                return false;
        }
    }

    /**
     * 匹配dnslog结果中的参数
     * @param input dnslog结果
     * @return 匹配到的参数列表
     */
    public static List<String> paramsMatches(String input) {
        Pattern PATTERN = Pattern.compile("\"([^\"]*?)y5neko");

        List<String> matches = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(input);

        while (matcher.find()) {
            String matchedValue = matcher.group(1);
            if (!matchedValue.isEmpty()) {
                matches.add(matchedValue);
            }
        }
        return matches;
    }
}
