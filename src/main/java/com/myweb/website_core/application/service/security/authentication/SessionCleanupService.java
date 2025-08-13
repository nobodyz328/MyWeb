package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.dto.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 会话清理服务
 * <p>
 * 提供用户会话数据的清理功能，包括：
 * - 用户退出时的会话清理
 * - 会话超时的自动清理
 * - Redis缓存数据的清理
 * - 定时清理过期会话
 * - 会话清理的审计记录
 * <p>
 * 符合GB/T 22239-2019二级等保要求的剩余信息保护机制（9.1, 9.2, 9.4）
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCleanupService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditMessageService auditLogService;
    private final SessionManagementService sessionManagementService;
    
    /**
     * 会话超时时间（分钟）
     */
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * 会话最大存活时间（小时）
     */
    private static final int SESSION_MAX_LIFETIME_HOURS = 24;
    
    /**
     * 清理批次大小
     */
    private static final int CLEANUP_BATCH_SIZE = 100;
    
    // 使用统一的Redis键管理
    private static final String CSRF_TOKEN_PREFIX = "csrf:token:";
    private static final String TEMP_DATA_PREFIX = "temp:";
    
    /**
     * 会话清理统计信息
     */
    public static class CleanupStatistics {
        private int clearedSessions = 0;
        private int clearedAuthTokens = 0;
        private int clearedCsrfTokens = 0;
        private int clearedTempData = 0;
        private int clearedUserCache = 0;
        
        // Getters and setters
        public int getClearedSessions() { return clearedSessions; }
        public void setClearedSessions(int clearedSessions) { this.clearedSessions = clearedSessions; }
        
        public int getClearedAuthTokens() { return clearedAuthTokens; }
        public void setClearedAuthTokens(int clearedAuthTokens) { this.clearedAuthTokens = clearedAuthTokens; }
        
        public int getClearedCsrfTokens() { return clearedCsrfTokens; }
        public void setClearedCsrfTokens(int clearedCsrfTokens) { this.clearedCsrfTokens = clearedCsrfTokens; }
        
        public int getClearedTempData() { return clearedTempData; }
        public void setClearedTempData(int clearedTempData) { this.clearedTempData = clearedTempData; }
        
        public int getClearedUserCache() { return clearedUserCache; }
        public void setClearedUserCache(int clearedUserCache) { this.clearedUserCache = clearedUserCache; }
        
        public void incrementClearedSessions() { this.clearedSessions++; }
        public void incrementClearedAuthTokens() { this.clearedAuthTokens++; }
        public void incrementClearedCsrfTokens() { this.clearedCsrfTokens++; }
        public void incrementClearedTempData() { this.clearedTempData++; }
        public void incrementClearedUserCache() { this.clearedUserCache++; }
        
        public int getTotalCleared() {
            return clearedSessions + clearedAuthTokens + clearedCsrfTokens + clearedTempData + clearedUserCache;
        }
        
        @Override
        public String toString() {
            return String.format(
                "清理统计: 会话=%d, 认证令牌=%d, CSRF令牌=%d, 临时数据=%d, 用户缓存=%d, 总计=%d",
                clearedSessions, clearedAuthTokens, clearedCsrfTokens, clearedTempData, clearedUserCache, getTotalCleared()
            );
        }
    }
    
    /**
     * 清理用户会话数据（用户退出时调用）
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @return 清理统计信息
     */
    @Async
    public CompletableFuture<CleanupStatistics> cleanupUserSession(Long userId, String sessionId) {
        try {
            log.info("开始清理用户会话数据 - 用户ID: {}, 会话ID: {}", userId, sessionId);
            
            CleanupStatistics statistics = new CleanupStatistics();
            
            // 1. 清理指定会话
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                clearSpecificSession(sessionId, statistics);
            }
            
            // 2. 清理用户的所有会话
            clearUserSessions(userId, statistics);
            
            // 3. 清理用户认证令牌
            clearUserAuthTokens(userId, statistics);
            
            // 4. 清理用户CSRF令牌
            clearUserCsrfTokens(userId, statistics);
            
            // 5. 清理用户临时数据
            clearUserTempData(userId, statistics);
            
            // 6. 清理用户缓存数据
            clearUserCacheData(userId, statistics);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(userId)
                    .operation(AuditOperation.USER_LOGOUT)
                    .resourceType("SESSION")
                    .resourceId(userId)
                    .result("SUCCESS")
                    .requestData("清理用户会话数据: " + statistics.toString())
                    .build()
            );
            
            log.info("用户会话数据清理完成 - 用户ID: {}, 统计: {}", userId, statistics);
            
            return CompletableFuture.completedFuture(statistics);
            
        } catch (Exception e) {
            log.error("清理用户会话数据失败 - 用户ID: {}", userId, e);
            
            // 记录失败审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(userId)
                    .operation(AuditOperation.USER_LOGOUT)
                    .resourceType("SESSION")
                    .resourceId(userId)
                    .result("FAILURE")
                    .errorMessage(e.getMessage())
                    .build()
            );
            
            return CompletableFuture.completedFuture(new CleanupStatistics());
        }
    }
    
    /**
     * 用户退出时的完整数据清理
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress 客户端IP地址
     * @param reason 清理原因
     * @return 清理是否成功
     */
    @Async
    public CompletableFuture<Boolean> performUserLogoutCleanup(String sessionId, Long userId, 
                                                              String username, String ipAddress, 
                                                              String reason) {
        try {
            log.info("开始执行用户退出清理: sessionId={}, userId={}, reason={}", 
                    sessionId, userId, reason);
            
            // 1. 清理会话相关数据
            cleanupSessionData(sessionId, userId);
            
            // 2. 清理用户缓存数据
            cleanupUserCacheData(userId);
            
            // 3. 清理用户相关的临时数据
            cleanupUserTemporaryData(userId);
            
            // 4. 记录清理审计日志
            recordCleanupAudit(sessionId, userId, username, ipAddress, reason, "USER_LOGOUT_CLEANUP");
            
            log.info("用户退出清理完成: sessionId={}, userId={}", sessionId, userId);
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("用户退出清理失败: sessionId={}, userId={}", sessionId, userId, e);
            
            // 尝试记录清理失败的审计日志，但不让审计失败影响返回结果
            try {
                recordCleanupAudit(sessionId, userId, username, ipAddress, 
                        "CLEANUP_FAILED: " + e.getMessage(), "USER_LOGOUT_CLEANUP_FAILED");
            } catch (Exception auditException) {
                log.error("记录清理失败审计日志也失败: sessionId={}, userId={}", sessionId, userId, auditException);
            }
            
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 会话超时自动清理
     * 
     * @param sessionId 会话ID
     * @param sessionInfo 会话信息
     * @return 清理是否成功
     */
    @Async
    public CompletableFuture<Boolean> performTimeoutCleanup(String sessionId, SessionInfo sessionInfo) {
        try {
            if (sessionInfo == null) {
                log.warn("会话信息为空，无法执行清理: sessionId={}", sessionId);
                return CompletableFuture.completedFuture(false);
            }
            
            log.info("开始执行会话超时清理: sessionId={}, userId={}", 
                    sessionId, sessionInfo.getUserId());
            
            // 1. 清理会话数据
            cleanupSessionData(sessionId, sessionInfo.getUserId());
            
            // 2. 清理用户缓存数据
            cleanupUserCacheData(sessionInfo.getUserId());
            
            // 3. 记录清理审计日志
            recordCleanupAudit(sessionId, sessionInfo.getUserId(), sessionInfo.getUsername(), 
                    sessionInfo.getIpAddress(), "SESSION_TIMEOUT", "SESSION_TIMEOUT_CLEANUP");
            
            log.info("会话超时清理完成: sessionId={}, userId={}", sessionId, sessionInfo.getUserId());
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("会话超时清理失败: sessionId={}, userId={}", sessionId, 
                    sessionInfo != null ? sessionInfo.getUserId() : "unknown", e);
            
            // 记录清理失败的审计日志
            if (sessionInfo != null) {
                recordCleanupAudit(sessionId, sessionInfo.getUserId(), sessionInfo.getUsername(), 
                        sessionInfo.getIpAddress(), "CLEANUP_FAILED: " + e.getMessage(), 
                        "SESSION_TIMEOUT_CLEANUP_FAILED");
            }
            
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 批量清理过期会话
     * 
     * @return 清理的会话数量
     */
    @Async
    public CompletableFuture<Integer> performBatchExpiredSessionCleanup() {
        try {
            log.debug("开始批量清理过期会话");
            
            Set<Object> sessionIds = redisTemplate.opsForSet().members(RedisKey.ACTIVE_SESSIONS_SET);
            if (sessionIds == null || sessionIds.isEmpty()) {
                return CompletableFuture.completedFuture(0);
            }
            
            int cleanedCount = 0;
            int batchCount = 0;
            List<String> expiredSessions = new ArrayList<>();
            
            // 分批处理会话
            for (Object sessionIdObj : sessionIds) {
                if (sessionIdObj instanceof String sessionId) {
                    SessionInfo sessionInfo = getSessionInfoSafely(sessionId);
                    
                    // 检查会话是否过期或超时
                    if (sessionInfo == null || sessionInfo.isExpired() || sessionInfo.isTimeout()) {
                        expiredSessions.add(sessionId);
                        
                        if (sessionInfo != null) {
                            // 执行清理
                            performTimeoutCleanup(sessionId, sessionInfo).join();
                        } else {
                            // 直接清理无效会话数据
                            cleanupOrphanedSessionData(sessionId);
                        }
                        
                        cleanedCount++;
                        batchCount++;
                        
                        // 达到批次大小时暂停，避免Redis压力过大
                        if (batchCount >= CLEANUP_BATCH_SIZE) {
                            Thread.sleep(100); // 暂停100ms
                            batchCount = 0;
                        }
                    }
                }
            }
            
            if (cleanedCount > 0) {
                log.info("批量清理过期会话完成，清理数量: {}", cleanedCount);
                
                // 记录批量清理审计日志
                recordBatchCleanupAudit(cleanedCount, expiredSessions);
            }
            
            return CompletableFuture.completedFuture(cleanedCount);
            
        } catch (Exception e) {
            log.error("批量清理过期会话失败", e);
            return CompletableFuture.completedFuture(0);
        }
    }
    
    /**
     * 清理用户的所有会话数据
     * 
     * @param userId 用户ID
     * @param reason 清理原因
     * @return 清理的会话数量
     */
    @Async
    public CompletableFuture<Integer> performUserAllSessionsCleanup(Long userId, String reason) {
        try {
            log.info("开始清理用户所有会话: userId={}, reason={}", userId, reason);
            
            int cleanedCount = 0;
            
            // 1. 获取用户当前活跃会话
            String userSessionKey = RedisKey.userActiveSessionKey(userId);
            Object activeSessionId = redisTemplate.opsForValue().get(userSessionKey);
            
            if (activeSessionId instanceof String sessionId) {
                SessionInfo sessionInfo = getSessionInfoSafely(sessionId);
                if (sessionInfo != null) {
                    performUserLogoutCleanup(sessionId, userId, sessionInfo.getUsername(), 
                            sessionInfo.getIpAddress(), reason).join();
                    cleanedCount++;
                }
            }
            
            // 2. 清理可能存在的其他会话数据
            cleanupUserCacheData(userId);
            cleanupUserTemporaryData(userId);
            
            // 3. 清理用户相关的Redis键
            cleanupUserRelatedKeys(userId);
            
            log.info("用户所有会话清理完成: userId={}, cleanedCount={}", userId, cleanedCount);
            return CompletableFuture.completedFuture(cleanedCount);
            
        } catch (Exception e) {
            log.error("清理用户所有会话失败: userId={}", userId, e);
            return CompletableFuture.completedFuture(0);
        }
    }
    
    /**
     * 定时清理过期会话（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void scheduledExpiredSessionCleanup() {
        try {
            log.debug("开始定时清理过期会话");
            
            CompletableFuture<Integer> cleanupResult = performBatchExpiredSessionCleanup();
            int cleanedCount = cleanupResult.join();
            
            if (cleanedCount > 0) {
                log.info("定时清理过期会话完成，清理数量: {}", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("定时清理过期会话失败", e);
        }
    }
    
    /**
     * 清理会话超时数据（定时任务）
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void cleanupExpiredSessions() {
        try {
            log.debug("开始清理过期会话数据");
            
            CleanupStatistics statistics = new CleanupStatistics();
            
            // 清理过期的会话数据
            clearExpiredSessions(statistics);
            
            // 清理过期的认证令牌
            clearExpiredAuthTokens(statistics);
            
            // 清理过期的CSRF令牌
            clearExpiredCsrfTokens(statistics);
            
            // 清理过期的临时数据
            clearExpiredTempData(statistics);
            
            if (statistics.getTotalCleared() > 0) {
                // 记录审计日志
                auditLogService.logOperation(
                    AuditLogRequest.builder()
                        .userId(0L)
                        .username("SYSTEM")
                        .operation(AuditOperation.SYSTEM_MONITOR)
                        .resourceType("SESSION")
                        .result("SUCCESS")
                        .requestData("定时清理过期会话数据: " + statistics.toString())
                        .build()
                );
                
                log.info("过期会话数据清理完成 - 统计: {}", statistics);
            }
            
        } catch (Exception e) {
            log.error("定时清理过期会话数据失败", e);
        }
    }
    
    /**
     * 定时清理孤立的会话数据（每小时执行一次）
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void scheduledOrphanedDataCleanup() {
        try {
            log.debug("开始定时清理孤立的会话数据");
            
            int cleanedCount = cleanupOrphanedSessionKeys();
            
            if (cleanedCount > 0) {
                log.info("定时清理孤立会话数据完成，清理数量: {}", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("定时清理孤立会话数据失败", e);
        }
    }
    
    /**
     * 清理过期认证令牌
     */
    private void clearExpiredAuthTokens(CleanupStatistics statistics) {
        try {
            Set<String> authTokenKeys = redisTemplate.keys(RedisKey.AUTH_TOKEN_PREFIX + "*");
            if (authTokenKeys != null) {
                for (String key : authTokenKeys) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                        statistics.incrementClearedAuthTokens();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理过期认证令牌失败", e);
        }
    }
    
    /**
     * 清理过期CSRF令牌
     */
    private void clearExpiredCsrfTokens(CleanupStatistics statistics) {
        try {
            Set<String> csrfTokenKeys = redisTemplate.keys(CSRF_TOKEN_PREFIX + "*");
            if (csrfTokenKeys != null) {
                for (String key : csrfTokenKeys) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                        statistics.incrementClearedCsrfTokens();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理过期CSRF令牌失败", e);
        }
    }
    
    /**
     * 清理过期临时数据
     */
    private void clearExpiredTempData(CleanupStatistics statistics) {
        try {
            Set<String> tempKeys = redisTemplate.keys(TEMP_DATA_PREFIX + "*");
            if (tempKeys != null) {
                for (String key : tempKeys) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                        statistics.incrementClearedTempData();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理过期临时数据失败", e);
        }
    }
    
    /**
     * 清理会话相关数据
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    private void cleanupSessionData(String sessionId, Long userId) {
        try {
            // 删除会话信息
            String sessionKey = RedisKey.sessionKey(sessionId);
            redisTemplate.delete(sessionKey);
            
            // 删除用户会话映射
            String userSessionKey = RedisKey.userActiveSessionKey(userId);
            redisTemplate.delete(userSessionKey);
            
            // 从活跃会话集合中移除
            redisTemplate.opsForSet().remove(RedisKey.ACTIVE_SESSIONS_SET, sessionId);
            
            // 删除会话活动记录
            String activityKey = RedisKey.sessionActivityKey(sessionId);
            redisTemplate.delete(activityKey);
            
            log.debug("会话数据清理完成: sessionId={}, userId={}", sessionId, userId);
            
        } catch (Exception e) {
            log.error("清理会话数据失败: sessionId={}, userId={}", sessionId, userId, e);
            throw e; // Re-throw to let the caller handle the failure
        }
    }
    
    /**
     * 清理用户缓存数据
     * 
     * @param userId 用户ID
     */
    private void cleanupUserCacheData(Long userId) {
        try {
            // 清理用户相关的缓存键
            Set<String> userCacheKeys = Set.of(
                    "user:profile:" + userId,
                    "user:permissions:" + userId,
                    "user:roles:" + userId,
                    "user:settings:" + userId,
                    "user:preferences:" + userId,
                    "user:stats:" + userId
            );
            
            for (String key : userCacheKeys) {
                redisTemplate.delete(key);
            }
            
            // 清理用户相关的点赞和收藏缓存
            cleanupUserInteractionCache(userId);
            
            log.debug("用户缓存数据清理完成: userId={}", userId);
            
        } catch (Exception e) {
            log.error("清理用户缓存数据失败: userId={}", userId, e);
            throw e; // Re-throw to let the caller handle the failure
        }
    }
    
    /**
     * 清理用户临时数据
     * 
     * @param userId 用户ID
     */
    private void cleanupUserTemporaryData(Long userId) {
        try {
            // 清理用户临时文件上传记录
            String tempUploadKey = "temp:upload:" + userId;
            redisTemplate.delete(tempUploadKey);
            
            // 清理用户搜索历史
            String searchHistoryKey = "search:history:" + userId;
            redisTemplate.delete(searchHistoryKey);
            
            // 清理用户操作锁
            String operationLockKey = "operation:lock:" + userId;
            redisTemplate.delete(operationLockKey);
            
            // 清理用户验证码
            String verificationKey = "verification:" + userId;
            redisTemplate.delete(verificationKey);
            
            log.debug("用户临时数据清理完成: userId={}", userId);
            
        } catch (Exception e) {
            log.error("清理用户临时数据失败: userId={}", userId, e);
            throw e; // Re-throw to let the caller handle the failure
        }
    }
    
    /**
     * 清理用户交互缓存（点赞、收藏等）
     * 
     * @param userId 用户ID
     */
    private void cleanupUserInteractionCache(Long userId) {
        try {
            // 获取用户相关的点赞和收藏键
            Set<String> keys = redisTemplate.keys("user:like:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            keys = redisTemplate.keys("user:collect:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            log.debug("用户交互缓存清理完成: userId={}", userId);
            
        } catch (Exception e) {
            log.error("清理用户交互缓存失败: userId={}", userId, e);
        }
    }
    
    /**
     * 清理用户相关的所有Redis键
     * 
     * @param userId 用户ID
     */
    private void cleanupUserRelatedKeys(Long userId) {
        try {
            // 查找所有包含用户ID的键
            Set<String> userKeys = redisTemplate.keys("*:" + userId + ":*");
            if (userKeys != null && !userKeys.isEmpty()) {
                redisTemplate.delete(userKeys);
            }
            
            // 查找以用户ID结尾的键
            Set<String> userEndKeys = redisTemplate.keys("*:" + userId);
            if (userEndKeys != null && !userEndKeys.isEmpty()) {
                redisTemplate.delete(userEndKeys);
            }
            
            log.debug("用户相关Redis键清理完成: userId={}", userId);
            
        } catch (Exception e) {
            log.error("清理用户相关Redis键失败: userId={}", userId, e);
        }
    }
    
    /**
     * 清理孤立的会话数据
     * 
     * @param sessionId 会话ID
     */
    private void cleanupOrphanedSessionData(String sessionId) {
        try {
            // 删除会话相关的所有键
            String sessionKey = RedisKey.sessionKey(sessionId);
            redisTemplate.delete(sessionKey);
            
            String activityKey = RedisKey.sessionActivityKey(sessionId);
            redisTemplate.delete(activityKey);
            
            // 从活跃会话集合中移除
            redisTemplate.opsForSet().remove(RedisKey.ACTIVE_SESSIONS_SET, sessionId);
            
            log.debug("孤立会话数据清理完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            log.error("清理孤立会话数据失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 清理孤立的会话键
     * 
     * @return 清理的键数量
     */
    private int cleanupOrphanedSessionKeys() {
        try {
            int cleanedCount = 0;
            
            // 查找所有会话相关的键
            Set<String> sessionKeys = redisTemplate.keys(RedisKey.SESSION_PREFIX + "*");
            if (sessionKeys != null) {
                for (String key : sessionKeys) {
                    // 检查键是否还在活跃会话集合中
                    String sessionId = key.substring(RedisKey.SESSION_PREFIX.length());
                    Boolean isMember = redisTemplate.opsForSet().isMember(RedisKey.ACTIVE_SESSIONS_SET, sessionId);
                    
                    if (Boolean.FALSE.equals(isMember)) {
                        // 这是一个孤立的会话键，删除它
                        redisTemplate.delete(key);
                        cleanedCount++;
                    }
                }
            }
            
            // 清理孤立的会话活动键
            Set<String> activityKeys = redisTemplate.keys(RedisKey.SESSION_ACTIVITY_PREFIX + "*");
            if (activityKeys != null) {
                for (String key : activityKeys) {
                    String sessionId = key.substring(RedisKey.SESSION_ACTIVITY_PREFIX.length());
                    Boolean isMember = redisTemplate.opsForSet().isMember(RedisKey.ACTIVE_SESSIONS_SET, sessionId);
                    
                    if (Boolean.FALSE.equals(isMember)) {
                        redisTemplate.delete(key);
                        cleanedCount++;
                    }
                }
            }
            
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("清理孤立会话键失败", e);
            return 0;
        }
    }
    
    /**
     * 安全获取会话信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息，如果获取失败则返回null
     */
    private SessionInfo getSessionInfoSafely(String sessionId) {
        try {
            return sessionManagementService.getSession(sessionId);
        } catch (Exception e) {
            log.warn("获取会话信息失败: sessionId={}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 记录清理审计日志
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @param reason 清理原因
     * @param operation 操作类型
     */
    private void recordCleanupAudit(String sessionId, Long userId, String username, 
                                   String ipAddress, String reason, String operation) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("sessionId", sessionId);
            details.put("reason", reason);
            details.put("cleanupTime", LocalDateTime.now());
            
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                    .userId(userId)
                    .username(username)
                    .operation(AuditOperation.SESSION_CLEANUP)
                    .resourceType("SESSION")
                    .resourceId(userId) // Use userId as resourceId since sessionId is String
                    .ipAddress(ipAddress)
                    .result("SUCCESS")
                    .requestData(details)
                    .build();
            
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录清理审计日志失败: sessionId={}, userId={}", sessionId, userId, e);
        }
    }
    
    /**
     * 记录批量清理审计日志
     * 
     * @param cleanedCount 清理数量
     * @param expiredSessions 过期会话列表
     */
    private void recordBatchCleanupAudit(int cleanedCount, List<String> expiredSessions) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("cleanedCount", cleanedCount);
            details.put("expiredSessions", expiredSessions.size() > 10 ? 
                    expiredSessions.subList(0, 10) : expiredSessions); // 只记录前10个
            details.put("cleanupTime", LocalDateTime.now());
            details.put("cleanupType", "BATCH_EXPIRED_SESSION_CLEANUP");
            
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                    .userId(0L) // 系统操作
                    .username("SYSTEM")
                    .operation(AuditOperation.SESSION_CLEANUP)
                    .resourceType("SESSION_BATCH")
                    .result("SUCCESS")
                    .requestData(details)
                    .build();
            
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录批量清理审计日志失败: cleanedCount={}", cleanedCount, e);
        }
    }
    
    /**
     * 清理指定会话
     */
    private void clearSpecificSession(String sessionId, CleanupStatistics statistics) {
        try {
            String sessionKey = RedisKey.sessionKey(sessionId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                redisTemplate.delete(sessionKey);
                statistics.incrementClearedSessions();
                log.debug("清理指定会话: {}", sessionId);
            }
        } catch (Exception e) {
            log.warn("清理指定会话失败: {}", sessionId, e);
        }
    }
    
    /**
     * 清理用户的所有会话
     */
    private void clearUserSessions(Long userId, CleanupStatistics statistics) {
        try {
            // 清理用户会话映射
            Set<String> userSessionKeys = redisTemplate.keys(RedisKey.USER_SESSION_PREFIX + userId + ":*");
            if (userSessionKeys != null && !userSessionKeys.isEmpty()) {
                redisTemplate.delete(userSessionKeys);
                statistics.setClearedSessions(statistics.getClearedSessions() + userSessionKeys.size());
            }
            
            // 清理Spring Session会话
            Set<String> sessionKeys = redisTemplate.keys(RedisKey.SESSION_PREFIX + "*");
            if (sessionKeys != null) {
                for (String key : sessionKeys) {
                    Object sessionData = redisTemplate.opsForValue().get(key);
                    if (sessionData != null && sessionData.toString().contains(userId.toString())) {
                        redisTemplate.delete(key);
                        statistics.incrementClearedSessions();
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("清理用户会话失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 清理用户认证令牌
     */
    private void clearUserAuthTokens(Long userId, CleanupStatistics statistics) {
        try {
            Set<String> authTokenKeys = redisTemplate.keys(RedisKey.AUTH_TOKEN_PREFIX + userId + ":*");
            if (authTokenKeys != null && !authTokenKeys.isEmpty()) {
                redisTemplate.delete(authTokenKeys);
                statistics.setClearedAuthTokens(authTokenKeys.size());
            }
            
            // 清理JWT令牌黑名单相关的键
            Set<String> jwtKeys = redisTemplate.keys("jwt:blacklist:" + userId + ":*");
            if (jwtKeys != null && !jwtKeys.isEmpty()) {
                redisTemplate.delete(jwtKeys);
                statistics.setClearedAuthTokens(statistics.getClearedAuthTokens() + jwtKeys.size());
            }
            
        } catch (Exception e) {
            log.warn("清理用户认证令牌失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 清理用户CSRF令牌
     */
    private void clearUserCsrfTokens(Long userId, CleanupStatistics statistics) {
        try {
            Set<String> csrfTokenKeys = redisTemplate.keys(CSRF_TOKEN_PREFIX + userId + ":*");
            if (csrfTokenKeys != null && !csrfTokenKeys.isEmpty()) {
                redisTemplate.delete(csrfTokenKeys);
                statistics.setClearedCsrfTokens(csrfTokenKeys.size());
            }
        } catch (Exception e) {
            log.warn("清理用户CSRF令牌失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 清理用户临时数据
     */
    private void clearUserTempData(Long userId, CleanupStatistics statistics) {
        try {
            String[] tempPatterns = {
                TEMP_DATA_PREFIX + "user:" + userId + ":*",
                "upload:temp:" + userId + ":*",
                "verification:" + userId + ":*",
                "captcha:" + userId + ":*",
                "rate_limit:" + userId + ":*"
            };
            
            for (String pattern : tempPatterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    statistics.setClearedTempData(statistics.getClearedTempData() + keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("清理用户临时数据失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 清理用户缓存数据
     */
    private void clearUserCacheData(Long userId, CleanupStatistics statistics) {
        try {
            String[] cachePatterns = {
                "user:profile:" + userId,
                "user:permissions:" + userId,
                "user:roles:" + userId,
                "user:posts:" + userId + ":*",
                "user:comments:" + userId + ":*",
                "user:likes:" + userId + ":*",
                "user:collects:" + userId + ":*",
                "user:followers:" + userId,
                "user:following:" + userId
            };
            
            for (String pattern : cachePatterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    statistics.setClearedUserCache(statistics.getClearedUserCache() + keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("清理用户缓存数据失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 强制清理用户的所有会话（管理员操作）
     * 
     * @param userId 用户ID
     * @param operatorUserId 操作者用户ID
     * @return 清理统计信息
     */
    public CleanupStatistics forceCleanupUserSessions(Long userId, Long operatorUserId) {
        try {
            log.info("强制清理用户会话 - 用户ID: {}, 操作者: {}", userId, operatorUserId);
            
            CleanupStatistics statistics = new CleanupStatistics();
            
            // 清理用户的所有会话数据
            clearUserSessions(userId, statistics);
            clearUserAuthTokens(userId, statistics);
            clearUserCsrfTokens(userId, statistics);
            clearUserTempData(userId, statistics);
            clearUserCacheData(userId, statistics);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(operatorUserId)
                    .operation(AuditOperation.SESSION_CLEANUP)
                    .resourceType("SESSION")
                    .resourceId(userId)
                    .result("SUCCESS")
                    .requestData("强制清理用户会话: " + statistics.toString())
                    .build()
            );
            
            log.info("强制清理用户会话完成 - 用户ID: {}, 统计: {}", userId, statistics);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("强制清理用户会话失败 - 用户ID: {}", userId, e);
            return new CleanupStatistics();
        }
    }
    
    /**
     * 清理系统重启时的临时数据
     */
    public void cleanupOnSystemRestart() {
        try {
            log.info("系统重启，开始清理临时数据");
            
            CleanupStatistics statistics = new CleanupStatistics();
            
            // 清理所有临时数据
            clearAllTempData(statistics);
            
            // 清理所有过期会话
            clearExpiredSessions(statistics);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.builder()
                    .userId(0L)
                    .username("SYSTEM")
                    .operation(AuditOperation.SYSTEM_MONITOR)
                    .resourceType("SYSTEM")
                    .result("SUCCESS")
                    .requestData("系统重启清理临时数据: " + statistics.toString())
                    .build()
            );
            
            log.info("系统重启临时数据清理完成 - 统计: {}", statistics);
            
        } catch (Exception e) {
            log.error("系统重启清理临时数据失败", e);
        }
    }
    
    /**
     * 清理所有临时数据
     */
    private void clearAllTempData(CleanupStatistics statistics) {
        try {
            String[] tempPatterns = {
                TEMP_DATA_PREFIX + "*",
                "upload:temp:*",
                "cache:temp:*",
                "session:temp:*"
            };
            
            for (String pattern : tempPatterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    statistics.setClearedTempData(statistics.getClearedTempData() + keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("清理所有临时数据失败", e);
        }
    }
    
    /**
     * 清理过期会话
     */
    private void clearExpiredSessions(CleanupStatistics statistics) {
        try {
            // Redis的TTL机制会自动清理过期的会话
            // 这里主要是清理一些可能遗留的会话数据
            Set<String> sessionKeys = redisTemplate.keys(RedisKey.SESSION_PREFIX + "*");
            if (sessionKeys != null) {
                for (String key : sessionKeys) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                        statistics.incrementClearedSessions();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理过期会话失败", e);
        }
    }
    
    /**
     * 获取会话统计信息
     * 
     * @return 会话统计信息
     */
    public Map<String, Object> getSessionStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 统计活跃会话数
            Set<String> sessionKeys = redisTemplate.keys(RedisKey.SESSION_PREFIX + "*");
            statistics.put("activeSessions", sessionKeys != null ? sessionKeys.size() : 0);
            
            // 统计认证令牌数
            Set<String> authTokenKeys = redisTemplate.keys(RedisKey.AUTH_TOKEN_PREFIX + "*");
            statistics.put("authTokens", authTokenKeys != null ? authTokenKeys.size() : 0);
            
            // 统计CSRF令牌数
            Set<String> csrfTokenKeys = redisTemplate.keys(CSRF_TOKEN_PREFIX + "*");
            statistics.put("csrfTokens", csrfTokenKeys != null ? csrfTokenKeys.size() : 0);
            
            // 统计临时数据数
            Set<String> tempKeys = redisTemplate.keys(TEMP_DATA_PREFIX + "*");
            statistics.put("tempData", tempKeys != null ? tempKeys.size() : 0);
            
            statistics.put("timestamp", LocalDateTime.now());
            
            return statistics;
            
        } catch (Exception e) {
            log.error("获取会话统计信息失败", e);
            return new HashMap<>();
        }
    }
}