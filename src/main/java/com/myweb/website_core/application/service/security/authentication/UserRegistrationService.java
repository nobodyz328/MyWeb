package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.application.service.security.authentication.EmailVerificationService.VerificationType;
import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.common.exception.RateLimitExceededException;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.business.dto.UserRegistrationDTO;
import com.myweb.website_core.domain.business.dto.UserRegistrationResult;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 用户注册服务
 * 
 * 提供完整的用户注册功能，包括：
 * - 用户名和邮箱唯一性验证
 * - 密码加密和验证
 * - 邮箱验证码验证
 * - 注册频率限制
 * - 欢迎邮件发送
 * - 审计日志记录
 * 
 * 符合GB/T 22239-2019 7.1.4.1 身份鉴别要求
 * 需求: 1.1, 1.2, 1.5
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {
    
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    
    // 使用统一的Redis键管理 - 已在RedisKey中定义
    
    // 注册频率限制配置
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_REGISTRATIONS_PER_IP = 5; // 每10分钟最多3次注册
    private static final Duration DAILY_LIMIT_WINDOW = Duration.ofHours(24);
    private static final int MAX_REGISTRATIONS_PER_IP_DAILY = 10; // 每天最多10次注册
    
    /**
     * 用户注册主方法
     * 
     * @param registrationDTO 注册请求数据
     * @return 注册结果
     */
    @Async
    @Transactional
    public CompletableFuture<UserRegistrationResult> registerUser(UserRegistrationDTO registrationDTO) {
        log.info("开始用户注册流程: username={}, email={}, clientIp={}", 
                registrationDTO.getUsername(), registrationDTO.getEmail(), registrationDTO.getClientIp());
        
        try {
            // 1. 检查注册频率限制
            checkRegistrationRateLimit(registrationDTO.getClientIp());
            
            // 2. 验证用户名和邮箱唯一性
            validateUserUniqueness(registrationDTO.getUsername(), registrationDTO.getEmail());
            
            // 3. 验证邮箱验证码
            validateEmailVerificationCode(registrationDTO.getEmail(), registrationDTO.getVerificationCode());
            
            // 4. 验证并加密密码
            String encodedPassword = passwordService.validateAndEncodePassword(null, registrationDTO.getPassword());
            
            // 5. 创建用户实体
            User user = createUserEntity(registrationDTO, encodedPassword);
            
            // 6. 保存用户到数据库
            User savedUser = userRepository.save(user);
            log.info("用户保存成功: userId={}, username={}", savedUser.getId(), savedUser.getUsername());
            
            // 7. 发送欢迎邮件（异步）
            sendWelcomeEmailAsync(savedUser);
            
            // 8. 记录注册频率限制计数器
            recordRegistrationAttempt(registrationDTO.getClientIp());
            
            // 9. 创建成功结果
            UserRegistrationResult result = UserRegistrationResult.success(savedUser);
            log.info("用户注册成功: userId={}, username={}, email={}", 
                    savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("用户注册失败: username={}, email={}, error={}", 
                    registrationDTO.getUsername(), registrationDTO.getEmail(), e.getMessage(), e);
            
            UserRegistrationResult result = UserRegistrationResult.failure(e.getMessage());
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * 验证用户名和邮箱的唯一性
     * 
     * @param username 用户名
     * @param email 邮箱地址
     * @throws ValidationException 如果用户名或邮箱已存在
     */
    private void validateUserUniqueness(String username, String email) {
        log.debug("验证用户名和邮箱唯一性: username={}, email={}", username, email);
        
        // 检查用户名是否已存在
        User existingUserByUsername = userRepository.findByUsername(username);
        if (existingUserByUsername != null) {
            log.warn("用户名已存在: {}", username);
            throw new ValidationException("username", username, "UNIQUENESS_CHECK", "用户名已存在，请选择其他用户名");
        }
        
        // 检查邮箱是否已存在
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            log.warn("邮箱已存在: {}", email);
            throw new ValidationException("email", email, "UNIQUENESS_CHECK", "邮箱已被注册，请使用其他邮箱");
        });
        
        log.debug("用户名和邮箱唯一性验证通过");
    }
    
    /**
     * 验证邮箱验证码
     * 
     * @param email 邮箱地址
     * @param verificationCode 验证码
     * @throws ValidationException 如果验证码无效
     */
    private void validateEmailVerificationCode(String email, String verificationCode) {
        log.debug("验证邮箱验证码: email={}", email);
        
        boolean isValid = emailVerificationService.verifyCode(email, verificationCode, VerificationType.REGISTRATION);
        if (!isValid) {
            log.warn("邮箱验证码验证失败: email={}", email);
            throw new ValidationException("verificationCode", verificationCode, "VERIFICATION_FAILED", 
                    "验证码无效或已过期，请重新获取验证码");
        }
        
        log.debug("邮箱验证码验证通过: email={}", email);
    }
    
    /**
     * 创建用户实体
     * 
     * @param registrationDTO 注册数据
     * @param encodedPassword 加密后的密码
     * @return 用户实体
     */
    private User createUserEntity(UserRegistrationDTO registrationDTO, String encodedPassword) {
        log.debug("创建用户实体: username={}", registrationDTO.getUsername());
        
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        
        // 设置密码相关字段
        user.setPasswordHash(encodedPassword); // 新的安全字段
        
        // 设置邮箱验证状态为已验证（因为已通过验证码验证）
        user.setEmailVerified(true);
        
        // 设置默认用户角色
        user.setRole(UserRole.USER);
        
        // 设置默认头像和简介
        user.setAvatarUrl("https://static.hdslb.com/images/member/noface.gif");
        user.setBio("这个人很懒，什么都没有留下");
        
        // 初始化统计数据
        user.setLikedCount(0);
        
        // 初始化安全字段
        user.setLoginAttempts(0);
        user.setTotpEnabled(false);
        
        // 设置时间戳（JPA回调会自动设置，这里显式设置确保一致性）
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        log.debug("用户实体创建完成: username={}", user.getUsername());
        return user;
    }
    
    /**
     * 检查注册频率限制
     * 
     * @param clientIp 客户端IP地址
     * @throws RateLimitExceededException 如果超过频率限制
     */
    private void checkRegistrationRateLimit(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            log.warn("客户端IP为空，跳过频率限制检查");
            return;
        }
        
        log.debug("检查注册频率限制: clientIp={}", clientIp);
        
        // 检查10分钟内的注册次数
        String minuteKey = RedisKey.registrationRateLimitKey(clientIp) + ":10min";
        String minuteCount = redisTemplate.opsForValue().get(minuteKey);
        
        if (minuteCount != null && Integer.parseInt(minuteCount) >= MAX_REGISTRATIONS_PER_IP) {
            log.warn("注册频率超限(10分钟): clientIp={}, count={}", clientIp, minuteCount);
            throw new RateLimitExceededException("注册过于频繁，请10分钟后再试");
        }
        
        // 检查24小时内的注册次数
        String dailyKey = RedisKey.registrationRateLimitKey(clientIp) + ":daily";
        String dailyCount = redisTemplate.opsForValue().get(dailyKey);
        
        if (dailyCount != null && Integer.parseInt(dailyCount) >= MAX_REGISTRATIONS_PER_IP_DAILY) {
            log.warn("注册频率超限(每日): clientIp={}, count={}", clientIp, dailyCount);
            throw new RateLimitExceededException("今日注册次数已达上限，请明日再试");
        }
        
        log.debug("注册频率限制检查通过: clientIp={}", clientIp);
    }
    
    /**
     * 记录注册尝试次数
     * 
     * @param clientIp 客户端IP地址
     */
    private void recordRegistrationAttempt(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return;
        }
        
        log.debug("记录注册尝试次数: clientIp={}", clientIp);
        
        // 更新10分钟计数器
        String minuteKey = RedisKey.registrationRateLimitKey(clientIp) + ":10min";
        String minuteCount = redisTemplate.opsForValue().get(minuteKey);
        
        if (minuteCount == null) {
            redisTemplate.opsForValue().set(minuteKey, "1", RATE_LIMIT_WINDOW);
        } else {
            redisTemplate.opsForValue().increment(minuteKey);
        }
        
        // 更新每日计数器
        String dailyKey = RedisKey.registrationRateLimitKey(clientIp) + ":daily";
        String dailyCount = redisTemplate.opsForValue().get(dailyKey);
        
        if (dailyCount == null) {
            redisTemplate.opsForValue().set(dailyKey, "1", DAILY_LIMIT_WINDOW);
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
        }
        
        log.debug("注册尝试次数记录完成: clientIp={}", clientIp);
    }
    
    /**
     * 异步发送欢迎邮件
     * 
     * @param user 注册成功的用户
     */
    @Async
    public void sendWelcomeEmailAsync(User user) {
        try {
            log.info("开始发送欢迎邮件: userId={}, email={}", user.getId(), user.getEmail());
            emailService.sendWelcomeEmail(user);
            log.info("欢迎邮件发送请求已提交: userId={}, email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("欢迎邮件发送失败: userId={}, email={}, error={}", 
                    user.getId(), user.getEmail(), e.getMessage(), e);
            // 欢迎邮件发送失败不应该影响注册流程，只记录日志
        }
    }
    
    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return 是否可用
     */
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        User existingUser = userRepository.findByUsername(username.trim());
        boolean available = existingUser == null;
        
        log.debug("用户名可用性检查: username={}, available={}", username, available);
        return available;
    }
    
    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱地址
     * @return 是否可用
     */
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        boolean available = userRepository.findByEmail(email.trim()).isEmpty();
        
        log.debug("邮箱可用性检查: email={}, available={}", email, available);
        return available;
    }
    
    /**
     * 获取注册频率限制状态
     * 
     * @param clientIp 客户端IP地址
     * @return 剩余可注册次数信息
     */
    public RegistrationRateLimitStatus getRegistrationRateLimitStatus(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return new RegistrationRateLimitStatus(MAX_REGISTRATIONS_PER_IP, MAX_REGISTRATIONS_PER_IP_DAILY);
        }
        
        // 获取10分钟内的注册次数
        String minuteKey = RedisKey.registrationRateLimitKey(clientIp) + ":10min";
        String minuteCount = redisTemplate.opsForValue().get(minuteKey);
        int usedMinute = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
        
        // 获取每日注册次数
        String dailyKey = RedisKey.registrationRateLimitKey(clientIp) + ":daily";
        String dailyCount = redisTemplate.opsForValue().get(dailyKey);
        int usedDaily = dailyCount != null ? Integer.parseInt(dailyCount) : 0;
        
        return new RegistrationRateLimitStatus(
                Math.max(0, MAX_REGISTRATIONS_PER_IP - usedMinute),
                Math.max(0, MAX_REGISTRATIONS_PER_IP_DAILY - usedDaily)
        );
    }
    
    /**
     * 注册频率限制状态
     */
    public static class RegistrationRateLimitStatus {
        private final int remainingMinute;
        private final int remainingDaily;
        
        public RegistrationRateLimitStatus(int remainingMinute, int remainingDaily) {
            this.remainingMinute = remainingMinute;
            this.remainingDaily = remainingDaily;
        }
        
        public int getRemainingMinute() {
            return remainingMinute;
        }
        
        public int getRemainingDaily() {
            return remainingDaily;
        }
        
        public boolean canRegister() {
            return remainingMinute > 0 && remainingDaily > 0;
        }
    }
}