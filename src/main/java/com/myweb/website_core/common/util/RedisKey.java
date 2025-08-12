package com.myweb.website_core.common.util;

/**
 * Redis键统一管理工具类
 * <p>
 * 统一管理系统中所有Redis键的命名规范，避免硬编码
 * 采用分层命名结构：业务模块:功能:具体标识
 * 
 * @author MyWeb
 * @version 2.0
 */
public class RedisKey {
    
    // ========== 基础分隔符 ==========
    private static final String SEPARATOR = ":";
    
    // ========== 业务模块前缀 ==========
    private static final String POST_MODULE = "post";
    private static final String USER_MODULE = "user";
    private static final String EMAIL_MODULE = "email";
    private static final String SESSION_MODULE = "session";
    private static final String SECURITY_MODULE = "security";
    private static final String SEARCH_MODULE = "search";
    private static final String CACHE_MODULE = "cache";
    private static final String AUTH_MODULE = "auth";
    private static final String SYSTEM_MODULE = "system";
    private static final String AUDIT_MODULE = "audit";
    
    // ========== 帖子相关键 ==========
    public static final String POST_LIKE_COUNT = POST_MODULE + SEPARATOR + "like" + SEPARATOR + "count" + SEPARATOR;
    public static final String POST_COLLECT_COUNT = POST_MODULE + SEPARATOR + "collect" + SEPARATOR + "count" + SEPARATOR;
    public static final String POST_VIEW_COUNT = POST_MODULE + SEPARATOR + "view" + SEPARATOR + "count" + SEPARATOR;
    public static final String POST_COMMENT_COUNT = POST_MODULE + SEPARATOR + "comment" + SEPARATOR + "count" + SEPARATOR;
    public static final String POST_CACHE_PREFIX = CACHE_MODULE + SEPARATOR + POST_MODULE + SEPARATOR;
    public static final String POST_HOT_LIST = POST_MODULE + SEPARATOR + "hot" + SEPARATOR + "list";
    public static final String POST_TRENDING_LIST = POST_MODULE + SEPARATOR + "trending" + SEPARATOR + "list";
    
    // ========== 用户相关键 ==========
    public static final String USER_LIKE_PREFIX = USER_MODULE + SEPARATOR + "like" + SEPARATOR;
    public static final String USER_COLLECT_PREFIX = USER_MODULE + SEPARATOR + "collect" + SEPARATOR;
    public static final String USER_FOLLOW_PREFIX = USER_MODULE + SEPARATOR + "follow" + SEPARATOR;
    public static final String USER_FOLLOWER_PREFIX = USER_MODULE + SEPARATOR + "follower" + SEPARATOR;
    public static final String USER_PROFILE_CACHE = CACHE_MODULE + SEPARATOR + USER_MODULE + SEPARATOR + "profile" + SEPARATOR;
    public static final String USER_STATS_PREFIX = USER_MODULE + SEPARATOR + "stats" + SEPARATOR;
    public static final String USER_ONLINE_SET = USER_MODULE + SEPARATOR + "online" + SEPARATOR + "set";
    public static final String USER_LAST_ACTIVITY = USER_MODULE + SEPARATOR + "last" + SEPARATOR + "activity" + SEPARATOR;
    
    // ========== 邮箱验证相关键 ==========
    public static final String EMAIL_VERIFICATION_PREFIX = EMAIL_MODULE + SEPARATOR + "verification" + SEPARATOR;
    public static final String EMAIL_RATE_LIMIT_PREFIX = EMAIL_MODULE + SEPARATOR + "rate_limit" + SEPARATOR;
    public static final String EMAIL_SEND_COUNT_PREFIX = EMAIL_MODULE + SEPARATOR + "send_count" + SEPARATOR;
    public static final String EMAIL_BLACKLIST_SET = EMAIL_MODULE + SEPARATOR + "blacklist" + SEPARATOR + "set";
    
    // ========== 会话管理相关键 ==========
    public static final String SESSION_PREFIX = SESSION_MODULE + SEPARATOR;
    public static final String USER_SESSION_PREFIX = USER_MODULE + SEPARATOR + SESSION_MODULE + SEPARATOR;
    public static final String SESSION_ACTIVITY_PREFIX = SESSION_MODULE + SEPARATOR + "activity" + SEPARATOR;
    public static final String SESSION_STATS_PREFIX = SESSION_MODULE + SEPARATOR + "stats" + SEPARATOR;
    public static final String ACTIVE_SESSIONS_SET = "active" + SEPARATOR + "sessions";
    public static final String USER_ACTIVE_SESSION_PREFIX = USER_MODULE + SEPARATOR + "active" + SEPARATOR + SESSION_MODULE + SEPARATOR;
    public static final String SESSION_CLEANUP_LOCK = SESSION_MODULE + SEPARATOR + "cleanup" + SEPARATOR + "lock";
    
