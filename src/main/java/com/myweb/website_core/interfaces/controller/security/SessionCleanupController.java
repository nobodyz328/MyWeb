package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.authentication.SessionCleanupService;
import com.myweb.website_core.application.service.security.authentication.SessionManagementService;
import com.myweb.website_core.domain.security.dto.SessionInfo;
import com.myweb.website_core.domain.security.dto.SessionStatistics;
import com.myweb.website_core.infrastructure.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 会话清理控制器
 * 
 * 提供会话清理相关的管理接口，包括：
 * - 手动清理过期会话
 * - 清理指定用户的所有会话
 * - 查看会话清理统计信息
 * - 强制清理会话
 * 
 * 符合GB/T 22239-2019剩余信息保护要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/session-cleanup")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
public class SessionCleanupController {
    
    private final SessionCleanupService sessionCleanupService;
    private final SessionManagementService sessionManagementService;
    
    /**
     * 手动触发过期会话清理
     * 
     * @return 清理结果
     */
    @PostMapping("/expired")
    //@Auditable(operation = "MANUAL_SESSION_CLEANUP", resourceType = "SESSION")
    public ResponseEntity<Map<String, Object>> cleanupExpiredSessions() {
        try {
            log.info("管理员手动触发过期会话清理");
            
            CompletableFuture<Integer> cleanupResult = sessionCleanupService.performBatchExpiredSessionCleanup();
            int cleanedCount = cleanupResult.join();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "过期会话清理完成");
            response.put("cleanedCount", cleanedCount);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("手动过期会话清理完成，清理数量: {}", cleanedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("手动过期会话清理失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "过期会话清理失败: " + e.getMessage());
            response.put("cleanedCount", 0);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 清理指定用户的所有会话
     * 
     * @param userId 用户ID
     * @param reason 清理原因
     * @return 清理结果
     */
    @PostMapping("/user/{userId}")
    //@Auditable(operation = "ADMIN_USER_SESSION_CLEANUP", resourceType = "USER_SESSION")
    public ResponseEntity<Map<String, Object>> cleanupUserSessions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "ADMIN_CLEANUP") String reason) {
        try {
            log.info("管理员清理用户会话: userId={}, reason={}", userId, reason);
            
            CompletableFuture<Integer> cleanupResult = sessionCleanupService.performUserAllSessionsCleanup(userId, reason);
            int cleanedCount = cleanupResult.join();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户会话清理完成");
            response.put("userId", userId);
            response.put("cleanedCount", cleanedCount);
            response.put("reason", reason);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("用户会话清理完成: userId={}, cleanedCount={}", userId, cleanedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户会话清理失败: userId={}", userId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户会话清理失败: " + e.getMessage());
            response.put("userId", userId);
            response.put("cleanedCount", 0);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 强制清理指定会话
     * 
     * @param sessionId 会话ID
     * @param reason 清理原因
     * @return 清理结果
     */
    @PostMapping("/session/{sessionId}")
    //@Auditable(operation = "ADMIN_FORCE_SESSION_CLEANUP", resourceType = "SESSION")
    public ResponseEntity<Map<String, Object>> forceCleanupSession(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "ADMIN_FORCE_CLEANUP") String reason) {
        try {
            log.info("管理员强制清理会话: sessionId={}, reason={}", sessionId, reason);
            
            // 获取会话信息
            SessionInfo sessionInfo = sessionManagementService.getSession(sessionId);
            if (sessionInfo == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "会话不存在或已过期");
                response.put("sessionId", sessionId);
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.notFound().build();
            }
            
            // 执行强制清理
            CompletableFuture<Boolean> cleanupResult = sessionCleanupService.performUserLogoutCleanup(
                    sessionId, sessionInfo.getUserId(), sessionInfo.getUsername(), 
                    sessionInfo.getIpAddress(), reason);
            
            boolean success = cleanupResult.join();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "会话强制清理完成" : "会话强制清理失败");
            response.put("sessionId", sessionId);
            response.put("userId", sessionInfo.getUserId());
            response.put("username", sessionInfo.getUsername());
            response.put("reason", reason);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("会话强制清理完成: sessionId={}, success={}", sessionId, success);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("会话强制清理失败: sessionId={}", sessionId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "会话强制清理失败: " + e.getMessage());
            response.put("sessionId", sessionId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取会话清理统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    //@Auditable(operation = "VIEW_SESSION_CLEANUP_STATISTICS", resourceType = "SESSION_STATISTICS")
    public ResponseEntity<Map<String, Object>> getCleanupStatistics() {
        try {
            log.debug("获取会话清理统计信息");
            
            // 获取会话统计信息
            SessionStatistics sessionStats = sessionManagementService.getSessionStatistics();
            
            // 获取活跃会话列表
            List<SessionInfo> activeSessions = sessionManagementService.getAllActiveSessions();
            
            // 计算需要清理的会话数量
            long expiredSessionsCount = activeSessions.stream()
                    .filter(session -> session.isExpired() || session.isTimeout())
                    .count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalActiveSessions", sessionStats.getTotalActiveSessions());
            response.put("totalOnlineUsers", sessionStats.getTotalOnlineUsers());
            response.put("expiredSessionsCount", expiredSessionsCount);
            response.put("averageSessionDuration", sessionStats.getAverageSessionDuration());
            response.put("maxSessionDuration", sessionStats.getMaxSessionDuration());
            response.put("sessionsByDevice", sessionStats.getSessionsByDevice());
            response.put("sessionsByBrowser", sessionStats.getSessionsByBrowser());
            response.put("sessionsByOS", sessionStats.getSessionsByOS());
            response.put("generatedAt", sessionStats.getGeneratedAt());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取会话清理统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取所有活跃会话列表
     * 
     * @return 活跃会话列表
     */
    @GetMapping("/active-sessions")
    //@Auditable(operation = "VIEW_ACTIVE_SESSIONS", resourceType = "SESSION_LIST")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        try {
            log.debug("获取所有活跃会话列表");
            
            List<SessionInfo> activeSessions = sessionManagementService.getAllActiveSessions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", activeSessions.size());
            response.put("sessions", activeSessions);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取活跃会话列表失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取活跃会话列表失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指定用户的会话信息
     * 
     * @param userId 用户ID
     * @return 用户会话信息
     */
    @GetMapping("/user/{userId}/session")
    //@Auditable(operation = "VIEW_USER_SESSION", resourceType = "USER_SESSION")
    public ResponseEntity<Map<String, Object>> getUserSession(@PathVariable Long userId) {
        try {
            log.debug("获取用户会话信息: userId={}", userId);
            
            SessionInfo userSession = sessionManagementService.getUserActiveSession(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("hasActiveSession", userSession != null);
            response.put("session", userSession);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户会话信息失败: userId={}", userId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取用户会话信息失败: " + e.getMessage());
            response.put("userId", userId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 批量清理多个用户的会话
     * 
     * @param userIds 用户ID列表
     * @param reason 清理原因
     * @return 清理结果
     */
    @PostMapping("/batch-users")
    //@Auditable(operation = "ADMIN_BATCH_USER_SESSION_CLEANUP", resourceType = "USER_SESSION_BATCH")
    public ResponseEntity<Map<String, Object>> batchCleanupUserSessions(
            @RequestBody List<Long> userIds,
            @RequestParam(defaultValue = "ADMIN_BATCH_CLEANUP") String reason) {
        try {
            log.info("管理员批量清理用户会话: userIds={}, reason={}", userIds, reason);
            
            Map<Long, Integer> cleanupResults = new HashMap<>();
            int totalCleaned = 0;
            
            for (Long userId : userIds) {
                try {
                    CompletableFuture<Integer> cleanupResult = sessionCleanupService.performUserAllSessionsCleanup(userId, reason);
                    int cleanedCount = cleanupResult.join();
                    cleanupResults.put(userId, cleanedCount);
                    totalCleaned += cleanedCount;
                } catch (Exception e) {
                    log.error("清理用户会话失败: userId={}", userId, e);
                    cleanupResults.put(userId, -1); // -1表示清理失败
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量用户会话清理完成");
            response.put("totalUsers", userIds.size());
            response.put("totalCleaned", totalCleaned);
            response.put("cleanupResults", cleanupResults);
            response.put("reason", reason);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("批量用户会话清理完成: totalUsers={}, totalCleaned={}", userIds.size(), totalCleaned);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量用户会话清理失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量用户会话清理失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}