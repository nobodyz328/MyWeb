package com.myweb.website_core.application.service.business;

import com.myweb.website_core.common.constant.SearchConstants;
import com.myweb.website_core.common.util.SearchUtils;
import com.myweb.website_core.domain.business.dto.SearchRequestDTO;
import com.myweb.website_core.domain.business.dto.SearchResultDTO;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.domain.business.vo.PostSearchVO;
import com.myweb.website_core.domain.business.vo.UserSearchVO;
import com.myweb.website_core.domain.business.vo.CommentSearchVO;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepositoryCustom;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepositoryCustom;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索服务类
 * 
 * 实现帖子和用户搜索业务逻辑，包括：
 * - 搜索关键词预处理和验证
 * - 搜索结果缓存机制
 * - 分页和排序功能
 * - 多类型搜索支持
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    
    private final PostRepositoryCustom postRepository;
    private final UserRepositoryCustom userRepository;
    private final CommentRepositoryCustom commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    

    
    /**
     * 综合搜索 - 同时搜索帖子、用户和评论
     * 
     * @param request 搜索请求
     * @return 搜索结果
     */
    public SearchResultDTO<Object> searchAll(SearchRequestDTO request) {
        log.info("执行综合搜索，关键词: {}, 页码: {}, 大小: {}", 
                request.getKeyword(), request.getPage(), request.getSize());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 尝试从缓存获取结果
        String cacheKey = SearchUtils.buildAllSearchCacheKey(request);
        SearchResultDTO<Object> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取搜索结果，关键词: {}", processedKeyword);
            return cachedResult;
        }
        
        // 分别搜索帖子、用户和评论
        SearchRequestDTO postRequest = createSubRequest(request, SearchConstants.SEARCH_TYPE_POST);
        SearchRequestDTO userRequest = createSubRequest(request, SearchConstants.SEARCH_TYPE_USER);
        SearchRequestDTO commentRequest = createSubRequest(request, SearchConstants.SEARCH_TYPE_COMMENT);
        
        SearchResultDTO<PostSearchVO> postResults = searchPosts(postRequest);
        SearchResultDTO<UserSearchVO> userResults = searchUsers(userRequest);
        SearchResultDTO<CommentSearchVO> commentResults = searchComments(commentRequest);
        
        // 合并结果
        List<Object> allResults = new ArrayList<>();
        allResults.addAll(postResults.getItems());
        allResults.addAll(userResults.getItems());
        allResults.addAll(commentResults.getItems());
        
        // 构建综合搜索结果
        SearchResultDTO<Object> result = new SearchResultDTO<>();
        result.setItems(allResults);
        result.setTotal(postResults.getTotal() + userResults.getTotal() + commentResults.getTotal());
        result.setPage(request.getPage());
        result.setSize(request.getSize());
        result.setKeyword(processedKeyword);
        result.setType(request.getType());
        result.setSortBy(request.getSortBy());
        
        // 缓存结果
        cacheSearchResult(cacheKey, result);
        
        log.info("综合搜索完成，关键词: {}, 帖子数: {}, 用户数: {}, 评论数: {}", 
                processedKeyword, postResults.getItems().size(), userResults.getItems().size(), commentResults.getItems().size());
        
        return result;
    }
    
    /**
     * 搜索帖子
     * 
     * @param request 搜索请求
     * @return 帖子搜索结果
     */
    public SearchResultDTO<PostSearchVO> searchPosts(SearchRequestDTO request) {
        log.info("执行帖子搜索，关键词: {}, 页码: {}, 大小: {}, 排序: {}", 
                request.getKeyword(), request.getPage(), request.getSize(), request.getSortBy());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 尝试从缓存获取结果
        String cacheKey = SearchUtils.buildPostSearchCacheKey(request);
        SearchResultDTO<PostSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取帖子搜索结果，关键词: {}", processedKeyword);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全搜索
            String sortField = mapSortByToField(request.getSortBy());
            String sortDirection = getSortDirection(request.getSortBy());
            
            Page<Post> postPage = postRepository.findPostsWithSafeSearch(
                    processedKeyword, 
                    sortField, 
                    sortDirection, 
                    request.getPage(), 
                    request.getSize()
            );
            
            // 转换为VO
            List<PostSearchVO> posts = postPage.getContent().stream()
                    .map(this::convertToPostSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            posts.forEach(this::processPostSearchResult);
            
            // 构建结果
            SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
            result.setItems(posts);
            result.setTotal(postPage.getTotalElements());
            result.setPage(request.getPage());
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("帖子搜索完成，关键词: {}, 结果数: {}, 总数: {}", 
                    processedKeyword, posts.size(), postPage.getTotalElements());
            
            return result;
            
        } catch (Exception e) {
            log.error("帖子搜索失败，关键词: {}", processedKeyword, e);
            return SearchResultDTO.empty(request);
        }
    }
    
    /**
     * 搜索用户
     * 
     * @param request 搜索请求
     * @return 用户搜索结果
     */
    public SearchResultDTO<UserSearchVO> searchUsers(SearchRequestDTO request) {
        log.info("执行用户搜索，关键词: {}, 页码: {}, 大小: {}, 排序: {}", 
                request.getKeyword(), request.getPage(), request.getSize(), request.getSortBy());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 尝试从缓存获取结果
        String cacheKey = SearchUtils.buildUserSearchCacheKey(request);
        SearchResultDTO<UserSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取用户搜索结果，关键词: {}", processedKeyword);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全搜索
            String sortField = mapSortByToField(request.getSortBy());
            String sortDirection = getSortDirection(request.getSortBy());
            
            Page<User> userPage = userRepository.findUsersWithSafeSearch(
                    processedKeyword, 
                    sortField, 
                    sortDirection, 
                    request.getPage(), 
                    request.getSize()
            );
            
            // 转换为VO
            List<UserSearchVO> users = userPage.getContent().stream()
                    .map(this::convertToUserSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            users.forEach(this::processUserSearchResult);
            
            // 构建结果
            SearchResultDTO<UserSearchVO> result = new SearchResultDTO<>();
            result.setItems(users);
            result.setTotal(userPage.getTotalElements());
            result.setPage(request.getPage());
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("用户搜索完成，关键词: {}, 结果数: {}, 总数: {}", 
                    processedKeyword, users.size(), userPage.getTotalElements());
            
            return result;
            
        } catch (Exception e) {
            log.error("用户搜索失败，关键词: {}", processedKeyword, e);
            return SearchResultDTO.empty(request);
        }
    }
    
    /**
     * 搜索评论
     * 
     * @param request 搜索请求
     * @return 评论搜索结果
     */
    public SearchResultDTO<CommentSearchVO> searchComments(SearchRequestDTO request) {
        log.info("执行评论搜索，关键词: {}, 页码: {}, 大小: {}, 排序: {}", 
                request.getKeyword(), request.getPage(), request.getSize(), request.getSortBy());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 尝试从缓存获取结果
        String cacheKey = SearchUtils.buildCommentSearchCacheKey(request);
        SearchResultDTO<CommentSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取评论搜索结果，关键词: {}", processedKeyword);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全搜索
            String sortField = mapSortByToField(request.getSortBy());
            String sortDirection = getSortDirection(request.getSortBy());
            
            Page<Comment> commentPage = commentRepository.findCommentsWithSafeSearch(
                    processedKeyword, 
                    sortField, 
                    sortDirection, 
                    request.getPage(), 
                    request.getSize()
            );
            
            // 转换为VO
            List<CommentSearchVO> comments = commentPage.getContent().stream()
                    .map(this::convertToCommentSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            comments.forEach(this::processCommentSearchResult);
            
            // 构建结果
            SearchResultDTO<CommentSearchVO> result = new SearchResultDTO<>();
            result.setItems(comments);
            result.setTotal(commentPage.getTotalElements());
            result.setPage(request.getPage());
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("评论搜索完成，关键词: {}, 结果数: {}, 总数: {}", 
                    processedKeyword, comments.size(), commentPage.getTotalElements());
            
            return result;
            
        } catch (Exception e) {
            log.error("评论搜索失败，关键词: {}", processedKeyword, e);
            return SearchResultDTO.empty(request);
        }
    }
    
    /**
     * 清除搜索缓存
     * 
     * @param keyword 关键词（可选，为空则清除所有搜索缓存）
     */
    public void clearSearchCache(String keyword) {
        try {
            if (StringUtils.hasText(keyword)) {
                // 清除特定关键词的缓存
                String pattern = SearchConstants.SEARCH_CACHE_PREFIX + "*" + keyword + "*";
                var keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                log.info("清除搜索缓存，关键词: {}", keyword);
            } else {
                // 清除所有搜索缓存
                String pattern = SearchConstants.SEARCH_CACHE_PREFIX + "*";
                var keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                log.info("清除所有搜索缓存");
            }
        } catch (Exception e) {
            log.error("清除搜索缓存失败，关键词: {}", keyword, e);
        }
    }
    
    /**
     * 搜索帖子（游标分页）
     * 
     * @param request 搜索请求
     * @param lastId 上次加载的最后一个帖子ID
     * @return 帖子搜索结果
     */
    public SearchResultDTO<PostSearchVO> searchPostsWithCursor(SearchRequestDTO request, Long lastId) {
        log.info("执行帖子游标搜索，关键词: {}, 上次ID: {}, 大小: {}, 排序: {}", 
                request.getKeyword(), lastId, request.getSize(), request.getSortBy());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 构建缓存键（包含游标信息）
        String cacheKey = buildCursorCacheKey("post", request, lastId);
        SearchResultDTO<PostSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取帖子游标搜索结果，关键词: {}, 上次ID: {}", processedKeyword, lastId);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全搜索实现游标分页
            String sortField = mapSortByToField(request.getSortBy());
            String sortDirection = getSortDirection(request.getSortBy());
            
            // 构建游标条件
            Map<String, Object> conditions = new HashMap<>();
            if (lastId != null && lastId > 0) {
                // 根据排序方向设置游标条件
                if ("DESC".equals(sortDirection)) {
                    conditions.put("id", "< " + lastId);
                } else {
                    conditions.put("id", "> " + lastId);
                }
            }
            
            // 多查一条用于判断是否有更多
            Page<Post> postPage = postRepository.findPostsWithSafeSearch(
                    processedKeyword, 
                    sortField, 
                    sortDirection, 
                    0, 
                    request.getSize() + 1
            );
            
            List<Post> posts = postPage.getContent();
            
            // 判断是否有更多数据
            boolean hasMore = posts.size() > request.getSize();
            if (hasMore) {
                posts = posts.subList(0, request.getSize()); // 移除多查的那一条
            }
            
            // 转换为VO
            List<PostSearchVO> postVOs = posts.stream()
                    .map(this::convertToPostSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            postVOs.forEach(this::processPostSearchResult);
            
            // 构建结果
            SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
            result.setItems(postVOs);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!postVOs.isEmpty()) {
                PostSearchVO lastPost = postVOs.get(postVOs.size() - 1);
                result.setNextCursor(lastPost.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("帖子游标搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    processedKeyword, postVOs.size(), hasMore);
            
            return result;
            
        } catch (Exception e) {
            log.error("帖子游标搜索失败，关键词: {}, 上次ID: {}", processedKeyword, lastId, e);
            return SearchResultDTO.emptyInfiniteScroll(processedKeyword, request.getType(), request.getSortBy(), request.getSize());
        }
    }
    
    /**
     * 搜索用户（游标分页）
     * 
     * @param request 搜索请求
     * @param lastId 上次加载的最后一个用户ID
     * @return 用户搜索结果
     */
    public SearchResultDTO<UserSearchVO> searchUsersWithCursor(SearchRequestDTO request, Long lastId) {
        log.info("执行用户游标搜索，关键词: {}, 上次ID: {}, 大小: {}, 排序: {}", 
                request.getKeyword(), lastId, request.getSize(), request.getSortBy());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 构建缓存键（包含游标信息）
        String cacheKey = buildCursorCacheKey("user", request, lastId);
        SearchResultDTO<UserSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取用户游标搜索结果，关键词: {}, 上次ID: {}", processedKeyword, lastId);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全搜索实现游标分页
            String sortField = mapSortByToField(request.getSortBy());
            String sortDirection = getSortDirection(request.getSortBy());
            
            // 多查一条用于判断是否有更多
            Page<User> userPage = userRepository.findUsersWithSafeSearch(
                    processedKeyword, 
                    sortField, 
                    sortDirection, 
                    0, 
                    request.getSize() + 1
            );
            
            List<User> users = userPage.getContent();
            
            // 判断是否有更多数据
            boolean hasMore = users.size() > request.getSize();
            if (hasMore) {
                users = users.subList(0, request.getSize()); // 移除多查的那一条
            }
            
            // 转换为VO
            List<UserSearchVO> userVOs = users.stream()
                    .map(this::convertToUserSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            userVOs.forEach(this::processUserSearchResult);
            
            // 构建结果
            SearchResultDTO<UserSearchVO> result = new SearchResultDTO<>();
            result.setItems(userVOs);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!userVOs.isEmpty()) {
                UserSearchVO lastUser = userVOs.get(userVOs.size() - 1);
                result.setNextCursor(lastUser.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("用户游标搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    processedKeyword, userVOs.size(), hasMore);
            
            return result;
            
        } catch (Exception e) {
            log.error("用户游标搜索失败，关键词: {}, 上次ID: {}", processedKeyword, lastId, e);
            return SearchResultDTO.emptyInfiniteScroll(processedKeyword, request.getType(), request.getSortBy(), request.getSize());
        }
    }
    
    /**
     * 综合搜索（游标分页）
     * 
     * @param request 搜索请求
     * @param lastPostId 上次加载的最后一个帖子ID
     * @param lastUserId 上次加载的最后一个用户ID
     * @return 综合搜索结果
     */
    public SearchResultDTO<Object> searchAllWithCursor(SearchRequestDTO request, Long lastPostId, Long lastUserId) {
        log.info("执行综合游标搜索，关键词: {}, 帖子上次ID: {}, 用户上次ID: {}, 大小: {}", 
                request.getKeyword(), lastPostId, lastUserId, request.getSize());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 构建缓存键
        String cacheKey = buildCombinedCursorCacheKey(request, lastPostId, lastUserId);
        SearchResultDTO<Object> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取综合游标搜索结果，关键词: {}", processedKeyword);
            return cachedResult;
        }
        
        try {
            // 分配搜索数量（帖子、用户、评论各1/3）
            int thirdSize = request.getSize() / 3;
            int postSize = thirdSize + (request.getSize() % 3 > 0 ? 1 : 0); // 余数优先给帖子
            int userSize = thirdSize + (request.getSize() % 3 > 1 ? 1 : 0); // 余数其次给用户
            int commentSize = thirdSize;
            
            // 分别搜索帖子、用户和评论
            SearchRequestDTO postRequest = createSubRequest(request, SearchConstants.SEARCH_TYPE_POST);
            postRequest.setSize(postSize);
            SearchRequestDTO userRequest = createSubRequest(request, SearchConstants.SEARCH_TYPE_USER);
            userRequest.setSize(userSize);
            SearchRequestDTO commentRequest = createSubRequest(request, SearchConstants.SEARCH_TYPE_COMMENT);
            commentRequest.setSize(commentSize);
            
            SearchResultDTO<PostSearchVO> postResults = searchPostsWithCursor(postRequest, lastPostId);
            SearchResultDTO<UserSearchVO> userResults = searchUsersWithCursor(userRequest, lastUserId);
            SearchResultDTO<CommentSearchVO> commentResults = searchCommentsWithCursor(commentRequest, 0L);
            
            // 合并结果
            List<Object> allResults = new ArrayList<>();
            allResults.addAll(postResults.getItems());
            allResults.addAll(userResults.getItems());
            allResults.addAll(commentResults.getItems());
            
            // 构建综合搜索结果
            SearchResultDTO<Object> result = new SearchResultDTO<>();
            result.setItems(allResults);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            
            // 设置是否有更多数据（任一类型有更多即为true）
            result.setHasMore(postResults.getHasMore() || userResults.getHasMore() || commentResults.getHasMore());
            
            // 设置游标信息（JSON格式包含三个游标）
            if (postResults.getNextCursor() != null || userResults.getNextCursor() != null || commentResults.getNextCursor() != null) {
                String nextCursor = String.format("{\"postCursor\":\"%s\",\"userCursor\":\"%s\",\"commentCursor\":\"%s\"}", 
                        postResults.getNextCursor() != null ? postResults.getNextCursor() : "0",
                        userResults.getNextCursor() != null ? userResults.getNextCursor() : "0",
                        commentResults.getNextCursor() != null ? commentResults.getNextCursor() : "0");
                result.setNextCursor(nextCursor);
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("综合游标搜索完成，关键词: {}, 帖子数: {}, 用户数: {}, 评论数: {}, 是否有更多: {}", 
                    processedKeyword, postResults.getItems().size(), userResults.getItems().size(), 
                    commentResults.getItems().size(), result.getHasMore());
            
            return result;
            
        } catch (Exception e) {
            log.error("综合游标搜索失败，关键词: {}", processedKeyword, e);
            return SearchResultDTO.emptyInfiniteScroll(processedKeyword, request.getType(), request.getSortBy(), request.getSize());
        }
    }
    
    /**
     * 搜索评论（游标分页）
     * 
     * @param request 搜索请求
     * @param lastId 上次加载的最后一个评论ID
     * @return 评论搜索结果
     */
    public SearchResultDTO<CommentSearchVO> searchCommentsWithCursor(SearchRequestDTO request, Long lastId) {
        log.info("执行评论游标搜索，关键词: {}, 上次ID: {}, 大小: {}, 排序: {}", 
                request.getKeyword(), lastId, request.getSize(), request.getSortBy());
        
        // 验证搜索请求
        SearchUtils.validateSearchRequest(request);
        
        // 预处理关键词
        String processedKeyword = SearchUtils.preprocessKeyword(request.getKeyword());
        request.setKeyword(processedKeyword);
        
        // 构建缓存键（包含游标信息）
        String cacheKey = buildCursorCacheKey("comment", request, lastId);
        SearchResultDTO<CommentSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取评论游标搜索结果，关键词: {}, 上次ID: {}", processedKeyword, lastId);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全搜索实现游标分页
            String sortField = mapSortByToField(request.getSortBy());
            String sortDirection = getSortDirection(request.getSortBy());
            
            // 多查一条用于判断是否有更多
            Page<Comment> commentPage = commentRepository.findCommentsWithSafeSearch(
                    processedKeyword, 
                    sortField, 
                    sortDirection, 
                    0, 
                    request.getSize() + 1
            );
            
            List<Comment> comments = commentPage.getContent();
            
            // 判断是否有更多数据
            boolean hasMore = comments.size() > request.getSize();
            if (hasMore) {
                comments = comments.subList(0, request.getSize()); // 移除多查的那一条
            }
            
            // 转换为VO
            List<CommentSearchVO> commentVOs = comments.stream()
                    .map(this::convertToCommentSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            commentVOs.forEach(this::processCommentSearchResult);
            
            // 构建结果
            SearchResultDTO<CommentSearchVO> result = new SearchResultDTO<>();
            result.setItems(commentVOs);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!commentVOs.isEmpty()) {
                CommentSearchVO lastComment = commentVOs.get(commentVOs.size() - 1);
                result.setNextCursor(lastComment.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("评论游标搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    processedKeyword, commentVOs.size(), hasMore);
            
            return result;
            
        } catch (Exception e) {
            log.error("评论游标搜索失败，关键词: {}, 上次ID: {}", processedKeyword, lastId, e);
            return SearchResultDTO.emptyInfiniteScroll(processedKeyword, request.getType(), request.getSortBy(), request.getSize());
        }
    }
    
    /**
     * 搜索所有帖子（游标分页）- 当关键词为"all"时使用
     * 
     * @param lastId 上次加载的最后一个帖子ID
     * @param size 每次加载数量
     * @param sortBy 排序方式
     * @return 所有帖子搜索结果
     */
    public SearchResultDTO<PostSearchVO> searchAllPostsWithCursor(Long lastId, Integer size, String sortBy) {
        log.info("执行搜索所有帖子，上次ID: {}, 大小: {}, 排序: {}", lastId, size, sortBy);
        
        // 构建缓存键
        String cacheKey = buildAllPostsCacheKey(lastId, size, sortBy);
        SearchResultDTO<PostSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取所有帖子搜索结果，上次ID: {}", lastId);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全查询获取所有帖子
            String sortField = mapSortByToField(sortBy);
            String sortDirection = getSortDirection(sortBy);
            
            Page<Post> postPage = postRepository.findSafePaginated(
                    new HashMap<>(), // 空条件表示查询所有
                    sortField, 
                    sortDirection, 
                    0, 
                    size + 1
            );
            
            List<Post> posts = postPage.getContent();
            
            // 判断是否有更多数据
            boolean hasMore = posts.size() > size;
            if (hasMore) {
                posts = posts.subList(0, size); // 移除多查的那一条
            }
            
            // 转换为VO
            List<PostSearchVO> postVOs = posts.stream()
                    .map(this::convertToPostSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            postVOs.forEach(this::processPostSearchResult);
            
            // 构建结果
            SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
            result.setItems(postVOs);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(size);
            result.setKeyword("all");
            result.setType(SearchConstants.SEARCH_TYPE_POST);
            result.setSortBy(sortBy);
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!postVOs.isEmpty()) {
                PostSearchVO lastPost = postVOs.get(postVOs.size() - 1);
                result.setNextCursor(lastPost.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("搜索所有帖子完成，结果数: {}, 是否有更多: {}", postVOs.size(), hasMore);
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索所有帖子失败，上次ID: {}", lastId, e);
            return SearchResultDTO.emptyInfiniteScroll("all", SearchConstants.SEARCH_TYPE_POST, sortBy, size);
        }
    }
    
    /**
     * 搜索所有用户（游标分页）- 当关键词为"all"时使用
     * 
     * @param lastId 上次加载的最后一个用户ID
     * @param size 每次加载数量
     * @param sortBy 排序方式
     * @return 所有用户搜索结果
     */
    public SearchResultDTO<UserSearchVO> searchAllUsersWithCursor(Long lastId, Integer size, String sortBy) {
        log.info("执行搜索所有用户，上次ID: {}, 大小: {}, 排序: {}", lastId, size, sortBy);
        
        // 构建缓存键
        String cacheKey = buildAllUsersCacheKey(lastId, size, sortBy);
        SearchResultDTO<UserSearchVO> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取所有用户搜索结果，上次ID: {}", lastId);
            return cachedResult;
        }
        
        try {
            // 使用JPA安全查询获取所有用户
            String sortField = mapSortByToField(sortBy);
            String sortDirection = getSortDirection(sortBy);
            
            Page<User> userPage = userRepository.findUsersWithSafePagination(
                    new HashMap<>(), // 空条件表示查询所有
                    sortField, 
                    sortDirection, 
                    0, 
                    size + 1
            );
            
            List<User> users = userPage.getContent();
            
            // 判断是否有更多数据
            boolean hasMore = users.size() > size;
            if (hasMore) {
                users = users.subList(0, size); // 移除多查的那一条
            }
            
            // 转换为VO
            List<UserSearchVO> userVOs = users.stream()
                    .map(this::convertToUserSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            userVOs.forEach(this::processUserSearchResult);
            
            // 构建结果
            SearchResultDTO<UserSearchVO> result = new SearchResultDTO<>();
            result.setItems(userVOs);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(size);
            result.setKeyword("all");
            result.setType(SearchConstants.SEARCH_TYPE_USER);
            result.setSortBy(sortBy);
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!userVOs.isEmpty()) {
                UserSearchVO lastUser = userVOs.get(userVOs.size() - 1);
                result.setNextCursor(lastUser.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("搜索所有用户完成，结果数: {}, 是否有更多: {}", userVOs.size(), hasMore);
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索所有用户失败，上次ID: {}", lastId, e);
            return SearchResultDTO.emptyInfiniteScroll("all", SearchConstants.SEARCH_TYPE_USER, sortBy, size);
        }
    }
    
    /**
     * 搜索所有内容（帖子、用户和评论，游标分页）- 当关键词为"all"时使用
     * 
     * @param lastPostId 上次加载的最后一个帖子ID
     * @param lastUserId 上次加载的最后一个用户ID
     * @param size 每次加载数量
     * @param sortBy 排序方式
     * @return 所有内容搜索结果
     */
    public SearchResultDTO<Object> searchAllContentWithCursor(Long lastPostId, Long lastUserId, Integer size, String sortBy) {
        log.info("执行搜索所有内容，帖子上次ID: {}, 用户上次ID: {}, 大小: {}, 排序: {}", 
                lastPostId, lastUserId, size, sortBy);
        
        // 构建缓存键
        String cacheKey = buildAllContentCacheKey(lastPostId, lastUserId, size, sortBy);
        SearchResultDTO<Object> cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取所有内容搜索结果");
            return cachedResult;
        }
        
        try {
            // 分配搜索数量（帖子、用户、评论各1/3）
            int thirdSize = size / 3;
            int postSize = thirdSize + (size % 3 > 0 ? 1 : 0); // 余数优先给帖子
            int userSize = thirdSize + (size % 3 > 1 ? 1 : 0); // 余数其次给用户
            int commentSize = thirdSize;
            
            // 分别搜索所有帖子、用户和评论
            SearchResultDTO<PostSearchVO> postResults = searchAllPostsWithCursor(lastPostId, postSize, sortBy);
            SearchResultDTO<UserSearchVO> userResults = searchAllUsersWithCursor(lastUserId, userSize, sortBy);
            SearchResultDTO<CommentSearchVO> commentResults = searchAllCommentsWithCursor(0L, commentSize, sortBy);
            
            // 合并结果
            List<Object> allResults = new ArrayList<>();
            allResults.addAll(postResults.getItems());
            allResults.addAll(userResults.getItems());
            allResults.addAll(commentResults.getItems());
            
            // 构建综合搜索结果
            SearchResultDTO<Object> result = new SearchResultDTO<>();
            result.setItems(allResults);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(size);
            result.setKeyword("all");
            result.setType(SearchConstants.SEARCH_TYPE_ALL);
            result.setSortBy(sortBy);
            
            // 设置是否有更多数据（任一类型有更多即为true）
            result.setHasMore(postResults.getHasMore() || userResults.getHasMore() || commentResults.getHasMore());
            
            // 设置游标信息（JSON格式包含三个游标）
            if (postResults.getNextCursor() != null || userResults.getNextCursor() != null || commentResults.getNextCursor() != null) {
                String nextCursor = String.format("{\"postCursor\":\"%s\",\"userCursor\":\"%s\",\"commentCursor\":\"%s\"}", 
                        postResults.getNextCursor() != null ? postResults.getNextCursor() : "0",
                        userResults.getNextCursor() != null ? userResults.getNextCursor() : "0",
                        commentResults.getNextCursor() != null ? commentResults.getNextCursor() : "0");
                result.setNextCursor(nextCursor);
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("搜索所有内容完成，帖子数: {}, 用户数: {}, 评论数: {}, 是否有更多: {}", 
                    postResults.getItems().size(), userResults.getItems().size(), 
                    commentResults.getItems().size(), result.getHasMore());
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索所有内容失败", e);
            return SearchResultDTO.emptyInfiniteScroll("all", SearchConstants.SEARCH_TYPE_ALL, sortBy, size);
        }
    }
    
    /**
     * 搜索所有评论（游标分页）- 当关键词为"all"时使用
     * 
     * @param lastId 上次加载的最后一个评论ID
     * @param size 每次加载数量
     * @param sortBy 排序方式
     * @return 所有评论搜索结果
     */
    public SearchResultDTO<CommentSearchVO> searchAllCommentsWithCursor(Long lastId, Integer size, String sortBy) {
        log.info("执行搜索所有评论，上次ID: {}, 大小: {}, 排序: {}", lastId, size, sortBy);
        
        try {
            // 使用JPA安全查询获取所有评论
            String sortField = mapSortByToField(sortBy);
            String sortDirection = getSortDirection(sortBy);
            
            Page<Comment> commentPage = commentRepository.findCommentsWithSafeSearch(
                    "", // 空关键词表示查询所有
                    sortField, 
                    sortDirection, 
                    0, 
                    size + 1
            );
            
            List<Comment> comments = commentPage.getContent();
            
            // 判断是否有更多数据
            boolean hasMore = comments.size() > size;
            if (hasMore) {
                comments = comments.subList(0, size); // 移除多查的那一条
            }
            
            // 转换为VO
            List<CommentSearchVO> commentVOs = comments.stream()
                    .map(this::convertToCommentSearchVO)
                    .collect(Collectors.toList());
            
            // 处理搜索结果
            commentVOs.forEach(this::processCommentSearchResult);
            
            // 构建结果
            SearchResultDTO<CommentSearchVO> result = new SearchResultDTO<>();
            result.setItems(commentVOs);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(size);
            result.setKeyword("all");
            result.setType(SearchConstants.SEARCH_TYPE_COMMENT);
            result.setSortBy(sortBy);
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!commentVOs.isEmpty()) {
                CommentSearchVO lastComment = commentVOs.get(commentVOs.size() - 1);
                result.setNextCursor(lastComment.getId().toString());
            }
            
            log.info("搜索所有评论完成，结果数: {}, 是否有更多: {}", commentVOs.size(), hasMore);
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索所有评论失败，上次ID: {}", lastId, e);
            return SearchResultDTO.emptyInfiniteScroll("all", SearchConstants.SEARCH_TYPE_COMMENT, sortBy, size);
        }
    }
    
    /**
     * 获取热门搜索关键词
     * 
     * @param limit 限制数量
     * @return 热门搜索关键词列表
     */
    public List<String> getHotSearchKeywords(int limit) {
        try {
            // 从Redis获取热门搜索关键词
            // 这里可以根据实际需求实现，比如统计搜索频率
            String hotKeywordsKey = SearchConstants.HOT_SEARCH_KEYWORDS_KEY;
            List<Object> hotKeywords = redisTemplate.opsForList().range(hotKeywordsKey, 0, limit - 1);
            
            if (hotKeywords != null && !hotKeywords.isEmpty()) {
                return hotKeywords.stream()
                        .map(Object::toString)
                        .toList();
            }
            
            // 如果没有热门关键词，返回空列表
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("获取热门搜索关键词失败", e);
            return new ArrayList<>();
        }
    }
    

    
    /**
     * 从缓存获取搜索结果
     * 
     * @param cacheKey 缓存键
     * @return 缓存的搜索结果，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    private <T> SearchResultDTO<T> getCachedResult(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof SearchResultDTO) {
                return (SearchResultDTO<T>) cached;
            }
        } catch (Exception e) {
            log.warn("获取缓存搜索结果失败，缓存键: {}，错误: {}，将删除该缓存", cacheKey, e.getMessage());
            // 删除有问题的缓存
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception deleteEx) {
                log.warn("删除有问题的缓存失败: {}", deleteEx.getMessage());
            }
        }
        return null;
    }
    
    /**
     * 缓存搜索结果
     * 
     * @param cacheKey 缓存键
     * @param result 搜索结果
     */
    private void cacheSearchResult(String cacheKey, SearchResultDTO<?> result) {
        try {
            redisTemplate.opsForValue().set(cacheKey, result, SearchConstants.CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("缓存搜索结果，缓存键: {}", cacheKey);
        } catch (Exception e) {
            log.warn("缓存搜索结果失败，缓存键: {}", cacheKey, e);
        }
    }
    
    // ========== 实体转换方法 ==========
    
    /**
     * 转换Post实体为PostSearchVO
     * 
     * @param post Post实体
     * @return PostSearchVO
     */
    private PostSearchVO convertToPostSearchVO(Post post) {
        PostSearchVO vo = new PostSearchVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContentSummary(SearchUtils.truncateContent(post.getContent()));
        vo.setAuthorId(post.getAuthor().getId());
        vo.setAuthorUsername(post.getAuthor().getUsername());
        vo.setAuthorAvatarUrl(post.getAuthor().getAvatarUrl());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
        vo.setCollectCount(post.getCollectCount() != null ? post.getCollectCount() : 0);
        vo.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0);
        vo.setImageIds(post.getImageIds() != null ? post.getImageIds() : new ArrayList<>());
        return vo;
    }
    
    /**
     * 转换User实体为UserSearchVO
     * 
     * @param user User实体
     * @return UserSearchVO
     */
    private UserSearchVO convertToUserSearchVO(User user) {
        UserSearchVO vo = new UserSearchVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(SearchUtils.maskEmail(user.getEmail()));
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBio(SearchUtils.truncateBio(user.getBio()));
        vo.setLikedCount(user.getLikedCount() != null ? user.getLikedCount() : 0);
        vo.setFollowersCount(user.getFollowerCount() != null ? user.getFollowerCount() : 0);
        vo.setFollowingCount(user.getFollowCount() != null ? user.getFollowCount() : 0);
        vo.setPostsCount(0); // 需要通过查询获取，暂时设为0
        return vo;
    }
    
    /**
     * 转换Comment实体为CommentSearchVO
     * 
     * @param comment Comment实体
     * @return CommentSearchVO
     */
    private CommentSearchVO convertToCommentSearchVO(Comment comment) {
        CommentSearchVO vo = new CommentSearchVO();
        vo.setId(comment.getId());
        vo.setContentSummary(SearchUtils.truncateCommentContent(comment.getContent()));
        vo.setAuthorId(comment.getAuthor().getId());
        vo.setAuthorUsername(comment.getAuthor().getUsername());
        vo.setAuthorAvatarUrl(comment.getAuthor().getAvatarUrl());
        vo.setPostId(comment.getPost().getId());
        vo.setPostTitle(comment.getPost().getTitle());
        vo.setIsReply(comment.getParent() != null ? comment.getParent().getId() : null);
        vo.setCreatedAt(comment.getCreatedAt());
        // 注意：这里需要根据实际的Comment实体字段调整
        vo.setLikeCount(0); // 如果Comment实体有likeCount字段，请替换
        vo.setReplyCount(comment.getReplies() != null ? comment.getReplies().size() : 0);
        return vo;
    }
    
    // ========== 搜索结果处理方法 ==========
    
    /**
     * 处理帖子搜索结果
     * 
     * @param post 帖子搜索结果
     */
    private void processPostSearchResult(PostSearchVO post) {
        // 计算热度评分
        post.calculatePopularityScore();
        
        // 计算相关性评分（简单实现）
        // 这里可以根据实际需求实现更复杂的相关性算法
        post.setRelevanceScore(1.0);
        
        // 确保内容摘要不为空
        if (post.getContentSummary() == null) {
            post.setContentSummary("");
        }
        
        // 确保图片ID列表不为空
        if (post.getImageIds() == null) {
            post.setImageIds(new ArrayList<>());
        }
    }
    
    /**
     * 处理用户搜索结果
     * 
     * @param user 用户搜索结果
     */
    private void processUserSearchResult(UserSearchVO user) {
        // 计算相关性评分（简单实现）
        user.setRelevanceScore(1.0);
        
        // 确保统计数据不为空
        if (user.getLikedCount() == null) {
            user.setLikedCount(0);
        }
        if (user.getFollowersCount() == null) {
            user.setFollowersCount(0);
        }
        if (user.getFollowingCount() == null) {
            user.setFollowingCount(0);
        }
        if (user.getPostsCount() == null) {
            user.setPostsCount(0);
        }
    }
    
    /**
     * 处理评论搜索结果
     * 
     * @param comment 评论搜索结果
     */
    private void processCommentSearchResult(CommentSearchVO comment) {
        // 计算热度评分
        comment.calculatePopularityScore();
        
        // 计算相关性评分（简单实现）
        comment.setRelevanceScore(1.0);
        
        // 确保内容摘要不为空
        if (comment.getContentSummary() == null) {
            comment.setContentSummary("");
        }
        
        // 确保统计数据不为空
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        if (comment.getReplyCount() == null) {
            comment.setReplyCount(0);
        }
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 创建子搜索请求
     * 
     * @param originalRequest 原始请求
     * @param type 搜索类型
     * @return 子搜索请求
     */
    private SearchRequestDTO createSubRequest(SearchRequestDTO originalRequest, String type) {
        SearchRequestDTO subRequest = new SearchRequestDTO();
        subRequest.setKeyword(originalRequest.getKeyword());
        subRequest.setType(type);
        subRequest.setPage(originalRequest.getPage());
        subRequest.setSize(originalRequest.getSize() / 3); // 综合搜索时每种类型分配1/3的数量
        subRequest.setSortBy(originalRequest.getSortBy());
        return subRequest;
    }
    
    /**
     * 映射排序方式到字段名
     * 
     * @param sortBy 排序方式
     * @return 字段名
     */
    private String mapSortByToField(String sortBy) {
        switch (sortBy) {
            case SearchConstants.SORT_BY_TIME:
                return "createdAt"; // JPA字段名
            case SearchConstants.SORT_BY_POPULARITY:
                return "likeCount"; // JPA字段名
            case SearchConstants.SORT_BY_RELEVANCE:
            default:
                return "createdAt"; // 默认按时间排序
        }
    }
    
    /**
     * 获取排序方向
     * 
     * @param sortBy 排序方式
     * @return 排序方向
     */
    private String getSortDirection(String sortBy) {
        switch (sortBy) {
            case SearchConstants.SORT_BY_TIME:
                return "DESC"; // 时间倒序
            case SearchConstants.SORT_BY_POPULARITY:
                return "DESC"; // 热度倒序
            case SearchConstants.SORT_BY_RELEVANCE:
            default:
                return "DESC"; // 默认倒序
        }
    }
    
    /**
     * 构建游标缓存键
     * 
     * @param type 搜索类型
     * @param request 搜索请求
     * @param lastId 上次ID
     * @return 缓存键
     */
    private String buildCursorCacheKey(String type, SearchRequestDTO request, Long lastId) {
        return SearchConstants.SEARCH_CACHE_PREFIX + "cursor:" + type + ":" + 
               SearchUtils.toCacheKeyword(request.getKeyword()) + ":" +
               (lastId != null ? lastId : 0) + ":" +
               request.getSize() + ":" +
               request.getSortBy();
    }
    
    /**
     * 构建综合搜索游标缓存键
     * 
     * @param request 搜索请求
     * @param lastPostId 上次帖子ID
     * @param lastUserId 上次用户ID
     * @return 缓存键
     */
    private String buildCombinedCursorCacheKey(SearchRequestDTO request, Long lastPostId, Long lastUserId) {
        return SearchConstants.SEARCH_CACHE_PREFIX + "cursor:all:" + 
               SearchUtils.toCacheKeyword(request.getKeyword()) + ":" +
               (lastPostId != null ? lastPostId : 0) + ":" +
               (lastUserId != null ? lastUserId : 0) + ":" +
               request.getSize() + ":" +
               request.getSortBy();
    }
    
    /**
     * 构建所有帖子缓存键
     * 
     * @param lastId 上次ID
     * @param size 大小
     * @param sortBy 排序方式
     * @return 缓存键
     */
    private String buildAllPostsCacheKey(Long lastId, Integer size, String sortBy) {
        return SearchConstants.SEARCH_CACHE_PREFIX + "all:posts:" + 
               (lastId != null ? lastId : 0) + ":" +
               size + ":" +
               sortBy;
    }
    
    /**
     * 构建所有用户缓存键
     * 
     * @param lastId 上次ID
     * @param size 大小
     * @param sortBy 排序方式
     * @return 缓存键
     */
    private String buildAllUsersCacheKey(Long lastId, Integer size, String sortBy) {
        return SearchConstants.SEARCH_CACHE_PREFIX + "all:users:" + 
               (lastId != null ? lastId : 0) + ":" +
               size + ":" +
               sortBy;
    }
    
    /**
     * 构建所有内容缓存键
     * 
     * @param lastPostId 上次帖子ID
     * @param lastUserId 上次用户ID
     * @param size 大小
     * @param sortBy 排序方式
     * @return 缓存键
     */
    private String buildAllContentCacheKey(Long lastPostId, Long lastUserId, Integer size, String sortBy) {
        return SearchConstants.SEARCH_CACHE_PREFIX + "all:content:" + 
               (lastPostId != null ? lastPostId : 0) + ":" +
               (lastUserId != null ? lastUserId : 0) + ":" +
               size + ":" +
               sortBy;
    }
}