    // ========== 安全相关键 ==========
    public static final String SECURITY_EVENT_PREFIX = SECURITY_MODULE + SEPARATOR + "event" + SEPARATOR;
    public static final String LOGIN_ATTEMPT_PREFIX = SECURITY_MODULE + SEPARATOR + "login_attempt" + SEPARATOR;
    public static final String ACCOUNT_LOCK_PREFIX = SECURITY_MODULE + SEPARATOR + "account_lock" + SEPARATOR;
    public static final String IP_BLACKLIST_SET = SECURITY_MODULE + SEPARATOR + "ip_blacklist" + SEPARATOR + "set";
    public static final String SUSPICIOUS_IP_PREFIX = SECURITY_MODULE + SEPARATOR + "suspicious_ip" + SEPARATOR;
    public static final String RATE_LIMIT_PREFIX = SECURITY_MODULE + SEPARATOR + "rate_limit" + SEPARATOR;
    public static final String CAPTCHA_PREFIX = SECURITY_MODULE + SEPARATOR + "captcha" + SEPARATOR;
    public static final String XSS_ATTEMPT_PREFIX = SECURITY_MODULE + SEPARATOR + "xss_attempt" + SEPARATOR;
    
    // ========== 认证相关键 ==========
    public static final String JWT_BLACKLIST_PREFIX = AUTH_MODULE + SEPARATOR + "jwt" + SEPARATOR + "blacklist" + SEPARATOR;
    public static final String AUTH_TOKEN_PREFIX = AUTH_MODULE + SEPARATOR + "token" + SEPARATOR;
    public static final String USER_ACTIVE_TOKENS_PREFIX = AUTH_MODULE + SEPARATOR + "user" + SEPARATOR + "tokens" + SEPARATOR;
    public static final String TOTP_SECRET_PREFIX = AUTH_MODULE + SEPARATOR + "totp" + SEPARATOR + "secret" + SEPARATOR;
    public static final String PASSWORD_RESET_PREFIX = AUTH_MODULE + SEPARATOR + "password_reset" + SEPARATOR;
    public static final String REGISTRATION_RATE_LIMIT_PREFIX = AUTH_MODULE + SEPARATOR + "registration" + SEPARATOR + "rate_limit" + SEPARATOR;
    
    // ========== 搜索相关键 ==========
    public static final String SEARCH_CACHE_PREFIX = SEARCH_MODULE + SEPARATOR + CACHE_MODULE + SEPARATOR;
    public static final String POST_SEARCH_CACHE_PREFIX = SEARCH_MODULE + SEPARATOR + POST_MODULE + SEPARATOR;
    public static final String USER_SEARCH_CACHE_PREFIX = SEARCH_MODULE + SEPARATOR + USER_MODULE + SEPARATOR;
    public static final String HOT_SEARCH_KEYWORDS_KEY = SEARCH_MODULE + SEPARATOR + "hot" + SEPARATOR + "keywords";
    public static final String SEARCH_STATS_PREFIX = SEARCH_MODULE + SEPARATOR + "stats" + SEPARATOR;
    public static final String SEARCH_HISTORY_PREFIX = SEARCH_MODULE + SEPARATOR + "history" + SEPARATOR;
    
    // ========== 系统相关键 ==========
    public static final String SYSTEM_CONFIG_PREFIX = SYSTEM_MODULE + SEPARATOR + "config" + SEPARATOR;
    public static final String SYSTEM_STATS_PREFIX = SYSTEM_MODULE + SEPARATOR + "stats" + SEPARATOR;
    public static final String SYSTEM_MAINTENANCE_LOCK = SYSTEM_MODULE + SEPARATOR + "maintenance" + SEPARATOR + "lock";
    public static final String SYSTEM_BACKUP_LOCK = SYSTEM_MODULE + SEPARATOR + "backup" + SEPARATOR + "lock";
    
    // ========== 审计相关键 ==========
    public static final String AUDIT_CACHE_PREFIX = AUDIT_MODULE + SEPARATOR + CACHE_MODULE + SEPARATOR;
    public static final String AUDIT_STATS_PREFIX = AUDIT_MODULE + SEPARATOR + "stats" + SEPARATOR;
    public static final String AUDIT_EXPORT_LOCK = AUDIT_MODULE + SEPARATOR + "export" + SEPARATOR + "lock";
    
