package com.myweb.website_core.application.service.security.authorization;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.security.AuthorizationException;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 资源访问控制服务
 * 
 * 实现基于角色的访问控制(RBAC)和资源所有权验证
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * 主要功能：
 * - 用户权限验证
 * - 资源所有权检查
 * - 基于角色的接口访问控制
 * - 权限缓存机制
 * - 权限变更时的缓存刷新
 * 
 * 需求: 2.3, 2.4, 2.5, 2.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {
    
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditLogService auditLogService;
    
    // 缓存键前缀
    private static final String USER_PERMISSIONS_CACHE_KEY = "user:permissions:";
    private static final String USER_ROLES_CACHE_KEY = "user:roles:";
    private static final String RESOURCE_OWNERSHIP_CACHE_KEY = "resource:ownership:";
    
    // 缓存过期时间（30分钟）
    private static final Duration CACHE_EXPIRE_TIME = Duration.ofMinutes(30);
    
    // ==================== 权限验证核心方法 ====================
    
    /**
     * 检查用户是否具有指定权限
     * 
     * @param userId 用户名
     * @param permission 权限名称
     * @return 是否具有权限
     */
    @Cacheable(value = "userPermissions", key = "#userId + ':' + #permission")
    public boolean hasPermission(Long userId, String permission) {
        log.debug("检查用户权限: userId={}, permission={}", userId, permission);
        
        if (userId == null || permission == null) {
            return false;
        }
        
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        
        // 检查用户是否具有指定权限
        boolean hasPermission = user.get().hasPermission(permission);
        
        log.debug("权限检查结果: userId={}, permission={}, hasPermission={}",
                 userId, permission, hasPermission);
        
        return hasPermission;
    }
    
    /**
     * 检查用户是否具有对指定资源执行指定操作的权限
     * 
     * @param id 用户名
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否具有权限
     */
    @Cacheable(value = "userResourcePermissions", key = "#id + ':' + #resourceType + ':' + #actionType")
    public boolean hasPermission(Long id, String resourceType, String actionType) {
        log.debug("检查用户资源权限: id={}, resourceType={}, actionType={}",
                 id, resourceType, actionType);
        
        if (id == null || resourceType == null || actionType == null) {
            return false;
        }
        
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            log.warn("用户不存在: id={}", id);
            return false;
        }
        
        // 检查用户是否具有对指定资源执行指定操作的权限
        boolean hasPermission = user.get().hasPermission(resourceType, actionType);
        
        log.debug("资源权限检查结果: id={}, resourceType={}, actionType={}, hasPermission={}",
                 id, resourceType, actionType, hasPermission);
        
        return hasPermission;
    }
    
    /**
     * 检查用户是否可以访问指定资源
     * 综合考虑基本权限和资源所有权
     * 
     * @param userId 用户名
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param actionType 操作类型
     * @return 是否可以访问
     */
    public boolean canAccessResource(Long userId, String resourceType, Long resourceId, String actionType) {
        log.debug("检查资源访问权限: userId={}, resourceType={}, resourceId={}, actionType={}",
                 userId, resourceType, resourceId, actionType);
        
        if (userId == null || resourceType == null || resourceId == null || actionType == null) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        User user = userOpt.get();
        
        // 1. 检查基本权限
        String permission = resourceType + "_" + actionType;
        if (!hasPermission(userId, permission)) {
            log.debug("用户缺少基本权限: userId={}, permission={}", userId, permission);
            return false;
        }
        
        // 2. 检查资源所有权（针对特定操作）
        if (requiresOwnershipCheck(resourceType, actionType)) {
            boolean isOwner = isResourceOwner(userId, resourceType, resourceId);
            boolean hasManagePermission = hasPermission(userId, resourceType + "_MANAGE");
            
            if (!isOwner && !hasManagePermission) {
                log.debug("用户既不是资源所有者也没有管理权限: userId={}, resourceType={}, resourceId={}",
                         userId, resourceType, resourceId);
                return false;
            }
        }
        
        log.debug("资源访问权限检查通过: userId={}, resourceType={}, resourceId={}, actionType={}",
                 userId, resourceType, resourceId, actionType);
        
        return true;
    }
    
    // ==================== 资源所有权检查 ====================
    
    /**
     * 检查用户是否为指定资源的所有者
     * 
     * @param userId 用户名
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 是否为资源所有者
     */
    @Cacheable(value = "resourceOwnership", key = "#userId + ':' + #resourceType + ':' + #resourceId")
    public boolean isResourceOwner(Long userId, String resourceType, Long resourceId) {
        log.debug("检查资源所有权: userId={}, resourceType={}, resourceId={}",
                 userId, resourceType, resourceId);
        
        if (userId == null || resourceType == null || resourceId == null) {
            return false;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        User user = userOpt.get();
        boolean isOwner = false;
        
        switch (resourceType.toUpperCase()) {
            case "POST":
                isOwner = isPostOwner(user.getId(), resourceId);
                break;
            case "COMMENT":
                isOwner = isCommentOwner(user.getId(), resourceId);
                break;
            case "USER":
                isOwner = user.getId().equals(resourceId);
                break;
            default:
                log.warn("不支持的资源类型: resourceType={}", resourceType);
                return false;
        }
        
        log.debug("资源所有权检查结果: userId={}, resourceType={}, resourceId={}, isOwner={}",
                 userId, resourceType, resourceId, isOwner);
        
        return isOwner;
    }
    
    /**
     * 检查用户是否为帖子所有者
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 是否为帖子所有者
     */
    private boolean isPostOwner(Long userId, Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("帖子不存在: postId={}", postId);
            return false;
        }
        
        Post post = postOpt.get();
        return post.getAuthor() != null && userId.equals(post.getAuthor().getId());
    }
    
    /**
     * 检查用户是否为评论所有者
     * 
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 是否为评论所有者
     */
    private boolean isCommentOwner(Long userId, Long commentId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            log.warn("评论不存在: commentId={}", commentId);
            return false;
        }
        
        Comment comment = commentOpt.get();
        return comment.getAuthor() != null && userId.equals(comment.getAuthor().getId());
    }
    
    /**
     * 检查用户是否可以编辑帖子
     * 帖子可以被帖子作者或管理员编辑
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 是否可以编辑
     */
    public boolean canEditPost(Long userId, Long postId) {
        log.debug("检查帖子编辑权限: userId={}, postId={}", userId, postId);
        
        if (userId == null || postId == null) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        User user = userOpt.get();
        
        // 1. 检查是否为帖子作者
        if (isPostOwner(user.getId(), postId)) {
            return true;
        }
        
        // 2. 检查是否有管理权限
        if (hasPermission(userId, "POST_MANAGE")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查用户是否可以删除帖子
     * 帖子可以被帖子作者或管理员删除
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 是否可以删除
     */
    public boolean canDeletePost(Long userId, Long postId) {
        log.debug("检查帖子删除权限: userId={}, postId={}", userId, postId);
        
        if (userId == null || postId == null) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        User user = userOpt.get();
        
        // 1. 检查是否为帖子作者
        if (isPostOwner(user.getId(), postId)) {
            return true;
        }
        
        // 2. 检查是否有管理权限
        if (hasPermission(userId, "POST_MANAGE")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查用户是否可以删除评论
     * 评论可以被评论作者、帖子作者或管理员删除
     * 
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 是否可以删除
     */
    public boolean canDeleteComment(Long userId, Long commentId) {
        log.debug("检查评论删除权限: userId={}, commentId={}", userId, commentId);
        
        if (userId == null || commentId == null) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        User user = userOpt.get();
        
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            log.warn("评论不存在: commentId={}", commentId);
            return false;
        }
        
        Comment comment = commentOpt.get();
        
        // 1. 检查是否为评论作者
        if (comment.getAuthor() != null && user.getId().equals(comment.getAuthor().getId())) {
            return true;
        }
        
        // 2. 检查是否为帖子作者
        if (comment.getPost() != null && comment.getPost().getAuthor() != null && 
            user.getId().equals(comment.getPost().getAuthor().getId())) {
            return true;
        }
        
        // 3. 检查是否有管理权限
        if (hasPermission(userId, "COMMENT_MANAGE")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查用户是否可以编辑评论
     * 评论可以被评论作者、帖子作者或管理员编辑
     * 
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 是否可以编辑
     */
    public boolean canEditComment(Long userId, Long commentId) {
        log.debug("检查评论编辑权限: userId={}, commentId={}", userId, commentId);
        
        if (userId == null || commentId == null) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: userId={}", userId);
            return false;
        }
        User user = userOpt.get();
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return false;
        }
        
        Comment comment = commentOpt.get();
        
        // 1. 检查是否为评论作者
        if (comment.getAuthor() != null && user.getId().equals(comment.getAuthor().getId())) {
            return true;
        }
        
        // 2. 检查是否为帖子作者
        if (comment.getPost() != null && comment.getPost().getAuthor() != null && 
            user.getId().equals(comment.getPost().getAuthor().getId())) {
            return true;
        }
        
        // 3. 检查是否有管理权限
        if (hasPermission(userId, "COMMENT_MANAGE")) {
            return true;
        }
        
        return false;
    }
    
    // ==================== 基于角色的接口访问控制 ====================
    
    /**
     * 检查用户是否具有指定角色
     * 
     * @param username 用户名
     * @param roleName 角色名称
     * @return 是否具有角色
     */
    @Cacheable(value = "userRoles", key = "#username + ':' + #roleName")
    public boolean hasRole(String username, String roleName) {
        log.debug("检查用户角色: username={}, roleName={}", username, roleName);
        
        if (username == null || roleName == null) {
            return false;
        }
        
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        
        boolean hasRole = user.hasRoleName(roleName);
        
        log.debug("角色检查结果: username={}, roleName={}, hasRole={}", username, roleName, hasRole);
        
        return hasRole;
    }
    
    /**
     * 检查用户是否具有管理权限
     * 
     * @param username 用户名
     * @return 是否具有管理权限
     */
    public boolean hasManagementPermission(String username) {
        return hasRole(username, "ADMIN") || hasRole(username, "MODERATOR");
    }
    
    /**
     * 检查用户是否具有系统管理权限
     * 
     * @param username 用户名
     * @return 是否具有系统管理权限
     */
    public boolean hasSystemAdminPermission(String username) {
        return hasRole(username, "ADMIN");
    }
    
    /**
     * 检查用户是否可以管理其他用户
     * 
     * @param operatorUserId 操作者用户名
     * @param targetUserId 目标用户ID
     * @return 是否可以管理
     */
    public boolean canManageUser(Long operatorUserId, Long targetUserId) {
        log.debug("检查用户管理权限: operatorUserId={}, targetUserId={}", operatorUserId, targetUserId);
        
        if (operatorUserId == null || targetUserId == null) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findById(operatorUserId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: operatorUserId={}", operatorUserId);
            return false;
        }
        User operator = userOpt.get();
        // 用户可以管理自己
        if (operator.getId().equals(targetUserId)) {
            return true;
        }
        
        // 检查是否有用户管理权限
        if (!hasPermission(operatorUserId, "USER_MANAGE")) {
            return false;
        }
        
        // 使用RoleService检查角色层级权限
        return roleService.hasPermissionToManageUser(operator.getId(), targetUserId);
    }
    
    // ==================== 权限验证辅助方法 ====================
    
    /**
     * 验证用户权限，如果没有权限则抛出异常
     * 
     * @param userId 用户名
     * @param permission 权限名称
     * @throws AuthorizationException 如果没有权限
     */
    public void requirePermission(Long userId, String permission) {
        if (!hasPermission(userId, permission)) {
            log.warn("权限验证失败: userId={}, permission={}", userId, permission);
            
            // 记录审计日志
            auditLogService.logSecurityEvent(AuditOperation.ACCESS_DENIED , "Id:"+userId,
                                            "权限验证失败: " + permission);
            
            throw new AuthorizationException("权限不足: " + permission);
        }
    }
    
    /**
     * 验证资源访问权限，如果没有权限则抛出异常
     * 
     * @param userId 用户名
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param actionType 操作类型
     * @throws AuthorizationException 如果没有权限
     */
    public void requireResourceAccess(Long userId, String resourceType, Long resourceId, String actionType) {
        if (!canAccessResource(userId, resourceType, resourceId, actionType)) {
            log.warn("资源访问权限验证失败: userId={}, resourceType={}, resourceId={}, actionType={}",
                    userId, resourceType, resourceId, actionType);
            
            // 记录审计日志
            auditLogService.logSecurityEvent(AuditOperation.ACCESS_DENIED, "Id:"+userId,
                                            String.format("资源访问被拒绝: %s:%s:%s", resourceType, resourceId, actionType));
            
            throw new AuthorizationException(String.format("无权访问资源: %s:%s", resourceType, resourceId));
        }
    }
    
    /**
     * 验证角色权限，如果没有角色则抛出异常
     * 
     * @param username 用户名
     * @param roleName 角色名称
     * @throws AuthorizationException 如果没有角色
     */
    public void requireRole(String username, String roleName) {
        if (!hasRole(username, roleName)) {
            log.warn("角色验证失败: username={}, roleName={}", username, roleName);
            
            // 记录审计日志
            auditLogService.logSecurityEvent(AuditOperation.ACCESS_DENIED, username,
                                            "角色验证失败: " + roleName);
            
            throw new AuthorizationException("角色权限不足: " + roleName);
        }
    }
    
    // ==================== 权限缓存管理 ====================
    
    /**
     * 刷新用户权限缓存
     * 当用户权限发生变更时调用
     * 
     * @param username 用户名
     */
    @CacheEvict(value = {"userPermissions", "userResourcePermissions", "userRoles"}, key = "#username")
    public void refreshUserPermissionCache(String username) {
        log.info("刷新用户权限缓存: username={}", username);
        
        // 清除Redis中的相关缓存
        String permissionPattern = USER_PERMISSIONS_CACHE_KEY + username + ":*";
        String rolePattern = USER_ROLES_CACHE_KEY + username + ":*";
        
        clearCacheByPattern(permissionPattern);
        clearCacheByPattern(rolePattern);
        
        log.info("用户权限缓存刷新完成: username={}", username);
    }
    
    /**
     * 刷新资源所有权缓存
     * 当资源所有权发生变更时调用
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     */
    @CacheEvict(value = "resourceOwnership", allEntries = true)
    public void refreshResourceOwnershipCache(String resourceType, Long resourceId) {
        log.info("刷新资源所有权缓存: resourceType={}, resourceId={}", resourceType, resourceId);
        
        // 清除Redis中的相关缓存
        String pattern = RESOURCE_OWNERSHIP_CACHE_KEY + "*:" + resourceType + ":" + resourceId;
        clearCacheByPattern(pattern);
        
        log.info("资源所有权缓存刷新完成: resourceType={}, resourceId={}", resourceType, resourceId);
    }
    
    /**
     * 清除所有权限相关缓存
     * 当进行批量权限变更时调用
     */
    @CacheEvict(value = {"userPermissions", "userResourcePermissions", "userRoles", "resourceOwnership"}, allEntries = true)
    public void clearAllPermissionCache() {
        log.info("清除所有权限缓存");
        
        // 清除Redis中的所有权限相关缓存
        clearCacheByPattern(USER_PERMISSIONS_CACHE_KEY + "*");
        clearCacheByPattern(USER_ROLES_CACHE_KEY + "*");
        clearCacheByPattern(RESOURCE_OWNERSHIP_CACHE_KEY + "*");
        
        log.info("所有权限缓存清除完成");
    }
    
    /**
     * 根据模式清除Redis缓存
     * 
     * @param pattern 缓存键模式
     */
    private void clearCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("清除缓存键: count={}, pattern={}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("清除缓存失败: pattern={}, error={}", pattern, e.getMessage());
        }
    }
    
    // ==================== 权限检查策略方法 ====================
    
    /**
     * 判断指定操作是否需要所有权检查
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否需要所有权检查
     */
    private boolean requiresOwnershipCheck(String resourceType, String actionType) {
        // 编辑和删除操作通常需要所有权检查
        if ("EDIT".equalsIgnoreCase(actionType) || "UPDATE".equalsIgnoreCase(actionType) || 
            "DELETE".equalsIgnoreCase(actionType)) {
            return true;
        }
        
        // 特定资源的特定操作需要所有权检查
        if ("POST".equalsIgnoreCase(resourceType)) {
            return "EDIT".equalsIgnoreCase(actionType) || "DELETE".equalsIgnoreCase(actionType);
        }
        
        if ("COMMENT".equalsIgnoreCase(resourceType)) {
            return "EDIT".equalsIgnoreCase(actionType) || "DELETE".equalsIgnoreCase(actionType);
        }
        
        if ("USER".equalsIgnoreCase(resourceType)) {
            return "EDIT".equalsIgnoreCase(actionType) || "UPDATE".equalsIgnoreCase(actionType);
        }
        
        return false;
    }
    
    // ==================== 获取用户权限信息 ====================
    
    /**
     * 获取用户的所有权限
     * 
     * @param username 用户名
     * @return 权限名称集合
     */
    @Cacheable(value = "userAllPermissions", key = "#username")
    public Set<String> getUserPermissions(String username) {
        log.debug("获取用户所有权限: username={}", username);
        
        if (username == null) {
            return Set.of();
        }
        
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return Set.of();
        }
        
        return user.getAllPermissions();
    }
    
    /**
     * 获取用户的所有角色
     * 
     * @param username 用户名
     * @return 角色名称集合
     */
    @Cacheable(value = "userAllRoles", key = "#username")
    public Set<String> getUserRoles(String username) {
        log.debug("获取用户所有角色: username={}", username);
        
        if (username == null) {
            return Set.of();
        }
        
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return Set.of();
        }
        
        return user.getAllRoleNames();
    }
    
    /**
     * 检查用户是否有任何管理权限
     * 
     * @param username 用户名
     * @return 是否有管理权限
     */
    public boolean hasAnyManagementPermission(String username) {
        if (username == null) {
            return false;
        }
        
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        
        return user.hasAnyManagementPermission();
    }
}