package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.audit.SqlSecurityAuditAdapter;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * MyBatis SQL审计拦截器
 * 
 * 拦截所有MyBatis SQL执行，进行安全检查和审计日志记录：
 * 1. SQL注入检测
 * 2. 参数化查询验证
 * 3. 执行时间监控
 * 4. 审计日志记录
 * 
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SqlAuditInterceptor implements Interceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlAuditInterceptor.class);
    
    @Autowired
    private SqlInjectionProtectionService sqlInjectionProtectionService;
    
    @Autowired
    private SqlSecurityAuditAdapter auditLogService;
    
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
                logger.error("SQL security validation failed for statement: {}", sqlId);
                auditLogService.logSecurityEvent(
                    "SQL_SECURITY_VIOLATION",
                    String.format("SQL security validation failed for statement: %s", sqlId),
                    sql,
                    "HIGH"
                );
                throw new SecurityException("SQL security validation failed");
            }
            
            // 执行SQL
            Object result = invocation.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录成功的数据库操作
            auditLogService.logDatabaseOperation(
                sqlCommandType,
                sqlId,
                parameter != null ? parameter.toString() : "null",
                "SUCCESS"
            );
            
            // 监控慢查询
            if (executionTime > 5000) { // 5秒以上的查询
                logger.warn("Slow query detected: {} took {}ms", sqlId, executionTime);
                auditLogService.logSecurityEvent(
                    "SLOW_QUERY_DETECTED",
                    String.format("Slow query detected: %s took %dms", sqlId, executionTime),
                    sql,
                    "MEDIUM"
                );
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录失败的数据库操作
            auditLogService.logDatabaseOperation(
                sqlCommandType,
                sqlId,
                parameter != null ? parameter.toString() : "null",
                "FAILURE: " + e.getMessage()
            );
            
            logger.error("SQL execution failed for statement: {}, error: {}", sqlId, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 可以通过properties配置拦截器参数
    }
    
    /**
     * 获取SQL语句（简化版本，实际实现可能需要更复杂的逻辑）
     */
    private String getSqlStatement(MappedStatement mappedStatement, Object parameter) {
        try {
            // 这里简化处理，实际应该解析BoundSql获取完整SQL
            return mappedStatement.getSqlSource().getBoundSql(parameter).getSql();
        } catch (Exception e) {
            logger.debug("Failed to extract SQL statement: {}", e.getMessage());
            return mappedStatement.getId(); // 返回statement ID作为fallback
        }
    }
}