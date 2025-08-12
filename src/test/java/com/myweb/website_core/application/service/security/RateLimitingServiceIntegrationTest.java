package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.IPS.ratelimit.RateLimitingService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.infrastructure.config.properties.RateLimitProperties;
import com.myweb.website_core.common.enums.AuditOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 访问频率限制服务集成测试
 * 测试与AuditLogService的集成
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingServiceIntegrationTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private RateLimitProperties rateLimitProperties;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private RateLimitingService rateLimitingService;
    
    private RateLimitProperties.EndpointLimit defaultLimit;
    
    @BeforeEach
    void setUp() {
        // 设置默认限制配置
        defaultLimit = new RateLimitProperties.EndpointLimit();
        defaultLimit.setMaxRequests(10);
        defaultLimit.setWindowSizeSeconds(60);
        defaultLimit.setLimitType("IP");
        defaultLimit.setEnabled(true);
        
        // 设置Redis配置
        RateLimitProperties.Redis redisConfig = new RateLimitProperties.Redis();
        redisConfig.setKeyPrefix("rate_limit:");
        
        // 设置告警配置
        RateLimitProperties.Alert alertConfig = new RateLimitProperties.Alert();
        alertConfig.setEnabled(true);
        alertConfig.setThreshold(0.8);
        
        when(rateLimitProperties.isEnabled()).thenReturn(true);
        when(rateLimitProperties.getEndpointLimit(anyString())).thenReturn(defaultLimit);
        when(rateLimitProperties.getRedis()).thenReturn(redisConfig);
        when(rateLimitProperties.getAlert()).thenReturn(alertConfig);
        when(rateLimitProperties.shouldAlert(anyInt(), anyInt())).thenReturn(false);
    }
    
    @Test
    void testIsAllowed_WhenExceedsLimit_ShouldRecordSecurityEvent() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(Arrays.asList(10L, 10L, 0L)); // current=10, limit=10, allowed=0
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertFalse(result);
        
        // 验证安全事件被记录
        verify(auditLogService, timeout(1000)).logSecurityEvent(
            eq(AuditOperation.SUSPICIOUS_ACTIVITY),
            eq("testuser"),
            contains("访问频率超限")
        );
    }
    
    @Test
    void testIsAllowed_WhenExceedsLimitWithAnonymousUser_ShouldRecordSecurityEvent() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(Arrays.asList(10L, 10L, 0L)); // current=10, limit=10, allowed=0
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", null);
        
        // Then
        assertFalse(result);
        
        // 验证安全事件被记录，匿名用户应该记录为"anonymous"
        verify(auditLogService, timeout(1000)).logSecurityEvent(
            eq(AuditOperation.SUSPICIOUS_ACTIVITY),
            eq("anonymous"),
            contains("访问频率超限")
        );
    }
    
    @Test
    void testIsAllowed_WhenWithinLimit_ShouldNotRecordSecurityEvent() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(Arrays.asList(5L, 10L, 1L)); // current=5, limit=10, allowed=1
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertTrue(result);
        
        // 验证没有记录安全事件
        verify(auditLogService, never()).logSecurityEvent(any(), any(), any());
    }
    
    @Test
    void testIsAllowed_WhenAlertThresholdReached_ShouldRecordAlertEvent() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(Arrays.asList(8L, 10L, 1L)); // current=8, limit=10, allowed=1 (80% threshold)
        when(rateLimitProperties.shouldAlert(8, 10)).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // 没有告警间隔限制
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertTrue(result);
        
        // 验证告警事件被记录
        verify(auditLogService, timeout(1000)).logSecurityEvent(
            eq(AuditOperation.SUSPICIOUS_ACTIVITY),
            anyString(),
            contains("访问频率告警")
        );
    }
    
    @Test
    void testIsAllowed_WhenDisabled_ShouldNotRecordAnyEvent() {
        // Given
        when(rateLimitProperties.isEnabled()).thenReturn(false);
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertTrue(result);
        
        // 验证没有记录任何事件
        verify(auditLogService, never()).logSecurityEvent(any(), any(), any());
        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any());
    }
    
    @Test
    void testIsAllowed_WhenEndpointDisabled_ShouldNotRecordAnyEvent() {
        // Given
        defaultLimit.setEnabled(false);
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertTrue(result);
        
        // 验证没有记录任何事件
        verify(auditLogService, never()).logSecurityEvent(any(), any(), any());
        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any());
    }
    
    @Test
    void testIsAllowed_WhenRedisException_ShouldNotRecordEvent() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertTrue(result); // Should allow access when Redis fails
        
        // 验证没有记录安全事件（因为异常时允许访问）
        verify(auditLogService, never()).logSecurityEvent(any(), any(), any());
    }
}