package com.myweb.website_core.domain.business.dto;

import com.myweb.website_core.domain.business.entity.User;
import lombok.Data;

/**
 * 用户注册结果
 * 
 * 封装用户注册操作的结果信息
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
     * 注册成功的用户对象
     */
    private User user;
    
    /**
     * 错误消息（注册失败时）
     */
    private String errorMessage;
    
    /**
     * 错误代码（注册失败时）
     */
    private String errorCode;
    
    /**
     * 私有构造函数
     */
    private UserRegistrationResult() {}
    
    /**
     * 创建成功结果
     * 
     * @param user 注册成功的用户
     * @return 成功结果
     */
    public static UserRegistrationResult success(User user) {
        UserRegistrationResult result = new UserRegistrationResult();
        result.success = true;
        result.user = user;
        return result;
    }
    
    /**
     * 创建失败结果
     * 
     * @param errorMessage 错误消息
     * @return 失败结果
     */
    public static UserRegistrationResult failure(String errorMessage) {
        UserRegistrationResult result = new UserRegistrationResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }
    
    /**
     * 创建失败结果（带错误代码）
     * 
     * @param errorMessage 错误消息
     * @param errorCode 错误代码
     * @return 失败结果
     */
    public static UserRegistrationResult failure(String errorMessage, String errorCode) {
        UserRegistrationResult result = new UserRegistrationResult();
        result.success = false;
        result.errorMessage = errorMessage;
        result.errorCode = errorCode;
        return result;
    }
}