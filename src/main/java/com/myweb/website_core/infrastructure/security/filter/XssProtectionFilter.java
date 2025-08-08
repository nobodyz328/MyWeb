package com.myweb.website_core.infrastructure.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * XSS防护过滤器
 * <p>
 * 实现对用户输入的XSS攻击代码过滤，符合GB/T 22239-2019二级等保要求。
 * <p>
 * 主要功能：
 * 1. 过滤HTTP请求参数中的XSS攻击代码
 * 2. 对用户输入进行HTML编码和JavaScript编码
 * 3. 检测并阻止多种XSS攻击向量
 * 4. 记录XSS攻击尝试的审计日志
 * <p>
 * 符合需求：4.2, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE+1)
public class XssProtectionFilter implements Filter {

    /**
     * 过滤器初始化
     */
    @Override
    public void init(FilterConfig filterConfig) {
        log.info("XSS防护过滤器初始化完成");
    }
    
    /**
     * 执行XSS防护过滤
     * 
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param chain    过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 获取客户端IP地址用于日志记录
        String clientIp = getClientIpAddress(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        
        try {
            // 包装请求以进行XSS过滤
            XssHttpServletRequestWrapper wrappedRequest =
                new XssHttpServletRequestWrapper(httpRequest);

            // 检查是否检测到XSS攻击
            if (wrappedRequest.hasXssAttempt()) {
                log.warn("检测到XSS攻击尝试 - IP: {}, URI: {}, User-Agent: {}",
                    clientIp, requestUri, httpRequest.getHeader("User-Agent"));
                
                //这里选择继续处理已清理的请求
                //根据安全策略决定是否阻止请求
            }
            
            // 继续过滤器链
            chain.doFilter(wrappedRequest, httpResponse);
            
        } catch (Exception e) {
            log.error("XSS防护过滤器处理异常 - IP: {}, URI: {}", clientIp, requestUri, e);
            throw e;
        }
    }
    
    /**
     * 过滤器销毁
     */
    @Override
    public void destroy() {
        log.info("XSS防护过滤器销毁");
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}