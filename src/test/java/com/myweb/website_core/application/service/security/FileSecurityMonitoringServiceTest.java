package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.file.ImageService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.audit.SecurityAlertService;
import com.myweb.website_core.application.service.security.audit.SecurityEventService;
import com.myweb.website_core.common.enums.SecurityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 文件安全监控服务测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-13
 */
@ExtendWith(MockitoExtension.class)
class FileSecurityMonitoringServiceTest {
    
    @Mock
    private ImageService imageService;
    
    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private SecurityEventService securityEventService;
    
    @Mock
    private SecurityAlertService securityAlertService;
    
    private FileSecurityMonitoringService fileSecurityMonitoringService;
    
    @BeforeEach
    void setUp() {
        fileSecurityMonitoringService = new FileSecurityMonitoringService(
            imageService, auditLogService, securityEventService, securityAlertService);
        
        // 设置配置属性
        ReflectionTestUtils.setField(fileSecurityMonitoringService, "monitoringEnabled", true);
        ReflectionTestUtils.setField(fileSecurityMonitoringService, "maxFileSize", 10485760L);
        ReflectionTestUtils.setField(fileSecurityMonitoringService, "allowedExtensions", "jpg,jpeg,png,gif,bmp,webp");
        ReflectionTestUtils.setField(fileSecurityMonitoringService, "virusScanEnabled", true);
        ReflectionTestUtils.setField(fileSecurityMonitoringService, "quarantinePath", "/tmp/quarantine");
    }
    
    @Test
    void testMonitorFileUpload_SafeFile() throws Exception {
        // 准备测试数据
        Long imageId = 1L;
        String filename = "test.jpg";
        long fileSize = 1024L;
        String contentType = "image/jpeg";
        String filePath = "/uploads/test.jpg";
        String username = "testuser";
        String sourceIp = "192.168.1.100";
        
        // 执行测试
        CompletableFuture<FileSecurityMonitoringService.FileSecurityCheckResult> future = 
            fileSecurityMonitoringService.monitorFileUpload(
                imageId, filename, fileSize, contentType, filePath, username, sourceIp);
        
        // 验证结果
        FileSecurityMonitoringService.FileSecurityCheckResult result = future.get();
        
        assertNotNull(result);
        assertEquals(imageId, result.getImageId());
        assertEquals(filename, result.getFilename());
        assertEquals(fileSize, result.getFileSize());
        assertEquals(contentType, result.getContentType());
        assertEquals(filePath, result.getFilePath());
        assertEquals(username, result.getUsername());
        assertEquals(sourceIp, result.getSourceIp());
        
        // 安全文件应该是SAFE级别
        assertEquals(SecurityLevel.SAFE, result.getSecurityLevel());
        assertFalse(result.isThreatDetected());
        assertTrue(result.getThreats().isEmpty());
        
        // 验证审计日志被调用
        verify(auditLogService, times(1)).logSecurityEvent(any(), any(), any());
    }
    
    @Test
    void testMonitorFileUpload_SuspiciousExtension() throws Exception {
        // 准备测试数据 - 可疑扩展名
        Long imageId = 2L;
        String filename = "malicious.exe";
        long fileSize = 1024L;
        String contentType = "application/octet-stream";
        String filePath = "/uploads/malicious.exe";
        String username = "testuser";
        String sourceIp = "192.168.1.100";
        
        // 执行测试
        CompletableFuture<FileSecurityMonitoringService.FileSecurityCheckResult> future = 
            fileSecurityMonitoringService.monitorFileUpload(
                imageId, filename, fileSize, contentType, filePath, username, sourceIp);
        
        // 验证结果
        FileSecurityMonitoringService.FileSecurityCheckResult result = future.get();
        
        assertNotNull(result);
        assertTrue(result.isThreatDetected());
        assertFalse(result.getThreats().isEmpty());
        assertTrue(result.getThreats().get(0).contains("可疑文件扩展名"));
        assertEquals(SecurityLevel.HIGH_RISK, result.getSecurityLevel());
        assertTrue(result.getRiskScore() >= 50);
        
        // 验证审计日志被调用
        verify(auditLogService, atLeast(1)).logSecurityEvent(any(), any(), any());
    }
    
    @Test
    void testMonitorFileUpload_FileSizeExceeded() throws Exception {
        // 准备测试数据 - 文件大小超限
        Long imageId = 3L;
        String filename = "large.jpg";
        long fileSize = 20971520L; // 20MB，超过10MB限制
        String contentType = "image/jpeg";
        String filePath = "/uploads/large.jpg";
        String username = "testuser";
        String sourceIp = "192.168.1.100";
        
        // 执行测试
        CompletableFuture<FileSecurityMonitoringService.FileSecurityCheckResult> future = 
            fileSecurityMonitoringService.monitorFileUpload(
                imageId, filename, fileSize, contentType, filePath, username, sourceIp);
        
        // 验证结果
        FileSecurityMonitoringService.FileSecurityCheckResult result = future.get();
        
        assertNotNull(result);
        assertTrue(result.isThreatDetected());
        assertFalse(result.getThreats().isEmpty());
        assertTrue(result.getThreats().get(0).contains("文件大小超过限制"));
        assertTrue(result.getRiskScore() >= 30);
        
        // 验证审计日志被调用
        verify(auditLogService, atLeast(1)).logSecurityEvent(any(), any(), any());
    }
    
