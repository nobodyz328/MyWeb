package com.myweb.website_core.domain.business.dto;

import com.myweb.website_core.common.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户登录响应DTO
 * <p>
 * 用于返回登录成功后的用户信息和JWT令牌，避免直接序列化实体类导致的循环引用问题
 */
@Data
public class UserLoginResponse {
    
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
    private Integer likedCount;
    private UserRole role;
    private List<String> roleNames;
    private List<String> permissions;
    private Boolean emailVerified;
    private Boolean totpEnabled;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
    
    // 统计信息
    private Integer followersCount;
    private Integer followingCount;
    private Integer postsCount;
    
    // JWT令牌信息
    private String accessToken;
    //private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}