package com.myweb.website_core.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XSS编码工具类单元测试
 * 
 * 测试XssEncodingUtils的各种编码功能：
 * 1. HTML编码
 * 2. JavaScript编码
 * 3. CSS编码
 * 4. URL编码
 * 5. JSON编码
 * 6. XML编码
 * 7. 通用编码
 * 8. 清理功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
class XssEncodingUtilsTest {
    
    @Test
    void testHtmlEncode_BasicCharacters_ShouldEncode() {
        // Given
        String input = "<script>alert('XSS')</script>";
        
        // When
        String encoded = XssEncodingUtils.htmlEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&lt;"));
        assertTrue(encoded.contains("&gt;"));
        assertFalse(encoded.contains("<script>"));
        assertFalse(encoded.contains("</script>"));
    }
    
    @Test
    void testHtmlEncode_SpecialCharacters_ShouldEncode() {
        // Given
        String input = "Test & \"quotes\" 'apostrophes' <tags>";
        
        // When
        String encoded = XssEncodingUtils.htmlEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&amp;"));
        assertTrue(encoded.contains("&quot;"));
        assertTrue(encoded.contains("&#x27;"));
        assertTrue(encoded.contains("&lt;"));
        assertTrue(encoded.contains("&gt;"));
    }
    
    @Test
    void testHtmlEncode_NullAndEmpty_ShouldHandleGracefully() {
        // When & Then
        assertNull(XssEncodingUtils.htmlEncode(null));
        assertEquals("", XssEncodingUtils.htmlEncode(""));
    }
    
    @Test
    void testHtmlAttributeEncode_AttributeContext_ShouldEncode() {
        // Given
        String input = "value=\"test\" onclick='alert(1)'";
        
        // When
        String encoded = XssEncodingUtils.htmlAttributeEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&quot;"));
        assertTrue(encoded.contains("&#x27;"));
        assertFalse(encoded.contains("onclick='alert(1)'"));
    }
    
    @Test
    void testHtmlAttributeEncode_ControlCharacters_ShouldEncode() {
        // Given
        String input = "test\n\r\tvalue";
        
        // When
        String encoded = XssEncodingUtils.htmlAttributeEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&#xA;")); // \n
        assertTrue(encoded.contains("&#xD;")); // \r
        assertTrue(encoded.contains("&#x9;")); // \t
    }
    
    @Test
    void testJavascriptEncode_BasicCharacters_ShouldEncode() {
        // Given
        String input = "alert('Hello \"World\"')";
        
        // When
        String encoded = XssEncodingUtils.javascriptEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\'"));
        assertTrue(encoded.contains("\\\""));
        assertFalse(encoded.contains("'Hello \"World\"'"));
    }
    
    @Test
    void testJavascriptEncode_ControlCharacters_ShouldEncode() {
        // Given
        String input = "test\n\r\t\b\f\\content";
        
        // When
        String encoded = XssEncodingUtils.javascriptEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\n"));
        assertTrue(encoded.contains("\\r"));
        assertTrue(encoded.contains("\\t"));
        assertTrue(encoded.contains("\\b"));
        assertTrue(encoded.contains("\\f"));
        assertTrue(encoded.contains("\\\\"));
    }
    
    @Test
    void testJavascriptEncode_UnicodeCharacters_ShouldEncode() {
        // Given
        String input = "测试\u0001\u001F";
        
        // When
        String encoded = XssEncodingUtils.javascriptEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\u"));
    }
    
    @Test
    void testCssEncode_BasicCharacters_ShouldEncode() {
        // Given
        String input = "body{background:url('javascript:alert(1)')}";
        
        // When
        String encoded = XssEncodingUtils.cssEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\'"));
    }
    
    @Test
    void testCssEncode_ControlCharacters_ShouldEncode() {
        // Given
        String input = "test\n\r\fcontent";
        
        // When
        String encoded = XssEncodingUtils.cssEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\A "));  // \n
        assertTrue(encoded.contains("\\D "));  // \r
        assertTrue(encoded.contains("\\C "));  // \f
    }
    
