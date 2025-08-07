package com.myweb.website_core.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 安全文件名验证注解
 * 
 * 验证文件名是否安全：
 * 1. 长度不超过255字符
 * 2. 只包含安全字符
 * 3. 不包含危险文件扩展名
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeFilenameValidator.class)
@Documented
public @interface SafeFilename {
    
    /**
     * 错误消息
     */
    String message() default "文件名不安全";
    
    /**
     * 验证组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
}