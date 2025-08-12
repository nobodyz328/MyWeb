package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全Repository集成测试
 * <p>
 * 测试安全Repository基类扩展的功能：
 * 1. 安全查询方法
 * 2. 参数验证
 * 3. SQL注入防护
 * 4. 分页和排序安全
 * <p>
 * 符合需求：5.1, 5.4, 5.6 - 安全查询服务集成测试
 * 
 * @author MyWeb
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SafeRepositoryIntegrationTest {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    /**
     * 测试PostRepository安全查询功能
     */
    @Test
    public void testPostRepositorySafeQuery() {
        // 测试安全搜索
        Page<Post> searchResult = postRepository.findPostsWithSafeSearch(
            "test", "id", "ASC", 0, 10
        );
        assertNotNull(searchResult);
        assertTrue(searchResult.getSize() <= 10);
        
        // 测试安全分页
        Page<Post> paginatedResult = postRepository.findPostsByAuthorWithSafePagination(
            1L, 0, 5
        );
        assertNotNull(paginatedResult);
        assertTrue(paginatedResult.getSize() <= 5);
        
        // 测试安全条件查询
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("id", 1L);
        List<Post> conditionResult = postRepository.findPostsByConditionsSafely(
            conditions, "id", "ASC"
        );
        assertNotNull(conditionResult);
        
        // 测试安全统计
        long count = postRepository.countPostsSafely(conditions);
        assertTrue(count >= 0);
    }
    
    /**
     * 测试UserRepository安全查询功能
     */
    @Test
    public void testUserRepositorySafeQuery() {
        // 测试安全搜索
        Page<User> searchResult = userRepository.findUsersWithSafeSearch(
            "test", "id", "ASC", 0, 10
        );
        assertNotNull(searchResult);
        assertTrue(searchResult.getSize() <= 10);
        
        // 测试安全用户名搜索
        Page<User> usernameResult = userRepository.findByUsernameContainingSafely(
            "test", PageRequest.of(0, 5)
        );
        assertNotNull(usernameResult);
        
        // 测试安全分页查询
        Map<String, Object> conditions = new HashMap<>();
        Page<User> paginatedResult = userRepository.findUsersWithSafePagination(
            conditions, "id", "ASC", 0, 10
        );
        assertNotNull(paginatedResult);
        
        // 测试安全统计
        long count = userRepository.countUsersSafely(conditions);
        assertTrue(count >= 0);
    }
    
    /**
     * 测试CommentRepository安全查询功能
     */
    @Test
    public void testCommentRepositorySafeQuery() {
        // 测试安全搜索
        Page<Comment> searchResult = commentRepository.findCommentsWithSafeSearch(
            "test", "id", "ASC", 0, 10
        );
        assertNotNull(searchResult);
        assertTrue(searchResult.getSize() <= 10);
        
        // 测试安全帖子评论查询
        Page<Comment> postCommentsResult = commentRepository.findCommentsByPostWithSafePagination(
            1L, 0, 5
        );
        assertNotNull(postCommentsResult);
        
        // 测试安全顶级评论查询
        Page<Comment> topLevelResult = commentRepository.findTopLevelCommentsSafely(
            1L, PageRequest.of(0, 10)
        );
        assertNotNull(topLevelResult);
        
        // 测试安全统计
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("post.id", 1L);
        long count = commentRepository.countCommentsSafely(conditions);
        assertTrue(count >= 0);
    }
    
    /**
     * 测试参数验证功能
     */
    @Test
    public void testParameterValidation() {
        // 测试无效的分页参数
        assertThrows(IllegalArgumentException.class, () -> {
            postRepository.findPostsWithSafeSearch("test", "id", "ASC", -1, 10);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            postRepository.findPostsWithSafeSearch("test", "id", "ASC", 0, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            postRepository.findPostsWithSafeSearch("test", "id", "ASC", 0, 2000);
        });
        
        // 测试无效的排序字段
        assertThrows(IllegalArgumentException.class, () -> {
            postRepository.findPostsWithSafeSearch("test", "invalid_field", "ASC", 0, 10);
        });
        
        // 测试无效的排序方向
        assertThrows(IllegalArgumentException.class, () -> {
            postRepository.findPostsWithSafeSearch("test", "id", "INVALID", 0, 10);
        });
    }
    
    /**
     * 测试SQL注入防护
     */
    @Test
    public void testSqlInjectionProtection() {
        // 测试恶意搜索关键词
        String maliciousKeyword = "'; DROP TABLE posts; --";
        
        // 这些调用应该不会抛出异常，而是被安全处理
        assertDoesNotThrow(() -> {
            postRepository.findPostsWithSafeSearch(maliciousKeyword, "id", "ASC", 0, 10);
        });
        
        assertDoesNotThrow(() -> {
            userRepository.findUsersWithSafeSearch(maliciousKeyword, "id", "ASC", 0, 10);
        });
        
        assertDoesNotThrow(() -> {
            commentRepository.findCommentsWithSafeSearch(maliciousKeyword, "id", "ASC", 0, 10);
        });
    }
}