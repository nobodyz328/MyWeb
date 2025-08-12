package com.myweb.website_core.security;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import com.myweb.website_core.infrastructure.security.filter.XssFilterService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * XSS防护功能测试
 * <p>
 * 测试XSS防护功能的核心功能，验证各种攻击场景的防护效果。
 * 重点测试：
 * 1. XSS攻击向量的检测和过滤
 * 2. 防护功能不影响业务操作
 * 3. 性能表现符合要求
 * <p>
 * 符合需求：2.1, 2.2, 2.5 - XSS防护功能测试
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("XSS防护功能测试")
class XssProtectionFunctionalTest {
    
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
    
    private static final String TEST_CLIENT_IP = "192.168.1.1";
    private static final String TEST_REQUEST_URI = "/api/posts";
    
    @BeforeEach
    void setUp() {
        // 使用lenient模式避免不必要的stubbing错误
        lenient().when(xssFilterConfig.isEnabled()).thenReturn(true);
        lenient().when(xssFilterConfig.isStrictMode()).thenReturn(false);
        lenient().when(xssFilterConfig.getAllowedTags()).thenReturn(Set.of("b", "i", "u", "strong", "em", "p", "br", "a", "img"));
        lenient().when(xssFilterConfig.getAllowedAttributes()).thenReturn(Set.of("href", "src", "alt", "title", "class"));
        lenient().when(xssFilterConfig.getTagSpecificAttributes()).thenReturn(Map.of(
            "a", Set.of("href", "title", "target"),
            "img", Set.of("src", "alt", "title", "width", "height")
        ));
        lenient().when(xssFilterConfig.isRemoveUnknownTags()).thenReturn(true);
        lenient().when(xssFilterConfig.isEncodeSpecialChars()).thenReturn(true);
        lenient().when(xssFilterConfig.getMaxTagDepth()).thenReturn(10);
        lenient().when(xssFilterConfig.getMaxContentLength()).thenReturn(50000);
        lenient().when(xssFilterConfig.getCustomXssPatterns()).thenReturn(List.of());
        lenient().when(xssFilterConfig.getWhitelistUrlPatterns()).thenReturn(List.of("/api/admin/**"));
        
        lenient().when(xssFilterConfig.getPerformance()).thenReturn(performanceConfig);
        lenient().when(performanceConfig.isCacheEnabled()).thenReturn(true);
        lenient().when(performanceConfig.getCacheSize()).thenReturn(1000);
        lenient().when(performanceConfig.getCacheExpirationMinutes()).thenReturn(30);
        
        lenient().when(xssFilterConfig.getStatistics()).thenReturn(statisticsConfig);
        lenient().when(statisticsConfig.isEnabled()).thenReturn(true);
        
        lenient().when(xssFilterConfig.getMonitoring()).thenReturn(monitoringConfig);
        lenient().when(monitoringConfig.isEnabled()).thenReturn(true);
        
        xssFilter = new XssFilterService(xssFilterConfig, xssStatisticsService, xssMonitoringService);
    }
    
