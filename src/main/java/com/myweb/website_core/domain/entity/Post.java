package com.myweb.website_core.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<Image> images;


    private Integer likeCount = 0;
    private Integer commentCount = 0;
    private Integer collectCount = 0;


    public void setAuthor(User author) { this.author = author; }

    public void setId(Long id) { this.id = id; }

    public void setTitle(String title) { this.title = title; }

    public void setContent(String content) { this.content = content; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public void setCollectCount(Integer collectCount) { this.collectCount = collectCount; }

    public void setLikeCount(Integer likeCount) {this.likeCount = likeCount;
    }
    
    public void setImages(List<Image> images) {
        this.images = images; 
    }
    
    /**
     * 获取图片ID列表（从关联的图片实体中提取）
     */
    public List<Long> getImageIds() {
        List<Long> imageIds = new ArrayList<>();
        if (images == null || images.isEmpty()) {
            return imageIds;
        }
        for (Image image : images) {
            if (image != null && image.getId() != null) {
                imageIds.add(image.getId());
            }
        }
        return imageIds;
    }


}
