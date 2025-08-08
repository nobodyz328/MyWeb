package com.myweb.website_core.infrastructure.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * XSS HTTP请求包装器
 * <p>
 * 包装HttpServletRequest，对请求参数、请求体等进行XSS攻击代码清理。
 * <p>
 * 主要功能：
 * 1. 清理请求参数中的XSS攻击代码
 * 2. 清理请求头中的XSS攻击代码
 * 3. 清理请求体中的XSS攻击代码
 * 4. 支持多种XSS攻击向量检测
 * 5. 提供HTML编码和JavaScript编码
 * <p>
 * 符合需求：4.2, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(XssHttpServletRequestWrapper.class);
    
    /**
     * XSS攻击模式库 - 支持多种攻击向量检测
     */
    private static final Pattern[] XSS_PATTERNS = {
        // Script标签攻击
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        
        // JavaScript协议攻击
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),
        
        // 事件处理器攻击
        Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseover\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onfocus\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onblur\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onchange\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onsubmit\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onreset\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onselect\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeydown\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeypress\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeyup\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmousedown\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseup\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmousemove\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseout\\s*=", Pattern.CASE_INSENSITIVE),
        
        // 表达式攻击
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // 样式表攻击
        Pattern.compile("<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE),
        
        // 链接攻击
        Pattern.compile("src\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("href\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE),
        
        // 框架攻击
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<frame[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<frameset[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // 对象和嵌入攻击
        Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<applet[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // 表单攻击
        Pattern.compile("<form[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("formaction\\s*=", Pattern.CASE_INSENSITIVE),
        
        // Meta标签攻击
        Pattern.compile("<meta[^>]*http-equiv", Pattern.CASE_INSENSITIVE),
        
        // 注释攻击
        Pattern.compile("<!--.*?-->", Pattern.DOTALL),
        
        // 编码绕过攻击 - 只检测可疑的编码模式
        Pattern.compile("&#x?[0-9a-f]+;?.*(?:script|javascript|onload|onerror)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%[0-9a-f]{2}.*(?:script|javascript|onload|onerror)", Pattern.CASE_INSENSITIVE),
        
        // 其他危险标签
        Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<base[^>]*>", Pattern.CASE_INSENSITIVE)
    };
    
    /**
     * 危险字符模式 - 只检测可疑的编码模式
     */
    private static final Pattern[] DANGEROUS_CHAR_PATTERNS = {
        Pattern.compile("\\\\x[0-9a-f]{2}.*(?:script|javascript|onload|onerror)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\\u[0-9a-f]{4}.*(?:script|javascript|onload|onerror)", Pattern.CASE_INSENSITIVE)
    };
    
    /**
     * 是否检测到XSS攻击尝试
     */
    private boolean xssAttemptDetected = false;
    
    /**
     * 缓存的请求体内容
     */
    private byte[] cachedBody;
    
    /**
     * 构造函数
     * 
     * @param request 原始HTTP请求
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        
        // 预读取请求体内容进行XSS检查
        try {
            cacheRequestBody();
        } catch (IOException e) {
            logger.warn("缓存请求体失败", e);
        }
    }
    
    /**
     * 获取清理后的请求参数值数组
     * 
     * @param parameter 参数名
     * @return 清理后的参数值数组
     */
    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        
        String[] cleanedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanedValues[i] = cleanXSS(values[i]);
        }
        
        return cleanedValues;
    }
    
    /**
     * 获取清理后的请求参数值
     * 
     * @param parameter 参数名
     * @return 清理后的参数值
     */
    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        return cleanXSS(value);
    }
    
    /**
     * 获取清理后的请求头值
     * 
     * @param name 请求头名称
     * @return 清理后的请求头值
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXSS(value);
    }
    
    /**
     * 获取清理后的请求体读取器
     * 
     * @return BufferedReader
     * @throws IOException IO异常
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (cachedBody != null) {
            String cleanedBody = cleanXSS(new String(cachedBody, StandardCharsets.UTF_8));
            return new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(cleanedBody.getBytes(StandardCharsets.UTF_8))));
        }
        return super.getReader();
    }
    
    /**
     * 清理XSS攻击代码
     * 
     * @param value 原始值
     * @return 清理后的值
     */
    private String cleanXSS(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String originalValue = value;
        
        // 1. 首先检查XSS攻击模式（在编码之前）
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                xssAttemptDetected = true;
                logger.warn("检测到XSS攻击模式: {} in value: {}", pattern.pattern(), 
                    originalValue.length() > 100 ? originalValue.substring(0, 100) + "..." : originalValue);
            }
            value = pattern.matcher(value).replaceAll("");
        }
        
        // 2. 检查危险字符模式
        for (Pattern pattern : DANGEROUS_CHAR_PATTERNS) {
            if (pattern.matcher(value).find()) {
                xssAttemptDetected = true;
            }
            value = pattern.matcher(value).replaceAll("");
        }
        
        // 3. HTML编码危险字符
        value = htmlEncode(value);
        
        // 4. JavaScript编码（对于可能在JavaScript上下文中使用的内容）
        value = javascriptEncode(value);
        
        // 5. 移除null字符和控制字符
        value = value.replaceAll("\\x00", "").replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // 6. 限制长度防止DoS攻击
        if (value.length() > 10000) {
            value = value.substring(0, 10000);
            logger.warn("输入值过长，已截断到10000字符");
        }
        
        return value;
    }
    
    /**
     * HTML编码
     * 
     * @param input 输入字符串
     * @return HTML编码后的字符串
     */
    private String htmlEncode(String input) {
        if (input == null) {
            return null;
        }
        
        // 使用Spring的HtmlUtils进行HTML编码
        return HtmlUtils.htmlEscape(input);
    }
    
    /**
     * JavaScript编码
     * 
     * @param input 输入字符串
     * @return JavaScript编码后的字符串
     */
    private String javascriptEncode(String input) {
        if (input == null) {
            return null;
        }
        
        StringBuilder encoded = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\'':
                    encoded.append("\\'");
                    break;
                case '"':
                    encoded.append("\\\"");
                    break;
                case '\\':
                    encoded.append("\\\\");
                    break;
                case '/':
                    encoded.append("\\/");
                    break;
                case '\b':
                    encoded.append("\\b");
                    break;
                case '\f':
                    encoded.append("\\f");
                    break;
                case '\n':
                    encoded.append("\\n");
                    break;
                case '\r':
                    encoded.append("\\r");
                    break;
                case '\t':
                    encoded.append("\\t");
                    break;
                default:
                    if (c < 32 || c > 126) {
                        encoded.append(String.format("\\u%04x", (int) c));
                    } else {
                        encoded.append(c);
                    }
                    break;
            }
        }
        
        return encoded.toString();
    }
    
    /**
     * 缓存请求体内容
     * 
     * @throws IOException IO异常
     */
    private void cacheRequestBody() throws IOException {
        if ("POST".equalsIgnoreCase(getMethod()) || "PUT".equalsIgnoreCase(getMethod())) {
            try (var inputStream = getInputStream()) {
                cachedBody = inputStream.readAllBytes();
            }
        }
    }
    
    /**
     * 是否检测到XSS攻击尝试
     * 
     * @return true如果检测到XSS攻击尝试
     */
    public boolean hasXssAttempt() {
        return xssAttemptDetected;
    }
    
    /**
     * 获取XSS攻击检测统计信息
     * 
     * @return 检测统计信息
     */
    public String getXssDetectionStats() {
        return String.format("XSS检测统计 - 攻击尝试: %s, 请求URI: %s", 
            xssAttemptDetected ? "是" : "否", getRequestURI());
    }
}