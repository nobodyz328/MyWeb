package com.myweb.website_core.domain.security.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一安全消息DTO
 * 
 * 统一了以下DTO的功能，避免重复：
 * - AuditLogRequest
 * - SecurityAuditMessage
 * - SecurityEvent相关字段
 * 
 * 用于在RabbitMQ中传输所有安全相关的消息
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter@Getter
public class UnifiedSecurityMessage {

    // ==================== 消息分类信息 ====================
    
    /**
     * 消息类型：AUDIT_LOG, SECURITY_EVENT, USER_AUTH, FILE_UPLOAD, SEARCH, ACCESS_CONTROL
     */
    private String messageType;
    
    /**
     * 是否为安全事件
     */
    private Boolean isSecurityEvent;
    
    /**
     * 安全事件类型（当isSecurityEvent为true时使用）
     */
    private SecurityEventType securityEventType;
    
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
     * 审计操作类型
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
    
    /**
     * 事件标题（安全事件专用）
     */
    private String title;
    
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
    
    /**
     * 请求URI
     */
    private String requestUri;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
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
     * 操作结果（SUCCESS/FAILURE/ERROR/DETECTED）
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
     * 请求数据
     */
    private String requestData;
    
    /**
     * 响应数据
     */
    private String responseData;
    
    /**
     * 事件详细数据（JSON格式）
     */
    private String eventData;
    
    /**
     * 额外的上下文信息
     */
    private Map<String, Object> context;
    
    // ==================== 安全相关信息 ====================
    
    /**
     * 风险级别（1-5）
     */
    private Integer riskLevel;
    
    /**
     * 严重级别（1-5，安全事件专用）
     */
    private Integer severity;
    
    /**
     * 风险评分（0-100，安全事件专用）
     */
    private Integer riskScore;
    
    /**
     * 标签（逗号分隔）
     */
    private String tags;
    
    // ==================== 时间信息 ====================
    
    /**
     * 操作时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 事件发生时间（安全事件专用）
     */
    private LocalDateTime eventTime;
    
    // ==================== 处理状态信息 ====================
    
    /**
     * 事件状态（NEW, PROCESSING, RESOLVED, IGNORED）
     */
    private String status;
    
    /**
     * 是否已告警
     */
    private Boolean alerted;
    
    /**
     * 告警时间
     */
    private LocalDateTime alertTime;
    
    /**
     * 相关事件数量
     */
    private Integer relatedEventCount;
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建审计日志消息
     */
    public static UnifiedSecurityMessage auditLog(AuditOperation operation, Long userId, String username, 
                                                 String result, String ipAddress) {
        return UnifiedSecurityMessage.builder()
                .messageType("AUDIT_LOG")
                .isSecurityEvent(false)
                .operation(operation)
                .userId(userId)
                .username(username)
                .result(result)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .riskLevel(operation != null ? operation.getRiskLevel() : 1)
                .build();
    }
    
    /**
     * 创建安全事件消息
     */
    public static UnifiedSecurityMessage securityEvent(SecurityEventType eventType, AuditOperation operation,
                                                      Long userId, String username, String ipAddress,
                                                      String description, Integer severity) {
        return UnifiedSecurityMessage.builder()
                .messageType("SECURITY_EVENT")
                .isSecurityEvent(true)
                .securityEventType(eventType)
                .operation(operation)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .description(description)
                .title(eventType != null ? eventType.getDescription() : "安全事件")
                .severity(severity)
                .riskLevel(severity)
                .result("DETECTED")
                .status("NEW")
                .alerted(false)
                .timestamp(LocalDateTime.now())
                .eventTime(LocalDateTime.now())
                .tags("security,event")
                .build();
    }
    
    /**
     * 创建用户认证消息
     */
    public static UnifiedSecurityMessage userAuth(String username, AuditOperation operation, String ipAddress, 
                                                 String result, String errorMessage, String sessionId) {
        return UnifiedSecurityMessage.builder()
                .messageType("USER_AUTH")
                .isSecurityEvent(false)
                .username(username)
                .operation(operation)
                .resourceType("USER")
                .ipAddress(ipAddress)
                .result(result)
                .errorMessage(errorMessage)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .riskLevel(operation != null ? operation.getRiskLevel() : 3)
                .build();
    }
    
