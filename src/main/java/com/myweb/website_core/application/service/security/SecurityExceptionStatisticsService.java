package com.myweb.website_core.application.service.security;

import com.myweb.website_core.common.exception.SecurityErrorResponse;
import com.myweb.website_core.common.util.SecurityEventUtils;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 安全异常统计监控服务
 * <p>
 * 提供安全异常的统计和监控功能，包括：
 * - 异常计数统计
 * - 异常趋势分析
 * - 异常告警触发
 * - 异常报告生成
 * <p>
 * 符合需求：1.6, 2.6, 3.4, 4.6 - 异常统计和监控
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityExceptionStatisticsService {
    
    // ==================== 配置属性 ====================
    
    /**
     * 异常统计功能开关
     */
    @Value("${app.security.exception-statistics.enabled:true}")
    private boolean statisticsEnabled;
    
    /**
     * 异常告警阈值
     */
    @Value("${app.security.exception-statistics.alert-threshold:10}")
    private int alertThreshold;
    
    /**
     * 统计时间窗口（分钟）
     */
    @Value("${app.security.exception-statistics.time-window-minutes:60}")
    private int timeWindowMinutes;
    
    /**
     * 是否启用实时告警
     */
    @Value("${app.security.exception-statistics.real-time-alert:true}")
    private boolean realTimeAlertEnabled;
    
    // ==================== 统计数据存储 ====================
    
    /**
     * 异常总计数器
     */
    private final AtomicLong totalExceptionCount = new AtomicLong(0);
    
    /**
     * 按错误代码分类的计数器
     */
    private final Map<String, AtomicLong> exceptionCountByCode = new ConcurrentHashMap<>();
    
    /**
     * 按错误分类的计数器
     */
    private final Map<SecurityErrorResponse.SecurityErrorCategory, AtomicLong> exceptionCountByCategory = new ConcurrentHashMap<>();
    
    /**
     * 按严重级别的计数器
     */
    private final Map<SecurityErrorResponse.SecurityErrorSeverity, AtomicLong> exceptionCountBySeverity = new ConcurrentHashMap<>();
    
    /**
     * 按用户的计数器
     */
    private final Map<String, AtomicLong> exceptionCountByUser = new ConcurrentHashMap<>();
    
    /**
     * 按IP地址的计数器
     */
    private final Map<String, AtomicLong> exceptionCountByIp = new ConcurrentHashMap<>();
    
    /**
     * 时间窗口内的异常记录
     */
    private final Map<String, ExceptionRecord> recentExceptions = new ConcurrentHashMap<>();
    
    /**
     * 最后告警时间
     */
    private volatile LocalDateTime lastAlertTime = LocalDateTime.now().minusHours(1);
    
    // ==================== 核心统计方法 ====================
    
    /**
     * 记录安全异常
     * 
     * @param errorResponse 错误响应
     * @param exception 原始异常
     * @param username 用户名
     * @param ipAddress IP地址
     */
    public void recordSecurityException(SecurityErrorResponse errorResponse, 
                                      Throwable exception, 
                                      String username, 
                                      String ipAddress) {
        if (!statisticsEnabled) {
            return;
        }
        
        try {
            // 更新总计数
            totalExceptionCount.incrementAndGet();
            
            // 按错误代码统计
            exceptionCountByCode.computeIfAbsent(errorResponse.getErrorCode(), k -> new AtomicLong(0))
                    .incrementAndGet();
            
            // 按错误分类统计
            if (errorResponse.getCategory() != null) {
                exceptionCountByCategory.computeIfAbsent(errorResponse.getCategory(), k -> new AtomicLong(0))
                        .incrementAndGet();
            }
            
            // 按严重级别统计
            if (errorResponse.getSeverity() != null) {
                exceptionCountBySeverity.computeIfAbsent(errorResponse.getSeverity(), k -> new AtomicLong(0))
                        .incrementAndGet();
            }
            
            // 按用户统计
            if (username != null && !username.isEmpty()) {
                exceptionCountByUser.computeIfAbsent(username, k -> new AtomicLong(0))
                        .incrementAndGet();
            }
            
            // 按IP地址统计
            if (ipAddress != null && !ipAddress.isEmpty()) {
                exceptionCountByIp.computeIfAbsent(ipAddress, k -> new AtomicLong(0))
                        .incrementAndGet();
            }
            
            // 记录到时间窗口
            recordToTimeWindow(errorResponse, username, ipAddress);
            
            // 检查是否需要告警
            checkAndTriggerAlert(errorResponse, username, ipAddress);
            
            log.debug("SecurityExceptionStatisticsService: 记录安全异常 - 错误代码: {}, 用户: {}, IP: {}", 
                    errorResponse.getErrorCode(), username, ipAddress);
            
        } catch (Exception e) {
            log.error("SecurityExceptionStatisticsService: 记录异常统计时发生错误", e);
        }
    }
    
    /**
     * 获取异常统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getExceptionStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 基本统计
        statistics.put("totalExceptions", totalExceptionCount.get());
        statistics.put("statisticsEnabled", statisticsEnabled);
        statistics.put("timeWindowMinutes", timeWindowMinutes);
        statistics.put("alertThreshold", alertThreshold);
        
        // 按错误代码统计
        Map<String, Long> codeStats = new HashMap<>();
        exceptionCountByCode.forEach((code, count) -> codeStats.put(code, count.get()));
        statistics.put("exceptionsByCode", codeStats);
        
        // 按错误分类统计
        Map<String, Long> categoryStats = new HashMap<>();
        exceptionCountByCategory.forEach((category, count) -> 
                categoryStats.put(category.name(), count.get()));
        statistics.put("exceptionsByCategory", categoryStats);
        
        // 按严重级别统计
        Map<String, Long> severityStats = new HashMap<>();
        exceptionCountBySeverity.forEach((severity, count) -> 
                severityStats.put(severity.name(), count.get()));
        statistics.put("exceptionsBySeverity", severityStats);
        
        // 按用户统计（只显示前10个）
        Map<String, Long> userStats = new HashMap<>();
        exceptionCountByUser.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .forEach(entry -> userStats.put(entry.getKey(), entry.getValue().get()));
        statistics.put("topExceptionsByUser", userStats);
        
        // 按IP统计（只显示前10个）
        Map<String, Long> ipStats = new HashMap<>();
        exceptionCountByIp.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .forEach(entry -> ipStats.put(entry.getKey(), entry.getValue().get()));
        statistics.put("topExceptionsByIp", ipStats);
        
        // 时间窗口统计
        statistics.put("recentExceptionsCount", getRecentExceptionsCount());
        statistics.put("lastAlertTime", lastAlertTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return statistics;
    }
    
    /**
     * 获取异常趋势分析
     * 
     * @return 趋势分析数据
     */
    public Map<String, Object> getExceptionTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        // 计算最近时间窗口内的异常数量
        int recentCount = getRecentExceptionsCount();
        trends.put("recentExceptionsCount", recentCount);
        
        // 计算异常增长率
        double growthRate = calculateGrowthRate();
        trends.put("growthRate", growthRate);
        
        // 识别异常热点
        trends.put("hotspotUsers", getHotspotUsers());
        trends.put("hotspotIps", getHotspotIps());
        trends.put("hotspotErrorCodes", getHotspotErrorCodes());
        
        // 风险评估
        trends.put("riskLevel", assessRiskLevel(recentCount, growthRate));
        
        return trends;
    }
    
    /**
     * 重置统计数据
     */
    public void resetStatistics() {
        if (!statisticsEnabled) {
            return;
        }
        
        totalExceptionCount.set(0);
        exceptionCountByCode.clear();
        exceptionCountByCategory.clear();
        exceptionCountBySeverity.clear();
        exceptionCountByUser.clear();
        exceptionCountByIp.clear();
        recentExceptions.clear();
        
        log.info("SecurityExceptionStatisticsService: 统计数据已重置");
    }
    
    /**
     * 清理过期的时间窗口数据
     */
    public void cleanupExpiredData() {
        if (!statisticsEnabled) {
            return;
        }
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeWindowMinutes);
        
        recentExceptions.entrySet().removeIf(entry -> 
                entry.getValue().getTimestamp().isBefore(cutoffTime));
        
        log.debug("SecurityExceptionStatisticsService: 清理过期数据完成");
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 记录到时间窗口
     * 
     * @param errorResponse 错误响应
     * @param username 用户名
     * @param ipAddress IP地址
     */
    private void recordToTimeWindow(SecurityErrorResponse errorResponse, String username, String ipAddress) {
        String key = generateTimeWindowKey(errorResponse, username, ipAddress);
        ExceptionRecord record = new ExceptionRecord(
                errorResponse.getErrorCode(),
                errorResponse.getCategory(),
                errorResponse.getSeverity(),
                username,
                ipAddress,
                LocalDateTime.now()
        );
        
        recentExceptions.put(key, record);
    }
    
    /**
     * 生成时间窗口键
     * 
     * @param errorResponse 错误响应
     * @param username 用户名
     * @param ipAddress IP地址
     * @return 时间窗口键
     */
    private String generateTimeWindowKey(SecurityErrorResponse errorResponse, String username, String ipAddress) {
        return String.format("%s_%s_%s_%d", 
                errorResponse.getErrorCode(),
                username != null ? username : "anonymous",
                ipAddress != null ? ipAddress : "unknown",
                System.currentTimeMillis());
    }
    
    /**
     * 检查并触发告警
     * 
     * @param errorResponse 错误响应
     * @param username 用户名
     * @param ipAddress IP地址
     */
    private void checkAndTriggerAlert(SecurityErrorResponse errorResponse, String username, String ipAddress) {
        if (!realTimeAlertEnabled) {
            return;
        }
        
        // 检查是否达到告警阈值
        int recentCount = getRecentExceptionsCount();
        if (recentCount >= alertThreshold) {
            // 避免频繁告警，至少间隔10分钟
            if (lastAlertTime.isBefore(LocalDateTime.now().minusMinutes(10))) {
                triggerAlert(errorResponse, recentCount, username, ipAddress);
                lastAlertTime = LocalDateTime.now();
            }
        }
        
        // 检查严重级别异常
        if (errorResponse.getSeverity() == SecurityErrorResponse.SecurityErrorSeverity.CRITICAL) {
            triggerCriticalAlert(errorResponse, username, ipAddress);
        }
    }
    
    /**
     * 触发告警
     * 
     * @param errorResponse 错误响应
     * @param exceptionCount 异常数量
     * @param username 用户名
     * @param ipAddress IP地址
     */
    private void triggerAlert(SecurityErrorResponse errorResponse, int exceptionCount, String username, String ipAddress) {
        try {
            // 创建告警安全事件
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("exceptionCount", exceptionCount);
            alertData.put("timeWindowMinutes", timeWindowMinutes);
            alertData.put("alertThreshold", alertThreshold);
            alertData.put("errorCode", errorResponse.getErrorCode());
            alertData.put("errorCategory", errorResponse.getCategory());
            alertData.put("errorSeverity", errorResponse.getSeverity());
            
            SecurityEventRequest alertEvent = SecurityEventUtils.createCustomEvent(
                    com.myweb.website_core.common.enums.SecurityEventType.ABNORMAL_ACCESS_FREQUENCY,
                    "安全异常频率告警",
                    String.format("检测到异常频率过高：%d分钟内发生%d次安全异常，超过阈值%d", 
                            timeWindowMinutes, exceptionCount, alertThreshold),
                    alertData
            );
            
            // 这里可以集成到安全事件处理系统
            log.warn("SecurityExceptionStatisticsService: 触发安全异常告警 - 异常数量: {}, 用户: {}, IP: {}", 
                    exceptionCount, username, ipAddress);
            
        } catch (Exception e) {
            log.error("SecurityExceptionStatisticsService: 触发告警时发生错误", e);
        }
    }
    
    /**
     * 触发严重级别告警
     * 
     * @param errorResponse 错误响应
     * @param username 用户名
     * @param ipAddress IP地址
     */
    private void triggerCriticalAlert(SecurityErrorResponse errorResponse, String username, String ipAddress) {
        try {
            Map<String, Object> criticalData = new HashMap<>();
            criticalData.put("errorCode", errorResponse.getErrorCode());
            criticalData.put("errorMessage", errorResponse.getMessage());
            criticalData.put("errorCategory", errorResponse.getCategory());
            
            log.error("SecurityExceptionStatisticsService: 严重安全异常告警 - 错误代码: {}, 用户: {}, IP: {}", 
                    errorResponse.getErrorCode(), username, ipAddress);
            
        } catch (Exception e) {
            log.error("SecurityExceptionStatisticsService: 触发严重告警时发生错误", e);
        }
    }
    
    /**
     * 获取最近时间窗口内的异常数量
     * 
     * @return 异常数量
     */
    private int getRecentExceptionsCount() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeWindowMinutes);
        return (int) recentExceptions.values().stream()
                .filter(record -> record.getTimestamp().isAfter(cutoffTime))
                .count();
    }
    
    /**
     * 计算异常增长率
     * 
     * @return 增长率（百分比）
     */
    private double calculateGrowthRate() {
        // 简化实现：比较最近30分钟和之前30分钟的异常数量
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recent30Min = now.minusMinutes(30);
        LocalDateTime previous30Min = now.minusMinutes(60);
        
        long recentCount = recentExceptions.values().stream()
                .filter(record -> record.getTimestamp().isAfter(recent30Min))
                .count();
        
        long previousCount = recentExceptions.values().stream()
                .filter(record -> record.getTimestamp().isAfter(previous30Min) && 
                                record.getTimestamp().isBefore(recent30Min))
                .count();
        
        if (previousCount == 0) {
            return recentCount > 0 ? 100.0 : 0.0;
        }
        
        return ((double) (recentCount - previousCount) / previousCount) * 100.0;
    }
    
    /**
     * 获取异常热点用户
     * 
     * @return 热点用户列表
     */
    private Map<String, Long> getHotspotUsers() {
        return exceptionCountByUser.entrySet().stream()
                .filter(entry -> entry.getValue().get() >= 5) // 阈值：5次异常
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(5)
                .collect(HashMap::new, 
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                        HashMap::putAll);
    }
    
    /**
     * 获取异常热点IP
     * 
     * @return 热点IP列表
     */
    private Map<String, Long> getHotspotIps() {
        return exceptionCountByIp.entrySet().stream()
                .filter(entry -> entry.getValue().get() >= 5) // 阈值：5次异常
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(5)
                .collect(HashMap::new, 
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                        HashMap::putAll);
    }
    
    /**
     * 获取异常热点错误代码
     * 
     * @return 热点错误代码列表
     */
    private Map<String, Long> getHotspotErrorCodes() {
        return exceptionCountByCode.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(5)
                .collect(HashMap::new, 
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                        HashMap::putAll);
    }
    
    /**
     * 评估风险级别
     * 
     * @param recentCount 最近异常数量
     * @param growthRate 增长率
     * @return 风险级别
     */
    private String assessRiskLevel(int recentCount, double growthRate) {
        if (recentCount >= alertThreshold * 2 || growthRate >= 100) {
            return "HIGH";
        } else if (recentCount >= alertThreshold || growthRate >= 50) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 异常记录
     */
    private static class ExceptionRecord {
        private final String errorCode;
        private final SecurityErrorResponse.SecurityErrorCategory category;
        private final SecurityErrorResponse.SecurityErrorSeverity severity;
        private final String username;
        private final String ipAddress;
        private final LocalDateTime timestamp;
        
        public ExceptionRecord(String errorCode, 
                             SecurityErrorResponse.SecurityErrorCategory category,
                             SecurityErrorResponse.SecurityErrorSeverity severity,
                             String username, 
                             String ipAddress, 
                             LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.category = category;
            this.severity = severity;
            this.username = username;
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
        }
        
        public String getErrorCode() { return errorCode; }
        public SecurityErrorResponse.SecurityErrorCategory getCategory() { return category; }
        public SecurityErrorResponse.SecurityErrorSeverity getSeverity() { return severity; }
        public String getUsername() { return username; }
        public String getIpAddress() { return ipAddress; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}