package com.myweb.website_core.common.exception;

/**
 * Exception thrown when attempting to create a duplicate interaction
 */
public class DuplicateInteractionException extends InteractionException {
    
    public DuplicateInteractionException(String message) {
        super(message);
    }
    
    public DuplicateInteractionException(String operation, Long postId, Long userId) {
        super(String.format("Duplicate %s operation for post %d by user %d", operation, postId, userId));
    }
}