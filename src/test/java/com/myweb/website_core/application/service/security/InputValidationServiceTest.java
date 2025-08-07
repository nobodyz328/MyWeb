package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 输入验证服务测试类
 * 
 * 测试InputValidationService的各种验证功能，确保安全验证逻辑正确。
 * 
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("输入验证服务测试")
class InputValidationServiceTest {
    
    @InjectMocks
    private InputValidationService inputValidationService;
    
    @BeforeEach
    void setUp() {
        // 测试前准备
    }
    
    @Nested
    @DisplayName("字符串输入验证测试")
    class StringInputValidationTest {
        
        @Test
        @DisplayName("正常字符串应该通过验证")
        void shouldPassValidationForNormalString() {
            // Given
            String validInput = "这是一个正常的字符串，包含中文、English、123和符号!@#";
            
            // When & Then
            assertDoesNotThrow(() -> 
                inputValidationService.validateStringInput(validInput, "测试字段")
            );
        }
        
        @Test
        @DisplayName("null值应该通过验证")
        void shouldPassValidationForNullValue() {
            // When & Then
            assertDoesNotThrow(() -> 
                inputValidationService.validateStringInput(null, "测试字段")
            );
        }
        
        @Test
        @DisplayName("空字符串应该通过验证")
        void shouldPassValidationForEmptyString() {
            // When & Then
            assertDoesNotThrow(() -> 
                inputValidationService.validateStringInput("", "测试字段")
            );
        }
        
        @Test
        @DisplayName("超长字符串应该抛出异常")
        void shouldThrowExceptionForTooLongString() {
            // Given
            String tooLongString = "a".repeat(10001);
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateStringInput(tooLongString, "测试字段")
            );
            
            assertEquals("测试字段长度超过限制（最大10000字符）", exception.getMessage());
            assertEquals("测试字段", exception.getFieldName());
            assertEquals("LENGTH_EXCEEDED", exception.getReasonCode());
        }
        
        @Test
        @DisplayName("包含XSS攻击代码的字符串应该抛出异常")
        void shouldThrowExceptionForXssAttack() {
            // Given
            String xssInput = "<script>alert('xss')</script>";
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateStringInput(xssInput, "测试字段")
            );
            
