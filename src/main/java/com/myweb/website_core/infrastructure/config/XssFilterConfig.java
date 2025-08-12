package com.myweb.website_core.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * XSS过滤配置类
 * <p>
 * 提供可配置的XSS过滤策略，支持HTML标签白名单、属性白名单等高级配置。
 * <p>
 * 主要功能：
 * 1. 可配置的XSS过滤规则
 * 2. HTML标签和属性白名单管理
 * 3. 过滤策略配置
 * 4. 性能优化配置
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - XSS防护配置
 * 
 * @author MyWeb
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.security.xss-protection")
public class XssFilterConfig {
    
    /**
     * XSS防护是否启用
     */
    private boolean enabled = true;
    
    /**
     * 严格模式 - 检测到XSS攻击时是否直接拒绝请求
     */
    private boolean strictMode = false;
    
    /**
     * 允许的HTML标签白名单
     */
    private Set<String> allowedTags = Set.of("b", "i", "u", "strong", "em", "p", "br", "a", "img");
    
    /**
     * 允许的HTML属性白名单
     */
    private Set<String> allowedAttributes = Set.of("href", "src", "alt", "title", "class");
    
    /**
     * 标签特定的属性白名单
     */
    private Map<String, Set<String>> tagSpecificAttributes = Map.of(
        "a", Set.of("href", "title", "target"),
        "img", Set.of("src", "alt", "title", "width", "height"),
        "p", Set.of("class", "style"),
        "div", Set.of("class", "id", "style")
    );
    
    /**
     * 是否移除未知标签
     */
    private boolean removeUnknownTags = true;
    
    /**
     * 是否编码特殊字符
     */
    private boolean encodeSpecialChars = true;
    
    /**
     * 最大标签嵌套深度
     */
    private int maxTagDepth = 10;
    
    /**
     * 内容最大长度限制
     */
    private int maxContentLength = 50000;
    
    /**
     * 自定义XSS攻击模式
     */
    private List<String> customXssPatterns = List.of();
    
    /**
     * 白名单URL模式 - 这些URL不进行XSS过滤
     */
    private List<String> whitelistUrlPatterns = List.of(
            "/blog/*","/blog/post/*","/blog/posts/edit/*","/blog/posts/new",
            "/blog/api/**",
            "/blog/admin/**"
    );
    private final Map<String, Set<String>> whitelistUrlMethods = Map.of(
            "/blog/**", Set.of("GET")
    );

    public Set<String> getWhitelistUrlPattern() {
        return whitelistUrlMethods.keySet();
    }

    public Set<String> getAllowedHttpMethods(String urlPattern) {
        return whitelistUrlMethods.get(urlPattern);
    }
    
    /**
     * 性能优化配置
     */
    private PerformanceConfig performance = new PerformanceConfig();
    
    /**
     * 统计配置
     */
    private StatisticsConfig statistics = new StatisticsConfig();
    
    /**
     * 监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();
    
    /**
     * 性能优化配置
     */
    @Data
    public static class PerformanceConfig {
        /**
         * 是否启用缓存
         */
        private boolean cacheEnabled = true;
        
        /**
         * 缓存大小
         */
        private int cacheSize = 1000;
        
        /**
         * 缓存过期时间（分钟）
         */
        private int cacheExpirationMinutes = 30;
        
        /**
         * 是否启用异步处理
         */
        private boolean asyncProcessing = false;
        
        /**
         * 批处理大小
         */
        private int batchSize = 100;
        
        /**
         * 处理超时时间（毫秒）
         */
        private long processingTimeoutMs = 5000;
    }
    
    /**
     * 统计配置
     */
    @Data
    public static class StatisticsConfig {
        /**
         * 是否启用统计
         */
        private boolean enabled = true;
        
        /**
         * 统计数据保留天数
         */
        private int retentionDays = 30;
        
        /**
         * 统计报告生成间隔（小时）
         */
        private int reportIntervalHours = 24;
        
        /**
         * 是否启用实时统计
         */
        private boolean realTimeStats = true;
        
        /**
         * 统计聚合间隔（分钟）
         */
        private int aggregationIntervalMinutes = 5;
    }
    
    /**
     * 监控配置
     */
    @Data
    public static class MonitoringConfig {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;
        
        /**
         * XSS攻击阈值 - 超过此数量触发告警
         */
        private int attackThreshold = 10;
        
        /**
         * 监控时间窗口（分钟）
         */
        private int timeWindowMinutes = 60;
        
        /**
         * 是否启用实时告警
         */
        private boolean realTimeAlert = true;
        
        /**
         * 告警冷却时间（分钟）
         */
        private int alertCooldownMinutes = 30;
        
        /**
         * 性能监控阈值（毫秒）
         */
        private long performanceThresholdMs = 1000;
    }
}