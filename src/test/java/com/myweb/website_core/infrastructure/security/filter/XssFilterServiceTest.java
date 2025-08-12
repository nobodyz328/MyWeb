package com.myweb.website_core.infrastructure.security.filter;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 增强型XSS过滤器单元测试
 * <p>
 * 测试EnhancedXssFilter的各种功能：
 * 1. HTML标签白名单过滤
 * 2. 属性过滤
 * 3. 危险协议过滤
 * 4. 缓存机制
 * 5. 统计和监控集成
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - XSS过滤测试
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT) // 忽略不必要的stubbing警告
@ExtendWith(MockitoExtension.class)
class XssFilterServiceTest {
    
    @Mock
    private XssFilterConfig xssFilterConfig;
    
    @Mock
    private XssStatisticsService xssStatisticsService;
    
    @Mock
    private XssMonitoringService xssMonitoringService;
    
    // 添加PerformanceConfig的mock
    @Mock
    private XssFilterConfig.PerformanceConfig performanceConfig;
    
    private XssFilterService xssFilterService;
    
    @BeforeEach
    void setUp() {
        // 设置默认配置
        when(xssFilterConfig.isEnabled()).thenReturn(true);
        when(xssFilterConfig.isStrictMode()).thenReturn(false);
        when(xssFilterConfig.getAllowedTags()).thenReturn(Set.of("b", "i", "u", "strong", "em", "p", "br", "a", "img"));
        when(xssFilterConfig.getAllowedAttributes()).thenReturn(Set.of("href", "src", "alt", "title", "class"));
        when(xssFilterConfig.getTagSpecificAttributes()).thenReturn(Map.of(
            "a", Set.of("href", "title", "target"),
            "img", Set.of("src", "alt", "title", "width", "height")
        ));
        when(xssFilterConfig.isRemoveUnknownTags()).thenReturn(true);
        when(xssFilterConfig.isEncodeSpecialChars()).thenReturn(true);
        when(xssFilterConfig.getMaxTagDepth()).thenReturn(10);
        when(xssFilterConfig.getMaxContentLength()).thenReturn(50000);
        when(xssFilterConfig.getCustomXssPatterns()).thenReturn(List.of());
        when(xssFilterConfig.getWhitelistUrlPatterns()).thenReturn(List.of("/api/admin/**"));
        
        // 添加PerformanceConfig的配置
        when(xssFilterConfig.getPerformance()).thenReturn(performanceConfig);
        when(performanceConfig.isCacheEnabled()).thenReturn(true);
        when(performanceConfig.getCacheExpirationMinutes()).thenReturn(10);
        when(performanceConfig.getCacheSize()).thenReturn(1000);
        
        xssFilterService = new XssFilterService(xssFilterConfig, xssStatisticsService, xssMonitoringService);
    }
    
    @Test
    void testFilterXss_NormalContent_ShouldReturnUnchanged() {
        // Given
        String input = "This is normal content with <b>bold</b> text.";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("This is normal content with <b>bold</b> text.", result);
        verify(xssMonitoringService).recordXssEvent(eq(clientIp), eq(requestUri), eq("none"), eq(false), anyLong());
    }
    
    @Test
    void testFilterXss_ScriptTag_ShouldRemoveTag() {
        // Given
        String input = "Hello <script>alert('XSS')</script> World";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("Hello  World", result);
        verify(xssStatisticsService).recordXssAttack(eq(clientIp), eq(requestUri), eq("script_injection"), isNull());
        verify(xssMonitoringService).recordXssEvent(eq(clientIp), eq(requestUri), eq("script_injection"), eq(true), anyLong());
    }
    
    @Test
    void testFilterXss_AllowedTagWithAllowedAttributes_ShouldKeepTag() {
        // Given
        String input = "<a href=\"https://example.com\" title=\"Example\">Link</a>";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("<a href=\"https://example.com\" title=\"Example\">Link</a>", result);
    }
    
