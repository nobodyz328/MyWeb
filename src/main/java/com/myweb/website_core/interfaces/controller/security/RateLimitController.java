package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.IPS.ratelimit.RateLimitAlertService;
import com.myweb.website_core.application.service.security.IPS.ratelimit.RateLimitConfigService;
import com.myweb.website_core.application.service.security.IPS.ratelimit.RateLimitingService;
import com.myweb.website_core.common.config.RateLimitProperties;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 访问频率限制管理控制器
 * 提供访问频率限制的配置管理和监控功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@RestController
@RequestMapping("/api/admin/rate-limit")
@PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
public class RateLimitController {
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    @Autowired
    private RateLimitConfigService rateLimitConfigService;
    
    @Autowired
    private RateLimitAlertService rateLimitAlertService;
    
    /**
     * 获取当前访问频率限制状态
     * 
     * @param request HTTP请求
     * @return 访问频率限制状态
     */
    @GetMapping("/status")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String uri = request.getParameter("uri");
        String username = request.getParameter("username");
        
        if (uri == null) {
            uri = "/api/test"; // 默认测试URI
        }
        
        RateLimitingService.RateLimitStatus status = 
            rateLimitingService.getRateLimitStatus(clientIp, uri, username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", status.isAllowed());
        response.put("ipCount", status.getIpCount());
        response.put("userCount", status.getUserCount());
        response.put("maxRequests", status.getMaxRequests());
        response.put("remainingRequests", status.getRemainingRequests());
        response.put("clientIp", clientIp);
        response.put("uri", uri);
        response.put("username", username);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取所有动态配置
     * 
     * @return 动态配置列表
     */
    @GetMapping("/configs")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, RateLimitProperties.EndpointLimit>> getAllConfigs() {
        Map<String, RateLimitProperties.EndpointLimit> configs = 
            rateLimitConfigService.getAllDynamicConfigs();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 获取指定接口的配置
     * 
     * @param endpoint 接口路径
     * @return 接口配置
     */
    @GetMapping("/configs/{endpoint}")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<RateLimitProperties.EndpointLimit> getEndpointConfig(@PathVariable String endpoint) {
        RateLimitProperties.EndpointLimit config = rateLimitConfigService.getEndpointConfig(endpoint);
        return ResponseEntity.ok(config);
    }
    
    /**
     * 更新接口配置
     * 
     * @param endpoint 接口路径
     * @param config 新配置
     * @return 更新结果
     */
    @PutMapping("/configs/{endpoint}")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> updateEndpointConfig(
            @PathVariable String endpoint,
            @RequestBody RateLimitProperties.EndpointLimit config) {
        
        rateLimitConfigService.updateEndpointConfig(endpoint, config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "配置更新成功");
        response.put("endpoint", endpoint);
        response.put("config", config);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 批量更新配置
     * 
     * @param configs 配置映射
     * @return 更新结果
     */
    @PutMapping("/configs")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> batchUpdateConfigs(
            @RequestBody Map<String, RateLimitProperties.EndpointLimit> configs) {
        
        rateLimitConfigService.batchUpdateConfigs(configs);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "批量配置更新成功");
        response.put("updatedCount", configs.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除接口配置
     * 
     * @param endpoint 接口路径
     * @return 删除结果
     */
    @DeleteMapping("/configs/{endpoint}")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> removeEndpointConfig(@PathVariable String endpoint) {
        rateLimitConfigService.removeEndpointConfig(endpoint);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "配置删除成功");
        response.put("endpoint", endpoint);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取配置模板
     * 
     * @return 配置模板列表
     */
    @GetMapping("/templates")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, RateLimitProperties.EndpointLimit>> getConfigTemplates() {
        Map<String, RateLimitProperties.EndpointLimit> templates = 
            rateLimitConfigService.getConfigTemplates();
        return ResponseEntity.ok(templates);
    }
    
    /**
     * 应用配置模板
     * 
     * @param templateName 模板名称
     * @param endpoint 接口路径
     * @return 应用结果
     */
    @PostMapping("/templates/{templateName}/apply")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> applyTemplate(
            @PathVariable String templateName,
            @RequestParam String endpoint) {
        
        rateLimitConfigService.applyTemplate(templateName, endpoint);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "模板应用成功");
        response.put("templateName", templateName);
        response.put("endpoint", endpoint);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 重新加载配置
     * 
     * @return 重新加载结果
     */
    @PostMapping("/configs/reload")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> reloadConfigs() {
        rateLimitConfigService.reloadConfigs();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "配置重新加载成功");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 清除访问频率限制
     * 
     * @param request HTTP请求
     * @return 清除结果
     */
    @PostMapping("/clear")
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> clearRateLimit(HttpServletRequest request) {
        String clientIp = request.getParameter("clientIp");
        String uri = request.getParameter("uri");
        String username = request.getParameter("username");
        
        if (clientIp == null) {
            clientIp = getClientIpAddress(request);
        }
        
        rateLimitingService.clearRateLimit(clientIp, uri, username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "访问频率限制清除成功");
        response.put("clientIp", clientIp);
        response.put("uri", uri);
        response.put("username", username);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取配置统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> getConfigStats() {
        Map<String, Object> stats = rateLimitConfigService.getConfigStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取告警统计信息
     * 
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 告警统计信息
     */
    @GetMapping("/alerts/stats")
    @Auditable(operation = AuditOperation.SECURITY_EVENT_QUERY, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> getAlertStats(@RequestParam(required = false) String date) {
        if (date == null) {
            date = java.time.LocalDate.now().toString();
        }
        
        Map<String, Object> stats = rateLimitAlertService.getAlertStats(date);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取告警趋势
     * 
     * @return 告警趋势数据
     */
    @GetMapping("/alerts/trend")
    @Auditable(operation = AuditOperation.SECURITY_EVENT_QUERY, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Integer>> getAlertTrend() {
        Map<String, Integer> trend = rateLimitAlertService.getAlertTrend();
        return ResponseEntity.ok(trend);
    }
    
    /**
     * 测试访问频率限制
     * 
     * @param request HTTP请求
     * @return 测试结果
     */
    @PostMapping("/test")
    @Auditable(operation = AuditOperation.SECURITY_EVENT_QUERY, resourceType = "RATE_LIMIT")
    public ResponseEntity<Map<String, Object>> testRateLimit(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String uri = request.getParameter("uri");
        String username = request.getParameter("username");
        
        if (uri == null) {
            uri = "/api/test";
        }
        
        boolean allowed = rateLimitingService.isAllowed(clientIp, uri, username);
        RateLimitingService.RateLimitStatus status = 
            rateLimitingService.getRateLimitStatus(clientIp, uri, username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", allowed);
        response.put("status", status);
        response.put("clientIp", clientIp);
        response.put("uri", uri);
        response.put("username", username);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
}