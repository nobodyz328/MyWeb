package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.business.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户安全Repository实现类
 * <p>
 * 扩展SafeRepositoryBase，提供安全的用户查询方法：
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
public class UserRepositoryImpl extends SafeRepositoryBase<User, Long> implements UserRepositoryCustom {
    
    private final UserRepository userRepository;
    
    public UserRepositoryImpl(@Lazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }
    
    @Override
    protected JpaSpecificationExecutor<User> getSpecificationExecutor() {
        return userRepository;
    }
    
    @Override
    protected String getTableName() {
        return "users";
    }
    
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
    @Override
    public Page<User> findUsersWithSafeSearch(String keyword, String sortField, 
                                            String sortDirection, int page, int size) {
        log.debug("执行安全用户搜索: keyword={}, sortField={}, sortDirection={}, page={}, size={}", 
                 keyword, sortField, sortDirection, page, size);
        
        // 定义搜索字段
        List<String> searchFields = List.of("username", "email");
        
        // 创建分页参数
        Pageable pageable = createSafePageable(page, size, sortField, sortDirection);
        
        // 执行安全搜索
        return findSafeSearch(searchFields, keyword, null, pageable);
    }
    
    /**
     * 安全的用户名搜索
     * 
     * @param username 用户名关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Override
    public Page<User> findByUsernameContainingSafely(String username, Pageable pageable) {
        log.debug("执行安全用户名搜索: username={}", username);
        
        List<String> searchFields = List.of("username");
        return findSafeSearch(searchFields, username, null, pageable);
    }
    
    /**
     * 安全的邮箱搜索
     * 
     * @param email 邮箱关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Override
    public Page<User> findByEmailContainingSafely(String email, Pageable pageable) {
        log.debug("执行安全邮箱搜索: email={}", email);
        
        List<String> searchFields = List.of("email");
        return findSafeSearch(searchFields, email, null, pageable);
    }
    
    /**
     * 安全的活跃用户查询
     * 
     * @param limit 限制数量
     * @return 活跃用户列表
     */
    @Override
    public List<User> findActiveUsersSafely(int limit) {
        log.debug("执行安全活跃用户查询: limit={}", limit);
        
        // 验证限制参数
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        
        Map<String, Object> conditions = new HashMap<>();
        // 可以根据业务需求添加活跃用户的条件，比如最近登录时间等
        
        Page<User> result = findSafePaginated(conditions, "liked_count", "DESC", 0, limit);
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
    public List<User> findUsersByConditionsSafely(Map<String, Object> conditions, 
                                                String sortField, String sortDirection) {
        log.debug("执行安全复合条件查询: conditions={}, sortField={}, sortDirection={}", 
                 conditions, sortField, sortDirection);
        
        Page<User> result = findSafePaginated(conditions, sortField, sortDirection, 0, 1000);
        return result.getContent();
    }
    
    /**
     * 安全的用户统计
     * 
     * @param conditions 统计条件
     * @return 用户数量
     */
    @Override
    public long countUsersSafely(Map<String, Object> conditions) {
        log.debug("执行安全用户统计: conditions={}", conditions);
        
        return countSafe(conditions);
    }
    
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
    @Override
    public Page<User> findUsersWithSafePagination(Map<String, Object> conditions, 
                                                String sortField, String sortDirection, 
                                                int page, int size) {
        log.debug("执行安全用户分页查询: conditions={}, sortField={}, sortDirection={}, page={}, size={}", 
                 conditions, sortField, sortDirection, page, size);
        
        return findSafePaginated(conditions, sortField, sortDirection, page, size);
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