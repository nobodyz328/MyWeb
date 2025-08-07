package com.myweb.website_core.common.exception;

import lombok.Getter;

/**
 * 输入验证异常类
 * 
 * 当输入数据不符合安全验证规则时抛出此异常。
 * 
 * 主要用途：
 * 1. 输入长度超限
 * 2. 包含非法字符
 * 3. XSS攻击检测
 * 4. SQL注入检测
 * 5. 格式验证失败
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Getter
public class ValidationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 验证失败的字段名
     * -- GETTER --
     *  获取字段名
     *

     */
    private final String fieldName;
    
    /**
     * 验证失败的原因代码
     * -- GETTER --
     *  获取原因代码
     *
     * @return 原因代码

     */
    private final String reasonCode;
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
        this.reasonCode = null;
    }
    
    /**
     * 构造函数
     * 
     * @param message   异常消息
     * @param fieldName 字段名
     */
    public ValidationException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
        this.reasonCode = null;
    }
    
    /**
     * 构造函数
     * 
     * @param message    异常消息
     * @param fieldName  字段名
     * @param reasonCode 原因代码
     */
    public ValidationException(String message, String fieldName, String reasonCode) {
        super(message);
        this.fieldName = fieldName;
        this.reasonCode = reasonCode;
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause   原因异常
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
        this.reasonCode = null;
    }
    
    /**
     * 构造函数
     * 
     * @param message   异常消息
     * @param cause     原因异常
     * @param fieldName 字段名
     */
    public ValidationException(String message, Throwable cause, String fieldName) {
        super(message, cause);
        this.fieldName = fieldName;
        this.reasonCode = null;
    }
    
    /**
     * 构造函数
     * 
     * @param message    异常消息
     * @param cause      原因异常
     * @param fieldName  字段名
     * @param reasonCode 原因代码
     */
    public ValidationException(String message, Throwable cause, String fieldName, String reasonCode) {
        super(message, cause);
        this.fieldName = fieldName;
        this.reasonCode = reasonCode;
    }

}