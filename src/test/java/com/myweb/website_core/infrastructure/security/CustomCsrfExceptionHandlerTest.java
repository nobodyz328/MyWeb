package com.myweb.website_core.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.common.security.exception.CustomCsrfException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CSRF异常处理器单元测试
 * 
 * @author Kiro
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CustomCsrfExceptionHandlerTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private CsrfExceptionHandler csrfExceptionHandler;
    private ObjectMapper objectMapper;
    private StringWriter responseWriter;
    
    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        csrfExceptionHandler = new CsrfExceptionHandler(objectMapper);
        
        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        
        when(response.getWriter()).thenReturn(printWriter);
        when(request.getRequestURI()).thenReturn("/test/endpoint");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }
    
    @Test
    void testHandleCsrfException() throws Exception {
        // 准备CSRF异常
        CsrfException csrfException = new CsrfException("CSRF token mismatch");
        
        // 执行处理
        csrfExceptionHandler.handle(request, response, csrfException);
        
        // 验证响应状态
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding("UTF-8");
        
        // 验证响应内容
        String responseContent = responseWriter.toString();
        assertFalse(responseContent.isEmpty());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        
        assertEquals("csrf_token_invalid", responseMap.get("error"));
        assertEquals("CSRF令牌无效或已过期，请刷新页面后重试", responseMap.get("message"));
        assertEquals(403, responseMap.get("code"));
        assertEquals("/test/endpoint", responseMap.get("path"));
        assertNotNull(responseMap.get("timestamp"));
    }
    
    @Test
    void testHandleCsrfExceptionWithAjaxRequest() throws Exception {
        // 模拟Ajax请求
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

        CsrfException csrfException = new CsrfException("CSRF token mismatch");
        
        // 执行处理
        csrfExceptionHandler.handle(request, response, csrfException);
        
        // 验证响应内容包含Ajax特定字段
        String responseContent = responseWriter.toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        
        assertEquals("refresh_token", responseMap.get("action"));
        assertEquals("/api/csrf/token", responseMap.get("refresh_url"));
    }
    
    @Test
    void testHandleCsrfExceptionWithJsonContentType() throws Exception {
        // 模拟JSON请求
        when(request.getContentType()).thenReturn("application/json");

        CsrfException csrfException = new CsrfException("CSRF token mismatch");
        
        // 执行处理
        csrfExceptionHandler.handle(request, response, csrfException);
        
        // 验证响应内容包含Ajax特定字段
        String responseContent = responseWriter.toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        
        assertEquals("refresh_token", responseMap.get("action"));
        assertEquals("/api/csrf/token", responseMap.get("refresh_url"));
    }
    
    @Test
    void testHandleCustomCsrfException() throws Exception {
        // 准备自定义CSRF异常
        CustomCsrfException customCsrfException = new CustomCsrfException("Custom CSRF error");
        AccessDeniedException accessDeniedException =
                new AccessDeniedException("CSRF token invalid", customCsrfException);
        // 执行处理
        csrfExceptionHandler.handle(request, response, accessDeniedException);
        
        // 验证响应状态和内容
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        String responseContent = responseWriter.toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        
        assertEquals("csrf_token_invalid", responseMap.get("error"));
    }
    
    @Test
    void testHandleGeneralAccessDeniedException() throws Exception {
        // 准备一般访问拒绝异常
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
        
        // 执行处理
        csrfExceptionHandler.handle(request, response, accessDeniedException);
        
        // 验证响应状态
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding("UTF-8");
        
        // 验证响应内容
        String responseContent = responseWriter.toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        
        assertEquals("access_denied", responseMap.get("error"));
        assertEquals("访问被拒绝，您没有足够的权限执行此操作", responseMap.get("message"));
        assertEquals(403, responseMap.get("code"));
        assertEquals("/test/endpoint", responseMap.get("path"));
        assertNotNull(responseMap.get("timestamp"));
    }
    
    @Test
    void testGetClientIpAddressWithXForwardedFor() throws Exception {
        // 模拟X-Forwarded-For头
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        AccessDeniedException exception = new AccessDeniedException("Test");
        csrfExceptionHandler.handle(request, response, exception);
        
        // 验证日志中使用了正确的IP地址（通过验证方法被调用）
        verify(request).getHeader("X-Forwarded-For");
    }
    
    @Test
    void testGetClientIpAddressWithXRealIp() throws Exception {
        // 模拟X-Real-IP头
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.100");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        AccessDeniedException exception = new AccessDeniedException("Test");
        csrfExceptionHandler.handle(request, response, exception);
        
        // 验证方法被调用
        verify(request).getHeader("X-Real-IP");
    }
    
    @Test
    void testGetClientIpAddressWithRemoteAddr() throws Exception {
        // 只使用RemoteAddr
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        
        AccessDeniedException exception = new AccessDeniedException("Test");
        csrfExceptionHandler.handle(request, response, exception);
        
        // 验证方法被调用
        verify(request).getRemoteAddr();
    }
    
    @Test
    void testIsAjaxRequestWithAcceptHeader() throws Exception {
        // 模拟Accept头包含application/json
        when(request.getHeader("Accept")).thenReturn("application/json, text/plain, */*");

        CsrfException csrfException = new CsrfException("CSRF token mismatch");
        csrfExceptionHandler.handle(request, response, csrfException);
        
        // 验证响应包含Ajax特定字段
        String responseContent = responseWriter.toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        
        assertEquals("refresh_token", responseMap.get("action"));
    }
}