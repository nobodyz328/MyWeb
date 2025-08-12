package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.vo.UserSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问接口
 * 
 * 提供用户相关的数据库操作，包括基本的CRUD操作和搜索功能
 */
@Mapper
public interface UserMapper {
    
    // ========== 搜索功能相关方法 ==========
    
    /**
     * 高性能用户搜索 - 支持用户名和昵称模糊搜索
     * 
     * @param keyword 搜索关键词
     * @param sortBy 排序方式：RELEVANCE（相关性）、POPULARITY（热度）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户搜索结果列表
     */
    List<UserSearchVO> searchUsersWithPagination(
        @Param("keyword") String keyword,
        @Param("sortBy") String sortBy,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 统计用户搜索结果总数
     * 
     * @param keyword 搜索关键词
     * @return 搜索结果总数
     */
    Long countSearchUsers(@Param("keyword") String keyword);
    
    /**
     * 根据用户名搜索用户（精确匹配优先）
     * 
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户搜索结果列表
     */
    List<UserSearchVO> searchUsersByUsername(
        @Param("keyword") String keyword,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 根据个人简介搜索用户
     * 
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户搜索结果列表
     */
    List<UserSearchVO> searchUsersByBio(
        @Param("keyword") String keyword,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 获取活跃用户（用于搜索结果排序参考）
     * 
     * @param limit 限制数量
     * @return 活跃用户列表
     */
    List<UserSearchVO> getActiveUsers(@Param("limit") Integer limit);
    
    // ========== 统计相关方法 ==========
    

    /**
     * 游标分页搜索用户（用于无限滚动）
     * 
     * @param keyword 搜索关键词
     * @param sortBy 排序方式：RELEVANCE（相关性）、POPULARITY（热度）
     * @param lastId 上次加载的最后一个用户ID，首次加载传0
     * @param limit 限制数量
     * @return 用户搜索结果列表
     */
    List<UserSearchVO> searchUsersWithCursor(
        @Param("keyword") String keyword,
        @Param("sortBy") String sortBy,
        @Param("lastId") Long lastId,
        @Param("limit") Integer limit
    );
    
    /**
     * 获取所有用户（游标分页）- 用于关键词为"all"时
     * 
     * @param sortBy 排序方式：POPULARITY（热度）
     * @param lastId 上次加载的最后一个用户ID，首次加载传0
     * @param limit 限制数量
     * @return 所有用户列表
     */
    List<UserSearchVO> getAllUsersWithCursor(
        @Param("sortBy") String sortBy,
        @Param("lastId") Long lastId,
        @Param("limit") Integer limit
    );
}