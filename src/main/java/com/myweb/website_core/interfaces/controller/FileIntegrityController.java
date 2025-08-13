package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.file.FileIntegrityManagementService;
import com.myweb.website_core.application.service.file.FileIntegrityManagementService.FileIntegrityReport;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.PermissionUtils;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import com.myweb.website_core.common.enums.AuditOperation;
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
 * 文件完整性管理控制器
 * <p>
 * 提供文件完整性管理的API接口：
 * - 手动触发完整性检查
 * - 获取完整性报告
 * - 查看完整性统计信息
 * - 管理损坏文件处理
 * <p>
 * 符合需求 6.5, 6.6 的要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-12
 */
@Slf4j
@RestController
@RequestMapping("/api/file-integrity")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileIntegrityController {
    
    private final FileIntegrityManagementService fileIntegrityManagementService;
    
    /**
     * 获取文件完整性统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "SYSTEM", description = "查看文件完整性统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIntegrityStatistics() {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> statistics = fileIntegrityManagementService.getIntegrityStatistics();
            
            // 添加系统状态信息
            Map<String, Object> result = new HashMap<>(statistics);
            result.put("systemStatus", determineSystemStatus(statistics));
            result.put("queryTime", LocalDateTime.now());
            result.put("queryUser", username);
            
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_INTEGRITY_STATISTICS_QUERY", username, "SYSTEM", "SUCCESS",
                "查询文件完整性统计信息", executionTime
            );
            log.info(logMessage);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_INTEGRITY_STATISTICS_ERROR", e.getMessage(), username,
                null, "SYSTEM", null
            );
            log.error(errorLog, e);
            
            return ResponseEntity.status(500).body(
                ApiResponse.error("获取文件完整性统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 手动触发文件完整性检查
     * 
     * @return 检查任务信息
     */
    @PostMapping("/check/trigger")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "SYSTEM", description = "手动触发文件完整性检查")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerIntegrityCheck() {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            // 异步触发完整性检查
            CompletableFuture<FileIntegrityReport> checkFuture = 
                fileIntegrityManagementService.triggerManualIntegrityCheck();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "STARTED");
            result.put("message", "文件完整性检查已启动，请稍后查看报告");
            result.put("triggerTime", LocalDateTime.now());
            result.put("triggerUser", username);
            result.put("taskId", "MANUAL_CHECK_" + System.currentTimeMillis());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_INTEGRITY_MANUAL_TRIGGER", username, "SYSTEM", "SUCCESS",
                "手动触发文件完整性检查", executionTime
            );
            log.info(logMessage);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_INTEGRITY_TRIGGER_ERROR", e.getMessage(), username,
                null, "SYSTEM", null
            );
            log.error(errorLog, e);
            
            return ResponseEntity.status(500).body(
                ApiResponse.error("触发文件完整性检查失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取最新的完整性检查报告
     * 
     * @return 完整性报告
     */
    @GetMapping("/report/latest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "SYSTEM", description = "查看最新完整性报告")
    public ResponseEntity<ApiResponse<FileIntegrityReport>> getLatestIntegrityReport() {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            FileIntegrityReport report = fileIntegrityManagementService.getLatestIntegrityReport();
            
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_INTEGRITY_REPORT_QUERY", username, "SYSTEM", "SUCCESS",
                String.format("查看完整性报告: 总计=%d, 有效=%d, 损坏=%d", 
                             report.getTotalFiles(), report.getValidFiles(), report.getCorruptedFiles()),
                executionTime
            );
            log.info(logMessage);
            
            return ResponseEntity.ok(ApiResponse.success(report));
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_INTEGRITY_REPORT_ERROR", e.getMessage(), username,
                null, "SYSTEM", null
            );
            log.error(errorLog, e);
            
            return ResponseEntity.status(500).body(
                ApiResponse.error("获取完整性报告失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取文件完整性检查状态
     * 
     * @return 检查状态信息
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "SYSTEM", description = "查看文件完整性检查状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIntegrityCheckStatus() {
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            Map<String, Object> statistics = fileIntegrityManagementService.getIntegrityStatistics();
            
            Map<String, Object> status = new HashMap<>();
            status.put("serviceEnabled", true);
            status.put("lastCheckTime", statistics.get("lastCheckTime"));
            status.put("totalFiles", statistics.getOrDefault("totalFiles", 0));
            status.put("validFiles", statistics.getOrDefault("validFiles", 0));
            status.put("corruptedFiles", statistics.getOrDefault("corruptedFiles", 0));
            status.put("missingFiles", statistics.getOrDefault("missingFiles", 0));
            status.put("systemHealth", determineSystemHealth(statistics));
            status.put("nextScheduledCheck", getNextScheduledCheckTime());
            status.put("statusTime", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            log.error("获取文件完整性检查状态失败: user={}, error={}", username, e.getMessage(), e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("获取检查状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取文件完整性健康报告
     * 
     * @return 健康报告
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "SYSTEM", description = "查看文件完整性健康报告")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIntegrityHealthReport() {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> statistics = fileIntegrityManagementService.getIntegrityStatistics();
            
            Map<String, Object> healthReport = new HashMap<>();
            
            // 基础统计
            int totalFiles = (Integer) statistics.getOrDefault("totalFiles", 0);
            int validFiles = (Integer) statistics.getOrDefault("validFiles", 0);
            int corruptedFiles = (Integer) statistics.getOrDefault("corruptedFiles", 0);
            int missingFiles = (Integer) statistics.getOrDefault("missingFiles", 0);
            
            // 计算健康指标
            double integrityRate = totalFiles > 0 ? (double) validFiles / totalFiles * 100 : 100.0;
            double corruptionRate = totalFiles > 0 ? (double) corruptedFiles / totalFiles * 100 : 0.0;
            
            healthReport.put("totalFiles", totalFiles);
            healthReport.put("validFiles", validFiles);
            healthReport.put("corruptedFiles", corruptedFiles);
            healthReport.put("missingFiles", missingFiles);
            healthReport.put("integrityRate", Math.round(integrityRate * 100.0) / 100.0);
            healthReport.put("corruptionRate", Math.round(corruptionRate * 100.0) / 100.0);
            
            // 健康等级评估
            String healthGrade = determineHealthGrade(integrityRate, corruptedFiles);
            healthReport.put("healthGrade", healthGrade);
            healthReport.put("healthStatus", determineSystemHealth(statistics));
            
            // 建议和警告
            healthReport.put("recommendations", generateRecommendations(statistics));
            healthReport.put("warnings", generateWarnings(statistics));
            
            // 报告信息
            healthReport.put("reportTime", LocalDateTime.now());
            healthReport.put("lastCheckTime", statistics.get("lastCheckTime"));
            healthReport.put("reportUser", username);
            
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_INTEGRITY_HEALTH_REPORT", username, "SYSTEM", "SUCCESS",
                String.format("健康报告: 完整性率=%.2f%%, 损坏文件=%d", integrityRate, corruptedFiles),
                executionTime
            );
            log.info(logMessage);
            
            return ResponseEntity.ok(ApiResponse.success(healthReport));
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_INTEGRITY_HEALTH_ERROR", e.getMessage(), username,
                null, "SYSTEM", null
            );
            log.error(errorLog, e);
            
            return ResponseEntity.status(500).body(
                ApiResponse.error("获取健康报告失败: " + e.getMessage()));
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 确定系统状态
     */
    private String determineSystemStatus(Map<String, Object> statistics) {
        int corruptedFiles = (Integer) statistics.getOrDefault("corruptedFiles", 0);
        int missingFiles = (Integer) statistics.getOrDefault("missingFiles", 0);
        
        if (corruptedFiles == 0 && missingFiles == 0) {
            return "HEALTHY";
        } else if (corruptedFiles <= 5 && missingFiles <= 2) {
            return "WARNING";
        } else {
            return "CRITICAL";
        }
    }
    
    /**
     * 确定系统健康状态
     */
    private String determineSystemHealth(Map<String, Object> statistics) {
        int totalFiles = (Integer) statistics.getOrDefault("totalFiles", 0);
        int corruptedFiles = (Integer) statistics.getOrDefault("corruptedFiles", 0);
        int missingFiles = (Integer) statistics.getOrDefault("missingFiles", 0);
        
        if (totalFiles == 0) {
            return "UNKNOWN";
        }
        
        double problemRate = (double) (corruptedFiles + missingFiles) / totalFiles;
        
        if (problemRate == 0) {
            return "EXCELLENT";
        } else if (problemRate <= 0.01) { // 1%以下
            return "GOOD";
        } else if (problemRate <= 0.05) { // 5%以下
            return "FAIR";
        } else {
            return "POOR";
        }
    }
    
    /**
     * 确定健康等级
     */
    private String determineHealthGrade(double integrityRate, int corruptedFiles) {
        if (integrityRate >= 99.0 && corruptedFiles == 0) {
            return "A+";
        } else if (integrityRate >= 95.0 && corruptedFiles <= 2) {
            return "A";
        } else if (integrityRate >= 90.0 && corruptedFiles <= 5) {
            return "B";
        } else if (integrityRate >= 80.0) {
            return "C";
        } else {
            return "D";
        }
    }
    
    /**
     * 生成建议
     */
    private java.util.List<String> generateRecommendations(Map<String, Object> statistics) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        int corruptedFiles = (Integer) statistics.getOrDefault("corruptedFiles", 0);
        int missingFiles = (Integer) statistics.getOrDefault("missingFiles", 0);
        LocalDateTime lastCheckTime = (LocalDateTime) statistics.get("lastCheckTime");
        
        if (corruptedFiles > 0) {
            recommendations.add("发现损坏文件，建议立即检查文件存储系统的完整性");
            recommendations.add("考虑启用文件备份和恢复机制");
        }
        
        if (missingFiles > 0) {
            recommendations.add("发现缺失文件，建议检查文件存储路径配置");
            recommendations.add("考虑实施文件存储监控机制");
        }
        
        if (lastCheckTime == null || lastCheckTime.isBefore(LocalDateTime.now().minusDays(1))) {
            recommendations.add("建议启用定期文件完整性检查");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("文件完整性状况良好，继续保持当前的安全策略");
        }
        
        return recommendations;
    }
    
    /**
     * 生成警告
     */
    private java.util.List<String> generateWarnings(Map<String, Object> statistics) {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        
        int corruptedFiles = (Integer) statistics.getOrDefault("corruptedFiles", 0);
        int missingFiles = (Integer) statistics.getOrDefault("missingFiles", 0);
        int totalFiles = (Integer) statistics.getOrDefault("totalFiles", 0);
        
        if (corruptedFiles > 10) {
            warnings.add("严重警告：发现大量损坏文件，可能存在系统性问题");
        } else if (corruptedFiles > 5) {
            warnings.add("警告：发现多个损坏文件，建议立即调查");
        }
        
        if (missingFiles > 5) {
            warnings.add("严重警告：发现大量缺失文件，可能存在存储问题");
        } else if (missingFiles > 2) {
            warnings.add("警告：发现多个缺失文件，建议检查存储配置");
        }
        
        if (totalFiles > 0) {
            double problemRate = (double) (corruptedFiles + missingFiles) / totalFiles;
            if (problemRate > 0.1) {
                warnings.add("严重警告：文件问题率超过10%，系统完整性存在重大风险");
            } else if (problemRate > 0.05) {
                warnings.add("警告：文件问题率超过5%，建议加强监控");
            }
        }
        
        return warnings;
    }
    
    /**
     * 获取下次定时检查时间
     */
    private String getNextScheduledCheckTime() {
        // 定时任务配置为每天凌晨2点执行
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextCheck = now.withHour(2).withMinute(0).withSecond(0).withNano(0);
        
        // 如果今天的2点已经过了，则是明天的2点
        if (now.getHour() >= 2) {
            nextCheck = nextCheck.plusDays(1);
        }
        
        return nextCheck.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}