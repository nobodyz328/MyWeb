package com.myweb.website_core.application.service;

import com.myweb.website_core.common.exception.security.AuthorizationException;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.authorization.AuthorizationService;
import com.myweb.website_core.application.service.security.authorization.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 资源访问控制服务测试
 * 
 * 测试AuthorizationService的各项功能：
 * - 权限验证
 * - 资源所有权检查
 * - 基于角色的访问控制
 * - 权限缓存机制
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    
    @Mock
    private RoleService roleService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private CommentRepository commentRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private AuthorizationService authorizationService;
    
    private User testUser;
    private User adminUser;
    private Post testPost;
    private Comment testComment;
    private Role userRole;
    private Role adminRole;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        
        // 创建测试角色
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
        userRole.setEnabled(true);
        
        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");
        adminRole.setEnabled(true);
        
        // 为用户分配角色
        testUser.addRole(userRole);
        adminUser.addRole(adminRole);
        
        // 创建测试帖子
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setAuthor(testUser);
        
        // 创建测试评论
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setContent("Test Comment");
        testComment.setPost(testPost);
        testComment.setAuthor(testUser);
    }
    
    // ==================== 权限验证测试 ====================
    
    @Test
    void testHasPermission_WithValidUserAndPermission_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // 模拟用户具有权限
        testUser = spy(testUser);
        when(testUser.hasPermission("POST_CREATE")).thenReturn(true);
        
        // When
        boolean result = authorizationService.hasPermission(1L, "POST_CREATE");
        
        // Then
        assertTrue(result);
        verify(userRepository).findById(1L);
    }
    
    @Test
    void testHasPermission_WithInvalidUser_ShouldReturnFalse() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When
        boolean result = authorizationService.hasPermission(999L, "POST_CREATE");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testHasPermission_WithNullParameters_ShouldReturnFalse() {
        // When & Then
        assertFalse(authorizationService.hasPermission(null, "POST_CREATE"));
        assertFalse(authorizationService.hasPermission(1L, null));
        assertFalse(authorizationService.hasPermission(null, null));
    }
    
    @Test
    void testHasResourcePermission_WithValidParameters_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // 模拟用户具有资源权限
        testUser = spy(testUser);
        when(testUser.hasPermission("POST", "CREATE")).thenReturn(true);
        
        // When
        boolean result = authorizationService.hasPermission(1L, "POST", "CREATE");
        
        // Then
        assertTrue(result);
    }
    
    // ==================== 资源访问控制测试 ====================
    
    @Test
    void testCanAccessResource_AsOwner_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // 模拟用户具有基本权限
        testUser = spy(testUser);
        when(testUser.hasPermission("POST_EDIT")).thenReturn(true);
        
        // When
        boolean result = authorizationService.canAccessResource(1L, "POST", 1L, "EDIT");
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testCanAccessResource_AsNonOwnerWithoutManagePermission_ShouldReturnFalse() {
        // Given
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otheruser");
        
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // 模拟用户具有基本权限但不是所有者且没有管理权限
        otherUser = spy(otherUser);
        when(otherUser.hasPermission("POST_EDIT")).thenReturn(true);
        when(otherUser.hasPermission("POST_MANAGE")).thenReturn(false);
        
        // When
        boolean result = authorizationService.canAccessResource(3L, "POST", 1L, "EDIT");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testCanAccessResource_AsNonOwnerWithManagePermission_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // 模拟管理员具有管理权限
        adminUser = spy(adminUser);
        when(adminUser.hasPermission("POST_EDIT")).thenReturn(true);
        when(adminUser.hasPermission("POST_MANAGE")).thenReturn(true);
        
        // When
        boolean result = authorizationService.canAccessResource(2L, "POST", 1L, "EDIT");
        
        // Then
        assertTrue(result);
    }
    
    // ==================== 资源所有权检查测试 ====================
    
    @Test
    void testIsResourceOwner_PostOwner_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // When
        boolean result = authorizationService.isResourceOwner(1L, "POST", 1L);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testIsResourceOwner_NotPostOwner_ShouldReturnFalse() {
        // Given
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otheruser");
        
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        
        // When
        boolean result = authorizationService.isResourceOwner(3L, "POST", 1L);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsResourceOwner_CommentOwner_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        
        // When
        boolean result = authorizationService.isResourceOwner(1L, "COMMENT", 1L);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testIsResourceOwner_UserSelf_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        boolean result = authorizationService.isResourceOwner(1L, "USER", 1L);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testIsResourceOwner_NonexistentResource_ShouldReturnFalse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When
        boolean result = authorizationService.isResourceOwner(1L, "POST", 999L);
        
        // Then
        assertFalse(result);
    }
    
    // ==================== 评论编辑权限测试 ====================
    
    @Test
    void testCanEditComment_AsCommentAuthor_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        
        // When
        boolean result = authorizationService.canEditComment(1L, 1L);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testCanEditComment_AsPostAuthor_ShouldReturnTrue() {
        // Given
        // 创建另一个用户作为评论作者
        User commentAuthor = new User();
        commentAuthor.setId(3L);
        commentAuthor.setUsername("commentauthor");
        testComment.setAuthor(commentAuthor);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        
        // When
        boolean result = authorizationService.canEditComment(1L, 1L);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testCanEditComment_WithManagePermission_ShouldReturnTrue() {
        // Given
        // 创建另一个用户作为评论作者
        User commentAuthor = new User();
        commentAuthor.setId(3L);
        commentAuthor.setUsername("commentauthor");
        testComment.setAuthor(commentAuthor);
        
        // 创建另一个帖子作者
        User postAuthor = new User();
        postAuthor.setId(4L);
        postAuthor.setUsername("postauthor");
        testPost.setAuthor(postAuthor);
        testComment.setPost(testPost);
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        
        // 模拟管理员具有管理权限
        adminUser = spy(adminUser);
        when(adminUser.hasPermission("COMMENT_MANAGE")).thenReturn(true);
        
        // When
        boolean result = authorizationService.canEditComment(2L, 1L);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testCanEditComment_WithoutPermission_ShouldReturnFalse() {
        // Given
        // 创建另一个用户作为评论作者
        User commentAuthor = new User();
        commentAuthor.setId(3L);
        commentAuthor.setUsername("commentauthor");
        testComment.setAuthor(commentAuthor);
        
        // 创建另一个帖子作者
        User postAuthor = new User();
        postAuthor.setId(4L);
        postAuthor.setUsername("postauthor");
        testPost.setAuthor(postAuthor);
        testComment.setPost(testPost);
        
        User otherUser = new User();
        otherUser.setId(5L);
        otherUser.setUsername("otheruser");
        
        when(userRepository.findById(5L)).thenReturn(Optional.of(otherUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        
        // 模拟用户没有管理权限
        otherUser = spy(otherUser);
        when(otherUser.hasPermission("COMMENT_MANAGE")).thenReturn(false);
        
        // When
        boolean result = authorizationService.canEditComment(5L, 1L);
        
        // Then
        assertFalse(result);
    }
    
    // ==================== 角色检查测试 ====================
    
    @Test
    void testHasRole_WithValidRole_ShouldReturnTrue() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        // 模拟用户具有角色
        testUser = spy(testUser);
        when(testUser.hasRoleName("USER")).thenReturn(true);
        
        // When
        boolean result = authorizationService.hasRole("testuser", "USER");
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testHasManagementPermission_WithAdminRole_ShouldReturnTrue() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(adminUser);
        
        // 模拟管理员具有ADMIN角色
        adminUser = spy(adminUser);
        when(adminUser.hasRoleName("ADMIN")).thenReturn(true);
        
        // When
        boolean result = authorizationService.hasManagementPermission("admin");
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testHasSystemAdminPermission_WithAdminRole_ShouldReturnTrue() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(adminUser);
        
        // 模拟管理员具有ADMIN角色
        adminUser = spy(adminUser);
        when(adminUser.hasRoleName("ADMIN")).thenReturn(true);
        
        // When
        boolean result = authorizationService.hasSystemAdminPermission("admin");
        
        // Then
        assertTrue(result);
    }
    
    // ==================== 用户管理权限测试 ====================
    
    @Test
    void testCanManageUser_ManageSelf_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        boolean result = authorizationService.canManageUser(1L, 1L);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testCanManageUser_WithPermissionAndRoleHierarchy_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(roleService.hasPermissionToManageUser(2L, 1L)).thenReturn(true);
        
        // 模拟管理员具有用户管理权限
        adminUser = spy(adminUser);
        when(adminUser.hasPermission("USER_MANAGE")).thenReturn(true);
        
        // When
        boolean result = authorizationService.canManageUser(2L, 1L);
        
        // Then
        assertTrue(result);
    }
    
    // ==================== 权限验证异常测试 ====================
    
    @Test
    void testRequirePermission_WithoutPermission_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // 模拟用户没有权限
        testUser = spy(testUser);
        when(testUser.hasPermission("ADMIN_MANAGE")).thenReturn(false);
        
        // When & Then
        assertThrows(AuthorizationException.class, () -> authorizationService.requirePermission(1L, "ADMIN_MANAGE"));
        
        // 验证审计日志被记录
        verify(auditLogService).logSecurityEvent(any(), anyString(), anyString());
    }
    
    @Test
    void testRequireResourceAccess_WithoutAccess_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // 模拟用户没有基本权限
        testUser = spy(testUser);
        when(testUser.hasPermission("POST_DELETE")).thenReturn(false);
        
        // When & Then
        assertThrows(AuthorizationException.class, () -> authorizationService.requireResourceAccess(1L, "POST", 1L, "DELETE"));
        
        // 验证审计日志被记录
        verify(auditLogService).logSecurityEvent(any(), anyString(), anyString());
    }
    
    @Test
    void testRequireRole_WithoutRole_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        // 模拟用户没有角色
        testUser = spy(testUser);
        when(testUser.hasRoleName("ADMIN")).thenReturn(false);
        
        // When & Then
        assertThrows(AuthorizationException.class, () -> authorizationService.requireRole("testuser", "ADMIN"));
        
        // 验证审计日志被记录
        verify(auditLogService).logSecurityEvent(any(), anyString(), anyString());
    }
    
    // ==================== 权限信息获取测试 ====================
    
    @Test
    void testGetUserPermissions_ShouldReturnPermissions() {
        // Given
        Set<String> expectedPermissions = Set.of("POST_CREATE", "POST_READ", "COMMENT_CREATE");
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        testUser = spy(testUser);
        when(testUser.getAllPermissions()).thenReturn(expectedPermissions);
        
        // When
        Set<String> result = authorizationService.getUserPermissions("testuser");
        
        // Then
        assertEquals(expectedPermissions, result);
    }
    
    @Test
    void testGetUserRoles_ShouldReturnRoles() {
        // Given
        Set<String> expectedRoles = Set.of("USER");
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        
        testUser = spy(testUser);
        when(testUser.getAllRoleNames()).thenReturn(expectedRoles);
        
        // When
        Set<String> result = authorizationService.getUserRoles("testuser");
        
        // Then
        assertEquals(expectedRoles, result);
    }
    
    @Test
    void testHasAnyManagementPermission_WithManagementPermission_ShouldReturnTrue() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(adminUser);
        
        adminUser = spy(adminUser);
        when(adminUser.hasAnyManagementPermission()).thenReturn(true);
        
        // When
        boolean result = authorizationService.hasAnyManagementPermission("admin");
        
        // Then
        assertTrue(result);
    }
    
    // ==================== 缓存管理测试 ====================
    
    @Test
    void testRefreshUserPermissionCache_ShouldClearCache() {
        // Given
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("key1", "key2"));
        
        // When
        authorizationService.refreshUserPermissionCache("testuser");
        
        // Then
        verify(redisTemplate, times(2)).keys(anyString());
        verify(redisTemplate, times(2)).delete(any(Set.class));
    }
    
    @Test
    void testRefreshResourceOwnershipCache_ShouldClearCache() {
        // Given
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("key1"));
        
        // When
        authorizationService.refreshResourceOwnershipCache("POST", 1L);
        
        // Then
        verify(redisTemplate).keys(anyString());
        verify(redisTemplate).delete(any(Set.class));
    }
    
    @Test
    void testClearAllPermissionCache_ShouldClearAllCaches() {
        // Given
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("key1", "key2"));
        
        // When
        authorizationService.clearAllPermissionCache();
        
        // Then
        verify(redisTemplate, times(3)).keys(anyString());
        verify(redisTemplate, times(3)).delete(any(Set.class));
    }
}