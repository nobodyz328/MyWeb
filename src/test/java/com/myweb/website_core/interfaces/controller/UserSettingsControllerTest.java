package com.myweb.website_core.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.business.UserSettingsService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.application.service.security.authentication.TOTPManagementService;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.domain.business.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户设置控制器测试
 */
@WebMvcTest(UserSettingsController.class)
class UserSettingsControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UserSettingsService userSettingsService;
    
    @MockBean
    private TOTPManagementService totpManagementService;
    
    @MockBean
    private AuthenticationService authenticationService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        testUser.setTotpEnabled(false);
        
        when(authenticationService.getCurrentUser()).thenReturn(testUser);
    }
    
    @Test
    void testGetUserSettings() throws Exception {
        UserSettingsService.UserSettingsInfo settingsInfo = UserSettingsService.UserSettingsInfo.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .emailVerified(true)
                .role(UserRole.USER)
                .totpEnabled(false)
                .totpRequired(false)
                .build();
        
        when(userSettingsService.getUserSettings(1L)).thenReturn(settingsInfo);
        
        mockMvc.perform(get("/users/1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.totpEnabled").value(false));
    }
    
    @Test
    void testUpdateBasicInfo() throws Exception {
        UpdateBasicInfoRequest request = new UpdateBasicInfoRequest();
        request.setBio("Updated bio");
        request.setAvatarUrl("http://example.com/avatar.jpg");
        
        mockMvc.perform(put("/users/1/settings/basic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("基本信息更新成功"));
    }
    
    @Test
    void testGetTOTPStatus() throws Exception {
        TOTPManagementService.TOTPStatusInfo statusInfo = TOTPManagementService.TOTPStatusInfo.builder()
                .enabled(false)
                .configured(false)
                .required(false)
                .remainingTime(25)
                .build();
        
        when(totpManagementService.getTOTPStatus(1L)).thenReturn(statusInfo);
        
        mockMvc.perform(get("/users/1/settings/totp/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.required").value(false));
    }
    
    @Test
    void testUnauthorizedAccess() throws Exception {
        // 测试访问其他用户的设置
        mockMvc.perform(get("/users/2/settings"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("无权限访问此用户设置"));
    }
    
    @Test
    void testAdminAccessCheck() throws Exception {
        // 设置为管理员用户
        testUser.setRole(UserRole.ADMIN);
        testUser.setTotpEnabled(true);
        
        UserSettingsService.AdminAccessInfo accessInfo = UserSettingsService.AdminAccessInfo.builder()
                .canAccess(true)
                .requiresTOTP(true)
                .totpEnabled(true)
                .message("验证成功")
                .build();
        
        when(userSettingsService.checkAdminAccess(eq(1L), any())).thenReturn(accessInfo);
        
        CheckAdminAccessRequest request = new CheckAdminAccessRequest();
        request.setTotpCode("123456");
        
        mockMvc.perform(post("/users/1/settings/admin/check-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canAccess").value(true))
                .andExpect(jsonPath("$.requiresTOTP").value(true));
    }
}