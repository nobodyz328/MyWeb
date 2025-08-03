package com.myweb.website_core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话统计信息DTO
 * 
 * 用于存储和传输系统会话统计数据，包括：
 * - 在线用户统计
 * - 会话活动统计
 * - 设备和浏览器分布
 * - 地理位置分布
 * 
 * 符合GB/T 22239-2019安全审计要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatistics {
    
    /**
     * 当前在线用户总数
     */
    private Long totalOnlineUsers;
    
    /**
     * 当前活跃会话总数
     */
    private Long totalActiveSessions;
    
    /**
     * 今日登录用户数
     */
    private Long todayLoginUsers;
    
    /**
     * 今日新增会话数
     */
    private Long todayNewSessions;
    
    /**
     * 平均会话持续时间（分钟）
     */
    private Double averageSessionDuration;
    
    /**
     * 最长会话持续时间（分钟）
     */
    private Long maxSessionDuration;
    
    /**
     * 按角色分组的在线用户数
     * Key: 角色名称, Value: 用户数量
     */
    private Map<String, Long> usersByRole;
    
    /**
     * 按设备类型分组的会话数
     * Key: 设备类型, Value: 会话数量
     */
    private Map<String, Long> sessionsByDevice;
    
    /**
     * 按浏览器类型分组的会话数
     * Key: 浏览器类型, Value: 会话数量
     */
    private Map<String, Long> sessionsByBrowser;
    
    /**
     * 按操作系统分组的会话数
     * Key: 操作系统, Value: 会话数量
     */
    private Map<String, Long> sessionsByOS;
    
    /**
     * 按小时分组的登录数量（过去24小时）
     * Key: 小时（0-23）, Value: 登录数量
     */
    private Map<Integer, Long> loginsByHour;
    
    /**
     * 最近活跃的IP地址列表
     */
    private Map<String, Long> recentActiveIPs;
    
    /**
     * 统计生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 统计数据有效期（分钟）
     */
    private Integer validityMinutes;
    
    /**
     * 检查统计数据是否过期
     * 
     * @return 统计数据是否过期
     */
    public boolean isExpired() {
        if (generatedAt == null || validityMinutes == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(generatedAt.plusMinutes(validityMinutes));
    }
    
    /**
     * 获取用户活跃度（在线用户数/今日登录用户数）
     * 
     * @return 用户活跃度百分比
     */
    public Double getUserActivityRate() {
        if (todayLoginUsers == null || todayLoginUsers == 0) {
            return 0.0;
        }
        return (totalOnlineUsers != null ? totalOnlineUsers.doubleValue() : 0.0) / todayLoginUsers * 100;
    }
    
    /**
     * 获取会话保持率（活跃会话数/今日新增会话数）
     * 
     * @return 会话保持率百分比
     */
    public Double getSessionRetentionRate() {
        if (todayNewSessions == null || todayNewSessions == 0) {
            return 0.0;
        }
        return (totalActiveSessions != null ? totalActiveSessions.doubleValue() : 0.0) / todayNewSessions * 100;
    }
}