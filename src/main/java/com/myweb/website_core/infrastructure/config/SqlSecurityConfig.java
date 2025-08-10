package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.infrastructure.security.defense.SqlAuditInterceptor;
import com.myweb.website_core.infrastructure.security.defense.JpaAuditInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

/**
 * SQL安全配置类
 * <p>
 * 统一的SQL安全拦截器注册，包括：
 * 1. MyBatis拦截器注册
 * 2. JPA/Hibernate拦截器注册
 * 3. 数据源代理配置
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SqlSecurityConfig {
    
    private final List<SqlSessionFactory> sqlSessionFactoryList;
    private final SqlAuditInterceptor sqlAuditInterceptor;
    private final JpaAuditInterceptor jpaAuditInterceptor;
    
//    @Autowired(required = false)
//    private EntityManagerFactory entityManagerFactory;

    /**
     * 注册MyBatis拦截器
     */
    @PostConstruct
    public void addMyBatisInterceptors() {
        if (sqlSessionFactoryList != null && !sqlSessionFactoryList.isEmpty()) {
            for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
                sqlSessionFactory.getConfiguration().addInterceptor(sqlAuditInterceptor);
                log.info("MyBatis SQL audit interceptor registered for session factory: {}", 
                        sqlSessionFactory.getClass().getSimpleName());
            }
        } else {
            log.warn("No MyBatis SqlSessionFactory found, skipping MyBatis interceptor registration");
        }
    }
    
    /**
     * 注册JPA/Hibernate拦截器
     * 防御太敏感，先关掉
     */
//    @PostConstruct
//    public void addJpaInterceptors() {
//        if (entityManagerFactory != null) {
//            try {
//                SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
//                EventListenerRegistry registry = sessionFactory.getServiceRegistry()
//                    .getService(EventListenerRegistry.class);
//
//
//                // 注册各种事件监听器
//                registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(jpaAuditInterceptor);
//                registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(jpaAuditInterceptor);
//                registry.getEventListenerGroup(EventType.PRE_DELETE).appendListener(jpaAuditInterceptor);
//                registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(jpaAuditInterceptor);
//                registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(jpaAuditInterceptor);
//                registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(jpaAuditInterceptor);
//
//                log.info("JPA/Hibernate SQL audit interceptor registered successfully");
//            } catch (Exception e) {
//                log.error("Failed to register JPA interceptor: {}", e.getMessage());
//            }
//        } else {
//            log.warn("No JPA EntityManagerFactory found, skipping JPA interceptor registration");
//        }
//    }
    

}