package com.myweb.website_core.domain.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.Post;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子数据传输对象
 * 
 * 用于API响应，包含图片URL而不是图片ID
 */
@Getter
@Setter
public class PostDTO {
    
    private Long id;
    private String title;
    private String content;
    private User author;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private List<String> images; // 图片URL列表
    private Integer likeCount = 0;
    private Integer commentCount = 0;
    private Integer collectCount = 0;
    
    public PostDTO() {}
    
    public PostDTO(Post post, List<String> imageUrls) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.author = post.getAuthor();
        this.createdAt = post.getCreatedAt();
        this.images = imageUrls;
        this.likeCount = post.getLikeCount();
        this.commentCount = post.getCommentCount();
        this.collectCount = post.getCollectCount();
    }
}