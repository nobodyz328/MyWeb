package com.myweb.website_core.application.service;

import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import com.myweb.website_core.application.service.security.authentication.EmailVerificationService.VerificationType;
import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.common.security.exception.ValidationException;
import com.myweb.website_core.domain.business.dto.UserRegistrationDTO;
import com.myweb.website_core.domain.business.dto.UserRegistrationResult;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.application.service.security.authentication.PasswordService;
import com.myweb.website_core.application.service.security.authentication.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户注册服务测试类
 * 
 * 测试用户注册服务的各种场景，包括：
 * - 正常注册流程
 * - 用户名和邮箱唯一性验证
 * - 邮箱验证码验证
 * - 密码策略验证
 * - 注册频率限制
 * - 异常处理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordService passwordService;
    
    @Mock
    private EmailVerificationService emailVerificationService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private UserRegistrationService userRegistrationService;
    
    private UserRegistrationDTO validRegistrationDTO;
    
    @BeforeEach
    void setUp() {
        // 创建有效的注册DTO
        validRegistrationDTO = new UserRegistrationDTO();
        validRegistrationDTO.setUsername("testuser");
        validRegistrationDTO.setEmail("test@example.com");
        validRegistrationDTO.setPassword("TestPassword123!");
        validRegistrationDTO.setVerificationCode("123456");
        validRegistrationDTO.setClientIp("192.168.1.1");
        validRegistrationDTO.setUserAgent("Mozilla/5.0");
    }
    
    @Test
    void testRegisterUser_Success() throws Exception {
        // 准备测试数据
        String encodedPassword = "$2a$12$encodedPassword";
        User savedUser = createTestUser();
        
        // 设置模拟行为
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 频率限制检查通过
        when(userRepository.findByUsername("testuser")).thenReturn(null); // 用户名不存在
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty()); // 邮箱不存在
        when(emailVerificationService.verifyCode("test@example.com", "123456", VerificationType.REGISTRATION))
                .thenReturn(true); // 验证码验证通过
        when(passwordService.validateAndEncodePassword(null, "TestPassword123!"))
                .thenReturn(encodedPassword); // 密码验证和加密成功
        when(userRepository.save(any(User.class))).thenReturn(savedUser); // 保存用户成功
        
        // 执行测试
        CompletableFuture<UserRegistrationResult> future = userRegistrationService.registerUser(validRegistrationDTO);
        UserRegistrationResult result = future.get();
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertTrue(result.isEmailVerified());
        assertEquals("用户注册成功", result.getMessage());
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(emailVerificationService).verifyCode("test@example.com", "123456", VerificationType.REGISTRATION);
        verify(passwordService).validateAndEncodePassword(null, "TestPassword123!");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(any(User.class));
        verify(valueOperations, atLeast(2)).set(anyString(), eq("1"), any());
    }
    
    @Test
    void testRegisterUser_UsernameAlreadyExists() throws Exception {
        // 设置模拟行为 - 用户名已存在
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 频率限制检查通过
        when(userRepository.findByUsername("testuser")).thenReturn(new User()); // 用户名已存在
        
        // 执行测试
        CompletableFuture<UserRegistrationResult> future = userRegistrationService.registerUser(validRegistrationDTO);
        UserRegistrationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("用户注册失败", result.getMessage());
        assertTrue(result.getErrorMessage().contains("用户名已存在"));
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(any(User.class));
    }
    
    @Test
    void testRegisterUser_EmailAlreadyExists() throws Exception {
        // 设置模拟行为 - 邮箱已存在
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 频率限制检查通过
        when(userRepository.findByUsername("testuser")).thenReturn(null); // 用户名不存在
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User())); // 邮箱已存在
        
        // 执行测试
        CompletableFuture<UserRegistrationResult> future = userRegistrationService.registerUser(validRegistrationDTO);
        UserRegistrationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("用户注册失败", result.getMessage());
        assertTrue(result.getErrorMessage().contains("邮箱已被注册"));
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(any(User.class));
    }
    
    @Test
    void testRegisterUser_InvalidVerificationCode() throws Exception {
        // 设置模拟行为 - 验证码无效
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 频率限制检查通过
        when(userRepository.findByUsername("testuser")).thenReturn(null); // 用户名不存在
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty()); // 邮箱不存在
        when(emailVerificationService.verifyCode("test@example.com", "123456", VerificationType.REGISTRATION))
                .thenReturn(false); // 验证码验证失败
        
        // 执行测试
        CompletableFuture<UserRegistrationResult> future = userRegistrationService.registerUser(validRegistrationDTO);
        UserRegistrationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("用户注册失败", result.getMessage());
        assertTrue(result.getErrorMessage().contains("验证码无效或已过期"));
        
        // 验证方法调用
        verify(emailVerificationService).verifyCode("test@example.com", "123456", VerificationType.REGISTRATION);
        verify(passwordService, never()).validateAndEncodePassword(any(), any());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(any(User.class));
    }
    
    @Test
    void testRegisterUser_PasswordValidationFailed() throws Exception {
        // 设置模拟行为 - 密码验证失败
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 频率限制检查通过
        when(userRepository.findByUsername("testuser")).thenReturn(null); // 用户名不存在
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty()); // 邮箱不存在
        when(emailVerificationService.verifyCode("test@example.com", "123456", VerificationType.REGISTRATION))
                .thenReturn(true); // 验证码验证通过
        when(passwordService.validateAndEncodePassword(null, "TestPassword123!"))
                .thenThrow(new ValidationException("password", "TestPassword123!", "POLICY_CHECK", "密码不符合策略要求"));
        
        // 执行测试
        CompletableFuture<UserRegistrationResult> future = userRegistrationService.registerUser(validRegistrationDTO);
        UserRegistrationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("用户注册失败", result.getMessage());
        assertTrue(result.getErrorMessage().contains("密码不符合策略要求"));
        
        // 验证方法调用
        verify(passwordService).validateAndEncodePassword(null, "TestPassword123!");
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(any(User.class));
    }
    
    @Test
    void testRegisterUser_RateLimitExceeded() throws Exception {
        // 设置模拟行为 - 超过频率限制
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("registration:rate_limit:10min:192.168.1.1")).thenReturn("3"); // 已达到10分钟限制
        
        // 执行测试
        CompletableFuture<UserRegistrationResult> future = userRegistrationService.registerUser(validRegistrationDTO);
        UserRegistrationResult result = future.get();
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("用户注册失败", result.getMessage());
        assertTrue(result.getErrorMessage().contains("注册过于频繁"));
        
        // 验证方法调用
        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(any(User.class));
    }
    
    @Test
    void testIsUsernameAvailable_Available() {
        // 设置模拟行为
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        
        // 执行测试
        boolean available = userRegistrationService.isUsernameAvailable("newuser");
        
        // 验证结果
        assertTrue(available);
        verify(userRepository).findByUsername("newuser");
    }
    
    @Test
    void testIsUsernameAvailable_NotAvailable() {
        // 设置模拟行为
        when(userRepository.findByUsername("existinguser")).thenReturn(new User());
        
        // 执行测试
        boolean available = userRegistrationService.isUsernameAvailable("existinguser");
        
        // 验证结果
        assertFalse(available);
        verify(userRepository).findByUsername("existinguser");
    }
    
    @Test
    void testIsUsernameAvailable_NullOrEmpty() {
        // 测试null用户名
        assertFalse(userRegistrationService.isUsernameAvailable(null));
        
        // 测试空用户名
        assertFalse(userRegistrationService.isUsernameAvailable(""));
        
        // 测试空白用户名
        assertFalse(userRegistrationService.isUsernameAvailable("   "));
        
        // 验证没有调用数据库
        verify(userRepository, never()).findByUsername(any());
    }
    
    @Test
    void testIsEmailAvailable_Available() {
        // 设置模拟行为
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        
        // 执行测试
        boolean available = userRegistrationService.isEmailAvailable("new@example.com");
        
        // 验证结果
        assertTrue(available);
        verify(userRepository).findByEmail("new@example.com");
    }
    
    @Test
    void testIsEmailAvailable_NotAvailable() {
        // 设置模拟行为
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));
        
        // 执行测试
        boolean available = userRegistrationService.isEmailAvailable("existing@example.com");
        
        // 验证结果
        assertFalse(available);
        verify(userRepository).findByEmail("existing@example.com");
    }
    
    @Test
    void testIsEmailAvailable_NullOrEmpty() {
        // 测试null邮箱
        assertFalse(userRegistrationService.isEmailAvailable(null));
        
        // 测试空邮箱
        assertFalse(userRegistrationService.isEmailAvailable(""));
        
        // 测试空白邮箱
        assertFalse(userRegistrationService.isEmailAvailable("   "));
        
        // 验证没有调用数据库
        verify(userRepository, never()).findByEmail(any());
    }
    
    @Test
    void testGetRegistrationRateLimitStatus() {
        // 设置模拟行为
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("registration:rate_limit:10min:192.168.1.1")).thenReturn("1");
        when(valueOperations.get("registration:rate_limit:daily:192.168.1.1")).thenReturn("5");
        
        // 执行测试
        UserRegistrationService.RegistrationRateLimitStatus status = 
                userRegistrationService.getRegistrationRateLimitStatus("192.168.1.1");
        
        // 验证结果
        assertEquals(2, status.getRemainingMinute()); // 3 - 1 = 2
        assertEquals(5, status.getRemainingDaily()); // 10 - 5 = 5
        assertTrue(status.canRegister());
    }
    
    @Test
    void testGetRegistrationRateLimitStatus_NoUsage() {
        // 设置模拟行为 - 没有使用记录
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // 执行测试
        UserRegistrationService.RegistrationRateLimitStatus status = 
                userRegistrationService.getRegistrationRateLimitStatus("192.168.1.1");
        
        // 验证结果
        assertEquals(3, status.getRemainingMinute());
        assertEquals(10, status.getRemainingDaily());
        assertTrue(status.canRegister());
    }
    
    @Test
    void testGetRegistrationRateLimitStatus_ExceededLimits() {
        // 设置模拟行为 - 超过限制
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("registration:rate_limit:10min:192.168.1.1")).thenReturn("3");
        when(valueOperations.get("registration:rate_limit:daily:192.168.1.1")).thenReturn("10");
        
        // 执行测试
        UserRegistrationService.RegistrationRateLimitStatus status = 
                userRegistrationService.getRegistrationRateLimitStatus("192.168.1.1");
        
        // 验证结果
        assertEquals(0, status.getRemainingMinute());
        assertEquals(0, status.getRemainingDaily());
        assertFalse(status.canRegister());
    }
    
    @Test
    void testGetRegistrationRateLimitStatus_NullIp() {
        // 执行测试
        UserRegistrationService.RegistrationRateLimitStatus status = 
                userRegistrationService.getRegistrationRateLimitStatus(null);
        
        // 验证结果
        assertEquals(3, status.getRemainingMinute());
        assertEquals(10, status.getRemainingDaily());
        assertTrue(status.canRegister());
        
        // 验证没有调用Redis
        verify(valueOperations, never()).get(anyString());
    }
    
    /**
     * 创建测试用户
     */
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$12$encodedPassword");
        user.setEmailVerified(true);
        return user;
    }
}