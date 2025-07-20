package com.myweb.website_core.demos.web.interaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    /**
     * Find a like by user and post
     */
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * Check if a user has liked a specific post
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * Count total likes for a post
     */
    long countByPostId(Long postId);
    
    /**
     * Find all likes by a user
     */
    List<PostLike> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find all likes for a post
     */
    List<PostLike> findByPostIdOrderByCreatedAtDesc(Long postId);
    
    /**
     * Delete a like by user and post
     */
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * Get like counts for multiple posts
     */
    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<Object[]> countLikesByPostIds(@Param("postIds") List<Long> postIds);
    
    /**
     * Get user's like status for multiple posts
     */
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    List<Long> findLikedPostIdsByUserAndPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}