package com.myweb.website_core.domain.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 搜索统计数据传输对象
 * 
 * 用于记录和分析搜索性能和使用情况
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchStatsDTO {
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 搜索类型
     */
    private String type;
    
    /**
     * 搜索结果总数
     */
    private Long totalResults;
    
    /**
     * 搜索执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 是否使用了缓存
     */
    private Boolean cacheHit;
    
    /**
     * 搜索的用户ID（如果是登录用户）
     */
    private Long userId;
    
    /**
     * 搜索时间戳
     */
    private Long timestamp;
    
    /**
     * 搜索来源：WEB, API, MOBILE
     */
    private String source;
    
    /**
     * 是否返回了结果
     */
    private Boolean hasResults;
    
    /**
     * 用户是否点击了搜索结果
     */
    private Boolean clicked;
    
    /**
     * 点击的结果位置（第几个结果）
     */
    private Integer clickPosition;
    
    /**
     * 创建搜索统计记录
     */
    public static SearchStatsDTO create(String keyword, String type, Long totalResults, Long executionTime) {
        SearchStatsDTO stats = new SearchStatsDTO();
        stats.setKeyword(keyword);
        stats.setType(type);
        stats.setTotalResults(totalResults);
        stats.setExecutionTime(executionTime);
        stats.setTimestamp(System.currentTimeMillis());
        stats.setHasResults(totalResults > 0);
        stats.setCacheHit(false);
        stats.setClicked(false);
        return stats;
    }
}