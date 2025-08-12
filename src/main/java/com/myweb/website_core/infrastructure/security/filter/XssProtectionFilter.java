package com.myweb.website_core.infrastructure.security.filter;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.myweb.website_core.common.util.SecurityEventUtils.*;

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
 * 符合需求：2.1, 2.3, 2.5 - XSS防护机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XssProtectionFilter implements Filter {

    private final AuditLogServiceAdapter securityAuditService;
    private final XssFilterConfig xssFilterConfig;
    private final XssFilterService xssFilterService;

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
        
        // 检查XSS防护是否启用
        if (!xssFilterConfig.isEnabled()) {
            //log.debug("XSS防护已禁用，跳过过滤");
            chain.doFilter(request, response);
            return;
        }
        
        // 获取客户端IP地址用于日志记录
        String clientIp = getIpAddress();
        String requestUri = getRequestUri();
        String userAgent = getUserAgent();
        
        try {
            // 包装请求以进行XSS过滤（使用增强过滤器）
            XssHttpServletRequestWrapper wrappedRequest =
                new XssHttpServletRequestWrapper(httpRequest, xssFilterService);

            // 检查是否检测到XSS攻击
            if (wrappedRequest.hasXssAttempt()) {
                // 记录XSS攻击审计日志
                recordXssAttackAudit(clientIp, requestUri, userAgent, httpRequest);
                
                // 严格模式下阻止请求
                if (xssFilterConfig.isStrictMode()) {
                    log.warn("严格模式下阻止XSS攻击请求 - IP: {}, URI: {}", clientIp, requestUri);
                    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    httpResponse.getWriter().write("{\"error\":\"检测到XSS攻击，请求被拒绝\"}");
                    return;
                }
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
     * 记录XSS攻击审计日志
     *
     * @param clientIp 客户端IP
     * @param requestUri 请求URI
     * @param userAgent 用户代理
     * @param request HTTP请求
     */
    private void recordXssAttackAudit(String clientIp, String requestUri, String userAgent, HttpServletRequest request) {
        try {
            log.warn("检测到XSS攻击尝试 - IP: {}, URI: {}, User-Agent: {}",
                    clientIp, requestUri, userAgent);
            Map<String, Object> details = new HashMap<>();
            details.put("clientIp", clientIp);
            details.put("requestUri", requestUri);
            details.put("userAgent", userAgent);
            details.put("method", request.getMethod());
            details.put("timestamp", LocalDateTime.now());
            details.put("sessionId", request.getSession(false) != null ? request.getSession().getId() : "anonymous");
            
            // 记录部分请求参数（脱敏处理）
            Map<String, String> parameters = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    String value = values[0];
                    // 限制参数值长度并脱敏
                    if (value.length() > 100) {
                        value = value.substring(0, 100) + "...";
                    }
                    parameters.put(key, value);
                }
            });
            details.put("parameters", parameters);
            
            securityAuditService.logSecurityEvent(
                    SecurityEventRequest.builder()
                            .eventType(SecurityEventType.XSS_ATTACK_ATTEMPT)
                            .title("XSS攻击尝试")
                            .description("检测到XSS攻击尝试")
                            .eventData(details)
                            .riskScore(75)
                            .build()
            );
            
        } catch (Exception e) {
            log.error("记录XSS攻击审计日志失败", e);
        }
    }

}