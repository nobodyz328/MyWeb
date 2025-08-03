package com.myweb.website_core.domain.business.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求DTO
 * 
 * 封装用户登录时提交的信息，包括：
 * - 基本认证信息（用户名、密码）
 * - 验证码信息（连续失败3次后需要）
 * - TOTP动态口令（管理员需要）
 * 
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    /**
     * 验证码
     * 连续登录失败3次后需要提供
     */
    private String captcha;
    
    /**
     * TOTP动态口令
     * 管理员账户登录时需要提供
     */
    private String totpCode;
    
    /**
     * 记住我选项
     */
    @Builder.Default
    private Boolean rememberMe = false;
    
    /**
     * 客户端设备信息
     */
    private String deviceInfo;
    
    /**
     * 检查是否提供了验证码
     * 
     * @return 是否提供了验证码
     */
    public boolean hasCaptcha() {
        return captcha != null && !captcha.trim().isEmpty();
    }
    
    /**
     * 检查是否提供了TOTP代码
     * 
     * @return 是否提供了TOTP代码
     */
    public boolean hasTotpCode() {
        return totpCode != null && !totpCode.trim().isEmpty();
    }
    
    /**
     * 清理敏感信息
     * 用于日志记录时隐藏密码等敏感信息
     * 
     * @return 清理后的登录请求对象
     */
    public LoginRequest sanitized() {
        return LoginRequest.builder()
                .username(this.username)
                .password("***")
                .captcha(this.captcha)
                .totpCode(this.totpCode != null ? "***" : null)
                .rememberMe(this.rememberMe)
                .deviceInfo(this.deviceInfo)
                .build();
    }
}