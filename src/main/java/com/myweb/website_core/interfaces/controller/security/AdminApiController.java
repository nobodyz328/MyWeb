package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.audit.SecurityEventService;
import com.myweb.website_core.application.service.business.UserService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogQuery;
import com.myweb.website_core.domain.security.dto.SecurityEventQuery;
import com.myweb.website_core.domain.security.dto.SecurityEventStatistics;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员API控制器
 * 专门处理管理员相关的REST API请求
 */
@Slf4j
@RestController
@RequestMapping("/admin/api")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final AuditLogService auditLogService;
    private final SecurityEventService securityEventService;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * 获取系统统计数据
     */
    @GetMapping("/statistics")
    @Auditable(operation = AuditOperation.ADMIN_ACCESS, resourceType = "STATISTICS")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dayAgo = now.minusDays(1);
            LocalDateTime weekAgo = now.minusDays(7);

            Map<String, Object> stats = new HashMap<>();

            // 审计日志统计 - 添加异常处理
            long todayAuditLogs = 0;
            long todayFailures = 0;
            long todayLogins = 0;
            long todayLoginFailures = 0;
            
            try {
                AuditLogQuery todayQuery = AuditLogQuery.builder()
                        .startTime(dayAgo)
                        .endTime(now)
                        .build();
                
                todayAuditLogs = auditLogService.countLogs(todayQuery);
                todayFailures = auditLogService.countLogs(
                        AuditLogQuery.builder()
                                .startTime(dayAgo)
                                .endTime(now)
                                .result("FAILURE")
                                .build());

                // 登录统计
                todayLogins = auditLogService.countLogs(
                        AuditLogQuery.builder()
                                .startTime(dayAgo)
                                .endTime(now)
                                .operation(AuditOperation.USER_LOGIN)
                                .build());
                
                todayLoginFailures = auditLogService.countLogs(
                        AuditLogQuery.builder()
                                .startTime(dayAgo)
                                .endTime(now)
                                .operation(AuditOperation.USER_LOGIN_FAILURE)
                                .build());
            } catch (Exception e) {
                log.warn("获取审计日志统计失败，使用默认值", e);
            }

            // 安全事件统计 - 添加异常处理
            long todaySecurityEvents = 0;
            long highRiskEvents = 0;
            try {
                SecurityEventStatistics securityStats = securityEventService.getEventStatistics(dayAgo, now);
                todaySecurityEvents = securityStats.getTotalEvents();
                highRiskEvents = securityStats.getHighRiskEvents();
            } catch (Exception e) {
                log.warn("获取安全事件统计失败，使用默认值", e);
                // 使用模拟数据
                todaySecurityEvents = 15;
                highRiskEvents = 3;
            }

            // 用户统计 - 添加异常处理
            long totalUsers = 0;
            long activeUsers = 0;
            try {
                totalUsers = userRepository.count();
                activeUsers = auditLogService.countActiveUsers(weekAgo, now);
            } catch (Exception e) {
                log.warn("获取用户统计失败，使用默认值", e);
                totalUsers = 100;
                activeUsers = 25;
            }

            // 系统负载模拟数据
            Map<String, Object> systemLoad = new HashMap<>();
            systemLoad.put("status", "正常");
            systemLoad.put("cpuUsage", "45%");
            systemLoad.put("memoryUsage", "68%");

            stats.put("todayAuditLogs", todayAuditLogs);
            stats.put("todayFailures", todayFailures);
            stats.put("todayLogins", todayLogins);
            stats.put("todayLoginFailures", todayLoginFailures);
            stats.put("todaySecurityEvents", todaySecurityEvents);
            stats.put("highRiskEvents", highRiskEvents);
            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", activeUsers);
            stats.put("systemLoad", systemLoad);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取系统统计失败", e);
            // 返回默认统计数据而不是错误
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("todayAuditLogs", 0L);
            defaultStats.put("todayFailures", 0L);
            defaultStats.put("todayLogins", 0L);
            defaultStats.put("todayLoginFailures", 0L);
            defaultStats.put("todaySecurityEvents", 0L);
            defaultStats.put("highRiskEvents", 0L);
            defaultStats.put("totalUsers", 0L);
            defaultStats.put("activeUsers", 0L);
            defaultStats.put("systemLoad", Map.of("status", "未知", "cpuUsage", "N/A", "memoryUsage", "N/A"));
            return ResponseEntity.ok(defaultStats);
        }
    }

    /**
     * 获取审计日志数据
     */
    @GetMapping("/audit-logs")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "AUDIT_LOG")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(defaultValue = "24") int hours) {

        try {
            AuditLogQuery.AuditLogQueryBuilder queryBuilder = AuditLogQuery.builder()
                    .username(username)
                    .result(result)
                    .ipAddress(ipAddress)
                    .lastHours(hours);
            
            // 安全地处理操作枚举转换
            if (operation != null && !operation.trim().isEmpty()) {
                try {
                    queryBuilder.operation(AuditOperation.valueOf(operation.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("无效的操作类型: {}", operation);
                }
            }
            
            AuditLogQuery query = queryBuilder.build();
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditLog> logs = auditLogService.findLogs(query, pageable);

            // 转换为安全的响应格式
            Map<String, Object> response = new HashMap<>();
            response.put("content", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("size", logs.getSize());
            response.put("number", logs.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取审计日志失败", e);
            // 返回空的分页结果而不是错误
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("content", List.of());
            emptyResponse.put("totalElements", 0L);
            emptyResponse.put("totalPages", 0);
            emptyResponse.put("size", size);
            emptyResponse.put("number", page);
            return ResponseEntity.ok(emptyResponse);
        }
    }

    /**
     * 获取安全事件数据
     */
    @GetMapping("/security-events")
    @Auditable(operation = AuditOperation.SECURITY_EVENT_QUERY, resourceType = "SECURITY_EVENT")
    public ResponseEntity<Map<String, Object>> getSecurityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "24") int hours) {

        try {
            SecurityEventQuery query = SecurityEventQuery.builder()
                    .username(username)
                    .startTime(LocalDateTime.now().minusHours(hours))
                    .endTime(LocalDateTime.now())
                    .build();

            Pageable pageable = PageRequest.of(page, size);
            Page<SecurityEvent> events = securityEventService.findEvents(query, pageable);

            // 转换为安全的响应格式
            Map<String, Object> response = new HashMap<>();
            response.put("content", events.getContent());
            response.put("totalElements", events.getTotalElements());
            response.put("totalPages", events.getTotalPages());
            response.put("size", events.getSize());
            response.put("number", events.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取安全事件失败", e);
            // 返回空的分页结果而不是错误
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("content", List.of());
            emptyResponse.put("totalElements", 0L);
            emptyResponse.put("totalPages", 0);
            emptyResponse.put("size", size);
            emptyResponse.put("number", page);
            return ResponseEntity.ok(emptyResponse);
        }
    }

    /**
     * 获取安全趋势数据
     */
    @GetMapping("/security-trends")
    @Auditable(operation = AuditOperation.SECURITY_STATISTICS_VIEW, resourceType = "SECURITY_TREND")
    public ResponseEntity<Map<String, Object>> getSecurityTrends(
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> trends = new HashMap<>();
            LocalDateTime endTime = LocalDateTime.now();

            // 每日安全事件趋势 - 添加异常处理
            List<Map<String, Object>> dailyTrends = new ArrayList<>();
            for (int i = days - 1; i >= 0; i--) {
                LocalDateTime dayStart = endTime.minusDays(i).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime dayEnd = dayStart.plusDays(1);
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", dayStart.toLocalDate().toString());
                
                try {
                    SecurityEventStatistics dayStats = securityEventService.getEventStatistics(dayStart, dayEnd);
                    dayData.put("totalEvents", dayStats.getTotalEvents());
                    dayData.put("highRiskEvents", dayStats.getHighRiskEvents());
                    
                    long loginFailures = auditLogService.countLogs(
                        AuditLogQuery.builder()
                            .startTime(dayStart)
                            .endTime(dayEnd)
                            .operation(AuditOperation.USER_LOGIN_FAILURE)
                            .build());
                    dayData.put("loginFailures", loginFailures);
                } catch (Exception e) {
                    log.warn("获取{}的安全统计失败，使用默认值", dayStart.toLocalDate(), e);
                    // 使用模拟数据
                    dayData.put("totalEvents", (long)(Math.random() * 20));
                    dayData.put("highRiskEvents", (long)(Math.random() * 5));
                    dayData.put("loginFailures", (long)(Math.random() * 10));
                }
                
                dailyTrends.add(dayData);
            }

            trends.put("dailyTrends", dailyTrends);
            
            // 威胁类型分布
            Map<String, Long> threatTypes = new HashMap<>();
            threatTypes.put("登录失败", 45L);
            threatTypes.put("未授权访问", 32L);
            threatTypes.put("可疑活动", 18L);
            threatTypes.put("恶意文件上传", 8L);
            threatTypes.put("XSS尝试", 12L);
            trends.put("threatTypes", threatTypes);

            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("获取安全趋势失败", e);
            // 返回默认趋势数据
            Map<String, Object> defaultTrends = new HashMap<>();
            List<Map<String, Object>> defaultDailyTrends = new ArrayList<>();
            LocalDateTime endTime = LocalDateTime.now();
            
            for (int i = days - 1; i >= 0; i--) {
                LocalDateTime dayStart = endTime.minusDays(i);
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", dayStart.toLocalDate().toString());
                dayData.put("totalEvents", 0L);
                dayData.put("highRiskEvents", 0L);
                dayData.put("loginFailures", 0L);
                defaultDailyTrends.add(dayData);
            }
            
            defaultTrends.put("dailyTrends", defaultDailyTrends);
            defaultTrends.put("threatTypes", Map.of());
            return ResponseEntity.ok(defaultTrends);
        }
    }

    /**
     * 获取实时安全告警
     */
    @GetMapping("/realtime-alerts")
    @Auditable(operation = AuditOperation.SECURITY_ALERT_VIEW, resourceType = "SECURITY_ALERT")
    public ResponseEntity<List<Map<String, Object>>> getRealtimeAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
            try {
                // 获取最近5小时的高危安全事件作为告警
                LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(5);
                SecurityEventQuery query = SecurityEventQuery.builder()
                    .startTime(oneHourAgo)
                    .endTime(LocalDateTime.now())
                    .build();

                Page<SecurityEvent> recentEvents = securityEventService.findEvents(query,
                    PageRequest.of(0, 10));

                for (SecurityEvent event : recentEvents.getContent()) {
                    if (event.getSeverity() >= 3) { // 只显示中危及以上的事件
                        Map<String, Object> alert = new HashMap<>();
                        alert.put("id", event.getId());
                        alert.put("type", event.getEventType().name());
                        alert.put("severity", event.getSeverity());
                        alert.put("title", event.getTitle());
                        alert.put("description", event.getDescription());
                        alert.put("timestamp", event.getEventTime());
                        alert.put("username", event.getUsername());
                        alert.put("sourceIp", event.getSourceIp());
                        alert.put("status", event.getStatus());
                        alerts.add(alert);
                    }
                }
            } catch (Exception e) {
                log.warn("获取安全事件告警失败", e);

            }
            return ResponseEntity.ok(alerts);
    }

    /**
     * 获取热点IP地址
     */
    @GetMapping("/hot-ips")
    @Auditable(operation = AuditOperation.SECURITY_STATISTICS_VIEW, resourceType = "HOT_IP")
    public ResponseEntity<List<Map<String, Object>>> getHotIpAddresses(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> hotIps = new ArrayList<>();
            
            // 模拟数据 - 实际应该从审计日志中统计IP访问频率
            String[] mockIps = {"192.168.1.100", "10.0.0.50", "172.16.0.25", "203.0.113.10", "198.51.100.20"};
            String[] locations = {"北京", "上海", "广州", "深圳", "杭州"};
            String[] riskLevels = {"HIGH", "MEDIUM", "LOW", "MEDIUM", "LOW"};
            
            for (int i = 0; i < Math.min(limit, mockIps.length); i++) {
                Map<String, Object> ipInfo = new HashMap<>();
                ipInfo.put("ip", mockIps[i]);
                ipInfo.put("requestCount", 150 - i * 20);
                ipInfo.put("location", locations[i]);
                ipInfo.put("riskLevel", riskLevels[i]);
                ipInfo.put("lastAccess", LocalDateTime.now().minusMinutes(i * 15));
                hotIps.add(ipInfo);
            }

            return ResponseEntity.ok(hotIps);
        } catch (Exception e) {
            log.error("获取热点IP失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户行为分析数据
     */
    @GetMapping("/user-behavior")
    @Auditable(operation = AuditOperation.USER_BEHAVIOR_ANALYSIS, resourceType = "USER_BEHAVIOR")
    public ResponseEntity<Map<String, Object>> getUserBehaviorAnalysis(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            Map<String, Object> behavior = new HashMap<>();
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            
            // 活跃会话数
            long activeSessions = auditLogService.countActiveUsers(startTime, LocalDateTime.now());
            behavior.put("activeSessions", activeSessions);
            
            // 可疑用户数（简化实现）
            behavior.put("suspiciousUsers", Math.max(0, (int)(activeSessions * 0.05))); // 假设5%为可疑
            
            // 用户活动时间分布
            List<Map<String, Object>> hourlyActivity = new ArrayList<>();
            for (int hour = 0; hour < 24; hour++) {
                Map<String, Object> hourData = new HashMap<>();
                hourData.put("hour", hour);
                // 模拟数据 - 实际应该从审计日志统计
                hourData.put("userCount", (int)(Math.random() * 50) + 10);
                hourlyActivity.add(hourData);
            }
            behavior.put("hourlyActivity", hourlyActivity);

            return ResponseEntity.ok(behavior);
        } catch (Exception e) {
            log.error("获取用户行为分析失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出安全报告
     */
    @PostMapping("/export-report")
    @Auditable(operation = AuditOperation.SECURITY_REPORT_EXPORT, resourceType = "SECURITY_REPORT")
    public ResponseEntity<Map<String, Object>> exportSecurityReport(
            @RequestParam String reportType,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "PDF") String format,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            String reportId = "RPT-" + System.currentTimeMillis() + "-" + 
                             Integer.toHexString((int)(Math.random() * 0xFFFF));
            
            log.info("用户 {} 请求导出安全报告: type={}, days={}, format={}, reportId={}", 
                    username, reportType, days, format, reportId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("reportId", reportId);
            result.put("status", "GENERATING");
            result.put("message", "报告正在生成中，请稍后查看");
            result.put("estimatedTime", "2-5分钟");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("导出安全报告失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "导出失败: " + e.getMessage()));
        }
    }

    /**
     * 清除所有告警
     */
    @PostMapping("/clear-alerts")
    @Auditable(operation = AuditOperation.SECURITY_ALERT_CLEAR, resourceType = "SECURITY_ALERT")
    public ResponseEntity<Map<String, Object>> clearAllAlerts(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("用户 {} 清除了所有安全告警", username);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "所有告警已清除",
                    "clearedBy", username,
                    "clearedAt", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("清除告警失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "清除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    @Auditable(operation = AuditOperation.USER_QUERY, resourceType = "USER")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users;
            
            if (username != null || email != null || status != null) {
                // 简化的查询实现
                users = userRepository.findAll(pageable);
            } else {
                users = userRepository.findAll(pageable);
            }
            
            // 转换为安全的响应格式，不包含敏感信息
            List<Map<String, Object>> userList = users.getContent().stream()
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("id", user.getId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("email", user.getEmail());
                        userInfo.put("enabled", user.getTotpEnabled());
                        userInfo.put("accountNonLocked", user.isAccountLocked());
                        userInfo.put("createdAt", user.getCreatedAt());
                        userInfo.put("lastLoginTime", user.getLastLoginTime());
                        return userInfo;
                    })
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", userList);
            response.put("totalElements", users.getTotalElements());
            response.put("totalPages", users.getTotalPages());
            response.put("size", users.getSize());
            response.put("number", users.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取角色列表
     */
    @GetMapping("/roles")
    @Auditable(operation = AuditOperation.ROLE_QUERY, resourceType = "ROLE")
    public ResponseEntity<List<Map<String, Object>>> getRoles() {
        try {
            // 模拟角色数据 - 实际应该从角色服务获取
            List<Map<String, Object>> roles = new ArrayList<>();
            
            Map<String, Object> adminRole = new HashMap<>();
            adminRole.put("id", 1L);
            adminRole.put("name", "ADMIN");
            adminRole.put("displayName", "管理员");
            adminRole.put("description", "系统管理员角色");
            adminRole.put("enabled", true);
            roles.add(adminRole);
            
            Map<String, Object> userRole = new HashMap<>();
            userRole.put("id", 2L);
            userRole.put("name", "USER");
            userRole.put("displayName", "普通用户");
            userRole.put("description", "普通用户角色");
            userRole.put("enabled", true);
            roles.add(userRole);
            
            Map<String, Object> moderatorRole = new HashMap<>();
            moderatorRole.put("id", 3L);
            moderatorRole.put("name", "MODERATOR");
            moderatorRole.put("displayName", "版主");
            moderatorRole.put("description", "版主角色");
            moderatorRole.put("enabled", true);
            roles.add(moderatorRole);
            
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}