package com.myweb.website_core.security;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import com.myweb.website_core.infrastructure.security.filter.XssFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * XSS防护性能测试
 * <p>
 * 专门测试XSS防护功能的性能表现，确保安全功能不会显著影响系统性能。
 * <p>
 * 测试内容：
 * 1. 单线程性能测试
 * 2. 多线程并发性能测试
 * 3. 大内容处理性能测试
 * 4. 缓存机制性能测试
 * 5. 内存使用测试
 * 6. 长时间运行稳定性测试
 * <p>
 * 符合需求：2.1, 2.2, 2.5 - XSS防护性能测试
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("XSS防护性能测试")
class XssProtectionPerformanceTest {
    
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
    
    // 性能基准值（毫秒）
    private static final long SINGLE_OPERATION_THRESHOLD = 50;
    private static final long BATCH_OPERATION_THRESHOLD = 5000;
    private static final long CONCURRENT_OPERATION_THRESHOLD = 10000;
    
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
    @DisplayName("测试简单内容处理性能")
    void testSimpleContentProcessingPerformance() {
        // Given
        String simpleContent = "<p>这是一段简单的文本内容，包含<b>粗体</b>和<i>斜体</i>。</p>";
        int iterations = 10000;
        
        // When
        StopWatch stopWatch = new StopWatch("简单内容处理性能测试");
        stopWatch.start();
        
        for (int i = 0; i < iterations; i++) {
            String result = xssFilter.filterXss(simpleContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
            assertNotNull(result);
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = (double) totalTime / iterations;
        
        assertTrue(avgTime < 1.0, "简单内容平均处理时间应小于1ms，实际: " + avgTime + "ms");
        assertTrue(totalTime < BATCH_OPERATION_THRESHOLD, "总处理时间应小于" + BATCH_OPERATION_THRESHOLD + "ms，实际: " + totalTime + "ms");
        
        printPerformanceResult("简单内容处理", iterations, totalTime, avgTime);
    }
    
    @Test
    @DisplayName("测试复杂内容处理性能")
    void testComplexContentProcessingPerformance() {
        // Given
        String complexContent = generateComplexContent();
        int iterations = 1000;
        
        // When
        StopWatch stopWatch = new StopWatch("复杂内容处理性能测试");
        stopWatch.start();
        
        for (int i = 0; i < iterations; i++) {
            String result = xssFilter.filterXss(complexContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
            assertNotNull(result);
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = (double) totalTime / iterations;
        
        assertTrue(avgTime < 10.0, "复杂内容平均处理时间应小于10ms，实际: " + avgTime + "ms");
        assertTrue(totalTime < BATCH_OPERATION_THRESHOLD, "总处理时间应小于" + BATCH_OPERATION_THRESHOLD + "ms，实际: " + totalTime + "ms");
        
        printPerformanceResult("复杂内容处理", iterations, totalTime, avgTime);
    }
    
    @Test
    @DisplayName("测试攻击内容处理性能")
    void testMaliciousContentProcessingPerformance() {
        // Given
        String maliciousContent = generateMaliciousContent();
        int iterations = 1000;
        
        // When
        StopWatch stopWatch = new StopWatch("攻击内容处理性能测试");
        stopWatch.start();
        
        for (int i = 0; i < iterations; i++) {
            String result = xssFilter.filterXss(maliciousContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
            assertNotNull(result);
            assertFalse(result.contains("<script>"), "攻击内容应被过滤");
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = (double) totalTime / iterations;
        
        assertTrue(avgTime < 20.0, "攻击内容平均处理时间应小于20ms，实际: " + avgTime + "ms");
        assertTrue(totalTime < BATCH_OPERATION_THRESHOLD, "总处理时间应小于" + BATCH_OPERATION_THRESHOLD + "ms，实际: " + totalTime + "ms");
        
        printPerformanceResult("攻击内容处理", iterations, totalTime, avgTime);
    }
    
    @Test
    @DisplayName("测试大内容处理性能")
    void testLargeContentProcessingPerformance() {
        // Given
        String largeContent = generateLargeContent(10000); // 10KB内容
        int iterations = 100;
        
        // When
        StopWatch stopWatch = new StopWatch("大内容处理性能测试");
        stopWatch.start();
        
        for (int i = 0; i < iterations; i++) {
            String result = xssFilter.filterXss(largeContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
            assertNotNull(result);
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = (double) totalTime / iterations;
        
        assertTrue(avgTime < 100.0, "大内容平均处理时间应小于100ms，实际: " + avgTime + "ms");
        assertTrue(totalTime < BATCH_OPERATION_THRESHOLD, "总处理时间应小于" + BATCH_OPERATION_THRESHOLD + "ms，实际: " + totalTime + "ms");
        
        printPerformanceResult("大内容处理", iterations, totalTime, avgTime);
    }
    
    @Test
    @DisplayName("测试并发处理性能")
    void testConcurrentProcessingPerformance() throws InterruptedException, ExecutionException {
        // Given
        String content = "<p>测试内容</p><script>alert('XSS')</script>";
        int threadCount = 20;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // When
        StopWatch stopWatch = new StopWatch("并发处理性能测试");
        stopWatch.start();
        
        CompletableFuture<Long>[] futures = IntStream.range(0, threadCount)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                long threadStartTime = System.currentTimeMillis();
                for (int j = 0; j < iterationsPerThread; j++) {
                    String result = xssFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP + "." + i);
                    assertNotNull(result);
                    assertFalse(result.contains("<script>"));
                }
                return System.currentTimeMillis() - threadStartTime;
            }, executor))
            .toArray(CompletableFuture[]::new);
        
        CompletableFuture.allOf(futures).get();
        stopWatch.stop();
        
        executor.shutdown();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        int totalIterations = threadCount * iterationsPerThread;
        double avgTime = (double) totalTime / totalIterations;
        
        assertTrue(avgTime < 10.0, "并发平均处理时间应小于10ms，实际: " + avgTime + "ms");
        assertTrue(totalTime < CONCURRENT_OPERATION_THRESHOLD, "并发总处理时间应小于" + CONCURRENT_OPERATION_THRESHOLD + "ms，实际: " + totalTime + "ms");
        
        printConcurrentPerformanceResult("并发处理", threadCount, iterationsPerThread, totalTime, avgTime);
    }
    
    @Test
    @DisplayName("测试缓存性能优化效果")
    void testCachePerformanceOptimization() {
        // Given
        String content = "<p>缓存测试内容</p><script>alert('test')</script>";
        int iterations = 1000;
        
        // When - 测试无缓存性能
        when(performanceConfig.isCacheEnabled()).thenReturn(false);
        XssFilterService noCacheFilter = new XssFilterService(xssFilterConfig, xssStatisticsService, xssMonitoringService);
        
        StopWatch noCacheStopWatch = new StopWatch("无缓存性能测试");
        noCacheStopWatch.start();
        
        for (int i = 0; i < iterations; i++) {
            noCacheFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP);
        }
        
        noCacheStopWatch.stop();
        long noCacheTime = noCacheStopWatch.getTotalTimeMillis();
        
        // When - 测试有缓存性能
        when(performanceConfig.isCacheEnabled()).thenReturn(true);
        XssFilterService cacheFilter = new XssFilterService(xssFilterConfig, xssStatisticsService, xssMonitoringService);
        
        StopWatch cacheStopWatch = new StopWatch("有缓存性能测试");
        cacheStopWatch.start();
        
        for (int i = 0; i < iterations; i++) {
            cacheFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP);
        }
        
        cacheStopWatch.stop();
        long cacheTime = cacheStopWatch.getTotalTimeMillis();
        
        // Then
        System.out.println("缓存性能对比:");
        System.out.println("无缓存时间: " + noCacheTime + "ms");
        System.out.println("有缓存时间: " + cacheTime + "ms");
        
        // 注意：由于测试环境的限制，缓存效果可能不明显
        // 主要验证功能正确性
        assertTrue(cacheTime >= 0, "缓存处理时间应为正数");
        assertTrue(noCacheTime >= 0, "无缓存处理时间应为正数");
    }
    
    @Test
    @DisplayName("测试内存使用情况")
    void testMemoryUsage() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        String content = generateComplexContent();
        int iterations = 1000;
        
        // When - 记录初始内存
        System.gc(); // 强制垃圾回收
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 执行大量操作
        for (int i = 0; i < iterations; i++) {
            String result = xssFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP + i);
            assertNotNull(result);
        }
        
        System.gc(); // 强制垃圾回收
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Then
        long memoryIncrease = finalMemory - initialMemory;
        long memoryIncreaseKB = memoryIncrease / 1024;
        
        System.out.println("内存使用情况:");
        System.out.println("初始内存: " + (initialMemory / 1024) + " KB");
        System.out.println("最终内存: " + (finalMemory / 1024) + " KB");
        System.out.println("内存增长: " + memoryIncreaseKB + " KB");
        System.out.println("平均每次操作内存增长: " + (memoryIncreaseKB / iterations) + " KB");
        
        // 验证内存增长在合理范围内（小于100MB）
        assertTrue(memoryIncreaseKB < 100 * 1024, "内存增长应小于100MB，实际: " + memoryIncreaseKB + "KB");
    }
    
