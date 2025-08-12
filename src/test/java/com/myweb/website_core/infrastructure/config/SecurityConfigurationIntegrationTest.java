package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全配置集成测试
 * 验证配置加载、绑定和验证的完整流程
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.security.enabled=true",
    "app.security.input-validation.enabled=true",
    "app.security.input-validation.max-string-length=5000",
    "app.security.password-policy.min-length=8",
    "app.security.password-policy.max-length=128",
    "app.security.password-policy.bcrypt-strength=12",
    "app.security.account-lock.max-failed-attempts=5",
    "app.security.account-lock.captcha-threshold=3",
    "app.security.data-integrity.hash-algorithm=SHA-256",
    "app.security.file-upload-security.max-file-size=10485760",
    "app.security.monitoring.enabled=true"
})
class SecurityConfigurationIntegrationTest {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private SecurityConfigurationManager configurationManager;

    @Test
    void testSecurityPropertiesLoaded() {
        // 验证全局配置
        assertTrue(securityProperties.isEnabled());
        
        // 验证输入验证配置
        assertTrue(securityProperties.getInputValidation().isEnabled());
        assertEquals(5000, securityProperties.getInputValidation().getMaxStringLength());
        
        // 验证密码策略配置
        assertEquals(8, securityProperties.getPasswordPolicy().getMinLength());
        assertEquals(128, securityProperties.getPasswordPolicy().getMaxLength());
        assertEquals(12, securityProperties.getPasswordPolicy().getBcryptStrength());
        
        // 验证账户锁定配置
        assertEquals(5, securityProperties.getAccountLock().getMaxFailedAttempts());
        assertEquals(3, securityProperties.getAccountLock().getCaptchaThreshold());
        
        // 验证数据完整性配置
        assertEquals("SHA-256", securityProperties.getDataIntegrity().getHashAlgorithm());
        
        // 验证文件上传配置
        assertEquals(10485760L, securityProperties.getFileUploadSecurity().getMaxFileSize());
        
        // 验证监控配置
        assertTrue(securityProperties.getMonitoring().isEnabled());
    }

    @Test
    void testConfigurationValidation() {
        // 验证配置验证不会抛出异常
        assertDoesNotThrow(() -> configurationManager.validateSecurityConfiguration());
    }

    @Test
    void testSecurityFeatureEnabled() {
        // 验证功能开关检查
        assertTrue(configurationManager.isSecurityFeatureEnabled("input-validation"));
        assertTrue(configurationManager.isSecurityFeatureEnabled("data-integrity"));
        assertTrue(configurationManager.isSecurityFeatureEnabled("monitoring"));
        
        // 验证未知功能返回false
        assertFalse(configurationManager.isSecurityFeatureEnabled("unknown-feature"));
    }

    @Test
    void testDefaultValues() {
        // 验证默认值
        assertNotNull(securityProperties.getInputValidation().getAllowedFileExtensions());
        assertFalse(securityProperties.getInputValidation().getAllowedFileExtensions().isEmpty());
        
        assertNotNull(securityProperties.getXssProtection().getAllowedTags());
        assertFalse(securityProperties.getXssProtection().getAllowedTags().isEmpty());
        
        assertNotNull(securityProperties.getDataMasking().getSensitiveFields());
        assertFalse(securityProperties.getDataMasking().getSensitiveFields().isEmpty());
    }

    @Test
    void testConfigValueWithDefault() {
        // 测试默认值处理
        assertEquals("test", configurationManager.getConfigValueWithDefault("test", "default"));
        assertEquals("default", configurationManager.getConfigValueWithDefault(null, "default"));
        assertEquals(100, configurationManager.getConfigValueWithDefault(100, 200));
        assertEquals(200, configurationManager.getConfigValueWithDefault(null, 200));
    }

    @Test
    void testNestedConfigurationObjects() {
        // 验证嵌套配置对象正确初始化
        assertNotNull(securityProperties.getDataMasking().getMaskLength());
        assertNotNull(securityProperties.getDataMasking().getRolePermission());
        assertNotNull(securityProperties.getMonitoring().getThresholdAlerts());
        
        // 验证嵌套配置的默认值
        assertEquals(2, securityProperties.getDataMasking().getMaskLength().getEmailPrefixLength());
        assertEquals(10, securityProperties.getMonitoring().getThresholdAlerts().getFailedLoginThreshold());
    }

    @Test
    void testConfigurationConsistency() {
        // 验证配置一致性
        SecurityProperties.PasswordPolicy passwordPolicy = securityProperties.getPasswordPolicy();
        assertTrue(passwordPolicy.getMinLength() < passwordPolicy.getMaxLength(), 
                  "密码最小长度应小于最大长度");
        
        SecurityProperties.AccountLock accountLock = securityProperties.getAccountLock();
        assertTrue(accountLock.getCaptchaThreshold() < accountLock.getMaxFailedAttempts(), 
                  "验证码阈值应小于最大失败次数");
        
        SecurityProperties.FileUploadSecurity fileUpload = securityProperties.getFileUploadSecurity();
        assertTrue(fileUpload.getMaxFileSize() <= fileUpload.getMaxTotalSize(), 
                  "单个文件大小应不超过总文件大小限制");
    }
}