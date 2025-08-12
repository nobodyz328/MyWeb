package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.SearchService;
import com.myweb.website_core.application.service.security.SafeQueryService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import com.myweb.website_core.common.constant.SearchConstants;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.SearchException;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.domain.business.dto.SearchRequestDTO;
import com.myweb.website_core.domain.business.dto.SearchResultDTO;
import com.myweb.website_core.domain.business.vo.PostSearchVO;
import com.myweb.website_core.domain.business.vo.UserSearchVO;
import com.myweb.website_core.domain.business.vo.CommentSearchVO;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索控制器
 * 
 * 提供RESTful搜索API，包括：
 * - 帖子搜索
 * - 用户搜索
 * - 综合搜索
 * - 热门搜索关键词
 * - 搜索缓存管理
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    private final SafeQueryService safeQueryService;
    private final SqlInjectionProtectionService sqlInjectionProtectionService;
    
    /**
     * 搜索帖子（无限滚动分页）
     * 
     * @param keyword 搜索关键词
     * @param lastId 上次加载的最后一个帖子ID（用于游标分页），首次加载传0或不传
     * @param size 每次加载数量，默认20，最大50
     * @param sortBy 排序方式：RELEVANCE（相关性）、TIME（时间）、POPULARITY（热度），默认RELEVANCE
     * @return 帖子搜索结果
     */
    @GetMapping("/posts")
    @Auditable(operation = AuditOperation.SEARCH_OPERATION, resourceType = "SEARCH", description = "搜索帖子")
    public ResponseEntity<ApiResponse<SearchResultDTO<PostSearchVO>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "RELEVANCE") String sortBy) {
        
        log.info("接收到帖子搜索请求，关键词: {}, 上次ID: {}, 大小: {}, 排序: {}", 
                keyword, lastId, size, sortBy);
        
        try {
            // 验证参数
            validateInfiniteScrollParams(keyword, size, sortBy);
            // 检查是否为特殊关键词"all"
            if ("all".equalsIgnoreCase(keyword.trim())) {
                // 搜索所有帖子
                SearchResultDTO<PostSearchVO> result = searchService.searchAllPostsWithCursor(lastId, size, sortBy);
                
                // 应用安全过滤
                result = applySecurityFiltering(result);
                
                // 设置下一页游标信息
                if (!result.getItems().isEmpty()) {
                    PostSearchVO lastPost = result.getItems().get(result.getItems().size() - 1);
                    result.setNextCursor(lastPost.getId().toString());
                    result.setHasMore(result.getItems().size() >= size);
                } else {
                    result.setNextCursor(null);
                    result.setHasMore(false);
                }
                
                log.info("搜索所有帖子完成，结果数: {}, 是否有更多: {}", 
                        result.getItems().size(), result.getHasMore());
                
                return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
            }
            
            // 构建搜索请求
            SearchRequestDTO request = new SearchRequestDTO();
            request.setKeyword(keyword);
            request.setType(SearchConstants.SEARCH_TYPE_POST);
            request.setPage(0); // 无限滚动不使用页码
            request.setSize(size);
            request.setSortBy(sortBy);

            // 执行搜索（带游标）
            SearchResultDTO<PostSearchVO> result = searchService.searchPostsWithCursor(request, lastId);

            // 应用安全过滤
            result = applySecurityFiltering(result);

            // 设置下一页游标信息
            if (!result.getItems().isEmpty()) {
                PostSearchVO lastPost = result.getItems().get(result.getItems().size() - 1);
                result.setNextCursor(lastPost.getId().toString());
                result.setHasMore(result.getItems().size() >= size);
            } else {
                result.setNextCursor(null);
                result.setHasMore(false);
            }
            
            log.info("帖子搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    keyword, result.getItems().size(), result.getHasMore());
            
            return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
            
        } catch (SearchException e) {
            log.warn("帖子搜索失败，关键词: {}, 错误: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("帖子搜索异常，关键词: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索服务暂时不可用"));
        }
    }
    
    /**
     * 搜索用户（无限滚动分页）
     * 
     * @param keyword 搜索关键词
     * @param lastId 上次加载的最后一个用户ID（用于游标分页），首次加载传0或不传
     * @param size 每次加载数量，默认20，最大50
     * @param sortBy 排序方式：RELEVANCE（相关性）、TIME（时间）、POPULARITY（热度），默认RELEVANCE
     * @return 用户搜索结果
     */
    @GetMapping("/users")
    @Auditable(operation = AuditOperation.SEARCH_OPERATION, resourceType = "SEARCH", description = "搜索用户")
    public ResponseEntity<ApiResponse<SearchResultDTO<UserSearchVO>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "RELEVANCE") String sortBy) {
        
        log.info("接收到用户搜索请求，关键词: {}, 上次ID: {}, 大小: {}, 排序: {}", 
                keyword, lastId, size, sortBy);
        
        try {
            // 验证参数
            validateInfiniteScrollParams(keyword, size, sortBy);
            // 检查是否为特殊关键词"all"
            if ("all".equalsIgnoreCase(keyword.trim())) {
                // 搜索所有用户
                SearchResultDTO<UserSearchVO> result = searchService.searchAllUsersWithCursor(lastId, size, sortBy);
                
                // 应用安全过滤
                result = applySecurityFiltering(result);
                
                // 设置下一页游标信息
                if (!result.getItems().isEmpty()) {
                    UserSearchVO lastUser = result.getItems().get(result.getItems().size() - 1);
                    result.setNextCursor(lastUser.getId().toString());
                    result.setHasMore(result.getItems().size() >= size);
                } else {
                    result.setNextCursor(null);
                    result.setHasMore(false);
                }
                
                log.info("搜索所有用户完成，结果数: {}, 是否有更多: {}", 
                        result.getItems().size(), result.getHasMore());
                
                return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
            }
            
            // 构建搜索请求
            SearchRequestDTO request = new SearchRequestDTO();
            request.setKeyword(keyword);
            request.setType(SearchConstants.SEARCH_TYPE_USER);
            request.setPage(0); // 无限滚动不使用页码
            request.setSize(size);
            request.setSortBy(sortBy);
            
            // 执行搜索（带游标）
            SearchResultDTO<UserSearchVO> result = searchService.searchUsersWithCursor(request, lastId);
            
            // 应用安全过滤
            result = applySecurityFiltering(result);
            
            // 设置下一页游标信息
            if (!result.getItems().isEmpty()) {
                UserSearchVO lastUser = result.getItems().get(result.getItems().size() - 1);
                result.setNextCursor(lastUser.getId().toString());
                result.setHasMore(result.getItems().size() >= size);
            } else {
                result.setNextCursor(null);
                result.setHasMore(false);
            }
            
            log.info("用户搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    keyword, result.getItems().size(), result.getHasMore());
            
            return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
            
        } catch (SearchException e) {
            log.warn("用户搜索失败，关键词: {}, 错误: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("用户搜索异常，关键词: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索服务暂时不可用"));
        }
    }
    
    /**
     * 搜索评论（无限滚动分页）
     * 
     * @param keyword 搜索关键词
     * @param lastId 上次加载的最后一个评论ID（用于游标分页），首次加载传0或不传
     * @param size 每次加载数量，默认20，最大50
     * @param sortBy 排序方式：RELEVANCE（相关性）、TIME（时间）、POPULARITY（热度），默认RELEVANCE
     * @return 评论搜索结果
     */
    @GetMapping("/comments")
    @Auditable(operation = AuditOperation.SEARCH_OPERATION, resourceType = "SEARCH", description = "搜索评论")
    public ResponseEntity<ApiResponse<SearchResultDTO<CommentSearchVO>>> searchComments(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "RELEVANCE") String sortBy) {
        
        log.info("接收到评论搜索请求，关键词: {}, 上次ID: {}, 大小: {}, 排序: {}", 
                keyword, lastId, size, sortBy);
        
        try {
            // 验证参数
            validateInfiniteScrollParams(keyword, size, sortBy);
            
            // 构建搜索请求
            SearchRequestDTO request = new SearchRequestDTO();
            request.setKeyword(keyword);
            request.setType(SearchConstants.SEARCH_TYPE_COMMENT);
            request.setPage(0); // 无限滚动不使用页码
            request.setSize(size);
            request.setSortBy(sortBy);
            
            // 执行搜索（带游标）
            SearchResultDTO<CommentSearchVO> result = searchService.searchCommentsWithCursor(request, lastId);
            
            // 应用安全过滤
            result = applySecurityFiltering(result);
            
            // 设置下一页游标信息
            if (!result.getItems().isEmpty()) {
                CommentSearchVO lastComment = result.getItems().get(result.getItems().size() - 1);
                result.setNextCursor(lastComment.getId().toString());
                result.setHasMore(result.getItems().size() >= size);
            } else {
                result.setNextCursor(null);
                result.setHasMore(false);
            }
            
            log.info("评论搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    keyword, result.getItems().size(), result.getHasMore());
            
            return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
            
        } catch (SearchException e) {
            log.warn("评论搜索失败，关键词: {}, 错误: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("评论搜索异常，关键词: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索服务暂时不可用"));
        }
    }
    
    /**
     * 综合搜索
     * 
     * @param keyword 搜索关键词
     * @param lastPostId 上次加载的最后一个帖子ID，首次加载传0或不传
     * @param lastUserId 上次加载的最后一个用户ID，首次加载传0或不传
     * @param size 每次加载数量，默认20，最大50
     * @param sortBy 排序方式：RELEVANCE（相关性）、TIME（时间）、POPULARITY（热度），默认RELEVANCE
     * @return 综合搜索结果
     */
    @GetMapping("/all")
    @Auditable(operation = AuditOperation.SEARCH_OPERATION, resourceType = "SEARCH", description = "综合搜索")
    public ResponseEntity<ApiResponse<SearchResultDTO<Object>>> searchAll(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Long lastPostId,
            @RequestParam(defaultValue = "0") Long lastUserId,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "RELEVANCE") String sortBy) {
        
        log.info("接收到综合搜索请求，关键词: {}, 帖子上次ID: {}, 用户上次ID: {}, 大小: {}, 排序: {}", 
                keyword, lastPostId, lastUserId, size, sortBy);
        
        try {
            // 验证参数
            validateInfiniteScrollParams(keyword, size, sortBy);
            // 检查是否为特殊关键词"all"
            if ("all".equalsIgnoreCase(keyword.trim())) {
                // 搜索所有内容（帖子和用户）
                SearchResultDTO<Object> result = searchService.searchAllContentWithCursor(lastPostId, lastUserId, size, sortBy);
                
                // 应用安全过滤
                result = applySecurityFiltering(result);
                
                log.info("搜索所有内容完成，结果数: {}, 是否有更多: {}", 
                        result.getItems().size(), result.getHasMore());
                
                return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
            }
            
            // 构建搜索请求
            SearchRequestDTO request = new SearchRequestDTO();
            request.setKeyword(keyword);
            request.setType(SearchConstants.SEARCH_TYPE_ALL);
            request.setPage(0); // 无限滚动不使用页码
            request.setSize(size);
            request.setSortBy(sortBy);
            
            // 执行综合搜索（带游标）
            SearchResultDTO<Object> result = searchService.searchAllWithCursor(request, lastPostId, lastUserId);
            
            // 应用安全过滤
            result = applySecurityFiltering(result);
            
            log.info("综合搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    keyword, result.getItems().size(), result.getHasMore());
            
            return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
            
        } catch (SearchException e) {
            log.warn("综合搜索失败，关键词: {}, 错误: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("综合搜索异常，关键词: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索服务暂时不可用"));
        }
    }
    
    /**
     * 使用POST方式进行高级搜索
     * 
     * @param request 搜索请求对象
     * @param bindingResult 验证结果
     * @return 搜索结果
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> advancedSearch(
            @Valid @RequestBody SearchRequestDTO request,
            BindingResult bindingResult) {
        
        log.info("接收到高级搜索请求: {}", request);
        
        // 检查验证结果
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("请求参数验证失败");
            
            log.warn("高级搜索请求参数验证失败: {}", errorMessage);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("请求参数验证失败: " + errorMessage));
        }
        
        try {
            // 验证请求参数
            validateSearchRequest(request);
            
            // 根据搜索类型执行相应的搜索
            switch (request.getType()) {
                case SearchConstants.SEARCH_TYPE_POST:
                    SearchResultDTO<PostSearchVO> postResult = searchService.searchPosts(request);
                    // 应用安全过滤
                    postResult = applySecurityFiltering(postResult);
                    log.info("高级搜索完成，关键词: {}, 类型: {}, 结果数: {}, 总数: {}", 
                            request.getKeyword(), request.getType(), 
                            postResult.getItems().size(), postResult.getTotal());
                    return ResponseEntity.ok(ApiResponse.success("搜索成功", postResult));
                    
                case SearchConstants.SEARCH_TYPE_USER:
                    SearchResultDTO<UserSearchVO> userResult = searchService.searchUsers(request);
                    // 应用安全过滤
                    userResult = applySecurityFiltering(userResult);
                    log.info("高级搜索完成，关键词: {}, 类型: {}, 结果数: {}, 总数: {}", 
                            request.getKeyword(), request.getType(), 
                            userResult.getItems().size(), userResult.getTotal());
                    return ResponseEntity.ok(ApiResponse.success("搜索成功", userResult));
                    
                case SearchConstants.SEARCH_TYPE_COMMENT:
                    SearchResultDTO<CommentSearchVO> commentResult = searchService.searchComments(request);
                    // 应用安全过滤
                    commentResult = applySecurityFiltering(commentResult);
                    log.info("高级搜索完成，关键词: {}, 类型: {}, 结果数: {}, 总数: {}", 
                            request.getKeyword(), request.getType(), 
                            commentResult.getItems().size(), commentResult.getTotal());
                    return ResponseEntity.ok(ApiResponse.success("搜索成功", commentResult));
                    
                case SearchConstants.SEARCH_TYPE_ALL:
                default:
                    SearchResultDTO<Object> allResult = searchService.searchAll(request);
                    // 应用安全过滤
                    allResult = applySecurityFiltering(allResult);
                    log.info("高级搜索完成，关键词: {}, 类型: {}, 结果数: {}, 总数: {}", 
                            request.getKeyword(), request.getType(), 
                            allResult.getItems().size(), allResult.getTotal());
                    return ResponseEntity.ok(ApiResponse.success("搜索成功", allResult));
            }
            
        } catch (SearchException e) {
            log.warn("高级搜索失败，请求: {}, 错误: {}", request, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("高级搜索异常，请求: {}", request, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜索服务暂时不可用"));
        }
    }
    
    /**
     * 获取热门搜索关键词
     * 
     * @param limit 限制数量，默认10，最大20
     * @return 热门搜索关键词列表
     */
    @GetMapping("/hot-keywords")
    public ResponseEntity<ApiResponse<List<String>>> getHotSearchKeywords(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("接收到获取热门搜索关键词请求，限制数量: {}", limit);
        
        try {
            // 验证限制数量
            if (limit <= 0 || limit > SearchConstants.MAX_HOT_KEYWORDS) {
                throw SearchException.invalidPagination(0, limit);
            }
            
            // 获取热门关键词
            List<String> hotKeywords = searchService.getHotSearchKeywords(limit);
            
            log.info("获取热门搜索关键词完成，数量: {}", hotKeywords.size());
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", hotKeywords));
            
        } catch (SearchException e) {
            log.warn("获取热门搜索关键词失败，错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("获取热门搜索关键词异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("服务暂时不可用"));
        }
    }
    
    /**
     * 清除搜索缓存
     * 
     * @param keyword 关键词（可选，为空则清除所有搜索缓存）
     * @return 操作结果
     */
    @DeleteMapping("/cache")
    @Auditable(operation = AuditOperation.CACHE_CLEANUP, resourceType = "SEARCH", description = "清除搜索缓存")
    public ResponseEntity<ApiResponse<Void>> clearSearchCache(
            @RequestParam(required = false) String keyword) {
        
        log.info("接收到清除搜索缓存请求，关键词: {}", keyword);
        
        try {
            // 清除缓存
            searchService.clearSearchCache(keyword);
            
            String message = keyword != null ? 
                    "清除关键词 '" + keyword + "' 的搜索缓存成功" : 
                    "清除所有搜索缓存成功";
            
            log.info("清除搜索缓存完成，关键词: {}", keyword);
            
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            log.error("清除搜索缓存异常，关键词: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("清除缓存失败"));
        }
    }
    
    /**
     * 强制清除所有搜索缓存（管理员功能）
     * 
     * @return 操作结果
     */
    @DeleteMapping("/cache/all")
    @Auditable(operation = AuditOperation.CACHE_CLEANUP, resourceType = "SEARCH", description = "强制清除所有搜索缓存")
    public ResponseEntity<ApiResponse<Void>> forceCleanAllCache() {
        
        log.info("接收到强制清除所有搜索缓存请求");
        
        try {
            // 强制清除所有搜索相关缓存
            searchService.clearSearchCache(null);
            
            log.info("强制清除所有搜索缓存完成");
            
            return ResponseEntity.ok(ApiResponse.success("强制清除所有搜索缓存成功"));
            
        } catch (Exception e) {
            log.error("强制清除所有搜索缓存异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("强制清除缓存失败"));
        }
    }
    
    /**
     * 验证无限滚动搜索参数（集成安全验证）
     * 
     * @param keyword 搜索关键词
     * @param size 每次加载数量
     * @param sortBy 排序方式
     * @throws SearchException 验证失败时抛出异常
     */
    private void validateInfiniteScrollParams(String keyword, Integer size, String sortBy) throws SearchException {
        // 验证关键词
        if (keyword == null || keyword.trim().isEmpty()) {
            throw SearchException.invalidKeyword("关键词不能为空");
        }
        
        String trimmedKeyword = keyword.trim();
        
        // 安全验证：检查SQL注入
        try {
            sqlInjectionProtectionService.validateAndSanitizeInput(trimmedKeyword, "SEARCH", "keyword");
        } catch (ValidationException e) {
            log.warn("搜索关键词安全验证失败: {}", e.getMessage());
            throw SearchException.invalidKeyword("搜索关键词包含非法字符");
        }
        
        if (!SearchConstants.isValidKeywordLength(trimmedKeyword)) {
            if (trimmedKeyword.length() < SearchConstants.MIN_KEYWORD_LENGTH) {
                throw SearchException.keywordTooShort(trimmedKeyword, SearchConstants.MIN_KEYWORD_LENGTH);
            } else {
                throw SearchException.keywordTooLong(trimmedKeyword, SearchConstants.MAX_KEYWORD_LENGTH);
            }
        }
        
        // 安全验证：验证排序字段
        if (!SearchConstants.isValidSortBy(sortBy)) {
            throw SearchException.invalidSortBy(sortBy);
        }
        
        // 使用SafeQueryService验证排序字段安全性
        try {
            sqlInjectionProtectionService.validateAndSanitizeInput(sortBy, "SORT", "sortBy");
        } catch (ValidationException e) {
            log.warn("排序字段安全验证失败: {}", e.getMessage());
            throw SearchException.invalidSortBy("排序字段包含非法字符");
        }
        
        // 验证加载数量（无限滚动模式下限制更严格）
        if (size <= 0 || size > 50) { // 无限滚动最大50条
            throw SearchException.invalidPagination(0, size);
        }
    }
    
    /**
     * 验证搜索请求参数（用于POST高级搜索，集成安全验证）
     * 
     * @param request 搜索请求
     * @throws SearchException 验证失败时抛出异常
     */
    private void validateSearchRequest(SearchRequestDTO request) throws SearchException {
        // 验证关键词
        if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            throw SearchException.invalidKeyword("关键词不能为空");
        }
        
        String keyword = request.getKeyword().trim();
        
        // 安全验证：检查SQL注入
        try {
            sqlInjectionProtectionService.validateAndSanitizeInput(keyword, "SEARCH", "keyword");
        } catch (ValidationException e) {
            log.warn("高级搜索关键词安全验证失败: {}", e.getMessage());
            throw SearchException.invalidKeyword("搜索关键词包含非法字符");
        }
        
        if (!SearchConstants.isValidKeywordLength(keyword)) {
            if (keyword.length() < SearchConstants.MIN_KEYWORD_LENGTH) {
                throw SearchException.keywordTooShort(keyword, SearchConstants.MIN_KEYWORD_LENGTH);
            } else {
                throw SearchException.keywordTooLong(keyword, SearchConstants.MAX_KEYWORD_LENGTH);
            }
        }
        
        // 验证搜索类型
        if (!request.isValidType()) {
            throw SearchException.invalidSearchType(request.getType());
        }
        
        // 安全验证：验证排序方式
        if (!request.isValidSortBy()) {
            throw SearchException.invalidSortBy(request.getSortBy());
        }
        
        try {
            sqlInjectionProtectionService.validateAndSanitizeInput(request.getSortBy(), "SORT", "sortBy");
        } catch (ValidationException e) {
            log.warn("高级搜索排序字段安全验证失败: {}", e.getMessage());
            throw SearchException.invalidSortBy("排序字段包含非法字符");
        }
        
        // 验证分页参数
        if (request.getPage() < 0) {
            throw SearchException.invalidPagination(request.getPage(), request.getSize());
        }
        
        if (!SearchConstants.isValidPageSize(request.getSize())) {
            throw SearchException.invalidPagination(request.getPage(), request.getSize());
        }
    }
    
    /**
     * 安全过滤搜索结果
     * 
     * @param results 原始搜索结果
     * @param <T> 结果类型
     * @return 过滤后的安全结果
     */
    private <T> SearchResultDTO<T> applySecurityFiltering(SearchResultDTO<T> results) {
        if (results == null || results.getItems() == null) {
            return results;
        }
        
        // 对搜索结果进行安全过滤
        List<T> filteredItems = results.getItems().stream()
                .filter(this::isItemSecure)
                .toList();
        
        // 创建新的结果对象
        SearchResultDTO<T> filteredResults = new SearchResultDTO<>();
        filteredResults.setItems(filteredItems);
        filteredResults.setTotal(results.getTotal());
        filteredResults.setPage(results.getPage());
        filteredResults.setSize(results.getSize());
        filteredResults.setKeyword(results.getKeyword());
        filteredResults.setType(results.getType());
        filteredResults.setSortBy(results.getSortBy());
        filteredResults.setNextCursor(results.getNextCursor());
        filteredResults.setHasMore(results.getHasMore());
        
        log.debug("安全过滤完成，原始结果数: {}, 过滤后结果数: {}", 
                results.getItems().size(), filteredItems.size());
        
        return filteredResults;
    }
    
    /**
     * 检查搜索结果项是否安全
     * 
     * @param item 搜索结果项
     * @return 是否安全
     */
    private boolean isItemSecure(Object item) {
        if (item == null) {
            return false;
        }
        
        try {
            // 对于帖子搜索结果，检查标题和内容摘要
            if (item instanceof PostSearchVO) {
                PostSearchVO post = (PostSearchVO) item;
                
                // 检查标题安全性
                if (post.getTitle() != null && 
                    sqlInjectionProtectionService.detectSqlInjection(post.getTitle(), "CONTENT")) {
                    log.warn("检测到不安全的帖子标题，帖子ID: {}", post.getId());
                    return false;
                }
                
                // 检查内容摘要安全性
                if (post.getContentSummary() != null && 
                    sqlInjectionProtectionService.detectSqlInjection(post.getContentSummary(), "CONTENT")) {
                    log.warn("检测到不安全的帖子内容，帖子ID: {}", post.getId());
                    return false;
                }
            }
            
            // 对于用户搜索结果，检查用户名和简介
            if (item instanceof UserSearchVO) {
                UserSearchVO user = (UserSearchVO) item;
                
                // 检查用户名安全性
                if (user.getUsername() != null && 
                    sqlInjectionProtectionService.detectSqlInjection(user.getUsername(), "CONTENT")) {
                    log.warn("检测到不安全的用户名，用户ID: {}", user.getId());
                    return false;
                }
                
                // 检查个人简介安全性
                if (user.getBio() != null && 
                    sqlInjectionProtectionService.detectSqlInjection(user.getBio(), "CONTENT")) {
                    log.warn("检测到不安全的用户简介，用户ID: {}", user.getId());
                    return false;
                }
            }
            
            // 对于评论搜索结果，检查评论内容
            if (item instanceof CommentSearchVO) {
                CommentSearchVO comment = (CommentSearchVO) item;
                
                // 检查评论内容安全性
                if (comment.getContentSummary() != null && 
                    sqlInjectionProtectionService.detectSqlInjection(comment.getContentSummary(), "CONTENT")) {
                    log.warn("检测到不安全的评论内容，评论ID: {}", comment.getId());
                    return false;
                }
                
                // 检查作者用户名安全性
                if (comment.getAuthorUsername() != null && 
                    sqlInjectionProtectionService.detectSqlInjection(comment.getAuthorUsername(), "CONTENT")) {
                    log.warn("检测到不安全的评论作者用户名，评论ID: {}", comment.getId());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("安全检查过程中发生异常", e);
            return false; // 出现异常时采用保守策略，过滤掉该项
        }
    }
}