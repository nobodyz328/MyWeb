package com.myweb.website_core.application.service.security.authorization;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.util.SecurityEventUtils;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 访问控制服务
 * <p>
 * 实现基于角色的访问控制(RBAC)机制
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {
    
    private final MessageProducerService messageProducerService;
    
    /**
     * 检查用户是否可以查看帖子
     */
    public boolean canViewPost(User user, Post post) {
        try {
            // 公开帖子所有人都可以查看
            if (post == null) {
                return false;
            }

            // 记录访问控制检查
            recordAccessAllowed(user, "POST_GET", post.getId(), "VIEW");

            return true; // 默认所有帖子都可以查看
            
        } catch (Exception e) {
            log.error("检查帖子查看权限失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 检查用户是否可以编辑帖子
     */
    public boolean canEditPost(User user, Post post) {
        try {
            if (user == null || post == null) {
                return false;
            }
            
            // 帖子作者可以编辑
            if (post.getAuthor() != null && post.getAuthor().getId().equals(user.getId())) {
                recordAccessAllowed(user, "POST_PUT", post.getId(), "EDIT");
                return true;
            }
            
            // 管理员可以编辑任何帖子
            if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.MODERATOR)) {
                recordAccessAllowed(user, "POST_PUT", post.getId(), "EDIT");
                return true;
            }
            
            recordAccessDenied(user, "POST_PUT", post.getId(), "EDIT", "用户不是帖子作者且无管理权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查帖子编辑权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST_PUT", post.getId(), "EDIT", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以删除帖子
     */
    public boolean canDeletePost(User user, Post post) {
        try {
            if (user == null || post == null) {
                return false;
            }
            
            // 帖子作者可以删除
            if (post.getAuthor() != null && post.getAuthor().getId().equals(user.getId())) {
                recordAccessAllowed(user, "POST_DELETE", post.getId(), "DELETE");
                return true;
            }
            
            // 管理员可以删除任何帖子
            if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.MODERATOR)) {
                recordAccessAllowed(user, "POST_DELETE", post.getId(), "DELETE");
                return true;
            }
            
            recordAccessDenied(user, "POST_DELETE", post.getId(), "DELETE", "用户不是帖子作者且无管理权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查帖子删除权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST", post.getId(), "DELETE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以创建评论
     */
    public boolean canCreateComment(User user, Post post) {
        try {
            if (user == null) {
                return false;
            }
            
            if (post == null) {
                return false;
            }
            
            // 已登录用户都可以评论
            recordAccessAllowed(user, "COMMENT_POST", null, "CREATE");
            return true;
            
        } catch (Exception e) {
            log.error("检查评论创建权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "COMMENT_POST", null, "CREATE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    
    /**
     * 检查用户是否可以删除评论
     */
    public boolean canDeleteComment(User user, Comment comment) {
        try {
            if (user == null || comment == null) {
                return false;
            }
            
            // 评论作者可以删除
            if (comment.getAuthor() != null && comment.getAuthor().getId().equals(user.getId())) {
                recordAccessAllowed(user, "COMMENT", comment.getId(), "DELETE");
                return true;
            }
            
            // 帖子作者可以删除帖子下的评论
            if (comment.getPost() != null && comment.getPost().getAuthor() != null &&
                comment.getPost().getAuthor().getId().equals(user.getId())) {
                recordAccessAllowed(user, "COMMENT", comment.getId(), "DELETE");
                return true;
            }
            
            // 管理员可以删除任何评论
            if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.MODERATOR)) {
                recordAccessAllowed(user, "COMMENT", comment.getId(), "DELETE");
                return true;
            }
            
            recordAccessDenied(user, "COMMENT_DELETE", comment.getId(), "DELETE", "用户无删除权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查评论删除权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "COMMENT_DELETE", comment.getId(), "DELETE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以访问管理功能
     */
    public boolean canAccessAdmin(User user) {
        try {
            if (user == null) {
                return false;
            }
            
            if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.MODERATOR)) {
                recordAccessAllowed(user, "ADMIN", null, "ACCESS");
                return true;
            }
            
            recordAccessDenied(user, "ADMIN", null, "ACCESS", "用户无管理权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查管理功能访问权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "ADMIN", null, "ACCESS", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以上传文件
     */
    public boolean canUploadFile(User user) {
        try {
            if (user == null) {
                return false;
            }
            
            // 已登录用户都可以上传文件
            recordAccessAllowed(user, "FILE", null, "UPLOAD");
            return true;
            
        } catch (Exception e) {
            log.error("检查文件上传权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "FILE", null, "UPLOAD", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以执行搜索
     */
    public boolean canSearch(User user) {
        try {
            // 搜索功能对所有人开放，包括未登录用户
            recordAccessAllowed(user, "SEARCH", null, "EXECUTE");
            return true;
            
        } catch (Exception e) {
            log.error("检查搜索权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "SEARCH", null, "EXECUTE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以点赞
     */
    public boolean canLikePost(User user, Post post) {
        try {
            if (user == null) {
                return false;
            }
            
            if (post == null) {
                return false;
            }
            
            recordAccessAllowed(user, "POST_LIKE", post.getId(), "LIKE");
            return true;
            
        } catch (Exception e) {
            log.error("检查点赞权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST_LIKE", post.getId(), "LIKE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以收藏
     */
    public boolean canCollectPost(User user, Post post) {
        try {
            if (user == null) {
                return false;
            }
            
            if (post == null) {
                return false;
            }
            
            recordAccessAllowed(user, "POST_COLLECT", post.getId(), "COLLECT");
            return true;
            
        } catch (Exception e) {
            log.error("检查收藏权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST_COLLECT", post.getId(), "COLLECT", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以绑定邮箱
     */
    public boolean canBindEmail(User user, Long targetUserId) {
        try {
            if (user == null || targetUserId == null) {
                return false;
            }
            
            // 用户只能绑定自己的邮箱
            if (user.getId().equals(targetUserId)) {
                recordAccessAllowed(user, "USER_EMAIL", targetUserId, "BIND");
                return true;
            }
            
            // 管理员可以为任何用户绑定邮箱
            if (user.hasRole(UserRole.ADMIN)) {
                recordAccessAllowed(user, "USER_EMAIL", targetUserId, "BIND");
                return true;
            }
            
            recordAccessDenied(user, "USER_EMAIL", targetUserId, "BIND", "用户无权限绑定此邮箱");
            return false;
            
        } catch (Exception e) {
            log.error("检查邮箱绑定权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "USER_EMAIL", targetUserId, "BIND", "权限检查异常: " + e.getMessage());
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 记录访问允许的审计日志
     */
    private void recordAccessAllowed( User user, String resourceType, Long resourceId, String action) {
        String ipAddress = getClientIpAddress();
        messageProducerService.sendAccessControlAuditMessage(
                AuditLogRequest.builder()
                        .operation(AuditOperation.ACCESS_ALLOWED)
                        .username(user.getUsername())
                        .userId(user.getId())
                        .ipAddress(ipAddress)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .tags("ACTION:" + action)
                        .result("SUCCESS")
                        .build()
        );
    }
    
    /**
     * 记录访问拒绝的审计日志
     */
    private void recordAccessDenied(User user, String resourceType, Long resourceId, String action, String reason) {
        String ipAddress = getClientIpAddress();
        messageProducerService.sendAccessControlAuditMessage(
                AuditLogRequest.builder()
                        .operation(AuditOperation.ACCESS_DENIED)
                        .username( user.getUsername())
                        .userId(user.getId())
                        .ipAddress(ipAddress)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .description(reason)
                        .tags("ACTION:" + action)
                        .result("FAILED")
                        .build()
        );
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
        return SecurityEventUtils.getClientIpAddress();
    }
}