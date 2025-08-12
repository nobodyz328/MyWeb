package com.myweb.website_core.domain.business.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体类
 * 
 * 表示用户之间的关注关系：
 * - follower: 关注者（主动关注的用户）
 * - following: 被关注者（被关注的用户）
 */
@Getter
@Setter
@Entity
@Table(name = "user_follows", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
public class UserFollow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关注者ID
     */
    @Column(name = "follower_id", nullable = false)
    private Long followerId;
    
    /**
     * 被关注者ID
     */
    @Column(name = "following_id", nullable = false)
    private Long followingId;
    
    /**
     * 关注者用户对象
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", insertable = false, updatable = false)
    private User follower;
    
    /**
     * 被关注者用户对象
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", insertable = false, updatable = false)
    private User following;
    
    /**
     * 关注时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 实体持久化前的回调
     * 自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    // 构造函数
    public UserFollow() {}
    
    public UserFollow(Long followerId, Long followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }
}