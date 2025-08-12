package com.myweb.website_core.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据脱敏配置类
 * <p>
 * 管理数据脱敏的配置规则，包括：
 * - 脱敏功能开关
 * - 各种数据类型的脱敏级别
 * - 脱敏字符配置
 * - 权限相关配置
 * - 自定义脱敏规则
 * <p>
 * 符合需求：7.3 - 创建脱敏规则配置管理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security.data-masking")
public class DataMaskingConfig {
    
    // ==================== 基础配置 ====================
    
    /**
     * 数据脱敏功能开关
     */
    private boolean enabled = true;
    
    /**
     * 脱敏字符
     */
    private String maskChar = "*";
    
    /**
     * 管理员是否可以查看完整信息
     */
    private boolean adminFullAccess = true;
    
    /**
     * 用户是否可以查看自己的完整信息
     */
    private boolean selfFullAccess = true;
    
    // ==================== 脱敏级别配置 ====================
    
    /**
     * 邮箱脱敏级别
     * none: 不脱敏
     * partial: 部分脱敏
     * full: 完全脱敏
     */
    private String emailMaskLevel = "partial";
    
    /**
     * 手机号脱敏级别
     */
    private String phoneMaskLevel = "partial";
    
    /**
     * 用户名脱敏级别
     */
    private String usernameMaskLevel = "partial";
    
    /**
     * 身份证号脱敏级别
     */
    private String idCardMaskLevel = "partial";
    
    // ==================== 高级配置 ====================
    
    /**
     * 自定义脱敏规则
     * 键：数据类型，值：脱敏级别
     */
    private Map<String, String> customRules = new HashMap<>();
    
    /**
     * 敏感字段列表
     * 在日志和导出中需要脱敏的字段名
     */
    private String[] sensitiveFields = {
        "email", "phone", "mobile", "idCard", "password", "token", "secret"
    };
    
    /**
     * 脱敏长度配置
     */
    private MaskLengthConfig maskLength = new MaskLengthConfig();
    
    /**
     * 角色权限配置
     */
    private RolePermissionConfig rolePermission = new RolePermissionConfig();
    
    // ==================== 内部配置类 ====================
    
    /**
     * 脱敏长度配置
     */
    @Data
    public static class MaskLengthConfig {
        /**
         * 邮箱用户名保留前缀长度
         */
        private int emailPrefixLength = 2;
        
        /**
         * 邮箱用户名保留后缀长度
         */
        private int emailSuffixLength = 2;
        
        /**
         * 手机号保留前缀长度
         */
        private int phonePrefixLength = 3;
        
        /**
         * 手机号保留后缀长度
         */
        private int phoneSuffixLength = 4;
        
        /**
         * 用户名保留前缀长度
         */
        private int usernamePrefixLength = 2;
        
        /**
         * 用户名保留后缀长度
         */
        private int usernameSuffixLength = 2;
        
        /**
         * 身份证号保留前缀长度
         */
        private int idCardPrefixLength = 4;
        
        /**
         * 身份证号保留后缀长度
         */
        private int idCardSuffixLength = 4;
    }
    
    /**
     * 角色权限配置
     */
    @Data
    public static class RolePermissionConfig {
        /**
         * 可以查看完整邮箱的角色
         */
        private String[] fullEmailAccessRoles = {"ADMIN", "SUPER_ADMIN"};
        
        /**
         * 可以查看完整手机号的角色
         */
        private String[] fullPhoneAccessRoles = {"ADMIN", "SUPER_ADMIN"};
        
        /**
         * 可以查看所有用户完整信息的角色
         */
        private String[] fullUserAccessRoles = {"SUPER_ADMIN"};
        
        /**
         * 可以导出完整数据的角色
         */
        private String[] dataExportRoles = {"ADMIN", "SUPER_ADMIN"};
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取指定数据类型的脱敏级别
     * 
     * @param dataType 数据类型
     * @return 脱敏级别
     */
    public String getMaskLevel(String dataType) {
        if (dataType == null) {
            return "partial";
        }
        
        // 首先检查自定义规则
        if (customRules.containsKey(dataType.toLowerCase())) {
            return customRules.get(dataType.toLowerCase());
        }
        
        // 然后检查预定义规则
        return switch (dataType.toLowerCase()) {
            case "email" -> emailMaskLevel;
            case "phone", "mobile" -> phoneMaskLevel;
            case "username" -> usernameMaskLevel;
            case "idcard" -> idCardMaskLevel;
            default -> "partial";
        };
    }
    
    /**
     * 检查字段是否为敏感字段
     * 
     * @param fieldName 字段名
     * @return 是否为敏感字段
     */
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        for (String sensitiveField : sensitiveFields) {
            if (lowerFieldName.contains(sensitiveField.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查角色是否有完整邮箱访问权限
     * 
     * @param roleName 角色名
     * @return 是否有权限
     */
    public boolean hasFullEmailAccess(String roleName) {
        if (roleName == null) {
            return false;
        }
        
        for (String role : rolePermission.fullEmailAccessRoles) {
            if (role.equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查角色是否有完整手机号访问权限
     * 
     * @param roleName 角色名
     * @return 是否有权限
     */
    public boolean hasFullPhoneAccess(String roleName) {
        if (roleName == null) {
            return false;
        }
        
        for (String role : rolePermission.fullPhoneAccessRoles) {
            if (role.equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查角色是否有完整用户信息访问权限
     * 
     * @param roleName 角色名
     * @return 是否有权限
     */
    public boolean hasFullUserAccess(String roleName) {
        if (roleName == null) {
            return false;
        }
        
        for (String role : rolePermission.fullUserAccessRoles) {
            if (role.equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查角色是否有数据导出权限
     * 
     * @param roleName 角色名
     * @return 是否有权限
     */
    public boolean hasDataExportAccess(String roleName) {
        if (roleName == null) {
            return false;
        }
        
        for (String role : rolePermission.dataExportRoles) {
            if (role.equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 添加自定义脱敏规则
     * 
     * @param dataType 数据类型
     * @param maskLevel 脱敏级别
     */
    public void addCustomRule(String dataType, String maskLevel) {
        if (dataType != null && maskLevel != null) {
            customRules.put(dataType.toLowerCase(), maskLevel.toLowerCase());
        }
    }
    
    /**
     * 移除自定义脱敏规则
     * 
     * @param dataType 数据类型
     */
    public void removeCustomRule(String dataType) {
        if (dataType != null) {
            customRules.remove(dataType.toLowerCase());
        }
    }
    
    /**
     * 获取配置摘要
     * 
     * @return 配置摘要
     */
    public Map<String, Object> getConfigSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("enabled", enabled);
        summary.put("maskChar", maskChar);
        summary.put("adminFullAccess", adminFullAccess);
        summary.put("selfFullAccess", selfFullAccess);
        summary.put("emailMaskLevel", emailMaskLevel);
        summary.put("phoneMaskLevel", phoneMaskLevel);
        summary.put("usernameMaskLevel", usernameMaskLevel);
        summary.put("idCardMaskLevel", idCardMaskLevel);
        summary.put("customRulesCount", customRules.size());
        summary.put("sensitiveFieldsCount", sensitiveFields.length);
        return summary;
    }
}