package com.myweb.website_core.domain.business.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.infrastructure.config.ApplicationContextProvider;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Entity
@Table(name = "posts")
@EntityListeners(com.myweb.website_core.domain.business.listener.DataIntegrityListener.class)
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
    
    // ==================== 数据完整性字段 ====================
    
    /**
     * 内容哈希值
     * 用于验证帖子内容的完整性，防止数据被篡改
     * 符合GB/T 22239-2019数据完整性保护要求
     */
    @Column(name = "content_hash")
    private String contentHash;
    
    /**
     * 哈希计算时间
     * 记录最后一次计算内容哈希的时间
     */
    @Column(name = "hash_calculated_at")
    private LocalDateTime hashCalculatedAt;


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
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public void setHashCalculatedAt(LocalDateTime hashCalculatedAt) {
        this.hashCalculatedAt = hashCalculatedAt;
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
    
    // ==================== 数据完整性相关方法 ====================
    
    /**
     * 计算并更新内容哈希值
     * 在内容变更时自动调用
     */
    @PrePersist
    @PreUpdate
    public void calculateContentHash() {
        if (this.content != null) {
            try {
                DataIntegrityService integrityService = 
                    ApplicationContextProvider.getBean(DataIntegrityService.class);
                this.contentHash = integrityService.calculateHash(this.content);
                this.hashCalculatedAt = LocalDateTime.now();
            } catch (Exception e) {
                // 如果无法获取服务，记录警告但不阻止保存
                System.err.println("警告: 无法计算内容哈希值 - " + e.getMessage());
            }
        }
    }
    
    /**
     * 验证内容完整性
     * 
     * @return 内容是否完整
     */
    public boolean verifyContentIntegrity() {
        if (this.content == null || this.contentHash == null) {
            return false;
        }
        
        try {
            DataIntegrityService integrityService = 
                ApplicationContextProvider.getBean(DataIntegrityService.class);
            return integrityService.verifyIntegrity(this.content, this.contentHash);
        } catch (Exception e) {
            System.err.println("警告: 无法验证内容完整性 - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查内容是否需要重新计算哈希
     * 
     * @return 是否需要重新计算
     */
    public boolean needsHashRecalculation() {
        return this.contentHash == null || this.hashCalculatedAt == null ||
               this.hashCalculatedAt.isBefore(LocalDateTime.now().minusDays(30));
    }


}
