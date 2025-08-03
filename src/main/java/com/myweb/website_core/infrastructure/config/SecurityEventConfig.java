package com.myweb.website_core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

import java.util.List;

/**
 * 安全事件配置类
 * 
 * 管理安全事件监控和告警的配置参数
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.event")
@Data
public class SecurityEventConfig {
    
    /**
     * 是否启用安全事件监控
     */
    private boolean enabled = true;
    
    /**
     * 事件告警配置
     */
    private Alert alert = new Alert();
    
    /**
     * 事件统计配置
     */
    private Statistics statistics = new Statistics();
    
    /**
     * 事件清理配置
     */
    private Cleanup cleanup = new Cleanup();
    
    /**
     * 异常检测配置
     */
    private AnomalyDetection anomalyDetection = new AnomalyDetection();
    
    @Data
    public static class Alert {
        /**
         * 是否启用邮件告警
         */
        private boolean emailEnabled = true;
        
        /**
         * 邮件告警收件人列表
         */
        private List<String> emailRecipients;
        
        /**
         * 告警邮件发件人
         */
        private String emailFrom = "security@myweb.com";
        
        /**
         * 是否启用短信告警
         */
        private boolean smsEnabled = false;
        
        /**
         * 短信告警收件人列表
         */
        private List<String> smsRecipients;
        
        /**
         * 告警阈值配置
         */
        private Threshold threshold = new Threshold();
        
        @Data
        public static class Threshold {
            /**
             * 高危事件立即告警
             */
            private boolean immediateAlertForHighRisk = true;
            
            /**
             * 批量告警的最小事件数量
             */
            private int batchAlertMinCount = 5;
            
            /**
             * 告警频率限制（分钟）
             */
            private int alertRateLimitMinutes = 15;
        }
    }
    
    @Data
    public static class Statistics {
        /**
         * 是否启用统计功能
         */
        private boolean enabled = true;
        
        /**
         * 统计缓存过期时间（小时）
         */
        private int cacheExpirationHours = 24;
        
        /**
         * 实时统计更新间隔（秒）
         */
        private int realtimeUpdateIntervalSeconds = 60;
        
        /**
         * 历史统计保留天数
         */
        private int historyRetentionDays = 90;
    }
    
    @Data
    public static class Cleanup {
        /**
         * 是否启用自动清理
         */
        private boolean enabled = true;
        
        /**
         * 事件保留天数
         */
        private int retentionDays = 90;
        
        /**
         * 清理任务执行时间（cron表达式）
         */
        private String schedule = "0 0 2 * * ?";
        
        /**
         * 批量删除大小
         */
        private int batchSize = 1000;
    }
    
    @Data
    public static class AnomalyDetection {
        /**
         * 是否启用异常检测
         */
        private boolean enabled = true;
        
        /**
         * 检测时间窗口（小时）
         */
        private int timeWindowHours = 1;
        
        /**
         * 异常事件阈值
         */
        private int eventThreshold = 10;
        
        /**
         * 用户异常检测阈值
         */
        private int userAnomalyThreshold = 5;
        
        /**
         * IP异常检测阈值
         */
        private int ipAnomalyThreshold = 20;
        
        /**
         * 检测间隔（分钟）
         */
        private int detectionIntervalMinutes = 5;
    }
}