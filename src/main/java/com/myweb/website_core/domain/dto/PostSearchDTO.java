package com.myweb.website_core.domain.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子搜索数据传输对象
 * 
 * 用于在数据访问层和业务层之间传输帖子搜索数据
 */
@Data
public class PostSearchDTO {
    
    /**
     * 帖子ID
     */
    private Long id;
    
    /**
     * 帖子标题
     */
    private String title;
    
    /**
     * 帖子完整内容
     */
    private String content;
    
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
     * 搜索相关性评分
     */
    private Double relevanceScore;
    
    /**
     * 热度评分
     */
    private Double popularityScore;
    
    /**
     * 搜索关键词匹配位置信息
     */
    private String matchInfo;
    
    /**
     * 是否为精确匹配
     */
    private Boolean exactMatch;
    
    /**
     * 匹配字段类型：TITLE, CONTENT, AUTHOR
     */
    private String matchType;
}