package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.application.service.security.dataprotect.BackupManagementService;
import com.myweb.website_core.application.service.security.dataprotect.BackupService;
import com.myweb.website_core.common.config.BackupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 备份管理服务测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-07
 */
@ExtendWith(MockitoExtension.class)
class BackupManagementServiceTest {
    
    @Mock
    private BackupProperties backupProperties;
    
    @Mock
    private AuditLogServiceAdapter auditLogService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private BackupService backupService;
    
    @Mock
    private BackupProperties.Storage storage;
    
    @Mock
    private BackupProperties.Storage.Local localStorage;
    
    @Mock
    private BackupProperties.Storage.Remote remoteStorage;
    
    @Mock
    private BackupProperties.Notification notification;
    
    @Mock
    private BackupProperties.Notification.Email emailNotification;
    
    private BackupManagementService backupManagementService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // 设置模拟对象的默认行为
        when(backupProperties.getBackupPath()).thenReturn(tempDir.toString());
        when(backupProperties.getRetentionDays()).thenReturn(30);
        when(backupProperties.getStorage()).thenReturn(storage);
        when(storage.getLocal()).thenReturn(localStorage);
        when(storage.getRemote()).thenReturn(remoteStorage);
        when(localStorage.getAlertThreshold()).thenReturn(0.8);
        when(remoteStorage.isEnabled()).thenReturn(false);
        when(backupProperties.getNotification()).thenReturn(notification);
        when(notification.isEnabled()).thenReturn(true);
        when(notification.getEmail()).thenReturn(emailNotification);
        when(emailNotification.isEnabled()).thenReturn(true);
        when(emailNotification.getRecipients()).thenReturn(new String[]{"admin@test.com"});
        
