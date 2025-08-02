package com.myweb.website_core.application.service;

import com.myweb.website_core.domain.entity.Post;
import com.myweb.website_core.domain.entity.PostCollect;
import com.myweb.website_core.domain.entity.PostLike;
import com.myweb.website_core.domain.entity.User;
import com.myweb.website_core.infrastructure.config.RabbitMQConfig;
import com.myweb.website_core.infrastructure.mapper.PostCollectRepository;
import com.myweb.website_core.infrastructure.mapper.PostLikeRepository;
import com.myweb.website_core.infrastructure.mapper.PostRepository;
import com.myweb.website_core.infrastructure.mapper.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumerService {
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCollectRepository postCollectRepository;

    /**
     * 处理帖子创建消息
     */
    @RabbitListener(queues = RabbitMQConfig.POST_CREATED_QUEUE)
    public void handlePostCreated(Map<String, Object> message) {
        try {
            log.info("处理帖子创建消息: {}", message);
            
            // 更新缓存中的帖子列表
            String cacheKey = "posts:all";
            redisTemplate.delete(cacheKey);
            
            // 发送通知给关注者
            Long authorId = Long.valueOf(message.get("authorId").toString());
            String postTitle = message.get("title").toString();
            
            // 这里可以查询作者的关注者并发送通知
            // 暂时记录日志
            log.info("用户 {} 发布了新帖子: {}", authorId, postTitle);
            
        } catch (Exception e) {
            log.error("处理帖子创建消息失败", e);
        }
    }

    /**
     * 处理帖子点赞消息
     */
    @RabbitListener(queues = RabbitMQConfig.INTERACTION_LIKE_QUEUE)
    public void handlePostLike(Map<String, Object> message) {
        try {
            log.info("处理帖子点赞消息: {}", message);
            
            long postId = Long.parseLong(message.get("postId").toString());
            long userId = Long.parseLong(message.get("userId").toString());
            boolean type = Boolean.parseBoolean(message.get("type").toString());

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if(type){
                // 添加点赞
                postLikeRepository.save(new PostLike(user, post));
            }else{
                // 取消点赞
                postLikeRepository.deleteByUserIdAndPostId(userId, postId);

            }
            post.setLikeCount(postLikeRepository.countByPostId(postId));
            user.setLikedCount(postLikeRepository.countByUserId(userId));
            postRepository.save(post);
            userRepository.save(user);

            log.info("用户 {} 点赞了帖子 {}", userId, postId);
            
        } catch (Exception e) {
            log.error("处理帖子点赞消息失败", e);
        }
    }
    @RabbitListener(queues = RabbitMQConfig.INTERACTION_BOOKMARK_QUEUE)
    public void handlePostCollect(Map<String,Object> message){
        try {
            log.info("处理帖子收藏消息: {}", message);

            long postId = Long.parseLong(message.get("postId").toString());
            long userId = Long.parseLong(message.get("userId").toString());
            boolean type = Boolean.parseBoolean(message.get("type").toString());

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if(type){
                // 添加收藏
                postCollectRepository.save(new PostCollect(user, post));
                // 更新点赞数
            }else{
                // 取消点赞
                postCollectRepository.deleteByUserIdAndPostId(userId, postId);

            }
            post.setCollectCount(postCollectRepository.countByPostId(postId));
            postRepository.save(post);

            log.info("用户 {} 收藏了帖子 {}", userId, postId);

        } catch (Exception e) {
            log.error("处理帖子收藏消息失败", e);
        }
    }
    /**
     * 处理邮件通知消息
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_NOTIFICATION_QUEUE)
    public void handleEmailNotification(Map<String, Object> message) {
        try {
            log.info("处理邮件通知消息: {}", message);
            
            String to = message.get("to").toString();
            String subject = message.get("subject").toString();
            String content = message.get("content").toString();
            
            // 发送邮件
            emailService.sendVerificationCode(to, content);
            
            log.info("邮件发送成功: {} -> {}", to, subject);
            
        } catch (Exception e) {
            log.error("处理邮件通知消息失败", e);
        }
    }

    /**
     * 处理审计日志消息
     */
    @RabbitListener(queues = RabbitMQConfig.AUDIT_LOG_QUEUE)
    public void handleAuditLog(Map<String, Object> message) {
        try {
            log.info("处理审计日志消息: {}", message);
            
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
            
            log.info("审计日志记录成功: {} - {} - {}", username, action, details);
            
        } catch (Exception e) {
            log.error("处理审计日志消息失败", e);
        }
    }
} 