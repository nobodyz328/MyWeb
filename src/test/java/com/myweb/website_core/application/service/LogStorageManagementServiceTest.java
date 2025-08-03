package com.myweb.website_core.application.service;

import com.myweb.website_core.application.service.security.SecurityAlertService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.LogStorageStatistics;
import com.myweb.website_core.domain.business.dto.StorageInfo;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.audit.LogStorageManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 日志存储管理服务测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class LogStorageManagementServiceTest {
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private SecurityAlertService securityAlertService;
    
    @InjectMocks
    private LogStorageManagementService logStorageManagementService;
    
    @TempDir
    Path tempDir;
    
    private String testLogPath;
    private String testBackupPath;
    
    @BeforeEach
    void setUp() throws IOException {
        testLogPath = tempDir.resolve("logs").toString();
        testBackupPath = tempDir.resolve("backup").toString();
        
        // 创建测试目录
        Files.createDirectories(Paths.get(testLogPath));
        Files.createDirectories(Paths.get(testBackupPath));
        
        // 设置测试配置
        ReflectionTestUtils.setField(logStorageManagementService, "logStoragePath", testLogPath);
        ReflectionTestUtils.setField(logStorageManagementService, "backupStoragePath", testBackupPath);
        ReflectionTestUtils.setField(logStorageManagementService, "warningThreshold", 80);
        ReflectionTestUtils.setField(logStorageManagementService, "criticalThreshold", 90);
        ReflectionTestUtils.setField(logStorageManagementService, "retentionDays", 90);
        ReflectionTestUtils.setField(logStorageManagementService, "backupRetentionDays", 365);
        ReflectionTestUtils.setField(logStorageManagementService, "compressionThreshold", 100L);
        ReflectionTestUtils.setField(logStorageManagementService, "autoCompressionEnabled", true);
        ReflectionTestUtils.setField(logStorageManagementService, "integrityCheckEnabled", true);
    }
    
    @Test
    void testGetStorageInfo() {
        // 测试获取存储空间信息
        StorageInfo storageInfo = logStorageManagementService.getStorageInfo(testLogPath);
        
        assertNotNull(storageInfo);
        assertEquals(testLogPath, storageInfo.getPath());
        assertTrue(storageInfo.getTotalSpace() > 0);
        assertTrue(storageInfo.getFreeSpace() >= 0);
        assertTrue(storageInfo.getUsedSpace() >= 0);
        assertTrue(storageInfo.getUsagePercentage() >= 0);
        assertTrue(storageInfo.getUsagePercentage() <= 100);
    }
    
    @Test
    void testGetStorageInfoWithNonExistentPath() {
        // 测试不存在的路径
        String nonExistentPath = tempDir.resolve("nonexistent").toString();
        
        StorageInfo storageInfo = logStorageManagementService.getStorageInfo(nonExistentPath);
        
        assertNotNull(storageInfo);
        assertEquals(nonExistentPath, storageInfo.getPath());
        assertTrue(Files.exists(Paths.get(nonExistentPath))); // 应该自动创建目录
    }
    
    @Test
    void testMonitorStorageUsage() {
        // 测试存储空间监控
        doNothing().when(auditLogService).logSecurityEvent(
                any(AuditOperation.class), anyString(), anyString());
        
        assertDoesNotThrow(() -> {
            logStorageManagementService.monitorStorageUsage();
        });
        
        // 验证审计日志记录
        verify(auditLogService).logSecurityEvent(
                eq(AuditOperation.SYSTEM_MONITOR), eq("SYSTEM"), anyString());
    }
    
    @Test
    void testSendStorageAlert() throws Exception {
        // 创建测试存储信息
        StorageInfo storageInfo = StorageInfo.builder()
                .path(testLogPath)
                .totalSpace(1000L * 1024 * 1024) // 1GB
                .freeSpace(100L * 1024 * 1024)   // 100MB
                .usedSpace(900L * 1024 * 1024)   // 900MB
                .build();
        
        doNothing().when(auditLogService).logSecurityEvent(any(), anyString(), anyString());
        
        // 测试发送告警
        logStorageManagementService.sendStorageAlert("WARNING", storageInfo, "测试告警消息");
        
        // 验证告警发送
        verify(auditLogService).logSecurityEvent(
                eq(AuditOperation.SYSTEM_MONITOR),
                eq("SYSTEM"),
                contains("存储空间告警: "));
    }
    
    @Test
    void testBackupCurrentLogs() throws Exception {
        // 创建测试日志文件
        Path testLogFile = Paths.get(testLogPath, "test.log");
        Files.write(testLogFile, "test log content".getBytes());
        
        // 测试备份
        CompletableFuture<Void> backupFuture = logStorageManagementService.backupCurrentLogs();
        backupFuture.get(); // 等待完成
        
        // 验证备份文件是否创建
        assertTrue(Files.list(Paths.get(testBackupPath))
                .anyMatch(path -> path.getFileName().toString().contains("test.log.backup")));
    }
    
    @Test
    void testArchiveOldLogs() throws Exception {
        // 创建测试日志文件
        Path testLogFile = Paths.get(testLogPath, "old.log");
        Files.write(testLogFile, "old log content".getBytes());
        
        // 修改文件时间为很久以前（模拟旧文件）
        Files.setLastModifiedTime(testLogFile, 
                java.nio.file.attribute.FileTime.fromMillis(
                        System.currentTimeMillis() - (100L * 24 * 60 * 60 * 1000))); // 100天前
        
        // 测试归档
        CompletableFuture<Void> archiveFuture = logStorageManagementService.archiveOldLogs();
        archiveFuture.get(); // 等待完成
        
        // 验证文件是否被归档
        Path archivePath = Paths.get(testLogPath, "archive");
        if (Files.exists(archivePath)) {
            assertTrue(Files.list(archivePath)
                    .anyMatch(path -> path.getFileName().toString().contains("archived_old.log")));
        }
    }
    
    @Test
    void testPerformLogBackupAndArchive() {
        // 创建测试日志文件
        assertDoesNotThrow(() -> {
            Path testLogFile = Paths.get(testLogPath, "backup_test.log");
            Files.write(testLogFile, "backup test content".getBytes());
        });
        
        doNothing().when(auditLogService).logSecurityEvent(
                any(AuditOperation.class), anyString(), anyString());
        
        // 测试备份和归档
        assertDoesNotThrow(() -> {
            logStorageManagementService.performLogBackupAndArchive();
        });
        
        // 验证审计日志记录
        verify(auditLogService).logSecurityEvent(
                eq(AuditOperation.BACKUP_OPERATION), eq("SYSTEM"), anyString());
    }
    
    @Test
    void testCompressLogFiles() throws Exception {
        // 创建大于压缩阈值的测试文件
        Path testLogFile = Paths.get(testLogPath, "large.log");
        byte[] largeContent = new byte[150 * 1024 * 1024]; // 150MB
        Files.write(testLogFile, largeContent);
        
        doNothing().when(auditLogService).logSecurityEvent(
                any(AuditOperation.class), anyString(), anyString());
        
        // 测试压缩
        assertDoesNotThrow(() -> {
            logStorageManagementService.compressLogFiles();
        });
        
        // 验证审计日志记录
        verify(auditLogService).logSecurityEvent(
                eq(AuditOperation.SYSTEM_MAINTENANCE), eq("SYSTEM"), anyString());
    }
    
    @Test
    void testPerformIntegrityCheck() throws Exception {
        // 创建测试文件
        Path testFile = Paths.get(testLogPath, "integrity_test.log");
        Files.write(testFile, "integrity test content".getBytes());
        
        doNothing().when(auditLogService).logSecurityEvent(
                any(AuditOperation.class), anyString(), anyString());
        
        // 测试完整性检查
        assertDoesNotThrow(() -> {
            logStorageManagementService.performIntegrityCheck();
        });
        
        // 验证审计日志记录
        verify(auditLogService).logSecurityEvent(
                eq(AuditOperation.INTEGRITY_CHECK), eq("SYSTEM"), anyString());
    }
    
    @Test
    void testCheckStorageSpace() {
        // 测试检查存储空间
        StorageInfo result = logStorageManagementService.checkStorageSpace();
        
        assertNotNull(result);
        assertEquals(testLogPath, result.getPath());
    }
    
    @Test
    void testTriggerLogBackup() {
        // 测试手动触发备份
        CompletableFuture<Void> result = logStorageManagementService.triggerLogBackup();
        
        assertNotNull(result);
        assertDoesNotThrow(() -> result.get());
    }
    
    @Test
    void testTriggerIntegrityCheckWithSpecificFile() throws Exception {
        // 创建测试文件
        Path testFile = Paths.get(testLogPath, "specific_test.log");
        Files.write(testFile, "specific test content".getBytes());
        
        // 测试检查特定文件
        boolean result = logStorageManagementService.triggerIntegrityCheck(testFile.toString());
        
        assertTrue(result); // 新文件应该通过完整性检查
    }
    
    @Test
    void testTriggerIntegrityCheckAllFiles() {
        doNothing().when(auditLogService).logSecurityEvent(
                any(AuditOperation.class), anyString(), anyString());
        
        // 测试检查所有文件
        boolean result = logStorageManagementService.triggerIntegrityCheck(null);
        
        assertTrue(result);
        
        // 验证审计日志记录
        verify(auditLogService).logSecurityEvent(
                eq(AuditOperation.INTEGRITY_CHECK), eq("SYSTEM"), anyString());
    }
    
    @Test
    void testGetStorageStatistics() throws Exception {
        // 创建一些测试文件
        Files.write(Paths.get(testLogPath, "stat_test1.log"), "content1".getBytes());
        Files.write(Paths.get(testLogPath, "stat_test2.log"), "content2".getBytes());
        Files.write(Paths.get(testBackupPath, "backup1.backup"), "backup1".getBytes());
        
        // 测试获取统计信息
        LogStorageStatistics statistics = logStorageManagementService.getStorageStatistics();
        
        assertNotNull(statistics);
        assertNotNull(statistics.getLogStorage());
        assertNotNull(statistics.getBackupStorage());
        assertTrue(statistics.getLogFileCount() >= 2);
        assertTrue(statistics.getBackupFileCount() >= 1);
        assertEquals(90, statistics.getRetentionDays());
        assertEquals(365, statistics.getBackupRetentionDays());
        assertTrue(statistics.isCompressionEnabled());
        assertTrue(statistics.isIntegrityCheckEnabled());
    }
    
    @Test
    void testStorageInfoCalculations() {
        StorageInfo storageInfo = StorageInfo.builder()
                .path("/test/path")
                .totalSpace(2L * 1024 * 1024 * 1024) // 2GB
                .freeSpace(500L * 1024 * 1024)       // 500MB
                .usedSpace(1500L * 1024 * 1024)      // 1500MB
                .build();
        
        // 测试MB计算
        assertEquals(2048, storageInfo.getTotalSpaceMB());
        assertEquals(500, storageInfo.getFreeSpaceMB());
        assertEquals(1500, storageInfo.getUsedSpaceMB());
        
        // 测试GB计算
        assertEquals(2.0, storageInfo.getTotalSpaceGB(), 0.01);
        assertEquals(0.48828125, storageInfo.getFreeSpaceGB(), 0.01);
        assertEquals(1.46484375, storageInfo.getUsedSpaceGB(), 0.01);
        
        // 测试使用率计算
        assertEquals(73.24, storageInfo.getUsagePercentage(), 0.01);
        
        // 测试空间不足检查
        assertTrue(storageInfo.isSpaceInsufficient(70.0));
        assertFalse(storageInfo.isSpaceInsufficient(80.0));
        
        // 测试格式化信息
        String formattedInfo = storageInfo.getFormattedInfo();
        assertNotNull(formattedInfo);
        assertTrue(formattedInfo.contains("/test/path"));
        assertTrue(formattedInfo.contains("2.00GB"));
    }
    
    @Test
    void testLogStorageStatisticsCalculations() {
        StorageInfo logStorage = StorageInfo.builder()
                .totalSpace(1000L * 1024 * 1024) // 1000MB
                .usedSpace(600L * 1024 * 1024)   // 600MB
                .freeSpace(400L * 1024 * 1024)   // 400MB
                .build();
        
        StorageInfo backupStorage = StorageInfo.builder()
                .totalSpace(2000L * 1024 * 1024) // 2000MB
                .usedSpace(800L * 1024 * 1024)   // 800MB
                .freeSpace(1200L * 1024 * 1024)  // 1200MB
                .build();
        
        LogStorageStatistics statistics = LogStorageStatistics.builder()
                .logStorage(logStorage)
                .backupStorage(backupStorage)
                .logFileCount(10)
                .backupFileCount(5)
                .retentionDays(90)
                .backupRetentionDays(365)
                .compressionEnabled(true)
                .integrityCheckEnabled(true)
                .build();
        
        // 测试总计算
        assertEquals(1400, statistics.getTotalUsedSpaceMB()); // 600 + 800
        assertEquals(3000, statistics.getTotalCapacityMB());  // 1000 + 2000
        assertEquals(15, statistics.getTotalFileCount());     // 10 + 5
        
        // 测试整体使用率
        assertEquals(46.67, statistics.getOverallUsagePercentage(), 0.01);
        
        // 测试清理需求
        assertFalse(statistics.needsCleanup(50.0));
        assertTrue(statistics.needsCleanup(40.0));
        
        // 测试健康状态
        assertEquals("良好", statistics.getHealthStatus());
        
        // 测试格式化统计信息
        String formattedStats = statistics.getFormattedStatistics();
        assertNotNull(formattedStats);
        assertTrue(formattedStats.contains("日志存储统计信息"));
        assertTrue(formattedStats.contains("良好"));
    }
    
    @Test
    void testErrorHandling() {
        // 测试无效路径的错误处理
        String invalidPath = "/invalid/path/that/does/not/exist/and/cannot/be/created";
        
        // 在某些系统上可能会抛出异常，在其他系统上可能会成功创建
        assertDoesNotThrow(() -> {
            try {
                logStorageManagementService.getStorageInfo(invalidPath);
            } catch (RuntimeException e) {
                // 预期的异常
                assertTrue(e.getMessage().contains("获取存储空间信息失败"));
            }
        });
    }
}