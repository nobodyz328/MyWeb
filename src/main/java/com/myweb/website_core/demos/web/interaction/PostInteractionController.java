package com.myweb.website_core.demos.web.interaction;

import com.myweb.website_core.dto.ErrorResponse;
import com.myweb.website_core.dto.InteractionResponse;
import com.myweb.website_core.dto.PostInteractionStatus;
import com.myweb.website_core.exception.DuplicateInteractionException;
import com.myweb.website_core.exception.InteractionException;
import com.myweb.website_core.exception.InteractionNotFoundException;
import com.myweb.website_core.exception.RateLimitExceededException;
import com.myweb.website_core.service.PostInteractionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for post interaction operations (likes and bookmarks)
 */
@RestController
@RequestMapping("/api/posts/{postId}/interactions")
public class PostInteractionController {

    private static final Logger logger = LoggerFactory.getLogger(PostInteractionController.class);
    
    private final PostInteractionService postInteractionService;

    public PostInteractionController(PostInteractionService postInteractionService) {
        this.postInteractionService = postInteractionService;
    }

    /**
     * Toggle like status for a post
     * POST /api/posts/{postId}/interactions/like
     */
    @PostMapping("/like")
    public CompletableFuture<ResponseEntity<InteractionResponse>> toggleLike(
            @PathVariable("postId") @NotNull Long postId,
            HttpServletRequest request) {
        
        logger.debug("Toggle like request for post: {}", postId);
        
        Long userId = getCurrentUserId();
        if (userId == null) {
            InteractionResponse response = InteractionResponse.failure("like", postId, null, "User not authenticated");
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        return postInteractionService.toggleLike(postId, userId)
                .thenApply(response -> {
                    logger.debug("Like toggle completed for post: {}, user: {}, result: {}", 
                               postId, userId, response.getOperation());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    logger.error("Error toggling like for post: {}, user: {}", postId, userId, throwable);
                    return handleInteractionException(throwable, "like", postId, userId, request);
                });
    }

    /**
     * Toggle bookmark status for a post
     * POST /api/posts/{postId}/interactions/bookmark
     */
    @PostMapping("/bookmark")
    public CompletableFuture<ResponseEntity<InteractionResponse>> toggleBookmark(
            @PathVariable("postId") @NotNull Long postId,
            HttpServletRequest request) {
        
        logger.debug("Toggle bookmark request for post: {}", postId);
        
        Long userId = getCurrentUserId();
        if (userId == null) {
            InteractionResponse response = InteractionResponse.failure("bookmark", postId, null, "User not authenticated");
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        return postInteractionService.toggleBookmark(postId, userId)
                .thenApply(response -> {
                    logger.debug("Bookmark toggle completed for post: {}, user: {}, result: {}", 
                               postId, userId, response.getOperation());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    logger.error("Error toggling bookmark for post: {}, user: {}", postId, userId, throwable);
                    return handleInteractionException(throwable, "bookmark", postId, userId, request);
                });
    }

    /**
     * Get interaction status for a post
     * GET /api/posts/{postId}/interactions/status
     */
    @GetMapping("/status")
    public CompletableFuture<ResponseEntity<PostInteractionStatus>> getInteractionStatus(
            @PathVariable("postId") @NotNull Long postId,
            HttpServletRequest request) {
        
        logger.debug("Get interaction status request for post: {}", postId);
        
        Long userId = getCurrentUserId(); // Can be null for anonymous users
        
        return postInteractionService.getInteractionStatus(postId, userId)
                .thenApply(status -> {
                    logger.debug("Interaction status retrieved for post: {}, user: {}", postId, userId);
                    return ResponseEntity.ok(status);
                })
                .exceptionally(throwable -> {
                    logger.error("Error getting interaction status for post: {}, user: {}", postId, userId, throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Exception handler for interaction-specific exceptions
     */
    @ExceptionHandler(DuplicateInteractionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateInteraction(
            DuplicateInteractionException e, HttpServletRequest request) {
        logger.warn("Duplicate interaction attempt: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("DUPLICATE_INTERACTION", e.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InteractionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInteractionNotFound(
            InteractionNotFoundException e, HttpServletRequest request) {
        logger.warn("Interaction not found: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("INTERACTION_NOT_FOUND", e.getMessage(), request.getRequestURI());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            RateLimitExceededException e, HttpServletRequest request) {
        logger.warn("Rate limit exceeded: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("RATE_LIMIT_EXCEEDED", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(InteractionException.class)
    public ResponseEntity<ErrorResponse> handleInteractionException(
            InteractionException e, HttpServletRequest request) {
        logger.error("Interaction error: {}", e.getMessage(), e);
        ErrorResponse error = new ErrorResponse("INTERACTION_ERROR", e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException e, HttpServletRequest request) {
        logger.warn("Invalid request parameter: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_PARAMETER", e.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Helper method to handle exceptions in async operations
     */
    private ResponseEntity<InteractionResponse> handleInteractionException(
            Throwable throwable, String operation, Long postId, Long userId, HttpServletRequest request) {
        
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        
        if (cause instanceof DuplicateInteractionException) {
            InteractionResponse response = InteractionResponse.failure(operation, postId, userId, cause.getMessage());
            return ResponseEntity.badRequest().body(response);
        } else if (cause instanceof InteractionNotFoundException) {
            InteractionResponse response = InteractionResponse.failure(operation, postId, userId, cause.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else if (cause instanceof RateLimitExceededException) {
            InteractionResponse response = InteractionResponse.failure(operation, postId, userId, cause.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        } else {
            InteractionResponse response = InteractionResponse.failure(operation, postId, userId, "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to get current authenticated user ID
     * Returns null if user is not authenticated
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        
        // In this implementation, we assume the username is stored as the principal
        // and we need to look up the user ID. For now, we'll return a mock ID.
        // In a real implementation, you would inject UserService and look up the user.
        String username = authentication.getName();
        
        // TODO: Implement proper user lookup
        // For now, return a mock user ID for testing
        // This should be replaced with actual user service lookup
        logger.debug("Current authenticated user: {}", username);
        
        // Mock implementation - replace with actual user service call
        return 1L; // This should be replaced with actual user ID lookup
    }
}