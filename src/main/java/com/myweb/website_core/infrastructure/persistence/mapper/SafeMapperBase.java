package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.common.util.SafeSqlBuilder;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全的MyBatis Mapper基类
 * <p>
 * 提供安全的动态SQL构建方法，集成SafeSqlBuilder：
 * 1. 安全的动态查询构建
 * 2. 参数验证和清理
 * 3. 排序和分页安全处理
 * 4. SQL注入防护
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
public abstract class SafeMapperBase {
    
    @Autowired
    protected SafeSqlBuilder safeSqlBuilder;
    
    @Autowired
    protected SqlInjectionProtectionService sqlInjectionProtectionService;
    
    /**
     * 获取实体对应的表名
     * 子类必须实现此方法
     */
    protected abstract String getTableName();
    
    /**
     * 构建安全的分页查询参数
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 安全的查询参数Map
     */
    protected Map<String, Object> buildSafePaginatedParams(Map<String, Object> conditions,
                                                         String sortField, String sortDirection,
                                                         Integer limit, Integer offset) {
        
        Map<String, Object> params = new HashMap<>();
        
        // 验证并添加查询条件
        if (conditions != null && !conditions.isEmpty()) {
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // 验证字段名安全性
                validateFieldName(key);
                
                // 验证字段值安全性
                if (value instanceof String) {
                    sqlInjectionProtectionService.validateAndSanitizeInput(
                        (String) value, "MYBATIS_CONDITION", key
                    );
                }
                
                params.put(key, value);
            }
        }
        
        // 添加安全的排序参数
        if (sortField != null && !sortField.trim().isEmpty()) {
            String safeOrderBy = safeSqlBuilder.buildSafeOrderByClause(
                getTableName(), sortField, sortDirection
            );
            params.put("orderBy", safeOrderBy.trim().substring(9)); // 移除 "ORDER BY "
        }
        
        // 添加安全的分页参数
        if (limit != null || offset != null) {
            String safeLimitClause = safeSqlBuilder.buildSafeLimitClause(limit, offset);
            if (!safeLimitClause.isEmpty()) {
                params.put("limitClause", safeLimitClause.trim());
            }
        }
        
        return params;
    }
    
    /**
     * 构建安全的搜索查询参数
     * 
     * @param searchFields 搜索字段列表
     * @param keyword 搜索关键词
     * @param additionalConditions 额外查询条件
     * @return 安全的搜索参数Map
     */
    protected Map<String, Object> buildSafeSearchParams(List<String> searchFields, String keyword,
                                                       Map<String, Object> additionalConditions) {
        
        Map<String, Object> params = new HashMap<>();
        
        // 验证并添加搜索条件
        if (searchFields != null && !searchFields.isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            String safeSearchCondition = safeSqlBuilder.buildSafeSearchCondition(
                searchFields, keyword, getTableName()
            );
            params.put("searchCondition", safeSearchCondition);
            params.put("keyword", keyword);
        }
        
        // 验证并添加额外条件
        if (additionalConditions != null && !additionalConditions.isEmpty()) {
            for (Map.Entry<String, Object> entry : additionalConditions.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                validateFieldName(key);
                
                if (value instanceof String) {
                    sqlInjectionProtectionService.validateAndSanitizeInput(
                        (String) value, "MYBATIS_SEARCH", key
                    );
                }
                
                params.put(key, value);
            }
        }
        
        return params;
    }
    
    /**
     * 构建安全的WHERE条件参数
     * 
     * @param conditions 查询条件
     * @return 安全的WHERE条件参数Map
     */
    protected Map<String, Object> buildSafeWhereParams(Map<String, Object> conditions) {
        Map<String, Object> params = new HashMap<>();
        
        if (conditions != null && !conditions.isEmpty()) {
            String safeWhereClause = safeSqlBuilder.buildSafeWhereClause(conditions);
            if (!safeWhereClause.isEmpty()) {
                params.put("whereClause", safeWhereClause.trim().substring(6)); // 移除 "WHERE "
            }
            
            // 添加参数值
            params.putAll(conditions);
        }
        
        return params;
    }
    
    /**
     * 构建安全的JOIN查询参数
     * 
     * @param joinType JOIN类型
     * @param tableName 表名
     * @param alias 表别名
     * @param onCondition ON条件
     * @return 安全的JOIN参数Map
     */
    protected Map<String, Object> buildSafeJoinParams(String joinType, String tableName, 
                                                     String alias, String onCondition) {
        
        Map<String, Object> params = new HashMap<>();
        
        String safeJoinClause = safeSqlBuilder.buildSafeJoinClause(joinType, tableName, alias, onCondition);
        params.put("joinClause", safeJoinClause.trim());
        
        return params;
    }
    
    /**
     * 验证字段名安全性
     * 
     * @param fieldName 字段名
     */
    protected void validateFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        
        // 检查字段名是否在允许列表中
        List<String> allowedFields = safeSqlBuilder.getAllowedSortFields(getTableName());
        if (!allowedFields.contains(fieldName)) {
            throw new IllegalArgumentException(
                String.format("Field '%s' not allowed for table '%s'", fieldName, getTableName())
            );
        }
    }
    
    /**
     * 验证分页参数
     * 
     * @param limit 限制数量
     * @param offset 偏移量
     */
    protected void validatePaginationParams(Integer limit, Integer offset) {
        if (limit != null && (limit <= 0 || limit > 1000)) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000");
        }
        
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
    }
    
    /**
     * 记录安全查询日志
     * 
     * @param operation 操作类型
     * @param params 查询参数
     */
    protected void logSafeQuery(String operation, Map<String, Object> params) {
        if (log.isDebugEnabled()) {
            log.debug("Safe {} query executed for table: {}, params count: {}", 
                     operation, getTableName(), params.size());
        }
    }
}