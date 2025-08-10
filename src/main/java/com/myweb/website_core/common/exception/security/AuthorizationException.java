package com.myweb.website_core.common.exception.security;

/**
 * 授权异常
 * <p>
 * 当用户没有足够权限执行某个操作时抛出此异常
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 */
public class AuthorizationException extends RuntimeException {
    
    public AuthorizationException(String message) {
        super(message);
    }
    
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}