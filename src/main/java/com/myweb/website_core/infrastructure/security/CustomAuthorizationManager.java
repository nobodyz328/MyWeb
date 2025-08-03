package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.domain.business.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 自定义授权管理器
 *
 * 实现复杂的权限判断逻辑，支持：
 * - 基于资源所有权的访问控制
 * - 动态权限验证
 * - 上下文相关的权限判断
 * 
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 */
@Component
public class CustomAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    
    private final AuthorizationService authorizationService;
    @Autowired
    public CustomAuthorizationManager(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * 进行授权决策
     * 
     * @param authentication 认证信息供应商
     * @param context 请求授权上下文
     * @return 授权决策结果
     */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        
        // 获取用户信息
        User user = getCurrentUser(auth);
        if (user == null) {
            return new AuthorizationDecision(false);
        }
        
        // 获取请求信息
        String requestUrl = context.getRequest().getRequestURI();
        String httpMethod = context.getRequest().getMethod();
        
        // 执行自定义权限判断逻辑
        try {
            boolean hasPermission = hasCustomPermission(user, requestUrl, httpMethod);
            return new AuthorizationDecision(hasPermission);
        } catch (Exception e) {
            // 权限判断异常时拒绝访问
            return new AuthorizationDecision(false);
        }
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
     * 执行自定义权限判断
     * 
     * @param user 用户对象
     * @param requestUrl 请求URL
     * @param httpMethod HTTP方法
     * @return 是否有权限
     */
    private boolean hasCustomPermission(User user, String requestUrl, String httpMethod) {
        
        // 1. 检查帖子相关权限
        if (requestUrl != null && requestUrl.contains("/posts/")) {
            return checkPostPermission(user, requestUrl, httpMethod);
        }
        
        // 2. 检查评论相关权限
        if (requestUrl != null && requestUrl.contains("/comments/")) {
            return checkCommentPermission(user, requestUrl, httpMethod);
        }
        
        // 3. 检查用户管理权限
        if (requestUrl != null && requestUrl.contains("/users/manage/")) {
            return checkUserManagementPermission(user, requestUrl, httpMethod);
        }
        
        // 4. 检查系统管理权限
        if (requestUrl != null && (requestUrl.contains("/admin/") || requestUrl.contains("/system/"))) {
            return checkSystemManagementPermission(user, requestUrl, httpMethod);
        }
        
        // 5. 检查审计日志权限
        if (requestUrl != null && requestUrl.contains("/audit/")) {
            return checkAuditPermission(user, requestUrl, httpMethod);
        }
        
        // 其他情况允许通过，由Spring Security的默认权限控制处理
        return true;
    }
    
    /**
     * 检查帖子相关权限
     * 
     * @param user 用户对象
     * @param requestUrl 请求URL
     * @param httpMethod HTTP方法
     * @return 是否有权限
     */
    private boolean checkPostPermission(User user, String requestUrl, String httpMethod) {
        // 创建帖子权限
        if (requestUrl.contains("/posts/new") || requestUrl.contains("/posts/create")) {
            return authorizationService.hasPermission(user.getId(), "POST", "CREATE");
        }
        
        // 编辑帖子权限 - 需要检查是否为帖子作者或有管理权限
        if (requestUrl.contains("/posts/edit/") || (requestUrl.contains("/posts/") && "PUT".equals(httpMethod))) {
            Long postId = extractPostIdFromUrl(requestUrl);
            if (postId != null) {
                return authorizationService.canEditPost(user.getId(), postId);
            }
        }
        
        // 删除帖子权限 - 需要检查是否为帖子作者或有管理权限
        if (requestUrl.contains("/posts/") && ("DELETE".equals(httpMethod) || requestUrl.contains("/delete"))) {
            Long postId = extractPostIdFromUrl(requestUrl);
            if (postId != null) {
                return authorizationService.canDeletePost(user.getId(), postId);
            }
        }
        
        return true; // 其他帖子相关操作允许通过
    }
    
    /**
     * 检查评论相关权限
     * 
     * @param user 用户对象
     * @param requestUrl 请求URL
     * @param httpMethod HTTP方法
     * @return 是否有权限
     */
    private boolean checkCommentPermission(User user, String requestUrl, String httpMethod) {
        // 创建评论权限
        if ("POST".equals(httpMethod) && requestUrl.contains("/comments")) {
            return authorizationService.hasPermission(user.getId(), "COMMENT", "CREATE");
        }
        
        // 编辑评论权限
        if ("PUT".equals(httpMethod) && requestUrl.contains("/comments/")) {
            Long commentId = extractCommentIdFromUrl(requestUrl);
            if (commentId != null) {
                return authorizationService.canEditComment(user.getId(), commentId);
            }
        }
        
        // 删除评论权限
        if ("DELETE".equals(httpMethod) && requestUrl.contains("/comments/")) {
            Long commentId = extractCommentIdFromUrl(requestUrl);
            if (commentId != null) {
                return authorizationService.canDeleteComment(user.getId(), commentId);
            }
        }
        
        return true; // 其他评论相关操作允许通过
    }
    
    /**
     * 检查用户管理权限
     * 
     * @param user 用户对象
     * @param requestUrl 请求URL
     * @param httpMethod HTTP方法
     * @return 是否有权限
     */
    private boolean checkUserManagementPermission(User user, String requestUrl, String httpMethod) {
        return authorizationService.hasPermission(user.getId(), "USER", "MANAGE");
    }
    
    /**
     * 检查系统管理权限
     * 
     * @param user 用户对象
     * @param requestUrl 请求URL
     * @param httpMethod HTTP方法
     * @return 是否有权限
     */
    private boolean checkSystemManagementPermission(User user, String requestUrl, String httpMethod) {
        return authorizationService.hasPermission(user.getId(), "SYSTEM", "MANAGE");
    }
    
    /**
     * 检查审计日志权限
     * 
     * @param user 用户对象
     * @param requestUrl 请求URL
     * @param httpMethod HTTP方法
     * @return 是否有权限
     */
    private boolean checkAuditPermission(User user, String requestUrl, String httpMethod) {
        return authorizationService.hasPermission(user.getId(), "AUDIT", "READ");
    }
    
    /**
     * 从URL中提取帖子ID
     * 
     * @param url 请求URL
     * @return 帖子ID
     */
    private Long extractPostIdFromUrl(String url) {
        try {
            // 匹配 /posts/{id} 或 /posts/edit/{id} 等格式
            String[] parts = url.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("posts".equals(parts[i]) && i + 1 < parts.length) {
                    String nextPart = parts[i + 1];
                    if (!"new".equals(nextPart) && !"create".equals(nextPart) && !"edit".equals(nextPart)) {
                        return Long.parseLong(nextPart);
                    }
                    if ("edit".equals(nextPart) && i + 2 < parts.length) {
                        return Long.parseLong(parts[i + 2]);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }
        return null;
    }
    
    /**
     * 从URL中提取评论ID
     * 
     * @param url 请求URL
     * @return 评论ID
     */
    private Long extractCommentIdFromUrl(String url) {
        try {
            // 匹配 /comments/{id} 格式
            String[] parts = url.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("comments".equals(parts[i]) && i + 1 < parts.length) {
                    return Long.parseLong(parts[i + 1]);
                }
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }
        return null;
    }
}