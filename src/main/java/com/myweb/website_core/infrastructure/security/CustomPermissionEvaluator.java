package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.domain.business.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 自定义权限评估器
 * 
 * 实现Spring Security的PermissionEvaluator接口，
 * 支持在@PreAuthorize注解中使用hasPermission()表达式
 * 
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    @Autowired
    private AuthorizationService authorizationService;
    
    /**
     * 评估用户对指定对象的权限
     * 
     * @param authentication 认证信息
     * @param targetDomainObject 目标对象
     * @param permission 权限标识
     * @return 是否有权限
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return false;
        }
        
        String permissionStr = permission.toString();
        
        // 如果目标对象为空，则进行一般权限检查
        if (targetDomainObject == null) {
            return checkGeneralPermission(user, permissionStr);
        }
        
        // 根据目标对象类型进行特定的权限检查
        String targetType = targetDomainObject.getClass().getSimpleName();
        return checkObjectPermission(user, targetDomainObject, targetType, permissionStr);
    }
    
    /**
     * 评估用户对指定ID和类型对象的权限
     * 
     * @param authentication 认证信息
     * @param targetId 目标对象ID
     * @param targetType 目标对象类型
     * @param permission 权限标识
     * @return 是否有权限
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, 
                               String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        User user = getCurrentUser(authentication);
        if (user == null) {
            return false;
        }
        
        String permissionStr = permission.toString();
        Long objectId = null;
        
        if (targetId instanceof Number) {
            objectId = ((Number) targetId).longValue();
        } else if (targetId instanceof String) {
            try {
                objectId = Long.parseLong((String) targetId);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return checkObjectPermissionById(user, objectId, targetType, permissionStr);
    }
    
    /**
     * 获取当前用户
     * 
     * @param authentication 认证信息
     * @return 用户对象
     */
    private User getCurrentUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetailsService.CustomUserPrincipal) {
            return ((CustomUserDetailsService.CustomUserPrincipal) principal).getUser();
        }
        return null;
    }
    
    /**
     * 检查一般权限（不涉及特定对象）
     * 
     * @param user 用户对象
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkGeneralPermission(User user, String permission) {
        // 解析权限格式：RESOURCE_ACTION 或 RESOURCE:ACTION
        String[] parts = parsePermission(permission);
        if (parts.length == 2) {
            return authorizationService.hasPermission(user.getId(), parts[0], parts[1]);
        }
        
        // 直接权限名称检查
        return user.hasPermission(permission);
    }
    
    /**
     * 检查对象权限
     * 
     * @param user 用户对象
     * @param targetObject 目标对象
     * @param targetType 目标类型
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkObjectPermission(User user, Object targetObject, String targetType, String permission) {
        switch (targetType.toLowerCase()) {
            case "post":
                return checkPostPermission(user, targetObject, permission);
            case "comment":
                return checkCommentPermission(user, targetObject, permission);
            case "user":
                return checkUserPermission(user, targetObject, permission);
            default:
                return checkGeneralPermission(user, permission);
        }
    }
    
    /**
     * 根据ID检查对象权限
     * 
     * @param user 用户对象
     * @param objectId 对象ID
     * @param targetType 目标类型
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkObjectPermissionById(User user, Long objectId, String targetType, String permission) {
        if (objectId == null) {
            return false;
        }
        
        switch (targetType.toLowerCase()) {
            case "post":
                return checkPostPermissionById(user, objectId, permission);
            case "comment":
                return checkCommentPermissionById(user, objectId, permission);
            case "user":
                return checkUserPermissionById(user, objectId, permission);
            default:
                return checkGeneralPermission(user, permission);
        }
    }
    
    /**
     * 检查帖子权限
     * 
     * @param user 用户对象
     * @param postObject 帖子对象
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkPostPermission(User user, Object postObject, String permission) {
        // 这里可以根据具体的Post对象进行权限检查
        // 由于没有直接的Post对象引用，使用反射或转换
        try {
            Long postId = (Long) postObject.getClass().getMethod("getId").invoke(postObject);
            return checkPostPermissionById(user, postId, permission);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 根据ID检查帖子权限
     * 
     * @param user 用户对象
     * @param postId 帖子ID
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkPostPermissionById(User user, Long postId, String permission) {
        switch (permission.toLowerCase()) {
            case "read":
            case "view":
                return true; // 所有用户都可以查看帖子
            case "edit":
            case "update":
                return authorizationService.canEditPost(user.getId(), postId);
            case "delete":
                return authorizationService.canDeletePost(user.getId(), postId);
            case "manage":
                return authorizationService.hasPermission(user.getId(), "POST", "MANAGE");
            default:
                return false;
        }
    }
    
    /**
     * 检查评论权限
     * 
     * @param user 用户对象
     * @param commentObject 评论对象
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkCommentPermission(User user, Object commentObject, String permission) {
        try {
            Long commentId = (Long) commentObject.getClass().getMethod("getId").invoke(commentObject);
            return checkCommentPermissionById(user, commentId, permission);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 根据ID检查评论权限
     * 
     * @param user 用户对象
     * @param commentId 评论ID
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkCommentPermissionById(User user, Long commentId, String permission) {
        switch (permission.toLowerCase()) {
            case "read":
            case "view":
                return true; // 所有用户都可以查看评论
            case "edit":
            case "update":
                return authorizationService.canEditComment(user.getId(), commentId);
            case "delete":
                return authorizationService.canDeleteComment(user.getId(), commentId);
            case "manage":
                return authorizationService.hasPermission(user.getId(), "COMMENT", "MANAGE");
            default:
                return false;
        }
    }
    
    /**
     * 检查用户权限
     * 
     * @param user 当前用户对象
     * @param userObject 目标用户对象
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkUserPermission(User user, Object userObject, String permission) {
        try {
            Long targetUserId = (Long) userObject.getClass().getMethod("getId").invoke(userObject);
            return checkUserPermissionById(user, targetUserId, permission);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 根据ID检查用户权限
     * 
     * @param user 当前用户对象
     * @param targetUserId 目标用户ID
     * @param permission 权限标识
     * @return 是否有权限
     */
    private boolean checkUserPermissionById(User user, Long targetUserId, String permission) {
        switch (permission.toLowerCase()) {
            case "view":
            case "read":
                return true; // 所有用户都可以查看其他用户的基本信息
            case "edit":
            case "update":
                // 只能编辑自己的信息，或者有用户管理权限
                return user.getId().equals(targetUserId) || 
                       authorizationService.hasPermission(user.getId(), "USER", "MANAGE");
            case "delete", "manage":
                // 只有管理员可以删除用户
                return authorizationService.hasPermission(user.getId(), "USER", "MANAGE");
            default:
                return false;
        }
    }
    
    /**
     * 解析权限字符串
     * 
     * @param permission 权限字符串
     * @return 解析后的资源类型和操作类型数组
     */
    private String[] parsePermission(String permission) {
        if (permission.contains("_")) {
            return permission.split("_", 2);
        } else if (permission.contains(":")) {
            return permission.split(":", 2);
        } else {
            return new String[]{permission};
        }
    }
}