package com.myweb.website_core.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全日志格式化工具类
 * <p>
 * 提供统一的安全日志格式化功能，包括：
 * - 安全事件日志格式化
 * - 审计日志格式化
 * - 操作日志格式化
 * - 错误日志格式化
 * - 性能日志格式化
 * - 敏感信息脱敏
 * <p>
 * 符合需求：10.5 - 提供安全日志格式化
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
public class LoggingUtils {
    
    // ==================== 常量定义 ====================
    
    /**
     * 日期时间格式化器
     */
    private static final DateTimeFormatter DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * JSON对象映射器
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 日志分隔符
     */
    private static final String LOG_SEPARATOR = " | ";
    
    /**
     * 敏感信息掩码
     */
    private static final String SENSITIVE_MASK = "***";
    
    /**
     * 最大日志长度
     */
    private static final int MAX_LOG_LENGTH = 10000;
    
    /**
     * 最大错误信息长度
     */
    private static final int MAX_ERROR_LENGTH = 2000;
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private LoggingUtils() {
        // 工具类不允许实例化
    }
    
    // ==================== 安全事件日志格式化 ====================
    
    /**
     * 格式化安全事件日志
     * 
     * @param eventType 事件类型
     * @param username 用户名
     * @param ipAddress IP地址
     * @param description 事件描述
     * @return 格式化的日志字符串
     */
    public static String formatSecurityEvent(SecurityEventType eventType, String username, 
                                           String ipAddress, String description) {
        return formatSecurityEvent(eventType, username, ipAddress, description, null, null);
    }
    
