package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.infrastructure.security.audit.AuditAspect;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AOP审计切面集成测试
 * 
 * 测试审计切面在Spring容器中的集成功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@SpringBootTest(classes = {AuditAspectIntegrationTest.TestConfig.class})
@ContextConfiguration
class AuditAspectIntegrationTest {
    
    @MockBean
    private AuditLogService auditLogService;
    
    @Autowired
    private TestAuditService testAuditService;
    
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testAuditAspectIntegration() {
        // 模拟审计日志服务
        when(auditLogService.logOperation(any(AuditLogRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // 执行被审计的方法
        String result = testAuditService.auditedMethod("test-param");
        
        // 验证方法执行结果
        assertEquals("success", result);
        
        // 验证审计日志被记录
        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService, times(1)).logOperation(captor.capture());
        
        AuditLogRequest auditRequest = captor.getValue();
        assertNotNull(auditRequest);
        assertEquals(AuditOperation.POST_CREATE, auditRequest.getOperation());
        assertEquals("testuser", auditRequest.getUsername());
        assertEquals("SUCCESS", auditRequest.getResult());
        assertEquals("POST", auditRequest.getResourceType());
        assertNotNull(auditRequest.getExecutionTime());
        assertTrue(auditRequest.getExecutionTime() >= 0);
    }
    
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testAuditAspectWithException() {
        // 模拟审计日志服务
        when(auditLogService.logOperation(any(AuditLogRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // 执行会抛出异常的方法
        assertThrows(RuntimeException.class, () -> 
            testAuditService.auditedMethodWithException("test-param"));
        
        // 验证审计日志被记录
        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService, times(1)).logOperation(captor.capture());
        
        AuditLogRequest auditRequest = captor.getValue();
        assertNotNull(auditRequest);
        assertEquals(AuditOperation.POST_DELETE, auditRequest.getOperation());
        assertEquals("testuser", auditRequest.getUsername());
        assertEquals("FAILURE", auditRequest.getResult());
        assertNotNull(auditRequest.getErrorMessage());
        assertTrue(auditRequest.getErrorMessage().contains("RuntimeException"));
    }
    
    @Test
    void testAuditAspectWithAnonymousUser() {
        // 模拟审计日志服务
        when(auditLogService.logOperation(any(AuditLogRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // 执行被审计的方法（匿名用户）
        String result = testAuditService.auditedMethod("test-param");
        
        // 验证方法执行结果
        assertEquals("success", result);
        
        // 验证审计日志被记录
        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService, times(1)).logOperation(captor.capture());
        
        AuditLogRequest auditRequest = captor.getValue();
        assertNotNull(auditRequest);
        assertEquals("anonymous", auditRequest.getUsername());
        assertNull(auditRequest.getUserId());
    }
    
    /**
     * 测试配置类
     */
    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {
        
        @Bean
        public AuditAspect auditAspect(AuditLogServiceAdapter auditLogService) {
            return new AuditAspect(auditLogService, new com.fasterxml.jackson.databind.ObjectMapper());
        }
        
        @Bean
        public TestAuditService testAuditService() {
            return new TestAuditService();
        }
    }
    
    /**
     * 测试审计服务
     */
    public static class TestAuditService {
        
        @Auditable(
            operation = AuditOperation.POST_CREATE,
            resourceType = "POST",
            description = "集成测试方法"
        )
        public String auditedMethod(String param) {
            return "success";
        }
        
        @Auditable(
            operation = AuditOperation.POST_DELETE,
            resourceType = "POST",
            description = "集成测试异常方法"
        )
        public String auditedMethodWithException(String param) {
            throw new RuntimeException("测试异常");
        }
    }
}