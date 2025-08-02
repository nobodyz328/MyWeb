package com.myweb.website_core.common.exception;

/**
 * Exception thrown when rate limit is exceeded for interactions
 */
public class RateLimitExceededException extends InteractionException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(String operation, Long userId) {
        super(String.format("Rate limit exceeded for %s operation by user %d", operation, userId));
    }
}