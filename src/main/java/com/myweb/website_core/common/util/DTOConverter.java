package com.myweb.website_core.common.util;

import com.myweb.website_core.application.service.security.DataMaskingService;
import com.myweb.website_core.application.service.business.UserFollowService;
import com.myweb.website_core.domain.business.dto.CommentDTO;
import com.myweb.website_core.domain.business.dto.UserProfileDTO;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.domain.business.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据脱敏DTO转换工具类
 * <p>
 * 提供集成数据脱敏功能的DTO转换方法，包括：
 * - 用户实体到DTO的脱敏转换
 * - 批量数据脱敏转换
 * - 基于权限的动态脱敏
 * - 审计日志数据脱敏
 * <p>
 * 符合需求：7.5 - 集成到现有DTO转换中
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DTOConverter {
    
    private final DataMaskingService dataMaskingService;
    private final UserFollowService userFollowService;
    
    // ==================== 用户DTO转换 ====================
    
    /**
     * 将用户实体转换为用户资料DTO（带脱敏）
     * 
     * @param user 用户实体
     * @param targetUserId 目标用户ID（用于权限判断）
     * @return 脱敏后的用户资料DTO
     */
    public UserProfileDTO convertToUserProfileDTO(User user, Long targetUserId) {
        if (user == null) {
            return null;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setAvatarUrl(user.getAvatarUrl());
            dto.setBio(user.getBio());
            dto.setLikedCount(user.getLikedCount());
            
            // 设置关注数据
            dto.setFollowersCount((int) userFollowService.getFollowersCount(user.getId()));
            dto.setFollowingCount((int) userFollowService.getFollowingCount(user.getId()));
            
            // 脱敏敏感信息
            if (user.getEmail() != null) {
                String maskedEmail = dataMaskingService.maskByCurrentUserPermission(
                    user.getEmail(), "email", targetUserId);
                dto.setEmail(maskedEmail);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("DataMaskingDTOConverter: 用户DTO转换完成，耗时: {}ms", executionTime);
            
            return dto;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("DataMaskingDTOConverter: 用户DTO转换失败，耗时: {}ms, 错误: {}", 
                     executionTime, e.getMessage());
            throw new RuntimeException("用户DTO转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 将用户实体转换为用户资料DTO（指定权限）
     * 
     * @param user 用户实体
     * @param hasFullAccess 是否有完整访问权限
     * @return 脱敏后的用户资料DTO
     */
    public UserProfileDTO convertToUserProfileDTO(User user, boolean hasFullAccess) {
        if (user == null) {
            return null;
        }
        
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setLikedCount(user.getLikedCount());
        
        // 设置关注数据
        dto.setFollowersCount((int) userFollowService.getFollowersCount(user.getId()));
        dto.setFollowingCount((int) userFollowService.getFollowingCount(user.getId()));
        
        // 脱敏敏感信息
        if (user.getEmail() != null) {
            String maskedEmail = dataMaskingService.maskByPermission(
                user.getEmail(), "email", hasFullAccess);
            dto.setEmail(maskedEmail);
        }
        
        return dto;
    }
    /**
     * 转换为DTO
     */

    public static CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());

        CommentDTO.AuthorInfo authorInfo = new CommentDTO.AuthorInfo();
        authorInfo.setId(comment.getAuthor().getId());
        authorInfo.setUsername(comment.getAuthor().getUsername());
        authorInfo.setAvatarUrl(comment.getAuthor().getAvatarUrl());
        dto.setAuthor(authorInfo);

        // 设置回复
        // 设置回复
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDTO> replies = comment.getReplies().stream()
                    .map(DTOConverter::convertToDTO)  // 使用类名引用静态方法
                    .toList();
            dto.setReplies(replies);
        }


        return dto;
    }
    
    // ==================== 批量转换方法 ====================
    
    /**
     * 批量转换用户数据并脱敏
     * 
     * @param userData 用户数据映射
     * @param targetUserId 目标用户ID
     * @return 脱敏后的用户数据
     */
    public Map<String, String> convertUserDataWithMasking(Map<String, String> userData, Long targetUserId) {
        if (userData == null || userData.isEmpty()) {
            return userData;
        }
        
        // 定义数据类型映射
        Map<String, String> dataTypeMap = new HashMap<>();
        dataTypeMap.put("email", "email");
        dataTypeMap.put("phone", "phone");
        dataTypeMap.put("mobile", "phone");
        dataTypeMap.put("username", "username");
        dataTypeMap.put("idCard", "idcard");
        
        // 判断权限
        boolean hasFullAccess = determineFullAccess(targetUserId);
        
        // 批量脱敏
        return dataMaskingService.maskBatch(userData, dataTypeMap, hasFullAccess);
    }
    
    /**
     * 转换审计日志数据并脱敏
     * 
     * @param logData 日志数据
     * @return 脱敏后的日志数据
     */
    public Map<String, String> convertAuditLogDataWithMasking(Map<String, String> logData) {
        if (logData == null || logData.isEmpty()) {
            return logData;
        }
        
        Map<String, String> maskedData = new HashMap<>();
        
        for (Map.Entry<String, String> entry : logData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 根据键名判断数据类型并脱敏
            String maskedValue = maskLogDataByKey(key, value);
            maskedData.put(key, maskedValue);
        }
        
        return maskedData;
    }
    
    /**
     * 转换导出数据并脱敏
     * 
     * @param exportData 导出数据
     * @param hasExportPermission 是否有导出权限
     * @return 脱敏后的导出数据
     */
    public Map<String, String> convertExportDataWithMasking(Map<String, String> exportData, 
                                                           boolean hasExportPermission) {
        if (exportData == null || exportData.isEmpty()) {
            return exportData;
        }
        
        // 如果有导出权限，可能返回部分脱敏或完整数据
        // 如果没有导出权限，进行完全脱敏
        Map<String, String> dataTypeMap = createDataTypeMapping(exportData.keySet());
        
        return dataMaskingService.maskBatch(exportData, dataTypeMap, hasExportPermission);
    }
    
    // ==================== 私有工具方法 ====================
    
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
                return true;
            }
            
            // 如果是管理员，有完整权限
            if (PermissionUtils.isAdmin()) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.warn("DataMaskingDTOConverter: 判断权限时发生异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据键名脱敏日志数据
     * 
     * @param key 键名
     * @param value 值
     * @return 脱敏后的值
     */
    private String maskLogDataByKey(String key, String value) {
        if (key == null || value == null) {
            return value;
        }
        
        String lowerKey = key.toLowerCase();
        
        // 根据键名判断数据类型
        if (lowerKey.contains("email")) {
            return dataMaskingService.maskByPermission(value, "email", false);
        } else if (lowerKey.contains("phone") || lowerKey.contains("mobile")) {
            return dataMaskingService.maskByPermission(value, "phone", false);
        } else if (lowerKey.contains("username") || lowerKey.contains("user")) {
            return dataMaskingService.maskByPermission(value, "username", false);
        } else if (lowerKey.contains("password") || lowerKey.contains("token") || 
                   lowerKey.contains("secret") || lowerKey.contains("key")) {
            return "***"; // 完全脱敏
        } else if (lowerKey.contains("idcard") || lowerKey.contains("id_card")) {
            return dataMaskingService.maskByPermission(value, "idcard", false);
        }
        
        return value; // 其他数据不脱敏
    }
    
    /**
     * 创建数据类型映射
     * 
     * @param keySet 键集合
     * @return 数据类型映射
     */
    private Map<String, String> createDataTypeMapping(java.util.Set<String> keySet) {
        Map<String, String> dataTypeMap = new HashMap<>();
        
        for (String key : keySet) {
            String lowerKey = key.toLowerCase();
            
            if (lowerKey.contains("email")) {
                dataTypeMap.put(key, "email");
            } else if (lowerKey.contains("phone") || lowerKey.contains("mobile")) {
                dataTypeMap.put(key, "phone");
            } else if (lowerKey.contains("username")) {
                dataTypeMap.put(key, "username");
            } else if (lowerKey.contains("idcard") || lowerKey.contains("id_card")) {
                dataTypeMap.put(key, "idcard");
            } else {
                dataTypeMap.put(key, "unknown");
            }
        }
        
        return dataTypeMap;
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证DTO转换的脱敏效果
     * 
     * @param originalUser 原始用户实体
     * @param maskedDTO 脱敏后的DTO
     * @param hasFullAccess 是否有完整访问权限
     * @return 验证结果
     */
    public boolean validateDTOMaskingEffect(User originalUser, UserProfileDTO maskedDTO, boolean hasFullAccess) {
        if (originalUser == null || maskedDTO == null) {
            return false;
        }
        
        try {
            // 验证邮箱脱敏效果
            if (originalUser.getEmail() != null && maskedDTO.getEmail() != null) {
                if (hasFullAccess) {
                    // 有完整权限时，应该返回原始邮箱
                    if (!originalUser.getEmail().equals(maskedDTO.getEmail())) {
                        log.warn("DataMaskingDTOConverter: 邮箱脱敏验证失败 - 有完整权限但返回了脱敏数据");
                        return false;
                    }
                } else {
                    // 无完整权限时，应该返回脱敏邮箱
                    if (originalUser.getEmail().equals(maskedDTO.getEmail())) {
                        log.warn("DataMaskingDTOConverter: 邮箱脱敏验证失败 - 无完整权限但返回了原始数据");
                        return false;
                    }
                }
            }
            
            // 验证其他字段是否正确复制
            if (!originalUser.getId().equals(maskedDTO.getId()) ||
                !originalUser.getUsername().equals(maskedDTO.getUsername()) ||
                !originalUser.getLikedCount().equals(maskedDTO.getLikedCount())) {
                log.warn("DataMaskingDTOConverter: DTO转换验证失败 - 基本字段复制错误");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("DataMaskingDTOConverter: DTO脱敏效果验证时发生异常: {}", e.getMessage());
            return false;
        }
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 获取转换统计信息
     * 
     * @return 转换统计信息
     */
    public Map<String, Object> getConversionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("dataMaskingEnabled", dataMaskingService.isDataMaskingEnabled());
        stats.put("supportedConversions", new String[]{"UserProfileDTO", "AuditLogData", "ExportData"});
        stats.put("supportedDataTypes", new String[]{"email", "phone", "username", "idcard"});
        return stats;
    }
}