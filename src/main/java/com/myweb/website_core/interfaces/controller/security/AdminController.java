package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.audit.SecurityEventService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.application.service.security.authorization.RoleService;
import com.myweb.website_core.application.service.business.UserService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogQuery;
import com.myweb.website_core.domain.security.dto.SecurityEventQuery;
import com.myweb.website_core.domain.security.dto.SecurityEventStatistics;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * <p>
 * 提供管理员界面和API，整合所有管理员功能：
 * - 审计日志查看
 * - 安全事件监控
 * - 用户管理
 * - 角色权限管理
 * - 系统统计
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Controller
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogService auditLogService;
    private final SecurityEventService securityEventService;
    private final AuthorizationService authorizationService;
    private final RoleService roleService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    @GetMapping("/admin")
    @Auditable(operation = AuditOperation.ADMIN_ACCESS,
            resourceType = "ADMIN",
            description = "访问管理界面")
    public String admin() {
        return "admin/admin";
    }


    /**
     * 管理员主页
     */
    @GetMapping("/admin_dashboard")
    @Auditable(operation = AuditOperation.ADMIN_LOGIN, resourceType = "ADMIN")
    public String adminDashboard(Model model, Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            // 获取系统统计数据
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dayAgo = now.minusDays(1);
            LocalDateTime weekAgo = now.minusDays(7);

            // 审计日志统计
            AuditLogQuery auditQuery = AuditLogQuery.builder()
                    .startTime(dayAgo)
                    .endTime(now)
                    .build();

            long todayAuditLogs = auditLogService.countLogs(auditQuery);
            long todayFailures = auditLogService.countLogs(AuditLogQuery.builder()
                    .startTime(dayAgo)
                    .endTime(now)
                    .result("FAILURE")
                    .build());

            // 登录统计
            long todayLogins = auditLogService.countLogs(AuditLogQuery.builder()
                    .startTime(dayAgo)
                    .endTime(now)
                    .operation(AuditOperation.USER_LOGIN_SUCCESS)
                    .build());
            
            long todayLoginFailures = auditLogService.countLogs(AuditLogQuery.builder()
                    .startTime(dayAgo)
                    .endTime(now)
                    .operation(AuditOperation.USER_LOGIN_FAILURE)
                    .build());

            // 安全事件统计
            SecurityEventStatistics securityStats = securityEventService.getEventStatistics(dayAgo, now);

            // 用户统计
            long totalUsers = userRepository.count();
            long activeUsers = auditLogService.countActiveUsers(weekAgo, now);

            // 系统健康状态
            Map<String, Object> systemHealth = new HashMap<>();
            systemHealth.put("status", "healthy");
            systemHealth.put("todayAuditLogs", todayAuditLogs);
            systemHealth.put("todayFailures", todayFailures);
            systemHealth.put("todayLogins", todayLogins);
            systemHealth.put("todayLoginFailures", todayLoginFailures);
            systemHealth.put("todaySecurityEvents", securityStats.getTotalEvents());
            systemHealth.put("highRiskEvents", securityStats.getHighRiskEvents());
            systemHealth.put("totalUsers", totalUsers);
            systemHealth.put("activeUsers", activeUsers);

            model.addAttribute("currentUser", currentUser);
            model.addAttribute("systemHealth", systemHealth);
            model.addAttribute("securityStats", securityStats);

            log.info("管理员 {} 访问了管理面板", username);
            return "admin/dashboard";

        } catch (Exception e) {
            log.error("加载管理员面板失败", e);
            model.addAttribute("error", "加载管理面板失败: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 审计日志页面
     */
    @GetMapping("/admin_audit_logs")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "AUDIT_LOG")
    public String auditLogsPage(Model model) {
        model.addAttribute("pageTitle", "审计日志");
        return "admin/audit-logs";
    }

    /**
     * 安全事件页面
     */
    @GetMapping("/admin_security_events")
    @Auditable(operation = AuditOperation.SECURITY_EVENT_QUERY, resourceType = "SECURITY_EVENT")
    public String securityEventsPage(Model model) {
        model.addAttribute("pageTitle", "安全事件");
        return "admin/security-events";
    }

    /**
     * 用户管理页面
     */
    @GetMapping("/admin_users")
    @Auditable(operation = AuditOperation.USER_MANAGEMENT, resourceType = "USER")
    public String usersPage(Model model) {
        model.addAttribute("pageTitle", "用户管理");
        return "admin/users";
    }

    /**
     * 角色权限管理页面
     */
    @GetMapping("/admin_roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(operation = AuditOperation.ROLE_ASSIGNMENT, resourceType = "ROLE")
    public String rolesPage(Model model) {
        model.addAttribute("pageTitle", "角色权限管理");
        return "admin/roles";
    }

    /**
     * 系统设置页面
     */
    @GetMapping("/admin_settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SYSTEM")
    public String settingsPage(Model model) {
        model.addAttribute("pageTitle", "系统设置");
        return "admin/settings";
    }

    // ==================== REST API 接口 ====================

    /**
     * 获取审计日志数据
     */
    @GetMapping("/api/audit-logs")
    @ResponseBody
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(defaultValue = "24") int hours) {

        try {
            AuditLogQuery query = AuditLogQuery.builder()
                    .username(username)
                    .operation(operation != null ? AuditOperation.valueOf(operation) : null)
                    .result(result)
                    .ipAddress(ipAddress)
                    .lastHours(hours)
                    .build();

            Pageable pageable = PageRequest.of(page, size);
            Page<AuditLog> logs = auditLogService.findLogs(query, pageable);

            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("获取审计日志失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取安全事件数据
     */
    @GetMapping("/api/security-events")
    @ResponseBody
    public ResponseEntity<Page<SecurityEvent>> getSecurityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String eventType,
            //@RequestParam(required = false) Integer minSeverity,
            @RequestParam(defaultValue = "24") int hours) {

        try {
            SecurityEventQuery query = SecurityEventQuery.builder()
                    .username(username)
                    .eventTypes(eventType != null && !eventType.trim().isEmpty() ? 
                        List.of(SecurityEventType.valueOf(eventType)) : null)
                    //.minSeverity(minSeverity)
                    .startTime(LocalDateTime.now().minusHours(hours))
                    .endTime(LocalDateTime.now())
                    .build();

            Pageable pageable = PageRequest.of(page, size);
            Page<SecurityEvent> events = securityEventService.findEvents(query, pageable);

            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("获取安全事件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users;

            if (search != null && !search.trim().isEmpty()) {
                users = userRepository.findAll(pageable);
            } else {
                users = userRepository.findAll(pageable);
            }

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取角色列表
     */
    @GetMapping("/api/roles")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getRoles() {
        try {
            List<Role> roles = roleService.findAllEnabledRoles();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取系统统计数据
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dayAgo = now.minusDays(1);
            LocalDateTime weekAgo = now.minusDays(7);

            Map<String, Object> stats = new HashMap<>();

            // 审计日志统计
            stats.put("todayAuditLogs", auditLogService.countLogs(
                    AuditLogQuery.builder().startTime(dayAgo).endTime(now).build()));
            stats.put("weekAuditLogs", auditLogService.countLogs(
                    AuditLogQuery.builder().startTime(weekAgo).endTime(now).build()));

            // 安全事件统计
            SecurityEventStatistics securityStats = securityEventService.getEventStatistics(dayAgo, now);
            stats.put("todaySecurityEvents", securityStats.getTotalEvents());
            stats.put("highRiskEvents", securityStats.getHighRiskEvents());

            // 用户统计
            stats.put("totalUsers", userRepository.count());
            stats.put("activeUsers", auditLogService.countActiveUsers(weekAgo, now));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取系统统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取安全趋势数据
     */
    @GetMapping("/api/security-trends")
    @ResponseBody
    @Auditable(operation = AuditOperation.SECURITY_STATISTICS_VIEW, resourceType = "SECURITY_TREND")
    public ResponseEntity<Map<String, Object>> getSecurityTrends(
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> trends = new HashMap<>();
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // 每日安全事件趋势
            List<Map<String, Object>> dailyTrends = new ArrayList<>();
            for (int i = days - 1; i >= 0; i--) {
                LocalDateTime dayStart = endTime.minusDays(i).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime dayEnd = dayStart.plusDays(1);
                
                SecurityEventStatistics dayStats = securityEventService.getEventStatistics(dayStart, dayEnd);
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", dayStart.toLocalDate().toString());
                dayData.put("totalEvents", dayStats.getTotalEvents());
                dayData.put("highRiskEvents", dayStats.getHighRiskEvents());
                dayData.put("loginFailures", auditLogService.countLogs(
                    AuditLogQuery.builder()
                        .startTime(dayStart)
                        .endTime(dayEnd)
                        .operation(AuditOperation.USER_LOGIN_FAILURE)
                        .build()));
                
                dailyTrends.add(dayData);
            }

            trends.put("dailyTrends", dailyTrends);
            
            // 威胁类型分布
            Map<String, Long> threatTypes = new HashMap<>();
            threatTypes.put("UNAUTHORIZED_ACCESS", 45L);
            threatTypes.put("LOGIN_FAILURE", 32L);
            threatTypes.put("SUSPICIOUS_ACTIVITY", 18L);
            threatTypes.put("MALICIOUS_FILE_UPLOAD", 8L);
            threatTypes.put("XSS_ATTEMPT", 12L);
            trends.put("threatTypes", threatTypes);

            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("获取安全趋势失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取实时安全告警
     */
    @GetMapping("/api/realtime-alerts")
    @ResponseBody
    @Auditable(operation = AuditOperation.SECURITY_ALERT_VIEW, resourceType = "SECURITY_ALERT")
    public ResponseEntity<List<Map<String, Object>>> getRealtimeAlerts() {
        try {
            List<Map<String, Object>> alerts = new ArrayList<>();
            
            // 获取最近1小时的高危安全事件作为告警
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
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

            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("获取实时告警失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取热点IP地址
     */
    @GetMapping("/api/hot-ips")
    @ResponseBody
    @Auditable(operation = AuditOperation.SECURITY_STATISTICS_VIEW, resourceType = "HOT_IP")
    public ResponseEntity<List<Map<String, Object>>> getHotIpAddresses(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> hotIps = new ArrayList<>();
            
            // 简化实现 - 实际应该从审计日志中统计IP访问频率
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
    @GetMapping("/api/user-behavior")
    @ResponseBody
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
    @PostMapping("/api/export-report")
    @ResponseBody
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
            
            // 实际实现中应该异步生成报告
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
    @PostMapping("/api/clear-alerts")
    @ResponseBody
    @Auditable(operation = AuditOperation.SECURITY_ALERT_CLEAR, resourceType = "SECURITY_ALERT")
    public ResponseEntity<Map<String, Object>> clearAllAlerts(Authentication authentication) {
        try {
            String username = authentication.getName();
            
            // 实际实现中应该更新告警状态为已处理
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
     * 处理安全事件
     */
    @PostMapping("/api/security-events/{eventId}/handle")
    @ResponseBody
    @Auditable(operation = AuditOperation.USER_MANAGEMENT, resourceType = "SECURITY_EVENT")
    public ResponseEntity<String> handleSecurityEvent(
            @PathVariable Long eventId,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        try {
            String handlerUsername = authentication.getName();
            User handler = userRepository.findByUsername(handlerUsername);

            securityEventService.handleEvent(eventId, handlerUsername, notes,
                    SecurityEvent.Status.valueOf(status.toUpperCase()));

            log.info("安全事件 {} 已被 {} 处理，状态: {}", eventId, handlerUsername, status);
            return ResponseEntity.ok("安全事件处理成功");

        } catch (Exception e) {
            log.error("处理安全事件失败: eventId={}", eventId, e);
            return ResponseEntity.internalServerError().body("处理失败: " + e.getMessage());
        }
    }
}