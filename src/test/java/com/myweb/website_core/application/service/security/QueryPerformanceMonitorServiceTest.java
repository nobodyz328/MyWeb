package com.myweb.website_core.application.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 查询性能监控服务测试
 * <p>
 * 测试查询性能监控的功能：
 * 1. 查询执行时间统计
 * 2. 慢查询检测
 * 3. 性能统计分析
 * 4. 性能问题检查
 * <p>
 * 符合需求：5.7 - 查询性能监控
 * 
 * @author MyWeb
 * @version 1.0
 */
@DisplayName("查询性能监控服务测试")
class QueryPerformanceMonitorServiceTest {
    
    private QueryPerformanceMonitorService queryPerformanceMonitorService;
    
    @BeforeEach
    void setUp() {
        queryPerformanceMonitorService = new QueryPerformanceMonitorService();
    }
    
    @Test
    @DisplayName("测试查询性能监控器 - 成功执行")
    void testQueryPerformanceMonitor_SuccessfulExecution() throws InterruptedException {
        // Given
        String queryType = "TEST_QUERY";
        String description = "测试查询描述";
        
        // When
        QueryPerformanceMonitorService.QueryPerformanceMonitor monitor = 
            queryPerformanceMonitorService.startMonitoring(queryType, description);
        
        // 模拟查询执行时间
        Thread.sleep(100);
        
        monitor.finish(5, true);
        
        // Then
        assertEquals(queryType, monitor.getQueryType());
        assertEquals(description, monitor.getQueryDescription());
        assertNotNull(monitor.getStartTime());
        
        // 验证统计数据
        QueryPerformanceMonitorService.QueryStatistics stats = 
            queryPerformanceMonitorService.getQueryStatistics(queryType);
        assertNotNull(stats);
        assertEquals(1, stats.getTotalExecutions());
        assertEquals(1, stats.getSuccessfulExecutions());
        assertEquals(5, stats.getTotalResultCount());
        assertTrue(stats.getAvgExecutionTime() >= 100);
        assertEquals(100.0, stats.getSuccessRate());
    }
    
    @Test
    @DisplayName("测试查询性能监控器 - 执行失败")
    void testQueryPerformanceMonitor_FailedExecution() throws InterruptedException {
        // Given
        String queryType = "FAILED_QUERY";
        String description = "失败的查询";
        
        // When
        QueryPerformanceMonitorService.QueryPerformanceMonitor monitor = 
            queryPerformanceMonitorService.startMonitoring(queryType, description);
        
        Thread.sleep(50);
        
        monitor.finishWithError(new RuntimeException("查询执行失败"));
        
        // Then
        QueryPerformanceMonitorService.QueryStatistics stats = 
            queryPerformanceMonitorService.getQueryStatistics(queryType);
        assertNotNull(stats);
        assertEquals(1, stats.getTotalExecutions());
        assertEquals(0, stats.getSuccessfulExecutions());
        assertEquals(0, stats.getTotalResultCount());
        assertTrue(stats.getAvgExecutionTime() >= 50);
        assertEquals(0.0, stats.getSuccessRate());
    }
    
    @Test
    @DisplayName("测试慢查询检测")
    void testSlowQueryDetection() {
        // Given
        String queryType = "SLOW_QUERY";
        
        // When - 记录一个慢查询（超过1000ms阈值）
        queryPerformanceMonitorService.recordQueryExecution(queryType, 1500, 10, true);
        
        // Then
        QueryPerformanceMonitorService.GlobalStatistics globalStats = 
            queryPerformanceMonitorService.getGlobalStatistics();
        
        assertEquals(1, globalStats.getTotalQueries());
        assertEquals(1, globalStats.getTotalSlowQueries());
        assertEquals(1500, globalStats.getTotalExecutionTime());
        assertEquals(1500.0, globalStats.getAvgExecutionTime());
        assertEquals(100.0, globalStats.getSlowQueryRate());
    }
    
