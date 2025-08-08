package com.myweb.website_core.application.service.security.authentication.JWT;

import com.myweb.website_core.application.service.security.authentication.UserTokenInfo;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.business.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT令牌管理服务
 * <p>
 * 基于JwtService提供令牌的高级管理功能，包括：
 * - 令牌黑名单管理
 * - 用户令牌会话管理
 * - 令牌刷新策略
 * - 安全策略执行
 * <p>
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {
    
    final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 生成完整的令牌对
     * 
     * @param user 用户信息
     * @return 令牌对信息
     */
    public TokenPair generateTokenPair(User user) {
        try {
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            // 存储用户的活跃令牌信息
            storeUserActiveTokens(user.getId(), accessToken, refreshToken);
            
            log.info("为用户 {} 生成令牌对成功", user.getUsername());
            
            return TokenPair.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiration())
                    .build();
                    
        } catch (Exception e) {
            log.error("生成令牌对失败: username={}", user.getUsername(), e);
            throw new RuntimeException("生成令牌对失败", e);
        }
    }
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @param user 用户信息
     * @return 新的令牌对
     */
    public TokenPair refreshToken(String refreshToken, User user) {
        try {
            // 验证刷新令牌
            if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                throw new RuntimeException("无效的刷新令牌");
            }
            
            // 检查令牌是否在黑名单中
            if (isTokenBlacklisted(refreshToken)) {
                throw new RuntimeException("令牌已被撤销");
            }
            
            // 生成新的访问令牌
            String newAccessToken = jwtService.generateAccessToken(user);
            
            // 更新用户活跃令牌
            updateUserActiveTokens(user.getId(), newAccessToken, refreshToken);
            
            log.info("为用户 {} 刷新令牌成功", user.getUsername());
            
            return TokenPair.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiration())
                    .build();
                    
        } catch (Exception e) {
            log.error("刷新令牌失败: username={}", user.getUsername(), e);
            throw new RuntimeException("刷新令牌失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 撤销用户所有令牌
     * 
     * @param userId 用户ID
     */
    public void revokeAllUserTokens(Long userId) {
        try {
            String userTokenKey = RedisKey.getUserActiveTokens(userId);
            
            // 获取用户当前的令牌
            Object tokenData = redisTemplate.opsForValue().get(userTokenKey);
            if (tokenData instanceof UserTokenInfo) {
                UserTokenInfo tokenInfo = (UserTokenInfo) tokenData;
                
                // 将令牌加入黑名单
                if (tokenInfo.getAccessToken() != null) {
                    blacklistToken(tokenInfo.getAccessToken());
                }
                if (tokenInfo.getRefreshToken() != null) {
                    blacklistToken(tokenInfo.getRefreshToken());
                }
            }
            
            // 删除用户活跃令牌记录
            redisTemplate.delete(userTokenKey);
            
            log.info("撤销用户 {} 的所有令牌成功", userId);
            
        } catch (Exception e) {
            log.error("撤销用户令牌失败: userId={}", userId, e);
            throw new RuntimeException("撤销用户令牌失败", e);
        }
    }
    
    /**
     * 撤销特定令牌
     * 
     * @param token 要撤销的令牌
     */
    public void revokeToken(String token) {
        try {
            blacklistToken(token);
            
            // 如果是访问令牌，也需要从用户活跃令牌中移除
            if (jwtService.isAccessToken(token)) {
                Long userId = jwtService.getUserIdFromToken(token);
                if (userId != null) {
                    removeUserActiveToken(userId, token);
                }
            }
            
            log.info("撤销令牌成功");
            
        } catch (Exception e) {
            log.error("撤销令牌失败", e);
            throw new RuntimeException("撤销令牌失败", e);
        }
    }
    
    /**
     * 验证令牌有效性
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 基础验证
            if (!jwtService.validateToken(token)) {
                return false;
            }
            
            // 检查黑名单
            if (isTokenBlacklisted(token)) {
                log.debug("令牌在黑名单中");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.debug("令牌验证失败", e);
            return false;
        }
    }
    
    /**
     * 存储用户活跃令牌信息
     */
    private void storeUserActiveTokens(Long userId, String accessToken, String refreshToken) {
        try {
            UserTokenInfo tokenInfo = UserTokenInfo.builder()
                    .userId(userId)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .createdAt(System.currentTimeMillis())
                    .build();
            
            String key = RedisKey.getUserActiveTokens(userId);
            redisTemplate.opsForValue().set(key, tokenInfo, 
                    jwtService.getRefreshTokenExpiration(), TimeUnit.SECONDS);
                    
        } catch (Exception e) {
            log.error("存储用户活跃令牌失败: userId={}", userId, e);
        }
    }
    
    /**
     * 更新用户活跃令牌信息
     */
    private void updateUserActiveTokens(Long userId, String newAccessToken, String refreshToken) {
        try {
            UserTokenInfo tokenInfo = UserTokenInfo.builder()
                    .userId(userId)
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .createdAt(System.currentTimeMillis())
                    .build();
            
            String key = RedisKey.getUserActiveTokens(userId);
            redisTemplate.opsForValue().set(key, tokenInfo, 
                    jwtService.getRefreshTokenExpiration(), TimeUnit.SECONDS);
                    
        } catch (Exception e) {
            log.error("更新用户活跃令牌失败: userId={}", userId, e);
        }
    }
    
    /**
     * 移除用户活跃令牌
     */
    private void removeUserActiveToken(Long userId, String token) {
        try {
            String key = RedisKey.getUserActiveTokens(userId);
            Object tokenData = redisTemplate.opsForValue().get(key);
            
            if (tokenData instanceof UserTokenInfo) {
                UserTokenInfo tokenInfo = (UserTokenInfo) tokenData;
                if (token.equals(tokenInfo.getAccessToken())) {
                    redisTemplate.delete(key);
                }
            }
            
        } catch (Exception e) {
            log.error("移除用户活跃令牌失败: userId={}", userId, e);
        }
    }
    
    /**
     * 将令牌加入黑名单
     */
    private void blacklistToken(String token) {
        try {
            String key = RedisKey.getBlacklistedToken(token);
            long remainingTime = jwtService.getRemainingTime(token);
            
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", 
                        remainingTime, TimeUnit.SECONDS);
            }
            
        } catch (Exception e) {
            log.error("加入令牌黑名单失败", e);
        }
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String key = RedisKey.getBlacklistedToken(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查令牌黑名单失败", e);
            return false;
        }
    }
    
    /**
     * 获取用户活跃令牌信息
     * 
     * @param userId 用户ID
     * @return 令牌信息
     */
    public UserTokenInfo getUserActiveTokens(Long userId) {
        try {
            String key = RedisKey.getUserActiveTokens(userId);
            Object tokenData = redisTemplate.opsForValue().get(key);
            
            if (tokenData instanceof UserTokenInfo) {
                return (UserTokenInfo) tokenData;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取用户活跃令牌失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 清理过期的黑名单令牌
     * 
     * 这个方法可以通过定时任务调用
     */
    public void cleanupExpiredBlacklistedTokens() {
        try {
            // Redis会自动清理过期的key，这里主要是记录日志
            log.debug("清理过期黑名单令牌任务执行");
        } catch (Exception e) {
            log.error("清理过期黑名单令牌失败", e);
        }
    }
    public String getUsernameFromToken(String token){
        return jwtService.getUsernameFromToken(token);
    }
}