package com.myweb.website_core.application.service.security.authentication.JWT;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT令牌对
 * <p>
 * 包含访问令牌和刷新令牌的完整信息
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型（通常为"Bearer"）
     */
    private String tokenType;
    
    /**
     * 访问令牌过期时间（秒）
     */
    private Long expiresIn;
}