    /**
     * 创建文件上传消息
     */
    public static UnifiedSecurityMessage fileUpload(Long userId, String username, String fileName, 
                                                   String fileType, Long fileSize, String result, 
                                                   String ipAddress, String errorMessage) {
        return UnifiedSecurityMessage.builder()
                .messageType("FILE_UPLOAD")
                .isSecurityEvent(false)
                .userId(userId)
                .username(username)
                .operation(AuditOperation.FILE_UPLOAD)
                .resourceType("FILE")
                .ipAddress(ipAddress)
                .result(result)
                .errorMessage(errorMessage)
                .description("文件上传: " + fileName + " (" + fileType + ", " + fileSize + " bytes)")
                .timestamp(LocalDateTime.now())
                .riskLevel(3)
                .build();
    }
    
    /**
     * 创建搜索操作消息
     */
    public static UnifiedSecurityMessage search(Long userId, String username, String searchQuery, 
                                               String searchType, Integer resultCount, String ipAddress) {
        return UnifiedSecurityMessage.builder()
                .messageType("SEARCH")
                .isSecurityEvent(false)
                .userId(userId)
                .username(username)
                .operation(AuditOperation.SEARCH_OPERATION)
                .resourceType("SEARCH")
                .ipAddress(ipAddress)
                .result("SUCCESS")
                .description("搜索操作: " + searchQuery + " (类型: " + searchType + ", 结果数: " + resultCount + ")")
                .timestamp(LocalDateTime.now())
                .riskLevel(1)
                .build();
    }
    
    /**
     * 创建访问控制消息
     */
    public static UnifiedSecurityMessage accessControl(Long userId, String username, String resourceType, 
                                                      Long resourceId, String action, String result, 
                                                      String ipAddress, String reason) {
        return UnifiedSecurityMessage.builder()
                .messageType("ACCESS_CONTROL")
                .isSecurityEvent("DENIED".equals(result))
                .userId(userId)
                .username(username)
                .operation("DENIED".equals(result) ? AuditOperation.ACCESS_DENIED : AuditOperation.POST_VIEW)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .result(result)
                .errorMessage(reason)
                .description("访问控制检查: " + action + " on " + resourceType + "#" + resourceId)
                .timestamp(LocalDateTime.now())
                .riskLevel("DENIED".equals(result) ? 4 : 1)
                .build();
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 检查是否为成功操作
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(result);
    }
    
    /**
     * 检查是否为失败操作
     */
    public boolean isFailure() {
        return "FAILURE".equals(result);
    }
    
    /**
     * 检查是否为高风险操作
     */
    public boolean isHighRisk() {
        return (riskLevel != null && riskLevel >= 4) || (severity != null && severity >= 4);
    }
    
    /**
     * 检查是否需要立即告警
     */
    public boolean requiresImmediateAlert() {
        return (severity != null && severity == 5) || 
               (Boolean.TRUE.equals(isSecurityEvent) && isHighRisk());
    }
    
    /**
     * 获取目标队列名称
     */
    public String getTargetQueue() {
        if (Boolean.TRUE.equals(isSecurityEvent)) {
            return "security.event.queue";
        }
        
        switch (messageType) {
            case "USER_AUTH":
                return "user.auth.queue";
            case "FILE_UPLOAD":
                return "file.upload.audit.queue";
            case "SEARCH":
                return "search.audit.queue";
            case "ACCESS_CONTROL":
                return "access.control.queue";
            default:
                return "security.audit.queue";
        }
    }
    
    /**
     * 获取路由键
     */
    public String getRoutingKey() {
        if (Boolean.TRUE.equals(isSecurityEvent)) {
            return "security.event";
        }
        
        switch (messageType) {
            case "USER_AUTH":
                return "user.auth";
            case "FILE_UPLOAD":
                return "file.upload.audit";
            case "SEARCH":
                return "search.audit";
            case "ACCESS_CONTROL":
                return "access.control";
            default:
                return "security.audit";
        }
    }
    
    /**
     * 添加标签
     */
    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        if (this.tags == null || this.tags.isEmpty()) {
            this.tags = tag.trim();
        } else if (!this.tags.contains(tag.trim())) {
            this.tags += "," + tag.trim();
        }
    }
    
    /**
     * 设置为安全事件
     */
    public void markAsSecurityEvent(SecurityEventType eventType, Integer severity) {
        this.isSecurityEvent = true;
        this.securityEventType = eventType;
        this.severity = severity;
        this.riskLevel = severity;
        this.result = "DETECTED";
        this.status = "NEW";
        this.alerted = false;
        this.eventTime = LocalDateTime.now();
        addTag("security");
        addTag("event");
    }
    
    @Override
    public String toString() {
        return String.format("UnifiedSecurityMessage{type=%s, operation=%s, username='%s', result='%s', isSecurityEvent=%s}", 
                           messageType, operation, username, result, isSecurityEvent);
    }
}