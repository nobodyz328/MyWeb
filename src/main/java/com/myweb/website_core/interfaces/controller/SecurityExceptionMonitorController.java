package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.security.SecurityExceptionMaskingService;
import com.myweb.website_core.application.service.security.SecurityExceptionStatisticsService;
import com.myweb.website_core.common.util.PermissionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 安全异常监控控制器
 * <p>
 * 提供安全异常统计和监控的REST API接口，包括：
 * - 异常统计信息查询
 * - 异常趋势分析
 * - 异常配置管理
 * - 统计数据重置
 * <p>
 * 符合需求：1.6, 2.6, 3.4, 4.6 - 异常统计和监控
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/security/exception-monitor")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.exception-statistics.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityExceptionMonitorController {
    
    private final SecurityExceptionStatisticsService statisticsService;
    private final SecurityExceptionMaskingService maskingService;
    
    /**
     * 获取异常统计信息
     * 只有管理员可以访问
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExceptionStatistics() {
        try {
            Map<String, Object> statistics = statisticsService.getExceptionStatistics();
            
            log.info("SecurityExceptionMonitorController: 管理员查询异常统计信息 - 用户: {}", 
                    PermissionUtils.getCurrentUsername());
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("SecurityExceptionMonitorController: 获取异常统计信息时发生错误", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取统计信息失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取异常趋势分析
     * 只有管理员可以访问
     */
    @GetMapping("/trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExceptionTrends() {
        try {
            Map<String, Object> trends = statisticsService.getExceptionTrends();
            
            log.info("SecurityExceptionMonitorController: 管理员查询异常趋势分析 - 用户: {}", 
                    PermissionUtils.getCurrentUsername());
            
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            log.error("SecurityExceptionMonitorController: 获取异常趋势分析时发生错误", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取趋势分析失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取异常处理配置信息
     * 只有管理员可以访问
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExceptionConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 统计服务配置
            Map<String, Object> statisticsConfig = new HashMap<>();
            statisticsConfig.put("enabled", statisticsService != null);
            if (statisticsService != null) {
                statisticsConfig.putAll(statisticsService.getExceptionStatistics());
            }
            config.put("statistics", statisticsConfig);
            
            // 脱敏服务配置
            Map<String, Object> maskingConfig = new HashMap<>();
            maskingConfig.put("enabled", maskingService != null);
            if (maskingService != null) {
                maskingConfig.putAll(maskingService.getMaskingConfig());
            }
            config.put("masking", maskingConfig);
            
            log.info("SecurityExceptionMonitorController: 管理员查询异常处理配置 - 用户: {}", 
                    PermissionUtils.getCurrentUsername());
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("SecurityExceptionMonitorController: 获取异常处理配置时发生错误", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取配置信息失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 重置异常统计数据
     * 只有管理员可以访问
     */
    @PostMapping("/reset-statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        try {
            if (statisticsService != null) {
                statisticsService.resetStatistics();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "异常统计数据已重置");
            response.put("resetTime", java.time.LocalDateTime.now());
            
            log.warn("SecurityExceptionMonitorController: 管理员重置异常统计数据 - 用户: {}", 
                    PermissionUtils.getCurrentUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("SecurityExceptionMonitorController: 重置异常统计数据时发生错误", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "重置统计数据失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 清理过期数据
     * 只有管理员可以访问
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupExpiredData() {
        try {
            if (statisticsService != null) {
                statisticsService.cleanupExpiredData();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "过期数据清理完成");
            response.put("cleanupTime", java.time.LocalDateTime.now());
            
            log.info("SecurityExceptionMonitorController: 管理员手动清理过期数据 - 用户: {}", 
                    PermissionUtils.getCurrentUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("SecurityExceptionMonitorController: 清理过期数据时发生错误", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "清理过期数据失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取异常处理健康状态
     * 管理员和运维人员可以访问
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // 检查统计服务状态
            boolean statisticsServiceHealthy = statisticsService != null;
            health.put("statisticsService", statisticsServiceHealthy ? "UP" : "DOWN");
            
            // 检查脱敏服务状态
            boolean maskingServiceHealthy = maskingService != null;
            health.put("maskingService", maskingServiceHealthy ? "UP" : "DOWN");
            
            // 整体健康状态
            boolean overallHealthy = statisticsServiceHealthy && maskingServiceHealthy;
            health.put("status", overallHealthy ? "UP" : "DOWN");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            // 如果统计服务可用，添加基本统计信息
            if (statisticsService != null) {
                try {
                    Map<String, Object> basicStats = statisticsService.getExceptionStatistics();
                    health.put("totalExceptions", basicStats.get("totalExceptions"));
                    health.put("recentExceptions", basicStats.get("recentExceptionsCount"));
                } catch (Exception e) {
                    health.put("statisticsError", e.getMessage());
                }
            }
            
            HttpStatus status = overallHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(health);
            
        } catch (Exception e) {
            log.error("SecurityExceptionMonitorController: 获取健康状态时发生错误", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", "获取健康状态失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试异常脱敏功能
     * 只有管理员可以访问，用于测试脱敏效果
     */
    @PostMapping("/test-masking")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testMasking(@RequestBody Map<String, String> testData) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (maskingService != null) {
                // 测试不同类型的脱敏
                for (Map.Entry<String, String> entry : testData.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    // 管理员视角（完整信息）
                    String adminMasked = maskingService.maskMessage(value, true);
                    
                    // 普通用户视角（脱敏信息）
                    String userMasked = maskingService.maskMessage(value, false);
                    
                    Map<String, String> maskingResult = new HashMap<>();
                    maskingResult.put("original", value);
                    maskingResult.put("adminView", adminMasked);
                    maskingResult.put("userView", userMasked);
                    
                    result.put(key, maskingResult);
                }
            } else {
                result.put("error", "脱敏服务未启用");
            }
            
            log.info("SecurityExceptionMonitorController: 管理员测试脱敏功能 - 用户: {}", 
                    PermissionUtils.getCurrentUsername());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("SecurityExceptionMonitorController: 测试脱敏功能时发生错误", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "测试脱敏功能失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}