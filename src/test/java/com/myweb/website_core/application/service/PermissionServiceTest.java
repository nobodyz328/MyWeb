package com.myweb.website_core.application.service;

import com.myweb.website_core.domain.security.entity.Permission;
import com.myweb.website_core.infrastructure.persistence.repository.PermissionRepository;
import com.myweb.website_core.infrastructure.persistence.repository.RoleRepository;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.authorization.PermissionService;
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
 * 权限管理服务测试类
 * 
 * 测试PermissionService的各项功能，包括：
 * - 权限CRUD操作
 * - 权限分组管理
 * - 权限级别管理
 * - 权限验证
 * - 批量操作
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("权限管理服务测试")
class PermissionServiceTest {
    
    @Mock
    private PermissionRepository permissionRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private PermissionService permissionService;
    
    private Permission testPermission;
    
    @BeforeEach
    void setUp() {
        // 创建测试权限
        testPermission = Permission.builder()
                .name("POST_READ")
                .displayName("查看帖子")
                .description("允许用户查看帖子内容")
                .resourceType("POST")
                .actionType("READ")
                .permissionGroup("内容管理")
                .permissionLevel(1)
                .systemPermission(false)
                .build();
        
        testPermission.setId(1L);
        testPermission.setEnabled(true);
        testPermission.setCreatedAt(LocalDateTime.now());
        testPermission.setUpdatedAt(LocalDateTime.now());
    }
    
    // ==================== 权限CRUD操作测试 ====================
    
    @Test
    @DisplayName("创建权限 - 成功")
    void createPermission_Success() {
        // Given
        String name = "POST_CREATE";
        String displayName = "创建帖子";
        String description = "允许用户创建新帖子";
        String resourceType = "POST";
        String actionType = "CREATE";
        String permissionGroup = "内容管理";
        Integer permissionLevel = 2;
        Long createdBy = 1L;
        
        when(permissionRepository.existsByName(name)).thenReturn(false);
        when(permissionRepository.existsByResourceTypeAndActionType(resourceType, actionType)).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);
        
        // When
        Permission result = permissionService.createPermission(name, displayName, description,
                resourceType, actionType, permissionGroup, permissionLevel, createdBy);
        
