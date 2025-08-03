package com.myweb.website_core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 日志存储管理配置类
 * 
 * 配置日志存储管理相关的异步执行器和调度器
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Configuration
@EnableAsync
@EnableScheduling
public class LogStorageConfig {
    
    /**
     * 日志存储管理异步执行器
     * 
     * @return 异步执行器
     */
    @Bean("logStorageTaskExecutor")
    public Executor logStorageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(2);
        
        // 最大线程数
        executor.setMaxPoolSize(5);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名前缀
        executor.setThreadNamePrefix("LogStorage-");
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}