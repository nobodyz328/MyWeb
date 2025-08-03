package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.SearchService;
import com.myweb.website_core.domain.business.dto.SearchResultDTO;
import com.myweb.website_core.domain.business.vo.PostSearchVO;
import com.myweb.website_core.domain.business.vo.UserSearchVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SearchController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SearchControllerTest {
    
    @Mock
    private SearchService searchService;
    
    @InjectMocks
    private SearchController searchController;
    
    @BeforeEach
    void setUp() {
        // 初始化设置
    }
    
    @Test
    void testSearchPosts_Success() {
        // 准备测试数据
        String keyword = "测试关键词";
        Long lastId = 0L;
        Integer size = 20;
        String sortBy = "RELEVANCE";
        
        SearchResultDTO<PostSearchVO> mockResult = createMockPostSearchResult();
        when(searchService.searchPostsWithCursor(any(), eq(lastId))).thenReturn(mockResult);
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchPosts(keyword, lastId, size, sortBy);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证方法调用
        verify(searchService).searchPostsWithCursor(any(), eq(lastId));
    }
    
    @Test
    void testSearchUsers_Success() {
        // 准备测试数据
        String keyword = "用户名";
        Long lastId = 0L;
        Integer size = 20;
        String sortBy = "RELEVANCE";
        
        SearchResultDTO<UserSearchVO> mockResult = createMockUserSearchResult();
        when(searchService.searchUsersWithCursor(any(), eq(lastId))).thenReturn(mockResult);
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchUsers(keyword, lastId, size, sortBy);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证方法调用
        verify(searchService).searchUsersWithCursor(any(), eq(lastId));
    }
    
    @Test
    void testSearchAll_Success() {
        // 准备测试数据
        String keyword = "综合搜索";
        Long lastPostId = 0L;
        Long lastUserId = 0L;
        Integer size = 20;
        String sortBy = "RELEVANCE";
        
        SearchResultDTO<Object> mockResult = createMockAllSearchResult();
        when(searchService.searchAllWithCursor(any(), eq(lastPostId), eq(lastUserId))).thenReturn(mockResult);
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchAll(keyword, lastPostId, lastUserId, size, sortBy);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证方法调用
        verify(searchService).searchAllWithCursor(any(), eq(lastPostId), eq(lastUserId));
    }
    
    @Test
    void testSearchPosts_InvalidKeyword() {
        // 准备无效关键词
        String keyword = ""; // 空关键词
        Long lastId = 0L;
        Integer size = 20;
        String sortBy = "RELEVANCE";
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchPosts(keyword, lastId, size, sortBy);
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证没有调用服务
        verify(searchService, never()).searchPostsWithCursor(any(), any());
    }
    
    @Test
    void testSearchPosts_InvalidSize() {
        // 准备无效大小
        String keyword = "测试";
        Long lastId = 0L;
        Integer size = 100; // 超过最大限制50
        String sortBy = "RELEVANCE";
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchPosts(keyword, lastId, size, sortBy);
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证没有调用服务
        verify(searchService, never()).searchPostsWithCursor(any(), any());
    }
    
    @Test
    void testGetHotSearchKeywords_Success() {
        // 准备测试数据
        Integer limit = 10;
        List<String> mockKeywords = List.of("热门1", "热门2", "热门3");
        when(searchService.getHotSearchKeywords(limit)).thenReturn(mockKeywords);
        
        // 执行测试
        ResponseEntity<?> response = searchController.getHotSearchKeywords(limit);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证方法调用
        verify(searchService).getHotSearchKeywords(limit);
    }
    
    @Test
    void testClearSearchCache_Success() {
        // 准备测试数据
        String keyword = "测试关键词";
        
        // 执行测试
        ResponseEntity<?> response = searchController.clearSearchCache(keyword);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证方法调用
        verify(searchService).clearSearchCache(keyword);
    }
    
    // ========== 辅助方法 ==========
    
    private SearchResultDTO<PostSearchVO> createMockPostSearchResult() {
        List<PostSearchVO> posts = new ArrayList<>();
        
        PostSearchVO post = new PostSearchVO();
        post.setId(1L);
        post.setTitle("测试帖子");
        post.setContentSummary("这是测试帖子的内容摘要");
        post.setAuthorId(1L);
        post.setAuthorUsername("作者");
        post.setCreatedAt(LocalDateTime.now());
        post.setLikeCount(10);
        post.setCollectCount(5);
        post.setCommentCount(3);
        post.setImageIds(new ArrayList<>());
        posts.add(post);
        
        SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
        result.setItems(posts);
        result.setTotal(-1L);
        result.setPage(0);
        result.setSize(20);
        result.setKeyword("测试关键词");
        result.setType("POST");
        result.setSortBy("RELEVANCE");
        result.setNextCursor("1");
        result.setHasMore(true);
        
        return result;
    }
    
    private SearchResultDTO<UserSearchVO> createMockUserSearchResult() {
        List<UserSearchVO> users = new ArrayList<>();
        
        UserSearchVO user = new UserSearchVO();
        user.setId(1L);
        user.setUsername("测试用户");
        user.setEmail("test@example.com");
        user.setBio("这是测试用户的个人简介");
        user.setLikedCount(100);
        user.setFollowersCount(50);
        user.setFollowingCount(30);
        user.setPostsCount(20);
        users.add(user);
        
        SearchResultDTO<UserSearchVO> result = new SearchResultDTO<>();
        result.setItems(users);
        result.setTotal(-1L);
        result.setPage(0);
        result.setSize(20);
        result.setKeyword("用户名");
        result.setType("USER");
        result.setSortBy("RELEVANCE");
        result.setNextCursor("1");
        result.setHasMore(true);
        
        return result;
    }
    
    private SearchResultDTO<Object> createMockAllSearchResult() {
        List<Object> items = new ArrayList<>();
        items.addAll(createMockPostSearchResult().getItems());
        items.addAll(createMockUserSearchResult().getItems());
        
        SearchResultDTO<Object> result = new SearchResultDTO<>();
        result.setItems(items);
        result.setTotal(-1L);
        result.setPage(0);
        result.setSize(20);
        result.setKeyword("综合搜索");
        result.setType("ALL");
        result.setSortBy("RELEVANCE");
        result.setNextCursor("{\"postCursor\":\"1\",\"userCursor\":\"1\"}");
        result.setHasMore(true);
        
        return result;
    }
}