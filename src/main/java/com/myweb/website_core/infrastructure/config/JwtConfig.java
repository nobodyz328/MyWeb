package com.myweb.website_core.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 * <p>
 * 从application.yml中读取JWT相关配置
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    
    /**
     * JWT签名密钥
     */
    private String secret = "mywebsecretkeythatisverylongandcomplex123456789";
    
    /**
     * 访问令牌过期时间（秒）
     */
    private Long accessTokenExpiration = 3600L; // 1小时
    
    /**
     * 刷新令牌过期时间（秒）
     */
    private Long refreshTokenExpiration = 604800L; // 7天
    
    /**
     * 令牌发行者
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
    private Boolean enableRefreshToken = true;
    
    /**
     * 是否启用JWT认证
     */
    private Boolean enabled = true;
    
    /**
     * 黑名单配置
     */
    private Blacklist blacklist = new Blacklist();
    
    @Data
    public static class Blacklist {
        /**
         * 是否启用黑名单
         */
        private Boolean enabled = false;
        
        /**
         * 清理间隔（分钟）
         */
        private Integer cleanupIntervalMinutes = 60;
        
        /**
         * Redis键前缀
         */
        private String redisKeyPrefix = "jwt:blacklist:";
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 获取访问令牌过期时间
     */
    public long getAccessTokenExpirationMillis() {
        return accessTokenExpiration ;
    }
    
    /**
     * 获取刷新令牌过期时间
     */
    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpiration ;
    }
    
    /**
     * 验证JWT密钥是否安全
     */
    public boolean isSecretSecure() {
        return secret != null && secret.length() >= 32;
    }
}