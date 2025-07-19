package com.myweb.website_core.demos.web.comment;

import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.blog.Post;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    private User author;

    @ManyToOne
    private Post post;

    private LocalDateTime createdAt;

    @ManyToOne
    private Comment parentComment;

    // getter/setter 省略
} 