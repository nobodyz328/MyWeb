package com.myweb.website_core.domain.business.vo;

import lombok.Data;

/**
 * 用户搜索结果VO
 * 
 * 用于展示用户搜索结果，包含用户的基本信息和统计数据
 */
@Data
public class UserSearchVO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱（脱敏显示）
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
     * 搜索相关性评分（用于排序）
     */
    private Double relevanceScore;
    
    /**
     * 设置脱敏邮箱
     * 只显示邮箱的前3个字符和@后的域名
     */
    public void setMaskedEmail(String email) {
        if (email == null || email.length() < 6) {
            this.email = "***";
            return;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 3) {
            this.email = "***" + email.substring(atIndex);
        } else {
            this.email = email.substring(0, 3) + "***" + email.substring(atIndex);
        }
    }
    
    /**
     * 设置个人简介摘要
     * 如果简介长度超过100字符，则截取前100字符并添加省略号
     */
    public void setBioSummary(String bio) {
        if (bio == null) {
            this.bio = "";
            return;
        }
        
        if (bio.length() <= 100) {
            this.bio = bio;
        } else {
            this.bio = bio.substring(0, 100) + "...";
        }
    }
}