        // Then
        assertNotNull(result);
        verify(permissionRepository).existsByName(name);
        verify(permissionRepository).existsByResourceTypeAndActionType(resourceType, actionType);
        verify(permissionRepository).save(any(Permission.class));
        verify(auditLogService).logPermissionOperation(eq("PERMISSION_CREATE"), any(), eq(createdBy), anyString());
    }
    
    @Test
    @DisplayName("创建权限 - 名称已存在")
    void createPermission_NameExists() {
        // Given
        String name = "EXISTING_PERMISSION";
        when(permissionRepository.existsByName(name)).thenReturn(true);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            permissionService.createPermission(name, "显示名", "描述", "RESOURCE", "ACTION", "分组", 1, 1L));
        
        verify(permissionRepository).existsByName(name);
        verify(permissionRepository, never()).save(any(Permission.class));
    }
    
    @Test
    @DisplayName("创建权限 - 资源类型和操作类型组合已存在")
    void createPermission_ResourceActionExists() {
        // Given
        String name = "NEW_PERMISSION";
        String resourceType = "POST";
        String actionType = "READ";
        
        when(permissionRepository.existsByName(name)).thenReturn(false);
        when(permissionRepository.existsByResourceTypeAndActionType(resourceType, actionType)).thenReturn(true);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            permissionService.createPermission(name, "显示名", "描述", resourceType, actionType, "分组", 1, 1L));
        
        verify(permissionRepository).existsByResourceTypeAndActionType(resourceType, actionType);
        verify(permissionRepository, never()).save(any(Permission.class));
    }
    
    @Test
    @DisplayName("更新权限 - 成功")
    void updatePermission_Success() {
        // Given
        Long permissionId = 1L;
        String newDisplayName = "更新后的权限";
        String newDescription = "更新后的描述";
        String newPermissionGroup = "新分组";
        Integer newPermissionLevel = 3;
        Boolean enabled = true;
        Long updatedBy = 1L;
        
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);
        
        // When
        Permission result = permissionService.updatePermission(permissionId, newDisplayName, newDescription,
                newPermissionGroup, newPermissionLevel, enabled, updatedBy);
        
        // Then
        assertNotNull(result);
        verify(permissionRepository).findById(permissionId);
        verify(permissionRepository).save(any(Permission.class));
        verify(auditLogService).logPermissionOperation(eq("PERMISSION_UPDATE"), eq(permissionId), eq(updatedBy), anyString());
    }
    
    @Test
    @DisplayName("更新权限 - 权限不存在")
    void updatePermission_NotFound() {
        // Given
        Long permissionId = 999L;
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            permissionService.updatePermission(permissionId, "新名称", "新描述", "新分组", 1, true, 1L));
        
        verify(permissionRepository).findById(permissionId);
        verify(permissionRepository, never()).save(any(Permission.class));
    }
    
    @Test
    @DisplayName("更新系统权限 - 限制修改")
    void updateSystemPermission_Restricted() {
        // Given
        testPermission.setSystemPermission(true);
        Long permissionId = 1L;
        
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);
        
        // When
        Permission result = permissionService.updatePermission(permissionId, "新显示名", "新描述", 
                "新分组", 5, false, 1L);
        
        // Then
        assertNotNull(result);
        // 系统权限的enabled状态不应该被修改
        assertTrue(result.getEnabled());
        verify(permissionRepository).save(any(Permission.class));
    }
    
    @Test
    @DisplayName("删除权限 - 成功")
    void deletePermission_Success() {
        // Given
        Long permissionId = 1L;
        Long deletedBy = 1L;
        
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.countRolesByPermissionId(permissionId)).thenReturn(0L);
        
        // When
        permissionService.deletePermission(permissionId, deletedBy);
        
        // Then
        verify(permissionRepository).findById(permissionId);
        verify(permissionRepository).countRolesByPermissionId(permissionId);
        verify(permissionRepository).delete(testPermission);
        verify(auditLogService).logPermissionOperation(eq("PERMISSION_DELETE"), eq(permissionId), eq(deletedBy), anyString());
    }
    
    @Test
    @DisplayName("删除权限 - 系统权限不允许删除")
    void deletePermission_SystemPermission() {
        // Given
        testPermission.setSystemPermission(true);
        Long permissionId = 1L;
        
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            permissionService.deletePermission(permissionId, 1L));
        
        verify(permissionRepository).findById(permissionId);
        verify(permissionRepository, never()).delete(any(Permission.class));
    }
    
    @Test
    @DisplayName("删除权限 - 仍有角色使用")
    void deletePermission_HasRoles() {
        // Given
        Long permissionId = 1L;
        
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.countRolesByPermissionId(permissionId)).thenReturn(3L);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            permissionService.deletePermission(permissionId, 1L));
        
        verify(permissionRepository).findById(permissionId);
        verify(permissionRepository).countRolesByPermissionId(permissionId);
        verify(permissionRepository, never()).delete(any(Permission.class));
    }
    
    // ==================== 权限查询操作测试 ====================
    
    @Test
    @DisplayName("根据ID查找权限")
    void findById() {
        // Given
        Long permissionId = 1L;
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        
        // When
        Optional<Permission> result = permissionService.findById(permissionId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testPermission, result.get());
        verify(permissionRepository).findById(permissionId);
    }
    
    @Test
    @DisplayName("根据名称查找权限")
    void findByName() {
        // Given
        String permissionName = "POST_READ";
        when(permissionRepository.findByName(permissionName)).thenReturn(Optional.of(testPermission));
        
        // When
        Optional<Permission> result = permissionService.findByName(permissionName);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testPermission, result.get());
        verify(permissionRepository).findByName(permissionName);
    }
    
    @Test
    @DisplayName("根据资源类型和操作类型查找权限")
    void findByResourceAndAction() {
        // Given
        String resourceType = "POST";
        String actionType = "READ";
        when(permissionRepository.findByResourceTypeAndActionType(resourceType, actionType))
                .thenReturn(Optional.of(testPermission));
        
        // When
        Optional<Permission> result = permissionService.findByResourceAndAction(resourceType, actionType);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testPermission, result.get());
        verify(permissionRepository).findByResourceTypeAndActionType(resourceType, actionType);
    }
    
    @Test
    @DisplayName("查找所有启用的权限")
    void findAllEnabledPermissions() {
        // Given
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findByEnabledTrueOrderByPermissionGroupAscPermissionLevelAsc())
                .thenReturn(permissions);
        
        // When
        List<Permission> result = permissionService.findAllEnabledPermissions();
        
        // Then
        assertEquals(1, result.size());
        assertEquals(testPermission, result.get(0));
        verify(permissionRepository).findByEnabledTrueOrderByPermissionGroupAscPermissionLevelAsc();
    }
    
    @Test
    @DisplayName("分页查找所有权限")
    void findAllPermissions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Permission> page = new PageImpl<>(Arrays.asList(testPermission));
        when(permissionRepository.findAll(pageable)).thenReturn(page);
        
        // When
        Page<Permission> result = permissionService.findAllPermissions(pageable);
        
        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(testPermission, result.getContent().get(0));
        verify(permissionRepository).findAll(pageable);
    }
    
    @Test
    @DisplayName("根据资源类型查找权限")
    void findByResourceType() {
        // Given
        String resourceType = "POST";
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findByResourceTypeAndEnabledTrueOrderByPermissionLevelAsc(resourceType))
                .thenReturn(permissions);
        
        // When
        List<Permission> result = permissionService.findByResourceType(resourceType);
        
        // Then
        assertEquals(1, result.size());
        assertEquals(testPermission, result.get(0));
        verify(permissionRepository).findByResourceTypeAndEnabledTrueOrderByPermissionLevelAsc(resourceType);
    }
    
    @Test
    @DisplayName("根据权限分组查找权限")
    void findByPermissionGroup() {
        // Given
        String permissionGroup = "内容管理";
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findByPermissionGroupAndEnabledTrueOrderByPermissionLevelAsc(permissionGroup))
                .thenReturn(permissions);
        
        // When
        List<Permission> result = permissionService.findByPermissionGroup(permissionGroup);
        
        // Then
        assertEquals(1, result.size());
        assertEquals(testPermission, result.get(0));
        verify(permissionRepository).findByPermissionGroupAndEnabledTrueOrderByPermissionLevelAsc(permissionGroup);
    }
    
    // ==================== 权限分组管理测试 ====================
    
    @Test
    @DisplayName("获取所有权限分组")
    void getAllPermissionGroups() {
        // Given
        List<String> groups = Arrays.asList("内容管理", "用户管理", "系统管理");
        when(permissionRepository.findAllPermissionGroups()).thenReturn(groups);
        
        // When
        List<String> result = permissionService.getAllPermissionGroups();
        
        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("内容管理"));
        verify(permissionRepository).findAllPermissionGroups();
    }
    
    @Test
    @DisplayName("获取指定资源类型的所有操作类型")
    void getActionTypesByResourceType() {
        // Given
        String resourceType = "POST";
        List<String> actionTypes = Arrays.asList("CREATE", "READ", "UPDATE", "DELETE");
        when(permissionRepository.findActionTypesByResourceType(resourceType)).thenReturn(actionTypes);
        
        // When
        List<String> result = permissionService.getActionTypesByResourceType(resourceType);
        
        // Then
        assertEquals(4, result.size());
        assertTrue(result.contains("READ"));
        verify(permissionRepository).findActionTypesByResourceType(resourceType);
    }
    
    @Test
    @DisplayName("获取所有资源类型")
    void getAllResourceTypes() {
        // Given
        List<String> resourceTypes = Arrays.asList("POST", "USER", "COMMENT", "SYSTEM");
        when(permissionRepository.findAllResourceTypes()).thenReturn(resourceTypes);
        
        // When
        List<String> result = permissionService.getAllResourceTypes();
        
        // Then
        assertEquals(4, result.size());
        assertTrue(result.contains("POST"));
        verify(permissionRepository).findAllResourceTypes();
    }
    
    @Test
    @DisplayName("按分组组织权限")
    void getPermissionsByGroup() {
        // Given
        Permission permission1 = Permission.builder()
                .permissionGroup("内容管理")
                .build();
        Permission permission2 = Permission.builder()
                .permissionGroup("用户管理")
                .build();
        Permission permission3 = Permission.builder()
                .permissionGroup(null) // 未分组
                .build();
        
        List<Permission> permissions = Arrays.asList(permission1, permission2, permission3);
        when(permissionRepository.findByEnabledTrueOrderByPermissionGroupAscPermissionLevelAsc())
                .thenReturn(permissions);
        
        // When
        Map<String, List<Permission>> result = permissionService.getPermissionsByGroup();
        
        // Then
        assertEquals(3, result.size());
        assertTrue(result.containsKey("内容管理"));
        assertTrue(result.containsKey("用户管理"));
        assertTrue(result.containsKey("未分组"));
        assertEquals(1, result.get("内容管理").size());
        assertEquals(1, result.get("用户管理").size());
        assertEquals(1, result.get("未分组").size());
    }
    
    @Test
    @DisplayName("按资源类型组织权限")
    void getPermissionsByResourceType() {
        // Given
        Permission permission1 = Permission.builder()
                .resourceType("POST")
                .build();
        Permission permission2 = Permission.builder()
                .resourceType("USER")
                .build();
        
        List<Permission> permissions = Arrays.asList(permission1, permission2);
        when(permissionRepository.findByEnabledTrueOrderByPermissionGroupAscPermissionLevelAsc())
                .thenReturn(permissions);
        
        // When
        Map<String, List<Permission>> result = permissionService.getPermissionsByResourceType();
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.containsKey("POST"));
        assertTrue(result.containsKey("USER"));
        assertEquals(1, result.get("POST").size());
        assertEquals(1, result.get("USER").size());
    }
    
    // ==================== 权限级别管理测试 ====================
    
    @Test
    @DisplayName("根据权限级别范围查找权限")
    void findByPermissionLevelRange() {
        // Given
        Integer minLevel = 1;
        Integer maxLevel = 3;
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findByPermissionLevelBetweenAndEnabledTrueOrderByPermissionLevelAsc(minLevel, maxLevel))
                .thenReturn(permissions);
        
        // When
        List<Permission> result = permissionService.findByPermissionLevelRange(minLevel, maxLevel);
        
        // Then
        assertEquals(1, result.size());
        assertEquals(testPermission, result.get(0));
        verify(permissionRepository).findByPermissionLevelBetweenAndEnabledTrueOrderByPermissionLevelAsc(minLevel, maxLevel);
    }
    
    @Test
    @DisplayName("检查权限级别是否足够 - 足够")
    void hasRequiredPermissionLevel_Sufficient() {
        // Given
        Permission userPermission = Permission.builder()
                .resourceType("POST")
                .permissionLevel(3)
                .build();
        
        Permission requiredPermission = Permission.builder()
                .resourceType("POST")
                .permissionLevel(2)
                .build();
        
        List<Permission> userPermissions = Arrays.asList(userPermission);
        
        // When
        boolean result = permissionService.hasRequiredPermissionLevel(userPermissions, requiredPermission);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("检查权限级别是否足够 - 不足")
    void hasRequiredPermissionLevel_Insufficient() {
        // Given
        Permission userPermission = Permission.builder()
                .resourceType("POST")
                .permissionLevel(1)
                .build();
        
        Permission requiredPermission = Permission.builder()
                .resourceType("POST")
                .permissionLevel(3)
                .build();
        
        List<Permission> userPermissions = Arrays.asList(userPermission);
        
        // When
        boolean result = permissionService.hasRequiredPermissionLevel(userPermissions, requiredPermission);
        
        // Then
        assertFalse(result);
    }
    
    // ==================== 权限验证方法测试 ====================
    
    @Test
    @DisplayName("验证权限名称格式 - 有效格式")
    void isValidPermissionName_Valid() {
        // Given
        String validName = "POST_READ";
        
        // When
        boolean result = permissionService.isValidPermissionName(validName);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("验证权限名称格式 - 无效格式")
    void isValidPermissionName_Invalid() {
        // Given
        String[] invalidNames = {"post_read", "POST-READ", "POST READ", "POST_", "_READ", ""};
        
        // When & Then
        for (String invalidName : invalidNames) {
            boolean result = permissionService.isValidPermissionName(invalidName);
            assertFalse(result, "权限名称应该无效: " + invalidName);
        }
    }
    
    @Test
    @DisplayName("根据资源类型和操作类型生成权限名称")
    void generatePermissionName() {
        // Given
        String resourceType = "post";
        String actionType = "read";
        
        // When
        String result = permissionService.generatePermissionName(resourceType, actionType);
        
        // Then
        assertEquals("POST_READ", result);
    }
    
    @Test
    @DisplayName("生成权限名称 - 参数为空")
    void generatePermissionName_NullParameters() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            permissionService.generatePermissionName(null, "READ"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            permissionService.generatePermissionName("POST", null));
    }
    
    @Test
    @DisplayName("检查权限冲突 - 有冲突")
    void hasPermissionConflict_True() {
        // Given
        Permission permission1 = Permission.builder()
                .resourceType("POST")
                .actionType("READ")
                .build();
        permission1.setId(1L);
        
        Permission permission2 = Permission.builder()
                .resourceType("POST")
                .actionType("READ")
                .build();
        permission2.setId(2L);
        
        // When
        boolean result = permissionService.hasPermissionConflict(permission1, permission2);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("检查权限冲突 - 无冲突")
    void hasPermissionConflict_False() {
        // Given
        Permission permission1 = Permission.builder()
                .resourceType("POST")
                .actionType("READ")
                .build();
        permission1.setId(1L);
        
        Permission permission2 = Permission.builder()
                .resourceType("POST")
                .actionType("CREATE")
                .build();
        permission2.setId(2L);
        
        // When
        boolean result = permissionService.hasPermissionConflict(permission1, permission2);
        
        // Then
        assertFalse(result);
    }
    
    // ==================== 批量操作方法测试 ====================
    
    @Test
    @DisplayName("批量创建权限 - 成功")
    void batchCreatePermissions_Success() {
        // Given
        List<PermissionService.PermissionCreateData> permissionData = Arrays.asList(
                new PermissionService.PermissionCreateData("POST_CREATE", "创建帖子", "允许创建帖子",
                        "POST", "CREATE", "内容管理", 2),
                new PermissionService.PermissionCreateData("POST_UPDATE", "编辑帖子", "允许编辑帖子",
                        "POST", "UPDATE", "内容管理", 3)
        );
        Long createdBy = 1L;
        
        when(permissionRepository.existsByName(anyString())).thenReturn(false);
        when(permissionRepository.existsByResourceTypeAndActionType(anyString(), anyString())).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);
        
        // When
        List<Permission> result = permissionService.batchCreatePermissions(permissionData, createdBy);
        
        // Then
        assertEquals(2, result.size());
        verify(permissionRepository, times(2)).save(any(Permission.class));
        verify(auditLogService, times(2)).logPermissionOperation(eq("PERMISSION_CREATE"), any(), eq(createdBy), anyString());
    }
    
    @Test
    @DisplayName("批量更新权限状态 - 成功")
    void batchUpdatePermissionStatus_Success() {
        // Given
        List<Long> permissionIds = Arrays.asList(1L, 2L);
        boolean enabled = false;
        Long updatedBy = 1L;
        
        Permission permission1 = Permission.builder().systemPermission(false).build();
        permission1.setId(1L);
        Permission permission2 = Permission.builder().systemPermission(false).build();
        permission2.setId(2L);
        
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission2));
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);
        
        // When
        int result = permissionService.batchUpdatePermissionStatus(permissionIds, enabled, updatedBy);
        
        // Then
        assertEquals(2, result);
        verify(permissionRepository, times(2)).save(any(Permission.class));
        verify(auditLogService).logPermissionOperation(eq("PERMISSION_BATCH_UPDATE"), isNull(), eq(updatedBy), anyString());
    }
    
    @Test
    @DisplayName("批量更新权限状态 - 跳过系统权限")
    void batchUpdatePermissionStatus_SkipSystemPermission() {
        // Given
        List<Long> permissionIds = Arrays.asList(1L);
        boolean enabled = false; // 尝试禁用
        Long updatedBy = 1L;
        
        Permission systemPermission = Permission.builder().systemPermission(true).build();
        systemPermission.setId(1L);
        
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(systemPermission));
        
        // When
        int result = permissionService.batchUpdatePermissionStatus(permissionIds, enabled, updatedBy);
        
        // Then
        assertEquals(0, result); // 系统权限被跳过，更新数量为0
        verify(permissionRepository, never()).save(any(Permission.class));
    }
}