package com.myweb.website_core.domain.business.listener;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.common.exception.DataIntegrityException;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.config.ApplicationContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DataIntegrityListener单元测试
 * <p>
 * 测试数据完整性监听器的功能：
 * - 实体持久化前的哈希计算
 * - 实体更新前的完整性验证
 * - 持久化和更新后的日志记录
 * - 异常处理
 * <p>
 * 符合需求 3.1, 3.4 的测试要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@ExtendWith(MockitoExtension.class)
class DataIntegrityListenerTest {
    
    @Mock
    private DataIntegrityService dataIntegrityService;
    
    @InjectMocks
    private DataIntegrityListener dataIntegrityListener;
    
    private Post post;
    private Comment comment;
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
        post.setTitle("测试帖子标题");
        post.setContent("这是一个测试帖子的内容。");
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
        
        // 创建测试评论
        comment = new Comment();
        comment.setId(1L);
        comment.setContent("这是一个测试评论。");
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());
    }
    
    // ==================== PrePersist 测试 ====================
    
    @Test
    void testPrePersist_Post_Success() {
        // Given
        String expectedHash = "test-post-hash";
        LocalDateTime beforePersist = LocalDateTime.now();
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(post.getContent())).thenReturn(expectedHash);
            
            // When
            dataIntegrityListener.prePersist(post);
            
            // Then
            assertEquals(expectedHash, post.getContentHash());
            assertNotNull(post.getHashCalculatedAt());
            assertTrue(post.getHashCalculatedAt().isAfter(beforePersist) || 
                      post.getHashCalculatedAt().isEqual(beforePersist));
            
            verify(dataIntegrityService).calculateHash(post.getContent());
        }
    }
    
    @Test
    void testPrePersist_Comment_Success() {
        // Given
        String expectedHash = "test-comment-hash";
        LocalDateTime beforePersist = LocalDateTime.now();
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(comment.getContent())).thenReturn(expectedHash);
            
            // When
            dataIntegrityListener.prePersist(comment);
            
            // Then
            assertEquals(expectedHash, comment.getContentHash());
            assertNotNull(comment.getHashCalculatedAt());
            assertTrue(comment.getHashCalculatedAt().isAfter(beforePersist) || 
                      comment.getHashCalculatedAt().isEqual(beforePersist));
            
            verify(dataIntegrityService).calculateHash(comment.getContent());
        }
    }
    
    @Test
    void testPrePersist_Post_NullContent() {
        // Given
        post.setContent(null);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            // When
            dataIntegrityListener.prePersist(post);
            
            // Then
            assertNull(post.getContentHash());
            assertNull(post.getHashCalculatedAt());
            
            verify(dataIntegrityService, never()).calculateHash(anyString());
        }
    }
    
    @Test
    void testPrePersist_ServiceUnavailable() {
        // Given
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenThrow(new RuntimeException("Service unavailable"));
            
            // When & Then - 不应该抛出异常
            assertDoesNotThrow(() -> dataIntegrityListener.prePersist(post));
            
            // 哈希值应该保持为null
            assertNull(post.getContentHash());
            assertNull(post.getHashCalculatedAt());
        }
    }
    
    @Test
    void testPrePersist_UnsupportedEntity() {
        // Given
        String unsupportedEntity = "This is not a supported entity";
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.prePersist(unsupportedEntity));
    }
    
    // ==================== PreUpdate 测试 ====================
    
    @Test
    void testPreUpdate_Post_Success() {
        // Given
        String oldHash = "old-hash";
        String newHash = "new-hash";
        post.setContentHash(oldHash);
        post.setHashCalculatedAt(LocalDateTime.now().minusMinutes(10));
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(post.getContent(), oldHash)).thenReturn(true);
            when(dataIntegrityService.calculateHash(post.getContent())).thenReturn(newHash);
            
            // When
            dataIntegrityListener.preUpdate(post);
            
            // Then
            assertEquals(newHash, post.getContentHash());
            assertNotNull(post.getHashCalculatedAt());
            
            verify(dataIntegrityService).verifyIntegrity(post.getContent(), oldHash);
            verify(dataIntegrityService).calculateHash(post.getContent());
        }
    }
    
    @Test
    void testPreUpdate_Comment_Success() {
        // Given
        String oldHash = "old-comment-hash";
        String newHash = "new-comment-hash";
        comment.setContentHash(oldHash);
        comment.setHashCalculatedAt(LocalDateTime.now().minusMinutes(10));
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(comment.getContent(), oldHash)).thenReturn(true);
            when(dataIntegrityService.calculateHash(comment.getContent())).thenReturn(newHash);
            
            // When
            dataIntegrityListener.preUpdate(comment);
            
            // Then
            assertEquals(newHash, comment.getContentHash());
            assertNotNull(comment.getHashCalculatedAt());
            
            verify(dataIntegrityService).verifyIntegrity(comment.getContent(), oldHash);
            verify(dataIntegrityService).calculateHash(comment.getContent());
        }
    }
    
    @Test
    void testPreUpdate_Post_IntegrityVerificationFailed() {
        // Given
        String oldHash = "old-hash";
        String newHash = "new-hash";
        post.setContentHash(oldHash);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(post.getContent(), oldHash)).thenReturn(false);
            when(dataIntegrityService.calculateHash(post.getContent())).thenReturn(newHash);
            
            // When - 不应该抛出异常（警告模式）
            assertDoesNotThrow(() -> dataIntegrityListener.preUpdate(post));
            
            // Then - 仍然应该更新哈希值
            assertEquals(newHash, post.getContentHash());
            assertNotNull(post.getHashCalculatedAt());
            
            verify(dataIntegrityService).verifyIntegrity(post.getContent(), oldHash);
            verify(dataIntegrityService).calculateHash(post.getContent());
        }
    }
    
    @Test
    void testPreUpdate_Post_NullContent() {
        // Given
        post.setContent(null);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            // When
            dataIntegrityListener.preUpdate(post);
            
            // Then
            verify(dataIntegrityService, never()).verifyIntegrity(anyString(), anyString());
            verify(dataIntegrityService, never()).calculateHash(anyString());
        }
    }
    
    @Test
    void testPreUpdate_Post_NullHash() {
        // Given
        post.setContentHash(null);
        String newHash = "new-hash";
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(post.getContent())).thenReturn(newHash);
            
            // When
            dataIntegrityListener.preUpdate(post);
            
            // Then
            assertEquals(newHash, post.getContentHash());
            assertNotNull(post.getHashCalculatedAt());
            
            verify(dataIntegrityService, never()).verifyIntegrity(anyString(), anyString());
            verify(dataIntegrityService).calculateHash(post.getContent());
        }
    }
    
    @Test
    void testPreUpdate_ServiceUnavailable() {
        // Given
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(null);
            
            // When & Then - 不应该抛出异常
            assertDoesNotThrow(() -> dataIntegrityListener.preUpdate(post));
        }
    }
    
    // ==================== PostPersist 测试 ====================
    
    @Test
    void testPostPersist_Post() {
        // Given
        post.setContentHash("test-hash");
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postPersist(post));
    }
    
    @Test
    void testPostPersist_Comment() {
        // Given
        comment.setContentHash("test-comment-hash");
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postPersist(comment));
    }
    
    @Test
    void testPostPersist_UnsupportedEntity() {
        // Given
        String unsupportedEntity = "Unsupported entity";
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postPersist(unsupportedEntity));
    }
    
    // ==================== PostUpdate 测试 ====================
    
    @Test
    void testPostUpdate_Post() {
        // Given
        post.setContentHash("updated-hash");
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postUpdate(post));
    }
    
    @Test
    void testPostUpdate_Comment() {
        // Given
        comment.setContentHash("updated-comment-hash");
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postUpdate(comment));
    }
    
    @Test
    void testPostUpdate_UnsupportedEntity() {
        // Given
        String unsupportedEntity = "Unsupported entity";
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postUpdate(unsupportedEntity));
    }
    
    // ==================== 异常处理测试 ====================
    
    @Test
    void testPrePersist_ExceptionHandling() {
        // Given
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(anyString()))
                .thenThrow(new RuntimeException("Hash calculation failed"));
            
            // When & Then - 不应该抛出异常
            assertDoesNotThrow(() -> dataIntegrityListener.prePersist(post));
            
            // 哈希值应该保持为null
            assertNull(post.getContentHash());
            assertNull(post.getHashCalculatedAt());
        }
    }
    
    @Test
    void testPreUpdate_ExceptionHandling() {
        // Given
        post.setContentHash("old-hash");
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(anyString(), anyString()))
                .thenThrow(new RuntimeException("Verification failed"));
            
            // When & Then - 不应该抛出异常（非DataIntegrityException）
            assertDoesNotThrow(() -> dataIntegrityListener.preUpdate(post));
        }
    }
    
    @Test
    void testPostPersist_ExceptionHandling() {
        // Given
        Post nullIdPost = new Post();
        nullIdPost.setContentHash("test-hash");
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postPersist(nullIdPost));
    }
    
    @Test
    void testPostUpdate_ExceptionHandling() {
        // Given
        Comment nullIdComment = new Comment();
        nullIdComment.setContentHash("test-hash");
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> dataIntegrityListener.postUpdate(nullIdComment));
    }
}