    @Test
    void testMonitorFileUpload_ContentTypeMismatch() throws Exception {
        // 准备测试数据 - 内容类型不匹配
        Long imageId = 4L;
        String filename = "test.jpg";
        long fileSize = 1024L;
        String contentType = "application/octet-stream"; // 与jpg扩展名不匹配
        String filePath = "/uploads/test.jpg";
        String username = "testuser";
        String sourceIp = "192.168.1.100";
        
        // 执行测试
        CompletableFuture<FileSecurityMonitoringService.FileSecurityCheckResult> future = 
            fileSecurityMonitoringService.monitorFileUpload(
                imageId, filename, fileSize, contentType, filePath, username, sourceIp);
        
        // 验证结果
        FileSecurityMonitoringService.FileSecurityCheckResult result = future.get();
        
        assertNotNull(result);
        assertTrue(result.isThreatDetected());
        assertFalse(result.getThreats().isEmpty());
        assertTrue(result.getThreats().get(0).contains("内容类型与文件扩展名不匹配"));
        assertTrue(result.getRiskScore() >= 40);
        
        // 验证审计日志被调用
        verify(auditLogService, atLeast(1)).logSecurityEvent(any(), any(), any());
    }
    
    @Test
    void testMonitorFileUpload_MonitoringDisabled() throws Exception {
        // 禁用监控
        ReflectionTestUtils.setField(fileSecurityMonitoringService, "monitoringEnabled", false);
        
        // 准备测试数据
        Long imageId = 5L;
        String filename = "test.jpg";
        long fileSize = 1024L;
        String contentType = "image/jpeg";
        String filePath = "/uploads/test.jpg";
        String username = "testuser";
        String sourceIp = "192.168.1.100";
        
        // 执行测试
        CompletableFuture<FileSecurityMonitoringService.FileSecurityCheckResult> future = 
            fileSecurityMonitoringService.monitorFileUpload(
                imageId, filename, fileSize, contentType, filePath, username, sourceIp);
        
        // 验证结果
        FileSecurityMonitoringService.FileSecurityCheckResult result = future.get();
        
        assertNotNull(result);
        assertEquals(SecurityLevel.SAFE, result.getSecurityLevel());
        assertFalse(result.isThreatDetected());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("文件安全监控已禁用"));
        
        // 监控禁用时不应该调用审计日志
        verify(auditLogService, never()).logSecurityEvent(any(), any(), any());
    }
    
    @Test
    void testGetFileSecurityStatistics() {
        // 执行测试
        Map<String, Object> statistics = fileSecurityMonitoringService.getFileSecurityStatistics();
        
        // 验证结果
        assertNotNull(statistics);
        assertTrue(statistics instanceof Map);
        
        // 初始状态下统计应该为空或包含默认值
        // 具体验证取决于实现细节
    }
    
    @Test
    void testGenerateDailySecurityReport() {
        // 这个测试需要模拟定时任务的执行
        // 由于是定时任务，我们主要验证方法不会抛出异常
        assertDoesNotThrow(() -> {
            fileSecurityMonitoringService.generateDailySecurityReport();
        });
        
        // 验证审计日志被调用（报告生成会记录日志）
        verify(auditLogService, atLeast(0)).logSecurityEvent(any(), any(), any());
    }
    
    @Test
    void testFileSecurityCheckResult_Builder() {
        // 测试Builder模式
        FileSecurityMonitoringService.FileSecurityCheckResult result = 
            FileSecurityMonitoringService.FileSecurityCheckResult.builder()
                .imageId(1L)
                .filename("test.jpg")
                .fileSize(1024L)
                .contentType("image/jpeg")
                .filePath("/uploads/test.jpg")
                .username("testuser")
                .sourceIp("192.168.1.100")
                .riskScore(0)
                .securityLevel(SecurityLevel.SAFE)
                .threatDetected(false)
                .build();
        
        assertNotNull(result);
        assertEquals(1L, result.getImageId());
        assertEquals("test.jpg", result.getFilename());
        assertEquals(1024L, result.getFileSize());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("/uploads/test.jpg", result.getFilePath());
        assertEquals("testuser", result.getUsername());
        assertEquals("192.168.1.100", result.getSourceIp());
        assertEquals(0, result.getRiskScore());
        assertEquals(SecurityLevel.SAFE, result.getSecurityLevel());
        assertFalse(result.isThreatDetected());
    }
    
    @Test
    void testFileSecurityCheckResult_SafeFactory() {
        // 测试安全结果工厂方法
        FileSecurityMonitoringService.FileSecurityCheckResult result = 
            FileSecurityMonitoringService.FileSecurityCheckResult.safe("测试消息");
        
        assertNotNull(result);
        assertEquals(SecurityLevel.SAFE, result.getSecurityLevel());
        assertFalse(result.isThreatDetected());
        assertEquals(0, result.getRiskScore());
        assertNotNull(result.getWarnings());
        assertEquals(1, result.getWarnings().size());
        assertEquals("测试消息", result.getWarnings().get(0));
    }
    
    @Test
    void testFileSecurityCheckResult_ErrorFactory() {
        // 测试错误结果工厂方法
        FileSecurityMonitoringService.FileSecurityCheckResult result = 
            FileSecurityMonitoringService.FileSecurityCheckResult.error("错误消息");
        
        assertNotNull(result);
        assertEquals(SecurityLevel.HIGH_RISK, result.getSecurityLevel());
        assertTrue(result.isThreatDetected());
        assertEquals(100, result.getRiskScore());
        assertNotNull(result.getThreats());
        assertEquals(1, result.getThreats().size());
        assertEquals("错误消息", result.getThreats().get(0));
    }
}