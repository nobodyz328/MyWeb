package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.business.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 帖子Repository自定义接口
 * <p>
 * 定义安全的帖子查询方法：
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
public interface PostRepositoryCustom {
    
    /**
     * 安全的帖子搜索
     * 
     * @param keyword 搜索关键词
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 搜索结果
     */
    Page<Post> findPostsWithSafeSearch(String keyword, String sortField, 
                                      String sortDirection, int page, int size);
    
    /**
     * 根据作者ID安全查询帖子
     * 
     * @param authorId 作者ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    Page<Post> findPostsByAuthorWithSafePagination(Long authorId, int page, int size);
    
    /**
     * 安全的热门帖子查询
     * 
     * @param limit 限制数量
     * @return 热门帖子列表
     */
    List<Post> findTopLikedPostsSafely(int limit);
    
    /**
     * 安全的标题搜索
     * 
     * @param title 标题关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<Post> findByTitleContainingSafely(String title, Pageable pageable);
    
    /**
     * 安全的内容搜索
     * 
     * @param content 内容关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<Post> findByContentContainingSafely(String content, Pageable pageable);
    
    /**
     * 安全的复合条件查询
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return 查询结果
     */
    List<Post> findPostsByConditionsSafely(Map<String, Object> conditions, 
                                         String sortField, String sortDirection);
    
    /**
     * 安全的帖子统计
     * 
     * @param conditions 统计条件
     * @return 帖子数量
     */
    long countPostsSafely(Map<String, Object> conditions);
}