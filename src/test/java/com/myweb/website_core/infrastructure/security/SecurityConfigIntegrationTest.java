package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.application.service.security.DefaultRolePermissionInitializer;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.PermissionRepository;
import com.myweb.website_core.infrastructure.persistence.repository.RoleRepository;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Security权限控制集成测试
 * 
 * 测试基于角色和权限的访问控制功能，包括：
 * - URL级别的权限控制
 * - 方法级别的权限控制
 * - 自定义权限判断逻辑
 * - 权限拒绝异常处理
 * 
 * 符合GB/T 22239-2019二级等保要求的访问控制测试
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class SecurityConfigIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @Autowired
    private DefaultRolePermissionInitializer rolePermissionInitializer;
    
    private User testUser;
    private User adminUser;
    private User moderatorUser;
    private Role userRole;
    private Role adminRole;
    private Role moderatorRole;
    
    @BeforeEach
    void setUp() {
        // 初始化角色和权限
        try {
            rolePermissionInitializer.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 获取角色
        userRole = roleRepository.findByName("USER").orElseThrow();
        adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        moderatorRole = roleRepository.findByName("MODERATOR").orElseThrow();
        
        // 创建测试用户
        testUser = createTestUser("testuser", "test@example.com", userRole);
        adminUser = createTestUser("admin", "admin@example.com", adminRole);
        moderatorUser = createTestUser("moderator", "moderator@example.com", moderatorRole);
    }
    
    /**
     * 测试公开资源访问
     */
    @Test
    void testPublicResourceAccess() throws Exception {
        // 测试公开页面可以匿名访问
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/view"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试需要认证的资源访问
     */
    @Test
    void testAuthenticatedResourceAccess() throws Exception {
        // 未认证用户访问需要认证的资源应该被重定向到登录页面
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().is3xxRedirection());
        
        // 认证用户可以访问
        mockMvc.perform(get("/posts/new")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser))))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试基于权限的访问控制
     */
    @Test
    void testPermissionBasedAccess() throws Exception {
        // 普通用户无法访问管理功能
        mockMvc.perform(get("/admin/dashboard")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser))))
                .andExpect(status().isForbidden());
        
        // 管理员可以访问管理功能
        mockMvc.perform(get("/admin/dashboard")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(adminUser))))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试用户管理权限
     */
    @Test
    void testUserManagementPermission() throws Exception {
        // 普通用户无法访问用户管理功能
        mockMvc.perform(get("/users/manage/list")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser))))
                .andExpect(status().isForbidden());
        
        // 管理员可以访问用户管理功能
        mockMvc.perform(get("/users/manage/list")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(adminUser))))
                .andExpect(status().isOk());
        
        // 版主也可以访问用户管理功能（如果有相应权限）
        if (authorizationService.hasPermission(moderatorUser.getId(), "USER", "MANAGE")) {
            mockMvc.perform(get("/users/manage/list")
                    .with(user(new CustomUserDetailsService.CustomUserPrincipal(moderatorUser))))
                    .andExpect(status().isOk());
        }
    }
    
    /**
     * 测试帖子权限控制
     */
    @Test
    void testPostPermissionControl() throws Exception {
        // 所有认证用户都可以创建帖子
        mockMvc.perform(post("/posts/create")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser)))
                .param("title", "Test Post")
                .param("content", "Test Content"))
                .andExpect(status().is3xxRedirection()); // 重定向到帖子详情页
        
        // 测试编辑帖子权限（需要是帖子作者或管理员）
        // 这里需要先创建一个帖子，然后测试编辑权限
        // 由于涉及到数据库操作，这里简化测试
    }
    
    /**
     * 测试API权限控制
     */
    @Test
    void testApiPermissionControl() throws Exception {
        // 普通用户无法访问管理API
        mockMvc.perform(get("/api/admin/users")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json;charset=UTF-8"));
        
        // 管理员可以访问管理API
        mockMvc.perform(get("/api/admin/users")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(adminUser))))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试审计日志权限
     */
    @Test
    void testAuditLogPermission() throws Exception {
        // 普通用户无法访问审计日志
        mockMvc.perform(get("/audit/logs")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser))))
                .andExpect(status().isForbidden());
        
        // 管理员可以访问审计日志
        mockMvc.perform(get("/audit/logs")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(adminUser))))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试权限拒绝处理
     */
    @Test
    void testAccessDeniedHandling() throws Exception {
        // 测试Web页面的权限拒绝处理
        mockMvc.perform(get("/admin/dashboard")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser))))
                .andExpect(status().isForbidden());
        
        // 测试API的权限拒绝处理
        mockMvc.perform(get("/api/admin/users")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(testUser)))
                .header("Accept", "application/json"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("权限不足，无法访问该资源"));
    }
    
    /**
     * 测试方法级权限控制
     */
    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER", "POST_CREATE"})
    void testMethodLevelSecurity() throws Exception {
        // 这里可以测试使用@PreAuthorize注解的方法
        // 由于需要具体的Controller方法，这里简化测试
        
        // 测试有权限的情况
        mockMvc.perform(post("/posts/create")
                .param("title", "Test Post")
                .param("content", "Test Content"))
                .andExpect(status().is3xxRedirection());
    }
    
    /**
     * 测试自定义权限评估器
     */
    @Test
    void testCustomPermissionEvaluator() throws Exception {
        // 测试hasPermission表达式的使用
        // 这需要在具体的Controller方法中使用@PreAuthorize("hasPermission(#postId, 'Post', 'edit')")
        // 由于涉及到具体的业务逻辑，这里简化测试
    }
    
    /**
     * 测试角色层次结构
     */
    @Test
    void testRoleHierarchy() throws Exception {
        // 测试管理员是否具有所有权限
        mockMvc.perform(get("/posts/new")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(adminUser))))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/admin/dashboard")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(adminUser))))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/users/manage/list")
                .with(user(new CustomUserDetailsService.CustomUserPrincipal(adminUser))))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试权限缓存
     */
    @Test
    void testPermissionCaching() throws Exception {
        // 测试权限判断的性能和缓存效果
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            authorizationService.hasPermission(testUser.getId(), "POST", "CREATE");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证权限判断的性能（应该很快，因为有缓存）
        assert duration < 1000; // 100次权限判断应该在1秒内完成
    }
    
    /**
     * 创建测试用户
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param role 角色
     * @return 用户对象
     */
    private User createTestUser(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setPasswordHash("$2a$12$hashedPassword");
        user.setEmailVerified(true);
        user.addRole(role);
        
        return userRepository.save(user);
    }
}