package com.myweb.website_core.infrastructure.persistence.repository.post;

import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.infrastructure.persistence.repository.SafeRepositoryBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
    
    @PersistenceContext
    private EntityManager entityManager;
    
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
     * 安全的分页查询
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Post> findSafePaginated(Map<String, Object> conditions, 
                                      String sortField, String sortDirection, 
                                      int page, int size) {
        log.debug("执行安全分页查询: conditions={}, sortField={}, sortDirection={}, page={}, size={}", 
                 conditions, sortField, sortDirection, page, size);
        
        return super.findSafePaginated(conditions, sortField, sortDirection, page, size);
    }
    
    /**
     * 执行复杂动态查询
     * 
     * @param dynamicQuery 动态查询SQL
     * @param parameters 查询参数
     * @return 查询结果
     */
    @Override
    public List<Post> findPostsByComplexQuery(String dynamicQuery, Map<String, Object> parameters) {
        log.debug("执行复杂动态查询: query={}, parameterCount={}", 
                 dynamicQuery.substring(0, Math.min(100, dynamicQuery.length())), 
                 parameters != null ? parameters.size() : 0);
        
        try {
            // 使用JPA原生查询执行复杂动态查询
            jakarta.persistence.Query query = entityManager.createNativeQuery(dynamicQuery, Post.class);
            
            // 设置参数
            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }
            
            @SuppressWarnings("unchecked")
            List<Post> results = query.getResultList();
            
            log.debug("复杂动态查询执行成功: resultCount={}", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("执行复杂动态查询失败: {}", e.getMessage());
            throw new RuntimeException("执行复杂动态查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行参数化查询
     * 
     * @param query 参数化查询SQL
     * @param parameters 查询参数
     * @return 查询结果
     */
    @Override
    public List<Post> findPostsByParameterizedQuery(String query, Map<String, Object> parameters) {
        log.debug("执行参数化查询: parameterCount={}", parameters != null ? parameters.size() : 0);
        
        try {
            // 使用JPA原生查询执行参数化查询
            jakarta.persistence.Query jpqlQuery = entityManager.createNativeQuery(query, Post.class);
            
            // 设置参数
            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    jpqlQuery.setParameter(entry.getKey(), entry.getValue());
                }
            }
            
            @SuppressWarnings("unchecked")
            List<Post> results = jpqlQuery.getResultList();
            
            log.debug("参数化查询执行成功: resultCount={}", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("执行参数化查询失败: {}", e.getMessage());
            throw new RuntimeException("执行参数化查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行聚合统计查询
     * 
     * @param aggregateQuery 聚合查询SQL
     * @param parameters 查询参数
     * @return 统计结果
     */
    @Override
    public List<Map<String, Object>> executeAggregateQuery(String aggregateQuery, Map<String, Object> parameters) {
        log.debug("执行聚合统计查询: parameterCount={}", parameters != null ? parameters.size() : 0);
        
        try {
            // 使用JPA原生查询执行聚合查询
            jakarta.persistence.Query query = entityManager.createNativeQuery(aggregateQuery);
            
            // 设置参数
            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }
            
            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = query.getResultList();
            
            // 转换结果为Map格式
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            for (Object[] row : rawResults) {
                Map<String, Object> resultMap = new HashMap<>();
                for (int i = 0; i < row.length; i++) {
                    resultMap.put("column_" + i, row[i]);
                }
                results.add(resultMap);
            }
            
            log.debug("聚合统计查询执行成功: resultCount={}", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("执行聚合统计查询失败: {}", e.getMessage());
            throw new RuntimeException("执行聚合统计查询失败: " + e.getMessage(), e);
        }
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