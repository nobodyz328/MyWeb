package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.common.exception.DataIntegrityException;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CommentService数据完整性集成测试
 * 
 * 测试评论服务中的数据完整性验证功能，包括：
 * - 评论修改时的完整性验证
 * - 评论删除时的完整性验证
 * - 完整性异常处理
 * - 完整性监控功能
 * 
 * 符合需求3.2, 3.4, 3.6
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService数据完整性测试")
class CommentServiceIntegrityTest {

    @Mock
    private CommentRepository commentRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DataIntegrityService dataIntegrityService;
    
    @InjectMocks
    private CommentService commentService;
    
    private User testUser;
    private Post testPost;
    private Comment testComment;
    private DataIntegrityService.IntegrityCheckResult validResult;
    private DataIntegrityService.IntegrityCheckResult invalidResult;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        // 创建测试帖子
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("测试帖子");
        testPost.setContent("测试内容");
        
        // 创建测试评论
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setContent("这是一条测试评论");
        testComment.setAuthor(testUser);
        testComment.setPost(testPost);
        testComment.setContentHash("validHash123");
        testComment.setHashCalculatedAt(LocalDateTime.now());
        
        // 创建完整性检查结果
        validResult = DataIntegrityService.IntegrityCheckResult.builder()
            .entityType("COMMENT")
            .entityId(1L)
            .isValid(true)
            .actualHash("validHash123")
            .expectedHash("validHash123")
            .checkTime(LocalDateTime.now())
            .build();
            
