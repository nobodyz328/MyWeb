package com.myweb.website_core.domain.business.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.domain.security.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户实体类
 * <p>
 * 表示系统中的用户信息，包括：
 * - 基本信息（用户名、邮箱等）
 * - 个人资料（头像、简介等）
 * - 社交关系（关注、粉丝等）
 * - 统计数据（获赞数等）
 * - 安全字段（密码哈希、登录跟踪、二次验证等）
 * <p>
 * 符合GB/T 22239-2019二级等保要求的身份鉴别和访问控制机制
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = false, nullable = true)
    private String email;

    
    private String avatarUrl;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @ManyToMany
    @JoinTable(
        name = "user_followers",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    @JsonBackReference("user-followers")
    private List<User> followers;
    
    @ManyToMany(mappedBy = "followers")
    @JsonBackReference("user-following")
    private List<User> following;
    
    private Integer likedCount = 0;
    
    // ==================== 安全增强字段 ====================
    
    /**
     * 密码哈希值
     * 使用BCrypt算法加密存储，替代明文密码
     * 符合GB/T 22239-2019身份鉴别要求
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    /**
     * 邮箱验证状态
     * 标识用户邮箱是否已通过验证
     * 符合GB/T 22239-2019身份鉴别要求
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    /**
     * 登录失败次数
     * 用于实现账户锁定机制，防止暴力破解
     * 符合GB/T 22239-2019身份鉴别要求
     */
    @Column(name = "login_attempts", nullable = false)
    private Integer loginAttempts = 0;
    
    /**
     * 账户锁定截止时间
     * 当登录失败次数超过阈值时，锁定账户到指定时间
     * 符合GB/T 22239-2019身份鉴别要求
     */
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;
    
    /**
     * 最后登录时间
     * 记录用户最后一次成功登录的时间
     * 用于安全审计和异常检测
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
    /**
     * 最后登录IP地址
     * 记录用户最后一次成功登录的IP地址
     * 用于安全审计和异常检测
     */
    @Column(name = "last_login_ip")
    private String lastLoginIp;
    
    /**
     * TOTP动态口令密钥
     * 用于二次验证，管理员账户必须启用
     * 符合GB/T 22239-2019身份鉴别要求
     */
    @Column(name = "totp_secret")
    private String totpSecret;
    
    /**
     * TOTP动态口令启用状态
     * 标识是否启用了二次验证
     * 管理员账户必须启用
     */
    @Column(name = "totp_enabled", nullable = false)
    private Boolean totpEnabled = false;
    
    /**
     * 用户角色（保留用于向后兼容）
     * 基于角色的访问控制(RBAC)
     * 符合GB/T 22239-2019访问控制要求
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;
    
    /**
     * 用户关联的角色集合
     * 多对多关系，通过user_roles中间表关联
     * 支持更灵活的RBAC权限模型
     */
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    /**
     * 创建时间
     * 记录用户账户创建时间
     * 用于审计和数据管理
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * 记录用户信息最后更新时间
     * 用于审计和数据管理
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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
    
    // ==================== 安全相关业务方法 ====================
    
    /**
     * 检查账户是否被锁定
     * 
     * @return 账户是否被锁定
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }
    
    /**
     * 检查是否需要二次验证
     * 管理员账户必须启用TOTP
     * 
     * @return 是否需要二次验证
     */
    public boolean requiresTwoFactorAuth() {
        return role == UserRole.ADMIN && totpEnabled;
    }
    
    /**
     * 重置登录失败次数
     * 登录成功后调用
     */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.accountLockedUntil = null;
    }
    
    /**
     * 增加登录失败次数
     * 登录失败后调用
     */
    public void incrementLoginAttempts() {
        this.loginAttempts++;
        
        // 失败5次后锁定账户15分钟
        if (this.loginAttempts >= 5) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }
    
    /**
     * 更新最后登录信息
     * 
     * @param loginTime 登录时间
     * @param loginIp 登录IP地址
     */
    public void updateLastLoginInfo(LocalDateTime loginTime, String loginIp) {
        this.lastLoginTime = loginTime;
        this.lastLoginIp = loginIp;
    }
    
    /**
     * 检查是否具有指定角色
     * 
     * @param requiredRole 需要的角色
     * @return 是否具有指定角色
     */
    public boolean hasRole(UserRole requiredRole) {
        return this.role == requiredRole;
    }
    
    /**
     * 检查是否具有管理权限
     * 
     * @return 是否具有管理权限
     */
    public boolean hasManagementPermission() {
        return this.role != null && this.role.hasManagementPermission();
    }
    
    /**
     * 检查是否具有系统管理权限
     * 
     * @return 是否具有系统管理权限
     */
    public boolean hasSystemAdminPermission() {
        return this.role != null && this.role.hasSystemAdminPermission();
    }
    
    // ==================== RBAC角色管理方法 ====================
    
    /**
     * 添加角色到用户
     * 
     * @param role 要添加的角色
     */
    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
            role.getUsers().add(this);
        }
    }
    
    /**
     * 从用户中移除角色
     * 
     * @param role 要移除的角色
     */
    public void removeRole(Role role) {
        if (role != null) {
            this.roles.remove(role);
            role.getUsers().remove(this);
        }
    }
    
    /**
     * 检查用户是否拥有指定名称的角色
     * 
     * @param roleName 角色名称
     * @return 是否拥有指定角色
     */
    public boolean hasRoleName(String roleName) {
        if (roleName == null || roles == null) {
            return false;
        }
        
        return roles.stream()
                .anyMatch(role -> roleName.equals(role.getName()) && role.isEnabled());
    }
    
    /**
     * 检查用户是否拥有指定的角色对象
     * 
     * @param role 角色对象
     * @return 是否拥有指定角色
     */
    public boolean hasRole(Role role) {
        if (role == null || roles == null) {
            return false;
        }
        
        return roles.contains(role) && role.isEnabled();
    }
    
    /**
     * 检查用户是否拥有指定权限
     * 
     * @param permissionName 权限名称
     * @return 是否拥有指定权限
     */
    public boolean hasPermission(String permissionName) {
        if (permissionName == null || roles == null) {
            return false;
        }
        
        return roles.stream()
                .filter(Role::isEnabled)
                .anyMatch(role -> role.hasPermission(permissionName));
    }
    
    /**
     * 检查用户是否拥有对指定资源执行指定操作的权限
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否拥有指定权限
     */
    public boolean hasPermission(String resourceType, String actionType) {
        if (resourceType == null || actionType == null || roles == null) {
            return false;
        }
        
        return roles.stream()
                .filter(Role::isEnabled)
                .anyMatch(role -> role.hasPermission(resourceType, actionType));
    }
    
    /**
     * 获取用户的所有权限名称
     * 
     * @return 权限名称集合
     */
    public Set<String> getAllPermissions() {
        if (roles == null) {
            return new HashSet<>();
        }
        
        return roles.stream()
                .filter(Role::isEnabled)
                .flatMap(role -> role.getPermissionNames().stream())
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * 获取用户的所有角色名称
     * 
     * @return 角色名称集合
     */
    public Set<String> getAllRoleNames() {
        if (roles == null) {
            return new HashSet<>();
        }
        
        return roles.stream()
                .filter(Role::isEnabled)
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * 检查用户是否拥有任何管理权限
     * 
     * @return 是否拥有管理权限
     */
    public boolean hasAnyManagementPermission() {
        if (roles == null) {
            return false;
        }
        
        return roles.stream()
                .filter(Role::isEnabled)
                .anyMatch(role -> 
                    role.hasPermission("SYSTEM", "MANAGE") ||
                    role.hasPermission("USER", "MANAGE") ||
                    role.hasPermission("POST", "MANAGE") ||
                    role.hasPermission("COMMENT", "MANAGE")
                );
    }
    
    /**
     * 清空用户的所有角色
     */
    public void clearRoles() {
        if (roles != null) {
            // 先复制一份避免并发修改异常
            Set<Role> rolesToRemove = new HashSet<>(roles);
            for (Role role : rolesToRemove) {
                removeRole(role);
            }
        }
    }
    
    /**
     * 获取用户的最高优先级角色
     * 
     * @return 最高优先级角色，如果没有角色则返回null
     */
    public Role getHighestPriorityRole() {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        
        return roles.stream()
                .filter(Role::isEnabled)
                .max((r1, r2) -> {
                    int p1 = r1.getPriority() != null ? r1.getPriority() : 0;
                    int p2 = r2.getPriority() != null ? r2.getPriority() : 0;
                    return Integer.compare(p1, p2);
                })
                .orElse(null);
    }

}