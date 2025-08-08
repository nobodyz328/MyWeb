package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.infrastructure.security.filter.XssHttpServletRequestWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XSS HTTP请求包装器单元测试
 * 
 * 测试XssHttpServletRequestWrapper的各种功能：
 * 1. 参数清理功能
 * 2. 请求头清理功能
 * 3. XSS攻击检测
 * 4. 编码功能
 * 5. 边界情况处理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
class XssHttpServletRequestWrapperTest {
    
    private MockHttpServletRequest mockRequest;
    private XssHttpServletRequestWrapper wrapper;
    
    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
    }
    
    @Test
    void testGetParameter_NormalContent_ShouldReturnUnchanged() {
        // Given
        mockRequest.setParameter("username", "testuser");
        mockRequest.setParameter("content", "This is normal content");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String username = wrapper.getParameter("username");
        String content = wrapper.getParameter("content");
        
        // Then
        assertNotNull(username);
        assertNotNull(content);
        assertFalse(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_ScriptTag_ShouldCleanAndDetect() {
        // Given
        mockRequest.setParameter("content", "<script>alert('XSS')</script>");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("<script>"));
        assertFalse(cleanedContent.contains("</script>"));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_JavascriptProtocol_ShouldCleanAndDetect() {
        // Given
        mockRequest.setParameter("url", "javascript:alert('XSS')");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedUrl = wrapper.getParameter("url");
        
        // Then
        assertNotNull(cleanedUrl);
        assertFalse(cleanedUrl.contains("javascript:"));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_EventHandler_ShouldCleanAndDetect() {
        // Given
        mockRequest.setParameter("content", "<img src='x' onerror='alert(1)'>");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("onerror"));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_MultipleEventHandlers_ShouldCleanAll() {
        // Given
        String maliciousContent = "<div onclick='alert(1)' onmouseover='alert(2)' onload='alert(3)'>Test</div>";
        mockRequest.setParameter("content", maliciousContent);
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("onclick"));
        assertFalse(cleanedContent.contains("onmouseover"));
        assertFalse(cleanedContent.contains("onload"));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_IframeTag_ShouldCleanAndDetect() {
        // Given
        mockRequest.setParameter("content", "<iframe src='javascript:alert(1)'></iframe>");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("<iframe"));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_StyleTag_ShouldCleanAndDetect() {
        // Given
        mockRequest.setParameter("content", "<style>body{background:url('javascript:alert(1)')}</style>");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("<style"));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_ExpressionAttack_ShouldCleanAndDetect() {
        // Given
        mockRequest.setParameter("content", "eval('alert(1)')");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("eval("));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameterValues_MultipleValues_ShouldCleanAll() {
        // Given
        mockRequest.setParameter("tags", new String[]{
            "normal-tag",
            "<script>alert('XSS1')</script>",
            "another-normal-tag",
            "javascript:alert('XSS2')"
        });
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String[] cleanedValues = wrapper.getParameterValues("tags");
        
        // Then
        assertNotNull(cleanedValues);
        assertEquals(4, cleanedValues.length);
        
        // 检查正常标签未被改变
        assertTrue(cleanedValues[0].contains("normal-tag"));
        assertTrue(cleanedValues[2].contains("another-normal-tag"));
        
        // 检查恶意内容被清理
        assertFalse(cleanedValues[1].contains("<script>"));
        assertFalse(cleanedValues[3].contains("javascript:"));
        
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetHeader_XssInHeader_ShouldCleanAndDetect() {
        // Given
        mockRequest.addHeader("X-Custom-Header", "<script>alert('XSS')</script>");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedHeader = wrapper.getHeader("X-Custom-Header");
        
        // Then
        assertNotNull(cleanedHeader);
        assertFalse(cleanedHeader.contains("<script>"));
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_NullValue_ShouldReturnNull() {
        // Given
        mockRequest.setParameter("nullParam", (String) null);
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String result = wrapper.getParameter("nullParam");
        
        // Then
        assertNull(result);
        assertFalse(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_EmptyValue_ShouldReturnEmpty() {
        // Given
        mockRequest.setParameter("emptyParam", "");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String result = wrapper.getParameter("emptyParam");
        
        // Then
        assertEquals("", result);
        assertFalse(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_HtmlEntities_ShouldEncode() {
        // Given
        mockRequest.setParameter("content", "Test <>&\"' content");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertTrue(cleanedContent.contains("&lt;"));
        assertTrue(cleanedContent.contains("&gt;"));
        assertTrue(cleanedContent.contains("&amp;"));
    }
    
    @Test
    void testGetParameter_UnicodeCharacters_ShouldHandleCorrectly() {
        // Given
        mockRequest.setParameter("content", "Unicode test: àáâãäåæçèéêë 中文 😀");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_EncodedXss_ShouldDetectAndClean() {
        // Given
        mockRequest.setParameter("content", "&#60;script&#62;alert('XSS')&#60;/script&#62;");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertTrue(wrapper.hasXssAttempt());
    }
    
    @Test
    void testGetParameter_VeryLongContent_ShouldTruncate() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 15000; i++) {
            longContent.append("a");
        }
        mockRequest.setParameter("content", longContent.toString());
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertTrue(cleanedContent.length() <= 10000);
    }
    
    @Test
    void testGetParameter_ControlCharacters_ShouldRemove() {
        // Given
        String contentWithControlChars = "Test\u0000\u0001\u0002content";
        mockRequest.setParameter("content", contentWithControlChars);
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("\u0000"));
        assertFalse(cleanedContent.contains("\u0001"));
        assertFalse(cleanedContent.contains("\u0002"));
    }
    
    @Test
    void testGetXssDetectionStats_ShouldReturnCorrectInfo() {
        // Given
        mockRequest.setParameter("content", "<script>alert('XSS')</script>");
        mockRequest.setRequestURI("/api/posts");
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        wrapper.getParameter("content"); // 触发XSS检测
        String stats = wrapper.getXssDetectionStats();
        
        // Then
        assertNotNull(stats);
        assertTrue(stats.contains("攻击尝试: 是"));
        assertTrue(stats.contains("/api/posts"));
    }
    
    @Test
    void testGetParameter_CombinedAttacks_ShouldHandleAll() {
        // Given
        String combinedAttack = "<script>alert('XSS1')</script>" +
                               "javascript:alert('XSS2')" +
                               "<img onerror='alert(3)' src='x'>" +
                               "<iframe src='javascript:alert(4)'></iframe>" +
                               "eval('alert(5)')";
        mockRequest.setParameter("content", combinedAttack);
        wrapper = new XssHttpServletRequestWrapper(mockRequest);
        
        // When
        String cleanedContent = wrapper.getParameter("content");
        
        // Then
        assertNotNull(cleanedContent);
        assertFalse(cleanedContent.contains("<script>"));
        assertFalse(cleanedContent.contains("javascript:"));
        assertFalse(cleanedContent.contains("onerror"));
        assertFalse(cleanedContent.contains("<iframe"));
        assertFalse(cleanedContent.contains("eval("));
        assertTrue(wrapper.hasXssAttempt());
    }
}