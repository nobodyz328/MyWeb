package com.myweb.website_core.domain.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据导出DTO
 * 
 * 用于用户数据的查看和导出功能
 * 支持敏感信息的脱敏处理
 * 符合GB/T 22239-2019二级等保要求的个人信息保护机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDataExportDTO {

    // ==================== 基本信息 ====================
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱地址（可能脱敏）
     */
    private String email;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 个人简介
     */
    private String bio;
    
    // ==================== 统计信息 ====================
    
    /**
     * 获赞数
     */
    private Integer likedCount;
    
    /**
     * 关注数
     */
    private Integer followingCount;
    
    /**
     * 粉丝数
     */
    private Integer followersCount;
    
    // ==================== 安全信息（敏感数据） ====================
    
    /**
     * 邮箱验证状态
     * 仅用户本人或管理员可见
     */
    private Boolean emailVerified;
    
    /**
     * 用户角色
     * 仅用户本人或管理员可见
     */
    private String role;
    
    /**
     * TOTP启用状态
     * 仅用户本人或管理员可见
     */
    private Boolean totpEnabled;
    
    /**
     * 登录失败次数
     * 仅管理员可见
     */
    private Integer loginAttempts;
    
    /**
     * 最后登录时间
     * 仅管理员可见
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    
    /**
     * 最后登录IP
     * 仅管理员可见
     */
    private String lastLoginIp;
    
    /**
     * 账户锁定状态
     * 仅管理员可见
     */
    private Boolean accountLocked;
    
    // ==================== 时间信息 ====================
    
    /**
     * 创建时间
     * 仅用户本人或管理员可见
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * 仅用户本人或管理员可见
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // ==================== 元数据信息 ====================
    
    /**
     * 数据导出时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exportTime = LocalDateTime.now();
    
    /**
     * 数据版本
     */
    private String dataVersion = "1.0";
    
    /**
     * 是否包含敏感数据
     */
    private Boolean containsSensitiveData;
    
    /**
     * 数据完整性标识
     */
    private String dataIntegrityHash;
    
    // ==================== 构造方法 ====================
    
    public UserDataExportDTO() {
        this.exportTime = LocalDateTime.now();
        this.dataVersion = "1.0";
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 标记为包含敏感数据
     */
    public void markAsSensitive() {
        this.containsSensitiveData = true;
    }
    
    /**
     * 标记为不包含敏感数据
     */
    public void markAsPublic() {
        this.containsSensitiveData = false;
    }
    
    /**
     * 检查是否包含敏感数据
     */
    public boolean hasSensitiveData() {
        return Boolean.TRUE.equals(containsSensitiveData);
    }
    
    /**
     * 设置数据完整性哈希
     */
    public void setDataIntegrityHash(String hash) {
        this.dataIntegrityHash = hash;
    }
    
    /**
     * 获取数据摘要信息
     */
    public String getDataSummary() {
        return String.format("用户数据导出 - ID: %d, 用户名: %s, 导出时间: %s, 版本: %s", 
                           id, username, exportTime, dataVersion);
    }
    
    @Override
    public String toString() {
        return String.format("UserDataExportDTO{id=%d, username='%s', email='%s', exportTime=%s}", 
                           id, username, email, exportTime);
    }
}