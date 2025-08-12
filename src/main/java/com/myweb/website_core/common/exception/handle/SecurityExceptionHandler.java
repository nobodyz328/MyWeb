package com.myweb.website_core.common.exception.handle;

import com.myweb.website_core.application.service.security.SecurityExceptionMaskingService;
import com.myweb.website_core.application.service.security.SecurityExceptionStatisticsService;
import com.myweb.website_core.common.exception.*;
import com.myweb.website_core.common.exception.security.*;
import com.myweb.website_core.common.util.SecurityEventUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一安全异常处理器
 * <p>
 * 扩展现有的GlobalExceptionHandler，提供统一的安全异常处理功能，包括：
 * - 所有安全相关异常的统一处理
 * - 异常信息的安全脱敏
 * - 异常统计和监控
 * - 统一的安全错误响应格式
 * - 安全事件记录
 * <p>
 * 符合需求：1.6, 2.6, 3.4, 4.6 - 统一异常处理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@ControllerAdvice
@Order(1) // 优先级高于GlobalExceptionHandler
@RequiredArgsConstructor
public class SecurityExceptionHandler {
    
    private final SecurityExceptionMaskingService maskingService;
    private final SecurityExceptionStatisticsService statisticsService;
    
    // ==================== 输入验证异常处理 ====================
    
    /**
     * 处理输入验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<SecurityErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.inputValidationError(
                "输入验证失败：" + ex.getMessage(),
                buildValidationDetails(ex),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId());
        
        // 添加验证相关的错误数据
        Map<String, Object> errorData = new HashMap<>();
        if (ex.getField() != null) {
            errorData.put("field", ex.getField());
        }
        if (ex.getErrorCode() != null) {
            errorData.put("validationErrorCode", ex.getErrorCode());
        }
        errorResponse.withErrorData(errorData);
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 输入验证异常 - 字段: {}, 用户: {}, IP: {}, 错误: {}", 
                ex.getField(), username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(maskedResponse);
    }
    
    // ==================== 认证异常处理 ====================
    
    /**
     * 处理认证异常
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<SecurityErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.authenticationError(
                "身份认证失败：" + ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId());
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录安全事件
        recordAuthenticationFailureEvent(username, ex.getMessage());
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 认证异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(maskedResponse);
    }
    
    /**
     * 处理账户锁定异常
     */
    @ExceptionHandler({AccountLockedException.class, LockedException.class})
    public ResponseEntity<SecurityErrorResponse> handleAccountLockedException(
            Exception ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.authenticationError(
                "账户已被锁定：" + ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId())
          .withSuggestedAction("请联系管理员解锁账户或等待锁定时间结束");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 账户锁定异常 - 用户: {}, IP: {}", username, ipAddress);
        
        return ResponseEntity.status(HttpStatus.LOCKED).body(maskedResponse);
    }
    
    /**
     * 处理账户禁用异常
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<SecurityErrorResponse> handleDisabledException(
            DisabledException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.authenticationError(
                "账户已被禁用：" + ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId())
          .withSuggestedAction("请联系管理员启用账户");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 账户禁用异常 - 用户: {}, IP: {}", username, ipAddress);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(maskedResponse);
    }
    
    /**
     * 处理令牌异常
     */
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<SecurityErrorResponse> handleTokenException(
            TokenException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.authenticationError(
                "令牌验证失败：" + ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId())
          .withSuggestedAction("请重新登录获取新的访问令牌");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 令牌异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(maskedResponse);
    }
    
