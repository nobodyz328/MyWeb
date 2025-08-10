package com.myweb.website_core.application.service.security.audit;

import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
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
     * 记录审计日志
     * 基于RabbitMQ消息队列，只发送普通审计日志
     */
    public CompletableFuture<Void> logOperation(AuditLogRequest request) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 直接发送审计日志消息，不处理安全事件
                sendAuditLogMessage(request);
                
                log.debug("审计日志记录成功: operation={}, user={}", 
                         request.getOperation(), request.getUsername());
                
            } catch (Exception e) {
                log.error("记录审计日志失败: operation={}, user={}, error={}", 
                         request.getOperation(), request.getUsername(), e.getMessage(), e);
                // 不抛出异常，避免影响业务流程
            }
        });
    }
    
    /**
     * 记录安全事件
     * 
     * @param userId 用户ID
     * @param operation 操作类型
     * @param eventType 事件类型
     * @param description 描述
     * @param ipAddress IP地址
     * @param success 是否成功
     */
    public void logSecurityEvent(String userId, AuditOperation operation, String eventType, 
                                String description, String ipAddress, boolean success) {
        try {
            SecurityEventRequest request = SecurityEventRequest.builder()
                    .eventType(SecurityEventType.valueOf(eventType)) // 简化处理
                    .title(operation != null ? operation.getName() : "安全事件")
                    .description(description)
                    .userId(userId != null ? Long.valueOf(userId) : null)
                    .username(userId) // 简化处理，实际应该查询用户名
                    .sourceIp(ipAddress)
                    .riskScore(success ? 20 : 60)
                    .eventTime(java.time.LocalDateTime.now())
                    .build();
            
            // 发送安全事件消息
            messageProducerService.sendSecurityEventMessage(request);
            
            log.debug("安全事件记录成功: userId={}, operation={}, eventType={}", 
                     userId, operation, eventType);
            
        } catch (Exception e) {
            log.error("记录安全事件失败: userId={}, operation={}, eventType={}, error={}", 
                     userId, operation, eventType, e.getMessage(), e);
        }
    }
    
    /**
     * 记录审计日志（同步）
     */
    public void logOperationSync(AuditLogRequest request) {
        try {
            // 直接发送审计日志消息，不处理安全事件
            sendAuditLogMessage(request);
            
            log.debug("审计日志同步记录成功: operation={}, user={}", 
                     request.getOperation(), request.getUsername());
            
        } catch (Exception e) {
            log.error("同步记录审计日志失败: operation={}, user={}, error={}", 
                     request.getOperation(), request.getUsername(), e.getMessage(), e);
            // 不抛出异常，避免影响业务流程
        }
    }

    /**
     * 发送审计日志消息
     */
    private void sendAuditLogMessage(AuditLogRequest request) {
        String messageType = determineMessageType(request.getOperation());
        
        // 根据消息类型发送到不同队列
        switch (messageType) {
            case "USER_AUTH" -> messageProducerService.sendUserAuthAuditMessage(request);
            case "FILE_OPERATION" -> sendFileOperationMessage(request);
            case "SEARCH" -> messageProducerService.sendSearchAuditMessage(request);
            case "ACCESS_CONTROL" -> messageProducerService.sendAccessControlAuditMessage(request);
            default -> messageProducerService.sendContentOperationAuditMessage(request);
        }
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
        
        // 文件相关操作
        if (isFileOperation(operation)) {
            return "FILE_OPERATION";
        }
        
        // 搜索相关操作
        if (isSearchOperation(operation)) {
            return "SEARCH";
        }
        
        // 访问控制操作
        if (operation == AuditOperation.ACCESS_DENIED) {
            return "ACCESS_CONTROL";
        }
        
        // 内容管理操作
        if (operation.isContentOperation()) {
            return "CONTENT_OPERATION";
        }
        
        // 管理员操作
        if (operation.isAdminOperation()) {
            return "ADMIN_OPERATION";
        }
        
        // 安全相关操作
        if (operation.isSecurityOperation()) {
            return "SECURITY_EVENT";
        }
        
        // 系统操作
        if (operation.isSystemOperation()) {
            return "SYSTEM_OPERATION";
        }
        
        // 默认为审计日志
        return "AUDIT_LOG";
    }
    
    /**
     * 判断是否为文件操作
     */
    private boolean isFileOperation(AuditOperation operation) {
        return operation == AuditOperation.FILE_UPLOAD ||
               operation == AuditOperation.FILE_DOWNLOAD ||
               operation == AuditOperation.FILE_DELETE ||
               operation == AuditOperation.AVATAR_UPLOAD ||
               operation == AuditOperation.AVATAR_DELETE ||
               operation == AuditOperation.FILE_REVIEW ||
               operation == AuditOperation.MALICIOUS_FILE_DETECTED;
    }
    
    /**
     * 判断是否为搜索操作
     */
    private boolean isSearchOperation(AuditOperation operation) {
        return operation == AuditOperation.SEARCH_OPERATION ||
               operation == AuditOperation.ADVANCED_SEARCH ||
               operation == AuditOperation.SEARCH_CACHE_CLEAR;
    }
    
    /**
     * 发送文件操作消息
     */
    private void sendFileOperationMessage(AuditLogRequest request) {
        AuditOperation operation = request.getOperation();
        
        if (operation == AuditOperation.FILE_UPLOAD || operation == AuditOperation.AVATAR_UPLOAD) {
            messageProducerService.sendFileUploadAuditMessage(request);
        } else {
            // 其他文件操作使用通用消息
            messageProducerService.sendContentOperationAuditMessage(request);
        }
    }
}