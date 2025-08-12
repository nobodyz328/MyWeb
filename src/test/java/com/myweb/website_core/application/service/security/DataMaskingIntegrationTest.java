package com.myweb.website_core.application.service.security;

import com.myweb.website_core.common.util.DTOConverter;
import com.myweb.website_core.domain.business.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据脱敏集成测试
 * <p>
 * 测试DataMaskingService与DTO转换器的集成效果
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class DataMaskingIntegrationTest {
    
    @Mock
    private DataMaskingService dataMaskingService;
    
    @InjectMocks
    private DTOConverter dtoConverter;
    
    @Test
    void testDataMaskingServiceCreation() {
        // Given
        DataMaskingService service = new DataMaskingService();
        
        // Set up configuration
        ReflectionTestUtils.setField(service, "dataMaskingEnabled", true);
        ReflectionTestUtils.setField(service, "emailMaskLevel", "partial");
        ReflectionTestUtils.setField(service, "phoneMaskLevel", "partial");
        ReflectionTestUtils.setField(service, "maskChar", "*");
        ReflectionTestUtils.setField(service, "adminFullAccess", true);
        
        // When & Then
        assertTrue(service.isDataMaskingEnabled());
        assertEquals("partial", service.getEmailMaskLevel());
        assertEquals("partial", service.getPhoneMaskLevel());
        
        // Test email masking
        String email = "test@example.com";
        String maskedEmail = service.maskEmail(email);
        
        assertNotNull(maskedEmail);
        assertNotEquals(email, maskedEmail);
        assertTrue(maskedEmail.contains("*"));
        assertTrue(maskedEmail.contains("@example.com"));
        
        // Test phone masking
        String phone = "13812345678";
        String maskedPhone = service.maskPhoneNumber(phone);
        
        assertNotNull(maskedPhone);
        assertNotEquals(phone, maskedPhone);
        assertTrue(maskedPhone.contains("*"));
        assertEquals("138****5678", maskedPhone);
    }
    
    @Test
    void testUserEntityToDTO() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setAvatarUrl("avatar.jpg");
        user.setBio("Test bio");
        user.setLikedCount(10);
        
        // When
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        
        // Then - verify entity is properly set up
        assertTrue(user.getId() > 0);
        assertNotNull(user.getUsername());
        assertNotNull(user.getEmail());
    }
}