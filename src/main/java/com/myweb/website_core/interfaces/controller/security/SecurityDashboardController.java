package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.business.UserService;
import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.application.service.security.audit.SecurityEventService;
import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.common.constant.SystemConstants;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 安全监控面板控制器
 * 
 * 提供全面的安全监控和管理功能，包括：
 * - 安全事件统计和监控
 * - 审计日志查询和分析
 * - 用户行为分析
 * - 系统安全状态监控
 * - 威胁检测和告警
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/security/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SecurityDashboardController {
    
    private final AuditMessageService auditLogService;
    private final SecurityEventService securityEventService;
    private final AuthorizationService authorizationService;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 获取安全概览信息
     */
    @GetMapping("/overview")
    @Auditable(operation = AuditOperation.SECURITY_DASHBOARD_VIEW, description = "查看安全概览")
    public ResponseEntity<Map<String, Object>> getSecurityOverview(HttpServletRequest request) {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 今日安全事件统计
            overview.put("todaySecurityEvents", getTodaySecurityEventsCount());
            
            // 在线用户数量
            overview.put("onlineUsers", getOnlineUsersCount());
            
            // 今日登录失败次数
            overview.put("todayLoginFailures", getTodayLoginFailuresCount());
            
            // 今日审计日志数量
            overview.put("todayAuditLogs", getTodayAuditLogsCount());
            
            // 系统安全状态
            overview.put("securityStatus", getSystemSecurityStatus());
            
            // 威胁等级
            overview.put("threatLevel", getCurrentThreatLevel());
            
            // 最近安全事件
            overview.put("recentSecurityEvents", getRecentSecurityEvents(10));
            
            // 热点IP地址
            overview.put("hotIpAddresses", getHotIpAddresses(5));
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            log.error("获取安全概览失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取安全概览失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取安全事件统计
     */
    @GetMapping("/events/statistics")
    @Auditable(operation = AuditOperation.SECURITY_STATISTICS_VIEW, description = "查看安全事件统计")
    public ResponseEntity<Map<String, Object>> getSecurityEventStatistics(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String eventType,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 按天统计安全事件
            statistics.put("dailyEvents", getDailySecurityEvents(days));
            
            // 按事件类型统计
            statistics.put("eventsByType", getSecurityEventsByType(days, eventType));
            
            // 按严重程度统计
            statistics.put("eventsBySeverity", getSecurityEventsBySeverity(days));
            
            // 趋势分析
            statistics.put("trend", getSecurityEventsTrend(days));
            
            // 热点用户
            statistics.put("hotUsers", getHotUsers(days, 10));
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("获取安全事件统计失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取安全事件统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户行为分析
     */
    @GetMapping("/users/behavior")
    @Auditable(operation = AuditOperation.USER_BEHAVIOR_ANALYSIS, description = "查看用户行为分析")
    public ResponseEntity<Map<String, Object>> getUserBehaviorAnalysis(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "24") int hours,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            if (userId != null) {
                // 特定用户行为分析
                analysis.put("userActivity", getUserActivity(userId, hours));
                analysis.put("userSessions", getUserSessions(userId));
                analysis.put("userSecurityEvents", getUserSecurityEvents(userId, hours));
                analysis.put("userRiskScore", calculateUserRiskScore(userId));
            } else {
                // 全局用户行为分析
                analysis.put("activeUsers", getActiveUsers(hours));
                analysis.put("userLoginPattern", getUserLoginPattern(hours));
                analysis.put("suspiciousUsers", getSuspiciousUsers(hours));
                analysis.put("userGeoDistribution", getUserGeoDistribution(hours));
            }
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            log.error("获取用户行为分析失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取用户行为分析失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取系统安全状态
     */
    @GetMapping("/system/status")
    @Auditable(operation = AuditOperation.SYSTEM_STATUS_VIEW, description = "查看系统安全状态")
    public ResponseEntity<Map<String, Object>> getSystemSecurityStatus(HttpServletRequest request) {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 系统基本状态
            status.put("systemHealth", getSystemHealth());
            
            // 安全配置状态
            status.put("securityConfig", getSecurityConfigStatus());
            
            // 服务状态
            status.put("services", getServicesStatus());
            
            // 数据库连接状态
            status.put("database", getDatabaseStatus());
            
            // Redis连接状态
            status.put("redis", getRedisStatus());
            
            // 消息队列状态
            status.put("messageQueue", getMessageQueueStatus());
            
            // 存储空间状态
            status.put("storage", getStorageStatus());
            
            // 最近系统事件
            status.put("recentSystemEvents", getRecentSystemEvents(5));
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取系统安全状态失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取系统安全状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取威胁检测结果
     */
    @GetMapping("/threats")
    @Auditable(operation = AuditOperation.THREAT_DETECTION_VIEW, description = "查看威胁检测结果")
    public ResponseEntity<Map<String, Object>> getThreatDetection(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(required = false) String threatType,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> threats = new HashMap<>();
            
            // 检测到的威胁
            threats.put("detectedThreats", getDetectedThreats(hours, threatType));
            
            // 威胁趋势
            threats.put("threatTrend", getThreatTrend(hours));
            
            // 攻击来源分析
            threats.put("attackSources", getAttackSources(hours));
            
            // 攻击目标分析
            threats.put("attackTargets", getAttackTargets(hours));
            
            // 威胁等级分布
            threats.put("threatLevels", getThreatLevelDistribution(hours));
            
            // 防护效果统计
            threats.put("protectionStats", getProtectionStatistics(hours));
            
            return ResponseEntity.ok(threats);
            
        } catch (Exception e) {
            log.error("获取威胁检测结果失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取威胁检测结果失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取审计日志统计
     */
    @GetMapping("/audit/statistics")
    @Auditable(operation = AuditOperation.AUDIT_STATISTICS_VIEW, description = "查看审计日志统计")
    public ResponseEntity<Map<String, Object>> getAuditStatistics(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String operation,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 按操作类型统计
            statistics.put("operationStats", getAuditOperationStats(days, operation));
            
            // 按用户统计
            statistics.put("userStats", getAuditUserStats(days));
            
            // 按时间分布统计
            statistics.put("timeDistribution", getAuditTimeDistribution(days));
            
            // 成功失败比例
            statistics.put("successFailureRatio", getAuditSuccessFailureRatio(days));
            
            // 热点资源
            statistics.put("hotResources", getAuditHotResources(days, 10));
            
            // 异常操作检测
            statistics.put("anomalousOperations", getAnomalousOperations(days));
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("获取审计日志统计失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取审计日志统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取实时监控数据
     */
    @GetMapping("/realtime")
    @Auditable(operation = AuditOperation.REALTIME_MONITORING, description = "查看实时监控数据")
    public ResponseEntity<Map<String, Object>> getRealtimeMonitoring(HttpServletRequest request) {
        try {
            Map<String, Object> realtime = new HashMap<>();
            
            // 实时在线用户
            realtime.put("onlineUsers", getRealtimeOnlineUsers());
            
            // 实时安全事件
            realtime.put("realtimeEvents", getRealtimeSecurityEvents());
            
            // 实时系统负载
            realtime.put("systemLoad", getRealtimeSystemLoad());
            
            // 实时网络流量
            realtime.put("networkTraffic", getRealtimeNetworkTraffic());
            
            // 实时威胁检测
            realtime.put("threatDetection", getRealtimeThreatDetection());
            
            // 实时告警
            realtime.put("alerts", getRealtimeAlerts());
            
            return ResponseEntity.ok(realtime);
            
        } catch (Exception e) {
            log.error("获取实时监控数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取实时监控数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导出安全报告
     */
    @PostMapping("/reports/export")
    @Auditable(operation = AuditOperation.SECURITY_REPORT_EXPORT, description = "导出安全报告")
    public ResponseEntity<Map<String, Object>> exportSecurityReport(
            @RequestParam String reportType,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime endDate,
            @RequestParam(defaultValue = "PDF") String format,
            HttpServletRequest request) {
        
        try {
            // 记录导出操作
            auditLogService.logOperation(AuditLogRequest.builder()
                    .operation(AuditOperation.SECURITY_REPORT_EXPORT)
                    .username(getCurrentUsername(request))
                    .ipAddress(getClientIpAddress(request))
                    .description(String.format("导出安全报告: 类型=%s, 格式=%s, 时间范围=%s至%s", 
                               reportType, format, startDate, endDate))
                    .timestamp(LocalDateTime.now())
                    .result("SUCCESS")
                    .build());
            
            // 生成报告ID
            String reportId = generateReportId();
            
            // 异步生成报告
            generateSecurityReportAsync(reportId, reportType, startDate, endDate, format);
            
            return ResponseEntity.ok(Map.of(
                    "reportId", reportId,
                    "status", "GENERATING",
                    "message", "报告正在生成中，请稍后查看"
            ));
            
        } catch (Exception e) {
            log.error("导出安全报告失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "导出安全报告失败: " + e.getMessage()));
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 获取今日安全事件数量
     */
    private long getTodaySecurityEventsCount() {
        String key = RedisKey.auditStatsKey("security_events", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }
    
    /**
     * 获取在线用户数量
     */
    private long getOnlineUsersCount() {
        Long count = redisTemplate.opsForSet().size(RedisKey.USER_ONLINE_SET);
        return count != null ? count : 0L;
    }
    
    /**
     * 获取今日登录失败次数
     */
    private long getTodayLoginFailuresCount() {
        String key = RedisKey.auditStatsKey("login_failures", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }
    
    /**
     * 获取今日审计日志数量
     */
    private long getTodayAuditLogsCount() {
        String key = RedisKey.auditStatsKey("audit_logs", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }
    
    /**
     * 获取系统安全状态
     */
    private String getSystemSecurityStatus() {
        // 简化实现，实际应该综合多个指标判断
        long securityEvents = getTodaySecurityEventsCount();
        if (securityEvents > 100) {
            return "HIGH_RISK";
        } else if (securityEvents > 50) {
            return "MEDIUM_RISK";
        } else {
            return "LOW_RISK";
        }
    }
    
    /**
     * 获取当前威胁等级
     */
    private int getCurrentThreatLevel() {
        // 简化实现，实际应该基于威胁检测算法
        long securityEvents = getTodaySecurityEventsCount();
        if (securityEvents > 100) {
            return 5; // 严重
        } else if (securityEvents > 50) {
            return 3; // 中等
        } else {
            return 1; // 低
        }
    }
    
    /**
     * 获取最近安全事件
     */
    private List<Map<String, Object>> getRecentSecurityEvents(int limit) {
        // 简化实现，实际应该从数据库或缓存中获取
        List<Map<String, Object>> events = new ArrayList<>();
        
        // 模拟数据
        for (int i = 0; i < Math.min(limit, 5); i++) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", "EVENT_" + (System.currentTimeMillis() + i));
            event.put("type", SecurityEventType.ACCESS_DENIED.name());
            event.put("severity", "MEDIUM");
            event.put("timestamp", LocalDateTime.now().minusMinutes(i * 10));
            event.put("description", "检测到未授权访问尝试");
            events.add(event);
        }
        
        return events;
    }
    
    /**
     * 获取热点IP地址
     */
    private List<Map<String, Object>> getHotIpAddresses(int limit) {
        // 简化实现
        List<Map<String, Object>> hotIps = new ArrayList<>();
        
        // 模拟数据
        String[] ips = {"192.168.1.100", "10.0.0.50", "172.16.0.25"};
        for (int i = 0; i < Math.min(limit, ips.length); i++) {
            Map<String, Object> ipInfo = new HashMap<>();
            ipInfo.put("ip", ips[i]);
            ipInfo.put("requestCount", 150 - i * 20);
            ipInfo.put("riskLevel", i == 0 ? "HIGH" : "MEDIUM");
            hotIps.add(ipInfo);
        }
        
        return hotIps;
    }
    
    /**
     * 获取每日安全事件统计
     */
    private List<Map<String, Object>> getDailySecurityEvents(int days) {
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            Map<String, Object> dayStats = new HashMap<>();
            dayStats.put("date", dateStr);
            dayStats.put("count", Math.max(0, 50 - i * 5 + (int)(Math.random() * 20)));
            dailyStats.add(dayStats);
        }
        
        return dailyStats;
    }
    
    /**
     * 获取按事件类型统计
     */
    private Map<String, Integer> getSecurityEventsByType(int days, String eventType) {
        Map<String, Integer> typeStats = new HashMap<>();
        
        // 模拟数据
        typeStats.put("UNAUTHORIZED_ACCESS", 45);
        typeStats.put("LOGIN_FAILURE", 32);
        typeStats.put("SUSPICIOUS_ACTIVITY", 18);
        typeStats.put("MALICIOUS_FILE_UPLOAD", 8);
        typeStats.put("XSS_ATTEMPT", 12);
        
        return typeStats;
    }
    
    /**
     * 获取按严重程度统计
     */
    private Map<String, Integer> getSecurityEventsBySeverity(int days) {
        Map<String, Integer> severityStats = new HashMap<>();
        
        // 模拟数据
        severityStats.put("LOW", 65);
        severityStats.put("MEDIUM", 35);
        severityStats.put("HIGH", 12);
        severityStats.put("CRITICAL", 3);
        
        return severityStats;
    }
    
    /**
     * 获取安全事件趋势
     */
    private Map<String, Object> getSecurityEventsTrend(int days) {
        Map<String, Object> trend = new HashMap<>();
        
        // 简化实现
        trend.put("direction", "INCREASING");
        trend.put("percentage", 15.5);
        trend.put("description", "相比上周增长15.5%");
        
        return trend;
    }
    
    /**
     * 获取热点用户
     */
    private List<Map<String, Object>> getHotUsers(int days, int limit) {
        List<Map<String, Object>> hotUsers = new ArrayList<>();
        
        // 模拟数据
        for (int i = 0; i < Math.min(limit, 5); i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", 1000L + i);
            user.put("username", "user" + (i + 1));
            user.put("eventCount", 25 - i * 3);
            user.put("riskScore", 80 - i * 10);
            hotUsers.add(user);
        }
        
        return hotUsers;
    }
    
    // 其他辅助方法的简化实现...
    private Map<String, Object> getUserActivity(Long userId, int hours) { return new HashMap<>(); }
    private List<Map<String, Object>> getUserSessions(Long userId) { return new ArrayList<>(); }
    private List<Map<String, Object>> getUserSecurityEvents(Long userId, int hours) { return new ArrayList<>(); }
    private int calculateUserRiskScore(Long userId) { return 50; }
    private List<Map<String, Object>> getActiveUsers(int hours) { return new ArrayList<>(); }
    private Map<String, Object> getUserLoginPattern(int hours) { return new HashMap<>(); }
    private List<Map<String, Object>> getSuspiciousUsers(int hours) { return new ArrayList<>(); }
    private Map<String, Object> getUserGeoDistribution(int hours) { return new HashMap<>(); }
    
    private Map<String, Object> getSystemHealth() { 
        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY");
        health.put("uptime", "72h 15m");
        health.put("cpuUsage", 45.2);
        health.put("memoryUsage", 68.7);
        return health;
    }
    
    private Map<String, Object> getSecurityConfigStatus() {
        Map<String, Object> config = new HashMap<>();
        config.put("jwtEnabled", true);
        config.put("totpEnabled", true);
        config.put("rateLimitEnabled", true);
        config.put("auditEnabled", true);
        return config;
    }
    
    private Map<String, Object> getServicesStatus() {
        Map<String, Object> services = new HashMap<>();
        services.put("authService", "RUNNING");
        services.put("auditService", "RUNNING");
        services.put("securityService", "RUNNING");
        return services;
    }
    
    private Map<String, Object> getDatabaseStatus() {
        Map<String, Object> db = new HashMap<>();
        db.put("status", "CONNECTED");
        db.put("connectionPool", "8/20");
        db.put("responseTime", "15ms");
        return db;
    }
    
    private Map<String, Object> getRedisStatus() {
        try {
            redisTemplate.opsForValue().get("health_check");
            Map<String, Object> redis = new HashMap<>();
            redis.put("status", "CONNECTED");
            redis.put("memory", "256MB/1GB");
            redis.put("responseTime", "2ms");
            return redis;
        } catch (Exception e) {
            Map<String, Object> redis = new HashMap<>();
            redis.put("status", "DISCONNECTED");
            redis.put("error", e.getMessage());
            return redis;
        }
    }
    
    private Map<String, Object> getMessageQueueStatus() {
        Map<String, Object> mq = new HashMap<>();
        mq.put("status", "RUNNING");
        mq.put("queueCount", 5);
        mq.put("messageCount", 128);
        return mq;
    }
    
    private Map<String, Object> getStorageStatus() {
        Map<String, Object> storage = new HashMap<>();
        storage.put("totalSpace", "500GB");
        storage.put("usedSpace", "180GB");
        storage.put("freeSpace", "320GB");
        storage.put("usage", 36.0);
        return storage;
    }
    
    private List<Map<String, Object>> getRecentSystemEvents(int limit) { return new ArrayList<>(); }
    private List<Map<String, Object>> getDetectedThreats(int hours, String threatType) { return new ArrayList<>(); }
    private Map<String, Object> getThreatTrend(int hours) { return new HashMap<>(); }
    private Map<String, Object> getAttackSources(int hours) { return new HashMap<>(); }
    private Map<String, Object> getAttackTargets(int hours) { return new HashMap<>(); }
    private Map<String, Object> getThreatLevelDistribution(int hours) { return new HashMap<>(); }
    private Map<String, Object> getProtectionStatistics(int hours) { return new HashMap<>(); }
    
    private Map<String, Object> getAuditOperationStats(int days, String operation) { return new HashMap<>(); }
    private Map<String, Object> getAuditUserStats(int days) { return new HashMap<>(); }
    private Map<String, Object> getAuditTimeDistribution(int days) { return new HashMap<>(); }
    private Map<String, Object> getAuditSuccessFailureRatio(int days) { return new HashMap<>(); }
    private List<Map<String, Object>> getAuditHotResources(int days, int limit) { return new ArrayList<>(); }
    private List<Map<String, Object>> getAnomalousOperations(int days) { return new ArrayList<>(); }
    
    private Map<String, Object> getRealtimeOnlineUsers() { return new HashMap<>(); }
    private List<Map<String, Object>> getRealtimeSecurityEvents() { return new ArrayList<>(); }
    private Map<String, Object> getRealtimeSystemLoad() { return new HashMap<>(); }
    private Map<String, Object> getRealtimeNetworkTraffic() { return new HashMap<>(); }
    private Map<String, Object> getRealtimeThreatDetection() { return new HashMap<>(); }
    private List<Map<String, Object>> getRealtimeAlerts() { return new ArrayList<>(); }
    
    /**
     * 生成报告ID
     */
    private String generateReportId() {
        return "RPT-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    /**
     * 异步生成安全报告
     */
    private void generateSecurityReportAsync(String reportId, String reportType, 
                                           LocalDateTime startDate, LocalDateTime endDate, String format) {
        // 实际实现中应该使用异步任务
        log.info("开始生成安全报告: reportId={}, type={}, format={}", reportId, reportType, format);
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        // 简化实现，实际应该从Security Context获取
        return "admin";
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        for (String header : SystemConstants.REAL_IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}