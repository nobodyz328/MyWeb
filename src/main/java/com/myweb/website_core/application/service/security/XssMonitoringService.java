package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * XSS防护监控服务
 * <p>
 * 提供XSS防护效果的实时监控、告警和性能分析功能。
 * <p>
 * 主要功能：
 * 1. 实时监控XSS攻击趋势
 * 2. 攻击阈值告警
 * 3. 防护效果评估
 * 4. 性能监控
 * 5. 自动化响应
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - XSS防护监控
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XssMonitoringService {
    
    private final XssFilterConfig xssFilterConfig;
    private final XssStatisticsService xssStatisticsService;
    private final AuditLogServiceAdapter auditLogService;
    
    // 监控指标
    private final AtomicLong blockedAttackCount = new AtomicLong(0);
    private final AtomicLong allowedRequestCount = new AtomicLong(0);
    private final AtomicLong processingTimeTotal = new AtomicLong(0);
    private final AtomicLong processingCount = new AtomicLong(0);
    
    // 时间窗口内的攻击计数
    private final ConcurrentHashMap<String, AtomicLong> timeWindowAttacks = new ConcurrentHashMap<>();
    
    // 告警冷却时间记录
    private final ConcurrentHashMap<String, LocalDateTime> alertCooldowns = new ConcurrentHashMap<>();
    
    /**
     * 记录XSS攻击监控事件
     * 
     * @param clientIp 客户端IP
     * @param requestUri 请求URI
     * @param attackType 攻击类型
     * @param isBlocked 是否被阻止
     * @param processingTimeMs 处理时间（毫秒）
     */
    @Async
    public void recordXssEvent(String clientIp, String requestUri, String attackType, 
                              boolean isBlocked, long processingTimeMs) {
        if (!xssFilterConfig.getMonitoring().isEnabled()) {
            return;
        }
        
        try {
            // 更新监控指标
            if (isBlocked) {
                blockedAttackCount.incrementAndGet();
            } else {
                allowedRequestCount.incrementAndGet();
            }
            
            // 记录处理时间
            processingTimeTotal.addAndGet(processingTimeMs);
            processingCount.incrementAndGet();
            
            // 更新时间窗口攻击计数
            String timeWindow = getCurrentTimeWindow();
            timeWindowAttacks.computeIfAbsent(timeWindow, k -> new AtomicLong(0)).incrementAndGet();
            
            // 检查是否需要告警
            checkAndTriggerAlerts(clientIp, requestUri, attackType, isBlocked);
            
            // 检查性能告警
            checkPerformanceAlert(processingTimeMs);
            
            log.debug("记录XSS监控事件 - IP: {}, URI: {}, 类型: {}, 阻止: {}, 处理时间: {}ms", 
                clientIp, requestUri, attackType, isBlocked, processingTimeMs);
            
        } catch (Exception e) {
            log.error("记录XSS监控事件失败", e);
        }
    }
    
    /**
     * 获取防护效果统计
     * 
     * @return 防护效果统计
     */
    public XssProtectionEffectiveness getProtectionEffectiveness() {
        long blocked = blockedAttackCount.get();
        long allowed = allowedRequestCount.get();
        long total = blocked + allowed;
        
        double blockRate = total > 0 ? (double) blocked / total * 100 : 0;
        double avgProcessingTime = processingCount.get() > 0 ? 
            (double) processingTimeTotal.get() / processingCount.get() : 0;
        
        return XssProtectionEffectiveness.builder()
                .totalRequests(total)
                .blockedAttacks(blocked)
                .allowedRequests(allowed)
                .blockRate(blockRate)
                .averageProcessingTimeMs(avgProcessingTime)
                .currentTimeWindow(getCurrentTimeWindow())
                .timeWindowAttacks(getCurrentTimeWindowAttacks())
                .build();
    }
    
    /**
     * 检查并触发告警
     * 
     * @param clientIp 客户端IP
     * @param requestUri 请求URI
     * @param attackType 攻击类型
     * @param isBlocked 是否被阻止
     */
    private void checkAndTriggerAlerts(String clientIp, String requestUri, String attackType, boolean isBlocked) {
        XssFilterConfig.MonitoringConfig config = xssFilterConfig.getMonitoring();
        
        if (!config.isRealTimeAlert()) {
            return;
        }
        
        // 检查时间窗口内的攻击次数
        long currentWindowAttacks = getCurrentTimeWindowAttacks();
        if (currentWindowAttacks >= config.getAttackThreshold()) {
            triggerAttackThresholdAlert(currentWindowAttacks);
        }
        
        // 检查单个IP的攻击频率
        Map<String, Long> ipStats = xssStatisticsService.getIpAttackStats();
        Long ipAttackCount = ipStats.get(clientIp);
        if (ipAttackCount != null && ipAttackCount >= config.getAttackThreshold() / 2) {
            triggerIpAttackAlert(clientIp, ipAttackCount);
        }
    }
    
    /**
     * 触发攻击阈值告警
     * 
     * @param attackCount 攻击次数
     */
    private void triggerAttackThresholdAlert(long attackCount) {
        String alertKey = "attack_threshold";
        if (isInCooldown(alertKey)) {
            return;
        }
        
        try {
            Map<String, Object> alertDetails = new HashMap<>();
            alertDetails.put("attackCount", attackCount);
            alertDetails.put("threshold", xssFilterConfig.getMonitoring().getAttackThreshold());
            alertDetails.put("timeWindow", getCurrentTimeWindow());
            alertDetails.put("alertTime", LocalDateTime.now());
            
            auditLogService.logSecurityEvent(
                SecurityEventRequest.builder()
                    .eventType(SecurityEventType.XSS_ATTACK_THRESHOLD_EXCEEDED)
                    .title("XSS攻击阈值告警")
                    .description(String.format("时间窗口内XSS攻击次数(%d)超过阈值(%d)", 
                        attackCount, xssFilterConfig.getMonitoring().getAttackThreshold()))
                    .eventData(alertDetails)
                    .riskScore(85)
                    .build()
            );
            
            // 设置告警冷却
            setAlertCooldown(alertKey);
            
            log.warn("XSS攻击阈值告警 - 攻击次数: {}, 阈值: {}", 
                attackCount, xssFilterConfig.getMonitoring().getAttackThreshold());
            
        } catch (Exception e) {
            log.error("触发XSS攻击阈值告警失败", e);
        }
    }
    
    /**
     * 触发IP攻击告警
     * 
     * @param clientIp 客户端IP
     * @param attackCount 攻击次数
     */
    private void triggerIpAttackAlert(String clientIp, long attackCount) {
        String alertKey = "ip_attack_" + clientIp;
        if (isInCooldown(alertKey)) {
            return;
        }
        
        try {
            Map<String, Object> alertDetails = new HashMap<>();
            alertDetails.put("clientIp", clientIp);
            alertDetails.put("attackCount", attackCount);
            alertDetails.put("alertTime", LocalDateTime.now());
            
            auditLogService.logSecurityEvent(
                SecurityEventRequest.builder()
                    .eventType(SecurityEventType.SUSPICIOUS_IP_ACTIVITY)
                    .title("可疑IP XSS攻击告警")
                    .description(String.format("IP %s 的XSS攻击次数异常: %d", clientIp, attackCount))
                    .eventData(alertDetails)
                    .riskScore(75)
                    .build()
            );
            
            // 设置告警冷却
            setAlertCooldown(alertKey);
            
            log.warn("可疑IP XSS攻击告警 - IP: {}, 攻击次数: {}", clientIp, attackCount);
            
        } catch (Exception e) {
            log.error("触发IP XSS攻击告警失败", e);
        }
    }
    
    /**
     * 检查性能告警
     * 
     * @param processingTimeMs 处理时间
     */
    private void checkPerformanceAlert(long processingTimeMs) {
        long threshold = xssFilterConfig.getMonitoring().getPerformanceThresholdMs();
        if (processingTimeMs > threshold) {
            String alertKey = "performance_slow";
            if (isInCooldown(alertKey)) {
                return;
            }
            
            try {
                Map<String, Object> alertDetails = new HashMap<>();
                alertDetails.put("processingTimeMs", processingTimeMs);
                alertDetails.put("thresholdMs", threshold);
                alertDetails.put("alertTime", LocalDateTime.now());
                
                auditLogService.logSecurityEvent(
                    SecurityEventRequest.builder()
                        .eventType(SecurityEventType.PERFORMANCE_DEGRADATION)
                        .title("XSS过滤性能告警")
                        .description(String.format("XSS过滤处理时间(%dms)超过阈值(%dms)", 
                            processingTimeMs, threshold))
                        .eventData(alertDetails)
                        .riskScore(60)
                        .build()
                );
                
                setAlertCooldown(alertKey);
                
                log.warn("XSS过滤性能告警 - 处理时间: {}ms, 阈值: {}ms", processingTimeMs, threshold);
                
            } catch (Exception e) {
                log.error("触发XSS性能告警失败", e);
            }
        }
    }
    
    /**
     * 检查是否在告警冷却期内
     * 
     * @param alertKey 告警键
     * @return 是否在冷却期内
     */
    private boolean isInCooldown(String alertKey) {
        LocalDateTime lastAlert = alertCooldowns.get(alertKey);
        if (lastAlert == null) {
            return false;
        }
        
        int cooldownMinutes = xssFilterConfig.getMonitoring().getAlertCooldownMinutes();
        return lastAlert.plusMinutes(cooldownMinutes).isAfter(LocalDateTime.now());
    }
    
    /**
     * 设置告警冷却
     * 
     * @param alertKey 告警键
     */
    private void setAlertCooldown(String alertKey) {
        alertCooldowns.put(alertKey, LocalDateTime.now());
    }
    
    /**
     * 获取当前时间窗口
     * 
     * @return 时间窗口字符串
     */
    private String getCurrentTimeWindow() {
        int windowMinutes = xssFilterConfig.getMonitoring().getTimeWindowMinutes();
        LocalDateTime now = LocalDateTime.now();
        int windowStart = (now.getMinute() / windowMinutes) * windowMinutes;
        return String.format("%04d-%02d-%02d %02d:%02d", 
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 
            now.getHour(), windowStart);
    }
    
    /**
     * 获取当前时间窗口的攻击次数
     * 
     * @return 攻击次数
     */
    private long getCurrentTimeWindowAttacks() {
        String currentWindow = getCurrentTimeWindow();
        AtomicLong count = timeWindowAttacks.get(currentWindow);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 定期清理过期的时间窗口数据
     */
    @Scheduled(cron = "0 */10 * * * ?") // 每10分钟清理一次
    public void cleanupExpiredTimeWindows() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // 保留24小时数据
            
            timeWindowAttacks.entrySet().removeIf(entry -> {
                try {
                    String[] parts = entry.getKey().split(" ");
                    if (parts.length == 2) {
                        LocalDateTime windowTime = LocalDateTime.parse(entry.getKey().replace(" ", "T"));
                        return windowTime.isBefore(cutoff);
                    }
                    return true;
                } catch (Exception e) {
                    return true; // 删除无效的时间窗口
                }
            });
            
            // 清理过期的告警冷却记录
            alertCooldowns.entrySet().removeIf(entry -> 
                entry.getValue().plusHours(24).isBefore(LocalDateTime.now()));
            
        } catch (Exception e) {
            log.error("清理XSS监控过期数据失败", e);
        }
    }
    
    /**
     * 生成监控报告
     */
    @Scheduled(cron = "0 0 */6 * * ?") // 每6小时生成报告
    public void generateMonitoringReport() {
        if (!xssFilterConfig.getMonitoring().isEnabled()) {
            return;
        }
        
        try {
            XssProtectionEffectiveness effectiveness = getProtectionEffectiveness();
            
            log.info("XSS防护效果报告 - 总请求: {}, 阻止攻击: {}, 允许请求: {}, 阻止率: {:.2f}%, 平均处理时间: {:.2f}ms",
                effectiveness.getTotalRequests(),
                effectiveness.getBlockedAttacks(),
                effectiveness.getAllowedRequests(),
                effectiveness.getBlockRate(),
                effectiveness.getAverageProcessingTimeMs());
            
        } catch (Exception e) {
            log.error("生成XSS监控报告失败", e);
        }
    }
    
    /**
     * XSS防护效果数据类
     */
    @lombok.Builder
    @lombok.Data
    public static class XssProtectionEffectiveness {
        private long totalRequests;
        private long blockedAttacks;
        private long allowedRequests;
        private double blockRate;
        private double averageProcessingTimeMs;
        private String currentTimeWindow;
        private long timeWindowAttacks;
    }
}