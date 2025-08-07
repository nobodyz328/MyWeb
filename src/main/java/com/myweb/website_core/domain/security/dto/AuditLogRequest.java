package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.common.enums.AuditOperation;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志记录请求DTO
 * 
 * 用于封装创建审计日志的请求参数，支持：
 * - 操作信息记录
 * - 用户和会话信息
 * - 网络和设备信息
 * - 性能和结果信息
 * 
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {
    
    // ==================== 用户相关信息 ====================
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    // ==================== 操作相关信息 ====================
    
    /**
     * 操作类型（必填）
     */
    private AuditOperation operation;
    
    /**
     * 资源类型
     */
    private String resourceType;
    
    /**
     * 资源ID
     */
    private Long resourceId;
    
    /**
     * 操作描述
     */
    private String description;
    
    // ==================== 网络相关信息 ====================
    
    /**
     * 客户端IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理字符串
     */
    private String userAgent;
    
    /**
     * 地理位置信息
     */
    private String location;
    
    /**
     * 设备指纹
     */
    private String deviceFingerprint;
    
    // ==================== 会话相关信息 ====================
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    // ==================== 结果相关信息 ====================
    
    /**
     * 操作结果（SUCCESS/FAILURE/ERROR）
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    // ==================== 数据相关信息 ====================
    
    /**
     * 请求数据（JSON格式）
     */
    private Object requestData;
    
    /**
     * 响应数据（JSON格式）
     */
    private Object responseData;
    
    // ==================== 安全相关信息 ====================
    
    /**
     * 风险级别（1-5）
     */
    private Integer riskLevel;
    
    /**
     * 标签（逗号分隔）
     */
    private String tags;
    
    // ==================== 时间信息 ====================
    
    /**
     * 操作时间戳
     */
    private LocalDateTime timestamp;
    
    // ==================== 构建器方法 ====================
    
    /**
     * 创建成功操作的审计日志请求
     * 
     * @param operation 操作类型
     * @param userId 用户ID
     * @param username 用户名
     * @return 审计日志请求
     */
    public static AuditLogRequest success(AuditOperation operation, Long userId, String username) {
        return AuditLogRequest.builder()
                .operation(operation)
                .userId(userId)
                .username(username)
                .result("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败操作的审计日志请求
     * 
     * @param operation 操作类型
     * @param userId 用户ID
     * @param username 用户名
     * @param errorMessage 错误信息
     * @return 审计日志请求
     */
    public static AuditLogRequest failure(AuditOperation operation, Long userId, String username, String errorMessage) {
        return AuditLogRequest.builder()
                .operation(operation)
                .userId(userId)
                .username(username)
                .result("FAILURE")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建错误操作的审计日志请求
     * 
     * @param operation 操作类型
     * @param userId 用户ID
     * @param username 用户名
     * @param errorMessage 错误信息
     * @return 审计日志请求
     */
    public static AuditLogRequest error(AuditOperation operation, Long userId, String username, String errorMessage) {
        return AuditLogRequest.builder()
                .operation(operation)
                .userId(userId)
                .username(username)
                .result("ERROR")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建系统操作的审计日志请求
     * 
     * @param operation 操作类型
     * @param description 操作描述
     * @return 审计日志请求
     */
    public static AuditLogRequest system(AuditOperation operation, String description) {
        return AuditLogRequest.builder()
                .operation(operation)
                .resourceType("SYSTEM")
                .description(description)
                .result("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建登录操作的审计日志请求
     * 
     * @param success 是否成功
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param errorMessage 错误信息（失败时）
     * @return 审计日志请求
     */
    public static AuditLogRequest login(boolean success, Long userId, String username, 
                                       String ipAddress, String userAgent, String errorMessage) {
        AuditLogRequestBuilder builder = AuditLogRequest.builder()
                .operation(success ? AuditOperation.USER_LOGIN_SUCCESS : AuditOperation.USER_LOGIN_FAILURE)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result(success ? "SUCCESS" : "FAILURE")
                .timestamp(LocalDateTime.now());
        
        if (!success && errorMessage != null) {
            builder.errorMessage(errorMessage);
        }
        
        return builder.build();
    }
    
    /**
     * 创建资源操作的审计日志请求
     * 
     * @param operation 操作类型
     * @param userId 用户ID
     * @param username 用户名
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 审计日志请求
     */
    public static AuditLogRequest resource(AuditOperation operation, Long userId, String username,
                                          String resourceType, Long resourceId) {
        return AuditLogRequest.builder()
                .operation(operation)
                .userId(userId)
                .username(username)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .result("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建安全事件的审计日志请求
     * 
     * @param operation 操作类型
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param description 事件描述
     * @param riskLevel 风险级别
     * @return 审计日志请求
     */
    public static AuditLogRequest securityEvent(AuditOperation operation, Long userId, String username,
                                               String ipAddress, String description, Integer riskLevel) {
        return AuditLogRequest.builder()
                .operation(operation)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .description(description)
                .riskLevel(riskLevel)
                .result("FAILURE")
                .tags("security,event")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // ==================== 链式设置方法 ====================
    
    /**
     * 设置网络信息
     * 
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @return 当前对象
     */
    public AuditLogRequest withNetworkInfo(String ipAddress, String userAgent) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        return this;
    }
    
    /**
     * 设置会话信息
     * 
     * @param sessionId 会话ID
     * @param requestId 请求ID
     * @return 当前对象
     */
    public AuditLogRequest withSessionInfo(String sessionId, String requestId) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        return this;
    }
    
    /**
     * 设置资源信息
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 当前对象
     */
    public AuditLogRequest withResource(String resourceType, Long resourceId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        return this;
    }
    
    /**
     * 设置性能信息
     * 
     * @param executionTime 执行时间
     * @return 当前对象
     */
    public AuditLogRequest withPerformance(Long executionTime) {
        this.executionTime = executionTime;
        return this;
    }
    
    /**
     * 设置数据信息
     * 
     * @param requestData 请求数据
     * @param responseData 响应数据
     * @return 当前对象
     */
    public AuditLogRequest withData(Object requestData, Object responseData) {
        this.requestData = requestData;
        this.responseData = responseData;
        return this;
    }
    
    /**
     * 设置安全信息
     * 
     * @param riskLevel 风险级别
     * @param tags 标签
     * @return 当前对象
     */
    public AuditLogRequest withSecurity(Integer riskLevel, String tags) {
        this.riskLevel = riskLevel;
        this.tags = tags;
        return this;
    }
    

    /**
     * 添加标签
     * 
     * @param tag 标签
     * @return 当前对象
     */
    public AuditLogRequest addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return this;
        }
        
        if (this.tags == null || this.tags.isEmpty()) {
            this.tags = tag.trim();
        } else if (!this.tags.contains(tag.trim())) {
            this.tags += "," + tag.trim();
        }
        return this;
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证请求参数的有效性
     * 
     * @return 验证结果
     */
    public boolean isValid() {
        return operation != null && result != null && !result.trim().isEmpty();
    }
    
    /**
     * 获取验证错误信息
     * 
     * @return 验证错误信息
     */
    public String getValidationError() {
        if (operation == null) {
            return "操作类型不能为空";
        }
        if (result == null || result.trim().isEmpty()) {
            return "操作结果不能为空";
        }
        return null;
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 检查是否为成功操作
     * 
     * @return 是否为成功操作
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(result);
    }
    
    /**
     * 检查是否为失败操作
     * 
     * @return 是否为失败操作
     */
    public boolean isFailure() {
        return "FAILURE".equals(result);
    }
    
    /**
     * 检查是否为错误操作
     * 
     * @return 是否为错误操作
     */
    public boolean isError() {
        return "ERROR".equals(result);
    }
    
    /**
     * 检查是否为高风险操作
     * 
     * @return 是否为高风险操作
     */
    public boolean isHighRisk() {
        return riskLevel != null && riskLevel >= 4;
    }
    
    /**
     * 检查是否为安全相关操作
     * 
     * @return 是否为安全相关操作
     */
    public boolean isSecurityOperation() {
        return operation != null && operation.isSecurityOperation();
    }
    
    /**
     * 检查是否包含敏感数据
     * 
     * @return 是否包含敏感数据
     */
    public boolean hasSensitiveData() {
        return requestData != null || responseData != null;
    }
    
    /**
     * 获取操作的显示名称
     * 
     * @return 操作的显示名称
     */
    public String getOperationDisplayName() {
        return operation != null ? operation.getName() : "未知操作";
    }
    
    /**
     * 获取风险级别的显示字符串
     * 
     * @return 风险级别的显示字符串
     */
    public String getRiskLevelDisplay() {
        if (riskLevel == null) {
            return "未知";
        }
        
        switch (riskLevel) {
            case 1: return "最低";
            case 2: return "低";
            case 3: return "中";
            case 4: return "高";
            case 5: return "最高";
            default: return "未知";
        }
    }
    
    @Override
    public String toString() {
        return String.format("AuditLogRequest{operation=%s, username='%s', result='%s', timestamp=%s}", 
                           operation, username, result, 
                           timestamp != null ? timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "null");
    }
}