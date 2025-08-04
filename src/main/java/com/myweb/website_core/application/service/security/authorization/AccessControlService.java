package com.myweb.website_core.application.service.security.authorization;

import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 访问控制服务
 * 
 * 实现基于角色的访问控制(RBAC)机制
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-08-01
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
            String ipAddress = getClientIpAddress();
            messageProducerService.sendAccessControlAuditMessage(
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : "anonymous",
                "POST",
                post.getId(),
                "VIEW",
                "ALLOWED",
                ipAddress,
                null
            );
            
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
                recordAccessDenied(user, "POST", post != null ? post.getId() : null, "EDIT", "用户未登录或帖子不存在");
                return false;
            }
            
            // 帖子作者可以编辑
            if (post.getAuthor() != null && post.getAuthor().getId().equals(user.getId())) {
                recordAccessAllowed(user, "POST", post.getId(), "EDIT");
                return true;
            }
            
            // 管理员可以编辑任何帖子
            if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.MODERATOR)) {
                recordAccessAllowed(user, "POST", post.getId(), "EDIT");
                return true;
            }
            
            recordAccessDenied(user, "POST", post.getId(), "EDIT", "用户不是帖子作者且无管理权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查帖子编辑权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST", post != null ? post.getId() : null, "EDIT", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以删除帖子
     */
    public boolean canDeletePost(User user, Post post) {
        try {
            if (user == null || post == null) {
                recordAccessDenied(user, "POST", post != null ? post.getId() : null, "DELETE", "用户未登录或帖子不存在");
                return false;
            }
            
            // 帖子作者可以删除
            if (post.getAuthor() != null && post.getAuthor().getId().equals(user.getId())) {
                recordAccessAllowed(user, "POST", post.getId(), "DELETE");
                return true;
            }
            
            // 管理员可以删除任何帖子
            if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.MODERATOR)) {
                recordAccessAllowed(user, "POST", post.getId(), "DELETE");
                return true;
            }
            
            recordAccessDenied(user, "POST", post.getId(), "DELETE", "用户不是帖子作者且无管理权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查帖子删除权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST", post != null ? post.getId() : null, "DELETE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以创建评论
     */
    public boolean canCreateComment(User user, Post post) {
        try {
            if (user == null) {
                recordAccessDenied(user, "COMMENT", null, "CREATE", "用户未登录");
                return false;
            }
            
            if (post == null) {
                recordAccessDenied(user, "COMMENT", null, "CREATE", "帖子不存在");
                return false;
            }
            
            // 已登录用户都可以评论
            recordAccessAllowed(user, "COMMENT", null, "CREATE");
            return true;
            
        } catch (Exception e) {
            log.error("检查评论创建权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "COMMENT", null, "CREATE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以编辑评论
     */
    public boolean canEditComment(User user, Comment comment) {
        try {
            if (user == null || comment == null) {
                recordAccessDenied(user, "COMMENT", comment != null ? comment.getId() : null, "EDIT", "用户未登录或评论不存在");
                return false;
            }
            
            // 评论作者可以编辑
            if (comment.getAuthor() != null && comment.getAuthor().getId().equals(user.getId())) {
                recordAccessAllowed(user, "COMMENT", comment.getId(), "EDIT");
                return true;
            }
            
            // 管理员可以编辑任何评论
            if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.MODERATOR)) {
                recordAccessAllowed(user, "COMMENT", comment.getId(), "EDIT");
                return true;
            }
            
            recordAccessDenied(user, "COMMENT", comment.getId(), "EDIT", "用户不是评论作者且无管理权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查评论编辑权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "COMMENT", comment != null ? comment.getId() : null, "EDIT", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以删除评论
     */
    public boolean canDeleteComment(User user, Comment comment) {
        try {
            if (user == null || comment == null) {
                recordAccessDenied(user, "COMMENT", comment != null ? comment.getId() : null, "DELETE", "用户未登录或评论不存在");
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
            
            recordAccessDenied(user, "COMMENT", comment.getId(), "DELETE", "用户无删除权限");
            return false;
            
        } catch (Exception e) {
            log.error("检查评论删除权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "COMMENT", comment != null ? comment.getId() : null, "DELETE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以访问管理功能
     */
    public boolean canAccessAdminFeatures(User user) {
        try {
            if (user == null) {
                recordAccessDenied(user, "ADMIN", null, "ACCESS", "用户未登录");
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
                recordAccessDenied(user, "FILE", null, "UPLOAD", "用户未登录");
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
                recordAccessDenied(user, "POST", post != null ? post.getId() : null, "LIKE", "用户未登录");
                return false;
            }
            
            if (post == null) {
                recordAccessDenied(user, "POST", null, "LIKE", "帖子不存在");
                return false;
            }
            
            recordAccessAllowed(user, "POST", post.getId(), "LIKE");
            return true;
            
        } catch (Exception e) {
            log.error("检查点赞权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST", post != null ? post.getId() : null, "LIKE", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否可以收藏
     */
    public boolean canCollectPost(User user, Post post) {
        try {
            if (user == null) {
                recordAccessDenied(user, "POST", post != null ? post.getId() : null, "COLLECT", "用户未登录");
                return false;
            }
            
            if (post == null) {
                recordAccessDenied(user, "POST", null, "COLLECT", "帖子不存在");
                return false;
            }
            
            recordAccessAllowed(user, "POST", post.getId(), "COLLECT");
            return true;
            
        } catch (Exception e) {
            log.error("检查收藏权限失败: {}", e.getMessage(), e);
            recordAccessDenied(user, "POST", post != null ? post.getId() : null, "COLLECT", "权限检查异常: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 记录访问允许的审计日志
     */
    private void recordAccessAllowed(User user, String resourceType, Long resourceId, String action) {
        String ipAddress = getClientIpAddress();
        messageProducerService.sendAccessControlAuditMessage(
            user != null ? user.getId() : null,
            user != null ? user.getUsername() : "anonymous",
            resourceType,
            resourceId,
            action,
            "ALLOWED",
            ipAddress,
            null
        );
    }
    
    /**
     * 记录访问拒绝的审计日志
     */
    private void recordAccessDenied(User user, String resourceType, Long resourceId, String action, String reason) {
        String ipAddress = getClientIpAddress();
        messageProducerService.sendAccessControlAuditMessage(
            user != null ? user.getId() : null,
            user != null ? user.getUsername() : "anonymous",
            resourceType,
            resourceId,
            action,
            "DENIED",
            ipAddress,
            reason
        );
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("获取客户端IP地址失败: {}", e.getMessage());
        }
        return "unknown";
    }
}