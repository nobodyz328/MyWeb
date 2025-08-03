package com.myweb.website_core.common.security.exception;

import java.util.List;
import java.util.Map;

/**
 * 输入验证异常
 * 当用户输入数据不符合安全要求或业务规则时抛出
 * 
 * 符合GB/T 22239-2019 7.1.4.4 入侵防范要求
 */
public class ValidationException extends RuntimeException {
    
    private final String fieldName;
    private final Object invalidValue;
    private final String validationType;
    private final Map<String, List<String>> fieldErrors;
    
    /**
     * 构造输入验证异常
     * 
     * @param message 异常消息
     */
    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
        this.invalidValue = null;
        this.validationType = null;
        this.fieldErrors = null;
    }
    
    /**
     * 构造输入验证异常
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
        this.invalidValue = null;
        this.validationType = null;
        this.fieldErrors = null;
    }
    
    /**
     * 构造输入验证异常
     * 
     * @param fieldName 字段名
     * @param invalidValue 无效值
     * @param message 异常消息
     */
    public ValidationException(String fieldName, Object invalidValue, String message) {
        super(String.format("字段 %s 验证失败: %s", fieldName, message));
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.validationType = null;
        this.fieldErrors = null;
    }
    
    /**
     * 构造输入验证异常
     * 
     * @param fieldName 字段名
     * @param invalidValue 无效值
     * @param validationType 验证类型
     * @param message 异常消息
     */
    public ValidationException(String fieldName, Object invalidValue, String validationType, String message) {
        super(String.format("字段 %s 验证失败 (%s): %s", fieldName, validationType, message));
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.validationType = validationType;
        this.fieldErrors = null;
    }
    
    /**
     * 构造多字段验证异常
     * 
     * @param fieldErrors 字段错误映射
     */
    public ValidationException(Map<String, List<String>> fieldErrors) {
        super("多个字段验证失败: " + fieldErrors.toString());
        this.fieldName = null;
        this.invalidValue = null;
        this.validationType = null;
        this.fieldErrors = fieldErrors;
    }
    
    /**
     * 创建长度超限异常
     * 
     * @param fieldName 字段名
     * @param actualLength 实际长度
     * @param maxLength 最大长度
     * @return 验证异常
     */
    public static ValidationException lengthExceeded(String fieldName, int actualLength, int maxLength) {
        return new ValidationException(fieldName, actualLength, "LENGTH_CHECK", 
            String.format("长度超过限制，实际: %d, 最大允许: %d", actualLength, maxLength));
    }
    
    /**
     * 创建非法字符异常
     * 
     * @param fieldName 字段名
     * @param invalidValue 包含非法字符的值
     * @return 验证异常
     */
    public static ValidationException illegalCharacters(String fieldName, String invalidValue) {
        return new ValidationException(fieldName, invalidValue, "CHARACTER_CHECK", "包含非法字符");
    }
    
    /**
     * 创建XSS攻击检测异常
     * 
     * @param fieldName 字段名
     * @param suspiciousValue 可疑值
     * @return 验证异常
     */
    public static ValidationException xssDetected(String fieldName, String suspiciousValue) {
        return new ValidationException(fieldName, suspiciousValue, "XSS_CHECK", 
            "检测到潜在的XSS攻击代码");
    }
    
    /**
     * 创建SQL注入检测异常
     * 
     * @param fieldName 字段名
     * @param suspiciousValue 可疑值
     * @return 验证异常
     */
    public static ValidationException sqlInjectionDetected(String fieldName, String suspiciousValue) {
        return new ValidationException(fieldName, suspiciousValue, "SQL_INJECTION_CHECK", 
            "检测到潜在的SQL注入代码");
    }
    
    /**
     * 创建格式不正确异常
     * 
     * @param fieldName 字段名
     * @param invalidValue 无效值
     * @param expectedFormat 期望格式
     * @return 验证异常
     */
    public static ValidationException invalidFormat(String fieldName, String invalidValue, String expectedFormat) {
        return new ValidationException(fieldName, invalidValue, "FORMAT_CHECK", 
            String.format("格式不正确，期望格式: %s", expectedFormat));
    }
    
    /**
     * 创建必填字段为空异常
     * 
     * @param fieldName 字段名
     * @return 验证异常
     */
    public static ValidationException requiredFieldEmpty(String fieldName) {
        return new ValidationException(fieldName, null, "REQUIRED_CHECK", "必填字段不能为空");
    }
    
    /**
     * 获取字段名
     * 
     * @return 字段名，如果未设置则返回null
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * 获取无效值
     * 
     * @return 无效值，如果未设置则返回null
     */
    public Object getInvalidValue() {
        return invalidValue;
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
     * 获取字段错误映射
     * 
     * @return 字段错误映射，如果未设置则返回null
     */
    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
    
    /**
     * 检查是否为多字段验证异常
     * 
     * @return 如果是多字段验证异常返回true，否则返回false
     */
    public boolean isMultiFieldValidation() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
}