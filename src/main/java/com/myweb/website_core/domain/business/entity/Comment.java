package com.myweb.website_core.domain.business.entity;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.infrastructure.config.ApplicationContextProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "comments")
@EntityListeners(com.myweb.website_core.domain.business.listener.DataIntegrityListener.class)
public class    Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> replies;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ==================== 数据完整性字段 ====================
    
    /**
     * 内容哈希值
     * 用于验证评论内容的完整性，防止数据被篡改
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

    public Comment() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Comment(String content, Post post, User author) {
        this.content = content;
        this.post = post;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }
    
    public Comment(String content, Post post, User author, Comment parent) {
        this.content = content;
        this.post = post;
        this.author = author;
        this.parent = parent;
        this.createdAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        calculateContentHash();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateContentHash();
    }
    
    // ==================== 数据完整性相关方法 ====================
    
    /**
     * 计算并更新内容哈希值
     * 在内容变更时自动调用
     */
    public void calculateContentHash() {
        if (this.content != null) {
            try {
                DataIntegrityService integrityService = 
                    ApplicationContextProvider.getBean(DataIntegrityService.class);
                this.contentHash = integrityService.calculateHash(this.content);
                this.hashCalculatedAt = LocalDateTime.now();
            } catch (Exception e) {
                // 如果无法获取服务，记录警告但不阻止保存
                System.err.println("警告: 无法计算评论内容哈希值 - " + e.getMessage());
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
            System.err.println("警告: 无法验证评论内容完整性 - " + e.getMessage());
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