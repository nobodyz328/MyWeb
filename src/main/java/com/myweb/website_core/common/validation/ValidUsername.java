package com.myweb.website_core.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 用户名验证注解
 * 
 * 验证用户名是否符合要求：
 * 1. 长度3-50字符
 * 2. 只能包含字母、数字、下划线和连字符
 * 3. 不能是系统保留用户名
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidUsernameValidator.class)
@Documented
public @interface ValidUsername {
    
    /**
     * 错误消息
     */
    String message() default "用户名格式不正确";
    
    /**
     * 验证组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
}