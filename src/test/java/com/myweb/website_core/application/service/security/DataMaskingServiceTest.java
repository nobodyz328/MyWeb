package com.myweb.website_core.application.service.security;

import com.myweb.website_core.common.util.PermissionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 数据脱敏服务测试类
 * <p>
 * 测试DataMaskingService的各种脱敏功能，包括：
 * - 邮箱脱敏测试
 * - 手机号脱敏测试
 * - 权限级别脱敏测试
 * - 批量脱敏测试
 * - 配置管理测试
 * - 脱敏效果验证测试
 * <p>
 * 符合需求：7.1, 7.2, 7.3, 7.4, 7.5 - 数据脱敏服务测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class DataMaskingServiceTest {
    
    @InjectMocks
    private DataMaskingService dataMaskingService;
    
    @BeforeEach
    void setUp() {
        // 设置默认配置
        ReflectionTestUtils.setField(dataMaskingService, "dataMaskingEnabled", true);
        ReflectionTestUtils.setField(dataMaskingService, "emailMaskLevel", "partial");
        ReflectionTestUtils.setField(dataMaskingService, "phoneMaskLevel", "partial");
        ReflectionTestUtils.setField(dataMaskingService, "maskChar", "*");
        ReflectionTestUtils.setField(dataMaskingService, "adminFullAccess", true);
    }
    
    // ==================== 邮箱脱敏测试 ====================
    
    @Test
    void testMaskEmail_ValidEmail_ShouldMaskPartially() {
        // Given
        String email = "test@example.com";
        
        // When
        String maskedEmail = dataMaskingService.maskEmail(email);
        System.out.println("Masked Email: " + maskedEmail);
        // Then
        assertNotNull(maskedEmail);
        assertNotEquals(email, maskedEmail);
        assertTrue(maskedEmail.contains("@example.com"));
        assertTrue(maskedEmail.contains("*"));
        assertTrue(maskedEmail.startsWith("t"));
        assertTrue(maskedEmail.endsWith("@example.com"));
    }
    
    @Test
    void testMaskEmail_ShortUsername_ShouldMaskCorrectly() {
        // Given
        String email = "ab@test.com";
        
        // When
        String maskedEmail = dataMaskingService.maskEmail(email);
        
        // Then
        assertNotNull(maskedEmail);
        assertTrue(maskedEmail.contains("*"));
        assertTrue(maskedEmail.contains("@test.com"));
    }
    
    @Test
    void testMaskEmail_NullEmail_ShouldReturnNull() {
        // When
        String maskedEmail = dataMaskingService.maskEmail(null);
        
        // Then
        assertNull(maskedEmail);
    }
    
    @Test
    void testMaskEmail_EmptyEmail_ShouldReturnEmpty() {
        // When
        String maskedEmail = dataMaskingService.maskEmail("");
        
        // Then
        assertEquals("", maskedEmail);
    }
    
    @Test
    void testMaskEmail_InvalidEmail_ShouldReturnOriginal() {
        // Given
        String invalidEmail = "invalid-email";
        
        // When
        String maskedEmail = dataMaskingService.maskEmail(invalidEmail);
        
        // Then
        assertEquals(invalidEmail, maskedEmail);
    }
    
    @Test
    void testMaskEmail_DisabledMasking_ShouldReturnOriginal() {
        // Given
        ReflectionTestUtils.setField(dataMaskingService, "dataMaskingEnabled", false);
        String email = "test@example.com";
        
        // When
        String maskedEmail = dataMaskingService.maskEmail(email);
        
        // Then
        assertEquals(email, maskedEmail);
    }
    
    @Test
    void testMaskEmail_FullMaskLevel_ShouldMaskCompletely() {
        // Given
        ReflectionTestUtils.setField(dataMaskingService, "emailMaskLevel", "full");
        String email = "test@example.com";
        
        // When
        String maskedEmail = dataMaskingService.maskEmail(email);
        
        // Then
        assertEquals("***@***", maskedEmail);
    }
    
    @Test
    void testMaskEmail_NoneMaskLevel_ShouldReturnOriginal() {
        // Given
        ReflectionTestUtils.setField(dataMaskingService, "emailMaskLevel", "none");
        String email = "test@example.com";
        
        // When
        String maskedEmail = dataMaskingService.maskEmail(email);
        
        // Then
        assertEquals(email, maskedEmail);
    }
    
    // ==================== 手机号脱敏测试 ====================
    
    @Test
    void testMaskPhoneNumber_ValidPhone_ShouldMaskPartially() {
        // Given
        String phone = "13812345678";
        
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);
        
        // Then
        assertNotNull(maskedPhone);
        assertNotEquals(phone, maskedPhone);
        assertTrue(maskedPhone.startsWith("138"));
        assertTrue(maskedPhone.endsWith("5678"));
        assertTrue(maskedPhone.contains("****"));
        assertEquals("138****5678", maskedPhone);
    }
    
    @Test
    void testMaskPhoneNumber_NullPhone_ShouldReturnNull() {
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber(null);
        
        // Then
        assertNull(maskedPhone);
    }
    
    @Test
    void testMaskPhoneNumber_EmptyPhone_ShouldReturnEmpty() {
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber("");
        
        // Then
        assertEquals("", maskedPhone);
    }
    
    @Test
    void testMaskPhoneNumber_InvalidPhone_ShouldReturnOriginal() {
        // Given
        String invalidPhone = "12345";
        
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber(invalidPhone);
        
        // Then
        assertEquals(invalidPhone, maskedPhone);
    }
    
    @Test
    void testMaskPhoneNumber_DisabledMasking_ShouldReturnOriginal() {
        // Given
        ReflectionTestUtils.setField(dataMaskingService, "dataMaskingEnabled", false);
        String phone = "13812345678";
        
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);
        
        // Then
        assertEquals(phone, maskedPhone);
    }
    
    @Test
    void testMaskPhoneNumber_FullMaskLevel_ShouldMaskCompletely() {
        // Given
        ReflectionTestUtils.setField(dataMaskingService, "phoneMaskLevel", "full");
        String phone = "13812345678";
        
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);
        
        // Then
        assertEquals("***", maskedPhone);
    }
    
    @Test
    void testMaskPhoneNumber_NoneMaskLevel_ShouldReturnOriginal() {
        // Given
        ReflectionTestUtils.setField(dataMaskingService, "phoneMaskLevel", "none");
        String phone = "13812345678";
        
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);
        
        // Then
        assertEquals(phone, maskedPhone);
    }
    
    // ==================== 权限级别脱敏测试 ====================
    
    @Test
    void testMaskByPermission_HasFullAccess_ShouldReturnOriginal() {
        // Given
        String email = "test@example.com";
        
        // When
        String maskedEmail = dataMaskingService.maskByPermission(email, "email", true);
        
        // Then
        assertEquals(email, maskedEmail);
    }
    
    @Test
    void testMaskByPermission_NoFullAccess_ShouldMask() {
        // Given
        String email = "test@example.com";
        
        // When
        String maskedEmail = dataMaskingService.maskByPermission(email, "email", false);
        
        // Then
        assertNotEquals(email, maskedEmail);
        assertTrue(maskedEmail.contains("*"));
    }
    
    @Test
    void testMaskByPermission_UnknownDataType_ShouldReturnOriginal() {
        // Given
        String data = "some-data";
        
        // When
        String maskedData = dataMaskingService.maskByPermission(data, "unknown", false);
        
        // Then
        assertEquals(data, maskedData);
    }
    
    @Test
    void testMaskByCurrentUserPermission_SameUser_ShouldReturnOriginal() {
        // Given
        String email = "test@example.com";
        Long userId = 1L;
        
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUserId).thenReturn(userId);
            
            // When
            String maskedEmail = dataMaskingService.maskByCurrentUserPermission(email, "email", userId);
            
            // Then
            assertEquals(email, maskedEmail);
        }
    }
    
    @Test
    void testMaskByCurrentUserPermission_DifferentUser_ShouldMask() {
        // Given
        String email = "test@example.com";
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUserId).thenReturn(currentUserId);
            mockedPermissionUtils.when(PermissionUtils::isAdmin).thenReturn(false);
            
            // When
            String maskedEmail = dataMaskingService.maskByCurrentUserPermission(email, "email", targetUserId);
            
            // Then
            assertNotEquals(email, maskedEmail);
            assertTrue(maskedEmail.contains("*"));
        }
    }
    
    @Test
    void testMaskByCurrentUserPermission_AdminUser_ShouldReturnOriginal() {
        // Given
        String email = "test@example.com";
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUserId).thenReturn(currentUserId);
            mockedPermissionUtils.when(PermissionUtils::isAdmin).thenReturn(true);
            
            // When
            String maskedEmail = dataMaskingService.maskByCurrentUserPermission(email, "email", targetUserId);
            
            // Then
            assertEquals(email, maskedEmail);
        }
    }
    
    // ==================== 批量脱敏测试 ====================
    
    @Test
    void testMaskBatch_ValidData_ShouldMaskCorrectly() {
        // Given
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("email", "test@example.com");
        dataMap.put("phone", "13812345678");
        dataMap.put("username", "testuser");
        
        Map<String, String> dataTypeMap = new HashMap<>();
        dataTypeMap.put("email", "email");
        dataTypeMap.put("phone", "phone");
        dataTypeMap.put("username", "username");
        
        // When
        Map<String, String> maskedData = dataMaskingService.maskBatch(dataMap, dataTypeMap, false);
        
        // Then
        assertNotNull(maskedData);
        assertEquals(3, maskedData.size());
        
        // 验证邮箱脱敏
        assertNotEquals(dataMap.get("email"), maskedData.get("email"));
        assertTrue(maskedData.get("email").contains("*"));
        
        // 验证手机号脱敏
        assertNotEquals(dataMap.get("phone"), maskedData.get("phone"));
        assertTrue(maskedData.get("phone").contains("*"));
        
        // 验证用户名脱敏
        assertNotEquals(dataMap.get("username"), maskedData.get("username"));
        assertTrue(maskedData.get("username").contains("*"));
    }
    
    @Test
    void testMaskBatch_HasFullAccess_ShouldReturnOriginal() {
        // Given
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("email", "test@example.com");
        dataMap.put("phone", "13812345678");
        
        Map<String, String> dataTypeMap = new HashMap<>();
        dataTypeMap.put("email", "email");
        dataTypeMap.put("phone", "phone");
        
        // When
        Map<String, String> maskedData = dataMaskingService.maskBatch(dataMap, dataTypeMap, true);
        
        // Then
        assertNotNull(maskedData);
        assertEquals(dataMap.get("email"), maskedData.get("email"));
        assertEquals(dataMap.get("phone"), maskedData.get("phone"));
    }
    
    @Test
    void testMaskBatch_NullDataMap_ShouldReturnNull() {
        // Given
        Map<String, String> dataTypeMap = new HashMap<>();
        
        // When
        Map<String, String> maskedData = dataMaskingService.maskBatch(null, dataTypeMap, false);
        
        // Then
        assertNull(maskedData);
    }
    
    @Test
    void testMaskBatch_EmptyDataMap_ShouldReturnEmpty() {
        // Given
        Map<String, String> dataMap = new HashMap<>();
        Map<String, String> dataTypeMap = new HashMap<>();
        
        // When
        Map<String, String> maskedData = dataMaskingService.maskBatch(dataMap, dataTypeMap, false);
        
        // Then
        assertEquals(dataMap, maskedData);
    }
    
    // ==================== 配置管理测试 ====================
    
    @Test
    void testIsDataMaskingEnabled_ShouldReturnConfigValue() {
        // When
        boolean enabled = dataMaskingService.isDataMaskingEnabled();
        
        // Then
        assertTrue(enabled);
    }
    
    @Test
    void testGetEmailMaskLevel_ShouldReturnConfigValue() {
        // When
        String level = dataMaskingService.getEmailMaskLevel();
        
        // Then
        assertEquals("partial", level);
    }
    
    @Test
    void testGetPhoneMaskLevel_ShouldReturnConfigValue() {
        // When
        String level = dataMaskingService.getPhoneMaskLevel();
        
        // Then
        assertEquals("partial", level);
    }
    
    @Test
    void testGetMaskingConfig_ShouldReturnAllConfig() {
        // When
        Map<String, Object> config = dataMaskingService.getMaskingConfig();
        
        // Then
        assertNotNull(config);
        assertEquals(true, config.get("enabled"));
        assertEquals("partial", config.get("emailMaskLevel"));
        assertEquals("partial", config.get("phoneMaskLevel"));
        assertEquals("*", config.get("maskChar"));
        assertEquals(true, config.get("adminFullAccess"));
    }
    
    // ==================== 脱敏效果验证测试 ====================
    
    @Test
    void testValidateMaskingEffect_EmailMasking_ShouldReturnTrue() {
        // Given
        String originalEmail = "test@example.com";
        String maskedEmail = dataMaskingService.maskEmail(originalEmail);
        
        // When
        boolean isValid = dataMaskingService.validateMaskingEffect(originalEmail, maskedEmail, "email");
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateMaskingEffect_PhoneMasking_ShouldReturnTrue() {
        // Given
        String originalPhone = "13812345678";
        String maskedPhone = dataMaskingService.maskPhoneNumber(originalPhone);
        
        // When
        boolean isValid = dataMaskingService.validateMaskingEffect(originalPhone, maskedPhone, "phone");
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateMaskingEffect_NoMasking_ShouldReturnFalse() {
        // Given
        String originalData = "test@example.com";
        String maskedData = "test@example.com"; // 未脱敏
        
        // When
        boolean isValid = dataMaskingService.validateMaskingEffect(originalData, maskedData, "email");
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testValidateMaskingEffect_DisabledMasking_ShouldReturnTrue() {
        // Given
        ReflectionTestUtils.setField(dataMaskingService, "dataMaskingEnabled", false);
        String originalData = "test@example.com";
        String maskedData = "test@example.com";
        
        // When
        boolean isValid = dataMaskingService.validateMaskingEffect(originalData, maskedData, "email");
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateMaskingEffect_NullData_ShouldReturnFalse() {
        // When
        boolean isValid = dataMaskingService.validateMaskingEffect(null, "masked", "email");
        
        // Then
        assertFalse(isValid);
    }
    
    // ==================== 统计信息测试 ====================
    
    @Test
    void testGetMaskingStatistics_ShouldReturnStatistics() {
        // When
        Map<String, Object> stats = dataMaskingService.getMaskingStatistics();
        
        // Then
        assertNotNull(stats);
        assertEquals(true, stats.get("enabled"));
        assertArrayEquals(new String[]{"email", "phone", "username", "idcard"}, 
                         (String[]) stats.get("supportedTypes"));
        assertArrayEquals(new String[]{"none", "partial", "full"}, 
                         (String[]) stats.get("maskLevels"));
        assertEquals("partial", stats.get("currentEmailLevel"));
        assertEquals("partial", stats.get("currentPhoneLevel"));
    }
    
    // ==================== 边界条件测试 ====================
    
    @Test
    void testMaskEmail_VeryLongEmail_ShouldHandleCorrectly() {
        // Given
        String longEmail = "verylongusernamethatexceedsnormallength@verylongdomainnamethatexceedsnormallength.com";
        
        // When
        String maskedEmail = dataMaskingService.maskEmail(longEmail);
        
        // Then
        assertNotNull(maskedEmail);
        assertNotEquals(longEmail, maskedEmail);
        assertTrue(maskedEmail.contains("*"));
        assertTrue(maskedEmail.contains("@"));
    }
    
    @Test
    void testMaskPhoneNumber_InvalidLength_ShouldReturnOriginal() {
        // Given
        String invalidPhone = "1381234567890"; // 13位
        
        // When
        String maskedPhone = dataMaskingService.maskPhoneNumber(invalidPhone);
        
        // Then
        assertEquals(invalidPhone, maskedPhone);
    }
    
    @Test
    void testMaskByPermission_ExceptionInPermissionCheck_ShouldMask() {
        // Given
        String email = "test@example.com";
        Long targetUserId = 1L;
        
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUserId)
                               .thenThrow(new RuntimeException("Permission check failed"));
            
            // When
            String maskedEmail = dataMaskingService.maskByCurrentUserPermission(email, "email", targetUserId);
            
            // Then
            assertNotEquals(email, maskedEmail);
            assertTrue(maskedEmail.contains("*"));
        }
    }
}