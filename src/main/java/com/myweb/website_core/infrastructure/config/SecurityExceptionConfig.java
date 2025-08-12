package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.application.service.security.SecurityExceptionMaskingService;
import com.myweb.website_core.application.service.security.SecurityExceptionStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 安全异常处理配置类
 * <p>
 * 配置安全异常处理相关的组件和定时任务，包括：
 * - 异常统计服务配置
 * - 异常脱敏服务配置
 * - 定时清理任务配置
 * - 异常监控配置
 * <p>
 * 符合需求：1.6, 2.6, 3.4, 4.6 - 统一异常处理配置
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@EnableScheduling
public class SecurityExceptionConfig {
    
    /**
     * 安全异常统计服务Bean
     * 只有在配置启用时才创建
     */
    @Bean
    @ConditionalOnProperty(name = "app.security.exception-statistics.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityExceptionStatisticsService securityExceptionStatisticsService() {
        log.info("SecurityExceptionConfig: 启用安全异常统计服务");
        return new SecurityExceptionStatisticsService();
    }
    
    /**
     * 安全异常脱敏服务Bean
     * 只有在配置启用时才创建
     */
    @Bean
    @ConditionalOnProperty(name = "app.security.exception-masking.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityExceptionMaskingService securityExceptionMaskingService() {
        log.info("SecurityExceptionConfig: 启用安全异常脱敏服务");
        return new SecurityExceptionMaskingService();
    }
    
    /**
     * 定时清理过期的异常统计数据
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    @ConditionalOnProperty(name = "app.security.exception-statistics.enabled", havingValue = "true", matchIfMissing = true)
    public void cleanupExpiredStatistics() {
        try {
            SecurityExceptionStatisticsService statisticsService = 
                securityExceptionStatisticsService();
            
            if (statisticsService != null) {
                statisticsService.cleanupExpiredData();
                log.debug("SecurityExceptionConfig: 定时清理异常统计数据完成");
            }
        } catch (Exception e) {
            log.error("SecurityExceptionConfig: 清理异常统计数据时发生错误", e);
        }
    }
    
    /**
     * 定时输出异常统计报告
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @ConditionalOnProperty(name = "app.security.exception-statistics.enabled", havingValue = "true", matchIfMissing = true)
    public void generateDailyStatisticsReport() {
        try {
            SecurityExceptionStatisticsService statisticsService = 
                securityExceptionStatisticsService();
            
            if (statisticsService != null) {
                var statistics = statisticsService.getExceptionStatistics();
                var trends = statisticsService.getExceptionTrends();
                
                log.info("SecurityExceptionConfig: 每日异常统计报告");
                log.info("总异常数量: {}", statistics.get("totalExceptions"));
                log.info("最近异常数量: {}", trends.get("recentExceptionsCount"));
                log.info("风险级别: {}", trends.get("riskLevel"));
                log.info("异常增长率: {}%", trends.get("growthRate"));
                
                // 这里可以扩展为发送邮件报告或推送到监控系统
            }
        } catch (Exception e) {
            log.error("SecurityExceptionConfig: 生成异常统计报告时发生错误", e);
        }
    }
    
    /**
     * 定时检查异常趋势并发送告警
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 1800000) // 30分钟 = 1800000毫秒
    @ConditionalOnProperty(name = "app.security.exception-statistics.real-time-alert", havingValue = "true", matchIfMissing = true)
    public void checkExceptionTrendsAndAlert() {
        try {
            SecurityExceptionStatisticsService statisticsService = 
                securityExceptionStatisticsService();
            
            if (statisticsService != null) {
                var trends = statisticsService.getExceptionTrends();
                String riskLevel = (String) trends.get("riskLevel");
                
                if ("HIGH".equals(riskLevel)) {
                    log.warn("SecurityExceptionConfig: 检测到高风险异常趋势 - 风险级别: {}", riskLevel);
                    
                    // 这里可以扩展为发送告警通知
                    // 例如：发送邮件、短信、推送到监控系统等
                }
            }
        } catch (Exception e) {
            log.error("SecurityExceptionConfig: 检查异常趋势时发生错误", e);
        }
    }
}