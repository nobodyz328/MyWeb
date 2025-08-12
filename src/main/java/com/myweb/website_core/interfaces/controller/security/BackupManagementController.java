package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.integeration.dataManage.BackupManagementService;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 备份管理控制器
 * <p>
 * 提供备份管理的REST API接口，包括：
 * - 备份文件生命周期管理
 * - 存储空间监控
 * - 备份策略配置
 * - 远程存储同步
 * <p>
 * 符合GB/T 22239-2019二级等保要求8.5、8.6
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-07
 */
@Slf4j
@RestController
@RequestMapping("/api/security/backup-management")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BackupManagementController {
    
    private final BackupManagementService backupManagementService;
    private final AuditLogServiceAdapter auditLogService;
    
    /**
     * 获取备份文件列表及元数据
     */
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> getBackupFiles() {
        try {
            log.info("获取备份文件列表");
            
            List<Map<String, Object>> backupFiles = backupManagementService.getBackupFilesWithMetadata();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取备份文件列表成功");
            response.put("data", backupFiles);
            response.put("total", backupFiles.size());
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    String.format("查询备份文件列表 - 文件数量: %d", backupFiles.size())
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取备份文件列表失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取备份文件列表失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取存储统计信息
     */
    @GetMapping("/storage/statistics")
    public ResponseEntity<Map<String, Object>> getStorageStatistics() {
        try {
            log.info("获取存储统计信息");
            
            BackupManagementService.StorageStatistics stats = backupManagementService.checkStorageStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取存储统计信息成功");
            response.put("data", createStorageStatsResponse(stats));
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    String.format("查询存储统计信息 - 使用率: %.2f%%", stats.getUsagePercentage())
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取存储统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取存储统计信息失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 手动清理过期备份
     */
    @PostMapping("/cleanup/expired")
    public ResponseEntity<Map<String, Object>> cleanupExpiredBackups() {
        try {
            log.info("手动清理过期备份");
            
            CompletableFuture<Integer> cleanupFuture = backupManagementService.cleanupExpiredBackupsAsync();
            int deletedCount = cleanupFuture.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "清理过期备份完成");
            response.put("data", Map.of("deletedCount", deletedCount));
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    String.format("手动清理过期备份 - 删除文件数: %d", deletedCount)
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("清理过期备份失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清理过期备份失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 手动同步到远程存储
     */
    @PostMapping("/sync/remote")
    public ResponseEntity<Map<String, Object>> syncToRemoteStorage() {
        try {
            log.info("手动同步到远程存储");
            
            CompletableFuture<Void> syncFuture = backupManagementService.syncToRemoteStorageAsync();
            syncFuture.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "远程存储同步完成");
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    "手动触发远程存储同步"
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("远程存储同步失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "远程存储同步失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新备份策略配置
     */
    @PutMapping("/policy")
    public ResponseEntity<Map<String, Object>> updateBackupPolicy(@RequestBody Map<String, Object> policyUpdates) {
        try {
            log.info("更新备份策略配置: {}", policyUpdates);
            
            // 验证输入参数
            validatePolicyUpdates(policyUpdates);
            
            backupManagementService.updateBackupPolicy(policyUpdates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "备份策略配置更新成功");
            response.put("data", policyUpdates);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    "更新备份策略配置: " + policyUpdates.toString()
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("备份策略配置参数无效: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "配置参数无效: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("更新备份策略配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新备份策略配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 手动执行备份生命周期管理
     */
    @PostMapping("/lifecycle/manage")
    @Auditable(operation = AuditOperation.BACKUP_OPERATION,
            description = "执行备份生命周期管理任务",
            resourceType = "BACKUP"
    )
    public ResponseEntity<Map<String, Object>> manageBackupLifecycle() {
        try {
            log.info("手动执行备份生命周期管理");
            
            // 异步执行生命周期管理任务
            CompletableFuture.runAsync(() -> {
                try {
                    backupManagementService.scheduledLifecycleManagement();
                } catch (Exception e) {
                    log.error("备份生命周期管理执行失败", e);
                }
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "备份生命周期管理任务已启动");
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    "手动触发备份生命周期管理"
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("启动备份生命周期管理失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "启动备份生命周期管理失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取备份管理状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBackupManagementStatus() {
        try {
            log.info("获取备份管理状态");
            
            // 获取存储统计信息
            BackupManagementService.StorageStatistics stats = backupManagementService.checkStorageStatistics();
            
            // 获取备份文件数量
            List<Map<String, Object>> backupFiles = backupManagementService.getBackupFilesWithMetadata();
            
            // 统计过期文件数量
            long expiredCount = backupFiles.stream()
                .filter(file -> Boolean.TRUE.equals(file.get("isExpired")))
                .count();
            
            // 统计需要同步的文件数量
            long needSyncCount = backupFiles.stream()
                .filter(file -> Boolean.FALSE.equals(file.get("remoteSynced")))
                .count();
            
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("totalFiles", backupFiles.size());
            statusData.put("expiredFiles", expiredCount);
            statusData.put("needSyncFiles", needSyncCount);
            statusData.put("storageUsagePercentage", stats.getUsagePercentage());
            statusData.put("storageAlertRequired", stats.isStorageAlertRequired());
            statusData.put("totalSpaceFormatted", stats.getFormattedTotalSpace());
            statusData.put("usedSpaceFormatted", stats.getFormattedUsedSpace());
            statusData.put("availableSpaceFormatted", stats.getFormattedAvailableSpace());
            statusData.put("backupTypeCount", stats.getBackupTypeCount());
            statusData.put("oldestBackupDays", stats.getOldestBackupDays());
            statusData.put("newestBackupDays", stats.getNewestBackupDays());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取备份管理状态成功");
            response.put("data", statusData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取备份管理状态失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取备份管理状态失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 手动更新备份元数据
     */
    @PostMapping("/metadata/update")
    public ResponseEntity<Map<String, Object>> updateBackupMetadata() {
        try {
            log.info("手动更新备份元数据");
            
            CompletableFuture<Void> updateFuture = backupManagementService.updateBackupMetadataAsync();
            updateFuture.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "备份元数据更新完成");
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    "手动更新备份元数据"
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新备份元数据失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新备份元数据失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建存储统计信息响应
     */
    private Map<String, Object> createStorageStatsResponse(BackupManagementService.StorageStatistics stats) {
        Map<String, Object> data = new HashMap<>();
        data.put("totalSpaceBytes", stats.getTotalSpaceBytes());
        data.put("usedSpaceBytes", stats.getUsedSpaceBytes());
        data.put("availableSpaceBytes", stats.getAvailableSpaceBytes());
        data.put("usagePercentage", stats.getUsagePercentage());
        data.put("totalFiles", stats.getTotalBackupFiles());
        data.put("oldestBackupDays", stats.getOldestBackupDays());
        data.put("newestBackupDays", stats.getNewestBackupDays());
        data.put("backupTypeCount", stats.getBackupTypeCount());
        data.put("isAlertRequired", stats.isStorageAlertRequired());
        data.put("totalSpaceFormatted", stats.getFormattedTotalSpace());
        data.put("usedSpaceFormatted", stats.getFormattedUsedSpace());
        data.put("availableSpaceFormatted", stats.getFormattedAvailableSpace());
        
        return data;
    }
    
    /**
     * 验证策略更新参数
     */
    private void validatePolicyUpdates(Map<String, Object> policyUpdates) {
        if (policyUpdates == null || policyUpdates.isEmpty()) {
            throw new IllegalArgumentException("策略更新参数不能为空");
        }
        
        // 验证保留天数
        if (policyUpdates.containsKey("retentionDays")) {
            Object value = policyUpdates.get("retentionDays");
            if (!(value instanceof Integer) || (Integer) value <= 0 || (Integer) value > 365) {
                throw new IllegalArgumentException("保留天数必须是1-365之间的整数");
            }
        }
        
        // 验证存储告警阈值
        if (policyUpdates.containsKey("storageAlertThreshold")) {
            Object value = policyUpdates.get("storageAlertThreshold");
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException("存储告警阈值必须是数字");
            }
            double threshold = ((Number) value).doubleValue();
            if (threshold <= 0 || threshold > 1.0) {
                throw new IllegalArgumentException("存储告警阈值必须是0-1之间的小数");
            }
        }
        
        // 验证布尔值参数
        String[] booleanParams = {"encryptionEnabled", "remoteStorageEnabled"};
        for (String param : booleanParams) {
            if (policyUpdates.containsKey(param)) {
                Object value = policyUpdates.get(param);
                if (!(value instanceof Boolean)) {
                    throw new IllegalArgumentException(param + "必须是布尔值");
                }
            }
        }
    }
}