package com.myweb.website_core.service;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.interaction.PostBookmark;
import com.myweb.website_core.demos.web.interaction.PostBookmarkRepository;
import com.myweb.website_core.demos.web.interaction.PostLike;
import com.myweb.website_core.demos.web.interaction.PostLikeRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.InteractionResponse;
import com.myweb.website_core.dto.PostInteractionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PostInteractionServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostBookmarkRepository postBookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InteractionMessageService interactionMessageService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PostInteractionServiceImpl postInteractionService;

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
        testPost.setLikeCount(0);
        testPost.setCommentCount(0);
        testPost.setCollectCount(0);

        testLike = new PostLike(testUser, testPost);
        testLike.setId(1L);

        testBookmark = new PostBookmark(testUser, testPost);
        testBookmark.setId(1L);

        // Mock Redis operations with lenient stubbing
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testToggleLike_AddLike_Success() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);

        // Act
        CompletableFuture<InteractionResponse> future = postInteractionService.toggleLike(1L, 1L);
        InteractionResponse response = future.get();

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("like", response.getOperation());
        assertEquals(1L, response.getPostId());
        assertEquals(1L, response.getUserId());
        assertEquals(1L, response.getNewCount());
        assertTrue(response.isCurrentStatus());

        // Verify interactions
        verify(postLikeRepository).save(any(PostLike.class));
        verify(interactionMessageService).sendLikeMessage(eq(1L), eq("testuser"), eq(1L), eq(true), eq("Test Post"), eq(2L));
        verify(interactionMessageService).sendStatsUpdateMessage(eq(1L), eq("testuser"), eq(1L), eq("like"), eq(1), eq("LIKE"));
    }

    @Test
    void testToggleLike_RemoveLike_Success() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testLike));
        when(postLikeRepository.countByPostId(1L)).thenReturn(0L);

        // Act
        CompletableFuture<InteractionResponse> future = postInteractionService.toggleLike(1L, 1L);
        InteractionResponse response = future.get();

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("unlike", response.getOperation());
        assertEquals(1L, response.getPostId());
        assertEquals(1L, response.getUserId());
        assertEquals(0L, response.getNewCount());
        assertFalse(response.isCurrentStatus());

        // Verify interactions
        verify(postLikeRepository).delete(testLike);
        verify(interactionMessageService).sendLikeMessage(eq(1L), eq("testuser"), eq(1L), eq(false), eq("Test Post"), eq(2L));
        verify(interactionMessageService).sendStatsUpdateMessage(eq(1L), eq("testuser"), eq(1L), eq("unlike"), eq(-1), eq("LIKE"));
    }

    @Test
    void testToggleLike_UserNotFound_Failure() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        CompletableFuture<InteractionResponse> future = postInteractionService.toggleLike(1L, 1L);
        InteractionResponse response = future.get();

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("like", response.getOperation());
        assertEquals("用户不存在", response.getMessage());

        // Verify no interactions with repositories
        verify(postLikeRepository, never()).save(any());
        verify(postLikeRepository, never()).delete(any());
    }

    @Test
    void testToggleLike_PostNotFound_Failure() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        CompletableFuture<InteractionResponse> future = postInteractionService.toggleLike(1L, 1L);
        InteractionResponse response = future.get();

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("like", response.getOperation());
        assertEquals("帖子不存在", response.getMessage());

        // Verify no interactions with repositories
        verify(postLikeRepository, never()).save(any());
        verify(postLikeRepository, never()).delete(any());
    }

    @Test
    void testToggleBookmark_AddBookmark_Success() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenReturn(testBookmark);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(1L);

        // Act
        CompletableFuture<InteractionResponse> future = postInteractionService.toggleBookmark(1L, 1L);
        InteractionResponse response = future.get();

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("bookmark", response.getOperation());
        assertEquals(1L, response.getPostId());
        assertEquals(1L, response.getUserId());
        assertEquals(1L, response.getNewCount());
        assertTrue(response.isCurrentStatus());

        // Verify interactions
        verify(postBookmarkRepository).save(any(PostBookmark.class));
        verify(interactionMessageService).sendBookmarkMessage(eq(1L), eq("testuser"), eq(1L), eq(true), eq("Test Post"), eq(2L), eq("author"));
        verify(interactionMessageService).sendStatsUpdateMessage(eq(1L), eq("testuser"), eq(1L), eq("bookmark"), eq(1), eq("BOOKMARK"));
    }

    @Test
    void testToggleBookmark_RemoveBookmark_Success() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postBookmarkRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testBookmark));
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(0L);

        // Act
        CompletableFuture<InteractionResponse> future = postInteractionService.toggleBookmark(1L, 1L);
        InteractionResponse response = future.get();

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("unbookmark", response.getOperation());
        assertEquals(1L, response.getPostId());
        assertEquals(1L, response.getUserId());
        assertEquals(0L, response.getNewCount());
        assertFalse(response.isCurrentStatus());

        // Verify interactions
        verify(postBookmarkRepository).delete(testBookmark);
        verify(interactionMessageService).sendBookmarkMessage(eq(1L), eq("testuser"), eq(1L), eq(false), eq("Test Post"), eq(2L), eq("author"));
        verify(interactionMessageService).sendStatsUpdateMessage(eq(1L), eq("testuser"), eq(1L), eq("unbookmark"), eq(-1), eq("BOOKMARK"));
    }

    @Test
    void testGetInteractionStatus_LoggedInUser_Success() throws ExecutionException, InterruptedException {
        // Arrange
        when(postRepository.existsById(1L)).thenReturn(true);
        when(postLikeRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(true);
        when(postBookmarkRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(false);
        when(postLikeRepository.countByPostId(1L)).thenReturn(5L);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(3L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // Mock cache misses
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        CompletableFuture<PostInteractionStatus> future = postInteractionService.getInteractionStatus(1L, 1L);
        PostInteractionStatus status = future.get();

        // Assert
        assertNotNull(status);
        assertEquals(1L, status.getPostId());
        assertEquals(1L, status.getUserId());
        assertTrue(status.isLiked());
        assertFalse(status.isBookmarked());
        assertEquals(5L, status.getLikeCount());
        assertEquals(3L, status.getBookmarkCount());
        assertEquals(0L, status.getCommentCount()); // From testPost.getCommentCount()
    }

    @Test
    void testGetInteractionStatus_AnonymousUser_Success() throws ExecutionException, InterruptedException {
        // Arrange
        when(postRepository.existsById(1L)).thenReturn(true);
        when(postLikeRepository.countByPostId(1L)).thenReturn(5L);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(3L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // Mock cache misses
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        CompletableFuture<PostInteractionStatus> future = postInteractionService.getInteractionStatus(1L, null);
        PostInteractionStatus status = future.get();

        // Assert
        assertNotNull(status);
        assertEquals(1L, status.getPostId());
        assertNull(status.getUserId());
        assertFalse(status.isLiked());
        assertFalse(status.isBookmarked());
        assertEquals(5L, status.getLikeCount());
        assertEquals(3L, status.getBookmarkCount());
        assertEquals(0L, status.getCommentCount());

        // Verify user-specific checks were not called
        verify(postLikeRepository, never()).existsByUserIdAndPostId(anyLong(), anyLong());
        verify(postBookmarkRepository, never()).existsByUserIdAndPostId(anyLong(), anyLong());
    }

    @Test
    void testGetInteractionStatus_PostNotFound_ReturnsNull() throws ExecutionException, InterruptedException {
        // Arrange
        when(postRepository.existsById(1L)).thenReturn(false);

        // Act
        CompletableFuture<PostInteractionStatus> future = postInteractionService.getInteractionStatus(1L, 1L);
        PostInteractionStatus status = future.get();

        // Assert
        assertNull(status);
    }

    @Test
    void testGetUserBookmarks_Success() throws ExecutionException, InterruptedException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<PostBookmark> bookmarkList = Arrays.asList(testBookmark);
        Page<PostBookmark> bookmarkPage = new PageImpl<>(bookmarkList, pageable, 1);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(postBookmarkRepository.findUserBookmarksWithPostDetails(1L, pageable)).thenReturn(bookmarkPage);

        // Act
        CompletableFuture<Page<PostBookmark>> future = postInteractionService.getUserBookmarks(1L, pageable);
        Page<PostBookmark> result = future.get();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testBookmark, result.getContent().get(0));

        verify(postBookmarkRepository).findUserBookmarksWithPostDetails(1L, pageable);
    }

    @Test
    void testGetUserBookmarks_UserNotFound_ReturnsEmpty() throws ExecutionException, InterruptedException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.existsById(1L)).thenReturn(false);

        // Act
        CompletableFuture<Page<PostBookmark>> future = postInteractionService.getUserBookmarks(1L, pageable);
        Page<PostBookmark> result = future.get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(postBookmarkRepository, never()).findUserBookmarksWithPostDetails(anyLong(), any());
    }

    @Test
    void testCacheIntegration_LikeCount() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.existsById(1L)).thenReturn(true);
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(testLike);
        when(postLikeRepository.countByPostId(1L)).thenReturn(1L);
        when(postLikeRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(true);
        when(postBookmarkRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(false);
        when(postBookmarkRepository.countByPostId(1L)).thenReturn(0L);
        
        // Mock cache operations
        lenient().when(valueOperations.get(anyString())).thenReturn(null);

        // Act - First toggle like
        CompletableFuture<InteractionResponse> likeResponse = postInteractionService.toggleLike(1L, 1L);
        likeResponse.get();

        // Act - Then get interaction status
        CompletableFuture<PostInteractionStatus> statusResponse = postInteractionService.getInteractionStatus(1L, 1L);
        PostInteractionStatus status = statusResponse.get();

        // Assert
        assertNotNull(status);
        assertEquals(1L, status.getLikeCount());
        assertEquals(0L, status.getBookmarkCount());

        // Verify cache operations
        verify(redisTemplate, atLeastOnce()).delete(contains("post:likes:count:"));
    }

    @Test
    void testExceptionHandling_DatabaseError() throws ExecutionException, InterruptedException {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postLikeRepository.findByUserIdAndPostId(1L, 1L)).thenThrow(new RuntimeException("Database error"));

        // Act
        CompletableFuture<InteractionResponse> future = postInteractionService.toggleLike(1L, 1L);
        InteractionResponse response = future.get();

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("操作失败"));
        assertTrue(response.getMessage().contains("Database error"));
    }
}