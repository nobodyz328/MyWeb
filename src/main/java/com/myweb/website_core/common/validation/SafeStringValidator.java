package com.myweb.website_core.common.validation;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.exception.security.ValidationException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 安全字符串验证器
 * 
 * 实现SafeString注解的验证逻辑，使用InputValidationService进行验证。
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Component
public class SafeStringValidator implements ConstraintValidator<SafeString, String> {
    
    @Autowired
    private InputValidationService inputValidationService;
    
    private SafeString annotation;
    
    @Override
    public void initialize(SafeString constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果值为null或空字符串，根据allowEmpty决定
        if (value == null || value.isEmpty()) {
            return annotation.allowEmpty();
        }
        
        try {
            // 使用InputValidationService进行验证
            String fieldName = annotation.fieldName().isEmpty() ? "字段" : annotation.fieldName();
            inputValidationService.validateStringInput(value, fieldName, annotation.maxLength());
            return true;
        } catch (ValidationException e) {
            // 自定义错误消息
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage())
                   .addConstraintViolation();
            return false;
        }
    }
}