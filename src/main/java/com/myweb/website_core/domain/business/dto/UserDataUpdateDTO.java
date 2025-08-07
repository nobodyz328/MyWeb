package com.myweb.website_core.domain.business.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户数据更新DTO
 * 
 * 用于用户数据的修改功能
 * 包含数据验证规则和权限控制
 * 符合GB/T 22239-2019二级等保要求的数据完整性保护机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDataUpdateDTO {

    // ==================== 基本信息字段 ====================
    
    /**
     * 头像URL
     * 用户可以修改
     */
    @Size(max = 500, message = "头像URL长度不能超过500字符")
    @Pattern(regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp))?$", 
             message = "头像URL格式不正确，必须是有效的图片链接")
    private String avatarUrl;
    
    /**
     * 个人简介
     * 用户可以修改
     */
    @Size(max = 500, message = "个人简介长度不能超过500字符")
    private String bio;
    
    // ==================== 敏感信息字段（仅管理员可修改） ====================
    
    /**
     * 邮箱地址
     * 仅管理员可以修改
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100字符")
    private String email;
    
    /**
     * 用户角色
     * 仅管理员可以修改
     */
    @Pattern(regexp = "^(USER|ADMIN|MODERATOR)$", 
             message = "用户角色必须是USER、ADMIN或MODERATOR之一")
    private String role;
    
    // ==================== 元数据信息 ====================
    
    /**
     * 更新原因
     * 用于审计记录
     */
    @Size(max = 200, message = "更新原因长度不能超过200字符")
    private String updateReason;
    
    /**
     * 是否强制更新
     * 管理员专用标识
     */
    private Boolean forceUpdate;
    
    // ==================== 构造方法 ====================
    
    public UserDataUpdateDTO() {
        this.forceUpdate = false;
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 检查是否有任何字段需要更新
     */
    public boolean hasAnyUpdate() {
        return avatarUrl != null || 
               bio != null || 
               email != null || 
               role != null;
    }
    
    /**
     * 检查是否包含敏感字段更新
     */
    public boolean hasSensitiveUpdate() {
        return email != null || role != null;
    }
    
    /**
     * 检查是否只包含基本字段更新
     */
    public boolean hasOnlyBasicUpdate() {
        return (avatarUrl != null || bio != null) && 
               (email == null && role == null);
    }
    
    /**
     * 获取更新字段摘要
     */
    public String getUpdateSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (avatarUrl != null) {
            summary.append("头像URL, ");
        }
        if (bio != null) {
            summary.append("个人简介, ");
        }
        if (email != null) {
            summary.append("邮箱地址, ");
        }
        if (role != null) {
            summary.append("用户角色, ");
        }
        
        if (summary.length() > 0) {
            summary.setLength(summary.length() - 2); // 移除最后的逗号和空格
        }
        
        return summary.toString();
    }
    
    /**
     * 清理敏感字段（用于非管理员用户）
     */
    public void clearSensitiveFields() {
        this.email = null;
        this.role = null;
    }
    
    /**
     * 验证字段长度和格式
     */
    public void validateFields() {
        if (avatarUrl != null && avatarUrl.length() > 500) {
            throw new IllegalArgumentException("头像URL长度不能超过500字符");
        }
        
        if (bio != null && bio.length() > 500) {
            throw new IllegalArgumentException("个人简介长度不能超过500字符");
        }
        
        if (email != null) {
            if (email.length() > 100) {
                throw new IllegalArgumentException("邮箱长度不能超过100字符");
            }
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new IllegalArgumentException("邮箱格式不正确");
            }
        }
        
        if (role != null && !role.matches("^(USER|ADMIN|MODERATOR)$")) {
            throw new IllegalArgumentException("用户角色必须是USER、ADMIN或MODERATOR之一");
        }
        
        if (updateReason != null && updateReason.length() > 200) {
            throw new IllegalArgumentException("更新原因长度不能超过200字符");
        }
    }
    
    @Override
    public String toString() {
        return String.format("UserDataUpdateDTO{avatarUrl='%s', bio='%s', email='%s', role='%s', updateReason='%s'}", 
                           avatarUrl != null ? "[SET]" : null, 
                           bio != null ? "[SET]" : null,
                           email != null ? "[MASKED]" : null, 
                           role, 
                           updateReason);
    }
}