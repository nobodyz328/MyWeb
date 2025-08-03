package com.myweb.website_core.common.security.exception;

/**
 * 文件验证异常
 * 当文件上传或处理过程中发现安全问题时抛出
 * 
 * 符合GB/T 22239-2019 7.1.4.5 恶意代码防范要求
 */
public class FileValidationException extends RuntimeException {
    
    private final String fileName;
    private final String validationType;
    private final long fileSize;
    
    /**
     * 构造文件验证异常
     * 
     * @param message 异常消息
     */
    public FileValidationException(String message) {
        super(message);
        this.fileName = null;
        this.validationType = null;
        this.fileSize = 0;
    }
    
    /**
     * 构造文件验证异常
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.validationType = null;
        this.fileSize = 0;
    }
    
    /**
     * 构造文件验证异常
     * 
     * @param fileName 文件名
     * @param validationType 验证类型
     * @param message 异常消息
     */
    public FileValidationException(String fileName, String validationType, String message) {
        super(String.format("文件 %s 验证失败 (%s): %s", fileName, validationType, message));
        this.fileName = fileName;
        this.validationType = validationType;
        this.fileSize = 0;
    }
    
    /**
     * 构造文件验证异常
     * 
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param validationType 验证类型
     * @param message 异常消息
     */
    public FileValidationException(String fileName, long fileSize, String validationType, String message) {
        super(String.format("文件 %s (大小: %d bytes) 验证失败 (%s): %s", 
            fileName, fileSize, validationType, message));
        this.fileName = fileName;
        this.validationType = validationType;
        this.fileSize = fileSize;
    }
    
    /**
     * 创建文件大小超限异常
     * 
     * @param fileName 文件名
     * @param actualSize 实际大小
     * @param maxSize 最大允许大小
     * @return 文件验证异常
     */
    public static FileValidationException fileSizeExceeded(String fileName, long actualSize, long maxSize) {
        return new FileValidationException(fileName, actualSize, "SIZE_CHECK", 
            String.format("文件大小超过限制，实际: %d bytes, 最大允许: %d bytes", actualSize, maxSize));
    }
    
    /**
     * 创建文件类型不支持异常
     * 
     * @param fileName 文件名
     * @param fileType 文件类型
     * @return 文件验证异常
     */
    public static FileValidationException unsupportedFileType(String fileName, String fileType) {
        return new FileValidationException(fileName, "TYPE_CHECK", 
            String.format("不支持的文件类型: %s", fileType));
    }
    
    /**
     * 创建文件魔数不匹配异常
     * 
     * @param fileName 文件名
     * @param expectedType 期望类型
     * @param actualMagic 实际魔数
     * @return 文件验证异常
     */
    public static FileValidationException magicNumberMismatch(String fileName, String expectedType, String actualMagic) {
        return new FileValidationException(fileName, "MAGIC_NUMBER_CHECK", 
            String.format("文件内容与扩展名不匹配，期望类型: %s, 实际魔数: %s", expectedType, actualMagic));
    }
    
    /**
     * 创建恶意内容检测异常
     * 
     * @param fileName 文件名
     * @param maliciousPattern 检测到的恶意模式
     * @return 文件验证异常
     */
    public static FileValidationException maliciousContentDetected(String fileName, String maliciousPattern) {
        return new FileValidationException(fileName, "MALICIOUS_CONTENT_CHECK", 
            String.format("检测到恶意内容，模式: %s", maliciousPattern));
    }
    
    /**
     * 获取文件名
     * 
     * @return 文件名，如果未设置则返回null
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * 获取验证类型
     * 
     * @return 验证类型，如果未设置则返回null
     */
    public String getValidationType() {
        return validationType;
    }
    
    /**
     * 获取文件大小
     * 
     * @return 文件大小，如果未设置则返回0
     */
    public long getFileSize() {
        return fileSize;
    }
}