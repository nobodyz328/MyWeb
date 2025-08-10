package com.myweb.website_core.application.service.security;

import com.myweb.website_core.common.util.SafeSqlBuilder;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import com.myweb.website_core.infrastructure.persistence.repository.SafeAuditLogRepository;
import com.myweb.website_core.infrastructure.persistence.mapper.AuditLogMapperService;
import com.myweb.website_core.domain.security.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 安全查询服务
 * <p>
 * 演示如何使用SafeSqlBuilder和SqlInjectionProtectionService
 * 构建安全的动态SQL查询，集成JPA和MyBatis
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafeQueryService {
    
    private final SafeSqlBuilder safeSqlBuilder;
    private final SqlInjectionProtectionService sqlInjectionProtectionService;
    private final SafeAuditLogRepository safeAuditLogRepository;
    private final AuditLogMapperService auditLogMapperService;
    
    /**
     * 安全的审计日志分页查询（使用JPA）
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    public Page<AuditLog> findAuditLogsSafely(Map<String, Object> conditions, 
                                            String sortField, String sortDirection,
                                            int page, int size) {
        
        log.info("Executing safe audit logs query - JPA approach");
        
        try {
            // 使用安全的Repository进行查询
            return safeAuditLogRepository.findSafePaginated(
                conditions, sortField, sortDirection, page, size
            );
            
        } catch (Exception e) {
            log.error("Safe JPA query failed: {}", e.getMessage());
            throw new RuntimeException("Safe query execution failed", e);
        }
    }
    
    /**
     * 安全的审计日志搜索查询（使用JPA）
     * 
     * @param searchFields 搜索字段列表
     * @param keyword 搜索关键词
     * @param additionalConditions 额外查询条件
     * @param page 页码
     * @param size 页大小
     * @return 搜索结果
     */
    public Page<AuditLog> searchAuditLogsSafely(List<String> searchFields, String keyword,
                                              Map<String, Object> additionalConditions,
                                              int page, int size) {
        
        log.info("Executing safe audit logs search - JPA approach");
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            
            // 使用安全的Repository进行搜索
            return safeAuditLogRepository.findSafeSearch(
                searchFields, keyword, additionalConditions, pageable
            );
            
        } catch (Exception e) {
            log.error("Safe JPA search failed: {}", e.getMessage());
            throw new RuntimeException("Safe search execution failed", e);
        }
    }
    
    /**
     * 安全的审计日志分页查询（使用MyBatis）
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    public Page<AuditLog> findAuditLogsWithMyBatis(Map<String, Object> conditions,
                                                  String sortField, String sortDirection,
                                                  int page, int size) {
        
        log.info("Executing safe audit logs query - MyBatis approach");
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            
            // 使用安全的Mapper服务进行查询
            return auditLogMapperService.findSafePaginated(
                conditions, sortField, sortDirection, pageable
            );
            
        } catch (Exception e) {
            log.error("Safe MyBatis query failed: {}", e.getMessage());
            throw new RuntimeException("Safe MyBatis query execution failed", e);
        }
    }
    
    /**
     * 构建安全的动态SQL示例
     * 
     * @param baseQuery 基础查询
     * @param conditions 查询条件
     * @param tableName 表名
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 安全的完整SQL
     */
    public String buildSafeDynamicSqlExample(String baseQuery, Map<String, Object> conditions,
                                           String tableName, String sortField, String sortDirection,
                                           Integer limit, Integer offset) {
        
        log.info("Building safe dynamic SQL for table: {}", tableName);
        
        try {
            // 使用SafeSqlBuilder构建安全的分页查询
            String safeSql = safeSqlBuilder.buildSafePaginatedQuery(
                baseQuery, conditions, tableName, sortField, sortDirection, limit, offset
            );
            
            log.debug("Safe SQL generated: {}", safeSql);
            return safeSql;
            
        } catch (Exception e) {
            log.error("Safe SQL building failed: {}", e.getMessage());
            throw new RuntimeException("Safe SQL building failed", e);
        }
    }
    
    /**
     * 验证用户输入安全性示例
     * 
     * @param userInput 用户输入
     * @param context 输入上下文
     * @param fieldName 字段名称
     * @return 验证结果
     */
    public boolean validateUserInputSafety(String userInput, String context, String fieldName) {
        
        log.debug("Validating user input safety - Context: {}, Field: {}", context, fieldName);
        
        try {
            // 使用SqlInjectionProtectionService验证输入
            sqlInjectionProtectionService.validateAndSanitizeInput(userInput, context, fieldName);
            
            log.debug("User input validation passed");
            return true;
            
        } catch (Exception e) {
            log.warn("User input validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检测SQL注入攻击示例
     * 
     * @param input 用户输入
     * @param context 输入上下文
     * @return 是否检测到SQL注入
     */
    public boolean detectSqlInjectionAttempt(String input, String context) {
        
        log.debug("Detecting SQL injection attempt - Context: {}", context);
        
        boolean isInjection = sqlInjectionProtectionService.detectSqlInjection(input, context);
        
        if (isInjection) {
            log.warn("SQL injection attempt detected - Context: {}, Input: {}", context, input);
        } else {
            log.debug("No SQL injection detected");
        }
        
        return isInjection;
    }
    
    /**
     * 获取表的允许排序字段
     * 
     * @param tableName 表名
     * @return 允许的排序字段列表
     */
    public List<String> getAllowedSortFields(String tableName) {
        return safeSqlBuilder.getAllowedSortFields(tableName);
    }
    
    /**
     * 添加新的允许排序字段
     * 
     * @param tableName 表名
     * @param fields 字段列表
     */
    public void addAllowedSortFields(String tableName, List<String> fields) {
        safeSqlBuilder.addAllowedSortFields(tableName, fields);
        log.info("Added allowed sort fields for table {}: {}", tableName, fields);
    }
}