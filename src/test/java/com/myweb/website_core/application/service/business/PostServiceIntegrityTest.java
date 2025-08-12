package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.exception.DataIntegrityException;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.quality.Strictness.LENIENT;

/**
 * PostService数据完整性集成测试
 * 
 * 测试PostService中数据完整性检查功能的集成，包括：
 * - 帖子编辑时的完整性验证
 * - 帖子删除时的完整性验证
 * - 完整性验证失败的处理
 * - 审计日志记录
 * 
 * @author MyWeb Security Team
 * @version 1.0
 */
@MockitoSettings(strictness = LENIENT)
@DisplayName("PostService数据完整性集成测试")
public class PostServiceIntegrityTest {

    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DataIntegrityService dataIntegrityService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private PostLikeService postLikeService;
    
    private PostService postService;
    
    private User testUser;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建PostService实例，注入必要的依赖
        postService = new PostService(
            postRepository,
            userRepository,
            null, // messageProducerService
            null, // postMapper
            null, // redisTemplate
            null, // postCollectRepository
            postLikeService,
            null, // imageService
            dataIntegrityService,
            auditLogService,
            null  // safeQueryService
                ,null
        );
        
        // 初始化测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("测试帖子");
        testPost.setContent("这是测试内容");
        testPost.setContentHash("original_hash");
        testPost.setHashCalculatedAt(LocalDateTime.now());
        testPost.setAuthor(testUser);
    }
    
    @Test
    @DisplayName("编辑帖子时完整性验证通过")
    void testEditPost_IntegrityCheckPassed() {
        // Arrange
        Long postId = 1L;
        Post updatedPost = new Post();
        updatedPost.setTitle("更新后的标题");
        updatedPost.setContent("更新后的内容");
        updatedPost.setAuthor(testUser);
        
        DataIntegrityService.IntegrityCheckResult validResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(true)
                .actualHash("original_hash")
                .expectedHash("original_hash")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(dataIntegrityService.checkPostIntegrity(postId, testPost.getContent(), testPost.getContentHash()))
            .thenReturn(validResult);
        when(dataIntegrityService.calculateHash(updatedPost.getContent())).thenReturn("new_hash");
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // Act
        Post result = postService.editPost(postId, updatedPost);
        
        // Assert
        assertNotNull(result);
        assertEquals("更新后的标题", testPost.getTitle());
        assertEquals("更新后的内容", testPost.getContent());
        assertEquals("new_hash", testPost.getContentHash());
        assertNotNull(testPost.getHashCalculatedAt());
        
        // 验证完整性检查被调用
        verify(dataIntegrityService).checkPostIntegrity(postId, testPost.getContent(), "original_hash");
        
        // 验证审计日志记录
        verify(auditLogService).logSecurityEvent(
            eq(AuditOperation.INTEGRITY_CHECK),
            eq("testuser"),
            contains("帖子编辑前完整性验证通过")
        );
        
        verify(auditLogService).logSecurityEvent(
            eq(AuditOperation.POST_UPDATE),
            eq("testuser"),
            contains("帖子编辑成功")
        );
    }
    
    @Test
    @DisplayName("编辑帖子时完整性验证失败")
    void testEditPost_IntegrityCheckFailed() {
        // Arrange
        Long postId = 1L;
        Post updatedPost = new Post();
        updatedPost.setTitle("更新后的标题");
        updatedPost.setContent("更新后的内容");
        updatedPost.setAuthor(testUser);
        
        DataIntegrityService.IntegrityCheckResult invalidResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(false)
                .actualHash("tampered_hash")
                .expectedHash("original_hash")
                .errorMessage("帖子内容哈希值不匹配，可能已被篡改")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(dataIntegrityService.checkPostIntegrity(postId, testPost.getContent(), testPost.getContentHash()))
            .thenReturn(invalidResult);
        
        // Act & Assert
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            postService.editPost(postId, updatedPost);
        });
        
        assertTrue(exception.getMessage().contains("帖子内容完整性验证失败"));
        
        // 验证完整性检查被调用
        verify(dataIntegrityService).checkPostIntegrity(postId, testPost.getContent(), "original_hash");
        
        // 验证审计日志记录失败事件
        verify(auditLogService).logSecurityEvent(
            eq(AuditOperation.INTEGRITY_CHECK),
            eq("testuser"),
            contains("帖子编辑时完整性验证失败")
        );
        
        // 验证没有保存帖子
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    @DisplayName("编辑帖子时无哈希值跳过完整性检查")
    void testEditPost_NoHashSkipIntegrityCheck() {
        // Arrange
        Long postId = 1L;
        testPost.setContentHash(null); // 没有哈希值
        
        Post updatedPost = new Post();
        updatedPost.setTitle("更新后的标题");
        updatedPost.setContent("更新后的内容");
        updatedPost.setAuthor(testUser);
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(dataIntegrityService.calculateHash(updatedPost.getContent())).thenReturn("new_hash");
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // Act
        Post result = postService.editPost(postId, updatedPost);
        
        // Assert
        assertNotNull(result);
        
        // 验证没有进行完整性检查
        verify(dataIntegrityService, never()).checkPostIntegrity(anyLong(), anyString(), anyString());
        
        // 验证仍然计算新哈希
        verify(dataIntegrityService).calculateHash(updatedPost.getContent());
    }
    
    @Test
    @DisplayName("删除帖子时完整性验证通过")
    void testDeletePost_IntegrityCheckPassed() throws Exception {
        // Arrange
        Long postId = 1L;
        Long userId = 1L;
        
        DataIntegrityService.IntegrityCheckResult validResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(true)
                .actualHash("original_hash")
                .expectedHash("original_hash")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(dataIntegrityService.checkPostIntegrity(postId, testPost.getContent(), testPost.getContentHash()))
            .thenReturn(validResult);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Act
        CompletableFuture<Void> future = postService.deletePost(postId, userId);
        future.get(); // 等待异步操作完成
        
        // Assert
        // 验证完整性检查被调用
        verify(dataIntegrityService).checkPostIntegrity(postId, testPost.getContent(), "original_hash");
        
        // 验证删除操作被调用
        verify(postLikeService).deleteAllLikesForPost(postId);
        verify(postRepository).deleteById(postId);
        
        // 验证审计日志记录
        verify(auditLogService).logSecurityEvent(
            eq(AuditOperation.INTEGRITY_CHECK),
            eq("testuser"),
            contains("帖子删除前完整性验证通过")
        );
        
        verify(auditLogService).logSecurityEvent(
            eq(AuditOperation.POST_DELETE),
            eq("testuser"),
            contains("帖子删除成功")
        );
    }
    
    @Test
    @DisplayName("删除帖子时完整性验证失败")
    void testDeletePost_IntegrityCheckFailed() throws Exception {
        // Arrange
        Long postId = 1L;
        Long userId = 1L;
        
        DataIntegrityService.IntegrityCheckResult invalidResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(false)
                .actualHash("tampered_hash")
                .expectedHash("original_hash")
                .errorMessage("帖子内容哈希值不匹配，可能已被篡改")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(dataIntegrityService.checkPostIntegrity(postId, testPost.getContent(), testPost.getContentHash()))
            .thenReturn(invalidResult);
        
        // Act
        CompletableFuture<Void> future = postService.deletePost(postId, userId);
        
        // Assert - 异步操作中的异常会被捕获并记录，但不会抛出
        future.get(); // 等待异步操作完成
        
        // 验证完整性检查被调用
        verify(dataIntegrityService).checkPostIntegrity(postId, testPost.getContent(), "original_hash");
        
        // 验证审计日志记录失败事件
        verify(auditLogService).logSecurityEvent(
            eq(AuditOperation.INTEGRITY_CHECK),
            eq("testuser"),
            contains("帖子删除前完整性验证失败")
        );
        
        // 验证没有执行删除操作
        verify(postLikeService, never()).deleteAllLikesForPost(postId);
        verify(postRepository, never()).deleteById(postId);
    }
    
    @Test
    @DisplayName("删除帖子时无哈希值跳过完整性检查")
    void testDeletePost_NoHashSkipIntegrityCheck() throws Exception {
        // Arrange
        Long postId = 1L;
        Long userId = 1L;
        testPost.setContentHash(null); // 没有哈希值
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Act
        CompletableFuture<Void> future = postService.deletePost(postId, userId);
        future.get(); // 等待异步操作完成
        
        // Assert
        // 验证没有进行完整性检查
        verify(dataIntegrityService, never()).checkPostIntegrity(anyLong(), anyString(), anyString());
        
        // 验证删除操作被执行
        verify(postLikeService).deleteAllLikesForPost(postId);
        verify(postRepository).deleteById(postId);
    }
    
    @Test
    @DisplayName("编辑帖子时权限检查失败")
    void testEditPost_PermissionDenied() {
        // Arrange
        Long postId = 1L;
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");
        
        Post updatedPost = new Post();
        updatedPost.setTitle("更新后的标题");
        updatedPost.setContent("更新后的内容");
        updatedPost.setAuthor(anotherUser); // 不同的用户
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            postService.editPost(postId, updatedPost);
        });
        
        assertEquals("无权限编辑", exception.getMessage());
        
        // 验证没有进行完整性检查
        verify(dataIntegrityService, never()).checkPostIntegrity(anyLong(), anyString(), anyString());
        
        // 验证没有保存帖子
        verify(postRepository, never()).save(any(Post.class));
    }
    
    @Test
    @DisplayName("删除帖子时权限检查失败")
    void testDeletePost_PermissionDenied() throws Exception {
        // Arrange
        Long postId = 1L;
        Long userId = 2L; // 不同的用户ID
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        
        // Act
        CompletableFuture<Void> future = postService.deletePost(postId, userId);
        future.get(); // 等待异步操作完成
        
        // Assert
        // 验证没有进行完整性检查
        verify(dataIntegrityService, never()).checkPostIntegrity(anyLong(), anyString(), anyString());
        
        // 验证没有执行删除操作
        verify(postLikeService, never()).deleteAllLikesForPost(postId);
        verify(postRepository, never()).deleteById(postId);
    }
    
    @Test
    @DisplayName("创建帖子时计算哈希值")
    void testCreatePost_CalculateHash() {
        // Arrange
        Post newPost = new Post();
        newPost.setTitle("新帖子");
        newPost.setContent("新帖子内容");
        newPost.setAuthor(testUser);
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(dataIntegrityService.calculateHash(newPost.getContent())).thenReturn("calculated_hash");
        when(postRepository.save(any(Post.class))).thenReturn(newPost);
        
        // Act
        Post result = postService.createPost(newPost);
        
        // Assert
        assertNotNull(result);
        assertEquals("calculated_hash", newPost.getContentHash());
        assertNotNull(newPost.getHashCalculatedAt());
        
        // 验证哈希计算被调用
        verify(dataIntegrityService).calculateHash(newPost.getContent());
        
        // 验证帖子被保存
        verify(postRepository).save(newPost);
    }
    
    @Test
    @DisplayName("审计日志记录验证")
    void testAuditLogRecording() {
        // Arrange
        Long postId = 1L;
        Post updatedPost = new Post();
        updatedPost.setTitle("更新后的标题");
        updatedPost.setContent("更新后的内容");
        updatedPost.setAuthor(testUser);
        
        DataIntegrityService.IntegrityCheckResult validResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(true)
                .actualHash("original_hash")
                .expectedHash("original_hash")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(dataIntegrityService.checkPostIntegrity(postId, testPost.getContent(), testPost.getContentHash()))
            .thenReturn(validResult);
        when(dataIntegrityService.calculateHash(updatedPost.getContent())).thenReturn("new_hash");
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // Act
        postService.editPost(postId, updatedPost);
        
        // Assert - 验证审计日志调用的详细参数
        ArgumentCaptor<AuditOperation> operationCaptor = ArgumentCaptor.forClass(AuditOperation.class);
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(auditLogService, times(2)).logSecurityEvent(
            operationCaptor.capture(),
            usernameCaptor.capture(),
            messageCaptor.capture()
        );
        
        // 验证第一次调用（完整性检查通过）
        assertEquals(AuditOperation.INTEGRITY_CHECK, operationCaptor.getAllValues().get(0));
        assertEquals("testuser", usernameCaptor.getAllValues().get(0));
        assertTrue(messageCaptor.getAllValues().get(0).contains("帖子编辑前完整性验证通过"));
        
        // 验证第二次调用（编辑成功）
        assertEquals(AuditOperation.POST_UPDATE, operationCaptor.getAllValues().get(1));
        assertEquals("testuser", usernameCaptor.getAllValues().get(1));
        assertTrue(messageCaptor.getAllValues().get(1).contains("帖子编辑成功"));
    }
}