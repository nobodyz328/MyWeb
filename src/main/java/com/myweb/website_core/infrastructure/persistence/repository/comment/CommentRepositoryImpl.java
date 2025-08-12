package com.myweb.website_core.infrastructure.persistence.repository.comment;

import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.infrastructure.persistence.repository.SafeRepositoryBase;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论安全Repository实现类
 * <p>
 * 扩展SafeRepositoryBase，提供安全的评论查询方法：
 * 1. 安全的搜索查询
 * 2. 安全的分页查询
 * 3. 安全的条件查询
 * 4. SQL注入防护
 * <p>
 * 符合需求：5.1, 5.4, 5.6 - 安全查询服务集成
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Repository
public class CommentRepositoryImpl extends SafeRepositoryBase<Comment, Long> implements CommentRepositoryCustom {
    
    private final CommentRepository commentRepository;
    
    public CommentRepositoryImpl(@Lazy CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    
    @Override
    protected JpaRepository<Comment, Long> getRepository() {
        return commentRepository;
    }
    
    @Override
    protected JpaSpecificationExecutor<Comment> getSpecificationExecutor() {
        return commentRepository;
    }
    
    @Override
    protected String getTableName() {
        return "comments";
    }
    
    /**
     * 安全的评论搜索
     * 
     * @param keyword 搜索关键词
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 搜索结果
     */
    @Override
    public Page<Comment> findCommentsWithSafeSearch(String keyword, String sortField, 
                                                  String sortDirection, int page, int size) {
        log.debug("执行安全评论搜索: keyword={}, sortField={}, sortDirection={}, page={}, size={}", 
                 keyword, sortField, sortDirection, page, size);
        
        // 定义搜索字段
        List<String> searchFields = List.of("content");
        
        // 创建分页参数
        Pageable pageable = createSafePageable(page, size, sortField, sortDirection);
        
        // 执行安全搜索
        return findSafeSearch(searchFields, keyword, null, pageable);
    }
    
    /**
     * 根据帖子ID安全查询评论
     * 
     * @param postId 帖子ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Comment> findCommentsByPostWithSafePagination(Long postId, int page, int size) {
        log.debug("执行安全帖子评论查询: postId={}, page={}, size={}", postId, page, size);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("post.id", postId);
        
        return findSafePaginated(conditions, "created_at", "ASC", page, size);
    }
    
    /**
     * 根据作者ID安全查询评论
     * 
     * @param authorId 作者ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Comment> findCommentsByAuthorWithSafePagination(Long authorId, int page, int size) {
        log.debug("执行安全作者评论查询: authorId={}, page={}, size={}", authorId, page, size);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("author.id", authorId);
        
        return findSafePaginated(conditions, "created_at", "DESC", page, size);
    }
    
    /**
     * 安全的顶级评论查询
     * 
     * @param postId 帖子ID
     * @param pageable 分页参数
     * @return 顶级评论列表
     */
    @Override
    public Page<Comment> findTopLevelCommentsSafely(Long postId, Pageable pageable) {
        log.debug("执行安全顶级评论查询: postId={}", postId);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("post.id", postId);
        conditions.put("parent", null);
        
        return findSafePaginated(conditions, "created_at", "ASC", 
                               pageable.getPageNumber(), pageable.getPageSize());
    }
    
    /**
     * 安全的回复评论查询
     * 
     * @param parentId 父评论ID
     * @param pageable 分页参数
     * @return 回复评论列表
     */
    @Override
    public Page<Comment> findRepliesSafely(Long parentId, Pageable pageable) {
        log.debug("执行安全回复评论查询: parentId={}", parentId);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("parent.id", parentId);
        
        return findSafePaginated(conditions, "created_at", "ASC", 
                               pageable.getPageNumber(), pageable.getPageSize());
    }
    
    /**
     * 安全的热门评论查询
     * 
     * @param postId 帖子ID
     * @param limit 限制数量
     * @return 热门评论列表
     */
    @Override
    public List<Comment> findTopLikedCommentsSafely(Long postId, int limit) {
        log.debug("执行安全热门评论查询: postId={}, limit={}", postId, limit);
        
        // 验证限制参数
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("post.id", postId);
        // 只查询有点赞的评论 - 使用大于0的条件需要特殊处理
        // 这里暂时不添加条件，让基类处理所有评论然后按点赞数排序
        
        Page<Comment> result = findSafePaginated(conditions, "like_count", "DESC", 0, limit);
        return result.getContent();
    }
    
    /**
     * 安全的复合条件查询
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return 查询结果
     */
    @Override
    public List<Comment> findCommentsByConditionsSafely(Map<String, Object> conditions, 
                                                      String sortField, String sortDirection) {
        log.debug("执行安全复合条件查询: conditions={}, sortField={}, sortDirection={}", 
                 conditions, sortField, sortDirection);
        
        Page<Comment> result = findSafePaginated(conditions, sortField, sortDirection, 0, 1000);
        return result.getContent();
    }
    
    /**
     * 安全的评论统计
     * 
     * @param conditions 统计条件
     * @return 评论数量
     */
    @Override
    public long countCommentsSafely(Map<String, Object> conditions) {
        log.debug("执行安全评论统计: conditions={}", conditions);
        
        return countSafe(conditions);
    }
    
    /**
     * 安全的评论内容搜索
     * 
     * @param content 内容关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Override
    public Page<Comment> findByContentContainingSafely(String content, Pageable pageable) {
        log.debug("执行安全评论内容搜索: content={}", content);
        
        List<String> searchFields = List.of("content");
        return findSafeSearch(searchFields, content, null, pageable);
    }
    
    /**
     * 创建安全的分页参数
     * 
     * @param page 页码
     * @param size 页大小
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return Pageable对象
     */
    private Pageable createSafePageable(int page, int size, String sortField, String sortDirection) {
        validatePaginationParams(page, size);
        
        if (sortField != null && !sortField.trim().isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(
                page, size, createSafeSort(sortField, sortDirection)
            );
        } else {
            return org.springframework.data.domain.PageRequest.of(page, size);
        }
    }
}