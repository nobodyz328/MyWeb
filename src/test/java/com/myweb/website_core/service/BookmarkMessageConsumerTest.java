package com.myweb.website_core.service;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.interaction.PostBookmark;
import com.myweb.website_core.demos.web.interaction.PostBookmarkRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.BookmarkMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkMessageConsumerTest {

    @Mock
    private PostBookmarkRepository postBookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private BookmarkMessageConsumer bookmarkMessageConsumer;

    private User testUser;
    private Post testPost;
    private PostBookmark testBookmark;
    private BookmarkMessageDto bookmarkMessage;
    private BookmarkMessageDto unbookmarkMessage;

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

        testBookmark = new PostBookmark(testUser, testPost);
        testBookmark.setId(1L);

        // 创建书签消息
        bookmarkMessage = new BookmarkMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L,
            "author"
        );

        // 创建取消书签消息
        unbookmarkMessage = new BookmarkMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            false,
            "Test Post",
            2L,
            "author"
        );

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testHandleBookmarkMessage_AddBookmark_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("user:1:bookmarks:page:0"));

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert
        verify(postBookmarkRepository).findByUserIdAndPostId(1L, 1L);
        verify(postBookmarkRepository).save(any(PostBookmark.class));
        verify(postBookmarkRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:bookmarks:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));
        verify(redisTemplate).delete(contains("user:1:post:1:interactions"));
        verify(redisTemplate).keys("user:1:bookmarks:page:*");
    }

    @Test
    void testHandleBookmarkMessage_RemoveBookmark_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testBookmark));
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(0L);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("user:1:bookmarks:page:0"));

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(unbookmarkMessage));

        // Assert
        verify(postBookmarkRepository).findByUserIdAndPostId(1L, 1L);
        verify(postBookmarkRepository).delete(testBookmark);
        verify(postBookmarkRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:bookmarks:count:1"), eq(0L), eq(1L), eq(TimeUnit.HOURS));
        verify(redisTemplate).delete(contains("user:1:post:1:interactions"));
        verify(redisTemplate).keys("user:1:bookmarks:page:*");
    }

    @Test
    void testHandleBookmarkMessage_DuplicateBookmark_NoOperation() {
        // Arrange - 用户已经收藏过
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testBookmark));
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("user:1:bookmarks:page:0"));

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert - 不应该创建新的书签记录
        verify(postBookmarkRepository).findByUserIdAndPostId(1L, 1L);
        verify(postBookmarkRepository, never()).save(any(PostBookmark.class));
        verify(postBookmarkRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:bookmarks:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void testHandleBookmarkMessage_RemoveNonExistentBookmark_NoOperation() {
        // Arrange - 用户没有收藏过
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(0L);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(unbookmarkMessage));

        // Assert - 不应该删除任何记录
        verify(postBookmarkRepository).findByUserIdAndPostId(1L, 1L);
        verify(postBookmarkRepository, never()).delete(any(PostBookmark.class));
        verify(postBookmarkRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:bookmarks:count:1"), eq(0L), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void testHandleBookmarkMessage_UserNotFound_NoOperation() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert - 不应该进行任何数据库操作
        verify(userRepository).findById(1L);
        verify(postRepository, never()).findById(anyLong());
        verify(postBookmarkRepository, never()).findByUserIdAndPostId(anyLong(), anyLong());
        verify(postBookmarkRepository, never()).save(any());
        verify(postBookmarkRepository, never()).delete(any());
    }

    @Test
    void testHandleBookmarkMessage_PostNotFound_NoOperation() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert - 不应该进行书签相关操作
        verify(userRepository).findById(1L);
        verify(postRepository).findById(1L);
        verify(postBookmarkRepository, never()).findByUserIdAndPostId(anyLong(), anyLong());
        verify(postBookmarkRepository, never()).save(any());
        verify(postBookmarkRepository, never()).delete(any());
    }

    @Test
    void testHandleBookmarkMessage_InvalidMessage_NoOperation() {
        // Test null message
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(null));

        // Test message with null messageId
        BookmarkMessageDto invalidMessage = new BookmarkMessageDto();
        invalidMessage.setMessageId(null);
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setUsername("testuser");
        invalidMessage.setPostTitle("Test Post");
        BookmarkMessageDto finalInvalidMessage = invalidMessage;
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(finalInvalidMessage));

        // Test message with invalid userId
        invalidMessage = new BookmarkMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(null);
        invalidMessage.setPostId(1L);
        invalidMessage.setUsername("testuser");
        invalidMessage.setPostTitle("Test Post");
        BookmarkMessageDto finalInvalidMessage1 = invalidMessage;
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(finalInvalidMessage1));

        // Test message with invalid postId
        invalidMessage = new BookmarkMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(null);
        invalidMessage.setUsername("testuser");
        invalidMessage.setPostTitle("Test Post");
        BookmarkMessageDto finalInvalidMessage2 = invalidMessage;
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(finalInvalidMessage2));

        // Test message with empty username
        invalidMessage = new BookmarkMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setUsername("");
        invalidMessage.setPostTitle("Test Post");
        BookmarkMessageDto finalInvalidMessage3 = invalidMessage;
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(finalInvalidMessage3));

        // Test message with empty post title
        invalidMessage = new BookmarkMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setUsername("testuser");
        invalidMessage.setPostTitle("");
        BookmarkMessageDto finalInvalidMessage4 = invalidMessage;
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(finalInvalidMessage4));

        // Verify no database operations were performed
        verify(userRepository, never()).findById(anyLong());
        verify(postRepository, never()).findById(anyLong());
        verify(postBookmarkRepository, never()).findByUserIdAndPostId(anyLong(), anyLong());
    }

    @Test
    void testHandleBookmarkMessage_DatabaseException_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        verify(userRepository).findById(1L);
        verify(postRepository).findById(1L);
        verify(postBookmarkRepository).findByUserIdAndPostId(1L, 1L);
    }

    @Test
    void testHandleBookmarkMessage_CacheUpdateFailure_DoesNotThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        doThrow(new RuntimeException("Redis error")).when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        // Act - 缓存更新失败不应该影响主要业务逻辑
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert - 主要业务逻辑应该正常执行
        verify(postBookmarkRepository).save(any(PostBookmark.class));
        verify(postBookmarkRepository).countByPostId(1L);
    }

    @Test
    void testGetCurrentBookmarkCount() {
        // Arrange
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(3L);

        // Act
        long count = bookmarkMessageConsumer.getCurrentBookmarkCount(1L);

        // Assert
        assertEquals(3L, count);
        verify(postBookmarkRepository).countByPostId(1L);
    }

    @Test
    void testIsUserBookmarked() {
        // Arrange
        when(postBookmarkRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(true);

        // Act
        boolean isBookmarked = bookmarkMessageConsumer.isUserBookmarked(1L, 1L);

        // Assert
        assertTrue(isBookmarked);
        verify(postBookmarkRepository).existsByUserIdAndPostId(1L, 1L);
    }

    @Test
    void testGetUserBookmarkCount() {
        // Arrange
        when(postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testBookmark, testBookmark, testBookmark, testBookmark, testBookmark));

        // Act
        long count = bookmarkMessageConsumer.getUserBookmarkCount(1L);

        // Assert
        assertEquals(5L, count);
        verify(postBookmarkRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void testCacheKeyGeneration() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("user:1:bookmarks:page:0"));

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert - 验证缓存键的格式
        verify(valueOperations).set(eq("post:bookmarks:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));
        verify(redisTemplate).delete(eq("user:1:post:1:interactions"));
        verify(redisTemplate).keys("user:1:bookmarks:page:*");
    }

    @Test
    void testUserBookmarksCacheClear() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        
        Set<String> cacheKeys = Set.of(
            "user:1:bookmarks:page:0",
            "user:1:bookmarks:page:1",
            "user:1:bookmarks:page:2"
        );
        when(redisTemplate.keys("user:1:bookmarks:page:*")).thenReturn(cacheKeys);

        // Act
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert - 验证所有用户书签缓存页面都被清除
        verify(redisTemplate).keys("user:1:bookmarks:page:*");
        verify(redisTemplate).delete(cacheKeys);
    }

    @Test
    void testTransactionRollback_OnException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenThrow(new RuntimeException("Save failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Verify that the transaction should be rolled back
        verify(postBookmarkRepository).save(any(PostBookmark.class));
        // Cache operations should not be called if save fails
        verify(postBookmarkRepository, never()).countByPostId(anyLong());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testCacheClearFailure_DoesNotAffectMainLogic() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis keys error"));

        // Act - 缓存清除失败不应该影响主要业务逻辑
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Assert - 主要业务逻辑应该正常执行
        verify(postBookmarkRepository).save(any(PostBookmark.class));
        verify(postBookmarkRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:bookmarks:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));
    }
}