package com.myweb.website_core.demos.web.blog;



import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;


@Entity
@Table(name = "blog_post")
public class BlogPost {
    @Id//主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)//自增主键
    private long id;

    private String title;       // 文章标题
    private String content;     // 文章内容
    private String author;      // 作者
    private LocalDateTime createdAt; // 创建时间

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }

    public BlogPost() {
        this.createdAt = LocalDateTime.now(); // 默认设置为当前时间
    }
    public BlogPost(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now(); // 默认设置为当前时间
    }




}
