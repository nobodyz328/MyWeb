package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * XSS监控控制器
 * <p>
 * 提供XSS防护统计和监控数据的REST API接口。
 * <p>
 * 主要功能：
 * 1. XSS攻击统计查询
 * 2. 防护效果监控
 * 3. 配置管理
 * 4. 实时监控数据
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - XSS监控接口
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/security/xss")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class XssMonitoringController {
    
    private final XssStatisticsService xssStatisticsService;
    private final XssMonitoringService xssMonitoringService;
    private final XssFilterConfig xssFilterConfig;
    
    /**
     * 获取XSS攻击统计报告
     * 
     * @return XSS攻击统计报告
     */
    @GetMapping("/statistics")
    public ResponseEntity<XssStatisticsService.XssStatisticsReport> getStatistics() {
        try {
            XssStatisticsService.XssStatisticsReport report = xssStatisticsService.getStatisticsReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("获取XSS统计报告失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取XSS防护效果
     * 
     * @return XSS防护效果数据
     */
    @GetMapping("/effectiveness")
    public ResponseEntity<XssMonitoringService.XssProtectionEffectiveness> getEffectiveness() {
        try {
            XssMonitoringService.XssProtectionEffectiveness effectiveness = 
                xssMonitoringService.getProtectionEffectiveness();
            return ResponseEntity.ok(effectiveness);
        } catch (Exception e) {
            log.error("获取XSS防护效果失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取XSS攻击概览
     * 
     * @return XSS攻击概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 基础统计
            overview.put("totalAttacks", xssStatisticsService.getTotalAttackCount());
            overview.put("todayAttacks", xssStatisticsService.getTodayAttackCount());
            overview.put("hourlyAttacks", xssStatisticsService.getHourlyAttackCount());
            
            // 防护效果
            XssMonitoringService.XssProtectionEffectiveness effectiveness = 
                xssMonitoringService.getProtectionEffectiveness();
            overview.put("blockRate", effectiveness.getBlockRate());
            overview.put("avgProcessingTime", effectiveness.getAverageProcessingTimeMs());
            
            // 配置状态
            overview.put("xssProtectionEnabled", xssFilterConfig.isEnabled());
            overview.put("strictMode", xssFilterConfig.isStrictMode());
            overview.put("cacheEnabled", xssFilterConfig.getPerformance().isCacheEnabled());
            overview.put("monitoringEnabled", xssFilterConfig.getMonitoring().isEnabled());
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("获取XSS攻击概览失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取IP攻击统计
     * 
     * @return IP攻击统计数据
     */
    @GetMapping("/ip-stats")
    public ResponseEntity<Map<String, Long>> getIpStats() {
        try {
            Map<String, Long> ipStats = xssStatisticsService.getIpAttackStats();
            return ResponseEntity.ok(ipStats);
        } catch (Exception e) {
            log.error("获取IP攻击统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取攻击类型统计
     * 
     * @return 攻击类型统计数据
     */
    @GetMapping("/attack-types")
    public ResponseEntity<Map<String, Long>> getAttackTypes() {
        try {
            Map<String, Long> attackTypes = xssStatisticsService.getAttackTypeStats();
            return ResponseEntity.ok(attackTypes);
        } catch (Exception e) {
            log.error("获取攻击类型统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取URI攻击统计
     * 
     * @return URI攻击统计数据
     */
    @GetMapping("/uri-stats")
    public ResponseEntity<Map<String, Long>> getUriStats() {
        try {
            Map<String, Long> uriStats = xssStatisticsService.getUriAttackStats();
            return ResponseEntity.ok(uriStats);
        } catch (Exception e) {
            log.error("获取URI攻击统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取时间窗口统计
     * 
     * @return 时间窗口统计数据
     */
    @GetMapping("/time-window-stats")
    public ResponseEntity<Map<String, Long>> getTimeWindowStats() {
        try {
            Map<String, Long> timeWindowStats = xssStatisticsService.getTimeWindowStats();
            return ResponseEntity.ok(timeWindowStats);
        } catch (Exception e) {
            log.error("获取时间窗口统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取XSS过滤配置
     * 
     * @return XSS过滤配置
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", xssFilterConfig.isEnabled());
            config.put("strictMode", xssFilterConfig.isStrictMode());
            config.put("allowedTags", xssFilterConfig.getAllowedTags());
            config.put("allowedAttributes", xssFilterConfig.getAllowedAttributes());
            config.put("tagSpecificAttributes", xssFilterConfig.getTagSpecificAttributes());
            config.put("removeUnknownTags", xssFilterConfig.isRemoveUnknownTags());
            config.put("encodeSpecialChars", xssFilterConfig.isEncodeSpecialChars());
            config.put("maxTagDepth", xssFilterConfig.getMaxTagDepth());
            config.put("maxContentLength", xssFilterConfig.getMaxContentLength());
            config.put("whitelistUrlPatterns", xssFilterConfig.getWhitelistUrlPatterns());
            config.put("performance", xssFilterConfig.getPerformance());
            config.put("statistics", xssFilterConfig.getStatistics());
            config.put("monitoring", xssFilterConfig.getMonitoring());
            
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("获取XSS过滤配置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 更新XSS过滤配置
     * 
     * @param configUpdates 配置更新数据
     * @return 更新结果
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> configUpdates) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "配置更新功能需要重启应用才能生效");
            result.put("receivedUpdates", configUpdates);
            
            log.info("收到XSS配置更新请求: {}", configUpdates);
            
            // 注意：实际的配置更新需要修改配置文件并重启应用
            // 这里只是记录更新请求，实际实现可能需要配置管理系统
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("更新XSS过滤配置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 清理XSS统计数据
     * 
     * @return 清理结果
     */
    @DeleteMapping("/statistics")
    public ResponseEntity<Map<String, Object>> clearStatistics() {
        try {
            // 注意：这里需要实现统计数据清理逻辑
            // 由于当前统计服务使用内存存储，重启应用即可清理
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "统计数据清理请求已记录，需要重启应用才能完全清理");
            
            log.info("收到XSS统计数据清理请求");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("清理XSS统计数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}