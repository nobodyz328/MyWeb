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
 * Post实体数据完整性功能单元测试
 * <p>
 * 测试Post实体的数据完整性相关功能：
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
class PostIntegrityTest {
    
    @Mock
    private DataIntegrityService dataIntegrityService;
    
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
        post.setTitle("测试帖子标题");
        post.setContent("这是一个测试帖子的内容，用于验证数据完整性功能。");
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCalculateContentHash_Success() {
        // Given
        String expectedHash = "test-hash-value";
        LocalDateTime beforeCalculation = LocalDateTime.now();
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.calculateHash(anyString())).thenReturn(expectedHash);
            
            // When
            post.calculateContentHash();
            
            // Then
            assertEquals(expectedHash, post.getContentHash());
            assertNotNull(post.getHashCalculatedAt());
            assertTrue(post.getHashCalculatedAt().isAfter(beforeCalculation) || 
                      post.getHashCalculatedAt().isEqual(beforeCalculation));
            
            verify(dataIntegrityService).calculateHash(post.getContent());
        }
    }
    
    @Test
    void testCalculateContentHash_NullContent() {
        // Given
        post.setContent(null);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            // When
            post.calculateContentHash();
            
            // Then
            assertNull(post.getContentHash());
            assertNull(post.getHashCalculatedAt());
            
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
            post.calculateContentHash();
            
            // Then
            assertNull(post.getContentHash());
            assertNull(post.getHashCalculatedAt());
        }
    }
    
    @Test
    void testVerifyContentIntegrity_Success() {
        // Given
        String contentHash = "valid-hash";
        post.setContentHash(contentHash);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(post.getContent(), contentHash))
                .thenReturn(true);
            
            // When
            boolean result = post.verifyContentIntegrity();
            
            // Then
            assertTrue(result);
            verify(dataIntegrityService).verifyIntegrity(post.getContent(), contentHash);
        }
    }
    
    @Test
    void testVerifyContentIntegrity_Failed() {
        // Given
        String contentHash = "invalid-hash";
        post.setContentHash(contentHash);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenReturn(dataIntegrityService);
            
            when(dataIntegrityService.verifyIntegrity(post.getContent(), contentHash))
                .thenReturn(false);
            
            // When
            boolean result = post.verifyContentIntegrity();
            
            // Then
            assertFalse(result);
            verify(dataIntegrityService).verifyIntegrity(post.getContent(), contentHash);
        }
    }
    
    @Test
    void testVerifyContentIntegrity_NullContent() {
        // Given
        post.setContent(null);
        post.setContentHash("some-hash");
        
        // When
        boolean result = post.verifyContentIntegrity();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testVerifyContentIntegrity_NullHash() {
        // Given
        post.setContentHash(null);
        
        // When
        boolean result = post.verifyContentIntegrity();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testVerifyContentIntegrity_ServiceException() {
        // Given
        String contentHash = "test-hash";
        post.setContentHash(contentHash);
        
        try (MockedStatic<ApplicationContextProvider> mockedProvider = 
             mockStatic(ApplicationContextProvider.class)) {
            
            mockedProvider.when(() -> ApplicationContextProvider.getBean(DataIntegrityService.class))
                         .thenThrow(new RuntimeException("Service error"));
            
            // When
            boolean result = post.verifyContentIntegrity();
            
            // Then
            assertFalse(result);
        }
    }
    
    @Test
    void testNeedsHashRecalculation_NullHash() {
        // Given
        post.setContentHash(null);
        post.setHashCalculatedAt(LocalDateTime.now());
        
        // When
        boolean result = post.needsHashRecalculation();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNeedsHashRecalculation_NullCalculatedAt() {
        // Given
        post.setContentHash("some-hash");
        post.setHashCalculatedAt(null);
        
        // When
        boolean result = post.needsHashRecalculation();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNeedsHashRecalculation_OldHash() {
        // Given
        post.setContentHash("some-hash");
        post.setHashCalculatedAt(LocalDateTime.now().minusDays(31));
        
        // When
        boolean result = post.needsHashRecalculation();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testNeedsHashRecalculation_RecentHash() {
        // Given
        post.setContentHash("some-hash");
        post.setHashCalculatedAt(LocalDateTime.now().minusDays(15));
        
        // When
        boolean result = post.needsHashRecalculation();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testNeedsHashRecalculation_CurrentHash() {
        // Given
        post.setContentHash("some-hash");
        post.setHashCalculatedAt(LocalDateTime.now());
        
        // When
        boolean result = post.needsHashRecalculation();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testGetImageIds_EmptyImages() {
        // Given
        post.setImages(null);
        
        // When
        var imageIds = post.getImageIds();
        
        // Then
        assertNotNull(imageIds);
        assertTrue(imageIds.isEmpty());
    }
    
    @Test
    void testContentHashSettersAndGetters() {
        // Given
        String testHash = "test-hash-value";
        LocalDateTime testTime = LocalDateTime.now();
        
        // When
        post.setContentHash(testHash);
        post.setHashCalculatedAt(testTime);
        
        // Then
        assertEquals(testHash, post.getContentHash());
        assertEquals(testTime, post.getHashCalculatedAt());
    }
    
    @Test
    void testIntegrityFieldsInitialState() {
        // Given
        Post newPost = new Post();
        
        // When & Then
        assertNull(newPost.getContentHash());
        assertNull(newPost.getHashCalculatedAt());
        assertTrue(newPost.needsHashRecalculation());
        assertFalse(newPost.verifyContentIntegrity());
    }
}