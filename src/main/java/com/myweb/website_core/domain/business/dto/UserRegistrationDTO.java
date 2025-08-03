package com.myweb.website_core.domain.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 * 
 * 用于接收用户注册时提交的数据
 * 包含必要的验证注解确保数据完整性
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
public class UserRegistrationDTO {
    
    /**
     * 用户名
     * 必须唯一，长度3-20字符
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20字符之间")
    private String username;
    
    /**
     * 邮箱地址
     * 必须唯一，用于邮箱验证
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 密码
     * 将通过PasswordService进行策略验证
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 邮箱验证码
     * 用于验证邮箱所有权
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须为6位数字")
    private String verificationCode;
    
    /**
     * 客户端IP地址
     * 用于注册频率限制和审计日志
     */
    private String clientIp;
    
    /**
     * 用户代理信息
     * 用于审计日志
     */
    private String userAgent;
}