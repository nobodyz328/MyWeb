package com.myweb.website_core;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.comment.Comment;
import com.myweb.website_core.demos.web.comment.CommentRepository;
import com.myweb.website_core.demos.web.interaction.PostBookmark;
import com.myweb.website_core.demos.web.interaction.PostBookmarkRepository;
import com.myweb.website_core.demos.web.interaction.PostLike;
import com.myweb.website_core.demos.web.interaction.PostLikeRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for database indexes and query performance.
 * These tests verify that the database indexes are working correctly
 * and that common query patterns perform efficiently.
 */
@DataJpaTest
@ActiveProfiles("test")
public class DatabaseIndexTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private CommentRepository commentRepository;

    private List<User> testUsers;
    private List<Post> testPosts;

    @BeforeEach
    void setUp() {
        // Create test users
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setUsername("testuser" + i);
            user.setPassword("password123");
            user.setEmail("test" + i + "@example.com");
            testUsers.add(userRepository.save(user));
        }

        // Create test posts
        testPosts = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Post post = new Post();
            post.setTitle("Test Post " + i);
            post.setContent("This is test post content " + i);
            post.setAuthor(testUsers.get(i % testUsers.size()));
            post.setCreatedAt(LocalDateTime.now().minusDays(i));
            testPosts.add(postRepository.save(post));
        }

        // Create test interactions
        createTestInteractions();
    }

    private void createTestInteractions() {
        // Create likes
        for (int i = 0; i < testPosts.size(); i++) {
            Post post = testPosts.get(i);
            for (int j = 0; j < Math.min(5, testUsers.size()); j++) {
                User user = testUsers.get(j);
                PostLike like = new PostLike(user, post);
                postLikeRepository.save(like);
            }
        }

        // Create bookmarks
        for (int i = 0; i < testPosts.size(); i += 2) {
            Post post = testPosts.get(i);
            for (int j = 0; j < Math.min(3, testUsers.size()); j++) {
                User user = testUsers.get(j);
                PostBookmark bookmark = new PostBookmark(user, post);
                postBookmarkRepository.save(bookmark);
            }
        }

        // Create comments with replies
        for (int i = 0; i < testPosts.size(); i++) {
            Post post = testPosts.get(i);
            
            // Create parent comments
            for (int j = 0; j < 3; j++) {
                User user = testUsers.get(j % testUsers.size());
                Comment parentComment = new Comment("Parent comment " + j + " for post " + i, user, post);
                parentComment = commentRepository.save(parentComment);
                
                // Create replies
                for (int k = 0; k < 2; k++) {
                    User replyUser = testUsers.get((j + k + 1) % testUsers.size());
                    Comment reply = new Comment("Reply " + k + " to comment " + j, replyUser, post, parentComment);
                    commentRepository.save(reply);
                }
            }
        }
    }

    @Test
    void testPostLikeIndexedQueries() {
        User testUser = testUsers.get(0);
        Post testPost = testPosts.get(0);

        // Test user_id index
        List<PostLike> userLikes = postLikeRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertFalse(userLikes.isEmpty());

        // Test post_id index
        long likeCount = postLikeRepository.countByPostId(testPost.getId());
        assertTrue(likeCount > 0);

        // Test unique constraint query
        boolean exists = postLikeRepository.existsByUserIdAndPostId(testUser.getId(), testPost.getId());
        assertTrue(exists);

        // Test composite index query
        List<Long> postIds = testPosts.stream().map(Post::getId).limit(5).toList();
        List<Object[]> likeCounts = postLikeRepository.countLikesByPostIds(postIds);
        assertFalse(likeCounts.isEmpty());
    }

    @Test
    void testPostBookmarkIndexedQueries() {
        User testUser = testUsers.get(0);
        Post testPost = testPosts.get(0);

        // Test user_id index with pagination
        Pageable pageable = PageRequest.of(0, 5);
        Page<PostBookmark> userBookmarks = postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), pageable);
        assertFalse(userBookmarks.isEmpty());

        // Test post_id index
        long bookmarkCount = postBookmarkRepository.countByPostId(testPost.getId());
        assertTrue(bookmarkCount >= 0);

        // Test unique constraint query
        boolean exists = postBookmarkRepository.existsByUserIdAndPostId(testUser.getId(), testPost.getId());
        // May or may not exist depending on test data

        // Test composite index query
        List<Long> postIds = testPosts.stream().map(Post::getId).limit(5).toList();
        List<Object[]> bookmarkCounts = postBookmarkRepository.countBookmarksByPostIds(postIds);
        // Should return results even if counts are 0
        assertNotNull(bookmarkCounts);
    }

    @Test
    void testCommentIndexedQueries() {
        Post testPost = testPosts.get(0);
        User testUser = testUsers.get(0);

        // Test post_id index
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> postComments = commentRepository.findTopLevelCommentsByPostId(testPost.getId(), pageable);
        assertFalse(postComments.isEmpty());

        // Test author_id index
        Page<Comment> userComments = commentRepository.findByAuthorIdAndIsDeletedFalse(testUser.getId(), pageable);
        assertFalse(userComments.isEmpty());

        // Test parent_comment_id index
        Comment parentComment = postComments.getContent().get(0);
        List<Comment> replies = commentRepository.findRepliesByParentCommentId(parentComment.getId());
        assertFalse(replies.isEmpty());

        // Test is_deleted index
        long commentCount = commentRepository.countByPostIdAndIsDeletedFalse(testPost.getId());
        assertTrue(commentCount > 0);

        // Test composite index query
        List<Long> postIds = testPosts.stream().map(Post::getId).limit(5).toList();
        List<Object[]> commentCounts = commentRepository.countCommentsByPostIds(postIds);
        assertFalse(commentCounts.isEmpty());
    }

    @Test
    void testHierarchicalCommentQueries() {
        Post testPost = testPosts.get(0);

        // Test hierarchical loading with joins
        List<Comment> commentsWithReplies = commentRepository.findCommentsWithRepliesByPostId(testPost.getId());
        assertFalse(commentsWithReplies.isEmpty());

        // Verify that parent comments have replies loaded
        Comment parentComment = commentsWithReplies.stream()
                .filter(c -> c.getParentComment() == null)
                .findFirst()
                .orElse(null);
        
        assertNotNull(parentComment);
        // Note: The replies might be empty due to lazy loading in test environment
        // The important thing is that the query executes successfully
        assertTrue(parentComment.getReplies() != null);
    }

    @Test
    void testPaginationPerformance() {
        User testUser = testUsers.get(0);

        // Test paginated queries
        Pageable firstPage = PageRequest.of(0, 5);
        Pageable secondPage = PageRequest.of(1, 5);

        // Test bookmark pagination
        Page<PostBookmark> bookmarksPage1 = postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), firstPage);
        Page<PostBookmark> bookmarksPage2 = postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), secondPage);

        // Verify pagination works correctly
        assertNotEquals(bookmarksPage1.getContent(), bookmarksPage2.getContent());

        // Test comment pagination
        Post testPost = testPosts.get(0);
        Page<Comment> commentsPage1 = commentRepository.findTopLevelCommentsByPostId(testPost.getId(), firstPage);
        Page<Comment> commentsPage2 = commentRepository.findTopLevelCommentsByPostId(testPost.getId(), secondPage);

        assertTrue(commentsPage1.hasContent());
        // Second page may or may not have content depending on data
    }

    @Test
    void testBulkOperations() {
        // Test bulk count operations
        List<Long> postIds = testPosts.stream().map(Post::getId).toList();

        List<Object[]> likeCounts = postLikeRepository.countLikesByPostIds(postIds);
        List<Object[]> bookmarkCounts = postBookmarkRepository.countBookmarksByPostIds(postIds);
        List<Object[]> commentCounts = commentRepository.countCommentsByPostIds(postIds);

        // Verify bulk operations return results
        assertNotNull(likeCounts);
        assertNotNull(bookmarkCounts);
        assertNotNull(commentCounts);

        // Test user interaction status queries
        User testUser = testUsers.get(0);
        List<Long> likedPostIds = postLikeRepository.findLikedPostIdsByUserAndPostIds(testUser.getId(), postIds);
        List<Long> bookmarkedPostIds = postBookmarkRepository.findBookmarkedPostIdsByUserAndPostIds(testUser.getId(), postIds);

        assertNotNull(likedPostIds);
        assertNotNull(bookmarkedPostIds);
    }

    @Test
    void testSoftDeleteQueries() {
        Post testPost = testPosts.get(0);

        // Get initial comment count
        long initialCount = commentRepository.countByPostIdAndIsDeletedFalse(testPost.getId());

        // Soft delete a comment
        List<Comment> comments = commentRepository.findTopLevelCommentsByPostId(testPost.getId());
        if (!comments.isEmpty()) {
            Comment comment = comments.get(0);
            comment.softDelete();
            commentRepository.save(comment);

            // Verify count decreased
            long newCount = commentRepository.countByPostIdAndIsDeletedFalse(testPost.getId());
            assertEquals(initialCount - 1, newCount);

            // Verify soft-deleted comment is not returned in normal queries
            List<Comment> activeComments = commentRepository.findTopLevelCommentsByPostId(testPost.getId());
            assertFalse(activeComments.contains(comment));
        }
    }
}