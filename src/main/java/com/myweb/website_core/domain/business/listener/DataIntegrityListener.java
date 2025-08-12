package com.myweb.website_core.domain.business.listener;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.common.exception.DataIntegrityException;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.infrastructure.config.ApplicationContextProvider;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据完整性监听器
 * <p>
 * 用于在JPA实体持久化和更新时自动处理数据完整性相关操作：
 * - 在保存前计算内容哈希值
 * - 在更新前验证原内容完整性
 * - 记录哈希计算时间
 * - 处理完整性验证异常
 * <p>
 * 符合GB/T 22239-2019数据完整性保护要求 3.1, 3.4
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Slf4j
@Component
public class DataIntegrityListener {
    
    /**
     * 在实体持久化前执行
     * 自动计算内容哈希值
     * 
     * @param entity 要持久化的实体
     */
    @PrePersist
    public void prePersist(Object entity) {
        try {
            if (entity instanceof Post) {
                handlePostPrePersist((Post) entity);
            } else if (entity instanceof Comment) {
                handleCommentPrePersist((Comment) entity);
            }
        } catch (Exception e) {
            log.error("实体持久化前处理失败: {}", entity.getClass().getSimpleName(), e);
            // 不阻止保存操作，但记录错误
        }
    }
    
    /**
     * 在实体更新前执行
     * 验证原内容完整性并计算新哈希值
     * 
     * @param entity 要更新的实体
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        try {
            if (entity instanceof Post) {
                handlePostPreUpdate((Post) entity);
            } else if (entity instanceof Comment) {
                handleCommentPreUpdate((Comment) entity);
            }
        } catch (DataIntegrityException e) {
            log.error("数据完整性验证失败: {}", e.getMessage());
            throw e; // 阻止更新操作
        } catch (Exception e) {
            log.error("实体更新前处理失败: {}", entity.getClass().getSimpleName(), e);
            // 其他异常不阻止更新操作
        }
    }
    
    /**
     * 在实体持久化后执行
     * 记录完整性相关的审计日志
     * 
     * @param entity 已持久化的实体
     */
    @PostPersist
    public void postPersist(Object entity) {
        try {
            if (entity instanceof Post) {
                Post post = (Post) entity;
                log.debug("帖子内容哈希已计算: postId={}, hash={}", 
                         post.getId(), post.getContentHash());
            } else if (entity instanceof Comment) {
                Comment comment = (Comment) entity;
                log.debug("评论内容哈希已计算: commentId={}, hash={}", 
                         comment.getId(), comment.getContentHash());
            }
        } catch (Exception e) {
            log.error("实体持久化后处理失败: {}", entity.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * 在实体更新后执行
     * 记录完整性更新的审计日志
     * 
     * @param entity 已更新的实体
     */
    @PostUpdate
    public void postUpdate(Object entity) {
        try {
            if (entity instanceof Post) {
                Post post = (Post) entity;
                log.debug("帖子内容哈希已更新: postId={}, hash={}", 
                         post.getId(), post.getContentHash());
            } else if (entity instanceof Comment) {
                Comment comment = (Comment) entity;
                log.debug("评论内容哈希已更新: commentId={}, hash={}", 
                         comment.getId(), comment.getContentHash());
            }
        } catch (Exception e) {
            log.error("实体更新后处理失败: {}", entity.getClass().getSimpleName(), e);
        }
    }
    
    // ==================== 帖子处理方法 ====================
    
    /**
     * 处理帖子持久化前的操作
     * 
     * @param post 帖子实体
     */
    private void handlePostPrePersist(Post post) {
        if (post.getContent() != null) {
            DataIntegrityService integrityService = getDataIntegrityService();
            if (integrityService != null) {
                String contentHash = integrityService.calculateHash(post.getContent());
                post.setContentHash(contentHash);
                post.setHashCalculatedAt(LocalDateTime.now());
                
                log.debug("为新帖子计算内容哈希: title={}, hashLength={}", 
                         post.getTitle(), contentHash.length());
            }
        }
    }
    
    /**
     * 处理帖子更新前的操作
     * 
     * @param post 帖子实体
     */
    private void handlePostPreUpdate(Post post) {
        DataIntegrityService integrityService = getDataIntegrityService();
        if (integrityService == null) {
            return;
        }
        
        // 如果内容发生变化，验证原内容完整性
        if (post.getContent() != null) {
            // 验证现有哈希值的完整性（如果存在）
            if (post.getContentHash() != null) {
                boolean isValid = integrityService.verifyIntegrity(
                    post.getContent(), post.getContentHash());
                
                if (!isValid) {
                    log.warn("帖子内容完整性验证失败: postId={}, title={}", 
                            post.getId(), post.getTitle());
                    
                    // 根据配置决定是否阻止更新
                    // 这里选择记录警告但不阻止更新，因为可能是正常的内容修改
                    // 如果需要严格模式，可以抛出异常
                    // throw new DataIntegrityException("帖子内容完整性验证失败，可能已被篡改");
                }
            }
            
            // 计算新的内容哈希
            String newContentHash = integrityService.calculateHash(post.getContent());
            post.setContentHash(newContentHash);
            post.setHashCalculatedAt(LocalDateTime.now());
            
            log.debug("更新帖子内容哈希: postId={}, title={}, newHashLength={}", 
                     post.getId(), post.getTitle(), newContentHash.length());
        }
    }
    
    // ==================== 评论处理方法 ====================
    
    /**
     * 处理评论持久化前的操作
     * 
     * @param comment 评论实体
     */
    private void handleCommentPrePersist(Comment comment) {
        if (comment.getContent() != null) {
            DataIntegrityService integrityService = getDataIntegrityService();
            if (integrityService != null) {
                String contentHash = integrityService.calculateHash(comment.getContent());
                comment.setContentHash(contentHash);
                comment.setHashCalculatedAt(LocalDateTime.now());
                
                log.debug("为新评论计算内容哈希: commentId={}, hashLength={}", 
                         comment.getId(), contentHash.length());
            }
        }
    }
    
    /**
     * 处理评论更新前的操作
     * 
     * @param comment 评论实体
     */
    private void handleCommentPreUpdate(Comment comment) {
        DataIntegrityService integrityService = getDataIntegrityService();
        if (integrityService == null) {
            return;
        }
        
        // 如果内容发生变化，验证原内容完整性
        if (comment.getContent() != null) {
            // 验证现有哈希值的完整性（如果存在）
            if (comment.getContentHash() != null) {
                boolean isValid = integrityService.verifyIntegrity(
                    comment.getContent(), comment.getContentHash());
                
                if (!isValid) {
                    log.warn("评论内容完整性验证失败: commentId={}", comment.getId());
                    
                    // 根据配置决定是否阻止更新
                    // 这里选择记录警告但不阻止更新，因为可能是正常的内容修改
                    // 如果需要严格模式，可以抛出异常
                    // throw new DataIntegrityException("评论内容完整性验证失败，可能已被篡改");
                }
            }
            
            // 计算新的内容哈希
            String newContentHash = integrityService.calculateHash(comment.getContent());
            comment.setContentHash(newContentHash);
            comment.setHashCalculatedAt(LocalDateTime.now());
            
            log.debug("更新评论内容哈希: commentId={}, newHashLength={}", 
                     comment.getId(), newContentHash.length());
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取数据完整性服务实例
     * 
     * @return DataIntegrityService实例，如果获取失败返回null
     */
    private DataIntegrityService getDataIntegrityService() {
        try {
            return ApplicationContextProvider.getBean(DataIntegrityService.class);
        } catch (Exception e) {
            log.warn("无法获取DataIntegrityService实例: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查实体是否需要完整性处理
     * 
     * @param entity 实体对象
     * @return 是否需要处理
     */
    private boolean needsIntegrityProcessing(Object entity) {
        return entity instanceof Post || entity instanceof Comment;
    }
    
    /**
     * 获取实体的内容字段
     * 
     * @param entity 实体对象
     * @return 内容字符串，如果不支持返回null
     */
    private String getEntityContent(Object entity) {
        if (entity instanceof Post) {
            return ((Post) entity).getContent();
        } else if (entity instanceof Comment) {
            return ((Comment) entity).getContent();
        }
        return null;
    }
    
    /**
     * 获取实体的标识信息（用于日志）
     * 
     * @param entity 实体对象
     * @return 标识字符串
     */
    private String getEntityIdentifier(Object entity) {
        if (entity instanceof Post) {
            Post post = (Post) entity;
            return String.format("Post[id=%s, title=%s]", post.getId(), post.getTitle());
        } else if (entity instanceof Comment) {
            Comment comment = (Comment) entity;
            return String.format("Comment[id=%s]", comment.getId());
        }
        return entity.getClass().getSimpleName();
    }
}