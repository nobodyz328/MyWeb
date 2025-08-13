package com.myweb.website_core.application.service.security.confirm;

import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 重要操作二次确认服务
 * <p>
 * 为敏感操作提供二次确认机制，包括：
 * - 删除操作确认
 * - 敏感配置修改确认
 * - 账户注销确认
 * - 权限变更确认
 * <p>
 * 符合GB/T 22239-2019二级等保要求的操作确认机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmationService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final AuditMessageService auditLogService;
    
    private static final String CONFIRMATION_TOKEN_PREFIX = "security:confirmation:";
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRE_MINUTES = 10; // 确认令牌10分钟过期
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * 操作类型枚举
     */
    @Getter
    public enum OperationType {
        DELETE_POST("删除帖子", "您正在删除帖子，此操作不可撤销"),
        DELETE_COMMENT("删除评论", "您正在删除评论，此操作不可撤销"),
        DELETE_USER("删除用户", "您正在删除用户账户，此操作不可撤销"),
        DEACTIVATE_ACCOUNT("注销账户", "您正在注销账户，此操作不可撤销"),
        CHANGE_ROLE("变更角色", "您正在变更用户角色，请确认此操作"),
        SYSTEM_CONFIG("系统配置", "您正在修改系统配置，请确认此操作"),
        DATA_EXPORT("数据导出", "您正在导出敏感数据，请确认此操作"),
        SECURITY_POLICY("安全策略", "您正在修改安全策略，请确认此操作");
        
        private final String displayName;
        private final String description;
        
        OperationType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

    }
    
    /**
     * 确认令牌信息
     */
    @Getter
    public static class ConfirmationToken {
        // Getters
        private final String token;
        private final String userId;
        private final OperationType operationType;
        private final String resourceId;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        
        public ConfirmationToken(String token, String userId, OperationType operationType, 
                               String resourceId, LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.token = token;
            this.userId = userId;
            this.operationType = operationType;
            this.resourceId = resourceId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
    
    /**
     * 生成确认令牌
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param resourceId 资源ID（可选）
     * @return 确认令牌
     */
    public ConfirmationToken generateConfirmationToken(String userId, OperationType operationType, String resourceId) {
        try {
            // 生成随机令牌
            String token = generateSecureToken();
            
            // 设置过期时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(TOKEN_EXPIRE_MINUTES);
            
            // 创建令牌对象
            ConfirmationToken confirmationToken = new ConfirmationToken(
                token, userId, operationType, resourceId, now, expiresAt
            );
            
            // 存储到Redis
            String cacheKey = CONFIRMATION_TOKEN_PREFIX + token;
            redisTemplate.opsForValue().set(cacheKey, confirmationToken, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            // 记录审计日志
            auditLogService.logOperation(
                    AuditLogRequest.system(
                            AuditOperation.SYSTEM_MONITOR,
                            "生成确认令牌: " + operationType.getDisplayName()
                    )
            );
            
            log.info("生成确认令牌成功 - 用户: {}, 操作: {}, 令牌: {}", 
                    userId, operationType.getDisplayName(), token);
            
            return confirmationToken;
            
        } catch (Exception e) {
            log.error("生成确认令牌失败 - 用户: {}, 操作: {}", userId, operationType.getDisplayName(), e);
            throw new ValidationException("生成确认令牌失败");
        }
    }
    
    /**
     * 验证确认令牌
     * 
     * @param token 确认令牌
     * @return 令牌信息，如果无效则返回null
     */
    public ConfirmationToken validateConfirmationToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }
            
            String cacheKey = CONFIRMATION_TOKEN_PREFIX + token;
            ConfirmationToken confirmationToken = (ConfirmationToken) redisTemplate.opsForValue().get(cacheKey);
            
            if (confirmationToken == null) {
                log.warn("确认令牌不存在或已过期: {}", token);
                return null;
            }
            
            if (confirmationToken.isExpired()) {
                // 删除过期令牌
                redisTemplate.delete(cacheKey);
                log.warn("确认令牌已过期: {}", token);
                return null;
            }
            
            log.info("确认令牌验证成功: {}", token);
            return confirmationToken;
            
        } catch (Exception e) {
            log.error("验证确认令牌失败: {}", token, e);
            return null;
        }
    }
    
    /**
     * 消费确认令牌（验证后删除）
     * 
     * @param token 确认令牌
     * @return 令牌信息，如果无效则返回null
     */
    public ConfirmationToken consumeConfirmationToken(String token) {
        try {
            ConfirmationToken confirmationToken = validateConfirmationToken(token);
            if (confirmationToken == null) {
                return null;
            }
            
            // 删除令牌
            String cacheKey = CONFIRMATION_TOKEN_PREFIX + token;
            redisTemplate.delete(cacheKey);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                        AuditOperation.SYSTEM_MONITOR,
                        "消费确认令牌: " + confirmationToken.getOperationType().getDisplayName()
                )
            );
            
            log.info("确认令牌消费成功 - 用户: {}, 操作: {}", 
                    confirmationToken.getUserId(), confirmationToken.getOperationType().getDisplayName());
            
            return confirmationToken;
            
        } catch (Exception e) {
            log.error("消费确认令牌失败: {}", token, e);
            return null;
        }
    }
    
    /**
     * 发送邮件确认
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param resourceId 资源ID
     * @return 确认令牌
     */
    public ConfirmationToken sendEmailConfirmation(String userId, OperationType operationType, String resourceId) {
        try {
            // 查找用户
            User user = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new ValidationException("用户不存在"));
            
            if (user.getEmail() == null || !user.getEmailVerified()) {
                throw new ValidationException("用户邮箱未验证，无法发送确认邮件");
            }
            
            // 生成确认令牌
            ConfirmationToken token = generateConfirmationToken(userId, operationType, resourceId);
            
            // 构建确认链接
            String confirmationUrl = buildConfirmationUrl(token.getToken());
            
            // 发送邮件
            String subject = "重要操作确认 - " + operationType.getDisplayName();
            String content = buildConfirmationEmailContent(user.getUsername(), operationType, confirmationUrl);
            
            emailService.sendEmail(user.getEmail(), subject, content);
            
            log.info("发送确认邮件成功 - 用户: {}, 邮箱: {}, 操作: {}", 
                    userId, user.getEmail(), operationType.getDisplayName());
            
            return token;
            
        } catch (Exception e) {
            log.error("发送确认邮件失败 - 用户: {}, 操作: {}", userId, operationType.getDisplayName(), e);
            throw new ValidationException("发送确认邮件失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查操作是否需要确认
     * 
     * @param operationType 操作类型
     * @param userId 用户ID
     * @return 是否需要确认
     */
    public boolean requiresConfirmation(OperationType operationType, String userId) {
        try {
            // 所有删除操作都需要确认
            if (operationType == OperationType.DELETE_POST || 
                operationType == OperationType.DELETE_COMMENT ||
                operationType == OperationType.DELETE_USER ||
                operationType == OperationType.DEACTIVATE_ACCOUNT) {
                return true;
            }
            
            // 管理员操作需要确认
            User user = userRepository.findById(Long.valueOf(userId)).orElse(null);
            if (user != null && user.hasManagementPermission()) {
                return operationType == OperationType.CHANGE_ROLE ||
                       operationType == OperationType.SYSTEM_CONFIG ||
                       operationType == OperationType.SECURITY_POLICY;
            }
            
            // 敏感数据操作需要确认
            return operationType == OperationType.DATA_EXPORT;
            
        } catch (Exception e) {
            log.error("检查确认需求失败 - 用户: {}, 操作: {}", userId, operationType, e);
            return true; // 出错时默认需要确认
        }
    }
    
    /**
     * 生成安全令牌
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        
        StringBuilder token = new StringBuilder();
        for (byte b : tokenBytes) {
            token.append(String.format("%02x", b & 0xff));
        }
        
        return token.toString();
    }
    
    /**
     * 构建确认URL
     */
    private String buildConfirmationUrl(String token) {
        // 这里应该根据实际的前端路由配置
        return "https://localhost:8443/blog//api/security/confirmation?token=" + token;
    }
    
    /**
     * 构建确认邮件内容
     */
    private String buildConfirmationEmailContent(String username, OperationType operationType, String confirmationUrl) {
        return String.format("""
            用户: %s：
            
            您正在执行一个重要操作：%s
            
            %s
            
            如果这是您本人的操作，请点击以下链接确认：
            %s
            
            此链接将在 %d 分钟后失效。
            
            如果这不是您本人的操作，请立即联系管理员。
            
            MyWeb博客系统
            %s
            """, 
            username,
            operationType.getDisplayName(),
            operationType.getDescription(),
            confirmationUrl,
            TOKEN_EXPIRE_MINUTES,
            LocalDateTime.now()
        );
    }
}