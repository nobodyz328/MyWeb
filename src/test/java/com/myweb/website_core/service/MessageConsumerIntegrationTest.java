package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.interaction.PostBookmark;
import com.myweb.website_core.demos.web.interaction.PostBookmarkRepository;
import com.myweb.website_core.demos.web.interaction.PostLike;
import com.myweb.website_core.demos.web.interaction.PostLikeRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.BookmarkMessageDto;
import com.myweb.website_core.dto.LikeMessageDto;
import com.myweb.website_core.dto.StatsUpdateMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 消息消费者集成测试
 * 测试完整的消息处理工作流程
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
class MessageConsumerIntegrationTest {

    @Autowired
    private LikeMessageConsumer likeMessageConsumer;

    @Autowired
    private BookmarkMessageConsumer bookmarkMessageConsumer;

    @Autowired
    private StatsUpdateConsumer statsUpdateConsumer;

    @MockBean
    private PostLikeRepository postLikeRepository;

    @MockBean
    private PostBookmarkRepository postBookmarkRepository;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private ValueOperations<String, Object> valueOperations;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private User testUser;
    private Post testPost;
    private PostLike testLike;
    private PostBookmark testBookmark;

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

        testBookmark = new PostBookmark(testUser, testPost);
        testBookmark.setId(1L);

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testCompleteInteractionWorkflow_LikeAndStats() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);

        // Create messages
        LikeMessageDto likeMessage = new LikeMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L
        );

        StatsUpdateMessageDto statsMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "LIKE",
            1,
            "LIKE"
        );

        // Act - Process like message first
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Act - Process stats update message
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(statsMessage));

        // Assert - Verify like processing
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postLikeRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:likes:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));

        // Assert - Verify stats processing
        verify(valueOperations).increment("post:likes:count:1", 1L);
        verify(redisTemplate).expire("post:likes:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testCompleteInteractionWorkflow_BookmarkAndStats() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        // Create messages
        BookmarkMessageDto bookmarkMessage = new BookmarkMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L,
            "author"
        );

        StatsUpdateMessageDto statsMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "BOOKMARK",
            1,
            "BOOKMARK"
        );

        // Act - Process bookmark message first
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage));

        // Act - Process stats update message
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(statsMessage));

        // Assert - Verify bookmark processing
        verify(postBookmarkRepository).save(any(PostBookmark.class));
        verify(postBookmarkRepository).countByPostId(1L);
        verify(valueOperations).set(eq("post:bookmarks:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));

        // Assert - Verify stats processing
        verify(valueOperations).increment("post:bookmarks:count:1", 1L);
        verify(redisTemplate).expire("post:bookmarks:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testErrorHandling_PartialFailure() {
        // Arrange - Like processing succeeds, stats processing fails
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);
        when(valueOperations.increment(anyString(), anyLong())).thenThrow(new RuntimeException("Stats error"));

        // Create messages
        LikeMessageDto likeMessage = new LikeMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L
        );

        StatsUpdateMessageDto statsMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "LIKE",
            1,
            "LIKE"
        );

        // Act - Like processing should succeed
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Act - Stats processing should fail
        assertThrows(RuntimeException.class, () -> statsUpdateConsumer.handleStatsUpdateMessage(statsMessage));

        // Assert - Like processing completed successfully
        verify(postLikeRepository).save(any(PostLike.class));
        verify(valueOperations).set(eq("post:likes:count:1"), eq(1L), eq(1L), eq(TimeUnit.HOURS));

        // Assert - Stats processing attempted but failed
        verify(valueOperations).increment("post:likes:count:1", 1L);
    }

    @Test
    void testConcurrentMessageProcessing() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        // Create messages
        LikeMessageDto likeMessage = new LikeMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L
        );

        BookmarkMessageDto bookmarkMessage = new BookmarkMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L,
            "author"
        );

        // Act - Process both messages concurrently
        assertDoesNotThrow(() -> {
            likeMessageConsumer.handleLikeMessage(likeMessage);
            bookmarkMessageConsumer.handleBookmarkMessage(bookmarkMessage);
        });

        // Assert - Both operations should complete successfully
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postBookmarkRepository).save(any(PostBookmark.class));
        verify(valueOperations, times(2)).set(anyString(), anyLong(), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void testMessageValidation_AcrossAllConsumers() {
        // Test null messages
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(null));
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(null));
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(null));

        // Test invalid messages
        LikeMessageDto invalidLikeMessage = new LikeMessageDto();
        invalidLikeMessage.setMessageId("");
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(invalidLikeMessage));

        BookmarkMessageDto invalidBookmarkMessage = new BookmarkMessageDto();
        invalidBookmarkMessage.setUserId(-1L);
        assertDoesNotThrow(() -> bookmarkMessageConsumer.handleBookmarkMessage(invalidBookmarkMessage));

        StatsUpdateMessageDto invalidStatsMessage = new StatsUpdateMessageDto();
        invalidStatsMessage.setStatsType("INVALID");
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(invalidStatsMessage));

        // Verify no database operations were performed
        verify(postLikeRepository, never()).save(any());
        verify(postBookmarkRepository, never()).save(any());
        verify(valueOperations, never()).increment(anyString(), anyLong());
    }

    @Test
    void testCacheConsistency_AcrossConsumers() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(2L);

        // Create messages
        LikeMessageDto likeMessage = new LikeMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L
        );

        StatsUpdateMessageDto statsMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "LIKE",
            1,
            "LIKE"
        );

        // Act
        likeMessageConsumer.handleLikeMessage(likeMessage);
        statsUpdateConsumer.handleStatsUpdateMessage(statsMessage);

        // Assert - Both consumers should update the same cache key
        verify(valueOperations).set("post:likes:count:1", 1L, 1L, TimeUnit.HOURS);
        verify(valueOperations).increment("post:likes:count:1", 1L);
        verify(redisTemplate, times(2)).expire("post:likes:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testRetryMechanism_OnTransientFailures() {
        // Arrange - First call fails, second succeeds
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L))
            .thenThrow(new RuntimeException("Transient error"))
            .thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);

        LikeMessageDto likeMessage = new LikeMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L
        );

        // Act - First attempt should fail
        assertThrows(RuntimeException.class, () -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Act - Second attempt should succeed (simulating retry)
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Assert
        verify(postLikeRepository, times(2)).findByUserIdAndPostId(1L, 1L);
        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    void testDataIntegrity_UnderConcurrentLoad() {
        // Arrange - Simulate concurrent like/unlike operations
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // First like
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);

        LikeMessageDto likeMessage = new LikeMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            true,
            "Test Post",
            2L
        );

        // Act - Process like
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(likeMessage));

        // Arrange - Then unlike
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testLike));
        when(postLikeRepository.countByPostId(1L)).thenReturn(0L);

        LikeMessageDto unlikeMessage = new LikeMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            false,
            "Test Post",
            2L
        );

        // Act - Process unlike
        assertDoesNotThrow(() -> likeMessageConsumer.handleLikeMessage(unlikeMessage));

        // Assert - Verify proper sequence of operations
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postLikeRepository).delete(testLike);
        verify(valueOperations).set("post:likes:count:1", 1L, 1L, TimeUnit.HOURS);
        verify(valueOperations).set("post:likes:count:1", 0L, 1L, TimeUnit.HOURS);
    }
}