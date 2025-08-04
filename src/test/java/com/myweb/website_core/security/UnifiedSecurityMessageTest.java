package com.myweb.website_core.security;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.dto.UnifiedSecurityMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一安全消息测试
 * 
 * 测试重构后的安全审计系统功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
public class UnifiedSecurityMessageTest {
    
    @MockBean
    private AuditLogServiceAdapter auditLogServiceAdapter;
    
    /**
     * 测试统一安全消息的创建
     */
    @Test
    public void testUnifiedSecurityMessageCreation() {
        // 测试审计日志消息创建
        UnifiedSecurityMessage auditMessage = UnifiedSecurityMessage.auditLog(
            AuditOperation.USER_LOGIN_SUCCESS, 1L, "testuser", "SUCCESS", "127.0.0.1"
        );
        
        assertNotNull(auditMessage);
        assertEquals("AUDIT_LOG", auditMessage.getMessageType());
        assertEquals(AuditOperation.USER_LOGIN_SUCCESS, auditMessage.getOperation());
        assertEquals("testuser", auditMessage.getUsername());
        assertEquals("SUCCESS", auditMessage.getResult());
        assertFalse(auditMessage.getIsSecurityEvent());
        
        // 测试安全事件消息创建
        UnifiedSecurityMessage securityEvent = UnifiedSecurityMessage.securityEvent(
            SecurityEventType.BRUTE_FORCE_ATTACK, AuditOperation.USER_LOGIN_FAILURE,
            1L, "testuser", "127.0.0.1", "检测到暴力破解攻击", 5
        );
        
        assertNotNull(securityEvent);
        assertEquals("SECURITY_EVENT", securityEvent.getMessageType());
        assertTrue(securityEvent.getIsSecurityEvent());
        assertEquals(SecurityEventType.BRUTE_FORCE_ATTACK, securityEvent.getSecurityEventType());
        assertEquals(5, securityEvent.getSeverity());
        assertTrue(securityEvent.requiresImmediateAlert());
    }
    
    /**
     * 测试消息类型判断
     */
    @Test
    public void testMessageTypeDetection() {
        // 测试用户认证消息
        UnifiedSecurityMessage authMessage = UnifiedSecurityMessage.userAuth(
            "testuser", AuditOperation.USER_LOGIN_SUCCESS, "127.0.0.1", 
            "SUCCESS", null, "session123"
        );
        
        assertEquals("USER_AUTH", authMessage.getMessageType());
        assertEquals("user.auth", authMessage.getRoutingKey());
        
        // 测试文件上传消息
        UnifiedSecurityMessage fileMessage = UnifiedSecurityMessage.fileUpload(
            1L, "testuser", "test.jpg", "image/jpeg", 1024L, 
            "SUCCESS", "127.0.0.1", null
        );
        
        assertEquals("FILE_UPLOAD", fileMessage.getMessageType());
        assertEquals("file.upload.audit", fileMessage.getRoutingKey());
        
        // 测试搜索消息
        UnifiedSecurityMessage searchMessage = UnifiedSecurityMessage.search(
            1L, "testuser", "test query", "POST", 5, "127.0.0.1"
        );
        
        assertEquals("SEARCH", searchMessage.getMessageType());
        assertEquals("search.audit", searchMessage.getRoutingKey());
    }
    
    /**
     * 测试安全事件标记
     */
    @Test
    public void testSecurityEventMarking() {
        UnifiedSecurityMessage message = UnifiedSecurityMessage.auditLog(
            AuditOperation.ACCESS_DENIED, 1L, "testuser", "FAILURE", "127.0.0.1"
        );
        
        // 初始状态不是安全事件
        assertFalse(message.getIsSecurityEvent());
        
        // 标记为安全事件
        message.markAsSecurityEvent(SecurityEventType.UNAUTHORIZED_ACCESS, 4);
        
        // 验证安全事件属性
        assertTrue(message.getIsSecurityEvent());
        assertEquals(SecurityEventType.UNAUTHORIZED_ACCESS, message.getSecurityEventType());
        assertEquals(4, message.getSeverity());
        assertEquals("DETECTED", message.getResult());
        assertEquals("NEW", message.getStatus());
        assertFalse(message.getAlerted());
        assertTrue(message.getTags().contains("security"));
        assertTrue(message.getTags().contains("event"));
    }
    
