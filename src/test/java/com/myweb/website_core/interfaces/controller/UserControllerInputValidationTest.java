package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.UserService;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import com.myweb.website_core.application.service.security.authentication.UserRegistrationService;
import com.myweb.website_core.application.service.security.authorization.AccessControlService;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 用户控制器输入验证测试
 * 
 * 测试UserController中集成的输入验证功能
 */
@ExtendWith(MockitoExtension.class)
class UserControllerInputValidationTest {

    @Mock
    private UserService userService;
    
    @Mock
    private MessageProducerService messageProducerService;
    
    @Mock
    private AccessControlService accessControlService;
    
    @Mock
    private AuthenticationService authenticationService;
    
    @Mock
    private UserRegistrationService userRegistrationService;
    
    @Mock
    private EmailVerificationService emailVerificationService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private InputValidationService inputValidationService;

    @InjectMocks
    private UserController userController;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
    }

    @Test
    void testSendRegisterCode_ValidEmail_Success() {
        // Given
        String validEmail = "test@example.com";
        when(userRegistrationService.isEmailAvailable(validEmail)).thenReturn(true);
        doNothing().when(inputValidationService).validateEmail(validEmail);
        doNothing().when(emailVerificationService).sendRegistrationVerificationCode(validEmail);

        // When
        ResponseEntity<?> response = userController.sendRegisterCode(validEmail, request);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("验证码已发送到您的邮箱，请查收", response.getBody());
        verify(inputValidationService).validateEmail(validEmail);
        verify(emailVerificationService).sendRegistrationVerificationCode(validEmail);
    }

    @Test
    void testSendRegisterCode_InvalidEmail_ValidationFailure() {
        // Given
        String invalidEmail = "invalid-email";
        doThrow(new ValidationException("邮箱地址格式不正确", "email", "INVALID_FORMAT"))
                .when(inputValidationService).validateEmail(invalidEmail);

        // When
        ResponseEntity<?> response = userController.sendRegisterCode(invalidEmail, request);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("输入验证失败"));
        verify(inputValidationService).validateEmail(invalidEmail);
        verify(emailVerificationService, never()).sendRegistrationVerificationCode(anyString());
    }

    @Test
    void testCheckUsername_ValidUsername_Success() {
        // Given
        String validUsername = "testuser";
        doNothing().when(inputValidationService).validateUsername(validUsername);
        when(userRegistrationService.isUsernameAvailable(validUsername)).thenReturn(true);

        // When
        ResponseEntity<?> response = userController.checkUsername(validUsername);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        verify(inputValidationService).validateUsername(validUsername);
        verify(userRegistrationService).isUsernameAvailable(validUsername);
    }

    @Test
    void testCheckUsername_InvalidUsername_ValidationFailure() {
        // Given
        String invalidUsername = "a"; // 太短
        doThrow(new ValidationException("用户名只能包含字母、数字、下划线和连字符，长度3-50字符", "username", "INVALID_FORMAT"))
                .when(inputValidationService).validateUsername(invalidUsername);

        // When
        ResponseEntity<?> response = userController.checkUsername(invalidUsername);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("输入验证失败"));
        verify(inputValidationService).validateUsername(invalidUsername);
        verify(userRegistrationService, never()).isUsernameAvailable(anyString());
    }

    @Test
    void testCheckEmail_ValidEmail_Success() {
        // Given
        String validEmail = "test@example.com";
        doNothing().when(inputValidationService).validateEmail(validEmail);
        when(userRegistrationService.isEmailAvailable(validEmail)).thenReturn(true);

        // When
        ResponseEntity<?> response = userController.checkEmail(validEmail);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        verify(inputValidationService).validateEmail(validEmail);
        verify(userRegistrationService).isEmailAvailable(validEmail);
    }

    @Test
    void testCheckEmail_InvalidEmail_ValidationFailure() {
        // Given
        String invalidEmail = "not-an-email";
        doThrow(new ValidationException("邮箱地址格式不正确", "email", "INVALID_FORMAT"))
                .when(inputValidationService).validateEmail(invalidEmail);

        // When
        ResponseEntity<?> response = userController.checkEmail(invalidEmail);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("输入验证失败"));
        verify(inputValidationService).validateEmail(invalidEmail);
        verify(userRegistrationService, never()).isEmailAvailable(anyString());
    }

    @Test
    void testSendBindEmailCode_ValidEmail_Success() {
        // Given
        Long userId = 1L;
        String validEmail = "newemail@example.com";
        doNothing().when(inputValidationService).validateEmail(validEmail);
        when(accessControlService.canBindEmail(any(), eq(userId))).thenReturn(true);
        doNothing().when(emailVerificationService).sendEmailBindingVerificationCode(validEmail);

        // When
        ResponseEntity<?> response = userController.sendBindEmailCode(userId, validEmail, request);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("验证码已发送到您的邮箱，请查收", response.getBody());
        verify(inputValidationService).validateEmail(validEmail);
        verify(emailVerificationService).sendEmailBindingVerificationCode(validEmail);
    }

    @Test
    void testSendBindEmailCode_InvalidEmail_ValidationFailure() {
        // Given
        Long userId = 1L;
        String invalidEmail = "invalid@";
        doThrow(new ValidationException("邮箱地址格式不正确", "email", "INVALID_FORMAT"))
                .when(inputValidationService).validateEmail(invalidEmail);

        // When
        ResponseEntity<?> response = userController.sendBindEmailCode(userId, invalidEmail, request);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("输入验证失败"));
        verify(inputValidationService).validateEmail(invalidEmail);
        verify(emailVerificationService, never()).sendEmailBindingVerificationCode(anyString());
    }
}