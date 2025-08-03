package com.myweb.website_core.application.service.security;

import com.myweb.website_core.domain.security.entity.Permission;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.infrastructure.persistence.repository.PermissionRepository;
import com.myweb.website_core.infrastructure.persistence.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认角色和权限初始化器
 * 
 * 在系统启动时自动创建默认的角色和权限
 * 确保系统具备基本的RBAC权限控制能力
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * 初始化内容：
 * - 系统默认权限（POST、USER、COMMENT、SYSTEM等资源的CRUD权限）
 * - 系统默认角色（ADMIN、MODERATOR、USER）
 * - 角色权限关联关系
 */
@Slf4j
@Component
@Order(1) // 确保在其他组件之前执行
@RequiredArgsConstructor
public class DefaultRolePermissionInitializer implements CommandLineRunner {
    
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("开始初始化默认角色和权限...");
        
        try {
            // 1. 初始化默认权限
            initializeDefaultPermissions();
            
            // 2. 初始化默认角色
            initializeDefaultRoles();
            
            // 3. 分配角色权限
            assignRolePermissions();
            
            log.info("默认角色和权限初始化完成");
        } catch (Exception e) {
            log.error("初始化默认角色和权限失败", e);
            throw e;
        }
    }
    
    /**
     * 初始化默认权限
     */
    private void initializeDefaultPermissions() {
        log.info("初始化默认权限...");
        
        List<PermissionData> defaultPermissions = getDefaultPermissions();
        
        for (PermissionData data : defaultPermissions) {
            // 检查权限是否已存在
            if (!permissionRepository.existsByName(data.name)) {
                Permission permission = Permission.builder()
                        .name(data.name)
                        .displayName(data.displayName)
                        .description(data.description)
                        .resourceType(data.resourceType)
                        .actionType(data.actionType)
                        .permissionGroup(data.permissionGroup)
                        .permissionLevel(data.permissionLevel)
                        .systemPermission(true)
                        .build();
                
                permission.setEnabled(true);
                permission.setCreatedAt(LocalDateTime.now());
                permission.setUpdatedAt(LocalDateTime.now());
                
                permissionRepository.save(permission);
                log.debug("创建默认权限: {}", data.name);
            }
        }
        
        log.info("默认权限初始化完成，共{}个权限", defaultPermissions.size());
    }
    
    /**
     * 初始化默认角色
     */
    private void initializeDefaultRoles() {
        log.info("初始化默认角色...");
        
        List<RoleData> defaultRoles = getDefaultRoles();
        
        for (RoleData data : defaultRoles) {
            // 检查角色是否已存在
            if (!roleRepository.existsByName(data.name)) {
                Role role = new Role();
                role.setName(data.name);
                role.setDisplayName(data.displayName);
                role.setDescription(data.description);
                role.setPriority(data.priority);
                role.setEnabled(true);
                role.setSystemRole(true);
                role.setCreatedAt(LocalDateTime.now());
                role.setUpdatedAt(LocalDateTime.now());
                
                roleRepository.save(role);
                log.debug("创建默认角色: {}", data.name);
            }
        }
        
        log.info("默认角色初始化完成，共{}个角色", defaultRoles.size());
    }
    
    /**
     * 分配角色权限
     */
    private void assignRolePermissions() {
        log.info("分配角色权限...");
        
        // 管理员角色 - 拥有所有权限
        assignPermissionsToRole("ADMIN", getAllPermissionNames());
        
        // 版主角色 - 拥有内容管理权限
        assignPermissionsToRole("MODERATOR", getModeratorPermissions());
        
        // 普通用户角色 - 拥有基本权限
        assignPermissionsToRole("USER", getUserPermissions());
        
        log.info("角色权限分配完成");
    }
    
    /**
     * 为角色分配权限
     * 
     * @param roleName 角色名称
     * @param permissionNames 权限名称列表
     */
    private void assignPermissionsToRole(String roleName, List<String> permissionNames) {
        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role == null) {
            log.warn("角色不存在: {}", roleName);
            return;
        }
        
        // 清除现有权限
        role.getPermissions().clear();
        
        // 添加新权限
        for (String permissionName : permissionNames) {
            Permission permission = permissionRepository.findByName(permissionName).orElse(null);
            if (permission != null) {
                role.addPermission(permission);
            } else {
                log.warn("权限不存在: {}", permissionName);
            }
        }
        
        roleRepository.save(role);
        log.debug("为角色{}分配了{}个权限", roleName, permissionNames.size());
    }
    
    /**
     * 获取默认权限列表
     */
    private List<PermissionData> getDefaultPermissions() {
        List<PermissionData> permissions = new ArrayList<>();
        
        // POST相关权限
        permissions.add(new PermissionData("POST_CREATE", "创建帖子", "允许用户创建新帖子", 
                "POST", "CREATE", "内容管理", 1));
        permissions.add(new PermissionData("POST_READ", "查看帖子", "允许用户查看帖子内容", 
                "POST", "READ", "内容管理", 1));
        permissions.add(new PermissionData("POST_UPDATE", "编辑帖子", "允许用户编辑自己的帖子", 
                "POST", "UPDATE", "内容管理", 2));
        permissions.add(new PermissionData("POST_DELETE", "删除帖子", "允许用户删除自己的帖子", 
                "POST", "DELETE", "内容管理", 3));
        permissions.add(new PermissionData("POST_MANAGE", "管理帖子", "允许管理所有帖子", 
                "POST", "MANAGE", "内容管理", 4));
        
        // COMMENT相关权限
        permissions.add(new PermissionData("COMMENT_CREATE", "创建评论", "允许用户创建评论", 
                "COMMENT", "CREATE", "内容管理", 1));
        permissions.add(new PermissionData("COMMENT_READ", "查看评论", "允许用户查看评论", 
                "COMMENT", "READ", "内容管理", 1));
        permissions.add(new PermissionData("COMMENT_UPDATE", "编辑评论", "允许用户编辑自己的评论", 
                "COMMENT", "UPDATE", "内容管理", 2));
        permissions.add(new PermissionData("COMMENT_DELETE", "删除评论", "允许用户删除自己的评论", 
                "COMMENT", "DELETE", "内容管理", 3));
        permissions.add(new PermissionData("COMMENT_MANAGE", "管理评论", "允许管理所有评论", 
                "COMMENT", "MANAGE", "内容管理", 4));
        
        // USER相关权限
        permissions.add(new PermissionData("USER_READ", "查看用户", "允许查看用户信息", 
                "USER", "READ", "用户管理", 1));
        permissions.add(new PermissionData("USER_UPDATE", "编辑用户", "允许编辑自己的用户信息", 
                "USER", "UPDATE", "用户管理", 2));
        permissions.add(new PermissionData("USER_DELETE", "删除用户", "允许删除自己的账户", 
                "USER", "DELETE", "用户管理", 3));
        permissions.add(new PermissionData("USER_MANAGE", "管理用户", "允许管理所有用户", 
                "USER", "MANAGE", "用户管理", 4));
        
        // SYSTEM相关权限
        permissions.add(new PermissionData("SYSTEM_READ", "查看系统信息", "允许查看系统状态", 
                "SYSTEM", "READ", "系统管理", 1));
        permissions.add(new PermissionData("SYSTEM_MANAGE", "系统管理", "允许管理系统设置", 
                "SYSTEM", "MANAGE", "系统管理", 5));
        
        // ROLE相关权限
        permissions.add(new PermissionData("ROLE_READ", "查看角色", "允许查看角色信息", 
                "ROLE", "READ", "权限管理", 1));
        permissions.add(new PermissionData("ROLE_CREATE", "创建角色", "允许创建新角色", 
                "ROLE", "CREATE", "权限管理", 3));
        permissions.add(new PermissionData("ROLE_UPDATE", "编辑角色", "允许编辑角色信息", 
                "ROLE", "UPDATE", "权限管理", 3));
        permissions.add(new PermissionData("ROLE_DELETE", "删除角色", "允许删除角色", 
                "ROLE", "DELETE", "权限管理", 4));
        permissions.add(new PermissionData("ROLE_MANAGE", "管理角色", "允许管理所有角色", 
                "ROLE", "MANAGE", "权限管理", 5));
        
        // PERMISSION相关权限
        permissions.add(new PermissionData("PERMISSION_READ", "查看权限", "允许查看权限信息", 
                "PERMISSION", "READ", "权限管理", 1));
        permissions.add(new PermissionData("PERMISSION_CREATE", "创建权限", "允许创建新权限", 
                "PERMISSION", "CREATE", "权限管理", 4));
        permissions.add(new PermissionData("PERMISSION_UPDATE", "编辑权限", "允许编辑权限信息", 
                "PERMISSION", "UPDATE", "权限管理", 4));
        permissions.add(new PermissionData("PERMISSION_DELETE", "删除权限", "允许删除权限", 
                "PERMISSION", "DELETE", "权限管理", 5));
        permissions.add(new PermissionData("PERMISSION_MANAGE", "管理权限", "允许管理所有权限", 
                "PERMISSION", "MANAGE", "权限管理", 5));
        
        // AUDIT相关权限
        permissions.add(new PermissionData("AUDIT_READ", "查看审计日志", "允许查看审计日志", 
                "AUDIT", "READ", "安全管理", 3));
        permissions.add(new PermissionData("AUDIT_EXPORT", "导出审计日志", "允许导出审计日志", 
                "AUDIT", "EXPORT", "安全管理", 4));
        permissions.add(new PermissionData("AUDIT_MANAGE", "管理审计日志", "允许管理审计日志", 
                "AUDIT", "MANAGE", "安全管理", 5));
        
        return permissions;
    }
    
    /**
     * 获取默认角色列表
     */
    private List<RoleData> getDefaultRoles() {
        List<RoleData> roles = new ArrayList<>();
        
        roles.add(new RoleData("ADMIN", "系统管理员", 
                "拥有系统所有权限，可以管理用户、角色、权限和系统设置", 100));
        roles.add(new RoleData("MODERATOR", "版主", 
                "拥有内容管理权限，可以管理帖子和评论", 50));
        roles.add(new RoleData("USER", "普通用户", 
                "拥有基本权限，可以创建和管理自己的内容", 10));
        
        return roles;
    }
    
    /**
     * 获取所有权限名称
     */
    private List<String> getAllPermissionNames() {
        return getDefaultPermissions().stream()
                .map(p -> p.name)
                .toList();
    }
    
    /**
     * 获取版主权限
     */
    private List<String> getModeratorPermissions() {
        return List.of(
                // 内容管理权限
                "POST_READ", "POST_MANAGE",
                "COMMENT_READ", "COMMENT_MANAGE",
                // 基本用户权限
                "USER_READ", "USER_UPDATE",
                // 系统查看权限
                "SYSTEM_READ"
        );
    }
    
    /**
     * 获取普通用户权限
     */
    private List<String> getUserPermissions() {
        return List.of(
                // 基本内容权限
                "POST_CREATE", "POST_READ", "POST_UPDATE", "POST_DELETE",
                "COMMENT_CREATE", "COMMENT_READ", "COMMENT_UPDATE", "COMMENT_DELETE",
                // 基本用户权限
                "USER_READ", "USER_UPDATE", "USER_DELETE"
        );
    }
    
    // ==================== 内部数据类 ====================
    
    /**
     * 权限数据类
     */
    private static class PermissionData {
        final String name;
        final String displayName;
        final String description;
        final String resourceType;
        final String actionType;
        final String permissionGroup;
        final Integer permissionLevel;
        
        PermissionData(String name, String displayName, String description,
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
    }
    
    /**
     * 角色数据类
     */
    private static class RoleData {
        final String name;
        final String displayName;
        final String description;
        final Integer priority;
        
        RoleData(String name, String displayName, String description, Integer priority) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.priority = priority;
        }
    }
}