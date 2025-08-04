package com.myweb.website_core.domain.business.dto;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册数据传输对象
 * 
 * 包含用户注册所需的所有信息
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
public class UserRegistrationDTO {
    
    /**
     * 用户名
     * 3-20个字符，只能包含字母、数字、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;
    
    /**
     * 密码
     * 8-50个字符，必须包含字母和数字
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度必须在8-50个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]+$", 
             message = "密码必须包含至少一个字母和一个数字")
    private String password;
    
    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
    
    /**
     * 邮箱验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    private String verificationCode;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 用户代理字符串
     */
    private String userAgent;
    
    /**
     * 推荐人用户名（可选）
     */
    private String referrer;
    
    /**
     * 注册来源（web, mobile, api等）
     */
    @Builder.Default
    private String source = "web";
}