package com.myweb.website_core.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 安全配置中心配置类
 * <p>
 * 启用安全配置中心相关功能：
 * - 配置属性绑定
 * - 异步事件处理
 * - 配置变更监听
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties({
    com.myweb.website_core.common.config.SecurityProperties.class,
    com.myweb.website_core.infrastructure.config.JwtConfig.class,
    com.myweb.website_core.common.config.RateLimitProperties.class,
    com.myweb.website_core.common.config.BackupProperties.class
})
public class SecurityConfigCenter {
    
    public SecurityConfigCenter() {
        log.info("安全配置中心已启用 - 支持动态配置管理、实时变更生效、配置备份恢复");
    }
}