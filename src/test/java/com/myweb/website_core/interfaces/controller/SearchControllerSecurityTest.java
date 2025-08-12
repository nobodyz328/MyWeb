package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.SearchService;
import com.myweb.website_core.application.service.security.SafeQueryService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.domain.business.dto.SearchResultDTO;
import com.myweb.website_core.domain.business.vo.PostSearchVO;
import com.myweb.website_core.domain.business.vo.UserSearchVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.quality.Strictness.LENIENT;

/**
 * SearchController 安全功能测试
 * <p>
 * 测试搜索功能的安全集成，包括：
 * - SQL注入防护
 * - 输入验证
 * - 结果安全过滤
 */
@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
class SearchControllerSecurityTest {
    
    @Mock
    private SearchService searchService;
    
    @Mock
    private SafeQueryService safeQueryService;
    
    @Mock
    private SqlInjectionProtectionService sqlInjectionProtectionService;
    
    @InjectMocks
    private SearchController searchController;
    
    @BeforeEach
    void setUp() {
        // 默认情况下，安全验证通过（使用lenient模式避免不必要的stubbing警告）
        lenient().doNothing().when(sqlInjectionProtectionService)
                .validateAndSanitizeInput(anyString(), anyString(), anyString());
        lenient().when(sqlInjectionProtectionService.detectSqlInjection(anyString(), anyString()))
                .thenReturn(false);
    }
    
    @Test
    void testSearchPosts_WithValidKeyword_Success() {
        // 准备测试数据
        String keyword = "正常搜索关键词";
        SearchResultDTO<PostSearchVO> mockResult = createMockPostSearchResult();
        when(searchService.searchPostsWithCursor(any(), anyLong())).thenReturn(mockResult);
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchPosts(keyword, 0L, 20, "RELEVANCE");
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证安全验证被调用
        verify(sqlInjectionProtectionService).validateAndSanitizeInput(keyword, "SEARCH", "keyword");
        verify(sqlInjectionProtectionService).validateAndSanitizeInput("RELEVANCE", "SORT", "sortBy");
    }
    
    @Test
    void testSearchPosts_WithSqlInjectionKeyword_Blocked() {
        // 准备SQL注入攻击关键词
        String maliciousKeyword = "test' OR '1'='1";
        
        // 模拟SQL注入检测
        doThrow(new ValidationException("搜索关键词包含潜在的SQL注入代码", "keyword", "SQL_INJECTION_DETECTED"))
                .when(sqlInjectionProtectionService)
                .validateAndSanitizeInput(eq(maliciousKeyword), eq("SEARCH"), eq("keyword"));
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchPosts(maliciousKeyword, 0L, 20, "RELEVANCE");
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证搜索服务没有被调用
        verify(searchService, never()).searchPostsWithCursor(any(), anyLong());
    }
    
    @Test
    void testSearchPosts_WithMaliciousSortBy_Blocked() {
        // 准备恶意排序参数
        String keyword = "正常关键词";
        String maliciousSortBy = "id; DROP TABLE posts--";
        
        // 模拟排序字段SQL注入检测
        doThrow(new ValidationException("排序字段包含潜在的SQL注入代码", "sortBy", "SQL_INJECTION_DETECTED"))
                .when(sqlInjectionProtectionService)
                .validateAndSanitizeInput(eq(maliciousSortBy), eq("SORT"), eq("sortBy"));
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchPosts(keyword, 0L, 20, maliciousSortBy);
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证搜索服务没有被调用
        verify(searchService, never()).searchPostsWithCursor(any(), anyLong());
    }
    
    @Test
    void testSearchUsers_WithValidKeyword_Success() {
        // 准备测试数据
        String keyword = "用户名";
        SearchResultDTO<UserSearchVO> mockResult = createMockUserSearchResult();
        when(searchService.searchUsersWithCursor(any(), anyLong())).thenReturn(mockResult);
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchUsers(keyword, 0L, 20, "RELEVANCE");
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证安全验证被调用
        verify(sqlInjectionProtectionService).validateAndSanitizeInput(keyword, "SEARCH", "keyword");
        verify(sqlInjectionProtectionService).validateAndSanitizeInput("RELEVANCE", "SORT", "sortBy");
    }
    
    @Test
    void testSearchUsers_WithSqlInjectionKeyword_Blocked() {
        // 准备SQL注入攻击关键词
        String maliciousKeyword = "admin' UNION SELECT * FROM users--";
        
        // 模拟SQL注入检测
        doThrow(new ValidationException("搜索关键词包含潜在的SQL注入代码", "keyword", "SQL_INJECTION_DETECTED"))
                .when(sqlInjectionProtectionService)
                .validateAndSanitizeInput(eq(maliciousKeyword), eq("SEARCH"), eq("keyword"));
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchUsers(maliciousKeyword, 0L, 20, "RELEVANCE");
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证搜索服务没有被调用
        verify(searchService, never()).searchUsersWithCursor(any(), anyLong());
    }
    
