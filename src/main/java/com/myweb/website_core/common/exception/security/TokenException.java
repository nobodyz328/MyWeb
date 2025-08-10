package com.myweb.website_core.common.exception.security;

import lombok.Getter;

/**
 * Token异常
 * <p>
 * 符合GB/T 22239-2019身份鉴别要求
 */
@Getter
public class TokenException extends RuntimeException {

    /**
     * -- GETTER --
     *  获取用户名
     *
     */
    private final String username;
    /**
     * -- GETTER --
     *  获取验证码
     *
     */
    private final String code;
    
    public TokenException(String message) {
        super(message);
        this.username = null;
        this.code = null;
    }
    
    public TokenException(String message, Throwable cause) {
        super(message, cause);
        this.username = null;
        this.code = null;
    }
    
    public TokenException(String message, String username, String code) {
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
    public static TokenException invalidCode(String username) {
        return new TokenException("TOTP验证码格式不正确", username, null);
    }
    
    /**
     * 创建代码不匹配异常
     * 
     * @param username 用户名
     * @param code 验证码
     * @return TOTP验证异常
     */
    public static TokenException codeNotMatch(String username, String code) {
        return new TokenException("TOTP验证码不正确", username, code);
    }

}