    @Test
    void testUrlEncode_SpecialCharacters_ShouldEncode() {
        // Given
        String input = "test value with spaces & special chars";
        
        // When
        String encoded = XssEncodingUtils.urlEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("%20")); // space
        assertTrue(encoded.contains("%26")); // &
        assertFalse(encoded.contains(" "));
    }
    
    @Test
    void testJsonEncode_BasicString_ShouldEncode() {
        // Given
        String input = "Hello \"World\"";
        
        // When
        String encoded = XssEncodingUtils.jsonEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.startsWith("\""));
        assertTrue(encoded.endsWith("\""));
        assertTrue(encoded.contains("\\\""));
    }
    
    @Test
    void testJsonEncode_ControlCharacters_ShouldEncode() {
        // Given
        String input = "test\n\r\t\b\f\\content";
        
        // When
        String encoded = XssEncodingUtils.jsonEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\n"));
        assertTrue(encoded.contains("\\r"));
        assertTrue(encoded.contains("\\t"));
        assertTrue(encoded.contains("\\b"));
        assertTrue(encoded.contains("\\f"));
        assertTrue(encoded.contains("\\\\"));
    }
    
    @Test
    void testJsonEncode_NullValue_ShouldReturnNull() {
        // When
        String encoded = XssEncodingUtils.jsonEncode(null);
        
        // Then
        assertEquals("null", encoded);
    }
    
    @Test
    void testJsonEncode_EmptyString_ShouldReturnEmptyQuotes() {
        // When
        String encoded = XssEncodingUtils.jsonEncode("");
        
        // Then
        assertEquals("\"\"", encoded);
    }
    
    @Test
    void testXmlEncode_BasicCharacters_ShouldEncode() {
        // Given
        String input = "<test>content & \"value\" 'apostrophe'</test>";
        
        // When
        String encoded = XssEncodingUtils.xmlEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&lt;"));
        assertTrue(encoded.contains("&gt;"));
        assertTrue(encoded.contains("&amp;"));
        assertTrue(encoded.contains("&quot;"));
        assertTrue(encoded.contains("&apos;"));
    }
    
    @Test
    void testXmlEncode_ControlCharacters_ShouldRemoveInvalid() {
        // Given
        String input = "test\u0001\u0002\u0003content\t\n\r";
        
        // When
        String encoded = XssEncodingUtils.xmlEncode(input);
        
        // Then
        assertNotNull(encoded);
        // 应该保留合法的控制字符（\t, \n, \r）
        assertTrue(encoded.contains("\t"));
        assertTrue(encoded.contains("\n"));
        assertTrue(encoded.contains("\r"));
        // 应该移除非法的控制字符
        assertFalse(encoded.contains("\u0001"));
        assertFalse(encoded.contains("\u0002"));
        assertFalse(encoded.contains("\u0003"));
    }
    
    @Test
    void testEncode_ContextBasedEncoding_ShouldUseCorrectEncoder() {
        // Given
        String input = "<script>alert('XSS')</script>";
        
        // When & Then
        String htmlEncoded = XssEncodingUtils.encode(input, "html");
        assertTrue(htmlEncoded.contains("&lt;"));
        
        String jsEncoded = XssEncodingUtils.encode(input, "javascript");
        assertTrue(jsEncoded.contains("\\'"));
        
        String cssEncoded = XssEncodingUtils.encode(input, "css");
        assertNotNull(cssEncoded);
        
        String urlEncoded = XssEncodingUtils.encode(input, "url");
        assertTrue(urlEncoded.contains("%"));
        
        String jsonEncoded = XssEncodingUtils.encode(input, "json");
        assertTrue(jsonEncoded.startsWith("\""));
        
        String xmlEncoded = XssEncodingUtils.encode(input, "xml");
        assertTrue(xmlEncoded.contains("&lt;"));
    }
    
    @Test
    void testEncode_UnknownContext_ShouldUseHtmlEncoding() {
        // Given
        String input = "<script>alert('XSS')</script>";
        
        // When
        String encoded = XssEncodingUtils.encode(input, "unknown");
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&lt;"));
    }
    
    @Test
    void testStripHtmlTags_ShouldRemoveAllTags() {
        // Given
        String input = "<div>Hello <b>World</b> <script>alert('XSS')</script></div>";
        
        // When
        String stripped = XssEncodingUtils.stripHtmlTags(input);
        
        // Then
        assertNotNull(stripped);
        assertEquals("Hello World alert('XSS')", stripped);
        assertFalse(stripped.contains("<"));
        assertFalse(stripped.contains(">"));
    }
    
    @Test
    void testStripHtmlTags_NullAndEmpty_ShouldHandleGracefully() {
        // When & Then
        assertNull(XssEncodingUtils.stripHtmlTags(null));
        assertEquals("", XssEncodingUtils.stripHtmlTags(""));
    }
    
    @Test
    void testSanitize_ShouldCleanAndEncode() {
        // Given
        String input = "Test\u0000\u0001content<script>alert('XSS')</script>";
        
        // When
        String sanitized = XssEncodingUtils.sanitize(input);
        
        // Then
        assertNotNull(sanitized);
        assertFalse(sanitized.contains("\u0000"));
        assertFalse(sanitized.contains("\u0001"));
        assertTrue(sanitized.contains("&lt;"));
        assertTrue(sanitized.contains("&gt;"));
    }
    
    @Test
    void testSanitize_NullAndEmpty_ShouldHandleGracefully() {
        // When & Then
        assertNull(XssEncodingUtils.sanitize(null));
        assertEquals("", XssEncodingUtils.sanitize(""));
    }
    
    @Test
    void testHtmlAttributeEncode_NonAsciiCharacters_ShouldEncode() {
        // Given
        String input = "测试内容\u2028\u2029";
        
        // When
        String encoded = XssEncodingUtils.htmlAttributeEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&#x"));
    }
    
    @Test
    void testJavascriptEncode_SlashCharacter_ShouldEncode() {
        // Given
        String input = "</script>";
        
        // When
        String encoded = XssEncodingUtils.javascriptEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\/"));
    }
    
    @Test
    void testCssEncode_UnicodeCharacters_ShouldEncode() {
        // Given
        String input = "测试\u2028\u2029";
        
        // When
        String encoded = XssEncodingUtils.cssEncode(input);
        
        // Then
        assertNotNull(encoded);
        // Unicode字符应该被编码为CSS十六进制格式
        assertTrue(encoded.contains("\\"));
    }
    
    @Test
    void testJsonEncode_UnicodeCharacters_ShouldEncode() {
        // Given
        String input = "测试\u2028\u2029";
        
        // When
        String encoded = XssEncodingUtils.jsonEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("\\u"));
    }
    
    @Test
    void testXmlEncode_UnicodeCharacters_ShouldEncode() {
        // Given
        String input = "测试\u2028\u2029";
        
        // When
        String encoded = XssEncodingUtils.xmlEncode(input);
        
        // Then
        assertNotNull(encoded);
        assertTrue(encoded.contains("&#"));
    }
}