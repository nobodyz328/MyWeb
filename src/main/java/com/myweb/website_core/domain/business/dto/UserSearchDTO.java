package com.myweb.website_core.domain.business.dto;

import lombok.Data;

/**
 * 用户搜索数据传输对象
 * 
 * 用于在数据访问层和业务层之间传输用户搜索数据
 */
@Data
public class UserSearchDTO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 获赞总数
     */
    private Integer likedCount;
    
    /**
     * 关注者数量
     */
    private Integer followersCount;
    
    /**
     * 关注数量
     */
    private Integer followingCount;
    
    /**
     * 帖子数量
     */
    private Integer postsCount;
    
    /**
     * 搜索相关性评分
     */
    private Double relevanceScore;
    
    /**
     * 搜索关键词匹配位置信息
     */
    private String matchInfo;
    
    /**
     * 是否为精确匹配
     */
    private Boolean exactMatch;
    
    /**
     * 匹配字段类型：USERNAME, BIO
     */
    private String matchType;
    
    /**
     * 用户活跃度评分
     */
    private Double activityScore;
    
    /**
     * 计算用户活跃度评分
     * 公式：获赞数 * 0.3 + 关注者数 * 0.4 + 帖子数 * 0.3
     */
    public void calculateActivityScore() {
        int likes = likedCount != null ? likedCount : 0;
        int followers = followersCount != null ? followersCount : 0;
        int posts = postsCount != null ? postsCount : 0;
        
        this.activityScore = likes * 0.3 + followers * 0.4 + posts * 0.3;
    }
}