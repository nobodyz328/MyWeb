package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.domain.business.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 自定义访问决策投票器
 * 
 * 实现复杂的权限判断逻辑，支持：
 * - 基于资源所有权的访问控制
 * - 动态权限验证
 * - 上下文相关的权限判断
 * 
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 */
@Component
public class CustomAccessDecisionVoter implements AccessDecisionVoter<Object> {
    
    @Autowired
    private AuthorizationService authorizationService;
    
    /**
     * 检查是否支持指定的配置属性
     * 
     * @param attribute 配置属性
     * @return 是否支持
     */
    @Override
    public boolean supports(ConfigAttribute attribute) {
        // 支持所有配置属性，让具体的投票逻辑来判断
        return true;
    }
    
    /**
     * 检查是否支持指定的安全对象类型
     * 
     * @param clazz 安全对象类型
     * @return 是否支持
     */
    @Override
    public boolean supports(Class<?> clazz) {
        // 主要支持Web请求的权限判断
        return FilterInvocation.class.isAssignableFrom(clazz);
    }
    
    /**
     * 进行访问决策投票
     * 
     * @param authentication 认证信息
     * @param object 安全对象（通常是FilterInvocation）
     * @param attributes 配置属性集合
     * @return 投票结果
     */
    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ACCESS_DENIED;
        }
        
        // 获取用户信息
        User user = getCurrentUser(authentication);
        if (user == null) {
            return ACCESS_DENIED;
        }
        
        // 获取请求信息
        String requestUrl = getRequestUrl(object);
        String httpMethod = getHttpMethod(object);
        
        // 执行自定义权限判断逻辑
        try {
            if (hasCustomPermission(user, requestUrl, httpMethod, attributes)) {
                return ACCESS_GRANTED;
            }
        } catch (Exception e) {
            // 权限判断异常时拒绝访问
            return ACCESS_DENIED;
        }
        
        // 如果没有明确的权限判断结果，则弃权
        return ACCESS_ABSTAIN;
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
     * 获取请求URL
     * 
     * @param object 安全对象
     * @return 请求URL
     */
    private String getRequestUrl(Object object) {
        if (object instanceof FilterInvocation) {
            FilterInvocation filterInvocation = (FilterInvocation) object;
            return filterInvocation.getRequestUrl();
        }
        return null;
    }
    
    /**
     * 获取HTTP方法
     * 
     * @param object 安全对象
     * @return HTTP方法
     */
    private String getHttpMethod(Object object) {
        if (object instanceof FilterInvocation) {
            FilterInvocation filterInvocation = (FilterInvocation) object;
            return filterInvocation.getHttpRequest().getMethod();
        }
        return null;
    }
    
    /**
     * 执行自定义权限判断
     * 
     * @param user 用户对象
     * @param requestUrl 请求URL
     * @param httpMethod HTTP方法
     * @param attributes 配置属性
     * @return 是否有权限
     */
    private boolean hasCustomPermission(User user, String requestUrl, String httpMethod, 
                                      Collection<ConfigAttribute> attributes) {
        
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
        
        // 其他情况弃权，由其他投票器处理
        return false;
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
        
        return false;
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
        
        return false;
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