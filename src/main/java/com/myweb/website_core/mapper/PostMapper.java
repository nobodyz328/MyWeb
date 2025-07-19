package com.myweb.website_core.mapper;

import com.myweb.website_core.demos.web.blog.Post;
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
} 