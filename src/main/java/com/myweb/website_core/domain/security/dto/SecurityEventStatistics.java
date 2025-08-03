package com.myweb.website_core.domain.security.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 安全事件统计DTO
 * 
 * 用于安全事件的统计分析数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEventStatistics {
    
    /**
     * 统计时间范围开始
     */
    private LocalDateTime startTime;
    
    /**
     * 统计时间范围结束
     */
    private LocalDateTime endTime;
    
    /**
     * 总事件数量
     */
    private Long totalEvents;
    
    /**
     * 高危事件数量
     */
    private Long highRiskEvents;
    
    /**
     * 中等风险事件数量
     */
    private Long mediumRiskEvents;
    
    /**
     * 低风险事件数量
     */
    private Long lowRiskEvents;
    
    /**
     * 未处理事件数量
     */
    private Long unhandledEvents;
    
    /**
     * 已告警事件数量
     */
    private Long alertedEvents;
    
    /**
     * 按事件类型统计
     */
    private Map<String, Long> eventTypeStats;
    
    /**
     * 按严重级别统计
     */
    private Map<Integer, Long> severityStats;
    
    /**
     * 按状态统计
     */
    private Map<String, Long> statusStats;
    
    /**
     * 按小时统计（24小时）
     */
    private Map<Integer, Long> hourlyStats;
    
    /**
     * 按日期统计（最近7天）
     */
    private Map<String, Long> dailyStats;
    
    /**
     * 按IP地址统计（Top 10）
     */
    private Map<String, Long> ipStats;
    
    /**
     * 按用户统计（Top 10）
     */
    private Map<String, Long> userStats;
    
    /**
     * 平均风险评分
     */
    private Double averageRiskScore;
    
    /**
     * 最高风险评分
     */
    private Integer maxRiskScore;
    
    /**
     * 事件处理率（已处理/总数）
     */
    private Double handlingRate;
    
    /**
     * 告警率（已告警/总数）
     */
    private Double alertRate;
    
    /**
     * 趋势分析（相比上一周期的变化百分比）
     */
    private Double trendPercentage;
    
    /**
     * 计算风险等级分布百分比
     */
    public Map<String, Double> getRiskDistribution() {
        if (totalEvents == 0) {
            return Map.of(
                "high", 0.0,
                "medium", 0.0,
                "low", 0.0
            );
        }
        
        return Map.of(
            "high", (highRiskEvents * 100.0) / totalEvents,
            "medium", (mediumRiskEvents * 100.0) / totalEvents,
            "low", (lowRiskEvents * 100.0) / totalEvents
        );
    }
    
    /**
     * 获取安全状态评级
     */
    public String getSecurityRating() {
        if (totalEvents == 0) {
            return "EXCELLENT";
        }
        
        double highRiskRatio = (highRiskEvents * 100.0) / totalEvents;
        double unhandledRatio = (unhandledEvents * 100.0) / totalEvents;
        
        if (highRiskRatio > 20 || unhandledRatio > 30) {
            return "CRITICAL";
        } else if (highRiskRatio > 10 || unhandledRatio > 20) {
            return "WARNING";
        } else if (highRiskRatio > 5 || unhandledRatio > 10) {
            return "ATTENTION";
        } else {
            return "GOOD";
        }
    }
    
    /**
     * 检查是否需要紧急关注
     */
    public boolean requiresUrgentAttention() {
        return "CRITICAL".equals(getSecurityRating()) || 
               (unhandledEvents != null && unhandledEvents > 50) ||
               (highRiskEvents != null && highRiskEvents > 20);
    }
}