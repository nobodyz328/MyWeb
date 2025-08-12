package com.myweb.website_core.common.exception.handle;

import com.myweb.website_core.application.service.security.SecurityExceptionMaskingService;
import com.myweb.website_core.application.service.security.SecurityExceptionStatisticsService;
import com.myweb.website_core.common.exception.SecurityErrorResponse;
import com.myweb.website_core.common.exception.security.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * 安全异常处理器测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class SecurityExceptionHandlerTest {
    
    @Mock
    private SecurityExceptionMaskingService maskingService;
    
    @Mock
    private SecurityExceptionStatisticsService statisticsService;
    
    @Mock
    private WebRequest webRequest;
    
    private SecurityExceptionHandler securityExceptionHandler;
    
    @BeforeEach
    void setUp() {
        securityExceptionHandler = new SecurityExceptionHandler(maskingService, statisticsService);
    }
    
    @Test
    void testHandleValidationException() {
        // 准备测试数据
        ValidationException exception = new ValidationException("测试验证异常", "testField", "TEST_ERROR");
        
        // 模拟Web请求
        when(webRequest.getDescription(false)).thenReturn("uri=/test/path");
        
        // 模拟脱敏服务返回脱敏后的响应
        SecurityErrorResponse maskedResponse = SecurityErrorResponse.inputValidationError(
                "输入验证失败：测试验证异常",
                "验证失败 - 字段: testField - 错误代码: TEST_ERROR",
                "/test/path"
        );
        when(maskingService.maskByCurrentUserPermission(any(SecurityErrorResponse.class)))
                .thenReturn(maskedResponse);
        
        // 执行测试
        ResponseEntity<SecurityErrorResponse> response = securityExceptionHandler
                .handleValidationException(exception, webRequest);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INPUT_VALIDATION_ERROR", response.getBody().getErrorCode());
        assertEquals(SecurityErrorResponse.SecurityErrorCategory.INPUT_VALIDATION, 
                response.getBody().getCategory());
        
        // 验证服务调用
        verify(maskingService, times(1)).maskByCurrentUserPermission(any(SecurityErrorResponse.class));
        verify(statisticsService, times(1)).recordSecurityException(
                any(SecurityErrorResponse.class), eq(exception), anyString(), anyString());
    }
    
    @Test
    void testSecurityErrorResponseCreation() {
        // 测试输入验证错误响应创建
        SecurityErrorResponse response = SecurityErrorResponse.inputValidationError(
                "测试消息", "测试详情", "/test/path");
        
        assertNotNull(response);
        assertEquals("INPUT_VALIDATION_ERROR", response.getErrorCode());
        assertEquals("测试消息", response.getMessage());
        assertEquals("测试详情", response.getDetails());
        assertEquals(SecurityErrorResponse.SecurityErrorCategory.INPUT_VALIDATION, response.getCategory());
        assertEquals(SecurityErrorResponse.SecurityErrorSeverity.MEDIUM, response.getSeverity());
        assertEquals("/test/path", response.getPath());
        assertEquals(Integer.valueOf(400), response.getStatus());
        assertTrue(response.isRequiresUserAction());
        assertEquals("请检查输入数据的格式和内容", response.getSuggestedAction());
    }
    
    @Test
    void testSecurityErrorResponseSeverityLevels() {
        // 测试不同严重级别
        SecurityErrorResponse lowSeverity = SecurityErrorResponse.genericSecurityError(
                "TEST_LOW", "测试消息", 
                SecurityErrorResponse.SecurityErrorCategory.OTHER,
                SecurityErrorResponse.SecurityErrorSeverity.LOW,
                "/test", 200);
        
        SecurityErrorResponse criticalSeverity = SecurityErrorResponse.genericSecurityError(
                "TEST_CRITICAL", "测试消息",
                SecurityErrorResponse.SecurityErrorCategory.DATA_INTEGRITY,
                SecurityErrorResponse.SecurityErrorSeverity.CRITICAL,
                "/test", 500);
        
        assertFalse(lowSeverity.isHighSeverity());
        assertFalse(lowSeverity.requiresImmediateAttention());
        
        assertTrue(criticalSeverity.isHighSeverity());
        assertTrue(criticalSeverity.requiresImmediateAttention());
    }
    
    @Test
    void testSecurityErrorResponseBuilder() {
        // 测试构建器模式
        SecurityErrorResponse response = SecurityErrorResponse.builder()
                .errorCode("TEST_CODE")
                .message("测试消息")
                .category(SecurityErrorResponse.SecurityErrorCategory.AUTHENTICATION)
                .severity(SecurityErrorResponse.SecurityErrorSeverity.HIGH)
                .path("/test/path")
                .status(401)
                .build()
                .withRequestId("test-request-id")
                .withSuggestedAction("测试建议操作");
        
        assertNotNull(response);
        assertEquals("TEST_CODE", response.getErrorCode());
        assertEquals("测试消息", response.getMessage());
        assertEquals("test-request-id", response.getRequestId());
        assertEquals("测试建议操作", response.getSuggestedAction());
        assertTrue(response.isSecurityRelated());
    }
    
    @Test
    void testErrorResponseCategories() {
        // 测试所有错误分类
        SecurityErrorResponse.SecurityErrorCategory[] categories = 
                SecurityErrorResponse.SecurityErrorCategory.values();
        
        assertTrue(categories.length > 0);
        
        for (SecurityErrorResponse.SecurityErrorCategory category : categories) {
            assertNotNull(category.getDisplayName());
            assertFalse(category.getDisplayName().isEmpty());
        }
    }
    
    @Test
    void testErrorResponseSeverities() {
        // 测试所有严重级别
        SecurityErrorResponse.SecurityErrorSeverity[] severities = 
                SecurityErrorResponse.SecurityErrorSeverity.values();
        
        assertTrue(severities.length > 0);
        
        for (SecurityErrorResponse.SecurityErrorSeverity severity : severities) {
            assertNotNull(severity.getDisplayName());
            assertFalse(severity.getDisplayName().isEmpty());
            assertTrue(severity.getLevel() > 0);
        }
        
        // 验证级别排序
        assertTrue(SecurityErrorResponse.SecurityErrorSeverity.LOW.getLevel() < 
                  SecurityErrorResponse.SecurityErrorSeverity.MEDIUM.getLevel());
        assertTrue(SecurityErrorResponse.SecurityErrorSeverity.MEDIUM.getLevel() < 
                  SecurityErrorResponse.SecurityErrorSeverity.HIGH.getLevel());
        assertTrue(SecurityErrorResponse.SecurityErrorSeverity.HIGH.getLevel() < 
                  SecurityErrorResponse.SecurityErrorSeverity.CRITICAL.getLevel());
    }
}