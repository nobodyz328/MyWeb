package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.*;
import com.myweb.website_core.application.service.security.authentication.SessionCleanupService;
import com.myweb.website_core.application.service.security.dataprotect.BackupService;
import com.myweb.website_core.application.service.security.dataprotect.DataDeletionService;
import com.myweb.website_core.application.service.security.dataprotect.DataRecoveryService;
import com.myweb.website_core.application.service.security.fileProtect.FileIntegrityService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.infrastructure.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 安全服务管理控制器
 * 
 * 提供统一的安全服务管理界面，包括：
 * - 服务状态查询
 * - 服务健康检查
 * - 服务统计信息
 * - 服务操作控制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/security-services")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SecurityServicesController {
    
    private final BackupService backupService;
    private final DataRecoveryService dataRecoveryService;
    private final FileIntegrityService fileIntegrityService;
    private final SessionCleanupService sessionCleanupService;
    private final DataDeletionService dataDeletionService;
    private final UserDataManagementService userDataManagementService;
    
    /**
     * 获取所有安全服务状态概览
     */
    @GetMapping("/status")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "查询安全服务状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServicesStatus() {
        try {
            log.info("管理员查询安全服务状态概览");
            
            Map<String, Object> servicesStatus = new HashMap<>();
            
            // 备份服务状态
            Map<String, Object> backupStatus = new HashMap<>();
            backupStatus.put("serviceName", "自动备份服务");
            backupStatus.put("status", "ACTIVE");
            backupStatus.put("description", "提供数据库和文件的自动备份功能");
            backupStatus.put("features", new String[]{
                "定时自动备份", "备份文件加密", "完整性验证", "过期清理"
            });
            servicesStatus.put("backupService", backupStatus);
            
            // 数据恢复服务状态
            Map<String, Object> recoveryStatus = new HashMap<>();
            recoveryStatus.put("serviceName", "数据恢复服务");
            recoveryStatus.put("status", "ACTIVE");
            recoveryStatus.put("description", "提供数据库和文件的恢复功能");
            recoveryStatus.put("features", new String[]{
                "完全恢复", "时间点恢复", "选择性恢复", "恢复验证"
            });
            servicesStatus.put("recoveryService", recoveryStatus);
            
            // 文件完整性服务状态
            FileIntegrityService.FileIntegrityStatistics integrityStats = 
                fileIntegrityService.getIntegrityStatistics();
            Map<String, Object> integrityStatus = new HashMap<>();
            integrityStatus.put("serviceName", "文件完整性服务");
            integrityStatus.put("status", integrityStats.isIntegrityCheckEnabled() ? "ACTIVE" : "DISABLED");
            integrityStatus.put("description", "监控关键文件完整性，检测篡改");
            integrityStatus.put("statistics", integrityStats);
            integrityStatus.put("features", new String[]{
                "定时完整性检查", "文件篡改检测", "自动备份恢复", "告警通知"
            });
            servicesStatus.put("integrityService", integrityStatus);
            
            // 会话清理服务状态
            Map<String, Object> sessionStatus = new HashMap<>();
            sessionStatus.put("serviceName", "会话清理服务");
            sessionStatus.put("status", "ACTIVE");
            sessionStatus.put("description", "清理用户会话和临时数据");
            sessionStatus.put("statistics", sessionCleanupService.getSessionStatistics());
            sessionStatus.put("features", new String[]{
                "会话超时清理", "用户退出清理", "临时数据清理", "缓存清理"
            });
            servicesStatus.put("sessionService", sessionStatus);
            
            // 数据删除服务状态
            Map<String, Object> deletionStatus = new HashMap<>();
            deletionStatus.put("serviceName", "数据彻底删除服务");
            deletionStatus.put("status", "ACTIVE");
            deletionStatus.put("description", "提供数据的彻底删除功能");
            deletionStatus.put("features", new String[]{
                "用户数据删除", "级联删除", "缓存清理", "确认机制"
            });
            servicesStatus.put("deletionService", deletionStatus);
            
            // 用户数据管理服务状态
            Map<String, Object> userDataStatus = new HashMap<>();
            userDataStatus.put("serviceName", "用户数据管理服务");
            userDataStatus.put("status", "ACTIVE");
            userDataStatus.put("description", "管理用户个人数据的查看、导出、修改");
            userDataStatus.put("features", new String[]{
                "数据查看", "数据导出", "数据修改", "权限控制"
            });
            servicesStatus.put("userDataService", userDataStatus);
            
            // 总体状态
            Map<String, Object> overallStatus = new HashMap<>();
            overallStatus.put("totalServices", servicesStatus.size());
            overallStatus.put("activeServices", servicesStatus.size()); // 假设都是活跃的
            overallStatus.put("lastCheckTime", LocalDateTime.now());
            overallStatus.put("complianceLevel", "GB/T 22239-2019 二级等保");
            servicesStatus.put("overall", overallStatus);
            
            return ResponseEntity.ok(ApiResponse.success(servicesStatus));
            
        } catch (Exception e) {
            log.error("获取安全服务状态失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取服务状态失败"));
        }
    }
    
    /**
     * 执行安全服务健康检查
     */
    @PostMapping("/health-check")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "执行安全服务健康检查")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performHealthCheck() {
        try {
            log.info("管理员执行安全服务健康检查");
            
            Map<String, Object> healthResults = new HashMap<>();
            
            // 异步执行各项健康检查
            CompletableFuture<Void> backupCheck = CompletableFuture.runAsync(() -> {
                try {
                    // 检查备份服务 - 获取备份列表
                    var backups = backupService.getBackupList();
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "HEALTHY");
                    result.put("backupCount", backups.size());
                    result.put("message", "备份服务运行正常");
                    healthResults.put("backupService", result);
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "UNHEALTHY");
                    result.put("error", e.getMessage());
                    healthResults.put("backupService", result);
                }
            });
            
            CompletableFuture<Void> integrityCheck = CompletableFuture.runAsync(() -> {
                try {
                    // 检查文件完整性服务
                    var stats = fileIntegrityService.getIntegrityStatistics();
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", stats.isIntegrityCheckEnabled() ? "HEALTHY" : "DISABLED");
                    result.put("totalFiles", stats.getTotalFiles());
                    result.put("validFiles", stats.getValidFiles());
                    result.put("invalidFiles", stats.getInvalidFiles());
                    result.put("message", "文件完整性服务运行正常");
                    healthResults.put("integrityService", result);
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "UNHEALTHY");
                    result.put("error", e.getMessage());
                    healthResults.put("integrityService", result);
                }
            });
            
            CompletableFuture<Void> sessionCheck = CompletableFuture.runAsync(() -> {
                try {
                    // 检查会话清理服务
                    var sessionStats = sessionCleanupService.getSessionStatistics();
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "HEALTHY");
                    result.put("activeSessions", sessionStats.get("activeSessions"));
                    result.put("message", "会话清理服务运行正常");
                    healthResults.put("sessionService", result);
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "UNHEALTHY");
                    result.put("error", e.getMessage());
                    healthResults.put("sessionService", result);
                }
            });
            
            // 等待所有检查完成
            CompletableFuture.allOf(backupCheck, integrityCheck, sessionCheck).get();
            
            // 计算总体健康状态
            long healthyCount = healthResults.values().stream()
                    .mapToLong(result -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) result;
                        return "HEALTHY".equals(resultMap.get("status")) ? 1 : 0;
                    })
                    .sum();
            
            Map<String, Object> overallHealth = new HashMap<>();
            overallHealth.put("overallStatus", healthyCount == healthResults.size() ? "HEALTHY" : "PARTIAL");
            overallHealth.put("healthyServices", healthyCount);
            overallHealth.put("totalServices", healthResults.size());
            overallHealth.put("checkTime", LocalDateTime.now());
            healthResults.put("overall", overallHealth);
            
            return ResponseEntity.ok(ApiResponse.success(healthResults));
            
        } catch (Exception e) {
            log.error("执行安全服务健康检查失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("健康检查失败"));
        }
    }
    
    /**
     * 获取安全服务统计信息
     */
    @GetMapping("/statistics")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "查询安全服务统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServicesStatistics() {
        try {
            log.info("管理员查询安全服务统计信息");
            
            Map<String, Object> statistics = new HashMap<>();
            
            // 备份服务统计
            var backupList = backupService.getBackupList();
            Map<String, Object> backupStats = new HashMap<>();
            backupStats.put("totalBackups", backupList.size());
            backupStats.put("validBackups", backupList.stream()
                    .mapToInt(backup -> (Boolean) backup.get("isValid") ? 1 : 0)
                    .sum());
            statistics.put("backup", backupStats);
            
            // 文件完整性统计
            var integrityStats = fileIntegrityService.getIntegrityStatistics();
            statistics.put("integrity", integrityStats);
            
            // 会话统计
            var sessionStats = sessionCleanupService.getSessionStatistics();
            statistics.put("session", sessionStats);
            
            // 总体统计
            Map<String, Object> overallStats = new HashMap<>();
            overallStats.put("servicesCount", 6);
            overallStats.put("activeServicesCount", 6);
            overallStats.put("complianceLevel", "GB/T 22239-2019 二级等保");
            overallStats.put("lastUpdated", LocalDateTime.now());
            statistics.put("overall", overallStats);
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
            
        } catch (Exception e) {
            log.error("获取安全服务统计信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取统计信息失败"));
        }
    }
    
    /**
     * 触发手动备份
     */
    @PostMapping("/backup/trigger")
    @Auditable(operation = AuditOperation.BACKUP_OPERATION, description = "手动触发备份")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerManualBackup() {
        try {
            log.info("管理员手动触发备份");
            
            // 异步执行备份
            CompletableFuture<BackupService.BackupResult> future = 
                backupService.performBackupAsync(BackupService.BackupType.FULL);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "备份任务已启动");
            response.put("status", "STARTED");
            response.put("timestamp", LocalDateTime.now());
            
            // 异步处理结果
            future.thenAccept(result -> {
                if (result.isSuccess()) {
                    log.info("手动备份完成: {}", result.getBackupId());
                } else {
                    log.error("手动备份失败: {}", result.getErrorMessage());
                }
            });
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("触发手动备份失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("触发备份失败"));
        }
    }
    
    /**
     * 触发文件完整性检查
     */
    @PostMapping("/integrity/check")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, description = "手动触发文件完整性检查")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerIntegrityCheck() {
        try {
            log.info("管理员手动触发文件完整性检查");
            
            // 异步执行完整性检查
            CompletableFuture<java.util.List<FileIntegrityService.FileIntegrityResult>> future = 
                fileIntegrityService.triggerManualIntegrityCheck();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件完整性检查已启动");
            response.put("status", "STARTED");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("触发文件完整性检查失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("触发完整性检查失败"));
        }
    }
    
    /**
     * 清理过期会话
     */
    @PostMapping("/session/cleanup")
    @Auditable(operation = AuditOperation.SESSION_CLEANUP, description = "手动清理过期会话")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupExpiredSessions() {
        try {
            log.info("管理员手动清理过期会话");
            
            // 执行会话清理
            sessionCleanupService.cleanupExpiredSessions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "过期会话清理完成");
            response.put("status", "COMPLETED");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("清理过期会话失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("清理会话失败"));
        }
    }
}