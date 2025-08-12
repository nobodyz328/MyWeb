package com.myweb.website_core.infrastructure.security.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static com.myweb.website_core.common.util.SecurityEventUtils.*;

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
@Slf4j
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 是否检测到XSS攻击尝试
     */
    private boolean xssAttemptDetected = false;
    
    /**
     * 缓存的请求体内容
     */
    private byte[] cachedBody;
    
    /**
     * 增强型XSS过滤器
     */
    private final XssFilterService xssFilterService;
    
    /**
     * 构造函数
     * 
     * @param request 原始HTTP请求
     * @param xssFilterService 增强型XSS过滤器
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request, XssFilterService xssFilterService) {
        super(request);
        this.xssFilterService = xssFilterService;
        
        // 预读取请求体内容进行XSS检查
        try {
            cacheRequestBody();
        } catch (IOException e) {
            log.warn("缓存请求体失败", e);
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
     * 获取清理后的请求体输入流
     * 
     * @return ServletInputStream
     * @throws IOException IO异常
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBody != null) {
            String bodyContent = new String(cachedBody, StandardCharsets.UTF_8);
            
            // 对于JSON请求，只检测XSS但不修改内容，避免破坏JSON格式
            if (isJsonRequest()) {
                // 检测XSS攻击但不修改内容    
                detectXssInJson(bodyContent);
                // 返回原始内容
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
                return createServletInputStream(byteArrayInputStream);
            } else {
                // 非JSON请求进行XSS清理
                String cleanedBody = cleanXSS(bodyContent);
                byte[] cleanedBytes = cleanedBody.getBytes(StandardCharsets.UTF_8);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cleanedBytes);
                return createServletInputStream(byteArrayInputStream);
            }
        }
        return super.getInputStream();
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
            String bodyContent = new String(cachedBody, StandardCharsets.UTF_8);
            
            // 对于JSON请求，只检测XSS但不修改内容
            if (isJsonRequest()) {
                detectXssInJson(bodyContent);
                return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(cachedBody)));
            } else {
                String cleanedBody = cleanXSS(bodyContent);
                return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(cleanedBody.getBytes(StandardCharsets.UTF_8))));
            }
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

        String clientIp = getIpAddress();
        String requestUri = getRequestUri();
        String originalValue = value;
            
        String filteredValue = xssFilterService.filterXss(value, requestUri, clientIp);
            
        // 检查是否有XSS攻击
        if (!originalValue.equals(filteredValue)) {
            xssAttemptDetected = true;
            log.warn("增强XSS过滤器检测到攻击 - IP: {}, URI: {}", clientIp, requestUri);
        }
            
        return filteredValue;

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
    
    /**
     * 判断是否为JSON请求
     * 
     * @return true如果是JSON请求
     */
    private boolean isJsonRequest() {
        String contentType = getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
    
    /**
     * 检测JSON内容中的XSS攻击但不修改内容
     * 
     * @param jsonContent JSON内容
     */
    private void detectXssInJson(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return;
        }
        
        String clientIp = getIpAddress();
        String requestUri = getRequestUri();
        
        // 使用XSS过滤服务检测攻击，但不应用过滤
        if (xssFilterService.containsXss(jsonContent, requestUri, clientIp)) {
            xssAttemptDetected = true;
            log.warn("JSON请求体中检测到XSS攻击 - IP: {}, URI: {}", clientIp, requestUri);
        }
    }
    
    /**
     * 创建ServletInputStream
     * 
     * @param byteArrayInputStream 字节数组输入流
     * @return ServletInputStream
     */
    private ServletInputStream createServletInputStream(ByteArrayInputStream byteArrayInputStream) {
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Not implemented for synchronous reading
            }

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
    }
}