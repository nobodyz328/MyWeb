package com.myweb.website_core.application.service;

import com.myweb.website_core.common.exception.RateLimitExceededException;
import com.myweb.website_core.common.security.exception.ValidationException;
import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("邮箱验证服务测试")
class EmailVerificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EmailVerificationService emailVerificationService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_CODE = "123456";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        emailVerificationService = new EmailVerificationService(mailSender, redisTemplate);
    }

    @Nested
    @DisplayName("验证码生成和发送测试")
    class CodeGenerationAndSendingTests {

        @Test
        @DisplayName("应该成功发送注册验证码")
        void shouldSendRegistrationVerificationCodeSuccessfully() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null); // 没有频率限制

            // When
            CompletableFuture<Void> result = emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);

            // Then
            assertNotNull(result);
            assertTrue(result.isDone());

            // 验证Redis存储调用
            verify(valueOperations, atLeastOnce()).set(anyString(), anyString(), eq(Duration.ofMinutes(5)));

            // 验证邮件发送
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals(TEST_EMAIL, sentMessage.getTo()[0]);
            assertEquals("MyWeb - 注册验证码", sentMessage.getSubject());
            assertNotNull(sentMessage.getText());
            assertTrue(sentMessage.getText().contains("注册验证码"));
        }

        @Test
        @DisplayName("应该成功发送密码重置验证码")
        void shouldSendPasswordResetVerificationCodeSuccessfully() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null); // 没有频率限制

            // When
            CompletableFuture<Void> result = emailVerificationService.sendPasswordResetVerificationCode(TEST_EMAIL);

            // Then
            assertNotNull(result);
            assertTrue(result.isDone());

            // 验证邮件发送
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals(TEST_EMAIL, sentMessage.getTo()[0]);
            assertEquals("MyWeb - 密码重置验证码", sentMessage.getSubject());
            assertNotNull(sentMessage.getText());
            assertTrue(sentMessage.getText().contains("密码重置"));
        }

        @Test
        @DisplayName("邮件发送失败时应该抛出ValidationException")
        void shouldThrowValidationExceptionWhenEmailSendingFails() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null); // 没有频率限制
            doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

            // When & Then
            assertThrows(ValidationException.class, () -> {
                emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);
            });
        }
    }

    @Nested
    @DisplayName("验证码验证测试")
    class CodeVerificationTests {

        @Test
        @DisplayName("应该成功验证正确的注册验证码")
        void shouldVerifyCorrectRegistrationCodeSuccessfully() {
            // Given
            String expectedKey = "email:verification:registration:" + TEST_EMAIL;
            when(valueOperations.get(expectedKey)).thenReturn(TEST_CODE);
            when(redisTemplate.delete(expectedKey)).thenReturn(true);

            // When
            boolean result = emailVerificationService.verifyCode(TEST_EMAIL, TEST_CODE, 
                EmailVerificationService.VerificationType.REGISTRATION);

            // Then
            assertTrue(result);
            verify(valueOperations).get(expectedKey);
            verify(redisTemplate).delete(expectedKey);
        }

        @Test
        @DisplayName("应该成功验证正确的密码重置验证码")
        void shouldVerifyCorrectPasswordResetCodeSuccessfully() {
            // Given
            String expectedKey = "email:verification:password_reset:" + TEST_EMAIL;
            when(valueOperations.get(expectedKey)).thenReturn(TEST_CODE);
            when(redisTemplate.delete(expectedKey)).thenReturn(true);

            // When
            boolean result = emailVerificationService.verifyCode(TEST_EMAIL, TEST_CODE, 
                EmailVerificationService.VerificationType.PASSWORD_RESET);

            // Then
            assertTrue(result);
            verify(valueOperations).get(expectedKey);
            verify(redisTemplate).delete(expectedKey);
        }

        @Test
        @DisplayName("验证码不存在时应该返回false")
        void shouldReturnFalseWhenCodeDoesNotExist() {
            // Given
            String expectedKey = "email:verification:registration:" + TEST_EMAIL;
            when(valueOperations.get(expectedKey)).thenReturn(null);

            // When
            boolean result = emailVerificationService.verifyCode(TEST_EMAIL, TEST_CODE, 
                EmailVerificationService.VerificationType.REGISTRATION);

            // Then
            assertFalse(result);
            verify(valueOperations).get(expectedKey);
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("验证码错误时应该返回false")
        void shouldReturnFalseWhenCodeIsIncorrect() {
            // Given
            String expectedKey = "email:verification:registration:" + TEST_EMAIL;
            when(valueOperations.get(expectedKey)).thenReturn("654321");

            // When
            boolean result = emailVerificationService.verifyCode(TEST_EMAIL, TEST_CODE, 
                EmailVerificationService.VerificationType.REGISTRATION);

            // Then
            assertFalse(result);
            verify(valueOperations).get(expectedKey);
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("参数为null时应该返回false")
        void shouldReturnFalseWhenParametersAreNull() {
            // When & Then
            assertFalse(emailVerificationService.verifyCode(null, TEST_CODE, 
                EmailVerificationService.VerificationType.REGISTRATION));
            assertFalse(emailVerificationService.verifyCode(TEST_EMAIL, null, 
                EmailVerificationService.VerificationType.REGISTRATION));
            assertFalse(emailVerificationService.verifyCode(TEST_EMAIL, TEST_CODE, null));
        }
    }

    @Nested
    @DisplayName("频率限制测试")
    class RateLimitTests {

        @Test
        @DisplayName("超过每分钟限制时应该抛出RateLimitExceededException")
        void shouldThrowRateLimitExceptionWhenMinuteLimitExceeded() {
            // Given
            String minuteKey = "email:rate_limit:minute:" + TEST_EMAIL;
            when(valueOperations.get(minuteKey)).thenReturn("1"); // 已达到每分钟限制

            // When & Then
            assertThrows(RateLimitExceededException.class, () -> {
                emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);
            });

            verify(valueOperations).get(minuteKey);
        }

        @Test
        @DisplayName("超过每日限制时应该抛出RateLimitExceededException")
        void shouldThrowRateLimitExceptionWhenDailyLimitExceeded() {
            // Given
            String minuteKey = "email:rate_limit:minute:" + TEST_EMAIL;
            String dailyKey = "email:rate_limit:daily:" + TEST_EMAIL;
            when(valueOperations.get(minuteKey)).thenReturn(null); // 每分钟限制未达到
            when(valueOperations.get(dailyKey)).thenReturn("10"); // 已达到每日限制

            // When & Then
            assertThrows(RateLimitExceededException.class, () -> {
                emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);
            });

            verify(valueOperations).get(minuteKey);
            verify(valueOperations).get(dailyKey);
        }

        @Test
        @DisplayName("未超过限制时应该更新计数器")
        void shouldUpdateCountersWhenLimitNotExceeded() {
            // Given
            String minuteKey = "email:rate_limit:minute:" + TEST_EMAIL;
            String dailyKey = "email:rate_limit:daily:" + TEST_EMAIL;
            when(valueOperations.get(minuteKey)).thenReturn(null);
            when(valueOperations.get(dailyKey)).thenReturn(null);

            // When
            emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);

            // Then
            verify(valueOperations).set(minuteKey, "1", Duration.ofMinutes(1));
            verify(valueOperations).set(dailyKey, "1", Duration.ofHours(24));
        }

        @Test
        @DisplayName("计数器存在时应该递增")
        void shouldIncrementCountersWhenTheyExist() {
            // Given
            String minuteKey = "email:rate_limit:minute:" + TEST_EMAIL;
            String dailyKey = "email:rate_limit:daily:" + TEST_EMAIL;
            when(valueOperations.get(minuteKey)).thenReturn("0"); // 未达到限制
            when(valueOperations.get(dailyKey)).thenReturn("5"); // 未达到限制

            // When
            emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);

            // Then
            verify(valueOperations).increment(minuteKey);
            verify(valueOperations).increment(dailyKey);
        }
    }

    @Nested
    @DisplayName("验证码状态检查测试")
    class CodeStatusCheckTests {

        @Test
        @DisplayName("验证码存在时hasValidCode应该返回true")
        void shouldReturnTrueWhenCodeExists() {
            // Given
            String expectedKey = "email:verification:registration:" + TEST_EMAIL;
            when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

            // When
            boolean result = emailVerificationService.hasValidCode(TEST_EMAIL, 
                EmailVerificationService.VerificationType.REGISTRATION);

            // Then
            assertTrue(result);
            verify(redisTemplate).hasKey(expectedKey);
        }

        @Test
        @DisplayName("验证码不存在时hasValidCode应该返回false")
        void shouldReturnFalseWhenCodeDoesNotExist() {
            // Given
            String expectedKey = "email:verification:registration:" + TEST_EMAIL;
            when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

            // When
            boolean result = emailVerificationService.hasValidCode(TEST_EMAIL, 
                EmailVerificationService.VerificationType.REGISTRATION);

            // Then
            assertFalse(result);
            verify(redisTemplate).hasKey(expectedKey);
        }
    }

    @Nested
    @DisplayName("邮件内容测试")
    class EmailContentTests {

        @Test
        @DisplayName("注册验证邮件应该包含正确的内容")
        void registrationEmailShouldContainCorrectContent() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            String content = sentMessage.getText();
            
            assertTrue(content.contains("欢迎注册MyWeb博客系统"));
            assertTrue(content.contains("注册验证码"));
            assertTrue(content.contains("有效期为5分钟"));
            assertTrue(content.contains("MyWeb团队"));
        }

        @Test
        @DisplayName("密码重置验证邮件应该包含正确的内容")
        void passwordResetEmailShouldContainCorrectContent() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            emailVerificationService.sendPasswordResetVerificationCode(TEST_EMAIL);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            String content = sentMessage.getText();
            
            assertTrue(content.contains("密码重置操作"));
            assertTrue(content.contains("验证码"));
            assertTrue(content.contains("有效期为5分钟"));
            assertTrue(content.contains("账户安全"));
            assertTrue(content.contains("MyWeb团队"));
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("验证码应该是6位数字")
        void verificationCodeShouldBeSixDigits() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);

            // Then
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations, atLeastOnce()).set(anyString(), codeCaptor.capture(), any(Duration.class));

            String capturedCode = codeCaptor.getAllValues().stream()
                .filter(code -> code.matches("\\d{6}"))
                .findFirst()
                .orElse(null);

            assertNotNull(capturedCode, "应该生成6位数字验证码");
            assertEquals(6, capturedCode.length());
            assertTrue(capturedCode.matches("\\d{6}"));
        }

        @Test
        @DisplayName("不同类型的验证码应该使用不同的Redis键")
        void differentVerificationTypesShouldUseDifferentRedisKeys() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            emailVerificationService.sendRegistrationVerificationCode(TEST_EMAIL);
            emailVerificationService.sendPasswordResetVerificationCode(TEST_EMAIL);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations, atLeastOnce()).set(keyCaptor.capture(), anyString(), any(Duration.class));

            boolean hasRegistrationKey = keyCaptor.getAllValues().stream()
                .anyMatch(key -> key.contains("registration"));
            boolean hasPasswordResetKey = keyCaptor.getAllValues().stream()
                .anyMatch(key -> key.contains("password_reset"));

            assertTrue(hasRegistrationKey, "应该包含注册验证码键");
            assertTrue(hasPasswordResetKey, "应该包含密码重置验证码键");
        }
    }
}