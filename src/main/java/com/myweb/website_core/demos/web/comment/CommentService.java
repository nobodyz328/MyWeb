package com.myweb.website_core.demos.web.comment;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.CommentCreateRequest;
import com.myweb.website_core.dto.CommentReplyRequest;
import com.myweb.website_core.exception.CommentNotFoundException;
import com.myweb.website_core.exception.InvalidCommentException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Validated
@Transactional
public class CommentService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    
    public CommentService(CommentRepository commentRepository, 
                         UserRepository userRepository, 
                         PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }
    
    /**
     * Create a new comment with validation and async processing
     */
    @Async
    @Transactional
    public CompletableFuture<Comment> createComment(@Valid CommentCreateRequest request) {
        logger.info("Creating comment for post {} by user {}", request.getPostId(), request.getAuthorId());
        
        try {
            // Validate input
            validateCommentRequest(request.getContent(), request.getPostId(), request.getAuthorId());
            
            // Fetch entities
            User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new InvalidCommentException("User not found with ID: " + request.getAuthorId()));
            
            Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new InvalidCommentException("Post not found with ID: " + request.getPostId()));
            
            // Create and save comment
            Comment comment = new Comment(request.getContent(), author, post);
            Comment savedComment = commentRepository.save(comment);
            
            logger.info("Successfully created comment with ID: {}", savedComment.getId());
            return CompletableFuture.completedFuture(savedComment);
            
        } catch (Exception e) {
            logger.error("Error creating comment for post {} by user {}: {}", 
                        request.getPostId(), request.getAuthorId(), e.getMessage(), e);
            throw new InvalidCommentException("Failed to create comment: " + e.getMessage());
        }
    }
    
    /**
     * Reply to an existing comment with parent comment validation
     */
    @Async
    @Transactional
    public CompletableFuture<Comment> replyToComment(@Valid CommentReplyRequest request) {
        logger.info("Creating reply to comment {} for post {} by user {}", 
                   request.getParentCommentId(), request.getPostId(), request.getAuthorId());
        
        try {
            // Validate input
            validateCommentRequest(request.getContent(), request.getPostId(), request.getAuthorId());
            
            // Fetch entities
            User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new InvalidCommentException("User not found with ID: " + request.getAuthorId()));
            
            Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new InvalidCommentException("Post not found with ID: " + request.getPostId()));
            
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                .orElseThrow(() -> new CommentNotFoundException(request.getParentCommentId()));
            
            // Validate parent comment belongs to the same post
            if (!parentComment.getPost().getId().equals(request.getPostId())) {
                throw new InvalidCommentException("Parent comment does not belong to the specified post");
            }
            
            // Validate parent comment is not deleted
            if (parentComment.getIsDeleted()) {
                throw new InvalidCommentException("Cannot reply to a deleted comment");
            }
            
            // Create and save reply
            Comment reply = new Comment(request.getContent(), author, post, parentComment);
            Comment savedReply = commentRepository.save(reply);
            
            logger.info("Successfully created reply with ID: {}", savedReply.getId());
            return CompletableFuture.completedFuture(savedReply);
            
        } catch (CommentNotFoundException | InvalidCommentException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            logger.error("Error creating reply to comment {} for post {} by user {}: {}", 
                        request.getParentCommentId(), request.getPostId(), request.getAuthorId(), e.getMessage(), e);
            throw new InvalidCommentException("Failed to create reply: " + e.getMessage());
        }
    }
    
    /**
     * Get post comments with hierarchical loading
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Comment>> getPostComments(Long postId, Pageable pageable) {
        logger.info("Fetching comments for post {} with pagination", postId);
        
        try {
            // Validate post exists
            if (!postRepository.existsById(postId)) {
                throw new InvalidCommentException("Post not found with ID: " + postId);
            }
            
            // Get top-level comments with pagination
            Page<Comment> commentsPage = commentRepository.findTopLevelCommentsByPostId(postId, pageable);
            List<Comment> comments = commentsPage.getContent();
            
            logger.info("Successfully fetched {} comments for post {}", comments.size(), postId);
            return CompletableFuture.completedFuture(comments);
            
        } catch (Exception e) {
            logger.error("Error fetching comments for post {}: {}", postId, e.getMessage(), e);
            throw new InvalidCommentException("Failed to fetch comments: " + e.getMessage());
        }
    }
    
    /**
     * Get comment replies with pagination
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Comment>> getCommentReplies(Long commentId, Pageable pageable) {
        logger.info("Fetching replies for comment {} with pagination", commentId);
        
        try {
            // Validate parent comment exists
            Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
            
            // Validate parent comment is not deleted
            if (parentComment.getIsDeleted()) {
                throw new InvalidCommentException("Cannot fetch replies for a deleted comment");
            }
            
            // Get replies with pagination
            Page<Comment> repliesPage = commentRepository.findRepliesByParentCommentId(commentId, pageable);
            List<Comment> replies = repliesPage.getContent();
            
            logger.info("Successfully fetched {} replies for comment {}", replies.size(), commentId);
            return CompletableFuture.completedFuture(replies);
            
        } catch (CommentNotFoundException | InvalidCommentException e) {
            // Re-throw specific exceptions without wrapping
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching replies for comment {}: {}", commentId, e.getMessage(), e);
            throw new InvalidCommentException("Failed to fetch replies: " + e.getMessage());
        }
    }
    
    /**
     * Get all comments for a post with hierarchical structure
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Comment>> getPostCommentsWithReplies(Long postId) {
        logger.info("Fetching all comments with replies for post {}", postId);
        
        try {
            // Validate post exists
            if (!postRepository.existsById(postId)) {
                throw new InvalidCommentException("Post not found with ID: " + postId);
            }
            
            // Get comments with replies
            List<Comment> comments = commentRepository.findCommentsWithRepliesByPostId(postId);
            
            logger.info("Successfully fetched {} comments with replies for post {}", comments.size(), postId);
            return CompletableFuture.completedFuture(comments);
            
        } catch (Exception e) {
            logger.error("Error fetching comments with replies for post {}: {}", postId, e.getMessage(), e);
            throw new InvalidCommentException("Failed to fetch comments with replies: " + e.getMessage());
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    @Async
    @Deprecated
    public CompletableFuture<Comment> addComment(Comment comment) {
        logger.warn("Using deprecated addComment method. Please use createComment with CommentCreateRequest instead.");
        
        if (comment == null) {
            throw new InvalidCommentException("Comment cannot be null");
        }
        
        try {
            Comment savedComment = commentRepository.save(comment);
            return CompletableFuture.completedFuture(savedComment);
        } catch (Exception e) {
            logger.error("Error in legacy addComment method: {}", e.getMessage(), e);
            throw new InvalidCommentException("Failed to add comment: " + e.getMessage());
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    @Async
    @Deprecated
    public CompletableFuture<List<Comment>> getCommentsByPost(Long postId) {
        logger.warn("Using deprecated getCommentsByPost method. Please use getPostComments with Pageable instead.");
        
        try {
            List<Comment> comments = commentRepository.findTopLevelCommentsByPostId(postId);
            return CompletableFuture.completedFuture(comments);
        } catch (Exception e) {
            logger.error("Error in legacy getCommentsByPost method: {}", e.getMessage(), e);
            throw new InvalidCommentException("Failed to get comments: " + e.getMessage());
        }
    }
    
    /**
     * Validate comment request data
     */
    private void validateCommentRequest(String content, Long postId, Long authorId) {
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidCommentException("Comment content cannot be empty");
        }
        
        if (content.length() > 2000) {
            throw new InvalidCommentException("Comment content cannot exceed 2000 characters");
        }
        
        if (postId == null || postId <= 0) {
            throw new InvalidCommentException("Invalid post ID");
        }
        
        if (authorId == null || authorId <= 0) {
            throw new InvalidCommentException("Invalid author ID");
        }
        
        // Basic content sanitization - remove potentially harmful content
        if (content.contains("<script>") || content.contains("javascript:")) {
            throw new InvalidCommentException("Comment contains invalid content");
        }
    }
} 