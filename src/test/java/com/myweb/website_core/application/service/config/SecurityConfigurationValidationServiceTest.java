package com.myweb.website_core.application.service.config;

import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 安全配置验证服务测试类
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigurationValidationServiceTest {

    @Mock
    private Validator validator;

    private SecurityConfigurationValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new SecurityConfigurationValidationService(validator);
    }

    @Test
    void testValidateConfiguration_Success() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        verify(validator).validate(properties);
    }

    @Test
    void testValidateConfiguration_WithConstraintViolations() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        
        ConstraintViolation<SecurityProperties> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getPropertyPath().toString()).thenReturn("passwordPolicy.minLength");
        when(violation.getMessage()).thenReturn("最小值不能小于6");
        violations.add(violation);
        
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).contains("最小值不能小于6"));
    }

    @Test
    void testValidateConfiguration_PasswordPolicyErrors() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        properties.getPasswordPolicy().setMinLength(20);
        properties.getPasswordPolicy().setMaxLength(10); // 无效：最小长度大于最大长度
        
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("密码最小长度不能大于等于最大长度")));
    }

    @Test
    void testValidateConfiguration_AccountLockErrors() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        properties.getAccountLock().setCaptchaThreshold(10);
        properties.getAccountLock().setMaxFailedAttempts(5); // 无效：验证码阈值大于最大失败次数
        
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("验证码阈值不能大于等于最大失败次数")));
    }

    @Test
    void testValidateConfiguration_FileUploadErrors() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        properties.getFileUploadSecurity().setMaxFileSize(100L * 1024 * 1024); // 100MB
        properties.getFileUploadSecurity().setMaxTotalSize(50L * 1024 * 1024);  // 50MB - 无效
        
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("单个文件最大大小不能超过总文件大小限制")));
    }

    @Test
    void testValidateConfiguration_WithWarnings() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        properties.getPasswordPolicy().setMinLength(6); // 会产生警告
        properties.getPasswordPolicy().setBcryptStrength(8); // 会产生警告
        
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertTrue(result.isValid()); // 警告不影响有效性
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("密码最小长度建议不少于8位")));
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("BCrypt强度建议不少于10")));
    }

    @Test
    void testValidateConfiguration_CrossModuleConsistency() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        properties.getSessionCleanup().setMaxInactiveMinutes(60);
        properties.getSession().setTimeoutMinutes(30); // 会话清理时间大于超时时间
        
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("会话清理的最大非活跃时间大于会话超时时间")));
    }

    @Test
    void testApplyDefaults() {
        // Given
        SecurityProperties properties = new SecurityProperties();
        
        // 清空一些列表以测试默认值应用
        properties.getInputValidation().setAllowedFileExtensions(List.of());
        properties.getXssProtection().setAllowedTags(List.of());
        properties.getDataMasking().setSensitiveFields(List.of());

        // When
        SecurityProperties result = validationService.applyDefaults(properties);

        // Then
        assertNotNull(result);
        assertFalse(result.getInputValidation().getAllowedFileExtensions().isEmpty());
        assertFalse(result.getXssProtection().getAllowedTags().isEmpty());
        assertFalse(result.getDataMasking().getSensitiveFields().isEmpty());
        
        // 验证具体默认值
        assertTrue(result.getInputValidation().getAllowedFileExtensions().contains("jpg"));
        assertTrue(result.getXssProtection().getAllowedTags().contains("b"));
        assertTrue(result.getDataMasking().getSensitiveFields().contains("email"));
    }

    @Test
    void testApplyDefaults_NullProperties() {
        // When
        SecurityProperties result = validationService.applyDefaults(null);

        // Then
        assertNotNull(result);
        assertFalse(result.getInputValidation().getAllowedFileExtensions().isEmpty());
        assertFalse(result.getXssProtection().getAllowedTags().isEmpty());
    }

    @Test
    void testValidateConfiguration_InvalidCronExpression() {
        // Given
        SecurityProperties properties = createValidSecurityProperties();
        properties.getDataIntegrity().setScheduleCheckCron("invalid-cron");
        properties.getSessionCleanup().setExpiredSessionCleanupCron("also-invalid");
        
        Set<ConstraintViolation<SecurityProperties>> violations = new HashSet<>();
        when(validator.validate(any(SecurityProperties.class))).thenReturn(violations);

        // When
        SecurityConfigurationValidationService.ValidationResult result = 
            validationService.validateConfiguration(properties);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("数据完整性检查的Cron表达式格式无效")));
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("过期会话清理的Cron表达式格式无效")));
    }

    /**
     * 创建有效的安全配置属性用于测试
     */
    private SecurityProperties createValidSecurityProperties() {
        SecurityProperties properties = new SecurityProperties();
        
        // 设置有效的密码策略
        properties.getPasswordPolicy().setMinLength(8);
        properties.getPasswordPolicy().setMaxLength(128);
        properties.getPasswordPolicy().setBcryptStrength(12);
        
        // 设置有效的账户锁定策略
        properties.getAccountLock().setMaxFailedAttempts(5);
        properties.getAccountLock().setCaptchaThreshold(3);
        
        // 设置有效的文件上传配置
        properties.getFileUploadSecurity().setMaxFileSize(10L * 1024 * 1024); // 10MB
        properties.getFileUploadSecurity().setMaxTotalSize(50L * 1024 * 1024); // 50MB
        
        // 设置有效的会话配置
        properties.getSession().setTimeoutMinutes(30);
        properties.getSessionCleanup().setMaxInactiveMinutes(25);
        
        // 设置有效的数据完整性配置
        properties.getDataIntegrity().setHashAlgorithm("SHA-256");
        properties.getDataIntegrity().setScheduleCheckCron("0 0 4 * * ?");
        
        // 设置有效的会话清理配置
        properties.getSessionCleanup().setExpiredSessionCleanupCron("0 */5 * * * ?");
        properties.getSessionCleanup().setOrphanedCleanupCron("0 0 2 * * ?");
        
        // 设置有效的文件完整性配置
        properties.getFileIntegrity().setBackupDirectory("backup/config");
        
        return properties;
    }
}