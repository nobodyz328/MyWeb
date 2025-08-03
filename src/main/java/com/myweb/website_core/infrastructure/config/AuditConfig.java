package com.myweb.website_core.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 审计配置类
 * 
 * 启用AOP自动代理，支持审计切面功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuditConfig {
    
    // AOP配置通过注解自动完成
    // proxyTargetClass = true 表示使用CGLIB代理而不是JDK动态代理
    // 这样可以代理没有实现接口的类
}