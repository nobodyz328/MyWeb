package com.myweb.website_core.application.service.security.IPS.virusprotect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CSRF令牌服务
 * 提供CSRF令牌的生成、验证和刷新功能
 * 
 * @author Kiro
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsrfTokenService {

    private static final String CSRF_TOKEN_CACHE_PREFIX = "csrf:token:";
    private static final String CSRF_SESSION_CACHE_PREFIX = "csrf:session:";
    private static final Duration TOKEN_EXPIRATION = Duration.ofHours(2);

    private final RedisTemplate<String, Object> redisTemplate;

    private final CsrfTokenRepository csrfTokenRepository;
    
    /**
     * 生成新的CSRF令牌
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @return CSRF令牌信息
     */
    public Map<String, String> generateToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 生成新的CSRF令牌
            CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
            
            // 保存令牌到存储库
            csrfTokenRepository.saveToken(csrfToken, request, response);
            
            // 缓存令牌信息到Redis
            cacheTokenInfo(csrfToken, request);
            
            Map<String, String> tokenInfo = new HashMap<>();
            tokenInfo.put("token", csrfToken.getToken());
            tokenInfo.put("headerName", csrfToken.getHeaderName());
            tokenInfo.put("parameterName", csrfToken.getParameterName());
            tokenInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            log.debug("Generated new CSRF token for session: {}", getSessionId(request));
            
            return tokenInfo;
            
        } catch (Exception e) {
            log.error("Failed to generate CSRF token", e);
            throw new RuntimeException("Failed to generate CSRF token", e);
        }
    }
    
    /**
     * 刷新CSRF令牌
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @return 新的CSRF令牌信息
     */
    public Map<String, String> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 清除旧的令牌缓存
            clearTokenCache(request);
            
            // 生成新令牌
            return generateToken(request, response);
            
        } catch (Exception e) {
            log.error("Failed to refresh CSRF token", e);
            throw new RuntimeException("Failed to refresh CSRF token", e);
        }
    }
    
    /**
     * 验证CSRF令牌
     * 
     * @param request HTTP请求
     * @param token 待验证的令牌
     * @return 验证结果
     */
    public boolean validateToken(HttpServletRequest request, String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("CSRF token is null or empty for request: {}", request.getRequestURI());
                return false;
            }
            
            // 从存储库加载令牌
            CsrfToken expectedToken = csrfTokenRepository.loadToken(request);
            
            if (expectedToken == null) {
                log.warn("No CSRF token found in repository for session: {}", getSessionId(request));
                return false;
            }
            
            // 验证令牌
            boolean isValid = expectedToken.getToken().equals(token);
            
            if (isValid) {
                log.debug("CSRF token validation successful for session: {}", getSessionId(request));
                // 更新令牌使用时间
                updateTokenUsage(request);
            } else {
                log.warn("CSRF token validation failed for session: {}, expected: {}, actual: {}",
                           getSessionId(request), expectedToken.getToken(), token);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating CSRF token", e);
            return false;
        }
    }
    
    /**
     * 获取当前CSRF令牌信息
     * 
     * @param request HTTP请求
     * @return CSRF令牌信息，如果不存在则返回null
     */
    public Map<String, String> getCurrentTokenInfo(HttpServletRequest request) {
        try {
            CsrfToken csrfToken = csrfTokenRepository.loadToken(request);
            
            if (csrfToken == null) {
                return null;
            }
            
            Map<String, String> tokenInfo = new HashMap<>();
            tokenInfo.put("token", csrfToken.getToken());
            tokenInfo.put("headerName", csrfToken.getHeaderName());
            tokenInfo.put("parameterName", csrfToken.getParameterName());
            
            // 从缓存获取时间戳
            String cacheKey = CSRF_TOKEN_CACHE_PREFIX + getSessionId(request);
            Object timestamp = redisTemplate.opsForHash().get(cacheKey, "timestamp");
            if (timestamp != null) {
                tokenInfo.put("timestamp", timestamp.toString());
            }
            
            return tokenInfo;
            
        } catch (Exception e) {
            log.error("Error getting current CSRF token info", e);
            return null;
        }
    }
    
    /**
     * 清除CSRF令牌
     * 
     * @param request HTTP请求
     */
    public void clearToken(HttpServletRequest request) {
        try {
            // 清除存储库中的令牌
            csrfTokenRepository.saveToken(null, request, null);
            
            // 清除缓存
            clearTokenCache(request);
            
            log.debug("Cleared CSRF token for session: {}", getSessionId(request));
            
        } catch (Exception e) {
            log.error("Error clearing CSRF token", e);
        }
    }
    
    /**
     * 缓存令牌信息到Redis
     */
    private void cacheTokenInfo(CsrfToken csrfToken, HttpServletRequest request) {
        try {
            String sessionId = getSessionId(request);
            String cacheKey = CSRF_TOKEN_CACHE_PREFIX + sessionId;
            
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("token", csrfToken.getToken());
            tokenData.put("headerName", csrfToken.getHeaderName());
            tokenData.put("parameterName", csrfToken.getParameterName());
            tokenData.put("timestamp", System.currentTimeMillis());
            tokenData.put("lastUsed", System.currentTimeMillis());
            
            redisTemplate.opsForHash().putAll(cacheKey, tokenData);
            redisTemplate.expire(cacheKey, TOKEN_EXPIRATION);
            
        } catch (Exception e) {
            log.warn("Failed to cache CSRF token info", e);
        }
    }
    
    /**
     * 更新令牌使用时间
     */
    private void updateTokenUsage(HttpServletRequest request) {
        try {
            String sessionId = getSessionId(request);
            String cacheKey = CSRF_TOKEN_CACHE_PREFIX + sessionId;
            
            redisTemplate.opsForHash().put(cacheKey, "lastUsed", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.warn("Failed to update token usage time", e);
        }
    }
    
    /**
     * 清除令牌缓存
     */
    private void clearTokenCache(HttpServletRequest request) {
        try {
            String sessionId = getSessionId(request);
            String cacheKey = CSRF_TOKEN_CACHE_PREFIX + sessionId;
            
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.warn("Failed to clear token cache", e);
        }
    }
    
    /**
     * 获取会话ID
     */
    private String getSessionId(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            return request.getSession().getId();
        }
        
        // 如果没有会话，使用请求的唯一标识
        String remoteAddr = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return UUID.nameUUIDFromBytes((remoteAddr + userAgent).getBytes()).toString();
    }
}