package com.myweb.website_core.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一安全错误响应格式
 * <p>
 * 提供统一的安全异常响应格式，包括：
 * - 错误基本信息
 * - 安全相关的错误代码和分类
 * - 脱敏后的错误详情
 * - 请求追踪信息
 * <p>
 * 符合需求：1.6, 2.6, 3.4, 4.6 - 统一异常处理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityErrorResponse {
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 错误消息（用户友好的消息）
     */
    private String message;
    
    /**
     * 错误详情（技术详情，可能被脱敏）
     */
    private String details;
    
    /**
     * 错误分类
     */
    private SecurityErrorCategory category;
    
    /**
     * 错误严重级别
     */
    private SecurityErrorSeverity severity;
    
    /**
     * 发生时间
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 请求路径
     */
    private String path;
    
    /**
     * HTTP状态码
     */
    private Integer status;
    
    /**
     * 请求ID（用于追踪）
     */
    private String requestId;
    
    /**
     * 是否需要用户操作
     */
    @Builder.Default
    private boolean requiresUserAction = false;
    
    /**
     * 建议的用户操作
     */
    private String suggestedAction;
    
    /**
     * 额外的错误数据（已脱敏）
     */
    private Map<String, Object> errorData;
    
    /**
     * 是否为安全相关错误
     */
    @Builder.Default
    private boolean securityRelated = true;
    
    /**
     * 错误分类枚举
     */
    public enum SecurityErrorCategory {
        /**
         * 输入验证错误
         */
        INPUT_VALIDATION("输入验证"),
        
        /**
         * 身份认证错误
         */
        AUTHENTICATION("身份认证"),
        
        /**
         * 权限授权错误
         */
        AUTHORIZATION("权限授权"),
        
        /**
         * 数据完整性错误
         */
        DATA_INTEGRITY("数据完整性"),
        
        /**
         * 文件安全错误
         */
        FILE_SECURITY("文件安全"),
        
        /**
         * XSS防护错误
         */
        XSS_PROTECTION("XSS防护"),
        
        /**
         * SQL注入防护错误
         */
        SQL_INJECTION_PROTECTION("SQL注入防护"),
        
        /**
         * 会话管理错误
         */
        SESSION_MANAGEMENT("会话管理"),
        
        /**
         * 访问控制错误
         */
        ACCESS_CONTROL("访问控制"),
        
        /**
         * 系统安全错误
         */
        SYSTEM_SECURITY("系统安全"),
        
        /**
         * 其他安全错误
         */
        OTHER("其他");
        
        private final String displayName;
        
        SecurityErrorCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 错误严重级别枚举
     */
    public enum SecurityErrorSeverity {
        /**
         * 低级别 - 一般性错误
         */
        LOW("低", 1),
        
        /**
         * 中级别 - 需要关注的错误
         */
        MEDIUM("中", 2),
        
        /**
         * 高级别 - 严重安全错误
         */
        HIGH("高", 3),
        
        /**
         * 严重级别 - 紧急安全事件
         */
        CRITICAL("严重", 4);
        
        private final String displayName;
        private final int level;
        
        SecurityErrorSeverity(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建输入验证错误响应
     * 
     * @param message 错误消息
     * @param details 错误详情
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse inputValidationError(String message, String details, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("INPUT_VALIDATION_ERROR")
                .message(message)
                .details(details)
                .category(SecurityErrorCategory.INPUT_VALIDATION)
                .severity(SecurityErrorSeverity.MEDIUM)
                .path(path)
                .status(400)
                .requiresUserAction(true)
                .suggestedAction("请检查输入数据的格式和内容")
                .build();
    }
    
    /**
     * 创建认证错误响应
     * 
     * @param message 错误消息
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse authenticationError(String message, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("AUTHENTICATION_ERROR")
                .message(message)
                .category(SecurityErrorCategory.AUTHENTICATION)
                .severity(SecurityErrorSeverity.HIGH)
                .path(path)
                .status(401)
                .requiresUserAction(true)
                .suggestedAction("请重新登录或检查认证信息")
                .build();
    }
    
    /**
     * 创建授权错误响应
     * 
     * @param message 错误消息
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse authorizationError(String message, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("AUTHORIZATION_ERROR")
                .message(message)
                .category(SecurityErrorCategory.AUTHORIZATION)
                .severity(SecurityErrorSeverity.HIGH)
                .path(path)
                .status(403)
                .requiresUserAction(true)
                .suggestedAction("请联系管理员获取相应权限")
                .build();
    }
    
    /**
     * 创建数据完整性错误响应
     * 
     * @param message 错误消息
     * @param details 错误详情
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse dataIntegrityError(String message, String details, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("DATA_INTEGRITY_ERROR")
                .message(message)
                .details(details)
                .category(SecurityErrorCategory.DATA_INTEGRITY)
                .severity(SecurityErrorSeverity.CRITICAL)
                .path(path)
                .status(409)
                .requiresUserAction(false)
                .suggestedAction("请联系系统管理员")
                .build();
    }
    
    /**
     * 创建文件上传安全错误响应
     * 
     * @param message 错误消息
     * @param details 错误详情
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse fileSecurityError(String message, String details, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("FILE_SECURITY_ERROR")
                .message(message)
                .details(details)
                .category(SecurityErrorCategory.FILE_SECURITY)
                .severity(SecurityErrorSeverity.HIGH)
                .path(path)
                .status(400)
                .requiresUserAction(true)
                .suggestedAction("请上传安全的文件类型")
                .build();
    }
    
    /**
     * 创建XSS攻击错误响应
     * 
     * @param message 错误消息
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse xssProtectionError(String message, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("XSS_PROTECTION_ERROR")
                .message(message)
                .category(SecurityErrorCategory.XSS_PROTECTION)
                .severity(SecurityErrorSeverity.HIGH)
                .path(path)
                .status(400)
                .requiresUserAction(true)
                .suggestedAction("请检查输入内容，避免使用可疑的脚本代码")
                .build();
    }
    
    /**
     * 创建SQL注入防护错误响应
     * 
     * @param message 错误消息
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse sqlInjectionError(String message, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("SQL_INJECTION_ERROR")
                .message(message)
                .category(SecurityErrorCategory.SQL_INJECTION_PROTECTION)
                .severity(SecurityErrorSeverity.CRITICAL)
                .path(path)
                .status(400)
                .requiresUserAction(true)
                .suggestedAction("请检查查询参数，避免使用SQL关键字")
                .build();
    }
    
    /**
     * 创建会话管理错误响应
     * 
     * @param message 错误消息
     * @param path 请求路径
     * @return 错误响应
     */
    public static SecurityErrorResponse sessionError(String message, String path) {
        return SecurityErrorResponse.builder()
                .errorCode("SESSION_ERROR")
                .message(message)
                .category(SecurityErrorCategory.SESSION_MANAGEMENT)
                .severity(SecurityErrorSeverity.MEDIUM)
                .path(path)
                .status(401)
                .requiresUserAction(true)
                .suggestedAction("请重新登录")
                .build();
    }
    
    /**
     * 创建通用安全错误响应
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param category 错误分类
     * @param severity 严重级别
     * @param path 请求路径
     * @param status HTTP状态码
     * @return 错误响应
     */
    public static SecurityErrorResponse genericSecurityError(String errorCode, String message, 
                                                           SecurityErrorCategory category, 
                                                           SecurityErrorSeverity severity,
                                                           String path, Integer status) {
        return SecurityErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .category(category)
                .severity(severity)
                .path(path)
                .status(status)
                .build();
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 设置请求追踪信息
     * 
     * @param requestId 请求ID
     * @return 当前对象
     */
    public SecurityErrorResponse withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
    
    /**
     * 设置错误数据
     * 
     * @param errorData 错误数据
     * @return 当前对象
     */
    public SecurityErrorResponse withErrorData(Map<String, Object> errorData) {
        this.errorData = errorData;
        return this;
    }
    
    /**
     * 设置建议操作
     * 
     * @param suggestedAction 建议操作
     * @return 当前对象
     */
    public SecurityErrorResponse withSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
        return this;
    }
    
    /**
     * 检查是否为高严重级别错误
     * 
     * @return 是否为高严重级别
     */
    public boolean isHighSeverity() {
        return severity == SecurityErrorSeverity.HIGH || severity == SecurityErrorSeverity.CRITICAL;
    }
    
    /**
     * 检查是否需要立即处理
     * 
     * @return 是否需要立即处理
     */
    public boolean requiresImmediateAttention() {
        return severity == SecurityErrorSeverity.CRITICAL;
    }
    
    /**
     * 获取错误的唯一标识
     * 
     * @return 错误唯一标识
     */
    public String getErrorId() {
        return errorCode + "_" + timestamp.toString().replace(":", "").replace("-", "");
    }
}