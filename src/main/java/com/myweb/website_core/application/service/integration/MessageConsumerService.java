package com.myweb.website_core.application.service.integration;

import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.PostCollect;
import com.myweb.website_core.domain.business.entity.PostLike;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.UserFollow;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import com.myweb.website_core.infrastructure.config.RabbitMQConfig;
import com.myweb.website_core.infrastructure.persistence.repository.PostCollectRepository;
import com.myweb.website_core.infrastructure.persistence.repository.PostLikeRepository;
import com.myweb.website_core.infrastructure.persistence.repository.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.infrastructure.persistence.repository.UserFollowRepository;
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
    private final UserFollowRepository userFollowRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityEventRepository securityEventRepository;

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
     * 处理用户关注消息
     */
    @RabbitListener(queues = RabbitMQConfig.INTERACTION_FOLLOW_QUEUE)
    public void handleUserFollow(Map<String, Object> message) {
        try {
            log.info("处理用户关注消息: {}", message);

            long targetUserId = Long.parseLong(message.get("targetUserId").toString());
            long userId = Long.parseLong(message.get("userId").toString());
            boolean isFollowed = Boolean.parseBoolean(message.get("isFollowed").toString());

            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("目标用户不存在"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (isFollowed) {
                // 添加关注关系
                UserFollow userFollow = new UserFollow(userId, targetUserId);
                userFollowRepository.save(userFollow);
                log.info("用户 {} 关注了用户 {}", userId, targetUserId);
            } else {
                // 取消关注关系
                userFollowRepository.deleteByFollowerIdAndFollowingId(userId, targetUserId);
                log.info("用户 {} 取消关注了用户 {}", userId, targetUserId);
            }
            user.setFollowCount(userFollowRepository.countByFollowerId(userId));
            targetUser.setFollowerCount(userFollowRepository.countByFollowingId(targetUserId));
        } catch (Exception e) {
            log.error("处理用户关注消息失败", e);
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
    public void handleSecurityAudit(AuditLogRequest request) {
        try {
            log.info("处理安全审计消息: {} - {}", request.getOperation(), request.getUsername());
            
            // 创建审计日志实体
            AuditLog auditLog = createAuditLogFromRequest(request);
            
            // 保存审计日志
            auditLogRepository.save(auditLog);
            
            // 更新Redis中的审计统计
            updateAuditStatisticsFromRequest(request);
            
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
    public void handleUserAuthAudit(AuditLogRequest request) {
        try {
            log.info("处理用户认证审计消息: {} - {}", request.getOperation(), request.getUsername());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromRequest(request);
            auditLogRepository.save(auditLog);
            
            // 特殊处理登录失败事件
            if (request.getOperation() == com.myweb.website_core.common.enums.AuditOperation.USER_LOGIN_FAILURE) {
                handleLoginFailureEventFromRequest(request);
            }
            
            // 特殊处理账户锁定事件
            if (request.getOperation() == com.myweb.website_core.common.enums.AuditOperation.ACCOUNT_LOCKED) {
                handleAccountLockedEventFromRequest(request);
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
    public void handleFileUploadAudit(AuditLogRequest request) {
        try {
            log.info("处理文件上传审计消息: {} - {}", request.getUsername(), request.getDescription());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromRequest(request);
            auditLogRepository.save(auditLog);
            
            // 如果上传失败，可能需要创建安全事件
            if ("FAILURE".equals(request.getResult())) {
                createSecurityEventFromRequest(request);
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
    public void handleSearchAudit(AuditLogRequest request) {
        try {
            log.debug("处理搜索审计消息: {} - {}", request.getUsername(), request.getDescription());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromRequest(request);
            auditLogRepository.save(auditLog);
            
            // 更新搜索统计
            updateSearchStatisticsFromRequest(request);
            
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
    public void handleAccessControlAudit(AuditLogRequest request) {
        try {
            log.info("处理访问控制审计消息: {} - {}", request.getUsername(), request.getDescription());
            
            // 创建审计日志
            AuditLog auditLog = createAuditLogFromRequest(request);
            auditLogRepository.save(auditLog);
            
            // 如果访问被拒绝，创建安全事件
            if ("DENIED".equals(request.getResult())) {
                createSecurityEventFromRequest(request);
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
    public void handleSecurityEvent(SecurityEventRequest request) {
        try {
            // 创建安全事件
            createSecurityEventFromRequest(request);
            
        } catch (Exception e) {
            log.error("处理安全事件消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从AuditLogRequest创建审计日志实体
     */
    private AuditLog createAuditLogFromRequest(AuditLogRequest request) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(request.getUserId());
        auditLog.setUsername(request.getUsername());
        auditLog.setOperation(request.getOperation());
        auditLog.setResourceType(request.getResourceType());
        auditLog.setResourceId(request.getResourceId());
        auditLog.setIpAddress(request.getIpAddress());
        auditLog.setUserAgent(request.getUserAgent());
        auditLog.setResult(request.getResult());
        auditLog.setErrorMessage(request.getErrorMessage());
        //requestData和responseData需要特殊处理，因为AuditLogRequest中的类型可能不同
        auditLog.setRequestData(request.getRequestData() != null ? request.getRequestData().toString() : null);
        auditLog.setResponseData(request.getResponseData() != null ? request.getResponseData().toString() : null);
        auditLog.setExecutionTime(request.getExecutionTime());
        auditLog.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        auditLog.setSessionId(request.getSessionId());
        auditLog.setRequestId(request.getRequestId());
        auditLog.setDescription(request.getDescription());
        auditLog.setRiskLevel(request.getRiskLevel());
        auditLog.setTags(request.getTags());
        return auditLog;
    }

    /**
     * 从SecurityEventRequest创建安全事件
     */
    private void createSecurityEventFromRequest(SecurityEventRequest request) {
        try {
            SecurityEvent securityEvent = new SecurityEvent();
            securityEvent.setEventType(request.getEventType());
            securityEvent.setTitle(request.getTitle());
            securityEvent.setDescription(request.getDescription());
            securityEvent.setSeverity(request.getRiskScore() != null ? request.getRiskScore() / 20 : null);
            securityEvent.setUserId(request.getUserId());
            securityEvent.setUsername(request.getUsername());
            securityEvent.setSourceIp(request.getSourceIp());
            securityEvent.setUserAgent(request.getUserAgent());
            securityEvent.setSessionId(request.getSessionId());
            securityEvent.setEventTime(request.getEventTime() != null ? request.getEventTime() : LocalDateTime.now());
            securityEvent.setStatus("NEW");
            securityEvent.setAlerted(false);
            securityEvent.setRiskScore(request.getRiskScore());
            securityEvent.setRequestUri(request.getRequestUri());
            securityEvent.setRequestMethod(request.getRequestMethod());
            // eventData需要特殊处理
            securityEvent.setEventData(request.getEventData() != null ? request.getEventData().toString() : null);
            
            securityEventRepository.save(securityEvent);
            log.info("安全事件创建成功: {}", securityEvent.getId());
        } catch (Exception e) {
            log.error("创建安全事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从AuditLogRequest创建安全事件
     */
    private void createSecurityEventFromRequest(AuditLogRequest request) {
        try {
            SecurityEvent securityEvent = new SecurityEvent();
            // 从AuditLogRequest中提取安全事件信息
            securityEvent.setTitle(request.getOperation() != null ? request.getOperation().getName() : "安全事件");
            securityEvent.setDescription(request.getDescription());
            securityEvent.setSeverity(request.getRiskLevel());
            securityEvent.setUserId(request.getUserId());
            securityEvent.setUsername(request.getUsername());
            securityEvent.setSourceIp(request.getIpAddress());
            securityEvent.setUserAgent(request.getUserAgent());
            securityEvent.setSessionId(request.getSessionId());
            securityEvent.setEventTime(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
            securityEvent.setStatus("NEW");
            securityEvent.setAlerted(false);
            securityEvent.setRiskScore(request.getRiskLevel() != null ? request.getRiskLevel() * 20 : 60);
            
            securityEventRepository.save(securityEvent);
            log.info("安全事件创建成功: {}", securityEvent.getId());
        } catch (Exception e) {
            log.error("创建安全事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新AuditLogRequest的审计统计信息
     */
    private void updateAuditStatisticsFromRequest(AuditLogRequest request) {
        try {
            String dateKey = "audit_stats:" + LocalDateTime.now().toLocalDate();
            String operationKey = dateKey + ":" + (request.getOperation() != null ? request.getOperation().getCode() : "UNKNOWN");
            String resultKey = dateKey + ":" + request.getResult();
            String typeKey = dateKey + ":type:" + "AUDIT_LOG"; // 默认类型
            
            redisTemplate.opsForValue().increment(operationKey);
            redisTemplate.opsForValue().increment(resultKey);
            redisTemplate.opsForValue().increment(typeKey);
            redisTemplate.expire(dateKey, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("更新审计统计失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理登录失败事件（AuditLogRequest版本）
     */
    private void handleLoginFailureEventFromRequest(AuditLogRequest request) {
        try {
            String key = "login_failures:" + request.getIpAddress();
            Long failures = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            
            // 如果同一IP连续失败超过10次，创建安全事件
            if (failures != null && failures >= 10) {
                SecurityEventRequest securityEvent = SecurityEventRequest.builder()
                        .eventType(com.myweb.website_core.common.enums.SecurityEventType.BRUTE_FORCE_ATTACK)
                        .title("暴力破解攻击")
                        .description("检测到可疑登录活动: IP " + request.getIpAddress() + " 连续登录失败 " + failures + " 次")
                        .userId(request.getUserId())
                        .username(request.getUsername())
                        .sourceIp(request.getIpAddress())
                        .riskScore(100)
                        .eventTime(LocalDateTime.now())
                        .build();
                
                createSecurityEventFromRequest(securityEvent);
            }
        } catch (Exception e) {
            log.error("处理登录失败事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理账户锁定事件（AuditLogRequest版本）
     */
    private void handleAccountLockedEventFromRequest(AuditLogRequest request) {
        try {
            // 发送账户锁定通知邮件
            if (request.getUsername() != null) {
                emailService.sendVerificationCode(
                    request.getUsername() + "@example.com", // 这里应该从用户信息中获取真实邮箱
                    "您的账户已被锁定，请联系管理员或等待15分钟后重试。"
                );
            }
        } catch (Exception e) {
            log.error("处理账户锁定事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新搜索统计信息（AuditLogRequest版本）
     */
    private void updateSearchStatisticsFromRequest(AuditLogRequest request) {
        try {
            String dateKey = "search_stats:" + LocalDateTime.now().toLocalDate();
            String userKey = dateKey + ":user:" + request.getUserId();
            
            redisTemplate.opsForValue().increment(dateKey + ":total");
            redisTemplate.opsForValue().increment(userKey);
            redisTemplate.expire(dateKey, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("更新搜索统计失败: {}", e.getMessage(), e);
        }
    }
}