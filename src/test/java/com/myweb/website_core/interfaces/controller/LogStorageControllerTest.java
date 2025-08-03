package com.myweb.website_core.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.LogStorageManagementService;
import com.myweb.website_core.domain.security.dto.LogStorageStatistics;
import com.myweb.website_core.domain.business.dto.StorageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 日志存储管理控制器测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@WebMvcTest(LogStorageController.class)
class LogStorageControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private LogStorageManagementService logStorageManagementService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private StorageInfo testStorageInfo;
    private LogStorageStatistics testStatistics;
    
    @BeforeEach
    void setUp() {
        testStorageInfo = StorageInfo.builder()
                .path("/test/logs")
                .totalSpace(1000L * 1024 * 1024) // 1000MB
                .freeSpace(400L * 1024 * 1024)   // 400MB
                .usedSpace(600L * 1024 * 1024)   // 600MB
                .build();
        
        StorageInfo backupStorage = StorageInfo.builder()
                .path("/test/backup")
                .totalSpace(2000L * 1024 * 1024) // 2000MB
                .freeSpace(1200L * 1024 * 1024)  // 1200MB
                .usedSpace(800L * 1024 * 1024)   // 800MB
                .build();
        
        testStatistics = LogStorageStatistics.builder()
                .logStorage(testStorageInfo)
                .backupStorage(backupStorage)
                .logFileCount(10)
                .backupFileCount(5)
                .retentionDays(90)
                .backupRetentionDays(365)
                .compressionEnabled(true)
                .integrityCheckEnabled(true)
                .build();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetStorageStatistics() throws Exception {
        when(logStorageManagementService.getStorageStatistics()).thenReturn(testStatistics);
        
        mockMvc.perform(get("/api/admin/log-storage/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.logFileCount").value(10))
                .andExpect(jsonPath("$.backupFileCount").value(5))
                .andExpect(jsonPath("$.retentionDays").value(90))
                .andExpect(jsonPath("$.backupRetentionDays").value(365))
                .andExpect(jsonPath("$.compressionEnabled").value(true))
                .andExpect(jsonPath("$.integrityCheckEnabled").value(true))
                .andExpect(jsonPath("$.logStorage.path").value("/test/logs"))
                .andExpect(jsonPath("$.backupStorage.path").value("/test/backup"));
        
        verify(logStorageManagementService).getStorageStatistics();
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void testGetStorageStatisticsWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/log-storage/statistics"))
                .andExpect(status().isForbidden());
        
        verify(logStorageManagementService, never()).getStorageStatistics();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetStorageStatisticsWithException() throws Exception {
        when(logStorageManagementService.getStorageStatistics())
                .thenThrow(new RuntimeException("Service error"));
        
        mockMvc.perform(get("/api/admin/log-storage/statistics"))
                .andExpect(status().isInternalServerError());
        
        verify(logStorageManagementService).getStorageStatistics();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCheckStorageSpace() throws Exception {
        when(logStorageManagementService.checkStorageSpace()).thenReturn(testStorageInfo);
        
        mockMvc.perform(get("/api/admin/log-storage/space-check"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value("/test/logs"))
                .andExpect(jsonPath("$.totalSpace").value(1000L * 1024 * 1024))
                .andExpect(jsonPath("$.freeSpace").value(400L * 1024 * 1024))
                .andExpect(jsonPath("$.usedSpace").value(600L * 1024 * 1024));
        
        verify(logStorageManagementService).checkStorageSpace();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCheckStorageSpaceWithException() throws Exception {
        when(logStorageManagementService.checkStorageSpace())
                .thenThrow(new RuntimeException("Storage check failed"));
        
        mockMvc.perform(get("/api/admin/log-storage/space-check"))
                .andExpect(status().isInternalServerError());
        
        verify(logStorageManagementService).checkStorageSpace();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerLogBackup() throws Exception {
        CompletableFuture<Void> mockFuture = CompletableFuture.completedFuture(null);
        when(logStorageManagementService.triggerLogBackup()).thenReturn(mockFuture);
        
        mockMvc.perform(post("/api/admin/log-storage/backup")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("日志备份已启动，请稍后查看备份结果"));
        
        verify(logStorageManagementService).triggerLogBackup();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerLogBackupWithException() throws Exception {
        when(logStorageManagementService.triggerLogBackup())
                .thenThrow(new RuntimeException("Backup failed"));
        
        mockMvc.perform(post("/api/admin/log-storage/backup")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("触发日志备份失败: Backup failed"));
        
        verify(logStorageManagementService).triggerLogBackup();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerIntegrityCheckWithSpecificFile() throws Exception {
        String testFilePath = "/test/logs/test.log";
        when(logStorageManagementService.triggerIntegrityCheck(testFilePath)).thenReturn(true);
        
        mockMvc.perform(post("/api/admin/log-storage/integrity-check")
                        .param("filePath", testFilePath)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("文件完整性检查通过"));
        
        verify(logStorageManagementService).triggerIntegrityCheck(testFilePath);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerIntegrityCheckWithSpecificFileFailure() throws Exception {
        String testFilePath = "/test/logs/corrupted.log";
        when(logStorageManagementService.triggerIntegrityCheck(testFilePath)).thenReturn(false);
        
        mockMvc.perform(post("/api/admin/log-storage/integrity-check")
                        .param("filePath", testFilePath)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("文件完整性检查失败"));
        
        verify(logStorageManagementService).triggerIntegrityCheck(testFilePath);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerIntegrityCheckAllFiles() throws Exception {
        when(logStorageManagementService.triggerIntegrityCheck(null)).thenReturn(true);
        
        mockMvc.perform(post("/api/admin/log-storage/integrity-check")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("完整性检查已启动，请查看日志获取详细结果"));
        
        verify(logStorageManagementService).triggerIntegrityCheck(null);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testTriggerIntegrityCheckWithException() throws Exception {
        when(logStorageManagementService.triggerIntegrityCheck(anyString()))
                .thenThrow(new RuntimeException("Integrity check failed"));
        
        mockMvc.perform(post("/api/admin/log-storage/integrity-check")
                        .param("filePath", "/test/file.log")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("触发完整性检查失败: Integrity check failed"));
        
        verify(logStorageManagementService).triggerIntegrityCheck("/test/file.log");
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetStorageHealth() throws Exception {
        when(logStorageManagementService.getStorageStatistics()).thenReturn(testStatistics);
        
        mockMvc.perform(get("/api/admin/log-storage/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("良好"))
                .andExpect(jsonPath("$.overallUsage").value(46.67))
                .andExpect(jsonPath("$.logStorageUsage").value(60.0))
                .andExpect(jsonPath("$.backupStorageUsage").value(40.0))
                .andExpect(jsonPath("$.totalFiles").value(15))
                .andExpect(jsonPath("$.compressionEnabled").value(true))
                .andExpect(jsonPath("$.integrityCheckEnabled").value(true))
                .andExpect(jsonPath("$.needsAttention").value(false));
        
        verify(logStorageManagementService).getStorageStatistics();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetStorageHealthWithHighUsage() throws Exception {
        // 创建高使用率的存储信息
        StorageInfo highUsageStorage = StorageInfo.builder()
                .path("/test/logs")
                .totalSpace(1000L * 1024 * 1024) // 1000MB
                .freeSpace(50L * 1024 * 1024)    // 50MB
                .usedSpace(950L * 1024 * 1024)   // 950MB (95% usage)
                .build();
        
        LogStorageStatistics highUsageStats = LogStorageStatistics.builder()
                .logStorage(highUsageStorage)
                .backupStorage(testStatistics.getBackupStorage())
                .logFileCount(100)
                .backupFileCount(50)
                .retentionDays(90)
                .backupRetentionDays(365)
                .compressionEnabled(true)
                .integrityCheckEnabled(true)
                .build();
        
        when(logStorageManagementService.getStorageStatistics()).thenReturn(highUsageStats);
        
        mockMvc.perform(get("/api/admin/log-storage/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("严重"))
                .andExpect(jsonPath("$.logStorageUsage").value(95.0))
                .andExpect(jsonPath("$.needsAttention").value(true));
        
        verify(logStorageManagementService).getStorageStatistics();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetStorageHealthWithException() throws Exception {
        when(logStorageManagementService.getStorageStatistics())
                .thenThrow(new RuntimeException("Statistics error"));
        
        mockMvc.perform(get("/api/admin/log-storage/health"))
                .andExpect(status().isInternalServerError());
        
        verify(logStorageManagementService).getStorageStatistics();
    }
    
    @Test
    void testUnauthorizedAccess() throws Exception {
        // 测试未认证用户访问
        mockMvc.perform(get("/api/admin/log-storage/statistics"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/admin/log-storage/space-check"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(post("/api/admin/log-storage/backup")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(post("/api/admin/log-storage/integrity-check")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/admin/log-storage/health"))
                .andExpect(status().isUnauthorized());
        
        // 验证服务方法未被调用
        verify(logStorageManagementService, never()).getStorageStatistics();
        verify(logStorageManagementService, never()).checkStorageSpace();
        verify(logStorageManagementService, never()).triggerLogBackup();
        verify(logStorageManagementService, never()).triggerIntegrityCheck(anyString());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCsrfProtection() throws Exception {
        // 测试没有CSRF令牌的POST请求
        mockMvc.perform(post("/api/admin/log-storage/backup"))
                .andExpect(status().isForbidden());
        
        mockMvc.perform(post("/api/admin/log-storage/integrity-check"))
                .andExpect(status().isForbidden());
        
        // 验证服务方法未被调用
        verify(logStorageManagementService, never()).triggerLogBackup();
        verify(logStorageManagementService, never()).triggerIntegrityCheck(anyString());
    }
}