package com.myweb.website_core.application.service.security.audit;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SQL安全审计适配器
 * 
 * 专门用于SQL注入防护相关的审计日志记录，
 * 适配现有的审计服务体系，避免重复代码
 * 
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Service
public class SqlSecurityAuditAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlSecurityAuditAdapter.class);
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private SecurityEventService securityEventService;
    
    /**
     * 记录SQL安全事件
     * 
     * @param eventType 事件类型
     * @param description 事件描述
     * @param details 事件详情
     * @param severity 严重级别
     */
    public void logSecurityEvent(String eventType, String description, String details, String severity) {
        try {
            // 简化版本，直接记录日志而不依赖复杂的DTO构建
            logger.info("SQL Security Event - Type: {}, Description: {}, Details: {}, Severity: {}", 
                       eventType, description, truncateInput(details), severity);
            
        } catch (Exception e) {
            logger.error("Failed to log SQL security event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录数据库操作审计
     * 
     * @param operation 操作类型
     * @param sqlStatement SQL语句标识
     * @param parameters 参数信息
     * @param result 执行结果
     */
    public void logDatabaseOperation(String operation, String sqlStatement, String parameters, String result) {
        try {
            // 简化版本，直接记录日志
            logger.info("Database Operation - Type: {}, Statement: {}, Parameters: {}, Result: {}", 
                       operation, truncateInput(sqlStatement), truncateInput(parameters), result);
            
        } catch (Exception e) {
            logger.error("Failed to log database operation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录SQL注入检查操作
     * 
     * @param input 输入内容
     * @param context 检查上下文
     * @param fieldName 字段名称
     * @param result 检查结果
     */
    public void logSqlInjectionCheck(String input, String context, String fieldName, String result) {
        try {
            // 简化版本，直接记录日志
            logger.info("SQL Injection Check - Field: {}, Context: {}, Input: {}, Result: {}", 
                       fieldName, context, truncateInput(input), result);
            
        } catch (Exception e) {
            logger.error("Failed to log SQL injection check: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录MyBatis语句验证
     * 
     * @param sqlId SQL语句ID
     * @param validationResult 验证结果
     * @param issues 发现的问题
     */
    public void logMybatisValidation(String sqlId, boolean validationResult, String issues) {
        try {
            String result = validationResult ? "PASSED" : "FAILED";
            String description = String.format("MyBatis statement validation: %s", sqlId);
            
            if (!validationResult && issues != null) {
                description += " - Issues: " + issues;
            }
            
            // 简化版本，直接记录日志
            logger.info("MyBatis Validation - SQL ID: {}, Result: {}, Issues: {}", 
                       sqlId, result, issues);
            
            // 如果验证失败，同时记录为安全事件
            if (!validationResult) {
                logSecurityEvent(
                    "MYBATIS_VALIDATION_FAILED",
                    "MyBatis statement validation failed: " + sqlId,
                    issues,
                    "HIGH"
                );
            }
            
        } catch (Exception e) {
            logger.error("Failed to log MyBatis validation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录动态SQL构建
     * 
     * @param baseQuery 基础查询
     * @param parameters 参数
     * @param result 构建结果
     */
    public void logDynamicSqlBuild(String baseQuery, String parameters, String result) {
        try {
            AuditLogRequest request = AuditLogRequest.builder()
                .operation(AuditOperation.DATABASE_OPERATION)
                .resourceType("DYNAMIC_SQL_BUILD")
                .description("Dynamic SQL query construction")
                .requestData(truncateInput(baseQuery))
                .responseData(parameters)
                .result(result)
                .timestamp(LocalDateTime.now())
                .build();
            
            auditLogService.logOperation(request);
            
        } catch (Exception e) {
            logger.error("Failed to log dynamic SQL build: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将事件类型映射到SecurityEventType枚举
     */
    private SecurityEventType mapToSecurityEventType(String eventType) {
        switch (eventType) {
            case "SQL_INJECTION_ATTEMPT":
            case "SQL_INJECTION_BLOCKED":
                return SecurityEventType.SQL_INJECTION_ATTEMPT;
            case "SQL_SECURITY_VIOLATION":
            case "DANGEROUS_DYNAMIC_SQL":
                return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
            case "NON_PARAMETERIZED_QUERY":
                return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
            case "SLOW_QUERY_DETECTED":
                return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
            case "MYBATIS_VALIDATION_FAILED":
                return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
            default:
                return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
        }
    }
    
    /**
     * 截断输入内容以避免日志过长
     */
    private String truncateInput(String input) {
        if (input == null) {
            return null;
        }
        
        final int MAX_LENGTH = 500;
        if (input.length() <= MAX_LENGTH) {
            return input;
        }
        
        return input.substring(0, MAX_LENGTH) + "... (truncated)";
    }
    
    /**
     * 获取当前请求的IP地址
     * 这里简化处理，实际应该从RequestContext获取
     */
    private String getCurrentIpAddress() {
        // TODO: 从当前请求上下文获取真实IP地址
        return "127.0.0.1";
    }
    
    /**
     * 获取当前请求的User-Agent
     * 这里简化处理，实际应该从RequestContext获取
     */
    private String getCurrentUserAgent() {
        // TODO: 从当前请求上下文获取User-Agent
        return "SQL-Security-Service";
    }
}