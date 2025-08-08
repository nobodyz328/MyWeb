package com.myweb.website_core.common.security.exception;

/**
 * TOTP验证异常
 * 
 * 当TOTP验证失败时抛出的异常
 * 符合GB/T 22239-2019身份鉴别要求
 */
public class TOTPValidationException extends RuntimeException {
    
    private final String username;
    private final String code;
    
    public TOTPValidationException(String message) {
        super(message);
        this.username = null;
        this.code = null;
    }
    
    public TOTPValidationException(String message, Throwable cause) {
        super(message, cause);
        this.username = null;
        this.code = null;
    }
    
    public TOTPValidationException(String message, String username, String code) {
        super(message);
        this.username = username;
        this.code = code;
    }
    
    /**
     * 创建无效代码异常
     * 
     * @param username 用户名
     * @return TOTP验证异常
     */
    public static TOTPValidationException invalidCode(String username) {
        return new TOTPValidationException("TOTP验证码格式不正确", username, null);
    }
    
    /**
     * 创建代码不匹配异常
     * 
     * @param username 用户名
     * @param code 验证码
     * @return TOTP验证异常
     */
    public static TOTPValidationException codeNotMatch(String username, String code) {
        return new TOTPValidationException("TOTP验证码不正确", username, code);
    }
    
    /**
     * 获取用户名
     * 
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * 获取验证码
     * 
     * @return 验证码
     */
    public String getCode() {
        return code;
    }
}