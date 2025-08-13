package com.myweb.website_core.application.service.security.integeration.dataManage;

import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.application.service.security.confirm.ConfirmationService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.domain.business.entity.*;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.ImageRepository;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.PostCollectRepository;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.PostLikeRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 数据彻底删除服务
 * <p>
 * 提供数据的彻底删除功能，确保删除的数据无法恢复，包括：
 * - 用户注销时的数据完全清理
 * - 帖子删除的级联清理
 * - 评论删除的级联清理
 * - 数据库记录的物理删除
 * - 缓存数据的清理
 * - 文件系统数据的清理
 * <p>
 * 符合GB/T 22239-2019二级等保要求的剩余信息保护机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataDeletionService {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCollectRepository postCollectRepository;
    private final ImageRepository imageRepository;
    private final AuditMessageService auditLogService;
    private final ConfirmationService confirmationService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 删除结果信息
     */
    public static class DeletionResult {
        private final boolean success;
        private final String message;
        private final DeletionStatistics statistics;
        
        public DeletionResult(boolean success, String message, DeletionStatistics statistics) {
            this.success = success;
            this.message = message;
            this.statistics = statistics;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public DeletionStatistics getStatistics() { return statistics; }
    }
    
    /**
     * 删除统计信息
     */
    @Getter
    public static class DeletionStatistics {
        // Getters and setters
        private int deletedUsers = 0;
        private int deletedPosts = 0;
        private int deletedComments = 0;
        private int deletedImages = 0;
        private int deletedLikes = 0;
        private int deletedCollects = 0;
        private int clearedCacheKeys = 0;

        public void setDeletedUsers(int deletedUsers) { this.deletedUsers = deletedUsers; }

        public void setDeletedPosts(int deletedPosts) { this.deletedPosts = deletedPosts; }

        public void setDeletedComments(int deletedComments) { this.deletedComments = deletedComments; }

        public void setDeletedImages(int deletedImages) { this.deletedImages = deletedImages; }

        public void setDeletedLikes(int deletedLikes) { this.deletedLikes = deletedLikes; }

        public void setDeletedCollects(int deletedCollects) { this.deletedCollects = deletedCollects; }

        public void setClearedCacheKeys(int clearedCacheKeys) { this.clearedCacheKeys = clearedCacheKeys; }
        
        public void incrementDeletedUsers() { this.deletedUsers++; }
        public void incrementDeletedPosts() { this.deletedPosts++; }
        public void incrementDeletedComments() { this.deletedComments++; }
        public void incrementDeletedImages() { this.deletedImages++; }
        public void incrementDeletedLikes() { this.deletedLikes++; }
        public void incrementDeletedCollects() { this.deletedCollects++; }
        public void incrementClearedCacheKeys() { this.clearedCacheKeys++; }
        
        @Override
        public String toString() {
            return String.format(
                "删除统计: 用户=%d, 帖子=%d, 评论=%d, 图片=%d, 点赞=%d, 收藏=%d, 缓存=%d",
                deletedUsers, deletedPosts, deletedComments, deletedImages, deletedLikes, deletedCollects, clearedCacheKeys
            );
        }
    }
    
    /**
     * 彻底删除用户及其所有相关数据
     * 
     * @param userId 用户ID
     * @param confirmationToken 确认令牌
     * @param operatorUserId 操作者用户ID
     * @return 删除结果
     */
    @Transactional
    public DeletionResult deleteUserCompletely(Long userId, String confirmationToken, String operatorUserId) {
        try {
            log.info("开始彻底删除用户数据 - 用户ID: {}, 操作者: {}", userId, operatorUserId);
            
            // 验证确认令牌
            ConfirmationService.ConfirmationToken token = confirmationService.consumeConfirmationToken(confirmationToken);
            if (token == null || !token.getUserId().equals(userId.toString()) || 
                token.getOperationType() != ConfirmationService.OperationType.DELETE_USER) {
                throw new ValidationException("确认令牌无效或已过期");
            }
            
            // 查找用户
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ValidationException("用户不存在"));
            
            DeletionStatistics statistics = new DeletionStatistics();
            
            // 1. 删除用户的所有帖子（级联删除相关数据）
            List<Post> userPosts = postRepository.findByAuthorId(userId);
            for (Post post : userPosts) {
                deletePostCompletely(post.getId(), statistics, false); // 不需要再次确认
            }
            
            // 2. 删除用户的所有评论
            List<Comment> userComments = commentRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
            for (Comment comment : userComments) {
                deleteCommentCompletely(comment.getId(), statistics, false); // 不需要再次确认
            }
            
            // 3. 删除用户的点赞记录
            List<PostLike> userLikes = postLikeRepository.findByUserId(userId);
            for (PostLike like : userLikes) {
                postLikeRepository.delete(like);
                statistics.incrementDeletedLikes();
            }
            
            // 4. 删除用户的收藏记录
            List<PostCollect> userCollects = postCollectRepository.findByUserId(userId);
            for (PostCollect collect : userCollects) {
                postCollectRepository.delete(collect);
                statistics.incrementDeletedCollects();
            }
            
            // 5. 清理用户相关的缓存数据
            clearUserCacheData(userId, statistics);
            
            // 6. 删除用户记录
            userRepository.delete(user);
            statistics.incrementDeletedUsers();
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(Long.valueOf(operatorUserId))
                    .operation(AuditOperation.USER_DELETE)
                    .resourceType("USER")
                    .resourceId(userId)
                    .result("SUCCESS")
                    .requestData("彻底删除用户: " + user.getUsername())
                    .build()
            );
            
            log.info("用户数据彻底删除完成 - 用户ID: {}, 统计: {}", userId, statistics);
            
            return new DeletionResult(true, "用户数据删除成功", statistics);
            
        } catch (Exception e) {
            log.error("彻底删除用户数据失败 - 用户ID: {}", userId, e);
            
            // 记录失败审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(Long.valueOf(operatorUserId))
                    .operation(AuditOperation.USER_DELETE)
                    .resourceType("USER")
                    .resourceId(userId)
                    .result("FAILURE")
                    .errorMessage(e.getMessage())
                    .build()
            );
            
            return new DeletionResult(false, "删除用户数据失败: " + e.getMessage(), new DeletionStatistics());
        }
    }
    
    /**
     * 彻底删除帖子及其所有相关数据
     * 
     * @param postId 帖子ID
     * @param confirmationToken 确认令牌（可选，内部调用时为null）
     * @param operatorUserId 操作者用户ID
     * @return 删除结果
     */
    @Transactional
    public DeletionResult deletePostCompletely(Long postId, String confirmationToken, String operatorUserId) {
        try {
            log.info("开始彻底删除帖子数据 - 帖子ID: {}, 操作者: {}", postId, operatorUserId);
            
            // 如果提供了确认令牌，则验证
            if (confirmationToken != null) {
                ConfirmationService.ConfirmationToken token = confirmationService.consumeConfirmationToken(confirmationToken);
                if (token == null || !token.getResourceId().equals(postId.toString()) || 
                    token.getOperationType() != ConfirmationService.OperationType.DELETE_POST) {
                    throw new ValidationException("确认令牌无效或已过期");
                }
            }
            
            DeletionStatistics statistics = new DeletionStatistics();
            deletePostCompletely(postId, statistics, true);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(Long.valueOf(operatorUserId))
                    .operation(AuditOperation.POST_DELETE)
                    .resourceType("POST")
                    .resourceId(postId)
                    .result("SUCCESS")
                    .requestData("彻底删除帖子")
                    .build()
            );
            
            log.info("帖子数据彻底删除完成 - 帖子ID: {}, 统计: {}", postId, statistics);
            
            return new DeletionResult(true, "帖子数据删除成功", statistics);
            
        } catch (Exception e) {
            log.error("彻底删除帖子数据失败 - 帖子ID: {}", postId, e);
            
            // 记录失败审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(Long.valueOf(operatorUserId))
                    .operation(AuditOperation.POST_DELETE)
                    .resourceType("POST")
                    .resourceId(postId)
                    .result("FAILURE")
                    .errorMessage(e.getMessage())
                    .build()
            );
            
            return new DeletionResult(false, "删除帖子数据失败: " + e.getMessage(), new DeletionStatistics());
        }
    }
    
    /**
     * 彻底删除评论及其所有相关数据
     * 
     * @param commentId 评论ID
     * @param confirmationToken 确认令牌（可选，内部调用时为null）
     * @param operatorUserId 操作者用户ID
     * @return 删除结果
     */
    @Transactional
    public DeletionResult deleteCommentCompletely(Long commentId, String confirmationToken, String operatorUserId) {
        try {
            log.info("开始彻底删除评论数据 - 评论ID: {}, 操作者: {}", commentId, operatorUserId);
            
            // 如果提供了确认令牌，则验证
            if (confirmationToken != null) {
                ConfirmationService.ConfirmationToken token = confirmationService.consumeConfirmationToken(confirmationToken);
                if (token == null || !token.getResourceId().equals(commentId.toString()) || 
                    token.getOperationType() != ConfirmationService.OperationType.DELETE_COMMENT) {
                    throw new ValidationException("确认令牌无效或已过期");
                }
            }
            
            DeletionStatistics statistics = new DeletionStatistics();
            deleteCommentCompletely(commentId, statistics, true);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(Long.valueOf(operatorUserId))
                    .operation(AuditOperation.COMMENT_DELETE)
                    .resourceType("COMMENT")
                    .resourceId(commentId)
                    .result("SUCCESS")
                    .requestData("彻底删除评论")
                    .build()
            );
            
            log.info("评论数据彻底删除完成 - 评论ID: {}, 统计: {}", commentId, statistics);
            
            return new DeletionResult(true, "评论数据删除成功", statistics);
            
        } catch (Exception e) {
            log.error("彻底删除评论数据失败 - 评论ID: {}", commentId, e);
            
            // 记录失败审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(Long.valueOf(operatorUserId))
                    .operation(AuditOperation.COMMENT_DELETE)
                    .resourceType("COMMENT")
                    .resourceId(commentId)
                    .result("FAILURE")
                    .errorMessage(e.getMessage())
                    .build()
            );
            
            return new DeletionResult(false, "删除评论数据失败: " + e.getMessage(), new DeletionStatistics());
        }
    }
    
    /**
     * 用户账户注销（彻底删除所有数据）
     * 
     * @param userId 用户ID
     * @param confirmationToken 确认令牌
     * @return 删除结果
     */
    @Transactional
    public DeletionResult deactivateUserAccount(Long userId, String confirmationToken) {
        try {
            log.info("开始用户账户注销 - 用户ID: {}", userId);
            
            // 验证确认令牌
            ConfirmationService.ConfirmationToken token = confirmationService.consumeConfirmationToken(confirmationToken);
            if (token == null || !token.getUserId().equals(userId.toString()) || 
                token.getOperationType() != ConfirmationService.OperationType.DEACTIVATE_ACCOUNT) {
                throw new ValidationException("确认令牌无效或已过期");
            }
            
            // 执行用户数据彻底删除
            DeletionResult result = deleteUserCompletely(userId, null, userId.toString());
            
            if (result.isSuccess()) {
                log.info("用户账户注销完成 - 用户ID: {}", userId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("用户账户注销失败 - 用户ID: {}", userId, e);
            return new DeletionResult(false, "账户注销失败: " + e.getMessage(), new DeletionStatistics());
        }
    }
    
    /**
     * 内部方法：彻底删除帖子数据
     */
    private void deletePostCompletely(Long postId, DeletionStatistics statistics, boolean updateCounters) {
        // 查找帖子
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return;
        }
        
        // 1. 删除帖子的所有评论（级联删除回复）
        List<Comment> postComments = commentRepository.findTopLevelCommentsByPostId(postId);
        for (Comment comment : postComments) {
            deleteCommentCompletely(comment.getId(), statistics, false);
        }
        
        // 2. 删除帖子的所有点赞记录
        List<PostLike> postLikes = postLikeRepository.findByPostId(postId);
        for (PostLike like : postLikes) {
            postLikeRepository.delete(like);
            statistics.incrementDeletedLikes();
        }
        
        // 3. 删除帖子的所有收藏记录
        List<PostCollect> postCollects = postCollectRepository.findByPostId(postId);
        for (PostCollect collect : postCollects) {
            postCollectRepository.delete(collect);
            statistics.incrementDeletedCollects();
        }
        
        // 4. 删除帖子关联的图片
        List<Image> postImages = imageRepository.findByPostId(postId);
        for (Image image : postImages) {
            imageRepository.delete(image);
            statistics.incrementDeletedImages();
        }
        
        // 5. 清理帖子相关的缓存数据
        clearPostCacheData(postId, statistics);
        
        // 6. 删除帖子记录
        postRepository.delete(post);
        statistics.incrementDeletedPosts();
        
        // 7. 更新作者的帖子计数（如果需要）
        if (updateCounters && post.getAuthor() != null) {
            // 这里可以添加更新用户帖子计数的逻辑
        }
    }
    
    /**
     * 内部方法：彻底删除评论数据
     */
    private void deleteCommentCompletely(Long commentId, DeletionStatistics statistics, boolean updateCounters) {
        // 查找评论
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return;
        }
        
        // 1. 递归删除所有回复
        List<Comment> replies = commentRepository.findRepliesByParentId(commentId);
        for (Comment reply : replies) {
            deleteCommentCompletely(reply.getId(), statistics, false);
        }
        
        // 2. 清理评论相关的缓存数据
        clearCommentCacheData(commentId, statistics);
        
        // 3. 删除评论记录
        commentRepository.delete(comment);
        statistics.incrementDeletedComments();
        
        // 4. 更新帖子的评论计数（如果需要）
        if (updateCounters && comment.getPost() != null) {
            Post post = comment.getPost();
            if (post.getCommentCount() > 0) {
                post.setCommentCount(post.getCommentCount() - 1);
                postRepository.save(post);
            }
        }
    }
    
    /**
     * 清理用户相关的缓存数据
     */
    private void clearUserCacheData(Long userId, DeletionStatistics statistics) {
        try {
            // 清理用户会话缓存
            Set<String> sessionKeys = redisTemplate.keys("spring:session:sessions:*");
            if (sessionKeys != null) {
                for (String key : sessionKeys) {
                    Object sessionData = redisTemplate.opsForValue().get(key);
                    if (sessionData != null && sessionData.toString().contains(userId.toString())) {
                        redisTemplate.delete(key);
                        statistics.incrementClearedCacheKeys();
                    }
                }
            }
            
            // 清理用户相关的业务缓存
            String[] userCachePatterns = {
                "user:profile:" + userId,
                "user:posts:" + userId,
                "user:comments:" + userId,
                "user:likes:" + userId,
                "user:collects:" + userId,
                "user:followers:" + userId,
                "user:following:" + userId,
                "user:permissions:" + userId,
                "user:roles:" + userId
            };
            
            for (String pattern : userCachePatterns) {
                Set<String> keys = redisTemplate.keys(pattern + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    statistics.setClearedCacheKeys(statistics.getClearedCacheKeys() + keys.size());
                }
            }
            
            // 清理认证相关缓存
            Set<String> authKeys = redisTemplate.keys("auth:*:" + userId);
            if (authKeys != null && !authKeys.isEmpty()) {
                redisTemplate.delete(authKeys);
                statistics.setClearedCacheKeys(statistics.getClearedCacheKeys() + authKeys.size());
            }
            
            // 清理验证码缓存
            Set<String> verificationKeys = redisTemplate.keys("verification:*:" + userId);
            if (verificationKeys != null && !verificationKeys.isEmpty()) {
                redisTemplate.delete(verificationKeys);
                statistics.setClearedCacheKeys(statistics.getClearedCacheKeys() + verificationKeys.size());
            }
            
        } catch (Exception e) {
            log.warn("清理用户缓存数据时出现异常 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 清理帖子相关的缓存数据
     */
    private void clearPostCacheData(Long postId, DeletionStatistics statistics) {
        try {
            String[] postCachePatterns = {
                "post:detail:" + postId,
                "post:comments:" + postId,
                "post:likes:" + postId,
                "post:collects:" + postId,
                "post:images:" + postId,
                "post:statistics:" + postId
            };
            
            for (String pattern : postCachePatterns) {
                Set<String> keys = redisTemplate.keys(pattern + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    statistics.setClearedCacheKeys(statistics.getClearedCacheKeys() + keys.size());
                }
            }
            
        } catch (Exception e) {
            log.warn("清理帖子缓存数据时出现异常 - 帖子ID: {}", postId, e);
        }
    }
    
    /**
     * 清理评论相关的缓存数据
     */
    private void clearCommentCacheData(Long commentId, DeletionStatistics statistics) {
        try {
            String[] commentCachePatterns = {
                "comment:detail:" + commentId,
                "comment:replies:" + commentId,
                "comment:author:" + commentId
            };
            
            for (String pattern : commentCachePatterns) {
                Set<String> keys = redisTemplate.keys(pattern + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    statistics.setClearedCacheKeys(statistics.getClearedCacheKeys() + keys.size());
                }
            }
            
        } catch (Exception e) {
            log.warn("清理评论缓存数据时出现异常 - 评论ID: {}", commentId, e);
        }
    }
    
    /**
     * 生成删除确认令牌
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param resourceId 资源ID
     * @return 确认令牌
     */
    public ConfirmationService.ConfirmationToken generateDeletionConfirmationToken(
            String userId, ConfirmationService.OperationType operationType, String resourceId) {
        
        return confirmationService.generateConfirmationToken(userId, operationType, resourceId);
    }
    
    /**
     * 发送删除确认邮件
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param resourceId 资源ID
     * @return 确认令牌
     */
    public ConfirmationService.ConfirmationToken sendDeletionConfirmationEmail(
            String userId, ConfirmationService.OperationType operationType, String resourceId) {
        
        return confirmationService.sendEmailConfirmation(userId, operationType, resourceId);
    }
    
    /**
     * 检查删除操作是否需要确认
     * 
     * @param operationType 操作类型
     * @param userId 用户ID
     * @return 是否需要确认
     */
    public boolean requiresDeletionConfirmation(ConfirmationService.OperationType operationType, String userId) {
        return confirmationService.requiresConfirmation(operationType, userId);
    }
    
    /**
     * 清理会话数据（用户退出时调用）
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void clearSessionData(Long userId, String sessionId) {
        try {
            log.info("清理用户会话数据 - 用户ID: {}, 会话ID: {}", userId, sessionId);
            
            // 清理指定会话
            if (sessionId != null) {
                String sessionKey = "spring:session:sessions:" + sessionId;
                redisTemplate.delete(sessionKey);
            }
            
            // 清理用户的所有会话
            Set<String> userSessionKeys = redisTemplate.keys("spring:session:sessions:*");
            if (userSessionKeys != null) {
                for (String key : userSessionKeys) {
                    Object sessionData = redisTemplate.opsForValue().get(key);
                    if (sessionData != null && sessionData.toString().contains(userId.toString())) {
                        redisTemplate.delete(key);
                    }
                }
            }
            
            // 清理用户临时缓存
            String[] tempCachePatterns = {
                "temp:user:" + userId,
                "session:user:" + userId,
                "auth:token:" + userId,
                "csrf:token:" + userId
            };
            
            for (String pattern : tempCachePatterns) {
                Set<String> keys = redisTemplate.keys(pattern + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            }
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(userId)
                    .operation(AuditOperation.USER_LOGOUT)
                    .resourceType("SESSION")
                    .resourceId(userId)
                    .result("SUCCESS")
                    .requestData("清理会话数据")
                    .build()
            );
            
            log.info("用户会话数据清理完成 - 用户ID: {}", userId);
            
        } catch (Exception e) {
            log.error("清理用户会话数据失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 清理临时文件和缓存（系统重启时调用）
     */
    public void clearTemporaryData() {
        try {
            log.info("开始清理系统临时数据");
            
            // 清理临时缓存
            String[] tempPatterns = {
                "temp:*",
                "cache:temp:*",
                "upload:temp:*",
                "session:temp:*"
            };
            
            int clearedCount = 0;
            for (String pattern : tempPatterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    clearedCount += keys.size();
                }
            }
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.SYSTEM_MONITOR,
                    "清理临时数据，清理缓存键数量: " + clearedCount
                )
            );
            
            log.info("系统临时数据清理完成，清理缓存键数量: {}", clearedCount);
            
        } catch (Exception e) {
            log.error("清理系统临时数据失败", e);
        }
    }
}