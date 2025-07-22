package com.myweb.website_core.exception;

/**
 * Exception thrown when an interaction is not found
 */
public class InteractionNotFoundException extends InteractionException {
    
    public InteractionNotFoundException(String message) {
        super(message);
    }
    
    public InteractionNotFoundException(String operation, Long postId, Long userId) {
        super(String.format("%s not found for post %d by user %d", operation, postId, userId));
    }
}