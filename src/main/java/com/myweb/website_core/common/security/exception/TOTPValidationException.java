package com.myweb.website_core.common.security.exception;

/**
 * TOTP动态口令验证异常
 * 当TOTP（Time-based One-Time Password）验证失败时抛出
 * 
 * 符合GB/T 22239-2019 7.1.4.1 身份鉴别要求
 * 用于管理员二次验证场景
 */
public class TOTPValidationException extends AuthenticationException {
    
    private final String totpCode;
    private final String reason;
    
    /**
     * 构造TOTP验证异常
     * 
     * @param message 异常消息
     */
    public TOTPValidationException(String message) {
        super(message);
        this.totpCode = null;
        this.reason = null;
    }
    
    /**
     * 构造TOTP验证异常
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public TOTPValidationException(String message, Throwable cause) {
        super(message, cause);
        this.totpCode = null;
        this.reason = null;
    }
    
    /**
     * 构造TOTP验证异常
     * 
     * @param username 用户名
     * @param totpCode 提供的TOTP代码
     * @param reason 验证失败原因
     */
    public TOTPValidationException(String username, String totpCode, String reason) {
        super(String.format("用户 %s 的TOTP验证失败: %s", username, reason));
        this.totpCode = totpCode;
        this.reason = reason;
    }
    
    /**
     * 构造TOTP验证异常（代码为空或格式错误）
     * 
     * @param username 用户名
     */
    public static TOTPValidationException invalidCode(String username) {
        return new TOTPValidationException(username, null, "TOTP代码为空或格式不正确");
    }
    
    /**
     * 构造TOTP验证异常（代码不匹配）
     * 
     * @param username 用户名
     * @param providedCode 提供的代码
     */
    public static TOTPValidationException codeNotMatch(String username, String providedCode) {
        return new TOTPValidationException(username, providedCode, "TOTP代码不匹配");
    }
    
    /**
     * 构造TOTP验证异常（时间窗口过期）
     * 
     * @param username 用户名
     * @param providedCode 提供的代码
     */
    public static TOTPValidationException timeWindowExpired(String username, String providedCode) {
        return new TOTPValidationException(username, providedCode, "TOTP代码时间窗口已过期");
    }
    
    /**
     * 获取提供的TOTP代码
     * 
     * @return TOTP代码，如果未设置则返回null
     */
    public String getTotpCode() {
        return totpCode;
    }
    
    /**
     * 获取验证失败原因
     * 
     * @return 失败原因，如果未设置则返回null
     */
    public String getReason() {
        return reason;
    }
}