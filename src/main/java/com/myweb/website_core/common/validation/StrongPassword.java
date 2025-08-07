package com.myweb.website_core.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 强密码验证注解
 * 
 * 验证密码是否符合强度要求：
 * 1. 长度8-128字符
 * 2. 包含至少3种字符类型（大写字母、小写字母、数字、特殊字符）
 * 3. 不是常见弱密码
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
@Documented
public @interface StrongPassword {
    
    /**
     * 错误消息
     */
    String message() default "密码强度不符合要求";
    
    /**
     * 验证组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
}