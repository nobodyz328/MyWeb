package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.util.SafeSqlBuilder;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
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
    private final AuditLogMapperService auditLogMapperService;
    private final AuditMessageService auditMessageService;
    
    /**
     * 安全的审计日志分页查询（使用JPA）
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @return 分页结果
     */
    public Page<AuditLog> findAuditLogsSafely(Map<String, Object> conditions, 
                                            String sortField, String sortDirection,
                                            Pageable page) {
        
        log.info("Executing safe audit logs query - JPA approach");
        
        try {
            // 使用安全的Repository进行查询
            return auditLogMapperService.findSafePaginated(
                conditions, sortField, sortDirection, page
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
            return auditLogMapperService.findSafeSearch(
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
    
    /**
     * 构建复杂的动态查询
     * 使用SafeSqlBuilder的DynamicQueryBuilder进行安全的复杂查询构建
     * 
     * @param queryBuilder 查询构建器
     * @return 安全的动态查询字符串
     */
    public String buildComplexDynamicQuery(SafeSqlBuilder.DynamicQueryBuilder queryBuilder) {
        try {
            log.debug("构建复杂动态查询: mainTable={}", queryBuilder.getMainTable());
            
            // 使用SafeSqlBuilder构建查询
            String query = safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);

            
            log.debug("复杂动态查询构建成功: {}", query);
            return query;
            
        } catch (Exception e) {
            log.error("构建复杂动态查询失败: {}", e.getMessage());
            
            // 记录错误审计日志
            auditMessageService.logOperation(
                    AuditLogRequest.system(
                            AuditOperation.COMPLEX_QUERY_BUILD,
                            "构建动态查询失败: " + e.getMessage()
                    ).withResult( false)

            );
            
            throw new RuntimeException("构建复杂动态查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建参数化查询
     * 确保所有参数都被正确参数化，防止SQL注入
     * 
     * @param baseQuery 基础查询模板
     * @param parameters 查询参数
     * @return 参数化查询结果
     */
    public SafeSqlBuilder.ParameterizedQuery buildParameterizedQuery(String baseQuery, Map<String, Object> parameters) {
        try {
            log.debug("构建参数化查询: parameterCount={}", parameters != null ? parameters.size() : 0);
            
            // 验证基础查询
            boolean isValidQuery = validateUserInputSafety(baseQuery, "QUERY_TEMPLATE", "baseQuery");
            if (!isValidQuery) {
                throw new IllegalArgumentException("基础查询模板包含非法字符");
            }
            
            // 使用SafeSqlBuilder构建参数化查询
            SafeSqlBuilder.ParameterizedQuery result = safeSqlBuilder.buildParameterizedQuery(baseQuery, parameters);
            
            log.debug("参数化查询构建成功");
            return result;
            
        } catch (Exception e) {
            log.error("构建参数化查询失败: {}", e.getMessage());
            
            // 记录错误审计日志
            auditMessageService.logOperation(
                    AuditLogRequest.system(
                        AuditOperation.PARAMETERIZED_QUERY_BUILD,
                        "构建参数化查询失败: " + e.getMessage()
                    ).withResult( false)
            );
            
            throw new RuntimeException("构建参数化查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建安全的子查询
     * 
     * @param subQuery 子查询内容
     * @param alias 子查询别名
     * @return 安全的子查询字符串
     */
    public String buildSafeSubQuery(String subQuery, String alias) {
        try {
            log.debug("构建安全子查询: alias={}", alias);
            
            // 验证子查询内容
            boolean isValidSubQuery = validateUserInputSafety(subQuery, "SUBQUERY", "subQuery");
            if (!isValidSubQuery) {
                throw new IllegalArgumentException("子查询内容包含非法字符");
            }
            
            // 使用SafeSqlBuilder构建子查询
            String result = safeSqlBuilder.buildSafeSubQuery(subQuery, alias);
            
            log.debug("安全子查询构建成功");
            return result;
            
        } catch (Exception e) {
            log.error("构建安全子查询失败: {}", e.getMessage());
            throw new RuntimeException("构建安全子查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建安全的UNION查询
     * 
     * @param queries 要联合的查询列表
     * @param unionType UNION类型（UNION或UNION ALL）
     * @return 安全的UNION查询
     */
    public String buildSafeUnionQuery(List<String> queries, String unionType) {
        try {
            log.debug("构建安全UNION查询: queryCount={}, unionType={}", 
                     queries != null ? queries.size() : 0, unionType);
            
            // 验证每个查询
            if (queries != null) {
                for (int i = 0; i < queries.size(); i++) {
                    String query = queries.get(i);
                    boolean isValidQuery = validateUserInputSafety(query, "UNION_QUERY", "query" + i);
                    if (!isValidQuery) {
                        throw new IllegalArgumentException("UNION查询" + i + "包含非法字符");
                    }
                }
            }
            
            // 使用SafeSqlBuilder构建UNION查询
            String result = safeSqlBuilder.buildSafeUnionQuery(queries, unionType);
            
            log.debug("安全UNION查询构建成功");
            return result;
            
        } catch (Exception e) {
            log.error("构建安全UNION查询失败: {}", e.getMessage());
            throw new RuntimeException("构建安全UNION查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建动态查询构建器
     * 
     * @param mainTable 主表名
     * @return 动态查询构建器
     */
    public SafeSqlBuilder.DynamicQueryBuilder createQueryBuilder(String mainTable) {
        // 验证表名
        List<String> allowedTables = List.of("posts", "users", "comments", "audit_logs");
        if (!allowedTables.contains(mainTable)) {
            throw new IllegalArgumentException("不允许的表名: " + mainTable);
        }
        
        log.debug("创建动态查询构建器: mainTable={}", mainTable);
        return new SafeSqlBuilder.DynamicQueryBuilder(mainTable);
    }
    
    /**
     * 验证查询构建器的安全性
     * 
     * @param queryBuilder 查询构建器
     * @return 验证结果
     */
    public boolean validateQueryBuilder(SafeSqlBuilder.DynamicQueryBuilder queryBuilder) {
        try {
            if (queryBuilder == null) {
                return false;
            }
            
            // 验证主表
            if (queryBuilder.getMainTable() == null || queryBuilder.getMainTable().trim().isEmpty()) {
                return false;
            }
            
            // 验证选择字段
            if (queryBuilder.getSelectFields() != null) {
                List<String> allowedFields = getAllowedSortFields(queryBuilder.getMainTable());
                for (String field : queryBuilder.getSelectFields()) {
                    if (!allowedFields.contains(field) && !"*".equals(field)) {
                        log.warn("查询构建器包含不允许的字段: {}", field);
                        return false;
                    }
                }
            }
            
            // 验证排序字段
            if (queryBuilder.getSortField() != null) {
                List<String> allowedFields = getAllowedSortFields(queryBuilder.getMainTable());
                if (!allowedFields.contains(queryBuilder.getSortField())) {
                    log.warn("查询构建器包含不允许的排序字段: {}", queryBuilder.getSortField());
                    return false;
                }
            }
            
            // 验证条件值
            if (queryBuilder.getConditions() != null) {
                for (Map.Entry<String, Object> entry : queryBuilder.getConditions().entrySet()) {
                    if (entry.getValue() instanceof String) {
                        boolean isValidValue = validateUserInputSafety(
                            (String) entry.getValue(), "CONDITION", entry.getKey());
                        if (!isValidValue) {
                            log.warn("查询构建器包含不安全的条件值: {}={}", entry.getKey(), entry.getValue());
                            return false;
                        }
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("验证查询构建器失败: {}", e.getMessage());
            return false;
        }
    }
}