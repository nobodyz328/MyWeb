package com.myweb.website_core.application.service.security.audit;

import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.dto.UnifiedSecurityMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 审计日志服务适配器
 * 
 * 完全基于RabbitMQ消息队列的审计日志处理
 * 根据AuditOperation判断发送到不同的消息队列
 * 自动识别安全事件并发送到安全事件队列
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceAdapter {
    
    private final MessageProducerService messageProducerService;
    
    /**
     * 记录审计日志（异步）
     * 基于RabbitMQ消息队列，
     */
    public CompletableFuture<Void> logOperation(AuditLogRequest request) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 转换为统一安全消息
                UnifiedSecurityMessage message = convertToUnifiedMessage(request);
                
                // 判断是否为安全事件
                if (isSecurityEvent(request)) {
                    SecurityEventType eventType = determineSecurityEventType(request);
                    message.markAsSecurityEvent(eventType, request.getRiskLevel());
                }
                
                // 根据消息类型发送到相应队列
                sendToAppropriateQueue(message);
                
                log.debug("审计日志记录成功: operation={}, user={}, messageType={}", 
                         request.getOperation(), request.getUsername(), message.getMessageType());
                
            } catch (Exception e) {
                log.error("记录审计日志失败: operation={}, user={}, error={}", 
                         request.getOperation(), request.getUsername(), e.getMessage(), e);
                // 不抛出异常，避免影响业务流程
            }
        });
    }
    
    /**
     * 记录审计日志（同步）
     */
    public void logOperationSync(AuditLogRequest request) {
        try {
            // 转换为统一安全消息
            UnifiedSecurityMessage message = convertToUnifiedMessage(request);
            
            // 判断是否为安全事件
            if (isSecurityEvent(request)) {
                SecurityEventType eventType = determineSecurityEventType(request);
                message.markAsSecurityEvent(eventType, request.getRiskLevel());
            }
            
            // 根据消息类型发送到相应队列
            sendToAppropriateQueue(message);
            
            log.debug("审计日志同步记录成功: operation={}, user={}, messageType={}", 
                     request.getOperation(), request.getUsername(), message.getMessageType());
            
        } catch (Exception e) {
            log.error("同步记录审计日志失败: operation={}, user={}, error={}", 
                     request.getOperation(), request.getUsername(), e.getMessage(), e);
            // 不抛出异常，避免影响业务流程
        }
    }
    
    /**
     * 将AuditLogRequest转换为UnifiedSecurityMessage
     */
    private UnifiedSecurityMessage convertToUnifiedMessage(AuditLogRequest request) {
        return UnifiedSecurityMessage.builder()
                .messageType(determineMessageType(request.getOperation()))
                .isSecurityEvent(false) // 默认不是安全事件，后续会判断
                .userId(request.getUserId())
                .username(request.getUsername())
                .operation(request.getOperation())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .result(request.getResult())
                .errorMessage(request.getErrorMessage())
                .requestData(convertObjectToString(request.getRequestData()))
                .responseData(convertObjectToString(request.getResponseData()))
                .executionTime(request.getExecutionTime())
                .timestamp(request.getTimestamp())
                .sessionId(request.getSessionId())
                .requestId(request.getRequestId())
                .description(request.getDescription())
                .riskLevel(request.getRiskLevel())
                .location(request.getLocation())
                .deviceFingerprint(request.getDeviceFingerprint())
                .tags(request.getTags())
                .build();
    }
    
    /**
     * 根据AuditOperation确定消息类型
     */
    private String determineMessageType(AuditOperation operation) {
        if (operation == null) {
            return "AUDIT_LOG";
        }
        
        // 用户认证相关操作
        if (operation.isAuthenticationOperation()) {
            return "USER_AUTH";
        }
        
        // 文件上传操作
        if (operation == AuditOperation.FILE_UPLOAD) {
            return "FILE_UPLOAD";
        }
        
        // 搜索操作
        if (operation == AuditOperation.SEARCH_OPERATION) {
            return "SEARCH";
        }
        
        // 访问控制操作
        if (operation == AuditOperation.ACCESS_DENIED) {
            return "ACCESS_CONTROL";
        }
        
        // 默认为审计日志
        return "AUDIT_LOG";
    }
    
    /**
     * 判断是否为安全事件
     */
    private boolean isSecurityEvent(AuditLogRequest request) {
        if (request.getOperation() == null) {
            return false;
        }
        
        // 失败的操作且风险级别较高
        if ("FAILURE".equals(request.getResult()) && 
            request.getRiskLevel() != null && request.getRiskLevel() >= 3) {
            return true;
        }
        
        // 特定的安全相关操作
        switch (request.getOperation()) {
            case ACCESS_DENIED:
            case SUSPICIOUS_ACTIVITY:
            case ACCOUNT_LOCKED:
            case USER_LOGIN_FAILURE:
                return true;
            case USER_LOGIN_SUCCESS:
                // 异常登录地点或时间可能是安全事件
                return isAbnormalLogin(request);
            default:
                return false;
        }
    }
    
    /**
     * 判断是否为异常登录
     */
    private boolean isAbnormalLogin(AuditLogRequest request) {
        // 这里可以实现更复杂的异常登录检测逻辑
        // 比如检查IP地址、登录时间等
        return false; // 暂时返回false，后续可以扩展
    }
    
    /**
     * 根据审计请求确定安全事件类型
     */
    private SecurityEventType determineSecurityEventType(AuditLogRequest request) {
        if (request.getOperation() == null) {
            return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
        }
        
        switch (request.getOperation()) {
            case ACCESS_DENIED:
                return SecurityEventType.UNAUTHORIZED_ACCESS;
            case ACCOUNT_LOCKED:
                return SecurityEventType.ACCOUNT_LOCKED;
            case USER_LOGIN_FAILURE:
                // 根据失败次数判断是否为暴力破解
                return isMultipleFailures(request) ? 
                       SecurityEventType.BRUTE_FORCE_ATTACK : 
                       SecurityEventType.CONTINUOUS_LOGIN_FAILURE;
            case SUSPICIOUS_ACTIVITY:
                return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
            case FILE_UPLOAD:
                return "FAILURE".equals(request.getResult()) ? 
                       SecurityEventType.MALICIOUS_FILE_UPLOAD : 
                       SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
            default:
                return SecurityEventType.ABNORMAL_BUSINESS_OPERATION;
        }
    }
    
    /**
     * 判断是否为多次失败（暴力破解）
     */
    private boolean isMultipleFailures(AuditLogRequest request) {
        // 这里可以实现检查同一IP或用户的连续失败次数
        // 暂时简单判断
        return request.getTags() != null && request.getTags().contains("multiple_failures");
    }
    
    /**
     * 根据消息类型发送到相应队列
     */
    private void sendToAppropriateQueue(UnifiedSecurityMessage message) {
        try {
            if (message.getIsSecurityEvent()) {
                // 发送安全事件消息
                messageProducerService.sendSecurityEventMessage(message);
            }
                // 根据消息类型发送到不同队列
            switch (message.getMessageType()) {
                case "USER_AUTH" -> messageProducerService.sendUserAuthAuditMessage(
                        message.getUsername(),
                        message.getOperation(),
                        message.getIpAddress(),
                        message.getResult(),
                        message.getErrorMessage(),
                        message.getSessionId()
                );
                case "FILE_UPLOAD" -> messageProducerService.sendFileUploadAuditMessage(
                        message.getUserId(),
                        message.getUsername(),
                        extractFileName(message.getDescription()),
                        extractFileType(message.getDescription()),
                        extractFileSize(message.getDescription()),
                        message.getResult(),
                        message.getIpAddress(),
                        message.getErrorMessage()
                );
                case "SEARCH" -> messageProducerService.sendSearchAuditMessage(
                        message.getUserId(),
                        message.getUsername(),
                        extractSearchQuery(message.getDescription()),
                        extractSearchType(message.getDescription()),
                        extractResultCount(message.getDescription()),
                        message.getIpAddress()
                );
                case "ACCESS_CONTROL" -> messageProducerService.sendAccessControlAuditMessage(
                        message.getUserId(),
                        message.getUsername(),
                        message.getResourceType(),
                        message.getResourceId(),
                        extractAction(message.getDescription()),
                        message.getResult(),
                        message.getIpAddress(),
                        message.getErrorMessage()
                );
                default ->
                    // 发送通用审计消息
                        messageProducerService.sendContentOperationAuditMessage(
                                message.getUserId(),
                                message.getUsername(),
                                message.getOperation(),
                                message.getResourceType(),
                                message.getResourceId(),
                                message.getIpAddress(),
                                message.getResult(),
                                message.getDescription()
                        );
            }
        } catch (Exception e) {
            log.error("发送消息到队列失败: messageType={}, error={}", 
                     message.getMessageType(), e.getMessage(), e);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 将对象转换为字符串
     */
    private String convertObjectToString(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof String) {
            return (String) obj;
        }
        
        try {
            return obj.toString();
        } catch (Exception e) {
            log.debug("对象转换为字符串失败: {}", e.getMessage());
            return "转换失败";
        }
    }
    
    /**
     * 从描述中提取文件名
     */
    private String extractFileName(String description) {
        if (description == null) return "unknown";
        // 简单提取，实际可以使用正则表达式
        if (description.contains("文件上传: ")) {
            String[] parts = description.split("文件上传: ");
            if (parts.length > 1) {
                String fileInfo = parts[1];
                if (fileInfo.contains(" (")) {
                    return fileInfo.substring(0, fileInfo.indexOf(" ("));
                }
            }
        }
        return "unknown";
    }
    
    /**
     * 从描述中提取文件类型
     */
    private String extractFileType(String description) {
        if (description == null) return "unknown";
        if (description.contains("(") && description.contains(",")) {
            String fileInfo = description.substring(description.indexOf("(") + 1);
            if (fileInfo.contains(",")) {
                return fileInfo.substring(0, fileInfo.indexOf(","));
            }
        }
        return "unknown";
    }
    
    /**
     * 从描述中提取文件大小
     */
    private Long extractFileSize(String description) {
        if (description == null) return 0L;
        if (description.contains(" bytes)")) {
            String sizeStr = description.substring(description.lastIndexOf(", ") + 2);
            sizeStr = sizeStr.replace(" bytes)", "");
            try {
                return Long.parseLong(sizeStr);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
    
    /**
     * 从描述中提取搜索查询
     */
    private String extractSearchQuery(String description) {
        if (description == null) return "unknown";
        if (description.contains("搜索操作: ")) {
            String query = description.substring(description.indexOf("搜索操作: ") + 5);
            if (query.contains(" (")) {
                return query.substring(0, query.indexOf(" ("));
            }
        }
        return "unknown";
    }
    
    /**
     * 从描述中提取搜索类型
     */
    private String extractSearchType(String description) {
        if (description == null) return "unknown";
        if (description.contains("类型: ")) {
            String type = description.substring(description.indexOf("类型: ") + 3);
            if (type.contains(",")) {
                return type.substring(0, type.indexOf(","));
            }
        }
        return "unknown";
    }
    
    /**
     * 从描述中提取结果数量
     */
    private Integer extractResultCount(String description) {
        if (description == null) return 0;
        if (description.contains("结果数: ")) {
            String countStr = description.substring(description.indexOf("结果数: ") + 4);
            if (countStr.contains(")")) {
                countStr = countStr.substring(0, countStr.indexOf(")"));
            }
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 从描述中提取操作动作
     */
    private String extractAction(String description) {
        if (description == null) return "unknown";
        if (description.contains("访问控制检查: ")) {
            String action = description.substring(description.indexOf("访问控制检查: ") + 7);
            if (action.contains(" on ")) {
                return action.substring(0, action.indexOf(" on "));
            }
        }
        return "unknown";
    }
}