package com.myweb.website_core.common.exception;

/**
 * 频率限制超出异常
 * <p>
 * 当用户操作频率超出限制时抛出此异常
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public class RateLimitExceededException extends RuntimeException {
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因
     */
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}