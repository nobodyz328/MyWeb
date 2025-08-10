package com.myweb.website_core.common.exception;

/**
 * 交互操作异常
 */
public class InteractionException extends RuntimeException {
    
    public InteractionException(String message) {
        super(message);
    }
    
    public InteractionException(String message, Throwable cause) {
        super(message, cause);
    }
}