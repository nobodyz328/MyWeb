package com.myweb.website_core.application.service.integration;

import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.PostCollect;
import com.myweb.website_core.domain.business.entity.PostLike;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.security.dto.UnifiedSecurityMessage;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import com.myweb.website_core.infrastructure.config.RabbitMQConfig;
import com.myweb.website_core.infrastructure.persistence.repository.PostCollectRepository;
import com.myweb.website_core.infrastructure.persistence.repository.PostLikeRepository;
import com.myweb.website_core.infrastructure.persistence.repository.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.infrastructure.persistence.repository.AuditLogRepository;
import com.myweb.website_core.infrastructure.persistence.repository.SecurityEventRepository;
import com.myweb.website_core.application.service.security.audit.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AuditLogRepository auditLogRepository;
    private final SecurityEventRepository securityEventRepository;
    private final SecurityEventService securityEventService;

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
            
            //String userId = message.get("userId").toString();
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

    // ==================== 安全审计消息消费者 ====================

    /**
     * 处理安全审计消息
     */
    @RabbitListener(queues = RabbitMQConfig.SECURITY_AUDIT_QUEUE)
    @Transactional
    public void handleSecurityAudit(UnifiedSecurityMessage message) {
        try {
            log.info("处理安全审计消息: {} - {}", message.getOperation(), message.getUsername());
            
            // 创建审计日志实体
            AuditLog auditLog = createAuditLogFromUnified(message);
            
            // 保存审计日志
            auditLogRepository.save(auditLog);
            
            // 如果是安全事件，创建安全事件记录
            if (Boolean.TRUE.equals(message.getIsSecurityEvent())) {
                createSecurityEventFromUnified(message);
            }
            
            // 更新Redis中的审计统计
            updateAuditStatisticsFromUnified(message);
            
            log.debug("安全审计日志保存成功: {}", auditLog.getId());
            
        } catch (Exception e) {
            log.error("处理安全审计消息失败: {}", e.getMessage(), e);
            throw e; // 重新抛出异常，让消息进入死信队列
        }
    }

    /**
     * 处理用户认证审计消息
     */
    @RabbitListener(queues = RabbitMQConfig.USER_AUTH_QUEUE)
    @Transactional
    public void handleUserAuthAudit(UnifiedSecurityMessage message) {
        try {
            log.info("处理用户认证审计消息: {} - {}", message.getOperation(), message.getUsername());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromUnified(message);
            auditLogRepository.save(auditLog);
            
            // 特殊处理登录失败事件
            if (message.getOperation() == com.myweb.website_core.common.enums.AuditOperation.USER_LOGIN_FAILURE) {
                handleLoginFailureEventFromUnified(message);
            }
            
            // 特殊处理账户锁定事件
            if (message.getOperation() == com.myweb.website_core.common.enums.AuditOperation.ACCOUNT_LOCKED) {
                handleAccountLockedEventFromUnified(message);
            }
            
            log.debug("用户认证审计日志保存成功: {}", auditLog.getId());
            
        } catch (Exception e) {
            log.error("处理用户认证审计消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理文件上传审计消息
     */
    @RabbitListener(queues = RabbitMQConfig.FILE_UPLOAD_AUDIT_QUEUE)
    @Transactional
    public void handleFileUploadAudit(UnifiedSecurityMessage message) {
        try {
            log.info("处理文件上传审计消息: {} - {}", message.getUsername(), message.getDescription());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromUnified(message);
            auditLogRepository.save(auditLog);
            
            // 如果上传失败，可能需要创建安全事件
            if ("FAILURE".equals(message.getResult())) {
                createSecurityEventFromUnified(message);
            }
            
            log.debug("文件上传审计日志保存成功: {}", auditLog.getId());
            
        } catch (Exception e) {
            log.error("处理文件上传审计消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理搜索审计消息
     */
    @RabbitListener(queues = RabbitMQConfig.SEARCH_AUDIT_QUEUE)
    @Transactional
    public void handleSearchAudit(UnifiedSecurityMessage message) {
        try {
            log.debug("处理搜索审计消息: {} - {}", message.getUsername(), message.getDescription());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromUnified(message);
            auditLogRepository.save(auditLog);
            
            // 更新搜索统计
            updateSearchStatisticsFromUnified(message);
            
        } catch (Exception e) {
            log.error("处理搜索审计消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理访问控制审计消息
     */
    @RabbitListener(queues = RabbitMQConfig.ACCESS_CONTROL_QUEUE)
    @Transactional
    public void handleAccessControlAudit(UnifiedSecurityMessage message) {
        try {
            log.info("处理访问控制审计消息: {} - {}", message.getUsername(), message.getDescription());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromUnified(message);
            auditLogRepository.save(auditLog);
            
            // 如果访问被拒绝，创建安全事件
            if ("DENIED".equals(message.getResult())) {
                createSecurityEventFromUnified(message);
            }
            
            log.debug("访问控制审计日志保存成功: {}", auditLog.getId());
            
        } catch (Exception e) {
            log.error("处理访问控制审计消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理安全事件消息
     */
    @RabbitListener(queues = RabbitMQConfig.SECURITY_EVENT_QUEUE)
    @Transactional
    public void handleSecurityEvent(UnifiedSecurityMessage message) {
        try {
            log.warn("处理安全事件消息: {}", message.getDescription());

            
            // 创建安全事件
            createSecurityEvent(message);

            
            log.warn("安全事件处理完成: {}", message.getDescription());
            
        } catch (Exception e) {
            log.error("处理安全事件消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 私有辅助方法 ====================



    /**
     * 创建安全事件
     */
    private void createSecurityEvent(UnifiedSecurityMessage message) {
        try {
            SecurityEvent securityEvent = new SecurityEvent(message);
            securityEventRepository.save(securityEvent);
            log.info("安全事件创建成功: {}", securityEvent.getId());
        } catch (Exception e) {
            log.error("创建安全事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理登录失败事件（统一消息版本）
     */
    private void handleLoginFailureEventFromUnified(UnifiedSecurityMessage message) {
        try {
            String key = "login_failures:" + message.getIpAddress();
            Long failures = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            
            // 如果同一IP连续失败超过10次，创建安全事件
            if (failures != null && failures >= 10) {
                UnifiedSecurityMessage securityEvent = UnifiedSecurityMessage.securityEvent(
                    com.myweb.website_core.common.enums.SecurityEventType.BRUTE_FORCE_ATTACK,
                    com.myweb.website_core.common.enums.AuditOperation.SUSPICIOUS_ACTIVITY,
                    message.getUserId(),
                    message.getUsername(),
                    message.getIpAddress(),
                    "检测到可疑登录活动: IP " + message.getIpAddress() + " 连续登录失败 " + failures + " 次",
                    5
                );
                
                createSecurityEventFromUnified(securityEvent);
            }
        } catch (Exception e) {
            log.error("处理登录失败事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理账户锁定事件（统一消息版本）
     */
    private void handleAccountLockedEventFromUnified(UnifiedSecurityMessage message) {
        try {
            // 发送账户锁定通知邮件
            if (message.getUsername() != null) {
                emailService.sendVerificationCode(
                    message.getUsername() + "@example.com", // 这里应该从用户信息中获取真实邮箱
                    "您的账户已被锁定，请联系管理员或等待15分钟后重试。"
                );
            }
        } catch (Exception e) {
            log.error("处理账户锁定事件失败: {}", e.getMessage(), e);
        }
    }



    /**
     * 更新搜索统计信息（统一消息版本）
     */
    private void updateSearchStatisticsFromUnified(UnifiedSecurityMessage message) {
        try {
            String dateKey = "search_stats:" + LocalDateTime.now().toLocalDate();
            String userKey = dateKey + ":user:" + message.getUserId();
            
            redisTemplate.opsForValue().increment(dateKey + ":total");
            redisTemplate.opsForValue().increment(userKey);
            redisTemplate.expire(dateKey, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("更新搜索统计失败: {}", e.getMessage(), e);
        }
    }

    // ==================== 统一安全消息处理方法 ====================

    /**
     * 从统一安全消息创建审计日志实体
     */
    private AuditLog createAuditLogFromUnified(UnifiedSecurityMessage message) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(message.getUserId());
        auditLog.setUsername(message.getUsername());
        auditLog.setOperation(message.getOperation());
        auditLog.setResourceType(message.getResourceType());
        auditLog.setResourceId(message.getResourceId());
        auditLog.setIpAddress(message.getIpAddress());
        auditLog.setUserAgent(message.getUserAgent());
        auditLog.setResult(message.getResult());
        auditLog.setErrorMessage(message.getErrorMessage());
        auditLog.setRequestData(message.getRequestData());
        auditLog.setResponseData(message.getResponseData());
        auditLog.setExecutionTime(message.getExecutionTime());
        auditLog.setTimestamp(message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now());
        auditLog.setSessionId(message.getSessionId());
        auditLog.setRequestId(message.getRequestId());
        auditLog.setDescription(message.getDescription());
        auditLog.setRiskLevel(message.getRiskLevel());
        auditLog.setLocation(message.getLocation());
        auditLog.setTags(message.getTags());
        return auditLog;
    }

    /**
     * 从统一安全消息创建安全事件
     */
    private void createSecurityEventFromUnified(UnifiedSecurityMessage message) {
        try {
            SecurityEvent securityEvent = new SecurityEvent();
            securityEvent.setEventType(message.getSecurityEventType());
            securityEvent.setTitle(message.getTitle());
            securityEvent.setDescription(message.getDescription());
            securityEvent.setSeverity(message.getSeverity());
            securityEvent.setUserId(message.getUserId());
            securityEvent.setUsername(message.getUsername());
            securityEvent.setSourceIp(message.getIpAddress());
            securityEvent.setUserAgent(message.getUserAgent());
            securityEvent.setRequestUri(message.getRequestUri());
            securityEvent.setRequestMethod(message.getRequestMethod());
            securityEvent.setSessionId(message.getSessionId());
            securityEvent.setEventData(message.getEventData());
            securityEvent.setEventTime(message.getEventTime() != null ? message.getEventTime() : LocalDateTime.now());
            securityEvent.setStatus(message.getStatus() != null ? message.getStatus() : "NEW");
            securityEvent.setAlerted(message.getAlerted() != null ? message.getAlerted() : false);
            securityEvent.setAlertTime(message.getAlertTime());
            securityEvent.setRiskScore(message.getRiskScore());
            securityEvent.setRelatedEventCount(message.getRelatedEventCount());
            
            securityEventRepository.save(securityEvent);
            log.info("安全事件创建成功: {}", securityEvent.getId());
        } catch (Exception e) {
            log.error("创建安全事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新统一消息的审计统计信息
     */
    private void updateAuditStatisticsFromUnified(UnifiedSecurityMessage message) {
        try {
            String dateKey = "audit_stats:" + LocalDateTime.now().toLocalDate();
            String operationKey = dateKey + ":" + (message.getOperation() != null ? message.getOperation().getCode() : "UNKNOWN");
            String resultKey = dateKey + ":" + message.getResult();
            String typeKey = dateKey + ":type:" + message.getMessageType();
            
            redisTemplate.opsForValue().increment(operationKey);
            redisTemplate.opsForValue().increment(resultKey);
            redisTemplate.opsForValue().increment(typeKey);
            redisTemplate.expire(dateKey, 30, TimeUnit.DAYS);
            
            // 如果是安全事件，额外统计
            if (Boolean.TRUE.equals(message.getIsSecurityEvent())) {
                String eventKey = dateKey + ":security_event:" + message.getSecurityEventType().getCode();
                redisTemplate.opsForValue().increment(eventKey);
            }
        } catch (Exception e) {
            log.error("更新审计统计失败: {}", e.getMessage(), e);
        }
    }
} 