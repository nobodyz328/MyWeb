package com.myweb.website_core.application.service.security;

import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;

/**
 * XSS攻击统计服务
 * <p>
 * 提供XSS攻击的统计、分析和报告功能。
 * <p>
 * 主要功能：
 * 1. XSS攻击次数统计
 * 2. 攻击来源IP统计
 * 3. 攻击类型分析
 * 4. 实时统计数据
 * 5. 定期统计报告
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - XSS攻击统计
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XssStatisticsService {
    
    private final XssFilterConfig xssFilterConfig;
    
    // 总攻击次数
    private final LongAdder totalAttackCount = new LongAdder();
    
    // 今日攻击次数
    private final LongAdder todayAttackCount = new LongAdder();
    
    // 本小时攻击次数
    private final LongAdder hourlyAttackCount = new LongAdder();
    
    // IP攻击统计
    private final ConcurrentHashMap<String, LongAdder> ipAttackCount = new ConcurrentHashMap<>();
    
    // 攻击类型统计
    private final ConcurrentHashMap<String, LongAdder> attackTypeCount = new ConcurrentHashMap<>();
    
    // URI攻击统计
    private final ConcurrentHashMap<String, LongAdder> uriAttackCount = new ConcurrentHashMap<>();
    
    // 时间窗口统计
    private final ConcurrentHashMap<String, LongAdder> timeWindowStats = new ConcurrentHashMap<>();
    
    // 最后重置时间
    private volatile LocalDateTime lastResetTime = LocalDateTime.now();
    private volatile LocalDateTime lastHourlyResetTime = LocalDateTime.now();
    
    /**
     * 记录XSS攻击事件
     * 
     * @param clientIp 客户端IP
     * @param requestUri 请求URI
     * @param attackType 攻击类型
     * @param userAgent 用户代理
     */
    @Async
    public void recordXssAttack(String clientIp, String requestUri, String attackType, String userAgent) {
        if (!xssFilterConfig.getStatistics().isEnabled()) {
            return;
        }
        
        try {
            // 增加总计数
            totalAttackCount.increment();
            todayAttackCount.increment();
            hourlyAttackCount.increment();
            
            // IP统计
            ipAttackCount.computeIfAbsent(clientIp, k -> new LongAdder()).increment();
            
            // 攻击类型统计
            attackTypeCount.computeIfAbsent(attackType, k -> new LongAdder()).increment();
            
            // URI统计
            uriAttackCount.computeIfAbsent(requestUri, k -> new LongAdder()).increment();
            
            // 时间窗口统计
            String timeWindow = getCurrentTimeWindow();
            timeWindowStats.computeIfAbsent(timeWindow, k -> new LongAdder()).increment();
            
            log.debug("记录XSS攻击统计 - IP: {}, URI: {}, 类型: {}", clientIp, requestUri, attackType);
            
        } catch (Exception e) {
            log.error("记录XSS攻击统计失败", e);
        }
    }
    
    /**
     * 获取总攻击次数
     * 
     * @return 总攻击次数
     */
    public long getTotalAttackCount() {
        return totalAttackCount.sum();
    }
    
    /**
     * 获取今日攻击次数
     * 
     * @return 今日攻击次数
     */
    public long getTodayAttackCount() {
        return todayAttackCount.sum();
    }
    
    /**
     * 获取本小时攻击次数
     * 
     * @return 本小时攻击次数
     */
    public long getHourlyAttackCount() {
        return hourlyAttackCount.sum();
    }
    
    /**
     * 获取IP攻击统计
     * 
     * @return IP攻击统计Map
     */
    public Map<String, Long> getIpAttackStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        ipAttackCount.forEach((ip, count) -> stats.put(ip, count.sum()));
        return stats;
    }
    
    /**
     * 获取攻击类型统计
     * 
     * @return 攻击类型统计Map
     */
    public Map<String, Long> getAttackTypeStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        attackTypeCount.forEach((type, count) -> stats.put(type, count.sum()));
        return stats;
    }
    
    /**
     * 获取URI攻击统计
     * 
     * @return URI攻击统计Map
     */
    public Map<String, Long> getUriAttackStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        uriAttackCount.forEach((uri, count) -> stats.put(uri, count.sum()));
        return stats;
    }
    
    /**
     * 获取时间窗口统计
     * 
     * @return 时间窗口统计Map
     */
    public Map<String, Long> getTimeWindowStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        timeWindowStats.forEach((window, count) -> stats.put(window, count.sum()));
        return stats;
    }
    
    /**
     * 获取完整统计报告
     * 
     * @return 统计报告
     */
    public XssStatisticsReport getStatisticsReport() {
        return XssStatisticsReport.builder()
                .totalAttackCount(getTotalAttackCount())
                .todayAttackCount(getTodayAttackCount())
                .hourlyAttackCount(getHourlyAttackCount())
                .ipAttackStats(getIpAttackStats())
                .attackTypeStats(getAttackTypeStats())
                .uriAttackStats(getUriAttackStats())
                .timeWindowStats(getTimeWindowStats())
                .reportTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 重置今日统计
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天午夜重置
    public void resetDailyStats() {
        todayAttackCount.reset();
        lastResetTime = LocalDateTime.now();
        log.info("XSS攻击今日统计已重置");
    }
    
    /**
     * 重置小时统计
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时重置
    public void resetHourlyStats() {
        hourlyAttackCount.reset();
        lastHourlyResetTime = LocalDateTime.now();
        log.debug("XSS攻击小时统计已重置");
    }
    
    /**
     * 清理过期统计数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点清理
    public void cleanupExpiredStats() {
        try {
            int retentionDays = xssFilterConfig.getStatistics().getRetentionDays();
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            // 清理过期的时间窗口统计
            timeWindowStats.entrySet().removeIf(entry -> {
                try {
                    LocalDateTime windowTime = LocalDateTime.parse(entry.getKey(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
                    return windowTime.isBefore(cutoffTime);
                } catch (Exception e) {
                    log.warn("解析时间窗口失败: {}", entry.getKey());
                    return true; // 删除无效的时间窗口
                }
            });
            
            log.info("XSS攻击过期统计数据清理完成，保留{}天数据", retentionDays);
            
        } catch (Exception e) {
            log.error("清理XSS攻击过期统计数据失败", e);
        }
    }
    
    /**
     * 生成统计报告
     */
    @Scheduled(cron = "0 0 */24 * * ?") // 每24小时生成报告
    public void generateStatisticsReport() {
        if (!xssFilterConfig.getStatistics().isEnabled()) {
            return;
        }
        
        try {
            XssStatisticsReport report = getStatisticsReport();
            log.info("XSS攻击统计报告 - 总计: {}, 今日: {}, 本小时: {}", 
                report.getTotalAttackCount(), 
                report.getTodayAttackCount(), 
                report.getHourlyAttackCount());
            
            // 输出Top 10攻击IP
            report.getIpAttackStats().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> log.info("攻击IP统计 - {}: {} 次", entry.getKey(), entry.getValue()));
            
            // 输出攻击类型统计
            report.getAttackTypeStats().forEach((type, count) -> 
                log.info("攻击类型统计 - {}: {} 次", type, count));
            
        } catch (Exception e) {
            log.error("生成XSS攻击统计报告失败", e);
        }
    }
    
    /**
     * 获取当前时间窗口
     * 
     * @return 时间窗口字符串
     */
    private String getCurrentTimeWindow() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
    }
    
    /**
     * XSS统计报告数据类
     */
    @lombok.Builder
    @lombok.Data
    public static class XssStatisticsReport {
        private long totalAttackCount;
        private long todayAttackCount;
        private long hourlyAttackCount;
        private Map<String, Long> ipAttackStats;
        private Map<String, Long> attackTypeStats;
        private Map<String, Long> uriAttackStats;
        private Map<String, Long> timeWindowStats;
        private LocalDateTime reportTime;
    }
}