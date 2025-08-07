package com.myweb.website_core.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 安全字符串验证注解
 * 
 * 验证字符串是否符合安全要求，包括：
 * 1. 长度限制
 * 2. 字符集检查
 * 3. XSS攻击检测
 * 4. SQL注入检测
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeStringValidator.class)
@Documented
public @interface SafeString {
    
    /**
     * 错误消息
     */
    String message() default "字符串包含不安全内容";
    
    /**
     * 验证组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 最大长度
     */
    int maxLength() default 10000;
    
    /**
     * 是否允许为空
     */
    boolean allowEmpty() default true;
    
    /**
     * 是否检查XSS
     */
    boolean checkXss() default true;
    
    /**
     * 是否检查SQL注入
     */
    boolean checkSqlInjection() default true;
    
    /**
     * 自定义字段名（用于错误消息）
     */
    String fieldName() default "";
}