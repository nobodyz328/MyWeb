package com.myweb.website_core.infrastructure.security.defense;

import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * MyBatis SQL审计拦截器
 * <p>
 * 拦截所有MyBatis SQL执行，进行安全检查和审计日志记录：
 * 1. SQL注入检测
 * 2. 参数化查询验证
 * 3. 执行时间监控
 * 4. 审计日志记录
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SqlAuditInterceptor implements Interceptor {

    private final SqlInjectionProtectionService sqlInjectionProtectionService;
    //private final AuditLogService auditLogService;
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        String sqlId = mappedStatement.getId();
        String sqlCommandType = mappedStatement.getSqlCommandType().name();
        
        try {
            // 获取实际执行的SQL语句
            String sql = getSqlStatement(mappedStatement, parameter);
            
            // 进行SQL安全检查
            if (!sqlInjectionProtectionService.validateMybatisStatement(sql, sqlId)) {
//                // 记录安全违规审计日志
//                auditLogService.logSecurityEvent(
//                    AuditOperation.SQL_INJECTION_CHECK,
//                    "SQL_SECURITY",
//                    sqlId,
//                    "FAILED",
//                    "SQL security validation failed for statement: " + sqlId,
//                    4,
//                    "security,sql_injection,mybatis"
//                );
                log.error("SQL security validation failed for statement: " + sqlId);
            }
            
            // 执行SQL
            Object result = invocation.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;

            
            // 监控慢查询
            if (executionTime > 5000) { // 5秒以上的查询
                log.warn("Slow query detected: {} took {}ms", sqlId, executionTime);
                
                // 记录慢查询审计日志
//                auditLogService.logSecurityEvent(
//                    AuditOperation.DATABASE_OPERATION,
//                    "SLOW_QUERY",
//                    sqlId,
//                    "WARNING",
//                    String.format("Slow query detected: %s took %dms", sqlId, executionTime),
//                    2,
//                    "performance,slow_query,mybatis"
//                );
            }
            
            // 记录正常的SQL执行审计日志
//            if (log.isDebugEnabled()) {
//                auditLogService.logSecurityEvent(
//                    AuditOperation.DATABASE_OPERATION,
//                    "SQL_EXECUTION",
//                    sqlId,
//                    "SUCCESS",
//                    String.format("SQL executed successfully: %s (%s) in %dms", sqlId, sqlCommandType, executionTime),
//                    "database,mybatis,sql_execution"
//                );
//            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("SQL execution failed for statement: {}, error: {}", sqlId, e.getMessage());
            
            // 记录SQL执行失败审计日志
//            auditLogService.logSecurityEvent(
//                AuditOperation.DATABASE_OPERATION,
//                "SQL_EXECUTION",
//                sqlId,
//                "FAILED",
//                String.format("SQL execution failed: %s - %s (execution time: %dms)", sqlId, e.getMessage(), executionTime),
//                3,
//                "database,mybatis,sql_error"
//            );
            
            throw e;
        }
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 过xml配置拦截器参数
    }
    
    /**
     * 获取完整的SQL语句，包含参数替换
     * 
     * @param mappedStatement MyBatis映射语句
     * @param parameter 参数对象
     * @return 完整的SQL语句
     */
    private String getSqlStatement(MappedStatement mappedStatement, Object parameter) {
        try {
            BoundSql boundSql = mappedStatement.getSqlSource().getBoundSql(parameter);
            Configuration configuration = mappedStatement.getConfiguration();
            
            // 获取原始SQL
            String sql = boundSql.getSql();
            if (!StringUtils.hasText(sql)) {
                log.debug("Empty SQL found for statement: {}", mappedStatement.getId());
                return mappedStatement.getId();
            }
            
            // 获取参数映射
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            Object parameterObject = boundSql.getParameterObject();
            
            // 如果没有参数，直接返回SQL
            if (parameterMappings == null || parameterMappings.isEmpty()) {
                return formatSql(sql);
            }
            
            // 替换参数占位符
            String formattedSql = replaceParameters(sql, parameterMappings, parameterObject, configuration);
            return formatSql(formattedSql);
            
        } catch (Exception e) {
            log.debug("Failed to extract SQL statement for {}: {}", mappedStatement.getId(), e.getMessage());
            return mappedStatement.getId(); // 返回statement ID作为fallback
        }
    }
    
    /**
     * 替换SQL中的参数占位符
     * 
     * @param sql 原始SQL
     * @param parameterMappings 参数映射列表
     * @param parameterObject 参数对象
     * @param configuration MyBatis配置
     * @return 替换参数后的SQL
     */
    private String replaceParameters(String sql, List<ParameterMapping> parameterMappings, 
                                   Object parameterObject, Configuration configuration) {
        try {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            MetaObject metaObject = parameterObject == null ? null : 
                configuration.newMetaObject(parameterObject);
            
            for (ParameterMapping parameterMapping : parameterMappings) {
                String propertyName = parameterMapping.getProperty();
                Object value;
                
                if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    value = metaObject == null ? null : metaObject.getValue(propertyName);
                }
                
                // 将参数值转换为SQL字符串表示
                String parameterValue = getParameterValue(value);
                
                // 替换第一个?占位符
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(parameterValue));
            }
            
            return sql;
        } catch (Exception e) {
            log.debug("Failed to replace parameters in SQL: {}", e.getMessage());
            return sql; // 返回原始SQL
        }
    }
    
    /**
     * 将参数值转换为SQL字符串表示
     * 
     * @param obj 参数值
     * @return SQL字符串表示
     */
    private String getParameterValue(Object obj) {
        if (obj == null) {
            return "NULL";
        }
        
        if (obj instanceof String) {
            return "'" + ((String) obj).replace("'", "''") + "'";
        }
        
        if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
            return "'" + formatter.format((Date) obj) + "'";
        }
        
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        // 对于其他类型，转换为字符串并加引号
        return "'" + obj.toString().replace("'", "''") + "'";
    }
    
    /**
     * 格式化SQL语句，移除多余的空白字符
     * 
     * @param sql 原始SQL
     * @return 格式化后的SQL
     */
    private String formatSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            return sql;
        }
        
        // 移除多余的空白字符和换行符
        return sql.replaceAll("\\s+", " ").trim();
    }
}