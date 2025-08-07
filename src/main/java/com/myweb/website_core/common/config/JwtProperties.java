package com.myweb.website_core.common.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性类
 * 管理JWT相关配置
 * 
 * @author MyWeb Team
 * @since 1.0.0
 */
@Getter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    // Getters and Setters
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

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) {
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public void setEnableRefreshToken(boolean enableRefreshToken) {
        this.enableRefreshToken = enableRefreshToken;
    }

    public void setBlacklist(Blacklist blacklist) {
        this.blacklist = blacklist;
    }
    
    /**
     * JWT令牌黑名单配置
     */
    @Getter
    public static class Blacklist {
        // Getters and Setters
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

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
            this.cleanupIntervalMinutes = cleanupIntervalMinutes;
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