package com.myweb.website_core.domain.business.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 搜索请求DTO
 * 
 * 用于封装搜索请求的参数，包括：
 * - 搜索关键词
 * - 搜索类型（帖子、用户、全部）
 * - 分页参数
 * - 排序方式
 */
@Data
public class SearchRequestDTO {
    
    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;
    
    /**
     * 搜索类型：POST（帖子）、USER（用户）、ALL（全部）
     */
    private String type = "ALL";
    
    /**
     * 页码，从0开始
     */
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 20;
    
    /**
     * 排序方式：RELEVANCE（相关性）、TIME（时间）、POPULARITY（热度）
     */
    private String sortBy = "RELEVANCE";
    
    /**
     * 获取偏移量
     */
    public Integer getOffset() {
        return page * size;
    }
    
    /**
     * 验证搜索类型是否有效
     */
    public boolean isValidType() {
        return "POST".equals(type) || "USER".equals(type) || "ALL".equals(type);
    }
    
    /**
     * 验证排序方式是否有效
     */
    public boolean isValidSortBy() {
        return "RELEVANCE".equals(sortBy) || "TIME".equals(sortBy) || "POPULARITY".equals(sortBy);
    }
}