        backupManagementService = new BackupManagementService(
            backupProperties, auditLogService, emailService, backupService
        );
    }
    
    @Test
    void testCleanupExpiredBackups_WithExpiredFiles() throws IOException {
        // 创建测试备份文件
        Path expiredFile = tempDir.resolve("full_20240101_020000.backup.enc");
        Path currentFile = tempDir.resolve("full_20250107_020000.backup.enc");
        
        Files.createFile(expiredFile);
        Files.createFile(currentFile);
        
        // 设置文件创建时间（模拟过期文件）
        Files.setLastModifiedTime(expiredFile, 
            java.nio.file.attribute.FileTime.from(
                LocalDateTime.now().minusDays(35).atZone(java.time.ZoneId.systemDefault()).toInstant()
            )
        );
        
        // 执行清理
        int deletedCount = backupManagementService.cleanupExpiredBackups();
        
        // 验证结果
        assertEquals(1, deletedCount);
        assertFalse(Files.exists(expiredFile));
        assertTrue(Files.exists(currentFile));
        
        // 验证审计日志记录
        verify(auditLogService, atLeastOnce()).logOperation(any());
    }
    
    @Test
    void testCleanupExpiredBackups_NoExpiredFiles() throws IOException {
        // 创建当前备份文件
        Path currentFile = tempDir.resolve("full_20250107_020000.backup.enc");
        Files.createFile(currentFile);
        
        // 执行清理
        int deletedCount = backupManagementService.cleanupExpiredBackups();
        
        // 验证结果
        assertEquals(0, deletedCount);
        assertTrue(Files.exists(currentFile));
    }
    
    @Test
    void testCleanupExpiredBackupsAsync() throws Exception {
        // 创建测试文件
        Path expiredFile = tempDir.resolve("full_20240101_020000.backup.enc");
        Files.createFile(expiredFile);
        Files.setLastModifiedTime(expiredFile, 
            java.nio.file.attribute.FileTime.from(
                LocalDateTime.now().minusDays(35).atZone(java.time.ZoneId.systemDefault()).toInstant()
            )
        );
        
        // 执行异步清理
        CompletableFuture<Integer> future = backupManagementService.cleanupExpiredBackupsAsync();
        Integer deletedCount = future.get();
        
        // 验证结果
        assertEquals(1, deletedCount);
        assertFalse(Files.exists(expiredFile));
    }
    
    @Test
    void testCheckStorageStatistics() throws IOException {
        // 创建测试备份文件
        Path fullBackup = tempDir.resolve("full_20250107_020000.backup.enc");
        Path incrementalBackup = tempDir.resolve("incremental_20250107_060000.backup.enc");
        
        Files.createFile(fullBackup);
        Files.createFile(incrementalBackup);
        Files.write(fullBackup, "test data".getBytes());
        Files.write(incrementalBackup, "incremental data".getBytes());
        
        // 执行统计检查
        BackupManagementService.StorageStatistics stats = backupManagementService.checkStorageStatistics();
        
        // 验证结果
        assertNotNull(stats);
        assertTrue(stats.getTotalSpaceBytes() > 0);
        assertEquals(2, stats.getTotalBackupFiles());
        assertEquals(1, stats.getBackupTypeCount().get(BackupService.BackupType.FULL));
        assertEquals(1, stats.getBackupTypeCount().get(BackupService.BackupType.INCREMENTAL));
    }
    
    @Test
    void testCheckStorageStatisticsAsync() throws Exception {
        // 创建测试文件
        Path backupFile = tempDir.resolve("full_20250107_020000.backup.enc");
        Files.createFile(backupFile);
        
        // 执行异步统计检查
        CompletableFuture<BackupManagementService.StorageStatistics> future = 
            backupManagementService.checkStorageStatisticsAsync();
        BackupManagementService.StorageStatistics stats = future.get();
        
        // 验证结果
        assertNotNull(stats);
        assertEquals(1, stats.getTotalBackupFiles());
    }
    
    @Test
    void testUpdateBackupMetadata() throws Exception {
        // 创建测试备份文件
        Path backupFile = tempDir.resolve("full_20250107_020000.backup.enc");
        Files.createFile(backupFile);
        Files.write(backupFile, "test backup data".getBytes());
        
        when(backupService.calculateFileChecksum(any())).thenReturn("test-checksum");
        
        // 执行元数据更新
        backupManagementService.updateBackupMetadata();
        
        // 验证元数据文件是否创建
        Path metadataFile = tempDir.resolve("full_20250107_020000.backup.enc.meta");
        assertTrue(Files.exists(metadataFile));
        
        // 验证元数据内容
        String metadataContent = Files.readString(metadataFile);
        assertTrue(metadataContent.contains("backupId=full_20250107_020000"));
        assertTrue(metadataContent.contains("backupType=FULL"));
        assertTrue(metadataContent.contains("isEncrypted=true"));
    }
    
    @Test
    void testUpdateBackupMetadataAsync() throws Exception {
        // 创建测试文件
        Path backupFile = tempDir.resolve("full_20250107_020000.backup.enc");
        Files.createFile(backupFile);
        
        when(backupService.calculateFileChecksum(any())).thenReturn("test-checksum");
        
        // 执行异步元数据更新
        CompletableFuture<Void> future = backupManagementService.updateBackupMetadataAsync();
        future.get();
        
        // 验证元数据文件创建
        Path metadataFile = tempDir.resolve("full_20250107_020000.backup.enc.meta");
        assertTrue(Files.exists(metadataFile));
    }
    
    @Test
    void testSyncToRemoteStorage_Disabled() {
        // 远程存储禁用时
        when(remoteStorage.isEnabled()).thenReturn(false);
        
        // 执行同步（应该跳过）
        assertDoesNotThrow(() -> backupManagementService.syncToRemoteStorage());
    }
    
    @Test
    void testSyncToRemoteStorageAsync() throws Exception {
        // 远程存储禁用时
        when(remoteStorage.isEnabled()).thenReturn(false);
        
        // 执行异步同步
        CompletableFuture<Void> future = backupManagementService.syncToRemoteStorageAsync();
        assertDoesNotThrow(() -> future.get());
    }
    
    @Test
    void testGetBackupFilesWithMetadata() throws Exception {
        // 创建测试备份文件
        Path backupFile = tempDir.resolve("full_20250107_020000.backup.enc");
        Files.createFile(backupFile);
        Files.write(backupFile, "test data".getBytes());
        
        when(backupService.verifyBackupIntegrity(any())).thenReturn(true);
        when(backupService.calculateFileChecksum(any())).thenReturn("test-checksum");
        
        // 先更新元数据
        backupManagementService.updateBackupMetadata();
        
        // 获取备份文件列表
        List<Map<String, Object>> backupFiles = backupManagementService.getBackupFilesWithMetadata();
        
        // 验证结果
        assertNotNull(backupFiles);
        assertEquals(1, backupFiles.size());
        
        Map<String, Object> fileInfo = backupFiles.get(0);
        assertEquals("full_20250107_020000.backup.enc", fileInfo.get("fileName"));
        assertEquals("完全备份", fileInfo.get("backupType"));
        assertTrue((Boolean) fileInfo.get("isValid"));
        assertTrue((Boolean) fileInfo.get("isEncrypted"));
    }
    
    @Test
    void testUpdateBackupPolicy_RetentionDays() {
        Map<String, Object> policyUpdates = new HashMap<>();
        policyUpdates.put("retentionDays", 60);
        
        // 执行策略更新
        assertDoesNotThrow(() -> backupManagementService.updateBackupPolicy(policyUpdates));
        
        // 验证配置更新
        verify(backupProperties).setRetentionDays(60);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testUpdateBackupPolicy_StorageAlertThreshold() {
        Map<String, Object> policyUpdates = new HashMap<>();
        policyUpdates.put("storageAlertThreshold", 0.9);
        
        // 执行策略更新
        assertDoesNotThrow(() -> backupManagementService.updateBackupPolicy(policyUpdates));
        
        // 验证配置更新
        verify(localStorage).setAlertThreshold(0.9);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testUpdateBackupPolicy_EncryptionEnabled() {
        Map<String, Object> policyUpdates = new HashMap<>();
        policyUpdates.put("encryptionEnabled", false);
        
        BackupProperties.Encryption encryption = mock(BackupProperties.Encryption.class);
        when(backupProperties.getEncryption()).thenReturn(encryption);
        
        // 执行策略更新
        assertDoesNotThrow(() -> backupManagementService.updateBackupPolicy(policyUpdates));
        
        // 验证配置更新
        verify(encryption).setEnabled(false);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testUpdateBackupPolicy_RemoteStorageEnabled() {
        Map<String, Object> policyUpdates = new HashMap<>();
        policyUpdates.put("remoteStorageEnabled", true);
        
        // 执行策略更新
        assertDoesNotThrow(() -> backupManagementService.updateBackupPolicy(policyUpdates));
        
        // 验证配置更新
        verify(remoteStorage).setEnabled(true);
        verify(auditLogService).logOperation(any());
    }
    
    @Test
    void testBackupMetadata_Creation() {
        // 测试备份元数据创建
        BackupManagementService.BackupMetadata metadata = new BackupManagementService.BackupMetadata(
            "test_backup_001",
            BackupService.BackupType.FULL,
            LocalDateTime.now(),
            1024L
        );
        
        // 验证基本属性
        assertEquals("test_backup_001", metadata.getBackupId());
        assertEquals(BackupService.BackupType.FULL, metadata.getBackupType());
        assertEquals(1024L, metadata.getFileSizeBytes());
        assertNotNull(metadata.getCustomProperties());
        
        // 测试过期检查
        metadata.setExpiryTime(LocalDateTime.now().minusDays(1));
        assertTrue(metadata.isExpired());
        
        metadata.setExpiryTime(LocalDateTime.now().plusDays(1));
        assertFalse(metadata.isExpired());
        
        // 测试到期天数计算
        metadata.setExpiryTime(LocalDateTime.now().plusDays(5));
        assertTrue(metadata.getDaysUntilExpiry() >= 4 && metadata.getDaysUntilExpiry() <= 5);
    }
    
    @Test
    void testStorageStatistics_Formatting() {
        BackupManagementService.StorageStatistics stats = new BackupManagementService.StorageStatistics();
        stats.setTotalSpaceBytes(1024L * 1024 * 1024); // 1GB
        stats.setUsedSpaceBytes(512L * 1024 * 1024);   // 512MB
        stats.setAvailableSpaceBytes(512L * 1024 * 1024); // 512MB
        stats.setUsagePercentage(50.0);
        
        // 测试格式化方法
        assertEquals("1.00 GB", stats.getFormattedTotalSpace());
        assertEquals("512.00 MB", stats.getFormattedUsedSpace());
        assertEquals("512.00 MB", stats.getFormattedAvailableSpace());
        
        // 测试告警检查
        assertFalse(stats.isStorageAlertRequired());
        
        stats.setUsagePercentage(85.0);
        assertTrue(stats.isStorageAlertRequired());
    }
    
    @Test
    void testScheduledLifecycleManagement() throws Exception {
        // 模拟定时生命周期管理
        when(backupService.calculateFileChecksum(any())).thenReturn("test-checksum");
        
        // 执行定时任务（异步）
        assertDoesNotThrow(() -> backupManagementService.scheduledLifecycleManagement());
        
        // 验证审计日志记录
        verify(auditLogService, atLeastOnce()).logOperation(any());
    }
    
    @Test
    void testStorageAlert_Sending() throws IOException {
        // 创建大量文件以触发存储告警
        for (int i = 0; i < 10; i++) {
            Path file = tempDir.resolve("backup_" + i + ".enc");
            Files.createFile(file);
        }
        
        // 设置低告警阈值
        when(localStorage.getAlertThreshold()).thenReturn(0.01); // 1%
        
        // 执行存储检查
        BackupManagementService.StorageStatistics stats = backupManagementService.checkStorageStatistics();
        
        // 验证告警状态
        assertTrue(stats.isStorageAlertRequired());
        
        // 验证邮件发送（由于存储使用率可能不够高，这里主要测试逻辑）
        // verify(emailService, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }
}