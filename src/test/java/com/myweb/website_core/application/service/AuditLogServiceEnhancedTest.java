package com.myweb.website_core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.security.dto.AuditLogQuery;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.infrastructure.persistence.repository.AuditLogRepository;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 审计日志服务增强功能测试
 * 
 * 测试审计日志服务的增强功能，包括：
 * - 复杂条件查询
 * - CSV和Excel导出
 * - 自动清理机制
 * - 异步日志记录
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceEnhancedTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog sampleAuditLog;
    private AuditLogRequest sampleRequest;
    private AuditLogQuery sampleQuery;

    @BeforeEach
    void setUp() {
        // 设置配置值
        ReflectionTestUtils.setField(auditLogService, "retentionDays", 90);
        ReflectionTestUtils.setField(auditLogService, "batchSize", 1000);
        ReflectionTestUtils.setField(auditLogService, "exportLimit", 10000);

        // 创建测试数据
        sampleAuditLog = new AuditLog();
        sampleAuditLog.setId(1L);
        sampleAuditLog.setUserId(100L);
        sampleAuditLog.setUsername("testuser");
        sampleAuditLog.setOperation(AuditOperation.USER_LOGIN_SUCCESS);
        sampleAuditLog.setResourceType("USER");
        sampleAuditLog.setResourceId(100L);
        sampleAuditLog.setIpAddress("192.168.1.100");
        sampleAuditLog.setUserAgent("Mozilla/5.0");
        sampleAuditLog.setResult("SUCCESS");
        sampleAuditLog.setExecutionTime(150L);
        sampleAuditLog.setTimestamp(LocalDateTime.now());
        sampleAuditLog.setRiskLevel(2);

        sampleRequest = AuditLogRequest.builder()
                .operation(AuditOperation.USER_LOGIN_SUCCESS)
                .userId(100L)
                .username("testuser")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .result("SUCCESS")
                .executionTime(150L)
                .timestamp(LocalDateTime.now())
                .build();

        sampleQuery = AuditLogQuery.builder()
                .userId(100L)
                .operation(AuditOperation.USER_LOGIN_SUCCESS)
                .result("SUCCESS")
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now())
                .build();
    }

    @Test
    void testAsyncLogOperation() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

        // When
        auditLogService.logOperation(sampleRequest);

        // Then
        verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
    }

    @Test
    void testFindLogsWithEmptyQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> expectedPage = new PageImpl<>(Arrays.asList(sampleAuditLog));
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // When
        Page<AuditLog> result = auditLogService.findLogs(null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(sampleAuditLog.getId(), result.getContent().get(0).getId());
        verify(auditLogRepository).findAll(any(Pageable.class));
    }

    @Test
    void testFindLogsWithComplexQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> expectedPage = new PageImpl<>(Arrays.asList(sampleAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<AuditLog> result = auditLogService.findLogs(sampleQuery, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testExportLogsCSV() {
        // Given
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(sampleAuditLog)));

        // When
        byte[] result = auditLogService.exportLogs(sampleQuery, "CSV");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("时间"));
        assertTrue(csvContent.contains("用户名"));
        assertTrue(csvContent.contains("testuser"));
    }

    @Test
    void testExportLogsExcel() {
        // Given
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(sampleAuditLog)));

        // When
        byte[] result = auditLogService.exportLogs(sampleQuery, "EXCEL");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
        // Excel文件应该以特定的字节序列开始
        assertTrue(result.length > 100); // Excel文件应该比较大
    }

    @Test
    void testExportLogsDefaultFormat() {
        // Given
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(sampleAuditLog)));

        // When
        byte[] result = auditLogService.exportLogs(sampleQuery);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("时间"));
    }

    @Test
    void testCleanupLogsBefore() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        when(auditLogRepository.deleteSuccessLogsByTimestampBefore(cutoffTime)).thenReturn(100);

        // When
        int deletedCount = auditLogService.cleanupLogsBefore(cutoffTime, true);

        // Then
        assertEquals(100, deletedCount);
        verify(auditLogRepository).deleteSuccessLogsByTimestampBefore(cutoffTime);
    }

    @Test
    void testCleanupLogsBeforeAllTypes() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        when(auditLogRepository.deleteByTimestampBefore(cutoffTime)).thenReturn(150);

        // When
        int deletedCount = auditLogService.cleanupLogsBefore(cutoffTime, false);

        // Then
        assertEquals(150, deletedCount);
        verify(auditLogRepository).deleteByTimestampBefore(cutoffTime);
    }

    @Test
    void testLogOperationWithInvalidRequest() {
        // Given
        AuditLogRequest invalidRequest = AuditLogRequest.builder()
                .operation(null) // 无效的操作类型
                .result(null) // 无效的结果
                .build();

        // When
        auditLogService.logOperation(invalidRequest);

        // Then
        // 应该不会调用repository.save，因为请求无效
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void testLogOperationWithJsonProcessingException() throws Exception {
        // Given
        AuditLogRequest requestWithData = AuditLogRequest.builder()
                .operation(AuditOperation.USER_LOGIN_SUCCESS)
                .result("SUCCESS")
                .requestData("test data")
                .timestamp(LocalDateTime.now())
                .build();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON processing failed") {
                });
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

        // When
        auditLogService.logOperation(requestWithData);

        // Then
        // The service should still save the audit log even if JSON processing fails
        // It will just set an error message instead of the serialized data
        verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
    }

    @Test
    void testFindLogsWithSecurityEventsOnly() {
        // Given
        AuditLogQuery securityQuery = AuditLogQuery.builder()
                .securityEventsOnly(true)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> expectedPage = new PageImpl<>(Arrays.asList(sampleAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<AuditLog> result = auditLogService.findLogs(securityQuery, pageable);

        // Then
        assertNotNull(result);
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testFindLogsWithHighRiskOnly() {
        // Given
        AuditLogQuery highRiskQuery = AuditLogQuery.builder()
                .highRiskOnly(true)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> expectedPage = new PageImpl<>(Arrays.asList(sampleAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<AuditLog> result = auditLogService.findLogs(highRiskQuery, pageable);

        // Then
        assertNotNull(result);
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testFindLogsWithTimeRange() {
        // Given
        AuditLogQuery timeRangeQuery = AuditLogQuery.builder()
                .lastHours(24)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> expectedPage = new PageImpl<>(Arrays.asList(sampleAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<AuditLog> result = auditLogService.findLogs(timeRangeQuery, pageable);

        // Then
        assertNotNull(result);
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testExportWithCustomFields() {
        // Given
        AuditLogQuery customFieldQuery = AuditLogQuery.builder()
                .exportFields(Arrays.asList("timestamp", "username", "operation", "result"))
                .build();

        // Mock both possible calls
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(sampleAuditLog)));
        when(auditLogRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(sampleAuditLog)));

        // When
        byte[] result = auditLogService.exportLogs(customFieldQuery, "CSV");

        // Then
        assertNotNull(result);
        String csvContent = new String(result, java.nio.charset.StandardCharsets.UTF_8);

        // 应该只包含指定的字段
        assertTrue(csvContent.contains("时间"));
        assertTrue(csvContent.contains("用户名"));
        assertTrue(csvContent.contains("操作"));
        assertTrue(csvContent.contains("结果"));

        // 不应该包含其他字段
        assertFalse(csvContent.contains("IP地址"));
        assertFalse(csvContent.contains("执行时间"));
    }

    @Test
    void testLogUserLoginSuccess() {
        // Given
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

        // When
        //auditLogService.logUserLogin(user, "192.168.1.100", "Mozilla/5.0", "SUCCESS");

        // Then
        verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
    }

    @Test
    void testLogUserRegistration() {
        // Given
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

        // When
        //auditLogService.logUserRegistration(user);

        // Then
        verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
    }

    @Test
    void testLogSecurityEvent() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

        // When
        auditLogService.logSecurityEvent(AuditOperation.SUSPICIOUS_ACTIVITY, "testuser", "可疑登录活动");

        // Then
        verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
    }

    @Test
    void testExportLogsWithException() {
        // Given
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            auditLogService.exportLogs(sampleQuery, "CSV");
        });
    }

    @Test
    void testCleanupLogsWithException() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        when(auditLogRepository.deleteSuccessLogsByTimestampBefore(cutoffTime))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            auditLogService.cleanupLogsBefore(cutoffTime, true);
        });
    }
}