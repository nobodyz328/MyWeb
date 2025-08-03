package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.domain.business.dto.StorageInfo;
import lombok.Builder;
import lombok.Data;

/**
 * 日志存储统计信息DTO
 * 
 * 用于表示日志存储系统的整体统计信息
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
public class LogStorageStatistics {
    
    /**
     * 日志存储空间信息
     */
    private StorageInfo logStorage;
    
    /**
     * 备份存储空间信息
     */
    private StorageInfo backupStorage;
    
    /**
     * 日志文件数量
     */
    private long logFileCount;
    
    /**
     * 备份文件数量
     */
    private long backupFileCount;
    
    /**
     * 日志保留天数
     */
    private int retentionDays;
    
    /**
     * 备份保留天数
     */
    private int backupRetentionDays;
    
    /**
     * 是否启用压缩
     */
    private boolean compressionEnabled;
    
    /**
     * 是否启用完整性检查
     */
    private boolean integrityCheckEnabled;
    
    /**
     * 获取总存储使用量（MB）
     * 
     * @return 总存储使用量
     */
    public long getTotalUsedSpaceMB() {
        long logUsed = logStorage != null ? logStorage.getUsedSpaceMB() : 0;
        long backupUsed = backupStorage != null ? backupStorage.getUsedSpaceMB() : 0;
        return logUsed + backupUsed;
    }
    
    /**
     * 获取总存储容量（MB）
     * 
     * @return 总存储容量
     */
    public long getTotalCapacityMB() {
        long logTotal = logStorage != null ? logStorage.getTotalSpaceMB() : 0;
        long backupTotal = backupStorage != null ? backupStorage.getTotalSpaceMB() : 0;
        return logTotal + backupTotal;
    }
    
    /**
     * 获取总文件数量
     * 
     * @return 总文件数量
     */
    public long getTotalFileCount() {
        return logFileCount + backupFileCount;
    }
    
    /**
     * 获取整体使用率
     * 
     * @return 整体使用率百分比
     */
    public double getOverallUsagePercentage() {
        long totalCapacity = getTotalCapacityMB();
        if (totalCapacity == 0) {
            return 0.0;
        }
        return (double) getTotalUsedSpaceMB() / totalCapacity * 100.0;
    }
    
    /**
     * 检查是否需要清理
     * 
     * @param threshold 阈值百分比
     * @return 是否需要清理
     */
    public boolean needsCleanup(double threshold) {
        return getOverallUsagePercentage() >= threshold;
    }
    
    /**
     * 获取存储健康状态
     * 
     * @return 健康状态描述
     */
    public String getHealthStatus() {
        double usage = getOverallUsagePercentage();
        
        if (usage < 70) {
            return "良好";
        } else if (usage < 85) {
            return "正常";
        } else if (usage < 95) {
            return "警告";
        } else {
            return "严重";
        }
    }
    
    /**
     * 获取格式化的统计信息
     * 
     * @return 格式化字符串
     */
    public String getFormattedStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 日志存储统计信息 ===\n");
        
        if (logStorage != null) {
            sb.append("日志存储: ").append(logStorage.getFormattedInfo()).append("\n");
        }
        
        if (backupStorage != null) {
            sb.append("备份存储: ").append(backupStorage.getFormattedInfo()).append("\n");
        }
        
        sb.append("文件统计: 日志文件").append(logFileCount).append("个, 备份文件").append(backupFileCount).append("个\n");
        sb.append("保留策略: 日志").append(retentionDays).append("天, 备份").append(backupRetentionDays).append("天\n");
        sb.append("功能状态: 压缩").append(compressionEnabled ? "启用" : "禁用")
          .append(", 完整性检查").append(integrityCheckEnabled ? "启用" : "禁用").append("\n");
        sb.append("健康状态: ").append(getHealthStatus())
          .append(" (整体使用率: ").append(String.format("%.1f%%", getOverallUsagePercentage())).append(")");
        
        return sb.toString();
    }
}