    /**
     * 格式化安全事件日志（完整版本）
     * 
     * @param eventType 事件类型
     * @param username 用户名
     * @param ipAddress IP地址
     * @param description 事件描述
     * @param userAgent 用户代理
     * @param additionalData 附加数据
     * @return 格式化的日志字符串
     */
    public static String formatSecurityEvent(SecurityEventType eventType, String username, 
                                           String ipAddress, String description, 
                                           String userAgent, Map<String, Object> additionalData) {
        StringBuilder logBuilder = new StringBuilder();
        
        // 时间戳
        logBuilder.append("[").append(getCurrentTimestamp()).append("]");
        logBuilder.append(LOG_SEPARATOR);
        
        // 事件类型
        logBuilder.append("EVENT_TYPE=").append(eventType != null ? eventType.name() : "UNKNOWN");
        logBuilder.append(LOG_SEPARATOR);
        
        // 用户信息
        logBuilder.append("USER=").append(maskSensitiveData(username, "username"));
        logBuilder.append(LOG_SEPARATOR);
        
        // IP地址
        logBuilder.append("IP=").append(ipAddress != null ? ipAddress : "unknown");
        logBuilder.append(LOG_SEPARATOR);
        
        // 用户代理（截取前100个字符）
        if (userAgent != null && !userAgent.isEmpty()) {
            String truncatedUserAgent = userAgent.length() > 100 ? 
                userAgent.substring(0, 100) + "..." : userAgent;
            logBuilder.append("USER_AGENT=").append(truncatedUserAgent);
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 事件描述
        logBuilder.append("DESCRIPTION=").append(description != null ? description : "");
        
        // 附加数据
        if (additionalData != null && !additionalData.isEmpty()) {
            logBuilder.append(LOG_SEPARATOR);
            logBuilder.append("DATA=").append(formatAdditionalData(additionalData));
        }
        
        return truncateLog(logBuilder.toString());
    }
    
    // ==================== 审计日志格式化 ====================
    
    /**
     * 格式化审计日志
     * 
     * @param operation 操作类型
     * @param username 用户名
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param result 操作结果
     * @return 格式化的日志字符串
     */
    public static String formatAuditLog(AuditOperation operation, String username, 
                                      String resourceType, String resourceId, String result) {
        return formatAuditLog(operation, username, resourceType, resourceId, result, 
                            null, null, null, null);
    }
    
    /**
     * 格式化审计日志（完整版本）
     * 
     * @param operation 操作类型
     * @param username 用户名
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param result 操作结果
     * @param ipAddress IP地址
     * @param errorMessage 错误信息
     * @param executionTime 执行时间（毫秒）
     * @param additionalData 附加数据
     * @return 格式化的日志字符串
     */
    public static String formatAuditLog(AuditOperation operation, String username, 
                                      String resourceType, String resourceId, String result,
                                      String ipAddress, String errorMessage, Long executionTime,
                                      Map<String, Object> additionalData) {
        StringBuilder logBuilder = new StringBuilder();
        
        // 时间戳
        logBuilder.append("[").append(getCurrentTimestamp()).append("]");
        logBuilder.append(LOG_SEPARATOR);
        
        // 操作类型
        logBuilder.append("OPERATION=").append(operation != null ? operation.name() : "UNKNOWN");
        logBuilder.append(LOG_SEPARATOR);
        
        // 用户信息
        logBuilder.append("USER=").append(maskSensitiveData(username, "username"));
        logBuilder.append(LOG_SEPARATOR);
        
        // 资源信息
        logBuilder.append("RESOURCE=").append(resourceType != null ? resourceType : "unknown");
        if (resourceId != null && !resourceId.isEmpty()) {
            logBuilder.append(":").append(resourceId);
        }
        logBuilder.append(LOG_SEPARATOR);
        
        // 操作结果
        logBuilder.append("RESULT=").append(result != null ? result : "UNKNOWN");
        logBuilder.append(LOG_SEPARATOR);
        
        // IP地址
        if (ipAddress != null && !ipAddress.isEmpty()) {
            logBuilder.append("IP=").append(ipAddress);
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 执行时间
        if (executionTime != null) {
            logBuilder.append("EXEC_TIME=").append(executionTime).append("ms");
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 错误信息
        if (errorMessage != null && !errorMessage.isEmpty()) {
            String truncatedError = errorMessage.length() > MAX_ERROR_LENGTH ? 
                errorMessage.substring(0, MAX_ERROR_LENGTH) + "..." : errorMessage;
            logBuilder.append("ERROR=").append(truncatedError);
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 附加数据
        if (additionalData != null && !additionalData.isEmpty()) {
            logBuilder.append("DATA=").append(formatAdditionalData(additionalData));
        }
        
        // 移除末尾的分隔符
        String logString = logBuilder.toString();
        if (logString.endsWith(LOG_SEPARATOR)) {
            logString = logString.substring(0, logString.length() - LOG_SEPARATOR.length());
        }
        
        return truncateLog(logString);
    }
    
    // ==================== 操作日志格式化 ====================
    
    /**
     * 格式化操作日志
     * 
     * @param action 操作动作
     * @param username 用户名
     * @param target 操作目标
     * @param result 操作结果
     * @return 格式化的日志字符串
     */
    public static String formatOperationLog(String action, String username, String target, String result) {
        return formatOperationLog(action, username, target, result, null, null);
    }
    
    /**
     * 格式化操作日志（完整版本）
     * 
     * @param action 操作动作
     * @param username 用户名
     * @param target 操作目标
     * @param result 操作结果
     * @param details 操作详情
     * @param executionTime 执行时间（毫秒）
     * @return 格式化的日志字符串
     */
    public static String formatOperationLog(String action, String username, String target, 
                                          String result, String details, Long executionTime) {
        StringBuilder logBuilder = new StringBuilder();
        
        // 时间戳
        logBuilder.append("[").append(getCurrentTimestamp()).append("]");
        logBuilder.append(LOG_SEPARATOR);
        
        // 操作动作
        logBuilder.append("ACTION=").append(action != null ? action : "UNKNOWN");
        logBuilder.append(LOG_SEPARATOR);
        
        // 用户信息
        logBuilder.append("USER=").append(maskSensitiveData(username, "username"));
        logBuilder.append(LOG_SEPARATOR);
        
        // 操作目标
        logBuilder.append("TARGET=").append(target != null ? target : "unknown");
        logBuilder.append(LOG_SEPARATOR);
        
        // 操作结果
        logBuilder.append("RESULT=").append(result != null ? result : "UNKNOWN");
        
        // 执行时间
        if (executionTime != null) {
            logBuilder.append(LOG_SEPARATOR);
            logBuilder.append("EXEC_TIME=").append(executionTime).append("ms");
        }
        
        // 操作详情
        if (details != null && !details.isEmpty()) {
            logBuilder.append(LOG_SEPARATOR);
            logBuilder.append("DETAILS=").append(details);
        }
        
        return truncateLog(logBuilder.toString());
    }
    
    // ==================== 错误日志格式化 ====================
    
    /**
     * 格式化错误日志
     * 
     * @param errorType 错误类型
     * @param errorMessage 错误信息
     * @param username 用户名
     * @return 格式化的日志字符串
     */
    public static String formatErrorLog(String errorType, String errorMessage, String username) {
        return formatErrorLog(errorType, errorMessage, username, null, null, null);
    }
    
    /**
     * 格式化错误日志（完整版本）
     * 
     * @param errorType 错误类型
     * @param errorMessage 错误信息
     * @param username 用户名
     * @param stackTrace 堆栈跟踪
     * @param context 错误上下文
     * @param additionalData 附加数据
     * @return 格式化的日志字符串
     */
    public static String formatErrorLog(String errorType, String errorMessage, String username,
                                      String stackTrace, String context, Map<String, Object> additionalData) {
        StringBuilder logBuilder = new StringBuilder();
        
        // 时间戳
        logBuilder.append("[").append(getCurrentTimestamp()).append("]");
        logBuilder.append(LOG_SEPARATOR);
        
        // 错误类型
        logBuilder.append("ERROR_TYPE=").append(errorType != null ? errorType : "UNKNOWN");
        logBuilder.append(LOG_SEPARATOR);
        
        // 用户信息
        if (username != null && !username.isEmpty()) {
            logBuilder.append("USER=").append(maskSensitiveData(username, "username"));
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 错误信息
        if (errorMessage != null && !errorMessage.isEmpty()) {
            String truncatedMessage = errorMessage.length() > MAX_ERROR_LENGTH ? 
                errorMessage.substring(0, MAX_ERROR_LENGTH) + "..." : errorMessage;
            logBuilder.append("MESSAGE=").append(truncatedMessage);
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 错误上下文
        if (context != null && !context.isEmpty()) {
            logBuilder.append("CONTEXT=").append(context);
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 堆栈跟踪（截取前1000个字符）
        if (stackTrace != null && !stackTrace.isEmpty()) {
            String truncatedStackTrace = stackTrace.length() > 1000 ? 
                stackTrace.substring(0, 1000) + "..." : stackTrace;
            logBuilder.append("STACK_TRACE=").append(truncatedStackTrace);
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 附加数据
        if (additionalData != null && !additionalData.isEmpty()) {
            logBuilder.append("DATA=").append(formatAdditionalData(additionalData));
        }
        
        // 移除末尾的分隔符
        String logString = logBuilder.toString();
        if (logString.endsWith(LOG_SEPARATOR)) {
            logString = logString.substring(0, logString.length() - LOG_SEPARATOR.length());
        }
        
        return truncateLog(logString);
    }
    
    // ==================== 性能日志格式化 ====================
    
    /**
     * 格式化性能日志
     * 
     * @param operation 操作名称
     * @param executionTime 执行时间（毫秒）
     * @param username 用户名
     * @return 格式化的日志字符串
     */
    public static String formatPerformanceLog(String operation, long executionTime, String username) {
        return formatPerformanceLog(operation, executionTime, username, null, null);
    }
    
    /**
     * 格式化性能日志（完整版本）
     * 
     * @param operation 操作名称
     * @param executionTime 执行时间（毫秒）
     * @param username 用户名
     * @param parameters 操作参数
     * @param metrics 性能指标
     * @return 格式化的日志字符串
     */
    public static String formatPerformanceLog(String operation, long executionTime, String username,
                                            Map<String, Object> parameters, Map<String, Object> metrics) {
        StringBuilder logBuilder = new StringBuilder();
        
        // 时间戳
        logBuilder.append("[").append(getCurrentTimestamp()).append("]");
        logBuilder.append(LOG_SEPARATOR);
        
        // 操作名称
        logBuilder.append("OPERATION=").append(operation != null ? operation : "UNKNOWN");
        logBuilder.append(LOG_SEPARATOR);
        
        // 执行时间
        logBuilder.append("EXEC_TIME=").append(executionTime).append("ms");
        logBuilder.append(LOG_SEPARATOR);
        
        // 性能等级
        String performanceLevel = getPerformanceLevel(executionTime);
        logBuilder.append("LEVEL=").append(performanceLevel);
        logBuilder.append(LOG_SEPARATOR);
        
        // 用户信息
        if (username != null && !username.isEmpty()) {
            logBuilder.append("USER=").append(maskSensitiveData(username, "username"));
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 操作参数
        if (parameters != null && !parameters.isEmpty()) {
            logBuilder.append("PARAMS=").append(formatAdditionalData(parameters));
            logBuilder.append(LOG_SEPARATOR);
        }
        
        // 性能指标
        if (metrics != null && !metrics.isEmpty()) {
            logBuilder.append("METRICS=").append(formatAdditionalData(metrics));
        }
        
        // 移除末尾的分隔符
        String logString = logBuilder.toString();
        if (logString.endsWith(LOG_SEPARATOR)) {
            logString = logString.substring(0, logString.length() - LOG_SEPARATOR.length());
        }
        
        return truncateLog(logString);
    }
    
    // ==================== 敏感信息脱敏 ====================
    
    /**
     * 脱敏敏感数据
     * 
     * @param data 原始数据
     * @param dataType 数据类型
     * @return 脱敏后的数据
     */
    public static String maskSensitiveData(String data, String dataType) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        switch (dataType.toLowerCase()) {
            case "email":
                return maskEmail(data);
            case "phone":
            case "mobile":
                return maskPhone(data);
            case "password":
                return SENSITIVE_MASK;
            case "token":
            case "key":
                return maskToken(data);
            case "username":
                return maskUsername(data);
            case "idcard":
                return maskIdCard(data);
            default:
                return data;
        }
    }
    
    /**
     * 脱敏邮箱地址
     * 
     * @param email 邮箱地址
     * @return 脱敏后的邮箱地址
     */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        
        String username = parts[0];
        String domain = parts[1];
        
        if (username.length() <= 2) {
            return username.charAt(0) + SENSITIVE_MASK + "@" + domain;
        } else {
            return username.charAt(0) + SENSITIVE_MASK + username.charAt(username.length() - 1) + "@" + domain;
        }
    }
    
    /**
     * 脱敏手机号
     * 
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            return phone.substring(0, 3) + SENSITIVE_MASK + phone.substring(phone.length() - 2);
        }
    }
    
    /**
     * 脱敏令牌
     * 
     * @param token 令牌
     * @return 脱敏后的令牌
     */
    private static String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return SENSITIVE_MASK;
        }
        
        return token.substring(0, 4) + SENSITIVE_MASK + token.substring(token.length() - 4);
    }
    
    /**
     * 脱敏用户名
     * 
     * @param username 用户名
     * @return 脱敏后的用户名
     */
    private static String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return username;
        }
        
        if (username.length() <= 4) {
            return username.charAt(0) + SENSITIVE_MASK;
        } else {
            return username.substring(0, 2) + SENSITIVE_MASK + username.substring(username.length() - 2);
        }
    }
    
    /**
     * 脱敏身份证号
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return SENSITIVE_MASK;
        }
        
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取当前时间戳
     * 
     * @return 格式化的时间戳字符串
     */
    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
    
    /**
     * 格式化附加数据
     * 
     * @param data 附加数据
     * @return 格式化的数据字符串
     */
    private static String formatAdditionalData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        
        try {
            // 脱敏敏感数据
            Map<String, Object> maskedData = new HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (isSensitiveKey(key)) {
                        maskedData.put(key, maskSensitiveData(stringValue, key));
                    } else {
                        maskedData.put(key, stringValue);
                    }
                } else {
                    maskedData.put(key, value);
                }
            }
            
            return objectMapper.writeValueAsString(maskedData);
        } catch (JsonProcessingException e) {
            log.warn("格式化附加数据失败", e);
            return data.toString();
        }
    }
    
