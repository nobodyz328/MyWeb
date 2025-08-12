package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.business.entity.Post;
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
 * 帖子安全Repository实现类
 * <p>
 * 扩展SafeRepositoryBase，提供安全的帖子查询方法：
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
public class PostRepositoryImpl extends SafeRepositoryBase<Post, Long> implements PostRepositoryCustom {
    
    private final PostRepository postRepository;
    
    public PostRepositoryImpl(@Lazy PostRepository postRepository) {
        this.postRepository = postRepository;
    }
    
    @Override
    protected JpaRepository<Post, Long> getRepository() {
        return postRepository;
    }
    
    @Override
    protected JpaSpecificationExecutor<Post> getSpecificationExecutor() {
        return postRepository;
    }
    
    @Override
    protected String getTableName() {
        return "posts";
    }
    
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
    @Override
    public Page<Post> findPostsWithSafeSearch(String keyword, String sortField, 
                                            String sortDirection, int page, int size) {
        log.debug("执行安全帖子搜索: keyword={}, sortField={}, sortDirection={}, page={}, size={}", 
                 keyword, sortField, sortDirection, page, size);
        
        // 定义搜索字段
        List<String> searchFields = List.of("title", "content");
        
        // 创建分页参数
        Pageable pageable = createSafePageable(page, size, sortField, sortDirection);
        
        // 执行安全搜索
        return findSafeSearch(searchFields, keyword, null, pageable);
    }
    
    /**
     * 根据作者ID安全查询帖子
     * 
     * @param authorId 作者ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Post> findPostsByAuthorWithSafePagination(Long authorId, int page, int size) {
        log.debug("执行安全作者帖子查询: authorId={}, page={}, size={}", authorId, page, size);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("author.id", authorId);
        
        return findSafePaginated(conditions, "created_at", "DESC", page, size);
    }
    
    /**
     * 安全的热门帖子查询
     * 
     * @param limit 限制数量
     * @return 热门帖子列表
     */
    @Override
    public List<Post> findTopLikedPostsSafely(int limit) {
        log.debug("执行安全热门帖子查询: limit={}", limit);
        
        // 验证限制参数
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        
        Map<String, Object> conditions = new HashMap<>();
        // 只查询有点赞的帖子 - 使用大于0的条件需要特殊处理
        // 这里暂时不添加条件，让基类处理所有帖子然后按点赞数排序
        
        Page<Post> result = findSafePaginated(conditions, "like_count", "DESC", 0, limit);
        return result.getContent();
    }
    
    /**
     * 安全的标题搜索
     * 
     * @param title 标题关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Override
    public Page<Post> findByTitleContainingSafely(String title, Pageable pageable) {
        log.debug("执行安全标题搜索: title={}", title);
        
        List<String> searchFields = List.of("title");
        return findSafeSearch(searchFields, title, null, pageable);
    }
    
    /**
     * 安全的内容搜索
     * 
     * @param content 内容关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Override
    public Page<Post> findByContentContainingSafely(String content, Pageable pageable) {
        log.debug("执行安全内容搜索: content={}", content);
        
        List<String> searchFields = List.of("content");
        return findSafeSearch(searchFields, content, null, pageable);
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
    public List<Post> findPostsByConditionsSafely(Map<String, Object> conditions, 
                                                String sortField, String sortDirection) {
        log.debug("执行安全复合条件查询: conditions={}, sortField={}, sortDirection={}", 
                 conditions, sortField, sortDirection);
        
        Page<Post> result = findSafePaginated(conditions, sortField, sortDirection, 0, 1000);
        return result.getContent();
    }
    
    /**
     * 安全的帖子统计
     * 
     * @param conditions 统计条件
     * @return 帖子数量
     */
    @Override
    public long countPostsSafely(Map<String, Object> conditions) {
        log.debug("执行安全帖子统计: conditions={}", conditions);
        
        return countSafe(conditions);
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