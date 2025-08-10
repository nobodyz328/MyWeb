package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.application.service.security.authentication.JWT.JwtService;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.security.dto.SessionInfo;
import com.myweb.website_core.domain.security.dto.SessionStatistics;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 会话管理服务
 * <p>
 * 提供用户会话的全生命周期管理，包括：
 * - 会话创建和销毁
 * - 会话超时检查和自动清理
 * - 单用户单会话限制
 * - 会话监控和统计
 * - 会话数据清理
 * <p>
 * 符合GB/T 22239-2019身份鉴别和剩余信息保护要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    
    /**
     * 会话超时时间（分钟）
     */
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * 会话最大存活时间（小时）
     */
    private static final int SESSION_MAX_LIFETIME_HOURS = 24;
    
    /**
     * 统计数据缓存时间（分钟）
     */
    private static final int STATS_CACHE_MINUTES = 5;
    
    /**
     * 创建用户会话
     * 
     * @param user 用户信息
     * @param sessionId 会话ID
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理字符串
     * @param accessToken JWT访问令牌
     * @param refreshToken JWT刷新令牌
     * @return 会话信息
     */
    @Async
    public CompletableFuture<SessionInfo> createSession(User user, String sessionId, 
                                                       String ipAddress, String userAgent,
                                                       String accessToken, String refreshToken) {
        try {
            // 检查并清理用户的旧会话（单用户单会话限制）
            terminateUserExistingSessions(user.getId()).join();
            
            LocalDateTime now = LocalDateTime.now();
            
            // 解析用户代理信息
            Map<String, String> userAgentInfo = parseUserAgent(userAgent);
            
            // 创建会话信息
            SessionInfo sessionInfo = SessionInfo.builder()
                    .sessionId(sessionId)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .loginTime(now)
                    .lastActivityTime(now)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .active(true)
                    .expirationTime(now.plusHours(SESSION_MAX_LIFETIME_HOURS))
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .deviceType(userAgentInfo.get("device"))
                    .browserType(userAgentInfo.get("browser"))
                    .osType(userAgentInfo.get("os"))
                    .build();
            
            // 存储会话信息到Redis
            String sessionKey = RedisKey.sessionKey(sessionId);
            redisTemplate.opsForValue().set(sessionKey, sessionInfo, SESSION_MAX_LIFETIME_HOURS, TimeUnit.HOURS);
            
            // 建立用户到会话的映射
            String userSessionKey = RedisKey.userActiveSessionKey(user.getId());
            redisTemplate.opsForValue().set(userSessionKey, sessionId, SESSION_MAX_LIFETIME_HOURS, TimeUnit.HOURS);
            
            // 添加到活跃会话集合
            redisTemplate.opsForSet().add(RedisKey.ACTIVE_SESSIONS_SET, sessionId);
            
            // 记录会话活动
            recordSessionActivity(sessionId, "SESSION_CREATED", ipAddress);


            return CompletableFuture.completedFuture(sessionInfo);
            
        } catch (Exception e) {
            log.error("创建用户会话失败: userId={}, sessionId={}", user.getId(), sessionId, e);
            throw new RuntimeException("创建用户会话失败", e);
        }
    }
    
    /**
     * 获取会话信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息，如果不存在则返回null
     */
    public SessionInfo getSession(String sessionId) {
        try {
            String sessionKey = RedisKey.sessionKey(sessionId);
            Object sessionObj = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionObj instanceof SessionInfo sessionInfo) {
                // 检查会话是否过期或超时
                if (sessionInfo.isExpired() || sessionInfo.isTimeout()) {
                    terminateSession(sessionId, "SESSION_TIMEOUT");
                    return null;
                }
                
                return sessionInfo;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取会话信息失败: sessionId={}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 更新会话活动时间
     * 
     * @param sessionId 会话ID
     * @param ipAddress 客户端IP地址
     * @return 更新是否成功
     */
    @Async
    public CompletableFuture<Boolean> updateSessionActivity(String sessionId, String ipAddress) {
        try {
            SessionInfo sessionInfo = getSession(sessionId);
            if (sessionInfo == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            // 更新最后活动时间
            sessionInfo.updateLastActivity();
            
            // 保存更新后的会话信息
            String sessionKey = RedisKey.sessionKey(sessionId);
            redisTemplate.opsForValue().set(sessionKey, sessionInfo, SESSION_MAX_LIFETIME_HOURS, TimeUnit.HOURS);
            
            // 记录会话活动
            recordSessionActivity(sessionId, "SESSION_ACTIVITY", ipAddress);
            
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("更新会话活动时间失败: sessionId={}", sessionId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 终止指定会话
     * 
     * @param sessionId 会话ID
     * @param reason 终止原因
     * @return 终止是否成功
     */
    @Async
    public CompletableFuture<Boolean> terminateSession(String sessionId, String reason) {
        try {
            // 直接从Redis获取会话信息，避免循环调用
            String sessionKey = RedisKey.sessionKey(sessionId);
            Object sessionObj = redisTemplate.opsForValue().get(sessionKey);
            
            if (!(sessionObj instanceof SessionInfo sessionInfo)) {
                return CompletableFuture.completedFuture(false);
            }
            
            // 清理会话数据
            cleanupSessionData(sessionId, sessionInfo.getUserId());
            
            // 记录会话活动
            recordSessionActivity(sessionId, "SESSION_TERMINATED", null);

            log.info("会话终止成功: sessionId={}, userId={}, reason={}", 
                    sessionId, sessionInfo.getUserId(), reason);
            
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("终止会话失败: sessionId={}, reason={}", sessionId, reason, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 终止用户的所有现有会话
     * 
     * @param userId 用户ID
     * @return 终止的会话数量
     */
    @Async
    public CompletableFuture<Integer> terminateUserExistingSessions(Long userId) {
        try {
            int terminatedCount = 0;
            
            // 获取用户当前活跃会话
            String userSessionKey = RedisKey.userActiveSessionKey(userId);
            Object existingSessionId = redisTemplate.opsForValue().get(userSessionKey);
            
            if (existingSessionId instanceof String sessionId) {
                // 终止现有会话
                boolean terminated = terminateSession(sessionId, "NEW_LOGIN_OVERRIDE").join();
                if (terminated) {
                    terminatedCount++;
                    log.info("因新登录而终止用户旧会话: userId={}, oldSessionId={}", userId, sessionId);
                }
            }
            
            return CompletableFuture.completedFuture(terminatedCount);
            
        } catch (Exception e) {
            log.error("终止用户现有会话失败: userId={}", userId, e);
            return CompletableFuture.completedFuture(0);
        }
    }
    
    /**
     * 用户主动退出登录
     * 
     * @param sessionId 会话ID
     * @param ipAddress 客户端IP地址
     * @return 退出是否成功
     */
    @Async
    public CompletableFuture<Boolean> userLogout(String sessionId, String ipAddress) {
        try {
            SessionInfo sessionInfo = getSession(sessionId);
            if (sessionInfo == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            // 记录用户主动退出
            recordSessionActivity(sessionId, "USER_LOGOUT", ipAddress);
            
            // 终止会话
            return terminateSession(sessionId, "USER_LOGOUT");
            
        } catch (Exception e) {
            log.error("用户退出登录失败: sessionId={}", sessionId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 用户主动退出登录（使用清理服务）
     * 
     * @param sessionId 会话ID
     * @param ipAddress 客户端IP地址
     * @return 退出是否成功
     */
    @Async
    public CompletableFuture<Boolean> userLogoutWithCleanup(String sessionId, String ipAddress) {
        try {
            SessionInfo sessionInfo = getSession(sessionId);
            if (sessionInfo == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            // 记录用户主动退出
            recordSessionActivity(sessionId, "USER_LOGOUT", ipAddress);
            
            // 注意：这里不能直接注入SessionCleanupService，因为会造成循环依赖
            // 改为直接调用清理逻辑
            cleanupSessionData(sessionId, sessionInfo.getUserId());

            log.info("用户退出登录成功: sessionId={}, userId={}", sessionId, sessionInfo.getUserId());
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("用户退出登录失败: sessionId={}", sessionId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 获取用户当前活跃会话
     * 
     * @param userId 用户ID
     * @return 会话信息，如果不存在则返回null
     */
    public SessionInfo getUserActiveSession(Long userId) {
        try {
            String userSessionKey = RedisKey.userActiveSessionKey(userId);
            Object sessionId = redisTemplate.opsForValue().get(userSessionKey);
            
            if (sessionId instanceof String) {
                return getSession((String) sessionId);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取用户活跃会话失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 获取所有活跃会话列表
     * 
     * @return 活跃会话列表
     */
    public List<SessionInfo> getAllActiveSessions() {
        try {
            Set<Object> sessionIds = redisTemplate.opsForSet().members(RedisKey.ACTIVE_SESSIONS_SET);
            if (sessionIds == null || sessionIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<SessionInfo> activeSessions = new ArrayList<>();
            for (Object sessionIdObj : sessionIds) {
                if (sessionIdObj instanceof String sessionId) {
                    SessionInfo sessionInfo = getSession(sessionId);
                    if (sessionInfo != null && sessionInfo.getActive()) {
                        activeSessions.add(sessionInfo);
                    }
                }
            }
            
            return activeSessions;
            
        } catch (Exception e) {
            log.error("获取所有活跃会话失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取会话统计信息
     * 
     * @return 会话统计信息
     */
    public SessionStatistics getSessionStatistics() {
        try {
            String statsKey = RedisKey.sessionStatsKey("current");
            Object cachedStats = redisTemplate.opsForValue().get(statsKey);
            
            if (cachedStats instanceof SessionStatistics stats && !stats.isExpired()) {
                return stats;
            }
            
            // 计算实时统计数据
            SessionStatistics statistics = calculateSessionStatistics();
            
            // 缓存统计数据
            redisTemplate.opsForValue().set(statsKey, statistics, STATS_CACHE_MINUTES, TimeUnit.MINUTES);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("获取会话统计信息失败", e);
            return SessionStatistics.builder()
                    .totalOnlineUsers(0L)
                    .totalActiveSessions(0L)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * 定时清理过期会话（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupExpiredSessions() {
        try {
            log.debug("开始清理过期会话");
            
            Set<Object> sessionIds = redisTemplate.opsForSet().members(RedisKey.ACTIVE_SESSIONS_SET);
            if (sessionIds == null || sessionIds.isEmpty()) {
                return;
            }
            
            int cleanedCount = 0;
            for (Object sessionIdObj : sessionIds) {
                if (sessionIdObj instanceof String sessionId) {
                    SessionInfo sessionInfo = getSession(sessionId);
                    
                    // 检查会话是否过期或超时
                    if (sessionInfo == null || sessionInfo.isExpired() || sessionInfo.isTimeout()) {
                        terminateSession(sessionId, "SESSION_CLEANUP");
                        cleanedCount++;
                    }
                }
            }
            
            if (cleanedCount > 0) {
                log.info("清理过期会话完成，清理数量: {}", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("清理过期会话失败", e);
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
        }
    }
    
    /**
     * 记录会话活动
     * 
     * @param sessionId 会话ID
     * @param activity 活动类型
     * @param ipAddress IP地址
     */
    private void recordSessionActivity(String sessionId, String activity, String ipAddress) {
        try {
            String activityKey = RedisKey.sessionActivityKey(sessionId);
            String activityRecord = String.format("%s|%s|%s", 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    activity, 
                    ipAddress != null ? ipAddress : "unknown");
            
            redisTemplate.opsForList().leftPush(activityKey, activityRecord);
            redisTemplate.expire(activityKey, SESSION_MAX_LIFETIME_HOURS, TimeUnit.HOURS);
            
            // 只保留最近50条活动记录
            redisTemplate.opsForList().trim(activityKey, 0, 49);
            
        } catch (Exception e) {
            log.error("记录会话活动失败: sessionId={}, activity={}", sessionId, activity, e);
        }
    }
    
    /**
     * 解析用户代理字符串
     * 
     * @param userAgent 用户代理字符串
     * @return 解析结果Map
     */
    private Map<String, String> parseUserAgent(String userAgent) {
        Map<String, String> result = new HashMap<>();
        
        if (userAgent == null || userAgent.trim().isEmpty()) {
            result.put("device", "Unknown");
            result.put("browser", "Unknown");
            result.put("os", "Unknown");
            return result;
        }
        
        String ua = userAgent.toLowerCase();
        
        // 检测设备类型
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            result.put("device", "Mobile");
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            result.put("device", "Tablet");
        } else {
            result.put("device", "Desktop");
        }
        
        // 检测浏览器类型
        if (ua.contains("chrome")) {
            result.put("browser", "Chrome");
        } else if (ua.contains("firefox")) {
            result.put("browser", "Firefox");
        } else if (ua.contains("safari")) {
            result.put("browser", "Safari");
        } else if (ua.contains("edge")) {
            result.put("browser", "Edge");
        } else {
            result.put("browser", "Other");
        }
        
        // 检测操作系统
        if (ua.contains("windows")) {
            result.put("os", "Windows");
        } else if (ua.contains("mac")) {
            result.put("os", "macOS");
        } else if (ua.contains("linux")) {
            result.put("os", "Linux");
        } else if (ua.contains("android")) {
            result.put("os", "Android");
        } else if (ua.contains("ios")) {
            result.put("os", "iOS");
        } else {
            result.put("os", "Other");
        }
        
        return result;
    }
    
    /**
     * 计算会话统计数据
     * 
     * @return 会话统计信息
     */
    private SessionStatistics calculateSessionStatistics() {
        List<SessionInfo> activeSessions = getAllActiveSessions();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        
        // 基础统计
        long totalOnlineUsers = activeSessions.size();
        long totalActiveSessions = activeSessions.size();
        
        // 今日登录统计
        long todayLoginUsers = activeSessions.stream()
                .filter(session -> session.getLoginTime().isAfter(todayStart))
                .count();
        
        // 平均会话持续时间
        double averageSessionDuration = activeSessions.stream()
                .mapToLong(SessionInfo::getSessionDurationMinutes)
                .average()
                .orElse(0.0);
        
        // 最长会话持续时间
        long maxSessionDuration = activeSessions.stream()
                .mapToLong(SessionInfo::getSessionDurationMinutes)
                .max()
                .orElse(0L);
        
        // 按角色分组统计
        Map<String, Long> usersByRole = activeSessions.stream()
                .collect(Collectors.groupingBy(
                        SessionInfo::getRole,
                        Collectors.counting()
                ));
        
        // 按设备类型分组统计
        Map<String, Long> sessionsByDevice = activeSessions.stream()
                .collect(Collectors.groupingBy(
                        session -> session.getDeviceType() != null ? session.getDeviceType() : "Unknown",
                        Collectors.counting()
                ));
        
        // 按浏览器分组统计
        Map<String, Long> sessionsByBrowser = activeSessions.stream()
                .collect(Collectors.groupingBy(
                        session -> session.getBrowserType() != null ? session.getBrowserType() : "Unknown",
                        Collectors.counting()
                ));
        
        // 按操作系统分组统计
        Map<String, Long> sessionsByOS = activeSessions.stream()
                .collect(Collectors.groupingBy(
                        session -> session.getOsType() != null ? session.getOsType() : "Unknown",
                        Collectors.counting()
                ));
        
        // 按IP地址统计
        Map<String, Long> recentActiveIPs = activeSessions.stream()
                .collect(Collectors.groupingBy(
                        session -> session.getIpAddress() != null ? session.getIpAddress() : "Unknown",
                        Collectors.counting()
                ));
        
        return SessionStatistics.builder()
                .totalOnlineUsers(totalOnlineUsers)
                .totalActiveSessions(totalActiveSessions)
                .todayLoginUsers(todayLoginUsers)
                .todayNewSessions(todayLoginUsers) // 简化处理，实际应该从历史数据计算
                .averageSessionDuration(averageSessionDuration)
                .maxSessionDuration(maxSessionDuration)
                .usersByRole(usersByRole)
                .sessionsByDevice(sessionsByDevice)
                .sessionsByBrowser(sessionsByBrowser)
                .sessionsByOS(sessionsByOS)
                .recentActiveIPs(recentActiveIPs)
                .loginsByHour(new HashMap<>()) // 需要从历史数据计算
                .generatedAt(now)
                .validityMinutes(STATS_CACHE_MINUTES)
                .build();
    }
}