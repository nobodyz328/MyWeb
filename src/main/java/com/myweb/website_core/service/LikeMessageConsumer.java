package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.interaction.PostLike;
import com.myweb.website_core.demos.web.interaction.PostLikeRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.LikeMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 点赞消息消费者
 * 处理异步点赞/取消点赞操作
 */

@Service
public class LikeMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LikeMessageConsumer.class);
    
    // Cache key constants
    private static final String CACHE_POST_LIKES_COUNT = "post:likes:count:";
    private static final String CACHE_USER_INTERACTION_STATUS = "user:%d:post:%d:interactions";

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理点赞消息
     */
    @RabbitListener(queues = RabbitMQConfig.INTERACTION_LIKE_QUEUE)
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public void handleLikeMessage(LikeMessageDto message) {
        logger.info("开始处理点赞消息: {}", message);

        try {
            // 验证消息数据
            if (!validateMessage(message)) {
                logger.error("点赞消息验证失败: {}", message);
                return;
            }

            // 获取用户和帖子实体
            Optional<User> userOpt = userRepository.findById(message.getUserId());
            Optional<Post> postOpt = postRepository.findById(message.getPostId());

            if (userOpt.isEmpty()) {
                logger.error("用户不存在: userId={}", message.getUserId());
                return;
            }

            if (postOpt.isEmpty()) {
                logger.error("帖子不存在: postId={}", message.getPostId());
                return;
            }

            User user = userOpt.get();
            Post post = postOpt.get();

            // 处理点赞/取消点赞逻辑
            if (message.isLike()) {
                handleLikeOperation(user, post, message);
            } else {
                handleUnlikeOperation(user, post, message);
            }

            // 更新缓存
            updateLikeCache(message.getPostId(), message.getUserId());

            logger.info("点赞消息处理成功: messageId={}, operation={}", 
                       message.getMessageId(), message.isLike() ? "like" : "unlike");

        } catch (Exception e) {
            logger.error("处理点赞消息失败: {}", message, e);
            throw e; // 重新抛出异常以触发重试机制
        }
    }

    /**
     * 处理点赞操作
     */
    private void handleLikeOperation(User user, Post post, LikeMessageDto message) {
        // 检查是否已经点赞（防止重复点赞）
        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(
            message.getUserId(), message.getPostId());

        if (existingLike.isEmpty()) {
            // 创建新的点赞记录
            PostLike newLike = new PostLike(user, post);
            postLikeRepository.save(newLike);
            
            logger.info("创建点赞记录成功: userId={}, postId={}", 
                       message.getUserId(), message.getPostId());
        } else {
            logger.warn("用户已经点赞过该帖子: userId={}, postId={}", 
                       message.getUserId(), message.getPostId());
        }
    }

    /**
     * 处理取消点赞操作
     */
    private void handleUnlikeOperation(User user, Post post, LikeMessageDto message) {
        // 查找并删除点赞记录
        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(
            message.getUserId(), message.getPostId());

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            
            logger.info("删除点赞记录成功: userId={}, postId={}", 
                       message.getUserId(), message.getPostId());
        } else {
            logger.warn("未找到要删除的点赞记录: userId={}, postId={}", 
                       message.getUserId(), message.getPostId());
        }
    }

    /**
     * 更新点赞相关缓存
     */
    private void updateLikeCache(Long postId, Long userId) {
        try {
            // 更新点赞计数缓存
            long newLikeCount = postLikeRepository.countByPostId(postId);
            String countCacheKey = CACHE_POST_LIKES_COUNT + postId;
            redisTemplate.opsForValue().set(countCacheKey, newLikeCount, 1, TimeUnit.HOURS);

            // 清除用户交互状态缓存
            String userInteractionKey = String.format(CACHE_USER_INTERACTION_STATUS, userId, postId);
            redisTemplate.delete(userInteractionKey);

            // 清除相关的Spring Cache
            // 这里可以通过CacheManager来清除特定的缓存项
            
            logger.debug("更新点赞缓存成功: postId={}, newCount={}", postId, newLikeCount);

        } catch (Exception e) {
            logger.error("更新点赞缓存失败: postId={}, userId={}", postId, userId, e);
            // 缓存更新失败不应该影响主要业务逻辑，所以不重新抛出异常
        }
    }

    /**
     * 验证消息数据的完整性
     */
    private boolean validateMessage(LikeMessageDto message) {
        if (message == null) {
            logger.error("点赞消息为空");
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

        if (message.getUsername() == null || message.getUsername().trim().isEmpty()) {
            logger.error("用户名为空: {}", message);
            return false;
        }

        return true;
    }

    /**
     * 获取当前点赞数（用于统计和监控）
     */
    public long getCurrentLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    /**
     * 检查用户是否已点赞（用于状态查询）
     */
    public boolean isUserLiked(Long userId, Long postId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }
}