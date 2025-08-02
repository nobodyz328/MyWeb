package com.myweb.website_core.application.service;

import com.myweb.website_core.common.exception.SearchException;
import com.myweb.website_core.domain.dto.SearchRequestDTO;
import com.myweb.website_core.domain.dto.SearchResultDTO;
import com.myweb.website_core.domain.vo.PostSearchVO;
import com.myweb.website_core.domain.vo.UserSearchVO;
import com.myweb.website_core.infrastructure.mapper.PostMapper;
import com.myweb.website_core.infrastructure.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * SearchService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {
    
    @Mock
    private PostMapper postMapper;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @InjectMocks
    private SearchService searchService;
    
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testSearchPosts_Success() {
        // 准备测试数据
        SearchRequestDTO request = new SearchRequestDTO();
        request.setKeyword("测试关键词");
        request.setType("POST");
        request.setPage(0);
        request.setSize(20);
        request.setSortBy("RELEVANCE");
        
        List<PostSearchVO> mockPosts = createMockPostSearchResults();
        Long mockTotal = 10L;
        
        // 模拟缓存未命中
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // 模拟数据库查询
        when(postMapper.searchPostsWithPagination(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockPosts);
        when(postMapper.countSearchPosts(anyString())).thenReturn(mockTotal);
        
        // 执行测试
        SearchResultDTO<PostSearchVO> result = searchService.searchPosts(request);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(mockPosts.size(), result.getItems().size());
        assertEquals(mockTotal, result.getTotal());
        assertEquals(request.getPage(), result.getPage());
        assertEquals(request.getSize(), result.getSize());
        assertEquals("测试关键词", result.getKeyword());
        
        // 验证方法调用
        verify(postMapper).searchPostsWithPagination(eq("测试关键词"), eq("RELEVANCE"), eq(0), eq(20));
        verify(postMapper).countSearchPosts(eq("测试关键词"));
        verify(valueOperations).set(anyString(), eq(result), anyLong(), any());
    }
    
    @Test
    void testSearchUsers_Success() {
        // 准备测试数据
        SearchRequestDTO request = new SearchRequestDTO();
        request.setKeyword("用户名");
        request.setType("USER");
        request.setPage(0);
        request.setSize(20);
        request.setSortBy("RELEVANCE");
        
        List<UserSearchVO> mockUsers = createMockUserSearchResults();
        Long mockTotal = 5L;
        
        // 模拟缓存未命中
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // 模拟数据库查询
        when(userMapper.searchUsersWithPagination(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockUsers);
        when(userMapper.countSearchUsers(anyString())).thenReturn(mockTotal);
        
        // 执行测试
        SearchResultDTO<UserSearchVO> result = searchService.searchUsers(request);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(mockUsers.size(), result.getItems().size());
        assertEquals(mockTotal, result.getTotal());
        assertEquals(request.getPage(), result.getPage());
        assertEquals(request.getSize(), result.getSize());
        assertEquals("用户名", result.getKeyword());
        
        // 验证方法调用
        verify(userMapper).searchUsersWithPagination(eq("用户名"), eq("RELEVANCE"), eq(0), eq(20));
        verify(userMapper).countSearchUsers(eq("用户名"));
        verify(valueOperations).set(anyString(), eq(result), anyLong(), any());
    }
    
    @Test
    void testSearchAll_Success() {
        // 准备测试数据
        SearchRequestDTO request = new SearchRequestDTO();
        request.setKeyword("综合搜索");
        request.setType("ALL");
        request.setPage(0);
        request.setSize(20);
        request.setSortBy("RELEVANCE");
        
        List<PostSearchVO> mockPosts = createMockPostSearchResults();
        List<UserSearchVO> mockUsers = createMockUserSearchResults();
        
        // 模拟缓存未命中
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // 模拟数据库查询
        when(postMapper.searchPostsWithPagination(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockPosts);
        when(postMapper.countSearchPosts(anyString())).thenReturn(10L);
        when(userMapper.searchUsersWithPagination(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockUsers);
        when(userMapper.countSearchUsers(anyString())).thenReturn(5L);
        
        // 执行测试
        SearchResultDTO<Object> result = searchService.searchAll(request);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(mockPosts.size() + mockUsers.size(), result.getItems().size());
        assertEquals(15L, result.getTotal()); // 10 + 5
        assertEquals("综合搜索", result.getKeyword());
    }
    
    @Test
    void testSearchPosts_WithCache() {
        // 准备测试数据
        SearchRequestDTO request = new SearchRequestDTO();
        request.setKeyword("缓存测试");
        request.setType("POST");
        request.setPage(0);
        request.setSize(20);
        request.setSortBy("RELEVANCE");
        
        SearchResultDTO<PostSearchVO> cachedResult = new SearchResultDTO<>();
        cachedResult.setItems(createMockPostSearchResults());
        cachedResult.setTotal(10L);
        cachedResult.setKeyword("缓存测试");
        
        // 模拟缓存命中
        when(valueOperations.get(anyString())).thenReturn(cachedResult);
        
        // 执行测试
        SearchResultDTO<PostSearchVO> result = searchService.searchPosts(request);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(cachedResult.getItems().size(), result.getItems().size());
        assertEquals(cachedResult.getTotal(), result.getTotal());
        
        // 验证没有调用数据库查询
        verify(postMapper, never()).searchPostsWithPagination(anyString(), anyString(), anyInt(), anyInt());
        verify(postMapper, never()).countSearchPosts(anyString());
    }
    
    @Test
    void testSearchPosts_InvalidKeyword() {
        // 准备无效的搜索请求
        SearchRequestDTO request = new SearchRequestDTO();
        request.setKeyword(""); // 空关键词
        request.setType("POST");
        request.setPage(0);
        request.setSize(20);
        request.setSortBy("RELEVANCE");
        
        // 执行测试并验证异常
        assertThrows(SearchException.class, () -> {
            searchService.searchPosts(request);
        });
    }
    
    @Test
    void testSearchPosts_DatabaseException() {
        // 准备测试数据
        SearchRequestDTO request = new SearchRequestDTO();
        request.setKeyword("异常测试");
        request.setType("POST");
        request.setPage(0);
        request.setSize(20);
        request.setSortBy("RELEVANCE");
        
        // 模拟缓存未命中
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // 模拟数据库异常
        when(postMapper.searchPostsWithPagination(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("数据库连接失败"));
        
        // 执行测试
        SearchResultDTO<PostSearchVO> result = searchService.searchPosts(request);
        
        // 验证返回空结果
        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0L, result.getTotal());
    }
    
    @Test
    void testClearSearchCache() {
        // 模拟Redis操作
        when(redisTemplate.keys(anyString())).thenReturn(null);
        
        // 执行测试
        assertDoesNotThrow(() -> {
            searchService.clearSearchCache("测试关键词");
            searchService.clearSearchCache(null);
        });
        
        // 验证方法调用
        verify(redisTemplate, times(2)).keys(anyString());
    }
    
    @Test
    void testGetHotSearchKeywords() {
        // 模拟Redis操作
        when(redisTemplate.opsForList()).thenReturn(mock(org.springframework.data.redis.core.ListOperations.class));
        when(redisTemplate.opsForList().range(anyString(), anyLong(), anyLong()))
                .thenReturn(List.of("热门1", "热门2", "热门3"));
        
        // 执行测试
        List<String> result = searchService.getHotSearchKeywords(10);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("热门1"));
        assertTrue(result.contains("热门2"));
        assertTrue(result.contains("热门3"));
    }
    
    // ========== 辅助方法 ==========
    
    private List<PostSearchVO> createMockPostSearchResults() {
        List<PostSearchVO> posts = new ArrayList<>();
        
        PostSearchVO post1 = new PostSearchVO();
        post1.setId(1L);
        post1.setTitle("测试帖子1");
        post1.setContentSummary("这是测试帖子1的内容摘要");
        post1.setAuthorId(1L);
        post1.setAuthorUsername("作者1");
        post1.setCreatedAt(LocalDateTime.now());
        post1.setLikeCount(10);
        post1.setCollectCount(5);
        post1.setCommentCount(3);
        post1.setImageIds(new ArrayList<>());
        posts.add(post1);
        
        PostSearchVO post2 = new PostSearchVO();
        post2.setId(2L);
        post2.setTitle("测试帖子2");
        post2.setContentSummary("这是测试帖子2的内容摘要");
        post2.setAuthorId(2L);
        post2.setAuthorUsername("作者2");
        post2.setCreatedAt(LocalDateTime.now());
        post2.setLikeCount(20);
        post2.setCollectCount(8);
        post2.setCommentCount(6);
        post2.setImageIds(new ArrayList<>());
        posts.add(post2);
        
        return posts;
    }
    
    private List<UserSearchVO> createMockUserSearchResults() {
        List<UserSearchVO> users = new ArrayList<>();
        
        UserSearchVO user1 = new UserSearchVO();
        user1.setId(1L);
        user1.setUsername("测试用户1");
        user1.setEmail("test1@example.com");
        user1.setBio("这是测试用户1的个人简介");
        user1.setLikedCount(100);
        user1.setFollowersCount(50);
        user1.setFollowingCount(30);
        user1.setPostsCount(20);
        users.add(user1);
        
        UserSearchVO user2 = new UserSearchVO();
        user2.setId(2L);
        user2.setUsername("测试用户2");
        user2.setEmail("test2@example.com");
        user2.setBio("这是测试用户2的个人简介");
        user2.setLikedCount(200);
        user2.setFollowersCount(80);
        user2.setFollowingCount(60);
        user2.setPostsCount(40);
        users.add(user2);
        
        return users;
    }
}