        invalidResult = DataIntegrityService.IntegrityCheckResult.builder()
            .entityType("COMMENT")
            .entityId(1L)
            .isValid(false)
            .actualHash("actualHash456")
            .expectedHash("validHash123")
            .errorMessage("评论内容哈希值不匹配，可能已被篡改")
            .checkTime(LocalDateTime.now())
            .build();
    }
    
    // ==================== 评论修改完整性测试 ====================
    
    @Test
    @DisplayName("评论修改 - 完整性验证通过")
    void testEditComment_IntegrityValid_Success() {
        // Given
        String originalContent = testComment.getContent();
        String newContent = "修改后的评论内容";
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, originalContent, testComment.getContentHash()))
            .thenReturn(validResult);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        
        // When
        Comment result = commentService.editComment(1L, 1L, newContent);
        
        // Then
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        verify(dataIntegrityService).checkCommentIntegrity(1L, originalContent, testComment.getContentHash());
        verify(commentRepository).save(any(Comment.class));
    }
    
    @Test
    @DisplayName("评论修改 - 完整性验证失败")
    void testEditComment_IntegrityInvalid_ThrowsException() {
        // Given
        String originalContent = testComment.getContent();
        String newContent = "修改后的评论内容";
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, originalContent, testComment.getContentHash()))
            .thenReturn(invalidResult);
        
        // When & Then
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            commentService.editComment(1L, 1L, newContent);
        });
        
        assertTrue(exception.getMessage().contains("评论内容完整性验证失败"));
        assertTrue(exception.getMessage().contains("可能已被篡改"));
        verify(dataIntegrityService).checkCommentIntegrity(1L, originalContent, testComment.getContentHash());
        verify(commentRepository, never()).save(any(Comment.class));
    }
    
    @Test
    @DisplayName("评论修改 - 无哈希值时跳过验证")
    void testEditComment_NoHash_SkipIntegrityCheck() {
        // Given
        testComment.setContentHash(null);
        String newContent = "修改后的评论内容";
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        
        // When
        Comment result = commentService.editComment(1L, 1L, newContent);
        
        // Then
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        verify(dataIntegrityService, never()).checkCommentIntegrity(anyLong(), anyString(), anyString());
        verify(commentRepository).save(any(Comment.class));
    }
    
    @Test
    @DisplayName("评论修改 - 权限验证失败")
    void testEditComment_UnauthorizedUser_ThrowsException() {
        // Given
        String newContent = "修改后的评论内容";
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.editComment(1L, 999L, newContent); // 不同的用户ID
        });
        
        assertEquals("无权限修改此评论", exception.getMessage());
        verify(dataIntegrityService, never()).checkCommentIntegrity(anyLong(), anyString(), anyString());
        verify(commentRepository, never()).save(any(Comment.class));
    }
    
    // ==================== 评论删除完整性测试 ====================
    
    @Test
    @DisplayName("评论删除 - 完整性验证通过")
    void testDeleteComment_IntegrityValid_Success() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash()))
            .thenReturn(validResult);
        when(commentRepository.countByPostId(1L)).thenReturn(5L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // When
        assertDoesNotThrow(() -> {
            commentService.deleteComment(1L, 1L);
        });
        
        // Then
        verify(dataIntegrityService).checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash());
        verify(commentRepository).delete(testComment);
        verify(postRepository).save(any(Post.class));
    }
    
    @Test
    @DisplayName("评论删除 - 完整性验证失败")
    void testDeleteComment_IntegrityInvalid_ThrowsException() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash()))
            .thenReturn(invalidResult);
        
        // When & Then
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            commentService.deleteComment(1L, 1L);
        });
        
        assertTrue(exception.getMessage().contains("评论删除失败"));
        assertTrue(exception.getMessage().contains("内容完整性验证失败"));
        verify(dataIntegrityService).checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash());
        verify(commentRepository, never()).delete(any(Comment.class));
    }
    
    @Test
    @DisplayName("评论删除 - 无哈希值时跳过验证")
    void testDeleteComment_NoHash_SkipIntegrityCheck() {
        // Given
        testComment.setContentHash(null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.countByPostId(1L)).thenReturn(5L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // When
        assertDoesNotThrow(() -> {
            commentService.deleteComment(1L, 1L);
        });
        
        // Then
        verify(dataIntegrityService, never()).checkCommentIntegrity(anyLong(), anyString(), anyString());
        verify(commentRepository).delete(testComment);
        verify(postRepository).save(any(Post.class));
    }
    
    // ==================== 完整性监控测试 ====================
    
    @Test
    @DisplayName("验证单个评论完整性")
    void testVerifyCommentIntegrity_Success() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash()))
            .thenReturn(validResult);
        
        // When
        DataIntegrityService.IntegrityCheckResult result = commentService.verifyCommentIntegrity(1L);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("COMMENT", result.getEntityType());
        assertEquals(1L, result.getEntityId());
        verify(dataIntegrityService).checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash());
    }
    
    @Test
    @DisplayName("批量验证评论完整性")
    void testVerifyCommentsIntegrity_Success() {
        // Given
        List<Long> commentIds = Arrays.asList(1L, 2L);
        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setContent("第二条评论");
        comment2.setContentHash("hash2");
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(comment2));
        when(dataIntegrityService.checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash()))
            .thenReturn(validResult);
        when(dataIntegrityService.checkCommentIntegrity(2L, comment2.getContent(), comment2.getContentHash()))
            .thenReturn(invalidResult);
        
        // When
        List<DataIntegrityService.IntegrityCheckResult> results = commentService.verifyCommentsIntegrity(commentIds);
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.get(0).isValid());
        assertFalse(results.get(1).isValid());
    }
    
    @Test
    @DisplayName("验证帖子下所有评论完整性")
    void testVerifyPostCommentsIntegrity_Success() {
        // Given
        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setContent("第二条评论");
        comment2.setContentHash("hash2");
        
        List<Comment> comments = Arrays.asList(testComment, comment2);
        when(commentRepository.findByPostId(1L)).thenReturn(comments);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(comment2));
        when(dataIntegrityService.checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash()))
            .thenReturn(validResult);
        when(dataIntegrityService.checkCommentIntegrity(2L, comment2.getContent(), comment2.getContentHash()))
            .thenReturn(invalidResult);
        
        // When
        List<DataIntegrityService.IntegrityCheckResult> results = commentService.verifyPostCommentsIntegrity(1L);
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.get(0).isValid());
        assertFalse(results.get(1).isValid());
    }
    
    @Test
    @DisplayName("获取评论完整性统计信息")
    void testGetCommentIntegrityStats_Success() {
        // Given
        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setContent("第二条评论");
        comment2.setContentHash("hash2");
        
        Comment comment3 = new Comment();
        comment3.setId(3L);
        comment3.setContent("第三条评论");
        comment3.setContentHash(null); // 无哈希值
        
        List<Comment> allComments = Arrays.asList(testComment, comment2, comment3);
        when(commentRepository.findAll()).thenReturn(allComments);
        when(dataIntegrityService.checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash()))
            .thenReturn(validResult);
        when(dataIntegrityService.checkCommentIntegrity(2L, comment2.getContent(), comment2.getContentHash()))
            .thenReturn(invalidResult);
        
        // When
        CommentService.CommentIntegrityStats stats = commentService.getCommentIntegrityStats();
        
        // Then
        assertNotNull(stats);
        assertEquals(3, stats.getTotalComments());
        assertEquals(2, stats.getCommentsWithHash());
        assertEquals(1, stats.getValidComments());
        assertEquals(1, stats.getInvalidComments());
        assertEquals(0.5, stats.getIntegrityRate(), 0.01);
    }
    
    // ==================== 异常处理测试 ====================
    
    @Test
    @DisplayName("评论不存在时抛出异常")
    void testEditComment_CommentNotFound_ThrowsException() {
        // Given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.editComment(999L, 1L, "新内容");
        });
        
        assertEquals("评论不存在", exception.getMessage());
    }
    
    @Test
    @DisplayName("完整性检查异常时的处理")
    void testVerifyCommentIntegrity_ServiceException_HandledGracefully() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, testComment.getContent(), testComment.getContentHash()))
            .thenThrow(new RuntimeException("服务异常"));
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            commentService.verifyCommentIntegrity(1L);
        });
    }
    
    // ==================== 边界条件测试 ====================
    
    @Test
    @DisplayName("空内容评论的完整性处理")
    void testEditComment_EmptyContent_HandledCorrectly() {
        // Given
        testComment.setContent("");
        testComment.setContentHash("emptyHash");
        String newContent = "新的非空内容";
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, "", "emptyHash"))
            .thenReturn(validResult);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        
        // When
        Comment result = commentService.editComment(1L, 1L, newContent);
        
        // Then
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        verify(dataIntegrityService).checkCommentIntegrity(1L, "", "emptyHash");
    }
    
    @Test
    @DisplayName("长内容评论的完整性处理")
    void testEditComment_LongContent_HandledCorrectly() {
        // Given
        String longContent = "很长的评论内容".repeat(1000);
        testComment.setContent(longContent);
        testComment.setContentHash("longContentHash");
        String newContent = "修改后的内容";
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(dataIntegrityService.checkCommentIntegrity(1L, longContent, "longContentHash"))
            .thenReturn(validResult);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        
        // When
        Comment result = commentService.editComment(1L, 1L, newContent);
        
        // Then
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        verify(dataIntegrityService).checkCommentIntegrity(1L, longContent, "longContentHash");
    }
}