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
        // 初始化允许的排序字段 - 使用JPA字段名
        ALLOWED_SORT_FIELDS.put("users", List.of(
            "id", "username", "email", "bio", "createdAt", "likedCount", "followerCount", "followCount"
        ));
        ALLOWED_SORT_FIELDS.put("posts", List.of(
            "id", "title", "content", "createdAt", "likeCount", "collectCount", "commentCount"
        ));
        ALLOWED_SORT_FIELDS.put("comments", List.of(
            "id", "content", "createdAt", "likeCount"
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
        
        // 允许通配符
        if ("*".equals(fieldName)) {
            return true;
        }
        
        // 处理带表别名的字段名（如 p.id, u.username）
        String actualFieldName = fieldName;
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            if (parts.length == 2) {
                // 验证表别名部分
                if (!isValidIdentifier(parts[0])) {
                    return false;
                }
                actualFieldName = parts[1];
            } else {
                return false; // 不允许多层嵌套
            }
        }
        
        // 检查是否符合安全的字段名模式
        if (!isValidIdentifier(actualFieldName)) {
            return false;
        }
        
        // 检查长度限制
        if (fieldName.length() > 128) { // 增加长度限制以支持表别名
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证标识符是否有效（表名、字段名、别名等）
     * 
     * @param identifier 标识符
     * @return 如果有效返回true
     */
    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否符合安全的标识符模式
        if (!SAFE_FIELD_PATTERN.matcher(identifier).matches()) {
            return false;
        }
        
        // 检查是否为SQL关键词
        List<String> sqlKeywords = List.of(
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "UNION", "WHERE", "ORDER", "GROUP", "HAVING", "LIMIT", "OFFSET",
            "FROM", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "ON", "AS"
        );
        
        return !sqlKeywords.contains(identifier.toUpperCase());
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
    
    /**
     * 构建复杂的动态查询
     * 支持多表关联、子查询、聚合函数等复杂场景
     * 
     * @param queryBuilder 查询构建器
     * @return 安全的动态查询
     */
    public String buildComplexDynamicQuery(DynamicQueryBuilder queryBuilder) {
        validateQueryBuilder(queryBuilder);
        
        StringBuilder query = new StringBuilder();
        
        // 构建SELECT子句
        query.append(buildSelectClause(queryBuilder.getSelectFields(), queryBuilder.getAggregations()));
        
        // 构建FROM子句
        query.append(" FROM ").append(queryBuilder.getMainTable());
        if (queryBuilder.getMainTableAlias() != null) {
            query.append(" ").append(queryBuilder.getMainTableAlias());
        }
        
        // 构建JOIN子句
        for (JoinClause join : queryBuilder.getJoins()) {
            query.append(buildSafeJoinClause(join.getJoinType(), join.getTableName(), 
                                           join.getAlias(), join.getOnCondition()));
        }
        
        // 构建WHERE子句
        if (!queryBuilder.getConditions().isEmpty()) {
            query.append(buildSafeWhereClause(queryBuilder.getConditions()));
        }
        
        // 构建GROUP BY子句
        if (!queryBuilder.getGroupByFields().isEmpty()) {
            query.append(buildGroupByClause(queryBuilder.getGroupByFields(), queryBuilder.getMainTable()));
        }
        
        // 构建HAVING子句
        if (!queryBuilder.getHavingConditions().isEmpty()) {
            query.append(buildHavingClause(queryBuilder.getHavingConditions()));
        }
        
        // 构建ORDER BY子句
        if (queryBuilder.getSortField() != null) {
            query.append(buildSafeOrderByClause(queryBuilder.getMainTable(), 
                                              queryBuilder.getSortField(), 
                                              queryBuilder.getSortDirection()));
        }
        
        // 构建LIMIT子句
        if (queryBuilder.getLimit() != null || queryBuilder.getOffset() != null) {
            query.append(buildSafeLimitClause(queryBuilder.getLimit(), queryBuilder.getOffset()));
        }
        
        return query.toString();
    }
    
    /**
     * 构建参数化的动态查询
     * 确保所有参数都被正确参数化，防止SQL注入
     * 
     * @param baseQuery 基础查询模板
     * @param parameters 查询参数
     * @return 参数化查询结果
     */
    public ParameterizedQuery buildParameterizedQuery(String baseQuery, Map<String, Object> parameters) {
        // 验证基础查询模板
        sqlInjectionProtectionService.validateAndSanitizeInput(baseQuery, "QUERY_TEMPLATE", "baseQuery");
        
        Map<String, Object> safeParameters = new HashMap<>();
        StringBuilder processedQuery = new StringBuilder(baseQuery);
        
        // 处理参数化
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            
            // 验证参数名安全性
            if (!isSafeFieldName(paramName)) {
                throw new IllegalArgumentException("Unsafe parameter name: " + paramName);
            }
            
            // 验证参数值
            if (paramValue instanceof String) {
                sqlInjectionProtectionService.validateAndSanitizeInput(
                    (String) paramValue, "PARAMETER", paramName);
            }
            
            safeParameters.put(paramName, paramValue);
        }
        
        return new ParameterizedQuery(processedQuery.toString(), safeParameters);
    }
    
    /**
     * 构建安全的子查询
     * 
     * @param subQuery 子查询内容
     * @param alias 子查询别名
     * @return 安全的子查询字符串
     */
    public String buildSafeSubQuery(String subQuery, String alias) {
        // 验证子查询
        sqlInjectionProtectionService.validateAndSanitizeInput(subQuery, "SUBQUERY", "subQuery");
        
        // 验证别名
        if (alias != null && !isSafeFieldName(alias)) {
            throw new IllegalArgumentException("Unsafe subquery alias: " + alias);
        }
        
        StringBuilder result = new StringBuilder("(");
        result.append(subQuery);
        result.append(")");
        
        if (alias != null) {
            result.append(" AS ").append(alias);
        }
        
        return result.toString();
    }
    
    /**
     * 构建安全的UNION查询
     * 
     * @param queries 要联合的查询列表
     * @param unionType UNION类型（UNION或UNION ALL）
     * @return 安全的UNION查询
     */
    public String buildSafeUnionQuery(List<String> queries, String unionType) {
        if (queries == null || queries.isEmpty()) {
            throw new IllegalArgumentException("Union queries cannot be empty");
        }
        
        // 验证UNION类型
        if (!"UNION".equalsIgnoreCase(unionType) && !"UNION ALL".equalsIgnoreCase(unionType)) {
            throw new IllegalArgumentException("Invalid union type: " + unionType);
        }
        
        // 验证每个查询
        for (String query : queries) {
            sqlInjectionProtectionService.validateAndSanitizeInput(query, "UNION_QUERY", "query");
        }
        
        return String.join(" " + unionType.toUpperCase() + " ", queries);
    }
    
    /**
     * 构建SELECT子句
     */
    private String buildSelectClause(List<String> selectFields, List<String> aggregations) {
        StringBuilder select = new StringBuilder("SELECT ");
        List<String> allFields = new ArrayList<>();
        
        // 添加普通字段
        if (selectFields != null && !selectFields.isEmpty()) {
            for (String field : selectFields) {
                if (!isSafeFieldName(field)) {
                    throw new IllegalArgumentException("Unsafe select field: " + field);
                }
                allFields.add(field);
            }
        }
        
        // 添加聚合函数
        if (aggregations != null && !aggregations.isEmpty()) {
            for (String aggregation : aggregations) {
                validateAggregationFunction(aggregation);
                allFields.add(aggregation);
            }
        }
        
        if (allFields.isEmpty()) {
            allFields.add("*");
        }
        
        select.append(String.join(", ", allFields));
        return select.toString();
    }
    
    /**
     * 构建GROUP BY子句
     */
    private String buildGroupByClause(List<String> groupByFields, String tableName) {
        if (groupByFields == null || groupByFields.isEmpty()) {
            return "";
        }
        
        // 验证GROUP BY字段
        List<String> allowedFields = ALLOWED_SORT_FIELDS.get(tableName);
        for (String field : groupByFields) {
            if (!isSafeFieldName(field)) {
                throw new IllegalArgumentException("Unsafe GROUP BY field: " + field);
            }
            
            // 对于GROUP BY字段，需要更灵活的验证
            String actualField = field;
            if (field.contains(".")) {
                actualField = field.split("\\.")[1]; // 获取字段名部分
            }
            
            // 如果有允许字段列表，检查字段是否在列表中
            if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(actualField)) {
                // 对于一些常见的GROUP BY字段，给予特殊处理
                List<String> commonGroupByFields = List.of("author_id", "user_id", "category_id", "status", "created_at", "updated_at");
                if (!commonGroupByFields.contains(actualField)) {
                    throw new IllegalArgumentException("GROUP BY field not allowed: " + field);
                }
            }
        }
        
        return " GROUP BY " + String.join(", ", groupByFields);
    }
    
    /**
     * 构建HAVING子句
     */
    private String buildHavingClause(Map<String, Object> havingConditions) {
        if (havingConditions == null || havingConditions.isEmpty()) {
            return "";
        }
        
        StringBuilder having = new StringBuilder(" HAVING ");
        List<String> conditions = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : havingConditions.entrySet()) {
            String condition = entry.getKey();
            Object value = entry.getValue();
            
            // 验证HAVING条件
            validateAggregationFunction(condition);
            
            if (value instanceof String) {
                sqlInjectionProtectionService.validateAndSanitizeInput(
                    (String) value, "HAVING_CONDITION", condition);
            }
            
            // 为HAVING条件生成安全的参数名
            String paramName = "HAVING_" + condition.replaceAll("[^a-zA-Z0-9_]", "_");
            conditions.add(condition + " = #{" + paramName + "}");
        }
        
        having.append(String.join(" AND ", conditions));
        return having.toString();
    }
    
    /**
     * 验证聚合函数
     */
    private void validateAggregationFunction(String aggregation) {
        if (aggregation == null || aggregation.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregation function cannot be empty");
        }
        
        // 允许的聚合函数
        List<String> allowedFunctions = List.of("COUNT", "SUM", "AVG", "MAX", "MIN", "GROUP_CONCAT");
        
        String upperAggregation = aggregation.toUpperCase();
        boolean isValidFunction = allowedFunctions.stream()
            .anyMatch(func -> upperAggregation.startsWith(func + "("));
        
        if (!isValidFunction) {
            throw new IllegalArgumentException("Invalid aggregation function: " + aggregation);
        }
    }
    
    /**
     * 验证查询构建器
     */
    private void validateQueryBuilder(DynamicQueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException("Query builder cannot be null");
        }
        
        if (queryBuilder.getMainTable() == null || queryBuilder.getMainTable().trim().isEmpty()) {
            throw new IllegalArgumentException("Main table cannot be empty");
        }
        
        if (!isSafeFieldName(queryBuilder.getMainTable())) {
            throw new IllegalArgumentException("Unsafe main table name: " + queryBuilder.getMainTable());
        }
    }
    
    /**
     * 动态查询构建器类
     */
    public static class DynamicQueryBuilder {
        private String mainTable;
        private String mainTableAlias;
        private List<String> selectFields = new ArrayList<>();
        private List<String> aggregations = new ArrayList<>();
        private List<JoinClause> joins = new ArrayList<>();
        private Map<String, Object> conditions = new HashMap<>();
        private List<String> groupByFields = new ArrayList<>();
        private Map<String, Object> havingConditions = new HashMap<>();
        private String sortField;
        private String sortDirection;
        private Integer limit;
        private Integer offset;
        
        public DynamicQueryBuilder(String mainTable) {
            this.mainTable = mainTable;
        }
        
        public DynamicQueryBuilder alias(String alias) {
            this.mainTableAlias = alias;
            return this;
        }
        
        public DynamicQueryBuilder select(String... fields) {
            this.selectFields.addAll(List.of(fields));
            return this;
        }
        
        public DynamicQueryBuilder aggregate(String... aggregations) {
            this.aggregations.addAll(List.of(aggregations));
            return this;
        }
        
        public DynamicQueryBuilder join(String joinType, String tableName, String alias, String onCondition) {
            this.joins.add(new JoinClause(joinType, tableName, alias, onCondition));
            return this;
        }
        
        public DynamicQueryBuilder where(String field, Object value) {
            this.conditions.put(field, value);
            return this;
        }
        
        public DynamicQueryBuilder where(Map<String, Object> conditions) {
            this.conditions.putAll(conditions);
            return this;
        }
        
        public DynamicQueryBuilder groupBy(String... fields) {
            this.groupByFields.addAll(List.of(fields));
            return this;
        }
        
        public DynamicQueryBuilder having(String condition, Object value) {
            this.havingConditions.put(condition, value);
            return this;
        }
        
        public DynamicQueryBuilder orderBy(String field, String direction) {
            this.sortField = field;
            this.sortDirection = direction;
            return this;
        }
        
        public DynamicQueryBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        public DynamicQueryBuilder offset(int offset) {
            this.offset = offset;
            return this;
        }
        
        // Getters
        public String getMainTable() { return mainTable; }
        public String getMainTableAlias() { return mainTableAlias; }
        public List<String> getSelectFields() { return selectFields; }
        public List<String> getAggregations() { return aggregations; }
        public List<JoinClause> getJoins() { return joins; }
        public Map<String, Object> getConditions() { return conditions; }
        public List<String> getGroupByFields() { return groupByFields; }
        public Map<String, Object> getHavingConditions() { return havingConditions; }
        public String getSortField() { return sortField; }
        public String getSortDirection() { return sortDirection; }
        public Integer getLimit() { return limit; }
        public Integer getOffset() { return offset; }
    }
    
    /**
     * JOIN子句类
     */
    public static class JoinClause {
        private final String joinType;
        private final String tableName;
        private final String alias;
        private final String onCondition;
        
        public JoinClause(String joinType, String tableName, String alias, String onCondition) {
            this.joinType = joinType;
            this.tableName = tableName;
            this.alias = alias;
            this.onCondition = onCondition;
        }
        
        public String getJoinType() { return joinType; }
        public String getTableName() { return tableName; }
        public String getAlias() { return alias; }
        public String getOnCondition() { return onCondition; }
    }
    
    /**
     * 参数化查询结果类
     */
    public static class ParameterizedQuery {
        private final String query;
        private final Map<String, Object> parameters;
        
        public ParameterizedQuery(String query, Map<String, Object> parameters) {
            this.query = query;
            this.parameters = parameters;
        }
        
        public String getQuery() { return query; }
        public Map<String, Object> getParameters() { return parameters; }
    }
}