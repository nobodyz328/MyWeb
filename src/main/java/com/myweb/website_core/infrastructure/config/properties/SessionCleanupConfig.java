package com.myweb.website_core.infrastructure.config.properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 会话清理配置类
 * 
 * 配置会话清理服务的相关设置，包括：
 * - 启用异步处理
 * - 启用定时任务调度
 * - 配置清理策略参数
 * 
 * 符合GB/T 22239-2019剩余信息保护要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class SessionCleanupConfig {
    
    /**
     * 会话超时时间（分钟）
     */
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * 会话最大存活时间（小时）
     */
    public static final int SESSION_MAX_LIFETIME_HOURS = 24;
    
    /**
     * 清理批次大小
     */
    public static final int CLEANUP_BATCH_SIZE = 100;
    
    /**
     * 定时清理间隔（毫秒）- 5分钟
     */
    public static final long CLEANUP_INTERVAL_MS = 300000;
    
    /**
     * 孤立数据清理间隔（毫秒）- 1小时
     */
    public static final long ORPHANED_CLEANUP_INTERVAL_MS = 3600000;
    
    public SessionCleanupConfig() {
        log.info("会话清理配置初始化完成");
        log.info("会话超时时间: {} 分钟", SESSION_TIMEOUT_MINUTES);
        log.info("会话最大存活时间: {} 小时", SESSION_MAX_LIFETIME_HOURS);
        log.info("清理批次大小: {}", CLEANUP_BATCH_SIZE);
        log.info("定时清理间隔: {} 毫秒", CLEANUP_INTERVAL_MS);
        log.info("孤立数据清理间隔: {} 毫秒", ORPHANED_CLEANUP_INTERVAL_MS);
    }
}