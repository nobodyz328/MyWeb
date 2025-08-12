package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.IPS.ratelimit.RateLimitingService;
import com.myweb.website_core.infrastructure.config.properties.RateLimitProperties;
import com.myweb.website_core.common.util.SecurityEventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 访问频率限制服务测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private RateLimitProperties rateLimitProperties;
    
    @Mock
    private SecurityEventUtils securityEventUtils;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;
    
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
        
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }
    
    @Test
    void testIsAllowed_WhenDisabled_ShouldReturnTrue() {
        // Given
        when(rateLimitProperties.isEnabled()).thenReturn(false);
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", null);
        
        // Then
        assertTrue(result);
        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any());
    }
    
    @Test
    void testIsAllowed_WhenEndpointDisabled_ShouldReturnTrue() {
        // Given
        defaultLimit.setEnabled(false);
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", null);
        
        // Then
        assertTrue(result);
        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any());
    }
    
    @Test
    void testIsAllowed_WhenWithinLimit_ShouldReturnTrue() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(Arrays.asList(5L, 10L, 1L)); // current=5, limit=10, allowed=1
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", null);
        
        // Then
        assertTrue(result);
        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), any());
    }
    
    @Test
    void testIsAllowed_WhenExceedsLimit_ShouldReturnFalse() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(Arrays.asList(10L, 10L, 0L)); // current=10, limit=10, allowed=0
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", null);
        
        // Then
        assertFalse(result);
        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), any());
    }
    
    @Test
    void testIsAllowed_WithUser_ShouldCheckBothIpAndUser() {
        // Given
        defaultLimit.setLimitType("USER");
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(Arrays.asList(5L, 10L, 1L)); // both IP and user within limit
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertTrue(result);
        verify(redisTemplate, times(2)).execute(any(DefaultRedisScript.class), anyList(), any());
    }
    
    @Test
    void testIsAllowed_WhenRedisException_ShouldReturnTrue() {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When
        boolean result = rateLimitingService.isAllowed("192.168.1.1", "/api/test", null);
        
        // Then
        assertTrue(result); // Should allow access when Redis fails
    }
    
    @Test
    void testGetRateLimitStatus_WhenDisabled_ShouldReturnDefaultStatus() {
        // Given
        when(rateLimitProperties.isEnabled()).thenReturn(false);
        
        // When
        RateLimitingService.RateLimitStatus status = 
            rateLimitingService.getRateLimitStatus("192.168.1.1", "/api/test", null);
        
        // Then
        assertTrue(status.isAllowed());
        assertEquals(0, status.getIpCount());
        assertEquals(0, status.getUserCount());
        assertEquals(0, status.getMaxRequests());
    }
    
    @Test
    void testGetRateLimitStatus_WithValidData_ShouldReturnCorrectStatus() {
        // Given
        when(zSetOperations.zCard(anyString())).thenReturn(5L);
        
        // When
        RateLimitingService.RateLimitStatus status = 
            rateLimitingService.getRateLimitStatus("192.168.1.1", "/api/test", null);
        
        // Then
        assertTrue(status.isAllowed());
        assertEquals(5, status.getIpCount());
        assertEquals(0, status.getUserCount());
        assertEquals(10, status.getMaxRequests());
        assertEquals(5, status.getRemainingRequests());
    }
    
    @Test
    void testGetRateLimitStatus_WithUser_ShouldReturnBothCounts() {
        // Given
        defaultLimit.setLimitType("USER");
        when(zSetOperations.zCard(anyString())).thenReturn(3L, 7L); // IP count, then user count
        
        // When
        RateLimitingService.RateLimitStatus status = 
            rateLimitingService.getRateLimitStatus("192.168.1.1", "/api/test", "testuser");
        
        // Then
        assertTrue(status.isAllowed());
        assertEquals(3, status.getIpCount());
        assertEquals(7, status.getUserCount());
        assertEquals(10, status.getMaxRequests());
        assertEquals(3, status.getRemainingRequests()); // max - max(ip, user)
    }
    
    @Test
    void testClearRateLimit_ShouldDeleteRedisKeys() {
        // When
        rateLimitingService.clearRateLimit("192.168.1.1", "/api/test", "testuser");
        
        // Then
        verify(redisTemplate, times(2)).delete(anyString());
    }
    
    @Test
    void testClearRateLimit_WithoutUser_ShouldDeleteOnlyIpKey() {
        // When
        rateLimitingService.clearRateLimit("192.168.1.1", "/api/test", null);
        
        // Then
        verify(redisTemplate, times(1)).delete(anyString());
    }
    
    @Test
    void testRateLimitStatus_GettersAndSetters() {
        // Given
        RateLimitingService.RateLimitStatus status = 
            new RateLimitingService.RateLimitStatus(true, 5, 3, 10);
        
        // Then
        assertTrue(status.isAllowed());
        assertEquals(5, status.getIpCount());
        assertEquals(3, status.getUserCount());
        assertEquals(10, status.getMaxRequests());
        assertEquals(5, status.getRemainingRequests()); // 10 - max(5, 3)
    }
    
    @Test
    void testRateLimitStatus_RemainingRequests_WhenExceedsLimit() {
        // Given
        RateLimitingService.RateLimitStatus status = 
            new RateLimitingService.RateLimitStatus(false, 15, 12, 10);
        
        // Then
        assertFalse(status.isAllowed());
        assertEquals(15, status.getIpCount());
        assertEquals(12, status.getUserCount());
        assertEquals(10, status.getMaxRequests());
        assertEquals(0, status.getRemainingRequests()); // Should not be negative
    }
}