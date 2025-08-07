package com.myweb.website_core.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 安全服务配置类
 * <p>
 * 启用所有安全相关服务的配置，包括：
 * - 异步处理支持
 * - 定时任务支持
 * - 配置属性绑定
 * - 安全服务组件扫描
 * <p>
 * 确保所有安全服务能够正常启动和运行
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
    com.myweb.website_core.common.config.BackupProperties.class
})
public class SecurityServicesConfig {
    
    public SecurityServicesConfig() {
        log.info("安全服务配置已启用 - 异步处理、定时任务、配置属性绑定已激活");
    }
}