package com.myweb.website_core.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.security.CustomCsrfException;
import com.myweb.website_core.infrastructure.security.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一访问拒绝处理器
 * <p>
 * 处理所有访问拒绝异常，包括：
 * - CSRF令牌验证失败
 * - 权限不足
 * - 其他访问拒绝场景
 * <p>
 * 支持API和Web页面两种响应模式
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedAccessDeniedHandler implements AccessDeniedHandler {
    
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    
    @Override
    public void handle(HttpServletRequest request, 
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        // 记录访问拒绝的审计日志
        recordAccessDeniedAudit(request, accessDeniedException);
        
        // 检查异常类型并处理
        if (isCsrfException(accessDeniedException)) {
            handleCsrfException(request, response, accessDeniedException);
        } else {
            handleGeneralAccessDenied(request, response, accessDeniedException);
        }
    }
    
    /**
     * 处理CSRF异常
     */
    private void handleCsrfException(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   AccessDeniedException exception) throws IOException {
        
        log.warn("CSRF token validation failed for request: {} from IP: {}", 
                request.getRequestURI(), getClientIpAddress(request));
        
        if (isApiRequest(request)) {
            handleApiCsrfException(request, response, exception);
        } else {
            try {
                handleWebCsrfException(request, response, exception);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * 处理API请求的CSRF异常
     */
    private void handleApiCsrfException(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      AccessDeniedException exception) throws IOException {
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "csrf_token_invalid");
        errorResponse.put("code", "CSRF_TOKEN_INVALID");
        errorResponse.put("message", "CSRF令牌无效或已过期，请刷新页面后重试");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        // 添加刷新令牌的提示
        errorResponse.put("action", "refresh_token");
        errorResponse.put("refresh_url", "/api/csrf/token");
        
        // 开发环境下添加详细信息
        if (isDevelopmentMode()) {
            errorResponse.put("detail", exception.getMessage());
            errorResponse.put("exceptionType", exception.getClass().getSimpleName());
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * 处理Web页面的CSRF异常
     */
    private void handleWebCsrfException(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      AccessDeniedException exception) throws IOException, ServletException {
        
        // 设置错误信息到请求属性中
        request.setAttribute("errorCode", "CSRF_TOKEN_INVALID");
        request.setAttribute("errorMessage", "CSRF令牌无效或已过期，请刷新页面后重试");
        request.setAttribute("requestUrl", request.getRequestURI());
        request.setAttribute("timestamp", System.currentTimeMillis());
        request.setAttribute("action", "refresh_page");
        
        if (isDevelopmentMode()) {
            request.setAttribute("exceptionDetail", exception.getMessage());
            request.setAttribute("exceptionType", exception.getClass().getSimpleName());
        }
        
        // 重定向到CSRF错误页面
        response.sendRedirect("/error/csrf");
    }
    
    /**
     * 处理一般的访问拒绝异常
     */
    private void handleGeneralAccessDenied(HttpServletRequest request, 
                                         HttpServletResponse response, 
                                         AccessDeniedException exception) throws IOException, ServletException {
        
        log.warn("Access denied for request: {} from IP: {}, reason: {}", 
                request.getRequestURI(), getClientIpAddress(request), exception.getMessage());
        
        if (isApiRequest(request)) {
            handleApiAccessDenied(request, response, exception);
        } else {
            handleWebAccessDenied(request, response, exception);
        }
    }
    
    /**
     * 处理API请求的访问拒绝
     */
    private void handleApiAccessDenied(HttpServletRequest request, 
                                     HttpServletResponse response,
                                     AccessDeniedException exception) throws IOException {
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "access_denied");
        errorResponse.put("code", "ACCESS_DENIED");
        errorResponse.put("message", "权限不足，无法访问该资源");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        // 开发环境下添加详细信息
        if (isDevelopmentMode()) {
            errorResponse.put("detail", exception.getMessage());
            errorResponse.put("exceptionType", exception.getClass().getSimpleName());
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * 处理Web页面请求的访问拒绝
     */
    private void handleWebAccessDenied(HttpServletRequest request, 
                                     HttpServletResponse response,
                                     AccessDeniedException exception) throws IOException, ServletException {
        
        // 设置错误信息到请求属性中
        request.setAttribute("errorCode", "ACCESS_DENIED");
        request.setAttribute("errorMessage", "权限不足，无法访问该页面");
        request.setAttribute("requestUrl", request.getRequestURI());
        request.setAttribute("timestamp", System.currentTimeMillis());
        
        // 开发环境下添加详细信息
        if (isDevelopmentMode()) {
            request.setAttribute("exceptionDetail", exception.getMessage());
            request.setAttribute("exceptionType", exception.getClass().getSimpleName());
        }
        
        // 重定向到403错误页面
        response.sendRedirect("/error/403");
    }
    
    /**
     * 记录访问拒绝的审计日志
     */
    private void recordAccessDeniedAudit(HttpServletRequest request, AccessDeniedException exception) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = getCurrentUserId(authentication);
            String username = getCurrentUsername(authentication);
            
            String requestUrl = request.getRequestURI();
            String httpMethod = request.getMethod();
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // 构建审计详情
            Map<String, Object> details = new HashMap<>();
            details.put("requestUrl", requestUrl);
            details.put("httpMethod", httpMethod);
            details.put("clientIp", clientIp);
            details.put("userAgent", userAgent);
            details.put("exceptionMessage", exception.getMessage());
            details.put("exceptionType", exception.getClass().getSimpleName());
            details.put("isCsrfException", isCsrfException(exception));
            
            // 记录审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.ACCESS_DENIED,
                username,
                "访问拒绝: " + requestUrl + " - " + exception.getMessage()
            );
            
        } catch (Exception e) {
            // 审计日志记录失败不应影响正常的异常处理流程
            log.error("记录访问拒绝审计日志失败", e);
        }
    }
    
    /**
     * 判断是否为CSRF异常
     */
    private boolean isCsrfException(Exception exception) {
        return exception instanceof CsrfException ||
               exception.getCause() instanceof CsrfException ||
               exception instanceof CustomCsrfException ||
               exception.getCause() instanceof CustomCsrfException ||
               (exception.getMessage() != null && 
                exception.getMessage().toLowerCase().contains("csrf"));
    }
    
    /**
     * 判断是否为API请求
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        String xRequestedWith = request.getHeader("X-Requested-With");
        
        // 判断条件：
        // 1. URL路径包含 /api/
        // 2. Accept头包含 application/json
        // 3. Content-Type包含 application/json
        // 4. X-Requested-With为XMLHttpRequest（Ajax请求）
        return (requestURI != null && requestURI.startsWith("/api/")) ||
               (acceptHeader != null && acceptHeader.contains("application/json")) ||
               (contentType != null && contentType.contains("application/json")) ||
               "XMLHttpRequest".equals(xRequestedWith);
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal) {
            return ((CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal()).getUserId();
        }
        return null;
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        return "anonymous";
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
    
    /**
     * 判断是否为开发模式
     */
    private boolean isDevelopmentMode() {
        String profile = System.getProperty("spring.profiles.active");
        return "dev".equals(profile) || "development".equals(profile);
    }
}