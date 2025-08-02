package com.myweb.website_core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 图片实体类
 * 
 * 用于管理上传的图片文件信息：
 * - 图片ID（用于访问）
 * - 原始文件名
 * - 存储路径
 * - 文件大小
 * - 内容类型
 * - 关联的帖子
 */
@Getter
@Setter
@Entity
@Table(name = "post_images")
public class Image {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String originalFilename;
    
    @Column(nullable = false)
    private String storedFilename;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(nullable = false)
    private String contentType;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private LocalDateTime uploadTime;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    public Image() {
        this.uploadTime = LocalDateTime.now();
    }
    
    public Image(String originalFilename, String storedFilename, String filePath, 
                 String contentType, Long fileSize) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadTime = LocalDateTime.now();
    }
}