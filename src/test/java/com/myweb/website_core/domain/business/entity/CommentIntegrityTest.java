package com.myweb.website_core.domain.business.entity;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.infrastructure.config.ApplicationContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comment实体数据完整性功能单元测试
 * <p>
 * 测试Comment实体的数据完整性相关功能：
 * - 内容哈希自动计算
 * - 完整性验证
 * - 哈希重新计算判断
 * - 异常处理
 * <p>
 * 符合需求 3.1, 3.4 的测试要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@ExtendWith(MockitoExtension.class)
class CommentIntegrityTest {
    
    @Mock
    private DataIntegrityService dataIntegrityService;
    
    private Comment comment;
    private Post post;
    private User author;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        author = new User();
        author.setId(1L);
        author.setUsername("testuser");
        
        // 创建测试帖子
        post = new Post();
        post.setId(1L);
        post.setTitle("测试帖子");
        post.setAuthor(author);
        
        // 创建测试评论
        comment = new Comment();
        comment.setId(1L);
        comment.setContent("这是一个测试评论，用于验证数据完整性功能。");
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCalculateContentHash_Success() {
        // Given
        String expectedHash = "test-comment-hash";
        LocalDateTime beforeCalculation = LocalDateTime.now();
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(anyString())).thenReturn(expectedHash);
            
            // When
            comment.calculateContentHash();
            
            // Then
            assertEquals(expectedHash, comment.getContentHash());
            assertNotNull(comment.getHashCalculatedAt());
            assertTrue(comment.getHashCalculatedAt().isAfter(beforeCalculation) || 
                      comment.getHashCalculatedAt().isEqual(beforeCalculation));
            
