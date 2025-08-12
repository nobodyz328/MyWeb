package com.myweb.website_core.application.service.security;

import com.myweb.website_core.common.util.PermissionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务
 * <p>
 * 提供统一的数据脱敏功能，包括：
 * - 邮箱地址脱敏
 * - 手机号码脱敏
 * - 基于权限级别的脱敏策略
 * - 可配置的脱敏规则
 * - 批量数据脱敏
 * <p>
 *  数据脱敏服务业务集成
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
public class DataMaskingService {
    
    // ==================== 配置属性 ====================
    
    /**
     * 数据脱敏功能开关
     */
    @Value("${app.security.data-masking.enabled:true}")
    private boolean dataMaskingEnabled;
    
    /**
     * 邮箱脱敏级别
     */
    @Value("${app.security.data-masking.email-mask-level:partial}")
    private String emailMaskLevel;
    
    /**
     * 手机号脱敏级别
     */
    @Value("${app.security.data-masking.phone-mask-level:partial}")
    private String phoneMaskLevel;
    
    /**
     * 默认脱敏字符
     */
    @Value("${app.security.data-masking.mask-char:*}")
    private String maskChar;
    
    /**
     * 管理员是否可以查看完整信息
     */
    @Value("${app.security.data-masking.admin-full-access:true}")
    private boolean adminFullAccess;
    
    // ==================== 常量定义 ====================
    
    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    /**
     * 手机号正则表达式（中国大陆）
     */
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * 脱敏级别常量
     */
    private static final String MASK_LEVEL_NONE = "none";
    private static final String MASK_LEVEL_PARTIAL = "partial";
    private static final String MASK_LEVEL_FULL = "full";
    
    /**
     * 默认脱敏字符
     */
    private static final String DEFAULT_MASK = "***";
    
    // ==================== 核心脱敏方法 ====================
    
    /**
     * 脱敏邮箱地址
     * 
     * @param email 原始邮箱地址
     * @return 脱敏后的邮箱地址
     */
    public String maskEmail(String email) {
        if (!dataMaskingEnabled || email == null || email.isEmpty()) {
            return email;
        }
        
        // 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.warn("DataMaskingService: 无效的邮箱格式: {}", email);
            return email;
        }
        
