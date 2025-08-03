package com.myweb.website_core.domain.business.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子搜索结果VO
 * 
 * 用于展示帖子搜索结果，包含帖子的基本信息和作者信息
 */
@Data
public class PostSearchVO {
    
    /**
     * 帖子ID
     */
    private Long id;
    
    /**
     * 帖子标题
     */
    private String title;
    
    /**
     * 帖子内容摘要（截取前200字符）
     */
    private String contentSummary;
    
    /**
     * 作者ID
     */
    private Long authorId;
    
    /**
     * 作者用户名
     */
    private String authorUsername;
    
    /**
     * 作者头像URL
     */
    private String authorAvatarUrl;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 收藏数
     */
    private Integer collectCount;
    
    /**
     * 评论数
     */
    private Integer commentCount;
    
    /**
     * 图片ID列表
     */
    private List<Long> imageIds;
    
    /**
     * 搜索相关性评分（用于排序）
     */
    private Double relevanceScore;
    
    /**
     * 热度评分（基于点赞数、收藏数、评论数计算）
     */
    private Double popularityScore;
    
    /**
     * 设置内容摘要
     * 如果内容长度超过200字符，则截取前200字符并添加省略号
     */
    public void setContentSummary(String content) {
        if (content == null) {
            this.contentSummary = "";
            return;
        }
        
        if (content.length() <= 200) {
            this.contentSummary = content;
        } else {
            this.contentSummary = content.substring(0, 200) + "...";
        }
    }
    
    /**
     * 计算热度评分
     * 公式：点赞数 * 2 + 收藏数 * 3 + 评论数 * 1.5
     */
    public void calculatePopularityScore() {
        int likes = likeCount != null ? likeCount : 0;
        int collects = collectCount != null ? collectCount : 0;
        int comments = commentCount != null ? commentCount : 0;
        
        this.popularityScore = likes * 2.0 + collects * 3.0 + comments * 1.5;
    }
}