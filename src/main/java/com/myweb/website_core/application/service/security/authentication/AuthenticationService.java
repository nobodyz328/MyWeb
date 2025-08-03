package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.integration.CaptchaService;
import com.myweb.website_core.application.service.security.JwtService;
import com.myweb.website_core.domain.security.dto.AuthenticationResult;
import com.myweb.website_core.domain.business.dto.LoginRequest;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 身份验证服务
 * 
 * 提供用户身份验证功能，包括：
 * - 用户登录验证
 * - 登录失败次数统计和账户锁定
 * - 验证码验证（连续失败3次后）
 * - 管理员TOTP二次验证
 * - 登录审计日志记录
 * 
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final TOTPService totpService;
    private final CaptchaService captchaService;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    
    /**
     * 登录失败锁定阈值
     */
    private static final int LOCK_THRESHOLD = 5;
    
    /**
     * 验证码要求阈值
     */
    private static final int CAPTCHA_THRESHOLD = 3;
    
    /**
     * 账户锁定时长（分钟）
     */
    private static final int LOCK_DURATION_MINUTES = 15;
    
    @Autowired
    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            TOTPService totpService,
            CaptchaService captchaService,
            JwtService jwtService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.totpService = totpService;
        this.captchaService = captchaService;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }
    
    /**
     * 用户登录认证
     * 
     * @param request 登录请求
     * @param clientIp 客户端IP地址
     * @return 认证结果
     */
    @com.myweb.website_core.infrastructure.security.Auditable(
        operation = com.myweb.website_core.common.enums.AuditOperation.USER_LOGIN_SUCCESS,
        resourceType = "USER",
        description = "用户登录认证",
        sensitiveParams = {0}, // LoginRequest包含敏感信息
        riskLevel = 3,
        tags = "authentication,login"
    )
    @Transactional
    public CompletableFuture<AuthenticationResult> authenticateUser(LoginRequest request, String clientIp) {
        long startTime = System.currentTimeMillis();
        String username = request.getUsername();
        
        try {
            log.info("开始用户登录认证: username={}, ip={}", username, clientIp);
            
            // 1. 查找用户
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return handleUserNotFound(username, clientIp, startTime);
            }
            
            // 2. 检查账户锁定状态
            if (isAccountLocked(user)) {
                return handleAccountLocked(user, clientIp, startTime);
            }
            
            // 3. 检查是否需要验证码
            if (user.getLoginAttempts() >= CAPTCHA_THRESHOLD) {
                if (!request.hasCaptcha() || !captchaService.validateCaptcha(request.getCaptcha())) {
                    return handleCaptchaRequired(user, clientIp, startTime);
                }
            }
            
            // 4. 验证密码
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return handlePasswordMismatch(user, clientIp, startTime);
            }
            
            // 5. 管理员TOTP二次验证
            if (user.requiresTwoFactorAuth()) {
                if (!request.hasTotpCode() || !totpService.validateTOTP(user.getTotpSecret(), request.getTotpCode(), username)) {
                    return handleTotpValidationFailure(user, clientIp, startTime);
                }
            }
            
            // 6. 登录成功处理
            return handleLoginSuccess(user, clientIp, startTime);
            
        } catch (Exception e) {
            log.error("用户登录认证过程中发生异常: username={}, ip={}", username, clientIp, e);
            
            // 记录异常审计日志
            long executionTime = System.currentTimeMillis() - startTime;
            auditLogService.logUserLogin(null, username, clientIp, null, false, "系统异常: " + e.getMessage());
            
            return CompletableFuture.completedFuture(
                AuthenticationResult.failure("登录过程中发生系统异常")
            );
        }
    }
    
    /**
     * 处理用户不存在的情况
     */
    private CompletableFuture<AuthenticationResult> handleUserNotFound(String username, String clientIp, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        log.warn("登录失败 - 用户不存在: username={}, ip={}, executionTime={}ms", username, clientIp, executionTime);
        
        // 记录审计日志
        auditLogService.logUserLogin(null, username, clientIp, null, false, "用户不存在");
        
        return CompletableFuture.completedFuture(
            AuthenticationResult.failure("用户名或密码错误")
        );
    }
    
    /**
     * 处理账户锁定的情况
     */
    private CompletableFuture<AuthenticationResult> handleAccountLocked(User user, String clientIp, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        log.warn("登录失败 - 账户已锁定: username={}, ip={}, lockedUntil={}, executionTime={}ms", 
                user.getUsername(), clientIp, user.getAccountLockedUntil(), executionTime);
        
        // 记录审计日志
        auditLogService.logUserLogin(user.getId(), user.getUsername(), clientIp, null, false, "账户已锁定");
        
        return CompletableFuture.completedFuture(
            AuthenticationResult.accountLocked(user.getAccountLockedUntil())
        );
    }
    
    /**
     * 处理需要验证码的情况
     */
    private CompletableFuture<AuthenticationResult> handleCaptchaRequired(User user, String clientIp, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        int remainingAttempts = LOCK_THRESHOLD - user.getLoginAttempts();
        
        log.warn("登录失败 - 需要验证码: username={}, ip={}, attempts={}, remaining={}, executionTime={}ms", 
                user.getUsername(), clientIp, user.getLoginAttempts(), remainingAttempts, executionTime);
        
        // 记录审计日志
        auditLogService.logUserLogin(user.getId(), user.getUsername(), clientIp, null, false, "需要验证码");
        
        return CompletableFuture.completedFuture(
            AuthenticationResult.requiresCaptcha(remainingAttempts)
        );
    }
    
    /**
     * 处理密码不匹配的情况
     */
    private CompletableFuture<AuthenticationResult> handlePasswordMismatch(User user, String clientIp, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        // 增加登录失败次数
        user.incrementLoginAttempts();
        userRepository.save(user);
        
        int remainingAttempts = LOCK_THRESHOLD - user.getLoginAttempts();
        boolean isLocked = user.getLoginAttempts() >= LOCK_THRESHOLD;
        
        log.warn("登录失败 - 密码错误: username={}, ip={}, attempts={}, remaining={}, locked={}, executionTime={}ms", 
                user.getUsername(), clientIp, user.getLoginAttempts(), remainingAttempts, isLocked, executionTime);
        
        // 记录审计日志
        String errorMessage = isLocked ? "密码错误，账户已锁定" : "密码错误";
        auditLogService.logUserLogin(user.getId(), user.getUsername(), clientIp, null, false, errorMessage);
        
        if (isLocked) {
            return CompletableFuture.completedFuture(
                AuthenticationResult.accountLocked(user.getAccountLockedUntil())
            );
        } else if (user.getLoginAttempts() >= CAPTCHA_THRESHOLD && remainingAttempts > 0) {
            return CompletableFuture.completedFuture(
                AuthenticationResult.requiresCaptcha(remainingAttempts)
            );
        } else {
            return CompletableFuture.completedFuture(
                AuthenticationResult.failure("用户名或密码错误")
            );
        }
    }
    
    /**
     * 处理TOTP验证失败的情况
     */
    private CompletableFuture<AuthenticationResult> handleTotpValidationFailure(User user, String clientIp, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        log.warn("登录失败 - TOTP验证失败: username={}, ip={}, executionTime={}ms", 
                user.getUsername(), clientIp, executionTime);
        
        // 记录审计日志
        auditLogService.logUserLogin(user.getId(), user.getUsername(), clientIp, null, false, "TOTP验证失败");
        
        return CompletableFuture.completedFuture(
            AuthenticationResult.failure("动态口令验证失败")
        );
    }
    
    /**
     * 处理登录成功的情况
     */
    private CompletableFuture<AuthenticationResult> handleLoginSuccess(User user, String clientIp, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        try {
            // 重置登录失败次数
            user.resetLoginAttempts();
            
            // 更新最后登录信息
            user.updateLastLoginInfo(LocalDateTime.now(), clientIp);
            
            // 保存用户信息
            userRepository.save(user);
            
            // 生成JWT令牌
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            Long expiresIn = jwtService.getAccessTokenExpiration();
            
            log.info("登录成功: username={}, ip={}, executionTime={}ms", 
                    user.getUsername(), clientIp, executionTime);
            
            // 记录成功审计日志
            auditLogService.logUserLogin(user.getId(), user.getUsername(), clientIp, null, true, null);
            
            return CompletableFuture.completedFuture(
                AuthenticationResult.success(user, accessToken, refreshToken, expiresIn)
            );
            
        } catch (Exception e) {
            log.error("登录成功处理过程中发生异常: username={}, ip={}", user.getUsername(), clientIp, e);
            
            // 记录异常审计日志
            auditLogService.logUserLogin(user.getId(), user.getUsername(), clientIp, null, false, "登录成功处理异常: " + e.getMessage());
            
            return CompletableFuture.completedFuture(
                AuthenticationResult.failure("登录处理过程中发生异常")
            );
        }
    }
    
    /**
     * 检查账户是否被锁定
     * 
     * @param user 用户对象
     * @return 是否被锁定
     */
    private boolean isAccountLocked(User user) {
        if (user.getAccountLockedUntil() == null) {
            return false;
        }
        
        // 如果锁定时间已过，清除锁定状态
        if (user.getAccountLockedUntil().isBefore(LocalDateTime.now())) {
            user.setAccountLockedUntil(null);
            user.setLoginAttempts(0);
            userRepository.save(user);
            return false;
        }
        
        return true;
    }
    
    /**
     * 手动解锁用户账户
     * 
     * @param username 用户名
     * @param adminUsername 管理员用户名
     * @return 是否解锁成功
     */
    @Transactional
    public boolean unlockUserAccount(String username, String adminUsername) {
        try {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                log.warn("解锁账户失败 - 用户不存在: username={}, admin={}", username, adminUsername);
                return false;
            }
            
            if (!user.isAccountLocked()) {
                log.info("账户未被锁定，无需解锁: username={}, admin={}", username, adminUsername);
                return true;
            }
            
            // 清除锁定状态
            user.setAccountLockedUntil(null);
            user.setLoginAttempts(0);
            userRepository.save(user);
            
            log.info("账户解锁成功: username={}, admin={}", username, adminUsername);
            
            // 记录审计日志
            auditLogService.logLoginAttempt(username, null, "ACCOUNT_UNLOCKED_BY_ADMIN");
            
            return true;
            
        } catch (Exception e) {
            log.error("解锁账户过程中发生异常: username={}, admin={}", username, adminUsername, e);
            return false;
        }
    }
    
    /**
     * 获取用户登录状态信息
     * 
     * @param username 用户名
     * @return 登录状态信息
     */
    public String getUserLoginStatus(String username) {
        try {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return "用户不存在";
            }
            
            if (user.isAccountLocked()) {
                return String.format("账户已锁定，解锁时间: %s", user.getAccountLockedUntil());
            }
            
            if (user.getLoginAttempts() >= CAPTCHA_THRESHOLD) {
                return String.format("需要验证码，失败次数: %d", user.getLoginAttempts());
            }
            
            if (user.getLoginAttempts() > 0) {
                return String.format("正常状态，失败次数: %d", user.getLoginAttempts());
            }
            
            return "正常状态";
            
        } catch (Exception e) {
            log.error("获取用户登录状态失败: username={}", username, e);
            return "状态查询失败";
        }
    }
    
    /**
     * 重置用户登录失败次数
     * 
     * @param username 用户名
     * @param adminUsername 管理员用户名
     * @return 是否重置成功
     */
    @Transactional
    public boolean resetLoginAttempts(String username, String adminUsername) {
        try {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                log.warn("重置登录失败次数失败 - 用户不存在: username={}, admin={}", username, adminUsername);
                return false;
            }
            
            int oldAttempts = user.getLoginAttempts();
            user.resetLoginAttempts();
            userRepository.save(user);
            
            log.info("重置登录失败次数成功: username={}, oldAttempts={}, admin={}", username, oldAttempts, adminUsername);
            
            // 记录审计日志
            auditLogService.logLoginAttempt(username, null, "LOGIN_ATTEMPTS_RESET_BY_ADMIN");
            
            return true;
            
        } catch (Exception e) {
            log.error("重置登录失败次数过程中发生异常: username={}, admin={}", username, adminUsername, e);
            return false;
        }
    }
}