package com.myweb.website_core.demos.web.announcement;

import com.myweb.website_core.demos.web.user.User;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;

    @ManyToOne
    private User author;

    // getter/setter 省略
} 