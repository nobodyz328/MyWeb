package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.application.service.security.integeration.dataManage.BackupService;
import com.myweb.website_core.common.config.BackupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;

/**
 * BackupService 单元测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupProperties backupProperties;
    
    @Mock
    private AuditLogServiceAdapter auditLogService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private BackupProperties.Schedule schedule;
    
    @Mock
    private BackupProperties.Encryption encryption;
    
    @Mock
    private BackupProperties.Notification notification;
    
    @Mock
    private BackupProperties.Notification.Email emailNotification;
    
    @Mock
    private BackupProperties.Storage storage;
    
    @Mock
    private BackupProperties.Storage.Local localStorage;
    
    private BackupService backupService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // 使用lenient模式避免不必要的stubbing错误
        lenient().when(backupProperties.getBackupPath()).thenReturn(tempDir.toString());
        lenient().when(backupProperties.getRetentionDays()).thenReturn(30);
        lenient().when(backupProperties.getSchedule()).thenReturn(schedule);
        lenient().when(backupProperties.getEncryption()).thenReturn(encryption);
        lenient().when(backupProperties.getNotification()).thenReturn(notification);
        lenient().when(backupProperties.getStorage()).thenReturn(storage);
        
        lenient().when(schedule.getTimeoutMinutes()).thenReturn(60);
        
        lenient().when(encryption.isEnabled()).thenReturn(false); // 禁用加密以简化测试
        lenient().when(encryption.getKey()).thenReturn("testEncryptionKey123456789012345678901234567890");
        
        lenient().when(notification.isEnabled()).thenReturn(true);
        lenient().when(notification.getEmail()).thenReturn(emailNotification);
        
        lenient().when(emailNotification.isEnabled()).thenReturn(true);
        lenient().when(emailNotification.getRecipients()).thenReturn(new String[]{"admin@test.com"});
        lenient().when(emailNotification.isNotifyOnSuccess()).thenReturn(false);
        lenient().when(emailNotification.isNotifyOnFailure()).thenReturn(true);
        
        lenient().when(storage.getLocal()).thenReturn(localStorage);
        lenient().when(localStorage.getAlertThreshold()).thenReturn(0.8);
        
        backupService = new TestableBackupService(backupProperties, auditLogService, emailService);
    }
    
    @Test
    void testPerformBackupAsync() throws Exception {
        // Given
        BackupService.BackupType backupType = BackupService.BackupType.FULL;
        
        // When
        CompletableFuture<BackupService.BackupResult> future = backupService.performBackupAsync(backupType);
        BackupService.BackupResult result = future.get();
        
        // Then
        assertNotNull(result);
        assertEquals(backupType, result.getBackupType());
        assertNotNull(result.getBackupId());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        
        // 验证审计日志被调用
        verify(auditLogService, atLeastOnce()).logOperation(any());
    }
    
    @Test
    void testVerifyBackupIntegrity() {
        // Given
        String nonExistentFile = tempDir.resolve("nonexistent.backup").toString();
        
        // When
        boolean result = backupService.verifyBackupIntegrity(nonExistentFile);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testGetBackupList() {
        // When
        List<Map<String, Object>> backupList = backupService.getBackupList();
        
        // Then
        assertNotNull(backupList);
        assertTrue(backupList.isEmpty()); // 临时目录中没有备份文件
    }
    
    @Test
    void testCleanupExpiredBackups() {
        // When & Then - 应该不抛出异常
        assertDoesNotThrow(() -> backupService.cleanupExpiredBackups());
        
        // 验证审计日志可能被调用（如果有过期文件的话）
        verify(auditLogService, atMost(1)).logOperation(any());
    }
    
    @Test
    void testCheckStorageSpaceAndAlert() {
        // When & Then - 应该不抛出异常
        assertDoesNotThrow(() -> backupService.checkStorageSpaceAndAlert());
    }
    
    @Test
    void testBackupResultGetters() {
        // Given
        BackupService.BackupResult result = new BackupService.BackupResult(
            true,
            "test_backup_id",
            BackupService.BackupType.FULL,
            java.time.LocalDateTime.now().minusMinutes(5),
            java.time.LocalDateTime.now(),
            1024L,
            "/path/to/backup.enc",
            "checksum123",
            null
        );
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals("test_backup_id", result.getBackupId());
        assertEquals(BackupService.BackupType.FULL, result.getBackupType());
        assertEquals(1024L, result.getFileSizeBytes());
        assertEquals("/path/to/backup.enc", result.getFilePath());
        assertEquals("checksum123", result.getChecksum());
        assertNull(result.getErrorMessage());
        assertTrue(result.getDurationMillis() > 0);
    }
    
    @Test
    void testBackupTypeEnum() {
        // Test enum values
        assertEquals("完全备份", BackupService.BackupType.FULL.getDisplayName());
        assertEquals("增量备份", BackupService.BackupType.INCREMENTAL.getDisplayName());
        assertEquals("差异备份", BackupService.BackupType.DIFFERENTIAL.getDisplayName());
        
        assertEquals("备份所有数据", BackupService.BackupType.FULL.getDescription());
        assertEquals("备份自上次备份以来的变更数据", BackupService.BackupType.INCREMENTAL.getDescription());
        assertEquals("备份自上次完全备份以来的变更数据", BackupService.BackupType.DIFFERENTIAL.getDescription());
    }
    
    @Test
    void testScheduledBackupWithNotificationDisabled() {
        // Given
        when(notification.isEnabled()).thenReturn(false);
        
        // When & Then - 应该不抛出异常
        assertDoesNotThrow(() -> backupService.scheduledBackup());
        
        // 验证邮件服务没有被调用
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
    
    @Test
    void testScheduledBackupWithEmailNotificationDisabled() {
        // Given
        when(emailNotification.isEnabled()).thenReturn(false);
        
        // When & Then - 应该不抛出异常
        assertDoesNotThrow(() -> backupService.scheduledBackup());
        
        // 验证邮件服务没有被调用
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
    
    /**
     * 可测试的BackupService，重写数据库备份方法以避免依赖外部pg_dump命令
     */
    private static class TestableBackupService extends BackupService {
        
        public TestableBackupService(BackupProperties backupProperties, 
                                   AuditLogServiceAdapter auditLogService, 
                                   EmailService emailService) {
            super(backupProperties, auditLogService, emailService);
        }
        
        @Override
        protected boolean performDatabaseBackup(Path backupFilePath, BackupType backupType) {
            try {
                // 创建一个模拟的备份文件
                String mockBackupContent = String.format(
                    "-- Mock database backup\n" +
                    "-- Backup Type: %s\n" +
                    "-- Generated at: %s\n" +
                    "CREATE TABLE test_table (id INTEGER, name VARCHAR(100));\n" +
                    "INSERT INTO test_table VALUES (1, 'test data');\n",
                    backupType.name(),
                    java.time.LocalDateTime.now()
                );
                
                Files.writeString(backupFilePath, mockBackupContent);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
}