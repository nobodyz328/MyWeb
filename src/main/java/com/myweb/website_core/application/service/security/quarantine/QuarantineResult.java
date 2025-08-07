package com.myweb.website_core.application.service.security.quarantine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 隔离操作结果
 * <p>
 * 封装文件隔离操作的结果信息
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarantineResult {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 隔离ID
     */
    private String quarantineId;
    
    /**
     * 隔离文件路径
     */
    private String quarantinePath;
    
    /**
     * 元数据文件路径
     */
    private String metadataPath;
    
    /**
     * 文件哈希值
     */
    private String fileHash;
    
    /**
     * 隔离时间
     */
    private LocalDateTime quarantineTime;
    
    /**
     * 处理耗时（毫秒）
     */
    private long processingTimeMs;
    
    /**
     * 结果消息
     */
    private String message;
    
    /**
     * 错误消息
     */
    private String errorMessage;
}