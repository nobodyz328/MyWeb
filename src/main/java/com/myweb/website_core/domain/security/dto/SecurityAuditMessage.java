package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 安全审计消息DTO
 * 
 * 用于在RabbitMQ中传输安全审计相关的消息
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
public class SecurityAuditMessage {
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 操作类型
     */
    private AuditOperation operation;
    /**
     *安全类型
     */
    private SecurityEventType securityType;
    
    /**
     * 资源类型
     */
    private String resourceType;
    
    /**
     * 资源ID
     */
    private Long resourceId;
    
    /**
     * 客户端IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理字符串
     */
    private String userAgent;
    
    /**
     * 操作结果
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 请求数据
     */
    private String requestData;
    
    /**
     * 响应数据
     */
    private String responseData;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 操作时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 风险级别
     */
    private Integer riskLevel;
    
    /**
     * 地理位置信息
     */
    private String location;
    
    /**
     * 设备指纹
     */
    private String deviceFingerprint;
    
    /**
     * 标签
     */
    private String tags;
    
    /**
     * 额外的上下文信息
     */
    private Map<String, Object> context;
    
    /**
     * 创建成功的审计消息
     */
    public static SecurityAuditMessage success(Long userId, String username, AuditOperation operation, 
                                             String resourceType, Long resourceId, String ipAddress) {
        return SecurityAuditMessage.builder()
                .messageType("SECURITY_AUDIT")
                .userId(userId)
                .username(username)
                .operation(operation)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .result("SUCCESS")
                .timestamp(LocalDateTime.now())
                .riskLevel(operation != null ? operation.getRiskLevel() : 1)
                .build();
    }
    
    /**
     * 创建失败的审计消息
     */
    public static SecurityAuditMessage failure(Long userId, String username, AuditOperation operation, 
                                             String resourceType, Long resourceId, String ipAddress, 
                                             String errorMessage) {
        return SecurityAuditMessage.builder()
                .messageType("SECURITY_AUDIT")
                .userId(userId)
                .username(username)
                .operation(operation)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .result("FAILURE")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .riskLevel(operation != null ? operation.getRiskLevel() : 3)
                .build();
    }
    
    /**
     * 创建认证相关的审计消息
     */
    public static SecurityAuditMessage auth(String username, AuditOperation operation, String ipAddress, 
                                          String result, String errorMessage) {
        return SecurityAuditMessage.builder()
                .messageType("USER_AUTH")
                .username(username)
                .operation(operation)
                .resourceType("USER")
                .ipAddress(ipAddress)
                .result(result)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .riskLevel(operation != null ? operation.getRiskLevel() : 3)
                .build();
    }
    
    /**
     * 创建访问控制相关的审计消息
     */
    public static SecurityAuditMessage accessControl(Long userId, String username, String resourceType, 
                                                   Long resourceId, String action, String result, 
                                                   String ipAddress) {
        return SecurityAuditMessage.builder()
                .messageType("ACCESS_CONTROL")
                .userId(userId)
                .username(username)
                .operation(AuditOperation.ACCESS_DENIED)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .result(result)
                .description("访问控制检查: " + action)
                .timestamp(LocalDateTime.now())
                .riskLevel(3)
                .build();
    }
}