        return switch (emailMaskLevel.toLowerCase()) {
            case MASK_LEVEL_NONE -> email;
            case MASK_LEVEL_FULL -> DEFAULT_MASK + "@" + DEFAULT_MASK;
            case MASK_LEVEL_PARTIAL -> maskEmailPartial(email);
            default -> maskEmailPartial(email);
        };
    }
    
    /**
     * 脱敏手机号码
     * 
     * @param phoneNumber 原始手机号码
     * @return 脱敏后的手机号码
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (!dataMaskingEnabled || phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        
        // 验证手机号格式
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            log.warn("DataMaskingService: 无效的手机号格式: {}", phoneNumber);
            return phoneNumber;
        }
        
        return switch (phoneMaskLevel.toLowerCase()) {
            case MASK_LEVEL_NONE -> phoneNumber;
            case MASK_LEVEL_FULL -> DEFAULT_MASK;
            case MASK_LEVEL_PARTIAL -> maskPhonePartial(phoneNumber);
            default -> maskPhonePartial(phoneNumber);
        };
    }
    
    /**
     * 根据权限级别脱敏数据
     * 
     * @param data 原始数据
     * @param dataType 数据类型（email, phone等）
     * @param hasFullAccess 是否有完整访问权限
     * @return 脱敏后的数据
     */
    public String maskByPermission(String data, String dataType, boolean hasFullAccess) {
        if (!dataMaskingEnabled || data == null || data.isEmpty()) {
            return data;
        }
        
        // 如果有完整访问权限，直接返回原数据
        if (hasFullAccess) {
            log.debug("DataMaskingService: 用户有完整访问权限，返回原始数据");
            return data;
        }
        
        // 根据数据类型进行脱敏
        return switch (dataType.toLowerCase()) {
            case "email" -> maskEmail(data);
            case "phone", "mobile" -> maskPhoneNumber(data);
            case "username" -> maskUsername(data);
            case "idcard" -> maskIdCard(data);
            default -> {
                log.warn("DataMaskingService: 未知的数据类型: {}", dataType);
                yield data;
            }
        };
    }
    
    /**
     * 根据当前用户权限脱敏数据
     * 
     * @param data 原始数据
     * @param dataType 数据类型
     * @param targetUserId 目标用户ID（用于判断是否为本人数据）
     * @return 脱敏后的数据
     */
    public String maskByCurrentUserPermission(String data, String dataType, Long targetUserId) {
        if (!dataMaskingEnabled || data == null || data.isEmpty()) {
            return data;
        }
        
        // 获取当前用户权限
        boolean hasFullAccess = determineFullAccess(targetUserId);
        
        return maskByPermission(data, dataType, hasFullAccess);
    }
    
    /**
     * 批量脱敏数据
     * 
     * @param dataMap 数据映射（字段名 -> 数据值）
     * @param dataTypeMap 数据类型映射（字段名 -> 数据类型）
     * @param hasFullAccess 是否有完整访问权限
     * @return 脱敏后的数据映射
     */
    public Map<String, String> maskBatch(Map<String, String> dataMap, 
                                        Map<String, String> dataTypeMap, 
                                        boolean hasFullAccess) {
        if (!dataMaskingEnabled || dataMap == null || dataMap.isEmpty()) {
            return dataMap;
        }
        
        Map<String, String> maskedData = new HashMap<>();
        
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            String dataType = dataTypeMap.getOrDefault(fieldName, "unknown");
            
            String maskedValue = maskByPermission(fieldValue, dataType, hasFullAccess);
            maskedData.put(fieldName, maskedValue);
        }
        
        log.debug("DataMaskingService: 批量脱敏完成，处理字段数: {}", dataMap.size());
        return maskedData;
    }
    
    // ==================== 私有脱敏方法 ====================
    
    /**
     * 部分脱敏邮箱地址
     * 
     * @param email 邮箱地址
     * @return 部分脱敏后的邮箱地址
     */
    private String maskEmailPartial(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return username.charAt(0) + maskChar + domain;
        } else if (username.length() <= 4) {
            return username.charAt(0) + maskChar.repeat(2) + username.charAt(username.length() - 1) + domain;
        } else {
            return username.substring(0, 2) + maskChar.repeat(3) + 
                   username.substring(username.length() - 2) + domain;
        }
    }
    
    /**
     * 部分脱敏手机号码
     * 
     * @param phoneNumber 手机号码
     * @return 部分脱敏后的手机号码
     */
    private String maskPhonePartial(String phoneNumber) {
        if (phoneNumber.length() != 11) {
            return phoneNumber;
        }
        
        return phoneNumber.substring(0, 3) + maskChar.repeat(4) + phoneNumber.substring(7);
    }
    
    /**
     * 脱敏用户名
     * 
     * @param username 用户名
     * @return 脱敏后的用户名
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return username;
        }
        
        if (username.length() <= 4) {
            return username.charAt(0) + maskChar.repeat(2);
        } else {
            return username.substring(0, 2) + maskChar.repeat(3) + 
                   username.substring(username.length() - 2);
        }
    }
    
    /**
     * 脱敏身份证号
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return DEFAULT_MASK;
        }
        
        return idCard.substring(0, 4) + maskChar.repeat(10) + 
               idCard.substring(idCard.length() - 4);
    }
    
    // ==================== 权限判断方法 ====================
    
    /**
     * 判断是否有完整访问权限
     * 
     * @param targetUserId 目标用户ID
     * @return 是否有完整访问权限
     */
    private boolean determineFullAccess(Long targetUserId) {
        try {
            // 获取当前用户ID
            Long currentUserId = PermissionUtils.getCurrentUserId();
            
            // 如果是查看自己的数据，有完整权限
            if (currentUserId != null && currentUserId.equals(targetUserId)) {
                log.debug("DataMaskingService: 用户查看自己的数据，有完整权限");
                return true;
            }
            
            // 如果是管理员且配置允许管理员查看完整信息
            if (adminFullAccess && PermissionUtils.isAdmin()) {
                log.debug("DataMaskingService: 管理员用户，有完整权限");
                return true;
            }
            
            // 其他情况没有完整权限
            log.debug("DataMaskingService: 普通用户查看他人数据，无完整权限");
            return false;
            
        } catch (Exception e) {
            log.warn("DataMaskingService: 判断权限时发生异常: {}", e.getMessage());
            // 异常情况下，为了安全起见，不给予完整权限
            return false;
        }
    }
    
    // ==================== 配置管理方法 ====================
    
    /**
     * 检查数据脱敏是否启用
     * 
     * @return 数据脱敏是否启用
     */
    public boolean isDataMaskingEnabled() {
        return dataMaskingEnabled;
    }
    
    /**
     * 获取邮箱脱敏级别
     * 
     * @return 邮箱脱敏级别
     */
    public String getEmailMaskLevel() {
        return emailMaskLevel;
    }
    
    /**
     * 获取手机号脱敏级别
     * 
     * @return 手机号脱敏级别
     */
    public String getPhoneMaskLevel() {
        return phoneMaskLevel;
    }
    
    /**
     * 获取脱敏配置信息
     * 
     * @return 脱敏配置信息
     */
    public Map<String, Object> getMaskingConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", dataMaskingEnabled);
        config.put("emailMaskLevel", emailMaskLevel);
        config.put("phoneMaskLevel", phoneMaskLevel);
        config.put("maskChar", maskChar);
        config.put("adminFullAccess", adminFullAccess);
        return config;
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证脱敏效果
     * 
     * @param originalData 原始数据
     * @param maskedData 脱敏后数据
     * @param dataType 数据类型
     * @return 验证结果
     */
    public boolean validateMaskingEffect(String originalData, String maskedData, String dataType) {
        if (originalData == null || maskedData == null) {
            return false;
        }
        
        // 如果脱敏功能未启用，脱敏后数据应该与原数据相同
        if (!dataMaskingEnabled) {
            return originalData.equals(maskedData);
        }
        
        // 根据数据类型验证脱敏效果
        return switch (dataType.toLowerCase()) {
            case "email" -> validateEmailMasking(originalData, maskedData);
            case "phone", "mobile" -> validatePhoneMasking(originalData, maskedData);
            default -> !originalData.equals(maskedData); // 基本验证：脱敏后应该不同
        };
    }
    
    /**
     * 验证邮箱脱敏效果
     * 
     * @param originalEmail 原始邮箱
     * @param maskedEmail 脱敏后邮箱
     * @return 验证结果
     */
    private boolean validateEmailMasking(String originalEmail, String maskedEmail) {
        if (emailMaskLevel.equals(MASK_LEVEL_NONE)) {
            return originalEmail.equals(maskedEmail);
        }
        
        if (emailMaskLevel.equals(MASK_LEVEL_FULL)) {
            return maskedEmail.contains(DEFAULT_MASK);
        }
        
        // 部分脱敏验证
        return maskedEmail.contains(maskChar) && 
               maskedEmail.contains("@") && 
               !originalEmail.equals(maskedEmail);
    }
    
    /**
     * 验证手机号脱敏效果
     * 
     * @param originalPhone 原始手机号
     * @param maskedPhone 脱敏后手机号
     * @return 验证结果
     */
    private boolean validatePhoneMasking(String originalPhone, String maskedPhone) {
        if (phoneMaskLevel.equals(MASK_LEVEL_NONE)) {
            return originalPhone.equals(maskedPhone);
        }
        
        if (phoneMaskLevel.equals(MASK_LEVEL_FULL)) {
            return maskedPhone.equals(DEFAULT_MASK);
        }
        
        // 部分脱敏验证
        return maskedPhone.contains(maskChar) && 
               !originalPhone.equals(maskedPhone);
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 获取脱敏统计信息
     * 
     * @return 脱敏统计信息
     */
    public Map<String, Object> getMaskingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", dataMaskingEnabled);
        stats.put("supportedTypes", new String[]{"email", "phone", "username", "idcard"});
        stats.put("maskLevels", new String[]{MASK_LEVEL_NONE, MASK_LEVEL_PARTIAL, MASK_LEVEL_FULL});
        stats.put("currentEmailLevel", emailMaskLevel);
        stats.put("currentPhoneLevel", phoneMaskLevel);
        return stats;
    }
}