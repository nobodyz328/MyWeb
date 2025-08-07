package com.myweb.website_core.application.service.security.IPS.ratelimit;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.config.RateLimitProperties;
import com.myweb.website_core.common.enums.AuditOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * 访问频率限制服务
 * 基于Redis实现滑动窗口算法的访问频率控制
 * 支持基于IP和用户的双重限制策略
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class RateLimitingService {
    

    private final RedisTemplate<String, Object> redisTemplate;

    private final RateLimitProperties rateLimitProperties;

    private final AuditLogService auditLogService;
    
    /**
     * Lua脚本实现原子性的滑动窗口计数
     * 使用ZSET存储时间戳，实现精确的滑动窗口
     */
    private static final String SLIDING_WINDOW_SCRIPT = 
        "local key = KEYS[1] " +
        "local window = tonumber(ARGV[1]) " +
        "local limit = tonumber(ARGV[2]) " +
        "local now = tonumber(ARGV[3]) " +
        "local clearBefore = now - window * 1000 " +
        
        // 清理过期的记录
        "redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore) " +
        
        // 获取当前窗口内的请求数
        "local current = redis.call('ZCARD', key) " +
        
        // 如果超过限制，返回当前计数
        "if current >= limit then " +
        "  return {current, limit, 0} " +
        "end " +
        
        // 添加当前请求
        "redis.call('ZADD', key, now, now) " +
        "redis.call('EXPIRE', key, window + 1) " +
        
        // 返回更新后的计数
        "local newCurrent = redis.call('ZCARD', key) " +
        "return {newCurrent, limit, 1}";
    
    private final DefaultRedisScript<Object> slidingWindowScript;

    @Autowired
    public RateLimitingService(RedisTemplate<String, Object> redisTemplate, RateLimitProperties rateLimitProperties, AuditLogService auditLogService) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;
        this.auditLogService = auditLogService;
        this.slidingWindowScript = new DefaultRedisScript<>();
        this.slidingWindowScript.setScriptText(SLIDING_WINDOW_SCRIPT);
        this.slidingWindowScript.setResultType(Object.class);
    }
    
    /**
     * 检查访问频率是否超限
     * 
     * @param clientIp 客户端IP地址
     * @param uri 请求URI
     * @param username 用户名（可选）
     * @return 是否允许访问
     */
    public boolean isAllowed(String clientIp, String uri, String username) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }
        
        try {
            // 获取接口配置
            RateLimitProperties.EndpointLimit endpointLimit = rateLimitProperties.getEndpointLimit(uri);
            if (!endpointLimit.isEnabled()) {
                return true;
            }
            
            // 基于IP的限制检查
            boolean ipAllowed = checkIpLimit(clientIp, uri, endpointLimit);
            
            // 基于用户的限制检查（如果用户已登录）
            boolean userAllowed = true;
            if (username != null && !"IP".equals(endpointLimit.getLimitType())) {
                userAllowed = checkUserLimit(username, uri, endpointLimit);
            }
            
            boolean allowed = ipAllowed && userAllowed;
            
            // 记录访问频率超限事件
            if (!allowed) {
                recordRateLimitExceededEvent(clientIp, uri, username, endpointLimit);
            }
            
            return allowed;
            
        } catch (Exception e) {
            log.error("访问频率检查失败: clientIp={}, uri={}, username={}", clientIp, uri, username, e);
            // 发生异常时允许访问，避免影响正常业务
            return true;
        }
    }
    
    /**
     * 检查IP访问频率限制
     */
    private boolean checkIpLimit(String clientIp, String uri, RateLimitProperties.EndpointLimit limit) {
        String key = buildIpLimitKey(clientIp, uri);
        return checkLimit(key, limit);
    }
    
    /**
     * 检查用户访问频率限制
     */
    private boolean checkUserLimit(String username, String uri, RateLimitProperties.EndpointLimit limit) {
        String key = buildUserLimitKey(username, uri);
        return checkLimit(key, limit);
    }
    
    /**
     * 执行频率限制检查
     */
    private boolean checkLimit(String key, RateLimitProperties.EndpointLimit limit) {
        long now = System.currentTimeMillis();
        
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Long> result = (java.util.List<Long>) redisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(key),
                limit.getWindowSizeSeconds(),
                limit.getMaxRequests(),
                now
            );
            
            if (result != null && result.size() >= 3) {
                long currentCount = result.get(0);
                long maxRequests = result.get(1);
                long allowed = result.get(2);
                
                // 检查是否需要告警
                if (rateLimitProperties.shouldAlert((int) currentCount, (int) maxRequests)) {
                    sendRateLimitAlert(key, currentCount, maxRequests);
                }
                
                return allowed == 1;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Redis频率限制检查失败: key={}", key, e);
            return true; // 异常时允许访问
        }
    }
    
    /**
     * 构建IP限制的Redis键
     */
    private String buildIpLimitKey(String clientIp, String uri) {
        return rateLimitProperties.getRedis().getKeyPrefix() + "ip:" + clientIp + ":" + normalizeUri(uri);
    }
    
    /**
     * 构建用户限制的Redis键
     */
    private String buildUserLimitKey(String username, String uri) {
        return rateLimitProperties.getRedis().getKeyPrefix() + "user:" + username + ":" + normalizeUri(uri);
    }
    
    /**
     * 标准化URI，移除参数和特殊字符
     */
    private String normalizeUri(String uri) {
        if (uri == null) {
            return "unknown";
        }
        
        // 移除查询参数
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }
        
        // 替换特殊字符
        return uri.replaceAll("[^a-zA-Z0-9/_-]", "_");
    }
    
    /**
     * 记录访问频率超限事件
     */
    private void recordRateLimitExceededEvent(String clientIp, String uri, String username, 
                                            RateLimitProperties.EndpointLimit limit) {
        CompletableFuture.runAsync(() -> {
            try {
                // 使用现有的AuditLogService记录安全事件
                auditLogService.logSecurityEvent(
                    AuditOperation.SUSPICIOUS_ACTIVITY,
                    username != null ? username : "anonymous",
                    String.format("访问频率超限: IP=%s, URI=%s, 限制=%d/%ds", 
                                clientIp, uri, limit.getMaxRequests(), limit.getWindowSizeSeconds())
                );
                
                log.warn("访问频率超限: clientIp={}, uri={}, username={}, limit={}/{}",
                          clientIp, uri, username, limit.getMaxRequests(), limit.getWindowSizeSeconds());
                          
            } catch (Exception e) {
                log.error("记录访问频率超限事件失败", e);
            }
        });
    }
    
    /**
     * 发送访问频率告警
     */
    private void sendRateLimitAlert(String key, long currentCount, long maxRequests) {
        CompletableFuture.runAsync(() -> {
            try {
                String alertKey = "rate_limit_alert:" + key;
                
                // 检查告警间隔，避免频繁发送
                Boolean alertSent = redisTemplate.hasKey(alertKey);
                if (Boolean.TRUE.equals(alertSent)) {
                    return;
                }
                
                // 设置告警间隔
                redisTemplate.opsForValue().set(
                    alertKey, 
                    "1", 
                    Duration.ofMinutes(rateLimitProperties.getAlert().getIntervalMinutes())
                );
                
                // 发送告警（这里可以集成邮件、短信等告警方式）
                log.warn("访问频率告警: key={}, current={}, max={}, ratio={}%",
                          key, currentCount, maxRequests, 
                          (double) currentCount / maxRequests * 100);
                
                // 记录安全事件
                auditLogService.logSecurityEvent(
                    AuditOperation.SUSPICIOUS_ACTIVITY,
                    getCurrentUsername(),
                    String.format("访问频率告警: IP=%s, 当前=%d, 限制=%d, 比例=%.2f%%", 
                                extractIpFromKey(key), currentCount, maxRequests, 
                                (double) currentCount / maxRequests * 100)
                );
                
            } catch (Exception e) {
                log.error("发送访问频率告警失败: key={}", key, e);
            }
        });
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取当前用户名失败", e);
        }
        return "anonymous";
    }
    
    /**
     * 从Redis键中提取IP地址
     */
    private String extractIpFromKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length >= 3 && "ip".equals(parts[1])) {
                return parts[2];
            }
        } catch (Exception e) {
            log.debug("从Redis键提取IP失败: key={}", key, e);
        }
        return "unknown";
    }
    
    /**
     * 获取当前访问统计信息
     * 
     * @param clientIp 客户端IP
     * @param uri 请求URI
     * @param username 用户名
     * @return 访问统计信息
     */
    public RateLimitStatus getRateLimitStatus(String clientIp, String uri, String username) {
        if (!rateLimitProperties.isEnabled()) {
            return new RateLimitStatus(true, 0, 0, 0);
        }
        
        try {
            RateLimitProperties.EndpointLimit limit = rateLimitProperties.getEndpointLimit(uri);
            
            // 获取IP限制状态
            String ipKey = buildIpLimitKey(clientIp, uri);
            long ipCount = getCurrentCount(ipKey);
            
            // 获取用户限制状态（如果适用）
            long userCount = 0;
            if (username != null && !"IP".equals(limit.getLimitType())) {
                String userKey = buildUserLimitKey(username, uri);
                userCount = getCurrentCount(userKey);
            }
            
            boolean allowed = ipCount < limit.getMaxRequests() && 
                            (userCount == 0 || userCount < limit.getMaxRequests());
            
            return new RateLimitStatus(allowed, ipCount, userCount, limit.getMaxRequests());
            
        } catch (Exception e) {
            log.error("获取访问频率状态失败: clientIp={}, uri={}, username={}", clientIp, uri, username, e);
            return new RateLimitStatus(true, 0, 0, 0);
        }
    }
    
    /**
     * 获取当前计数
     */
    private long getCurrentCount(String key) {
        try {
            Long count = redisTemplate.opsForZSet().zCard(key);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.debug("获取Redis计数失败: key={}", key, e);
            return 0;
        }
    }
    
    /**
     * 清除指定键的访问记录
     * 
     * @param clientIp 客户端IP
     * @param uri 请求URI
     * @param username 用户名
     */
    public void clearRateLimit(String clientIp, String uri, String username) {
        try {
            String ipKey = buildIpLimitKey(clientIp, uri);
            redisTemplate.delete(ipKey);
            
            if (username != null) {
                String userKey = buildUserLimitKey(username, uri);
                redisTemplate.delete(userKey);
            }
            
            log.info("清除访问频率限制: clientIp={}, uri={}, username={}", clientIp, uri, username);
            
        } catch (Exception e) {
            log.error("清除访问频率限制失败: clientIp={}, uri={}, username={}", clientIp, uri, username, e);
        }
    }
    
    /**
     * 访问频率限制状态
     */
    public static class RateLimitStatus {
        private final boolean allowed;
        private final long ipCount;
        private final long userCount;
        private final long maxRequests;
        
        public RateLimitStatus(boolean allowed, long ipCount, long userCount, long maxRequests) {
            this.allowed = allowed;
            this.ipCount = ipCount;
            this.userCount = userCount;
            this.maxRequests = maxRequests;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getIpCount() {
            return ipCount;
        }

        public long getUserCount() {
            return userCount;
        }

        public long getMaxRequests() {
            return maxRequests;
        }

        public long getRemainingRequests() {
            return Math.max(0, maxRequests - Math.max(ipCount, userCount));
        }
    }
}