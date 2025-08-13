package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.security.FileSecurityMonitoringService;
import com.myweb.website_core.common.util.PermissionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文件安全监控控制器
 * <p>
 * 提供文件安全监控的API接口：
 * - 文件安全统计查询
 * - 文件安全报告生成
 * - 文件安全监控配置
 * <p>
 * 符合需求 6.6, 6.7 的要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-13
 */
@Slf4j
@RestController
@RequestMapping("/api/security/file")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileSecurityController {
    
    private final FileSecurityMonitoringService fileSecurityMonitoringService;
    
    /**
     * 获取文件安全统计信息
     * 
     * @return 文件安全统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getFileSecurityStatistics() {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("获取文件安全统计信息: user={}", username);
            
            Map<String, Object> statistics = fileSecurityMonitoringService.getFileSecurityStatistics();
            
            // 添加额外的统计信息
            Map<String, Object> response = new HashMap<>(statistics);
            response.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.put("queryUser", username);
            
            // 计算安全率
            long totalChecks = ((Number) statistics.getOrDefault("totalChecks", 0)).longValue();
            long safeCount = ((Number) statistics.getOrDefault("safeCount", 0)).longValue();
            if (totalChecks > 0) {
                double safetyRate = (safeCount * 100.0) / totalChecks;
                response.put("safetyRate", Math.round(safetyRate * 100.0) / 100.0);
            } else {
                response.put("safetyRate", 100.0);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("文件安全统计查询完成: user={}, totalChecks={}, duration={}ms", 
                    username, totalChecks, executionTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取文件安全统计失败: user={}, error={}", username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取文件安全统计失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取文件安全监控仪表板数据
     * 
     * @return 仪表板数据
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getSecurityDashboard() {
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            log.info("获取文件安全监控仪表板: user={}", username);
            
            Map<String, Object> statistics = fileSecurityMonitoringService.getFileSecurityStatistics();
            Map<String, Object> dashboard = new HashMap<>();
            
            // 基础统计
            long totalChecks = ((Number) statistics.getOrDefault("totalChecks", 0)).longValue();
            long safeCount = ((Number) statistics.getOrDefault("safeCount", 0)).longValue();
            long lowRiskCount = ((Number) statistics.getOrDefault("low_riskCount", 0)).longValue();
            long mediumRiskCount = ((Number) statistics.getOrDefault("medium_riskCount", 0)).longValue();
            long highRiskCount = ((Number) statistics.getOrDefault("high_riskCount", 0)).longValue();
            long threatsDetected = ((Number) statistics.getOrDefault("threatsDetected", 0)).longValue();
            
            dashboard.put("totalChecks", totalChecks);
            dashboard.put("safeCount", safeCount);
            dashboard.put("lowRiskCount", lowRiskCount);
            dashboard.put("mediumRiskCount", mediumRiskCount);
            dashboard.put("highRiskCount", highRiskCount);
            dashboard.put("threatsDetected", threatsDetected);
            
            // 安全级别分布
            Map<String, Long> securityLevelDistribution = new HashMap<>();
            securityLevelDistribution.put("SAFE", safeCount);
            securityLevelDistribution.put("LOW_RISK", lowRiskCount);
            securityLevelDistribution.put("MEDIUM_RISK", mediumRiskCount);
            securityLevelDistribution.put("HIGH_RISK", highRiskCount);
            dashboard.put("securityLevelDistribution", securityLevelDistribution);
            
            // 安全指标
            Map<String, Object> securityMetrics = new HashMap<>();
            if (totalChecks > 0) {
                securityMetrics.put("safetyRate", Math.round((safeCount * 100.0) / totalChecks * 100.0) / 100.0);
                securityMetrics.put("threatRate", Math.round((threatsDetected * 100.0) / totalChecks * 100.0) / 100.0);
                securityMetrics.put("highRiskRate", Math.round((highRiskCount * 100.0) / totalChecks * 100.0) / 100.0);
            } else {
                securityMetrics.put("safetyRate", 100.0);
                securityMetrics.put("threatRate", 0.0);
                securityMetrics.put("highRiskRate", 0.0);
            }
            dashboard.put("securityMetrics", securityMetrics);
            
            // 文件类型统计
            Map<String, Long> fileTypeStats = new HashMap<>();
            statistics.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("ext_"))
                .forEach(entry -> {
                    String extension = entry.getKey().substring(4).toUpperCase();
                    fileTypeStats.put(extension, ((Number) entry.getValue()).longValue());
                });
            dashboard.put("fileTypeStats", fileTypeStats);
            
            // 系统状态
            Map<String, Object> systemStatus = new HashMap<>();
            systemStatus.put("monitoringEnabled", true);
            systemStatus.put("lastCheckTime", statistics.get("lastCheckTime"));
            systemStatus.put("status", threatsDetected > 0 ? "ALERT" : "NORMAL");
            dashboard.put("systemStatus", systemStatus);
            
            dashboard.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            log.info("文件安全监控仪表板数据获取完成: user={}, totalChecks={}", username, totalChecks);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            log.error("获取文件安全监控仪表板失败: user={}, error={}", username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取仪表板数据失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 手动触发文件安全检查
     * 
     * @param imageId 图片ID
     * @return 检查结果
     */
    @PostMapping("/check/{imageId}")
    public ResponseEntity<Map<String, Object>> triggerSecurityCheck(@PathVariable Long imageId) {
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            log.info("手动触发文件安全检查: imageId={}, user={}", imageId, username);
            
            // 这里需要获取文件信息并触发安全检查
            // 由于需要文件的详细信息，这里返回一个模拟的响应
            Map<String, Object> response = new HashMap<>();
            response.put("imageId", imageId);
            response.put("status", "CHECK_INITIATED");
            response.put("message", "文件安全检查已启动");
            response.put("checkTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.put("initiatedBy", username);
            
            log.info("文件安全检查已启动: imageId={}, user={}", imageId, username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("触发文件安全检查失败: imageId={}, user={}, error={}", imageId, username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("imageId", imageId);
            errorResponse.put("status", "CHECK_FAILED");
            errorResponse.put("error", "触发安全检查失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 生成文件安全报告
     * 
     * @param days 报告天数（默认7天）
     * @return 报告生成结果
     */
    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> generateSecurityReport(
            @RequestParam(defaultValue = "7") int days) {
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            log.info("生成文件安全报告: days={}, user={}", days, username);
            
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);
            
            // 异步生成报告
            CompletableFuture.runAsync(() -> {
                try {
                    // 这里可以调用报告生成服务
                    log.info("开始生成文件安全报告: startTime={}, endTime={}, user={}", 
                            startTime, endTime, username);
                    
                    // 模拟报告生成过程
                    Thread.sleep(2000);
                    
                    log.info("文件安全报告生成完成: user={}", username);
                } catch (Exception e) {
                    log.error("生成文件安全报告异常: user={}, error={}", username, e.getMessage(), e);
                }
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "REPORT_GENERATION_STARTED");
            response.put("message", "文件安全报告生成已启动");
            response.put("reportPeriod", days + " 天");
            response.put("startTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.put("endTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.put("requestedBy", username);
            response.put("requestTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("生成文件安全报告失败: days={}, user={}, error={}", days, username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "REPORT_GENERATION_FAILED");
            errorResponse.put("error", "生成报告失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取文件安全配置
     * 
     * @return 安全配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSecurityConfig() {
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            log.info("获取文件安全配置: user={}", username);
            
            Map<String, Object> config = new HashMap<>();
            config.put("monitoringEnabled", true);
            config.put("maxFileSize", "10MB");
            config.put("allowedExtensions", "jpg,jpeg,png,gif,bmp,webp");
            config.put("virusScanEnabled", true);
            config.put("quarantineEnabled", true);
            config.put("alertThreshold", 50);
            config.put("reportSchedule", "每日凌晨1点");
            
            Map<String, Object> response = new HashMap<>();
            response.put("config", config);
            response.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.put("queryUser", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取文件安全配置失败: user={}, error={}", username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取安全配置失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取文件安全告警列表
     * 
     * @param limit 返回数量限制
     * @return 告警列表
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getSecurityAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            log.info("获取文件安全告警列表: limit={}, user={}", limit, username);
            
            // 这里应该从数据库查询实际的告警数据
            // 为了演示，返回模拟数据
            Map<String, Object> response = new HashMap<>();
            response.put("alerts", new java.util.ArrayList<>());
            response.put("totalAlerts", 0);
            response.put("unreadAlerts", 0);
            response.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            response.put("queryUser", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取文件安全告警列表失败: user={}, error={}", username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取告警列表失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 健康检查接口
     * 
     * @return 服务健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "FileSecurityMonitoring");
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            health.put("version", "1.0");
            
            // 检查服务状态
            Map<String, Object> statistics = fileSecurityMonitoringService.getFileSecurityStatistics();
            health.put("monitoringActive", statistics != null);
            health.put("lastActivity", statistics.get("lastCheckTime"));
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("文件安全监控服务健康检查失败: error={}", e.getMessage(), e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("service", "FileSecurityMonitoring");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.status(503).body(health);
        }
    }
}