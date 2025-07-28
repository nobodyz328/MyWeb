package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.user.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageProducerService {

    private final RabbitTemplate rabbitTemplate;
    @Autowired
    public MessageProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送帖子创建消息
     */
    public void sendPostCreatedMessage(Post post) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("postId", post.getId());
            message.put("title", post.getTitle());
            message.put("authorId", post.getAuthor().getId());
            message.put("authorName", post.getAuthor().getUsername());
            message.put("createdAt", LocalDateTime.now().toString()); // 转换为字符串避免序列化问题
            message.put("type", "POST_CREATED");

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.POST_EXCHANGE,
                RabbitMQConfig.POST_CREATED_ROUTING_KEY,
                message
            );
        } catch (Exception e) {
            System.err.println("Failed to send post created message: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送帖子点赞消息
     */
    public void sendPostLikedMessage(Long postId, Long userId, String username) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("postId", postId);
            message.put("userId", userId);
            message.put("username", username);
            message.put("likedAt", LocalDateTime.now().toString()); // 转换为字符串避免序列化问题
            message.put("type", "POST_LIKED");

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.POST_EXCHANGE,
                RabbitMQConfig.POST_LIKED_ROUTING_KEY,
                message
            );
        } catch (Exception e) {
            System.err.println("Failed to send post liked message: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送邮件通知消息
     */
    public void sendEmailNotificationMessage(String to, String subject, String content) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("to", to);
            message.put("subject", subject);
            message.put("content", content);
            message.put("sentAt", LocalDateTime.now().toString()); // 转换为字符串避免序列化问题
            message.put("type", "EMAIL_NOTIFICATION");

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.EMAIL_NOTIFICATION_ROUTING_KEY,
                message
            );
        } catch (Exception e) {
            System.err.println("Failed to send email notification message: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送审计日志消息
     */
    public void sendAuditLogMessage(String userId, String username, String action, String details) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("username", username);
            message.put("action", action);
            message.put("details", details);
            message.put("timestamp", LocalDateTime.now().toString()); // 转换为字符串避免序列化问题
            message.put("type", "AUDIT_LOG");

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.AUDIT_EXCHANGE,
                RabbitMQConfig.AUDIT_LOG_ROUTING_KEY,
                message
            );
        } catch (Exception e) {
            System.err.println("Failed to send audit log message: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送用户关注消息
     */
    public void sendUserFollowMessage(Long followerId, String followerName, Long targetId, String targetName) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("followerId", followerId);
            message.put("followerName", followerName);
            message.put("targetId", targetId);
            message.put("targetName", targetName);
            message.put("followedAt", LocalDateTime.now().toString()); // 转换为字符串避免序列化问题
            message.put("type", "USER_FOLLOW");

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                "user.follow",
                message
            );
        } catch (Exception e) {
            System.err.println("Failed to send user follow message: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响主业务流程
        }
    }
} 