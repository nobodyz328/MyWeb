package com.myweb.website_core.application.service.security.IPS.virusprotect;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 病毒告警服务
 * <p>
 * 负责病毒检测事件的告警和通知：
 * - 实时安全事件告警
 * - 邮件通知管理员
 * - 告警级别管理
 * - 告警统计和监控
 * - 告警历史记录
 * <p>
 * 符合GB/T 22239-2019二级等保要求的安全事件监控机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirusAlertService {
    
    private final JavaMailSender mailSender;
    private final AuditLogServiceAdapter auditLogService;
    
    @Value("${app.security.alert.admin-email:3281314509@qq.com}")
    private String adminEmail;
    
    @Value("${app.security.alert.enabled:true}")
    private boolean alertEnabled;
    
    @Value("${app.security.alert.email-enabled:true}")
    private boolean emailAlertEnabled;
    
    @Value("${app.security.alert.min-threat-level:MEDIUM}")
    private String minThreatLevel;
    
    /**
     * 发送病毒检测告警
     * 
     * @param scanResult 病毒扫描结果
     * @return 告警发送结果
     */
    @Async
    public CompletableFuture<Boolean> sendVirusAlert(VirusScanResult scanResult) {
        if (!alertEnabled || !scanResult.isRequiresAlert()) {
            log.debug("告警已禁用或不需要告警，跳过: filename={}", scanResult.getFilename());
            return CompletableFuture.completedFuture(false);
        }
        
        log.info("开始发送病毒检测告警: filename={}, virus={}, user={}", 
                scanResult.getFilename(), scanResult.getVirusName(), scanResult.getUsername());
        
        try {
            // 检查威胁级别是否达到告警阈值
            if (!shouldAlert(scanResult.getThreatLevel())) {
                log.debug("威胁级别未达到告警阈值，跳过: level={}, min={}", 
                         scanResult.getThreatLevel(), minThreatLevel);
                return CompletableFuture.completedFuture(false);
            }
            
            boolean emailSent = false;
            
            // 发送邮件告警
            if (emailAlertEnabled) {
                emailSent = sendEmailAlert(scanResult);
            }
            
            // 记录告警审计日志
            recordAlertAuditLog(scanResult, emailSent);
            
            log.info("病毒检测告警发送完成: filename={}, emailSent={}", 
                    scanResult.getFilename(), emailSent);
            
            return CompletableFuture.completedFuture(emailSent);
            
        } catch (Exception e) {
            log.error("发送病毒检测告警失败: filename={}, error={}", 
                     scanResult.getFilename(), e.getMessage(), e);
            
            // 记录告警失败的审计日志
            recordAlertFailureAuditLog(scanResult, e.getMessage());
            
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 发送扫描引擎不可用告警
     * 
     * @param engineName 引擎名称
     * @param errorMessage 错误消息
     * @return 告警发送结果
     */
    @Async
    public CompletableFuture<Boolean> sendEngineUnavailableAlert(String engineName, String errorMessage) {
        if (!alertEnabled || !emailAlertEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        log.warn("病毒扫描引擎不可用，发送告警: engine={}, error={}", engineName, errorMessage);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("【安全告警】病毒扫描引擎不可用");
            message.setText(buildEngineUnavailableAlertContent(engineName, errorMessage));
            
            mailSender.send(message);
            
            // 记录告警审计日志
            recordEngineAlertAuditLog(engineName, errorMessage, true);
            
            log.info("病毒扫描引擎不可用告警发送成功: engine={}", engineName);
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("发送病毒扫描引擎不可用告警失败: engine={}, error={}", 
                     engineName, e.getMessage(), e);
            
            recordEngineAlertAuditLog(engineName, errorMessage, false);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 发送隔离空间不足告警
     * 
     * @param currentSize 当前大小
     * @param maxSize 最大大小
     * @return 告警发送结果
     */
    @Async
    public CompletableFuture<Boolean> sendQuarantineSpaceAlert(long currentSize, long maxSize) {
        if (!alertEnabled || !emailAlertEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        double usagePercent = (double) currentSize / maxSize * 100;
        
        log.warn("隔离空间使用率过高，发送告警: usage={}%", String.format("%.2f", usagePercent));
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("【安全告警】隔离空间使用率过高");
            message.setText(buildQuarantineSpaceAlertContent(currentSize, maxSize, usagePercent));
            
            mailSender.send(message);
            
            log.info("隔离空间告警发送成功: usage={}%", String.format("%.2f", usagePercent));
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("发送隔离空间告警失败: error={}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 判断是否应该发送告警
     */
    private boolean shouldAlert(VirusScanResult.ThreatLevel threatLevel) {
        if (threatLevel == null) {
            return false;
        }
        
        VirusScanResult.ThreatLevel minLevel;
        try {
            minLevel = VirusScanResult.ThreatLevel.valueOf(minThreatLevel);
        } catch (IllegalArgumentException e) {
            log.warn("无效的最小威胁级别配置: {}, 使用默认值 MEDIUM", minThreatLevel);
            minLevel = VirusScanResult.ThreatLevel.MEDIUM;
        }
        
        return threatLevel.ordinal() >= minLevel.ordinal();
    }
    
    /**
     * 发送邮件告警
     */
    private boolean sendEmailAlert(VirusScanResult scanResult) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject(buildAlertSubject(scanResult));
            message.setText(buildAlertContent(scanResult));
            
            mailSender.send(message);
            
            log.info("病毒检测邮件告警发送成功: to={}, virus={}", 
                    adminEmail, scanResult.getVirusName());
            return true;
            
        } catch (Exception e) {
            log.error("发送病毒检测邮件告警失败: error={}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建告警主题
     */
    private String buildAlertSubject(VirusScanResult scanResult) {
        String threatLevel = scanResult.getThreatLevel() != null ? 
            scanResult.getThreatLevel().name() : "UNKNOWN";
        
        return String.format("【安全告警】检测到病毒文件 - 威胁级别: %s", threatLevel);
    }
    
    /**
     * 构建告警内容
     */
    private String buildAlertContent(VirusScanResult scanResult) {
        StringBuilder content = new StringBuilder();
        
        content.append("MyWeb博客系统安全告警\n");
        content.append("=" .repeat(50)).append("\n\n");
        
        content.append("告警时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("告警类型: 病毒文件检测\n");
        content.append("威胁级别: ").append(scanResult.getThreatLevel()).append("\n\n");
        
        content.append("文件信息:\n");
        content.append("- 文件名: ").append(scanResult.getFilename()).append("\n");
        content.append("- 文件大小: ").append(formatFileSize(scanResult.getFileSize())).append("\n");
        content.append("- 病毒名称: ").append(scanResult.getVirusName()).append("\n");
        content.append("- 扫描引擎: ").append(scanResult.getEngineName()).append("\n");
        content.append("- 扫描时间: ").append(scanResult.getScanEndTime()).append("\n\n");
        
        content.append("用户信息:\n");
        content.append("- 用户ID: ").append(scanResult.getUserId()).append("\n");
        content.append("- 用户名: ").append(scanResult.getUsername()).append("\n\n");
        
        content.append("扫描详情:\n");
        content.append(scanResult.getDetails()).append("\n\n");
        
        content.append("建议操作:\n");
        content.append(scanResult.getRecommendedAction()).append("\n\n");
        
        content.append("请立即检查系统安全状态并采取相应措施。\n");
        content.append("\n--\n");
        content.append("MyWeb安全监控系统");
        
        return content.toString();
    }
    
    /**
     * 构建引擎不可用告警内容
     */
    private String buildEngineUnavailableAlertContent(String engineName, String errorMessage) {
        StringBuilder content = new StringBuilder();
        
        content.append("MyWeb博客系统安全告警\n");
        content.append("=" .repeat(50)).append("\n\n");
        
        content.append("告警时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("告警类型: 病毒扫描引擎不可用\n");
        content.append("威胁级别: HIGH\n\n");
        
        content.append("引擎信息:\n");
        content.append("- 引擎名称: ").append(engineName).append("\n");
        content.append("- 错误信息: ").append(errorMessage).append("\n\n");
        
        content.append("影响:\n");
        content.append("- 文件上传安全检查可能受到影响\n");
        content.append("- 恶意文件可能无法被及时检测\n\n");
        
        content.append("建议操作:\n");
        content.append("1. 检查病毒扫描引擎服务状态\n");
        content.append("2. 确认网络连接和配置正确性\n");
        content.append("3. 考虑启用备用扫描方案\n");
        content.append("4. 暂时限制文件上传功能\n\n");
        
        content.append("请立即处理此问题以确保系统安全。\n");
        content.append("\n--\n");
        content.append("MyWeb安全监控系统");
        
        return content.toString();
    }
    
    /**
     * 构建隔离空间告警内容
     */
    private String buildQuarantineSpaceAlertContent(long currentSize, long maxSize, double usagePercent) {
        StringBuilder content = new StringBuilder();
        
        content.append("MyWeb博客系统安全告警\n");
        content.append("=" .repeat(50)).append("\n\n");
        
        content.append("告警时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("告警类型: 隔离空间使用率过高\n");
        content.append("威胁级别: MEDIUM\n\n");
        
        content.append("空间使用情况:\n");
        content.append("- 当前使用: ").append(formatFileSize(currentSize)).append("\n");
        content.append("- 最大容量: ").append(formatFileSize(maxSize)).append("\n");
        content.append("- 使用率: ").append(String.format("%.2f%%", usagePercent)).append("\n\n");
        
        content.append("建议操作:\n");
        content.append("1. 清理过期的隔离文件\n");
        content.append("2. 检查隔离文件的必要性\n");
        content.append("3. 考虑增加隔离空间容量\n");
        content.append("4. 调整隔离文件保留策略\n\n");
        
        content.append("请及时处理以避免影响安全功能。\n");
        content.append("\n--\n");
        content.append("MyWeb安全监控系统");
        
        return content.toString();
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 记录告警审计日志
     */
    private void recordAlertAuditLog(VirusScanResult scanResult, boolean success) {
        try {
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .userId(scanResult.getUserId())
                .username(scanResult.getUsername())
                .operation(AuditOperation.SECURITY_ALERT)
                .resourceType("VIRUS_ALERT")
                .result(success ? "SUCCESS" : "FAILURE")
                .timestamp(LocalDateTime.now())
                .description(String.format("病毒检测告警: %s - %s", 
                           scanResult.getFilename(), scanResult.getVirusName()))
                .riskLevel(4) // 安全告警为中高风险
                .build();
            
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录告警审计日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录告警失败审计日志
     */
    private void recordAlertFailureAuditLog(VirusScanResult scanResult, String errorMessage) {
        try {
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .userId(scanResult.getUserId())
                .username(scanResult.getUsername())
                .operation(AuditOperation.SECURITY_ALERT)
                .resourceType("VIRUS_ALERT")
                .result("ERROR")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .description(String.format("病毒检测告警失败: %s", scanResult.getFilename()))
                .riskLevel(3) // 告警失败为中等风险
                .build();
            
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录告警失败审计日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录引擎告警审计日志
     */
    private void recordEngineAlertAuditLog(String engineName, String errorMessage, boolean success) {
        try {
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .operation(AuditOperation.SECURITY_ALERT)
                .resourceType("SCAN_ENGINE")
                .result(success ? "SUCCESS" : "FAILURE")
                .errorMessage(success ? null : errorMessage)
                .timestamp(LocalDateTime.now())
                .description(String.format("病毒扫描引擎告警: %s", engineName))
                .riskLevel(4) // 引擎不可用为中高风险
                .build();
            
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录引擎告警审计日志失败: {}", e.getMessage(), e);
        }
    }
}