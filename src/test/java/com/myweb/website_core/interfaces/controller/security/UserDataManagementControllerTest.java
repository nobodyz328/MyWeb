package com.myweb.website_core.interfaces.controller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.UserDataManagementService;
import com.myweb.website_core.domain.business.dto.UserDataExportDTO;
import com.myweb.website_core.domain.business.dto.UserDataUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户数据管理控制器测试类
 * 
 * 测试用户数据管理相关的REST API接口
 * 验证权限控制、参数验证和响应格式
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@WebMvcTest(UserDataManagementController.class)
@DisplayName("用户数据管理控制器测试")
class UserDataManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDataManagementService userDataManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDataExportDTO testUserData;
    private UserDataUpdateDTO testUpdateData;

    @BeforeEach
    void setUp() {
        // 创建测试用户数据
        testUserData = new UserDataExportDTO();
        testUserData.setId(1L);
        testUserData.setUsername("testuser");
        testUserData.setEmail("test@example.com");
        testUserData.setAvatarUrl("http://example.com/avatar.jpg");
        testUserData.setBio("Test user bio");
        testUserData.setLikedCount(10);
        testUserData.setFollowersCount(5);
        testUserData.setFollowingCount(3);
        testUserData.setEmailVerified(true);
        testUserData.setRole("USER");
        testUserData.setTotpEnabled(false);
        testUserData.setCreatedAt(LocalDateTime.now().minusDays(30));
        testUserData.setUpdatedAt(LocalDateTime.now().minusDays(1));

        // 创建测试更新数据
        testUpdateData = new UserDataUpdateDTO();
        testUpdateData.setAvatarUrl("http://example.com/new-avatar.jpg");
        testUpdateData.setBio("Updated bio");
        testUpdateData.setUpdateReason("用户自主更新");
    }

    // ==================== 数据查看接口测试 ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("查看用户数据 - 成功")
    void testViewUserData_Success() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);
        when(userDataManagementService.viewUserData(eq(1L), anyString()))
            .thenReturn(CompletableFuture.completedFuture(testUserData));

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.bio").value("Test user bio"))
                .andExpect(jsonPath("$.likedCount").value(10));

        verify(userDataManagementService).viewUserData(eq(1L), anyString());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("管理员查看用户数据 - 成功")
    void testViewUserData_AdminAccess_Success() throws Exception {
        // Given
        when(userDataManagementService.viewUserData(eq(1L), anyString()))
            .thenReturn(CompletableFuture.completedFuture(testUserData));

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userDataManagementService).viewUserData(eq(1L), anyString());
    }

    @Test
    @WithMockUser(username = "otheruser", roles = {"USER"})
    @DisplayName("查看其他用户数据 - 权限不足")
    void testViewUserData_AccessDenied() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userDataManagementService, never()).viewUserData(anyLong(), anyString());
    }

    @Test
    @DisplayName("未登录查看用户数据 - 需要认证")
    void testViewUserData_Unauthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/user-data/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userDataManagementService, never()).viewUserData(anyLong(), anyString());
    }

    // ==================== 数据导出接口测试 ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("导出用户数据为JSON - 成功")
    void testExportUserDataAsJson_Success() throws Exception {
        // Given
        String jsonData = "{\"id\":1,\"username\":\"testuser\"}";
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);
        when(userDataManagementService.exportUserDataAsJson(eq(1L), anyString()))
            .thenReturn(CompletableFuture.completedFuture(jsonData.getBytes()));

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1/export/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(jsonData));

        verify(userDataManagementService).exportUserDataAsJson(eq(1L), anyString());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("导出用户数据为CSV - 成功")
    void testExportUserDataAsCsv_Success() throws Exception {
        // Given
        String csvData = "字段,值\n用户ID,1\n用户名,testuser";
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);
        when(userDataManagementService.exportUserDataAsCsv(eq(1L), anyString()))
            .thenReturn(CompletableFuture.completedFuture(csvData.getBytes()));

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"));

        verify(userDataManagementService).exportUserDataAsCsv(eq(1L), anyString());
    }

    @Test
    @WithMockUser(username = "otheruser", roles = {"USER"})
    @DisplayName("导出其他用户数据 - 权限不足")
    void testExportUserData_AccessDenied() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1/export/json"))
                .andExpect(status().isForbidden());

        verify(userDataManagementService, never()).exportUserDataAsJson(anyLong(), anyString());
    }

    // ==================== 数据修改接口测试 ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("修改用户数据 - 成功")
    void testUpdateUserData_Success() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);
        when(userDataManagementService.updateUserData(eq(1L), any(UserDataUpdateDTO.class), anyString()))
            .thenReturn(CompletableFuture.completedFuture(testUserData));

        // When & Then
        mockMvc.perform(put("/api/security/user-data/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUpdateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userDataManagementService).updateUserData(eq(1L), any(UserDataUpdateDTO.class), anyString());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("管理员修改用户敏感数据 - 成功")
    void testUpdateUserData_AdminSensitiveFields_Success() throws Exception {
        // Given
        UserDataUpdateDTO adminUpdateData = new UserDataUpdateDTO();
        adminUpdateData.setEmail("newemail@example.com");
        adminUpdateData.setRole("MODERATOR");
        adminUpdateData.setUpdateReason("管理员调整");

        when(userDataManagementService.updateUserData(eq(1L), any(UserDataUpdateDTO.class), anyString()))
            .thenReturn(CompletableFuture.completedFuture(testUserData));

        // When & Then
        mockMvc.perform(put("/api/security/user-data/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminUpdateData)))
                .andExpect(status().isOk());

        verify(userDataManagementService).updateUserData(eq(1L), any(UserDataUpdateDTO.class), anyString());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("修改用户数据 - 无效参数")
    void testUpdateUserData_InvalidData() throws Exception {
        // Given
        UserDataUpdateDTO invalidData = new UserDataUpdateDTO();
        invalidData.setEmail("invalid-email"); // 无效邮箱格式

        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/security/user-data/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidData)))
                .andExpect(status().isBadRequest());

        verify(userDataManagementService, never()).updateUserData(anyLong(), any(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("修改用户数据 - 空更新")
    void testUpdateUserData_EmptyUpdate() throws Exception {
        // Given
        UserDataUpdateDTO emptyData = new UserDataUpdateDTO();

        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/security/user-data/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyData)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("没有提供任何需要更新的字段"));

        verify(userDataManagementService, never()).updateUserData(anyLong(), any(), anyString());
    }

    // ==================== 数据删除接口测试 ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("删除用户数据 - 成功")
    void testDeleteUserData_Success() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);
        when(userDataManagementService.deleteUserData(eq(1L), anyString()))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When & Then
        mockMvc.perform(delete("/api/security/user-data/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("用户数据删除成功"));

        verify(userDataManagementService).deleteUserData(eq(1L), anyString());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("管理员删除用户数据 - 成功")
    void testDeleteUserData_AdminAccess_Success() throws Exception {
        // Given
        when(userDataManagementService.deleteUserData(eq(1L), anyString()))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When & Then
        mockMvc.perform(delete("/api/security/user-data/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("用户数据删除成功"));

        verify(userDataManagementService).deleteUserData(eq(1L), anyString());
    }

    @Test
    @WithMockUser(username = "otheruser", roles = {"USER"})
    @DisplayName("删除其他用户数据 - 权限不足")
    void testDeleteUserData_AccessDenied() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/security/user-data/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(userDataManagementService, never()).deleteUserData(anyLong(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("删除用户数据 - 服务异常")
    void testDeleteUserData_ServiceException() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);
        when(userDataManagementService.deleteUserData(eq(1L), anyString()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // When & Then
        mockMvc.perform(delete("/api/security/user-data/1")
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("用户数据删除失败"));

        verify(userDataManagementService).deleteUserData(eq(1L), anyString());
    }

    // ==================== 数据管理信息接口测试 ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("获取用户数据管理信息 - 成功")
    void testGetUserDataInfo_Success() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1/info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.dataProtectionLevel").value("GB/T 22239-2019 二级等保"))
                .andExpect(jsonPath("$.supportedExportFormats").isArray())
                .andExpect(jsonPath("$.supportedExportFormats[0]").value("JSON"))
                .andExpect(jsonPath("$.supportedExportFormats[1]").value("CSV"))
                .andExpect(jsonPath("$.privacyRights").isArray())
                .andExpect(jsonPath("$.privacyRights[0]").value("数据查看权"));

        verify(userDataManagementService).isCurrentUser(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("管理员获取用户数据管理信息 - 成功")
    void testGetUserDataInfo_AdminAccess_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/user-data/1/info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.dataProtectionLevel").exists());
    }

    @Test
    @WithMockUser(username = "otheruser", roles = {"USER"})
    @DisplayName("获取其他用户数据管理信息 - 权限不足")
    void testGetUserDataInfo_AccessDenied() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1/info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ==================== 异常处理测试 ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("服务异常处理测试")
    void testServiceException() throws Exception {
        // Given
        when(userDataManagementService.isCurrentUser(1L)).thenReturn(true);
        when(userDataManagementService.viewUserData(eq(1L), anyString()))
            .thenThrow(new RuntimeException("服务异常"));

        // When & Then
        mockMvc.perform(get("/api/security/user-data/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("查看用户数据失败: 服务异常"));

        verify(userDataManagementService).viewUserData(eq(1L), anyString());
    }
}