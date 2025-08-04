package com.myweb.website_core.application.service.integration;

import com.myweb.website_core.infrastructure.config.RabbitMQConfig;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.security.dto.SecurityAuditMessage;
import com.myweb.website_core.domain.security.dto.UnifiedSecurityMessage;
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
            log.error("Failed to send user follow message: " + e.getMessage());
            // 不抛出异常，避免影响主业务流程
        }
    }

    // ==================== 安全审计消息发送方法 ====================

    /**
     * 发送安全审计消息（兼容旧版本）
     */
    public void sendSecurityAuditMessage(SecurityAuditMessage auditMessage) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.SECURITY_AUDIT_ROUTING_KEY,
                auditMessage
            );
            log.debug("安全审计消息发送成功: {}", auditMessage.getOperation());
        } catch (Exception e) {
            log.error("发送安全审计消息失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送统一安全消息（新版本）
     */
    public void sendUnifiedSecurityMessage(UnifiedSecurityMessage message) {
        try {
            String exchange = RabbitMQConfig.SECURITY_EXCHANGE;
            String routingKey = message.getRoutingKey();
            
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            
            log.debug("统一安全消息发送成功: type={}, operation={}", 
                     message.getMessageType(), message.getOperation());
        } catch (Exception e) {
            log.error("发送统一安全消息失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发送用户认证审计消息
     */
    public void sendUserAuthAuditMessage(String username, AuditOperation operation, String ipAddress, 
                                       String result, String errorMessage, String sessionId) {
        try {
            UnifiedSecurityMessage message = UnifiedSecurityMessage.userAuth(
                username, operation, ipAddress, result, errorMessage, sessionId
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.USER_AUTH_ROUTING_KEY,
                message
            );
            log.debug("用户认证审计消息发送成功: {} - {}", username, operation);
        } catch (Exception e) {
            log.error("发送用户认证审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送文件上传审计消息
     */
    public void sendFileUploadAuditMessage(Long userId, String username, String fileName, 
                                         String fileType, Long fileSize, String result, 
                                         String ipAddress, String errorMessage) {
        try {
            UnifiedSecurityMessage message = UnifiedSecurityMessage.fileUpload(
                userId, username, fileName, fileType, fileSize, result, ipAddress, errorMessage
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.FILE_UPLOAD_AUDIT_ROUTING_KEY,
                message
            );
            log.debug("文件上传审计消息发送成功: {} - {}", username, fileName);
        } catch (Exception e) {
            log.error("发送文件上传审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送搜索操作审计消息
     */
    public void sendSearchAuditMessage(Long userId, String username, String searchQuery, 
                                     String searchType, Integer resultCount, String ipAddress) {
        try {
            UnifiedSecurityMessage message = UnifiedSecurityMessage.search(
                userId, username, searchQuery, searchType, resultCount, ipAddress
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.SEARCH_AUDIT_ROUTING_KEY,
                message
            );
            log.debug("搜索审计消息发送成功: {} - {}", username, searchQuery);
        } catch (Exception e) {
            log.error("发送搜索审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送访问控制审计消息
     */
    public void sendAccessControlAuditMessage(Long userId, String username, String resourceType, 
                                            Long resourceId, String action, String result, 
                                            String ipAddress, String reason) {
        try {
            UnifiedSecurityMessage message = UnifiedSecurityMessage.accessControl(
                userId, username, resourceType, resourceId, action, result, ipAddress, reason
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.ACCESS_CONTROL_ROUTING_KEY,
                message
            );
            log.debug("访问控制审计消息发送成功: {} - {} - {}", username, action, result);
        } catch (Exception e) {
            log.error("发送访问控制审计消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送安全事件消息
     */
    public void sendSecurityEventMessage(UnifiedSecurityMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.SECURITY_EVENT_ROUTING_KEY,
                message
            );
            log.warn("安全事件消息发送成功: {} - {}", message.getSecurityEventType(), message.getDescription());
        } catch (Exception e) {
            log.error("发送安全事件消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送用户注册审计消息
     */
    public void sendUserRegistrationAuditMessage(String username, String email, String ipAddress, 
                                               String result, String errorMessage) {
        sendUserAuthAuditMessage(username, AuditOperation.USER_REGISTER, ipAddress, result, errorMessage, null);
    }

    /**
     * 发送用户登录审计消息
     */
    public void sendUserLoginAuditMessage(String username, String ipAddress, String result, 
                                        String errorMessage, String sessionId) {
        AuditOperation operation = "SUCCESS".equals(result) ? 
                AuditOperation.USER_LOGIN_SUCCESS : AuditOperation.USER_LOGIN_FAILURE;
        sendUserAuthAuditMessage(username, operation, ipAddress, result, errorMessage, sessionId);
    }

    /**
     * 发送用户退出登录审计消息
     */
    public void sendUserLogoutAuditMessage(String username, String ipAddress, String sessionId) {
        sendUserAuthAuditMessage(username, AuditOperation.USER_LOGOUT, ipAddress, "SUCCESS", null, sessionId);
    }

    /**
     * 发送内容操作审计消息
     */
    public void sendContentOperationAuditMessage(Long userId, String username, AuditOperation operation, 
                                                String resourceType, Long resourceId, String ipAddress, 
                                                String result, String description) {
        try {
            UnifiedSecurityMessage message = UnifiedSecurityMessage.auditLog(
                operation, userId, username, result, ipAddress
            );
            message.setResourceType(resourceType);
            message.setResourceId(resourceId);
            message.setDescription(description);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECURITY_EXCHANGE,
                RabbitMQConfig.SECURITY_AUDIT_ROUTING_KEY,
                message
            );
            log.debug("内容操作审计消息发送成功: {} - {} - {}", username, operation, resourceType);
        } catch (Exception e) {
            log.error("发送内容操作审计消息失败: {}", e.getMessage(), e);
        }
    }
} 