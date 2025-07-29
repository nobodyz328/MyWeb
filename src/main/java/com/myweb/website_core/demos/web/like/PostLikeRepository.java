package com.myweb.website_core.demos.web.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    /**
     * 查找用户对特定帖子的点赞记录
     */
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * 检查用户是否已点赞某帖子
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * 统计帖子的点赞数
     */
    long countByPostId(Long postId);
    
    /**
     * 获取用户点赞的所有帖子
     */
    @Query("SELECT pl FROM PostLike pl JOIN FETCH pl.post WHERE pl.user.id = :userId ORDER BY pl.createdAt DESC")
    List<PostLike> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    /**
     * 删除用户对帖子的点赞
     */
    @Modifying
    @Transactional
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * 删除帖子的所有点赞记录
     */
    @Modifying
    @Transactional
    void deleteByPostId(Long postId);
    
    /**
     * 获取帖子的点赞用户列表
     */
    @Query("SELECT pl FROM PostLike pl JOIN FETCH pl.user WHERE pl.post.id = :postId ORDER BY pl.createdAt DESC")
    List<PostLike> findByPostIdOrderByCreatedAtDesc(@Param("postId") Long postId);
    
    /**
     * 根据帖子ID和用户ID查找点赞记录
     */
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    
    /**
     * 获取用户点赞的帖子列表
     */
    @Query("SELECT pl.post FROM PostLike pl WHERE pl.user.id = :userId ORDER BY pl.createdAt DESC")
    List<com.myweb.website_core.demos.web.blog.Post> findPostsByUserId(@Param("userId") Long userId);
}