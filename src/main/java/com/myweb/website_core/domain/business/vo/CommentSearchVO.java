package com.myweb.website_core.domain.business.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 评论搜索结果VO
 * <p>
 * 用于展示评论搜索结果，包含评论的基本信息、作者信息和所属帖子信息
 */
@Data
public class CommentSearchVO {
    
    /**
     * 评论ID
     */
    private Long id;
    
    /**
     * 评论内容摘要（截取前150字符）
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
     * 所属帖子ID
     */
    private Long postId;
    
    /**
     * 所属帖子标题
     */
    private String postTitle;
    
    /**
     * 父评论ID（如果是回复）
     */
    private Long parentId;
    
    /**
     * 是否为回复评论
     */
    private Boolean isReply;
    
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
     * 回复数
     */
    private Integer replyCount;
    
    /**
     * 搜索相关性评分（用于排序）
     */
    private Double relevanceScore;
    
    /**
     * 热度评分（基于点赞数、回复数计算）
     */
    private Double popularityScore;
    
    /**
     * 设置内容摘要
     * 如果内容长度超过150字符，则截取前150字符并添加省略号
     */
    public void setContentSummary(String content) {
        if (content == null) {
            this.contentSummary = "";
            return;
        }
        
        if (content.length() <= 150) {
            this.contentSummary = content;
        } else {
            this.contentSummary = content.substring(0, 150) + "...";
        }
    }
    
    /**
     * 计算热度评分
     * 公式：点赞数 * 1.5 + 回复数 * 2
     */
    public void calculatePopularityScore() {
        int likes = likeCount != null ? likeCount : 0;
        int replies = replyCount != null ? replyCount : 0;
        
        this.popularityScore = likes * 1.5 + replies * 2.0;
    }
    
    /**
     * 设置是否为回复评论
     */
    public void setIsReply(Long parentId) {
        this.isReply = parentId != null;
        this.parentId = parentId;
    }
}