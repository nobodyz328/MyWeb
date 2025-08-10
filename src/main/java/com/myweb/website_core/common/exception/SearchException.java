package com.myweb.website_core.common.exception;

/**
 * 搜索相关异常
 * 
 * 用于处理搜索功能中的各种异常情况，包括：
 * - 无效的搜索参数
 * - 搜索超时
 * - 缓存操作失败
 * - 搜索服务不可用
 */
public class SearchException extends RuntimeException {
    
    // ========== 错误代码常量 ==========
    
    /**
     * 无效的搜索关键词
     */
    public static final String INVALID_KEYWORD = "无效的搜索关键词";
    
    /**
     * 搜索超时
     */
    public static final String SEARCH_TIMEOUT = "搜索超时";
    
    /**
     * 无效的搜索类型
     */
    public static final String INVALID_SEARCH_TYPE = "无效的搜索类型";
    
    /**
     * 无效的排序方式
     */
    public static final String INVALID_SORT_BY = "无效的排序方式";
    
    /**
     * 无效的分页参数
     */
    public static final String INVALID_PAGINATION = "无效的分页参数";
    
    /**
     * 缓存操作失败
     */
    public static final String CACHE_OPERATION_FAILED = "缓存操作失败";
    
    /**
     * 搜索服务不可用
     */
    public static final String SERVICE_UNAVAILABLE = "搜索服务不可用";
    
    /**
     * 关键词长度超限
     */
    public static final String KEYWORD_TOO_LONG = "关键词长度超限";

    /**
     * 关键词长度不足
     */
    public static final String KEYWORD_TOO_SHORT = "关键词长度不足";

    /**
     * 搜索结果过多
     */
    public static final String TOO_MANY_RESULTS = "搜索结果过多";

    // ========== 属性 ==========

    /**
     * 错误代码
     */
    private final String errorType;

    // ========== 构造函数 ==========

    /**
     * 构造函数
     *
     * @param errorType
     *错误代码
     * @param message 错误消息
     */
    public SearchException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * 构造函数
     *
     * @param errorType
     *错误代码
     * @param message 错误消息
     * @param cause 原因异常
     */
    public SearchException(String errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    /**
     * 构造函数（使用默认错误代码）
     *
     * @param message 错误消息
     */
    public SearchException(String message) {
        super(message);
        this.errorType = "SEARCH_UNKNOWN";
    }

    /**
     * 构造函数（使用默认错误代码）
     *
     * @param message 错误消息
     * @param cause 原因异常
     */
    public SearchException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = "SEARCH_UNKNOWN";
    }

    // ========== Getter方法 ==========
    
    /**
     * 获取错误代码
     * 
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorType;
    }
    
    // ========== 静态工厂方法 ==========
    
    /**
     * 创建无效关键词异常
     * 
     * @param keyword 关键词
     * @return 搜索异常
     */
    public static SearchException invalidKeyword(String keyword) {
        return new SearchException(INVALID_KEYWORD,  keyword);
    }
    
    /**
     * 创建搜索超时异常
     * 
     * @param keyword 关键词
     * @return 搜索异常
     */
    public static SearchException searchTimeout(String keyword) {
        return new SearchException(SEARCH_TIMEOUT, keyword);
    }
    
    /**
     * 创建无效搜索类型异常
     * 
     * @param type 搜索类型
     * @return 搜索异常
     */
    public static SearchException invalidSearchType(String type) {
        return new SearchException(INVALID_SEARCH_TYPE, type);
    }
    
    /**
     * 创建无效排序方式异常
     * 
     * @param sortBy 排序方式
     * @return 搜索异常
     */
    public static SearchException invalidSortBy(String sortBy) {
        return new SearchException(INVALID_SORT_BY, sortBy);
    }
    
    /**
     * 创建无效分页参数异常
     * 
     * @param page 页码
     * @param size 页面大小
     * @return 搜索异常
     */
    public static SearchException invalidPagination(Integer page, Integer size) {
        return new SearchException(INVALID_PAGINATION, 
                String.format("页码: %d, 大小: %d", page, size));
    }
    
    /**
     * 创建缓存操作失败异常
     * 
     * @param operation 操作类型
     * @param cause 原因异常
     * @return 搜索异常
     */
    public static SearchException cacheOperationFailed(String operation, Throwable cause) {
        return new SearchException(CACHE_OPERATION_FAILED, 
                operation, cause);
    }
    
    /**
     * 创建服务不可用异常
     * 
     * @return 搜索异常
     */
    public static SearchException serviceUnavailable() {
        return new SearchException(SERVICE_UNAVAILABLE);
    }
    
    /**
     * 创建关键词过长异常
     * 
     * @param keyword 关键词
     * @param maxLength 最大长度
     * @return 搜索异常
     */
    public static SearchException keywordTooLong(String keyword, int maxLength) {
        return new SearchException(KEYWORD_TOO_LONG, 
                String.format("当前: %d, 最大: %d", keyword.length(), maxLength));
    }
    
    /**
     * 创建关键词过短异常
     * 
     * @param keyword 关键词
     * @param minLength 最小长度
     * @return 搜索异常
     */
    public static SearchException keywordTooShort(String keyword, int minLength) {
        return new SearchException(KEYWORD_TOO_SHORT, 
                String.format("当前: %d, 最小: %d", keyword.length(), minLength));
    }
    
    /**
     * 创建搜索结果过多异常
     * 
     * @param count 结果数量
     * @param maxCount 最大数量
     * @return 搜索异常
     */
    public static SearchException tooManyResults(long count, int maxCount) {
        return new SearchException(TOO_MANY_RESULTS, 
                String.format("当前: %d, 最大: %d", count, maxCount));
    }
    
    // ========== toString方法 ==========
    
    @Override
    public String toString() {
        return String.format("SearchException{errorType='%s', message='%s'}",
                errorType, getMessage());
    }
}