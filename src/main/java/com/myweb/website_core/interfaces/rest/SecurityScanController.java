package com.myweb.website_core.interfaces.rest;

import com.myweb.website_core.application.service.security.IPS.virusprotect.DependencySecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 安全扫描控制器
 * 提供依赖安全扫描相关的REST API
 * 
 * @author MyWeb Security Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/security/scan")
@RequiredArgsConstructor
public class SecurityScanController {

    private final DependencySecurityService dependencySecurityService;

    /**
     * 获取最新的安全扫描报告
     */
    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
 //   @Auditable(operation = "SECURITY_SCAN_REPORT_VIEW", resourceType = "SECURITY_REPORT")
    public ResponseEntity<DependencySecurityService.SecurityScanResult> getSecurityReport() {
        log.info("管理员请求查看安全扫描报告");
        
        DependencySecurityService.SecurityScanResult result = dependencySecurityService.parseSecurityReport();
        return ResponseEntity.ok(result);
    }

    /**
     * 获取安全扫描摘要
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
  //  @Auditable(operation = "SECURITY_SCAN_SUMMARY_VIEW", resourceType = "SECURITY_REPORT")
    public ResponseEntity<Map<String, Object>> getSecuritySummary() {
        log.info("管理员请求查看安全扫描摘要");
        
        DependencySecurityService.SecurityScanResult result = dependencySecurityService.parseSecurityReport();
        String summary = dependencySecurityService.generateSecuritySummary(result);
        boolean hasHighSeverity = dependencySecurityService.hasHighSeverityVulnerabilities(result);
        
        Map<String, Object> response = Map.of(
            "summary", summary,
            "hasHighSeverityVulnerabilities", hasHighSeverity,
            "totalDependencies", result.getTotalDependencies(),
            "vulnerableDependencies", result.getVulnerableDependencies(),
            "scanTime", result.getScanTime()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 手动触发安全报告通知
     */
    @PostMapping("/notify")
    @PreAuthorize("hasRole('ADMIN')")
 //   @Auditable(operation = "SECURITY_SCAN_NOTIFY_TRIGGER", resourceType = "SECURITY_REPORT")
    public ResponseEntity<Map<String, String>> triggerSecurityNotification() {
        log.info("管理员手动触发安全报告通知");
        
        try {
            DependencySecurityService.SecurityScanResult result = dependencySecurityService.parseSecurityReport();
            CompletableFuture<Void> notificationFuture = dependencySecurityService.sendSecurityNotification(result);
            
            // 异步发送通知，立即返回响应
            notificationFuture.thenRun(() -> 
                log.info("安全报告通知发送完成")
            ).exceptionally(throwable -> {
                log.error("安全报告通知发送失败", throwable);
                return null;
            });
            
            return ResponseEntity.ok(Map.of(
                "message", "安全报告通知已触发",
                "status", "success"
            ));
            
        } catch (Exception e) {
            log.error("触发安全报告通知失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "触发安全报告通知失败: " + e.getMessage(),
                "status", "error"
            ));
        }
    }

    /**
     * 检查系统安全状态
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
   // @Auditable(operation = "SECURITY_STATUS_CHECK", resourceType = "SECURITY_REPORT")
    public ResponseEntity<Map<String, Object>> getSecurityStatus() {
        log.info("管理员请求检查系统安全状态");
        
        DependencySecurityService.SecurityScanResult result = dependencySecurityService.parseSecurityReport();
        boolean hasHighSeverity = dependencySecurityService.hasHighSeverityVulnerabilities(result);
        
        String status;
        String message;
        
        if (result.getTotalDependencies() == 0) {
            status = "UNKNOWN";
            message = "未找到安全扫描报告，请先执行依赖检查";
        } else if (hasHighSeverity) {
            status = "CRITICAL";
            message = "发现高危漏洞，需要立即处理";
        } else if (result.getVulnerableDependencies() > 0) {
            status = "WARNING";
            message = "发现中低危漏洞，建议及时处理";
        } else {
            status = "SECURE";
            message = "未发现安全漏洞";
        }
        
        Map<String, Object> response = Map.of(
            "status", status,
            "message", message,
            "totalDependencies", result.getTotalDependencies(),
            "vulnerableDependencies", result.getVulnerableDependencies(),
            "hasHighSeverityVulnerabilities", hasHighSeverity,
            "lastScanTime", result.getScanTime()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取漏洞详情
     */
    @GetMapping("/vulnerabilities")
    @PreAuthorize("hasRole('ADMIN')")
   // @Auditable(operation = "SECURITY_VULNERABILITIES_VIEW", resourceType = "SECURITY_REPORT")
    public ResponseEntity<Map<String, Object>> getVulnerabilities(
            @RequestParam(defaultValue = "ALL") String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("管理员请求查看漏洞详情，严重程度: {}, 页码: {}, 大小: {}", severity, page, size);
        
        DependencySecurityService.SecurityScanResult result = dependencySecurityService.parseSecurityReport();
        
        // 根据严重程度过滤漏洞
        var vulnerabilities = result.getVulnerabilities().stream()
            .filter(v -> "ALL".equals(severity) || severity.equals(v.getSeverity()))
            .skip((long) page * size)
            .limit(size)
            .toList();
        
        long totalCount = result.getVulnerabilities().stream()
            .filter(v -> "ALL".equals(severity) || severity.equals(v.getSeverity()))
            .count();
        
        Map<String, Object> response = Map.of(
            "vulnerabilities", vulnerabilities,
            "totalCount", totalCount,
            "page", page,
            "size", size,
            "hasNext", (page + 1) * size < totalCount
        );
        
        return ResponseEntity.ok(response);
    }
}