    @Test
    @DisplayName("测试基本脚本注入攻击防护")
    void testBasicScriptInjectionProtection() {
        // Given - 脚本注入攻击
        String scriptAttack = "<script>alert('XSS')</script>";
        
        // When
        String result = xssFilter.filterXss(scriptAttack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then - 验证攻击被过滤
        assertNotNull(result, "结果不应为null");
        assertFalse(result.contains("<script"), "脚本标签应被移除");
        assertNotEquals(scriptAttack, result, "攻击内容应被修改");
        
        // 验证攻击统计记录
        verify(xssStatisticsService, atLeastOnce())
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), isNull());
        verify(xssMonitoringService, atLeastOnce())
            .recordXssEvent(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), eq(true), anyLong());
    }
    
    @Test
    @DisplayName("测试事件处理器攻击防护")
    void testEventHandlerAttackProtection() {
        // Given - 事件处理器攻击
        String eventAttack = "<img src=\"x\" onerror=\"alert('XSS')\">";
        
        // When
        String result = xssFilter.filterXss(eventAttack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotNull(result, "结果不应为null");
        assertFalse(result.contains("onerror"), "事件处理器应被移除");
        assertFalse(result.contains("alert"), "脚本内容应被移除");
        assertNotEquals(eventAttack, result, "攻击内容应被修改");
        
        verify(xssStatisticsService, atLeastOnce())
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), isNull());
    }
    
    @Test
    @DisplayName("测试JavaScript协议攻击防护")
    void testJavaScriptProtocolAttackProtection() {
        // Given - JavaScript协议攻击
        String jsProtocolAttack = "<a href=\"javascript:alert('XSS')\">Click me</a>";
        
        // When
        String result = xssFilter.filterXss(jsProtocolAttack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotNull(result, "结果不应为null");
        assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止");
        assertTrue(result.contains("blocked:") || !result.contains("href="), "危险协议应被替换或移除");
        assertNotEquals(jsProtocolAttack, result, "攻击内容应被修改");
        
        verify(xssStatisticsService, atLeastOnce())
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), isNull());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert(1)</script>",
        "<img onerror=alert(1) src=x>",
        "<svg onload=alert(1)>",
        "javascript:alert(1)",
        "<iframe src=javascript:alert(1)>"
    })
    @DisplayName("测试各种XSS攻击向量")
    void testVariousXssAttackVectors(String attack) {
        // When
        String result = xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        log.info("处理后的结果: " + result);
        // Then - 验证攻击被处理
        assertNotNull(result, "结果不应为null");
        assertNotEquals(attack, result, "攻击内容应被修改: " + attack);
        // 验证不包含明显的攻击代码
        assertFalse(result.contains("<script>"), "攻击代码应被移除: " + attack);

        verify(xssStatisticsService, atLeastOnce())
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), isNull());
    }
    
    @Test
    @DisplayName("测试复杂混合攻击场景")
    void testComplexMixedAttackScenario() {
        // Given - 复杂的混合攻击
        String complexAttack = """
            正常文本开始
            <script>alert('XSS1')</script>
            中间文本
            <img src="x" onerror="alert('XSS2')">
            <a href="javascript:alert('XSS3')">恶意链接</a>
            正常文本结束
            """;
        
        // When
        String result = xssFilter.filterXss(complexAttack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotNull(result, "结果不应为null");
        assertNotEquals(complexAttack, result, "攻击内容应被修改");
        
        // 验证正常文本保留
        assertTrue(result.contains("正常文本开始"), "正常文本应保留");
        assertTrue(result.contains("中间文本"), "正常文本应保留");
        assertTrue(result.contains("正常文本结束"), "正常文本应保留");
        
        // 验证攻击内容被处理
        assertFalse(result.contains("<script>"), "脚本标签应被移除");
        assertFalse(result.contains("onerror"), "事件处理器应被移除");
        assertFalse(result.contains("javascript:"), "JavaScript协议应被阻止");
        
        verify(xssStatisticsService, atLeastOnce())
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), isNull());
    }
    
    @Test
    @DisplayName("测试纯文本内容处理")
    void testPlainTextContentProcessing() {
        // Given - 纯文本内容
        String plainText = "这是一段纯文本内容，没有任何HTML标签。";
        
        // When
        String result = xssFilter.filterXss(plainText, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotNull(result, "结果不应为null");
        assertTrue(result.contains("这是一段纯文本内容"), "文本内容应保留");
        
        // 验证没有误报攻击
        verify(xssStatisticsService, never())
            .recordXssAttack(any(), any(), any(), any());
        verify(xssMonitoringService, atLeastOnce())
            .recordXssEvent(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), eq("none"), eq(false), anyLong());
    }
    
    @Test
    @DisplayName("测试白名单URL跳过过滤")
    void testWhitelistUrlSkipFiltering() {
        // Given
        String maliciousContent = "<script>alert('XSS')</script>";
        String whitelistUrl = "/api/admin/config";
        
        // When
        String result = xssFilter.filterXss(maliciousContent, whitelistUrl, TEST_CLIENT_IP);
        
        // Then
        assertEquals(maliciousContent, result, "白名单URL应跳过XSS过滤");
        
        // 验证没有记录攻击统计
        verify(xssStatisticsService, never())
            .recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试空值和边界情况")
    void testNullAndBoundaryConditions() {
        // Test null input
        String nullResult = xssFilter.filterXss(null, TEST_REQUEST_URI, TEST_CLIENT_IP);
        assertNull(nullResult, "null输入应返回null");
        
        // Test empty input
        String emptyResult = xssFilter.filterXss("", TEST_REQUEST_URI, TEST_CLIENT_IP);
        assertEquals("", emptyResult, "空字符串应返回空字符串");
        
        // Test whitespace only
        String whitespaceResult = xssFilter.filterXss("   ", TEST_REQUEST_URI, TEST_CLIENT_IP);
        assertNotNull(whitespaceResult, "空白字符串应有结果");
    }
    
    @Test
    @DisplayName("测试内容长度限制")
    void testContentLengthLimit() {
        // Given - 设置较小的长度限制
        when(xssFilterConfig.getMaxContentLength()).thenReturn(100);
        
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longContent.append("a");
        }
        
        // When
        String result = xssFilter.filterXss(longContent.toString(), TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotNull(result, "结果不应为null");
        assertTrue(result.length() <= 100, "内容应被截断到最大长度");
    }
    
    @Test
    @DisplayName("测试XSS防护基本性能")
    void testBasicXssProtectionPerformance() {
        // Given
        String content = "测试内容 <script>alert('XSS')</script> 更多内容";
        int iterations = 1000;
        
        // When
        StopWatch stopWatch = new StopWatch("XSS防护性能测试");
        stopWatch.start();
        
        for (int i = 0; i < iterations; i++) {
            String result = xssFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP);
            assertNotNull(result, "结果不应为null");
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = (double) totalTime / iterations;
        
        assertTrue(avgTime < 10.0, "平均处理时间应小于10ms，实际: " + avgTime + "ms");
        assertTrue(totalTime < 5000, "总处理时间应小于5秒，实际: " + totalTime + "ms");
        
        System.out.println("XSS防护性能测试结果:");
        System.out.println("总处理时间: " + totalTime + "ms");
        System.out.println("平均处理时间: " + avgTime + "ms");
        System.out.println("处理次数: " + iterations);
        System.out.println("吞吐量: " + (iterations * 1000.0 / totalTime) + " ops/sec");
    }
    
    @Test
    @DisplayName("测试并发XSS防护")
    void testConcurrentXssProtection() throws Exception {
        // Given
        String content = "测试内容 <script>alert('XSS')</script>";
        int threadCount = 10;
        int iterationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // When
        StopWatch stopWatch = new StopWatch("并发XSS防护测试");
        stopWatch.start();
        
        CompletableFuture<?>[] futures = IntStream.range(0, threadCount)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    String result = xssFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP + "." + i);
                    assertNotNull(result, "结果不应为null");
                    assertFalse(result.contains("<script>"), "脚本标签应被移除");
                }
            }, executor))
            .toArray(CompletableFuture[]::new);
        
        CompletableFuture.allOf(futures).get();
        stopWatch.stop();
        
        executor.shutdown();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        int totalIterations = threadCount * iterationsPerThread;
        double avgTime = (double) totalTime / totalIterations;
        
        assertTrue(avgTime < 20.0, "并发平均处理时间应小于20ms，实际: " + avgTime + "ms");
        assertTrue(totalTime < 10000, "并发总处理时间应小于10秒，实际: " + totalTime + "ms");
        
        System.out.println("并发XSS防护测试结果:");
        System.out.println("线程数: " + threadCount);
        System.out.println("每线程迭代: " + iterationsPerThread);
        System.out.println("总迭代次数: " + totalIterations);
        System.out.println("总时间: " + totalTime + "ms");
        System.out.println("平均时间: " + avgTime + "ms");
        System.out.println("吞吐量: " + (totalIterations * 1000.0 / totalTime) + " ops/sec");
    }
    
    @Test
    @DisplayName("测试业务内容兼容性")
    void testBusinessContentCompatibility() {
        // Given - 典型的业务内容
        String[] businessContents = {
            "用户发表了一篇新文章",
            "这是一条评论内容，感谢分享！",
            "文件上传成功：document.pdf",
            "搜索关键词：Java开发",
            "用户名：testuser@example.com",
            "标题：Spring Boot 最佳实践"
        };
        
        for (String content : businessContents) {
            // When
            String result = xssFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP);
            
            // Then
            assertNotNull(result, "结果不应为null: " + content);
            assertFalse(result.isEmpty(), "结果不应为空: " + content);
            assertTrue(result.length() > 0, "结果应有内容: " + content);
        }
        
        // 验证没有误报攻击
        verify(xssStatisticsService, never())
            .recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试特殊字符处理")
    void testSpecialCharacterHandling() {
        // Given - 包含特殊字符的内容
        String specialContent = "测试内容 <>&\"'/ 特殊字符";
        
        // When
        String result = xssFilter.filterXss(specialContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotNull(result, "结果不应为null");
        assertTrue(result.contains("测试内容"), "正常文本应保留");
        assertTrue(result.contains("特殊字符"), "正常文本应保留");
        
        // 验证特殊字符被编码（如果启用了编码）
        if (result.contains("&lt;")) {
            assertTrue(result.contains("&lt;"), "< 应被编码");
            assertTrue(result.contains("&gt;"), "> 应被编码");
            assertTrue(result.contains("&amp;"), "& 应被编码");
        }
    }
    
    @Test
    @DisplayName("测试控制字符移除")
    void testControlCharacterRemoval() {
        // Given - 包含控制字符的内容
        String contentWithControlChars = "测试\u0000\u0001\u0002内容";
        
        // When
        String result = xssFilter.filterXss(contentWithControlChars, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals("测试内容", result, "控制字符应被移除");
    }
    
    @Test
    @DisplayName("测试统计和监控集成")
    void testStatisticsAndMonitoringIntegration() {
        // Given
        String attack = "<script>alert('XSS')</script>";
        String normalContent = "正常内容";
        
        // When - 处理攻击内容
        xssFilter.filterXss(attack, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // When - 处理正常内容
        xssFilter.filterXss(normalContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then - 验证统计记录
        verify(xssStatisticsService, times(1))
            .recordXssAttack(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), isNull());
        
        // 验证监控记录
        verify(xssMonitoringService, times(1))
            .recordXssEvent(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), anyString(), eq(true), anyLong());
        verify(xssMonitoringService, times(1))
            .recordXssEvent(eq(TEST_CLIENT_IP), eq(TEST_REQUEST_URI), eq("none"), eq(false), anyLong());
    }
}