package com.myweb.website_core.exception;

/**
 * Base exception for comment-related operations
 */
public class CommentException extends RuntimeException {
    
    public CommentException(String message) {
        super(message);
    }
    
    public CommentException(String message, Throwable cause) {
        super(message, cause);
    }
}