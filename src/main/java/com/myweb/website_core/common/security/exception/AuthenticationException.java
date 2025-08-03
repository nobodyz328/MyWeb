package com.myweb.website_core.common.security.exception;

/**
 * 自定义认证异常
 * 用于处理用户身份认证过程中的各种异常情况
 * 
 * 符合GB/T 22239-2019 7.1.4.1 身份鉴别要求
 */
public class AuthenticationException extends RuntimeException {
    
    /**
     * 构造认证异常
     * 
     * @param message 异常消息
     */
    public AuthenticationException(String message) {
        super(message);
    }
    
    /**
     * 构造认证异常
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造认证异常
     * 
     * @param username 用户名
     * @param reason 认证失败原因
     */
    public AuthenticationException(String username, String reason) {
        super(String.format("用户 %s 认证失败: %s", username, reason));
    }
}