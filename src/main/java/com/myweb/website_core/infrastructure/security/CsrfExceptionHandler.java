package com.myweb.website_core.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.common.security.exception.CustomCsrfException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * CSRF异常处理器
 * 处理CSRF令牌验证失败的情况，提供友好的错误提示
 * 
 * @author Kiro
 * @since 1.0.0
 */
@Component
public class CsrfExceptionHandler implements AccessDeniedHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CsrfExceptionHandler.class);
    
    private final ObjectMapper objectMapper;
    
    public CsrfExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        
        // 检查是否为CSRF异常
        if (isCsrfException(accessDeniedException)) {
            handleCsrfException(request, response, accessDeniedException);
        } else {
            handleGeneralAccessDenied(request, response, accessDeniedException);
        }
    }
    
    /**
     * 处理CSRF异常
     */
    private void handleCsrfException(HttpServletRequest request, HttpServletResponse response, 
                                   Exception exception) throws IOException {
        
        logger.warn("CSRF token validation failed for request: {} from IP: {}", 
                   request.getRequestURI(), getClientIpAddress(request));
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "csrf_token_invalid");
        errorResponse.put("message", "CSRF令牌无效或已过期，请刷新页面后重试");
        errorResponse.put("code", 403);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        // 如果是Ajax请求，添加刷新令牌的提示
        if (isAjaxRequest(request)) {
            errorResponse.put("action", "refresh_token");
            errorResponse.put("refresh_url", "/api/csrf/token");
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * 处理一般的访问拒绝异常
     */
    private void handleGeneralAccessDenied(HttpServletRequest request, HttpServletResponse response, 
                                         Exception exception) throws IOException {
        
        logger.warn("Access denied for request: {} from IP: {}, reason: {}", 
                   request.getRequestURI(), getClientIpAddress(request), exception.getMessage());
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "access_denied");
        errorResponse.put("message", "访问被拒绝，您没有足够的权限执行此操作");
        errorResponse.put("code", 403);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * 判断是否为CSRF异常
     */
    private boolean isCsrfException(Exception exception) {
        return exception instanceof CsrfException ||
               exception.getCause() instanceof CsrfException ||
               //exception instanceof CustomCsrfException ||
               exception.getCause() instanceof CustomCsrfException;
    }
    
    /**
     * 判断是否为Ajax请求
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getContentType();
        String accept = request.getHeader("Accept");
        
        return "XMLHttpRequest".equals(requestedWith) ||
               (contentType != null && contentType.contains("application/json")) ||
               (accept != null && accept.contains("application/json"));
    }
    
    /**
     * 获取客户端IP地址
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