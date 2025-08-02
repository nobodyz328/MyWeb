package com.myweb.website_core.common.util;

import com.myweb.website_core.common.constant.SearchConstants;
import com.myweb.website_core.common.exception.SearchException;
import com.myweb.website_core.domain.dto.SearchRequestDTO;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 搜索工具类
 * 
 * 提供搜索功能相关的工具方法，包括：
 * - 关键词预处理和验证
 * - 搜索参数验证
 * - 缓存键生成
 * - 搜索结果处理
 */
public class SearchUtils {
    
    // ========== 正则表达式模式 ==========
    
    /**
     * 特殊字符模式（用于清理关键词）
     */
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[<>\"'%;()&+\\-\\\\]");
    
    /**
     * 多个空格模式（用于规范化空格）
     */
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    
    /**
     * SQL注入风险字符模式
     */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)");
    
    // ========== 私有构造函数 ==========
    
    private SearchUtils() {
        // 防止实例化
    }
    
    // ========== 关键词处理方法 ==========
    
    /**
     * 预处理搜索关键词
     * 
     * @param keyword 原始关键词
     * @return 处理后的关键词
     * @throws SearchException 如果关键词无效
     */
    public static String preprocessKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw SearchException.invalidKeyword(keyword);
        }
        
        // 去除首尾空格
        String processed = keyword.trim();
        
        // 验证长度
        if (processed.length() < SearchConstants.MIN_KEYWORD_LENGTH) {
            throw SearchException.keywordTooShort(processed, SearchConstants.MIN_KEYWORD_LENGTH);
        }
        
        if (processed.length() > SearchConstants.MAX_KEYWORD_LENGTH) {
            throw SearchException.keywordTooLong(processed, SearchConstants.MAX_KEYWORD_LENGTH);
        }
        
        // 检查SQL注入风险
        if (SQL_INJECTION_PATTERN.matcher(processed).find()) {
            throw SearchException.invalidKeyword("关键词包含不安全字符");
        }
        
        // 去除特殊字符
        processed = SPECIAL_CHARS_PATTERN.matcher(processed).replaceAll("");
        
        // 规范化空格
        processed = MULTIPLE_SPACES_PATTERN.matcher(processed).replaceAll(" ");
        
        // 再次验证处理后的关键词
        if (!StringUtils.hasText(processed)) {
            throw SearchException.invalidKeyword("关键词处理后为空");
        }
        
        return processed;
    }
    
    /**
     * 验证搜索关键词
     * 
     * @param keyword 关键词
     * @return 是否有效
     */
    public static boolean isValidKeyword(String keyword) {
        try {
            preprocessKeyword(keyword);
            return true;
        } catch (SearchException e) {
            return false;
        }
    }
    
    /**
     * 生成搜索关键词的缓存键版本（小写，用于缓存）
     * 
     * @param keyword 关键词
     * @return 缓存键版本的关键词
     */
    public static String toCacheKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.toLowerCase().trim();
    }
    
    // ========== 搜索请求验证方法 ==========
    
    /**
     * 验证搜索请求
     * 
     * @param request 搜索请求
     * @throws SearchException 如果请求无效
     */
    public static void validateSearchRequest(SearchRequestDTO request) {
        if (request == null) {
            throw new SearchException("搜索请求不能为空");
        }
        
        // 验证关键词
        preprocessKeyword(request.getKeyword());
        
        // 验证搜索类型
        if (!SearchConstants.isValidSearchType(request.getType())) {
            throw SearchException.invalidSearchType(request.getType());
        }
        
        // 验证排序方式
        if (!SearchConstants.isValidSortBy(request.getSortBy())) {
            throw SearchException.invalidSortBy(request.getSortBy());
        }
        
        // 验证分页参数
        if (request.getPage() < 0) {
            throw SearchException.invalidPagination(request.getPage(), request.getSize());
        }
        
        if (!SearchConstants.isValidPageSize(request.getSize())) {
            throw SearchException.invalidPagination(request.getPage(), request.getSize());
        }
    }
    
    // ========== 缓存键生成方法 ==========
    
    /**
     * 构建搜索缓存键
     * 
     * @param type 搜索类型
     * @param request 搜索请求
     * @return 缓存键
     */
    public static String buildSearchCacheKey(String type, SearchRequestDTO request) {
        return SearchConstants.SEARCH_CACHE_PREFIX + type + ":" + 
               toCacheKeyword(request.getKeyword()) + ":" +
               request.getPage() + ":" +
               request.getSize() + ":" +
               request.getSortBy();
    }
    
    /**
     * 构建帖子搜索缓存键
     * 
     * @param request 搜索请求
     * @return 缓存键
     */
    public static String buildPostSearchCacheKey(SearchRequestDTO request) {
        return buildSearchCacheKey("post", request);
    }
    
    /**
     * 构建用户搜索缓存键
     * 
     * @param request 搜索请求
     * @return 缓存键
     */
    public static String buildUserSearchCacheKey(SearchRequestDTO request) {
        return buildSearchCacheKey("user", request);
    }
    
    /**
     * 构建综合搜索缓存键
     * 
     * @param request 搜索请求
     * @return 缓存键
     */
    public static String buildAllSearchCacheKey(SearchRequestDTO request) {
        return buildSearchCacheKey("all", request);
    }
    
    /**
     * 构建搜索统计缓存键
     * 
     * @param keyword 关键词
     * @return 缓存键
     */
    public static String buildSearchStatsCacheKey(String keyword) {
        return SearchConstants.SEARCH_STATS_PREFIX + toCacheKeyword(keyword);
    }
    
    // ========== 内容处理方法 ==========
    
    /**
     * 截取内容摘要
     * 
     * @param content 原始内容
     * @param maxLength 最大长度
     * @return 内容摘要
     */
    public static String truncateContent(String content, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        
        if (content.length() <= maxLength) {
            return content;
        }
        
        return content.substring(0, maxLength) + "...";
    }
    
    /**
     * 截取内容摘要（使用默认长度）
     * 
     * @param content 原始内容
     * @return 内容摘要
     */
    public static String truncateContent(String content) {
        return truncateContent(content, SearchConstants.CONTENT_SUMMARY_MAX_LENGTH);
    }
    
    /**
     * 截取个人简介摘要
     * 
     * @param bio 原始个人简介
     * @return 个人简介摘要
     */
    public static String truncateBio(String bio) {
        return truncateContent(bio, SearchConstants.BIO_SUMMARY_MAX_LENGTH);
    }
    
    /**
     * 脱敏邮箱地址
     * 
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || email.length() < 6) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 3) {
            return "***" + email.substring(atIndex);
        } else {
            return email.substring(0, 3) + "***" + email.substring(atIndex);
        }
    }
    
    // ========== 评分计算方法 ==========
    
    /**
     * 计算热度评分
     * 
     * @param likeCount 点赞数
     * @param collectCount 收藏数
     * @param commentCount 评论数
     * @return 热度评分
     */
    public static double calculatePopularityScore(Integer likeCount, Integer collectCount, Integer commentCount) {
        int likes = likeCount != null ? likeCount : 0;
        int collects = collectCount != null ? collectCount : 0;
        int comments = commentCount != null ? commentCount : 0;
        
        return likes * SearchConstants.POPULARITY_WEIGHT_LIKE + 
               collects * SearchConstants.POPULARITY_WEIGHT_COLLECT + 
               comments * SearchConstants.POPULARITY_WEIGHT_COMMENT;
    }
    
    /**
     * 计算相关性评分（简单实现，基于关键词匹配度）
     * 
     * @param keyword 搜索关键词
     * @param title 标题
     * @param content 内容
     * @return 相关性评分
     */
    public static double calculateRelevanceScore(String keyword, String title, String content) {
        if (!StringUtils.hasText(keyword)) {
            return 0.0;
        }
        
        double score = 0.0;
        String lowerKeyword = keyword.toLowerCase();
        
        // 标题匹配权重更高
        if (StringUtils.hasText(title)) {
            String lowerTitle = title.toLowerCase();
            if (lowerTitle.contains(lowerKeyword)) {
                score += 10.0;
                // 完全匹配额外加分
                if (lowerTitle.equals(lowerKeyword)) {
                    score += 20.0;
                }
            }
        }
        
        // 内容匹配
        if (StringUtils.hasText(content)) {
            String lowerContent = content.toLowerCase();
            if (lowerContent.contains(lowerKeyword)) {
                score += 5.0;
                // 计算匹配次数
                int count = 0;
                int index = 0;
                while ((index = lowerContent.indexOf(lowerKeyword, index)) != -1) {
                    count++;
                    index += lowerKeyword.length();
                }
                score += count * 2.0;
            }
        }
        
        return score;
    }
    
    // ========== 分页计算方法 ==========
    
    /**
     * 计算总页数
     * 
     * @param total 总记录数
     * @param size 每页大小
     * @return 总页数
     */
    public static int calculateTotalPages(long total, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }
    
    /**
     * 计算偏移量
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 偏移量
     */
    public static int calculateOffset(int page, int size) {
        return Math.max(0, page) * Math.max(1, size);
    }
    
    /**
     * 验证分页参数是否会导致过多结果
     * 
     * @param page 页码
     * @param size 每页大小
     * @throws SearchException 如果会导致过多结果
     */
    public static void validateResultLimit(int page, int size) {
        int offset = calculateOffset(page, size);
        if (offset >= SearchConstants.MAX_SEARCH_RESULTS) {
            throw SearchException.tooManyResults(offset, SearchConstants.MAX_SEARCH_RESULTS);
        }
    }
}