    // ========== 通用缓存键 ==========
    public static final String GENERAL_CACHE_PREFIX = CACHE_MODULE + SEPARATOR + "general" + SEPARATOR;
    public static final String TEMP_DATA_PREFIX = "temp" + SEPARATOR + "data" + SEPARATOR;
    public static final String DISTRIBUTED_LOCK_PREFIX = "lock" + SEPARATOR;
    
    // ========== 帖子相关方法 ==========
    
    /**
     * 用户点赞帖子键
     */
    public static String likeKey(Long postId, Long userId) {
        return USER_LIKE_PREFIX + userId + SEPARATOR + postId;
    }
    
    /**
     * 用户收藏帖子键
     */
    public static String collectKey(Long postId, Long userId) {
        return USER_COLLECT_PREFIX + userId + SEPARATOR + postId;
    }
    
    /**
     * 用户关注键
     */
    public static String followKey(Long targetUserId, Long userId) {
        return USER_FOLLOW_PREFIX + userId + SEPARATOR + targetUserId;
    }
    
    /**
     * 帖子点赞数量键
     */
    public static String postLikeCountKey(Long postId) {
        return POST_LIKE_COUNT + postId;
    }
    
    /**
     * 帖子收藏数量键
     */
    public static String postCollectCountKey(Long postId) {
        return POST_COLLECT_COUNT + postId;
    }
    
    /**
     * 帖子浏览数量键
     */
    public static String postViewCountKey(Long postId) {
        return POST_VIEW_COUNT + postId;
    }
    
    /**
     * 帖子评论数量键
     */
    public static String postCommentCountKey(Long postId) {
        return POST_COMMENT_COUNT + postId;
    }
    
    /**
     * 帖子缓存键
     */
    public static String postCacheKey(Long postId) {
        return POST_CACHE_PREFIX + postId;
    }
    
    // ========== 用户相关方法 ==========
    
    /**
     * 用户关注键
     */
    public static String userFollowKey(Long userId, Long targetUserId) {
        return USER_FOLLOW_PREFIX + userId + SEPARATOR + targetUserId;
    }
    
    /**
     * 用户粉丝键
     */
    public static String userFollowerKey(Long userId, Long followerUserId) {
        return USER_FOLLOWER_PREFIX + userId + SEPARATOR + followerUserId;
    }
    
    /**
     * 用户资料缓存键
     */
    public static String userProfileCacheKey(Long userId) {
        return USER_PROFILE_CACHE + userId;
    }
    
    /**
     * 用户统计信息键
     */
    public static String userStatsKey(Long userId) {
        return USER_STATS_PREFIX + userId;
    }
    
    /**
     * 用户最后活动时间键
     */
    public static String userLastActivityKey(Long userId) {
        return USER_LAST_ACTIVITY + userId;
    }
    
    // ========== 邮箱验证相关方法 ==========
    
    /**
     * 邮箱验证码键
     */
    public static String emailVerificationKey(String email, String type) {
        return EMAIL_VERIFICATION_PREFIX + type.toLowerCase() + SEPARATOR + email;
    }
    
    /**
     * 邮箱发送频率限制键
     */
    public static String emailRateLimitKey(String email, String period) {
        return EMAIL_RATE_LIMIT_PREFIX + period + SEPARATOR + email;
    }
    
    /**
     * 邮箱发送次数统计键
     */
    public static String emailSendCountKey(String email, String date) {
        return EMAIL_SEND_COUNT_PREFIX + date + SEPARATOR + email;
    }
    
    // ========== 会话管理相关方法 ==========
    
