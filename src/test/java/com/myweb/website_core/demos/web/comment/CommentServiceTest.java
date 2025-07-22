package com.myweb.website_core.demos.web.comment;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.CommentCreateRequest;
import com.myweb.website_core.dto.CommentReplyRequest;
import com.myweb.website_core.exception.CommentNotFoundException;
import com.myweb.website_core.exception.InvalidCommentException;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private Post testPost;
    private Comment testComment;
    private Comment testParentComment;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Setup test post
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test post content");
        testPost.setAuthor(testUser);
        testPost.setCreatedAt(LocalDateTime.now());

        // Setup test comment
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setContent("Test comment");
        testComment.setAuthor(testUser);
        testComment.setPost(testPost);
        testComment.setCreatedAt(LocalDateTime.now());
        testComment.setIsDeleted(false);

        // Setup test parent comment
        testParentComment = new Comment();
        testParentComment.setId(2L);
        testParentComment.setContent("Parent comment");
        testParentComment.setAuthor(testUser);
        testParentComment.setPost(testPost);
        testParentComment.setCreatedAt(LocalDateTime.now());
        testParentComment.setIsDeleted(false);
    }

    @Test
    void createComment_Success() throws ExecutionException, InterruptedException {
        // Arrange
        CommentCreateRequest request = new CommentCreateRequest("Test comment", 1L, 1L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        CompletableFuture<Comment> result = commentService.createComment(request);
        Comment savedComment = result.get();

        // Assert
        assertNotNull(savedComment);
        assertEquals("Test comment", savedComment.getContent());
        assertEquals(testUser, savedComment.getAuthor());
        assertEquals(testPost, savedComment.getPost());
        
        verify(userRepository).findById(1L);
        verify(postRepository).findById(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_UserNotFound_ThrowsException() {
        // Arrange
        CommentCreateRequest request = new CommentCreateRequest("Test comment", 1L, 999L);
        
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.createComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(userRepository).findById(999L);
        verify(postRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_PostNotFound_ThrowsException() {
        // Arrange
        CommentCreateRequest request = new CommentCreateRequest("Test comment", 999L, 1L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.createComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(userRepository).findById(1L);
        verify(postRepository).findById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_EmptyContent_ThrowsException() {
        // Arrange
        CommentCreateRequest request = new CommentCreateRequest("", 1L, 1L);

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.createComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(userRepository, never()).findById(anyLong());
        verify(postRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_ContentTooLong_ThrowsException() {
        // Arrange
        String longContent = "a".repeat(2001);
        CommentCreateRequest request = new CommentCreateRequest(longContent, 1L, 1L);

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.createComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(userRepository, never()).findById(anyLong());
        verify(postRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void replyToComment_Success() throws ExecutionException, InterruptedException {
        // Arrange
        CommentReplyRequest request = new CommentReplyRequest("Test reply", 1L, 2L, 1L);
        Comment expectedReply = new Comment("Test reply", testUser, testPost, testParentComment);
        expectedReply.setId(3L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testParentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(expectedReply);

        // Act
        CompletableFuture<Comment> result = commentService.replyToComment(request);
        Comment savedReply = result.get();

        // Assert
        assertNotNull(savedReply);
        assertEquals("Test reply", savedReply.getContent());
        assertEquals(testUser, savedReply.getAuthor());
        assertEquals(testPost, savedReply.getPost());
        assertEquals(testParentComment, savedReply.getParentComment());
        
        verify(userRepository).findById(1L);
        verify(postRepository).findById(1L);
        verify(commentRepository).findById(2L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void replyToComment_ParentCommentNotFound_ThrowsException() {
        // Arrange
        CommentReplyRequest request = new CommentReplyRequest("Test reply", 1L, 999L, 1L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CommentNotFoundException.class, () -> {
            try {
                commentService.replyToComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(commentRepository).findById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void replyToComment_ParentCommentFromDifferentPost_ThrowsException() {
        // Arrange
        Post differentPost = new Post();
        differentPost.setId(2L);
        testParentComment.setPost(differentPost);
        
        CommentReplyRequest request = new CommentReplyRequest("Test reply", 1L, 2L, 1L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testParentComment));

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.replyToComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void replyToComment_ParentCommentDeleted_ThrowsException() {
        // Arrange
        testParentComment.setIsDeleted(true);
        CommentReplyRequest request = new CommentReplyRequest("Test reply", 1L, 2L, 1L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testParentComment));

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.replyToComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void getPostComments_Success() throws ExecutionException, InterruptedException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> comments = Arrays.asList(testComment, testParentComment);
        Page<Comment> commentsPage = new PageImpl<>(comments, pageable, comments.size());
        
        when(postRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findTopLevelCommentsByPostId(1L, pageable)).thenReturn(commentsPage);

        // Act
        CompletableFuture<List<Comment>> result = commentService.getPostComments(1L, pageable);
        List<Comment> retrievedComments = result.get();

        // Assert
        assertNotNull(retrievedComments);
        assertEquals(2, retrievedComments.size());
        assertTrue(retrievedComments.contains(testComment));
        assertTrue(retrievedComments.contains(testParentComment));
        
        verify(postRepository).existsById(1L);
        verify(commentRepository).findTopLevelCommentsByPostId(1L, pageable);
    }

    @Test
    void getPostComments_PostNotFound_ThrowsException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        
        when(postRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.getPostComments(999L, pageable).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(postRepository).existsById(999L);
        verify(commentRepository, never()).findTopLevelCommentsByPostId(anyLong(), any(Pageable.class));
    }

    @Test
    void getCommentReplies_Success() throws ExecutionException, InterruptedException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Comment reply1 = new Comment("Reply 1", testUser, testPost, testParentComment);
        Comment reply2 = new Comment("Reply 2", testUser, testPost, testParentComment);
        List<Comment> replies = Arrays.asList(reply1, reply2);
        Page<Comment> repliesPage = new PageImpl<>(replies, pageable, replies.size());
        
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testParentComment));
        when(commentRepository.findRepliesByParentCommentId(2L, pageable)).thenReturn(repliesPage);

        // Act
        CompletableFuture<List<Comment>> result = commentService.getCommentReplies(2L, pageable);
        List<Comment> retrievedReplies = result.get();

        // Assert
        assertNotNull(retrievedReplies);
        assertEquals(2, retrievedReplies.size());
        assertTrue(retrievedReplies.contains(reply1));
        assertTrue(retrievedReplies.contains(reply2));
        
        verify(commentRepository).findById(2L);
        verify(commentRepository).findRepliesByParentCommentId(2L, pageable);
    }

    @Test
    void getCommentReplies_ParentCommentNotFound_ThrowsException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CommentNotFoundException.class, () -> {
            try {
                commentService.getCommentReplies(999L, pageable).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(commentRepository).findById(999L);
        verify(commentRepository, never()).findRepliesByParentCommentId(anyLong(), any(Pageable.class));
    }

    @Test
    void getCommentReplies_ParentCommentDeleted_ThrowsException() {
        // Arrange
        testParentComment.setIsDeleted(true);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testParentComment));

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.getCommentReplies(2L, pageable).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(commentRepository).findById(2L);
        verify(commentRepository, never()).findRepliesByParentCommentId(anyLong(), any(Pageable.class));
    }

    @Test
    void getPostCommentsWithReplies_Success() throws ExecutionException, InterruptedException {
        // Arrange
        List<Comment> commentsWithReplies = Arrays.asList(testComment, testParentComment);
        
        when(postRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findCommentsWithRepliesByPostId(1L)).thenReturn(commentsWithReplies);

        // Act
        CompletableFuture<List<Comment>> result = commentService.getPostCommentsWithReplies(1L);
        List<Comment> retrievedComments = result.get();

        // Assert
        assertNotNull(retrievedComments);
        assertEquals(2, retrievedComments.size());
        assertTrue(retrievedComments.contains(testComment));
        assertTrue(retrievedComments.contains(testParentComment));
        
        verify(postRepository).existsById(1L);
        verify(commentRepository).findCommentsWithRepliesByPostId(1L);
    }

    @Test
    void createComment_MaliciousContent_ThrowsException() {
        // Arrange
        CommentCreateRequest request = new CommentCreateRequest("<script>alert('xss')</script>", 1L, 1L);

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.createComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(userRepository, never()).findById(anyLong());
        verify(postRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_JavaScriptContent_ThrowsException() {
        // Arrange
        CommentCreateRequest request = new CommentCreateRequest("javascript:alert('xss')", 1L, 1L);

        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.createComment(request).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(userRepository, never()).findById(anyLong());
        verify(postRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void legacyAddComment_Success() throws ExecutionException, InterruptedException {
        // Arrange
        when(commentRepository.save(testComment)).thenReturn(testComment);

        // Act
        CompletableFuture<Comment> result = commentService.addComment(testComment);
        Comment savedComment = result.get();

        // Assert
        assertNotNull(savedComment);
        assertEquals(testComment, savedComment);
        
        verify(commentRepository).save(testComment);
    }

    @Test
    void legacyAddComment_NullComment_ThrowsException() {
        // Act & Assert
        assertThrows(InvalidCommentException.class, () -> {
            try {
                commentService.addComment(null).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
        
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void legacyGetCommentsByPost_Success() throws ExecutionException, InterruptedException {
        // Arrange
        List<Comment> comments = Arrays.asList(testComment, testParentComment);
        
        when(commentRepository.findTopLevelCommentsByPostId(1L)).thenReturn(comments);

        // Act
        CompletableFuture<List<Comment>> result = commentService.getCommentsByPost(1L);
        List<Comment> retrievedComments = result.get();

        // Assert
        assertNotNull(retrievedComments);
        assertEquals(2, retrievedComments.size());
        assertTrue(retrievedComments.contains(testComment));
        assertTrue(retrievedComments.contains(testParentComment));
        
        verify(commentRepository).findTopLevelCommentsByPostId(1L);
    }
}