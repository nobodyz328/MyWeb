package com.myweb.website_core.infrastructure.mapper;

import com.myweb.website_core.domain.entity.Post;
import com.myweb.website_core.domain.vo.PostSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {
    
    /**
     * 插入帖子
     */
    int insertPost(Post post);
    
    /**
     * 根据ID查询帖子
     */
    Post selectPostById(@Param("id") Long id);
    
    /**
     * 查询所有帖子
     */
    List<Post> selectAllPosts();
    
    /**
     * 根据用户ID查询帖子
     */
    List<Post> selectPostsByUserId(@Param("userId") Long userId);
    
    /**
     * 更新帖子
     */
    int updatePost(Post post);
    
    /**
     * 删除帖子
     */
    int deletePost(@Param("id") Long id);
    
    /**
     * 搜索帖子
     */
    List<Post> searchPosts(@Param("keyword") String keyword);
    
    /**
     * 获取点赞数前20的帖子
     */
    List<Post> selectTopLikedPosts(@Param("limit") int limit);
    
    /**
     * 更新帖子点赞数
     */
    int updateLikeCount(@Param("id") Long id, @Param("likeCount") Integer likeCount);
    
    /**
     * 更新帖子收藏数
     */
    int updateCollectCount(@Param("id") Long id, @Param("collectCount") Integer collectCount);
    
    // ========== 搜索功能相关方法 ==========
    
    /**
     * 高性能帖子搜索 - 支持全文搜索和分页
     * 
     * @param keyword 搜索关键词
     * @param sortBy 排序方式：RELEVANCE（相关性）、TIME（时间）、POPULARITY（热度）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 帖子搜索结果列表
     */
    List<PostSearchVO> searchPostsWithPagination(
        @Param("keyword") String keyword,
        @Param("sortBy") String sortBy,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 统计帖子搜索结果总数
     * 
     * @param keyword 搜索关键词
     * @return 搜索结果总数
     */
    Long countSearchPosts(@Param("keyword") String keyword);
    
    /**
     * 根据标题搜索帖子（精确匹配优先）
     * 
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 帖子搜索结果列表
     */
    List<PostSearchVO> searchPostsByTitle(
        @Param("keyword") String keyword,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 根据内容搜索帖子
     * 
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 帖子搜索结果列表
     */
    List<PostSearchVO> searchPostsByContent(
        @Param("keyword") String keyword,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 根据作者搜索帖子
     * 
     * @param keyword 搜索关键词（作者用户名）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 帖子搜索结果列表
     */
    List<PostSearchVO> searchPostsByAuthor(
        @Param("keyword") String keyword,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 获取热门帖子（用于搜索结果排序参考）
     * 
     * @param limit 限制数量
     * @return 热门帖子列表
     */
    List<PostSearchVO> getHotPosts(@Param("limit") Integer limit);
    
    /**
     * 游标分页搜索帖子（用于无限滚动）
     * 
     * @param keyword 搜索关键词
     * @param sortBy 排序方式：RELEVANCE（相关性）、TIME（时间）、POPULARITY（热度）
     * @param lastId 上次加载的最后一个帖子ID，首次加载传0
     * @param limit 限制数量
     * @return 帖子搜索结果列表
     */
    List<PostSearchVO> searchPostsWithCursor(
        @Param("keyword") String keyword,
        @Param("sortBy") String sortBy,
        @Param("lastId") Long lastId,
        @Param("limit") Integer limit
    );
    
    /**
     * 获取所有帖子（游标分页）- 用于关键词为"all"时
     * 
     * @param sortBy 排序方式：TIME（时间）、POPULARITY（热度）
     * @param lastId 上次加载的最后一个帖子ID，首次加载传0
     * @param limit 限制数量
     * @return 所有帖子列表
     */
    List<PostSearchVO> getAllPostsWithCursor(
        @Param("sortBy") String sortBy,
        @Param("lastId") Long lastId,
        @Param("limit") Integer limit
    );
} 