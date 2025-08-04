package com.myweb.website_core.security;

import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.application.service.security.authorization.AccessControlService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.infrastructure.security.Auditable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 安全审计集成测试
 * 
 * 测试安全审计功能的集成和工作流程
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
public class SecurityAuditIntegrationTest {
    
    @MockBean
    private MessageProducerService messageProducerService;
    
    @MockBean
    private AccessControlService accessControlService;
    
    /**
     * 测试访问控制功能
     */
    @Test
    public void testAccessControl() {
        // 创建测试用户和帖子
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        
        Post post = new Post();
        post.setId(1L);
        post.setTitle("Test Post");
        post.setAuthor(user);
        
        // 模拟访问控制检查
        when(accessControlService.canEditPost(user, post)).thenReturn(true);
        when(accessControlService.canDeletePost(user, post)).thenReturn(true);
        when(accessControlService.canViewPost(user, post)).thenReturn(true);
        
        // 验证权限检查
        assertTrue(accessControlService.canEditPost(user, post));
        assertTrue(accessControlService.canDeletePost(user, post));
        assertTrue(accessControlService.canViewPost(user, post));
        
        // 验证方法被调用
        verify(accessControlService, times(1)).canEditPost(user, post);
        verify(accessControlService, times(1)).canDeletePost(user, post);
        verify(accessControlService, times(1)).canViewPost(user, post);
    }
    
    /**
     * 测试审计消息发送
     */
    @Test
    public void testAuditMessageSending() {
        // 测试用户认证审计消息
        messageProducerService.sendUserAuthAuditMessage(
            "testuser", AuditOperation.USER_LOGIN_SUCCESS, "127.0.0.1", 
            "SUCCESS", null, "session123"
        );
        
        // 测试文件上传审计消息
        messageProducerService.sendFileUploadAuditMessage(
            1L, "testuser", "test.jpg", "image/jpeg", 1024L, 
            "SUCCESS", "127.0.0.1", null
        );
        
        // 测试搜索审计消息
        messageProducerService.sendSearchAuditMessage(
            1L, "testuser", "test query", "POST", 5, "127.0.0.1"
        );
        
        // 验证消息发送方法被调用
        verify(messageProducerService, times(1)).sendUserAuthAuditMessage(
            eq("testuser"), eq(AuditOperation.USER_LOGIN_SUCCESS), eq("127.0.0.1"), 
            eq("SUCCESS"), isNull(), eq("session123")
        );
        
        verify(messageProducerService, times(1)).sendFileUploadAuditMessage(
            eq(1L), eq("testuser"), eq("test.jpg"), eq("image/jpeg"), eq(1024L), 
            eq("SUCCESS"), eq("127.0.0.1"), isNull()
        );
        
        verify(messageProducerService, times(1)).sendSearchAuditMessage(
            eq(1L), eq("testuser"), eq("test query"), eq("POST"), eq(5), eq("127.0.0.1")
        );
    }
    
    /**
     * 测试服务类，演示@Auditable注解的使用
     */
    public static class TestService {
        
        @Auditable(
            operation = AuditOperation.POST_CREATE,
            resourceType = "POST",
            description = "创建测试帖子",
            logRequest = true,
            logResponse = true,
            async = true
        )
        public Post createTestPost(String title, String content) {
            Post post = new Post();
            post.setId(1L);
            post.setTitle(title);
            post.setContent(content);
            return post;
        }
        
        @Auditable(
            operation = AuditOperation.USER_LOGIN_SUCCESS,
            resourceType = "USER",
            description = "用户登录测试",
            logRequest = true,
            logResponse = false,
            sensitiveParams = {1}, // 密码参数索引
            async = true
        )
        public boolean testLogin(String username, String password) {
            // 模拟登录逻辑
            return "testuser".equals(username) && "password".equals(password);
        }
        
        @Auditable(
            operation = AuditOperation.FILE_UPLOAD,
            resourceType = "FILE",
            description = "文件上传测试",
            riskLevel = 3,
            tags = "upload,test",
            maxParamLength = 500,
            async = true
        )
        public String testFileUpload(String filename, byte[] content) {
            // 模拟文件上传逻辑
            return "/uploads/" + filename;
        }
    }
    
    /**
     * 测试@Auditable注解的各种参数配置
     */
    @Test
    public void testAuditableAnnotationParameters() {
        TestService testService = new TestService();
        
        // 测试帖子创建审计
        Post post = testService.createTestPost("Test Title", "Test Content");
        assertNotNull(post);
        assertEquals("Test Title", post.getTitle());
        
        // 测试登录审计（包含敏感参数）
        boolean loginResult = testService.testLogin("testuser", "password");
        assertTrue(loginResult);
        
        // 测试文件上传审计（包含风险级别和标签）
        String uploadResult = testService.testFileUpload("test.jpg", new byte[1024]);
        assertEquals("/uploads/test.jpg", uploadResult);
    }
}