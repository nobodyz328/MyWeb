package com.myweb.website_core.common.util;

import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 安全的动态SQL构建工具类
 * <p>
 * 提供安全的动态SQL构建功能，防止SQL注入攻击：
 * 1. 参数化查询构建
 * 2. 安全的条件拼接
 * 3. 排序和分页安全处理
 * 4. 输入验证和清理
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class SafeSqlBuilder {
    

    private final SqlInjectionProtectionService sqlInjectionProtectionService;
    
    // 允许的排序字段白名单
    private static final Map<String, List<String>> ALLOWED_SORT_FIELDS = new HashMap<>();
    
    // 允许的排序方向
    private static final List<String> ALLOWED_SORT_DIRECTIONS = List.of("ASC", "DESC", "asc", "desc");
    
    // 安全的字段名模式
    private static final Pattern SAFE_FIELD_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    
    static {
        // 初始化允许的排序字段
        ALLOWED_SORT_FIELDS.put("users", List.of(
            "id", "username", "email", "created_at", "liked_count"
        ));
        ALLOWED_SORT_FIELDS.put("posts", List.of(
            "id", "title", "created_at", "like_count", "collect_count", "comment_count"
        ));
        ALLOWED_SORT_FIELDS.put("comments", List.of(
            "id", "created_at", "like_count"
        ));
        ALLOWED_SORT_FIELDS.put("audit_logs", List.of(
            "id", "timestamp", "operation", "result", "username", "user_id", 
            "ip_address", "session_id", "resource_type", "resource_id", 
            "description", "risk_level", "tags", "details", "execution_time"
        ));
    }
    
    /**
     * 构建安全的WHERE条件
     * 
     * @param conditions 条件映射
     * @return 安全的WHERE子句
     */
    public String buildSafeWhereClause(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        
        StringBuilder whereClause = new StringBuilder(" WHERE ");
        List<String> conditionParts = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            
            // 验证字段名安全性
            if (!isSafeFieldName(field)) {
                throw new IllegalArgumentException("Unsafe field name: " + field);
            }
            
            // 验证值的安全性
            if (value instanceof String) {
                sqlInjectionProtectionService.validateAndSanitizeInput(
                    (String) value, "WHERE_CONDITION", field
                );
            }
            
            // 构建参数化条件
            if (value == null) {
                conditionParts.add(field + " IS NULL");
            } else if (value instanceof String && ((String) value).contains("%")) {
                conditionParts.add(field + " LIKE #{" + field + "}");
            } else {
                conditionParts.add(field + " = #{" + field + "}");
            }
        }
        
        whereClause.append(String.join(" AND ", conditionParts));
        return whereClause.toString();
    }
    
    /**
     * 构建安全的ORDER BY子句
     * 
     * @param tableName 表名
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return 安全的ORDER BY子句
     */
    public String buildSafeOrderByClause(String tableName, String sortField, String sortDirection) {
        if (sortField == null || sortField.trim().isEmpty()) {
            return "";
        }
        
        // 验证表名
        if (!isSafeFieldName(tableName)) {
            throw new IllegalArgumentException("Unsafe table name: " + tableName);
        }
        
        // 验证排序字段是否在白名单中
        List<String> allowedFields = ALLOWED_SORT_FIELDS.get(tableName);
        if (allowedFields == null || !allowedFields.contains(sortField)) {
            throw new IllegalArgumentException(
                String.format("Sort field '%s' not allowed for table '%s'", sortField, tableName)
            );
        }
        
        // 验证排序方向
        String direction = sortDirection != null ? sortDirection.toUpperCase() : "ASC";
        if (!ALLOWED_SORT_DIRECTIONS.contains(direction)) {
            throw new IllegalArgumentException("Invalid sort direction: " + sortDirection);
        }
        
        return String.format(" ORDER BY %s %s", sortField, direction);
    }
    
    /**
     * 构建安全的LIMIT子句
     * 
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 安全的LIMIT子句
     */
    public String buildSafeLimitClause(Integer limit, Integer offset) {
        StringBuilder limitClause = new StringBuilder();
        
        if (limit != null) {
            // 验证limit值的合理性
            if (limit < 0 || limit > 1000) {
                throw new IllegalArgumentException("Invalid limit value: " + limit);
            }
            limitClause.append(" LIMIT ").append(limit);
        }
        
        if (offset != null) {
            // 验证offset值的合理性
            if (offset < 0) {
                throw new IllegalArgumentException("Invalid offset value: " + offset);
            }
            limitClause.append(" OFFSET ").append(offset);
        }
        
        return limitClause.toString();
    }
    
    /**
     * 构建安全的搜索条件
     * 
     * @param searchFields 搜索字段列表
     * @param keyword 搜索关键词
     * @param tableName 表名（用于字段验证）
     * @return 安全的搜索条件
     */
    public String buildSafeSearchCondition(List<String> searchFields, String keyword, String tableName) {
        if (searchFields == null || searchFields.isEmpty() || keyword == null || keyword.trim().isEmpty()) {
            return "";
        }
        
        // 验证搜索关键词
        sqlInjectionProtectionService.validateAndSanitizeInput(keyword, "SEARCH", "keyword");
        
        // 验证搜索字段
        List<String> allowedFields = ALLOWED_SORT_FIELDS.get(tableName);
        for (String field : searchFields) {
            if (allowedFields == null || !allowedFields.contains(field)) {
                throw new IllegalArgumentException(
                    String.format("Search field '%s' not allowed for table '%s'", field, tableName)
                );
            }
        }
        
        // 构建搜索条件
        List<String> searchConditions = new ArrayList<>();
        for (String field : searchFields) {
            searchConditions.add(field + " LIKE CONCAT('%', #{keyword}, '%')");
        }
        
        return "(" + String.join(" OR ", searchConditions) + ")";
    }
    
    /**
     * 构建安全的JOIN条件
     * 
     * @param joinType JOIN类型
     * @param tableName 表名
     * @param alias 表别名
     * @param onCondition ON条件
     * @return 安全的JOIN子句
     */
    public String buildSafeJoinClause(String joinType, String tableName, String alias, String onCondition) {
        // 验证JOIN类型
        List<String> allowedJoinTypes = List.of("INNER", "LEFT", "RIGHT", "FULL");
        if (!allowedJoinTypes.contains(joinType.toUpperCase())) {
            throw new IllegalArgumentException("Invalid join type: " + joinType);
        }
        
        // 验证表名和别名
        if (!isSafeFieldName(tableName)) {
            throw new IllegalArgumentException("Unsafe table name: " + tableName);
        }
        
        if (alias != null && !isSafeFieldName(alias)) {
            throw new IllegalArgumentException("Unsafe table alias: " + alias);
        }
        
        // 验证ON条件
        if (onCondition != null) {
            sqlInjectionProtectionService.validateAndSanitizeInput(onCondition, "JOIN_CONDITION", "onCondition");
        }
        
        StringBuilder joinClause = new StringBuilder();
        joinClause.append(" ").append(joinType.toUpperCase()).append(" JOIN ").append(tableName);
        
        if (alias != null) {
            joinClause.append(" ").append(alias);
        }
        
        if (onCondition != null) {
            joinClause.append(" ON ").append(onCondition);
        }
        
        return joinClause.toString();
    }
    
    /**
     * 构建安全的分页查询
     * 
     * @param baseQuery 基础查询
     * @param conditions 查询条件
     * @param tableName 表名
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 完整的安全查询
     */
    public String buildSafePaginatedQuery(String baseQuery, Map<String, Object> conditions,
                                        String tableName, String sortField, String sortDirection,
                                        Integer limit, Integer offset) {
        
        // 验证基础查询
        sqlInjectionProtectionService.validateAndSanitizeInput(baseQuery, "BASE_QUERY", "baseQuery");
        
        StringBuilder query = new StringBuilder(baseQuery);
        
        // 添加WHERE条件
        String whereClause = buildSafeWhereClause(conditions);
        if (!whereClause.isEmpty()) {
            query.append(whereClause);
        }
        
        // 添加ORDER BY
        String orderByClause = buildSafeOrderByClause(tableName, sortField, sortDirection);
        if (!orderByClause.isEmpty()) {
            query.append(orderByClause);
        }
        
        // 添加LIMIT
        String limitClause = buildSafeLimitClause(limit, offset);
        if (!limitClause.isEmpty()) {
            query.append(limitClause);
        }
        
        return query.toString();
    }
    
    /**
     * 验证字段名是否安全
     * 
     * @param fieldName 字段名
     * @return 如果安全返回true
     */
    private boolean isSafeFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否符合安全的字段名模式
        if (!SAFE_FIELD_PATTERN.matcher(fieldName).matches()) {
            return false;
        }
        
        // 检查长度限制
        if (fieldName.length() > 64) {
            return false;
        }
        
        // 检查是否为SQL关键词
        List<String> sqlKeywords = List.of(
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "UNION", "WHERE", "ORDER", "GROUP", "HAVING", "LIMIT", "OFFSET"
        );
        
        return !sqlKeywords.contains(fieldName.toUpperCase());
    }
    
    /**
     * 添加允许的排序字段
     * 
     * @param tableName 表名
     * @param fields 允许的字段列表
     */
    public void addAllowedSortFields(String tableName, List<String> fields) {
        ALLOWED_SORT_FIELDS.put(tableName, new ArrayList<>(fields));
    }
    
    /**
     * 获取允许的排序字段
     * 
     * @param tableName 表名
     * @return 允许的字段列表
     */
    public List<String> getAllowedSortFields(String tableName) {
        return ALLOWED_SORT_FIELDS.getOrDefault(tableName, new ArrayList<>());
    }
}