package com.myweb.website_core.application.service.security.IPS.ratelimit;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.infrastructure.config.properties.RateLimitProperties;
import com.myweb.website_core.common.enums.AuditOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 访问频率限制告警服务
 * 负责处理访问频率超限的告警通知
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-08-01
 */
@Service
public class RateLimitAlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitAlertService.class);

    @Autowired
    private RateLimitProperties rateLimitProperties;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    /**
     * 告警记录键前缀
     */
    private static final String ALERT_KEY_PREFIX = "rate_limit_alert:";
    
    /**
     * 告警统计键前缀
     */
    private static final String ALERT_STATS_KEY_PREFIX = "rate_limit_stats:";
    
    /**
     * 发送访问频率超限告警
     * 
     * @param clientIp 客户端IP
     * @param uri 请求URI
     * @param username 用户名
     * @param currentCount 当前请求数
     * @param maxRequests 最大请求数
     * @param windowSize 时间窗口大小
     */
    public void sendRateLimitAlert(String clientIp, String uri, String username, 
                                 long currentCount, long maxRequests, int windowSize) {
        
        if (!rateLimitProperties.getAlert().isEnabled()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                // 检查告警间隔，避免频繁发送
                String alertKey = buildAlertKey(clientIp, uri);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(alertKey))) {
                    return;
                }
                
                // 设置告警间隔
                redisTemplate.opsForValue().set(
                    alertKey, 
                    "1", 
                    Duration.ofMinutes(rateLimitProperties.getAlert().getIntervalMinutes())
                );
                
                // 构建告警信息
                AlertInfo alertInfo = new AlertInfo(
                    clientIp, uri, username, currentCount, maxRequests, windowSize
                );
                
                // 记录告警统计
                recordAlertStats(alertInfo);
                
                // 发送邮件告警
                sendEmailAlert(alertInfo);
                
                // 记录安全事件
                auditLogService.logSecurityEvent(
                    AuditOperation.SUSPICIOUS_ACTIVITY,
                    alertInfo.getUsername() != null ? alertInfo.getUsername() : "anonymous",
                    String.format("访问频率超限告警: IP=%s, URI=%s, 当前=%d, 限制=%d",
                                alertInfo.getClientIp(), alertInfo.getUri(), 
                                alertInfo.getCurrentCount(), alertInfo.getMaxRequests())
                );
                
                logger.warn("发送访问频率超限告警: {}", alertInfo);
                
            } catch (Exception e) {
                logger.error("发送访问频率告警失败: clientIp={}, uri={}", clientIp, uri, e);
            }
        });
    }
    
    /**
     * 发送访问频率接近限制的预警
     * 
     * @param clientIp 客户端IP
     * @param uri 请求URI
     * @param username 用户名
     * @param currentCount 当前请求数
     * @param maxRequests 最大请求数
     * @param windowSize 时间窗口大小
     */
    public void sendRateLimitWarning(String clientIp, String uri, String username,
                                   long currentCount, long maxRequests, int windowSize) {
        
        if (!rateLimitProperties.getAlert().isEnabled()) {
            return;
        }
        
        double ratio = (double) currentCount / maxRequests;
        if (ratio < rateLimitProperties.getAlert().getThreshold()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                // 检查预警间隔
                String warningKey = buildWarningKey(clientIp, uri);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(warningKey))) {
                    return;
                }
                
                // 设置预警间隔（比告警间隔短一些）
                redisTemplate.opsForValue().set(
                    warningKey, 
                    "1", 
                    Duration.ofMinutes(rateLimitProperties.getAlert().getIntervalMinutes() / 2)
                );
                
                // 记录预警事件
                auditLogService.logSecurityEvent(
                    AuditOperation.SUSPICIOUS_ACTIVITY,
                    username != null ? username : "anonymous",
                    String.format("访问频率预警: IP=%s, URI=%s, 当前=%d, 限制=%d, 比例=%.2f%%",
                                clientIp, uri, currentCount, maxRequests, ratio * 100)
                );
                
                logger.info("访问频率预警: clientIp={}, uri={}, username={}, ratio={}%", 
                          clientIp, uri, username, String.format("%.2f", ratio * 100));
                
            } catch (Exception e) {
                logger.error("发送访问频率预警失败: clientIp={}, uri={}", clientIp, uri, e);
            }
        });
    }
    
    /**
     * 发送邮件告警
     */
    private void sendEmailAlert(AlertInfo alertInfo) {
        if (mailSender == null || rateLimitProperties.getAlert().getRecipients().length == 0) {
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(rateLimitProperties.getAlert().getRecipients());
            message.setSubject("MyWeb系统 - 访问频率超限告警");
            message.setText(buildEmailContent(alertInfo));
            
            mailSender.send(message);
            logger.info("发送访问频率告警邮件成功: {}", alertInfo.getClientIp());
            
        } catch (Exception e) {
            logger.error("发送访问频率告警邮件失败", e);
        }
    }
    
    /**
     * 构建邮件内容
     */
    private String buildEmailContent(AlertInfo alertInfo) {
        StringBuilder content = new StringBuilder();
        content.append("MyWeb博客系统访问频率超限告警\n\n");
        content.append("告警时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("客户端IP: ").append(alertInfo.getClientIp()).append("\n");
        content.append("请求URI: ").append(alertInfo.getUri()).append("\n");
        
        if (alertInfo.getUsername() != null) {
            content.append("用户名: ").append(alertInfo.getUsername()).append("\n");
        }
        
        content.append("当前请求数: ").append(alertInfo.getCurrentCount()).append("\n");
        content.append("最大请求数: ").append(alertInfo.getMaxRequests()).append("\n");
        content.append("时间窗口: ").append(alertInfo.getWindowSize()).append("秒\n");
        content.append("超限比例: ").append(String.format("%.2f%%", (double) alertInfo.getCurrentCount() / alertInfo.getMaxRequests() * 100)).append("\n\n");
        
        content.append("请及时检查系统安全状况，必要时采取相应的安全措施。\n\n");
        content.append("此邮件由MyWeb安全监控系统自动发送，请勿回复。");
        
        return content.toString();
    }
    
    /**
     * 记录安全事件
     */
//    private void recordSecurityEvent(AlertInfo alertInfo) {
//        try {
//            securityEventUtils.recordSecurityEvent(
//                "RATE_LIMIT_EXCEEDED",
//                alertInfo.getUsername(),
//                alertInfo.getClientIp(),
//                String.format("访问频率超限: URI=%s, 当前=%d, 限制=%d",
//                            alertInfo.getUri(), alertInfo.getCurrentCount(), alertInfo.getMaxRequests()),
//                "HIGH"
//            );
//        } catch (Exception e) {
//            logger.error("记录访问频率超限安全事件失败", e);
//        }
//    }
    
    /**
     * 记录告警统计
     */
    private void recordAlertStats(AlertInfo alertInfo) {
        try {
            String statsKey = ALERT_STATS_KEY_PREFIX + "daily:" + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 增加总告警数
            redisTemplate.opsForHash().increment(statsKey, "total", 1);
            
            // 按IP统计
            redisTemplate.opsForHash().increment(statsKey, "ip:" + alertInfo.getClientIp(), 1);
            
            // 按URI统计
            redisTemplate.opsForHash().increment(statsKey, "uri:" + alertInfo.getUri(), 1);
            
            // 按用户统计（如果有用户信息）
            if (alertInfo.getUsername() != null) {
                redisTemplate.opsForHash().increment(statsKey, "user:" + alertInfo.getUsername(), 1);
            }
            
            // 设置过期时间为30天
            redisTemplate.expire(statsKey, Duration.ofDays(30));
            
        } catch (Exception e) {
            logger.error("记录告警统计失败", e);
        }
    }
    
    /**
     * 获取告警统计信息
     * 
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 统计信息
     */
    public Map<String, Object> getAlertStats(String date) {
        try {
            String statsKey = ALERT_STATS_KEY_PREFIX + "daily:" + date;
            Map<Object, Object> rawStats = redisTemplate.opsForHash().entries(statsKey);
            
            Map<String, Object> stats = new HashMap<>();
            Map<String, Integer> ipStats = new HashMap<>();
            Map<String, Integer> uriStats = new HashMap<>();
            Map<String, Integer> userStats = new HashMap<>();
            
            int total = 0;
            
            for (Map.Entry<Object, Object> entry : rawStats.entrySet()) {
                String key = (String) entry.getKey();
                Integer value = Integer.valueOf(entry.getValue().toString());
                
                if ("total".equals(key)) {
                    total = value;
                } else if (key.startsWith("ip:")) {
                    ipStats.put(key.substring(3), value);
                } else if (key.startsWith("uri:")) {
                    uriStats.put(key.substring(4), value);
                } else if (key.startsWith("user:")) {
                    userStats.put(key.substring(5), value);
                }
            }
            
            stats.put("date", date);
            stats.put("total", total);
            stats.put("ipStats", ipStats);
            stats.put("uriStats", uriStats);
            stats.put("userStats", userStats);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("获取告警统计失败: date={}", date, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 获取最近7天的告警趋势
     * 
     * @return 告警趋势数据
     */
    public Map<String, Integer> getAlertTrend() {
        Map<String, Integer> trend = new HashMap<>();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 6; i >= 0; i--) {
                String date = now.minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String statsKey = ALERT_STATS_KEY_PREFIX + "daily:" + date;
                
                Object totalObj = redisTemplate.opsForHash().get(statsKey, "total");
                int total = totalObj != null ? Integer.parseInt(totalObj.toString()) : 0;
                
                trend.put(date, total);
            }
            
        } catch (Exception e) {
            logger.error("获取告警趋势失败", e);
        }
        
        return trend;
    }
    
    /**
     * 构建告警键
     */
    private String buildAlertKey(String clientIp, String uri) {
        return ALERT_KEY_PREFIX + "alert:" + clientIp + ":" + normalizeUri(uri);
    }
    
    /**
     * 构建预警键
     */
    private String buildWarningKey(String clientIp, String uri) {
        return ALERT_KEY_PREFIX + "warning:" + clientIp + ":" + normalizeUri(uri);
    }
    
    /**
     * 标准化URI
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
     * 告警信息类
     */
    public static class AlertInfo {
        private final String clientIp;
        private final String uri;
        private final String username;
        private final long currentCount;
        private final long maxRequests;
        private final int windowSize;
        private final LocalDateTime timestamp;
        
        public AlertInfo(String clientIp, String uri, String username, 
                        long currentCount, long maxRequests, int windowSize) {
            this.clientIp = clientIp;
            this.uri = uri;
            this.username = username;
            this.currentCount = currentCount;
            this.maxRequests = maxRequests;
            this.windowSize = windowSize;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public String getClientIp() { return clientIp; }
        public String getUri() { return uri; }
        public String getUsername() { return username; }
        public long getCurrentCount() { return currentCount; }
        public long getMaxRequests() { return maxRequests; }
        public int getWindowSize() { return windowSize; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("AlertInfo{clientIp='%s', uri='%s', username='%s', currentCount=%d, maxRequests=%d, windowSize=%d, timestamp=%s}",
                               clientIp, uri, username, currentCount, maxRequests, windowSize, timestamp);
        }
    }
}