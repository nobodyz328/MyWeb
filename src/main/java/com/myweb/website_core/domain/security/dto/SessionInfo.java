package com.myweb.website_core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话信息DTO
 * 
 * 用于存储和传输用户会话相关信息，包括：
 * - 会话标识和用户信息
 * - 登录时间和最后活动时间
 * - 客户端信息（IP地址、用户代理）
 * - 会话状态和过期时间
 * 
 * 符合GB/T 22239-2019身份鉴别和剩余信息保护要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户角色
     */
    private String role;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 最后活动时间
     */
    private LocalDateTime lastActivityTime;
    
    /**
     * 客户端IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理字符串
     */
    private String userAgent;
    
    /**
     * 会话是否活跃
     */
    private Boolean active;
    
    /**
     * 会话过期时间
     */
    private LocalDateTime expirationTime;
    
    /**
     * JWT访问令牌
     */
    private String accessToken;
    
    /**
     * JWT刷新令牌
     */
    private String refreshToken;
    
    /**
     * 设备类型（Web、Mobile、API等）
     */
    private String deviceType;
    
    /**
     * 浏览器类型
     */
    private String browserType;
    
    /**
     * 操作系统类型
     */
    private String osType;
    
    /**
     * 检查会话是否过期
     * 
     * @return 会话是否过期
     */
    public boolean isExpired() {
        if (expirationTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expirationTime);
    }
    
    /**
     * 检查会话是否超时（30分钟无活动）
     * 
     * @return 会话是否超时
     */
    public boolean isTimeout() {
        if (lastActivityTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(lastActivityTime.plusMinutes(30));
    }
    
    /**
     * 更新最后活动时间
     */
    public void updateLastActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }
    
    /**
     * 获取会话持续时间（分钟）
     * 
     * @return 会话持续时间
     */
    public long getSessionDurationMinutes() {
        if (loginTime == null) {
            return 0;
        }
        LocalDateTime endTime = lastActivityTime != null ? lastActivityTime : LocalDateTime.now();
        return java.time.Duration.between(loginTime, endTime).toMinutes();
    }
}