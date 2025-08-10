package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.common.util.SafeSqlBuilder;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 安全的Repository基类
 * <p>
 * 提供安全的数据访问方法，集成SQL注入防护：
 * 1. 安全的动态查询构建
 * 2. 参数验证和清理
 * 3. 排序和分页安全处理
 * 4. 搜索条件安全验证
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
public abstract class SafeRepositoryBase<T, ID> {
    
    @Autowired
    protected SafeSqlBuilder safeSqlBuilder;
    
    @Autowired
    protected SqlInjectionProtectionService sqlInjectionProtectionService;
    
    /**
     * 获取具体的JPA Repository实例
     * 子类必须实现此方法
     */
    protected abstract JpaRepository<T, ID> getRepository();
    
    /**
     * 获取具体的JPA Specification Executor实例
     * 子类必须实现此方法
     */
    protected abstract JpaSpecificationExecutor<T> getSpecificationExecutor();
    
    /**
     * 获取实体对应的表名
     * 子类必须实现此方法
     */
    protected abstract String getTableName();
    
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
    public Page<T> findSafePaginated(Map<String, Object> conditions, 
                                   String sortField, String sortDirection, 
                                   int page, int size) {
        
        // 验证分页参数
        validatePaginationParams(page, size);
        
        // 创建安全的排序
        Sort sort = createSafeSort(sortField, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 创建安全的查询条件
        Specification<T> specification = createSafeSpecification(conditions);
        
        return getSpecificationExecutor().findAll(specification, pageable);
    }
    
    /**
     * 安全的搜索查询
     * 
     * @param searchFields 搜索字段列表
     * @param keyword 搜索关键词
     * @param additionalConditions 额外查询条件
     * @param pageable 分页参数
     * @return 搜索结果
     */
    public Page<T> findSafeSearch(List<String> searchFields, String keyword, 
                                Map<String, Object> additionalConditions, 
                                Pageable pageable) {
        
        // 验证搜索参数
        validateSearchParams(searchFields, keyword);
        
        // 创建搜索规范
        Specification<T> searchSpec = createSearchSpecification(searchFields, keyword);
        
        // 创建额外条件规范
        Specification<T> conditionSpec = createSafeSpecification(additionalConditions);
        
        // 组合规范
        Specification<T> combinedSpec = Specification.where(searchSpec).and(conditionSpec);
        
        return getSpecificationExecutor().findAll(combinedSpec, pageable);
    }
    
    /**
     * 安全的条件查询
     * 
     * @param conditions 查询条件
     * @return 查询结果列表
     */
    public List<T> findSafeByConditions(Map<String, Object> conditions) {
        Specification<T> specification = createSafeSpecification(conditions);
        return getSpecificationExecutor().findAll(specification);
    }
    
    /**
     * 安全的单条记录查询
     * 
     * @param conditions 查询条件
     * @return 查询结果
     */
    public T findSafeOne(Map<String, Object> conditions) {
        Specification<T> specification = createSafeSpecification(conditions);
        List<T> results = getSpecificationExecutor().findAll(specification);
        
        if (results.isEmpty()) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new IllegalStateException("Expected single result, but found " + results.size());
        }
    }
    
    /**
     * 安全的计数查询
     * 
     * @param conditions 查询条件
     * @return 记录数量
     */
    public long countSafe(Map<String, Object> conditions) {
        Specification<T> specification = createSafeSpecification(conditions);
        return getSpecificationExecutor().count(specification);
    }
    
    /**
     * 创建安全的查询规范
     * 
     * @param conditions 查询条件
     * @return JPA Specification
     */
    protected Specification<T> createSafeSpecification(Map<String, Object> conditions) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (conditions != null && !conditions.isEmpty()) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    String field = entry.getKey();
                    Object value = entry.getValue();
                    
                    // 验证字段名安全性
                    validateFieldName(field);
                    
                    // 验证字段值安全性
                    if (value instanceof String) {
                        sqlInjectionProtectionService.validateAndSanitizeInput(
                            (String) value, "JPA_CONDITION", field
                        );
                    }
                    
                    // 创建谓词
                    if (value == null) {
                        predicates.add(criteriaBuilder.isNull(root.get(field)));
                    } else if (value instanceof String && ((String) value).contains("%")) {
                        predicates.add(criteriaBuilder.like(root.get(field), (String) value));
                    } else {
                        predicates.add(criteriaBuilder.equal(root.get(field), value));
                    }
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 创建搜索规范
     * 
     * @param searchFields 搜索字段列表
     * @param keyword 搜索关键词
     * @return JPA Specification
     */
    protected Specification<T> createSearchSpecification(List<String> searchFields, String keyword) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            if (searchFields == null || searchFields.isEmpty() || keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            List<Predicate> searchPredicates = new ArrayList<>();
            String searchPattern = "%" + keyword.trim() + "%";
            
            for (String field : searchFields) {
                validateFieldName(field);
                searchPredicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get(field).as(String.class)), 
                        searchPattern.toLowerCase()
                    )
                );
            }
            
            return criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 创建安全的排序
     * 
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return Sort对象
     */
    protected Sort createSafeSort(String sortField, String sortDirection) {
        if (sortField == null || sortField.trim().isEmpty()) {
            return Sort.unsorted();
        }
        
        // 验证排序字段
        List<String> allowedFields = safeSqlBuilder.getAllowedSortFields(getTableName());
        if (!allowedFields.contains(sortField)) {
            throw new IllegalArgumentException(
                String.format("Sort field '%s' not allowed for table '%s'", sortField, getTableName())
            );
        }
        
        // 验证排序方向
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDirection != null) {
            try {
                direction = Sort.Direction.fromString(sortDirection);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid sort direction: " + sortDirection);
            }
        }
        
        return Sort.by(direction, sortField);
    }
    
    /**
     * 验证分页参数
     * 
     * @param page 页码
     * @param size 页大小
     */
    protected void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("Page size must be between 1 and 1000");
        }
    }
    
    /**
     * 验证搜索参数
     * 
     * @param searchFields 搜索字段列表
     * @param keyword 搜索关键词
     */
    protected void validateSearchParams(List<String> searchFields, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            sqlInjectionProtectionService.validateAndSanitizeInput(keyword, "SEARCH", "keyword");
        }
        
        if (searchFields != null) {
            List<String> allowedFields = safeSqlBuilder.getAllowedSortFields(getTableName());
            for (String field : searchFields) {
                if (!allowedFields.contains(field)) {
                    throw new IllegalArgumentException(
                        String.format("Search field '%s' not allowed for table '%s'", field, getTableName())
                    );
                }
            }
        }
    }
    
    /**
     * 验证字段名安全性
     * 
     * @param fieldName 字段名
     */
    protected void validateFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        
        // 检查字段名是否在允许列表中
        List<String> allowedFields = safeSqlBuilder.getAllowedSortFields(getTableName());
        if (!allowedFields.contains(fieldName)) {
            throw new IllegalArgumentException(
                String.format("Field '%s' not allowed for table '%s'", fieldName, getTableName())
            );
        }
    }
}