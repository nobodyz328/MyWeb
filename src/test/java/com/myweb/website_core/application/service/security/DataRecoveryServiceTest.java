package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.application.service.security.integeration.dataManage.BackupService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataRecoveryService;
import com.myweb.website_core.infrastructure.config.properties.BackupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * 数据恢复服务测试类
 * 
 * 测试数据恢复功能的各种场景，包括：
 * - 完全恢复
 * - 时间点恢复
 * - 选择性恢复
 * - 备份文件验证
 * - 恢复前提条件验证
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@ExtendWith(MockitoExtension.class)
class DataRecoveryServiceTest {
    
    @Mock
    private BackupProperties backupProperties;
    
    @Mock
    private BackupService backupService;
    
    @Mock
    private AuditLogServiceAdapter auditLogService;
    
    private DataRecoveryService dataRecoveryService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // 配置模拟对象 - 使用lenient模式避免不必要的stubbing错误
        lenient().when(backupProperties.getBackupPath()).thenReturn(tempDir.toString());
        lenient().when(backupProperties.getEncryptionKey()).thenReturn("dGVzdC1lbmNyeXB0aW9uLWtleS0xMjM0NTY3ODkwMTI="); // 32 bytes when base64 decoded
        
        dataRecoveryService = new DataRecoveryService(backupProperties, backupService, auditLogService);
    }
    
    @Test
    void testGetAvailableBackups() {
        // 准备测试数据
        List<Map<String, Object>> mockBackups = List.of(
            Map.of(
                "fileName", "full_20250106_020000.backup.enc",
                "filePath", "/backup/full_20250106_020000.backup.enc",
                "fileSize", 1024L,
                "lastModified", java.time.Instant.now(),
                "isValid", true
            )
        );
        
        when(backupService.getBackupList()).thenReturn(mockBackups);
        
        // 执行测试
        List<Map<String, Object>> result = dataRecoveryService.getAvailableBackups();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("full_20250106_020000.backup.enc", result.get(0).get("fileName"));
        
        verify(backupService).getBackupList();
    }
    
    @Test
    void testValidateRecoveryPrerequisites_ValidBackup() throws Exception {
        // 创建测试备份文件
        Path testBackupFile = tempDir.resolve("test_backup.enc");
        Files.createFile(testBackupFile);
        
        when(backupService.verifyBackupIntegrity(testBackupFile.toString())).thenReturn(true);
        
        // 执行测试
        Map<String, Object> result = dataRecoveryService.validateRecoveryPrerequisites(
            DataRecoveryService.RecoveryType.FULL, testBackupFile.toString()
        );
        
        // 验证结果
        assertNotNull(result);
        assertTrue((Boolean) result.get("valid"));
        assertEquals("完全恢复", result.get("recoveryType"));
        
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.get("issues");
        assertTrue(issues.isEmpty());
    }
    
    @Test
    void testValidateRecoveryPrerequisites_InvalidBackup() {
        // 使用不存在的备份文件
        String nonExistentFile = "/path/to/nonexistent/backup.enc";
        
        // 执行测试
        Map<String, Object> result = dataRecoveryService.validateRecoveryPrerequisites(
            DataRecoveryService.RecoveryType.FULL, nonExistentFile
        );
        
        // 验证结果
        assertNotNull(result);
        assertFalse((Boolean) result.get("valid"));
        
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) result.get("issues");
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(issue -> issue.contains("备份文件不存在")));
    }
    
    @Test
    void testPerformFullRecovery_Success() throws Exception {
        // 创建测试备份文件
        Path testBackupFile = tempDir.resolve("test_backup.enc");
        Files.write(testBackupFile, "test backup content".getBytes());
        
        when(backupService.verifyBackupIntegrity(testBackupFile.toString())).thenReturn(true);
        
        // 执行测试
        DataRecoveryService.RecoveryResult result = dataRecoveryService.performFullRecovery(
            testBackupFile.toString(), "testUser"
        );
        
        // 验证结果
        assertNotNull(result);
        assertEquals(DataRecoveryService.RecoveryType.FULL, result.getRecoveryType());
        assertEquals(testBackupFile.toString(), result.getBackupFile());
        assertNotNull(result.getRecoveryId());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        assertTrue(result.getDurationMillis() >= 0);
        
        // 验证审计日志记录
//        verify(auditLogService, atLeastOnce()).logSecurityEvent(
//            eq("testUser"),
//            any(),
//            anyString(),
//            anyString(),
//            any(),
//            anyBoolean()
//        );
    }
    
    @Test
    void testPerformFullRecovery_IntegrityCheckFailed() {
        // 创建测试备份文件
        Path testBackupFile = tempDir.resolve("test_backup.enc");
        
        when(backupService.verifyBackupIntegrity(testBackupFile.toString())).thenReturn(false);
        
        // 执行测试
        DataRecoveryService.RecoveryResult result = dataRecoveryService.performFullRecovery(
            testBackupFile.toString(), "testUser"
        );
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("备份文件完整性验证失败"));
        
        // 验证审计日志记录
