package com.myweb.website_core.domain.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.myweb.website_core.domain.business.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体类
 * 
 * 表示系统中的角色信息，用于基于角色的访问控制(RBAC)模型
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 * 
 * 角色是权限的集合，用户通过分配角色来获得相应的权限
 * 支持角色与权限的多对多关系，实现灵活的权限管理
 */
@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role {
    
    /**
     * 角色ID，主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 角色名称
     * 唯一标识，如：ADMIN、MODERATOR、USER等
     */
    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;
    
    /**
     * 角色显示名称
     * 用于前端显示的友好名称
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    
    /**
     * 角色描述
     * 详细说明角色的职责和权限范围
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 角色状态
     * true: 启用, false: 禁用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * 是否为系统内置角色
     * 系统内置角色不允许删除
     */
    @Column(name = "system_role", nullable = false)
    private Boolean systemRole = false;
    
    /**
     * 角色优先级
     * 数值越大优先级越高，用于权限判断
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;
    
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
     * 角色关联的权限集合
     * 多对多关系，通过role_permissions中间表关联
     */
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
    
    /**
     * 拥有此角色的用户集合
     * 多对多关系，通过user_roles中间表关联
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
    
    // ==================== JPA生命周期回调 ====================
    
    /**
     * 实体持久化前的回调
     * 自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
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
     * 添加权限到角色
     * 
     * @param permission 要添加的权限
     */
    public void addPermission(Permission permission) {
        if (permission != null) {
            this.permissions.add(permission);
            permission.getRoles().add(this);
        }
    }
    
    /**
     * 从角色中移除权限
     * 
     * @param permission 要移除的权限
     */
    public void removePermission(Permission permission) {
        if (permission != null) {
            this.permissions.remove(permission);
            permission.getRoles().remove(this);
        }
    }
    
    /**
     * 检查角色是否包含指定权限
     * 
     * @param permissionName 权限名称
     * @return 是否包含指定权限
     */
    public boolean hasPermission(String permissionName) {
        if (permissionName == null || permissions == null) {
            return false;
        }
        
        return permissions.stream()
                .anyMatch(permission -> permissionName.equals(permission.getName()));
    }
    
    /**
     * 检查角色是否包含指定资源和操作的权限
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否包含指定权限
     */
    public boolean hasPermission(String resourceType, String actionType) {
        if (resourceType == null || actionType == null || permissions == null) {
            return false;
        }
        
        return permissions.stream()
                .anyMatch(permission -> 
                    resourceType.equals(permission.getResourceType()) && 
                    actionType.equals(permission.getActionType()));
    }
    
    /**
     * 获取角色的所有权限名称
     * 
     * @return 权限名称集合
     */
    public Set<String> getPermissionNames() {
        if (permissions == null) {
            return new HashSet<>();
        }
        
        return permissions.stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * 检查角色是否启用
     * 
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    /**
     * 检查是否为系统角色
     * 
     * @return 是否为系统角色
     */
    public boolean isSystemRole() {
        return systemRole != null && systemRole;
    }
    
    /**
     * 比较角色优先级
     * 
     * @param otherRole 其他角色
     * @return 当前角色优先级是否高于其他角色
     */
    public boolean hasHigherPriorityThan(Role otherRole) {
        if (otherRole == null) {
            return true;
        }
        
        int thisPriority = this.priority != null ? this.priority : 0;
        int otherPriority = otherRole.priority != null ? otherRole.priority : 0;
        
        return thisPriority > otherPriority;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        
        Role role = (Role) o;
        return id != null && id.equals(role.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Role{id=%d, name='%s', displayName='%s', enabled=%s}", 
                           id, name, displayName, enabled);
    }
}