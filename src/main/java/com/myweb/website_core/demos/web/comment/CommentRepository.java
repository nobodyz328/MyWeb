package com.myweb.website_core.demos.web.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * Find all top-level comments for a post (not replies) that are not deleted
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.isDeleted = false ORDER BY c.createdAt ASC")
    Page<Comment> findTopLevelCommentsByPostId(@Param("postId") Long postId, Pageable pageable);
    
    /**
     * Find all top-level comments for a post (not replies) that are not deleted
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelCommentsByPostId(@Param("postId") Long postId);
    
    /**
     * Find all replies to a specific comment that are not deleted
     */
    @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :commentId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    Page<Comment> findRepliesByParentCommentId(@Param("commentId") Long commentId, Pageable pageable);
    
    /**
     * Find all replies to a specific comment that are not deleted
     */
    @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :commentId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentCommentId(@Param("commentId") Long commentId);
    
    /**
     * Count total comments for a post (including replies) that are not deleted
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.isDeleted = false")
    long countByPostIdAndIsDeletedFalse(@Param("postId") Long postId);
    
    /**
     * Count top-level comments for a post (excluding replies) that are not deleted
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.isDeleted = false")
    long countTopLevelCommentsByPostId(@Param("postId") Long postId);
    
    /**
     * Count replies for a specific comment that are not deleted
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentComment.id = :commentId AND c.isDeleted = false")
    long countRepliesByParentCommentId(@Param("commentId") Long commentId);
    
    /**
     * Find all comments by a user that are not deleted
     */
    @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByAuthorIdAndIsDeletedFalse(@Param("authorId") Long authorId, Pageable pageable);
    
    /**
     * Find all comments for a post with their replies (hierarchical loading)
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.replies r WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.isDeleted = false AND (r IS NULL OR r.isDeleted = false) ORDER BY c.createdAt ASC, r.createdAt ASC")
    List<Comment> findCommentsWithRepliesByPostId(@Param("postId") Long postId);
    
    /**
     * Find comments by post ID including deleted ones (for admin purposes)
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    Page<Comment> findAllCommentsByPostId(@Param("postId") Long postId, Pageable pageable);
    
    /**
     * Get comment counts for multiple posts
     */
    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds AND c.isDeleted = false GROUP BY c.post.id")
    List<Object[]> countCommentsByPostIds(@Param("postIds") List<Long> postIds);
} 