    // ==================== 授权异常处理 ====================
    
    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler({AuthorizationException.class, AccessDeniedException.class})
    public ResponseEntity<SecurityErrorResponse> handleAuthorizationException(
            Exception ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.authorizationError(
                "访问被拒绝：" + ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId());
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录安全事件
        recordAccessDeniedEvent(path, username);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 授权异常 - 用户: {}, IP: {}, 路径: {}", 
                username, ipAddress, path);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(maskedResponse);
    }
    
    // ==================== 数据完整性异常处理 ====================
    
    /**
     * 处理数据完整性异常
     */
    @ExceptionHandler(DataIntegrityException.class)
    public ResponseEntity<SecurityErrorResponse> handleDataIntegrityException(
            DataIntegrityException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.dataIntegrityError(
                "数据完整性验证失败",
                ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId());
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录安全事件
        recordDataIntegrityViolationEvent(ex.getMessage());
        
        // 记录日志
        log.error("SecurityExceptionHandler: 数据完整性异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(maskedResponse);
    }
    
    /**
     * 处理文件完整性异常
     */
    @ExceptionHandler(FileIntegrityException.class)
    public ResponseEntity<SecurityErrorResponse> handleFileIntegrityException(
            FileIntegrityException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.genericSecurityError(
                "FILE_INTEGRITY_ERROR",
                "文件完整性验证失败",
                SecurityErrorResponse.SecurityErrorCategory.FILE_SECURITY,
                SecurityErrorResponse.SecurityErrorSeverity.CRITICAL,
                path,
                HttpStatus.CONFLICT.value()
        ).withRequestId(SecurityEventUtils.generateRequestId())
         .withSuggestedAction("请联系系统管理员检查文件完整性");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.error("SecurityExceptionHandler: 文件完整性异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(maskedResponse);
    }
    
    // ==================== 文件安全异常处理 ====================
    
    /**
     * 处理文件上传异常
     */
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<SecurityErrorResponse> handleFileUploadException(
            FileUploadException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.fileSecurityError(
                "文件上传失败：" + ex.getMessage(),
                ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId());
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 文件上传异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(maskedResponse);
    }
    
    /**
     * 处理文件验证异常
     */
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<SecurityErrorResponse> handleFileValidationException(
            FileValidationException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.fileSecurityError(
                "文件验证失败：" + ex.getMessage(),
                ex.getMessage(),
                path
        ).withRequestId(SecurityEventUtils.generateRequestId());
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 文件验证异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(maskedResponse);
    }
    
    // ==================== 其他安全异常处理 ====================
    
    /**
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<SecurityErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.genericSecurityError(
                "RATE_LIMIT_EXCEEDED",
                "访问频率超限：" + ex.getMessage(),
                SecurityErrorResponse.SecurityErrorCategory.ACCESS_CONTROL,
                SecurityErrorResponse.SecurityErrorSeverity.MEDIUM,
                path,
                HttpStatus.TOO_MANY_REQUESTS.value()
        ).withRequestId(SecurityEventUtils.generateRequestId())
         .withSuggestedAction("请稍后再试");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 限流异常 - 用户: {}, IP: {}", username, ipAddress);
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(maskedResponse);
    }
    
    /**
     * 处理验证码异常
     */
    @ExceptionHandler(CaptchaRequiredException.class)
    public ResponseEntity<SecurityErrorResponse> handleCaptchaRequiredException(
            CaptchaRequiredException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.genericSecurityError(
                "CAPTCHA_REQUIRED",
                "需要验证码：" + ex.getMessage(),
                SecurityErrorResponse.SecurityErrorCategory.AUTHENTICATION,
                SecurityErrorResponse.SecurityErrorSeverity.MEDIUM,
                path,
                HttpStatus.BAD_REQUEST.value()
        ).withRequestId(SecurityEventUtils.generateRequestId())
         .withSuggestedAction("请完成验证码验证");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.info("SecurityExceptionHandler: 验证码异常 - 用户: {}, IP: {}", username, ipAddress);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(maskedResponse);
    }
    
    /**
     * 处理搜索异常
     */
    @ExceptionHandler(SearchException.class)
    public ResponseEntity<SecurityErrorResponse> handleSearchException(
            SearchException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.genericSecurityError(
                "SEARCH_ERROR",
                "搜索异常：" + ex.getMessage(),
                SecurityErrorResponse.SecurityErrorCategory.SQL_INJECTION_PROTECTION,
                SecurityErrorResponse.SecurityErrorSeverity.MEDIUM,
                path,
                HttpStatus.BAD_REQUEST.value()
        ).withRequestId(SecurityEventUtils.generateRequestId())
         .withSuggestedAction("请检查搜索关键词");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.warn("SecurityExceptionHandler: 搜索异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(maskedResponse);
    }
    
    // ==================== 通用安全异常处理 ====================
    
    /**
     * 处理通用安全异常
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<SecurityErrorResponse> handleSecurityException(
            SecurityException ex, WebRequest request) {
        
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.genericSecurityError(
                "SECURITY_ERROR",
                "安全异常：" + ex.getMessage(),
                SecurityErrorResponse.SecurityErrorCategory.SYSTEM_SECURITY,
                SecurityErrorResponse.SecurityErrorSeverity.HIGH,
                path,
                HttpStatus.FORBIDDEN.value()
        ).withRequestId(SecurityEventUtils.generateRequestId());
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, ex, username, ipAddress);
        
        // 记录日志
        log.error("SecurityExceptionHandler: 通用安全异常 - 用户: {}, IP: {}, 错误: {}", 
                username, ipAddress, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(maskedResponse);
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取请求路径
     * 
     * @param request Web请求
     * @return 请求路径
     */
    private String getRequestPath(WebRequest request) {
        try {
            return request.getDescription(false).replace("uri=", "");
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 构建验证详情
     * 
     * @param ex 验证异常
     * @return 验证详情
     */
    private String buildValidationDetails(ValidationException ex) {
        StringBuilder details = new StringBuilder();
        details.append("验证失败");
        
        if (ex.getField() != null) {
            details.append(" - 字段: ").append(ex.getField());
        }
        
        if (ex.getErrorCode() != null) {
            details.append(" - 错误代码: ").append(ex.getErrorCode());
        }
        
        if (ex.getValue() != null) {
            details.append(" - 值: ").append(maskingService.maskByCurrentUserPermission(
                SecurityErrorResponse.builder()
                    .details(ex.getValue().toString())
                    .build()
            ).getDetails());
        }
        
        return details.toString();
    }
    
    /**
     * 记录认证失败事件
     * 
     * @param username 用户名
     * @param reason 失败原因
     */
    private void recordAuthenticationFailureEvent(String username, String reason) {
        try {
            SecurityEventUtils.createLoginFailureEvent(username, reason);
        } catch (Exception e) {
            log.debug("SecurityExceptionHandler: 记录认证失败事件时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 记录访问拒绝事件
     * 
     * @param resource 资源路径
     * @param username 用户名
     */
    private void recordAccessDeniedEvent(String resource, String username) {
        try {
            SecurityEventUtils.createAccessDeniedEvent(resource, "访问");
        } catch (Exception e) {
            log.debug("SecurityExceptionHandler: 记录访问拒绝事件时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 记录数据完整性违规事件
     * 
     * @param details 详情
     */
    private void recordDataIntegrityViolationEvent(String details) {
        try {
            SecurityEventUtils.createDataIntegrityViolationEvent("未知", "未知", details);
        } catch (Exception e) {
            log.debug("SecurityExceptionHandler: 记录数据完整性事件时发生异常: {}", e.getMessage());
        }
    }
}