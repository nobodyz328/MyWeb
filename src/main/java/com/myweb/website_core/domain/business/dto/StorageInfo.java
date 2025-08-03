package com.myweb.website_core.domain.business.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 存储空间信息DTO
 * 
 * 用于表示文件系统存储空间的使用情况
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
public class StorageInfo {
    
    /**
     * 存储路径
     */
    private String path;
    
    /**
     * 总空间（字节）
     */
    private long totalSpace;
    
    /**
     * 可用空间（字节）
     */
    private long freeSpace;
    
    /**
     * 已用空间（字节）
     */
    private long usedSpace;
    
    /**
     * 获取总空间（MB）
     * 
     * @return 总空间MB数
     */
    public long getTotalSpaceMB() {
        return totalSpace / (1024 * 1024);
    }
    
    /**
     * 获取可用空间（MB）
     * 
     * @return 可用空间MB数
     */
    public long getFreeSpaceMB() {
        return freeSpace / (1024 * 1024);
    }
    
    /**
     * 获取已用空间（MB）
     * 
     * @return 已用空间MB数
     */
    public long getUsedSpaceMB() {
        return usedSpace / (1024 * 1024);
    }
    
    /**
     * 获取总空间（GB）
     * 
     * @return 总空间GB数
     */
    public double getTotalSpaceGB() {
        return totalSpace / (1024.0 * 1024.0 * 1024.0);
    }
    
    /**
     * 获取可用空间（GB）
     * 
     * @return 可用空间GB数
     */
    public double getFreeSpaceGB() {
        return freeSpace / (1024.0 * 1024.0 * 1024.0);
    }
    
    /**
     * 获取已用空间（GB）
     * 
     * @return 已用空间GB数
     */
    public double getUsedSpaceGB() {
        return usedSpace / (1024.0 * 1024.0 * 1024.0);
    }
    
    /**
     * 获取使用率百分比
     * 
     * @return 使用率百分比
     */
    public double getUsagePercentage() {
        if (totalSpace == 0) {
            return 0.0;
        }
        return (double) usedSpace / totalSpace * 100.0;
    }
    
    /**
     * 检查是否空间不足
     * 
     * @param threshold 阈值百分比
     * @return 是否空间不足
     */
    public boolean isSpaceInsufficient(double threshold) {
        return getUsagePercentage() >= threshold;
    }
    
    /**
     * 获取格式化的存储信息字符串
     * 
     * @return 格式化字符串
     */
    public String getFormattedInfo() {
        return String.format("存储路径: %s, 总空间: %.2fGB, 已用: %.2fGB (%.1f%%), 可用: %.2fGB",
                path, getTotalSpaceGB(), getUsedSpaceGB(), getUsagePercentage(), getFreeSpaceGB());
    }
}