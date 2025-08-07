package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.confirm.ConfirmationService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.infrastructure.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 重要操作确认控制器
 * 
 * 提供重要操作的二次确认功能，包括：
 * - 生成确认令牌
 * - 发送确认邮件
 * - 验证确认令牌
 * - 执行确认操作
 * 
 * 符合GB/T 22239-2019二级等保要求的操作确认机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@RestController
@RequestMapping("/api/security/confirmation")
@RequiredArgsConstructor
public class ConfirmationController {
    
    private final ConfirmationService confirmationService;
    
    /**
     * 请求确认令牌
     * 
     * @param request 确认请求
     * @param authentication 认证信息
     * @return 确认令牌信息
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "请求操作确认")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestConfirmation(
            @RequestBody ConfirmationRequest request,
            Authentication authentication) {
        
        try {
            String userId = authentication.getName();
            
            // 验证操作类型
            ConfirmationService.OperationType operationType = 
                    ConfirmationService.OperationType.valueOf(request.getOperationType());
            
            // 检查是否需要确认
            if (!confirmationService.requiresConfirmation(operationType, userId)) {
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "requiresConfirmation", false,
                    "message", "此操作无需确认"
                )));
            }
            
            // 生成确认令牌
            ConfirmationService.ConfirmationToken token = confirmationService.generateConfirmationToken(
                userId, operationType, request.getResourceId()
            );
            
            Map<String, Object> response = Map.of(
                "requiresConfirmation", true,
                "token", token.getToken(),
                "expiresAt", token.getExpiresAt(),
                "operationType", operationType.getDisplayName(),
                "description", operationType.getDescription()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("无效的操作类型: {}", request.getOperationType());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的操作类型"));
        } catch (Exception e) {
            log.error("请求确认令牌失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("请求确认失败"));
        }
    }
    
    /**
     * 发送邮件确认
     * 
     * @param request 确认请求
     * @param authentication 认证信息
     * @return 发送结果
     */
    @PostMapping("/send-email")
    @PreAuthorize("isAuthenticated()")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "发送邮件确认")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendEmailConfirmation(
            @RequestBody ConfirmationRequest request,
            Authentication authentication) {
        
        try {
            String userId = authentication.getName();
            
            // 验证操作类型
            ConfirmationService.OperationType operationType = 
                    ConfirmationService.OperationType.valueOf(request.getOperationType());
            
            // 发送邮件确认
            ConfirmationService.ConfirmationToken token = confirmationService.sendEmailConfirmation(
                userId, operationType, request.getResourceId()
            );
            
            Map<String, Object> response = Map.of(
                "sent", true,
                "token", token.getToken(),
                "expiresAt", token.getExpiresAt(),
                "message", "确认邮件已发送，请查收邮箱"
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("无效的操作类型: {}", request.getOperationType());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的操作类型"));
        } catch (Exception e) {
            log.error("发送邮件确认失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("发送确认邮件失败: " + e.getMessage()));
        }
    }
    
    /**
     * 验证确认令牌
     * 
     * @param token 确认令牌
     * @return 验证结果
     */
    @GetMapping("/validate/{token}")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "验证确认令牌")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateConfirmation(
            @PathVariable String token) {
        
        try {
            ConfirmationService.ConfirmationToken confirmationToken = 
                    confirmationService.validateConfirmationToken(token);
            
            if (confirmationToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("确认令牌无效或已过期"));
            }
            
            Map<String, Object> response = Map.of(
                "valid", true,
                "userId", confirmationToken.getUserId(),
                "operationType", confirmationToken.getOperationType().name(),
                "operationName", confirmationToken.getOperationType().getDisplayName(),
                "description", confirmationToken.getOperationType().getDescription(),
                "resourceId", confirmationToken.getResourceId() != null ? confirmationToken.getResourceId() : "",
                "expiresAt", confirmationToken.getExpiresAt()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("验证确认令牌失败: {}", token, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("验证确认令牌失败"));
        }
    }
    
    /**
     * 确认操作
     * 
     * @param token 确认令牌
     * @param authentication 认证信息
     * @return 确认结果
     */
    @PostMapping("/confirm/{token}")
    @PreAuthorize("isAuthenticated()")
    @Auditable(operation = AuditOperation.SYSTEM_MONITOR, description = "确认重要操作")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmOperation(
            @PathVariable String token,
            Authentication authentication) {
        
        try {
            String currentUserId = authentication.getName();
            
            // 消费确认令牌
            ConfirmationService.ConfirmationToken confirmationToken = 
                    confirmationService.consumeConfirmationToken(token);
            
            if (confirmationToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("确认令牌无效或已过期"));
            }
            
            // 验证用户身份
            if (!confirmationToken.getUserId().equals(currentUserId)) {
                log.warn("用户身份不匹配 - 令牌用户: {}, 当前用户: {}", 
                        confirmationToken.getUserId(), currentUserId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户身份验证失败"));
            }
            
            Map<String, Object> response = Map.of(
                "confirmed", true,
                "operationType", confirmationToken.getOperationType().name(),
                "operationName", confirmationToken.getOperationType().getDisplayName(),
                "resourceId", confirmationToken.getResourceId() != null ? confirmationToken.getResourceId() : "",
                "message", "操作确认成功，可以继续执行"
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("确认操作失败: {}", token, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("确认操作失败"));
        }
    }
    
    /**
     * 检查操作是否需要确认
     * 
     * @param operationType 操作类型
     * @param authentication 认证信息
     * @return 检查结果
     */
    @GetMapping("/check/{operationType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkConfirmationRequired(
            @PathVariable String operationType,
            Authentication authentication) {
        
        try {
            String userId = authentication.getName();
            
            // 验证操作类型
            ConfirmationService.OperationType opType = 
                    ConfirmationService.OperationType.valueOf(operationType);
            
            boolean requiresConfirmation = confirmationService.requiresConfirmation(opType, userId);
            
            Map<String, Object> response = Map.of(
                "requiresConfirmation", requiresConfirmation,
                "operationType", opType.name(),
                "operationName", opType.getDisplayName(),
                "description", opType.getDescription()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("无效的操作类型: {}", operationType);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的操作类型"));
        } catch (Exception e) {
            log.error("检查确认需求失败: {}", operationType, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("检查确认需求失败"));
        }
    }
    
    /**
     * 确认请求DTO
     */
    public static class ConfirmationRequest {
        private String operationType;
        private String resourceId;
        
        // Constructors
        public ConfirmationRequest() {}
        
        public ConfirmationRequest(String operationType, String resourceId) {
            this.operationType = operationType;
            this.resourceId = resourceId;
        }
        
        // Getters and Setters
        public String getOperationType() {
            return operationType;
        }
        
        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }
        
        public String getResourceId() {
            return resourceId;
        }
        
        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }
    }
}