package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.audit.LogStorageManagementService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.LogStorageStatistics;
import com.myweb.website_core.domain.business.dto.StorageInfo;
import com.myweb.website_core.infrastructure.security.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 日志存储管理控制器
 * 
 * 提供日志存储管理的REST API接口，包括：
 * - 存储空间监控
 * - 日志备份管理
 * - 完整性检查
 * - 存储统计信息
 * 
 * 符合GB/T 22239-2019安全审计要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/log-storage")
@PreAuthorize("hasRole('ADMIN')")
public class LogStorageController {
    
    private final LogStorageManagementService logStorageManagementService;
    
    @Autowired
    public LogStorageController(LogStorageManagementService logStorageManagementService) {
        this.logStorageManagementService = logStorageManagementService;
    }
    
    /**
     * 获取日志存储统计信息
     * 
     * @return 存储统计信息
     */
    @GetMapping("/statistics")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "LOG_STORAGE")
    public ResponseEntity<LogStorageStatistics> getStorageStatistics() {
        try {
            LogStorageStatistics statistics = logStorageManagementService.getStorageStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取日志存储统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查存储空间使用情况
     * 
     * @return 存储空间信息
     */
    @GetMapping("/space-check")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, resourceType = "LOG_STORAGE")
    public ResponseEntity<StorageInfo> checkStorageSpace() {
        try {
            StorageInfo storageInfo = logStorageManagementService.checkStorageSpace();
            return ResponseEntity.ok(storageInfo);
        } catch (Exception e) {
            log.error("检查存储空间失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 手动触发日志备份
     * 
     * @return 备份操作结果
     */
    @PostMapping("/backup")
    @Auditable(operation = AuditOperation.BACKUP_OPERATION, resourceType = "LOG_STORAGE")
    public ResponseEntity<String> triggerLogBackup() {
        try {
            CompletableFuture<Void> backupFuture = logStorageManagementService.triggerLogBackup();
            
            // 异步执行备份，立即返回响应
            backupFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("日志备份执行失败", throwable);
                } else {
                    log.info("日志备份执行成功");
                }
            });
            
            return ResponseEntity.ok("日志备份已启动，请稍后查看备份结果");
        } catch (Exception e) {
            log.error("触发日志备份失败", e);
            return ResponseEntity.internalServerError().body("触发日志备份失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发完整性检查
     * 
     * @param filePath 文件路径（可选，为空则检查所有文件）
     * @return 检查结果
     */
    @PostMapping("/integrity-check")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "LOG_STORAGE")
    public ResponseEntity<String> triggerIntegrityCheck(
            @RequestParam(required = false) String filePath) {
        try {
            boolean result = logStorageManagementService.triggerIntegrityCheck(filePath);
            
            if (filePath != null && !filePath.isEmpty()) {
                return ResponseEntity.ok(result ? "文件完整性检查通过" : "文件完整性检查失败");
            } else {
                return ResponseEntity.ok("完整性检查已启动，请查看日志获取详细结果");
            }
        } catch (Exception e) {
            log.error("触发完整性检查失败", e);
            return ResponseEntity.internalServerError().body("触发完整性检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取存储健康状态
     * 
     * @return 健康状态信息
     */
    @GetMapping("/health")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, resourceType = "LOG_STORAGE")
    public ResponseEntity<StorageHealthInfo> getStorageHealth() {
        try {
            LogStorageStatistics statistics = logStorageManagementService.getStorageStatistics();
            
            StorageHealthInfo healthInfo = StorageHealthInfo.builder()
                    .status(statistics.getHealthStatus())
                    .overallUsage(statistics.getOverallUsagePercentage())
                    .logStorageUsage(statistics.getLogStorage() != null ? 
                            statistics.getLogStorage().getUsagePercentage() : 0.0)
                    .backupStorageUsage(statistics.getBackupStorage() != null ? 
                            statistics.getBackupStorage().getUsagePercentage() : 0.0)
                    .totalFiles(statistics.getTotalFileCount())
                    .compressionEnabled(statistics.isCompressionEnabled())
                    .integrityCheckEnabled(statistics.isIntegrityCheckEnabled())
                    .needsAttention(statistics.needsCleanup(85.0))
                    .build();
            
            return ResponseEntity.ok(healthInfo);
        } catch (Exception e) {
            log.error("获取存储健康状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 存储健康状态信息DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class StorageHealthInfo {
        /**
         * 健康状态
         */
        private String status;
        
        /**
         * 整体使用率
         */
        private double overallUsage;
        
        /**
         * 日志存储使用率
         */
        private double logStorageUsage;
        
        /**
         * 备份存储使用率
         */
        private double backupStorageUsage;
        
        /**
         * 总文件数
         */
        private long totalFiles;
        
        /**
         * 是否启用压缩
         */
        private boolean compressionEnabled;
        
        /**
         * 是否启用完整性检查
         */
        private boolean integrityCheckEnabled;
        
        /**
         * 是否需要关注
         */
        private boolean needsAttention;
    }
}