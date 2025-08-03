package com.myweb.website_core.domain.business.dto;

import com.myweb.website_core.domain.business.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户注册结果DTO
 * 
 * 封装用户注册操作的结果信息
 * 包含注册成功的用户信息和相关状态
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
public class UserRegistrationResult {
    
    /**
     * 注册是否成功
     */
    private boolean success;
    
    /**
     * 注册的用户信息
     */
    private User user;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱地址
     */
    private String email;
    
    /**
     * 邮箱验证状态
     */
    private boolean emailVerified;
    
    /**
     * 注册时间
     */
    private LocalDateTime registrationTime;
    
    /**
     * 结果消息
     */
    private String message;
    
    /**
     * 错误信息（如果注册失败）
     */
    private String errorMessage;
    
    /**
     * 创建成功的注册结果
     * 
     * @param user 注册成功的用户
     * @return 注册结果
     */
    public static UserRegistrationResult success(User user) {
        UserRegistrationResult result = new UserRegistrationResult();
        result.setSuccess(true);
        result.setUser(user);
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setEmailVerified(user.getEmailVerified());
        result.setRegistrationTime(user.getCreatedAt());
        result.setMessage("用户注册成功");
        return result;
    }
    
    /**
     * 创建失败的注册结果
     * 
     * @param errorMessage 错误信息
     * @return 注册结果
     */
    public static UserRegistrationResult failure(String errorMessage) {
        UserRegistrationResult result = new UserRegistrationResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setMessage("用户注册失败");
        return result;
    }
}