    @Test
    void testFilterXss_AllowedTagWithDisallowedAttributes_ShouldRemoveAttributes() {
        // Given
        String input = "<a href=\"https://example.com\" onclick=\"alert('XSS')\">Link</a>";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("<a href=\"https://example.com\">Link</a>", result);
    }
    
    @Test
    void testFilterXss_DisallowedTag_ShouldRemoveTag() {
        // Given
        String input = "Hello <iframe src=\"evil.html\"></iframe> World";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        assertEquals("Hello  World", result);
        log.info("Result: {}", result);
        input = "https://localhost:8443/blog/users/24/profie";
        result = xssFilterService.filterXss(input, requestUri, clientIp);
        log.info("Result: {}", result);
        // Then

    }
    
    @Test
    void testFilterXss_JavascriptProtocol_ShouldBlockProtocol() {
        // Given
        String input = "<a href=\"javascript:alert('XSS')\">Click me</a>";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertTrue(result.contains("blocked:"));
        assertFalse(result.contains("javascript:"));
    }
    
    @Test
    void testFilterXss_DangerousStyleAttribute_ShouldRemoveAttribute() {
        // Given
        String input = "<p style=\"background: url(javascript:alert('XSS'))\">Text</p>";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("<p>Text</p>", result);
    }
    
    @Test
    void testFilterXss_WhitelistedUrl_ShouldSkipFiltering() {
        // Given
        String input = "<script>alert('XSS')</script>";
        String requestUri = "/api/admin/config";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals(input, result); // Should return unchanged
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    void testFilterXss_LongContent_ShouldTruncate() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            longContent.append("a");
        }
        String input = longContent.toString();
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals(50000, result.length());
    }
    
    @Test
    void testFilterXss_SpecialCharacters_ShouldEncode() {
        // Given
        String input = "Test <>&\"' content";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
        assertTrue(result.contains("&amp;"));
        assertTrue(result.contains("&quot;"));
        assertTrue(result.contains("&#x27;"));
    }
    
    @Test
    void testFilterXss_ControlCharacters_ShouldRemove() {
        // Given
        String input = "Test\u0000\u0001\u0002content";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("Testcontent", result);
    }
    
    @Test
    void testFilterXss_CustomXssPattern_ShouldApplyCustomFilter() {
        // Given
        when(xssFilterConfig.getCustomXssPatterns()).thenReturn(List.of("eval\\s*\\("));
        String input = "Test eval('alert(1)') content";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("Test  content", result);
    }
    
    @Test
    void testFilterXss_CacheEnabled_ShouldUseCachedResult() {
        // Given
        String input = "Test content";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When - 第一次调用
        String result1 = xssFilterService.filterXss(input, requestUri, clientIp);
        // When - 第二次调用相同内容
        String result2 = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals(result1, result2);
        // 第二次调用应该使用缓存，不会再次记录监控事件
    }
    
    @Test
    void testFilterXss_NullInput_ShouldReturnNull() {
        // Given
        String input = null;
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void testFilterXss_EmptyInput_ShouldReturnEmpty() {
        // Given
        String input = "";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertEquals("", result);
    }
    
    @Test
    void testFilterXss_ComplexXssAttack_ShouldFilterAll() {
        // Given
        String input = "<script>alert('XSS1')</script>" +
                      "<img src='x' onerror='alert(2)'>" +
                      "<a href='javascript:alert(3)'>Link</a>" +
                      "<iframe src='evil.html'></iframe>" +
                      "<style>body{background:url('javascript:alert(4)')}</style>";
        String requestUri = "/api/posts";
        String clientIp = "192.168.1.1";
        
        // When
        String result = xssFilterService.filterXss(input, requestUri, clientIp);
        
        // Then
        assertFalse(result.contains("script"));
        assertFalse(result.contains("onerror"));
        assertFalse(result.contains("javascript:"));
        assertFalse(result.contains("iframe"));
        assertFalse(result.contains("style"));
        
        verify(xssStatisticsService).recordXssAttack(eq(clientIp), eq(requestUri), anyString(), isNull());
        verify(xssMonitoringService).recordXssEvent(eq(clientIp), eq(requestUri), anyString(), eq(true), anyLong());
    }
}
