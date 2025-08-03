package com.myweb.website_core.interfaces.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 安全测试控制器
 * 
 * 用于测试Spring Security权限控制功能
 * 包括URL级别和方法级别的权限控制
 * 
 * 符合GB/T 22239-2019二级等保要求的访问控制测试
 */
@RestController
@RequestMapping("/api/security-test")
public class SecurityTestController {
    
    /**
     * 公开访问的测试接口
     */
    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is a public endpoint";
    }
    
    /**
     * 需要认证的测试接口
     */
    @GetMapping("/authenticated")
    public String authenticatedEndpoint() {
        return "This endpoint requires authentication";
    }
    
    /**
     * 需要POST_CREATE权限的测试接口
     */
    @PostMapping("/posts")
    @PreAuthorize("hasAuthority('POST_CREATE')")
    public String createPost() {
        return "Post created successfully";
    }
    
    /**
     * 需要POST_UPDATE权限的测试接口
     */
    @PutMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('POST_UPDATE') or hasPermission(#id, 'Post', 'edit')")
    public String updatePost(@PathVariable Long id) {
        return "Post " + id + " updated successfully";
    }
    
    /**
     * 需要POST_DELETE权限的测试接口
     */
    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('POST_DELETE') or hasPermission(#id, 'Post', 'delete')")
    public String deletePost(@PathVariable Long id) {
        return "Post " + id + " deleted successfully";
    }
    
    /**
     * 需要USER_MANAGE权限的测试接口
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String manageUsers() {
        return "User management interface";
    }
    
    /**
     * 需要SYSTEM_MANAGE权限的测试接口
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    public String adminPanel() {
        return "Admin panel";
    }
    
    /**
     * 需要AUDIT_READ权限的测试接口
     */
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public String auditLogs() {
        return "Audit logs";
    }
    
    /**
     * 测试复杂权限表达式
     */
    @GetMapping("/complex/{userId}")
    @PreAuthorize("hasAuthority('USER_MANAGE') or (authentication.principal.userId == #userId)")
    public String complexPermission(@PathVariable Long userId) {
        return "Access granted for user " + userId;
    }
    
    /**
     * 测试角色权限
     */
    @GetMapping("/role-test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public String roleTest() {
        return "Role-based access granted";
    }
    
    /**
     * 测试多重权限条件
     */
    @PostMapping("/multi-permission")
    @PreAuthorize("hasAuthority('POST_CREATE') and hasAuthority('COMMENT_CREATE')")
    public String multiPermission() {
        return "Multiple permissions verified";
    }
}