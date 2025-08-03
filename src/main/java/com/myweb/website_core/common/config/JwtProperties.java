package com.myweb.website_core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性类
 * 管理JWT相关配置
 * 
 * @author MyWeb Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    
    /**
     * JWT密钥
     */
    private String secret = "myWebSecretKeyForJWTTokenGenerationAndValidation2024";
    
    /**
     * JWT访问令牌过期时间（秒）
     */
    private long accessTokenExpirationSeconds = 3600; // 1小时
    
    /**
     * JWT刷新令牌过期时间（秒）
     */
    private long refreshTokenExpirationSeconds = 604800; // 7天
    
    /**
     * JWT发行者
     */
    private String issuer = "MyWeb";
    
    /**
     * JWT受众
     */
    private String audience = "MyWeb-Users";
    
    /**
     * JWT令牌前缀
     */
    private String tokenPrefix = "Bearer ";
    
    /**
     * JWT请求头名称
     */
    private String headerName = "Authorization";
    
    /**
     * 是否启用刷新令牌
     */
    private boolean enableRefreshToken = true;
    
    /**
     * 令牌黑名单配置
     */
    private Blacklist blacklist = new Blacklist();
    
    // Getters and Setters
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }
    
    public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }
    
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }
    
    public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) {
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public String getAudience() {
        return audience;
    }
    
    public void setAudience(String audience) {
        this.audience = audience;
    }
    
    public String getTokenPrefix() {
        return tokenPrefix;
    }
    
    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }
    
    public String getHeaderName() {
        return headerName;
    }
    
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
    
    public boolean isEnableRefreshToken() {
        return enableRefreshToken;
    }
    
    public void setEnableRefreshToken(boolean enableRefreshToken) {
        this.enableRefreshToken = enableRefreshToken;
    }
    
    public Blacklist getBlacklist() {
        return blacklist;
    }
    
    public void setBlacklist(Blacklist blacklist) {
        this.blacklist = blacklist;
    }
    
    /**
     * JWT令牌黑名单配置
     */
    public static class Blacklist {
        /**
         * 是否启用令牌黑名单
         */
        private boolean enabled = true;
        
        /**
         * 黑名单清理间隔（分钟）
         */
        private int cleanupIntervalMinutes = 60;
        
        /**
         * Redis键前缀
         */
        private String redisKeyPrefix = "jwt:blacklist:";
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getCleanupIntervalMinutes() {
            return cleanupIntervalMinutes;
        }
        
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
            this.cleanupIntervalMinutes = cleanupIntervalMinutes;
        }
        
        public String getRedisKeyPrefix() {
            return redisKeyPrefix;
        }
        
        public void setRedisKeyPrefix(String redisKeyPrefix) {
            this.redisKeyPrefix = redisKeyPrefix;
        }
    }
    
    /**
     * 获取访问令牌过期时间（毫秒）
     */
    public long getAccessTokenExpirationMillis() {
        return accessTokenExpirationSeconds * 1000;
    }
    
    /**
     * 获取刷新令牌过期时间（毫秒）
     */
    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationSeconds * 1000;
    }
    
    /**
     * 验证JWT密钥是否安全
     */
    public boolean isSecretSecure() {
        return secret != null && secret.length() >= 32;
    }
}