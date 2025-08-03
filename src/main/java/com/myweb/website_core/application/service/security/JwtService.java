package com.myweb.website_core.application.service.security;

import com.myweb.website_core.domain.business.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT令牌服务
 * 
 * 提供JWT令牌的生成、验证和管理功能，包括：
 * - 访问令牌生成和验证
 * - 刷新令牌生成和验证
 * - 令牌解析和信息提取
 * 
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class JwtService {
    
    @Value("${app.jwt.secret:mywebsecretkeythatisverylongandcomplex123456789}")
    private String jwtSecret;
    
    @Value("${app.jwt.access-token-expiration:3600}")
    private Long accessTokenExpiration; // 1小时
    
    @Value("${app.jwt.refresh-token-expiration:604800}")
    private Long refreshTokenExpiration; // 7天
    
    @Value("${app.jwt.issuer:MyWeb}")
    private String issuer;
    
    /**
     * 获取签名密钥
     * 
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * 生成访问令牌
     * 
     * @param user 用户信息
     * @return JWT访问令牌
     */
    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpiration, "access");
    }
    
    /**
     * 生成刷新令牌
     * 
     * @param user 用户信息
     * @return JWT刷新令牌
     */
    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenExpiration, "refresh");
    }
    
    /**
     * 生成JWT令牌
     * 
     * @param user 用户信息
     * @param expiration 过期时间（秒）
     * @param tokenType 令牌类型
     * @return JWT令牌
     */
    private String generateToken(User user, Long expiration, String tokenType) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration * 1000);
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
            claims.put("role", user.getRole().name());
            claims.put("tokenType", tokenType);
            claims.put("email", user.getEmail());
            
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(user.getUsername())
                    .setIssuer(issuer)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                    .compact();
            
            log.debug("为用户 {} 生成 {} 令牌成功，过期时间: {}", user.getUsername(), tokenType, expiryDate);
            return token;
            
        } catch (Exception e) {
            log.error("生成JWT令牌失败: username={}, tokenType={}", user.getUsername(), tokenType, e);
            throw new RuntimeException("生成JWT令牌失败", e);
        }
    }
    
    /**
     * 验证JWT令牌
     * 
     * @param token JWT令牌
     * @return 令牌是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从令牌中获取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从JWT令牌获取用户名失败", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.error("从JWT令牌获取用户ID失败", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取用户角色
     * 
     * @param token JWT令牌
     * @return 用户角色
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("从JWT令牌获取用户角色失败", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取令牌类型
     * 
     * @param token JWT令牌
     * @return 令牌类型
     */
    public String getTokenTypeFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("tokenType", String.class);
        } catch (Exception e) {
            log.error("从JWT令牌获取令牌类型失败", e);
            return null;
        }
    }
    
    /**
     * 获取令牌过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("从JWT令牌获取过期时间失败", e);
            return null;
        }
    }
    
    /**
     * 检查令牌是否过期
     * 
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.error("检查JWT令牌过期状态失败", e);
            return true;
        }
    }
    
    /**
     * 获取令牌剩余有效时间（秒）
     * 
     * @param token JWT令牌
     * @return 剩余有效时间，如果已过期则返回0
     */
    public long getRemainingTime(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            if (expiration == null) {
                return 0;
            }
            
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
            
        } catch (Exception e) {
            log.error("获取JWT令牌剩余时间失败", e);
            return 0;
        }
    }
    
    /**
     * 从令牌中获取Claims
     * 
     * @param token JWT令牌
     * @return Claims对象
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 检查是否为访问令牌
     * 
     * @param token JWT令牌
     * @return 是否为访问令牌
     */
    public boolean isAccessToken(String token) {
        String tokenType = getTokenTypeFromToken(token);
        return "access".equals(tokenType);
    }
    
    /**
     * 检查是否为刷新令牌
     * 
     * @param token JWT令牌
     * @return 是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        String tokenType = getTokenTypeFromToken(token);
        return "refresh".equals(tokenType);
    }
    
    /**
     * 刷新访问令牌
     * 
     * @param refreshToken 刷新令牌
     * @param user 用户信息
     * @return 新的访问令牌
     */
    public String refreshAccessToken(String refreshToken, User user) {
        try {
            if (!validateToken(refreshToken) || !isRefreshToken(refreshToken)) {
                throw new RuntimeException("无效的刷新令牌");
            }
            
            return generateAccessToken(user);
            
        } catch (Exception e) {
            log.error("刷新访问令牌失败: username={}", user.getUsername(), e);
            throw new RuntimeException("刷新访问令牌失败", e);
        }
    }
    
    /**
     * 获取访问令牌过期时间（秒）
     * 
     * @return 访问令牌过期时间
     */
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    /**
     * 获取刷新令牌过期时间（秒）
     * 
     * @return 刷新令牌过期时间
     */
    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
    
    /**
     * 从Bearer令牌中提取JWT令牌
     * 
     * @param bearerToken Bearer令牌字符串
     * @return JWT令牌，如果格式不正确则返回null
     */
    public String extractTokenFromBearer(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * 获取令牌信息摘要
     * 
     * @param token JWT令牌
     * @return 令牌信息摘要
     */
    public String getTokenSummary(String token) {
        try {
            if (!validateToken(token)) {
                return "无效令牌";
            }
            
            String username = getUsernameFromToken(token);
            String tokenType = getTokenTypeFromToken(token);
            Date expiration = getExpirationFromToken(token);
            
            return String.format("用户: %s, 类型: %s, 过期时间: %s", 
                    username, tokenType, 
                    expiration != null ? expiration.toString() : "未知");
                    
        } catch (Exception e) {
            return "令牌解析失败";
        }
    }
}