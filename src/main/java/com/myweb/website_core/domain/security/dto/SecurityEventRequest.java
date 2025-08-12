package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.common.util.SecurityEventUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

import static com.myweb.website_core.common.util.SecurityEventUtils.getIpAddress;

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
    @Builder.Default
    private Long userId=SecurityEventUtils.getUserId();
    
    /**
     * 相关用户名
     */
    @Builder.Default
    private String username = SecurityEventUtils.getUsername();
    
    /**
     * 源IP地址
     */
    @Builder.Default
    private String sourceIp=SecurityEventUtils.getIpAddress();
    
    /**
     * 用户代理
     */
    @Builder.Default
    private String userAgent=SecurityEventUtils.getUserAgent();
    /**
     * 请求URI
     */
    @Builder.Default
    private String requestUri=SecurityEventUtils.getRequestUri();
    
    /**
     * 请求方法
     */
    @Builder.Default
    private String requestMethod= SecurityEventUtils.getRequestMethod();
    
    /**
     * 会话ID
     */
    @Builder.Default
    private String sessionId=SecurityEventUtils.getSessionId();

    /**
     * 事件发生时间
     */
    @Builder.Default
    private LocalDateTime eventTime=LocalDateTime.now();

    /**
     * 事件详细数据
     */
    private Map<String, Object> eventData;

    /**
     * 风险评分（0-100）
     */
    private Integer riskScore;

    /**
     * 创建带用户信息的安全事件请求
     */
    public SecurityEventRequest withInfo(){
        this.username= SecurityEventUtils.getUsername();
        this.sourceIp= getIpAddress();
        this.userAgent=getUserAgent();
        this.requestUri=getRequestUri();
        this.requestMethod=getRequestMethod();
        this.sessionId=getSessionId();
        this.eventTime=LocalDateTime.now();
        return this;
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