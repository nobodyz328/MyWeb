package com.myweb.website_core.application.service.integration;

import com.myweb.website_core.infrastructure.config.RabbitMQConfig;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.common.enums.AuditOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@Slf4j
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
            log.error("Failed to send post created message: " + e.getMessage());
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送帖子点赞消息
     */
    public void sendPostLikeMessage(Long postId, Long userId, boolean type) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("postId", postId);
            message.put("userId", userId);
            message.put("type", type);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INTERACTION_EXCHANGE,
                RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY,
                message
            );
        } catch (Exception e) {
            System.err.println("Failed to send post liked message: " + e.getMessage());
            log.error("Failed to send post liked message: " + e.getMessage());
            // 不抛出异常，避免影响主业务流程
        }
    }
    public void sendPostCollectMessage(Long postId, Long userId, boolean type) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("postId", postId);
            message.put("userId", userId);
            message.put("type", type);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INTERACTION_EXCHANGE,
                    RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            System.err.println("Failed to send post liked message: " + e.getMessage());
            log.error("Failed to send post liked message: " + e.getMessage());
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
            log.error("Failed to send email notification message: " + e.getMessage());
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
            log.error("Failed to send audit log message: " + e.getMessage());
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送用户关注消息
     */
    public void sendUserFollowMessage(Long targetUserId, Long userId, boolean isFollowed) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("targetUserId", targetUserId);
            message.put("userId", userId);
            message.put("isFollowed", isFollowed);
            message.put("timestamp", LocalDateTime.now().toString());
            message.put("type", "USER_FOLLOW");

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INTERACTION_EXCHANGE,
                RabbitMQConfig.INTERACTION_FOLLOW_ROUTING_KEY,
                message
            );
        } catch (Exception e) {
            System.err.println("Failed to send user follow message: " + e.getMessage());
            log.error("Failed to send user follow message: " + e.getMessage());
            // 不抛出异常，避免影响主业务流程
        }
    }

    // ==================== 安全审计消息发送方法 ====================

    /**
     * 发送用户认证审计消息
     */
    public void sendUserAuthAuditMessage(AuditLogRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.USER_AUTH_ROUTING_KEY,
                request
            );
            log.debug("用户认证审计消息发送成功: {} - {}", request.getUsername(), request.getOperation());
        } catch (Exception e) {
            log.error("发送用户认证审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送文件上传审计消息
     */
    public void sendFileUploadAuditMessage(AuditLogRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.FILE_UPLOAD_AUDIT_ROUTING_KEY,
                request
            );
            log.debug("文件上传审计消息发送成功: {} - {}", request.getUsername(), request.getDescription());
        } catch (Exception e) {
            log.error("发送文件上传审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送搜索操作审计消息
     */
    public void sendSearchAuditMessage(AuditLogRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.SEARCH_AUDIT_ROUTING_KEY,
                request
            );
            log.debug("搜索审计消息发送成功: {} - {}", request.getUsername(), request.getDescription());
        } catch (Exception e) {
            log.error("发送搜索审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送访问控制审计消息
     */
    public void sendAccessControlAuditMessage(AuditLogRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.ACCESS_CONTROL_ROUTING_KEY,
                request
            );
            log.debug("访问控制审计消息发送成功: {} - {} - {}", request.getUsername(), 
                     request.getDescription(), request.getResult());
        } catch (Exception e) {
            log.error("发送访问控制审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送安全事件消息
     */
    public void sendSecurityEventMessage(SecurityEventRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.SECURITY_EVENT_ROUTING_KEY,
                request
            );
            log.warn("安全事件消息发送成功: {} - {}", request.getEventType(), request.getDescription());
        } catch (Exception e) {
            log.error("发送安全事件消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送用户注册审计消息
     */
    public void sendUserRegistrationAuditMessage(AuditLogRequest request) {
        sendUserAuthAuditMessage(request);
    }

    /**
     * 发送用户登录审计消息
     */
    public void sendUserLoginAuditMessage(AuditLogRequest request) {
        sendUserAuthAuditMessage(request);
    }

    /**
     * 发送用户退出登录审计消息
     */
    public void sendUserLogoutAuditMessage(AuditLogRequest request) {
        sendUserAuthAuditMessage(request);
    }

    /**
     * 发送内容操作审计消息
     */
    public void sendContentOperationAuditMessage(AuditLogRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.SECURITY_AUDIT_ROUTING_KEY,
                request
            );
            log.debug("内容操作审计消息发送成功: {} - {} - {}", request.getUsername(), 
                     request.getOperation(), request.getResourceType());
        } catch (Exception e) {
            log.error("发送内容操作审计消息失败: {}", e.getMessage(), e);
        }
    }
}