package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.domain.security.entity.Permission;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.PermissionRepository;
import com.myweb.website_core.infrastructure.persistence.repository.RoleRepository;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限控制功能测试
 * 
 * 测试Spring Security权限控制的各个组件，包括：
 * - 自定义权限评估器
 * - 访问决策投票器
 * - 权限拒绝处理器
 * - 方法级权限控制
 * 
 * 符合GB/T 22239-2019二级等保要求的访问控制测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PermissionControlTest {
    
    @Autowired
    private CustomPermissionEvaluator permissionEvaluator;
    
    @Autowired
    private CustomAccessDecisionVoter accessDecisionVoter;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    private User testUser;
    private User adminUser;
    private Role userRole;
    private Role adminRole;
    private Permission postCreatePermission;
    private Permission postUpdatePermission;
    private Permission systemManagePermission;
    
    @BeforeEach
    void setUp() {
        // 创建测试权限
        postCreatePermission = createPermission("POST_CREATE", "POST", "CREATE", "帖子创建权限");
        postUpdatePermission = createPermission("POST_UPDATE", "POST", "UPDATE", "帖子更新权限");
        systemManagePermission = createPermission("SYSTEM_MANAGE", "SYSTEM", "MANAGE", "系统管理权限");
        
        // 创建测试角色
        userRole = createRole("USER", "普通用户", 1);
        adminRole = createRole("ADMIN", "管理员", 10);
        
        // 为角色分配权限
        userRole.addPermission(postCreatePermission);
        adminRole.addPermission(postCreatePermission);
        adminRole.addPermission(postUpdatePermission);
        adminRole.addPermission(systemManagePermission);
        
        roleRepository.save(userRole);
        roleRepository.save(adminRole);
        
        // 创建测试用户
        testUser = createUser("testuser", "test@example.com", userRole);
        adminUser = createUser("admin", "admin@example.com", adminRole);
    }
    
    /**
     * 测试自定义权限评估器 - 一般权限检查
     */
    @Test
    void testCustomPermissionEvaluator_GeneralPermission() {
        // 创建用户认证信息
        CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
            new CustomUserDetailsService.CustomUserPrincipal(testUser);
        
        // 测试用户具有的权限
        assertTrue(permissionEvaluator.hasPermission(
            createAuthentication(userPrincipal), null, "POST_CREATE"));
        
        // 测试用户不具有的权限
        assertFalse(permissionEvaluator.hasPermission(
            createAuthentication(userPrincipal), null, "SYSTEM_MANAGE"));
        
        // 测试管理员权限
        CustomUserDetailsService.CustomUserPrincipal adminPrincipal = 
            new CustomUserDetailsService.CustomUserPrincipal(adminUser);
        
        assertTrue(permissionEvaluator.hasPermission(
            createAuthentication(adminPrincipal), null, "SYSTEM_MANAGE"));
    }
    
    /**
     * 测试自定义权限评估器 - 对象权限检查
     */
    @Test
    void testCustomPermissionEvaluator_ObjectPermission() {
        CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
            new CustomUserDetailsService.CustomUserPrincipal(testUser);
        
        // 测试帖子权限检查
        assertTrue(permissionEvaluator.hasPermission(
            createAuthentication(userPrincipal), 1L, "Post", "read"));
        
        // 测试用户权限检查
        assertTrue(permissionEvaluator.hasPermission(
            createAuthentication(userPrincipal), testUser.getId(), "User", "edit"));
        
        assertFalse(permissionEvaluator.hasPermission(
            createAuthentication(userPrincipal), adminUser.getId(), "User", "edit"));
    }
    
    /**
     * 测试访问决策投票器支持性检查
     */
    @Test
    void testAccessDecisionVoter_Supports() {
        ConfigAttribute attribute = new ConfigAttribute() {
            @Override
            public String getAttribute() {
                return null;
            }
        };
        // 测试配置属性支持
        assertTrue(accessDecisionVoter.supports(attribute));
        
        // 测试安全对象类型支持
        assertTrue(accessDecisionVoter.supports(org.springframework.security.web.FilterInvocation.class));
        assertFalse(accessDecisionVoter.supports(String.class));
    }
    
    /**
     * 测试用户详情服务
     */
    @Test
    void testCustomUserDetailsService() {
        // 测试根据用户名加载用户
        CustomUserDetailsService.CustomUserPrincipal userDetails = 
            (CustomUserDetailsService.CustomUserPrincipal) userDetailsService.loadUserByUsername("testuser");
        
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals(testUser.getId(), userDetails.getUserId());
        assertTrue(userDetails.hasPermission("POST_CREATE"));
        assertFalse(userDetails.hasPermission("SYSTEM_MANAGE"));
        
        // 测试根据邮箱加载用户
        userDetails = (CustomUserDetailsService.CustomUserPrincipal) 
            userDetailsService.loadUserByUsername("test@example.com");
        
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
    }
    
    /**
     * 测试用户详情服务 - 用户不存在
     */
    @Test
    void testCustomUserDetailsService_UserNotFound() {
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nonexistent");
        });
    }
    
    /**
     * 测试用户主体的权限检查方法
     */
    @Test
    void testCustomUserPrincipal_PermissionChecks() {
        CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
            new CustomUserDetailsService.CustomUserPrincipal(testUser);
        
        // 测试权限检查
        assertTrue(userPrincipal.hasPermission("POST_CREATE"));
        assertFalse(userPrincipal.hasPermission("SYSTEM_MANAGE"));
        
        // 测试角色检查
        assertTrue(userPrincipal.hasRole("USER"));
        assertFalse(userPrincipal.hasRole("ADMIN"));
        
        // 测试账户状态
        assertTrue(userPrincipal.isAccountNonLocked());
        assertTrue(userPrincipal.isEnabled());
        assertTrue(userPrincipal.isAccountNonExpired());
        assertTrue(userPrincipal.isCredentialsNonExpired());
    }
    
    /**
     * 测试管理员用户的权限
     */
    @Test
    void testAdminUserPermissions() {
        CustomUserDetailsService.CustomUserPrincipal adminPrincipal = 
            new CustomUserDetailsService.CustomUserPrincipal(adminUser);
        
        // 管理员应该具有所有权限
        assertTrue(adminPrincipal.hasPermission("POST_CREATE"));
        assertTrue(adminPrincipal.hasPermission("POST_UPDATE"));
        assertTrue(adminPrincipal.hasPermission("SYSTEM_MANAGE"));
        
        // 管理员角色检查
        assertTrue(adminPrincipal.hasRole("ADMIN"));
        
        // 检查权限数量
        assertTrue(adminPrincipal.getAuthorities().size() > 1);
    }
    
    /**
     * 测试权限缓存和性能
     */
    @Test
    void testPermissionCachingPerformance() {
        long startTime = System.currentTimeMillis();
        
        // 执行多次权限检查
        for (int i = 0; i < 100; i++) {
            authorizationService.hasPermission(testUser.getId(), "POST", "CREATE");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证性能（应该很快）
        assertTrue(duration < 1000, "权限检查性能测试失败，耗时: " + duration + "ms");
    }
    
    /**
     * 测试权限层次结构
     */
    @Test
    void testPermissionHierarchy() {
        // 测试管理员是否具有普通用户的所有权限
        assertTrue(authorizationService.hasPermission(adminUser.getId(), "POST", "CREATE"));
        
        // 测试普通用户不具有管理员权限
        assertFalse(authorizationService.hasPermission(testUser.getId(), "SYSTEM", "MANAGE"));
    }
    
    /**
     * 测试权限的动态变更
     */
    @Test
    void testDynamicPermissionChange() {
        // 初始状态：用户没有系统管理权限
        assertFalse(authorizationService.hasPermission(testUser.getId(), "SYSTEM", "MANAGE"));
        
        // 为用户角色添加系统管理权限
        userRole.addPermission(systemManagePermission);
        roleRepository.save(userRole);
        
        // 清除缓存后应该具有权限
        // 注意：这里可能需要清除权限缓存
        assertTrue(authorizationService.hasPermission(testUser.getId(), "SYSTEM", "MANAGE"));
    }
    
    /**
     * 测试权限的边界条件
     */
    @Test
    void testPermissionBoundaryConditions() {
        // 测试null值处理
        assertFalse(authorizationService.hasPermission(null, "POST", "CREATE"));
        assertFalse(authorizationService.hasPermission(testUser.getId(), null, "CREATE"));
        assertFalse(authorizationService.hasPermission(testUser.getId(), "POST", null));
        
        // 测试不存在的权限
        assertFalse(authorizationService.hasPermission(testUser.getId(), "NONEXISTENT", "ACTION"));
        
        // 测试不存在的用户
        assertFalse(authorizationService.hasPermission(99999L, "POST", "CREATE"));
    }
    
    /**
     * 创建测试用户
     */
    private User createUser(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setPasswordHash("$2a$12$hashedPassword");
        user.setEmailVerified(true);
        user.addRole(role);
        
        return userRepository.save(user);
    }
    
    /**
     * 创建测试角色
     */
    private Role createRole(String name, String displayName, int priority) {
        Role role = new Role();
        role.setName(name);
        role.setDisplayName(displayName);
        role.setPriority(priority);
        role.setEnabled(true);
        role.setSystemRole(true);
        
        return role;
    }
    
    /**
     * 创建测试权限
     */
    private Permission createPermission(String name, String resourceType, String actionType, String description) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setResourceType(resourceType);
        permission.setActionType(actionType);
        permission.setDisplayName(description);
        permission.setDescription(description);
        permission.setEnabled(true);
        permission.setSystemPermission(true);
        
        return permissionRepository.save(permission);
    }
    
    /**
     * 创建认证对象
     */
    private org.springframework.security.core.Authentication createAuthentication(
            CustomUserDetailsService.CustomUserPrincipal principal) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities());
    }
}