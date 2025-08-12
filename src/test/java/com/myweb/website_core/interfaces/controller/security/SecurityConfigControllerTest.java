package com.myweb.website_core.interfaces.controller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.SecurityConfigService;
import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import com.myweb.website_core.domain.security.dto.SecurityConfigBackupDTO;
import com.myweb.website_core.domain.security.dto.SecurityConfigDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 安全配置控制器测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@WebMvcTest(SecurityConfigController.class)
class SecurityConfigControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private SecurityConfigService securityConfigService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testGetSecurityConfig() throws Exception {
        // Given
        SecurityConfigDTO configDTO = new SecurityConfigDTO();
        configDTO.setSecurityProperties(new SecurityProperties());
        configDTO.setLastModified(LocalDateTime.now());
        
        when(securityConfigService.getSecurityConfig()).thenReturn(configDTO);
        
        // When & Then
        mockMvc.perform(get("/api/security/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityProperties").exists())
                .andExpect(jsonPath("$.lastModified").exists());
    }
    
    @Test
    @WithMockUser(authorities = "USER")
    void testGetSecurityConfigWithoutPermission() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/config"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testGetSpecificConfig() throws Exception {
        // Given
        SecurityProperties securityProperties = new SecurityProperties();
        when(securityConfigService.getConfig("security")).thenReturn(securityProperties);
        
        // When & Then
        mockMvc.perform(get("/api/security/config/security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordPolicy").exists());
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testGetNonExistentConfig() throws Exception {
        // Given
        when(securityConfigService.getConfig("nonexistent")).thenReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/security/config/nonexistent"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testUpdateConfigSuccess() throws Exception {
        // Given
        SecurityProperties newConfig = new SecurityProperties();
        newConfig.getPasswordPolicy().setMinLength(10);
        
        when(securityConfigService.updateConfig(eq("security"), any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
        
        // When & Then
        mockMvc.perform(put("/api/security/config/security")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("配置更新成功"))
                .andExpect(jsonPath("$.configType").value("security"));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testUpdateConfigFailure() throws Exception {
        // Given
        SecurityProperties newConfig = new SecurityProperties();
        
        when(securityConfigService.updateConfig(eq("security"), any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(false));
        
        // When & Then
        mockMvc.perform(put("/api/security/config/security")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConfig)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("配置更新失败"));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testBatchUpdateConfig() throws Exception {
        // Given
        Map<String, Object> configUpdates = new HashMap<>();
        configUpdates.put("security", new SecurityProperties());
        configUpdates.put("jwt", new HashMap<>());
        
        Map<String, Boolean> results = new HashMap<>();
        results.put("security", true);
        results.put("jwt", true);
        
        when(securityConfigService.batchUpdateConfig(any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(results));
        
        // When & Then
        mockMvc.perform(put("/api/security/config/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configUpdates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.successCount").value(2));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testResetConfig() throws Exception {
        // Given
        when(securityConfigService.resetConfig(eq("security"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
        
        // When & Then
        mockMvc.perform(post("/api/security/config/security/reset")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("配置重置成功"));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testGetConfigBackups() throws Exception {
        // Given
        SecurityConfigBackupDTO backup1 = createMockBackup("backup1");
        SecurityConfigBackupDTO backup2 = createMockBackup("backup2");
        List<SecurityConfigBackupDTO> backups = List.of(backup1, backup2);
        
        when(securityConfigService.getConfigBackups("security")).thenReturn(backups);
        
        // When & Then
        mockMvc.perform(get("/api/security/config/security/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].backupId").value("backup1"))
                .andExpect(jsonPath("$[1].backupId").value("backup2"));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testRestoreConfig() throws Exception {
        // Given
        when(securityConfigService.restoreConfig(eq("security"), eq("20250106_120000"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
        
        // When & Then
        mockMvc.perform(post("/api/security/config/security/restore/20250106_120000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("配置恢复成功"))
                .andExpect(jsonPath("$.backupTimestamp").value("20250106_120000"));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testValidateConfig() throws Exception {
        // Given
        SecurityProperties config = new SecurityProperties();
        
        // When & Then
        mockMvc.perform(post("/api/security/config/security/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("配置验证通过"));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testGetConfigChangeHistory() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/config/changes")
                        .param("configType", "security")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes").isArray())
                .andExpect(jsonPath("$.configType").value("security"))
                .andExpect(jsonPath("$.limit").value(10));
    }
    
    @Test
    @WithMockUser(authorities = "SYSTEM_MANAGE")
    void testGetAllConfigChangeHistory() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/config/changes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes").isArray())
                .andExpect(jsonPath("$.configType").value("all"))
                .andExpect(jsonPath("$.limit").value(50));
    }
    
    private SecurityConfigBackupDTO createMockBackup(String backupId) {
        SecurityConfigBackupDTO backup = new SecurityConfigBackupDTO();
        backup.setBackupId(backupId);
        backup.setConfigType("security");
        backup.setConfigData("{}");
        backup.setOperator("admin");
        backup.setBackupTime(LocalDateTime.now());
        backup.setBackupType("MANUAL");
        backup.setDescription("Test backup");
        return backup;
    }
}