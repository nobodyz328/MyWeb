package com.myweb.website_core.common.exception;

/**
 * Exception thrown when a comment is not found
 */
public class CommentNotFoundException extends CommentException {
    
    public CommentNotFoundException(String message) {
        super(message);
    }
    
    public CommentNotFoundException(Long commentId) {
        super("Comment not found with ID: " + commentId);
    }
}