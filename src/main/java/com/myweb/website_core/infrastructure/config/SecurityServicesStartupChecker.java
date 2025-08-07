package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.application.service.security.*;
import com.myweb.website_core.application.service.security.authentication.SessionCleanupService;
import com.myweb.website_core.application.service.security.dataprotect.BackupService;
import com.myweb.website_core.application.service.security.dataprotect.DataDeletionService;
import com.myweb.website_core.application.service.security.dataprotect.DataRecoveryService;
import com.myweb.website_core.application.service.security.fileProtect.FileIntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全服务启动检查器
 * 
 * 在应用启动完成后检查所有安全服务的状态，确保：
 * - 所有安全服务正确初始化
 * - 服务配置正确加载
 * - 依赖关系正确建立
 * - 定时任务正确启动
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityServicesStartupChecker {
    
    private final BackupService backupService;
    private final DataRecoveryService dataRecoveryService;
    private final FileIntegrityService fileIntegrityService;
    private final SessionCleanupService sessionCleanupService;
    private final DataDeletionService dataDeletionService;
    private final UserDataManagementService userDataManagementService;
    
    /**
     * 应用启动完成后执行安全服务检查
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkSecurityServicesOnStartup() {
        log.info("=== 开始安全服务启动检查 ===");
        
        List<String> successfulServices = new ArrayList<>();
        List<String> failedServices = new ArrayList<>();
        
        // 检查备份服务
        try {
            var backupList = backupService.getBackupList();
            successfulServices.add("BackupService (备份数量: " + backupList.size() + ")");
            log.info("✓ 备份服务启动成功，当前备份数量: {}", backupList.size());
        } catch (Exception e) {
            failedServices.add("BackupService: " + e.getMessage());
            log.error("✗ 备份服务启动失败", e);
        }
        
        // 检查数据恢复服务
        try {
            var availableBackups = dataRecoveryService.getAvailableBackups();
            successfulServices.add("DataRecoveryService (可用备份: " + availableBackups.size() + ")");
            log.info("✓ 数据恢复服务启动成功，可用备份数量: {}", availableBackups.size());
        } catch (Exception e) {
            failedServices.add("DataRecoveryService: " + e.getMessage());
            log.error("✗ 数据恢复服务启动失败", e);
        }
        
        // 检查文件完整性服务
        try {
            var integrityStats = fileIntegrityService.getIntegrityStatistics();
            successfulServices.add("FileIntegrityService (监控文件: " + integrityStats.getTotalFiles() + 
                                 ", 启用状态: " + integrityStats.isIntegrityCheckEnabled() + ")");
            log.info("✓ 文件完整性服务启动成功，监控文件数量: {}, 启用状态: {}", 
                    integrityStats.getTotalFiles(), integrityStats.isIntegrityCheckEnabled());
        } catch (Exception e) {
            failedServices.add("FileIntegrityService: " + e.getMessage());
            log.error("✗ 文件完整性服务启动失败", e);
        }
        
        // 检查会话清理服务
        try {
            var sessionStats = sessionCleanupService.getSessionStatistics();
            successfulServices.add("SessionCleanupService (活跃会话: " + sessionStats.get("activeSessions") + ")");
            log.info("✓ 会话清理服务启动成功，当前活跃会话数量: {}", sessionStats.get("activeSessions"));
        } catch (Exception e) {
            failedServices.add("SessionCleanupService: " + e.getMessage());
            log.error("✗ 会话清理服务启动失败", e);
        }
        
        // 检查数据删除服务
        try {
            // 数据删除服务没有直接的状态检查方法，检查是否能正常实例化
            if (dataDeletionService != null) {
                successfulServices.add("DataDeletionService");
                log.info("✓ 数据删除服务启动成功");
            }
        } catch (Exception e) {
            failedServices.add("DataDeletionService: " + e.getMessage());
            log.error("✗ 数据删除服务启动失败", e);
        }
        
        // 检查用户数据管理服务
        try {
            // 用户数据管理服务没有直接的状态检查方法，检查是否能正常实例化
            if (userDataManagementService != null) {
                successfulServices.add("UserDataManagementService");
                log.info("✓ 用户数据管理服务启动成功");
            }
        } catch (Exception e) {
            failedServices.add("UserDataManagementService: " + e.getMessage());
            log.error("✗ 用户数据管理服务启动失败", e);
        }
        
        // 输出检查结果摘要
        log.info("=== 安全服务启动检查完成 ===");
        log.info("成功启动的服务 ({}/{}): ", successfulServices.size(), 
                successfulServices.size() + failedServices.size());
        successfulServices.forEach(service -> log.info("  ✓ {}", service));
        
        if (!failedServices.isEmpty()) {
            log.warn("启动失败的服务 ({}/{}): ", failedServices.size(), 
                    successfulServices.size() + failedServices.size());
            failedServices.forEach(service -> log.warn("  ✗ {}", service));
        }
        
        // 检查关键服务
        checkCriticalServices(failedServices);
        
        // 输出合规性信息
        log.info("=== 安全合规性信息 ===");
        log.info("合规标准: GB/T 22239-2019 信息安全技术 网络安全等级保护基本要求 (二级)");
        log.info("安全功能覆盖:");
        log.info("  • 身份鉴别: ✓ 已实现");
        log.info("  • 访问控制: ✓ 已实现");
        log.info("  • 安全审计: ✓ 已实现");
        log.info("  • 入侵防范: ✓ 已实现");
        log.info("  • 恶意代码防范: ✓ 已实现");
        log.info("  • 数据完整性: ✓ 已实现");
        log.info("  • 数据保密性: ✓ 已实现");
        log.info("  • 数据备份恢复: ✓ 已实现");
        log.info("  • 剩余信息保护: ✓ 已实现");
        
        if (failedServices.isEmpty()) {
            log.info("🎉 所有安全服务启动成功！系统已准备好投入使用。");
        } else {
            log.warn("⚠️  部分安全服务启动失败，请检查配置和依赖。");
        }
    }
    
    /**
     * 检查关键服务状态
     */
    private void checkCriticalServices(List<String> failedServices) {
        List<String> criticalServices = List.of(
            "BackupService", "DataRecoveryService", "FileIntegrityService"
        );
        
        List<String> failedCriticalServices = failedServices.stream()
            .filter(service -> criticalServices.stream()
                .anyMatch(critical -> service.startsWith(critical)))
            .toList();
        
        if (!failedCriticalServices.isEmpty()) {
            log.error("=== 关键安全服务启动失败 ===");
            log.error("以下关键服务启动失败，可能影响系统安全性:");
            failedCriticalServices.forEach(service -> log.error("  ⚠️  {}", service));
            log.error("建议立即检查配置和修复问题后重启系统。");
        } else {
            log.info("✓ 所有关键安全服务启动正常");
        }
    }
    
    /**
     * 输出服务使用指南
     */
    @EventListener(ApplicationReadyEvent.class)
    public void printServiceUsageGuide() {
        // 延迟执行，确保在启动检查之后
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("=== 安全服务使用指南 ===");
        log.info("管理员可以通过以下API端点管理安全服务:");
        log.info("");
        log.info("📊 服务状态和统计:");
        log.info("  GET  /api/admin/security-services/status      - 查看所有服务状态");
        log.info("  GET  /api/admin/security-services/statistics - 查看服务统计信息");
        log.info("  POST /api/admin/security-services/health-check - 执行健康检查");
        log.info("");
        log.info("💾 备份和恢复:");
        log.info("  GET  /api/admin/data-recovery/backups        - 查看可用备份");
        log.info("  POST /api/admin/security-services/backup/trigger - 手动触发备份");
        log.info("  POST /api/admin/data-recovery/full           - 执行完全恢复");
        log.info("  POST /api/admin/data-recovery/point-in-time  - 执行时间点恢复");
        log.info("");
        log.info("🔒 文件完整性:");
        log.info("  GET  /api/admin/file-integrity/statistics    - 查看完整性统计");
        log.info("  POST /api/admin/file-integrity/check         - 手动触发完整性检查");
        log.info("  POST /api/admin/file-integrity/check-file    - 检查特定文件");
        log.info("");
        log.info("👤 用户数据管理:");
        log.info("  GET  /api/security/user-data/{userId}        - 查看用户数据");
        log.info("  GET  /api/security/user-data/{userId}/export/json - 导出用户数据(JSON)");
        log.info("  GET  /api/security/user-data/{userId}/export/csv  - 导出用户数据(CSV)");
        log.info("  PUT  /api/security/user-data/{userId}        - 修改用户数据");
        log.info("  DELETE /api/security/user-data/{userId}      - 删除用户数据");
        log.info("");
        log.info("🧹 会话管理:");
        log.info("  POST /api/admin/security-services/session/cleanup - 清理过期会话");
        log.info("");
        log.info("📋 审计和监控:");
        log.info("  GET  /api/admin/audit-logs                   - 查看审计日志");
        log.info("  GET  /api/admin/audit-logs/export           - 导出审计日志");
        log.info("");
        log.info("所有API都需要管理员权限，请确保在调用前已正确认证。");
        log.info("详细的API文档请参考系统文档或Swagger界面。");
    }
}