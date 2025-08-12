package com.myweb.website_core.application.service.security;

import com.myweb.website_core.common.exception.SecurityErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全异常脱敏服务测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class SecurityExceptionMaskingServiceTest {
    
    private SecurityExceptionMaskingService maskingService;
    
    @BeforeEach
    void setUp() {
        maskingService = new SecurityExceptionMaskingService();
        
        // 设置测试配置
        ReflectionTestUtils.setField(maskingService, "maskingEnabled", true);
        ReflectionTestUtils.setField(maskingService, "hideTechnicalDetails", true);
        ReflectionTestUtils.setField(maskingService, "adminFullAccess", true);
        ReflectionTestUtils.setField(maskingService, "maskChar", "*");
    }
    
    @Test
    void testMaskEmail() {
        // 测试邮箱脱敏
        String email = "test@example.com";
        String maskedEmail = maskingService.maskMessage(email, false);
        
        assertNotNull(maskedEmail);
        assertNotEquals(email, maskedEmail);
        assertTrue(maskedEmail.contains("*"));
        assertTrue(maskedEmail.contains("@example.com"));
    }
    
    @Test
    void testMaskPhoneNumber() {
        // 测试手机号脱敏
        String phone = "13812345678";
        String maskedPhone = maskingService.maskMessage(phone, false);
        
        assertNotNull(maskedPhone);
        assertNotEquals(phone, maskedPhone);
        assertTrue(maskedPhone.contains("*"));
        assertTrue(maskedPhone.startsWith("138"));
        assertTrue(maskedPhone.endsWith("5678"));
    }
    
    @Test
    void testMaskMessageForAdmin() {
        // 测试管理员用户的消息脱敏（应该保留更多信息）
        String message = "用户 test@example.com 登录失败，IP: 192.168.1.100";
        String adminMasked = maskingService.maskMessage(message, true);
        String userMasked = maskingService.maskMessage(message, false);
        
        assertNotNull(adminMasked);
        assertNotNull(userMasked);
        
        // 管理员应该看到更多信息
        assertTrue(adminMasked.length() >= userMasked.length());
    }
    
    @Test
    void testMaskSensitiveInfo() {
        // 测试敏感信息脱敏
        String message = "密码错误: password=123456, 邮箱: user@test.com, 手机: 13800138000";
        String masked = maskingService.maskMessage(message, false);
        
        assertNotNull(masked);
        assertFalse(masked.contains("123456"));
        assertFalse(masked.contains("user@test.com"));
        assertFalse(masked.contains("13800138000"));
        assertTrue(masked.contains("*"));
    }
    
    @Test
    void testMaskErrorData() {
        // 测试错误数据脱敏
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("email", "test@example.com");
        errorData.put("password", "secret123");
        errorData.put("username", "testuser");
        errorData.put("normalField", "normalValue");
        
        Map<String, Object> maskedData = maskingService.maskErrorData(errorData, false);
        
        assertNotNull(maskedData);
        assertEquals(4, maskedData.size());
        
        // 敏感字段应该被脱敏
        assertNotEquals("test@example.com", maskedData.get("email"));
        assertNotEquals("secret123", maskedData.get("password"));
        
        // 非敏感字段应该保持原样或进行内容脱敏
        assertNotNull(maskedData.get("normalField"));
    }
    
    @Test
    void testMaskSecurityErrorResponse() {
        // 测试安全错误响应脱敏
        SecurityErrorResponse originalResponse = SecurityErrorResponse.builder()
                .errorCode("TEST_ERROR")
                .message("用户 test@example.com 验证失败")
                .details("详细错误信息包含敏感数据: password=secret123")
                .category(SecurityErrorResponse.SecurityErrorCategory.INPUT_VALIDATION)
                .severity(SecurityErrorResponse.SecurityErrorSeverity.MEDIUM)
                .path("/api/test?token=abc123")
                .status(400)
                .build();
        
        SecurityErrorResponse maskedResponse = maskingService.maskSecurityErrorResponse(originalResponse, false);
        
        assertNotNull(maskedResponse);
        assertEquals(originalResponse.getErrorCode(), maskedResponse.getErrorCode());
        assertEquals(originalResponse.getCategory(), maskedResponse.getCategory());
        assertEquals(originalResponse.getSeverity(), maskedResponse.getSeverity());
        assertEquals(originalResponse.getStatus(), maskedResponse.getStatus());
        
        // 消息和详情应该被脱敏
        assertNotEquals(originalResponse.getMessage(), maskedResponse.getMessage());
        assertNotEquals(originalResponse.getDetails(), maskedResponse.getDetails());
        
        // 路径中的敏感参数应该被脱敏
        assertNotEquals(originalResponse.getPath(), maskedResponse.getPath());
        assertFalse(maskedResponse.getPath().contains("abc123"));
    }
    
    @Test
    void testMaskStackTrace() {
        // 测试堆栈信息脱敏
        String stackTrace = "java.lang.Exception: 错误信息\n" +
                "    at com.example.Service.method(Service.java:123)\n" +
                "    at com.example.Controller.handle(C:\\path\\to\\Controller.java:456)\n" +
                "    密码: password=secret123";
        
        String maskedStackTrace = maskingService.maskStackTrace(stackTrace, false);
        
        assertNotNull(maskedStackTrace);
        // 普通用户应该看到隐藏的堆栈信息
        assertEquals("堆栈信息已隐藏", maskedStackTrace);
        
        // 管理员应该看到脱敏后的堆栈信息
        String adminMaskedStackTrace = maskingService.maskStackTrace(stackTrace, true);
        assertNotNull(adminMaskedStackTrace);
        assertNotEquals("堆栈信息已隐藏", adminMaskedStackTrace);
        assertFalse(adminMaskedStackTrace.contains("secret123"));
    }
    
    @Test
    void testValidateMaskingEffect() {
        // 测试脱敏效果验证
        String originalEmail = "test@example.com";
        String maskedEmail = maskingService.maskMessage(originalEmail, false);
        
        // 验证脱敏后的文本确实被脱敏了
        assertNotEquals(originalEmail, maskedEmail);
        assertTrue(maskedEmail.contains("*"));
        
        // 验证脱敏效果 - 脱敏后的文本应该通过验证
        boolean isValid = maskingService.validateMaskingEffect(originalEmail, maskedEmail);
        assertTrue(isValid, "脱敏验证应该成功，因为脱敏功能已启用");
        
        // 测试未脱敏的情况 - 原始文本包含敏感信息，验证应该失败
        boolean isInvalid = maskingService.validateMaskingEffect(originalEmail, originalEmail);
        assertFalse(isInvalid, "原始文本验证应该失败，因为包含敏感信息");
    }
    
    @Test
    void testMaskingConfig() {
        // 测试脱敏配置获取
        Map<String, Object> config = maskingService.getMaskingConfig();
        
        assertNotNull(config);
        assertTrue(config.containsKey("enabled"));
        assertTrue(config.containsKey("hideTechnicalDetails"));
        assertTrue(config.containsKey("adminFullAccess"));
        assertTrue(config.containsKey("maskChar"));
        
        assertEquals(true, config.get("enabled"));
        assertEquals(true, config.get("hideTechnicalDetails"));
        assertEquals(true, config.get("adminFullAccess"));
        assertEquals("*", config.get("maskChar"));
    }
    
    @Test
    void testMaskingDisabled() {
        // 测试脱敏功能禁用时的行为
        ReflectionTestUtils.setField(maskingService, "maskingEnabled", false);
        
        String originalMessage = "test@example.com password=secret123";
        SecurityErrorResponse originalResponse = SecurityErrorResponse.builder()
                .message(originalMessage)
                .build();
        
        SecurityErrorResponse maskedResponse = maskingService.maskSecurityErrorResponse(originalResponse, false);
        
        // 脱敏功能禁用时，应该返回原始信息
        assertEquals(originalMessage, maskedResponse.getMessage());
    }
}