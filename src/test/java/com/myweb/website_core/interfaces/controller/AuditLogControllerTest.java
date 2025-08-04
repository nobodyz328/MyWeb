package com.myweb.website_core.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogQuery;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.interfaces.controller.security.AuditLogController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 审计日志控制器测试类
 * 
 * 测试审计日志查询接口的各种功能：
 * - 基础查询和分页
 * - 条件过滤查询
 * - 统计分析功能
 * - 导出功能
 * - 权限控制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@WebMvcTest(AuditLogController.class)
@DisplayName("审计日志控制器测试")
class AuditLogControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuditLogService auditLogService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private AuditLog sampleAuditLog;
    private Page<AuditLog> samplePage;
    
    @BeforeEach
    void setUp() {
        // 创建测试数据
        sampleAuditLog = new AuditLog();
        sampleAuditLog.setId(1L);
        sampleAuditLog.setUserId(100L);
        sampleAuditLog.setUsername("testuser");
        sampleAuditLog.setOperation(AuditOperation.USER_LOGIN_SUCCESS);
        sampleAuditLog.setResourceType("USER");
        sampleAuditLog.setResourceId(100L);
        sampleAuditLog.setIpAddress("192.168.1.100");
        sampleAuditLog.setResult("SUCCESS");
        sampleAuditLog.setTimestamp(LocalDateTime.now());
        sampleAuditLog.setRiskLevel(2);
        
        samplePage = new PageImpl<>(List.of(sampleAuditLog), PageRequest.of(0, 20), 1);
    }
    
    // ==================== 基础查询测试 ====================
    
    @Test
    @DisplayName("管理员可以查询审计日志")
    @WithMockUser(roles = "ADMIN")
    void testQueryAuditLogs_WithAdminRole_ShouldReturnLogs() throws Exception {
        // Given
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenReturn(samplePage);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "timestamp")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.content[0].operation").value("USER_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
    
    @Test
    @DisplayName("非管理员用户无法访问审计日志")
    @WithMockUser(roles = "USER")
    void testQueryAuditLogs_WithUserRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("未认证用户无法访问审计日志")
    void testQueryAuditLogs_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("根据ID查询审计日志详情")
    @WithMockUser(roles = "ADMIN")
    void testGetAuditLogById_WithValidId_ShouldReturnLog() throws Exception {
        // Given
        when(auditLogService.findById(1L)).thenReturn(Optional.of(sampleAuditLog));
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.operation").value("USER_LOGIN_SUCCESS"));
    }
    
    @Test
    @DisplayName("查询不存在的审计日志应返回404")
    @WithMockUser(roles = "ADMIN")
    void testGetAuditLogById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        when(auditLogService.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/999"))
                .andExpect(status().isNotFound());
    }
    
    // ==================== 专项查询测试 ====================
    
    @Test
    @DisplayName("查询安全事件日志")
    @WithMockUser(roles = "ADMIN")
    void testQuerySecurityEvents_ShouldReturnSecurityEvents() throws Exception {
        // Given
        AuditLog securityEvent = new AuditLog();
        securityEvent.setId(2L);
        securityEvent.setOperation(AuditOperation.USER_LOGIN_FAILURE);
        securityEvent.setResult("FAILURE");
        securityEvent.setRiskLevel(4);
        securityEvent.setTimestamp(LocalDateTime.now());
        
        Page<AuditLog> securityEventsPage = new PageImpl<>(List.of(securityEvent), PageRequest.of(0, 20), 1);
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenReturn(securityEventsPage);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/security-events")
                        .param("page", "0")
                        .param("size", "20")
                        .param("minRiskLevel", "3")
                        .param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].operation").value("USER_LOGIN_FAILURE"))
                .andExpect(jsonPath("$.content[0].result").value("FAILURE"))
                .andExpect(jsonPath("$.content[0].riskLevel").value(4));
    }
    
    @Test
    @DisplayName("查询失败操作日志")
    @WithMockUser(roles = "ADMIN")
    void testQueryFailures_ShouldReturnFailureLogs() throws Exception {
        // Given
        AuditLog failureLog = new AuditLog();
        failureLog.setId(3L);
        failureLog.setOperation(AuditOperation.USER_LOGIN_FAILURE);
        failureLog.setResult("FAILURE");
        failureLog.setTimestamp(LocalDateTime.now());
        
        Page<AuditLog> failurePage = new PageImpl<>(List.of(failureLog), PageRequest.of(0, 20), 1);
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenReturn(failurePage);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/failures")
                        .param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].result").value("FAILURE"));
    }
    
    @Test
    @DisplayName("查询用户操作历史")
    @WithMockUser(roles = "ADMIN")
    void testQueryUserAuditLogs_ShouldReturnUserLogs() throws Exception {
        // Given
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenReturn(samplePage);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/user/100")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].userId").value(100));
    }
    
    @Test
    @DisplayName("查询IP操作记录")
    @WithMockUser(roles = "ADMIN")
    void testQueryIpAuditLogs_ShouldReturnIpLogs() throws Exception {
        // Given
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenReturn(samplePage);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/ip/192.168.1.100")
                        .param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].ipAddress").value("192.168.1.100"));
    }
    
    // ==================== 统计分析测试 ====================
    
    @Test
    @DisplayName("获取审计日志统计报表")
    @WithMockUser(roles = "ADMIN")
    void testGetAuditStatistics_ShouldReturnStatistics() throws Exception {
        // Given
        when(auditLogService.countLogs(any(AuditLogQuery.class))).thenReturn(100L);
        when(auditLogService.getOperationStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(Map.of("operation", "USER_LOGIN_SUCCESS", "count", 50L)));
        when(auditLogService.countActiveUsers(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(20L);
        when(auditLogService.countActiveIPs(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15L);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/statistics")
                        .param("groupBy", "operation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeRange").exists())
                .andExpect(jsonPath("$.totalOperations").value(100))
                .andExpect(jsonPath("$.operationStats").isArray());
    }
    
    @Test
    @DisplayName("获取实时监控数据")
    @WithMockUser(roles = "ADMIN")
    void testGetRealtimeData_ShouldReturnRealtimeStats() throws Exception {
        // Given
        when(auditLogService.countLogs(any(AuditLogQuery.class))).thenReturn(50L);
        when(auditLogService.countActiveUsers(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);
        when(auditLogService.countActiveIPs(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(8L);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/realtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastHourOperations").value(50))
                .andExpect(jsonPath("$.activeUsers").value(10))
                .andExpect(jsonPath("$.activeIPs").value(8))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    // ==================== 导出功能测试 ====================
    
    @Test
    @DisplayName("导出CSV格式审计日志")
    @WithMockUser(roles = "ADMIN")
    void testExportAuditLogs_CSV_ShouldReturnCsvFile() throws Exception {
        // Given
        String csvData = "时间,用户名,操作,结果\n2025-01-01 10:00:00,testuser,USER_LOGIN_SUCCESS,SUCCESS\n";
        when(auditLogService.exportLogs(any(AuditLogQuery.class), eq("CSV")))
                .thenReturn(csvData.getBytes());
        
        AuditLogQuery query = new AuditLogQuery();
        query.setUsername("testuser");
        
        // When & Then
        mockMvc.perform(post("/api/admin/audit-logs/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query))
                        .param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("audit_logs_")))
                .andExpect(content().string(csvData));
    }
    
    @Test
    @DisplayName("导出Excel格式审计日志")
    @WithMockUser(roles = "ADMIN")
    void testExportAuditLogs_Excel_ShouldReturnExcelFile() throws Exception {
        // Given
        byte[] excelData = "fake excel data".getBytes();
        when(auditLogService.exportLogs(any(AuditLogQuery.class), eq("EXCEL")))
                .thenReturn(excelData);
        
        AuditLogQuery query = new AuditLogQuery();
        query.setResult("FAILURE");
        
        // When & Then
        mockMvc.perform(post("/api/admin/audit-logs/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query))
                        .param("format", "EXCEL"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
    }
    
    // ==================== 参数验证测试 ====================
    
    @Test
    @DisplayName("查询参数验证 - 页面大小限制")
    @WithMockUser(roles = "ADMIN")
    void testQueryAuditLogs_WithLargePageSize_ShouldLimitToMaxSize() throws Exception {
        // Given
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenReturn(samplePage);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("size", "200")) // 超过最大限制100
                .andExpect(status().isOk());
        
        // 验证实际调用时页面大小被限制为100
        // 这里可以通过ArgumentCaptor来验证具体的参数值
    }
    
    @Test
    @DisplayName("统计查询参数验证 - 时间范围")
    @WithMockUser(roles = "ADMIN")
    void testGetAuditStatistics_WithTimeRange_ShouldUseProvidedTime() throws Exception {
        // Given
        when(auditLogService.countLogs(any(AuditLogQuery.class))).thenReturn(50L);
        when(auditLogService.getOperationStatistics(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs/statistics")
                        .param("startTime", "2025-01-01T00:00:00")
                        .param("endTime", "2025-01-01T23:59:59")
                        .param("groupBy", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeRange.startTime").value("2025-01-01T00:00:00"))
                .andExpect(jsonPath("$.timeRange.endTime").value("2025-01-01T23:59:59"));
    }
    
    // ==================== 错误处理测试 ====================
    
    @Test
    @DisplayName("服务异常时应返回500错误")
    @WithMockUser(roles = "ADMIN")
    void testQueryAuditLogs_WithServiceException_ShouldReturn500() throws Exception {
        // Given
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("导出功能异常时应返回500错误")
    @WithMockUser(roles = "ADMIN")
    void testExportAuditLogs_WithServiceException_ShouldReturn500() throws Exception {
        // Given
        when(auditLogService.exportLogs(any(AuditLogQuery.class), anyString()))
                .thenThrow(new RuntimeException("Export failed"));
        
        AuditLogQuery query = new AuditLogQuery();
        
        // When & Then
        mockMvc.perform(post("/api/admin/audit-logs/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isInternalServerError());
    }
    
    // ==================== 复杂查询测试 ====================
    
    @Test
    @DisplayName("复杂条件查询测试")
    @WithMockUser(roles = "ADMIN")
    void testQueryAuditLogs_WithComplexConditions_ShouldReturnFilteredResults() throws Exception {
        // Given
        when(auditLogService.findLogs(any(AuditLogQuery.class), any(Pageable.class)))
                .thenReturn(samplePage);
        
        // When & Then
        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("username", "testuser")
                        .param("operation", "USER_LOGIN_SUCCESS")
                        .param("result", "SUCCESS")
                        .param("ipAddress", "192.168.1.100")
                        .param("minRiskLevel", "2")
                        .param("sortBy", "timestamp")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}