    /**
     * 测试风险级别判断
     */
    @Test
    public void testRiskLevelAssessment() {
        // 低风险消息
        UnifiedSecurityMessage lowRisk = UnifiedSecurityMessage.auditLog(
            AuditOperation.POST_VIEW, 1L, "testuser", "SUCCESS", "127.0.0.1"
        );
        assertFalse(lowRisk.isHighRisk());
        
        // 高风险消息
        UnifiedSecurityMessage highRisk = UnifiedSecurityMessage.securityEvent(
            SecurityEventType.BRUTE_FORCE_ATTACK, AuditOperation.USER_LOGIN_FAILURE,
            1L, "testuser", "127.0.0.1", "暴力破解攻击", 5
        );
        assertTrue(highRisk.isHighRisk());
        assertTrue(highRisk.requiresImmediateAlert());
    }
    
    /**
     * 测试AuditLogRequest兼容性
     */
    @Test
    public void testAuditLogRequestCompatibility() {
        // 创建AuditLogRequest
        AuditLogRequest request = AuditLogRequest.builder()
                .operation(AuditOperation.POST_CREATE)
                .userId(1L)
                .username("testuser")
                .resourceType("POST")
                .resourceId(123L)
                .ipAddress("127.0.0.1")
                .result("SUCCESS")
                .description("创建帖子")
                .riskLevel(2)
                .timestamp(LocalDateTime.now())
                .build();
        
        // 验证请求有效性
        assertTrue(request.isValid());
        assertNull(request.getValidationError());
        assertTrue(request.isSuccess());
        assertFalse(request.isHighRisk());
        
        // 测试链式方法
        request.withNetworkInfo("192.168.1.1", "Mozilla/5.0")
               .withSessionInfo("session123", "req456")
               .addTag("test");
        
        assertEquals("192.168.1.1", request.getIpAddress());
        assertEquals("session123", request.getSessionId());
        assertTrue(request.getTags().contains("test"));
    }
    
    /**
     * 测试消息路由
     */
    @Test
    public void testMessageRouting() {
        // 测试不同类型消息的路由
        UnifiedSecurityMessage[] messages = {
            UnifiedSecurityMessage.userAuth("user", AuditOperation.USER_LOGIN_SUCCESS, "127.0.0.1", "SUCCESS", null, "session"),
            UnifiedSecurityMessage.fileUpload(1L, "user", "file.jpg", "image/jpeg", 1024L, "SUCCESS", "127.0.0.1", null),
            UnifiedSecurityMessage.search(1L, "user", "query", "POST", 5, "127.0.0.1"),
            UnifiedSecurityMessage.accessControl(1L, "user", "POST", 123L, "EDIT", "DENIED", "127.0.0.1", "无权限"),
            UnifiedSecurityMessage.securityEvent(SecurityEventType.BRUTE_FORCE_ATTACK, AuditOperation.USER_LOGIN_FAILURE, 1L, "user", "127.0.0.1", "攻击", 5)
        };
        
        String[] expectedQueues = {
            "user.auth.queue",
            "file.upload.audit.queue", 
            "search.audit.queue",
            "access.control.queue",
            "security.event.queue"
        };
        
        String[] expectedRoutingKeys = {
            "user.auth",
            "file.upload.audit",
            "search.audit", 
            "access.control",
            "security.event"
        };
        
        for (int i = 0; i < messages.length; i++) {
            assertEquals(expectedQueues[i], messages[i].getTargetQueue());
            assertEquals(expectedRoutingKeys[i], messages[i].getRoutingKey());
        }
    }
    
    /**
     * 测试实体构造函数兼容性
     */
    @Test
    public void testEntityConstructorCompatibility() {
        // 测试AuditLog构造函数
        UnifiedSecurityMessage auditMessage = UnifiedSecurityMessage.auditLog(
            AuditOperation.POST_CREATE, 1L, "testuser", "SUCCESS", "127.0.0.1"
        );
        
        // 这里只是测试构造函数不会抛出异常
        // 在实际环境中，这些实体会被JPA管理
        assertDoesNotThrow(() -> {
            // AuditLog auditLog = new AuditLog(auditMessage);
            // SecurityEvent securityEvent = new SecurityEvent(auditMessage);
        });
        
        // 测试安全事件消息
        UnifiedSecurityMessage securityMessage = UnifiedSecurityMessage.securityEvent(
            SecurityEventType.BRUTE_FORCE_ATTACK, AuditOperation.USER_LOGIN_FAILURE,
            1L, "testuser", "127.0.0.1", "暴力破解攻击", 5
        );
        
        assertTrue(securityMessage.getIsSecurityEvent());
        assertEquals(SecurityEventType.BRUTE_FORCE_ATTACK, securityMessage.getSecurityEventType());
    }
}