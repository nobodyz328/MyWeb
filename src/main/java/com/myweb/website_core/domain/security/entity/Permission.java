package com.myweb.website_core.domain.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限实体类
 * 
 * 表示系统中的权限信息，用于基于角色的访问控制(RBAC)模型
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * 权限定义了对特定资源执行特定操作的能力
 * 通过资源类型和操作类型的组合来精确控制访问权限
 */
@Getter
@Setter
@Entity
@Table(name = "permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"resource_type", "action_type"}))
public class Permission {
    
    /**
     * 权限ID，主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 权限名称
     * 唯一标识，格式：RESOURCE_ACTION，如：POST_CREATE、USER_MANAGE等
     */
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;
    
    /**
     * 权限显示名称
     * 用于前端显示的友好名称
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    
    /**
     * 权限描述
     * 详细说明权限的作用和适用场景
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 资源类型
     * 权限控制的资源类型，如：POST、USER、COMMENT、SYSTEM等
     */
    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;
    
    /**
     * 操作类型
     * 对资源执行的操作类型，如：CREATE、READ、UPDATE、DELETE、MANAGE等
     */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;
    
    /**
     * 权限状态
     * true: 启用, false: 禁用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * 是否为系统内置权限
     * 系统内置权限不允许删除
     */
    @Column(name = "system_permission", nullable = false)
    private Boolean systemPermission = false;
    
    /**
     * 权限级别
     * 用于权限层级管理，数值越大权限级别越高
     */
    @Column(name = "permission_level", nullable = false)
    private Integer permissionLevel = 1;
    
    /**
     * 权限分组
     * 用于权限管理界面的分组显示
     */
    @Column(name = "permission_group", length = 50)
    private String permissionGroup;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;
    
    /**
     * 更新者ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;
    
    /**
     * 拥有此权限的角色集合
     * 多对多关系，通过role_permissions中间表关联
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();
    
    // ==================== JPA生命周期回调 ====================
    
    /**
     * 实体持久化前的回调
     * 自动设置创建时间和更新时间，生成权限名称
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        
        // 自动生成权限名称（如果未设置）
        if (this.name == null && this.resourceType != null && this.actionType != null) {
            this.name = this.resourceType.toUpperCase() + "_" + this.actionType.toUpperCase();
        }
    }
    
    /**
     * 实体更新前的回调
     * 自动更新更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // ==================== 业务方法 ====================
    
    /**
     * 检查权限是否启用
     * 
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    /**
     * 检查是否为系统权限
     * 
     * @return 是否为系统权限
     */
    public boolean isSystemPermission() {
        return systemPermission != null && systemPermission;
    }
    
    /**
     * 检查是否匹配指定的资源类型和操作类型
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否匹配
     */
    public boolean matches(String resourceType, String actionType) {
        return this.resourceType != null && this.resourceType.equals(resourceType) &&
               this.actionType != null && this.actionType.equals(actionType);
    }
    
    /**
     * 检查是否匹配指定的权限名称
     * 
     * @param permissionName 权限名称
     * @return 是否匹配
     */
    public boolean matches(String permissionName) {
        return this.name != null && this.name.equals(permissionName);
    }
    
    /**
     * 获取完整的权限标识
     * 格式：resourceType:actionType
     * 
     * @return 权限标识
     */
    public String getPermissionIdentifier() {
        if (resourceType == null || actionType == null) {
            return name;
        }
        return resourceType + ":" + actionType;
    }
    
    /**
     * 检查权限级别是否高于指定权限
     * 
     * @param otherPermission 其他权限
     * @return 是否具有更高级别
     */
    public boolean hasHigherLevelThan(Permission otherPermission) {
        if (otherPermission == null) {
            return true;
        }
        
        int thisLevel = this.permissionLevel != null ? this.permissionLevel : 1;
        int otherLevel = otherPermission.permissionLevel != null ? otherPermission.permissionLevel : 1;
        
        return thisLevel > otherLevel;
    }
    
    /**
     * 检查是否为读取权限
     * 
     * @return 是否为读取权限
     */
    public boolean isReadPermission() {
        return "READ".equalsIgnoreCase(actionType) || "VIEW".equalsIgnoreCase(actionType);
    }
    
    /**
     * 检查是否为写入权限
     * 
     * @return 是否为写入权限
     */
    public boolean isWritePermission() {
        return "CREATE".equalsIgnoreCase(actionType) || 
               "UPDATE".equalsIgnoreCase(actionType) || 
               "DELETE".equalsIgnoreCase(actionType) ||
               "EDIT".equalsIgnoreCase(actionType);
    }
    
    /**
     * 检查是否为管理权限
     * 
     * @return 是否为管理权限
     */
    public boolean isManagePermission() {
        return "MANAGE".equalsIgnoreCase(actionType) || 
               "ADMIN".equalsIgnoreCase(actionType) ||
               "CONTROL".equalsIgnoreCase(actionType);
    }
    
    /**
     * 创建权限的构建器
     * 
     * @return 权限构建器
     */
    public static PermissionBuilder builder() {
        return new PermissionBuilder();
    }
    
    /**
     * 权限构建器
     */
    public static class PermissionBuilder {
        private Permission permission = new Permission();
        
        public PermissionBuilder name(String name) {
            permission.setName(name);
            return this;
        }
        
        public PermissionBuilder displayName(String displayName) {
            permission.setDisplayName(displayName);
            return this;
        }
        
        public PermissionBuilder description(String description) {
            permission.setDescription(description);
            return this;
        }
        
        public PermissionBuilder resourceType(String resourceType) {
            permission.setResourceType(resourceType);
            return this;
        }
        
        public PermissionBuilder actionType(String actionType) {
            permission.setActionType(actionType);
            return this;
        }
        
        public PermissionBuilder permissionGroup(String permissionGroup) {
            permission.setPermissionGroup(permissionGroup);
            return this;
        }
        
        public PermissionBuilder permissionLevel(Integer permissionLevel) {
            permission.setPermissionLevel(permissionLevel);
            return this;
        }
        
        public PermissionBuilder systemPermission(Boolean systemPermission) {
            permission.setSystemPermission(systemPermission);
            return this;
        }
        
        public Permission build() {
            return permission;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;
        
        Permission that = (Permission) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Permission{id=%d, name='%s', resourceType='%s', actionType='%s', enabled=%s}", 
                           id, name, resourceType, actionType, enabled);
    }
}