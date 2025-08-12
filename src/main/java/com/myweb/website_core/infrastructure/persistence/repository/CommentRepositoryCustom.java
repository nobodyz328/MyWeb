package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.business.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 评论Repository自定义接口
 * <p>
 * 定义安全的评论查询方法：
 * 1. 安全搜索查询
 * 2. 安全分页查询
 * 3. 安全条件查询
 * 4. 安全统计查询
 * <p>
 * 符合需求：5.1, 5.4, 5.6 - 安全查询服务集成
 * 
 * @author MyWeb
 * @version 1.0
 */
public interface CommentRepositoryCustom {
    
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
    Page<Comment> findCommentsWithSafeSearch(String keyword, String sortField, 
                                           String sortDirection, int page, int size);
    
    /**
     * 根据帖子ID安全查询评论
     * 
     * @param postId 帖子ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    Page<Comment> findCommentsByPostWithSafePagination(Long postId, int page, int size);
    
    /**
     * 根据作者ID安全查询评论
     * 
     * @param authorId 作者ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    Page<Comment> findCommentsByAuthorWithSafePagination(Long authorId, int page, int size);
    
    /**
     * 安全的顶级评论查询
     * 
     * @param postId 帖子ID
     * @param pageable 分页参数
     * @return 顶级评论列表
     */
    Page<Comment> findTopLevelCommentsSafely(Long postId, Pageable pageable);
    
    /**
     * 安全的回复评论查询
     * 
     * @param parentId 父评论ID
     * @param pageable 分页参数
     * @return 回复评论列表
     */
    Page<Comment> findRepliesSafely(Long parentId, Pageable pageable);
    
    /**
     * 安全的热门评论查询
     * 
     * @param postId 帖子ID
     * @param limit 限制数量
     * @return 热门评论列表
     */
    List<Comment> findTopLikedCommentsSafely(Long postId, int limit);
    
    /**
     * 安全的复合条件查询
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return 查询结果
     */
    List<Comment> findCommentsByConditionsSafely(Map<String, Object> conditions, 
                                               String sortField, String sortDirection);
    
    /**
     * 安全的评论统计
     * 
     * @param conditions 统计条件
     * @return 评论数量
     */
    long countCommentsSafely(Map<String, Object> conditions);
    
    /**
     * 安全的评论内容搜索
     * 
     * @param content 内容关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<Comment> findByContentContainingSafely(String content, Pageable pageable);
}