package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.common.enums.SecurityEventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 安全事件请求DTO
 * <p>
 * 用于创建安全事件的请求数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEventRequest {
    
    /**
     * 事件类型
     */
    private SecurityEventType eventType;
    
    /**
     * 事件标题
     */
    private String title;
    
    /**
     * 事件描述
     */
    private String description;
    
    /**
     * 相关用户ID
     */
    private Long userId;
    
    /**
     * 相关用户名
     */
    private String username;
    
    /**
     * 源IP地址
     */
    private String sourceIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 请求URI
     */
    private String requestUri;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 事件详细数据
     */
    private Map<String, Object> eventData;
    
    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;
    
    /**
     * 风险评分（0-100）
     */
    private Integer riskScore;
    
    /**
     * 创建简单的安全事件请求
     */
    public static SecurityEventRequest simple(SecurityEventType eventType, String description) {
        return SecurityEventRequest.builder()
                .eventType(eventType)
                .title(eventType.getName())
                .description(description)
                .eventTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建带用户信息的安全事件请求
     */
    public static SecurityEventRequest withUser(SecurityEventType eventType, String description, 
                                              Long userId, String username) {
        return SecurityEventRequest.builder()
                .eventType(eventType)
                .title(eventType.getName())
                .description(description)
                .userId(userId)
                .username(username)
                .eventTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建带网络信息的安全事件请求
     */
    public static SecurityEventRequest withNetwork(SecurityEventType eventType, String description,
                                                 String sourceIp, String userAgent, String requestUri) {
        return SecurityEventRequest.builder()
                .eventType(eventType)
                .title(eventType.getName())
                .description(description)
                .sourceIp(sourceIp)
                .userAgent(userAgent)
                .requestUri(requestUri)
                .eventTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建完整的安全事件请求
     */
    public static SecurityEventRequest full(SecurityEventType eventType, String description,
                                          Long userId, String username, String sourceIp, 
                                          String userAgent, String requestUri, String requestMethod,
                                          String sessionId, Map<String, Object> eventData) {
        return SecurityEventRequest.builder()
                .eventType(eventType)
                .title(eventType.getName())
                .description(description)
                .userId(userId)
                .username(username)
                .sourceIp(sourceIp)
                .userAgent(userAgent)
                .requestUri(requestUri)
                .requestMethod(requestMethod)
                .sessionId(sessionId)
                .eventData(eventData)
                .eventTime(LocalDateTime.now())
                .build();
    }
}