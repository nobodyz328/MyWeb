package com.myweb.website_core.common.validation;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.exception.security.ValidationException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户名验证器
 * 
 * 实现ValidUsername注解的验证逻辑，使用InputValidationService进行验证。
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Component
public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {
    
    @Autowired
    private InputValidationService inputValidationService;
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false; // 用户名不能为null
        }
        
        try {
            inputValidationService.validateUsername(value);
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