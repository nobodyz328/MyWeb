package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.infrastructure.security.SqlAuditInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * SQL安全配置类
 * 配置SQL注入防护相关的组件：
 * 1. MyBatis拦截器注册
 * 2. SQL审计配置
 * 3. 安全参数设置
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Configuration
public class SqlSecurityConfig {
    
    private final List<SqlSessionFactory> sqlSessionFactoryList;
    
    private final SqlAuditInterceptor sqlAuditInterceptor;
    @Autowired
    public SqlSecurityConfig(List<SqlSessionFactory> sqlSessionFactoryList, SqlAuditInterceptor sqlAuditInterceptor) {
        this.sqlSessionFactoryList = sqlSessionFactoryList;
        this.sqlAuditInterceptor = sqlAuditInterceptor;
    }

    /**
     * 注册MyBatis拦截器
     */
    @PostConstruct
    public void addInterceptors() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(sqlAuditInterceptor);
        }
    }
}