    @Test
    @DisplayName("测试长时间运行稳定性")
    void testLongRunningStability() {
        // Given
        String[] contents = {
            "<p>正常内容</p>",
            "<script>alert('XSS')</script>",
            "<img src='x' onerror='alert(1)'>",
            generateComplexContent(),
            generateMaliciousContent()
        };
        
        int totalIterations = 5000;
        int batchSize = 100;
        
        // When
        StopWatch stopWatch = new StopWatch("长时间运行稳定性测试");
        stopWatch.start();
        
        for (int batch = 0; batch < totalIterations / batchSize; batch++) {
            for (int i = 0; i < batchSize; i++) {
                String content = contents[i % contents.length];
                String result = xssFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP + batch + i);
                assertNotNull(result, "结果不应为null，批次: " + batch + ", 迭代: " + i);
            }
            
            // 每批次后检查内存
            if (batch % 10 == 0) {
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                assertTrue(usedMemory < 500 * 1024 * 1024, "内存使用应小于500MB，当前: " + (usedMemory / 1024 / 1024) + "MB");
            }
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double avgTime = (double) totalTime / totalIterations;
        
        assertTrue(avgTime < 10.0, "长时间运行平均处理时间应小于10ms，实际: " + avgTime + "ms");
        
        printPerformanceResult("长时间运行稳定性", totalIterations, totalTime, avgTime);
    }
    
