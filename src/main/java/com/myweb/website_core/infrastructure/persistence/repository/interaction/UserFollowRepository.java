package com.myweb.website_core.infrastructure.persistence.repository.interaction;

import com.myweb.website_core.domain.business.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    /**
     * 查找关注关系
     */
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    /**
     * 检查是否存在关注关系
     */
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    /**
     * 获取用户的关注列表（按关注时间倒序）
     */
    List<UserFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);
    
    /**
     * 获取用户的粉丝列表（按关注时间倒序）
     */
    List<UserFollow> findByFollowingIdOrderByCreatedAtDesc(Long followingId);
    
    /**
     * 统计用户的关注数量
     */
    Integer countByFollowerId(Long followerId);
    
    /**
     * 统计用户的粉丝数量
     */
    Integer countByFollowingId(Long followingId);
    
    /**
     * 删除用户的所有关注关系（作为关注者或被关注者）
     */
    @Modifying
    @Query("DELETE FROM UserFollow uf WHERE uf.followerId = :userId OR uf.followingId = :userId")
    void deleteByFollowerIdOrFollowingId(@Param("userId") Long userId1, @Param("userId") Long userId2);
    
    /**
     * 删除特定的关注关系
     */
    @Modifying
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    /**
     * 获取用户关注的用户ID列表
     */
    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);
    
    /**
     * 获取用户的粉丝ID列表
     */
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followingId = :followingId")
    List<Long> findFollowerIdsByFollowingId(@Param("followingId") Long followingId);
}