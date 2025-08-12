package com.myweb.website_core.infrastructure.persistence.repository.post;

import com.myweb.website_core.domain.business.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 帖子数据访问接口
 * <p>
 * 提供帖子相关的数据库操作：
 * - 基础CRUD操作
 * - 自定义查询方法
 * - 分页和排序
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post>, PostRepositoryCustom {
    // 可扩展自定义查询
    List<Post> findByAuthorId(Long authorId);

    /**
     * 根据作者ID查找帖子
     * 
     * @param authorId 作者ID
     * @return 帖子列表
     */
    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    
    /**
     * 根据标题模糊查询帖子
     * 
     * @param title 标题关键词
     * @return 帖子列表
     */
    List<Post> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
    
    /**
     * 根据内容模糊查询帖子
     * 
     * @param content 内容关键词
     * @return 帖子列表
     */
    List<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String content);
    
    /**
     * 获取点赞数最多的帖子
     * 
     * @param limit 限制数量
     * @return 帖子列表
     */
    @Query("SELECT p FROM Post p ORDER BY p.likeCount DESC")
    List<Post> findTopLikedPosts(@Param("limit") int limit);
    
    /**
     * 搜索帖子（标题或内容包含关键词）
     * 
     * @param keyword 搜索关键词
     * @return 帖子列表
     */
    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY p.createdAt DESC")
    List<Post> searchPosts(@Param("keyword") String keyword);
}