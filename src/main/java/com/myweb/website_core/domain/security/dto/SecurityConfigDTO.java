package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.common.config.BackupProperties;
import com.myweb.website_core.common.config.JwtProperties;
import com.myweb.website_core.common.config.RateLimitProperties;
import com.myweb.website_core.common.config.SecurityProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全配置DTO
 * 
 * 包含所有安全相关配置的统一数据传输对象
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Data
public class SecurityConfigDTO {
    
    /**
     * 安全属性配置
     */
    private SecurityProperties securityProperties;
    
    /**
     * JWT属性配置
     */
    private JwtProperties jwtProperties;
    
    /**
     * 访问频率限制配置
     */
    private RateLimitProperties rateLimitProperties;
    
    /**
     * 备份属性配置
     */
    private BackupProperties backupProperties;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 配置版本号
     */
    private String version;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 是否启用
     */
    private Boolean enabled = true;
}