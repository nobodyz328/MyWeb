package com.myweb.website_core.infrastructure.security.aspect;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.SecurityEventUtils;
import com.myweb.website_core.common.validation.ValidateInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全输入验证切面
 * <p>
 * 提供基于AOP的统一输入验证功能，自动拦截标记了@ValidateInput注解的方法，
 * 对方法参数进行安全验证，包括XSS检测、SQL注入检测、长度检查等。
 * <p>
 * 主要功能：
 * 1. 自动拦截@ValidateInput注解的方法
 * 2. 对方法参数进行安全验证
 * 3. 记录验证失败的安全事件
 * 4. 提供详细的审计日志
 * 5. 支持多种验证类型
 * <p>
 * 符合需求：1.1, 1.2, 1.3, 1.6 - 输入验证服务业务集成
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityValidationAspect {
    
    private final InputValidationService inputValidationService;
    
    /**
     * 在执行标记了@ValidateInput注解的方法之前进行输入验证
     * 
     * @param joinPoint 连接点
     * @throws ValidationException 验证失败时抛出
     */
    @Before("@annotation(com.myweb.website_core.common.validation.ValidateInput)")
    public void validateInput(JoinPoint joinPoint) throws ValidationException {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取方法和注解信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            ValidateInput annotation = method.getAnnotation(ValidateInput.class);
            
            if (annotation == null) {
                return;
            }
            
            // 获取方法参数
            Object[] args = joinPoint.getArgs();
            Parameter[] parameters = method.getParameters();
            String[] fieldNames = annotation.fieldNames();
            String[] validationTypes = annotation.validationTypes();
            
            // 验证每个参数
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                
                // 跳过null参数（根据allowEmpty设置）
                if (arg == null) {
                    if (!annotation.allowEmpty()) {
                        String fieldName = getFieldName(fieldNames, parameters, i);
                        throw new ValidationException(
                            String.format("参数 %s 不能为空", fieldName),
                            fieldName,
                            "REQUIRED"
                        );
                    }
                    continue;
                }
                
                // 处理字符串类型的参数
                if (arg instanceof String) {
                    String stringValue = (String) arg;
                    String fieldName = getFieldName(fieldNames, parameters, i);
                    String validationType = getValidationType(validationTypes, i);
                    
                    // 执行验证
                    validateStringParameter(stringValue, fieldName, validationType, annotation);
                }
                // 处理复杂对象（如CreatePostRequest、Post、CommentRequest）
                else {
                    validateComplexObject(arg, fieldNames, validationTypes, annotation);
                }
            }
            
            // 记录成功的验证日志
            long executionTime = System.currentTimeMillis() - startTime;
            String className = method.getDeclaringClass() != null ? 
                method.getDeclaringClass().getSimpleName() : "UnknownClass";
            String methodName = className + "." + method.getName();
            
            log.debug("输入验证成功: {} - 执行时间: {}ms", methodName, executionTime);
            
        } catch (ValidationException e) {
            // 获取注解信息用于记录安全事件
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            ValidateInput annotation = method.getAnnotation(ValidateInput.class);
            
            // 记录验证失败的安全事件
            if (annotation != null && annotation.auditFailures()) {
                recordValidationFailure(joinPoint, e);
            }
            
            // 重新抛出异常
            throw e;
        } catch (Exception e) {
            // 记录意外错误
            log.error("输入验证切面执行异常", e);
            throw new ValidationException("输入验证过程中发生错误", "system", "VALIDATION_ERROR");
        }
    }
    
    /**
     * 验证字符串参数
     * 
     * @param value 参数值
     * @param fieldName 字段名
     * @param validationType 验证类型
     * @param annotation 注解配置
     * @throws ValidationException 验证失败时抛出
     */
    private void validateStringParameter(String value, String fieldName, String validationType, 
                                       ValidateInput annotation) throws ValidationException {
        try {
            // 根据验证类型选择相应的验证方法
            switch (validationType.toLowerCase()) {
                case "username":
                    inputValidationService.validateUsername(value);
                    break;
                case "email":
                    inputValidationService.validateEmail(value);
                    break;
                case "password":
                    inputValidationService.validatePassword(value);
                    break;
                case "title":
                    inputValidationService.validatePostTitle(value);
                    break;
                case "content":
                    inputValidationService.validatePostContent(value);
                    break;
                case "comment":
                    inputValidationService.validateCommentContent(value);
                    break;
                case "filename":
                    inputValidationService.validateFilename(value);
                    break;
                case "url":
                    inputValidationService.validateUrl(value);
                    break;
                case "default":
                default:
                    // 使用通用验证
                    inputValidationService.validateStringInput(value, fieldName, annotation.maxLength());
                    break;
            }
        } catch (ValidationException e) {
            // 增强错误信息
            String enhancedMessage = annotation.errorMessage()
                .replace("{fieldName}", fieldName)
                .replace("{maxLength}", String.valueOf(annotation.maxLength()));
            
            throw new ValidationException(
                enhancedMessage + ": " + e.getMessage(),
                e.getField(),
                e.getErrorCode()
            );
        }
    }
    
    /**
     * 验证复杂对象参数
     * 
     * @param obj 对象参数
     * @param fieldNames 字段名数组
     * @param validationTypes 验证类型数组
     * @param annotation 注解配置
     * @throws ValidationException 验证失败时抛出
     */
    private void validateComplexObject(Object obj, String[] fieldNames, String[] validationTypes, 
                                     ValidateInput annotation) throws ValidationException {
        if (obj == null) {
            return;
        }
        
        try {
            // 使用反射获取对象的字段值
            Class<?> objClass = obj.getClass();
            
            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];
                String validationType = getValidationType(validationTypes, i);
                
                // 尝试获取字段值
                String fieldValue = getFieldValueFromObject(obj, fieldName, objClass);
                
                if (fieldValue != null) {
                    validateStringParameter(fieldValue, fieldName, validationType, annotation);
                }
            }
            
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.debug("验证复杂对象时发生异常: {}", e.getMessage());
            // 如果反射失败，不抛出异常，让其他验证继续
        }
    }
    
    /**
     * 从对象中获取字段值
     * 
     * @param obj 对象
     * @param fieldName 字段名
     * @param objClass 对象类型
     * @return 字段值（字符串类型）
     */
    private String getFieldValueFromObject(Object obj, String fieldName, Class<?> objClass) {
        try {
            // 尝试通过getter方法获取值
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = objClass.getMethod(getterName);
            Object value = getter.invoke(obj);
            
            return value != null ? value.toString() : null;
            
        } catch (Exception e) {
            try {
                // 尝试直接访问字段
                java.lang.reflect.Field field = objClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(obj);
                
                return value != null ? value.toString() : null;
                
            } catch (Exception ex) {
                log.debug("无法获取字段值: {} from {}", fieldName, objClass.getSimpleName());
                return null;
            }
        }
    }
    
    /**
     * 获取字段名称
     * 
     * @param fieldNames 配置的字段名数组
     * @param parameters 方法参数
     * @param index 参数索引
     * @return 字段名称
     */
    private String getFieldName(String[] fieldNames, Parameter[] parameters, int index) {
        // 优先使用注解中配置的字段名
        if (fieldNames.length > index && !fieldNames[index].isEmpty()) {
            return fieldNames[index];
        }
        
        // 其次使用参数名
        if (parameters.length > index) {
            return parameters[index].getName();
        }
        
        // 最后使用默认格式
        return "field" + index;
    }
    
    /**
     * 获取验证类型
     * 
     * @param validationTypes 配置的验证类型数组
     * @param index 参数索引
     * @return 验证类型
     */
    private String getValidationType(String[] validationTypes, int index) {
        if (validationTypes.length > index) {
            return validationTypes[index];
        }
        
        // 如果只有一个验证类型，应用到所有参数
        if (validationTypes.length == 1) {
            return validationTypes[0];
        }
        
        return "default";
    }
    
    /**
     * 记录验证失败的安全事件
     * 
     * @param joinPoint 连接点
     * @param exception 验证异常
     */
    private void recordValidationFailure(JoinPoint joinPoint, ValidationException exception) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = method.getDeclaringClass() != null ? 
                method.getDeclaringClass().getSimpleName() : "UnknownClass";
            String methodName = className + "." + method.getName();
            
            // 确定安全事件类型
            SecurityEventType eventType = determineSecurityEventType(exception);
            
            // 创建事件数据
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("method", methodName);
            eventData.put("fieldName", exception.getField());
            eventData.put("errorCode", exception.getErrorCode());
            eventData.put("errorMessage", exception.getMessage());
            eventData.put("validationFailure", true);
            
            // 记录安全事件
            String title = "输入验证失败";
            String description = String.format("方法 %s 的参数 %s 验证失败：%s", 
                methodName, exception.getField(), exception.getMessage());
            
            // 使用SecurityEventUtils创建安全事件（这里假设有一个服务来处理安全事件）
            // 由于没有找到SecurityEventService，我们先记录日志
            String securityLog = LoggingUtils.formatSecurityEvent(
                eventType,
                SecurityEventUtils.getUsername(),
                SecurityEventUtils.getIpAddress(),
                description,
                SecurityEventUtils.getUserAgent(),
                eventData
            );
            
            log.warn("安全事件: {}", securityLog);
            
        } catch (Exception e) {
            log.error("记录验证失败安全事件时发生错误", e);
        }
    }
    
    /**
     * 根据验证异常确定安全事件类型
     * 
     * @param exception 验证异常
     * @return 安全事件类型
     */
    private SecurityEventType determineSecurityEventType(ValidationException exception) {
        String errorCode = exception.getErrorCode();
        
        if (errorCode == null) {
            return SecurityEventType.INPUT_VALIDATION_FAILURE;
        }
        
        switch (errorCode.toUpperCase()) {
            case "XSS_DETECTED":
                return SecurityEventType.XSS_ATTACK_ATTEMPT;
            case "SQL_INJECTION_DETECTED":
                return SecurityEventType.SQL_INJECTION_ATTEMPT;
            case "DANGEROUS_EXTENSION":
                return SecurityEventType.MALICIOUS_FILE_UPLOAD;
            case "ILLEGAL_CHARACTERS":
            case "LENGTH_EXCEEDED":
            case "INVALID_FORMAT":
                return SecurityEventType.DANGEROUS_INPUT_CONTENT;
            default:
                return SecurityEventType.INPUT_VALIDATION_FAILURE;
        }
    }
}