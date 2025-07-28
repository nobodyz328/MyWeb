package com.myweb.website_core.demos.web.blog;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.myweb.website_core.demos.web.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
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

    @ManyToOne
    @JsonManagedReference
    private User author;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ElementCollection
    private List<String> images;

    private Integer likeCount = 0;
    private Integer commentCount = 0;
    private Integer collectCount = 0;

    public void setAuthor(User author) { this.author = author; }

    public void setId(Long id) { this.id = id; }

    public void setTitle(String title) { this.title = title; }

    public void setContent(String content) { this.content = content; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setImages(List<String> images) { this.images = images; }

    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public void setCollectCount(Integer collectCount) { this.collectCount = collectCount; }
}