    /**
     * 会话键
     */
    public static String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }
    
    /**
     * 用户会话键
     */
    public static String userSessionKey(Long userId) {
        return USER_SESSION_PREFIX + userId;
    }
    
    /**
     * 会话活动键
     */
    public static String sessionActivityKey(String sessionId) {
        return SESSION_ACTIVITY_PREFIX + sessionId;
    }
    
    /**
     * 会话统计键
     */
    public static String sessionStatsKey(String type) {
        return SESSION_STATS_PREFIX + type;
    }
    
    /**
     * 用户活跃会话键
     */
    public static String userActiveSessionKey(Long userId) {
        return USER_ACTIVE_SESSION_PREFIX + userId;
    }
    
    // ========== 安全相关方法 ==========
    
    /**
     * 安全事件键
     */
    public static String securityEventKey(String eventId) {
        return SECURITY_EVENT_PREFIX + eventId;
    }
    
    /**
     * 登录尝试次数键
     */
    public static String loginAttemptKey(String identifier) {
        return LOGIN_ATTEMPT_PREFIX + identifier;
    }
    
    /**
     * 账户锁定键
     */
    public static String accountLockKey(String username) {
        return ACCOUNT_LOCK_PREFIX + username;
    }
    
    /**
     * 可疑IP键
     */
    public static String suspiciousIpKey(String ip) {
        return SUSPICIOUS_IP_PREFIX + ip;
    }
    
    /**
     * 频率限制键
     */
    public static String rateLimitKey(String resource, String identifier) {
        return RATE_LIMIT_PREFIX + resource + SEPARATOR + identifier;
    }
    
    /**
     * 验证码键
     */
    public static String captchaKey(String sessionId) {
        return CAPTCHA_PREFIX + sessionId;
    }
    
    /**
     * XSS攻击尝试键
     */
    public static String xssAttemptKey(String ip, String sessionId) {
        return XSS_ATTEMPT_PREFIX + ip + SEPARATOR + sessionId;
    }
    
    // ========== 认证相关方法 ==========
    
    /**
     * JWT黑名单键
     */
    public static String jwtBlacklistKey(String jti) {
        return JWT_BLACKLIST_PREFIX + jti;
    }
    
    /**
     * 黑名单令牌键
     */
    public static String getBlacklistedToken(String token) {
        return JWT_BLACKLIST_PREFIX + token.hashCode();
    }
    
    /**
     * 用户活跃令牌键
     */
    public static String getUserActiveTokens(Long userId) {
        return USER_ACTIVE_TOKENS_PREFIX + userId;
    }
    
    /**
     * TOTP密钥键
     */
    public static String totpSecretKey(Long userId) {
        return TOTP_SECRET_PREFIX + userId;
    }
    
    /**
     * 密码重置键
     */
    public static String passwordResetKey(String token) {
        return PASSWORD_RESET_PREFIX + token;
    }
    
    /**
     * 注册频率限制键
     */
    public static String registrationRateLimitKey(String ip) {
        return REGISTRATION_RATE_LIMIT_PREFIX + ip;
    }
    
    // ========== 搜索相关方法 ==========
    
    /**
     * 搜索缓存键
     */
    public static String searchCacheKey(String query, String type, String params) {
        return SEARCH_CACHE_PREFIX + type + SEPARATOR + query.hashCode() + SEPARATOR + params.hashCode();
    }
    
    /**
     * 帖子搜索缓存键
     */
    public static String postSearchCacheKey(String query, String sortBy, int page) {
        return POST_SEARCH_CACHE_PREFIX + query.hashCode() + SEPARATOR + sortBy + SEPARATOR + page;
    }
    
    /**
     * 用户搜索缓存键
     */
    public static String userSearchCacheKey(String query, int page) {
        return USER_SEARCH_CACHE_PREFIX + query.hashCode() + SEPARATOR + page;
    }
    
    /**
     * 搜索统计键
     */
    public static String searchStatsKey(String type, String date) {
        return SEARCH_STATS_PREFIX + type + SEPARATOR + date;
    }
    
    /**
     * 用户搜索历史键
     */
    public static String searchHistoryKey(Long userId) {
        return SEARCH_HISTORY_PREFIX + userId;
    }
    
    // ========== 系统相关方法 ==========
    
    /**
     * 系统配置键
     */
    public static String systemConfigKey(String configKey) {
        return SYSTEM_CONFIG_PREFIX + configKey;
    }
    
    /**
     * 系统统计键
     */
    public static String systemStatsKey(String type, String date) {
        return SYSTEM_STATS_PREFIX + type + SEPARATOR + date;
    }
    
    // ========== 审计相关方法 ==========
    
    /**
     * 审计缓存键
     */
    public static String auditCacheKey(String queryHash) {
        return AUDIT_CACHE_PREFIX + queryHash;
    }
    
    /**
     * 审计统计键
     */
    public static String auditStatsKey(String type, String date) {
        return AUDIT_STATS_PREFIX + type + SEPARATOR + date;
    }
    
    // ========== 通用方法 ==========
    
    /**
     * 通用缓存键
     */
    public static String generalCacheKey(String module, String key) {
        return GENERAL_CACHE_PREFIX + module + SEPARATOR + key;
    }
    
    /**
     * 临时数据键
     */
    public static String tempDataKey(String identifier) {
        return TEMP_DATA_PREFIX + identifier;
    }
    
    /**
     * 分布式锁键
     */
    public static String distributedLockKey(String resource) {
        return DISTRIBUTED_LOCK_PREFIX + resource;
    }
    
    /**
     * 构建复合键
     */
    public static String buildKey(String... parts) {
        return String.join(SEPARATOR, parts);
    }
    
    /**
     * 获取键的模式（用于批量操作）
     */
    public static String getKeyPattern(String prefix) {
        return prefix + "*";
    }
}
