package com.myweb.website_core.common.util;

import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.security.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限检查工具类
 * <p>
 * 提供统一的权限检查方法，包括：
 * - 用户角色检查
 * - 权限验证
 * - 资源访问权限检查
 * - 当前用户信息获取
 * - 管理员权限检查
 * <p>
 * 符合需求：10.4 - 提供权限检查方法
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
public class PermissionUtils {
    
    // ==================== 角色权限映射 ====================
    
    /**
     * 管理员角色集合
     */
    private static final Set<String> ADMIN_ROLES = Set.of(
        "ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ADMIN", "SUPER_ADMIN"
    );
    
    /**
     * 版主角色集合
     */
    private static final Set<String> MODERATOR_ROLES = Set.of(
        "ROLE_MODERATOR", "MODERATOR"
    );
    
    /**
     * 普通用户角色集合
     */
    private static final Set<String> USER_ROLES = Set.of(
        "ROLE_USER", "USER"
    );
    
    /**
     * 系统权限集合
     */
    private static final Set<String> SYSTEM_PERMISSIONS = Set.of(
        "SYSTEM_MANAGE", "USER_MANAGE", "ROLE_MANAGE", "PERMISSION_MANAGE",
        "AUDIT_VIEW", "SECURITY_MANAGE", "CONFIG_MANAGE", "BACKUP_MANAGE"
    );
    
    /**
     * 内容管理权限集合
     */
    private static final Set<String> CONTENT_PERMISSIONS = Set.of(
        "POST_MANAGE", "COMMENT_MANAGE", "CATEGORY_MANAGE", "TAG_MANAGE",
        "ANNOUNCEMENT_MANAGE", "CONTENT_REVIEW", "CONTENT_DELETE"
    );
    
    /**
     * 用户管理权限集合
     */
    private static final Set<String> USER_MANAGEMENT_PERMISSIONS = Set.of(
        "USER_CREATE", "USER_UPDATE", "USER_DELETE", "USER_VIEW",
        "USER_LOCK", "USER_UNLOCK", "USER_ROLE_ASSIGN"
    );
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private PermissionUtils() {
        // 工具类不允许实例化
    }
    
    // ==================== 当前用户信息获取 ====================
    
    /**
     * 获取当前认证用户
     * 
     * @return 当前用户，如果未认证则返回null
     */
    public static User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
                