    /**
     * 检查键名是否为敏感信息
     * 
     * @param key 键名
     * @return 如果是敏感信息返回true
     */
    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") || 
               lowerKey.contains("token") || 
               lowerKey.contains("key") || 
               lowerKey.contains("secret") || 
               lowerKey.contains("email") || 
               lowerKey.contains("phone") || 
               lowerKey.contains("mobile") || 
               lowerKey.contains("idcard");
    }
    
    /**
     * 截断日志长度
     * 
     * @param log 原始日志
     * @return 截断后的日志
     */
    private static String truncateLog(String log) {
        if (log == null) {
            return null;
        }
        
        if (log.length() > MAX_LOG_LENGTH) {
            return log.substring(0, MAX_LOG_LENGTH) + "...[TRUNCATED]";
        }
        
        return log;
    }
    
    /**
     * 获取性能等级
     * 
     * @param executionTime 执行时间（毫秒）
     * @return 性能等级
     */
    private static String getPerformanceLevel(long executionTime) {
        if (executionTime < 100) {
            return "FAST";
        } else if (executionTime < 500) {
            return "NORMAL";
        } else if (executionTime < 1000) {
            return "SLOW";
        } else if (executionTime < 5000) {
            return "VERY_SLOW";
        } else {
            return "CRITICAL";
        }
    }
    
    /**
     * 创建结构化日志数据
     * 
     * @param operation 操作
     * @param username 用户名
     * @param result 结果
     * @param executionTime 执行时间
     * @return 结构化日志数据
     */
    public static Map<String, Object> createStructuredLogData(String operation, String username, 
                                                             String result, Long executionTime) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", getCurrentTimestamp());
        logData.put("operation", operation);
        logData.put("username", maskSensitiveData(username, "username"));
        logData.put("result", result);
        if (executionTime != null) {
            logData.put("executionTime", executionTime);
            logData.put("performanceLevel", getPerformanceLevel(executionTime));
        }
        return logData;
    }
}