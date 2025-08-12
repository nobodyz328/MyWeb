package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.security.SafeQueryService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.domain.business.dto.PostDTO;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PostService安全查询功能单元测试
 * <p>
 * 测试安全查询服务集成功能：
 * 1. 安全搜索功能
 * 2. 安全分页查询
 * 3. 排序字段白名单验证
 * 4. SQL注入检测
 * <p>
 * 符合需求：5.2, 5.3, 5.5, 5.7 - 安全查询服务集成
 * 
 * @author MyWeb
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class PostServiceSecurityQueryTest {

    @Mock
    private PostRepository postRepository;
    
    @Mock
    private SafeQueryService safeQueryService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private DataIntegrityService dataIntegrityService;
    
    @InjectMocks
    private PostService postService;
    
    private Post testPost;
    private User testUser;
    private List<String> allowedSortFields;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        // 创建测试帖子
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("测试帖子");
        testPost.setContent("这是一个测试帖子的内容");
        testPost.setAuthor(testUser);
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setLikeCount(10);
        testPost.setCollectCount(5);
        testPost.setCommentCount(3);
        
        // 设置允许的排序字段
        allowedSortFields = Arrays.asList(
            "id", "title", "created_at", "like_count", "collect_count", "comment_count"
        );
    }
    
    @Test
    void testSearchPostsSafely_ValidInput_Success() {
        // 准备测试数据
        String keyword = "测试";
        String sortField = "created_at";
        String sortDirection = "DESC";
        int page = 0;
        int size = 10;
        
        List<Post> posts = Arrays.asList(testPost);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 1);
        
        // 模拟依赖服务
        when(safeQueryService.validateUserInputSafety(keyword, "SEARCH", "keyword")).thenReturn(true);
        when(safeQueryService.detectSqlInjectionAttempt(keyword, "POST_SEARCH")).thenReturn(false);
        when(safeQueryService.getAllowedSortFields("posts")).thenReturn(allowedSortFields);
        when(postRepository.findPostsWithSafeSearch(keyword, sortField, sortDirection, page, size))
            .thenReturn(postPage);
        
        // 执行测试
        Page<PostDTO> result = postService.searchPostsSafely(keyword, sortField, sortDirection, page, size);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("测试帖子", result.getContent().get(0).getTitle());
        
        // 验证方法调用
        verify(safeQueryService).validateUserInputSafety(keyword, "SEARCH", "keyword");
        verify(safeQueryService).detectSqlInjectionAttempt(keyword, "POST_SEARCH");
        verify(safeQueryService).getAllowedSortFields("posts");
        verify(postRepository).findPostsWithSafeSearch(keyword, sortField, sortDirection, page, size);
    }
    
    @Test
    void testSearchPostsSafely_SqlInjectionDetected_ThrowsException() {
        // 准备测试数据
        String maliciousKeyword = "'; DROP TABLE posts; --";
        String sortField = "created_at";
        String sortDirection = "DESC";
        int page = 0;
        int size = 10;
        
        // 模拟SQL注入检测
        when(safeQueryService.validateUserInputSafety(maliciousKeyword, "SEARCH", "keyword")).thenReturn(true);
        when(safeQueryService.detectSqlInjectionAttempt(maliciousKeyword, "POST_SEARCH")).thenReturn(true);
        
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            postService.searchPostsSafely(maliciousKeyword, sortField, sortDirection, page, size);
        });
        
        assertEquals("检测到潜在的SQL注入攻击", exception.getMessage());
        
        // 验证安全事件记录
        verify(auditLogService).logSecurityEvent(any(), eq("anonymous"), contains("SQL注入攻击"));
    }
    
    @Test
    void testSearchPostsSafely_InvalidSortField_ThrowsException() {
        // 准备测试数据
        String keyword = "测试";
        String invalidSortField = "malicious_field";
        String sortDirection = "DESC";
        int page = 0;
        int size = 10;
        
        // 模拟依赖服务
        when(safeQueryService.validateUserInputSafety(keyword, "SEARCH", "keyword")).thenReturn(true);
        when(safeQueryService.detectSqlInjectionAttempt(keyword, "POST_SEARCH")).thenReturn(false);
        when(safeQueryService.getAllowedSortFields("posts")).thenReturn(allowedSortFields);
        
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            postService.searchPostsSafely(keyword, invalidSortField, sortDirection, page, size);
        });
        
        assertEquals("不允许的排序字段: " + invalidSortField, exception.getMessage());
    }
    
    @Test
    void testSearchPostsSafely_InvalidInput_ThrowsException() {
        // 准备测试数据
        String invalidKeyword = "<script>alert('xss')</script>";
        String sortField = "created_at";
        String sortDirection = "DESC";
        int page = 0;
        int size = 10;
        
        // 模拟输入验证失败
        when(safeQueryService.validateUserInputSafety(invalidKeyword, "SEARCH", "keyword")).thenReturn(false);
        
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            postService.searchPostsSafely(invalidKeyword, sortField, sortDirection, page, size);
        });
        
        assertEquals("搜索关键词包含非法字符或潜在的安全威胁", exception.getMessage());
    }
    
    @Test
    void testGetPostsWithSafePagination_ValidParams_Success() {
        // 准备测试数据
        int page = 0;
        int size = 10;
        String sortField = "created_at";
        String sortDirection = "DESC";
        
        List<Post> posts = Arrays.asList(testPost);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 1);
        
        // 模拟依赖服务
        when(safeQueryService.getAllowedSortFields("posts")).thenReturn(allowedSortFields);
        when(postRepository.findSafePaginated(any(Map.class), eq(sortField), eq(sortDirection), eq(page), eq(size)))
            .thenReturn(postPage);
        
        // 执行测试
        Page<PostDTO> result = postService.getPostsWithSafePagination(page, size, sortField, sortDirection);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("测试帖子", result.getContent().get(0).getTitle());
        
        // 验证方法调用
        verify(safeQueryService).getAllowedSortFields("posts");
        verify(postRepository).findSafePaginated(any(Map.class), eq(sortField), eq(sortDirection), eq(page), eq(size));
    }
    
    @Test
    void testGetPostsWithSafePagination_InvalidPageParams_ThrowsException() {
        // 测试负数页码
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getPostsWithSafePagination(-1, 10, "created_at", "DESC");
        });
        assertEquals("页码不能为负数", exception1.getMessage());
        
        // 测试无效页大小
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getPostsWithSafePagination(0, 0, "created_at", "DESC");
        });
        assertEquals("页大小必须在1-100之间", exception2.getMessage());
        
        // 测试过大页大小
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getPostsWithSafePagination(0, 101, "created_at", "DESC");
        });
        assertEquals("页大小必须在1-100之间", exception3.getMessage());
    }
    
    @Test
    void testGetPostsByAuthorSafely_ValidAuthorId_Success() {
        // 准备测试数据
        Long authorId = 1L;
        int page = 0;
        int size = 10;
        
        List<Post> posts = Arrays.asList(testPost);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 1);
        
        // 模拟依赖服务
        when(postRepository.findPostsByAuthorWithSafePagination(authorId, page, size)).thenReturn(postPage);
        
        // 执行测试
        Page<PostDTO> result = postService.getPostsByAuthorSafely(authorId, page, size);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("测试帖子", result.getContent().get(0).getTitle());
        
        // 验证方法调用
        verify(postRepository).findPostsByAuthorWithSafePagination(authorId, page, size);
    }
    
    @Test
    void testGetPostsByAuthorSafely_InvalidAuthorId_ThrowsException() {
        // 测试null作者ID
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getPostsByAuthorSafely(null, 0, 10);
        });
        assertEquals("无效的作者ID", exception1.getMessage());
        
        // 测试负数作者ID
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getPostsByAuthorSafely(-1L, 0, 10);
        });
        assertEquals("无效的作者ID", exception2.getMessage());
        
        // 测试零作者ID
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getPostsByAuthorSafely(0L, 0, 10);
        });
        assertEquals("无效的作者ID", exception3.getMessage());
    }
    
    @Test
    void testGetTopLikedPostsSafely_ValidLimit_Success() {
        // 准备测试数据
        int limit = 10;
        List<Post> posts = Arrays.asList(testPost);
        
        // 模拟依赖服务
        when(postRepository.findTopLikedPostsSafely(limit)).thenReturn(posts);
        
        // 执行测试
        List<PostDTO> result = postService.getTopLikedPostsSafely(limit);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试帖子", result.get(0).getTitle());
        
        // 验证方法调用
        verify(postRepository).findTopLikedPostsSafely(limit);
    }
    
    @Test
    void testGetTopLikedPostsSafely_InvalidLimit_ThrowsException() {
        // 测试负数限制
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getTopLikedPostsSafely(-1);
        });
        assertEquals("限制数量必须在1-100之间", exception1.getMessage());
        
        // 测试零限制
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getTopLikedPostsSafely(0);
        });
        assertEquals("限制数量必须在1-100之间", exception2.getMessage());
        
        // 测试过大限制
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            postService.getTopLikedPostsSafely(101);
        });
        assertEquals("限制数量必须在1-100之间", exception3.getMessage());
    }
    
    @Test
    void testFindPostsByConditionsSafely_ValidConditions_Success() {
        // 准备测试数据
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("author.id", 1L);
        conditions.put("title", "测试");
        String sortField = "created_at";
        String sortDirection = "DESC";
        
        List<Post> posts = Arrays.asList(testPost);
        
        // 模拟依赖服务
        when(safeQueryService.validateUserInputSafety("测试", "CONDITION", "title")).thenReturn(true);
        when(safeQueryService.getAllowedSortFields("posts")).thenReturn(allowedSortFields);
        when(postRepository.findPostsByConditionsSafely(conditions, sortField, sortDirection)).thenReturn(posts);
        
        // 执行测试
        List<PostDTO> result = postService.findPostsByConditionsSafely(conditions, sortField, sortDirection);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试帖子", result.get(0).getTitle());
        
        // 验证方法调用
        verify(safeQueryService).validateUserInputSafety("测试", "CONDITION", "title");
        verify(safeQueryService).getAllowedSortFields("posts");
        verify(postRepository).findPostsByConditionsSafely(conditions, sortField, sortDirection);
    }
    
    @Test
    void testFindPostsByConditionsSafely_InvalidStringCondition_ThrowsException() {
        // 准备测试数据
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("title", "'; DROP TABLE posts; --");
        String sortField = "created_at";
        String sortDirection = "DESC";
        
        // 模拟输入验证失败
        when(safeQueryService.validateUserInputSafety("'; DROP TABLE posts; --", "CONDITION", "title"))
            .thenReturn(false);
        
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            postService.findPostsByConditionsSafely(conditions, sortField, sortDirection);
        });
        
        assertTrue(exception.getMessage().contains("查询条件包含非法字符"));
    }
    
    @Test
    void testGetAllowedSortFields_ReturnsCorrectFields() {
        // 模拟依赖服务
        when(safeQueryService.getAllowedSortFields("posts")).thenReturn(allowedSortFields);
        
        // 执行测试
        List<String> result = postService.getAllowedSortFields();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(allowedSortFields.size(), result.size());
        assertTrue(result.contains("id"));
        assertTrue(result.contains("title"));
        assertTrue(result.contains("created_at"));
        
        // 验证方法调用
        verify(safeQueryService).getAllowedSortFields("posts");
    }
    
    @Test
    void testAddAllowedSortFields_AddsFieldsSuccessfully() {
        // 准备测试数据
        List<String> newFields = Arrays.asList("updated_at", "view_count");
        
        // 执行测试
        postService.addAllowedSortFields(newFields);
        
        // 验证方法调用
        verify(safeQueryService).addAllowedSortFields("posts", newFields);
    }
    
    @Test
    void testSearchPostsSafely_EmptyKeyword_Success() {
        // 准备测试数据
        String keyword = "";
        String sortField = "created_at";
        String sortDirection = "DESC";
        int page = 0;
        int size = 10;
        
        List<Post> posts = Arrays.asList(testPost);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 1);
        
        // 模拟依赖服务
        when(safeQueryService.getAllowedSortFields("posts")).thenReturn(allowedSortFields);
        when(postRepository.findPostsWithSafeSearch(keyword, sortField, sortDirection, page, size))
            .thenReturn(postPage);
        
        // 执行测试
        Page<PostDTO> result = postService.searchPostsSafely(keyword, sortField, sortDirection, page, size);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        // 验证不会调用输入验证（因为关键词为空）
        verify(safeQueryService, never()).validateUserInputSafety(anyString(), anyString(), anyString());
        verify(safeQueryService, never()).detectSqlInjectionAttempt(anyString(), anyString());
    }
    
    @Test
    void testSearchPostsSafely_NullSortField_Success() {
        // 准备测试数据
        String keyword = "测试";
        String sortField = null;
        String sortDirection = "DESC";
        int page = 0;
        int size = 10;
        
        List<Post> posts = Arrays.asList(testPost);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 1);
        
        // 模拟依赖服务
        when(safeQueryService.validateUserInputSafety(keyword, "SEARCH", "keyword")).thenReturn(true);
        when(safeQueryService.detectSqlInjectionAttempt(keyword, "POST_SEARCH")).thenReturn(false);
        when(postRepository.findPostsWithSafeSearch(keyword, sortField, sortDirection, page, size))
            .thenReturn(postPage);
        
        // 执行测试
        Page<PostDTO> result = postService.searchPostsSafely(keyword, sortField, sortDirection, page, size);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        // 验证不会调用排序字段验证（因为排序字段为null）
        verify(safeQueryService, never()).getAllowedSortFields(anyString());
    }
}