    @Test
    void testSearchPosts_WithMaliciousContent_FilteredOut() {
        // 准备包含恶意内容的搜索结果
        String keyword = "正常关键词";
        SearchResultDTO<PostSearchVO> mockResult = createMockPostSearchResultWithMaliciousContent();
        when(searchService.searchPostsWithCursor(any(), anyLong())).thenReturn(mockResult);
        
        // 模拟内容安全检测
        when(sqlInjectionProtectionService.detectSqlInjection(contains("恶意标题"), eq("CONTENT")))
                .thenReturn(true);
        when(sqlInjectionProtectionService.detectSqlInjection(argThat(s -> !s.contains("恶意标题")), eq("CONTENT")))
                .thenReturn(false);
        
        // 执行测试
        ResponseEntity<?> response = searchController.searchPosts(keyword, 0L, 20, "RELEVANCE");
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证安全过滤被执行
        verify(sqlInjectionProtectionService, atLeastOnce()).detectSqlInjection(anyString(), eq("CONTENT"));
    }
    
    @Test
    void testSearchPosts_EmptyKeyword_Blocked() {
        // 执行测试 - 空关键词
        ResponseEntity<?> response = searchController.searchPosts("", 0L, 20, "RELEVANCE");
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证搜索服务没有被调用
        verify(searchService, never()).searchPostsWithCursor(any(), anyLong());
    }
    
    @Test
    void testSearchPosts_InvalidSize_Blocked() {
        // 执行测试 - 无效大小
        ResponseEntity<?> response = searchController.searchPosts("测试", 0L, 100, "RELEVANCE"); // 超过最大限制50
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证搜索服务没有被调用
        verify(searchService, never()).searchPostsWithCursor(any(), anyLong());
    }
    
    // ========== 辅助方法 ==========
    
    private SearchResultDTO<PostSearchVO> createMockPostSearchResult() {
        List<PostSearchVO> posts = new ArrayList<>();
        
        PostSearchVO post = new PostSearchVO();
        post.setId(1L);
        post.setTitle("正常帖子标题");
        post.setContentSummary("这是正常的帖子内容摘要");
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
    
    private SearchResultDTO<PostSearchVO> createMockPostSearchResultWithMaliciousContent() {
        List<PostSearchVO> posts = new ArrayList<>();
        
        // 正常帖子
        PostSearchVO normalPost = new PostSearchVO();
        normalPost.setId(1L);
        normalPost.setTitle("正常帖子标题");
        normalPost.setContentSummary("正常内容");
        normalPost.setAuthorId(1L);
        normalPost.setAuthorUsername("作者");
        normalPost.setCreatedAt(LocalDateTime.now());
        normalPost.setLikeCount(10);
        normalPost.setCollectCount(5);
        normalPost.setCommentCount(3);
        normalPost.setImageIds(new ArrayList<>());
        posts.add(normalPost);
        
        // 恶意帖子
        PostSearchVO maliciousPost = new PostSearchVO();
        maliciousPost.setId(2L);
        maliciousPost.setTitle("恶意标题' OR '1'='1");
        maliciousPost.setContentSummary("恶意内容");
        maliciousPost.setAuthorId(2L);
        maliciousPost.setAuthorUsername("恶意用户");
        maliciousPost.setCreatedAt(LocalDateTime.now());
        maliciousPost.setLikeCount(0);
        maliciousPost.setCollectCount(0);
        maliciousPost.setCommentCount(0);
        maliciousPost.setImageIds(new ArrayList<>());
        posts.add(maliciousPost);
        
        SearchResultDTO<PostSearchVO> result = new SearchResultDTO<>();
        result.setItems(posts);
        result.setTotal(-1L);
        result.setPage(0);
        result.setSize(20);
        result.setKeyword("测试关键词");
        result.setType("POST");
        result.setSortBy("RELEVANCE");
        result.setNextCursor("2");
        result.setHasMore(false);
        
        return result;
    }
    
    private SearchResultDTO<UserSearchVO> createMockUserSearchResult() {
        List<UserSearchVO> users = new ArrayList<>();
        
        UserSearchVO user = new UserSearchVO();
        user.setId(1L);
        user.setUsername("正常用户名");
        user.setEmail("test@example.com");
        user.setBio("正常的个人简介");
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
}