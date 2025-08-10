package com.myweb.website_core.application.service;

import com.myweb.website_core.domain.security.entity.Permission;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.PermissionRepository;
import com.myweb.website_core.infrastructure.persistence.repository.RoleRepository;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.authorization.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 角色管理服务测试类
 * 
 * 测试RoleService的各项功能，包括：
 * - 角色CRUD操作
 * - 角色权限管理
 * - 用户角色分配
 * - 角色层级管理
 * - 权限检查
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色管理服务测试")
class RoleServiceTest {
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private PermissionRepository permissionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private RoleService roleService;
    
    private Role testRole;
    private Permission testPermission;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // 创建测试角色
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("TEST_ROLE");
        testRole.setDisplayName("测试角色");
        testRole.setDescription("用于测试的角色");
        testRole.setPriority(10);
        testRole.setEnabled(true);
        testRole.setSystemRole(false);
        testRole.setCreatedAt(LocalDateTime.now());
        testRole.setUpdatedAt(LocalDateTime.now());
        
        // 创建测试权限
        testPermission = new Permission();
        testPermission.setId(1L);
        testPermission.setName("TEST_PERMISSION");
        testPermission.setDisplayName("测试权限");
        testPermission.setResourceType("TEST");
        testPermission.setActionType("READ");
        testPermission.setEnabled(true);
        testPermission.setSystemPermission(false);
        
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }
    
    // ==================== 角色CRUD操作测试 ====================
    
    @Test
    @DisplayName("创建角色 - 成功")
    void createRole_Success() {
        // Given
        String name = "NEW_ROLE";
        String displayName = "新角色";
        String description = "新创建的角色";
        Integer priority = 20;
        Long createdBy = 1L;
        
        when(roleRepository.existsByName(name)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        
        // When
        Role result = roleService.createRole(name, displayName, description, priority, createdBy);
        
        // Then
        assertNotNull(result);
        verify(roleRepository).existsByName(name);
        verify(roleRepository).save(any(Role.class));
        //verify(auditLogService).logRoleOperation(eq("ROLE_CREATE"), any(), eq(createdBy), anyString());
    }
    
    @Test
    @DisplayName("创建角色 - 名称已存在")
    void createRole_NameExists() {
        // Given
        String name = "EXISTING_ROLE";
        when(roleRepository.existsByName(name)).thenReturn(true);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            roleService.createRole(name, "显示名", "描述", 10, 1L));
        
        verify(roleRepository).existsByName(name);
        verify(roleRepository, never()).save(any(Role.class));
    }
    
    @Test
    @DisplayName("更新角色 - 成功")
    void updateRole_Success() {
        // Given
        Long roleId = 1L;
        String newDisplayName = "更新后的角色";
        String newDescription = "更新后的描述";
        Integer newPriority = 30;
        Boolean enabled = true;
        Long updatedBy = 1L;
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        
        // When
        Role result = roleService.updateRole(roleId, newDisplayName, newDescription, 
                                           newPriority, enabled, updatedBy);
        
        // Then
        assertNotNull(result);
        verify(roleRepository).findById(roleId);
        verify(roleRepository).save(any(Role.class));
        //verify(auditLogService).logRoleOperation(eq("ROLE_UPDATE"), eq(roleId), eq(updatedBy), anyString());
    }
    
    @Test
    @DisplayName("更新角色 - 角色不存在")
    void updateRole_NotFound() {
        // Given
        Long roleId = 999L;
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            roleService.updateRole(roleId, "新名称", "新描述", 10, true, 1L));
        
        verify(roleRepository).findById(roleId);
        verify(roleRepository, never()).save(any(Role.class));
    }
    
    @Test
    @DisplayName("更新系统角色 - 限制修改")
    void updateSystemRole_Restricted() {
        // Given
        testRole.setSystemRole(true);
        Long roleId = 1L;
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        
        // When
        Role result = roleService.updateRole(roleId, "新显示名", "新描述", 50, false, 1L);
        
        // Then
        assertNotNull(result);
        // 系统角色的enabled状态不应该被修改
        assertTrue(result.getEnabled());
        verify(roleRepository).save(any(Role.class));
    }
    
    @Test
    @DisplayName("删除角色 - 成功")
    void deleteRole_Success() {
        // Given
        Long roleId = 1L;
        Long deletedBy = 1L;
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(roleRepository.countUsersByRoleId(roleId)).thenReturn(0L);
        
        // When
        roleService.deleteRole(roleId, deletedBy);
        
        // Then
        verify(roleRepository).findById(roleId);
        verify(roleRepository).countUsersByRoleId(roleId);
        verify(roleRepository).save(any(Role.class)); // 清除权限关联
        verify(roleRepository).delete(testRole);
        //verify(auditLogService).logRoleOperation(eq("ROLE_DELETE"), eq(roleId), eq(deletedBy), anyString());
    }
    
    @Test
    @DisplayName("删除角色 - 系统角色不允许删除")
    void deleteRole_SystemRole() {
        // Given
        testRole.setSystemRole(true);
        Long roleId = 1L;
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            roleService.deleteRole(roleId, 1L));
        
        verify(roleRepository).findById(roleId);
        verify(roleRepository, never()).delete(any(Role.class));
    }
    
    @Test
    @DisplayName("删除角色 - 仍有用户使用")
    void deleteRole_HasUsers() {
        // Given
        Long roleId = 1L;
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(roleRepository.countUsersByRoleId(roleId)).thenReturn(5L);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            roleService.deleteRole(roleId, 1L));
        
        verify(roleRepository).findById(roleId);
        verify(roleRepository).countUsersByRoleId(roleId);
        verify(roleRepository, never()).delete(any(Role.class));
    }
    
    // ==================== 角色查询操作测试 ====================
    
    @Test
    @DisplayName("根据ID查找角色")
    void findById() {
        // Given
        Long roleId = 1L;
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        
        // When
        Optional<Role> result = roleService.findById(roleId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testRole, result.get());
        verify(roleRepository).findById(roleId);
    }
    
    @Test
    @DisplayName("根据名称查找角色")
    void findByName() {
        // Given
        String roleName = "TEST_ROLE";
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(testRole));
        
        // When
        Optional<Role> result = roleService.findByName(roleName);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testRole, result.get());
        verify(roleRepository).findByName(roleName);
    }
    
    @Test
    @DisplayName("查找所有启用的角色")
    void findAllEnabledRoles() {
        // Given
        List<Role> roles = Arrays.asList(testRole);
        when(roleRepository.findByEnabledTrueOrderByPriorityDesc()).thenReturn(roles);
        
        // When
        List<Role> result = roleService.findAllEnabledRoles();
        
        // Then
        assertEquals(1, result.size());
        assertEquals(testRole, result.get(0));
        verify(roleRepository).findByEnabledTrueOrderByPriorityDesc();
    }
    
    @Test
    @DisplayName("分页查找所有角色")
    void findAllRoles() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Role> page = new PageImpl<>(Arrays.asList(testRole));
        when(roleRepository.findAll(pageable)).thenReturn(page);
        
        // When
        Page<Role> result = roleService.findAllRoles(pageable);
        
        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(testRole, result.getContent().get(0));
        verify(roleRepository).findAll(pageable);
    }
    
    // ==================== 角色权限管理测试 ====================
    
    @Test
    @DisplayName("为角色分配权限 - 成功")
    void assignPermissionsToRole_Success() {
        // Given
        Long roleId = 1L;
        Set<Long> permissionIds = Set.of(1L, 2L);
        Long assignedBy = 1L;
        
        Permission permission2 = new Permission();
        permission2.setId(2L);
        permission2.setEnabled(true);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findAllById(permissionIds))
                .thenReturn(Arrays.asList(testPermission, permission2));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        
        // When
        roleService.assignPermissionsToRole(roleId, permissionIds, assignedBy);
        
        // Then
        verify(roleRepository).findById(roleId);
        verify(permissionRepository).findAllById(permissionIds);
        verify(roleRepository).save(any(Role.class));
        //verify(auditLogService).logRoleOperation(eq("ROLE_ASSIGN_PERMISSIONS"), eq(roleId),
                                              //  eq(assignedBy), anyString());
    }
    
    @Test
    @DisplayName("为角色添加权限 - 成功")
    void addPermissionToRole_Success() {
        // Given
        Long roleId = 1L;
        Long permissionId = 1L;
        Long assignedBy = 1L;
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        
        // When
        roleService.addPermissionToRole(roleId, permissionId, assignedBy);
        
        // Then
        verify(roleRepository).findById(roleId);
        verify(permissionRepository).findById(permissionId);
        verify(roleRepository).save(any(Role.class));
        //verify(auditLogService).logRoleOperation(eq("ROLE_ADD_PERMISSION"), eq(roleId),
        //                                        eq(assignedBy), anyString());
    }
    
    @Test
    @DisplayName("为角色添加权限 - 权限已禁用")
    void addPermissionToRole_PermissionDisabled() {
        // Given
        Long roleId = 1L;
        Long permissionId = 1L;
        testPermission.setEnabled(false);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            roleService.addPermissionToRole(roleId, permissionId, 1L));
        
        verify(roleRepository, never()).save(any(Role.class));
    }
    
    @Test
    @DisplayName("从角色移除权限 - 成功")
    void removePermissionFromRole_Success() {
        // Given
        Long roleId = 1L;
        Long permissionId = 1L;
        Long removedBy = 1L;
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);
        
        // When
        roleService.removePermissionFromRole(roleId, permissionId, removedBy);
        
        // Then
        verify(roleRepository).findById(roleId);
        verify(permissionRepository).findById(permissionId);
        verify(roleRepository).save(any(Role.class));
        //verify(auditLogService).logRoleOperation(eq("ROLE_REMOVE_PERMISSION"), eq(roleId),
       //                                         eq(removedBy), anyString());
    }
    
    @Test
    @DisplayName("获取角色的所有权限")
    void getRolePermissions() {
        // Given
        Long roleId = 1L;
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findByRoleId(roleId)).thenReturn(permissions);
        
        // When
        List<Permission> result = roleService.getRolePermissions(roleId);
        
        // Then
        assertEquals(1, result.size());
        assertEquals(testPermission, result.get(0));
        verify(permissionRepository).findByRoleId(roleId);
    }
    
    // ==================== 用户角色管理测试 ====================
    
    @Test
    @DisplayName("为用户分配角色 - 成功")
    void assignRolesToUser_Success() {
        // Given
        Long userId = 1L;
        Set<Long> roleIds = Set.of(1L);
        Long assignedBy = 1L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findAllById(roleIds)).thenReturn(Arrays.asList(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        roleService.assignRolesToUser(userId, roleIds, assignedBy);
        
        // Then
        verify(userRepository).findById(userId);
        verify(roleRepository).findAllById(roleIds);
        verify(userRepository).save(any(User.class));
       // verify(auditLogService).logUserOperation(eq("USER_ASSIGN_ROLES"), eq(userId),
         //                                       eq(assignedBy), anyString());
    }
    
    @Test
    @DisplayName("为用户添加角色 - 成功")
    void addRoleToUser_Success() {
        // Given
        Long userId = 1L;
        Long roleId = 1L;
        Long assignedBy = 1L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        roleService.addRoleToUser(userId, roleId, assignedBy);
        
        // Then
        verify(userRepository).findById(userId);
        verify(roleRepository).findById(roleId);
        verify(userRepository).save(any(User.class));
      //  verify(auditLogService).logUserOperation(eq("USER_ADD_ROLE"), eq(userId),
         //                                       eq(assignedBy), anyString());
    }
    
    @Test
    @DisplayName("从用户移除角色 - 成功")
    void removeRoleFromUser_Success() {
        // Given
        Long userId = 1L;
        Long roleId = 1L;
        Long removedBy = 1L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        roleService.removeRoleFromUser(userId, roleId, removedBy);
        
        // Then
        verify(userRepository).findById(userId);
        verify(roleRepository).findById(roleId);
        verify(userRepository).save(any(User.class));
       // verify(auditLogService).logUserOperation(eq("USER_REMOVE_ROLE"), eq(userId),
        //                                        eq(removedBy), anyString());
    }
    
    // ==================== 角色层级管理测试 ====================
    
    @Test
    @DisplayName("检查角色层级关系 - 有层级关系")
    void hasRoleHierarchy_True() {
        // Given
        Long higherRoleId = 1L;
        Long lowerRoleId = 2L;
        
        Role higherRole = new Role();
        higherRole.setId(higherRoleId);
        higherRole.setPriority(100);
        
        Role lowerRole = new Role();
        lowerRole.setId(lowerRoleId);
        lowerRole.setPriority(50);
        
        when(roleRepository.findById(higherRoleId)).thenReturn(Optional.of(higherRole));
        when(roleRepository.findById(lowerRoleId)).thenReturn(Optional.of(lowerRole));
        
        // When
        boolean result = roleService.hasRoleHierarchy(higherRoleId, lowerRoleId);
        
        // Then
        assertTrue(result);
        verify(roleRepository).findById(higherRoleId);
        verify(roleRepository).findById(lowerRoleId);
    }
    
    @Test
    @DisplayName("检查角色层级关系 - 无层级关系")
    void hasRoleHierarchy_False() {
        // Given
        Long higherRoleId = 1L;
        Long lowerRoleId = 2L;
        
        Role higherRole = new Role();
        higherRole.setId(higherRoleId);
        higherRole.setPriority(50);
        
        Role lowerRole = new Role();
        lowerRole.setId(lowerRoleId);
        lowerRole.setPriority(100);
        
        when(roleRepository.findById(higherRoleId)).thenReturn(Optional.of(higherRole));
        when(roleRepository.findById(lowerRoleId)).thenReturn(Optional.of(lowerRole));
        
        // When
        boolean result = roleService.hasRoleHierarchy(higherRoleId, lowerRoleId);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("获取用户最高优先级角色")
    void getUserHighestPriorityRole() {
        // Given
        Long userId = 1L;
        
        Role role1 = new Role();
        role1.setPriority(50);
        
        Role role2 = new Role();
        role2.setPriority(100);
        
        List<Role> userRoles = Arrays.asList(role1, role2);
        when(roleRepository.findByUserId(userId)).thenReturn(userRoles);
        
        // When
        Optional<Role> result = roleService.getUserHighestPriorityRole(userId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(role2, result.get());
        verify(roleRepository).findByUserId(userId);
    }
    
    // ==================== 权限检查测试 ====================
    
    @Test
    @DisplayName("检查用户权限 - 按权限名称")
    void hasPermission_ByName() {
        // Given
        Long userId = 1L;
        String permissionName = "TEST_PERMISSION";
        
        when(permissionRepository.hasPermission(userId, permissionName)).thenReturn(true);
        
        // When
        boolean result = roleService.hasPermission(userId, permissionName);
        
        // Then
        assertTrue(result);
        verify(permissionRepository).hasPermission(userId, permissionName);
    }
    
    @Test
    @DisplayName("检查用户权限 - 按资源和操作")
    void hasPermission_ByResourceAndAction() {
        // Given
        Long userId = 1L;
        String resourceType = "POST";
        String actionType = "READ";
        
        when(permissionRepository.hasPermission(userId, resourceType, actionType)).thenReturn(true);
        
        // When
        boolean result = roleService.hasPermission(userId, resourceType, actionType);
        
        // Then
        assertTrue(result);
        verify(permissionRepository).hasPermission(userId, resourceType, actionType);
    }
    
    @Test
    @DisplayName("获取用户权限列表")
    void getUserPermissions() {
        // Given
        Long userId = 1L;
        List<String> permissions = Arrays.asList("POST_READ", "POST_CREATE");
        
        when(permissionRepository.findPermissionNamesByUserId(userId)).thenReturn(permissions);
        
        // When
        List<String> result = roleService.getUserPermissions(userId);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("POST_READ"));
        assertTrue(result.contains("POST_CREATE"));
        verify(permissionRepository).findPermissionNamesByUserId(userId);
    }
    
    @Test
    @DisplayName("获取用户角色列表")
    void getUserRoles() {
        // Given
        Long userId = 1L;
        List<String> roles = Arrays.asList("USER", "MODERATOR");
        
        when(roleRepository.findRoleNamesByUserId(userId)).thenReturn(roles);
        
        // When
        List<String> result = roleService.getUserRoles(userId);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("USER"));
        assertTrue(result.contains("MODERATOR"));
        verify(roleRepository).findRoleNamesByUserId(userId);
    }
}