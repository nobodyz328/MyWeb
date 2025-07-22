package com.myweb.website_core.service;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.dto.StatsUpdateMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsUpdateConsumerTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private StatsUpdateConsumer statsUpdateConsumer;

    private Post testPost;
    private StatsUpdateMessageDto likeStatsMessage;
    private StatsUpdateMessageDto bookmarkStatsMessage;
    private StatsUpdateMessageDto commentStatsMessage;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        User postAuthor = new User();
        postAuthor.setId(2L);
        postAuthor.setUsername("author");

        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setAuthor(postAuthor);
        testPost.setCreatedAt(LocalDateTime.now());

        // 创建点赞统计消息
        likeStatsMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "LIKE",
            1,
            "LIKE"
        );

        // 创建书签统计消息
        bookmarkStatsMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "BOOKMARK",
            1,
            "BOOKMARK"
        );

        // 创建评论统计消息
        commentStatsMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "COMMENT",
            1,
            "COMMENT"
        );

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testHandleStatsUpdateMessage_LikeStats_Success() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert
        verify(postRepository).findById(1L);
        verify(valueOperations).increment("post:likes:count:1", 1L);
        verify(redisTemplate).expire("post:likes:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testHandleStatsUpdateMessage_BookmarkStats_Success() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(bookmarkStatsMessage));

        // Assert
        verify(postRepository).findById(1L);
        verify(valueOperations).increment("post:bookmarks:count:1", 1L);
        verify(redisTemplate).expire("post:bookmarks:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testHandleStatsUpdateMessage_CommentStats_Success() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(commentStatsMessage));

        // Assert
        verify(postRepository).findById(1L);
        verify(valueOperations).increment("post:comments:count:1", 1L);
        verify(redisTemplate).expire("post:comments:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testHandleStatsUpdateMessage_NegativeCount_SetsToZero() {
        // Arrange
        StatsUpdateMessageDto negativeMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "UNLIKE",
            -2,
            "LIKE"
        );
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(-1L);

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(negativeMessage));

        // Assert
        verify(valueOperations).increment("post:likes:count:1", -2L);
        verify(valueOperations).set("post:likes:count:1", 0);
    }

    @Test
    void testHandleStatsUpdateMessage_PostNotFound_NoOperation() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert
        verify(postRepository).findById(1L);
        verify(valueOperations, never()).increment(anyString(), anyLong());
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testHandleStatsUpdateMessage_InvalidStatsType_NoOperation() {
        // Arrange
        StatsUpdateMessageDto invalidMessage = new StatsUpdateMessageDto(
            UUID.randomUUID().toString(),
            1L,
            "testuser",
            1L,
            "INVALID_OPERATION",
            1,
            "INVALID_TYPE"
        );
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(invalidMessage));

        // Assert
        verify(postRepository).findById(1L);
        verify(valueOperations, never()).increment(anyString(), anyLong());
    }

    @Test
    void testHandleStatsUpdateMessage_InvalidMessage_NoOperation() {
        // Test null message
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(null));

        // Test message with null messageId
        StatsUpdateMessageDto invalidMessage = new StatsUpdateMessageDto();
        invalidMessage.setMessageId(null);
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setOperationType("LIKE");
        invalidMessage.setStatsType("LIKE");
        StatsUpdateMessageDto finalInvalidMessage = invalidMessage;
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(finalInvalidMessage));

        // Test message with invalid userId
        invalidMessage = new StatsUpdateMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(null);
        invalidMessage.setPostId(1L);
        invalidMessage.setOperationType("LIKE");
        invalidMessage.setStatsType("LIKE");
        StatsUpdateMessageDto finalInvalidMessage1 = invalidMessage;
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(finalInvalidMessage1));

        // Test message with invalid postId
        invalidMessage = new StatsUpdateMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(null);
        invalidMessage.setOperationType("LIKE");
        invalidMessage.setStatsType("LIKE");
        StatsUpdateMessageDto finalInvalidMessage2 = invalidMessage;
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(finalInvalidMessage2));

        // Test message with empty operationType
        invalidMessage = new StatsUpdateMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setOperationType("");
        invalidMessage.setStatsType("LIKE");
        StatsUpdateMessageDto finalInvalidMessage3 = invalidMessage;
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(finalInvalidMessage3));

        // Test message with empty statsType
        invalidMessage = new StatsUpdateMessageDto();
        invalidMessage.setMessageId(UUID.randomUUID().toString());
        invalidMessage.setUserId(1L);
        invalidMessage.setPostId(1L);
        invalidMessage.setOperationType("LIKE");
        invalidMessage.setStatsType("");
        StatsUpdateMessageDto finalInvalidMessage4 = invalidMessage;
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(finalInvalidMessage4));

        // Verify no database operations were performed
        verify(postRepository, never()).findById(anyLong());
        verify(valueOperations, never()).increment(anyString(), anyLong());
    }

    @Test
    void testUpdateAggregateStats_Success() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);
        
        Map<String, Object> existingStats = new HashMap<>();
        existingStats.put("like_count", 5L);
        when(valueOperations.get("post:stats:1")).thenReturn(existingStats);

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert
        verify(valueOperations).get("post:stats:1");
        verify(valueOperations).set(eq("post:stats:1"), any(Map.class), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    void testUpdateUserActivityStats_Success() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);
        
        Map<String, Object> existingActivity = new HashMap<>();
        existingActivity.put("like", 3L);
        when(valueOperations.get("user:activity:1")).thenReturn(existingActivity);

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert
        verify(valueOperations).get("user:activity:1");
        verify(valueOperations).set(eq("user:activity:1"), any(Map.class), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testUpdateDailyStats_Success() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);
        
        String today = LocalDateTime.now().toLocalDate().toString();
        Map<String, Object> existingDailyStats = new HashMap<>();
        existingDailyStats.put("like_count", 10L);
        when(valueOperations.get("stats:daily:" + today)).thenReturn(existingDailyStats);

        // Act
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert
        verify(valueOperations).get("stats:daily:" + today);
        verify(valueOperations).set(eq("stats:daily:" + today), any(Map.class), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    void testHandleStatsUpdateMessage_DatabaseException_ThrowsException() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenThrow(new RuntimeException("Redis error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        verify(postRepository).findById(1L);
        verify(valueOperations).increment("post:likes:count:1", 1L);
    }

    @Test
    void testGetPostStats() {
        // Arrange
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put("like_count", 5L);
        expectedStats.put("bookmark_count", 3L);
        when(valueOperations.get("post:stats:1")).thenReturn(expectedStats);

        // Act
        Map<String, Object> result = statsUpdateConsumer.getPostStats(1L);

        // Assert
        assertEquals(expectedStats, result);
        verify(valueOperations).get("post:stats:1");
    }

    @Test
    void testGetPostStats_NotFound_ReturnsEmptyMap() {
        // Arrange
        when(valueOperations.get("post:stats:1")).thenReturn(null);

        // Act
        Map<String, Object> result = statsUpdateConsumer.getPostStats(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(valueOperations).get("post:stats:1");
    }

    @Test
    void testGetDailyStats() {
        // Arrange
        String date = "2024-01-15";
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put("like_count", 100L);
        expectedStats.put("bookmark_count", 50L);
        when(valueOperations.get("stats:daily:" + date)).thenReturn(expectedStats);

        // Act
        Map<String, Object> result = statsUpdateConsumer.getDailyStats(date);

        // Assert
        assertEquals(expectedStats, result);
        verify(valueOperations).get("stats:daily:" + date);
    }

    @Test
    void testGetDailyStats_NotFound_ReturnsEmptyMap() {
        // Arrange
        String date = "2024-01-15";
        when(valueOperations.get("stats:daily:" + date)).thenReturn(null);

        // Act
        Map<String, Object> result = statsUpdateConsumer.getDailyStats(date);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(valueOperations).get("stats:daily:" + date);
    }

    @Test
    void testGetUserActivityStats() {
        // Arrange
        Map<String, Object> expectedActivity = new HashMap<>();
        expectedActivity.put("like", 10L);
        expectedActivity.put("bookmark", 5L);
        when(valueOperations.get("user:activity:1")).thenReturn(expectedActivity);

        // Act
        Map<String, Object> result = statsUpdateConsumer.getUserActivityStats(1L);

        // Assert
        assertEquals(expectedActivity, result);
        verify(valueOperations).get("user:activity:1");
    }

    @Test
    void testGetUserActivityStats_NotFound_ReturnsEmptyMap() {
        // Arrange
        when(valueOperations.get("user:activity:1")).thenReturn(null);

        // Act
        Map<String, Object> result = statsUpdateConsumer.getUserActivityStats(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(valueOperations).get("user:activity:1");
    }

    @Test
    void testValidStatsTypes() {
        // Test valid stats types
        assertTrue(statsUpdateConsumer.getClass().getDeclaredMethods().length > 0);
        
        // Test LIKE stats type
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Test BOOKMARK stats type
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(bookmarkStatsMessage));

        // Test COMMENT stats type
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(commentStatsMessage));
    }

    @Test
    void testAggregateStatsFailure_DoesNotAffectMainStats() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment("post:likes:count:1", 1L)).thenReturn(1L);
        when(valueOperations.get("post:stats:1")).thenThrow(new RuntimeException("Aggregate stats error"));

        // Act - 综合统计失败不应该影响主要统计
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert - 主要统计应该正常执行
        verify(valueOperations).increment("post:likes:count:1", 1L);
        verify(redisTemplate).expire("post:likes:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testUserActivityStatsFailure_DoesNotAffectMainStats() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment("post:likes:count:1", 1L)).thenReturn(1L);
        when(valueOperations.get("user:activity:1")).thenThrow(new RuntimeException("User activity error"));

        // Act - 用户活动统计失败不应该影响主要统计
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert - 主要统计应该正常执行
        verify(valueOperations).increment("post:likes:count:1", 1L);
        verify(redisTemplate).expire("post:likes:count:1", 1, TimeUnit.HOURS);
    }

    @Test
    void testDailyStatsFailure_DoesNotAffectMainStats() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(valueOperations.increment("post:likes:count:1", 1L)).thenReturn(1L);
        
        String today = LocalDateTime.now().toLocalDate().toString();
        when(valueOperations.get("stats:daily:" + today)).thenThrow(new RuntimeException("Daily stats error"));

        // Act - 每日统计失败不应该影响主要统计
        assertDoesNotThrow(() -> statsUpdateConsumer.handleStatsUpdateMessage(likeStatsMessage));

        // Assert - 主要统计应该正常执行
        verify(valueOperations).increment("post:likes:count:1", 1L);
        verify(redisTemplate).expire("post:likes:count:1", 1, TimeUnit.HOURS);
    }
}