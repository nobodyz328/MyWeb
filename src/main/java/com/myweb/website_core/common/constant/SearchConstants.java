package com.myweb.website_core.common.constant;

/**
 * 搜索相关常量
 * 
 * 定义搜索功能中使用的各种常量，包括：
 * - 搜索类型常量
 * - 排序方式常量
 * - 缓存相关常量
 * - 搜索限制常量
 */
public class SearchConstants {
    
    // ========== 搜索类型常量 ==========
    
    /**
     * 搜索类型：帖子
     */
    public static final String SEARCH_TYPE_POST = "POST";
    
    /**
     * 搜索类型：用户
     */
    public static final String SEARCH_TYPE_USER = "USER";
    
    /**
     * 搜索类型：评论
     */
    public static final String SEARCH_TYPE_COMMENT = "COMMENT";
    
    /**
     * 搜索类型：全部
     */
    public static final String SEARCH_TYPE_ALL = "ALL";
    
    // ========== 排序方式常量 ==========
    
    /**
     * 排序方式：相关性
     */
    public static final String SORT_BY_RELEVANCE = "RELEVANCE";
    
    /**
     * 排序方式：时间
     */
    public static final String SORT_BY_TIME = "TIME";
    
    /**
     * 排序方式：热度
     */
    public static final String SORT_BY_POPULARITY = "POPULARITY";
    
    // ========== 缓存相关常量 ==========
    
    /**
     * 搜索缓存前缀
     */
    public static final String SEARCH_CACHE_PREFIX = "search:cache:";
    
    /**
     * 帖子搜索缓存前缀
     */
    public static final String POST_SEARCH_CACHE_PREFIX = "search:post:";
    
    /**
     * 用户搜索缓存前缀
     */
    public static final String USER_SEARCH_CACHE_PREFIX = "search:user:";
    
    /**
     * 评论搜索缓存前缀
     */
    public static final String COMMENT_SEARCH_CACHE_PREFIX = "search:comment:";
    
    /**
     * 热门搜索关键词缓存键
     */
    public static final String HOT_SEARCH_KEYWORDS_KEY = "search:hot:keywords";
    
    /**
     * 搜索统计缓存键前缀
     */
    public static final String SEARCH_STATS_PREFIX = "search:stats:";
    
    // ========== 搜索限制常量 ==========
    
    /**
     * 最大关键词长度
     */
    public static final int MAX_KEYWORD_LENGTH = 100;
    
    /**
     * 最小关键词长度
     */
    public static final int MIN_KEYWORD_LENGTH = 1;
    
    /**
     * 默认页面大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * 最大页面大小
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * 最大搜索结果数
     */
    public static final int MAX_SEARCH_RESULTS = 1000;
    
    /**
     * 缓存过期时间（分钟）
     */
    public static final int CACHE_EXPIRE_MINUTES = 30;
    
    /**
     * 热门关键词最大数量
     */
    public static final int MAX_HOT_KEYWORDS = 20;
    
    // ========== 搜索配置常量 ==========
    
    /**
     * 内容摘要最大长度
     */
    public static final int CONTENT_SUMMARY_MAX_LENGTH = 200;
    
    /**
     * 个人简介摘要最大长度
     */
    public static final int BIO_SUMMARY_MAX_LENGTH = 100;
    
    /**
     * 热度评分权重 - 点赞
     */
    public static final double POPULARITY_WEIGHT_LIKE = 2.0;
    
    /**
     * 热度评分权重 - 收藏
     */
    public static final double POPULARITY_WEIGHT_COLLECT = 3.0;
    
    /**
     * 热度评分权重 - 评论
     */
    public static final double POPULARITY_WEIGHT_COMMENT = 1.5;
    
    // ========== 私有构造函数 ==========
    
    private SearchConstants() {
        // 防止实例化
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 验证搜索类型是否有效
     * 
     * @param type 搜索类型
     * @return 是否有效
     */
    public static boolean isValidSearchType(String type) {
        return SEARCH_TYPE_POST.equals(type) || 
               SEARCH_TYPE_USER.equals(type) || 
               SEARCH_TYPE_COMMENT.equals(type) ||
               SEARCH_TYPE_ALL.equals(type);
    }
    
    /**
     * 验证排序方式是否有效
     * 
     * @param sortBy 排序方式
     * @return 是否有效
     */
    public static boolean isValidSortBy(String sortBy) {
        return SORT_BY_RELEVANCE.equals(sortBy) || 
               SORT_BY_TIME.equals(sortBy) || 
               SORT_BY_POPULARITY.equals(sortBy);
    }
    
    /**
     * 验证页面大小是否有效
     * 
     * @param size 页面大小
     * @return 是否有效
     */
    public static boolean isValidPageSize(Integer size) {
        return size != null && size > 0 && size <= MAX_PAGE_SIZE;
    }
    
    /**
     * 验证关键词长度是否有效
     * 
     * @param keyword 关键词
     * @return 是否有效
     */
    public static boolean isValidKeywordLength(String keyword) {
        return keyword != null && 
               keyword.length() >= MIN_KEYWORD_LENGTH && 
               keyword.length() <= MAX_KEYWORD_LENGTH;
    }
}