    @Test
    @DisplayName("测试多个查询类型的统计")
    void testMultipleQueryTypesStatistics() {
        // Given
        String queryType1 = "QUERY_TYPE_1";
        String queryType2 = "QUERY_TYPE_2";
        
        // When
        queryPerformanceMonitorService.recordQueryExecution(queryType1, 200, 5, true);
        queryPerformanceMonitorService.recordQueryExecution(queryType1, 300, 8, true);
        queryPerformanceMonitorService.recordQueryExecution(queryType2, 150, 3, true);
        queryPerformanceMonitorService.recordQueryExecution(queryType2, 400, 0, false);
        
        // Then
        Map<String, QueryPerformanceMonitorService.QueryStatistics> allStats = 
            queryPerformanceMonitorService.getAllQueryStatistics();
        
        assertEquals(2, allStats.size());
        
        // 验证查询类型1的统计
        QueryPerformanceMonitorService.QueryStatistics stats1 = allStats.get(queryType1);
        assertNotNull(stats1);
        assertEquals(2, stats1.getTotalExecutions());
        assertEquals(2, stats1.getSuccessfulExecutions());
        assertEquals(13, stats1.getTotalResultCount());
        assertEquals(250.0, stats1.getAvgExecutionTime());
        assertEquals(100.0, stats1.getSuccessRate());
        assertEquals(6.5, stats1.getAvgResultCount());
        
        // 验证查询类型2的统计
        QueryPerformanceMonitorService.QueryStatistics stats2 = allStats.get(queryType2);
        assertNotNull(stats2);
        assertEquals(2, stats2.getTotalExecutions());
        assertEquals(1, stats2.getSuccessfulExecutions());
        assertEquals(3, stats2.getTotalResultCount());
        assertEquals(275.0, stats2.getAvgExecutionTime());
        assertEquals(50.0, stats2.getSuccessRate());
        assertEquals(1.5, stats2.getAvgResultCount());
    }
    
    @Test
    @DisplayName("测试全局统计信息")
    void testGlobalStatistics() {
        // Given
        queryPerformanceMonitorService.recordQueryExecution("QUERY1", 100, 5, true);
        queryPerformanceMonitorService.recordQueryExecution("QUERY2", 200, 3, true);
        queryPerformanceMonitorService.recordQueryExecution("QUERY3", 1200, 10, true); // 慢查询
        queryPerformanceMonitorService.recordQueryExecution("QUERY4", 150, 0, false); // 失败查询
        
        // When
        QueryPerformanceMonitorService.GlobalStatistics globalStats = 
            queryPerformanceMonitorService.getGlobalStatistics();
        
        // Then
        assertEquals(4, globalStats.getTotalQueries());
        assertEquals(1, globalStats.getTotalSlowQueries());
        assertEquals(1650, globalStats.getTotalExecutionTime());
        assertEquals(412.5, globalStats.getAvgExecutionTime());
        assertEquals(25.0, globalStats.getSlowQueryRate());
    }
    
    @Test
    @DisplayName("测试性能问题检查")
    void testPerformanceCheck() {
        // Given - 添加一些有性能问题的查询
        queryPerformanceMonitorService.recordQueryExecution("SLOW_QUERY", 1500, 5, true);
        queryPerformanceMonitorService.recordQueryExecution("SLOW_QUERY", 2000, 3, true);
        queryPerformanceMonitorService.recordQueryExecution("NORMAL_QUERY", 100, 10, true);
        
        // When
        QueryPerformanceMonitorService.PerformanceCheckResult checkResult = 
            queryPerformanceMonitorService.checkPerformance();
        
        // Then
        assertTrue(checkResult.hasIssue());
        String issueDescription = checkResult.getIssueDescription();
        assertTrue(issueDescription.contains("慢查询率过高") || 
                  issueDescription.contains("平均执行时间过长") ||
                  issueDescription.contains("查询类型"));
    }
    
