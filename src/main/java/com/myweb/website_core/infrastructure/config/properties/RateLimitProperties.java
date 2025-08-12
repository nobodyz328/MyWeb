package com.myweb.website_core.infrastructure.config.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 访问频率限制配置属性类
 * 管理访问频率限制相关配置
 * 
 * @author MyWeb Team
 * @since 1.0.0
 */
@Getter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    // Getters and Setters
    /**
     * 是否启用访问频率限制
     */
    private boolean enabled = true;
    
    /**
     * 默认限制配置
     */
    private DefaultLimit defaultLimit = new DefaultLimit();
    
    /**
     * 特定接口的限制配置
     */
    private Map<String, EndpointLimit> endpoints = new HashMap<>();
    
    /**
     * Redis配置
     */
    private Redis redis = new Redis();
    
    /**
     * 告警配置
     */
    private Alert alert = new Alert();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDefaultLimit(DefaultLimit defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public void setEndpoints(Map<String, EndpointLimit> endpoints) {
        this.endpoints = endpoints;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }
    
    /**
     * 默认限制配置
     */
    @Getter
    public static class DefaultLimit {
        // Getters and Setters
        /**
         * 时间窗口大小（秒）
         */
        private int windowSizeSeconds = 60;
        
        /**
         * 最大请求数
         */
        private int maxRequests = 100;
        
        /**
         * 限制类型（IP、用户、全局）
         */
        private String limitType = "IP";

        public void setWindowSizeSeconds(int windowSizeSeconds) {
            this.windowSizeSeconds = windowSizeSeconds;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public void setLimitType(String limitType) {
            this.limitType = limitType;
        }
    }
    
    /**
     * 特定接口限制配置
     */
    @Getter
    public static class EndpointLimit {
        // Getters and Setters
        /**
         * 时间窗口大小（秒）
         */
        private int windowSizeSeconds;
        
        /**
         * 最大请求数
         */
        private int maxRequests;
        
        /**
         * 限制类型（IP、用户、全局）
         */
        private String limitType = "IP";
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 描述
         */
        private String description;

        public void setWindowSizeSeconds(int windowSizeSeconds) {
            this.windowSizeSeconds = windowSizeSeconds;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public void setLimitType(String limitType) {
            this.limitType = limitType;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    /**
     * Redis配置
     */
    @Getter
    public static class Redis {
        // Getters and Setters
        /**
         * Redis键前缀
         */
        private String keyPrefix = "rate_limit:";
        
        /**
         * 键过期时间（秒）
         */
        private int keyExpirationSeconds = 3600;

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public void setKeyExpirationSeconds(int keyExpirationSeconds) {
            this.keyExpirationSeconds = keyExpirationSeconds;
        }
    }
    
    /**
     * 告警配置
     */
    @Getter
    public static class Alert {
        // Getters and Setters
        /**
         * 是否启用告警
         */
        private boolean enabled = true;
        
        /**
         * 告警阈值（超过限制的百分比）
         */
        private double threshold = 0.8;
        
        /**
         * 告警间隔（分钟）
         */
        private int intervalMinutes = 5;
        
        /**
         * 告警接收邮箱
         */
        private String[] recipients = {};

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public void setIntervalMinutes(int intervalMinutes) {
            this.intervalMinutes = intervalMinutes;
        }

        public void setRecipients(String[] recipients) {
            this.recipients = recipients;
        }
    }
    
    /**
     * 获取指定接口的限制配置
     */
    public EndpointLimit getEndpointLimit(String endpoint) {
        return endpoints.getOrDefault(endpoint, createDefaultEndpointLimit());
    }
    
    /**
     * 创建默认的接口限制配置
     */
    private EndpointLimit createDefaultEndpointLimit() {
        EndpointLimit limit = new EndpointLimit();
        limit.setWindowSizeSeconds(defaultLimit.getWindowSizeSeconds());
        limit.setMaxRequests(defaultLimit.getMaxRequests());
        limit.setLimitType(defaultLimit.getLimitType());
        return limit;
    }
    
    /**
     * 检查是否需要告警
     */
    public boolean shouldAlert(int currentRequests, int maxRequests) {
        if (!alert.isEnabled()) {
            return false;
        }
        double ratio = (double) currentRequests / maxRequests;
        return ratio >= alert.getThreshold();
    }
}