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
import com.myweb.website_core.infrastructure.security.Auditable;
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
@RequestMapping("/admin")
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

    @GetMapping
    @Auditable(operation = AuditOperation.ADMIN_ACCESS, resourceType = "ADMIN", description = "访问管理界面")
    public String adminIndex() {
        User currentUser = authenticationService.getCurrentUser();

        // 检查用户是否有管理权限
        if (currentUser == null || !currentUser.hasManagementPermission()) {
            log.warn("非管理员用户尝试访问管理界面: {}",
                    currentUser != null ? currentUser.getUsername() : "anonymous");
            return "redirect:/blog/view";
        }

        log.info("管理员 {} 访问管理界面", currentUser.getUsername());
        return "admin";
    }

    /**
     * 管理员主页
     */
    @GetMapping("/admin-dashboard")
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
    @GetMapping("/audit-logs")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "AUDIT_LOG")
    public String auditLogsPage(Model model) {
        model.addAttribute("pageTitle", "审计日志");
        return "admin/audit-logs";
    }

    /**
     * 安全事件页面
     */
    @GetMapping("/security-events")
    @Auditable(operation = AuditOperation.SECURITY_EVENT_QUERY, resourceType = "SECURITY_EVENT")
    public String securityEventsPage(Model model) {
        model.addAttribute("pageTitle", "安全事件");
        return "admin/security-events";
    }

    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    @Auditable(operation = AuditOperation.USER_MANAGEMENT, resourceType = "USER")
    public String usersPage(Model model) {
        model.addAttribute("pageTitle", "用户管理");
        return "admin/users";
    }

    /**
     * 角色权限管理页面
     */
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(operation = AuditOperation.ROLE_ASSIGNMENT, resourceType = "ROLE")
    public String rolesPage(Model model) {
        model.addAttribute("pageTitle", "角色权限管理");
        return "admin/roles";
    }

    /**
     * 系统设置页面
     */
    @GetMapping("/settings")
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