    @Test
    @DisplayName("测试性能检查 - 无问题")
    void testPerformanceCheck_NoIssues() {
        // Given - 添加一些正常的查询
        queryPerformanceMonitorService.recordQueryExecution("NORMAL_QUERY1", 100, 5, true);
        queryPerformanceMonitorService.recordQueryExecution("NORMAL_QUERY2", 200, 3, true);
        queryPerformanceMonitorService.recordQueryExecution("NORMAL_QUERY3", 150, 8, true);
        
        // When
        QueryPerformanceMonitorService.PerformanceCheckResult checkResult = 
            queryPerformanceMonitorService.checkPerformance();
        
        // Then
        assertFalse(checkResult.hasIssue());
        assertTrue(checkResult.getIssueDescription().isEmpty());
    }
    
    @Test
    @DisplayName("测试统计数据重置")
    void testResetStatistics() {
        // Given
        queryPerformanceMonitorService.recordQueryExecution("QUERY1", 100, 5, true);
        queryPerformanceMonitorService.recordQueryExecution("QUERY2", 200, 3, true);
        
        // 验证统计数据存在
        QueryPerformanceMonitorService.GlobalStatistics statsBefore = 
            queryPerformanceMonitorService.getGlobalStatistics();
        assertEquals(2, statsBefore.getTotalQueries());
        
        // When
        queryPerformanceMonitorService.resetStatistics();
        
        // Then
        QueryPerformanceMonitorService.GlobalStatistics statsAfter = 
            queryPerformanceMonitorService.getGlobalStatistics();
        assertEquals(0, statsAfter.getTotalQueries());
        assertEquals(0, statsAfter.getTotalSlowQueries());
        assertEquals(0, statsAfter.getTotalExecutionTime());
        assertEquals(0.0, statsAfter.getAvgExecutionTime());
        assertEquals(0.0, statsAfter.getSlowQueryRate());
        
        Map<String, QueryPerformanceMonitorService.QueryStatistics> allStats = 
            queryPerformanceMonitorService.getAllQueryStatistics();
        assertTrue(allStats.isEmpty());
    }
    
    @Test
    @DisplayName("测试查询统计信息的最小最大执行时间")
    void testQueryStatistics_MinMaxExecutionTime() {
        // Given
        String queryType = "TEST_QUERY";
        
        // When
        queryPerformanceMonitorService.recordQueryExecution(queryType, 100, 5, true);
        queryPerformanceMonitorService.recordQueryExecution(queryType, 300, 8, true);
        queryPerformanceMonitorService.recordQueryExecution(queryType, 50, 3, true);
        queryPerformanceMonitorService.recordQueryExecution(queryType, 200, 10, true);
        
        // Then
        QueryPerformanceMonitorService.QueryStatistics stats = 
            queryPerformanceMonitorService.getQueryStatistics(queryType);
        
        assertNotNull(stats);
        assertEquals(50, stats.getMinExecutionTime());
        assertEquals(300, stats.getMaxExecutionTime());
        assertEquals(162.5, stats.getAvgExecutionTime());
        assertEquals(4, stats.getTotalExecutions());
        assertEquals(26, stats.getTotalResultCount());
        assertEquals(6.5, stats.getAvgResultCount());
    }
    
    @Test
    @DisplayName("测试监控器的便捷方法")
    void testMonitorConvenienceMethods() throws InterruptedException {
        // Given
        String queryType = "CONVENIENCE_TEST";
        String description = "便捷方法测试";
        
        // When - 测试finish(int)方法
        QueryPerformanceMonitorService.QueryPerformanceMonitor monitor1 = 
            queryPerformanceMonitorService.startMonitoring(queryType, description);
        Thread.sleep(50);
        monitor1.finish(5); // 默认成功
        
        // When - 测试finishWithError方法
        QueryPerformanceMonitorService.QueryPerformanceMonitor monitor2 = 
            queryPerformanceMonitorService.startMonitoring(queryType, description);
        Thread.sleep(30);
        monitor2.finishWithError(new RuntimeException("测试异常"));
        
        // Then
        QueryPerformanceMonitorService.QueryStatistics stats = 
            queryPerformanceMonitorService.getQueryStatistics(queryType);
        
        assertNotNull(stats);
        assertEquals(2, stats.getTotalExecutions());
        assertEquals(1, stats.getSuccessfulExecutions());
        assertEquals(5, stats.getTotalResultCount());
        assertEquals(50.0, stats.getSuccessRate());
    }
}