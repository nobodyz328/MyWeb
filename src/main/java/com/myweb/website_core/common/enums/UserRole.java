package com.myweb.website_core.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 * <p>
 * 定义系统中的用户角色类型，用于基于角色的访问控制(RBAC)
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 */
@Getter
public enum UserRole {
    
    /**
     * 普通用户
     * - 可以发布、编辑、删除自己的帖子
     * - 可以评论其他用户的帖子
     * - 可以点赞和收藏帖子
     * - 可以关注其他用户
     * - 可以修改自己的个人资料
     */
    USER("USER", "普通用户", "系统的基础用户角色，具有基本的内容创建和互动权限"),
    
    /**
     * 版主
     * - 拥有普通用户的所有权限
     * - 可以删除任何用户的帖子和评论
     * - 可以禁言用户
     * - 可以审核举报内容
     * - 可以管理社区公告
     */
    MODERATOR("MODERATOR", "版主", "社区内容管理员，负责维护社区秩序和内容质量"),
    
    /**
     * 系统管理员
     * - 拥有系统的最高权限
     * - 可以管理所有用户账户
     * - 可以查看系统审计日志
     * - 可以配置系统安全策略
     * - 可以管理角色和权限
     * - 需要启用二次验证(TOTP)
     */
    ADMIN("ADMIN", "系统管理员", "系统最高权限角色，负责系统管理和安全配置");
    
    /**
     * 角色代码
     * -- GETTER --
     *  获取角色代码
     *

     */
    private final String code;
    
    /**
     * 角色名称
     * -- GETTER --
     *  获取角色名称
     *

     */
    private final String name;
    
    /**
     * 角色描述
     * -- GETTER --
     *  获取角色描述
     *

     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param code 角色代码
     * @param name 角色名称
     * @param description 角色描述
     */
    UserRole(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据代码获取角色枚举
     * 
     * @param code 角色代码
     * @return 角色枚举，如果不存在则返回null
     */
    public static UserRole fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (UserRole role : UserRole.values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
    
    /**
     * 检查是否为管理员角色
     * 
     * @return 是否为管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * 检查是否为版主角色
     * 
     * @return 是否为版主
     */
    public boolean isModerator() {
        return this == MODERATOR;
    }
    
    /**
     * 检查是否为普通用户角色
     * 
     * @return 是否为普通用户
     */
    public boolean isUser() {
        return this == USER;
    }
    
    /**
     * 检查是否具有管理权限（管理员或版主）
     * 
     * @return 是否具有管理权限
     */
    public boolean hasManagementPermission() {
        return this == ADMIN || this == MODERATOR;
    }
    
    /**
     * 检查是否具有系统管理权限（仅管理员）
     * 
     * @return 是否具有系统管理权限
     */
    public boolean hasSystemAdminPermission() {
        return this == ADMIN;
    }
    
    /**
     * 获取角色的权限级别（数值越大权限越高）
     * 
     * @return 权限级别
     */
    public int getPermissionLevel() {
        return switch (this) {
            case USER -> 1;
            case MODERATOR -> 2;
            case ADMIN -> 3;
            //default -> 0;
        };
    }
    
    /**
     * 检查当前角色是否具有比指定角色更高的权限
     * 
     * @param otherRole 其他角色
     * @return 是否具有更高权限
     */
    public boolean hasHigherPermissionThan(UserRole otherRole) {
        if (otherRole == null) {
            return true;
        }
        return this.getPermissionLevel() > otherRole.getPermissionLevel();
    }
    
    /**
     * 检查当前角色是否具有比指定角色相同或更高的权限
     * 
     * @param otherRole 其他角色
     * @return 是否具有相同或更高权限
     */
    public boolean hasPermissionEqualOrHigherThan(UserRole otherRole) {
        if (otherRole == null) {
            return true;
        }
        return this.getPermissionLevel() >= otherRole.getPermissionLevel();
    }
    
    @Override
    public String toString() {
        return String.format("UserRole{code='%s', name='%s', description='%s'}", 
                           code, name, description);
    }
}