package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.business.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 用户Repository自定义接口
 * <p>
 * 定义安全的用户查询方法：
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
public interface UserRepositoryCustom {
    
    /**
     * 安全的用户搜索
     * 
     * @param keyword 搜索关键词
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 搜索结果
     */
    Page<User> findUsersWithSafeSearch(String keyword, String sortField, 
                                      String sortDirection, int page, int size);
    
    /**
     * 安全的用户名搜索
     * 
     * @param username 用户名关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<User> findByUsernameContainingSafely(String username, Pageable pageable);
    
    /**
     * 安全的邮箱搜索
     * 
     * @param email 邮箱关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<User> findByEmailContainingSafely(String email, Pageable pageable);
    
    /**
     * 安全的活跃用户查询
     * 
     * @param limit 限制数量
     * @return 活跃用户列表
     */
    List<User> findActiveUsersSafely(int limit);
    
    /**
     * 安全的复合条件查询
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return 查询结果
     */
    List<User> findUsersByConditionsSafely(Map<String, Object> conditions, 
                                         String sortField, String sortDirection);
    
    /**
     * 安全的用户统计
     * 
     * @param conditions 统计条件
     * @return 用户数量
     */
    long countUsersSafely(Map<String, Object> conditions);
    
    /**
     * 安全的用户分页查询
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    Page<User> findUsersWithSafePagination(Map<String, Object> conditions, 
                                         String sortField, String sortDirection, 
                                         int page, int size);
}