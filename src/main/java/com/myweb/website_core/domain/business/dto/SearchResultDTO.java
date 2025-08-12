package com.myweb.website_core.domain.business.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 搜索结果DTO
 * 
 * 用于封装搜索结果，支持泛型以适应不同类型的搜索结果
 * 
 * @param <T> 搜索结果项的类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDTO<T> {
    
    /**
     * 搜索结果列表
     */
    private List<T> items;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 搜索类型
     */
    private String type;
    
    /**
     * 排序方式
     */
    private String sortBy;
    
    /**
     * 下一页游标（用于无限滚动分页）
     */
    private String nextCursor;
    
    /**
     * 是否有更多数据（用于无限滚动分页）
     */
    private Boolean hasMore;
    
    /**
     * 是否有下一页（传统分页）
     */
    public boolean hasNext() {
        return (page + 1) * size < total;
    }
    
    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return page > 0;
    }
    
    /**
     * 获取总页数
     */
    @JsonIgnore
    public Integer getTotalPages() {
        if (total == null || size == null || size == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }
    
    /**
     * 获取是否有更多数据（兼容方法）
     */
    public Boolean getHasMore() {
        return hasMore != null ? hasMore : false;
    }
    
    /**
     * 创建空结果
     */
    public static <T> SearchResultDTO<T> empty(SearchRequestDTO request) {
        SearchResultDTO<T> result = new SearchResultDTO<>();
        result.setItems(List.of());
        result.setTotal(0L);
        result.setPage(request.getPage());
        result.setSize(request.getSize());
        result.setKeyword(request.getKeyword());
        result.setType(request.getType());
        result.setSortBy(request.getSortBy());
        result.setNextCursor(null);
        result.setHasMore(false);
        return result;
    }
    
    /**
     * 创建无限滚动空结果
     */
    public static <T> SearchResultDTO<T> emptyInfiniteScroll(String keyword, String type, String sortBy, Integer size) {
        SearchResultDTO<T> result = new SearchResultDTO<>();
        result.setItems(List.of());
        result.setTotal(0L);
        result.setPage(0);
        result.setSize(size);
        result.setKeyword(keyword);
        result.setType(type);
        result.setSortBy(sortBy);
        result.setNextCursor(null);
        result.setHasMore(false);
        return result;
    }
}