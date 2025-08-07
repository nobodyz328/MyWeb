package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.dataprotect.BackupService;
import com.myweb.website_core.application.service.security.dataprotect.BackupService.BackupResult;
import com.myweb.website_core.application.service.security.dataprotect.BackupService.BackupType;
import com.myweb.website_core.common.validation.SafeString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 数据备份管理控制器
 * 
 * 提供数据备份相关的REST API接口，包括：
 * - 手动触发备份
 * - 查看备份列表
 * - 验证备份完整性
 * - 清理过期备份
 * - 存储空间监控
 * 
 * 符合GB/T 22239-2019二级等保要求的数据备份管理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@RestController
@RequestMapping("/api/security/backup")
@RequiredArgsConstructor
@Validated
public class BackupController {
    
    private final BackupService backupService;
    
    /**
     * 手动触发备份
     * 
     * @param backupType 备份类型
     * @return 备份结果
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerBackup(
            @RequestParam(defaultValue = "FULL") String backupType) {
        
        log.info("收到手动备份请求: backupType={}", backupType);
        
        try {
            BackupType type = BackupType.valueOf(backupType.toUpperCase());
            
            // 异步执行备份
            CompletableFuture<BackupResult> future = backupService.performBackupAsync(type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "备份任务已启动");
            response.put("backupType", type.getDisplayName());
            response.put("status", "RUNNING");
            
            log.info("手动备份任务已启动: backupType={}", type);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("无效的备份类型: {}", backupType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "无效的备份类型: " + backupType);
            response.put("availableTypes", new String[]{"FULL", "INCREMENTAL", "DIFFERENTIAL"});
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("触发备份失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "触发备份失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取备份列表
     * 
     * @return 备份文件列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getBackupList() {
        
        log.info("收到获取备份列表请求");
        
        try {
            List<Map<String, Object>> backupList = backupService.getBackupList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", backupList);
            response.put("total", backupList.size());
            
            log.info("获取备份列表成功: 共{}个备份文件", backupList.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取备份列表失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取备份列表失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 验证备份完整性
     * 
     * @param filePath 备份文件路径
     * @return 验证结果
     */
    @PostMapping("/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyBackupIntegrity(
            @RequestParam @SafeString String filePath) {
        
        log.info("收到备份完整性验证请求: filePath={}", filePath);
        
        try {
            boolean isValid = backupService.verifyBackupIntegrity(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            response.put("isValid", isValid);
            response.put("message", isValid ? "备份文件完整性验证通过" : "备份文件完整性验证失败");
            
            log.info("备份完整性验证完成: filePath={}, isValid={}", filePath, isValid);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("验证备份完整性失败: filePath={}", filePath, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "验证备份完整性失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 清理过期备份
     * 
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupExpiredBackups() {
        
        log.info("收到清理过期备份请求");
        
        try {
            backupService.cleanupExpiredBackups();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "过期备份清理完成");
            
            log.info("过期备份清理完成");
            
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
     * 检查存储空间
     * 
     * @return 存储空间信息
     */
    @GetMapping("/storage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkStorageSpace() {
        
        log.info("收到存储空间检查请求");
        
        try {
            // 触发存储空间检查
            backupService.checkStorageSpaceAndAlert();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "存储空间检查完成");
            
            log.info("存储空间检查完成");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("检查存储空间失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查存储空间失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取备份配置信息
     * 
     * @return 备份配置
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getBackupConfig() {
        
        log.info("收到获取备份配置请求");
        
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("backupTypes", new String[]{"FULL", "INCREMENTAL", "DIFFERENTIAL"});
            config.put("scheduledBackupTime", "每日凌晨2点");
            config.put("encryptionEnabled", true);
            config.put("compressionEnabled", true);
            config.put("notificationEnabled", true);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);
            
            log.info("获取备份配置成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取备份配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取备份配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}