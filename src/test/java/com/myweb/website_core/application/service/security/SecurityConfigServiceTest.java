package com.myweb.website_core.application.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.config.BackupProperties;
import com.myweb.website_core.common.config.JwtProperties;
import com.myweb.website_core.common.config.RateLimitProperties;
import com.myweb.website_core.common.config.SecurityProperties;
import com.myweb.website_core.common.exception.ValidationException;
import com.myweb.website_core.domain.security.dto.SecurityConfigBackupDTO;
import com.myweb.website_core.domain.security.dto.SecurityConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 安全配置服务测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigServiceTest {
    
    @Mock
    private SecurityProperties securityProperties;
    
    @Mock
    private JwtProperties jwtProperties;
    
    @Mock
    private RateLimitProperties rateLimitProperties;
    
    @Mock
    private BackupProperties backupProperties;
    
    @Mock
    private AuditLogServiceAdapter auditLogService;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private SecurityConfigService securityConfigService;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testGetSecurityConfig() {
        // Given
        when(securityProperties.getPasswordPolicy()).thenReturn(new SecurityProperties.PasswordPolicy());
        when(jwtProperties.getSecret()).thenReturn("test-secret");
        when(rateLimitProperties.isEnabled()).thenReturn(true);
        when(backupProperties.isEnabled()).thenReturn(true);
        
        // When
        SecurityConfigDTO result = securityConfigService.getSecurityConfig();
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getSecurityProperties());
        assertNotNull(result.getJwtProperties());
        assertNotNull(result.getRateLimitProperties());
        assertNotNull(result.getBackupProperties());
        assertNotNull(result.getLastModified());
    }
    
    @Test
    void testGetConfigFromCache() {
        // Given
        String configType = "security";
        SecurityProperties expectedConfig = new SecurityProperties();
        
        // When
        Object result = securityConfigService.getConfig(configType);
        
        // Then
        assertNotNull(result);
        verify(valueOperations, times(1)).get(anyString());
    }
    
    @Test
    void testUpdateConfigSuccess() throws Exception {
        // Given
        String configType = "security";
        SecurityProperties newConfig = new SecurityProperties();
        newConfig.getPasswordPolicy().setMinLength(10);
        String operator = "admin";
        
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // When
        CompletableFuture<Boolean> result = securityConfigService.updateConfig(configType, newConfig, operator);
        
        // Then
        assertTrue(result.get());
        verify(eventPublisher, times(1)).publishEvent(any());
        verify(auditLogService, times(1)).logSecurityEvent(any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void testUpdateConfigValidationFailure() {
        // Given
        String configType = "security";
        SecurityProperties invalidConfig = new SecurityProperties();
        invalidConfig.getPasswordPolicy().setMinLength(5); // 小于最小要求8位
        String operator = "admin";
        
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        
        // When & Then
        CompletableFuture<Boolean> result = securityConfigService.updateConfig(configType, invalidConfig, operator);
        
        assertDoesNotThrow(() -> {
            Boolean success = result.get();
            assertFalse(success);
        });
    }
    
    @Test
    void testBatchUpdateConfig() throws Exception {
        // Given
        Map<String, Object> configUpdates = new HashMap<>();
        configUpdates.put("security", new SecurityProperties());
        configUpdates.put("jwt", new JwtProperties());
        String operator = "admin";
        
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // When
        CompletableFuture<Map<String, Boolean>> result = securityConfigService.batchUpdateConfig(configUpdates, operator);
        
        // Then
        Map<String, Boolean> results = result.get();
        assertNotNull(results);
        assertEquals(2, results.size());
    }
    
    @Test
    void testResetConfig() throws Exception {
        // Given
        String configType = "security";
        String operator = "admin";
        
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // When
        CompletableFuture<Boolean> result = securityConfigService.resetConfig(configType, operator);
        
        // Then
        assertTrue(result.get());
    }
    
    @Test
    void testGetConfigBackups() {
        // Given
        String configType = "security";
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("backup1", "backup2"));
        when(valueOperations.get("backup1")).thenReturn(createMockBackup("backup1"));
        when(valueOperations.get("backup2")).thenReturn(createMockBackup("backup2"));
        
        // When
        List<SecurityConfigBackupDTO> result = securityConfigService.getConfigBackups(configType);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void testRestoreConfig() throws Exception {
        // Given
        String configType = "security";
        String backupTimestamp = "20250106_120000";
        String operator = "admin";
        
        SecurityConfigBackupDTO backup = createMockBackup("backup1");
        when(valueOperations.get(anyString())).thenReturn(backup);
        when(objectMapper.readValue(anyString(), eq(SecurityProperties.class))).thenReturn(new SecurityProperties());
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // When
        CompletableFuture<Boolean> result = securityConfigService.restoreConfig(configType, backupTimestamp, operator);
        
        // Then
        assertTrue(result.get());
    }
    
    @Test
    void testValidateSecurityPropertiesSuccess() {
        // Given
        SecurityProperties validConfig = new SecurityProperties();
        validConfig.getPasswordPolicy().setMinLength(10);
        validConfig.getPasswordPolicy().setBcryptStrength(12);
        validConfig.getAccountLock().setMaxFailedAttempts(5);
        
        // When & Then
        assertDoesNotThrow(() -> {
            // 通过反射调用私有方法进行测试
            java.lang.reflect.Method method = SecurityConfigService.class.getDeclaredMethod("validateConfig", String.class, Object.class);
            method.setAccessible(true);
            method.invoke(securityConfigService, "security", validConfig);
        });
    }
    
    @Test
    void testValidateSecurityPropertiesFailure() {
        // Given
        SecurityProperties invalidConfig = new SecurityProperties();
        invalidConfig.getPasswordPolicy().setMinLength(5); // 小于最小要求
        
        // When & Then
        assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = SecurityConfigService.class.getDeclaredMethod("validateConfig", String.class, Object.class);
            method.setAccessible(true);
            method.invoke(securityConfigService, "security", invalidConfig);
        });
    }
    
    @Test
    void testValidateJwtPropertiesSuccess() {
        // Given
        JwtProperties validConfig = new JwtProperties();
        validConfig.setSecret("this-is-a-very-long-secret-key-for-jwt-token-generation");
        validConfig.setAccessTokenExpirationSeconds(3600);
        
        // When & Then
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = SecurityConfigService.class.getDeclaredMethod("validateConfig", String.class, Object.class);
            method.setAccessible(true);
            method.invoke(securityConfigService, "jwt", validConfig);
        });
    }
    
    @Test
    void testValidateJwtPropertiesFailure() {
        // Given
        JwtProperties invalidConfig = new JwtProperties();
        invalidConfig.setSecret("short"); // 密钥太短
        
        // When & Then
        assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = SecurityConfigService.class.getDeclaredMethod("validateConfig", String.class, Object.class);
            method.setAccessible(true);
            method.invoke(securityConfigService, "jwt", invalidConfig);
        });
    }
    
    private SecurityConfigBackupDTO createMockBackup(String backupId) {
        SecurityConfigBackupDTO backup = new SecurityConfigBackupDTO();
        backup.setBackupId(backupId);
        backup.setConfigType("security");
        backup.setConfigData("{}");
        backup.setOperator("admin");
        backup.setBackupTime(java.time.LocalDateTime.now());
        return backup;
    }
}