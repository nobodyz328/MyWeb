package com.myweb.website_core.common.exception;

/**
 * 文件上传异常类
 * 
 * 用于处理文件上传过程中的各种异常情况：
 * - 文件类型不支持
 * - 文件大小超限
 * - 文件保存失败
 * - 其他上传相关错误
 */
public class FileUploadException extends Exception {
    
    public FileUploadException(String message) {
        super(message);
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}