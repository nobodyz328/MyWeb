package com.myweb.website_core.common.exception;

/**
 * Exception thrown when comment validation fails
 */
public class InvalidCommentException extends CommentException {
    
    public InvalidCommentException(String message) {
        super(message);
    }
}