package com.myweb.website_core.common.exception;

/**
 * 数据完整性异常
 * 
 * 当数据完整性检查失败或相关操作出现问题时抛出此异常
 * 符合GB/T 22239-2019数据完整性保护要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
public class DataIntegrityException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public DataIntegrityException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public DataIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造函数
     * 
     * @param cause 原因异常
     */
    public DataIntegrityException(Throwable cause) {
        super(cause);
    }
}