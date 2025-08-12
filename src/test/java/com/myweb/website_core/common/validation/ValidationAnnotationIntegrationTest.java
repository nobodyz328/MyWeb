package com.myweb.website_core.common.validation;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证注解集成测试
 * 
 * 测试自定义验证注解在Spring Boot环境中的集成和功能。
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("验证注解集成测试")
class ValidationAnnotationIntegrationTest {
    
    @Autowired
    private Validator validator;

    @Autowired
    private InputValidationService inputValidationService;
    
    /**
     * 测试用的数据传输对象
     */
    static class TestDto {
        @SafeString(maxLength = 100, fieldName = "测试字段")
        private String safeStringField;
        
        @ValidUsername
        private String usernameField;
        
        @StrongPassword
        private String passwordField;
        
        @SafeFilename
        private String filenameField;
        
        // 构造函数和getter/setter
        public TestDto(String safeStringField, String usernameField, String passwordField, String filenameField) {
            this.safeStringField = safeStringField;
            this.usernameField = usernameField;
            this.passwordField = passwordField;
            this.filenameField = filenameField;
        }
        
        public String getSafeStringField() { return safeStringField; }
        public void setSafeStringField(String safeStringField) { this.safeStringField = safeStringField; }
        
        public String getUsernameField() { return usernameField; }
        public void setUsernameField(String usernameField) { this.usernameField = usernameField; }
        
        public String getPasswordField() { return passwordField; }
        public void setPasswordField(String passwordField) { this.passwordField = passwordField; }
        
        public String getFilenameField() { return filenameField; }
        public void setFilenameField(String filenameField) { this.filenameField = filenameField; }
    }
    
    @Nested
    @DisplayName("SafeString注解测试")
    class SafeStringAnnotationTest {
        
        @Test
        @DisplayName("有效字符串应该通过验证")
        void shouldPassValidationForValidString() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("safeStringField")));
        }
        
        @Test
        @DisplayName("包含XSS攻击的字符串应该验证失败")
        void shouldFailValidationForXssString() {
            // Given
            TestDto dto = new TestDto("<script>alert('xss')</script>", "validuser", "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("safeStringField") &&
                              v.getMessage().contains("XSS攻击代码")));
        }
        
        @Test
        @DisplayName("超长字符串应该验证失败")
        void shouldFailValidationForTooLongString() {
            // Given
            String longString = "a".repeat(101);
            TestDto dto = new TestDto(longString, "validuser", "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("safeStringField") &&
                              v.getMessage().contains("长度超过限制")));
        }
        
        @Test
        @DisplayName("包含SQL注入的字符串应该验证失败")
        void shouldFailValidationForSqlInjectionString() {
            // Given
            TestDto dto = new TestDto("'; DROP TABLE users; --", "validuser", "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("safeStringField") &&
                              v.getMessage().contains("SQL注入代码")));
        }
    }
    
    @Nested
    @DisplayName("ValidUsername注解测试")
    class ValidUsernameAnnotationTest {
        
        @Test
        @DisplayName("有效用户名应该通过验证")
        void shouldPassValidationForValidUsername() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser123", "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("usernameField")));
        }
        
        @Test
        @DisplayName("包含非法字符的用户名应该验证失败")
        void shouldFailValidationForInvalidUsername() {
            // Given
            TestDto dto = new TestDto("正常字符串", "invalid@user", "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            boolean hasUsernameViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("usernameField"));
            assertTrue(hasUsernameViolation, "应该有用户名验证失败，但实际违规数量: " + violations.size() + 
                      ", 违规详情: " + violations.stream()
                      .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                      .toList());
        }
        
        @Test
        @DisplayName("保留用户名应该验证失败")
        void shouldFailValidationForReservedUsername() {
            // Given
            TestDto dto = new TestDto("正常字符串", "admin", "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("usernameField") &&
                              v.getMessage().contains("系统保留")));
        }
        
        @Test
        @DisplayName("null用户名应该验证失败")
        void shouldFailValidationForNullUsername() {
            // Given
            TestDto dto = new TestDto("正常字符串", null, "ValidPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("usernameField")));
        }
    }
    
    @Nested
    @DisplayName("StrongPassword注解测试")
    class StrongPasswordAnnotationTest {
        
        @Test
        @DisplayName("强密码应该通过验证")
        void shouldPassValidationForStrongPassword() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "StrongPass123!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("passwordField")));
        }
        
        @Test
        @DisplayName("弱密码应该验证失败")
        void shouldFailValidationForWeakPassword() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "12345678", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("passwordField") &&
                              (v.getMessage().contains("字符类型") || v.getMessage().contains("过于简单"))));
        }
        
        @Test
        @DisplayName("过短密码应该验证失败")
        void shouldFailValidationForTooShortPassword() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "Pass1!", "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("passwordField") &&
                              v.getMessage().contains("至少8个字符")));
        }
        
        @Test
        @DisplayName("null密码应该验证失败")
        void shouldFailValidationForNullPassword() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", null, "test.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("passwordField")));
        }
    }
    
    @Nested
    @DisplayName("SafeFilename注解测试")
    class SafeFilenameAnnotationTest {
        
        @Test
        @DisplayName("安全文件名应该通过验证")
        void shouldPassValidationForSafeFilename() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "ValidPass123!", "document.pdf");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("filenameField")));
        }
        
        @Test
        @DisplayName("危险文件扩展名应该验证失败")
        void shouldFailValidationForDangerousFilename() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "ValidPass123!", "virus.exe");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("filenameField") &&
                              v.getMessage().contains("不允许上传该类型")));
        }
        
        @Test
        @DisplayName("包含非法字符的文件名应该验证失败")
        void shouldFailValidationForIllegalCharactersInFilename() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "ValidPass123!", "file<name.txt");
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("filenameField") &&
                              v.getMessage().contains("非法字符")));
        }
        
        @Test
        @DisplayName("null文件名应该验证失败")
        void shouldFailValidationForNullFilename() {
            // Given
            TestDto dto = new TestDto("正常字符串", "validuser", "ValidPass123!", null);
            
            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
            
            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("filenameField")));
        }
    }
    
    @Test
    @DisplayName("多个字段同时验证失败")
    void shouldFailValidationForMultipleFields() {
        // Given
        TestDto dto = new TestDto(
            "<script>alert('xss')</script>",  // XSS攻击
            "invalid@user",                   // 非法用户名
            "weak",                          // 弱密码
            "virus.exe"                      // 危险文件
        );
        
        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // Then
        assertEquals(4, violations.size());
        
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("safeStringField")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("usernameField")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("passwordField")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("filenameField")));
    }
    
    @Test
    @DisplayName("所有字段都有效时应该通过验证")
    void shouldPassValidationForAllValidFields() {
        // Given
        TestDto dto = new TestDto(
            "正常的安全字符串内容",
            "validuser123",
            "StrongPass123!",
            "document.pdf"
        );
        
        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // Then
        assertTrue(violations.isEmpty());
    }
}