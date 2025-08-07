package com.myweb.website_core.common.security.exception;

/**
 * CSRF异常类
 * 用于处理CSRF令牌验证失败的情况
 * 
 * @author Kiro
 * @since 1.0.0
 */
public class CustomCsrfException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 默认构造函数
     */
    public CustomCsrfException() {
        super("CSRF token validation failed");
    }
    
    /**
     * 带消息的构造函数
     * 
     * @param message 异常消息
     */
    public CustomCsrfException(String message) {
        super(message);
    }
    
    /**
     * 带消息和原因的构造函数
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public CustomCsrfException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 带原因的构造函数
     * 
     * @param cause 异常原因
     */
    public CustomCsrfException(Throwable cause) {
        super("CSRF token validation failed", cause);
    }
}