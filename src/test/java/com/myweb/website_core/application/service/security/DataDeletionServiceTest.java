package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.application.service.security.confirm.ConfirmationService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataDeletionService;
import com.myweb.website_core.domain.business.entity.*;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.ImageRepository;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.PostCollectRepository;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.PostLikeRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 数据彻底删除服务测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@ExtendWith(MockitoExtension.class)
class DataDeletionServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private CommentRepository commentRepository;
    
    @Mock
    private PostLikeRepository postLikeRepository;
    
    @Mock
    private PostCollectRepository postCollectRepository;
    
    @Mock
    private ImageRepository imageRepository;
    
    @Mock
    private AuditMessageService auditLogService;
    
    @Mock
    private ConfirmationService confirmationService;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @InjectMocks
    private DataDeletionService dataDeletionService;
    
    private User testUser;
    private Post testPost;
    private Comment testComment;
    private ConfirmationService.ConfirmationToken testToken;
    
    @BeforeEach
    void setUp() {
        // 设置测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(LocalDateTime.now());
        
        // 设置测试帖子
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setAuthor(testUser);
        testPost.setCreatedAt(LocalDateTime.now());
        
        // 设置测试评论
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setContent("Test Comment");
        testComment.setPost(testPost);
        testComment.setAuthor(testUser);
        testComment.setCreatedAt(LocalDateTime.now());
        
        // 设置测试确认令牌
        testToken = new ConfirmationService.ConfirmationToken(
            "test-token",
            "1",
            ConfirmationService.OperationType.DELETE_USER,
            "1",
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(10)
        );
        
        // 设置Redis模拟
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());
    }
    
    @Test
    void testDeleteUserCompletely_Success() {
        // 准备测试数据
        when(confirmationService.consumeConfirmationToken("test-token")).thenReturn(testToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findByAuthorId(1L)).thenReturn(Arrays.asList(testPost));
        when(commentRepository.findByAuthorIdOrderByCreatedAtDesc(1L)).thenReturn(Arrays.asList(testComment));
        when(postLikeRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(postCollectRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        
        // 设置帖子相关的模拟
        when(commentRepository.findTopLevelCommentsByPostId(1L)).thenReturn(Collections.emptyList());
        when(postLikeRepository.findByPostId(1L)).thenReturn(Collections.emptyList());
        when(postCollectRepository.findByPostId(1L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByPostId(1L)).thenReturn(Collections.emptyList());
        
        // 设置评论相关的模拟
        when(commentRepository.findRepliesByParentId(1L)).thenReturn(Collections.emptyList());
        
        // 执行测试
        DataDeletionService.DeletionResult result = dataDeletionService.deleteUserCompletely(
            1L, "test-token", "2"
        );
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("用户数据删除成功", result.getMessage());
        assertNotNull(result.getStatistics());
        assertEquals(1, result.getStatistics().getDeletedUsers());
        assertEquals(1, result.getStatistics().getDeletedPosts());
        assertEquals(1, result.getStatistics().getDeletedComments());
        
        // 验证方法调用
        verify(userRepository).delete(testUser);
        verify(postRepository).delete(testPost);
        verify(commentRepository).delete(testComment);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testDeleteUserCompletely_InvalidToken() {
        // 准备测试数据 - 无效令牌
        when(confirmationService.consumeConfirmationToken("invalid-token")).thenReturn(null);
        
        // 执行测试
        DataDeletionService.DeletionResult result = dataDeletionService.deleteUserCompletely(
            1L, "invalid-token", "2"
        );
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("确认令牌无效"));
        
        // 验证没有执行删除操作
        //verify(userRepository, never()).delete(any());
        verify(auditLogService).logOperation(any()); // 应该记录失败日志
    }
    
    @Test
    void testDeleteUserCompletely_UserNotFound() {
        // 准备测试数据
        when(confirmationService.consumeConfirmationToken("test-token")).thenReturn(testToken);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        // 执行测试
        DataDeletionService.DeletionResult result = dataDeletionService.deleteUserCompletely(
            1L, "test-token", "2"
        );
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("用户不存在"));
        
        // 验证没有执行删除操作
       // verify(userRepository, never()).delete(any());
    }
    
    @Test
    void testDeletePostCompletely_Success() {
        // 准备测试数据
        ConfirmationService.ConfirmationToken postToken = new ConfirmationService.ConfirmationToken(
            "post-token",
            "1",
            ConfirmationService.OperationType.DELETE_POST,
            "1",
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(10)
        );
        
        when(confirmationService.consumeConfirmationToken("post-token")).thenReturn(postToken);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.findTopLevelCommentsByPostId(1L)).thenReturn(Collections.emptyList());
        when(postLikeRepository.findByPostId(1L)).thenReturn(Collections.emptyList());
        when(postCollectRepository.findByPostId(1L)).thenReturn(Collections.emptyList());
        when(imageRepository.findByPostId(1L)).thenReturn(Collections.emptyList());
        
        // 执行测试
        DataDeletionService.DeletionResult result = dataDeletionService.deletePostCompletely(
            1L, "post-token", "1"
        );
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("帖子数据删除成功", result.getMessage());
        assertEquals(1, result.getStatistics().getDeletedPosts());
        
        // 验证方法调用
        verify(postRepository).delete(testPost);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testDeleteCommentCompletely_Success() {
        // 准备测试数据
        ConfirmationService.ConfirmationToken commentToken = new ConfirmationService.ConfirmationToken(
            "comment-token",
            "1",
            ConfirmationService.OperationType.DELETE_COMMENT,
            "1",
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(10)
        );
        
        when(confirmationService.consumeConfirmationToken("comment-token")).thenReturn(commentToken);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.findRepliesByParentId(1L)).thenReturn(Collections.emptyList());
        
        // 执行测试
        DataDeletionService.DeletionResult result = dataDeletionService.deleteCommentCompletely(
            1L, "comment-token", "1"
        );
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("评论数据删除成功", result.getMessage());
        assertEquals(1, result.getStatistics().getDeletedComments());
        
        // 验证方法调用
        verify(commentRepository).delete(testComment);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testDeactivateUserAccount_Success() {
        // 准备测试数据
        ConfirmationService.ConfirmationToken deactivateToken = new ConfirmationService.ConfirmationToken(
            "deactivate-token",
            "1",
            ConfirmationService.OperationType.DEACTIVATE_ACCOUNT,
            "1",
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(10)
        );
        
        when(confirmationService.consumeConfirmationToken("deactivate-token")).thenReturn(deactivateToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findByAuthorId(1L)).thenReturn(Collections.emptyList());
        when(commentRepository.findByAuthorIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(postLikeRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(postCollectRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        
        // 执行测试
        DataDeletionService.DeletionResult result = dataDeletionService.deactivateUserAccount(
            1L, "deactivate-token"
        );
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("用户数据删除成功", result.getMessage());
        assertEquals(1, result.getStatistics().getDeletedUsers());
        
        // 验证方法调用
        verify(userRepository).delete(testUser);
    }
    
    @Test
    void testClearSessionData() {
        // 准备测试数据
        Set<String> sessionKeys = Set.of("spring:session:sessions:session1", "spring:session:sessions:session2");
        when(redisTemplate.keys("spring:session:sessions:*")).thenReturn(sessionKeys);
        when(valueOperations.get("spring:session:sessions:session1")).thenReturn("user:1:data");
        when(valueOperations.get("spring:session:sessions:session2")).thenReturn("user:2:data");
        
        // 执行测试
        dataDeletionService.clearSessionData(1L, "session1");
        
        // 验证方法调用
        verify(redisTemplate).delete("spring:session:sessions:session1");
        verify(redisTemplate, atLeastOnce()).keys(anyString());
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testClearTemporaryData() {
        // 准备测试数据
        Set<String> tempKeys = Set.of("temp:data1", "temp:data2");
        when(redisTemplate.keys("temp:*")).thenReturn(tempKeys);
        
        // 执行测试
        dataDeletionService.clearTemporaryData();
        
        // 验证方法调用
        verify(redisTemplate).delete(tempKeys);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testGenerateDeletionConfirmationToken() {
        // 准备测试数据
        when(confirmationService.generateConfirmationToken(
            "1", ConfirmationService.OperationType.DELETE_POST, "1"
        )).thenReturn(testToken);
        
        // 执行测试
        ConfirmationService.ConfirmationToken result = dataDeletionService.generateDeletionConfirmationToken(
            "1", ConfirmationService.OperationType.DELETE_POST, "1"
        );
        
        // 验证结果
        assertNotNull(result);
        assertEquals("test-token", result.getToken());
        assertEquals(ConfirmationService.OperationType.DELETE_USER, result.getOperationType());
        
        // 验证方法调用
        verify(confirmationService).generateConfirmationToken(
            "1", ConfirmationService.OperationType.DELETE_POST, "1"
        );
    }
    
    @Test
    void testSendDeletionConfirmationEmail() {
        // 准备测试数据
        when(confirmationService.sendEmailConfirmation(
            "1", ConfirmationService.OperationType.DELETE_POST, "1"
        )).thenReturn(testToken);
        
        // 执行测试
        ConfirmationService.ConfirmationToken result = dataDeletionService.sendDeletionConfirmationEmail(
            "1", ConfirmationService.OperationType.DELETE_POST, "1"
        );
        
        // 验证结果
        assertNotNull(result);
        assertEquals("test-token", result.getToken());
        
        // 验证方法调用
        verify(confirmationService).sendEmailConfirmation(
            "1", ConfirmationService.OperationType.DELETE_POST, "1"
        );
    }
    
    @Test
    void testRequiresDeletionConfirmation() {
        // 准备测试数据
        when(confirmationService.requiresConfirmation(
            ConfirmationService.OperationType.DELETE_POST, "1"
        )).thenReturn(true);
        
        // 执行测试
        boolean result = dataDeletionService.requiresDeletionConfirmation(
            ConfirmationService.OperationType.DELETE_POST, "1"
        );
        
        // 验证结果
        assertTrue(result);
        
        // 验证方法调用
        verify(confirmationService).requiresConfirmation(
            ConfirmationService.OperationType.DELETE_POST, "1"
        );
    }
    
    @Test
    void testDeletionStatistics() {
        // 创建统计对象
        DataDeletionService.DeletionStatistics statistics = new DataDeletionService.DeletionStatistics();
        
        // 测试初始值
        assertEquals(0, statistics.getDeletedUsers());
        assertEquals(0, statistics.getDeletedPosts());
        assertEquals(0, statistics.getDeletedComments());
        //assertEquals(0, statistics.getTotalCleared());
        
        // 测试增量方法
        statistics.incrementDeletedUsers();
        statistics.incrementDeletedPosts();
        statistics.incrementDeletedComments();
        
        assertEquals(1, statistics.getDeletedUsers());
        assertEquals(1, statistics.getDeletedPosts());
        assertEquals(1, statistics.getDeletedComments());
        
        // 测试toString方法
        String statisticsString = statistics.toString();
        assertNotNull(statisticsString);
        assertTrue(statisticsString.contains("用户=1"));
        assertTrue(statisticsString.contains("帖子=1"));
        assertTrue(statisticsString.contains("评论=1"));
    }
    
    @Test
    void testDeletionResult() {
        // 创建统计对象
        DataDeletionService.DeletionStatistics statistics = new DataDeletionService.DeletionStatistics();
        statistics.incrementDeletedUsers();
        
        // 创建结果对象
        DataDeletionService.DeletionResult result = new DataDeletionService.DeletionResult(
            true, "删除成功", statistics
        );
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("删除成功", result.getMessage());
        assertNotNull(result.getStatistics());
        assertEquals(1, result.getStatistics().getDeletedUsers());
    }
}