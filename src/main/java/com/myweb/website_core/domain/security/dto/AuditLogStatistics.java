package com.myweb.website_core.domain.security.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志统计数据DTO
 * 
 * 用于封装审计日志的统计分析结果，包括：
 * - 基础统计数据（总数、成功/失败数等）
 * - 分组统计数据（按操作、用户、IP等维度）
 * - 时间趋势数据（按小时、天等时间维度）
 * - 安全分析数据（风险级别、异常检测等）
 * 
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogStatistics {
    
    // ==================== 基础统计数据 ====================
    
    /**
     * 统计时间范围
     */
    private TimeRange timeRange;
    
    /**
     * 总操作数
     */
    private Long totalOperations;
    
    /**
     * 成功操作数
     */
    private Long successOperations;
    
    /**
     * 失败操作数
     */
    private Long failureOperations;
    
    /**
     * 错误操作数
     */
    private Long errorOperations;
    
    /**
     * 安全事件数
     */
    private Long securityEvents;
    
    /**
     * 高风险操作数
     */
    private Long highRiskOperations;
    
    /**
     * 未处理事件数
     */
    private Long unprocessedEvents;
    
    /**
     * 活跃用户数
     */
    private Long activeUsers;
    
    /**
     * 活跃IP数
     */
    private Long activeIPs;
    
    /**
     * 平均执行时间（毫秒）
     */
    private Double averageExecutionTime;
    
    /**
     * 最大执行时间（毫秒）
     */
    private Long maxExecutionTime;
    
    // ==================== 分组统计数据 ====================
    
    /**
     * 按操作类型统计
     */
    private List<OperationStatistic> operationStatistics;
    
    /**
     * 按用户统计
     */
    private List<UserStatistic> userStatistics;
    
    /**
     * 按IP地址统计
     */
    private List<IpStatistic> ipStatistics;
    
    /**
     * 按资源类型统计
     */
    private List<ResourceStatistic> resourceStatistics;
    
    /**
     * 按风险级别统计
     */
    private List<RiskLevelStatistic> riskLevelStatistics;
    
    // ==================== 时间趋势数据 ====================
    
    /**
     * 按小时统计
     */
    private List<HourlyStatistic> hourlyStatistics;
    
    /**
     * 按天统计
     */
    private List<DailyStatistic> dailyStatistics;
    
    /**
     * 按周统计
     */
    private List<WeeklyStatistic> weeklyStatistics;
    
    // ==================== 安全分析数据 ====================
    
    /**
     * 异常登录统计
     */
    private AbnormalLoginStatistic abnormalLoginStatistic;
    
    /**
     * 可疑活动统计
     */
    private SuspiciousActivityStatistic suspiciousActivityStatistic;
    
    /**
     * 性能异常统计
     */
    private PerformanceAnomalyStatistic performanceAnomalyStatistic;
    
    // ==================== 内部类定义 ====================
    
    /**
     * 时间范围
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRange {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String description;
    }
    
    /**
     * 操作类型统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationStatistic {
        private String operation;
        private String operationName;
        private Long count;
        private Long successCount;
        private Long failureCount;
        private Double percentage;
        private Double averageExecutionTime;
    }
    
    /**
     * 用户统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatistic {
        private Long userId;
        private String username;
        private Long operationCount;
        private Long successCount;
        private Long failureCount;
        private LocalDateTime lastActivity;
        private String mostFrequentOperation;
    }
    
    /**
     * IP地址统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpStatistic {
        private String ipAddress;
        private String location;
        private Long operationCount;
        private Long successCount;
        private Long failureCount;
        private Long uniqueUsers;
        private LocalDateTime firstSeen;
        private LocalDateTime lastSeen;
        private Boolean suspicious;
    }
    
    /**
     * 资源类型统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceStatistic {
        private String resourceType;
        private Long operationCount;
        private Long createCount;
        private Long updateCount;
        private Long deleteCount;
        private Long viewCount;
    }
    
    /**
     * 风险级别统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskLevelStatistic {
        private Integer riskLevel;
        private String riskLevelName;
        private Long count;
        private Double percentage;
    }
    
    /**
     * 小时统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyStatistic {
        private String hour;
        private Long operationCount;
        private Long successCount;
        private Long failureCount;
        private Double averageExecutionTime;
    }
    
    /**
     * 日统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatistic {
        private String date;
        private Long operationCount;
        private Long successCount;
        private Long failureCount;
        private Long uniqueUsers;
        private Long uniqueIPs;
    }
    
    /**
     * 周统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyStatistic {
        private String week;
        private Long operationCount;
        private Long successCount;
        private Long failureCount;
        private Long uniqueUsers;
        private Long uniqueIPs;
    }
    
    /**
     * 异常登录统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbnormalLoginStatistic {
        private Long totalAbnormalLogins;
        private Long differentLocationLogins;
        private Long unusualTimeLogins;
        private Long multipleFailureLogins;
        private List<AbnormalLoginDetail> details;
    }
    
    /**
     * 异常登录详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbnormalLoginDetail {
        private String username;
        private String ipAddress;
        private String location;
        private LocalDateTime timestamp;
        private String reason;
        private Integer riskLevel;
    }
    
    /**
     * 可疑活动统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuspiciousActivityStatistic {
        private Long totalSuspiciousActivities;
        private Long bruteForceAttempts;
        private Long sqlInjectionAttempts;
        private Long xssAttempts;
        private Long unauthorizedAccess;
        private List<SuspiciousActivityDetail> details;
    }
    
    /**
     * 可疑活动详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuspiciousActivityDetail {
        private String activityType;
        private String ipAddress;
        private String username;
        private LocalDateTime timestamp;
        private String description;
        private Integer riskLevel;
        private Boolean processed;
    }
    
    /**
     * 性能异常统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceAnomalyStatistic {
        private Long totalSlowOperations;
        private Double averageSlowOperationTime;
        private Long maxSlowOperationTime;
        private String slowestOperation;
        private List<SlowOperationDetail> details;
    }
    
    /**
     * 慢操作详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlowOperationDetail {
        private String operation;
        private String username;
        private Long executionTime;
        private LocalDateTime timestamp;
        private String description;
    }
    
    // ==================== 业务方法 ====================
    
    /**
     * 计算成功率
     * 
     * @return 成功率（百分比）
     */
    public Double getSuccessRate() {
        if (totalOperations == null || totalOperations == 0) {
            return 0.0;
        }
        return (successOperations != null ? successOperations : 0) * 100.0 / totalOperations;
    }
    
    /**
     * 计算失败率
     * 
     * @return 失败率（百分比）
     */
    public Double getFailureRate() {
        if (totalOperations == null || totalOperations == 0) {
            return 0.0;
        }
        return (failureOperations != null ? failureOperations : 0) * 100.0 / totalOperations;
    }
    
    /**
     * 计算安全事件率
     * 
     * @return 安全事件率（百分比）
     */
    public Double getSecurityEventRate() {
        if (totalOperations == null || totalOperations == 0) {
            return 0.0;
        }
        return (securityEvents != null ? securityEvents : 0) * 100.0 / totalOperations;
    }
    
    /**
     * 计算高风险操作率
     * 
     * @return 高风险操作率（百分比）
     */
    public Double getHighRiskRate() {
        if (totalOperations == null || totalOperations == 0) {
            return 0.0;
        }
        return (highRiskOperations != null ? highRiskOperations : 0) * 100.0 / totalOperations;
    }
    
    /**
     * 获取安全状态评级
     * 
     * @return 安全状态评级（EXCELLENT/GOOD/FAIR/POOR/CRITICAL）
     */
    public String getSecurityRating() {
        double failureRate = getFailureRate();
        double securityEventRate = getSecurityEventRate();
        double highRiskRate = getHighRiskRate();
        
        // 综合评分
        double score = 100.0 - (failureRate * 0.4 + securityEventRate * 0.4 + highRiskRate * 0.2);
        
        if (score >= 95) return "EXCELLENT";
        if (score >= 85) return "GOOD";
        if (score >= 70) return "FAIR";
        if (score >= 50) return "POOR";
        return "CRITICAL";
    }
    
    /**
     * 检查是否有未处理的安全事件
     * 
     * @return 是否有未处理的安全事件
     */
    public boolean hasUnprocessedSecurityEvents() {
        return unprocessedEvents != null && unprocessedEvents > 0;
    }
    
    /**
     * 检查是否有异常活动
     * 
     * @return 是否有异常活动
     */
    public boolean hasAbnormalActivities() {
        return (abnormalLoginStatistic != null && abnormalLoginStatistic.getTotalAbnormalLogins() > 0) ||
               (suspiciousActivityStatistic != null && suspiciousActivityStatistic.getTotalSuspiciousActivities() > 0);
    }
    
    /**
     * 检查是否有性能问题
     * 
     * @return 是否有性能问题
     */
    public boolean hasPerformanceIssues() {
        return performanceAnomalyStatistic != null && 
               performanceAnomalyStatistic.getTotalSlowOperations() > 0;
    }
    
    /**
     * 获取统计摘要
     * 
     * @return 统计摘要
     */
    public Map<String, Object> getSummary() {
        return Map.of(
            "totalOperations", totalOperations != null ? totalOperations : 0,
            "successRate", String.format("%.2f%%", getSuccessRate()),
            "failureRate", String.format("%.2f%%", getFailureRate()),
            "securityEventRate", String.format("%.2f%%", getSecurityEventRate()),
            "securityRating", getSecurityRating(),
            "hasUnprocessedEvents", hasUnprocessedSecurityEvents(),
            "hasAbnormalActivities", hasAbnormalActivities(),
            "hasPerformanceIssues", hasPerformanceIssues()
        );
    }
    
    @Override
    public String toString() {
        return String.format("AuditLogStatistics{totalOperations=%d, successRate=%.2f%%, failureRate=%.2f%%, securityRating=%s}", 
                           totalOperations != null ? totalOperations : 0, 
                           getSuccessRate(), 
                           getFailureRate(), 
                           getSecurityRating());
    }
}