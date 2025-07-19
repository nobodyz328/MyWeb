package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.demos.web.user.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MessageConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理帖子创建消息
     */
    @RabbitListener(queues = RabbitMQConfig.POST_CREATED_QUEUE)
    public void handlePostCreated(Map<String, Object> message) {
        try {
            logger.info("处理帖子创建消息: {}", message);
            
            // 更新缓存中的帖子列表
            String cacheKey = "posts:all";
            redisTemplate.delete(cacheKey);
            
            // 发送通知给关注者
            Long authorId = Long.valueOf(message.get("authorId").toString());
            String authorName = message.get("authorName").toString();
            String postTitle = message.get("title").toString();
            
            // 这里可以查询作者的关注者并发送通知
            // 暂时记录日志
            logger.info("用户 {} 发布了新帖子: {}", authorName, postTitle);
            
        } catch (Exception e) {
            logger.error("处理帖子创建消息失败", e);
        }
    }

    /**
     * 处理帖子点赞消息
     */
    @RabbitListener(queues = RabbitMQConfig.POST_LIKED_QUEUE)
    public void handlePostLiked(Map<String, Object> message) {
        try {
            logger.info("处理帖子点赞消息: {}", message);
            
            Long postId = Long.valueOf(message.get("postId").toString());
            Long userId = Long.valueOf(message.get("userId").toString());
            String username = message.get("username").toString();
            
            // 更新Redis中的点赞计数
            String likeKey = "post:like:" + postId;
            redisTemplate.opsForValue().increment(likeKey);
            
            // 记录用户点赞行为
            String userLikeKey = "user:like:" + userId + ":" + postId;
            redisTemplate.opsForValue().set(userLikeKey, LocalDateTime.now(), 30, TimeUnit.DAYS);
            
            logger.info("用户 {} 点赞了帖子 {}", username, postId);
            
        } catch (Exception e) {
            logger.error("处理帖子点赞消息失败", e);
        }
    }

    /**
     * 处理邮件通知消息
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_NOTIFICATION_QUEUE)
    public void handleEmailNotification(Map<String, Object> message) {
        try {
            logger.info("处理邮件通知消息: {}", message);
            
            String to = message.get("to").toString();
            String subject = message.get("subject").toString();
            String content = message.get("content").toString();
            
            // 发送邮件
            emailService.sendVerificationCode(to, content);
            
            logger.info("邮件发送成功: {} -> {}", to, subject);
            
        } catch (Exception e) {
            logger.error("处理邮件通知消息失败", e);
        }
    }

    /**
     * 处理审计日志消息
     */
    @RabbitListener(queues = RabbitMQConfig.AUDIT_LOG_QUEUE)
    public void handleAuditLog(Map<String, Object> message) {
        try {
            logger.info("处理审计日志消息: {}", message);
            
            String userId = message.get("userId").toString();
            String username = message.get("username").toString();
            String action = message.get("action").toString();
            String details = message.get("details").toString();
            LocalDateTime timestamp = LocalDateTime.parse(message.get("timestamp").toString());
            
            // 存储审计日志到Redis
            String auditKey = "audit:log:" + timestamp.toLocalDate();
            redisTemplate.opsForList().rightPush(auditKey, message);
            
            // 设置过期时间（保留30天）
            redisTemplate.expire(auditKey, 30, TimeUnit.DAYS);
            
            logger.info("审计日志记录成功: {} - {} - {}", username, action, details);
            
        } catch (Exception e) {
            logger.error("处理审计日志消息失败", e);
        }
    }
} 