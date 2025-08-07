package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.dataprotect.DataRecoveryService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.infrastructure.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据恢复控制器
 * 
 * 提供数据恢复管理功能，包括：
 * - 完全恢复
 * - 时间点恢复
 * - 选择性恢复
 * - 恢复前提条件验证
 * - 可用备份查询
 * 
 * 符合GB/T 22239-2019二级等保要求的数据恢复机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/data-recovery")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DataRecoveryController {
    
    private final DataRecoveryService dataRecoveryService;
    
    /**
     * 获取可用备份列表
     * 
     * @return 备份列表
     */
    @GetMapping("/backups")
    @Auditable(operation = AuditOperation.AUDIT_LOG_QUERY, description = "查询可用备份列表")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableBackups() {
        try {
            List<Map<String, Object>> backups = dataRecoveryService.getAvailableBackups();
            return ResponseEntity.ok(ApiResponse.success(backups));
            
        } catch (Exception e) {
            log.error("获取可用备份列表失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取备份列表失败"));
        }
    }
    
    /**
     * 验证恢复前提条件
     * 
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "验证恢复前提条件")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateRecoveryPrerequisites(
            @RequestBody ValidationRequest request) {
        
        try {
            DataRecoveryService.RecoveryType recoveryType = 
                    DataRecoveryService.RecoveryType.valueOf(request.getRecoveryType());
            
            Map<String, Object> result = dataRecoveryService.validateRecoveryPrerequisites(
                recoveryType, request.getBackupFilePath()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (IllegalArgumentException e) {
            log.warn("无效的恢复类型: {}", request.getRecoveryType());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的恢复类型"));
        } catch (Exception e) {
            log.error("验证恢复前提条件失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("验证失败"));
        }
    }
    
    /**
     * 执行完全恢复
     * 
     * @param request 恢复请求
     * @param authentication 认证信息
     * @return 恢复结果
     */
    @PostMapping("/full")
    @Auditable(operation = AuditOperation.DATA_RESTORE, description = "执行完全数据恢复")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performFullRecovery(
            @RequestBody FullRecoveryRequest request,
            Authentication authentication) {
        
        try {
            String userId = authentication.getName();
            
            DataRecoveryService.RecoveryResult result = dataRecoveryService.performFullRecovery(
                request.getBackupFilePath(), userId
            );
            
            Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "recoveryId", result.getRecoveryId(),
                "recoveryType", result.getRecoveryType().getDisplayName(),
                "backupFile", result.getBackupFile() != null ? result.getBackupFile() : "",
                "startTime", result.getStartTime(),
                "endTime", result.getEndTime(),
                "durationMillis", result.getDurationMillis(),
                "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : "",
                "details", result.getDetails()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("恢复失败: " + result.getErrorMessage(), response));
            }
            
        } catch (Exception e) {
            log.error("执行完全恢复失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("执行恢复失败"));
        }
    }
    
    /**
     * 执行时间点恢复
     * 
     * @param request 恢复请求
     * @param authentication 认证信息
     * @return 恢复结果
     */
    @PostMapping("/point-in-time")
    @Auditable(operation = AuditOperation.DATA_RESTORE, description = "执行时间点数据恢复")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performPointInTimeRecovery(
            @RequestBody PointInTimeRecoveryRequest request,
            Authentication authentication) {
        
        try {
            String userId = authentication.getName();
            
            DataRecoveryService.RecoveryResult result = dataRecoveryService.performPointInTimeRecovery(
                request.getTargetDateTime(), userId
            );
            
            Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "recoveryId", result.getRecoveryId(),
                "recoveryType", result.getRecoveryType().getDisplayName(),
                "backupFile", result.getBackupFile() != null ? result.getBackupFile() : "",
                "startTime", result.getStartTime(),
                "endTime", result.getEndTime(),
                "durationMillis", result.getDurationMillis(),
                "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : "",
                "details", result.getDetails()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("恢复失败: " + result.getErrorMessage(), response));
            }
            
        } catch (Exception e) {
            log.error("执行时间点恢复失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("执行恢复失败"));
        }
    }
    
    /**
     * 执行选择性恢复
     * 
     * @param request 恢复请求
     * @param authentication 认证信息
     * @return 恢复结果
     */
    @PostMapping("/selective")
    @Auditable(operation = AuditOperation.DATA_RESTORE, description = "执行选择性数据恢复")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performSelectiveRecovery(
            @RequestBody SelectiveRecoveryRequest request,
            Authentication authentication) {
        
        try {
            String userId = authentication.getName();
            
            DataRecoveryService.RecoveryResult result = dataRecoveryService.performSelectiveRecovery(
                request.getBackupFilePath(), request.getTablesToRestore(), userId
            );
            
            Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "recoveryId", result.getRecoveryId(),
                "recoveryType", result.getRecoveryType().getDisplayName(),
                "backupFile", result.getBackupFile() != null ? result.getBackupFile() : "",
                "startTime", result.getStartTime(),
                "endTime", result.getEndTime(),
                "durationMillis", result.getDurationMillis(),
                "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : "",
                "details", result.getDetails()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("恢复失败: " + result.getErrorMessage(), response));
            }
            
        } catch (Exception e) {
            log.error("执行选择性恢复失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("执行恢复失败"));
        }
    }
    
    /**
     * 验证请求DTO
     */
    public static class ValidationRequest {
        private String recoveryType;
        private String backupFilePath;
        
        // Constructors
        public ValidationRequest() {}
        
        public ValidationRequest(String recoveryType, String backupFilePath) {
            this.recoveryType = recoveryType;
            this.backupFilePath = backupFilePath;
        }
        
        // Getters and Setters
        public String getRecoveryType() {
            return recoveryType;
        }
        
        public void setRecoveryType(String recoveryType) {
            this.recoveryType = recoveryType;
        }
        
        public String getBackupFilePath() {
            return backupFilePath;
        }
        
        public void setBackupFilePath(String backupFilePath) {
            this.backupFilePath = backupFilePath;
        }
    }
    
    /**
     * 完全恢复请求DTO
     */
    public static class FullRecoveryRequest {
        private String backupFilePath;
        
        // Constructors
        public FullRecoveryRequest() {}
        
        public FullRecoveryRequest(String backupFilePath) {
            this.backupFilePath = backupFilePath;
        }
        
        // Getters and Setters
        public String getBackupFilePath() {
            return backupFilePath;
        }
        
        public void setBackupFilePath(String backupFilePath) {
            this.backupFilePath = backupFilePath;
        }
    }
    
    /**
     * 时间点恢复请求DTO
     */
    public static class PointInTimeRecoveryRequest {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime targetDateTime;
        
        // Constructors
        public PointInTimeRecoveryRequest() {}
        
        public PointInTimeRecoveryRequest(LocalDateTime targetDateTime) {
            this.targetDateTime = targetDateTime;
        }
        
        // Getters and Setters
        public LocalDateTime getTargetDateTime() {
            return targetDateTime;
        }
        
        public void setTargetDateTime(LocalDateTime targetDateTime) {
            this.targetDateTime = targetDateTime;
        }
    }
    
    /**
     * 选择性恢复请求DTO
     */
    public static class SelectiveRecoveryRequest {
        private String backupFilePath;
        private List<String> tablesToRestore;
        
        // Constructors
        public SelectiveRecoveryRequest() {}
        
        public SelectiveRecoveryRequest(String backupFilePath, List<String> tablesToRestore) {
            this.backupFilePath = backupFilePath;
            this.tablesToRestore = tablesToRestore;
        }
        
        // Getters and Setters
        public String getBackupFilePath() {
            return backupFilePath;
        }
        
        public void setBackupFilePath(String backupFilePath) {
            this.backupFilePath = backupFilePath;
        }
        
        public List<String> getTablesToRestore() {
            return tablesToRestore;
        }
        
        public void setTablesToRestore(List<String> tablesToRestore) {
            this.tablesToRestore = tablesToRestore;
        }
    }
}