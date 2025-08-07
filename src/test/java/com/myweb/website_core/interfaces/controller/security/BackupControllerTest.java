package com.myweb.website_core.interfaces.controller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.dataprotect.BackupService;
import com.myweb.website_core.application.service.security.dataprotect.BackupService.BackupResult;
import com.myweb.website_core.application.service.security.dataprotect.BackupService.BackupType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BackupController 单元测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@WebMvcTest(BackupController.class)
class BackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BackupService backupService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerBackup_Success() throws Exception {
        // Given
        BackupResult mockResult = new BackupResult(
            true, "test_backup_id", BackupType.FULL,
            LocalDateTime.now(), LocalDateTime.now(),
            1024L, "/path/to/backup", "checksum", null
        );
        when(backupService.performBackupAsync(any(BackupType.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResult));

        // When & Then
        mockMvc.perform(post("/api/security/backup/trigger")
                .with(csrf())
                .param("backupType", "FULL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("备份任务已启动"))
                .andExpect(jsonPath("$.backupType").value("完全备份"))
                .andExpect(jsonPath("$.status").value("RUNNING"));

        verify(backupService).performBackupAsync(BackupType.FULL);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerBackup_InvalidType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/security/backup/trigger")
                .with(csrf())
                .param("backupType", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("无效的备份类型: INVALID"))
                .andExpect(jsonPath("$.availableTypes").isArray());

        verify(backupService, never()).performBackupAsync(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testTriggerBackup_AccessDenied() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/security/backup/trigger")
                .with(csrf())
                .param("backupType", "FULL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(backupService, never()).performBackupAsync(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetBackupList_Success() throws Exception {
        // Given
        List<Map<String, Object>> mockBackupList = Arrays.asList(
            createMockBackupInfo("backup1.enc", 1024L, true),
            createMockBackupInfo("backup2.enc", 2048L, false)
        );
        when(backupService.getBackupList()).thenReturn(mockBackupList);

        // When & Then
        mockMvc.perform(get("/api/security/backup/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.total").value(2));

        verify(backupService).getBackupList();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetBackupList_ServiceException() throws Exception {
        // Given
        when(backupService.getBackupList()).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/security/backup/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取备份列表失败: Service error"));

        verify(backupService).getBackupList();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testVerifyBackupIntegrity_Success() throws Exception {
        // Given
        String filePath = "/path/to/backup.enc";
        when(backupService.verifyBackupIntegrity(filePath)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/security/backup/verify")
                .with(csrf())
                .param("filePath", filePath)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filePath").value(filePath))
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.message").value("备份文件完整性验证通过"));

        verify(backupService).verifyBackupIntegrity(filePath);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testVerifyBackupIntegrity_Failed() throws Exception {
        // Given
        String filePath = "/path/to/backup.enc";
        when(backupService.verifyBackupIntegrity(filePath)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/security/backup/verify")
                .with(csrf())
                .param("filePath", filePath)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filePath").value(filePath))
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.message").value("备份文件完整性验证失败"));

        verify(backupService).verifyBackupIntegrity(filePath);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCleanupExpiredBackups_Success() throws Exception {
        // Given
        doNothing().when(backupService).cleanupExpiredBackups();

        // When & Then
        mockMvc.perform(post("/api/security/backup/cleanup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("过期备份清理完成"));

        verify(backupService).cleanupExpiredBackups();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCleanupExpiredBackups_ServiceException() throws Exception {
        // Given
        doThrow(new RuntimeException("Cleanup failed")).when(backupService).cleanupExpiredBackups();

        // When & Then
        mockMvc.perform(post("/api/security/backup/cleanup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("清理过期备份失败: Cleanup failed"));

        verify(backupService).cleanupExpiredBackups();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCheckStorageSpace_Success() throws Exception {
        // Given
        doNothing().when(backupService).checkStorageSpaceAndAlert();

        // When & Then
        mockMvc.perform(get("/api/security/backup/storage")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("存储空间检查完成"));

        verify(backupService).checkStorageSpaceAndAlert();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetBackupConfig_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/backup/config")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.backupTypes").isArray())
                .andExpect(jsonPath("$.data.scheduledBackupTime").value("每日凌晨2点"))
                .andExpect(jsonPath("$.data.encryptionEnabled").value(true))
                .andExpect(jsonPath("$.data.compressionEnabled").value(true))
                .andExpect(jsonPath("$.data.notificationEnabled").value(true));
    }

    @Test
    void testTriggerBackup_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/security/backup/trigger")
                .with(csrf())
                .param("backupType", "FULL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(backupService, never()).performBackupAsync(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerBackup_ServiceException() throws Exception {
        // Given
        when(backupService.performBackupAsync(any(BackupType.class)))
            .thenThrow(new RuntimeException("Backup service error"));

        // When & Then
        mockMvc.perform(post("/api/security/backup/trigger")
                .with(csrf())
                .param("backupType", "FULL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("触发备份失败: Backup service error"));

        verify(backupService).performBackupAsync(BackupType.FULL);
    }

    private Map<String, Object> createMockBackupInfo(String fileName, long fileSize, boolean isValid) {
        Map<String, Object> backupInfo = new HashMap<>();
        backupInfo.put("fileName", fileName);
        backupInfo.put("filePath", "/backup/path/" + fileName);
        backupInfo.put("fileSize", fileSize);
        backupInfo.put("lastModified", java.time.Instant.now());
        backupInfo.put("isValid", isValid);
        return backupInfo;
    }
}