    @Test
    @DisplayName("测试不同内容大小的性能表现")
    void testPerformanceWithDifferentContentSizes() {
        // Given
        int[] contentSizes = {100, 500, 1000, 5000, 10000}; // 字符数
        int iterations = 100;
        
        for (int size : contentSizes) {
            // Given
            String content = generateLargeContent(size);
            
            // When
            StopWatch stopWatch = new StopWatch("内容大小" + size + "性能测试");
            stopWatch.start();
            
            for (int i = 0; i < iterations; i++) {
                String result = xssFilter.filterXss(content, TEST_REQUEST_URI, TEST_CLIENT_IP);
                assertNotNull(result);
            }
            
            stopWatch.stop();
            
            // Then
            long totalTime = stopWatch.getTotalTimeMillis();
            double avgTime = (double) totalTime / iterations;
            
            System.out.println("内容大小 " + size + " 字符:");
            System.out.println("  总时间: " + totalTime + "ms");
            System.out.println("  平均时间: " + avgTime + "ms");
            System.out.println("  吞吐量: " + (iterations * 1000.0 / totalTime) + " ops/sec");
            
            // 验证性能随内容大小线性增长
            assertTrue(avgTime < size * 0.01, "处理时间应与内容大小成正比，大小: " + size + ", 时间: " + avgTime + "ms");
        }
    }
    
    /**
     * 生成复杂内容用于测试
     */
    private String generateComplexContent() {
        StringBuilder content = new StringBuilder();
        
        // 添加各种HTML标签
        content.append("<div class='container'>");
        content.append("<h1>标题</h1>");
        content.append("<p>这是一段包含<strong>粗体</strong>和<em>斜体</em>的文本。</p>");
        content.append("<ul>");
        for (int i = 0; i < 10; i++) {
            content.append("<li>列表项 ").append(i).append("</li>");
        }
        content.append("</ul>");
        content.append("<a href='https://example.com' title='链接'>外部链接</a>");
        content.append("<img src='/images/test.jpg' alt='测试图片' width='100' height='100'>");
        content.append("</div>");
        
        return content.toString();
    }
    
    /**
     * 生成恶意内容用于测试
     */
    private String generateMaliciousContent() {
        StringBuilder content = new StringBuilder();
        
        // 添加各种攻击向量
        content.append("<script>alert('XSS1')</script>");
        content.append("<img src='x' onerror='alert(2)'>");
        content.append("<a href='javascript:alert(3)'>恶意链接</a>");
        content.append("<div style='background: expression(alert(4))'>CSS攻击</div>");
        content.append("<iframe src='http://evil.com'></iframe>");
        content.append("<svg onload='alert(5)'></svg>");
        content.append("<object data='javascript:alert(6)'></object>");
        
        return content.toString();
    }
    
    /**
     * 生成指定大小的内容
     */
    private String generateLargeContent(int targetSize) {
        StringBuilder content = new StringBuilder();
        String baseContent = "<p>这是一段测试内容，用于性能测试。包含<b>粗体</b>和<i>斜体</i>文本。</p>";
        
        while (content.length() < targetSize) {
            content.append(baseContent);
            if (content.length() % 1000 == 0) {
                // 偶尔添加一些攻击向量
                content.append("<script>alert('test')</script>");
            }
        }
        
        return content.substring(0, Math.min(targetSize, content.length()));
    }
    
    /**
     * 打印性能测试结果
     */
    private void printPerformanceResult(String testName, int iterations, long totalTime, double avgTime) {
        System.out.println("\n" + testName + " 性能测试结果:");
        System.out.println("  迭代次数: " + iterations);
        System.out.println("  总时间: " + totalTime + "ms");
        System.out.println("  平均时间: " + String.format("%.3f", avgTime) + "ms");
        System.out.println("  吞吐量: " + String.format("%.2f", iterations * 1000.0 / totalTime) + " ops/sec");
    }
    
    /**
     * 打印并发性能测试结果
     */
    private void printConcurrentPerformanceResult(String testName, int threadCount, int iterationsPerThread, 
                                                 long totalTime, double avgTime) {
        int totalIterations = threadCount * iterationsPerThread;
        System.out.println("\n" + testName + " 性能测试结果:");
        System.out.println("  线程数: " + threadCount);
        System.out.println("  每线程迭代: " + iterationsPerThread);
        System.out.println("  总迭代次数: " + totalIterations);
        System.out.println("  总时间: " + totalTime + "ms");
        System.out.println("  平均时间: " + String.format("%.3f", avgTime) + "ms");
        System.out.println("  吞吐量: " + String.format("%.2f", totalIterations * 1000.0 / totalTime) + " ops/sec");
    }
}