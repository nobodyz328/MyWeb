package com.myweb.website_core.domain.security.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全配置备份DTO
 * 
 * 用于配置备份和恢复功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Data
public class SecurityConfigBackupDTO {
    
    /**
     * 备份ID
     */
    private String backupId;
    
    /**
     * 配置类型
     */
    private String configType;
    
    /**
     * 配置数据（JSON格式）
     */
    private String configData;
    
    /**
     * 备份时间
     */
    private LocalDateTime backupTime;
    
    /**
     * 操作者
     */
    private String operator;
    
    /**
     * 备份原因
     */
    private String reason;
    
    /**
     * 备份类型（MANUAL, AUTO, SCHEDULED）
     */
    private String backupType = "MANUAL";
    
    /**
     * 备份描述
     */
    private String description;
    
    /**
     * 配置版本
     */
    private String configVersion;
    
    /**
     * 备份文件路径
     */
    private String filePath;
    
    /**
     * 备份文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 校验和
     */
    private String checksum;
    
    /**
     * 是否已验证
     */
    private Boolean verified = false;
}