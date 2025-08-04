package com.myweb.website_core.domain.entity;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志实体测试类
 * 
 * 测试AuditLog实体的基本功能，包括：
 * - 实体创建和属性设置
 * - 业务方法的正确性
 * - JPA生命周期回调
 * - 标签管理功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
class AuditLogTest {
    
    private AuditLog auditLog;
    
    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }
    
    @Test
    void testBasicProperties() {
        // 测试基本属性设置
        Long userId = 1L;
        String username = "testuser";
        AuditOperation operation = AuditOperation.USER_LOGIN_SUCCESS;
        String ipAddress = "192.168.1.1";
        String result = "SUCCESS";
        LocalDateTime timestamp = LocalDateTime.now();
        
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setOperation(operation);
        auditLog.setIpAddress(ipAddress);
        auditLog.setResult(result);
        auditLog.setTimestamp(timestamp);
        
        assertEquals(userId, auditLog.getUserId());
        assertEquals(username, auditLog.getUsername());
        assertEquals(operation, auditLog.getOperation());
        assertEquals(ipAddress, auditLog.getIpAddress());
        assertEquals(result, auditLog.getResult());
        assertEquals(timestamp, auditLog.getTimestamp());
    }
    
    @Test
    void testResultStatusMethods() {
        // 测试成功状态
        auditLog.setResult("SUCCESS");
        assertTrue(auditLog.isSuccess());
        assertFalse(auditLog.isFailure());
        assertFalse(auditLog.isError());
        
        // 测试失败状态
        auditLog.setResult("FAILURE");
        assertFalse(auditLog.isSuccess());
        assertTrue(auditLog.isFailure());
        assertFalse(auditLog.isError());
        
        // 测试错误状态
        auditLog.setResult("ERROR");
        assertFalse(auditLog.isSuccess());
        assertFalse(auditLog.isFailure());
        assertTrue(auditLog.isError());
    }
    
    @Test
    void testRiskLevelMethods() {
        // 测试低风险
        auditLog.setRiskLevel(2);
        assertFalse(auditLog.isHighRisk());
        
        // 测试高风险
        auditLog.setRiskLevel(4);
        assertTrue(auditLog.isHighRisk());
        
        // 测试最高风险
        auditLog.setRiskLevel(5);
        assertTrue(auditLog.isHighRisk());
        
        // 测试null风险级别
        auditLog.setRiskLevel(null);
        assertFalse(auditLog.isHighRisk());
    }
    
    @Test
    void testOperationTypeMethods() {
        // 测试安全相关操作
        auditLog.setOperation(AuditOperation.USER_LOGIN_FAILURE);
        assertTrue(auditLog.isSecurityOperation());
        
        // 测试管理员操作
        auditLog.setOperation(AuditOperation.ADMIN_LOGIN);
        assertTrue(auditLog.isAdminOperation());
        
        // 测试普通操作
        auditLog.setOperation(AuditOperation.POST_CREATE);
        assertFalse(auditLog.isSecurityOperation());
        assertFalse(auditLog.isAdminOperation());
    }
    
    @Test
    void testTagManagement() {
        // 测试添加标签
        auditLog.addTag("security");
        assertEquals("security", auditLog.getTags());
        assertTrue(auditLog.hasTag("security"));
        
        // 测试添加多个标签
        auditLog.addTag("login");
        assertEquals("security,login", auditLog.getTags());
        assertTrue(auditLog.hasTag("security"));
        assertTrue(auditLog.hasTag("login"));
        
        // 测试添加重复标签
        auditLog.addTag("security");
        assertEquals("security,login", auditLog.getTags());
        
        // 测试移除标签
        auditLog.removeTag("security");
        assertEquals("login", auditLog.getTags());
        assertFalse(auditLog.hasTag("security"));
        assertTrue(auditLog.hasTag("login"));
        
        // 测试添加空标签
        auditLog.addTag("");
        auditLog.addTag(null);
        assertEquals("login", auditLog.getTags());
    }
    
    @Test
    void testProcessingMethods() {
        // 初始状态
        assertFalse(auditLog.getProcessed());
        assertNull(auditLog.getProcessedAt());
        assertNull(auditLog.getProcessedBy());
        assertNull(auditLog.getProcessNotes());
        
        // 标记为已处理
        String processedBy = "admin";
        String processNotes = "已处理安全事件";
        auditLog.markAsProcessed(processedBy, processNotes);
        
        assertTrue(auditLog.getProcessed());
        assertNotNull(auditLog.getProcessedAt());
        assertEquals(processedBy, auditLog.getProcessedBy());
        assertEquals(processNotes, auditLog.getProcessNotes());
        
        // 标记为未处理
        auditLog.markAsUnprocessed();
        assertFalse(auditLog.getProcessed());
        assertNull(auditLog.getProcessedAt());
        assertNull(auditLog.getProcessedBy());
        assertNull(auditLog.getProcessNotes());
    }
    
    @Test
    void testDisplayMethods() {
        // 测试操作显示名称
        auditLog.setOperation(AuditOperation.USER_LOGIN_SUCCESS);
        assertEquals("用户登录成功", auditLog.getOperationDisplayName());
        
        // 测试null操作
        auditLog.setOperation(null);
        assertEquals("未知操作", auditLog.getOperationDisplayName());
        
        // 测试风险级别显示
        auditLog.setRiskLevel(1);
        assertEquals("最低", auditLog.getRiskLevelDisplay());
        
        auditLog.setRiskLevel(3);
        assertEquals("中", auditLog.getRiskLevelDisplay());
        
        auditLog.setRiskLevel(5);
        assertEquals("最高", auditLog.getRiskLevelDisplay());
        
        auditLog.setRiskLevel(null);
        assertEquals("未知", auditLog.getRiskLevelDisplay());
        
        // 测试执行时间显示
        auditLog.setExecutionTime(500L);
        assertEquals("500ms", auditLog.getExecutionTimeDisplay());
        
        auditLog.setExecutionTime(2500L);
        assertEquals("2.50s", auditLog.getExecutionTimeDisplay());
        
        auditLog.setExecutionTime(null);
        assertEquals("未知", auditLog.getExecutionTimeDisplay());
    }
    
    @Test
    void testPrePersistCallback() {
        // 设置操作类型
        auditLog.setOperation(AuditOperation.ADMIN_LOGIN);
        
        // 使用反射调用protected方法
        try {
            java.lang.reflect.Method onCreateMethod = AuditLog.class.getDeclaredMethod("onCreate");
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(auditLog);
        } catch (Exception e) {
            fail("Failed to invoke onCreate method: " + e.getMessage());
        }
        
        // 验证时间戳被设置
        assertNotNull(auditLog.getTimestamp());
        
        // 验证风险级别被自动设置
        assertEquals(5, auditLog.getRiskLevel());
        
        // 验证默认结果状态
        assertEquals("SUCCESS", auditLog.getResult());
    }
    
    @Test
    void testPrePersistCallbackWithExistingValues() {
        // 设置已有值
        LocalDateTime existingTimestamp = LocalDateTime.now().minusHours(1);
        auditLog.setTimestamp(existingTimestamp);
        auditLog.setRiskLevel(3);
        auditLog.setResult("FAILURE");
        auditLog.setOperation(AuditOperation.USER_LOGIN_SUCCESS);
        
        // 使用反射调用protected方法
        try {
            java.lang.reflect.Method onCreateMethod = AuditLog.class.getDeclaredMethod("onCreate");
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(auditLog);
        } catch (Exception e) {
            fail("Failed to invoke onCreate method: " + e.getMessage());
        }
        
        // 验证已有值不被覆盖
        assertEquals(existingTimestamp, auditLog.getTimestamp());
        assertEquals(3, auditLog.getRiskLevel());
        assertEquals("FAILURE", auditLog.getResult());
    }
    
    @Test
    void testToString() {
        auditLog.setId(1L);
        auditLog.setOperation(AuditOperation.USER_LOGIN_SUCCESS);
        auditLog.setUsername("testuser");
        auditLog.setResult("SUCCESS");
        auditLog.setTimestamp(LocalDateTime.of(2025, 1, 1, 12, 0, 0));
        
        String toString = auditLog.toString();
        
        // 验证toString包含关键信息
        assertNotNull(toString);
        assertTrue(toString.contains("AuditLog"));
        assertTrue(toString.contains("1"));
        assertTrue(toString.contains("USER_LOGIN_SUCCESS"));
        assertTrue(toString.contains("testuser"));
        assertTrue(toString.contains("SUCCESS"));
    }
    
    @Test
    void testFormattedTimestamp() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 12, 30, 45);
        auditLog.setTimestamp(timestamp);
        
        assertEquals("2025-01-01 12:30:45", auditLog.getFormattedTimestamp());
        
        // 测试null时间戳
        auditLog.setTimestamp(null);
        assertEquals("", auditLog.getFormattedTimestamp());
    }
    
    @Test
    void testSecurityFields() {
        // 测试安全相关字段
        String location = "北京市 海淀区";
        String deviceFingerprint = "abc123def456";
        String sessionId = "session-123";
        String requestId = "req-456";
        
        auditLog.setLocation(location);
        //auditLog.setDeviceFingerprint(deviceFingerprint);
        auditLog.setSessionId(sessionId);
        auditLog.setRequestId(requestId);
        
        assertEquals(location, auditLog.getLocation());
        //assertEquals(deviceFingerprint, auditLog.getDeviceFingerprint());
        assertEquals(sessionId, auditLog.getSessionId());
        assertEquals(requestId, auditLog.getRequestId());
    }
    
    @Test
    void testResourceFields() {
        // 测试资源相关字段
        String resourceType = "POST";
        Long resourceId = 123L;
        String description = "用户创建了新帖子";
        
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setDescription(description);
        
        assertEquals(resourceType, auditLog.getResourceType());
        assertEquals(resourceId, auditLog.getResourceId());
        assertEquals(description, auditLog.getDescription());
    }
    
    @Test
    void testDataFields() {
        // 测试数据相关字段
        String requestData = "{\"title\":\"测试帖子\"}";
        String responseData = "{\"id\":123,\"status\":\"success\"}";
        String errorMessage = "操作失败：权限不足";
        
        auditLog.setRequestData(requestData);
        auditLog.setResponseData(responseData);
        auditLog.setErrorMessage(errorMessage);
        
        assertEquals(requestData, auditLog.getRequestData());
        assertEquals(responseData, auditLog.getResponseData());
        assertEquals(errorMessage, auditLog.getErrorMessage());
    }
}