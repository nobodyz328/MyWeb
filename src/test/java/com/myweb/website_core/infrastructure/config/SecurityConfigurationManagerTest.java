package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 安全配置管理器测试类
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigurationManagerTest {

    @Mock
    private Validator validator;

    @Mock(lenient = true)
    private SecurityProperties securityProperties;

    private SecurityConfigurationManager configurationManager;

    @BeforeEach
    void setUp() {
        configurationManager = new SecurityConfigurationManager(securityProperties, validator);
    }

    @Test
    void testValidateSecurityConfiguration_Success() {
        // Given
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);
        
        // Mock nested properties
        SecurityProperties.PasswordPolicy passwordPolicy = new SecurityProperties.PasswordPolicy();
        passwordPolicy.setMinLength(8);
        passwordPolicy.setMaxLength(128);
        
        SecurityProperties.AccountLock accountLock = new SecurityProperties.AccountLock();
        accountLock.setCaptchaThreshold(3);
        accountLock.setMaxFailedAttempts(5);
        
        SecurityProperties.FileUploadSecurity fileUpload = new SecurityProperties.FileUploadSecurity();
        fileUpload.setMaxFileSize(10485760L);
        fileUpload.setMaxTotalSize(52428800L);
        
        SecurityProperties.Session session = new SecurityProperties.Session();
        session.setTimeoutMinutes(30);
        
        SecurityProperties.SessionCleanup sessionCleanup = new SecurityProperties.SessionCleanup();
        sessionCleanup.setMaxInactiveMinutes(25);
        
        when(securityProperties.getPasswordPolicy()).thenReturn(passwordPolicy);
        when(securityProperties.getAccountLock()).thenReturn(accountLock);
        when(securityProperties.getFileUploadSecurity()).thenReturn(fileUpload);
        when(securityProperties.getSession()).thenReturn(session);
        when(securityProperties.getSessionCleanup()).thenReturn(sessionCleanup);

        // When & Then
        assertDoesNotThrow(() -> configurationManager.validateSecurityConfiguration());
        
        verify(validator).validate(securityProperties);
    }

    @Test
    void testValidateSecurityConfiguration_ValidationFailure() {
        // Given
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        ConstraintViolation<SecurityProperties> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getMessage()).thenReturn("测试验证错误");
        violations.add(violation);
        
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationManager.validateSecurityConfiguration()
        );
        
        assertTrue(exception.getMessage().contains("安全配置验证失败"));
    }

    @Test
    void testValidateSecurityConfiguration_BusinessRuleFailure() {
        // Given
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);
        
        // Mock invalid password policy
        SecurityProperties.PasswordPolicy passwordPolicy = new SecurityProperties.PasswordPolicy();
        passwordPolicy.setMinLength(20);  // 大于最大长度
        passwordPolicy.setMaxLength(10);
        
        SecurityProperties.AccountLock accountLock = new SecurityProperties.AccountLock();
        accountLock.setCaptchaThreshold(3);
        accountLock.setMaxFailedAttempts(5);
        
        SecurityProperties.FileUploadSecurity fileUpload = new SecurityProperties.FileUploadSecurity();
        fileUpload.setMaxFileSize(10485760L);
        fileUpload.setMaxTotalSize(52428800L);
        
        SecurityProperties.Session session = new SecurityProperties.Session();
        session.setTimeoutMinutes(30);
        
        SecurityProperties.SessionCleanup sessionCleanup = new SecurityProperties.SessionCleanup();
        sessionCleanup.setMaxInactiveMinutes(25);
        
        when(securityProperties.getPasswordPolicy()).thenReturn(passwordPolicy);
        when(securityProperties.getAccountLock()).thenReturn(accountLock);
        when(securityProperties.getFileUploadSecurity()).thenReturn(fileUpload);
        when(securityProperties.getSession()).thenReturn(session);
        when(securityProperties.getSessionCleanup()).thenReturn(sessionCleanup);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationManager.validateSecurityConfiguration()
        );
        
        assertTrue(exception.getMessage().contains("密码最小长度不能大于等于最大长度"));
    }

    @Test
    void testIsSecurityFeatureEnabled_GlobalDisabled() {
        // Given
        when(securityProperties.isEnabled()).thenReturn(false);

        // When & Then
        assertFalse(configurationManager.isSecurityFeatureEnabled("input-validation"));
        assertFalse(configurationManager.isSecurityFeatureEnabled("xss-protection"));
        assertFalse(configurationManager.isSecurityFeatureEnabled("data-integrity"));
    }

    @Test
    void testIsSecurityFeatureEnabled_SpecificFeatures() {
        // Given
        when(securityProperties.isEnabled()).thenReturn(true);
        
        SecurityProperties.InputValidation inputValidation = new SecurityProperties.InputValidation();
        inputValidation.setEnabled(true);
        
        SecurityProperties.XssProtection xssProtection = new SecurityProperties.XssProtection();
        xssProtection.setEnabled(false);
        
        when(securityProperties.getInputValidation()).thenReturn(inputValidation);
        when(securityProperties.getXssProtection()).thenReturn(xssProtection);

        // When & Then
        assertTrue(configurationManager.isSecurityFeatureEnabled("input-validation"));
        assertFalse(configurationManager.isSecurityFeatureEnabled("xss-protection"));
        assertFalse(configurationManager.isSecurityFeatureEnabled("unknown-feature"));
    }

    @Test
    void testGetConfigValueWithDefault() {
        // When & Then
        assertEquals("test", configurationManager.getConfigValueWithDefault("test", "default"));
        assertEquals("default", configurationManager.getConfigValueWithDefault(null, "default"));
        assertEquals(100, configurationManager.getConfigValueWithDefault(100, 200));
        assertEquals(200, configurationManager.getConfigValueWithDefault(null, 200));
    }

    @Test
    void testOnConfigurationRefresh_Success() {
        // Given
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);
        
        // Mock nested properties for business rule validation
        SecurityProperties.PasswordPolicy passwordPolicy = new SecurityProperties.PasswordPolicy();
        passwordPolicy.setMinLength(8);
        passwordPolicy.setMaxLength(128);
        
        SecurityProperties.AccountLock accountLock = new SecurityProperties.AccountLock();
        accountLock.setCaptchaThreshold(3);
        accountLock.setMaxFailedAttempts(5);
        
        SecurityProperties.FileUploadSecurity fileUpload = new SecurityProperties.FileUploadSecurity();
        fileUpload.setMaxFileSize(10485760L);
        fileUpload.setMaxTotalSize(52428800L);
        
        SecurityProperties.Session session = new SecurityProperties.Session();
        session.setTimeoutMinutes(30);
        
        SecurityProperties.SessionCleanup sessionCleanup = new SecurityProperties.SessionCleanup();
        sessionCleanup.setMaxInactiveMinutes(25);
        
        // Mock all required properties for logConfigurationSummary
        when(securityProperties.isEnabled()).thenReturn(true);
        when(securityProperties.getPasswordPolicy()).thenReturn(passwordPolicy);
        when(securityProperties.getAccountLock()).thenReturn(accountLock);
        when(securityProperties.getFileUploadSecurity()).thenReturn(fileUpload);
        when(securityProperties.getSession()).thenReturn(session);
        when(securityProperties.getSessionCleanup()).thenReturn(sessionCleanup);
        
        // Mock all other properties needed for logConfigurationSummary
        when(securityProperties.getInputValidation()).thenReturn(new SecurityProperties.InputValidation());
        when(securityProperties.getXssProtection()).thenReturn(new SecurityProperties.XssProtection());
        when(securityProperties.getDataIntegrity()).thenReturn(new SecurityProperties.DataIntegrity());
        when(securityProperties.getFileIntegrity()).thenReturn(new SecurityProperties.FileIntegrity());
        when(securityProperties.getSafeQuery()).thenReturn(new SecurityProperties.SafeQuery());
        when(securityProperties.getDataMasking()).thenReturn(new SecurityProperties.DataMasking());
        when(securityProperties.getDataDeletion()).thenReturn(new SecurityProperties.DataDeletion());
        when(securityProperties.getMonitoring()).thenReturn(new SecurityProperties.Monitoring());

        // When & Then
        assertDoesNotThrow(() -> configurationManager.onConfigurationRefresh());
    }

    @Test
    void testOnConfigurationRefresh_Failure() {
        // Given
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        ConstraintViolation<SecurityProperties> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getMessage()).thenReturn("配置刷新验证错误");
        violations.add(violation);
        
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> configurationManager.onConfigurationRefresh()
        );
        
        assertTrue(exception.getMessage().contains("配置热更新失败"));
    }
}