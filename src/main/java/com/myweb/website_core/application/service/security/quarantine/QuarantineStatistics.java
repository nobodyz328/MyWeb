package com.myweb.website_core.application.service.security.quarantine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 隔离统计信息
 * <p>
 * 提供隔离区的统计数据
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarantineStatistics {
    
    /**
     * 隔离文件总数
     */
    private long totalFiles;
    
    /**
     * 隔离文件总大小（字节）
     */
    private long totalSizeBytes;
    
    /**
     * 隔离目录路径
     */
    private String quarantinePath;
    
    /**
     * 保留天数
     */
    private int retentionDays;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
    
    /**
     * 创建空的统计信息
     */
    public static QuarantineStatistics empty() {
        return QuarantineStatistics.builder()
            .totalFiles(0)
            .totalSizeBytes(0)
            .quarantinePath("N/A")
            .retentionDays(0)
            .lastUpdated(LocalDateTime.now())
            .build();
    }
    
    /**
     * 获取格式化的文件大小
     */
    public String getFormattedSize() {
        if (totalSizeBytes < 1024) {
            return totalSizeBytes + " B";
        } else if (totalSizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", totalSizeBytes / 1024.0);
        } else if (totalSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", totalSizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", totalSizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}