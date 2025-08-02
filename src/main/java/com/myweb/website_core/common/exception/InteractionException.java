package com.myweb.website_core.common.exception;

/**
 * Base exception for interaction-related operations
 */
public class InteractionException extends RuntimeException {
    
    public InteractionException(String message) {
        super(message);
    }
    
    public InteractionException(String message, Throwable cause) {
        super(message, cause);
    }
}