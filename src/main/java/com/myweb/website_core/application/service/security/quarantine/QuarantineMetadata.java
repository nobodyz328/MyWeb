package com.myweb.website_core.application.service.security.quarantine;

import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusScanResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 隔离文件元数据
 * <p>
 * 存储隔离文件的详细信息和上下文
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarantineMetadata {
    
    /**
     * 隔离ID
     */
    private String quarantineId;
    
    /**
     * 原始文件名
     */
    private String originalFilename;
    
    /**
     * 文件大小
     */
    private long fileSize;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 文件哈希值
     */
    private String fileHash;
    
    /**
     * 病毒名称
     */
    private String virusName;
    
    /**
     * 威胁级别
     */
    private VirusScanResult.ThreatLevel threatLevel;
    
    /**
     * 扫描引擎
     */
    private String scanEngine;
    
    /**
     * 扫描时间
     */
    private LocalDateTime scanTime;
    
    /**
     * 隔离时间
     */
    private LocalDateTime quarantineTime;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 扫描详情
     */
    private String scanDetails;
    
    /**
     * 建议操作
     */
    private String recommendedAction;
}