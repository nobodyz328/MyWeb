package com.myweb.website_core.application.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.domain.business.dto.UserDataExportDTO;
import com.myweb.website_core.domain.business.dto.UserDataUpdateDTO;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户数据管理服务测试类
 * 
 * 测试用户数据的查看、导出、修改和删除功能
 * 验证权限控制、数据脱敏和审计记录功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户数据管理服务测试")
class UserDataManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private MessageProducerService messageProducerService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserDataManagementService userDataManagementService;

    private User testUser;
    private User adminUser;
    private User currentUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
        testUser.setBio("Test user bio");
        testUser.setLikedCount(10);
        testUser.setEmailVerified(true);
        testUser.setRole(UserRole.USER);
        testUser.setTotpEnabled(false);
        testUser.setLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(30));
        testUser.setUpdatedAt(LocalDateTime.now().minusDays(1));
        testUser.setFollowers(new ArrayList<>());
        testUser.setFollowing(new ArrayList<>());

        // 创建管理员用户
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);

        // 创建当前用户
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("testuser");
        currentUser.setRole(UserRole.USER);
    }

    // ==================== 数据查看功能测试 ====================

    @Test
    @DisplayName("用户查看自己的数据 - 成功")
    void testViewUserData_SelfAccess_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);

        // When
        CompletableFuture<UserDataExportDTO> future = userDataManagementService.viewUserData(1L, "127.0.0.1");
        UserDataExportDTO result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail()); // 用户本人可以看到完整邮箱
        assertEquals("Test user bio", result.getBio());
        assertEquals(10, result.getLikedCount());
        assertTrue(result.getEmailVerified());
        assertEquals("USER", result.getRole());

        verify(userRepository).findById(1L);
        verify(authenticationService).getCurrentUser();
        verify(messageProducerService).sendUnifiedSecurityMessage(any());
    }

    @Test
    @DisplayName("管理员查看用户数据 - 成功")
    void testViewUserData_AdminAccess_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authenticationService.getCurrentUser()).thenReturn(adminUser);

        // When
        CompletableFuture<UserDataExportDTO> future = userDataManagementService.viewUserData(1L, "127.0.0.1");
        UserDataExportDTO result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail()); // 管理员可以看到完整邮箱
        assertNotNull(result.getLoginAttempts()); // 管理员可以看到安全信息

        verify(userRepository).findById(1L);
        verify(authenticationService).getCurrentUser();
        verify(messageProducerService).sendUnifiedSecurityMessage(any());
    }

    @Test
    @DisplayName("查看不存在的用户数据 - 失败")
    void testViewUserData_UserNotFound_Failure() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);

        // When & Then
        CompletableFuture<UserDataExportDTO> future = userDataManagementService.viewUserData(999L, "127.0.0.1");
        
        assertThrows(Exception.class, () -> future.get());

        verify(userRepository).findById(999L);
        verify(messageProducerService).sendUnifiedSecurityMessage(any()); // 应该记录失败日志
    }

    // ==================== 数据导出功能测试 ====================

    @Test
    @DisplayName("导出用户数据为JSON - 成功")
    void testExportUserDataAsJson_Success() throws Exception {
        // Given
        String expectedJson = "{\"id\":1,\"username\":\"testuser\"}";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);
        
        // Mock ObjectMapper chain properly
        com.fasterxml.jackson.databind.ObjectWriter mockWriter = mock(com.fasterxml.jackson.databind.ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any())).thenReturn(expectedJson);

        // When
        CompletableFuture<byte[]> future = userDataManagementService.exportUserDataAsJson(1L, "127.0.0.1");
        byte[] result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(expectedJson, new String(result));

        verify(userRepository).findById(1L);
        verify(authenticationService).getCurrentUser();
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(mockWriter).writeValueAsString(any());
        verify(messageProducerService).sendUnifiedSecurityMessage(any());
    }

    @Test
    @DisplayName("导出用户数据为CSV - 成功")
    void testExportUserDataAsCsv_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);

        // When
        CompletableFuture<byte[]> future = userDataManagementService.exportUserDataAsCsv(1L, "127.0.0.1");
        byte[] result = future.get();

        // Then
        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("字段,值"));
        assertTrue(csvContent.contains("用户ID,1"));
        assertTrue(csvContent.contains("用户名,testuser"));

        verify(userRepository).findById(1L);
        verify(authenticationService).getCurrentUser();
        verify(messageProducerService).sendUnifiedSecurityMessage(any());
    }

    // ==================== 数据修改功能测试 ====================

    @Test
    @DisplayName("修改用户基本信息 - 成功")
    void testUpdateUserData_BasicFields_Success() throws Exception {
        // Given
        UserDataUpdateDTO updateDTO = new UserDataUpdateDTO();
        updateDTO.setAvatarUrl("http://example.com/new-avatar.jpg");
        updateDTO.setBio("Updated bio");
        updateDTO.setUpdateReason("用户自主更新");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        CompletableFuture<UserDataExportDTO> future = userDataManagementService.updateUserData(1L, updateDTO, "127.0.0.1");
        UserDataExportDTO result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("http://example.com/new-avatar.jpg", testUser.getAvatarUrl());
        assertEquals("Updated bio", testUser.getBio());

        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(authenticationService).getCurrentUser();
        verify(messageProducerService).sendUnifiedSecurityMessage(any());
    }

    @Test
    @DisplayName("管理员修改用户敏感信息 - 成功")
    void testUpdateUserData_AdminSensitiveFields_Success() throws Exception {
        // Given
        UserDataUpdateDTO updateDTO = new UserDataUpdateDTO();
        updateDTO.setEmail("newemail@example.com");
        updateDTO.setRole("MODERATOR");
        updateDTO.setUpdateReason("管理员调整");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authenticationService.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        CompletableFuture<UserDataExportDTO> future = userDataManagementService.updateUserData(1L, updateDTO, "127.0.0.1");
        UserDataExportDTO result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("newemail@example.com", testUser.getEmail());
        assertEquals(UserRole.MODERATOR, testUser.getRole());
        assertFalse(testUser.getEmailVerified()); // 邮箱变更后需要重新验证

        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(authenticationService).getCurrentUser();
        verify(messageProducerService).sendUnifiedSecurityMessage(any());
    }

    // ==================== 数据删除功能测试 ====================

    @Test
    @DisplayName("删除用户数据 - 成功")
    void testDeleteUserData_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);
        doNothing().when(userRepository).delete(testUser);

        // When
        CompletableFuture<Boolean> future = userDataManagementService.deleteUserData(1L, "127.0.0.1");
        Boolean result = future.get();

        // Then
        assertTrue(result);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
        verify(authenticationService).getCurrentUser();
        verify(messageProducerService).sendUnifiedSecurityMessage(any());
    }

    @Test
    @DisplayName("删除不存在的用户数据 - 失败")
    void testDeleteUserData_UserNotFound_Failure() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);

        // When & Then
        CompletableFuture<Boolean> future = userDataManagementService.deleteUserData(999L, "127.0.0.1");
        
        assertThrows(Exception.class, () -> future.get());

        verify(userRepository).findById(999L);
        verify(userRepository, never()).delete(any());
        verify(messageProducerService).sendUnifiedSecurityMessage(any()); // 应该记录失败日志
    }

    // ==================== 权限检查功能测试 ====================

    @Test
    @DisplayName("检查当前用户权限 - 是本人")
    void testIsCurrentUser_SameUser_True() {
        // Given
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);

        // When
        boolean result = userDataManagementService.isCurrentUser(1L);

        // Then
        assertTrue(result);
        verify(authenticationService).getCurrentUser();
    }

    @Test
    @DisplayName("检查当前用户权限 - 不是本人")
    void testIsCurrentUser_DifferentUser_False() {
        // Given
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);

        // When
        boolean result = userDataManagementService.isCurrentUser(2L);

        // Then
        assertFalse(result);
        verify(authenticationService).getCurrentUser();
    }

    @Test
    @DisplayName("检查当前用户权限 - 未登录")
    void testIsCurrentUser_NotLoggedIn_False() {
        // Given
        when(authenticationService.getCurrentUser()).thenReturn(null);

        // When
        boolean result = userDataManagementService.isCurrentUser(1L);

        // Then
        assertFalse(result);
        verify(authenticationService).getCurrentUser();
    }

    @Test
    @DisplayName("检查当前用户权限 - 异常情况")
    void testIsCurrentUser_Exception_False() {
        // Given
        when(authenticationService.getCurrentUser()).thenThrow(new RuntimeException("认证服务异常"));

        // When
        boolean result = userDataManagementService.isCurrentUser(1L);

        // Then
        assertFalse(result);
        verify(authenticationService).getCurrentUser();
    }

    // ==================== 数据脱敏功能测试 ====================

    @Test
    @DisplayName("邮箱脱敏处理测试")
    void testEmailMasking() throws Exception {
        // Given
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setRole(UserRole.USER);
        otherUser.setFollowers(new ArrayList<>());
        otherUser.setFollowing(new ArrayList<>());

        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));
        when(authenticationService.getCurrentUser()).thenReturn(currentUser); // 不同用户

        // When
        CompletableFuture<UserDataExportDTO> future = userDataManagementService.viewUserData(3L, "127.0.0.1");
        UserDataExportDTO result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("o***r@example.com", result.getEmail()); // 应该被脱敏
        assertNull(result.getEmailVerified()); // 敏感信息应该为null
        assertNull(result.getLoginAttempts()); // 敏感信息应该为null

        verify(userRepository).findById(3L);
        verify(authenticationService).getCurrentUser();
    }
}