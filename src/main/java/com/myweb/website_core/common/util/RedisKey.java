package com.myweb.website_core.common.util;

public class RedisKey {
    public static final String POST_LIKE_COUNT = "post:like:count:";
    public static final String POST_COLLECT_COUNT = "post:collect:count:";
    
    // 邮箱验证相关键
    public static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
    public static final String EMAIL_RATE_LIMIT_PREFIX = "email:rate_limit:";
    
    // 会话管理相关键
    public static final String SESSION_PREFIX = "session:";
    public static final String USER_SESSION_PREFIX = "user:session:";
    public static final String SESSION_ACTIVITY_PREFIX = "session:activity:";
    public static final String SESSION_STATS_PREFIX = "session:stats:";
    public static final String ACTIVE_SESSIONS_SET = "active:sessions";
    public static final String USER_ACTIVE_SESSION_PREFIX = "user:active:session:";
    
    public static String likeKey(Long postId ,Long userId){
        return "user:like:" + userId + ":" + postId;
    }
    public static String collectKey(Long postId ,Long userId){
        return "user:collect:" + userId + ":" + postId;
    }
    
    public static String emailVerificationKey(String email, String type) {
        return EMAIL_VERIFICATION_PREFIX + type.toLowerCase() + ":" + email;
    }
    
    public static String emailRateLimitKey(String email, String period) {
        return EMAIL_RATE_LIMIT_PREFIX + period + ":" + email;
    }
    
    // 会话管理相关方法
    public static String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }
    
    public static String userSessionKey(Long userId) {
        return USER_SESSION_PREFIX + userId;
    }
    
    public static String sessionActivityKey(String sessionId) {
        return SESSION_ACTIVITY_PREFIX + sessionId;
    }
    
    public static String sessionStatsKey(String type) {
        return SESSION_STATS_PREFIX + type;
    }
    
    public static String userActiveSessionKey(Long userId) {
        return USER_ACTIVE_SESSION_PREFIX + userId;
    }
}
