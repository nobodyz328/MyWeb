package com.myweb.website_core.application.service;

import com.myweb.website_core.application.service.security.SecurityAlertService;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 安全告警服务测试类
 */
@ExtendWith(MockitoExtension.class)
class SecurityAlertServiceTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @InjectMocks
    private SecurityAlertService securityAlertService;
    
    private SecurityEvent criticalEvent;
    private SecurityEvent highRiskEvent;
    private SecurityEvent mediumRiskEvent;
    
    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(securityAlertService, "emailAlertEnabled", true);
        ReflectionTestUtils.setField(securityAlertService, "alertEmailRecipients", 
                List.of("admin@myweb.com", "security@myweb.com"));
        ReflectionTestUtils.setField(securityAlertService, "alertEmailFrom", "security@myweb.com");
        ReflectionTestUtils.setField(securityAlertService, "smsAlertEnabled", false);
        ReflectionTestUtils.setField(securityAlertService, "alertSmsRecipients", List.of());
        
        // 创建测试事件
        criticalEvent = SecurityEvent.builder()
                .id(1L)
                .eventType(SecurityEventType.BRUTE_FORCE_ATTACK)
                .title("暴力破解攻击")
                .description("检测到暴力破解攻击")
                .severity(5)
                .userId(1L)
                .username("testuser")
                .sourceIp("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .requestUri("/api/auth/login")
                .requestMethod("POST")
                .sessionId("test-session")
                .eventTime(LocalDateTime.now())
                .status("NEW")
                .alerted(false)
                .riskScore(95)
                .build();
        
        highRiskEvent = SecurityEvent.builder()
                .id(2L)
                .eventType(SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT)
                .title("未授权访问尝试")
                .description("检测到未授权访问尝试")
                .severity(4)
                .userId(2L)
                .username("testuser2")
                .sourceIp("192.168.1.101")
                .userAgent("Mozilla/5.0")
                .requestUri("/api/admin/users")
                .requestMethod("GET")
                .sessionId("test-session-2")
                .eventTime(LocalDateTime.now())
                .status("NEW")
                .alerted(false)
                .riskScore(80)
                .build();
        
        mediumRiskEvent = SecurityEvent.builder()
                .id(3L)
                .eventType(SecurityEventType.ABNORMAL_ACCESS_FREQUENCY)
                .title("访问频率异常")
                .description("检测到访问频率异常")
                .severity(3)
                .userId(3L)
                .username("testuser3")
                .sourceIp("192.168.1.102")
                .userAgent("Mozilla/5.0")
                .requestUri("/api/posts")
                .requestMethod("GET")
                .sessionId("test-session-3")
                .eventTime(LocalDateTime.now())
                .status("NEW")
                .alerted(false)
                .riskScore(60)
                .build();
    }
    
    @Test
    void testSendAlert_CriticalEvent() throws Exception {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendAlert(criticalEvent);
        result.get(); // 等待异步完成
        
        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        // 严重事件应该发送邮件告警（短信未启用）
    }
    
    @Test
    void testSendAlert_HighRiskEvent() throws Exception {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendAlert(highRiskEvent);
        result.get(); // 等待异步完成
        
        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        // 高危事件应该发送邮件告警
    }
    
    @Test
    void testSendAlert_MediumRiskEvent() throws Exception {
        // Given
        // 中等风险事件不应该发送告警，只记录日志
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendAlert(mediumRiskEvent);
        result.get(); // 等待异步完成
        
        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        // 中等风险事件不发送告警
    }
    
    @Test
    void testSendBatchAlert_MixedEvents() throws Exception {
        // Given
        List<SecurityEvent> events = Arrays.asList(criticalEvent, highRiskEvent, mediumRiskEvent);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendBatchAlert(events);
        result.get(); // 等待异步完成
        
        // Then
        // 应该发送2封邮件：1封严重事件告警，1封高危事件告警
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
    
    @Test
    void testSendAlert_EmailDisabled() throws Exception {
        // Given
        ReflectionTestUtils.setField(securityAlertService, "emailAlertEnabled", false);
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendAlert(criticalEvent);
        result.get(); // 等待异步完成
        
        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        // 邮件告警禁用时不发送邮件
    }
    
    @Test
    void testSendAlert_NoRecipients() throws Exception {
        // Given
        ReflectionTestUtils.setField(securityAlertService, "alertEmailRecipients", List.of());
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendAlert(criticalEvent);
        result.get(); // 等待异步完成
        
        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        // 没有收件人时不发送邮件
    }
    
    @Test
    void testSendAlert_MailSenderException() throws Exception {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendAlert(criticalEvent);
        result.get(); // 等待异步完成
        
        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        // 异常应该被捕获并记录日志，不影响程序执行
    }
    
    @Test
    void testSendAlert_SmsEnabled() throws Exception {
        // Given
        ReflectionTestUtils.setField(securityAlertService, "smsAlertEnabled", true);
        ReflectionTestUtils.setField(securityAlertService, "alertSmsRecipients", 
                List.of("13800138000", "13900139000"));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        
        // When
        CompletableFuture<Void> result = securityAlertService.sendAlert(criticalEvent);
        result.get(); // 等待异步完成
        
        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        // 严重事件应该同时发送邮件和短信告警（这里短信是模拟的）
    }
    
    @Test
    void testBuildEmailContent() {
        // 这个测试验证邮件内容构建的正确性
        // 由于buildEmailContent是私有方法，我们通过发送邮件来间接测试
        
        // Given
        doAnswer(invocation -> {
            SimpleMailMessage message = invocation.getArgument(0);
            
            // 验证邮件主题
            assertTrue(message.getSubject().contains("暴力破解攻击"));
            assertTrue(message.getSubject().contains("严重"));
            
            // 验证邮件内容包含关键信息
            String content = message.getText();
            assertTrue(content.contains("暴力破解攻击"));
            assertTrue(content.contains("testuser"));
            assertTrue(content.contains("192.168.1.100"));
            assertTrue(content.contains("/api/auth/login"));
            assertTrue(content.contains("95"));
            
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));
        
        // When
        securityAlertService.sendAlert(criticalEvent);
        
        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
    
    @Test
    void testBuildBatchEmailContent() {
        // Given
        List<SecurityEvent> events = Arrays.asList(criticalEvent, highRiskEvent);
        
        doAnswer(invocation -> {
            SimpleMailMessage message = invocation.getArgument(0);
            
            // 验证批量邮件主题
            assertTrue(message.getSubject().contains("严重安全事件告警"));
            assertTrue(message.getSubject().contains("2个事件"));
            
            // 验证邮件内容包含事件列表
            String content = message.getText();
            assertTrue(content.contains("暴力破解攻击"));
            assertTrue(content.contains("未授权访问尝试"));
            assertTrue(content.contains("testuser"));
            assertTrue(content.contains("testuser2"));
            
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));
        
        // When
        securityAlertService.sendBatchAlert(events);
        
        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}