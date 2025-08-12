package com.myweb.website_core.application.service.security;

import com.myweb.website_core.common.exception.SecurityErrorResponse;
import com.myweb.website_core.common.util.PermissionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 安全异常信息脱敏服务
 * <p>
 * 提供异常信息的安全脱敏功能，包括：
 * - 敏感信息脱敏
 * - 技术细节隐藏
 * - 基于权限的信息展示
 * - 安全日志脱敏
 * <p>
 * 符合需求：1.6, 2.6, 3.4, 4.6 - 异常信息的安全脱敏
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityExceptionMaskingService {
    
    // ==================== 配置属性 ====================
    
    /**
     * 异常脱敏功能开关
     */
    @Value("${app.security.exception-masking.enabled:true}")
    private boolean maskingEnabled;
    
    /**
     * 是否对普通用户隐藏技术细节
     */
    @Value("${app.security.exception-masking.hide-technical-details:true}")
    private boolean hideTechnicalDetails;
    
    /**
     * 管理员是否可以查看完整异常信息
     */
    @Value("${app.security.exception-masking.admin-full-access:true}")
    private boolean adminFullAccess;
    
    /**
     * 脱敏字符
     */
    @Value("${app.security.exception-masking.mask-char:*}")
    private String maskChar;
    
    // ==================== 敏感信息模式 ====================
    
    /**
     * 邮箱地址模式
     */
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    
    /**
     * 手机号模式
     */
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("\\b1[3-9]\\d{9}\\b");
    
    /**
     * IP地址模式
     */
    private static final Pattern IP_PATTERN = 
        Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");
    
    /**
     * 身份证号模式
     */
    private static final Pattern ID_CARD_PATTERN = 
        Pattern.compile("\\b[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]\\b");
    
    /**
     * SQL语句模式
     */
    private static final Pattern SQL_PATTERN = 
        Pattern.compile("(?i)\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION)\\b.*");
    
    /**
     * 文件路径模式
     */
    private static final Pattern FILE_PATH_PATTERN = 
        Pattern.compile("(?i)[a-z]:\\\\[^\\s<>:\"|?*]+|/[^\\s<>:\"|?*]+");
    
    /**
     * 密码相关模式
     */
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("(?i)(password|pwd|pass|secret|key)\\s*[=:]\\s*[^\\s]+");
    
    // ==================== 核心脱敏方法 ====================
    
    /**
     * 脱敏安全错误响应
     * 
     * @param errorResponse 原始错误响应
     * @param isAdminUser 是否为管理员用户
     * @return 脱敏后的错误响应
     */
    public SecurityErrorResponse maskSecurityErrorResponse(SecurityErrorResponse errorResponse, boolean isAdminUser) {
        if (!maskingEnabled) {
            return errorResponse;
        }
        
        // 如果是管理员且配置允许完整访问，返回原始信息
        if (isAdminUser && adminFullAccess) {
            log.debug("SecurityExceptionMaskingService: 管理员用户，返回完整异常信息");
            return errorResponse;
        }
        
        // 创建脱敏后的响应
        SecurityErrorResponse maskedResponse = SecurityErrorResponse.builder()
                .errorCode(errorResponse.getErrorCode())
                .message(maskMessage(errorResponse.getMessage(), isAdminUser))
                .details(maskDetails(errorResponse.getDetails(), isAdminUser))
                .category(errorResponse.getCategory())
                .severity(errorResponse.getSeverity())
                .timestamp(errorResponse.getTimestamp())
                .path(maskPath(errorResponse.getPath()))
                .status(errorResponse.getStatus())
                .requestId(errorResponse.getRequestId())
                .requiresUserAction(errorResponse.isRequiresUserAction())
                .suggestedAction(errorResponse.getSuggestedAction())
                .errorData(maskErrorData(errorResponse.getErrorData(), isAdminUser))
                .securityRelated(errorResponse.isSecurityRelated())
                .build();
        
        log.debug("SecurityExceptionMaskingService: 异常信息脱敏完成 - 错误代码: {}", errorResponse.getErrorCode());
        return maskedResponse;
    }
    
    /**
     * 脱敏异常消息
     * 
     * @param message 原始消息
     * @param isAdminUser 是否为管理员用户
     * @return 脱敏后的消息
     */
    public String maskMessage(String message, boolean isAdminUser) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        String maskedMessage = message;
        
        // 脱敏敏感信息
        maskedMessage = maskSensitiveInfo(maskedMessage);
        
        // 如果不是管理员且配置要求隐藏技术细节
        if (!isAdminUser && hideTechnicalDetails) {
            maskedMessage = hideTechnicalDetails(maskedMessage);
        }
        
        return maskedMessage;
    }
    
    /**
     * 脱敏异常详情
     * 
     * @param details 原始详情
     * @param isAdminUser 是否为管理员用户
     * @return 脱敏后的详情
     */
    public String maskDetails(String details, boolean isAdminUser) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        
        // 如果不是管理员且配置要求隐藏技术细节，返回通用消息
        if (!isAdminUser && hideTechnicalDetails) {
            return "技术详情已隐藏，如需查看请联系系统管理员";
        }
        
        // 脱敏敏感信息
        return maskSensitiveInfo(details);
    }
    
    /**
     * 脱敏错误数据
     * 
     * @param errorData 原始错误数据
     * @param isAdminUser 是否为管理员用户
     * @return 脱敏后的错误数据
     */
    public Map<String, Object> maskErrorData(Map<String, Object> errorData, boolean isAdminUser) {
        if (errorData == null || errorData.isEmpty()) {
            return errorData;
        }
        
        Map<String, Object> maskedData = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : errorData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 检查是否为敏感字段
            if (isSensitiveField(key)) {
                if (isAdminUser && adminFullAccess) {
                    maskedData.put(key, value);
                } else {
                    maskedData.put(key, maskValue(value));
                }
            } else {
                // 非敏感字段，但仍需要脱敏内容
                if (value instanceof String) {
                    maskedData.put(key, maskSensitiveInfo((String) value));
                } else {
                    maskedData.put(key, value);
                }
            }
        }
        
        return maskedData;
    }
    
    /**
     * 脱敏异常堆栈信息
     * 
     * @param stackTrace 原始堆栈信息
     * @param isAdminUser 是否为管理员用户
     * @return 脱敏后的堆栈信息
     */
    public String maskStackTrace(String stackTrace, boolean isAdminUser) {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return stackTrace;
        }
        
        // 如果不是管理员且配置要求隐藏技术细节
        if (!isAdminUser && hideTechnicalDetails) {
            return "堆栈信息已隐藏";
        }
        
        // 脱敏文件路径和敏感信息
        String maskedStackTrace = maskSensitiveInfo(stackTrace);
        maskedStackTrace = FILE_PATH_PATTERN.matcher(maskedStackTrace)
                .replaceAll("[文件路径已脱敏]");
        
        return maskedStackTrace;
    }
    
    /**
     * 根据当前用户权限脱敏异常响应
     * 
     * @param errorResponse 错误响应
     * @return 脱敏后的错误响应
     */
    public SecurityErrorResponse maskByCurrentUserPermission(SecurityErrorResponse errorResponse) {
        boolean isAdminUser = determineAdminAccess();
        return maskSecurityErrorResponse(errorResponse, isAdminUser);
    }
    
    // ==================== 私有脱敏方法 ====================
    
    /**
     * 脱敏敏感信息
     * 
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    private String maskSensitiveInfo(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String maskedText = text;
        
        // 脱敏邮箱地址
        maskedText = EMAIL_PATTERN.matcher(maskedText)
                .replaceAll(match -> maskEmail(match.group()));
        
        // 脱敏手机号
        maskedText = PHONE_PATTERN.matcher(maskedText)
                .replaceAll(match -> maskPhone(match.group()));
        
        // 脱敏IP地址
        maskedText = IP_PATTERN.matcher(maskedText)
                .replaceAll(match -> maskIpAddress(match.group()));
        
        // 脱敏身份证号
        maskedText = ID_CARD_PATTERN.matcher(maskedText)
                .replaceAll("[身份证号已脱敏]");
        
        // 脱敏密码相关信息
        maskedText = PASSWORD_PATTERN.matcher(maskedText)
                .replaceAll(match -> {
                    String[] parts = match.group().split("[=:]");
                    if (parts.length >= 2) {
                        return parts[0] + "=" + maskChar.repeat(6);
                    }
                    return match.group();
                });
        
        return maskedText;
    }
    
    /**
     * 隐藏技术细节
     * 
     * @param message 原始消息
     * @return 隐藏技术细节后的消息
     */
    private String hideTechnicalDetails(String message) {
        String hiddenMessage = message;
        
        // 隐藏SQL语句
        hiddenMessage = SQL_PATTERN.matcher(hiddenMessage)
                .replaceAll("[SQL语句已隐藏]");
        
        // 隐藏文件路径
        hiddenMessage = FILE_PATH_PATTERN.matcher(hiddenMessage)
                .replaceAll("[文件路径已隐藏]");
        
        // 隐藏Java类名和包名
        hiddenMessage = hiddenMessage.replaceAll("\\b[a-z]+\\.[a-z]+\\.[A-Z][a-zA-Z]*", "[类名已隐藏]");
        
        return hiddenMessage;
    }
    
    /**
     * 脱敏邮箱地址
     * 
     * @param email 邮箱地址
     * @return 脱敏后的邮箱地址
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return username.charAt(0) + maskChar + domain;
        } else {
            return username.charAt(0) + maskChar.repeat(3) + username.charAt(username.length() - 1) + domain;
        }
    }
    
    /**
     * 脱敏手机号
     * 
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (phone.length() != 11) {
            return phone;
        }
        
        return phone.substring(0, 3) + maskChar.repeat(4) + phone.substring(7);
    }
    
    /**
     * 脱敏IP地址
     * 
     * @param ip IP地址
     * @return 脱敏后的IP地址
     */
    private String maskIpAddress(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + maskChar.repeat(3) + "." + maskChar.repeat(3);
        }
        return ip;
    }
    
    /**
     * 脱敏请求路径
     * 
     * @param path 请求路径
     * @return 脱敏后的路径
     */
    private String maskPath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        // 脱敏路径中的敏感参数
        return path.replaceAll("([?&])(password|pwd|token|key|secret)=([^&]*)", "$1$2=" + maskChar.repeat(6));
    }
    
    /**
     * 脱敏值
     * 
     * @param value 原始值
     * @return 脱敏后的值
     */
    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.length() <= 3) {
                return maskChar.repeat(strValue.length());
            } else {
                return strValue.charAt(0) + maskChar.repeat(3) + strValue.charAt(strValue.length() - 1);
            }
        }
        
        return maskChar.repeat(6);
    }
    
    /**
     * 检查是否为敏感字段
     * 
     * @param fieldName 字段名
     * @return 是否为敏感字段
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        return lowerFieldName.contains("password") ||
               lowerFieldName.contains("pwd") ||
               lowerFieldName.contains("token") ||
               lowerFieldName.contains("key") ||
               lowerFieldName.contains("secret") ||
               lowerFieldName.contains("email") ||
               lowerFieldName.contains("phone") ||
               lowerFieldName.contains("mobile") ||
               lowerFieldName.contains("idcard") ||
               lowerFieldName.contains("ip");
    }
    
    /**
     * 判断是否有管理员访问权限
     * 
     * @return 是否有管理员权限
     */
    private boolean determineAdminAccess() {
        try {
            return PermissionUtils.isAdmin();
        } catch (Exception e) {
            log.debug("SecurityExceptionMaskingService: 判断管理员权限时发生异常: {}", e.getMessage());
            // 异常情况下，为了安全起见，不给予管理员权限
            return false;
        }
    }
    
    // ==================== 配置管理方法 ====================
    
    /**
     * 检查脱敏功能是否启用
     * 
     * @return 脱敏功能是否启用
     */
    public boolean isMaskingEnabled() {
        return maskingEnabled;
    }
    
    /**
     * 检查是否隐藏技术细节
     * 
     * @return 是否隐藏技术细节
     */
    public boolean isHideTechnicalDetails() {
        return hideTechnicalDetails;
    }
    
    /**
     * 获取脱敏配置信息
     * 
     * @return 脱敏配置信息
     */
    public Map<String, Object> getMaskingConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", maskingEnabled);
        config.put("hideTechnicalDetails", hideTechnicalDetails);
        config.put("adminFullAccess", adminFullAccess);
        config.put("maskChar", maskChar);
        return config;
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证脱敏效果
     * 
     * @param originalText 原始文本
     * @param maskedText 脱敏后文本
     * @return 验证结果
     */
    public boolean validateMaskingEffect(String originalText, String maskedText) {
        if (originalText == null || maskedText == null) {
            return false;
        }
        
        // 如果脱敏功能未启用，脱敏后文本应该与原文本相同
        if (!maskingEnabled) {
            return originalText.equals(maskedText);
        }
        
        // 如果原文本和脱敏后文本相同，说明没有进行脱敏
        if (originalText.equals(maskedText)) {
            return false;
        }
        
        // 基本验证：脱敏后应该包含脱敏字符，且不应该与原文本完全相同
        boolean containsMaskChar = maskedText.contains(maskChar);
        boolean isDifferent = !originalText.equals(maskedText);
        
        // 检查是否有效脱敏了敏感信息
        boolean hasValidMasking = true;
        
        // 如果原文本包含邮箱，检查脱敏效果
        if (EMAIL_PATTERN.matcher(originalText).find()) {
            // 脱敏后不应该包含完整的原始邮箱地址
            hasValidMasking = hasValidMasking && !maskedText.contains(originalText);
        }
        
        // 如果原文本包含手机号，检查脱敏效果
        if (PHONE_PATTERN.matcher(originalText).find()) {
            // 脱敏后不应该包含完整的原始手机号
            hasValidMasking = hasValidMasking && !maskedText.contains(originalText);
        }
        
        // 如果原文本包含密码信息，检查脱敏效果
        if (PASSWORD_PATTERN.matcher(originalText).find()) {
            // 脱敏后不应该包含原始密码模式
            hasValidMasking = hasValidMasking && !PASSWORD_PATTERN.matcher(maskedText).find();
        }
        
        return containsMaskChar && isDifferent && hasValidMasking;
    }
}