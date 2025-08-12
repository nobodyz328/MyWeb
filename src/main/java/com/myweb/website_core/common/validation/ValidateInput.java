package com.myweb.website_core.common.validation;

import java.lang.annotation.*;

/**
 * 输入验证注解
 * <p>
 * 用于标记需要进行输入验证的方法，支持自动验证方法参数。
 * 该注解配合SecurityValidationAspect切面使用，提供统一的输入验证功能。
 * <p>
 * 主要功能：
 * 1. 自动验证方法参数
 * 2. 支持指定字段名称
 * 3. 支持配置最大长度
 * 4. 记录验证失败的审计日志
 * <p>
 * 输入验证服务业务集成
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidateInput {
    
    /**
     * 字段名称数组，用于指定每个参数的字段名
     * 数组索引对应方法参数的索引
     * 如果未指定，将使用默认的字段名格式：field0, field1, ...
     */
    String[] fieldNames() default {};
    
    /**
     * 最大长度限制
     * 适用于字符串类型的参数
     */
    int maxLength() default 50000;
    
    /**
     * 是否验证空值
     * true: 允许空值，不进行验证
     * false: 空值也会触发验证失败
     */
    boolean allowEmpty() default true;
    
    /**
     * 是否检查XSS攻击
     */
    boolean checkXss() default true;
    
    /**
     * 是否检查SQL注入
     */
    boolean checkSqlInjection() default true;
    
    /**
     * 验证类型，用于指定特定的验证规则
     * 支持的类型：
     * - "default": 默认验证
     * - "username": 用户名验证
     * - "email": 邮箱验证
     * - "password": 密码验证
     * - "title": 标题验证
     * - "content": 内容验证
     * - "comment": 评论验证
     * - "filename": 文件名验证
     * - "url": URL验证
     */
    String[] validationTypes() default {"default"};
    
    /**
     * 是否记录验证失败的审计日志
     */
    boolean auditFailures() default true;
    
    /**
     * 验证失败时的错误消息模板
     * 可以使用占位符：{fieldName}, {maxLength}
     */
    String errorMessage() default "输入验证失败：{fieldName}";
}