                if (authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal principal) {
                    return principal.getUser();
                }
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 当前用户ID，如果未认证则返回null
     */
    public static Long getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 当前用户名，如果未认证则返回null
     */
    public static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取当前用户名失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取当前用户角色
     * 
     * @return 当前用户角色，如果未认证则返回null
     */
    public static UserRole getCurrentUserRole() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getRole() : null;
    }
    
    /**
     * 获取当前用户的所有权限
     * 
     * @return 权限集合
     */
    public static Set<String> getCurrentUserAuthorities() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.debug("获取当前用户权限失败: {}", e.getMessage());
        }
        return Set.of();
    }
    
    // ==================== 认证状态检查 ====================
    
    /**
     * 检查用户是否已认证
     * 
     * @return 如果已认证返回true
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && 
                   authentication.isAuthenticated() && 
                   !"anonymousUser".equals(authentication.getPrincipal());
        } catch (Exception e) {
            log.debug("检查认证状态失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否为匿名用户
     * 
     * @return 如果是匿名用户返回true
     */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }
    
    // ==================== 角色检查 ====================
    
    /**
     * 检查当前用户是否具有指定角色
     * 
     * @param role 角色名称
     * @return 如果具有指定角色返回true
     */
    public static boolean hasRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        
        Set<String> authorities = getCurrentUserAuthorities();
        
        // 检查完整角色名（带ROLE_前缀）
        if (authorities.contains(role)) {
            return true;
        }
        
        // 检查不带前缀的角色名
        if (!role.startsWith("ROLE_")) {
            return authorities.contains("ROLE_" + role);
        }
        
        // 检查去掉前缀的角色名
        if (role.startsWith("ROLE_")) {
            return authorities.contains(role.substring(5));
        }
        
        return false;
    }
    
    /**
     * 检查当前用户是否具有任意一个指定角色
     * 
     * @param roles 角色名称数组
     * @return 如果具有任意一个指定角色返回true
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查当前用户是否具有所有指定角色
     * 
     * @param roles 角色名称数组
     * @return 如果具有所有指定角色返回true
     */
    public static boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        
        for (String role : roles) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }
    
    // ==================== 权限检查 ====================
    
    /**
     * 检查当前用户是否具有指定权限
     * 
     * @param permission 权限名称
     * @return 如果具有指定权限返回true
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return false;
        }
        
        Set<String> authorities = getCurrentUserAuthorities();
        return authorities.contains(permission);
    }
    
    /**
     * 检查当前用户是否具有任意一个指定权限
     * 
     * @param permissions 权限名称数组
     * @return 如果具有任意一个指定权限返回true
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查当前用户是否具有所有指定权限
     * 
     * @param permissions 权限名称数组
     * @return 如果具有所有指定权限返回true
     */
    public static boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }
    
    // ==================== 特定角色检查 ====================
    
    /**
     * 检查当前用户是否为管理员
     * 
     * @return 如果是管理员返回true
     */
    public static boolean isAdmin() {
        Set<String> authorities = getCurrentUserAuthorities();
        return authorities.stream().anyMatch(ADMIN_ROLES::contains);
    }
    
    /**
     * 检查当前用户是否为超级管理员
     * 
     * @return 如果是超级管理员返回true
     */
    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN") || hasRole("ROLE_SUPER_ADMIN");
    }
    
    /**
     * 检查当前用户是否为版主
     * 
     * @return 如果是版主返回true
     */
    public static boolean isModerator() {
        Set<String> authorities = getCurrentUserAuthorities();
        return authorities.stream().anyMatch(MODERATOR_ROLES::contains);
    }
    
    /**
     * 检查当前用户是否为普通用户
     * 
     * @return 如果是普通用户返回true
     */
    public static boolean isUser() {
        Set<String> authorities = getCurrentUserAuthorities();
        return authorities.stream().anyMatch(USER_ROLES::contains);
    }
    
    /**
     * 检查当前用户是否为管理员或版主
     * 
     * @return 如果是管理员或版主返回true
     */
    public static boolean isAdminOrModerator() {
        return isAdmin() || isModerator();
    }
    
    // ==================== 资源访问权限检查 ====================
    
    /**
     * 检查当前用户是否可以访问指定用户的资源
     * 
     * @param targetUserId 目标用户ID
     * @return 如果可以访问返回true
     */
    public static boolean canAccessUserResource(Long targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        
        // 管理员可以访问所有用户资源
        if (isAdmin()) {
            return true;
        }
        
        // 用户可以访问自己的资源
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }
    
    /**
     * 检查当前用户是否可以修改指定用户的资源
     * 
     * @param targetUserId 目标用户ID
     * @return 如果可以修改返回true
     */
    public static boolean canModifyUserResource(Long targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        
        // 管理员可以修改所有用户资源
        if (isAdmin()) {
            return true;
        }
        
        // 用户只能修改自己的资源
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }
    
    /**
     * 检查当前用户是否可以删除指定用户的资源
     * 
     * @param targetUserId 目标用户ID
     * @return 如果可以删除返回true
     */
    public static boolean canDeleteUserResource(Long targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        
        // 管理员可以删除所有用户资源
        if (isAdmin()) {
            return true;
        }
        
        // 版主可以删除普通用户资源（需要额外的业务逻辑判断）
        if (isModerator()) {
            // 这里可以添加更复杂的业务逻辑
            return true;
        }
        
        // 用户只能删除自己的资源
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }
    
    // ==================== 系统权限检查 ====================
    
    /**
     * 检查当前用户是否具有系统管理权限
     * 
     * @return 如果具有系统管理权限返回true
     */
    public static boolean hasSystemManagePermission() {
        return isAdmin() || hasAnyPermission(SYSTEM_PERMISSIONS.toArray(new String[0]));
    }
    
    /**
     * 检查当前用户是否具有用户管理权限
     * 
     * @return 如果具有用户管理权限返回true
     */
    public static boolean hasUserManagePermission() {
        return isAdmin() || hasAnyPermission(USER_MANAGEMENT_PERMISSIONS.toArray(new String[0]));
    }
    
    /**
     * 检查当前用户是否具有内容管理权限
     * 
     * @return 如果具有内容管理权限返回true
     */
    public static boolean hasContentManagePermission() {
        return isAdminOrModerator() || hasAnyPermission(CONTENT_PERMISSIONS.toArray(new String[0]));
    }
    
    /**
     * 检查当前用户是否具有审计日志查看权限
     * 
     * @return 如果具有审计日志查看权限返回true
     */
    public static boolean hasAuditViewPermission() {
        return isAdmin() || hasPermission("AUDIT_VIEW");
    }
    
    /**
     * 检查当前用户是否具有安全管理权限
     * 
     * @return 如果具有安全管理权限返回true
     */
    public static boolean hasSecurityManagePermission() {
        return isAdmin() || hasPermission("SECURITY_MANAGE");
    }
    
    // ==================== 业务权限检查 ====================
    
    /**
     * 检查当前用户是否可以创建帖子
     * 
     * @return 如果可以创建帖子返回true
     */
    public static boolean canCreatePost() {
        return isAuthenticated(); // 所有认证用户都可以创建帖子
    }
    
    /**
     * 检查当前用户是否可以编辑指定帖子
     * 
     * @param postAuthorId 帖子作者ID
     * @return 如果可以编辑返回true
     */
    public static boolean canEditPost(Long postAuthorId) {
        if (postAuthorId == null) {
            return false;
        }
        
        // 管理员和版主可以编辑所有帖子
        if (isAdminOrModerator()) {
            return true;
        }
        
        // 作者可以编辑自己的帖子
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(postAuthorId);
    }
    
    /**
     * 检查当前用户是否可以删除指定帖子
     * 
     * @param postAuthorId 帖子作者ID
     * @return 如果可以删除返回true
     */
    public static boolean canDeletePost(Long postAuthorId) {
        if (postAuthorId == null) {
            return false;
        }
        
        // 管理员和版主可以删除所有帖子
        if (isAdminOrModerator()) {
            return true;
        }
        
        // 作者可以删除自己的帖子
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(postAuthorId);
    }
    
    /**
     * 检查当前用户是否可以评论
     * 
     * @return 如果可以评论返回true
     */
    public static boolean canComment() {
        return isAuthenticated(); // 所有认证用户都可以评论
    }
    
    /**
     * 检查当前用户是否可以删除指定评论
     * 
     * @param commentAuthorId 评论作者ID
     * @return 如果可以删除返回true
     */
    public static boolean canDeleteComment(Long commentAuthorId) {
        if (commentAuthorId == null) {
            return false;
        }
        
        // 管理员和版主可以删除所有评论
        if (isAdminOrModerator()) {
            return true;
        }
        
        // 作者可以删除自己的评论
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(commentAuthorId);
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 要求用户已认证，否则抛出异常
     * 
     * @throws SecurityException 如果用户未认证
     */
    public static void requireAuthenticated() {
        if (!isAuthenticated()) {
            throw new SecurityException("用户未认证");
        }
    }
    
    /**
     * 要求用户具有指定角色，否则抛出异常
     * 
     * @param role 角色名称
     * @throws SecurityException 如果用户没有指定角色
     */
    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new SecurityException("用户没有所需角色: " + role);
        }
    }
    
    /**
     * 要求用户具有指定权限，否则抛出异常
     * 
     * @param permission 权限名称
     * @throws SecurityException 如果用户没有指定权限
     */
    public static void requirePermission(String permission) {
        if (!hasPermission(permission)) {
            throw new SecurityException("用户没有所需权限: " + permission);
        }
    }
    
    /**
     * 要求用户为管理员，否则抛出异常
     * 
     * @throws SecurityException 如果用户不是管理员
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new SecurityException("需要管理员权限");
        }
    }
    
    /**
     * 获取权限检查结果的详细信息
     * 
     * @return 权限信息字符串
     */
    public static String getPermissionInfo() {
        if (!isAuthenticated()) {
            return "用户未认证";
        }
        
        User currentUser = getCurrentUser();
        Set<String> authorities = getCurrentUserAuthorities();
        
        return String.format(
            "用户信息: ID=%s, 用户名=%s, 角色=%s, 权限=%s, 是否管理员=%s, 是否版主=%s",
            currentUser != null ? currentUser.getId() : "未知",
            getCurrentUsername(),
            getCurrentUserRole(),
            authorities,
            isAdmin(),
            isModerator()
        );
    }
}