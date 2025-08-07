package com.myweb.website_core.application.service.security.IPS.ratelimit;

import com.myweb.website_core.common.config.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 访问频率限制配置管理服务
 * 支持动态配置和调整访问频率限制策略
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Service
public class RateLimitConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfigService.class);
    
    @Autowired
    private RateLimitProperties rateLimitProperties;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 动态配置缓存
     */
    private final Map<String, RateLimitProperties.EndpointLimit> dynamicConfigs = new ConcurrentHashMap<>();
    
    /**
     * Redis配置键前缀
     */
    private static final String CONFIG_KEY_PREFIX = "rate_limit_config:";
    
    /**
     * 获取接口的访问频率限制配置
     * 优先级：动态配置 > Redis配置 > 默认配置
     * 
     * @param endpoint 接口路径
     * @return 访问频率限制配置
     */
    public RateLimitProperties.EndpointLimit getEndpointConfig(String endpoint) {
        // 1. 检查动态配置缓存
        RateLimitProperties.EndpointLimit dynamicConfig = dynamicConfigs.get(endpoint);
        if (dynamicConfig != null) {
            return dynamicConfig;
        }
        
        // 2. 检查Redis中的配置
        RateLimitProperties.EndpointLimit redisConfig = loadConfigFromRedis(endpoint);
        if (redisConfig != null) {
            // 缓存到内存中
            dynamicConfigs.put(endpoint, redisConfig);
            return redisConfig;
        }
        
        // 3. 使用默认配置
        return rateLimitProperties.getEndpointLimit(endpoint);
    }
    
    /**
     * 动态更新接口的访问频率限制配置
     * 
     * @param endpoint 接口路径
     * @param config 新的配置
     */
    public void updateEndpointConfig(String endpoint, RateLimitProperties.EndpointLimit config) {
        try {
            // 更新内存缓存
            dynamicConfigs.put(endpoint, config);
            
            // 保存到Redis
            saveConfigToRedis(endpoint, config);
            
            logger.info("动态更新访问频率限制配置: endpoint={}, maxRequests={}, windowSize={}s", 
                       endpoint, config.getMaxRequests(), config.getWindowSizeSeconds());
            
        } catch (Exception e) {
            logger.error("更新访问频率限制配置失败: endpoint={}", endpoint, e);
            throw new RuntimeException("更新配置失败", e);
        }
    }
    
    /**
     * 批量更新配置
     * 
     * @param configs 配置映射
     */
    public void batchUpdateConfigs(Map<String, RateLimitProperties.EndpointLimit> configs) {
        for (Map.Entry<String, RateLimitProperties.EndpointLimit> entry : configs.entrySet()) {
            updateEndpointConfig(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 删除接口的动态配置
     * 
     * @param endpoint 接口路径
     */
    public void removeEndpointConfig(String endpoint) {
        try {
            // 从内存缓存中移除
            dynamicConfigs.remove(endpoint);
            
            // 从Redis中删除
            String redisKey = CONFIG_KEY_PREFIX + endpoint;
            redisTemplate.delete(redisKey);
            
            logger.info("删除访问频率限制配置: endpoint={}", endpoint);
            
        } catch (Exception e) {
            logger.error("删除访问频率限制配置失败: endpoint={}", endpoint, e);
        }
    }
    
    /**
     * 获取所有动态配置
     * 
     * @return 配置映射
     */
    public Map<String, RateLimitProperties.EndpointLimit> getAllDynamicConfigs() {
        return new HashMap<>(dynamicConfigs);
    }
    
    /**
     * 重新加载所有配置
     */
    public void reloadConfigs() {
        try {
            // 清空内存缓存
            dynamicConfigs.clear();
            
            // 从Redis重新加载所有配置
            String pattern = CONFIG_KEY_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    String endpoint = key.substring(CONFIG_KEY_PREFIX.length());
                    RateLimitProperties.EndpointLimit config = loadConfigFromRedis(endpoint);
                    if (config != null) {
                        dynamicConfigs.put(endpoint, config);
                    }
                }
            }
            
            logger.info("重新加载访问频率限制配置完成，共加载{}个配置", dynamicConfigs.size());
            
        } catch (Exception e) {
            logger.error("重新加载访问频率限制配置失败", e);
        }
    }
    
    /**
     * 从Redis加载配置
     */
    @SuppressWarnings("unchecked")
    private RateLimitProperties.EndpointLimit loadConfigFromRedis(String endpoint) {
        try {
            String redisKey = CONFIG_KEY_PREFIX + endpoint;
            Map<String, Object> configMap = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
            
            if (configMap != null) {
                RateLimitProperties.EndpointLimit config = new RateLimitProperties.EndpointLimit();
                
                if (configMap.containsKey("maxRequests")) {
                    config.setMaxRequests((Integer) configMap.get("maxRequests"));
                }
                if (configMap.containsKey("windowSizeSeconds")) {
                    config.setWindowSizeSeconds((Integer) configMap.get("windowSizeSeconds"));
                }
                if (configMap.containsKey("limitType")) {
                    config.setLimitType((String) configMap.get("limitType"));
                }
                if (configMap.containsKey("enabled")) {
                    config.setEnabled((Boolean) configMap.get("enabled"));
                }
                if (configMap.containsKey("description")) {
                    config.setDescription((String) configMap.get("description"));
                }
                
                return config;
            }
            
        } catch (Exception e) {
            logger.error("从Redis加载配置失败: endpoint={}", endpoint, e);
        }
        
        return null;
    }
    
    /**
     * 保存配置到Redis
     */
    private void saveConfigToRedis(String endpoint, RateLimitProperties.EndpointLimit config) {
        try {
            String redisKey = CONFIG_KEY_PREFIX + endpoint;
            
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("maxRequests", config.getMaxRequests());
            configMap.put("windowSizeSeconds", config.getWindowSizeSeconds());
            configMap.put("limitType", config.getLimitType());
            configMap.put("enabled", config.isEnabled());
            configMap.put("description", config.getDescription());
            configMap.put("updateTime", System.currentTimeMillis());
            
            // 保存配置，设置过期时间为7天
            redisTemplate.opsForValue().set(redisKey, configMap, Duration.ofDays(7));
            
        } catch (Exception e) {
            logger.error("保存配置到Redis失败: endpoint={}", endpoint, e);
            throw e;
        }
    }
    
    /**
     * 创建默认的接口配置
     * 
     * @param maxRequests 最大请求数
     * @param windowSizeSeconds 时间窗口大小（秒）
     * @param limitType 限制类型
     * @param description 描述
     * @return 接口配置
     */
    public RateLimitProperties.EndpointLimit createEndpointConfig(int maxRequests, int windowSizeSeconds, 
                                                                 String limitType, String description) {
        RateLimitProperties.EndpointLimit config = new RateLimitProperties.EndpointLimit();
        config.setMaxRequests(maxRequests);
        config.setWindowSizeSeconds(windowSizeSeconds);
        config.setLimitType(limitType);
        config.setEnabled(true);
        config.setDescription(description);
        return config;
    }
    
    /**
     * 获取预定义的配置模板
     * 
     * @return 配置模板映射
     */
    public Map<String, RateLimitProperties.EndpointLimit> getConfigTemplates() {
        Map<String, RateLimitProperties.EndpointLimit> templates = new HashMap<>();
        
        // 登录接口 - 严格限制
        templates.put("login", createEndpointConfig(5, 300, "IP", "登录接口限制"));
        
        // 注册接口 - 严格限制
        templates.put("register", createEndpointConfig(3, 600, "IP", "注册接口限制"));
        
        // 发帖接口 - 中等限制
        templates.put("post_create", createEndpointConfig(10, 300, "USER", "发帖接口限制"));
        
        // 评论接口 - 中等限制
        templates.put("comment_create", createEndpointConfig(20, 300, "USER", "评论接口限制"));
        
        // 搜索接口 - 宽松限制
        templates.put("search", createEndpointConfig(100, 60, "IP", "搜索接口限制"));
        
        // 文件上传 - 严格限制
        templates.put("file_upload", createEndpointConfig(5, 300, "USER", "文件上传限制"));
        
        // API接口 - 标准限制
        templates.put("api_default", createEndpointConfig(60, 60, "IP", "API默认限制"));
        
        return templates;
    }
    
    /**
     * 应用配置模板
     * 
     * @param templateName 模板名称
     * @param endpoint 接口路径
     */
    public void applyTemplate(String templateName, String endpoint) {
        Map<String, RateLimitProperties.EndpointLimit> templates = getConfigTemplates();
        RateLimitProperties.EndpointLimit template = templates.get(templateName);
        
        if (template != null) {
            updateEndpointConfig(endpoint, template);
            logger.info("应用配置模板: template={}, endpoint={}", templateName, endpoint);
        } else {
            throw new IllegalArgumentException("未找到配置模板: " + templateName);
        }
    }
    
    /**
     * 获取配置统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getConfigStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalDynamicConfigs", dynamicConfigs.size());
        stats.put("totalStaticConfigs", rateLimitProperties.getEndpoints().size());
        stats.put("globalEnabled", rateLimitProperties.isEnabled());
        
        // 统计不同限制类型的配置数量
        Map<String, Integer> limitTypeStats = new HashMap<>();
        dynamicConfigs.values().forEach(config -> {
            String limitType = config.getLimitType();
            limitTypeStats.put(limitType, limitTypeStats.getOrDefault(limitType, 0) + 1);
        });
        stats.put("limitTypeStats", limitTypeStats);
        
        return stats;
    }
}