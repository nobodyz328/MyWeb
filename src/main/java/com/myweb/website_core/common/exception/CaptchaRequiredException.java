package com.myweb.website_core.common.exception;

import com.myweb.website_core.common.exception.security.AuthenticationException;

/**
 * 验证码必需异常
 * 当用户连续登录失败达到阈值后需要验证码时抛出
 * 
 * 符合GB/T 22239-2019 7.1.4.1 身份鉴别要求
 */
public class CaptchaRequiredException extends AuthenticationException {
    
    private final int failedAttempts;
    private final int remainingAttempts;
    
    /**
     * 构造验证码必需异常
     * 
     * @param message 异常消息
     */
    public CaptchaRequiredException(String message) {
        super(message);
        this.failedAttempts = 0;
        this.remainingAttempts = 0;
    }
    
    /**
     * 构造验证码必需异常
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public CaptchaRequiredException(String message, Throwable cause) {
        super(message, cause);
        this.failedAttempts = 0;
        this.remainingAttempts = 0;
    }
    
    /**
     * 构造验证码必需异常
     * 
     * @param username 用户名
     * @param failedAttempts 已失败次数
     * @param remainingAttempts 剩余尝试次数
     */
    public CaptchaRequiredException(String username, int failedAttempts, int remainingAttempts) {
        super(String.format("用户 %s 连续登录失败 %d 次，需要验证码验证", username, failedAttempts));
        this.failedAttempts = failedAttempts;
        this.remainingAttempts = remainingAttempts;
    }
    
    /**
     * 获取已失败次数
     * 
     * @return 已失败次数
     */
    public int getFailedAttempts() {
        return failedAttempts;
    }
    
    /**
     * 获取剩余尝试次数
     * 
     * @return 剩余尝试次数
     */
    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}