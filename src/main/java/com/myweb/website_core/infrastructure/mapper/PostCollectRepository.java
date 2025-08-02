package com.myweb.website_core.infrastructure.mapper;

import com.myweb.website_core.domain.entity.Post;
import com.myweb.website_core.domain.entity.PostCollect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCollectRepository extends JpaRepository<PostCollect, Long> {
    
    // 查找用户是否已收藏某个帖子
    Optional<PostCollect> findByUserIdAndPostId(Long userId, Long postId);
    
    // 查找用户收藏的所有帖子
    @Query("SELECT pc FROM PostCollect pc JOIN FETCH pc.post WHERE pc.user.id = :userId ORDER BY pc.createdAt DESC")
    List<PostCollect> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 统计帖子的收藏数
    Integer countByPostId(Long postId);

    // 删除用户对某个帖子的收藏
    @Modifying
    @Transactional
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    // 检查用户是否收藏了某个帖子
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    // 根据帖子ID和用户ID查找收藏记录
    Optional<PostCollect> findByPostIdAndUserId(Long postId, Long userId);
    
    // 获取用户收藏的帖子列表
    @Query("SELECT pc.post FROM PostCollect pc WHERE pc.user.id = :userId ORDER BY pc.createdAt DESC")
    List<Post> findPostsByUserId(@Param("userId") Long userId);
}