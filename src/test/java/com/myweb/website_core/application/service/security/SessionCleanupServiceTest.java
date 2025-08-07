package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.application.service.security.authentication.SessionCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 会话清理服务测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@ExtendWith(MockitoExtension.class)
class SessionCleanupServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private AuditLogServiceAdapter auditLogService;
    
    @InjectMocks
    private SessionCleanupService sessionCleanupService;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testCleanupUserSession_Success() throws Exception {
        // 准备测试数据
        Long userId = 1L;
        String sessionId = "test-session-id";
        
        Set<String> sessionKeys = Set.of(
            "spring:session:sessions:session1",
            "spring:session:sessions:session2"
        );
        Set<String> authTokenKeys = Set.of("auth:token:1:token1");
        Set<String> csrfTokenKeys = Set.of("csrf:token:1:token1");
        Set<String> tempDataKeys = Set.of("temp:user:1:data1");
        Set<String> userCacheKeys = Set.of("user:profile:1");
        
        // 设置模拟行为
        when(redisTemplate.hasKey("spring:session:sessions:" + sessionId)).thenReturn(true);
        when(redisTemplate.keys("spring:session:sessions:*")).thenReturn(sessionKeys);
        when(redisTemplate.keys("auth:token:1:*")).thenReturn(authTokenKeys);
        when(redisTemplate.keys("csrf:token:1:*")).thenReturn(csrfTokenKeys);
        when(redisTemplate.keys("temp:user:1:*")).thenReturn(tempDataKeys);
        when(redisTemplate.keys("user:profile:1")).thenReturn(userCacheKeys);
        
        when(valueOperations.get("spring:session:sessions:session1")).thenReturn("user:1:data");
        when(valueOperations.get("spring:session:sessions:session2")).thenReturn("user:2:data");
        
        // 执行测试
        CompletableFuture<SessionCleanupService.CleanupStatistics> future = 
            sessionCleanupService.cleanupUserSession(userId, sessionId);
        
        SessionCleanupService.CleanupStatistics statistics = future.get();
        
        // 验证结果
        assertNotNull(statistics);
        assertTrue(statistics.getTotalCleared() > 0);
        
        // 验证方法调用
        verify(redisTemplate).delete("spring:session:sessions:" + sessionId);
        verify(redisTemplate).delete(authTokenKeys);
        verify(redisTemplate).delete(csrfTokenKeys);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testCleanupUserSession_WithoutSessionId() throws Exception {
        // 准备测试数据
        Long userId = 1L;
        String sessionId = null;
        
        Set<String> sessionKeys = Set.of("spring:session:sessions:session1");
        when(redisTemplate.keys("spring:session:sessions:*")).thenReturn(sessionKeys);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        when(valueOperations.get("spring:session:sessions:session1")).thenReturn("user:1:data");
        
        // 执行测试
        CompletableFuture<SessionCleanupService.CleanupStatistics> future = 
            sessionCleanupService.cleanupUserSession(userId, sessionId);
        
        SessionCleanupService.CleanupStatistics statistics = future.get();
        
        // 验证结果
        assertNotNull(statistics);
        
        // 验证没有删除指定会话（因为sessionId为null）
        verify(redisTemplate, never()).delete("spring:session:sessions:null");
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testCleanupExpiredSessions() {
        // 准备测试数据
        Set<String> sessionKeys = Set.of(
            "spring:session:sessions:expired1",
            "spring:session:sessions:active1"
        );
        Set<String> authTokenKeys = Set.of("auth:token:expired1");
        Set<String> csrfTokenKeys = Set.of("csrf:token:expired1");
        Set<String> tempKeys = Set.of("temp:expired1");
        
        when(redisTemplate.keys("spring:session:sessions:*")).thenReturn(sessionKeys);
        when(redisTemplate.keys("auth:token:*")).thenReturn(authTokenKeys);
        when(redisTemplate.keys("csrf:token:*")).thenReturn(csrfTokenKeys);
        when(redisTemplate.keys("temp:*")).thenReturn(tempKeys);
        
        // 设置过期时间
        when(redisTemplate.getExpire("spring:session:sessions:expired1", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.getExpire("spring:session:sessions:active1", TimeUnit.SECONDS)).thenReturn(3600L);
        when(redisTemplate.getExpire("auth:token:expired1", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.getExpire("csrf:token:expired1", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.getExpire("temp:expired1", TimeUnit.SECONDS)).thenReturn(-1L);
        
        // 执行测试
        sessionCleanupService.cleanupExpiredSessions();
        
        // 验证方法调用
        verify(redisTemplate).delete("spring:session:sessions:expired1");
        verify(redisTemplate).delete("auth:token:expired1");
        verify(redisTemplate).delete("csrf:token:expired1");
        verify(redisTemplate).delete("temp:expired1");
        
        // 验证没有删除活跃会话
        verify(redisTemplate, never()).delete("spring:session:sessions:active1");
    }
    
    @Test
    void testCleanupOnSystemRestart() {
        // 准备测试数据
        Set<String> tempKeys = Set.of("temp:data1", "temp:data2");
        Set<String> sessionKeys = Set.of("spring:session:sessions:session1");
        
        when(redisTemplate.keys("temp:*")).thenReturn(tempKeys);
        when(redisTemplate.keys("spring:session:sessions:*")).thenReturn(sessionKeys);
        when(redisTemplate.getExpire("spring:session:sessions:session1", TimeUnit.SECONDS)).thenReturn(-1L);
        
        // 执行测试
        sessionCleanupService.cleanupOnSystemRestart();
        
        // 验证方法调用
        verify(redisTemplate).delete(tempKeys);
        verify(redisTemplate).delete("spring:session:sessions:session1");
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testForceCleanupUserSessions() {
        // 准备测试数据
        Long userId = 1L;
        Long operatorUserId = 2L;
        
        Set<String> sessionKeys = Set.of("spring:session:sessions:session1");
        Set<String> authTokenKeys = Set.of("auth:token:1:token1");
        
        when(redisTemplate.keys("spring:session:sessions:*")).thenReturn(sessionKeys);
        when(redisTemplate.keys("auth:token:1:*")).thenReturn(authTokenKeys);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        when(valueOperations.get("spring:session:sessions:session1")).thenReturn("user:1:data");
        
        // 执行测试
        SessionCleanupService.CleanupStatistics statistics = 
            sessionCleanupService.forceCleanupUserSessions(userId, operatorUserId);
        
        // 验证结果
        assertNotNull(statistics);
        assertTrue(statistics.getTotalCleared() > 0);
        
        // 验证方法调用
        verify(redisTemplate).delete(authTokenKeys);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testGetSessionStatistics() {
        // 准备测试数据
        Set<String> sessionKeys = Set.of("session1", "session2");
        Set<String> authTokenKeys = Set.of("token1");
        Set<String> csrfTokenKeys = Set.of("csrf1");
        Set<String> tempKeys = Set.of("temp1", "temp2", "temp3");
        
        when(redisTemplate.keys("spring:session:sessions:*")).thenReturn(sessionKeys);
        when(redisTemplate.keys("auth:token:*")).thenReturn(authTokenKeys);
        when(redisTemplate.keys("csrf:token:*")).thenReturn(csrfTokenKeys);
        when(redisTemplate.keys("temp:*")).thenReturn(tempKeys);
        
        // 执行测试
        Map<String, Object> statistics = sessionCleanupService.getSessionStatistics();
        
        // 验证结果
        assertNotNull(statistics);
        assertEquals(2, statistics.get("activeSessions"));
        assertEquals(1, statistics.get("authTokens"));
        assertEquals(1, statistics.get("csrfTokens"));
        assertEquals(3, statistics.get("tempData"));
        assertNotNull(statistics.get("timestamp"));
    }
    
    @Test
    void testGetSessionStatistics_WithException() {
        // 设置异常情况
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis error"));
        
        // 执行测试
        Map<String, Object> statistics = sessionCleanupService.getSessionStatistics();
        
        // 验证结果
        assertNotNull(statistics);
        assertTrue(statistics.isEmpty());
    }
    
    @Test
    void testCleanupStatistics() {
        // 创建统计对象
        SessionCleanupService.CleanupStatistics statistics = new SessionCleanupService.CleanupStatistics();
        
        // 测试初始值
        assertEquals(0, statistics.getClearedSessions());
        assertEquals(0, statistics.getClearedAuthTokens());
        assertEquals(0, statistics.getClearedCsrfTokens());
        assertEquals(0, statistics.getClearedTempData());
        assertEquals(0, statistics.getClearedUserCache());
        assertEquals(0, statistics.getTotalCleared());
        
        // 测试增量方法
        statistics.incrementClearedSessions();
        statistics.incrementClearedAuthTokens();
        statistics.incrementClearedCsrfTokens();
        statistics.incrementClearedTempData();
        statistics.incrementClearedUserCache();
        
        assertEquals(1, statistics.getClearedSessions());
        assertEquals(1, statistics.getClearedAuthTokens());
        assertEquals(1, statistics.getClearedCsrfTokens());
        assertEquals(1, statistics.getClearedTempData());
        assertEquals(1, statistics.getClearedUserCache());
        assertEquals(5, statistics.getTotalCleared());
        
        // 测试设置方法
        statistics.setClearedSessions(5);
        statistics.setClearedAuthTokens(3);
        
        assertEquals(5, statistics.getClearedSessions());
        assertEquals(3, statistics.getClearedAuthTokens());
        assertEquals(12, statistics.getTotalCleared());
        
        // 测试toString方法
        String statisticsString = statistics.toString();
        assertNotNull(statisticsString);
        assertTrue(statisticsString.contains("会话=5"));
        assertTrue(statisticsString.contains("认证令牌=3"));
        assertTrue(statisticsString.contains("总计=12"));
    }
    
    @Test
    void testCleanupUserSession_WithException() throws Exception {
        // 准备测试数据
        Long userId = 1L;
        String sessionId = "test-session-id";
        
        // 设置异常情况
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));
        
        // 执行测试
        CompletableFuture<SessionCleanupService.CleanupStatistics> future = 
            sessionCleanupService.cleanupUserSession(userId, sessionId);
        
        SessionCleanupService.CleanupStatistics statistics = future.get();
        
        // 验证结果
        assertNotNull(statistics);
        assertEquals(0, statistics.getTotalCleared());
        
        // 验证记录了失败审计日志
        verify(auditLogService).logOperation(any());
    }
}