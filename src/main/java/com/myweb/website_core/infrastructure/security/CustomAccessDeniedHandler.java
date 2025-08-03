package com.myweb.website_core.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义访问拒绝处理器
 * 
 * 处理权限不足时的统一异常响应，包括：
 * - 记录访问拒绝的审计日志
 * - 返回统一的错误响应格式
 * - 区分Web页面和API请求的响应方式
 * 
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理访问拒绝异常
     * 
     * @param request 请求对象
     * @param response 响应对象
     * @param accessDeniedException 访问拒绝异常
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        // 记录访问拒绝的审计日志
        recordAccessDeniedAudit(request, accessDeniedException);
        
        // 判断是API请求还是Web页面请求
        if (isApiRequest(request)) {
            handleApiAccessDenied(request, response, accessDeniedException);
        } else {
            handleWebAccessDenied(request, response, accessDeniedException);
        }
    }
    
    /**
     * 记录访问拒绝的审计日志
     * 
     * @param request 请求对象
     * @param exception 访问拒绝异常
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
            
            // 记录审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.ACCESS_DENIED,
                username,
                userId+userAgent+clientIp
            );
            
        } catch (Exception e) {
            // 审计日志记录失败不应影响正常的异常处理流程
            // 可以记录到系统日志中
            System.err.println("记录访问拒绝审计日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理API请求的访问拒绝
     * 
     * @param request 请求对象
     * @param response 响应对象
     * @param exception 访问拒绝异常
     * @throws IOException IO异常
     */
    private void handleApiAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                     AccessDeniedException exception) throws IOException {
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("code", "ACCESS_DENIED");
        errorResponse.put("message", "权限不足，无法访问该资源");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        // 在开发环境下可以包含更详细的错误信息
        if (isDevelopmentMode()) {
            errorResponse.put("detail", exception.getMessage());
            errorResponse.put("exceptionType", exception.getClass().getSimpleName());
        }
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * 处理Web页面请求的访问拒绝
     * 
     * @param request 请求对象
     * @param response 响应对象
     * @param exception 访问拒绝异常
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    private void handleWebAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                     AccessDeniedException exception) throws IOException, ServletException {
        
        // 设置错误信息到请求属性中，供错误页面使用
        request.setAttribute("errorCode", "ACCESS_DENIED");
        request.setAttribute("errorMessage", "权限不足，无法访问该页面");
        request.setAttribute("requestUrl", request.getRequestURI());
        request.setAttribute("timestamp", System.currentTimeMillis());
        
        // 在开发环境下可以包含更详细的错误信息
        if (isDevelopmentMode()) {
            request.setAttribute("exceptionDetail", exception.getMessage());
            request.setAttribute("exceptionType", exception.getClass().getSimpleName());
        }
        
        // 重定向到错误页面
        response.sendRedirect("/error/403");
    }
    
    /**
     * 判断是否为API请求
     * 
     * @param request 请求对象
     * @return 是否为API请求
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        
        // 判断条件：
        // 1. URL路径包含 /api/
        // 2. Accept头包含 application/json
        // 3. Content-Type包含 application/json
        return (requestURI != null && requestURI.startsWith("/api/")) ||
               (acceptHeader != null && acceptHeader.contains("application/json")) ||
               (contentType != null && contentType.contains("application/json"));
    }
    
    /**
     * 获取当前用户ID
     * 
     * @param authentication 认证信息
     * @return 用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal) {
            return ((CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal()).getUserId();
        }
        return null;
    }
    
    /**
     * 获取当前用户名
     * 
     * @param authentication 认证信息
     * @return 用户名
     */
    private String getCurrentUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        return "anonymous";
    }
    
    /**
     * 获取客户端IP地址
     * 
     * @param request 请求对象
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
    
    /**
     * 判断是否为开发模式
     * 
     * @return 是否为开发模式
     */
    private boolean isDevelopmentMode() {
        // 可以通过系统属性或环境变量来判断
        String profile = System.getProperty("spring.profiles.active");
        return "dev".equals(profile) || "development".equals(profile);
    }
}