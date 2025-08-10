package com.myweb.website_core.infrastructure.security.audit;

import com.myweb.website_core.common.enums.AuditOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计注解
 * <p>
 * 用于标记需要记录审计日志的方法，支持：
 * - 自动记录方法调用的审计日志
 * - 记录请求参数和响应结果
 * - 统计执行时间和性能监控
 * - 处理审计过程中的异常
 * <p>
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * 审计操作类型
     * 
     * @return 审计操作类型
     */
    AuditOperation operation();
    
    /**
     * 资源类型
     * 
     * @return 资源类型，如：POST, USER, COMMENT等
     */
    String resourceType() default "";
    
    /**
     * 操作描述
     * 
     * @return 操作描述，用于补充说明操作内容
     */
    String description() default "";
    
    /**
     * 是否记录请求参数
     * 
     * @return 是否记录请求参数
     */
    boolean logRequest() default false;
    
    /**
     * 是否记录响应结果
     * 
     * @return 是否记录响应结果
     */
    boolean logResponse() default false;
    
    /**
     * 是否记录执行时间
     * 
     * @return 是否记录执行时间
     */
    boolean logExecutionTime() default true;
    
    /**
     * 风险级别（1-5）
     * 
     * @return 风险级别，0表示使用操作类型的默认风险级别
     */
    int riskLevel() default 0;
    
    /**
     * 标签
     * 
     * @return 标签，多个标签用逗号分隔
     */
    String tags() default "";
    
    /**
     * 是否忽略异常
     * 
     * @return 是否忽略审计过程中的异常，默认为true（确保不影响业务流程）
     */
    boolean ignoreAuditException() default true;
    
    /**
     * 敏感参数索引
     * 
     * @return 敏感参数的索引数组，这些参数将被脱敏处理
     */
    int[] sensitiveParams() default {};
    
    /**
     * 是否异步记录
     * 
     * @return 是否异步记录审计日志，默认为true
     */
    boolean async() default true;
    
    /**
     * 最大参数长度
     * 
     * @return 记录参数的最大长度，超过此长度将被截断，默认1000字符
     */
    int maxParamLength() default 1000;
    
    /**
     * 最大响应长度
     * 
     * @return 记录响应的最大长度，超过此长度将被截断，默认2000字符
     */
    int maxResponseLength() default 2000;
}