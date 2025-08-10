package com.myweb.website_core.common.exception.security;

/**
 * 验证异常
 * <p>
 * 用于表示数据验证失败的异常
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public class ValidationException extends RuntimeException {
    
    private final String field;
    private final Object value;
    private final String errorCode;
    
    /**
     * 构造函数
     * 
     * @param field 验证失败的字段名
     * @param value 验证失败的值
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public ValidationException(String field, Object value, String errorCode, String message) {
        super(message);
        this.field = field;
        this.value = value;
        this.errorCode = errorCode;
    }
    /**
     * 构造函数
     *
     * @param message    异常消息
     * @param field  字段名
     * @param errorCode 原因代码
     */
    public ValidationException(String message, String field, String errorCode) {
        super(message);
        this.field = field;
        this.errorCode = errorCode;
        this.value = null;
    }
    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.value = null;
        this.errorCode = null;
    }
    
    public String getField() {
        return field;
    }
    
    public Object getValue() {
        return value;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}