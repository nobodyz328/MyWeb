package com.myweb.website_core.application.service.security.authorization;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.domain.security.entity.Permission;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.PermissionRepository;
import com.myweb.website_core.infrastructure.persistence.repository.RoleRepository;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 角色管理服务
 * 
 * 提供角色的创建、修改、删除和查询功能
 * 实现基于角色的访问控制(RBAC)模型
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * 主要功能：
 * - 角色的CRUD操作
 * - 角色权限管理
 * - 用户角色分配
 * - 角色层级管理
 * - 默认角色初始化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    
    // ==================== 角色CRUD操作 ====================
    
    /**
     * 创建新角色
     * 
     * @param name 角色名称
     * @param displayName 显示名称
     * @param description 角色描述
     * @param priority 角色优先级
     * @param createdBy 创建者ID
     * @return 创建的角色
     * @throws IllegalArgumentException 如果角色名称已存在
     */
    @Transactional
    public Role createRole(String name, String displayName, String description, 
                          Integer priority, Long createdBy) {
        log.info("创建角色: name={}, displayName={}, priority={}", name, displayName, priority);
        
        // 验证角色名称唯一性
        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("角色名称已存在: " + name);
        }
        
        // 创建角色对象
        Role role = new Role();
        role.setName(name);
        role.setDisplayName(displayName);
        role.setDescription(description);
        role.setPriority(priority != null ? priority : 0);
        role.setEnabled(true);
        role.setSystemRole(false);
        role.setCreatedBy(createdBy);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        
        Role savedRole = roleRepository.save(role);
        
        // 记录审计日志
        auditLogService.logRoleOperation("ROLE_CREATE", savedRole.getId(), 
                                        createdBy, "创建角色: " + name);
        
        log.info("角色创建成功: id={}, name={}", savedRole.getId(), savedRole.getName());
        return savedRole;
    }
    
    /**
     * 更新角色信息
     * 
     * @param roleId 角色ID
     * @param displayName 显示名称
     * @param description 角色描述
     * @param priority 角色优先级
     * @param enabled 是否启用
     * @param updatedBy 更新者ID
     * @return 更新后的角色
     * @throws IllegalArgumentException 如果角色不存在或为系统角色
     */
    @Transactional
    public Role updateRole(Long roleId, String displayName, String description, 
                          Integer priority, Boolean enabled, Long updatedBy) {
        log.info("更新角色: roleId={}, displayName={}, priority={}, enabled={}", 
                roleId, displayName, priority, enabled);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
        
        // 系统角色不允许修改某些属性
        if (role.isSystemRole()) {
            log.warn("尝试修改系统角色: roleId={}, name={}", roleId, role.getName());
            // 系统角色只允许修改描述和优先级
            if (description != null) {
                role.setDescription(description);
            }
            if (priority != null) {
                role.setPriority(priority);
            }
        } else {
            // 非系统角色可以修改所有属性
            if (displayName != null) {
                role.setDisplayName(displayName);
            }
            if (description != null) {
                role.setDescription(description);
            }
            if (priority != null) {
                role.setPriority(priority);
            }
            if (enabled != null) {
                role.setEnabled(enabled);
            }
        }
        
        role.setUpdatedBy(updatedBy);
        role.setUpdatedAt(LocalDateTime.now());
        
        Role updatedRole = roleRepository.save(role);
        
        // 记录审计日志
        auditLogService.logRoleOperation("ROLE_UPDATE", roleId, 
                                        updatedBy, "更新角色: " + role.getName());
        
        log.info("角色更新成功: id={}, name={}", updatedRole.getId(), updatedRole.getName());
        return updatedRole;
    }
    
    /**
     * 删除角色
     * 
     * @param roleId 角色ID
     * @param deletedBy 删除者ID
     * @throws IllegalArgumentException 如果角色不存在、为系统角色或仍有用户使用
     */
    @Transactional
    public void deleteRole(Long roleId, Long deletedBy) {
        log.info("删除角色: roleId={}", roleId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
        
        // 系统角色不允许删除
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("系统角色不允许删除: " + role.getName());
        }
        
        // 检查是否有用户使用此角色
        Long userCount = roleRepository.countUsersByRoleId(roleId);
        if (userCount > 0) {
            throw new IllegalArgumentException("角色仍有用户使用，无法删除: " + role.getName());
        }
        
        // 清除角色的所有权限关联
        role.getPermissions().clear();
        roleRepository.save(role);
        
        // 删除角色
        roleRepository.delete(role);
        
        // 记录审计日志
        auditLogService.logRoleOperation("ROLE_DELETE", roleId, 
                                        deletedBy, "删除角色: " + role.getName());
        
        log.info("角色删除成功: id={}, name={}", roleId, role.getName());
    }
    
    // ==================== 角色查询操作 ====================
    
    /**
     * 根据ID查找角色
     * 
     * @param roleId 角色ID
     * @return 角色对象
     */
    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }
    
    /**
     * 根据名称查找角色
     * 
     * @param name 角色名称
     * @return 角色对象
     */
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }
    
    /**
     * 查找所有启用的角色
     * 
     * @return 角色列表，按优先级降序排列
     */
    public List<Role> findAllEnabledRoles() {
        return roleRepository.findByEnabledTrueOrderByPriorityDesc();
    }
    
    /**
     * 查找所有角色（分页）
     * 
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    public Page<Role> findAllRoles(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }
    
    /**
     * 查找系统角色
     * 
     * @return 系统角色列表
     */
    public List<Role> findSystemRoles() {
        return roleRepository.findBySystemRoleTrueOrderByPriorityDesc();
    }
    
    /**
     * 查找非系统角色
     * 
     * @return 非系统角色列表
     */
    public List<Role> findCustomRoles() {
        return roleRepository.findBySystemRoleFalseOrderByPriorityDesc();
    }
    
    /**
     * 根据用户ID查找角色
     * 
     * @param userId 用户ID
     * @return 用户拥有的角色列表
     */
    public List<Role> findRolesByUserId(Long userId) {
        return roleRepository.findByUserId(userId);
    }
    
    /**
     * 根据权限名称查找角色
     * 
     * @param permissionName 权限名称
     * @return 拥有该权限的角色列表
     */
    public List<Role> findRolesByPermission(String permissionName) {
        return roleRepository.findByPermissionName(permissionName);
    }
    
    // ==================== 角色权限管理 ====================
    
    /**
     * 为角色分配权限
     * 
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @param assignedBy 分配者ID
     * @throws IllegalArgumentException 如果角色不存在
     */
    @Transactional
    public void assignPermissionsToRole(Long roleId, Set<Long> permissionIds, Long assignedBy) {
        log.info("为角色分配权限: roleId={}, permissionIds={}", roleId, permissionIds);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
        
        // 查找权限
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("部分权限不存在");
        }
        
        // 清除现有权限
        role.getPermissions().clear();
        
        // 分配新权限
        for (Permission permission : permissions) {
            if (permission.isEnabled()) {
                role.addPermission(permission);
            }
        }
        
        role.setUpdatedBy(assignedBy);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
        
        // 记录审计日志
        auditLogService.logRoleOperation("ROLE_ASSIGN_PERMISSIONS", roleId, 
                                        assignedBy, "为角色分配权限: " + permissionIds.size() + "个");
        
        log.info("角色权限分配成功: roleId={}, permissionCount={}", roleId, permissions.size());
    }
    
    /**
     * 为角色添加权限
     * 
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @param assignedBy 分配者ID
     */
    @Transactional
    public void addPermissionToRole(Long roleId, Long permissionId, Long assignedBy) {
        log.info("为角色添加权限: roleId={}, permissionId={}", roleId, permissionId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("权限不存在: " + permissionId));
        
        if (!permission.isEnabled()) {
            throw new IllegalArgumentException("权限已禁用: " + permission.getName());
        }
        
        role.addPermission(permission);
        role.setUpdatedBy(assignedBy);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
        
        // 记录审计日志
        auditLogService.logRoleOperation("ROLE_ADD_PERMISSION", roleId, 
                                        assignedBy, "添加权限: " + permission.getName());
        
        log.info("角色权限添加成功: roleId={}, permissionName={}", roleId, permission.getName());
    }
    
    /**
     * 从角色中移除权限
     * 
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @param removedBy 移除者ID
     */
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId, Long removedBy) {
        log.info("从角色移除权限: roleId={}, permissionId={}", roleId, permissionId);
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("权限不存在: " + permissionId));
        
        role.removePermission(permission);
        role.setUpdatedBy(removedBy);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
        
        // 记录审计日志
        auditLogService.logRoleOperation("ROLE_REMOVE_PERMISSION", roleId, 
                                        removedBy, "移除权限: " + permission.getName());
        
        log.info("角色权限移除成功: roleId={}, permissionName={}", roleId, permission.getName());
    }
    
    /**
     * 获取角色的所有权限
     * 
     * @param roleId 角色ID
     * @return 权限列表
     */
    public List<Permission> getRolePermissions(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }
    
    // ==================== 用户角色管理 ====================
    
    /**
     * 为用户分配角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @param assignedBy 分配者ID
     */
    @Transactional
    public void assignRolesToUser(Long userId, Set<Long> roleIds, Long assignedBy) {
        log.info("为用户分配角色: userId={}, roleIds={}", userId, roleIds);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        
        // 查找角色
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new IllegalArgumentException("部分角色不存在");
        }
        
        // 清除现有角色
        user.clearRoles();
        
        // 分配新角色
        for (Role role : roles) {
            if (role.isEnabled()) {
                user.addRole(role);
            }
        }
        
        userRepository.save(user);
        
        // 记录审计日志
        auditLogService.logUserOperation("USER_ASSIGN_ROLES", userId, 
                                        assignedBy, "为用户分配角色: " + roleIds.size() + "个");
        
        log.info("用户角色分配成功: userId={}, roleCount={}", userId, roles.size());
    }
    
    /**
     * 为用户添加角色
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param assignedBy 分配者ID
     */
    @Transactional
    public void addRoleToUser(Long userId, Long roleId, Long assignedBy) {
        log.info("为用户添加角色: userId={}, roleId={}", userId, roleId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
        
        if (!role.isEnabled()) {
            throw new IllegalArgumentException("角色已禁用: " + role.getName());
        }
        
        user.addRole(role);
        userRepository.save(user);
        
        // 记录审计日志
        auditLogService.logUserOperation("USER_ADD_ROLE", userId, 
                                        assignedBy, "添加角色: " + role.getName());
        
        log.info("用户角色添加成功: userId={}, roleName={}", userId, role.getName());
    }
    
    /**
     * 从用户中移除角色
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param removedBy 移除者ID
     */
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId, Long removedBy) {
        log.info("从用户移除角色: userId={}, roleId={}", userId, roleId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
        
        user.removeRole(role);
        userRepository.save(user);
        
        // 记录审计日志
        auditLogService.logUserOperation("USER_REMOVE_ROLE", userId, 
                                        removedBy, "移除角色: " + role.getName());
        
        log.info("用户角色移除成功: userId={}, roleName={}", userId, role.getName());
    }
    
    // ==================== 角色层级管理 ====================
    
    /**
     * 检查角色是否具有层级关系
     * 基于优先级实现简单的角色层级
     * 
     * @param higherRoleId 高级角色ID
     * @param lowerRoleId 低级角色ID
     * @return 是否具有层级关系
     */
    public boolean hasRoleHierarchy(Long higherRoleId, Long lowerRoleId) {
        Optional<Role> higherRole = roleRepository.findById(higherRoleId);
        Optional<Role> lowerRole = roleRepository.findById(lowerRoleId);
        
        if (higherRole.isEmpty() || lowerRole.isEmpty()) {
            return false;
        }
        
        return higherRole.get().hasHigherPriorityThan(lowerRole.get());
    }
    
    /**
     * 获取用户的最高优先级角色
     * 
     * @param userId 用户ID
     * @return 最高优先级角色
     */
    public Optional<Role> getUserHighestPriorityRole(Long userId) {
        List<Role> userRoles = roleRepository.findByUserId(userId);
        
        return userRoles.stream()
                .max((r1, r2) -> {
                    int p1 = r1.getPriority() != null ? r1.getPriority() : 0;
                    int p2 = r2.getPriority() != null ? r2.getPriority() : 0;
                    return Integer.compare(p1, p2);
                });
    }
    
    /**
     * 检查用户是否具有足够的角色权限执行操作
     * 
     * @param operatorUserId 操作者用户ID
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    public boolean hasPermissionToManageUser(Long operatorUserId, Long targetUserId) {
        if (operatorUserId.equals(targetUserId)) {
            return true; // 用户可以管理自己
        }
        
        Optional<Role> operatorRole = getUserHighestPriorityRole(operatorUserId);
        Optional<Role> targetRole = getUserHighestPriorityRole(targetUserId);
        
        if (operatorRole.isEmpty()) {
            return false;
        }
        
        // 如果目标用户没有角色，操作者有管理权限即可
        if (targetRole.isEmpty()) {
            return operatorRole.get().hasPermission("USER", "MANAGE");
        }
        
        // 操作者角色优先级必须高于目标用户
        return operatorRole.get().hasHigherPriorityThan(targetRole.get()) &&
               operatorRole.get().hasPermission("USER", "MANAGE");
    }
    
    // ==================== 权限检查方法 ====================
    
    /**
     * 检查用户是否具有指定权限
     * 
     * @param userId 用户ID
     * @param permissionName 权限名称
     * @return 是否具有权限
     */
    public boolean hasPermission(Long userId, String permissionName) {
        return permissionRepository.hasPermission(userId, permissionName);
    }
    
    /**
     * 检查用户是否具有对指定资源执行指定操作的权限
     * 
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否具有权限
     */
    public boolean hasPermission(Long userId, String resourceType, String actionType) {
        return permissionRepository.hasPermission(userId, resourceType, actionType);
    }
    
    /**
     * 获取用户的所有权限名称
     * 
     * @param userId 用户ID
     * @return 权限名称列表
     */
    public List<String> getUserPermissions(Long userId) {
        return permissionRepository.findPermissionNamesByUserId(userId);
    }
    
    /**
     * 获取用户的所有角色名称
     * 
     * @param userId 用户ID
     * @return 角色名称列表
     */
    public List<String> getUserRoles(Long userId) {
        return roleRepository.findRoleNamesByUserId(userId);
    }
}