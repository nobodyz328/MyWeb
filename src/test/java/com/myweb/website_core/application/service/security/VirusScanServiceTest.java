package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.application.service.security.IPS.virusprotect.MockVirusScanService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * 病毒扫描服务测试类
 * 
 * 测试病毒扫描功能的各种场景：
 * - 正常文件扫描
 * - 病毒文件检测
 * - 扫描引擎不可用
 * - 扫描超时处理
 * - 隔离和告警功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class VirusScanServiceTest {
    
    @Mock
    private AuditLogServiceAdapter auditLogService;
    
    private MockVirusScanService mockVirusScanService;
    
    @BeforeEach
    void setUp() {
        // 模拟审计日志服务
        doNothing().when(auditLogService).logOperation(any());
        
        // 创建模拟病毒扫描服务
        mockVirusScanService = new MockVirusScanService(auditLogService);
    }
    
    @Test
    void testScanCleanFile() throws Exception {
        // 准备测试数据
        MockMultipartFile cleanFile = new MockMultipartFile(
            "file", 
            "clean-image.jpg", 
            "image/jpeg", 
            "clean file content".getBytes(StandardCharsets.UTF_8)
        );
        
        Long userId = 1L;
        String username = "testuser";
        
        // 执行扫描
        CompletableFuture<VirusScanResult> resultFuture = mockVirusScanService.scanFile(cleanFile, userId, username);
        VirusScanResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(VirusScanResult.ScanStatus.SUCCESS, result.getStatus());
        assertFalse(result.isVirusFound());
        assertEquals(VirusScanResult.ThreatLevel.NONE, result.getThreatLevel());
        assertEquals("clean-image.jpg", result.getFilename());
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals("Mock Virus Scanner", result.getEngineName());
        assertFalse(result.shouldBlockUpload());
        assertTrue(result.getScanDurationMs() > 0);
    }
    
    @Test
    void testScanEicarTestFile() throws Exception {
        // 准备EICAR测试病毒文件
        String eicarString = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        MockMultipartFile virusFile = new MockMultipartFile(
            "file", 
            "eicar-test.txt", 
            "text/plain", 
            eicarString.getBytes(StandardCharsets.UTF_8)
        );
        
        Long userId = 1L;
        String username = "testuser";
        
        // 执行扫描
        CompletableFuture<VirusScanResult> resultFuture = mockVirusScanService.scanFile(virusFile, userId, username);
        VirusScanResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(VirusScanResult.ScanStatus.SUCCESS, result.getStatus());
        assertTrue(result.isVirusFound());
        assertEquals("EICAR-Test-Signature", result.getVirusName());
        assertEquals(VirusScanResult.ThreatLevel.LOW, result.getThreatLevel());
        assertEquals("eicar-test.txt", result.getFilename());
        assertTrue(result.shouldBlockUpload());
        assertTrue(result.isRequiresQuarantine());
        assertTrue(result.isRequiresAlert());
    }
    
    @Test
    void testScanVirusNamedFile() throws Exception {
        // 准备包含病毒关键词的文件
        MockMultipartFile suspiciousFile = new MockMultipartFile(
            "file", 
            "malware-sample.jpg", 
            "image/jpeg", 
            "suspicious content".getBytes(StandardCharsets.UTF_8)
        );
        
        Long userId = 1L;
        String username = "testuser";
        
        // 执行扫描
        CompletableFuture<VirusScanResult> resultFuture = mockVirusScanService.scanFile(suspiciousFile, userId, username);
        VirusScanResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(VirusScanResult.ScanStatus.SUCCESS, result.getStatus());
        assertTrue(result.isVirusFound());
        assertNotNull(result.getVirusName());
        assertTrue(result.shouldBlockUpload());
    }
    
    @Test
    void testScanSuspiciousContent() throws Exception {
        // 准备包含可疑脚本的文件
        String suspiciousContent = "<script>alert('virus')</script>";
        MockMultipartFile suspiciousFile = new MockMultipartFile(
            "file", 
            "suspicious.html", 
            "text/html", 
            suspiciousContent.getBytes(StandardCharsets.UTF_8)
        );
        
        Long userId = 1L;
        String username = "testuser";
        
        // 执行扫描
        CompletableFuture<VirusScanResult> resultFuture = mockVirusScanService.scanFile(suspiciousFile, userId, username);
        VirusScanResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(VirusScanResult.ScanStatus.SUCCESS, result.getStatus());
        assertTrue(result.isVirusFound());
        assertEquals("Suspicious.Pattern.Detected", result.getVirusName());
        assertEquals(VirusScanResult.ThreatLevel.MEDIUM, result.getThreatLevel());
        assertTrue(result.shouldBlockUpload());
    }
    
    @Test
    void testScanInputStream() throws Exception {
        // 准备测试数据
        String content = "clean file content";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        
        Long userId = 1L;
        String username = "testuser";
        String filename = "test-stream.txt";
        
        // 执行扫描
        CompletableFuture<VirusScanResult> resultFuture = mockVirusScanService.scanInputStream(
            new java.io.ByteArrayInputStream(contentBytes), filename, userId, username);
        VirusScanResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(VirusScanResult.ScanStatus.SUCCESS, result.getStatus());
        assertFalse(result.isVirusFound());
        assertEquals(filename, result.getFilename());
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
    }
    
    @Test
    void testScanBytes() throws Exception {
        // 准备测试数据
        String content = "clean file content";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        
        Long userId = 1L;
        String username = "testuser";
        String filename = "test-bytes.txt";
        
        // 执行扫描
        CompletableFuture<VirusScanResult> resultFuture = mockVirusScanService.scanBytes(
            contentBytes, filename, userId, username);
        VirusScanResult result = resultFuture.get();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(VirusScanResult.ScanStatus.SUCCESS, result.getStatus());
        assertFalse(result.isVirusFound());
        assertEquals(filename, result.getFilename());
        assertEquals(contentBytes.length, result.getFileSize());
    }
    
    @Test
    void testEngineAvailability() {
        // 测试引擎可用性
        assertTrue(mockVirusScanService.isAvailable());
        
        // 测试引擎信息
        String engineInfo = mockVirusScanService.getEngineInfo();
        assertNotNull(engineInfo);
        assertTrue(engineInfo.contains("Mock Virus Scanner"));
        assertTrue(engineInfo.contains("Available"));
    }
    
    @Test
    void testUpdateVirusDatabase() throws Exception {
        // 测试病毒库更新
        CompletableFuture<Boolean> updateFuture = mockVirusScanService.updateVirusDatabase();
        Boolean result = updateFuture.get();
        
        assertTrue(result);
    }
    
    @Test
    void testVirusScanResultMethods() {
        // 测试成功结果
        VirusScanResult successResult = VirusScanResult.success(
            "test.jpg", 1L, "user", "TestEngine", 100L);
        
        assertFalse(successResult.shouldBlockUpload());
        assertEquals("文件安全，未发现威胁", successResult.getSummary());
        
        // 测试病毒检测结果
        VirusScanResult virusResult = VirusScanResult.virusDetected(
            "virus.exe", "TestVirus", VirusScanResult.ThreatLevel.HIGH, 
            1L, "user", "TestEngine", 200L);
        
        assertTrue(virusResult.shouldBlockUpload());
        assertTrue(virusResult.getSummary().contains("发现病毒"));
        assertTrue(virusResult.getSummary().contains("TestVirus"));
        
        // 测试失败结果
        VirusScanResult failureResult = VirusScanResult.failure(
            "error.txt", "Scan failed", 1L, "user", "TestEngine");
        
        assertTrue(failureResult.shouldBlockUpload());
        assertTrue(failureResult.getSummary().contains("扫描失败"));
        
        // 测试不可用结果
        VirusScanResult unavailableResult = VirusScanResult.unavailable(
            "test.txt", 1L, "user", "TestEngine");
        
        assertFalse(unavailableResult.shouldBlockUpload()); // 不可用时不阻止上传
        assertTrue(unavailableResult.getSummary().contains("扫描异常"));
    }
}