package com.myweb.website_core.common.exception;

/**
 * 文件完整性异常
 * 
 * 当文件完整性检查失败或相关操作出现错误时抛出此异常
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
public class FileIntegrityException extends RuntimeException {
    
    public FileIntegrityException(String message) {
        super(message);
    }
    
    public FileIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public FileIntegrityException(Throwable cause) {
        super(cause);
    }
}