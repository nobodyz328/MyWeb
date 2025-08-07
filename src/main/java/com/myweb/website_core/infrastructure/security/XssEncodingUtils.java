package com.myweb.website_core.infrastructure.security;

import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS编码工具类
 * 
 * 提供各种编码功能以防止XSS攻击，包括HTML编码、JavaScript编码、URL编码等。
 * 
 * 主要功能：
 * 1. HTML实体编码
 * 2. JavaScript字符串编码
 * 3. URL编码
 * 4. CSS编码
 * 5. 属性值编码
 * 6. JSON编码
 * 
 * 符合需求：4.2, 4.4 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public class XssEncodingUtils {
    
    /**
     * HTML实体编码映射表
     */
    private static final Map<Character, String> HTML_ENTITIES = new HashMap<>();
    
    /**
     * JavaScript转义字符映射表
     */
    private static final Map<Character, String> JS_ESCAPE_CHARS = new HashMap<>();
    
    /**
     * CSS转义字符映射表
     */
    private static final Map<Character, String> CSS_ESCAPE_CHARS = new HashMap<>();
    
    static {
        // 初始化HTML实体编码映射
        HTML_ENTITIES.put('<', "&lt;");
        HTML_ENTITIES.put('>', "&gt;");
        HTML_ENTITIES.put('&', "&amp;");
        HTML_ENTITIES.put('"', "&quot;");
        HTML_ENTITIES.put('\'', "&#x27;");
        HTML_ENTITIES.put('/', "&#x2F;");
        HTML_ENTITIES.put('`', "&#x60;");
        HTML_ENTITIES.put('=', "&#x3D;");
        
        // 初始化JavaScript转义字符映射
        JS_ESCAPE_CHARS.put('\'', "\\'");
        JS_ESCAPE_CHARS.put('"', "\\\"");
        JS_ESCAPE_CHARS.put('\\', "\\\\");
        JS_ESCAPE_CHARS.put('/', "\\/");
        JS_ESCAPE_CHARS.put('\b', "\\b");
        JS_ESCAPE_CHARS.put('\f', "\\f");
        JS_ESCAPE_CHARS.put('\n', "\\n");
        JS_ESCAPE_CHARS.put('\r', "\\r");
        JS_ESCAPE_CHARS.put('\t', "\\t");
        
        // 初始化CSS转义字符映射
        CSS_ESCAPE_CHARS.put('\\', "\\\\");
        CSS_ESCAPE_CHARS.put('"', "\\\"");
        CSS_ESCAPE_CHARS.put('\'', "\\'");
        CSS_ESCAPE_CHARS.put('\n', "\\A ");
        CSS_ESCAPE_CHARS.put('\r', "\\D ");
        CSS_ESCAPE_CHARS.put('\f', "\\C ");
    }
    
    /**
     * HTML编码 - 用于HTML内容上下文
     * 
     * @param input 输入字符串
     * @return HTML编码后的字符串
     */
    public static String htmlEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 使用Spring的HtmlUtils进行基础HTML编码
        String encoded = HtmlUtils.htmlEscape(input);
        
        // 额外编码一些特殊字符
        StringBuilder result = new StringBuilder();
        for (char c : encoded.toCharArray()) {
            if (HTML_ENTITIES.containsKey(c)) {
                result.append(HTML_ENTITIES.get(c));
            } else if (c < 32 || c > 126) {
                // 编码非ASCII字符
                result.append("&#").append((int) c).append(";");
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * HTML属性编码 - 用于HTML属性值上下文
     * 
     * @param input 输入字符串
     * @return HTML属性编码后的字符串
     */
    public static String htmlAttributeEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                case '\'':
                    result.append("&#x27;");
                    break;
                case '=':
                    result.append("&#x3D;");
                    break;
                case '`':
                    result.append("&#x60;");
                    break;
                case '\n':
                    result.append("&#xA;");
                    break;
                case '\r':
                    result.append("&#xD;");
                    break;
                case '\t':
                    result.append("&#x9;");
                    break;
                default:
                    if (c < 32 || c > 126) {
                        result.append("&#x").append(Integer.toHexString(c)).append(";");
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        
        return result.toString();
    }
    
    /**
     * JavaScript编码 - 用于JavaScript字符串上下文
     * 
     * @param input 输入字符串
     * @return JavaScript编码后的字符串
     */
    public static String javascriptEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (JS_ESCAPE_CHARS.containsKey(c)) {
                result.append(JS_ESCAPE_CHARS.get(c));
            } else if (c < 32 || c > 126) {
                // Unicode编码
                result.append(String.format("\\u%04x", (int) c));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * CSS编码 - 用于CSS上下文
     * 
     * @param input 输入字符串
     * @return CSS编码后的字符串
     */
    public static String cssEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (CSS_ESCAPE_CHARS.containsKey(c)) {
                result.append(CSS_ESCAPE_CHARS.get(c));
            } else if (c < 32 || c > 126) {
                // CSS十六进制编码
                result.append("\\").append(Integer.toHexString(c)).append(" ");
            } else if (isCSSSafeChar(c)) {
                result.append(c);
            } else {
                // 转义特殊CSS字符
                result.append("\\").append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * URL编码 - 用于URL参数上下文
     * 
     * @param input 输入字符串
     * @return URL编码后的字符串
     */
    public static String urlEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        try {
            return URLEncoder.encode(input, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // UTF-8应该总是支持的
            throw new RuntimeException("UTF-8编码不支持", e);
        }
    }
    
    /**
     * JSON编码 - 用于JSON上下文
     * 
     * @param input 输入字符串
     * @return JSON编码后的字符串
     */
    public static String jsonEncode(String input) {
        if (input == null) {
            return "null";
        }
        
        if (input.isEmpty()) {
            return "\"\"";
        }
        
        StringBuilder result = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '/':
                    result.append("\\/");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (c < 32 || c > 126) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        result.append("\"");
        
        return result.toString();
    }
    
    /**
     * XML编码 - 用于XML上下文
     * 
     * @param input 输入字符串
     * @return XML编码后的字符串
     */
    public static String xmlEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                case '\'':
                    result.append("&apos;");
                    break;
                default:
                    if (c < 32 && c != '\t' && c != '\n' && c != '\r') {
                        // 移除XML不允许的控制字符
                        continue;
                    } else if (c > 126) {
                        result.append("&#").append((int) c).append(";");
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        
        return result.toString();
    }
    
    /**
     * 通用安全编码 - 根据上下文自动选择编码方式
     * 
     * @param input   输入字符串
     * @param context 上下文类型（html, js, css, url, json, xml）
     * @return 编码后的字符串
     */
    public static String encode(String input, String context) {
        if (input == null) {
            return null;
        }
        
        switch (context.toLowerCase()) {
            case "html":
                return htmlEncode(input);
            case "htmlattr":
            case "html-attr":
                return htmlAttributeEncode(input);
            case "js":
            case "javascript":
                return javascriptEncode(input);
            case "css":
                return cssEncode(input);
            case "url":
                return urlEncode(input);
            case "json":
                return jsonEncode(input);
            case "xml":
                return xmlEncode(input);
            default:
                // 默认使用HTML编码
                return htmlEncode(input);
        }
    }
    
    /**
     * 检查字符是否为CSS安全字符
     * 
     * @param c 字符
     * @return 是否为CSS安全字符
     */
    private static boolean isCSSSafeChar(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               (c >= '0' && c <= '9') ||
               c == '-' || c == '_';
    }
    
    /**
     * 移除所有HTML标签
     * 
     * @param input 输入字符串
     * @return 移除HTML标签后的字符串
     */
    public static String stripHtmlTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 移除所有HTML标签
        return input.replaceAll("<[^>]+>", "");
    }
    
    /**
     * 清理危险字符
     * 
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 移除null字符和控制字符
        String cleaned = input.replaceAll("\\x00", "")
                             .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // HTML编码
        cleaned = htmlEncode(cleaned);
        
        return cleaned;
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private XssEncodingUtils() {
        // 工具类，不允许实例化
    }
}