package com.myweb.website_core.demos.web.user;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 用户实体类
 * 
 * 表示系统中的用户信息，包括：
 * - 基本信息（用户名、邮箱等）
 * - 个人资料（头像、简介等）
 * - 社交关系（关注、粉丝等）
 * - 统计数据（获赞数等）
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String avatarUrl;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @ManyToMany
    @JoinTable(
        name = "user_followers",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    @JsonBackReference("user-followers")
    private List<User> followers;
    
    @ManyToMany(mappedBy = "followers")
    @JsonBackReference("user-following")
    private List<User> following;
    
    private Integer likedCount = 0;

}