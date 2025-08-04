package com.myweb.website_core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 安全审计配置类
 * 
 * 配置安全审计相关的组件和设置
 * 符合GB/T 22239-2019二级等保要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Configuration
@EnableAspectJAutoProxy
@EnableAsync
public class SecurityAuditConfig {
    
    /**
     * 配置ObjectMapper用于审计日志的序列化
     */
    @Bean
    public ObjectMapper auditObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 配置时间格式
        mapper.registerModule(new JavaTimeModule());
        
        // 忽略未知属性
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 忽略空值
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        
        return mapper;
    }
}