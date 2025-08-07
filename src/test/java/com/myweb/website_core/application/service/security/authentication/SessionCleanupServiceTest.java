package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.dto.SessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * 会话清理服务测试类
 * 
 * 测试会话清理服务的各种功能，包括：
 * - 用户退出时的数据清理
 * - 会话超时的自动清理
 * - Redis缓存数据清理
 * - 审计日志记录
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class SessionCleanupServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private SessionManagementService sessionManagementService;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private SetOperations<String, Object> setOperations;
    
    @InjectMocks
    private SessionCleanupService sessionCleanupService;
    
    private SessionInfo testSessionInfo;
    private final String testSessionId = "test-session-123";
    private final Long testUserId = 1L;
    private final String testUsername = "testuser";
    private final String testIpAddress = "192.168.1.100";
    
    @BeforeEach
    void setUp() {
        // 设置Redis操作模拟
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        // 创建测试会话信息
        testSessionInfo = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUserId)
                .username(testUsername)
                .role(UserRole.USER.name())
                .loginTime(LocalDateTime.now().minusHours(1))
                .lastActivityTime(LocalDateTime.now().minusMinutes(10))
                .ipAddress(testIpAddress)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .active(true)
                .expirationTime(LocalDateTime.now().plusHours(23))
                .deviceType("Desktop")
                .browserType("Chrome")
                .osType("Windows")
                .build();
    }
    
    @Test
    void testPerformUserLogoutCleanup_Success() {
        // Given
        String reason = "USER_LOGOUT";
        
        // When
        CompletableFuture<Boolean> result = sessionCleanupService.performUserLogoutCleanup(
                testSessionId, testUserId, testUsername, testIpAddress, reason);
        
        // Then
        assertTrue(result.join());
        
        // 验证Redis清理操作
        verify(redisTemplate).delete(RedisKey.sessionKey(testSessionId));
        verify(redisTemplate).delete(RedisKey.userActiveSessionKey(testUserId));
        verify(setOperations).remove(RedisKey.ACTIVE_SESSIONS_SET, testSessionId);
        verify(redisTemplate).delete(RedisKey.sessionActivityKey(testSessionId));
        
        // 验证审计日志记录
        ArgumentCaptor<AuditLogRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logOperation(auditCaptor.capture());
        
        AuditLogRequest auditRequest = auditCaptor.getValue();
        assertEquals(AuditOperation.SESSION_CLEANUP, auditRequest.getOperation());
        assertEquals(testUserId, auditRequest.getUserId());
        assertEquals(testUsername, auditRequest.getUsername());
        assertEquals("SESSION", auditRequest.getResourceType());
        assertEquals(testUserId, auditRequest.getResourceId()); // Changed to userId since sessionId is String
        assertEquals(testIpAddress, auditRequest.getIpAddress());
        assertEquals("SUCCESS", auditRequest.getResult());
    }
    
    @Test
    void testPerformTimeoutCleanup_Success() {
        // Given
        lenient().when(sessionManagementService.getSession(testSessionId)).thenReturn(testSessionInfo);
        
        // When
        CompletableFuture<Boolean> result = sessionCleanupService.performTimeoutCleanup(
                testSessionId, testSessionInfo);
        
        // Then
        assertTrue(result.join());
        
        // 验证Redis清理操作
        verify(redisTemplate).delete(RedisKey.sessionKey(testSessionId));
        verify(redisTemplate).delete(RedisKey.userActiveSessionKey(testUserId));
        verify(setOperations).remove(RedisKey.ACTIVE_SESSIONS_SET, testSessionId);
        
        // 验证审计日志记录
        ArgumentCaptor<AuditLogRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logOperation(auditCaptor.capture());
        
        AuditLogRequest auditRequest = auditCaptor.getValue();
        assertEquals(AuditOperation.SESSION_CLEANUP, auditRequest.getOperation());
        assertEquals(testUserId, auditRequest.getUserId());
        assertEquals(testUsername, auditRequest.getUsername());
    }
    
    @Test
    void testPerformBatchExpiredSessionCleanup_WithExpiredSessions() {
        // Given
        Set<Object> sessionIds = Set.of(testSessionId, "expired-session-456");
        when(setOperations.members(RedisKey.ACTIVE_SESSIONS_SET)).thenReturn(sessionIds);
        
        // 第一个会话正常，第二个会话过期
        when(sessionManagementService.getSession(testSessionId)).thenReturn(testSessionInfo);
        
        SessionInfo expiredSession = SessionInfo.builder()
                .sessionId("expired-session-456")
                .userId(2L)
                .username("expireduser")
                .loginTime(LocalDateTime.now().minusHours(25)) // 超过24小时
                .lastActivityTime(LocalDateTime.now().minusHours(2)) // 超过30分钟
                .expirationTime(LocalDateTime.now().minusHours(1)) // 已过期
                .active(true)
                .build();
        when(sessionManagementService.getSession("expired-session-456")).thenReturn(expiredSession);
        
        // When
        CompletableFuture<Integer> result = sessionCleanupService.performBatchExpiredSessionCleanup();
        
        // Then
        assertEquals(1, result.join()); // 只有一个过期会话被清理
        
        // 验证审计日志记录
        verify(auditLogService, atLeastOnce()).logOperation(any(AuditLogRequest.class));
    }
    
    @Test
    void testPerformBatchExpiredSessionCleanup_NoExpiredSessions() {
        // Given
        Set<Object> sessionIds = Set.of(testSessionId);
        when(setOperations.members(RedisKey.ACTIVE_SESSIONS_SET)).thenReturn(sessionIds);
        when(sessionManagementService.getSession(testSessionId)).thenReturn(testSessionInfo);
        
        // When
        CompletableFuture<Integer> result = sessionCleanupService.performBatchExpiredSessionCleanup();
        
        // Then
        assertEquals(0, result.join()); // 没有过期会话
        
        // 验证没有清理操作
        verify(redisTemplate, never()).delete(anyString());
    }
    
    @Test
    void testPerformUserAllSessionsCleanup_Success() {
        // Given
        String reason = "ACCOUNT_DEACTIVATION";
        when(valueOperations.get(RedisKey.userActiveSessionKey(testUserId))).thenReturn(testSessionId);
        when(sessionManagementService.getSession(testSessionId)).thenReturn(testSessionInfo);
        
        // When
        CompletableFuture<Integer> result = sessionCleanupService.performUserAllSessionsCleanup(testUserId, reason);
        
        // Then
        assertEquals(1, result.join());
        
        // 验证清理操作
        verify(redisTemplate).delete(RedisKey.userActiveSessionKey(testUserId));
        verify(redisTemplate, atLeastOnce()).keys(anyString());
    }
    
    @Test
    void testPerformUserLogoutCleanup_WithException() {
        // Given
        String reason = "USER_LOGOUT";
        // Mock Redis operations to throw an exception during cleanup
        doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete(anyString());
        
        // When
        CompletableFuture<Boolean> result = sessionCleanupService.performUserLogoutCleanup(
                testSessionId, testUserId, testUsername, testIpAddress, reason);
        
        // Then
        assertFalse(result.join());
        
        // 验证Redis删除操作被尝试调用
        verify(redisTemplate, atLeastOnce()).delete(anyString());
    }
    
    @Test
    void testCleanupUserCacheData() {
        // Given - 通过反射调用私有方法或通过公共方法间接测试
        String reason = "TEST_CLEANUP";
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("user:like:1:123", "user:collect:1:456"));
        
        // When
        CompletableFuture<Boolean> result = sessionCleanupService.performUserLogoutCleanup(
                testSessionId, testUserId, testUsername, testIpAddress, reason);
        
        // Then
        assertTrue(result.join());
        
        // 验证用户缓存清理
        verify(redisTemplate, atLeastOnce()).delete(anyString());
        verify(redisTemplate, atLeastOnce()).keys(anyString());
    }
    
    @Test
    void testScheduledExpiredSessionCleanup() {
        // Given
        Set<Object> sessionIds = Set.of(testSessionId);
        when(setOperations.members(RedisKey.ACTIVE_SESSIONS_SET)).thenReturn(sessionIds);
        
        // 创建一个过期的会话
        SessionInfo expiredSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUserId)
                .username(testUsername)
                .loginTime(LocalDateTime.now().minusHours(25))
                .lastActivityTime(LocalDateTime.now().minusHours(2))
                .expirationTime(LocalDateTime.now().minusHours(1))
                .active(true)
                .build();
        when(sessionManagementService.getSession(testSessionId)).thenReturn(expiredSession);
        
        // When
        sessionCleanupService.scheduledExpiredSessionCleanup();
        
        // Then
        // 验证清理操作被执行
        verify(redisTemplate, atLeastOnce()).delete(anyString());
        verify(auditLogService, atLeastOnce()).logOperation(any(AuditLogRequest.class));
    }
    
    @Test
    void testScheduledOrphanedDataCleanup() {
        // Given
        Set<String> sessionKeys = Set.of(
                RedisKey.SESSION_PREFIX + "orphaned-session-1",
                RedisKey.SESSION_PREFIX + "orphaned-session-2"
        );
        when(redisTemplate.keys(RedisKey.SESSION_PREFIX + "*")).thenReturn(sessionKeys);
        when(setOperations.isMember(eq(RedisKey.ACTIVE_SESSIONS_SET), anyString())).thenReturn(false);
        
        // When
        sessionCleanupService.scheduledOrphanedDataCleanup();
        
        // Then
        // 验证孤立数据被清理
        verify(redisTemplate, times(2)).delete(anyString());
    }
    
    @Test
    void testCleanupWithNullSessionInfo() {
        // Given
        lenient().when(sessionManagementService.getSession(testSessionId)).thenReturn(null);
        
        // When
        CompletableFuture<Boolean> result = sessionCleanupService.performTimeoutCleanup(
                testSessionId, null);
        
        // Then
        // 应该处理null情况而不抛出异常
        assertFalse(result.join());
    }
    
    @Test
    void testCleanupWithEmptySessionSet() {
        // Given
        when(setOperations.members(RedisKey.ACTIVE_SESSIONS_SET)).thenReturn(Set.of());
        
        // When
        CompletableFuture<Integer> result = sessionCleanupService.performBatchExpiredSessionCleanup();
        
        // Then
        assertEquals(0, result.join());
        
        // 验证没有执行清理操作
        verify(redisTemplate, never()).delete(anyString());
    }
}