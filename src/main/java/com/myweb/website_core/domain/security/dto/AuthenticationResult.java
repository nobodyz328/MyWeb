package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.domain.business.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 身份验证结果DTO
 * 
 * 封装用户身份验证的结果信息，包括：
 * - 认证状态和用户信息
 * - JWT令牌信息
 * - 会话和安全信息
 * 
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResult {
    
    /**
     * 认证是否成功
     */
    private boolean success;
    
    /**
     * 认证的用户信息
     */
    private User user;
    
    /**
     * JWT访问令牌
     */
    private String accessToken;
    
    /**
     * JWT刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型（通常为"Bearer"）
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 认证时间
     */
    private LocalDateTime authenticatedAt;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 是否需要二次验证
     */
    private boolean requiresTwoFactor;
    
    /**
     * 认证失败原因（失败时）
     */
    private String failureReason;
    
    /**
     * 账户锁定截止时间（如果账户被锁定）
     */
    private LocalDateTime accountLockedUntil;
    
    /**
     * 剩余登录尝试次数
     */
    private Integer remainingAttempts;
    
    /**
     * 是否需要验证码
     */
    private boolean requiresCaptcha;
    
    /**
     * 创建成功的认证结果
     * 
     * @param user 认证的用户
     * @param accessToken JWT访问令牌
     * @param expiresIn 令牌过期时间（秒）
     * @return 认证结果
     */
    public static AuthenticationResult success(User user, String accessToken, Long expiresIn) {
        return AuthenticationResult.builder()
                .success(true)
                .user(user)
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .authenticatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功的认证结果（带刷新令牌）
     * 
     * @param user 认证的用户
     * @param accessToken JWT访问令牌
     * @param refreshToken JWT刷新令牌
     * @param expiresIn 令牌过期时间（秒）
     * @return 认证结果
     */
    public static AuthenticationResult success(User user, String accessToken, String refreshToken, Long expiresIn) {
        return AuthenticationResult.builder()
                .success(true)
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .authenticatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败的认证结果
     * 
     * @param reason 失败原因
     * @return 认证结果
     */
    public static AuthenticationResult failure(String reason) {
        return AuthenticationResult.builder()
                .success(false)
                .failureReason(reason)
                .build();
    }
    
    /**
     * 创建需要验证码的认证结果
     * 
     * @param remainingAttempts 剩余尝试次数
     * @return 认证结果
     */
    public static AuthenticationResult requiresCaptcha(Integer remainingAttempts) {
        return AuthenticationResult.builder()
                .success(false)
                .requiresCaptcha(true)
                .remainingAttempts(remainingAttempts)
                .failureReason("需要验证码")
                .build();
    }
    
    /**
     * 创建账户锁定的认证结果
     * 
     * @param lockedUntil 锁定截止时间
     * @return 认证结果
     */
    public static AuthenticationResult accountLocked(LocalDateTime lockedUntil) {
        return AuthenticationResult.builder()
                .success(false)
                .accountLockedUntil(lockedUntil)
                .failureReason("账户已锁定")
                .build();
    }
    
    /**
     * 创建需要二次验证的认证结果
     * 
     * @param user 用户信息
     * @param sessionId 会话ID
     * @return 认证结果
     */
    public static AuthenticationResult requiresTwoFactor(User user, String sessionId) {
        return AuthenticationResult.builder()
                .success(false)
                .user(user)
                .sessionId(sessionId)
                .requiresTwoFactor(true)
                .failureReason("需要二次验证")
                .build();
    }
    
    /**
     * 检查是否认证成功
     * 
     * @return 是否认证成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 检查是否认证失败
     * 
     * @return 是否认证失败
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * 检查是否需要验证码
     * 
     * @return 是否需要验证码
     */
    public boolean needsCaptcha() {
        return requiresCaptcha;
    }
    
    /**
     * 检查是否需要二次验证
     * 
     * @return 是否需要二次验证
     */
    public boolean needsTwoFactor() {
        return requiresTwoFactor;
    }
    
    /**
     * 检查账户是否被锁定
     * 
     * @return 账户是否被锁定
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }
    
    /**
     * 获取令牌的完整格式
     * 
     * @return 完整的令牌字符串
     */
    public String getFullToken() {
        if (accessToken == null) {
            return null;
        }
        return tokenType + " " + accessToken;
    }
    
    /**
     * 清理敏感信息
     * 用于日志记录时隐藏令牌等敏感信息
     * 
     * @return 清理后的认证结果对象
     */
    public AuthenticationResult sanitized() {
        return AuthenticationResult.builder()
                .success(this.success)
                .user(this.user)
                .accessToken(this.accessToken != null ? "***" : null)
                .refreshToken(this.refreshToken != null ? "***" : null)
                .tokenType(this.tokenType)
                .expiresIn(this.expiresIn)
                .authenticatedAt(this.authenticatedAt)
                .sessionId(this.sessionId)
                .requiresTwoFactor(this.requiresTwoFactor)
                .failureReason(this.failureReason)
                .accountLockedUntil(this.accountLockedUntil)
                .remainingAttempts(this.remainingAttempts)
                .requiresCaptcha(this.requiresCaptcha)
                .build();
    }
}