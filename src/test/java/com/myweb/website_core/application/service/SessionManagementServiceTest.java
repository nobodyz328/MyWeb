package com.myweb.website_core.application.service;

import com.myweb.website_core.application.service.security.authentication.JWT.JwtService;
import com.myweb.website_core.application.service.security.authentication.SessionManagementService;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.security.dto.SessionInfo;
import com.myweb.website_core.domain.security.dto.SessionStatistics;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * 会话管理服务测试类
 * 
 * 测试会话管理服务的各项功能，包括：
 * - 会话创建和销毁
 * - 会话超时检查
 * - 单用户单会话限制
 * - 会话监控和统计
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class SessionManagementServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private SetOperations<String, Object> setOperations;
    
    @Mock
    private ListOperations<String, Object> listOperations;
    
    @InjectMocks
    private SessionManagementService sessionManagementService;
    
    private User testUser;
    private String testSessionId;
    private String testIpAddress;
    private String testUserAgent;
    private String testAccessToken;
    private String testRefreshToken;
    
    @BeforeEach
    void setUp() {
        // 设置测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        
        testSessionId = "test-session-123";
        testIpAddress = "192.168.1.100";
        testUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        testAccessToken = "test-access-token";
        testRefreshToken = "test-refresh-token";
        
        // 设置Mock行为 - 使用lenient模式避免不必要的stubbing错误
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }
    
    @Test
    void testCreateSession_Success() throws Exception {
        // 准备测试数据
        when(valueOperations.get(RedisKey.userActiveSessionKey(testUser.getId()))).thenReturn(null);
        
        // 执行测试
        CompletableFuture<SessionInfo> future = sessionManagementService.createSession(
                testUser, testSessionId, testIpAddress, testUserAgent, testAccessToken, testRefreshToken);
        
        SessionInfo sessionInfo = future.get();
        
        // 验证结果
        assertNotNull(sessionInfo);
        assertEquals(testSessionId, sessionInfo.getSessionId());
        assertEquals(testUser.getId(), sessionInfo.getUserId());
        assertEquals(testUser.getUsername(), sessionInfo.getUsername());
        assertEquals(testUser.getRole().name(), sessionInfo.getRole());
        assertEquals(testIpAddress, sessionInfo.getIpAddress());
        assertEquals(testUserAgent, sessionInfo.getUserAgent());
        assertEquals(testAccessToken, sessionInfo.getAccessToken());
        assertEquals(testRefreshToken, sessionInfo.getRefreshToken());
        assertTrue(sessionInfo.getActive());
        assertNotNull(sessionInfo.getLoginTime());
        assertNotNull(sessionInfo.getLastActivityTime());
        assertNotNull(sessionInfo.getExpirationTime());
        
        // 验证Redis操作
        verify(valueOperations).set(eq(RedisKey.sessionKey(testSessionId)), any(SessionInfo.class), eq(24L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq(RedisKey.userActiveSessionKey(testUser.getId())), eq(testSessionId), eq(24L), eq(TimeUnit.HOURS));
        verify(setOperations).add(RedisKey.ACTIVE_SESSIONS_SET, testSessionId);
        
        // 验证审计日志
        verify(auditLogService).logUserLogin(testUser, testIpAddress, testUserAgent, "SUCCESS");
    }
    
    @Test
    void testCreateSession_WithExistingSession() throws Exception {
        // 准备测试数据 - 用户已有活跃会话
        String existingSessionId = "existing-session-456";
        when(valueOperations.get(RedisKey.userActiveSessionKey(testUser.getId()))).thenReturn(existingSessionId);
        
        // Mock现有会话信息
        SessionInfo existingSession = SessionInfo.builder()
                .sessionId(existingSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .loginTime(LocalDateTime.now().minusHours(1))
                .lastActivityTime(LocalDateTime.now().minusMinutes(10))
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(existingSessionId))).thenReturn(existingSession);
        
        // 执行测试
        CompletableFuture<SessionInfo> future = sessionManagementService.createSession(
                testUser, testSessionId, testIpAddress, testUserAgent, testAccessToken, testRefreshToken);
        
        SessionInfo sessionInfo = future.get();
        
        // 验证结果
        assertNotNull(sessionInfo);
        assertEquals(testSessionId, sessionInfo.getSessionId());
        
        // 验证旧会话被清理
        verify(redisTemplate).delete(RedisKey.sessionKey(existingSessionId));
        verify(setOperations).remove(RedisKey.ACTIVE_SESSIONS_SET, existingSessionId);
    }
    
    @Test
    void testGetSession_Success() {
        // 准备测试数据
        SessionInfo expectedSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .loginTime(LocalDateTime.now().minusMinutes(30))
                .lastActivityTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusHours(23))
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(expectedSession);
        
        // 执行测试
        SessionInfo actualSession = sessionManagementService.getSession(testSessionId);
        
        // 验证结果
        assertNotNull(actualSession);
        assertEquals(expectedSession.getSessionId(), actualSession.getSessionId());
        assertEquals(expectedSession.getUserId(), actualSession.getUserId());
        assertEquals(expectedSession.getUsername(), actualSession.getUsername());
    }
    
    @Test
    void testGetSession_NotFound() {
        // 准备测试数据
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(null);
        
        // 执行测试
        SessionInfo actualSession = sessionManagementService.getSession(testSessionId);
        
        // 验证结果
        assertNull(actualSession);
    }
    
    @Test
    void testGetSession_Expired() {
        // 准备测试数据 - 过期会话
        SessionInfo expiredSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .loginTime(LocalDateTime.now().minusHours(25)) // 超过24小时
                .lastActivityTime(LocalDateTime.now().minusHours(25))
                .expirationTime(LocalDateTime.now().minusHours(1)) // 已过期
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(expiredSession);
        
        // 执行测试
        SessionInfo actualSession = sessionManagementService.getSession(testSessionId);
        
        // 验证结果
        assertNull(actualSession);
        
        // 验证过期会话被清理
        verify(redisTemplate).delete(RedisKey.sessionKey(testSessionId));
    }
    
    @Test
    void testGetSession_Timeout() {
        // 准备测试数据 - 超时会话
        SessionInfo timeoutSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .loginTime(LocalDateTime.now().minusHours(2))
                .lastActivityTime(LocalDateTime.now().minusMinutes(35)) // 超过30分钟无活动
                .expirationTime(LocalDateTime.now().plusHours(22))
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(timeoutSession);
        
        // 执行测试
        SessionInfo actualSession = sessionManagementService.getSession(testSessionId);
        
        // 验证结果
        assertNull(actualSession);
        
        // 验证超时会话被清理
        verify(redisTemplate).delete(RedisKey.sessionKey(testSessionId));
    }
    
    @Test
    void testUpdateSessionActivity_Success() throws Exception {
        // 准备测试数据
        SessionInfo existingSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .loginTime(LocalDateTime.now().minusMinutes(30))
                .lastActivityTime(LocalDateTime.now().minusMinutes(10))
                .expirationTime(LocalDateTime.now().plusHours(23))
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(existingSession);
        
        // 执行测试
        CompletableFuture<Boolean> future = sessionManagementService.updateSessionActivity(testSessionId, testIpAddress);
        Boolean result = future.get();
        
        // 验证结果
        assertTrue(result);
        
        // 验证会话信息被更新
        verify(valueOperations).set(eq(RedisKey.sessionKey(testSessionId)), any(SessionInfo.class), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void testUpdateSessionActivity_SessionNotFound() throws Exception {
        // 准备测试数据
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(null);
        
        // 执行测试
        CompletableFuture<Boolean> future = sessionManagementService.updateSessionActivity(testSessionId, testIpAddress);
        Boolean result = future.get();
        
        // 验证结果
        assertFalse(result);
    }
    
    @Test
    void testTerminateSession_Success() throws Exception {
        // 准备测试数据
        SessionInfo existingSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .loginTime(LocalDateTime.now().minusMinutes(30))
                .lastActivityTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusHours(23))
                .ipAddress(testIpAddress)
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(existingSession);
        
        // 执行测试
        CompletableFuture<Boolean> future = sessionManagementService.terminateSession(testSessionId, "USER_LOGOUT");
        Boolean result = future.get();
        
        // 验证结果
        assertTrue(result);
        
        // 验证会话数据被清理
        verify(redisTemplate).delete(RedisKey.sessionKey(testSessionId));
        verify(redisTemplate).delete(RedisKey.userActiveSessionKey(testUser.getId()));
        verify(setOperations).remove(RedisKey.ACTIVE_SESSIONS_SET, testSessionId);
        
        // 验证审计日志
        verify(auditLogService).logUserLogout(testUser.getId(), testUser.getUsername(), testIpAddress, "USER_LOGOUT");
    }
    
    @Test
    void testTerminateSession_SessionNotFound() throws Exception {
        // 准备测试数据
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(null);
        
        // 执行测试
        CompletableFuture<Boolean> future = sessionManagementService.terminateSession(testSessionId, "USER_LOGOUT");
        Boolean result = future.get();
        
        // 验证结果
        assertFalse(result);
    }
    
    @Test
    void testUserLogout_Success() throws Exception {
        // 准备测试数据
        SessionInfo existingSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .loginTime(LocalDateTime.now().minusMinutes(30))
                .lastActivityTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusHours(23))
                .ipAddress(testIpAddress)
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(existingSession);
        
        // 执行测试
        CompletableFuture<Boolean> future = sessionManagementService.userLogout(testSessionId, testIpAddress);
        Boolean result = future.get();
        
        // 验证结果
        assertTrue(result);
        
        // 验证会话被终止
        verify(redisTemplate).delete(RedisKey.sessionKey(testSessionId));
    }
    
    @Test
    void testGetUserActiveSession_Success() {
        // 准备测试数据
        when(valueOperations.get(RedisKey.userActiveSessionKey(testUser.getId()))).thenReturn(testSessionId);
        
        SessionInfo expectedSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .active(true)
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(expectedSession);
        
        // 执行测试
        SessionInfo actualSession = sessionManagementService.getUserActiveSession(testUser.getId());
        
        // 验证结果
        assertNotNull(actualSession);
        assertEquals(testSessionId, actualSession.getSessionId());
        assertEquals(testUser.getId(), actualSession.getUserId());
    }
    
    @Test
    void testGetUserActiveSession_NotFound() {
        // 准备测试数据
        when(valueOperations.get(RedisKey.userActiveSessionKey(testUser.getId()))).thenReturn(null);
        
        // 执行测试
        SessionInfo actualSession = sessionManagementService.getUserActiveSession(testUser.getId());
        
        // 验证结果
        assertNull(actualSession);
    }
    
    @Test
    void testGetSessionStatistics_Success() {
        // 准备测试数据
        when(valueOperations.get(RedisKey.sessionStatsKey("current"))).thenReturn(null);
        when(setOperations.members(RedisKey.ACTIVE_SESSIONS_SET)).thenReturn(Set.of(testSessionId));
        
        SessionInfo activeSession = SessionInfo.builder()
                .sessionId(testSessionId)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .role(UserRole.USER.name())
                .active(true)
                .loginTime(LocalDateTime.now().minusMinutes(30))
                .lastActivityTime(LocalDateTime.now().minusMinutes(5))
                .deviceType("Desktop")
                .browserType("Chrome")
                .osType("Windows")
                .ipAddress(testIpAddress)
                .build();
        
        when(valueOperations.get(RedisKey.sessionKey(testSessionId))).thenReturn(activeSession);
        
        // 执行测试
        SessionStatistics statistics = sessionManagementService.getSessionStatistics();
        
        // 验证结果
        assertNotNull(statistics);
        assertEquals(1L, statistics.getTotalOnlineUsers());
        assertEquals(1L, statistics.getTotalActiveSessions());
        assertNotNull(statistics.getUsersByRole());
        assertNotNull(statistics.getSessionsByDevice());
        assertNotNull(statistics.getSessionsByBrowser());
        assertNotNull(statistics.getSessionsByOS());
        assertNotNull(statistics.getRecentActiveIPs());
        assertNotNull(statistics.getGeneratedAt());
        
        // 验证统计数据被缓存
        verify(valueOperations).set(eq(RedisKey.sessionStatsKey("current")), any(SessionStatistics.class), eq(5L), eq(TimeUnit.MINUTES));
    }
    
    @Test
    void testGetSessionStatistics_FromCache() {
        // 准备测试数据 - 缓存中的统计数据
        SessionStatistics cachedStats = SessionStatistics.builder()
                .totalOnlineUsers(5L)
                .totalActiveSessions(5L)
                .generatedAt(LocalDateTime.now().minusMinutes(2))
                .validityMinutes(5)
                .build();
        
        when(valueOperations.get(RedisKey.sessionStatsKey("current"))).thenReturn(cachedStats);
        
        // 执行测试
        SessionStatistics statistics = sessionManagementService.getSessionStatistics();
        
        // 验证结果
        assertNotNull(statistics);
        assertEquals(5L, statistics.getTotalOnlineUsers());
        assertEquals(5L, statistics.getTotalActiveSessions());
        
        // 验证没有重新计算统计数据
        verify(setOperations, never()).members(RedisKey.ACTIVE_SESSIONS_SET);
    }
}