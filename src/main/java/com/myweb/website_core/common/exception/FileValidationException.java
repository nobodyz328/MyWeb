package com.myweb.website_core.common.exception;

/**
 * 文件验证异常
 * 
 * 当文件上传安全验证失败时抛出此异常
 * 符合GB/T 22239-2019二级等保要求的恶意代码防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public class FileValidationException extends RuntimeException {
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public FileValidationException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}