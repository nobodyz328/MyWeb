package com.myweb.website_core.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.IPS.ratelimit.RateLimitingService;
import com.myweb.website_core.common.exception.RateLimitExceededException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 访问频率限制过滤器
 * 基于Redis实现的访问频率控制过滤器
 * 支持不同接口的差异化限制策略和基于IP/用户的双重限制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter implements Filter {
    


    private final RateLimitingService rateLimitingService;

    private final ObjectMapper objectMapper;
    
    /**
     * 不需要进行频率限制的URI模式
     */
    private static final String[] EXCLUDED_PATTERNS = {
        "/static/", "/css/", "/js/", "/images/", "/favicon.ico",
        "/actuator/health", "/actuator/info"
    };
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // 检查是否需要跳过频率限制
            if (shouldSkipRateLimit(httpRequest)) {
                chain.doFilter(request, response);
                return;
            }
            
            // 获取客户端信息
            String clientIp = getClientIpAddress(httpRequest);
            String uri = httpRequest.getRequestURI();
            String username = getCurrentUsername();
            
            // 执行频率限制检查
            if (!rateLimitingService.isAllowed(clientIp, uri, username)) {
                handleRateLimitExceeded(httpRequest, httpResponse, clientIp, uri, username);
                return;
            }
            
            // 添加频率限制状态到响应头
            addRateLimitHeaders(httpResponse, clientIp, uri, username);
            
            // 继续处理请求
            chain.doFilter(request, response);
            
        } catch (RateLimitExceededException e) {
            handleRateLimitExceeded(httpRequest, httpResponse, 
                                  getClientIpAddress(httpRequest), 
                                  httpRequest.getRequestURI(), 
                                  getCurrentUsername());
        } catch (Exception e) {
            log.error("访问频率限制过滤器异常", e);
            // 异常时继续处理请求，避免影响正常业务
            chain.doFilter(request, response);
        }
    }
    
    /**
     * 检查是否应该跳过频率限制
     */
    private boolean shouldSkipRateLimit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // 检查排除的URI模式
        for (String pattern : EXCLUDED_PATTERNS) {
            if (uri.contains(pattern)) {
                return true;
            }
        }
        
        // 检查HTTP方法限制POST、PUT、DELETE等修改操作
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            // 对于读取操作，可以设置更宽松的限制或跳过
            return isReadOnlyEndpoint(uri);
        }
        
        return false;
    }
    
    /**
     * 检查是否为只读端点
     */
    private boolean isReadOnlyEndpoint(String uri) {
        // 定义只读端点模式
        String[] readOnlyPatterns = {
            "/api/posts", "/api/images/", "/view/", "/post/",
            "/search", "/announcements"
        };
        
        for (String pattern : readOnlyPatterns) {
            if (uri.startsWith(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // 检查各种代理头
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 处理多个IP的情况，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                if (isValidIp(ip)) {
                    return ip;
                }
            }
        }
        
        // 最后使用远程地址
        String remoteAddr = request.getRemoteAddr();
        return isValidIp(remoteAddr) ? remoteAddr : "unknown";
    }
    
    /**
     * 验证IP地址格式
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        
        // 简单的IP格式验证
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取当前用户名失败", e);
        }
        return null;
    }
    
    /**
     * 处理访问频率超限
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response,
                                       String clientIp, String uri, String username) throws IOException {
        
        log.warn("访问频率超限被拒绝: clientIp={}, uri={}, username={}, userAgent={}",
                   clientIp, uri, username, request.getHeader("User-Agent"));
        
        // 设置响应状态和头部
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 添加Retry-After头部
        response.setHeader("Retry-After", "60"); // 建议60秒后重试
        
        // 构建错误响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "TOO_MANY_REQUESTS");
        errorResponse.put("message", "访问频率过高，请稍后再试");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", uri);
        
        // 如果是AJAX请求，返回JSON格式
        if (isAjaxRequest(request)) {
            errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
        } else {
            // 如果是普通请求，可以重定向到错误页面或返回HTML
            response.sendRedirect("/error/rate-limit");
        }
    }
    
    /**
     * 检查是否为AJAX请求
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        
        return "XMLHttpRequest".equals(requestedWith) ||
               (accept != null && accept.contains("application/json")) ||
               (contentType != null && contentType.contains("application/json"));
    }
    
    /**
     * 添加频率限制状态到响应头
     */
    private void addRateLimitHeaders(HttpServletResponse response, String clientIp, String uri, String username) {
        try {
            RateLimitingService.RateLimitStatus status = 
                rateLimitingService.getRateLimitStatus(clientIp, uri, username);
            
            // 添加标准的频率限制响应头
            response.setHeader("X-RateLimit-Limit", String.valueOf(status.getMaxRequests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemainingRequests()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000)); // 1分钟后重置
            
            // 添加自定义头部
            if (status.getIpCount() > 0) {
                response.setHeader("X-RateLimit-IP-Count", String.valueOf(status.getIpCount()));
            }
            if (status.getUserCount() > 0) {
                response.setHeader("X-RateLimit-User-Count", String.valueOf(status.getUserCount()));
            }
            
        } catch (Exception e) {
            log.debug("添加频率限制响应头失败", e);
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("访问频率限制过滤器初始化完成");
    }
    
    @Override
    public void destroy() {
        log.info("访问频率限制过滤器销毁");
    }
}