            assertEquals("测试字段包含潜在的XSS攻击代码", exception.getMessage());
            assertEquals("XSS_DETECTED", exception.getReasonCode());
        }
        
        @Test
        @DisplayName("包含SQL注入代码的字符串应该抛出异常")
        void shouldThrowExceptionForSqlInjection() {
            // Given
            String sqlInjectionInput = "'; DROP TABLE users; --";
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateStringInput(sqlInjectionInput, "测试字段")
            );
            
            assertEquals("测试字段包含潜在的SQL注入代码", exception.getMessage());
            assertEquals("SQL_INJECTION_DETECTED", exception.getReasonCode());
        }
        
        @Test
        @DisplayName("自定义最大长度验证")
        void shouldValidateWithCustomMaxLength() {
            // Given
            String input = "a".repeat(100);
            
            // When & Then
            assertDoesNotThrow(() -> 
                inputValidationService.validateStringInput(input, "测试字段", 200)
            );
            
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateStringInput(input, "测试字段", 50)
            );
            
            assertEquals("测试字段长度超过限制（最大50字符）", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("用户名验证测试")
    class UsernameValidationTest {
        
        @Test
        @DisplayName("有效用户名应该通过验证")
        void shouldPassValidationForValidUsername() {
            // Given
            String[] validUsernames = {"user123", "test_user", "my-name", "abc"};
            
            // When & Then
            for (String username : validUsernames) {
                assertDoesNotThrow(() -> 
                    inputValidationService.validateUsername(username),
                    "用户名 " + username + " 应该通过验证"
                );
            }
        }
        
        @Test
        @DisplayName("null或空用户名应该抛出异常")
        void shouldThrowExceptionForNullOrEmptyUsername() {
            // When & Then
            ValidationException exception1 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateUsername(null)
            );
            assertEquals("用户名不能为空", exception1.getMessage());
            assertEquals("REQUIRED", exception1.getReasonCode());
            
            ValidationException exception2 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateUsername("")
            );
            assertEquals("用户名不能为空", exception2.getMessage());
        }
        
        @Test
        @DisplayName("超长用户名应该抛出异常")
        void shouldThrowExceptionForTooLongUsername() {
            // Given
            String tooLongUsername = "a".repeat(51);
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateUsername(tooLongUsername)
            );
            
            assertEquals("用户名长度超过限制（最大50字符）", exception.getMessage());
            assertEquals("LENGTH_EXCEEDED", exception.getReasonCode());
        }
        
        @Test
        @DisplayName("包含非法字符的用户名应该抛出异常")
        void shouldThrowExceptionForInvalidCharacters() {
            // Given
            String[] invalidUsernames = {"user@123", "test user", "用户名", "user#123", "user.name"};
            
            // When & Then
            for (String username : invalidUsernames) {
                ValidationException exception = assertThrows(ValidationException.class, () -> 
                    inputValidationService.validateUsername(username),
                    "用户名 " + username + " 应该抛出异常"
                );
                assertEquals("INVALID_FORMAT", exception.getReasonCode());
            }
        }
        
        @Test
        @DisplayName("保留用户名应该抛出异常")
        void shouldThrowExceptionForReservedUsername() {
            // Given
            String[] reservedUsernames = {"admin", "root", "system", "administrator"};
            
            // When & Then
            for (String username : reservedUsernames) {
                ValidationException exception = assertThrows(ValidationException.class, () -> 
                    inputValidationService.validateUsername(username),
                    "保留用户名 " + username + " 应该抛出异常"
                );
                assertEquals("RESERVED_NAME", exception.getReasonCode());
            }
        }
    }
    
    @Nested
    @DisplayName("邮箱验证测试")
    class EmailValidationTest {
        
        @Test
        @DisplayName("有效邮箱应该通过验证")
        void shouldPassValidationForValidEmail() {
            // Given
            String[] validEmails = {
                "test@example.com",
                "user.name@domain.co.uk",
                "user+tag@example.org",
                "123@test.com"
            };
            
            // When & Then
            for (String email : validEmails) {
                assertDoesNotThrow(() -> 
                    inputValidationService.validateEmail(email),
                    "邮箱 " + email + " 应该通过验证"
                );
            }
        }
        
        @Test
        @DisplayName("null或空邮箱应该抛出异常")
        void shouldThrowExceptionForNullOrEmptyEmail() {
            // When & Then
            ValidationException exception1 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateEmail(null)
            );
            assertEquals("邮箱地址不能为空", exception1.getMessage());
            
            ValidationException exception2 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateEmail("")
            );
            assertEquals("邮箱地址不能为空", exception2.getMessage());
        }
        
        @Test
        @DisplayName("格式错误的邮箱应该抛出异常")
        void shouldThrowExceptionForInvalidEmailFormat() {
            // Given
            String[] invalidEmails = {
                "invalid-email",
                "@example.com",
                "test@",
                "test@.com",
                "test..test@example.com",
                "test@example"
            };
            
            // When & Then
            for (String email : invalidEmails) {
                ValidationException exception = assertThrows(ValidationException.class, () -> 
                    inputValidationService.validateEmail(email),
                    "邮箱 " + email + " 应该抛出异常"
                );
                assertEquals("INVALID_FORMAT", exception.getReasonCode());
            }
        }
        
        @Test
        @DisplayName("超长邮箱应该抛出异常")
        void shouldThrowExceptionForTooLongEmail() {
            // Given
            String tooLongEmail = "a".repeat(90) + "@example.com";
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateEmail(tooLongEmail)
            );
            
            assertEquals("邮箱地址长度超过限制（最大100字符）", exception.getMessage());
            assertEquals("LENGTH_EXCEEDED", exception.getReasonCode());
        }
    }
    
    @Nested
    @DisplayName("密码验证测试")
    class PasswordValidationTest {
        
        @Test
        @DisplayName("强密码应该通过验证")
        void shouldPassValidationForStrongPassword() {
            // Given
            String[] strongPasswords = {
                "MyStr0ng!Pass",
                "Complex123@Password",
                "Secure#Pass2024",
                "MyP@ssw0rd123"
            };
            
            // When & Then
            for (String password : strongPasswords) {
                assertDoesNotThrow(() -> 
                    inputValidationService.validatePassword(password),
                    "密码 " + password + " 应该通过验证"
                );
            }
        }
        
        @Test
        @DisplayName("null或空密码应该抛出异常")
        void shouldThrowExceptionForNullOrEmptyPassword() {
            // When & Then
            ValidationException exception1 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePassword(null)
            );
            assertEquals("密码不能为空", exception1.getMessage());
            
            ValidationException exception2 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePassword("")
            );
            assertEquals("密码不能为空", exception2.getMessage());
        }
        
        @Test
        @DisplayName("过短密码应该抛出异常")
        void shouldThrowExceptionForTooShortPassword() {
            // Given
            String shortPassword = "1234567";
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePassword(shortPassword)
            );
            
            assertEquals("密码长度至少8个字符", exception.getMessage());
            assertEquals("TOO_SHORT", exception.getReasonCode());
        }
        
        @Test
        @DisplayName("过长密码应该抛出异常")
        void shouldThrowExceptionForTooLongPassword() {
            // Given
            String longPassword = "a".repeat(129);
            
            // When & Then
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePassword(longPassword)
            );
            
            assertEquals("密码长度不能超过128个字符", exception.getMessage());
            assertEquals("TOO_LONG", exception.getReasonCode());
        }
        
        @Test
        @DisplayName("弱密码应该抛出异常")
        void shouldThrowExceptionForWeakPassword() {
            // Given
            String[] weakPasswords = {
                "12345678",      // 只有数字
                "abcdefgh",      // 只有小写字母
                "ABCDEFGH",      // 只有大写字母
                "!@#$%^&*",      // 只有特殊字符
                "password123",   // 常见弱密码
                "123456789"      // 常见弱密码
            };
            
            // When & Then
            for (String password : weakPasswords) {
                ValidationException exception = assertThrows(ValidationException.class, () -> 
                    inputValidationService.validatePassword(password),
                    "弱密码 " + password + " 应该抛出异常"
                );
                assertTrue(exception.getReasonCode().equals("WEAK_PASSWORD") || 
                          exception.getReasonCode().equals("COMMON_PASSWORD"));
            }
        }
    }
    
    @Nested
    @DisplayName("文件名验证测试")
    class FilenameValidationTest {
        
        @Test
        @DisplayName("安全文件名应该通过验证")
        void shouldPassValidationForSafeFilename() {
            // Given
            String[] safeFilenames = {
                "document.pdf",
                "image.jpg",
                "test_file.txt",
                "中文文件名.docx",
                "file-name.png"
            };
            
            // When & Then
            for (String filename : safeFilenames) {
                assertDoesNotThrow(() -> 
                    inputValidationService.validateFilename(filename),
                    "文件名 " + filename + " 应该通过验证"
                );
            }
        }
        
        @Test
        @DisplayName("null或空文件名应该抛出异常")
        void shouldThrowExceptionForNullOrEmptyFilename() {
            // When & Then
            ValidationException exception1 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateFilename(null)
            );
            assertEquals("文件名不能为空", exception1.getMessage());
            
            ValidationException exception2 = assertThrows(ValidationException.class, () -> 
                inputValidationService.validateFilename("")
            );
            assertEquals("文件名不能为空", exception2.getMessage());
        }
        
        @Test
        @DisplayName("危险文件扩展名应该抛出异常")
        void shouldThrowExceptionForDangerousExtension() {
            // Given
            String[] dangerousFilenames = {
                "virus.exe",
                "script.bat",
                "malware.cmd",
                "trojan.scr",
                "backdoor.php"
            };
            
            // When & Then
            for (String filename : dangerousFilenames) {
                ValidationException exception = assertThrows(ValidationException.class, () -> 
                    inputValidationService.validateFilename(filename),
                    "危险文件名 " + filename + " 应该抛出异常"
                );
                assertEquals("DANGEROUS_EXTENSION", exception.getReasonCode());
            }
        }
        
        @Test
        @DisplayName("包含非法字符的文件名应该抛出异常")
        void shouldThrowExceptionForIllegalCharacters() {
            // Given
            String[] illegalFilenames = {
                "file<name.txt",
                "file>name.txt",
                "file|name.txt",
                "file?name.txt",
                "file*name.txt"
            };
            
            // When & Then
            for (String filename : illegalFilenames) {
                ValidationException exception = assertThrows(ValidationException.class, () -> 
                    inputValidationService.validateFilename(filename),
                    "非法文件名 " + filename + " 应该抛出异常"
                );
                assertEquals("ILLEGAL_CHARACTERS", exception.getReasonCode());
            }
        }
    }
    
    @Nested
    @DisplayName("URL验证测试")
    class UrlValidationTest {
        
        @Test
        @DisplayName("有效URL应该通过验证")
        void shouldPassValidationForValidUrl() {
            // Given
            String[] validUrls = {
                "https://www.example.com",
                "http://test.org/path",
                "https://subdomain.example.com/path/to/resource",
                "http://localhost:8080/api"
            };
            
            // When & Then
            for (String url : validUrls) {
                assertDoesNotThrow(() -> 
                    inputValidationService.validateUrl(url),
                    "URL " + url + " 应该通过验证"
                );
            }
        }
        
        @Test
        @DisplayName("null或空URL应该通过验证")
        void shouldPassValidationForNullOrEmptyUrl() {
            // When & Then
            assertDoesNotThrow(() -> inputValidationService.validateUrl(null));
            assertDoesNotThrow(() -> inputValidationService.validateUrl(""));
        }
        
        @Test
        @DisplayName("格式错误的URL应该抛出异常")
        void shouldThrowExceptionForInvalidUrlFormat() {
            // Given
            String[] invalidUrls = {
                "not-a-url",
                "ftp://example.com",
                "javascript:alert('xss')",
                "http://",
                "https://"
            };
            
            // When & Then
            for (String url : invalidUrls) {
                ValidationException exception = assertThrows(ValidationException.class, () -> 
                    inputValidationService.validateUrl(url),
                    "无效URL " + url + " 应该抛出异常"
                );
                assertEquals("INVALID_FORMAT", exception.getReasonCode());
            }
        }
    }
    
    @Nested
    @DisplayName("帖子内容验证测试")
    class PostContentValidationTest {
        
        @Test
        @DisplayName("有效帖子标题应该通过验证")
        void shouldPassValidationForValidPostTitle() {
            // Given
            String validTitle = "这是一个正常的帖子标题";
            
            // When & Then
            assertDoesNotThrow(() -> 
                inputValidationService.validatePostTitle(validTitle)
            );
        }
        
        @Test
        @DisplayName("有效帖子内容应该通过验证")
        void shouldPassValidationForValidPostContent() {
            // Given
            String validContent = "这是一个正常的帖子内容，包含多行文本。\n\n可以有换行和各种标点符号！";
            
            // When & Then
            assertDoesNotThrow(() -> 
                inputValidationService.validatePostContent(validContent)
            );
        }
        
        @Test
        @DisplayName("空标题或内容应该抛出异常")
        void shouldThrowExceptionForEmptyTitleOrContent() {
            // When & Then
            assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePostTitle(null)
            );
            assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePostTitle("")
            );
            assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePostContent(null)
            );
            assertThrows(ValidationException.class, () -> 
                inputValidationService.validatePostContent("")
            );
        }
    }
}