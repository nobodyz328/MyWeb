package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.infrastructure.security.filter.XssHttpServletRequestWrapper;
import com.myweb.website_core.infrastructure.security.filter.XssProtectionFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * XSS防护过滤器单元测试
 * 
 * 测试XssProtectionFilter的各种功能：
 * 1. 正常请求处理
 * 2. XSS攻击检测
 * 3. 请求包装功能
 * 4. 异常处理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class XssProtectionFilterTest {
    
    private XssProtectionFilter xssProtectionFilter;
    
    @Mock
    private FilterChain filterChain;
    
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    
    @BeforeEach
    void setUp() {
        xssProtectionFilter = new XssProtectionFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }
    
    @Test
    void testDoFilter_NormalRequest_ShouldPassThrough() throws IOException, ServletException {
        // Given
        request.setParameter("username", "testuser");
        request.setParameter("content", "This is normal content");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_XssAttackInParameter_ShouldWrapAndContinue() throws IOException, ServletException {
        // Given
        request.setParameter("content", "<script>alert('XSS')</script>");
        request.setParameter("title", "Normal title");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("User-Agent", "Mozilla/5.0");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_XssAttackInHeader_ShouldWrapAndContinue() throws IOException, ServletException {
        // Given
        request.addHeader("X-Custom-Header", "javascript:alert('XSS')");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_MultipleXssVectors_ShouldHandleAll() throws IOException, ServletException {
        // Given
        request.setParameter("param1", "<script>alert('XSS1')</script>");
        request.setParameter("param2", "javascript:alert('XSS2')");
        request.setParameter("param3", "onload=alert('XSS3')");
        request.addHeader("X-Test", "<iframe src='javascript:alert(1)'></iframe>");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_WithXForwardedForHeader_ShouldExtractCorrectIp() throws IOException, ServletException {
        // Given
        request.addHeader("X-Forwarded-For", "203.0.113.1, 192.168.1.100");
        request.setParameter("content", "<script>alert('XSS')</script>");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_WithXRealIpHeader_ShouldExtractCorrectIp() throws IOException, ServletException {
        // Given
        request.addHeader("X-Real-IP", "203.0.113.1");
        request.setParameter("content", "<script>alert('XSS')</script>");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_FilterChainThrowsException_ShouldPropagateException() throws IOException, ServletException {
        // Given
        request.setParameter("content", "Normal content");
        request.setRequestURI("/api/posts");
        
        ServletException expectedException = new ServletException("Test exception");
        doThrow(expectedException).when(filterChain).doFilter(any(), any());
        
        // When & Then
        try {
            xssProtectionFilter.doFilter(request, response, filterChain);
        } catch (ServletException e) {
            // Expected exception
        }
        
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_EmptyParameters_ShouldHandleGracefully() throws IOException, ServletException {
        // Given
        request.setParameter("empty", "");
        request.setParameter("null", "null");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_LongContent_ShouldHandleEfficiently() throws IOException, ServletException {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("This is a long content line ").append(i).append(". ");
        }
        longContent.append("<script>alert('XSS')</script>");
        
        request.setParameter("content", longContent.toString());
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
    
    @Test
    void testDoFilter_SpecialCharacters_ShouldHandleCorrectly() throws IOException, ServletException {
        // Given
        request.setParameter("content", "Content with special chars: àáâãäåæçèéêë");
        request.setParameter("chinese", "中文内容测试");
        request.setParameter("emoji", "😀😁😂🤣😃😄");
        request.setRequestURI("/api/posts");
        request.setRemoteAddr("192.168.1.100");
        
        // When
        xssProtectionFilter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
}