            verify(dataIntegrityService).calculateHash(comment.getContent());
        }
    }
    
    @Test
    void testCalculateContentHash_NullContent() {
        // Given
        comment.setContent(null);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            // When
            comment.calculateContentHash();
            
            // Then
            assertNull(comment.getContentHash());
            assertNull(comment.getHashCalculatedAt());
            
            verify(dataIntegrityService, never()).calculateHash(anyString());
        }
    }
    
    @Test
    void testCalculateContentHash_ServiceUnavailable() {
        // Given
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenThrow(new RuntimeException("Service unavailable"));
            
            // When
            comment.calculateContentHash();
            
            // Then
            assertNull(comment.getContentHash());
            assertNull(comment.getHashCalculatedAt());
        }
    }
    
    @Test
    void testVerifyContentIntegrity_Success() {
        // Given
        String contentHash = "valid-comment-hash";
        comment.setContentHash(contentHash);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(comment.getContent(), contentHash))
                .thenReturn(true);
            
            // When
            boolean result = comment.verifyContentIntegrity();
            
            // Then
            assertTrue(result);
            verify(dataIntegrityService).verifyIntegrity(comment.getContent(), contentHash);
        }
    }
    
    @Test
    void testVerifyContentIntegrity_Failed() {
        // Given
        String contentHash = "invalid-comment-hash";
        comment.setContentHash(contentHash);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(comment.getContent(), contentHash))
                .thenReturn(false);
            
            // When
            boolean result = comment.verifyContentIntegrity();
            
            // Then
            assertFalse(result);
            verify(dataIntegrityService).verifyIntegrity(comment.getContent(), contentHash);
        }
    }
    
    @Test
    void testVerifyContentIntegrity_NullContent() {
        // Given
        comment.setContent(null);
        comment.setContentHash("some-hash");
        
        // When
        boolean result = comment.verifyContentIntegrity();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testVerifyContentIntegrity_NullHash() {
        // Given
        comment.setContentHash(null);
        
        // When
        boolean result = comment.verifyContentIntegrity();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testVerifyContentIntegrity_ServiceException() {
        // Given
        String contentHash = "test-hash";
        comment.setContentHash(contentHash);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenThrow(new RuntimeException("Service error"));
            
            // When
            boolean result = comment.verifyContentIntegrity();
            
            // Then
            assertFalse(result);
        }
    }
    
    @Test
    void testNeedsHashRecalculation_NullHash() {
        // Given
        comment.setContentHash(null);
        comment.setHashCalculatedAt(LocalDateTime.now());
        
        // When
        boolean result = comment.needsHashRecalculation();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNeedsHashRecalculation_NullCalculatedAt() {
        // Given
        comment.setContentHash("some-hash");
        comment.setHashCalculatedAt(null);
        
        // When
        boolean result = comment.needsHashRecalculation();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNeedsHashRecalculation_OldHash() {
        // Given
        comment.setContentHash("some-hash");
        comment.setHashCalculatedAt(LocalDateTime.now().minusDays(31));
        
        // When
        boolean result = comment.needsHashRecalculation();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNeedsHashRecalculation_RecentHash() {
        // Given
        comment.setContentHash("some-hash");
        comment.setHashCalculatedAt(LocalDateTime.now().minusDays(15));
        
        // When
        boolean result = comment.needsHashRecalculation();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testNeedsHashRecalculation_CurrentHash() {
        // Given
        comment.setContentHash("some-hash");
        comment.setHashCalculatedAt(LocalDateTime.now());
        
        // When
        boolean result = comment.needsHashRecalculation();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testCommentConstructors() {
        // Test default constructor
        Comment newComment = new Comment();
        assertNotNull(newComment.getCreatedAt());
        
        // Test constructor with content, post, and author
        Comment commentWithParams = new Comment("Test content", post, author);
        assertEquals("Test content", commentWithParams.getContent());
        assertEquals(post, commentWithParams.getPost());
        assertEquals(author, commentWithParams.getAuthor());
        assertNotNull(commentWithParams.getCreatedAt());
        assertNull(commentWithParams.getParent());
        
        // Test constructor with parent comment
        Comment parentComment = new Comment("Parent comment", post, author);
        Comment replyComment = new Comment("Reply content", post, author, parentComment);
        assertEquals("Reply content", replyComment.getContent());
        assertEquals(post, replyComment.getPost());
        assertEquals(author, replyComment.getAuthor());
        assertEquals(parentComment, replyComment.getParent());
        assertNotNull(replyComment.getCreatedAt());
    }
    
    @Test
    void testContentHashSettersAndGetters() {
        // Given
        String testHash = "test-comment-hash-value";
        LocalDateTime testTime = LocalDateTime.now();
        
        // When
        comment.setContentHash(testHash);
        comment.setHashCalculatedAt(testTime);
        
        // Then
        assertEquals(testHash, comment.getContentHash());
        assertEquals(testTime, comment.getHashCalculatedAt());
    }
    
    @Test
    void testIntegrityFieldsInitialState() {
        // Given
        Comment newComment = new Comment();
        
        // When & Then
        assertNull(newComment.getContentHash());
        assertNull(newComment.getHashCalculatedAt());
        assertTrue(newComment.needsHashRecalculation());
        assertFalse(newComment.verifyContentIntegrity());
    }
    
    @Test
    void testPrePersistCallback() {
        // Given
        Comment newComment = new Comment();
        newComment.setContent("New comment content");
        
        String expectedHash = "new-comment-hash";
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(anyString())).thenReturn(expectedHash);
            
            // When
            newComment.onCreate(); // Simulate @PrePersist callback
            
            // Then
            assertNotNull(newComment.getCreatedAt());
            assertEquals(expectedHash, newComment.getContentHash());
            assertNotNull(newComment.getHashCalculatedAt());
        }
    }
    
    @Test
    void testPreUpdateCallback() {
        // Given
        comment.setUpdatedAt(null);
        String expectedHash = "updated-comment-hash";
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(anyString())).thenReturn(expectedHash);
            
            // When
            comment.onUpdate(); // Simulate @PreUpdate callback
            
            // Then
            assertNotNull(comment.getUpdatedAt());
            assertEquals(expectedHash, comment.getContentHash());
            assertNotNull(comment.getHashCalculatedAt());
        }
    }
}