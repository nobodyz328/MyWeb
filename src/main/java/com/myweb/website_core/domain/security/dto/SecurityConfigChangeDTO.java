package com.myweb.website_core.domain.security.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全配置变更DTO
 * 
 * 用于记录和传递配置变更事件信息
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Data
public class SecurityConfigChangeDTO {
    
    /**
     * 配置类型
     */
    private String configType;
    
    /**
     * 旧配置（JSON格式）
     */
    private String oldConfig;
    
    /**
     * 新配置（JSON格式）
     */
    private String newConfig;
    
    /**
     * 操作者
     */
    private String operator;
    
    /**
     * 变更时间
     */
    private LocalDateTime changeTime;
    
    /**
     * 变更原因
     */
    private String reason;
    
    /**
     * 变更类型（CREATE, UPDATE, DELETE, RESET）
     */
    private String changeType;
    
    /**
     * 变更结果（SUCCESS, FAILURE, ERROR）
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
}