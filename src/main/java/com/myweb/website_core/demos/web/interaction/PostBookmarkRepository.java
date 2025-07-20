package com.myweb.website_core.demos.web.interaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {
    
    /**
     * Find a bookmark by user and post
     */
    Optional<PostBookmark> findByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * Check if a user has bookmarked a specific post
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * Count total bookmarks for a post
     */
    long countByPostId(Long postId);
    
    /**
     * Find all bookmarks by a user with pagination
     */
    Page<PostBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find all bookmarks by a user
     */
    List<PostBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find all bookmarks for a post
     */
    List<PostBookmark> findByPostIdOrderByCreatedAtDesc(Long postId);
    
    /**
     * Delete a bookmark by user and post
     */
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * Get bookmark counts for multiple posts
     */
    @Query("SELECT pb.post.id, COUNT(pb) FROM PostBookmark pb WHERE pb.post.id IN :postIds GROUP BY pb.post.id")
    List<Object[]> countBookmarksByPostIds(@Param("postIds") List<Long> postIds);
    
    /**
     * Get user's bookmark status for multiple posts
     */
    @Query("SELECT pb.post.id FROM PostBookmark pb WHERE pb.user.id = :userId AND pb.post.id IN :postIds")
    List<Long> findBookmarkedPostIdsByUserAndPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
    
    /**
     * Get user's bookmarked posts with post details
     */
    @Query("SELECT pb FROM PostBookmark pb JOIN FETCH pb.post p JOIN FETCH p.author WHERE pb.user.id = :userId ORDER BY pb.createdAt DESC")
    Page<PostBookmark> findUserBookmarksWithPostDetails(@Param("userId") Long userId, Pageable pageable);
}