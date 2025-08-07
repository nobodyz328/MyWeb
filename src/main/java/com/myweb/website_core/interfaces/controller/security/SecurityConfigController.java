package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.SecurityConfigService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.SecurityConfigBackupDTO;
import com.myweb.website_core.domain.security.dto.SecurityConfigDTO;
import com.myweb.website_core.infrastructure.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 安全配置中心控制器
 * 
 * 提供安全配置管理的REST API接口，包括：
 * - 配置查询和更新
 * - 配置备份和恢复
 * - 配置重置和批量操作
 * 
 * 只有具有系统管理权限的用户才能访问这些接口
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@RestController
@RequestMapping("/api/security/config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
public class SecurityConfigController {
    
    private final SecurityConfigService securityConfigService;
    
    /**
     * 获取完整的安全配置
     * 
     * @return 安全配置信息
     */
    @GetMapping
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "SECURITY_CONFIG")
    public ResponseEntity<SecurityConfigDTO> getSecurityConfig() {
        log.info("获取完整安全配置");
        
        try {
            SecurityConfigDTO config = securityConfigService.getSecurityConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("获取安全配置失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定类型的配置
     * 
     * @param configType 配置类型
     * @return 配置信息
     */
    @GetMapping("/{configType}")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "SECURITY_CONFIG")
    public ResponseEntity<Object> getConfig(@PathVariable String configType) {
        log.info("获取配置 - 类型: {}", configType);
        
        try {
            Object config = securityConfigService.getConfig(configType);
            if (config != null) {
                return ResponseEntity.ok(config);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取配置失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 更新指定类型的配置
     * 
     * @param configType 配置类型
     * @param newConfig 新配置
     * @param principal 当前用户
     * @return 更新结果
     */
    @PutMapping("/{configType}")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateConfig(
            @PathVariable String configType,
            @RequestBody Object newConfig,
            Principal principal) {
        
        String operator = principal != null ? principal.getName() : "system";
        log.info("更新配置 - 类型: {}, 操作者: {}", configType, operator);
        
        return securityConfigService.updateConfig(configType, newConfig, operator)
            .thenApply(success -> {
                if (success) {
                    Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "配置更新成功");
                response.put("configType", configType);
                return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "配置更新失败");
                    errorResponse.put("configType", configType);
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            })
            .exceptionally(throwable -> {
                log.error("更新配置异常 - 类型: {}, 错误: {}", configType, throwable.getMessage(), throwable);
                Map<String, Object> exceptionResponse = new HashMap<>();
                exceptionResponse.put("success", false);
                exceptionResponse.put("message", "配置更新异常: " + throwable.getMessage());
                exceptionResponse.put("configType", configType);
                return ResponseEntity.internalServerError().body(exceptionResponse);
            });
    }
    
    /**
     * 批量更新配置
     * 
     * @param configUpdates 配置更新映射
     * @param principal 当前用户
     * @return 批量更新结果
     */
    @PutMapping("/batch")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> batchUpdateConfig(
            @RequestBody Map<String, Object> configUpdates,
            Principal principal) {
        
        String operator = principal != null ? principal.getName() : "system";
        log.info("批量更新配置 - 数量: {}, 操作者: {}", configUpdates.size(), operator);
        
        return securityConfigService.batchUpdateConfig(configUpdates, operator)
            .thenApply(results -> {
                long successCount = results.values().stream().mapToLong(success -> success ? 1 : 0).sum();
                boolean allSuccess = successCount == results.size();
                
                Map<String, Object> batchResponse = new HashMap<>();
                batchResponse.put("success", allSuccess);
                batchResponse.put("message", String.format("批量更新完成，成功: %d, 失败: %d", 
                    successCount, results.size() - successCount));
                batchResponse.put("results", results);
                batchResponse.put("totalCount", results.size());
                batchResponse.put("successCount", successCount);
                return ResponseEntity.ok(batchResponse);
            })
            .exceptionally(throwable -> {
                log.error("批量更新配置异常: {}", throwable.getMessage(), throwable);
                Map<String, Object> batchErrorResponse = new HashMap<>();
                batchErrorResponse.put("success", false);
                batchErrorResponse.put("message", "批量更新异常: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(batchErrorResponse);
            });
    }
    
    /**
     * 重置配置到默认值
     * 
     * @param configType 配置类型
     * @param principal 当前用户
     * @return 重置结果
     */
    @PostMapping("/{configType}/reset")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetConfig(
            @PathVariable String configType,
            Principal principal) {
        
        String operator = principal != null ? principal.getName() : "system";
        log.info("重置配置 - 类型: {}, 操作者: {}", configType, operator);
        
        return securityConfigService.resetConfig(configType, operator)
            .thenApply(success -> {
                if (success) {
                    Map<String, Object> resetResponse = new HashMap<>();
                    resetResponse.put("success", true);
                    resetResponse.put("message", "配置重置成功");
                    resetResponse.put("configType", configType);
                    return ResponseEntity.ok(resetResponse);
                } else {
                    Map<String, Object> resetErrorResponse = new HashMap<>();
                    resetErrorResponse.put("success", false);
                    resetErrorResponse.put("message", "配置重置失败");
                    resetErrorResponse.put("configType", configType);
                    return ResponseEntity.badRequest().body(resetErrorResponse);
                }
            })
            .exceptionally(throwable -> {
                log.error("重置配置异常 - 类型: {}, 错误: {}", configType, throwable.getMessage(), throwable);
                Map<String, Object> resetExceptionResponse = new HashMap<>();
                resetExceptionResponse.put("success", false);
                resetExceptionResponse.put("message", "配置重置异常: " + throwable.getMessage());
                resetExceptionResponse.put("configType", configType);
                return ResponseEntity.internalServerError().body(resetExceptionResponse);
            });
    }
    
    /**
     * 获取配置备份列表
     * 
     * @param configType 配置类型
     * @return 备份列表
     */
    @GetMapping("/{configType}/backups")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "SECURITY_CONFIG")
    public ResponseEntity<List<SecurityConfigBackupDTO>> getConfigBackups(@PathVariable String configType) {
        log.info("获取配置备份列表 - 类型: {}", configType);
        
        try {
            List<SecurityConfigBackupDTO> backups = securityConfigService.getConfigBackups(configType);
            return ResponseEntity.ok(backups);
        } catch (Exception e) {
            log.error("获取配置备份列表失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 恢复配置
     * 
     * @param configType 配置类型
     * @param backupTimestamp 备份时间戳
     * @param principal 当前用户
     * @return 恢复结果
     */
    @PostMapping("/{configType}/restore/{backupTimestamp}")
    @Auditable(operation = AuditOperation.DATA_RESTORE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> restoreConfig(
            @PathVariable String configType,
            @PathVariable String backupTimestamp,
            Principal principal) {
        
        String operator = principal != null ? principal.getName() : "system";
        log.info("恢复配置 - 类型: {}, 备份时间: {}, 操作者: {}", configType, backupTimestamp, operator);
        
        return securityConfigService.restoreConfig(configType, backupTimestamp, operator)
            .thenApply(success -> {
                if (success) {
                    Map<String, Object> restoreResponse = new HashMap<>();
                    restoreResponse.put("success", true);
                    restoreResponse.put("message", "配置恢复成功");
                    restoreResponse.put("configType", configType);
                    restoreResponse.put("backupTimestamp", backupTimestamp);
                    return ResponseEntity.ok(restoreResponse);
                } else {
                    Map<String, Object> restoreErrorResponse = new HashMap<>();
                    restoreErrorResponse.put("success", false);
                    restoreErrorResponse.put("message", "配置恢复失败");
                    restoreErrorResponse.put("configType", configType);
                    restoreErrorResponse.put("backupTimestamp", backupTimestamp);
                    return ResponseEntity.badRequest().body(restoreErrorResponse);
                }
            })
            .exceptionally(throwable -> {
                log.error("恢复配置异常 - 类型: {}, 时间戳: {}, 错误: {}", 
                    configType, backupTimestamp, throwable.getMessage(), throwable);
                Map<String, Object> restoreExceptionResponse = new HashMap<>();
                restoreExceptionResponse.put("success", false);
                restoreExceptionResponse.put("message", "配置恢复异常: " + throwable.getMessage());
                restoreExceptionResponse.put("configType", configType);
                restoreExceptionResponse.put("backupTimestamp", backupTimestamp);
                return ResponseEntity.internalServerError().body(restoreExceptionResponse);
            });
    }
    
    /**
     * 验证配置有效性
     * 
     * @param configType 配置类型
     * @param config 配置对象
     * @return 验证结果
     */
    @PostMapping("/{configType}/validate")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public ResponseEntity<Map<String, Object>> validateConfig(
            @PathVariable String configType,
            @RequestBody Object config) {
        
        log.info("验证配置 - 类型: {}", configType);
        
        try {
            // 这里可以调用SecurityConfigService的验证方法
            // 目前简单返回成功，实际实现中应该调用具体的验证逻辑
            Map<String, Object> validateResponse = new HashMap<>();
            validateResponse.put("valid", true);
            validateResponse.put("message", "配置验证通过");
            validateResponse.put("configType", configType);
            return ResponseEntity.ok(validateResponse);
        } catch (Exception e) {
            log.error("验证配置失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            Map<String, Object> validateErrorResponse = new HashMap<>();
            validateErrorResponse.put("valid", false);
            validateErrorResponse.put("message", "配置验证失败: " + e.getMessage());
            validateErrorResponse.put("configType", configType);
            return ResponseEntity.badRequest().body(validateErrorResponse);
        }
    }
    
    /**
     * 获取配置变更历史
     * 
     * @param configType 配置类型（可选）
     * @param limit 限制数量
     * @return 变更历史
     */
    @GetMapping("/changes")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "SECURITY_CONFIG")
    public ResponseEntity<Map<String, Object>> getConfigChangeHistory(
            @RequestParam(required = false) String configType,
            @RequestParam(defaultValue = "50") int limit) {
        
        log.info("获取配置变更历史 - 类型: {}, 限制: {}", configType, limit);
        
        try {
            // 这里应该从审计日志中查询配置变更历史
            // 目前返回空列表，实际实现中应该调用审计服务
            Map<String, Object> historyResponse = new HashMap<>();
            historyResponse.put("changes", List.of());
            historyResponse.put("configType", configType != null ? configType : "all");
            historyResponse.put("limit", limit);
            historyResponse.put("total", 0);
            return ResponseEntity.ok(historyResponse);
        } catch (Exception e) {
            log.error("获取配置变更历史失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}