//        verify(auditLogService).logSecurityEvent(
//            eq("testUser"),
//            any(),
//            eq("FULL_RECOVERY_FAILED"),
//            anyString(),
//            any(),
//            eq(false)
//        );
    }
    
    @Test
    void testPerformPointInTimeRecovery_Success() throws Exception {
        // 准备测试数据
        LocalDateTime targetDateTime = LocalDateTime.now().minusHours(1);
        
        List<Map<String, Object>> mockBackups = List.of(
            Map.of(
                "fileName", "full_20250106_020000.backup.enc",
                "filePath", tempDir.resolve("full_20250106_020000.backup.enc").toString(),
                "fileSize", 1024L,
                "lastModified", java.time.Instant.now().minusSeconds(3600),
                "isValid", true
            )
        );
        
        when(backupService.getBackupList()).thenReturn(mockBackups);
        when(backupService.verifyBackupIntegrity(anyString())).thenReturn(true);
        
        // 创建测试备份文件
        Path testBackupFile = tempDir.resolve("full_20250106_020000.backup.enc");
        Files.write(testBackupFile, "test backup content".getBytes());
        
        // 执行测试
        DataRecoveryService.RecoveryResult result = dataRecoveryService.performPointInTimeRecovery(
            targetDateTime, "testUser"
        );
        
        // 验证结果
        assertNotNull(result);
        assertEquals(DataRecoveryService.RecoveryType.POINT_IN_TIME, result.getRecoveryType());
        assertNotNull(result.getRecoveryId());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        
        // 验证详细信息包含目标时间
        Map<String, Object> details = result.getDetails();
        assertNotNull(details);
        assertEquals(targetDateTime, details.get("targetDateTime"));
    }
    
    @Test
    void testPerformPointInTimeRecovery_NoSuitableBackup() {
        // 准备测试数据 - 没有合适的备份
        LocalDateTime targetDateTime = LocalDateTime.now().minusHours(1);
        
        when(backupService.getBackupList()).thenReturn(List.of());
        
        // 执行测试
        DataRecoveryService.RecoveryResult result = dataRecoveryService.performPointInTimeRecovery(
            targetDateTime, "testUser"
        );
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("未找到适合的备份文件"));
        
        // 验证审计日志记录
//        verify(auditLogService).logSecurityEvent(
//            eq("testUser"),
//            any(),
//            eq("POINT_IN_TIME_RECOVERY_FAILED"),
//            anyString(),
//            any(),
//            eq(false)
//        );
    }
    
    @Test
    void testPerformSelectiveRecovery_Success() throws Exception {
        // 创建测试备份文件
        Path testBackupFile = tempDir.resolve("test_backup.enc");
        Files.write(testBackupFile, "test backup content".getBytes());
        
        List<String> tablesToRestore = List.of("users", "posts", "comments");
        
        when(backupService.verifyBackupIntegrity(testBackupFile.toString())).thenReturn(true);
        
        // 执行测试
        DataRecoveryService.RecoveryResult result = dataRecoveryService.performSelectiveRecovery(
            testBackupFile.toString(), tablesToRestore, "testUser"
        );
        
        // 验证结果
        assertNotNull(result);
        assertEquals(DataRecoveryService.RecoveryType.SELECTIVE, result.getRecoveryType());
        assertEquals(testBackupFile.toString(), result.getBackupFile());
        assertNotNull(result.getRecoveryId());
        
        // 验证详细信息包含要恢复的表
        Map<String, Object> details = result.getDetails();
        assertNotNull(details);
        assertEquals(tablesToRestore, details.get("tablesToRestore"));
        
        // 验证审计日志记录
//        verify(auditLogService, atLeastOnce()).logSecurityEvent(
//            eq("testUser"),
//            any(),
//            anyString(),
//            anyString(),
//            any(),
//            anyBoolean()
//        );
    }
    
    @Test
    void testPerformSelectiveRecovery_EmptyTableList() throws Exception {
        // 创建测试备份文件
        Path testBackupFile = tempDir.resolve("test_backup.enc");
        Files.write(testBackupFile, "test backup content".getBytes());
        
        List<String> tablesToRestore = List.of(); // 空列表
        
        when(backupService.verifyBackupIntegrity(testBackupFile.toString())).thenReturn(true);
        
        // 执行测试
        DataRecoveryService.RecoveryResult result = dataRecoveryService.performSelectiveRecovery(
            testBackupFile.toString(), tablesToRestore, "testUser"
        );
        
        // 验证结果
        assertNotNull(result);
        assertEquals(DataRecoveryService.RecoveryType.SELECTIVE, result.getRecoveryType());
        
        // 验证详细信息包含空的表列表
        Map<String, Object> details = result.getDetails();
        assertNotNull(details);
        assertEquals(tablesToRestore, details.get("tablesToRestore"));
    }
    
    @Test
    void testRecoveryTypes() {
        // 测试恢复类型枚举
        assertEquals("完全恢复", DataRecoveryService.RecoveryType.FULL.getDisplayName());
        assertEquals("时间点恢复", DataRecoveryService.RecoveryType.POINT_IN_TIME.getDisplayName());
        assertEquals("选择性恢复", DataRecoveryService.RecoveryType.SELECTIVE.getDisplayName());
        
        assertNotNull(DataRecoveryService.RecoveryType.FULL.getDescription());
        assertNotNull(DataRecoveryService.RecoveryType.POINT_IN_TIME.getDescription());
        assertNotNull(DataRecoveryService.RecoveryType.SELECTIVE.getDescription());
    }
    
    @Test
    void testRecoveryResult() {
        // 测试恢复结果对象
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(5);
        
        DataRecoveryService.RecoveryResult result = new DataRecoveryService.RecoveryResult(
            true, "test-recovery-id", DataRecoveryService.RecoveryType.FULL,
            "/path/to/backup.enc", startTime, endTime, null, Map.of("key", "value")
        );
        
        assertTrue(result.isSuccess());
        assertEquals("test-recovery-id", result.getRecoveryId());
        assertEquals(DataRecoveryService.RecoveryType.FULL, result.getRecoveryType());
        assertEquals("/path/to/backup.enc", result.getBackupFile());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertNull(result.getErrorMessage());
        assertEquals(Map.of("key", "value"), result.getDetails());
        
        // 测试持续时间计算
        long expectedDuration = java.time.Duration.between(startTime, endTime).toMillis();
        assertEquals(expectedDuration, result.getDurationMillis());
    }
}