package com.myweb.website_core.application.task;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 数据完整性检查定时任务测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DataIntegrityTaskTest {
    
    @Mock
    private DataIntegrityService dataIntegrityService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private CommentRepository commentRepository;
    
    private DataIntegrityTask dataIntegrityTask;
    
    @BeforeEach
    void setUp() {
        dataIntegrityTask = new DataIntegrityTask(
            dataIntegrityService, auditLogService, postRepository, commentRepository);
        
        // 设置配置值
        ReflectionTestUtils.setField(dataIntegrityTask, "alertThreshold", 10);
        ReflectionTestUtils.setField(dataIntegrityTask, "batchSize", 100);
    }
    
    @Test
    void testPerformDailyIntegrityCheck() {
        // 准备测试数据
        Post post = createTestPost(1L, "测试帖子", "测试内容", "hash1");
        Comment comment = createTestComment(1L, "测试评论", "hash2");
        
        when(postRepository.findAll()).thenReturn(Arrays.asList(post));
        when(commentRepository.findAll()).thenReturn(Arrays.asList(comment));
        
        // 模拟完整性检查结果
        DataIntegrityService.IntegrityCheckResult validResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(1L)
                .isValid(true)
                .checkTime(LocalDateTime.now())
                .build();
        
        when(dataIntegrityService.checkPostIntegrity(anyLong(), anyString(), anyString()))
            .thenReturn(validResult);
        when(dataIntegrityService.checkCommentIntegrity(anyLong(), anyString(), anyString()))
            .thenReturn(validResult);
        
        // 执行测试
        assertDoesNotThrow(() -> dataIntegrityTask.performDailyIntegrityCheck());
        
        // 验证审计日志被调用
        verify(auditLogService, atLeastOnce()).logSecurityEvent(any(), eq("SYSTEM"), anyString());
    }
    
    @Test
    void testCheckAllPostsIntegrityActual() throws Exception {
        // 准备测试数据
        Post post1 = createTestPost(1L, "帖子1", "内容1", "hash1");
        Post post2 = createTestPost(2L, "帖子2", "内容2", "hash2");
        List<Post> posts = Arrays.asList(post1, post2);
        
        when(postRepository.findAll()).thenReturn(posts);
        
        // 模拟完整性检查结果 - 一个有问题，一个正常
        DataIntegrityService.IntegrityCheckResult validResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(1L)
                .isValid(true)
                .checkTime(LocalDateTime.now())
                .build();
        
        DataIntegrityService.IntegrityCheckResult invalidResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(2L)
                .isValid(false)
                .errorMessage("哈希值不匹配")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(dataIntegrityService.checkPostIntegrity(1L, "内容1", "hash1"))
            .thenReturn(validResult);
        when(dataIntegrityService.checkPostIntegrity(2L, "内容2", "hash2"))
            .thenReturn(invalidResult);
        
        // 执行测试
        CompletableFuture<Integer> result = dataIntegrityTask.checkAllPostsIntegrityActual();
        Integer issueCount = result.get();
        
        // 验证结果
        assertEquals(1, issueCount);
        verify(dataIntegrityService, times(2)).checkPostIntegrity(anyLong(), anyString(), anyString());
        verify(auditLogService).logSecurityEvent(any(), eq("SYSTEM"), contains("帖子完整性问题"));
    }
    
    @Test
    void testCheckAllCommentsIntegrityActual() throws Exception {
        // 准备测试数据
        Comment comment1 = createTestComment(1L, "评论1", "hash1");
        Comment comment2 = createTestComment(2L, "评论2", "hash2");
        List<Comment> comments = Arrays.asList(comment1, comment2);
        
        when(commentRepository.findAll()).thenReturn(comments);
        
        // 模拟完整性检查结果 - 都正常
        DataIntegrityService.IntegrityCheckResult validResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("COMMENT")
                .isValid(true)
                .checkTime(LocalDateTime.now())
                .build();
        
        when(dataIntegrityService.checkCommentIntegrity(anyLong(), anyString(), anyString()))
            .thenReturn(validResult);
        
        // 执行测试
        CompletableFuture<Integer> result = dataIntegrityTask.checkAllCommentsIntegrityActual();
        Integer issueCount = result.get();
        
        // 验证结果
        assertEquals(0, issueCount);
        verify(dataIntegrityService, times(2)).checkCommentIntegrity(anyLong(), anyString(), anyString());
    }
    
    @Test
    void testGenerateRepairSuggestions() {
        // 准备测试数据
        DataIntegrityTask.IntegrityCheckReport report = new DataIntegrityTask.IntegrityCheckReport();
        report.setPostIssues(5);
        report.setCommentIssues(3);
        report.setTotalIssues(8);
        report.setDeepCheck(false);
        
        // 执行测试
        List<String> suggestions = dataIntegrityTask.generateRepairSuggestions(report);
        
        // 验证结果
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("帖子完整性问题")));
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("评论完整性问题")));
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("通用建议")));
    }
    
    @Test
    void testGenerateRepairSuggestionsWithHighIssueCount() {
        // 准备测试数据 - 问题数量超过阈值
        DataIntegrityTask.IntegrityCheckReport report = new DataIntegrityTask.IntegrityCheckReport();
        report.setPostIssues(8);
        report.setCommentIssues(5);
        report.setTotalIssues(13); // 超过阈值10
        report.setDeepCheck(false);
        
        // 执行测试
        List<String> suggestions = dataIntegrityTask.generateRepairSuggestions(report);
        
        // 验证结果
        assertNotNull(suggestions);
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("超过告警阈值")));
    }
    
    @Test
    void testTriggerManualIntegrityCheck() throws Exception {
        // 准备测试数据
        when(postRepository.findAll()).thenReturn(Arrays.asList());
        when(commentRepository.findAll()).thenReturn(Arrays.asList());
        
        // 执行测试
        CompletableFuture<DataIntegrityTask.IntegrityCheckReport> result = 
            dataIntegrityTask.triggerManualIntegrityCheck();
        
        // 验证结果
        assertNotNull(result);
        DataIntegrityTask.IntegrityCheckReport report = result.get();
        assertNotNull(report);
        assertNotNull(report.getCheckTime());
    }
    
    @Test
    void testIntegrityCheckReportToString() {
        // 准备测试数据
        DataIntegrityTask.IntegrityCheckReport report = new DataIntegrityTask.IntegrityCheckReport();
        report.setCheckTime(LocalDateTime.now());
        report.setPostsChecked(10);
        report.setCommentsChecked(20);
        report.setPostIssues(1);
        report.setCommentIssues(2);
        report.setTotalIssues(3);
        report.setDeepCheck(true);
        
        // 执行测试
        String result = report.toString();
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("IntegrityCheckReport"));
        assertTrue(result.contains("postsChecked=10"));
        assertTrue(result.contains("commentsChecked=20"));
        assertTrue(result.contains("totalIssues=3"));
        assertTrue(result.contains("deepCheck=true"));
    }
    
    @Test
    void testIntegrityStatisticsReportToString() {
        // 准备测试数据
        DataIntegrityTask.IntegrityStatisticsReport report = new DataIntegrityTask.IntegrityStatisticsReport();
        report.setReportTime(LocalDateTime.now());
        report.setTotalChecked(100);
        report.setIssuesFound(5);
        report.setIntegrityRate(95.0);
        report.setTrend("良好");
        
        // 执行测试
        String result = report.toString();
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("IntegrityStatisticsReport"));
        assertTrue(result.contains("totalChecked=100"));
        assertTrue(result.contains("issuesFound=5"));
        assertTrue(result.contains("integrityRate=95.00%"));
        assertTrue(result.contains("trend='良好'"));
    }
    
    // ==================== 辅助方法 ====================
    
    private Post createTestPost(Long id, String title, String content, String contentHash) {
        Post post = new Post();
        ReflectionTestUtils.setField(post, "id", id);
        post.setTitle(title);
        post.setContent(content);
        post.setContentHash(contentHash);
        post.setHashCalculatedAt(LocalDateTime.now());
        post.setCreatedAt(LocalDateTime.now());
        return post;
    }
    
    private Comment createTestComment(Long id, String content, String contentHash) {
        Comment comment = new Comment();
        ReflectionTestUtils.setField(comment, "id", id);
        comment.setContent(content);
        comment.setContentHash(contentHash);
        comment.setHashCalculatedAt(LocalDateTime.now());
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }
}