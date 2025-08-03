package com.myweb.website_core.common.security.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 账户锁定异常
 * 当用户账户因多次登录失败或其他安全原因被锁定时抛出
 * 
 * 符合GB/T 22239-2019 7.1.4.1 身份鉴别要求
 */
public class AccountLockedException extends AuthenticationException {
    
    private final LocalDateTime lockedUntil;
    
    /**
     * 构造账户锁定异常
     * 
     * @param message 异常消息
     */
    public AccountLockedException(String message) {
        super(message);
        this.lockedUntil = null;
    }
    
    /**
     * 构造账户锁定异常
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
        this.lockedUntil = null;
    }
    
    /**
     * 构造账户锁定异常
     * 
     * @param username 用户名
     * @param lockedUntil 锁定截止时间
     */
    public AccountLockedException(String username, LocalDateTime lockedUntil) {
        super(String.format("账户 %s 已被锁定，解锁时间: %s", 
            username, 
            lockedUntil.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        this.lockedUntil = lockedUntil;
    }
    
    /**
     * 构造账户锁定异常
     * 
     * @param username 用户名
     * @param lockDurationMinutes 锁定时长（分钟）
     */
    public AccountLockedException(String username, int lockDurationMinutes) {
        this(username, LocalDateTime.now().plusMinutes(lockDurationMinutes));
    }
    
    /**
     * 获取锁定截止时间
     * 
     * @return 锁定截止时间，如果未设置则返回null
     */
    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
    
    /**
     * 检查账户是否仍处于锁定状态
     * 
     * @return 如果仍处于锁定状态返回true，否则返回false
     */
    public boolean isStillLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }
}