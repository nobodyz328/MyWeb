package com.myweb.website_core.service;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.interaction.PostLike;
import com.myweb.website_core.demos.web.interaction.PostLikeRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.LikeMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class LikeMessageConsumerTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LikeMessageConsumer likeMessageConsumer;

    private User testUser;
    private Post testPost;
    private PostLike testLike;
    private LikeMessageDto likeMessage;
    private LikeMessageDto unlikeMessage;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        User postAuthor = new User();
        postAuthor.setId(2L);
        postAuthor.setUsername("author");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setAuthor(postAuthor);
        testPost.setCreatedAt(LocalDateTime.now());

        testLike = new PostLike(testUser, testPost);
        testLike.setId(1L);

        // 创建点赞消息
        likeMessage = new LikeMessageDto(
                UUID.randomUUID().toString(),
                1L,
                "testuser",
                1L,
                true,
                "Test Post",
                2L);

        // 创建取消点赞消息
        unlikeMessage = new LikeMessageDto(
                UUID.randomUUID().toString(),
                1L,
                "testuser",
                1L,
                false,
                "Test Post",
                2L);

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testHandleLikeMessage_AddLike_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Assert
        verify(postLikeRepository).findByUserIdAndPostId(1L, 1L);
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postLikeRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:likes:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));
        verify(redisTemplate).delete(contains("user:1:post:1:interactions"));
    }

    @Test
    void testHandleLikeMessage_RemoveLike_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testLike));
        when(postLikeRepository.countByPostId(1L)).thenReturn(0L);

        // Act
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(unlikeMessage));

        // Assert
        verify(postLikeRepository).findByUserIdAndPostId(1L, 1L);
        verify(postLikeRepository).delete(testLike);
        verify(postLikeRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:likes:count:1"), eq(0L), eq(1L), eq(TimeUnit.HOURS));
        verify(redisTemplate).delete(contains("user:1:post:1:interactions"));
    }

    @Test
    void testHandleLikeMessage_DuplicateLike_NoOperation() {
        // Arrange - 用户已经点赞过
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testLike));
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Assert - 不应该创建新的点赞记录
        verify(postLikeRepository).findByUserIdAndPostId(1L, 1L);
        verify(postLikeRepository, never()).save(any(PostLike.class));
        verify(postLikeRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:likes:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void testHandleLikeMessage_RemoveNonExistentLike_NoOperation() {
        // Arrange - 用户没有点赞过
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.countByPostId(1L)).thenReturn(0L);

        // Act
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(unlikeMessage));

        // Assert - 不应该删除任何记录
        verify(postLikeRepository).findByUserIdAndPostId(1L, 1L);
        verify(postLikeRepository, never()).delete(any(PostLike.class));
        verify(postLikeRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:likes:count:1"), eq(0L), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void testHandleLikeMessage_UserNotFound_NoOperation() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Assert - 不应该进行任何数据库操作
        verify(userRepository).findById(1L);
        verify(postRepository, never()).findById(anyLong());
        verify(postLikeRepository, never()).findByUserIdAndPostId(anyLong(), anyLong());
        verify(postLikeRepository, never()).save(any());
        verify(postLikeRepository, never()).delete(any());
    }

    @Test
    void testHandleLikeMessage_PostNotFound_NoOperation() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Assert - 不应该进行点赞相关操作
        verify(userRepository).findById(1L);
        verify(postRepository).findById(1L);
        verify(postLikeRepository, never()).findByUserIdAndPostId(anyLong(), anyLong());
        verify(postLikeRepository, never()).save(any());
        verify(postLikeRepository, never()).delete(any());
    }

    @Test
    void testHandleLikeMessage_InvalidMessage_NoOperation() {
        // Test null message
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(null));

        // Test message with null messageId
        LikeMessageDto invalidMessage = new LikeMessageDto();
        invalidMessage.setMessageId(null);
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setUsername("testuser");
        LikeMessageDto finalInvalidMessage = invalidMessage;
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(finalInvalidMessage));

        // Test message with invalid userId
        invalidMessage = new LikeMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(null);
        invalidMessage.setPostId(1L);
        invalidMessage.setUsername("testuser");
        LikeMessageDto finalInvalidMessage1 = invalidMessage;
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(finalInvalidMessage1));

        // Test message with invalid postId
        invalidMessage = new LikeMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(null);
        invalidMessage.setUsername("testuser");
        LikeMessageDto finalInvalidMessage2 = invalidMessage;
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(finalInvalidMessage2));

        // Test message with empty username
        invalidMessage = new LikeMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setUsername("");
        LikeMessageDto finalInvalidMessage3 = invalidMessage;
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(finalInvalidMessage3));

        // Verify no database operations were performed
        verify(userRepository, never()).findById(anyLong());
        verify(postRepository, never()).findById(anyLong());
        verify(postLikeRepository, never()).findByUserIdAndPostId(anyLong(), anyLong());
    }

    @Test
    void testHandleLikeMessage_DatabaseException_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> likeMessageConsumer.handleLikeMessage(likeMessage));

        verify(userRepository).findById(1L);
        verify(postRepository).findById(1L);
        verify(postLikeRepository).findByUserIdAndPostId(1L, 1L);
    }

    @Test
    void testHandleLikeMessage_CacheUpdateFailure_DoesNotThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);
        doThrow(new RuntimeException("Redis error")).when(valueOperations).set(anyString(), any(), anyLong(),
                any(TimeUnit.class));

        // Act - 缓存更新失败不应该影响主要业务逻辑
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Assert - 主要业务逻辑应该正常执行
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postLikeRepository).countByPostId(1L);
    }

    @Test
    void testGetCurrentLikeCount() {
        // Arrange
        when(postLikeRepository.countByPostId(1L)).thenReturn(5L);

        // Act
        long count = likeMessageConsumer.getCurrentLikeCount(1L);

        // Assert
        assertEquals(5L, count);
        verify(postLikeRepository).countByPostId(1L);
    }

    @Test
    void testIsUserLiked() {
        // Arrange
        when(postLikeRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(true);

        // Act
        boolean isLiked = likeMessageConsumer.isUserLiked(1L, 1L);

        // Assert
        assertTrue(isLiked);
        verify(postLikeRepository).existsByUserIdAndPostId(1L, 1L);
    }

    @Test
    void testCacheKeyGeneration() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Assert - 验证缓存键的格式
        verify(valueOperations).set(eq("post:likes:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));
        verify(redisTemplate).delete(eq("user:1:post:1:interactions"));
    }

    @Test
    void testTransactionRollback_OnException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenThrow(new RuntimeException("Save failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Verify that the transaction should be rolled back
        verify(postLikeRepository).save(any(PostLike.class));
        // Cache operations should not be called if save fails
        verify(postLikeRepository, never()).countByPostId(anyLong());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }
}