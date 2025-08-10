package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.confirm.ConfirmationService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataDeletionService;
import com.myweb.website_core.common.validation.SafeString;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据彻底删除控制器
 * 
 * 提供数据彻底删除的REST API接口：
 * - 用户账户注销
 * - 帖子彻底删除
 * - 评论彻底删除
 * - 删除确认令牌管理
 * 
 * 符合GB/T 22239-2019二级等保要求的剩余信息保护机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@RestController
@RequestMapping("/api/security/data-deletion")
@RequiredArgsConstructor
public class DataDeletionController {
    
    private final DataDeletionService dataDeletionService;
    
    /**
     * 删除确认请求DTO
     */
    public static class DeletionConfirmationRequest {
        @NotNull(message = "操作类型不能为空")
        private ConfirmationService.OperationType operationType;
        
        @SafeString(message = "资源ID包含非法字符")
        private String resourceId;
        
        // Getters and setters
        public ConfirmationService.OperationType getOperationType() { return operationType; }
        public void setOperationType(ConfirmationService.OperationType operationType) { this.operationType = operationType; }
        
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    }
    
    /**
     * 删除执行请求DTO
     */
    public static class DeletionExecutionRequest {
        @NotBlank(message = "确认令牌不能为空")
        @SafeString(message = "确认令牌包含非法字符")
        private String confirmationToken;
        
        @NotNull(message = "资源ID不能为空")
        private Long resourceId;
        
        // Getters and setters
        public String getConfirmationToken() { return confirmationToken; }
        public void setConfirmationToken(String confirmationToken) { this.confirmationToken = confirmationToken; }
        
        public Long getResourceId() { return resourceId; }
        public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    }
    
    /**
     * 生成删除确认令牌
     */
    @PostMapping("/confirmation/generate")
    //@Auditable(operation = "GENERATE_DELETION_TOKEN", resourceType = "SECURITY")
    public ResponseEntity<Map<String, Object>> generateDeletionConfirmation(
            @Valid @RequestBody DeletionConfirmationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = authentication.getName();
            
            // 检查是否需要确认
            if (!dataDeletionService.requiresDeletionConfirmation(request.getOperationType(), userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("requiresConfirmation", false);
                response.put("message", "此操作不需要确认");
                return ResponseEntity.ok(response);
            }
            
            // 生成确认令牌
            ConfirmationService.ConfirmationToken token = dataDeletionService.generateDeletionConfirmationToken(
                userId, request.getOperationType(), request.getResourceId()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requiresConfirmation", true);
            response.put("token", token.getToken());
            response.put("operationType", token.getOperationType().getDisplayName());
            response.put("expiresAt", token.getExpiresAt());
            response.put("message", "确认令牌已生成，请在10分钟内完成确认");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("生成删除确认令牌失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "生成确认令牌失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 发送删除确认邮件
     */
    @PostMapping("/confirmation/email")
    //@Auditable(operation = "SEND_DELETION_EMAIL", resourceType = "SECURITY")
    public ResponseEntity<Map<String, Object>> sendDeletionConfirmationEmail(
            @Valid @RequestBody DeletionConfirmationRequest request,
            Authentication authentication) {
        
        try {
            String userId = authentication.getName();
            
            // 发送确认邮件
            ConfirmationService.ConfirmationToken token = dataDeletionService.sendDeletionConfirmationEmail(
                userId, request.getOperationType(), request.getResourceId()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token.getToken());
            response.put("operationType", token.getOperationType().getDisplayName());
            response.put("expiresAt", token.getExpiresAt());
            response.put("message", "确认邮件已发送，请查收邮件并点击确认链接");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("发送删除确认邮件失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送确认邮件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户账户注销
     */
    @PostMapping("/user/deactivate")
    //@Auditable(operation = "USER_DEACTIVATE", resourceType = "USER")
    public ResponseEntity<Map<String, Object>> deactivateUserAccount(
            @Valid @RequestBody DeletionExecutionRequest request,
            Authentication authentication) {
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 执行账户注销
            DataDeletionService.DeletionResult result = dataDeletionService.deactivateUserAccount(
                userId, request.getConfirmationToken()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("statistics", result.getStatistics());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("用户账户注销失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "账户注销失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 彻底删除帖子
     */
    @PostMapping("/post/{postId}")
    @PreAuthorize("hasPermission(#postId, 'POST', 'DELETE') or hasRole('ADMIN')")
    //@Auditable(operation = "POST_DELETE_COMPLETE", resourceType = "POST")
    public ResponseEntity<Map<String, Object>> deletePostCompletely(
            @PathVariable Long postId,
            @Valid @RequestBody DeletionExecutionRequest request,
            Authentication authentication) {
        
        try {
            String operatorUserId = authentication.getName();
            
            // 执行帖子删除
            DataDeletionService.DeletionResult result = dataDeletionService.deletePostCompletely(
                postId, request.getConfirmationToken(), operatorUserId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("statistics", result.getStatistics());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("彻底删除帖子失败 - 帖子ID: {}", postId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除帖子失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 彻底删除评论
     */
    @PostMapping("/comment/{commentId}")
    @PreAuthorize("hasPermission(#commentId, 'COMMENT', 'DELETE') or hasRole('ADMIN')")
    //@Auditable(operation = "COMMENT_DELETE_COMPLETE", resourceType = "COMMENT")
    public ResponseEntity<Map<String, Object>> deleteCommentCompletely(
            @PathVariable Long commentId,
            @Valid @RequestBody DeletionExecutionRequest request,
            Authentication authentication) {
        
        try {
            String operatorUserId = authentication.getName();
            
            // 执行评论删除
            DataDeletionService.DeletionResult result = dataDeletionService.deleteCommentCompletely(
                commentId, request.getConfirmationToken(), operatorUserId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("statistics", result.getStatistics());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("彻底删除评论失败 - 评论ID: {}", commentId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除评论失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 管理员彻底删除用户
     */
    @PostMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    //@Auditable(operation = "ADMIN_DELETE_USER", resourceType = "USER")
    public ResponseEntity<Map<String, Object>> adminDeleteUserCompletely(
            @PathVariable Long userId,
            @Valid @RequestBody DeletionExecutionRequest request,
            Authentication authentication) {
        
        try {
            String operatorUserId = authentication.getName();
            
            // 执行用户删除
            DataDeletionService.DeletionResult result = dataDeletionService.deleteUserCompletely(
                userId, request.getConfirmationToken(), operatorUserId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("statistics", result.getStatistics());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("管理员彻底删除用户失败 - 用户ID: {}", userId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 清理会话数据
     */
    @PostMapping("/session/clear")
    //@Auditable(operation = "CLEAR_SESSION", resourceType = "SESSION")
    public ResponseEntity<Map<String, Object>> clearSessionData(
            Authentication authentication,
            HttpServletRequest request) {
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            String sessionId = request.getSession().getId();
            
            // 清理会话数据
            dataDeletionService.clearSessionData(userId, sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "会话数据清理成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("清理会话数据失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清理会话数据失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 系统管理员清理临时数据
     */
    @PostMapping("/admin/cleanup/temporary")
    @PreAuthorize("hasRole('ADMIN')")
    //@Auditable(operation = "CLEANUP_TEMPORARY", resourceType = "SYSTEM")
    public ResponseEntity<Map<String, Object>> cleanupTemporaryData(Authentication authentication) {
        
        try {
            // 清理临时数据
            dataDeletionService.clearTemporaryData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "系统临时数据清理成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("清理系统临时数据失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清理临时数据失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}