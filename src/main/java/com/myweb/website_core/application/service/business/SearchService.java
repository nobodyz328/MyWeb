package com.myweb.website_core.application.service.business;

import com.myweb.website_core.common.constant.SearchConstants;
import com.myweb.website_core.common.util.SearchUtils;
import com.myweb.website_core.domain.business.dto.SearchRequestDTO;
import com.myweb.website_core.domain.business.dto.SearchResultDTO;
import com.myweb.website_core.domain.business.vo.PostSearchVO;
import com.myweb.website_core.domain.business.vo.UserSearchVO;
import com.myweb.website_core.infrastructure.persistence.mapper.PostMapper;
import com.myweb.website_core.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    

    
    /**
     * 综合搜索 - 同时搜索帖子和用户
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
        
        // 分别搜索帖子和用户
        SearchResultDTO<PostSearchVO> postResults = searchPosts(request);
        SearchResultDTO<UserSearchVO> userResults = searchUsers(request);
        
        // 合并结果
        List<Object> allResults = new ArrayList<>();
        allResults.addAll(postResults.getItems());
        allResults.addAll(userResults.getItems());
        
        // 构建综合搜索结果
        SearchResultDTO<Object> result = new SearchResultDTO<>();
        result.setItems(allResults);
        result.setTotal(postResults.getTotal() + userResults.getTotal());
        result.setPage(request.getPage());
        result.setSize(request.getSize());
        result.setKeyword(processedKeyword);
        result.setType(request.getType());
        result.setSortBy(request.getSortBy());
        
        // 缓存结果
        cacheSearchResult(cacheKey, result);
        
        log.info("综合搜索完成，关键词: {}, 帖子数: {}, 用户数: {}", 
                processedKeyword, postResults.getItems().size(), userResults.getItems().size());
        
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
            // 执行搜索
            List<PostSearchVO> posts = postMapper.searchPostsWithPagination(
                    processedKeyword, 
                    request.getSortBy(), 
                    request.getOffset(), 
                    request.getSize()
            );
            
            // 统计总数
            Long total = postMapper.countSearchPosts(processedKeyword);
            
            // 处理搜索结果
            posts.forEach(this::processPostSearchResult);
            
            // 构建结果
            SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
            result.setItems(posts);
            result.setTotal(total);
            result.setPage(request.getPage());
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("帖子搜索完成，关键词: {}, 结果数: {}, 总数: {}", 
                    processedKeyword, posts.size(), total);
            
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
            // 执行搜索
            List<UserSearchVO> users = userMapper.searchUsersWithPagination(
                    processedKeyword, 
                    request.getSortBy(), 
                    request.getOffset(), 
                    request.getSize()
            );
            
            // 统计总数
            Long total = userMapper.countSearchUsers(processedKeyword);
            
            // 处理搜索结果
            users.forEach(this::processUserSearchResult);
            
            // 构建结果
            SearchResultDTO<UserSearchVO> result = new SearchResultDTO<>();
            result.setItems(users);
            result.setTotal(total);
            result.setPage(request.getPage());
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("用户搜索完成，关键词: {}, 结果数: {}, 总数: {}", 
                    processedKeyword, users.size(), total);
            
            return result;
            
        } catch (Exception e) {
            log.error("用户搜索失败，关键词: {}", processedKeyword, e);
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
                redisTemplate.delete(redisTemplate.keys(pattern));
                log.info("清除搜索缓存，关键词: {}", keyword);
            } else {
                // 清除所有搜索缓存
                String pattern = SearchConstants.SEARCH_CACHE_PREFIX + "*";
                redisTemplate.delete(redisTemplate.keys(pattern));
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
            // 执行游标搜索
            List<PostSearchVO> posts = postMapper.searchPostsWithCursor(
                    processedKeyword, 
                    request.getSortBy(), 
                    lastId,
                    request.getSize() + 1 // 多查一条用于判断是否有更多
            );
            
            // 判断是否有更多数据
            boolean hasMore = posts.size() > request.getSize();
            if (hasMore) {
                posts = posts.subList(0, request.getSize()); // 移除多查的那一条
            }
            
            // 处理搜索结果
            posts.forEach(this::processPostSearchResult);
            
            // 构建结果
            SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
            result.setItems(posts);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!posts.isEmpty()) {
                PostSearchVO lastPost = posts.get(posts.size() - 1);
                result.setNextCursor(lastPost.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("帖子游标搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    processedKeyword, posts.size(), hasMore);
            
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
            // 执行游标搜索
            List<UserSearchVO> users = userMapper.searchUsersWithCursor(
                    processedKeyword, 
                    request.getSortBy(), 
                    lastId,
                    request.getSize() + 1 // 多查一条用于判断是否有更多
            );
            
            // 判断是否有更多数据
            boolean hasMore = users.size() > request.getSize();
            if (hasMore) {
                users = users.subList(0, request.getSize()); // 移除多查的那一条
            }
            
            // 处理搜索结果
            users.forEach(this::processUserSearchResult);
            
            // 构建结果
            SearchResultDTO<UserSearchVO> result = new SearchResultDTO<>();
            result.setItems(users);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(request.getSize());
            result.setKeyword(processedKeyword);
            result.setType(request.getType());
            result.setSortBy(request.getSortBy());
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!users.isEmpty()) {
                UserSearchVO lastUser = users.get(users.size() - 1);
                result.setNextCursor(lastUser.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("用户游标搜索完成，关键词: {}, 结果数: {}, 是否有更多: {}", 
                    processedKeyword, users.size(), hasMore);
            
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
            // 分配搜索数量（帖子和用户各一半）
            int halfSize = request.getSize() / 2;
            int postSize = halfSize + (request.getSize() % 2); // 奇数时帖子多一个
            int userSize = halfSize;
            
            // 分别搜索帖子和用户
            SearchRequestDTO postRequest = new SearchRequestDTO();
            postRequest.setKeyword(processedKeyword);
            postRequest.setType(SearchConstants.SEARCH_TYPE_POST);
            postRequest.setSize(postSize);
            postRequest.setSortBy(request.getSortBy());
            
            SearchRequestDTO userRequest = new SearchRequestDTO();
            userRequest.setKeyword(processedKeyword);
            userRequest.setType(SearchConstants.SEARCH_TYPE_USER);
            userRequest.setSize(userSize);
            userRequest.setSortBy(request.getSortBy());
            
            SearchResultDTO<PostSearchVO> postResults = searchPostsWithCursor(postRequest, lastPostId);
            SearchResultDTO<UserSearchVO> userResults = searchUsersWithCursor(userRequest, lastUserId);
            
            // 合并结果
            List<Object> allResults = new ArrayList<>();
            allResults.addAll(postResults.getItems());
            allResults.addAll(userResults.getItems());
            
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
            result.setHasMore(postResults.getHasMore() || userResults.getHasMore());
            
            // 设置游标信息（JSON格式包含两个游标）
            if (postResults.getNextCursor() != null || userResults.getNextCursor() != null) {
                String nextCursor = String.format("{\"postCursor\":\"%s\",\"userCursor\":\"%s\"}", 
                        postResults.getNextCursor() != null ? postResults.getNextCursor() : "0",
                        userResults.getNextCursor() != null ? userResults.getNextCursor() : "0");
                result.setNextCursor(nextCursor);
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("综合游标搜索完成，关键词: {}, 帖子数: {}, 用户数: {}, 是否有更多: {}", 
                    processedKeyword, postResults.getItems().size(), userResults.getItems().size(), result.getHasMore());
            
            return result;
            
        } catch (Exception e) {
            log.error("综合游标搜索失败，关键词: {}", processedKeyword, e);
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
            // 执行查询所有帖子
            List<PostSearchVO> posts = postMapper.getAllPostsWithCursor(sortBy, lastId, size + 1);
            
            // 判断是否有更多数据
            boolean hasMore = posts.size() > size;
            if (hasMore) {
                posts = posts.subList(0, size); // 移除多查的那一条
            }
            
            // 处理搜索结果
            posts.forEach(this::processPostSearchResult);
            
            // 构建结果
            SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
            result.setItems(posts);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(size);
            result.setKeyword("all");
            result.setType(SearchConstants.SEARCH_TYPE_POST);
            result.setSortBy(sortBy);
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!posts.isEmpty()) {
                PostSearchVO lastPost = posts.get(posts.size() - 1);
                result.setNextCursor(lastPost.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("搜索所有帖子完成，结果数: {}, 是否有更多: {}", posts.size(), hasMore);
            
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
            // 执行查询所有用户
            List<UserSearchVO> users = userMapper.getAllUsersWithCursor(sortBy, lastId, size + 1);
            
            // 判断是否有更多数据
            boolean hasMore = users.size() > size;
            if (hasMore) {
                users = users.subList(0, size); // 移除多查的那一条
            }
            
            // 处理搜索结果
            users.forEach(this::processUserSearchResult);
            
            // 构建结果
            SearchResultDTO<UserSearchVO> result = new SearchResultDTO<>();
            result.setItems(users);
            result.setTotal(-1L); // 游标分页不需要总数
            result.setPage(0);
            result.setSize(size);
            result.setKeyword("all");
            result.setType(SearchConstants.SEARCH_TYPE_USER);
            result.setSortBy(sortBy);
            result.setHasMore(hasMore);
            
            // 设置下一页游标
            if (!users.isEmpty()) {
                UserSearchVO lastUser = users.get(users.size() - 1);
                result.setNextCursor(lastUser.getId().toString());
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("搜索所有用户完成，结果数: {}, 是否有更多: {}", users.size(), hasMore);
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索所有用户失败，上次ID: {}", lastId, e);
            return SearchResultDTO.emptyInfiniteScroll("all", SearchConstants.SEARCH_TYPE_USER, sortBy, size);
        }
    }
    
    /**
     * 搜索所有内容（帖子和用户，游标分页）- 当关键词为"all"时使用
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
            // 分配搜索数量（帖子和用户各一半）
            int halfSize = size / 2;
            int postSize = halfSize + (size % 2); // 奇数时帖子多一个
            int userSize = halfSize;
            
            // 分别搜索所有帖子和用户
            SearchResultDTO<PostSearchVO> postResults = searchAllPostsWithCursor(lastPostId, postSize, sortBy);
            SearchResultDTO<UserSearchVO> userResults = searchAllUsersWithCursor(lastUserId, userSize, sortBy);
            
            // 合并结果
            List<Object> allResults = new ArrayList<>();
            allResults.addAll(postResults.getItems());
            allResults.addAll(userResults.getItems());
            
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
            result.setHasMore(postResults.getHasMore() || userResults.getHasMore());
            
            // 设置游标信息（JSON格式包含两个游标）
            if (postResults.getNextCursor() != null || userResults.getNextCursor() != null) {
                String nextCursor = String.format("{\"postCursor\":\"%s\",\"userCursor\":\"%s\"}", 
                        postResults.getNextCursor() != null ? postResults.getNextCursor() : "0",
                        userResults.getNextCursor() != null ? userResults.getNextCursor() : "0");
                result.setNextCursor(nextCursor);
            }
            
            // 缓存结果
            cacheSearchResult(cacheKey, result);
            
            log.info("搜索所有内容完成，帖子数: {}, 用户数: {}, 是否有更多: {}", 
                    postResults.getItems().size(), userResults.getItems().size(), result.getHasMore());
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索所有内容失败", e);
            return SearchResultDTO.emptyInfiniteScroll("all", SearchConstants.SEARCH_TYPE_ALL, sortBy, size);
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
            log.warn("获取缓存搜索结果失败，缓存键: {}", cacheKey, e);
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
    
    /**
     * 处理帖子搜索结果
     * 
     * @param post 帖子搜索结果
     */
    private void processPostSearchResult(PostSearchVO post) {
        // 计算热度评分
        post.calculatePopularityScore();
        
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
        // 设置脱敏邮箱
        if (StringUtils.hasText(user.getEmail())) {
            user.setMaskedEmail(user.getEmail());
        }
        
        // 设置个人简介摘要
        if (StringUtils.hasText(user.getBio())) {
            user.setBioSummary(user.getBio());
        }
        
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