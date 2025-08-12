package com.myweb.website_core.infrastructure.security.aspect;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.common.validation.ValidateInput;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * SecurityValidationAspect 单元测试
 * <p>
 * 测试安全输入验证切面的各种功能，包括：
 * 1. 正常验证流程
 * 2. 验证失败处理
 * 3. 不同验证类型
 * 4. 安全事件记录
 * 5. 异常处理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class SecurityValidationAspectTest {
    
    @Mock
    private InputValidationService inputValidationService;
    
    @Mock
    private JoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    @Mock
    private Method method;
    
    @Mock
    private Parameter parameter;
    
    @InjectMocks
    private SecurityValidationAspect securityValidationAspect;
    
    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(method.getDeclaringClass()).thenReturn((Class) TestController.class);
        lenient().when(method.getName()).thenReturn("testMethod");
    }
    
    @Test
    void testValidateInput_Success() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation();
        Object[] args = {"validInput", "anotherValidInput"};
        Parameter[] parameters = {parameter, parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("testParam");
        
        // 模拟验证服务正常工作
        doNothing().when(inputValidationService).validateStringInput(anyString(), anyString(), anyInt());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用
        verify(inputValidationService, times(2)).validateStringInput(anyString(), anyString(), eq(10000));
    }
    
    @Test
    void testValidateInput_ValidationFailure() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation();
        Object[] args = {"<script>alert('xss')</script>"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("testParam");
        
        // 模拟验证失败
        ValidationException validationException = new ValidationException(
            "包含XSS攻击代码", "testParam", "XSS_DETECTED");
        doThrow(validationException).when(inputValidationService)
            .validateStringInput(anyString(), anyString(), anyInt());
        
        // 执行测试并验证异常
        ValidationException thrown = assertThrows(ValidationException.class, 
            () -> securityValidationAspect.validateInput(joinPoint));
        
        assertEquals("输入验证失败：testParam: 包含XSS攻击代码", thrown.getMessage());
        assertEquals("testParam", thrown.getField());
        assertEquals("XSS_DETECTED", thrown.getErrorCode());
    }
    
    @Test
    void testValidateInput_UsernameValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"username"});
        Object[] args = {"testuser"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("username");
        
        // 模拟用户名验证
        doNothing().when(inputValidationService).validateUsername(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了用户名验证方法
        verify(inputValidationService).validateUsername("testuser");
        verify(inputValidationService, never()).validateStringInput(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testValidateInput_EmailValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"email"});
        Object[] args = {"test@example.com"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("email");
        
        // 模拟邮箱验证
        doNothing().when(inputValidationService).validateEmail(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了邮箱验证方法
        verify(inputValidationService).validateEmail("test@example.com");
    }
    
    @Test
    void testValidateInput_PasswordValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"password"});
        Object[] args = {"StrongPassword123!"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("password");
        
        // 模拟密码验证
        doNothing().when(inputValidationService).validatePassword(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了密码验证方法
        verify(inputValidationService).validatePassword("StrongPassword123!");
    }
    
    @Test
    void testValidateInput_TitleValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"title"});
        Object[] args = {"这是一个测试标题"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("title");
        
        // 模拟标题验证
        doNothing().when(inputValidationService).validatePostTitle(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了标题验证方法
        verify(inputValidationService).validatePostTitle("这是一个测试标题");
    }
    
    @Test
    void testValidateInput_ContentValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"content"});
        Object[] args = {"这是一个测试内容，包含更多的文字来测试内容验证功能。"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("content");
        
        // 模拟内容验证
        doNothing().when(inputValidationService).validatePostContent(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了内容验证方法
        verify(inputValidationService).validatePostContent("这是一个测试内容，包含更多的文字来测试内容验证功能。");
    }
    
    @Test
    void testValidateInput_CommentValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"comment"});
        Object[] args = {"这是一个测试评论"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("comment");
        
        // 模拟评论验证
        doNothing().when(inputValidationService).validateCommentContent(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了评论验证方法
        verify(inputValidationService).validateCommentContent("这是一个测试评论");
    }
    
    @Test
    void testValidateInput_FilenameValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"filename"});
        Object[] args = {"test-file.jpg"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("filename");
        
        // 模拟文件名验证
        doNothing().when(inputValidationService).validateFilename(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了文件名验证方法
        verify(inputValidationService).validateFilename("test-file.jpg");
    }
    
    @Test
    void testValidateInput_UrlValidation() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(new String[]{"url"});
        Object[] args = {"https://example.com/test"};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("url");
        
        // 模拟URL验证
        doNothing().when(inputValidationService).validateUrl(anyString());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证调用了URL验证方法
        verify(inputValidationService).validateUrl("https://example.com/test");
    }
    
    @Test
    void testValidateInput_NullParameter_AllowEmpty() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation();
        Object[] args = {null, "validInput"};
        Parameter[] parameters = {parameter, parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("testParam");
        
        // 模拟验证服务
        doNothing().when(inputValidationService).validateStringInput(anyString(), anyString(), anyInt());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证只调用了一次（跳过了null参数）
        verify(inputValidationService, times(1)).validateStringInput(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testValidateInput_NullParameter_NotAllowEmpty() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(false);
        Object[] args = {null};
        Parameter[] parameters = {parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("testParam");
        
        // 执行测试并验证异常
        ValidationException thrown = assertThrows(ValidationException.class, 
            () -> securityValidationAspect.validateInput(joinPoint));
        
        assertEquals("参数 testParam 不能为空", thrown.getMessage());
        assertEquals("testParam", thrown.getField());
        assertEquals("REQUIRED", thrown.getErrorCode());
    }
    
    @Test
    void testValidateInput_NonStringParameter() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation();
        Object[] args = {123, "validInput"};
        Parameter[] parameters = {parameter, parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("testParam");
        
        // 模拟验证服务
        doNothing().when(inputValidationService).validateStringInput(anyString(), anyString(), anyInt());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证只调用了一次（跳过了非字符串参数）
        verify(inputValidationService, times(1)).validateStringInput(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testValidateInput_CustomFieldNames() throws Exception {
        // 准备测试数据
        ValidateInput annotation = createValidateInputAnnotation(true, new String[]{"default"}, new String[]{"customField1", "customField2"});
        Object[] args = {"input1", "input2"};
        Parameter[] parameters = {parameter, parameter};
        
        when(method.getAnnotation(ValidateInput.class)).thenReturn(annotation);
        when(joinPoint.getArgs()).thenReturn(args);
        when(method.getParameters()).thenReturn(parameters);
        when(parameter.getName()).thenReturn("originalParam");
        
        // 模拟验证服务
        doNothing().when(inputValidationService).validateStringInput(anyString(), anyString(), anyInt());
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证使用了自定义字段名
        verify(inputValidationService).validateStringInput("input1", "customField1", 10000);
        verify(inputValidationService).validateStringInput("input2", "customField2", 10000);
    }
    
    @Test
    void testValidateInput_NoAnnotation() throws Exception {
        // 准备测试数据
        when(method.getAnnotation(ValidateInput.class)).thenReturn(null);
        
        // 执行测试
        assertDoesNotThrow(() -> securityValidationAspect.validateInput(joinPoint));
        
        // 验证没有调用验证服务
        verify(inputValidationService, never()).validateStringInput(anyString(), anyString(), anyInt());
    }
    
    /**
     * 创建ValidateInput注解的模拟对象
     */
    private ValidateInput createValidateInputAnnotation() {
        return createValidateInputAnnotation(new String[]{"default"});
    }
    
    /**
     * 创建ValidateInput注解的模拟对象（指定验证类型）
     */
    private ValidateInput createValidateInputAnnotation(String[] validationTypes) {
        return createValidateInputAnnotation(true, validationTypes, new String[]{});
    }
    
    /**
     * 创建ValidateInput注解的模拟对象（指定allowEmpty）
     */
    private ValidateInput createValidateInputAnnotation(boolean allowEmpty) {
        return createValidateInputAnnotation(allowEmpty, new String[]{"default"});
    }
    
    /**
     * 创建ValidateInput注解的模拟对象（完整参数）
     */
    private ValidateInput createValidateInputAnnotation(boolean allowEmpty, String[] validationTypes) {
        return createValidateInputAnnotation(allowEmpty, validationTypes, new String[]{});
    }
    
    private ValidateInput createValidateInputAnnotation(boolean allowEmpty, String[] validationTypes, String[] fieldNames) {
        return new ValidateInput() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ValidateInput.class;
            }
            
            @Override
            public String[] fieldNames() {
                return fieldNames;
            }
            
            @Override
            public int maxLength() {
                return 10000;
            }
            
            @Override
            public boolean allowEmpty() {
                return allowEmpty;
            }
            
            @Override
            public boolean checkXss() {
                return true;
            }
            
            @Override
            public boolean checkSqlInjection() {
                return true;
            }
            
            @Override
            public String[] validationTypes() {
                return validationTypes;
            }
            
            @Override
            public boolean auditFailures() {
                return true;
            }
            
            @Override
            public String errorMessage() {
                return "输入验证失败：{fieldName}";
            }
        };
    }
    
    /**
     * 测试用的控制器类
     */
    static class TestController {
        @ValidateInput
        public void testMethod(String param1, String param2) {
            // 测试方法
        }
    }
}