package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.CommentService;
import com.myweb.website_core.application.service.business.PostService;
import com.myweb.website_core.application.service.security.authorization.AccessControlService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.domain.business.dto.PostDTO;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PostController输入验证单元测试
 * <p>
 * 测试PostController中输入验证功能的单元测试，专注于验证逻辑而不依赖完整的Spring上下文。
 * <p>
 * 符合需求：1.2, 1.3, 1.7 - 输入验证服务业务集成
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostController输入验证单元测试")
public class PostControllerInputValidationUnitTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private PostController postController;

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
    @DisplayName("创建帖子 - 正常流程应该成功")
    void createPost_NormalFlow_ShouldSucceed() {
        // 准备测试数据
        PostController.CreatePostRequest request = new PostController.CreatePostRequest();
        request.setTitle("有效的帖子标题");
        request.setContent("这是一个有效的帖子内容，包含足够的信息来测试验证功能。");
        
        PostController.CreatePostRequest.AuthorInfo authorInfo = new PostController.CreatePostRequest.AuthorInfo();
        authorInfo.setId(1L);
        request.setAuthor(authorInfo);

        // Mock服务方法
        when(postService.createPost(any(Post.class))).thenReturn(testPost);
        when(postService.convertToDTO(any(Post.class))).thenReturn(new PostDTO());

        // 执行请求
        ResponseEntity<Object> response = postController.createPost(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证服务方法被调用
        verify(postService).createPost(any(Post.class));
        verify(postService).convertToDTO(any(Post.class));
    }

    @Test
    @DisplayName("创建帖子 - 服务异常应该返回错误响应")
    void createPost_ServiceException_ShouldReturnErrorResponse() {
        // 准备测试数据
        PostController.CreatePostRequest request = new PostController.CreatePostRequest();
        request.setTitle("测试标题");
        request.setContent("测试内容");
        
        PostController.CreatePostRequest.AuthorInfo authorInfo = new PostController.CreatePostRequest.AuthorInfo();
        authorInfo.setId(1L);
        request.setAuthor(authorInfo);

        // Mock服务抛出异常
        when(postService.createPost(any(Post.class))).thenThrow(new RuntimeException("服务异常"));

        // 执行请求
        ResponseEntity<Object> response = postController.createPost(request);

        // 验证结果
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();
        assertFalse(apiResponse.isSuccess());
        assertEquals("创建帖子失败", apiResponse.getMessage());
    }

    @Test
    @DisplayName("编辑帖子 - 正常流程应该成功")
    void editPost_NormalFlow_ShouldSucceed() {
        // 准备测试数据
        Post updateRequest = new Post();
        updateRequest.setTitle("更新后的标题");
        updateRequest.setContent("更新后的内容");
        updateRequest.setAuthor(testUser);

        // Mock服务方法
        when(postService.editPost(eq(1L), any(Post.class))).thenReturn(testPost);
        when(postService.convertToDTO(any(Post.class))).thenReturn(new PostDTO());

        // 执行请求
        ResponseEntity<Object> response = postController.editPost(1L, updateRequest);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证服务方法被调用
        verify(postService).editPost(eq(1L), any(Post.class));
        verify(postService).convertToDTO(any(Post.class));
    }

    @Test
    @DisplayName("编辑帖子 - 帖子不存在应该返回404")
    void editPost_PostNotFound_ShouldReturn404() {
        // 准备测试数据
        Post updateRequest = new Post();
        updateRequest.setTitle("更新后的标题");
        updateRequest.setContent("更新后的内容");
        updateRequest.setAuthor(testUser);

        // Mock帖子不存在
        when(postService.getPostById(1L)).thenReturn(Optional.empty());

        // 执行请求
        ResponseEntity<Object> response = postController.editPost(1L, updateRequest);

        // 验证结果
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();
        assertFalse(apiResponse.isSuccess());
        assertEquals("帖子不存在", apiResponse.getMessage());
    }

    @Test
    @DisplayName("编辑帖子 - 无权限应该返回403")
    void editPost_NoPermission_ShouldReturn403() {
        // 准备测试数据
        Post updateRequest = new Post();
        updateRequest.setTitle("更新后的标题");
        updateRequest.setContent("更新后的内容");
        updateRequest.setAuthor(testUser);

        // Mock权限检查失败
        when(accessControlService.canEditPost(any(), any())).thenReturn(false);

        // 执行请求
        ResponseEntity<Object> response = postController.editPost(1L, updateRequest);

        // 验证结果
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ApiResponse);
        
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();
        assertFalse(apiResponse.isSuccess());
        assertEquals("无权限编辑此帖子", apiResponse.getMessage());
    }

    @Test
    @DisplayName("创建评论 - 正常流程应该成功")
    void createComment_NormalFlow_ShouldSucceed() {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent("这是一个有效的评论内容");
        request.setUserId(1L);

        // Mock服务方法
        when(commentService.createComment(eq(1L), eq(1L), anyString())).thenReturn(null);

        // 执行请求
        var response = postController.createComment(1L, request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证服务方法被调用
        verify(commentService).createComment(eq(1L), eq(1L), eq("这是一个有效的评论内容"));
    }

    @Test
    @DisplayName("创建评论 - 无权限应该返回403")
    void createComment_NoPermission_ShouldReturn403() {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent("测试评论");
        request.setUserId(1L);

        // Mock权限检查失败
        when(accessControlService.canCreateComment(any(), any())).thenReturn(false);

        // 执行请求
        var response = postController.createComment(1L, request);

        // 验证结果
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("无权限创建评论", response.getBody().getMessage());
    }

    @Test
    @DisplayName("创建回复 - 正常流程应该成功")
    void createReply_NormalFlow_ShouldSucceed() {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent("这是一个有效的回复内容");
        request.setUserId(1L);

        // Mock服务方法
        when(commentService.createReply(eq(1L), eq(1L), eq(1L), anyString())).thenReturn(null);

        // 执行请求
        var response = postController.createReply(1L, 1L, request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证服务方法被调用
        verify(commentService).createReply(eq(1L), eq(1L), eq(1L), eq("这是一个有效的回复内容"));
    }

    @Test
    @DisplayName("创建回复 - 服务异常应该返回错误")
    void createReply_ServiceException_ShouldReturnError() {
        // 准备测试数据
        PostController.CommentRequest request = new PostController.CommentRequest();
        request.setContent("测试回复");
        request.setUserId(1L);

        // Mock服务抛出异常
        when(commentService.createReply(anyLong(), anyLong(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("服务异常"));

        // 执行请求
        var response = postController.createReply(1L, 1L, request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("创建回复失败", response.getBody().getMessage());
    }

    @Test
    @DisplayName("验证注解配置 - createPost方法应该有ValidateInput注解")
    void validateAnnotationConfiguration_CreatePost_ShouldHaveValidateInputAnnotation() throws NoSuchMethodException {
        // 获取方法
        var method = PostController.class.getMethod("createPost", PostController.CreatePostRequest.class);
        
        // 验证注解存在
        assertTrue(method.isAnnotationPresent(com.myweb.website_core.common.validation.ValidateInput.class));
        
        // 验证注解配置
        var annotation = method.getAnnotation(com.myweb.website_core.common.validation.ValidateInput.class);
        assertArrayEquals(new String[]{"title", "content"}, annotation.fieldNames());
        assertArrayEquals(new String[]{"title", "content"}, annotation.validationTypes());
        assertEquals(50000, annotation.maxLength());
    }

    @Test
    @DisplayName("验证注解配置 - editPost方法应该有ValidateInput注解")
    void validateAnnotationConfiguration_EditPost_ShouldHaveValidateInputAnnotation() throws NoSuchMethodException {
        // 获取方法
        var method = PostController.class.getMethod("editPost", Long.class, Post.class);
        
        // 验证注解存在
        assertTrue(method.isAnnotationPresent(com.myweb.website_core.common.validation.ValidateInput.class));
        
        // 验证注解配置
        var annotation = method.getAnnotation(com.myweb.website_core.common.validation.ValidateInput.class);
        assertArrayEquals(new String[]{"title", "content"}, annotation.fieldNames());
        assertArrayEquals(new String[]{"title", "content"}, annotation.validationTypes());
        assertEquals(50000, annotation.maxLength());
    }

    @Test
    @DisplayName("验证注解配置 - createComment方法应该有ValidateInput注解")
    void validateAnnotationConfiguration_CreateComment_ShouldHaveValidateInputAnnotation() throws NoSuchMethodException {
        // 获取方法
        var method = PostController.class.getMethod("createComment", Long.class, PostController.CommentRequest.class);
        
        // 验证注解存在
        assertTrue(method.isAnnotationPresent(com.myweb.website_core.common.validation.ValidateInput.class));
        
        // 验证注解配置
        var annotation = method.getAnnotation(com.myweb.website_core.common.validation.ValidateInput.class);
        assertArrayEquals(new String[]{"content"}, annotation.fieldNames());
        assertArrayEquals(new String[]{"comment"}, annotation.validationTypes());
        assertEquals(2000, annotation.maxLength());
    }

    @Test
    @DisplayName("验证注解配置 - createReply方法应该有ValidateInput注解")
    void validateAnnotationConfiguration_CreateReply_ShouldHaveValidateInputAnnotation() throws NoSuchMethodException {
        // 获取方法
        var method = PostController.class.getMethod("createReply", Long.class, Long.class, PostController.CommentRequest.class);
        
        // 验证注解存在
        assertTrue(method.isAnnotationPresent(com.myweb.website_core.common.validation.ValidateInput.class));
        
        // 验证注解配置
        var annotation = method.getAnnotation(com.myweb.website_core.common.validation.ValidateInput.class);
        assertArrayEquals(new String[]{"content"}, annotation.fieldNames());
        assertArrayEquals(new String[]{"comment"}, annotation.validationTypes());
        assertEquals(2000, annotation.maxLength());
    }
}