package com.myweb.website_core.application.service;

import com.myweb.website_core.application.service.integration.CaptchaService;
import com.myweb.website_core.application.service.security.JwtService;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.domain.security.dto.AuthenticationResult;
import com.myweb.website_core.domain.business.dto.LoginRequest;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import com.myweb.website_core.application.service.security.authentication.TOTPService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 身份验证服务测试类
 * 
 * 测试AuthenticationService的各种登录场景，包括：
 * - 正常登录
 * - 密码错误
 * - 账户锁定
 * - 验证码验证
 * - TOTP二次验证
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private EmailVerificationService emailVerificationService;
    
    @Mock
    private TOTPService totpService;
    
    @Mock
    private CaptchaService captchaService;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private AuthenticationService authenticationService;
    
    private User testUser;
    private LoginRequest loginRequest;
    private final String clientIp = "192.168.1.100";
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setRole(UserRole.USER);
        testUser.setLoginAttempts(0);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        
        // 创建登录请求
        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
    }
    
    @Test
    void testSuccessfulLogin() throws Exception {
        // 准备测试数据
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(testUser, result.getUser());
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(3600L, result.getExpiresIn());
        
        // 验证用户状态更新
        verify(userRepository).save(testUser);
        assertEquals(0, testUser.getLoginAttempts());
        assertNotNull(testUser.getLastLoginTime());
        assertEquals(clientIp, testUser.getLastLoginIp());
        
        // 验证审计日志记录
        verify(auditLogService).logUserLogin(eq(testUser.getId()), eq("testuser"), eq(clientIp), isNull(), eq(true), isNull());
    }
    
    @Test
    void testLoginWithUserNotFound() throws Exception {
        // 准备测试数据
        when(userRepository.findByUsername("testuser")).thenReturn(null);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("用户名或密码错误", result.getFailureReason());
        
        // 验证审计日志记录
        verify(auditLogService).logUserLogin(isNull(), eq("testuser"), eq(clientIp), isNull(), eq(false), eq("用户不存在"));
    }
    
    @Test
    void testLoginWithWrongPassword() throws Exception {
        // 准备测试数据
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(false);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("用户名或密码错误", result.getFailureReason());
        
        // 验证用户登录失败次数增加
        verify(userRepository).save(testUser);
        assertEquals(1, testUser.getLoginAttempts());
        
        // 验证审计日志记录
        verify(auditLogService).logUserLogin(eq(testUser.getId()), eq("testuser"), eq(clientIp), isNull(), eq(false), eq("密码错误"));
    }
    
    @Test
    void testLoginWithAccountLocked() throws Exception {
        // 准备测试数据 - 设置账户锁定
        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("账户已锁定", result.getFailureReason());
        assertTrue(result.isAccountLocked());
        assertNotNull(result.getAccountLockedUntil());
        
        // 验证审计日志记录
        verify(auditLogService).logUserLogin(eq(testUser.getId()), eq("testuser"), eq(clientIp), isNull(), eq(false), eq("账户已锁定"));
    }
    
    @Test
    void testLoginRequiresCaptcha() throws Exception {
        // 准备测试数据 - 设置登录失败次数达到验证码阈值
        testUser.setLoginAttempts(3);
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("需要验证码", result.getFailureReason());
        assertTrue(result.needsCaptcha());
        assertEquals(2, result.getRemainingAttempts()); // 5 - 3 = 2
        
        // 验证审计日志记录
        verify(auditLogService).logUserLogin(eq(testUser.getId()), eq("testuser"), eq(clientIp), isNull(), eq(false), eq("需要验证码"));
    }
    
    @Test
    void testLoginWithValidCaptcha() throws Exception {
        // 准备测试数据
        testUser.setLoginAttempts(3);
        loginRequest.setCaptcha("session123:1234");
        
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(captchaService.validateCaptcha("session123:1234")).thenReturn(true);
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("access-token", result.getAccessToken());
        
        // 验证验证码验证被调用
        verify(captchaService).validateCaptcha("session123:1234");
        
        // 验证用户登录失败次数被重置
        assertEquals(0, testUser.getLoginAttempts());
    }
    
    @Test
    void testAdminLoginWithTOTP() throws Exception {
        // 准备测试数据 - 管理员用户
        testUser.setRole(UserRole.ADMIN);
        testUser.setTotpEnabled(true);
        testUser.setTotpSecret("JBSWY3DPEHPK3PXP");
        
        loginRequest.setTotpCode("123456");
        
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(totpService.validateTOTP("JBSWY3DPEHPK3PXP", "123456", "testuser")).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("access-token", result.getAccessToken());
        
        // 验证TOTP验证被调用
        verify(totpService).validateTOTP("JBSWY3DPEHPK3PXP", "123456", "testuser");
    }
    
    @Test
    void testAdminLoginWithInvalidTOTP() throws Exception {
        // 准备测试数据 - 管理员用户
        testUser.setRole(UserRole.ADMIN);
        testUser.setTotpEnabled(true);
        testUser.setTotpSecret("JBSWY3DPEHPK3PXP");
        
        loginRequest.setTotpCode("123456");
        
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(totpService.validateTOTP("JBSWY3DPEHPK3PXP", "123456", "testuser")).thenReturn(false);
        
        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("动态口令验证失败", result.getFailureReason());
        
        // 验证审计日志记录
        verify(auditLogService).logUserLogin(eq(testUser.getId()), eq("testuser"), eq(clientIp), isNull(), eq(false), eq("TOTP验证失败"));
    }
    
    @Test
    void testAccountLockAfterMaxAttempts() throws Exception {
        // 准备测试数据 - 设置登录失败次数为4（再失败一次就锁定）
        testUser.setLoginAttempts(4);
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(false);

        // 执行测试
        CompletableFuture<AuthenticationResult> future = authenticationService.authenticateUser(loginRequest, clientIp);
        AuthenticationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("账户已锁定", result.getFailureReason());
        assertTrue(result.isAccountLocked());
        
        // 验证用户状态
        assertEquals(5, testUser.getLoginAttempts());
        assertNotNull(testUser.getAccountLockedUntil());
        
        // 验证审计日志记录
        verify(auditLogService).logUserLogin(eq(testUser.getId()), eq("testuser"), eq(clientIp), isNull(), eq(false), eq("密码错误，账户已锁定"));
    }
    
    @Test
    void testUnlockUserAccount() {
        // 准备测试数据
        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        testUser.setLoginAttempts(5);
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        // 执行测试
        boolean result = authenticationService.unlockUserAccount("testuser", "admin");
        
        // 验证结果
        assertTrue(result);
        assertNull(testUser.getAccountLockedUntil());
        assertEquals(0, testUser.getLoginAttempts());
        
        // 验证用户保存
        verify(userRepository).save(testUser);
        
        // 验证审计日志记录
        verify(auditLogService).logLoginAttempt("testuser", null, "ACCOUNT_UNLOCKED_BY_ADMIN");
    }
    
    @Test
    void testGetUserLoginStatus() {
        // 测试正常状态
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        String status = authenticationService.getUserLoginStatus("testuser");
        assertEquals("正常状态", status);
        
        // 测试需要验证码状态
        testUser.setLoginAttempts(3);
        status = authenticationService.getUserLoginStatus("testuser");
        assertEquals("需要验证码，失败次数: 3", status);
        
        // 测试账户锁定状态
        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        status = authenticationService.getUserLoginStatus("testuser");
        assertTrue(status.startsWith("账户已锁定，解锁时间:"));
    }
    
    @Test
    void testResetLoginAttempts() {
        // 准备测试数据
        testUser.setLoginAttempts(3);
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        // 执行测试
        boolean result = authenticationService.resetLoginAttempts("testuser", "admin");
        
        // 验证结果
        assertTrue(result);
        assertEquals(0, testUser.getLoginAttempts());
        
        // 验证用户保存
        verify(userRepository).save(testUser);
        
        // 验证审计日志记录
        verify(auditLogService).logLoginAttempt("testuser", null, "LOGIN_ATTEMPTS_RESET_BY_ADMIN");
    }
}