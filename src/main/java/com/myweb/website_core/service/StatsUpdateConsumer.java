package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.dto.StatsUpdateMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 统计更新消息消费者
 * 处理异步统计数据更新操作
 */
@Service
public class StatsUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(StatsUpdateConsumer.class);
    
    // Cache key constants
    private static final String CACHE_POST_LIKES_COUNT = "post:likes:count:";
    private static final String CACHE_POST_BOOKMARKS_COUNT = "post:bookmarks:count:";
    private static final String CACHE_POST_COMMENTS_COUNT = "post:comments:count:";
    private static final String CACHE_POST_STATS = "post:stats:";
    private static final String CACHE_DAILY_STATS = "stats:daily:";
    private static final String CACHE_USER_ACTIVITY = "user:activity:";

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理统计更新消息
     */
    @RabbitListener(queues = RabbitMQConfig.INTERACTION_STATS_UPDATE_QUEUE)
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public void handleStatsUpdateMessage(StatsUpdateMessageDto message) {
        logger.info("开始处理统计更新消息: {}", message);

        try {
            // 验证消息数据
            if (!validateMessage(message)) {
                logger.error("统计更新消息验证失败: {}", message);
                return;
            }

            // 验证帖子是否存在
            Optional<Post> postOpt = postRepository.findById(message.getPostId());
            if (postOpt.isEmpty()) {
                logger.error("帖子不存在: postId={}", message.getPostId());
                return;
            }

            // 根据统计类型更新相应的缓存
            switch (message.getStatsType()) {
                case "LIKE":
                    updateLikeStats(message);
                    break;
                case "BOOKMARK":
                    updateBookmarkStats(message);
                    break;
                case "COMMENT":
                    updateCommentStats(message);
                    break;
                default:
                    logger.warn("未知的统计类型: {}", message.getStatsType());
                    return;
            }

            // 更新综合统计信息
            updateAggregateStats(message);

            // 更新用户活动统计
            updateUserActivityStats(message);

            // 更新每日统计
            updateDailyStats(message);

            logger.info("统计更新消息处理成功: messageId={}, statsType={}, countChange={}", 
                       message.getMessageId(), message.getStatsType(), message.getCountChange());

        } catch (Exception e) {
            logger.error("处理统计更新消息失败: {}", message, e);
            throw e; // 重新抛出异常以触发重试机制
        }
    }

    /**
     * 更新点赞统计
     */
    private void updateLikeStats(StatsUpdateMessageDto message) {
        String cacheKey = CACHE_POST_LIKES_COUNT + message.getPostId();
        
        try {
            // 原子性地更新计数
            Long newCount = redisTemplate.opsForValue().increment(cacheKey, message.getCountChange());
            
            // 确保计数不会小于0
            if (newCount < 0) {
                redisTemplate.opsForValue().set(cacheKey, 0);
                newCount = 0L;
            }
            
            // 设置过期时间
            redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
            
            logger.debug("更新点赞统计成功: postId={}, newCount={}, change={}", 
                        message.getPostId(), newCount, message.getCountChange());

        } catch (Exception e) {
            logger.error("更新点赞统计失败: postId={}", message.getPostId(), e);
            throw e;
        }
    }

    /**
     * 更新书签统计
     */
    private void updateBookmarkStats(StatsUpdateMessageDto message) {
        String cacheKey = CACHE_POST_BOOKMARKS_COUNT + message.getPostId();
        
        try {
            // 原子性地更新计数
            Long newCount = redisTemplate.opsForValue().increment(cacheKey, message.getCountChange());
            
            // 确保计数不会小于0
            if (newCount < 0) {
                redisTemplate.opsForValue().set(cacheKey, 0);
                newCount = 0L;
            }
            
            // 设置过期时间
            redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
            
            logger.debug("更新书签统计成功: postId={}, newCount={}, change={}", 
                        message.getPostId(), newCount, message.getCountChange());

        } catch (Exception e) {
            logger.error("更新书签统计失败: postId={}", message.getPostId(), e);
            throw e;
        }
    }

    /**
     * 更新评论统计
     */
    private void updateCommentStats(StatsUpdateMessageDto message) {
        String cacheKey = CACHE_POST_COMMENTS_COUNT + message.getPostId();
        
        try {
            // 原子性地更新计数
            Long newCount = redisTemplate.opsForValue().increment(cacheKey, message.getCountChange());
            
            // 确保计数不会小于0
            if (newCount < 0) {
                redisTemplate.opsForValue().set(cacheKey, 0);
                newCount = 0L;
            }
            
            // 设置过期时间
            redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
            
            logger.debug("更新评论统计成功: postId={}, newCount={}, change={}", 
                        message.getPostId(), newCount, message.getCountChange());

        } catch (Exception e) {
            logger.error("更新评论统计失败: postId={}", message.getPostId(), e);
            throw e;
        }
    }

    /**
     * 更新综合统计信息
     */
    private void updateAggregateStats(StatsUpdateMessageDto message) {
        String cacheKey = CACHE_POST_STATS + message.getPostId();
        
        try {
            // 获取当前统计信息
            Map<String, Object> stats = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            if (stats == null) {
                stats = new HashMap<>();
            }

            // 更新对应的统计项
            String statsField = message.getStatsType().toLowerCase() + "_count";
            Object currentValue = stats.get(statsField);
            long currentCount = currentValue != null ? Long.parseLong(currentValue.toString()) : 0L;
            long newCount = Math.max(0, currentCount + message.getCountChange());
            
            stats.put(statsField, newCount);
            stats.put("last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 保存更新后的统计信息
            redisTemplate.opsForValue().set(cacheKey, stats, 2, TimeUnit.HOURS);
            
            logger.debug("更新综合统计成功: postId={}, statsType={}, newCount={}", 
                        message.getPostId(), message.getStatsType(), newCount);

        } catch (Exception e) {
            logger.error("更新综合统计失败: postId={}", message.getPostId(), e);
            // 综合统计失败不应该影响主要统计，所以不重新抛出异常
        }
    }

    /**
     * 更新用户活动统计
     */
    private void updateUserActivityStats(StatsUpdateMessageDto message) {
        String cacheKey = CACHE_USER_ACTIVITY + message.getUserId();
        
        try {
            // 获取当前用户活动统计
            Map<String, Object> activity = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            if (activity == null) {
                activity = new HashMap<>();
            }

            // 更新活动计数
            String activityType = message.getOperationType().toLowerCase();
            Object currentValue = activity.get(activityType);
            long currentCount = currentValue != null ? Long.parseLong(currentValue.toString()) : 0L;
            long newCount = Math.max(0, currentCount + Math.abs(message.getCountChange()));
            
            activity.put(activityType, newCount);
            activity.put("last_activity", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 保存用户活动统计（保留24小时）
            redisTemplate.opsForValue().set(cacheKey, activity, 24, TimeUnit.HOURS);
            
            logger.debug("更新用户活动统计成功: userId={}, activityType={}, newCount={}", 
                        message.getUserId(), activityType, newCount);

        } catch (Exception e) {
            logger.error("更新用户活动统计失败: userId={}", message.getUserId(), e);
            // 用户活动统计失败不应该影响主要统计，所以不重新抛出异常
        }
    }

    /**
     * 更新每日统计
     */
    private void updateDailyStats(StatsUpdateMessageDto message) {
        String today = LocalDateTime.now().toLocalDate().toString();
        String cacheKey = CACHE_DAILY_STATS + today;
        
        try {
            // 获取当日统计
            Map<String, Object> dailyStats = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            if (dailyStats == null) {
                dailyStats = new HashMap<>();
            }

            // 更新对应的统计项
            String statsField = message.getOperationType().toLowerCase() + "_count";
            Object currentValue = dailyStats.get(statsField);
            long currentCount = currentValue != null ? Long.parseLong(currentValue.toString()) : 0L;
            long newCount = currentCount + Math.abs(message.getCountChange());
            
            dailyStats.put(statsField, newCount);
            dailyStats.put("date", today);
            dailyStats.put("last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 保存每日统计（保留30天）
            redisTemplate.opsForValue().set(cacheKey, dailyStats, 30, TimeUnit.DAYS);
            
            logger.debug("更新每日统计成功: date={}, operationType={}, newCount={}", 
                        today, message.getOperationType(), newCount);

        } catch (Exception e) {
            logger.error("更新每日统计失败: date={}", today, e);
            // 每日统计失败不应该影响主要统计，所以不重新抛出异常
        }
    }

    /**
     * 验证消息数据的完整性
     */
    private boolean validateMessage(StatsUpdateMessageDto message) {
        if (message == null) {
            logger.error("统计更新消息为空");
            return false;
        }

        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            logger.error("消息ID为空: {}", message);
            return false;
        }

        if (message.getUserId() == null || message.getUserId() <= 0) {
            logger.error("用户ID无效: {}", message);
            return false;
        }

        if (message.getPostId() == null || message.getPostId() <= 0) {
            logger.error("帖子ID无效: {}", message);
            return false;
        }

        if (message.getOperationType() == null || message.getOperationType().trim().isEmpty()) {
            logger.error("操作类型为空: {}", message);
            return false;
        }

        if (message.getStatsType() == null || message.getStatsType().trim().isEmpty()) {
            logger.error("统计类型为空: {}", message);
            return false;
        }

        // 验证统计类型是否有效
        if (!isValidStatsType(message.getStatsType())) {
            logger.error("无效的统计类型: {}", message.getStatsType());
            return false;
        }

        return true;
    }

    /**
     * 验证统计类型是否有效
     */
    private boolean isValidStatsType(String statsType) {
        return "LIKE".equals(statsType) || 
               "BOOKMARK".equals(statsType) || 
               "COMMENT".equals(statsType);
    }

    /**
     * 获取帖子统计信息
     */
    public Map<String, Object> getPostStats(Long postId) {
        String cacheKey = CACHE_POST_STATS + postId;
        Map<String, Object> stats = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        return stats != null ? stats : new HashMap<>();
    }

    /**
     * 获取每日统计信息
     */
    public Map<String, Object> getDailyStats(String date) {
        String cacheKey = CACHE_DAILY_STATS + date;
        Map<String, Object> stats = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        return stats != null ? stats : new HashMap<>();
    }

    /**
     * 获取用户活动统计
     */
    public Map<String, Object> getUserActivityStats(Long userId) {
        String cacheKey = CACHE_USER_ACTIVITY + userId;
        Map<String, Object> activity = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        return activity != null ? activity : new HashMap<>();
    }
}