package com.myweb.website_core.application.service.security.audit;

import com.myweb.website_core.domain.security.entity.SecurityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 安全告警服务
 * 
 * 负责发送安全事件的告警通知，支持邮件和短信等多种方式
 * 符合GB/T 22239-2019二级等保要求的安全监控和告警机制
 */
@Slf4j
@Service
public class SecurityAlertService {
    
    private final JavaMailSender mailSender;
    
//    @Value("${app.security.event.alert.email-enabled:true}")
    private final boolean emailAlertEnabled= true;
    
 //   @Value("${app.security.event.alert.email-recipients:}")
    private final List<String> alertEmailRecipients= List.of(
            "3281314509@qq.com",
            "admin@myweb.com",
         "security@myweb.com",
         "ops@myweb.com"
 );
    
    //@Value("${app.security.event.alert.email-from:security@myweb.com}")
    private final String alertEmailFrom="3281314509@qq.com";
    
    //@Value("${app.security.event.alert.sms-enabled:false}")
    private final boolean smsAlertEnabled= false;
    
    //@Value("${app.security.event.alert.sms-recipients:}")
    private final List<String> alertSmsRecipients=List.of(
            "13800138000",
            "13900139000"
    );
    
    public SecurityAlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * 发送安全事件告警
     * 
     * @param event 安全事件
     * @return 异步结果
     */
    @Async
    public CompletableFuture<Void> sendAlert(SecurityEvent event) {
        try {
            log.info("发送安全事件告警: eventId={}, eventType={}, severity={}", 
                    event.getId(), event.getEventType(), event.getSeverity());
            
            // 根据严重级别决定告警方式
            if (event.requiresImmediateAlert()) {
                // 严重事件：邮件 + 短信
                sendEmailAlert(event);
                sendSmsAlert(event);
            } else if (event.isHighRisk()) {
                // 高危事件：邮件告警
                sendEmailAlert(event);
            } else {
                // 其他事件：记录日志
                log.warn("安全事件记录: {}", formatEventForLog(event));
            }
            
            log.info("安全事件告警发送完成: eventId={}", event.getId());
            
        } catch (Exception e) {
            log.error("发送安全事件告警失败: eventId={}, error={}", 
                    event.getId(), e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 批量发送安全事件告警
     * 
     * @param events 安全事件列表
     * @return 异步结果
     */
    @Async
    public CompletableFuture<Void> sendBatchAlert(List<SecurityEvent> events) {
        try {
            log.info("批量发送安全事件告警: eventCount={}", events.size());
            
            // 按严重级别分组
            List<SecurityEvent> criticalEvents = events.stream()
                    .filter(SecurityEvent::requiresImmediateAlert)
                    .toList();
            
            List<SecurityEvent> highRiskEvents = events.stream()
                    .filter(e -> e.isHighRisk() && !e.requiresImmediateAlert())
                    .toList();
            
            // 发送批量告警
            if (!criticalEvents.isEmpty()) {
                sendBatchEmailAlert(criticalEvents, "严重安全事件告警");
                sendBatchSmsAlert(criticalEvents);
            }
            
            if (!highRiskEvents.isEmpty()) {
                sendBatchEmailAlert(highRiskEvents, "高危安全事件告警");
            }
            
            log.info("批量安全事件告警发送完成: criticalCount={}, highRiskCount={}", 
                    criticalEvents.size(), highRiskEvents.size());
            
        } catch (Exception e) {
            log.error("批量发送安全事件告警失败: error={}", e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 发送邮件告警
     */
    private void sendEmailAlert(SecurityEvent event) {
        if (!emailAlertEnabled || alertEmailRecipients == null || alertEmailRecipients.isEmpty()) {
            log.debug("邮件告警未启用或未配置收件人");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(alertEmailFrom);
            message.setTo(alertEmailRecipients.toArray(new String[0]));
            message.setSubject(buildEmailSubject(event));
            message.setText(buildEmailContent(event));
            
            mailSender.send(message);
            log.info("安全事件邮件告警发送成功: eventId={}, recipients={}", 
                    event.getId(), alertEmailRecipients);
            
        } catch (Exception e) {
            log.error("发送安全事件邮件告警失败: eventId={}, error={}", 
                    event.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送批量邮件告警
     */
    private void sendBatchEmailAlert(List<SecurityEvent> events, String alertType) {
        if (!emailAlertEnabled || alertEmailRecipients == null || alertEmailRecipients.isEmpty()) {
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(alertEmailFrom);
            message.setTo(alertEmailRecipients.toArray(new String[0]));
            message.setSubject(String.format("[MyWeb安全告警] %s - %d个事件", alertType, events.size()));
            message.setText(buildBatchEmailContent(events, alertType));
            
            mailSender.send(message);
            log.info("批量安全事件邮件告警发送成功: eventCount={}, recipients={}", 
                    events.size(), alertEmailRecipients);
            
        } catch (Exception e) {
            log.error("发送批量安全事件邮件告警失败: eventCount={}, error={}", 
                    events.size(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送短信告警
     */
    private void sendSmsAlert(SecurityEvent event) {
        if (!smsAlertEnabled || alertSmsRecipients == null || alertSmsRecipients.isEmpty()) {
            log.debug("短信告警未启用或未配置收件人");
            return;
        }
        
        try {
            String smsContent = buildSmsContent(event);
            
            // 这里应该集成实际的短信服务提供商API
            // 例如阿里云短信、腾讯云短信等
            // 目前仅记录日志模拟发送
            log.info("模拟发送短信告警: recipients={}, content={}", 
                    alertSmsRecipients, smsContent);
            
            // TODO: 集成实际短信服务
            // smsService.sendSms(alertSmsRecipients, smsContent);
            
        } catch (Exception e) {
            log.error("发送安全事件短信告警失败: eventId={}, error={}", 
                    event.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送批量短信告警
     */
    private void sendBatchSmsAlert(List<SecurityEvent> events) {
        if (!smsAlertEnabled || alertSmsRecipients == null || alertSmsRecipients.isEmpty()) {
            return;
        }
        
        try {
            String smsContent = String.format("MyWeb安全告警：检测到%d个严重安全事件，请立即处理！", events.size());
            
            // 模拟发送短信
            log.info("模拟发送批量短信告警: recipients={}, content={}", 
                    alertSmsRecipients, smsContent);
            
            // TODO: 集成实际短信服务
            // smsService.sendSms(alertSmsRecipients, smsContent);
            
        } catch (Exception e) {
            log.error("发送批量安全事件短信告警失败: eventCount={}, error={}", 
                    events.size(), e.getMessage(), e);
        }
    }
    
    /**
     * 构建邮件主题
     */
    private String buildEmailSubject(SecurityEvent event) {
        return String.format("[MyWeb安全告警] %s - %s", 
                event.getEventType().getName(), 
                event.getEventType().getSeverityDescription());
    }
    
    /**
     * 构建邮件内容
     */
    private String buildEmailContent(SecurityEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return String.format("""
            MyWeb博客系统安全告警
            
            ==================== 事件详情 ====================
            
            事件类型：%s
            事件标题：%s
            严重级别：%s (%d)
            事件描述：%s
            
            发生时间：%s
            相关用户：%s (ID: %s)
            源IP地址：%s
            请求URI：%s
            会话ID：%s
            
            风险评分：%s
            事件状态：%s
            
            ==================== 处理建议 ====================
            
            %s
            
            ==================== 系统信息 ====================
            
            事件ID：%s
            用户代理：%s
            
            请及时登录系统查看详细信息并采取相应措施。
            
            此邮件由MyWeb安全监控系统自动发送，请勿回复。
            发送时间：%s
            """,
            event.getEventType().getName(),
            event.getTitle(),
            event.getEventType().getSeverityDescription(),
            event.getSeverity(),
            event.getDescription(),
            event.getEventTime().format(formatter),
            event.getUsername() != null ? event.getUsername() : "未知",
            event.getUserId() != null ? event.getUserId().toString() : "未知",
            event.getSourceIp() != null ? event.getSourceIp() : "未知",
            event.getRequestUri() != null ? event.getRequestUri() : "未知",
            event.getSessionId() != null ? event.getSessionId() : "未知",
            event.getRiskScore() != null ? event.getRiskScore().toString() : "未评分",
            event.getStatus(),
            getHandlingSuggestion(event),
            event.getId(),
            event.getUserAgent() != null ? event.getUserAgent() : "未知",
            java.time.LocalDateTime.now().format(formatter)
        );
    }
    
    /**
     * 构建批量邮件内容
     */
    private String buildBatchEmailContent(List<SecurityEvent> events, String alertType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder content = new StringBuilder();
        
        content.append(String.format("""
            MyWeb博客系统批量安全告警
            
            ==================== 告警概要 ====================
            
            告警类型：%s
            事件数量：%d
            告警时间：%s
            
            ==================== 事件列表 ====================
            
            """, alertType, events.size(), java.time.LocalDateTime.now().format(formatter)));
        
        for (int i = 0; i < events.size() && i < 10; i++) { // 最多显示10个事件
            SecurityEvent event = events.get(i);
            content.append(String.format("""
                %d. %s
                   时间：%s | 用户：%s | IP：%s
                   描述：%s
                
                """,
                i + 1,
                event.getTitle(),
                event.getEventTime().format(formatter),
                event.getUsername() != null ? event.getUsername() : "未知",
                event.getSourceIp() != null ? event.getSourceIp() : "未知",
                event.getDescription()
            ));
        }
        
        if (events.size() > 10) {
            content.append(String.format("... 还有 %d 个事件，请登录系统查看完整列表。\n\n", events.size() - 10));
        }
        
        content.append("""
            ==================== 处理建议 ====================
            
            1. 立即登录系统查看详细信息
            2. 分析事件模式和关联性
            3. 采取相应的安全措施
            4. 更新安全策略和规则
            
            此邮件由MyWeb安全监控系统自动发送，请勿回复。
            """);
        
        return content.toString();
    }
    
    /**
     * 构建短信内容
     */
    private String buildSmsContent(SecurityEvent event) {
        return String.format("MyWeb安全告警：%s，严重级别%d，时间%s，请立即处理！", 
                event.getTitle(), 
                event.getSeverity(),
                event.getEventTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
    }
    
    /**
     * 获取处理建议
     */
    private String getHandlingSuggestion(SecurityEvent event) {
        return switch (event.getEventType()) {
            case BRUTE_FORCE_ATTACK, CONTINUOUS_LOGIN_FAILURE -> 
                "建议立即锁定相关账户，检查登录日志，加强密码策略。";
            case SQL_INJECTION_ATTEMPT, XSS_ATTACK_ATTEMPT -> 
                "建议检查输入验证机制，更新安全过滤规则，审查相关代码。";
            case ACCESS_DENIED, PRIVILEGE_ESCALATION ->
                "建议检查权限配置，审查用户权限，加强访问控制。";
            case SUSPICIOUS_IP_ACCESS, DDOS_ATTACK -> 
                "建议将可疑IP加入黑名单，检查防火墙规则，考虑启用CDN防护。";
            case MALICIOUS_FILE_UPLOAD, VIRUS_DETECTED -> 
                "建议隔离相关文件，扫描系统安全，更新病毒库和检测规则。";
            case DATA_INTEGRITY_VIOLATION, SENSITIVE_DATA_LEAK -> 
                "建议立即检查数据完整性，审查数据访问日志，评估影响范围。";
            default -> "建议根据事件类型采取相应的安全措施，详细分析事件原因。";
        };
    }
    
    /**
     * 格式化事件用于日志记录
     */
    private String formatEventForLog(SecurityEvent event) {
        return String.format("SecurityEvent[id=%d, type=%s, severity=%d, user=%s, ip=%s, time=%s]",
                event.getId(),
                event.getEventType(),
                event.getSeverity(),
                event.getUsername(),
                event.getSourceIp(),
                event.getEventTime());
    }
}