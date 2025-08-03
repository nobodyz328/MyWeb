package com.myweb.website_core.application.service.security.authorization;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.domain.security.entity.Permission;
import com.myweb.website_core.infrastructure.persistence.repository.PermissionRepository;
import com.myweb.website_core.infrastructure.persistence.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限管理服务
 * 
 * 提供权限的创建、修改、删除和查询功能
 * 实现基于角色的访问控制(RBAC)模型中的权限管理
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * 主要功能：
 * - 权限的CRUD操作
 * - 权限分组管理
 * - 权限级别管理
 * - 权限分配和回收
 * - 默认权限初始化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    
    // ==================== 权限CRUD操作 ====================
    
    /**
     * 创建新权限
     * 
     * @param name 权限名称
     * @param displayName 显示名称
     * @param description 权限描述
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @param permissionGroup 权限分组
     * @param permissionLevel 权限级别
     * @param createdBy 创建者ID
     * @return 创建的权限
     * @throws IllegalArgumentException 如果权限名称已存在或资源类型+操作类型组合已存在
     */
    @Transactional
    public Permission createPermission(String name, String displayName, String description,
                                     String resourceType, String actionType, String permissionGroup,
                                     Integer permissionLevel, Long createdBy) {
        log.info("创建权限: name={}, resourceType={}, actionType={}, level={}", 
                name, resourceType, actionType, permissionLevel);
        
        // 验证权限名称唯一性
        if (permissionRepository.existsByName(name)) {
            throw new IllegalArgumentException("权限名称已存在: " + name);
        }
        
        // 验证资源类型和操作类型组合唯一性
        if (permissionRepository.existsByResourceTypeAndActionType(resourceType, actionType)) {
            throw new IllegalArgumentException("资源类型和操作类型组合已存在: " + resourceType + ":" + actionType);
        }
        
        // 创建权限对象
        Permission permission = Permission.builder()
                .name(name)
                .displayName(displayName)
                .description(description)
                .resourceType(resourceType)
                .actionType(actionType)
                .permissionGroup(permissionGroup)
                .permissionLevel(permissionLevel != null ? permissionLevel : 1)
                .systemPermission(false)
                .build();
        
        permission.setEnabled(true);
        permission.setCreatedBy(createdBy);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        
        Permission savedPermission = permissionRepository.save(permission);
        
        // 记录审计日志
        auditLogService.logPermissionOperation("PERMISSION_CREATE", savedPermission.getId(), 
                                              createdBy, "创建权限: " + name);
        
        log.info("权限创建成功: id={}, name={}", savedPermission.getId(), savedPermission.getName());
        return savedPermission;
    }
    
    /**
     * 更新权限信息
     * 
     * @param permissionId 权限ID
     * @param displayName 显示名称
     * @param description 权限描述
     * @param permissionGroup 权限分组
     * @param permissionLevel 权限级别
     * @param enabled 是否启用
     * @param updatedBy 更新者ID
     * @return 更新后的权限
     * @throws IllegalArgumentException 如果权限不存在或为系统权限
     */
    @Transactional
    public Permission updatePermission(Long permissionId, String displayName, String description,
                                     String permissionGroup, Integer permissionLevel, 
                                     Boolean enabled, Long updatedBy) {
        log.info("更新权限: permissionId={}, displayName={}, level={}, enabled={}", 
                permissionId, displayName, permissionLevel, enabled);
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("权限不存在: " + permissionId));
        
        // 系统权限不允许修改某些属性
        if (permission.isSystemPermission()) {
            log.warn("尝试修改系统权限: permissionId={}, name={}", permissionId, permission.getName());
            // 系统权限只允许修改描述和权限级别
            if (description != null) {
                permission.setDescription(description);
            }
            if (permissionLevel != null) {
                permission.setPermissionLevel(permissionLevel);
            }
        } else {
            // 非系统权限可以修改所有属性
            if (displayName != null) {
                permission.setDisplayName(displayName);
            }
            if (description != null) {
                permission.setDescription(description);
            }
            if (permissionGroup != null) {
                permission.setPermissionGroup(permissionGroup);
            }
            if (permissionLevel != null) {
                permission.setPermissionLevel(permissionLevel);
            }
            if (enabled != null) {
                permission.setEnabled(enabled);
            }
        }
        
        permission.setUpdatedBy(updatedBy);
        permission.setUpdatedAt(LocalDateTime.now());
        
        Permission updatedPermission = permissionRepository.save(permission);
        
        // 记录审计日志
        auditLogService.logPermissionOperation("PERMISSION_UPDATE", permissionId, 
                                              updatedBy, "更新权限: " + permission.getName());
        
        log.info("权限更新成功: id={}, name={}", updatedPermission.getId(), updatedPermission.getName());
        return updatedPermission;
    }
    
    /**
     * 删除权限
     * 
     * @param permissionId 权限ID
     * @param deletedBy 删除者ID
     * @throws IllegalArgumentException 如果权限不存在、为系统权限或仍有角色使用
     */
    @Transactional
    public void deletePermission(Long permissionId, Long deletedBy) {
        log.info("删除权限: permissionId={}", permissionId);
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("权限不存在: " + permissionId));
        
        // 系统权限不允许删除
        if (permission.isSystemPermission()) {
            throw new IllegalArgumentException("系统权限不允许删除: " + permission.getName());
        }
        
        // 检查是否有角色使用此权限
        Long roleCount = permissionRepository.countRolesByPermissionId(permissionId);
        if (roleCount > 0) {
            throw new IllegalArgumentException("权限仍有角色使用，无法删除: " + permission.getName());
        }
        
        // 删除权限
        permissionRepository.delete(permission);
        
        // 记录审计日志
        auditLogService.logPermissionOperation("PERMISSION_DELETE", permissionId, 
                                              deletedBy, "删除权限: " + permission.getName());
        
        log.info("权限删除成功: id={}, name={}", permissionId, permission.getName());
    }
    
    // ==================== 权限查询操作 ====================
    
    /**
     * 根据ID查找权限
     * 
     * @param permissionId 权限ID
     * @return 权限对象
     */
    public Optional<Permission> findById(Long permissionId) {
        return permissionRepository.findById(permissionId);
    }
    
    /**
     * 根据名称查找权限
     * 
     * @param name 权限名称
     * @return 权限对象
     */
    public Optional<Permission> findByName(String name) {
        return permissionRepository.findByName(name);
    }
    
    /**
     * 根据资源类型和操作类型查找权限
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 权限对象
     */
    public Optional<Permission> findByResourceAndAction(String resourceType, String actionType) {
        return permissionRepository.findByResourceTypeAndActionType(resourceType, actionType);
    }
    
    /**
     * 查找所有启用的权限
     * 
     * @return 权限列表，按分组和级别排序
     */
    public List<Permission> findAllEnabledPermissions() {
        return permissionRepository.findByEnabledTrueOrderByPermissionGroupAscPermissionLevelAsc();
    }
    
    /**
     * 查找所有权限（分页）
     * 
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    public Page<Permission> findAllPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable);
    }
    
    /**
     * 根据资源类型查找权限
     * 
     * @param resourceType 资源类型
     * @return 权限列表
     */
    public List<Permission> findByResourceType(String resourceType) {
        return permissionRepository.findByResourceTypeAndEnabledTrueOrderByPermissionLevelAsc(resourceType);
    }
    
    /**
     * 根据权限分组查找权限
     * 
     * @param permissionGroup 权限分组
     * @return 权限列表
     */
    public List<Permission> findByPermissionGroup(String permissionGroup) {
        return permissionRepository.findByPermissionGroupAndEnabledTrueOrderByPermissionLevelAsc(permissionGroup);
    }
    
    /**
     * 查找系统权限
     * 
     * @return 系统权限列表
     */
    public List<Permission> findSystemPermissions() {
        return permissionRepository.findBySystemPermissionTrueOrderByPermissionGroupAscPermissionLevelAsc();
    }
    
    /**
     * 查找自定义权限
     * 
     * @return 自定义权限列表
     */
    public List<Permission> findCustomPermissions() {
        return permissionRepository.findBySystemPermissionFalseOrderByPermissionGroupAscPermissionLevelAsc();
    }
    
    /**
     * 根据角色ID查找权限
     * 
     * @param roleId 角色ID
     * @return 角色拥有的权限列表
     */
    public List<Permission> findPermissionsByRoleId(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }
    
    /**
     * 根据用户ID查找权限
     * 
     * @param userId 用户ID
     * @return 用户拥有的权限列表
     */
    public List<Permission> findPermissionsByUserId(Long userId) {
        return permissionRepository.findByUserId(userId);
    }
    
    // ==================== 权限分组管理 ====================
    
    /**
     * 获取所有权限分组
     * 
     * @return 权限分组列表
     */
    public List<String> getAllPermissionGroups() {
        return permissionRepository.findAllPermissionGroups();
    }
    
    /**
     * 获取指定资源类型的所有操作类型
     * 
     * @param resourceType 资源类型
     * @return 操作类型列表
     */
    public List<String> getActionTypesByResourceType(String resourceType) {
        return permissionRepository.findActionTypesByResourceType(resourceType);
    }
    
    /**
     * 获取所有资源类型
     * 
     * @return 资源类型列表
     */
    public List<String> getAllResourceTypes() {
        return permissionRepository.findAllResourceTypes();
    }
    
    /**
     * 按分组组织权限
     * 
     * @return 按分组组织的权限映射
     */
    public Map<String, List<Permission>> getPermissionsByGroup() {
        List<Permission> allPermissions = findAllEnabledPermissions();
        
        return allPermissions.stream()
                .collect(Collectors.groupingBy(
                    permission -> permission.getPermissionGroup() != null ? 
                                 permission.getPermissionGroup() : "未分组",
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
    }
    
    /**
     * 按资源类型组织权限
     * 
     * @return 按资源类型组织的权限映射
     */
    public Map<String, List<Permission>> getPermissionsByResourceType() {
        List<Permission> allPermissions = findAllEnabledPermissions();
        
        return allPermissions.stream()
                .collect(Collectors.groupingBy(
                    Permission::getResourceType,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
    }
    
    // ==================== 权限级别管理 ====================
    
    /**
     * 根据权限级别范围查找权限
     * 
     * @param minLevel 最小权限级别
     * @param maxLevel 最大权限级别
     * @return 权限列表
     */
    public List<Permission> findByPermissionLevelRange(Integer minLevel, Integer maxLevel) {
        return permissionRepository.findByPermissionLevelBetweenAndEnabledTrueOrderByPermissionLevelAsc(minLevel, maxLevel);
    }
    
    /**
     * 检查权限级别是否足够
     * 
     * @param userPermissions 用户权限列表
     * @param requiredPermission 需要的权限
     * @return 是否有足够的权限级别
     */
    public boolean hasRequiredPermissionLevel(List<Permission> userPermissions, Permission requiredPermission) {
        if (requiredPermission == null) {
            return false;
        }
        
        return userPermissions.stream()
                .filter(p -> p.getResourceType().equals(requiredPermission.getResourceType()))
                .anyMatch(p -> p.hasHigherLevelThan(requiredPermission) || p.equals(requiredPermission));
    }
    
    // ==================== 权限验证方法 ====================
    
    /**
     * 验证权限名称格式
     * 
     * @param name 权限名称
     * @return 是否符合格式要求
     */
    public boolean isValidPermissionName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // 权限名称格式：RESOURCE_ACTION，全大写，下划线分隔
        return name.matches("^[A-Z]+_[A-Z]+$");
    }
    
    /**
     * 根据资源类型和操作类型生成权限名称
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 权限名称
     */
    public String generatePermissionName(String resourceType, String actionType) {
        if (resourceType == null || actionType == null) {
            throw new IllegalArgumentException("资源类型和操作类型不能为空");
        }
        
        return resourceType.toUpperCase() + "_" + actionType.toUpperCase();
    }
    
    /**
     * 检查权限是否冲突
     * 
     * @param permission1 权限1
     * @param permission2 权限2
     * @return 是否冲突
     */
    public boolean hasPermissionConflict(Permission permission1, Permission permission2) {
        if (permission1 == null || permission2 == null) {
            return false;
        }
        
        // 相同资源类型和操作类型的权限冲突
        return permission1.getResourceType().equals(permission2.getResourceType()) &&
               permission1.getActionType().equals(permission2.getActionType()) &&
               !permission1.getId().equals(permission2.getId());
    }
    
    // ==================== 批量操作方法 ====================
    
    /**
     * 批量创建权限
     * 
     * @param permissionData 权限数据列表
     * @param createdBy 创建者ID
     * @return 创建的权限列表
     */
    @Transactional
    public List<Permission> batchCreatePermissions(List<PermissionCreateData> permissionData, Long createdBy) {
        log.info("批量创建权限: count={}", permissionData.size());
        
        List<Permission> createdPermissions = new ArrayList<>();
        
        for (PermissionCreateData data : permissionData) {
            try {
                Permission permission = createPermission(
                    data.getName(),
                    data.getDisplayName(),
                    data.getDescription(),
                    data.getResourceType(),
                    data.getActionType(),
                    data.getPermissionGroup(),
                    data.getPermissionLevel(),
                    createdBy
                );
                createdPermissions.add(permission);
            } catch (Exception e) {
                log.warn("批量创建权限失败: name={}, error={}", data.getName(), e.getMessage());
                // 继续处理其他权限，不中断整个批量操作
            }
        }
        
        log.info("批量创建权限完成: 成功={}, 总数={}", createdPermissions.size(), permissionData.size());
        return createdPermissions;
    }
    
    /**
     * 批量启用/禁用权限
     * 
     * @param permissionIds 权限ID列表
     * @param enabled 是否启用
     * @param updatedBy 更新者ID
     * @return 更新的权限数量
     */
    @Transactional
    public int batchUpdatePermissionStatus(List<Long> permissionIds, boolean enabled, Long updatedBy) {
        log.info("批量更新权限状态: count={}, enabled={}", permissionIds.size(), enabled);
        
        int updatedCount = 0;
        
        for (Long permissionId : permissionIds) {
            try {
                Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
                if (permissionOpt.isPresent()) {
                    Permission permission = permissionOpt.get();
                    
                    // 系统权限不允许禁用
                    if (permission.isSystemPermission() && !enabled) {
                        log.warn("尝试禁用系统权限: permissionId={}, name={}", permissionId, permission.getName());
                        continue;
                    }
                    
                    permission.setEnabled(enabled);
                    permission.setUpdatedBy(updatedBy);
                    permission.setUpdatedAt(LocalDateTime.now());
                    permissionRepository.save(permission);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.warn("批量更新权限状态失败: permissionId={}, error={}", permissionId, e.getMessage());
            }
        }
        
        // 记录审计日志
        auditLogService.logPermissionOperation("PERMISSION_BATCH_UPDATE", null, 
                                              updatedBy, "批量更新权限状态: " + updatedCount + "个");
        
        log.info("批量更新权限状态完成: 成功={}, 总数={}", updatedCount, permissionIds.size());
        return updatedCount;
    }
    
    // ==================== 内部数据类 ====================
    
    /**
     * 权限创建数据类
     */
    public static class PermissionCreateData {
        private String name;
        private String displayName;
        private String description;
        private String resourceType;
        private String actionType;
        private String permissionGroup;
        private Integer permissionLevel;
        
        // 构造函数、getter和setter方法
        public PermissionCreateData(String name, String displayName, String description,
                                  String resourceType, String actionType, String permissionGroup,
                                  Integer permissionLevel) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.resourceType = resourceType;
            this.actionType = actionType;
            this.permissionGroup = permissionGroup;
            this.permissionLevel = permissionLevel;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getResourceType() { return resourceType; }
        public String getActionType() { return actionType; }
        public String getPermissionGroup() { return permissionGroup; }
        public Integer getPermissionLevel() { return permissionLevel; }
    }
}