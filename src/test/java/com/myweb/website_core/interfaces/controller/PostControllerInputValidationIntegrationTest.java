package com.myweb.website_core.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.business.CommentService;
import com.myweb.website_core.application.service.business.PostService;
import com.myweb.website_core.application.service.security.authorization.AccessControlService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import com.myweb.website_core.infrastructure.security.aspect.SecurityValidationAspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PostController输入验证集成测试
 * <p>
 * 测试PostController中输入验证功能的集成，包括：
 * 1. 创建帖子时的标题和内容验证
 * 2. 编辑帖子时的输入验证
 * 3. 评论相关方法的内容验证
 * 4. 验证失败时的错误处理
 * <p>
 * 符合需求：1.2, 1.3, 1.7 - 输入验证服务业务集成
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@WebMvcTest(PostController.class)
@Import(SecurityValidationAspect.class)
@DisplayName("PostController输入验证集成测试")
public class PostControllerInputValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private InputValidationService inputValidationService;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 设置测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // 设置测试帖子
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("测试帖子");
        testPost.setContent("这是一个测试帖子的内容");
        testPost.setAuthor(testUser);

        // Mock用户存在
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postService.getPostById(1L)).thenReturn(Optional.of(testPost));
        
        // Mock权限检查通过
        when(accessControlService.canEditPost(any(), any())).thenReturn(true);
        when(accessControlService.canCreateComment(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("创建帖子 - 有效输入应该成功")
    void createPost_ValidInput_ShouldSucceed() throws Exception {
        // 准备测试数据
        PostController.CreatePostRequest request = new PostController.CreatePostRequest();
        request.setTitle("有效的帖子标题");
        request.setContent("这是一个有效的帖子内容，包含足够的信息来测试验证功能。");
        
        PostController.CreatePostRequest.AuthorInfo authorInfo = new PostController.CreatePostRequest.AuthorInfo();
        authorInfo.setId(1L);
        request.setAuthor(authorInfo);

        // Mock服务方法
        when(postService.createPost(any(Post.class))).thenReturn(testPost);
        when(postService.convertToDTO(any(Post.class))).thenReturn(null);

        // 执行请求
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证输入验证服务被调用
        verify(inputValidationService).validatePostTitle("有效的帖子标题");
        verify(inputValidationService).validatePostContent("这是一个有效的帖子内容，包含足够的信息来测试验证功能。");
    }

    @Test
    @DisplayName("创建帖子 - 标题验证失败应该返回错误")
    void createPost_InvalidTitle_ShouldReturnError() throws Exception {
        // 准备测试数据
        PostController.CreatePostRequest request = new PostController.CreatePostRequest();
        request.setTitle(""); // 空标题
        request.setContent("有效的内容");
        
        PostController.CreatePostRequest.AuthorInfo authorInfo = new PostController.CreatePostRequest.AuthorInfo();
        authorInfo.setId(1L);
        request.setAuthor(authorInfo);

        // Mock验证失败
        doThrow(new ValidationException("标题不能为空", "title", "REQUIRED"))
                .when(inputValidationService).validatePostTitle("");

        // 执行请求
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(contains("输入验证失败")));

        // 验证服务方法没有被调用
        verify(postService, never()).createPost(any(Post.class));
    }

    @Test
    @DisplayName("创建帖子 - 内容验证失败应该返回错误")
    void createPost_InvalidContent_ShouldReturnError() throws Exception {
        // 准备测试数据
        PostController.CreatePostRequest request = new PostController.CreatePostRequest();
        request.setTitle("有效标题");
        request.setContent("<script>alert('xss')</script>"); // 包含XSS的内容
        
        PostController.CreatePostRequest.AuthorInfo authorInfo = new PostController.CreatePostRequest.AuthorInfo();
        authorInfo.setId(1L);
        request.setAuthor(authorInfo);

        // Mock验证失败
        doThrow(new ValidationException("内容包含潜在的XSS攻击代码", "content", "XSS_DETECTED"))
                .when(inputValidationService).validatePostContent(anyString());

        // 执行请求
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(contains("XSS攻击代码")));

        // 验证服务方法没有被调用
        verify(postService, never()).createPost(any(Post.class));
    }

    @Test
    @DisplayName("编辑帖子 - 有效输入应该成功")
    void editPost_ValidInput_ShouldSucceed() throws Exception {
        // 准备测试数据
        Post updateRequest = new Post();
        updateRequest.setTitle("更新后的标题");
        updateRequest.setContent("更新后的内容");
        updateRequest.setAuthor(testUser);

        // Mock服务方法
        when(postService.editPost(eq(1L), any(Post.class))).thenReturn(testPost);
        when(postService.convertToDTO(any(Post.class))).thenReturn(null);

        // 执行请求
        mockMvc.perform(put("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // 验证输入验证服务被调用
        verify(inputValidationService).validatePostTitle("更新后的标题");
        verify(inputValidationService).validatePostContent("更新后的内容");
    }

    @Test
    @DisplayName("编辑帖子 - 验证失败应该返回错误")
    void editPost_ValidationFailure_ShouldReturnError() throws Exception {
        // 准备测试数据
        Post updateRequest = new Post();
        updateRequest.setTitle("a".repeat(300)); // 超长标题
        updateRequest.setContent("有效内容");
        updateRequest.setAuthor(testUser);

        // Mock验证失败
        doThrow(new ValidationException("标题长度超过限制", "title", "LENGTH_EXCEEDED"))
                .when(inputValidationService).validatePostTitle(anyString());

        // 执行请求
        mockMvc.perform(put("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(contains("长度超过限制")));

        // 验证服务方法没有被调用
        verify(postService, never()).editPost(anyLong(), any(Post.class));
    }

    @Test
    @DisplayName("创建评论 - 有效输入应该成功")
    void createComment_ValidInput_ShouldSucceed() throws Exception {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent("这是一个有效的评论内容");
        request.setUserId(1L);

        // Mock服务方法
        when(commentService.createComment(eq(1L), eq(1L), anyString())).thenReturn(null);

        // 执行请求
        mockMvc.perform(post("/api/posts/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证输入验证服务被调用
        verify(inputValidationService).validateCommentContent("这是一个有效的评论内容");
    }

    @Test
    @DisplayName("创建评论 - 内容验证失败应该返回错误")
    void createComment_InvalidContent_ShouldReturnError() throws Exception {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent(""); // 空评论
        request.setUserId(1L);

        // Mock验证失败
        doThrow(new ValidationException("评论内容不能为空", "content", "REQUIRED"))
                .when(inputValidationService).validateCommentContent("");

        // 执行请求
        mockMvc.perform(post("/api/posts/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(contains("评论内容不能为空")));

        // 验证服务方法没有被调用
        verify(commentService, never()).createComment(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("创建回复 - 有效输入应该成功")
    void createReply_ValidInput_ShouldSucceed() throws Exception {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent("这是一个有效的回复内容");
        request.setUserId(1L);

        // Mock服务方法
        when(commentService.createReply(eq(1L), eq(1L), eq(1L), anyString())).thenReturn(null);

        // 执行请求
        mockMvc.perform(post("/api/posts/1/comments/1/replies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证输入验证服务被调用
        verify(inputValidationService).validateCommentContent("这是一个有效的回复内容");
    }

    @Test
    @DisplayName("创建回复 - SQL注入检测应该返回错误")
    void createReply_SqlInjectionDetected_ShouldReturnError() throws Exception {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent("'; DROP TABLE posts; --"); // SQL注入尝试
        request.setUserId(1L);

        // Mock验证失败
        doThrow(new ValidationException("评论内容包含潜在的SQL注入代码", "content", "SQL_INJECTION_DETECTED"))
                .when(inputValidationService).validateCommentContent(anyString());

        // 执行请求
        mockMvc.perform(post("/api/posts/1/comments/1/replies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(contains("SQL注入代码")));

        // 验证服务方法没有被调用
        verify(commentService, never()).createReply(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("创建帖子 - 长内容验证应该成功")
    void createPost_LongValidContent_ShouldSucceed() throws Exception {
        // 准备测试数据 - 长但有效的内容
        String longContent = "这是一个很长的帖子内容。".repeat(100); // 重复100次，但仍在限制内
        
        PostController.CreatePostRequest request = new PostController.CreatePostRequest();
        request.setTitle("长内容测试标题");
        request.setContent(longContent);
        
        PostController.CreatePostRequest.AuthorInfo authorInfo = new PostController.CreatePostRequest.AuthorInfo();
        authorInfo.setId(1L);
        request.setAuthor(authorInfo);

        // Mock服务方法
        when(postService.createPost(any(Post.class))).thenReturn(testPost);
        when(postService.convertToDTO(any(Post.class))).thenReturn(null);

        // 执行请求
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证输入验证服务被调用
        verify(inputValidationService).validatePostTitle("长内容测试标题");
        verify(inputValidationService).validatePostContent(longContent);
    }

    @Test
    @DisplayName("创建帖子 - 超长内容验证失败应该返回错误")
    void createPost_TooLongContent_ShouldReturnError() throws Exception {
        // 准备测试数据 - 超长内容
        String tooLongContent = "a".repeat(60000); // 超过最大长度限制
        
        PostController.CreatePostRequest request = new PostController.CreatePostRequest();
        request.setTitle("超长内容测试");
        request.setContent(tooLongContent);
        
        PostController.CreatePostRequest.AuthorInfo authorInfo = new PostController.CreatePostRequest.AuthorInfo();
        authorInfo.setId(1L);
        request.setAuthor(authorInfo);

        // Mock验证失败
        doThrow(new ValidationException("内容长度超过限制（最大50000字符）", "content", "LENGTH_EXCEEDED"))
                .when(inputValidationService).validatePostContent(anyString());

        // 执行请求
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(contains("长度超过限制")));

        // 验证服务方法没有被调用
        verify(postService, never()).createPost(any(Post.class));
    }
}