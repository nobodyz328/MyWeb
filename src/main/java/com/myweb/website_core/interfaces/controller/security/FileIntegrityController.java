package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.integeration.FileIntegrityService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文件完整性管理控制器
 * 
 * 提供文件完整性检查和管理的REST API接口，包括：
 * - 手动触发完整性检查
 * - 查看完整性统计信息
 * - 检查特定文件完整性
 * - 创建文件备份
 * - 尝试文件恢复
 * 
 * 符合GB/T 22239-2019可信验证机制要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/file-integrity")
@PreAuthorize("hasRole('ADMIN')")
public class FileIntegrityController {
    
    private final FileIntegrityService fileIntegrityService;
    private final AuditLogService auditLogService;
    
    @Autowired
    public FileIntegrityController(FileIntegrityService fileIntegrityService, 
                                 AuditLogService auditLogService) {
        this.fileIntegrityService = fileIntegrityService;
        this.auditLogService = auditLogService;
    }
    
    /**
     * 获取文件完整性统计信息
     */
    @GetMapping("/statistics")
    //@Auditable(operation = "FILE_INTEGRITY_STATISTICS", resourceType = "SYSTEM")
    public ResponseEntity<FileIntegrityService.FileIntegrityStatistics> getIntegrityStatistics() {
        try {
            log.info("管理员查询文件完整性统计信息");
            
            FileIntegrityService.FileIntegrityStatistics statistics = 
                fileIntegrityService.getIntegrityStatistics();
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("获取文件完整性统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 手动触发完整性检查
     */
    @PostMapping("/check")
    //@Auditable(operation = "FILE_INTEGRITY_CHECK", resourceType = "SYSTEM")
    public ResponseEntity<Map<String, Object>> triggerIntegrityCheck() {
        try {
            log.info("管理员手动触发文件完整性检查");
            
            CompletableFuture<List<FileIntegrityService.FileIntegrityResult>> future = 
                fileIntegrityService.triggerManualIntegrityCheck();
            
            // 异步执行，立即返回响应
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件完整性检查已启动");
            response.put("status", "STARTED");
            response.put("timestamp", System.currentTimeMillis());
            
            // 异步处理结果
            future.thenAccept(results -> {
                log.info("手动完整性检查完成，检查了{}个文件", results.size());
                
                long validFiles = results.stream().filter(r -> r.isExists() && r.isValid()).count();
                long invalidFiles = results.stream().filter(r -> r.isExists() && !r.isValid()).count();
                long missingFiles = results.stream().filter(r -> !r.isExists()).count();
                
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "ADMIN",
                    String.format("手动文件完整性检查完成: 总计=%d, 有效=%d, 无效=%d, 缺失=%d", 
                            results.size(), validFiles, invalidFiles, missingFiles)
                );
            }).exceptionally(throwable -> {
                log.error("手动完整性检查失败", throwable);
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "ADMIN",
                    "手动文件完整性检查失败: " + throwable.getMessage()
                );
                return null;
            });
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("触发文件完整性检查失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "触发完整性检查失败: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 检查特定文件的完整性
     */
    @PostMapping("/check-file")
    //@Auditable(operation = "FILE_INTEGRITY_CHECK", resourceType = "FILE")
    public ResponseEntity<FileIntegrityService.FileIntegrityResult> checkSpecificFile(
            @RequestParam String filePath) {
        try {
            log.info("管理员检查特定文件完整性: {}", filePath);
            
            FileIntegrityService.FileIntegrityResult result = 
                fileIntegrityService.checkSpecificFile(filePath);
            
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "ADMIN",
                String.format("检查文件完整性: %s - %s", filePath, 
                        result.isValid() ? "通过" : "失败")
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("检查特定文件完整性失败: {}", filePath, e);
            
            FileIntegrityService.FileIntegrityResult errorResult = 
                FileIntegrityService.FileIntegrityResult.builder()
                    .filePath(filePath)
                    .exists(false)
                    .isValid(false)
                    .errorMessage("检查失败: " + e.getMessage())
                    .build();
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 创建文件备份
     */
    @PostMapping("/backup")
    //@Auditable(operation = "FILE_BACKUP", resourceType = "FILE")
    public ResponseEntity<Map<String, Object>> createFileBackup(
            @RequestParam String filePath) {
        try {
            log.info("管理员创建文件备份: {}", filePath);
            
            CompletableFuture<Boolean> future = fileIntegrityService.createFileBackup(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件备份任务已启动");
            response.put("filePath", filePath);
            response.put("status", "STARTED");
            response.put("timestamp", System.currentTimeMillis());
            
            // 异步处理结果
            future.thenAccept(success -> {
                if (success) {
                    log.info("文件备份创建成功: {}", filePath);
                    auditLogService.logSecurityEvent(
                        AuditOperation.FILE_BACKUP,
                        "ADMIN",
                        "文件备份创建成功: " + filePath
                    );
                } else {
                    log.warn("文件备份创建失败: {}", filePath);
                    auditLogService.logSecurityEvent(
                        AuditOperation.FILE_BACKUP,
                        "ADMIN",
                        "文件备份创建失败: " + filePath
                    );
                }
            });
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建文件备份失败: {}", filePath, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建文件备份失败: " + e.getMessage());
            errorResponse.put("filePath", filePath);
            errorResponse.put("status", "ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 尝试文件恢复
     */
    @PostMapping("/recover")
    //@Auditable(operation = "FILE_RECOVERY", resourceType = "FILE")
    public ResponseEntity<Map<String, Object>> attemptFileRecovery(
            @RequestParam String filePath) {
        try {
            log.info("管理员尝试文件恢复: {}", filePath);
            
            CompletableFuture<Boolean> future = fileIntegrityService.attemptFileRecovery(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件恢复任务已启动");
            response.put("filePath", filePath);
            response.put("status", "STARTED");
            response.put("timestamp", System.currentTimeMillis());
            
            // 异步处理结果
            future.thenAccept(success -> {
                if (success) {
                    log.info("文件恢复成功: {}", filePath);
                    auditLogService.logSecurityEvent(
                        AuditOperation.FILE_RECOVERY,
                        "ADMIN",
                        "文件恢复成功: " + filePath
                    );
                } else {
                    log.warn("文件恢复失败: {}", filePath);
                    auditLogService.logSecurityEvent(
                        AuditOperation.FILE_RECOVERY,
                        "ADMIN",
                        "文件恢复失败: " + filePath
                    );
                }
            });
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("尝试文件恢复失败: {}", filePath, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "文件恢复失败: " + e.getMessage());
            errorResponse.put("filePath", filePath);
            errorResponse.put("status", "ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取支持的哈希算法
     */
    @GetMapping("/hash-algorithms")
    public ResponseEntity<Map<String, Object>> getSupportedHashAlgorithms() {
        try {
            String[] algorithms = fileIntegrityService.getSupportedHashAlgorithms();
            
            Map<String, Object> response = new HashMap<>();
            response.put("algorithms", algorithms);
            response.put("count", algorithms.length);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取支持的哈希算法失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取文件完整性服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        try {
            FileIntegrityService.FileIntegrityStatistics statistics = 
                fileIntegrityService.getIntegrityStatistics();
            
            Map<String, Object> status = new HashMap<>();
            status.put("enabled", statistics.isIntegrityCheckEnabled());
            status.put("alertEnabled", statistics.isAlertEnabled());
            status.put("totalFiles", statistics.getTotalFiles());
            status.put("validFiles", statistics.getValidFiles());
            status.put("invalidFiles", statistics.getInvalidFiles());
            status.put("lastCheckTime", statistics.getLastCheckTime());
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取文件完整性服务状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}