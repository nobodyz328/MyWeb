package com.myweb.website_core.application.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.IPS.virusprotect.DependencySecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 依赖安全服务测试
 * 
 * @author MyWeb Security Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DependencySecurityServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private ObjectMapper objectMapper;
    private DependencySecurityService dependencySecurityService;
    private Path tempReportDir;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        dependencySecurityService = new DependencySecurityService(mailSender, objectMapper);
        
        // 创建临时报告目录
        tempReportDir = Files.createTempDirectory("dependency-check-test");
        
        // 设置测试配置
        ReflectionTestUtils.setField(dependencySecurityService, "reportPath", tempReportDir.toString());
        ReflectionTestUtils.setField(dependencySecurityService, "notificationEnabled", true);
        ReflectionTestUtils.setField(dependencySecurityService, "notificationRecipients", 
                                   List.of("test@example.com"));
        ReflectionTestUtils.setField(dependencySecurityService, "highSeverityThreshold", 7.0);
    }

    @Test
    void testParseSecurityReport_WithValidReport() throws IOException {
        // 准备测试数据
        String reportJson = """
            {
                "projectInfo": {
                    "name": "MyWeb Test Project"
                },
                "dependencies": [
                    {
                        "fileName": "test-dependency-1.0.jar",
                        "vulnerabilities": [
                            {
                                "name": "CVE-2023-12345",
                                "severity": "HIGH",
                                "description": "Test high severity vulnerability",
                                "cvssv3": {
                                    "baseScore": 8.5
                                }
                            }
                        ]
                    },
                    {
                        "fileName": "safe-dependency-2.0.jar"
                    }
                ]
            }
            """;
        
        // 创建测试报告文件
        Path reportFile = tempReportDir.resolve("dependency-check-report.json");
        Files.writeString(reportFile, reportJson);
        
        // 执行测试
        DependencySecurityService.SecurityScanResult result = dependencySecurityService.parseSecurityReport();
        
        // 验证结果
        assertNotNull(result);
        assertEquals("MyWeb Test Project", result.getProjectName());
        assertEquals(2, result.getTotalDependencies());
        assertEquals(1, result.getVulnerableDependencies());
        assertEquals(1, result.getVulnerabilities().size());
        
        DependencySecurityService.VulnerabilityInfo vuln = result.getVulnerabilities().get(0);
        assertEquals("CVE-2023-12345", vuln.getCve());
        assertEquals("HIGH", vuln.getSeverity());
        assertEquals(8.5, vuln.getScore());
        assertEquals("test-dependency-1.0.jar", vuln.getDependencyName());
    }

    @Test
    void testParseSecurityReport_WithMissingReport() {
        // 执行测试（报告文件不存在）
        DependencySecurityService.SecurityScanResult result = dependencySecurityService.parseSecurityReport();
        
        // 验证结果
        assertNotNull(result);
        assertEquals("Unknown", result.getProjectName());
        assertEquals(0, result.getTotalDependencies());
        assertEquals(0, result.getVulnerableDependencies());
        assertTrue(result.getVulnerabilities().isEmpty());
    }

    @Test
    void testGenerateSecuritySummary() {
        // 准备测试数据
        DependencySecurityService.SecurityScanResult result = DependencySecurityService.SecurityScanResult.builder()
            .projectName("Test Project")
            .scanTime(LocalDateTime.of(2025, 1, 1, 12, 0))
            .totalDependencies(10)
            .vulnerableDependencies(2)
            .vulnerabilities(List.of(
                DependencySecurityService.VulnerabilityInfo.builder()
                    .cve("CVE-2023-12345")
                    .severity("HIGH")
                    .score(8.5)
                    .description("Test high vulnerability")
                    .dependencyName("test-dep.jar")
                    .build(),
                DependencySecurityService.VulnerabilityInfo.builder()
                    .cve("CVE-2023-67890")
                    .severity("MEDIUM")
                    .score(5.0)
                    .description("Test medium vulnerability")
                    .dependencyName("another-dep.jar")
                    .build()
            ))
            .build();
        
        // 执行测试
        String summary = dependencySecurityService.generateSecuritySummary(result);
        
        // 验证结果
        assertNotNull(summary);
        assertTrue(summary.contains("Test Project"));
        assertTrue(summary.contains("2025-01-01 12:00:00"));
        assertTrue(summary.contains("总依赖数量: 10"));
        assertTrue(summary.contains("存在漏洞的依赖: 2"));
        assertTrue(summary.contains("高危漏洞: 1"));
        assertTrue(summary.contains("中危漏洞: 1"));
        assertTrue(summary.contains("CVE-2023-12345"));
        assertTrue(summary.contains("⚠️ 高危漏洞详情"));
    }

    @Test
    void testGenerateSecuritySummary_NoHighSeverityVulnerabilities() {
        // 准备测试数据（无高危漏洞）
        DependencySecurityService.SecurityScanResult result = DependencySecurityService.SecurityScanResult.builder()
            .projectName("Safe Project")
            .scanTime(LocalDateTime.now())
            .totalDependencies(5)
            .vulnerableDependencies(0)
            .vulnerabilities(List.of())
            .build();
        
        // 执行测试
        String summary = dependencySecurityService.generateSecuritySummary(result);
        
        // 验证结果
        assertNotNull(summary);
        assertTrue(summary.contains("✅ 未发现高危漏洞"));
        assertFalse(summary.contains("⚠️ 高危漏洞详情"));
    }

    @Test
    void testSendSecurityNotification_Enabled() {
        // 准备测试数据
        DependencySecurityService.SecurityScanResult result = DependencySecurityService.SecurityScanResult.builder()
            .projectName("Test Project")
            .scanTime(LocalDateTime.now())
            .totalDependencies(5)
            .vulnerableDependencies(1)
            .vulnerabilities(List.of())
            .build();
        
        // 执行测试
        dependencySecurityService.sendSecurityNotification(result);
        
        // 验证邮件发送
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendSecurityNotification_Disabled() {
        // 禁用通知
        ReflectionTestUtils.setField(dependencySecurityService, "notificationEnabled", false);
        
        // 准备测试数据
        DependencySecurityService.SecurityScanResult result = DependencySecurityService.SecurityScanResult.builder()
            .projectName("Test Project")
            .scanTime(LocalDateTime.now())
            .totalDependencies(5)
            .vulnerableDependencies(1)
            .vulnerabilities(List.of())
            .build();
        
        // 执行测试
        dependencySecurityService.sendSecurityNotification(result);
        
        // 验证邮件未发送
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testHasHighSeverityVulnerabilities_WithHighSeverity() {
        // 准备测试数据
        DependencySecurityService.SecurityScanResult result = DependencySecurityService.SecurityScanResult.builder()
            .vulnerabilities(List.of(
                DependencySecurityService.VulnerabilityInfo.builder()
                    .severity("HIGH")
                    .score(8.0)
                    .build()
            ))
            .build();
        
        // 执行测试
        boolean hasHigh = dependencySecurityService.hasHighSeverityVulnerabilities(result);
        
        // 验证结果
        assertTrue(hasHigh);
    }

    @Test
    void testHasHighSeverityVulnerabilities_WithHighScore() {
        // 准备测试数据
        DependencySecurityService.SecurityScanResult result = DependencySecurityService.SecurityScanResult.builder()
            .vulnerabilities(List.of(
                DependencySecurityService.VulnerabilityInfo.builder()
                    .severity("MEDIUM")
                    .score(7.5) // 超过阈值
                    .build()
            ))
            .build();
        
        // 执行测试
        boolean hasHigh = dependencySecurityService.hasHighSeverityVulnerabilities(result);
        
        // 验证结果
        assertTrue(hasHigh);
    }

    @Test
    void testHasHighSeverityVulnerabilities_NoHighSeverity() {
        // 准备测试数据
        DependencySecurityService.SecurityScanResult result = DependencySecurityService.SecurityScanResult.builder()
            .vulnerabilities(List.of(
                DependencySecurityService.VulnerabilityInfo.builder()
                    .severity("MEDIUM")
                    .score(5.0)
                    .build(),
                DependencySecurityService.VulnerabilityInfo.builder()
                    .severity("LOW")
                    .score(3.0)
                    .build()
            ))
            .build();
        
        // 执行测试
        boolean hasHigh = dependencySecurityService.hasHighSeverityVulnerabilities(result);
        
        // 验证结果
        assertFalse(hasHigh);
    }

    @Test
    void testSecurityScanResult_Builder() {
        // 测试 Builder 模式
        LocalDateTime now = LocalDateTime.now();
        List<DependencySecurityService.VulnerabilityInfo> vulns = List.of();
        
        DependencySecurityService.SecurityScanResult result = 
            DependencySecurityService.SecurityScanResult.builder()
                .projectName("Test")
                .scanTime(now)
                .totalDependencies(10)
                .vulnerableDependencies(2)
                .vulnerabilities(vulns)
                .build();
        
        assertEquals("Test", result.getProjectName());
        assertEquals(now, result.getScanTime());
        assertEquals(10, result.getTotalDependencies());
        assertEquals(2, result.getVulnerableDependencies());
        assertEquals(vulns, result.getVulnerabilities());
    }

    @Test
    void testVulnerabilityInfo_Builder() {
        // 测试 VulnerabilityInfo Builder 模式
        DependencySecurityService.VulnerabilityInfo vuln = 
            DependencySecurityService.VulnerabilityInfo.builder()
                .cve("CVE-2023-12345")
                .severity("HIGH")
                .score(8.5)
                .description("Test vulnerability")
                .dependencyName("test.jar")
                .build();
        
        assertEquals("CVE-2023-12345", vuln.getCve());
        assertEquals("HIGH", vuln.getSeverity());
        assertEquals(8.5, vuln.getScore());
        assertEquals("Test vulnerability", vuln.getDescription());
        assertEquals("test.jar", vuln.getDependencyName());
    }
}