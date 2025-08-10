package com.myweb.website_core.application.service.security.IPS.virusprotect;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL注入防护服务
 * <p>
 * 提供SQL注入检测和阻断机制，包括：
 * 1. SQL注入模式检测
 * 2. 危险SQL语句识别
 * 3. 动态SQL构建安全检查
 * 4. 数据库操作审计日志
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Service
public class SqlInjectionProtectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlInjectionProtectionService.class);
    
    // SQL注入攻击关键词 - 扩展版本
    private static final List<String> SQL_INJECTION_KEYWORDS = Arrays.asList(
        // 基本SQL命令
        "union", "select", "insert", "update", "delete", "drop", "create", "alter", "truncate",
        "exec", "execute", "sp_", "xp_", "fn_", "sys", "information_schema",
        
        // 注释符号
        "--", "/*", "*/", "#",
        
        // 函数和操作符
        "@@", "char(", "nchar(", "varchar(", "nvarchar(", "cast(", "convert(",
        "ascii(", "substring(", "len(", "length(", "concat(", "concat_ws(",
        "sleep(", "benchmark(", "waitfor", "delay", "pg_sleep(",
        
        // 系统表和视图
        "sysobjects", "syscolumns", "sysusers", "systables", "pg_tables",
        "pg_user", "pg_database", "mysql.user", "information_schema.tables",
        "information_schema.columns", "master", "msdb", "tempdb", "model",
        
        // 文件操作
        "load_file(", "into outfile", "into dumpfile", "load data infile",
        "bulk insert", "openrowset", "opendatasource",
        
        // 条件注入
        "union all select", "union select", "order by", "group by", "having",
        "limit", "offset", "top", "distinct",
        
        // 逻辑操作
        "and", "or", "not", "xor", "like", "rlike", "regexp", "between", "in",
        
        // 数据库特定函数
        "version(", "user(", "database(", "schema(", "current_user",
        "session_user", "system_user", "current_database()", "current_schema()"
    );
    
    // 危险SQL模式 - 正则表达式
    private static final List<Pattern> DANGEROUS_SQL_PATTERNS = Arrays.asList(
        // Union注入
        Pattern.compile("\\bunion\\s+(all\\s+)?select\\b", Pattern.CASE_INSENSITIVE),
        
        // 条件注入
        Pattern.compile("\\s+(or|and)\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\s+(or|and)\\s+'[^']*'\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\s+(or|and)\\s+\\w+\\s*=\\s*\\w+", Pattern.CASE_INSENSITIVE),
        
        // 注释注入
        Pattern.compile("'\\s*--", Pattern.CASE_INSENSITIVE),
        Pattern.compile("'\\s*/\\*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("'\\s*#", Pattern.CASE_INSENSITIVE),
        
        // 函数注入
        Pattern.compile("\\b(sleep|benchmark|waitfor|pg_sleep)\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(load_file|into\\s+outfile|into\\s+dumpfile)\\b", Pattern.CASE_INSENSITIVE),
        
        // 系统信息获取
        Pattern.compile("\\b(@@version|@@user|@@database|user\\(\\)|version\\(\\)|database\\(\\))", Pattern.CASE_INSENSITIVE),
        
        // 堆叠查询
        Pattern.compile(";\\s*(select|insert|update|delete|drop|create|alter)", Pattern.CASE_INSENSITIVE),
        
        // 盲注
        Pattern.compile("\\b(ascii|substring|length|len)\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // 错误注入
        Pattern.compile("\\b(cast|convert)\\s*\\(.*\\bas\\s+", Pattern.CASE_INSENSITIVE)
    );
    
    // 允许的安全SQL关键词（在特定上下文中）
    private static final List<String> SAFE_SQL_KEYWORDS = Arrays.asList(
        "order", "by", "asc", "desc", "limit", "offset", "group", "having", "distinct"
    );
    
    /**
     * 检测输入是否包含SQL注入攻击
     * 
     * @param input 用户输入
     * @param context 输入上下文（如：search, filter, sort等）
     * @return 如果检测到SQL注入返回true
     */
    @Auditable(
        operation = AuditOperation.SQL_INJECTION_CHECK,
        resourceType = "SQL_SECURITY",
        description = "SQL注入检测",
        riskLevel = 3,
        tags = "security,sql_injection"
    )
    public boolean detectSqlInjection(String input, String context) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String normalizedInput = input.toLowerCase().trim();
        
        // 1. 检查SQL注入关键词
        for (String keyword : SQL_INJECTION_KEYWORDS) {
            if (normalizedInput.contains(keyword)) {
                // 检查是否在安全上下文中
                if (!isSafeInContext(keyword, context)) {
                    logger.warn("SQL injection keyword detected - Context: {}, Keyword: {}, Input: {}", 
                               context, keyword, input);
                    return true;
                }
            }
        }
        
        // 2. 检查危险SQL模式
        for (Pattern pattern : DANGEROUS_SQL_PATTERNS) {
            if (pattern.matcher(normalizedInput).find()) {
                logger.warn("SQL injection pattern detected - Context: {}, Pattern: {}, Input: {}", 
                           context, pattern.pattern(), input);
                return true;
            }
        }
        
        // 3. 检查特殊字符组合
        if (containsDangerousCharacterCombinations(normalizedInput)) {
            logger.warn("Dangerous character combinations detected - Context: {}, Input: {}", 
                       context, input);
            return true;
        }
        
        return false;
    }
    
    /**
     * 验证并清理用户输入，防止SQL注入
     * 
     * @param input 用户输入
     * @param context 输入上下文
     * @param fieldName 字段名称
     * @throws ValidationException 如果检测到SQL注入
     */
    @Auditable(
        operation = AuditOperation.SQL_INJECTION_CHECK,
        resourceType = "INPUT_VALIDATION",
        description = "用户输入SQL注入验证",
        riskLevel = 4,
        tags = "security,validation,sql_injection",
        sensitiveParams = {0} // 第一个参数（input）是敏感参数
    )
    public void validateAndSanitizeInput(String input, String context, String fieldName) {
        if (detectSqlInjection(input, context)) {
            logger.error("SQL injection attempt blocked - Field: {}, Context: {}, Input: {}", 
                        fieldName, context, input);
            
            throw new ValidationException(
                String.format("%s包含潜在的SQL注入代码", fieldName),
                fieldName,
                "SQL_INJECTION_DETECTED"
            );
        }
    }
    
    /**
     * 构建安全的动态SQL查询
     * 
     * @param baseQuery 基础查询
     * @param parameters 参数映射
     * @return 安全的SQL查询
     */
    @Auditable(
        operation = AuditOperation.DATABASE_OPERATION,
        resourceType = "DYNAMIC_SQL",
        description = "构建安全的动态SQL查询",
        riskLevel = 3,
        tags = "security,sql,dynamic_query",
        sensitiveParams = {0} // baseQuery参数敏感
    )
    public String buildSafeDynamicQuery(String baseQuery, java.util.Map<String, Object> parameters) {
        // 验证基础查询
        if (detectSqlInjection(baseQuery, "DYNAMIC_QUERY")) {
            throw new IllegalArgumentException("Base query contains potential SQL injection");
        }
        
        // 验证所有参数
        for (java.util.Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String stringValue = (String) value;
                if (detectSqlInjection(stringValue, "PARAMETER")) {
                    throw new IllegalArgumentException(
                        String.format("Parameter '%s' contains potential SQL injection", key)
                    );
                }
            }
        }
        
        logger.debug("Dynamic SQL query built successfully - Parameters count: {}", parameters.size());
        
        return baseQuery;
    }
    
    /**
     * 验证MyBatis映射文件中的SQL语句
     * 
     * @param sqlStatement SQL语句
     * @param mapperId 映射器ID
     * @return 验证结果
     */
    @Auditable(
        operation = AuditOperation.DATABASE_OPERATION,
        resourceType = "MYBATIS_VALIDATION",
        description = "MyBatis SQL语句安全验证",
        riskLevel = 4,
        tags = "security,mybatis,sql_validation",
        sensitiveParams = {0} // sqlStatement参数敏感
    )
    public boolean validateMybatisStatement(String sqlStatement, String mapperId) {
        // 检查是否使用了参数化查询
        StringBuilder issues = new StringBuilder();
        boolean isValid = true;

        if (!isParameterizedQuery(sqlStatement)) {
            logger.warn("Non-parameterized query detected in mapper: {}", mapperId);
            issues.append("Non-parameterized query detected; ");
            isValid = false;
        }
        
        // 检查是否包含危险的动态SQL
        if (containsDangerousDynamicSql(sqlStatement)) {
            logger.error("Dangerous dynamic SQL detected in mapper: {}", mapperId);
            issues.append("Dangerous dynamic SQL patterns detected; ");
            isValid = false;
        }
        
        if (!isValid) {
            logger.error("MyBatis validation failed - Mapper: {}, Issues: {}", mapperId, issues.toString());
        } else {
            logger.debug("MyBatis validation passed - Mapper: {}", mapperId);
        }
        
        return isValid;
    }
    
    /**
     * 检查是否在安全上下文中
     */
    private boolean isSafeInContext(String keyword, String context) {
        if ("SORT".equals(context) || "ORDER".equals(context)) {
            return SAFE_SQL_KEYWORDS.contains(keyword);
        }
        return false;
    }
    
    /**
     * 检查是否包含危险字符组合
     */
    private boolean containsDangerousCharacterCombinations(String input) {
        // 检查引号和分号组合
        if (input.matches(".*'.*;.*")) {
            return true;
        }
        
        // 检查引号和注释组合
        if (input.matches(".*'\\s*(--|/\\*|#).*")) {
            return true;
        }
        
        // 检查多个连续的特殊字符
        if (input.matches(".*[';\"\\-]{3,}.*")) {
            return true;
        }
        
        // 检查十六进制编码
        if (input.matches(".*0x[0-9a-f]+.*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否为参数化查询
     */
    private boolean isParameterizedQuery(String sql) {
        // MyBatis参数化查询使用 #{}
        return sql.contains("#{") && !sql.contains("${");
    }
    
    /**
     * 检查是否包含危险的动态SQL
     */
    private boolean containsDangerousDynamicSql(String sql) {
        // 检查是否使用了不安全的字符串拼接
        if (sql.contains("${")) {
            return true;
        }
        
        // 检查是否包含直接的SQL注入风险
        String lowerSql = sql.toLowerCase();
        for (Pattern pattern : DANGEROUS_SQL_PATTERNS) {
            if (pattern.matcher(lowerSql).find()) {
                return true;
            }
        }
        
        return false;
    }
    

}