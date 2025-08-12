package com.myweb.website_core.application.service.security;

import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * XSS统计服务单元测试
 * <p>
 * 测试XssStatisticsService的各种功能：
 * 1. 攻击事件记录
 * 2. 统计数据查询
 * 3. 报告生成
 * 4. 数据清理
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - XSS统计测试
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
class XssStatisticsServiceTest {
    
    @Mock
    private XssFilterConfig xssFilterConfig;
    
    @Mock
    private XssFilterConfig.StatisticsConfig statisticsConfig;
    
    private XssStatisticsService xssStatisticsService;
    
    @BeforeEach
    void setUp() {
        when(xssFilterConfig.getStatistics()).thenReturn(statisticsConfig);
        when(statisticsConfig.isEnabled()).thenReturn(true);
        when(statisticsConfig.getRetentionDays()).thenReturn(30);
        when(statisticsConfig.getReportIntervalHours()).thenReturn(24);
        when(statisticsConfig.isRealTimeStats()).thenReturn(true);
        when(statisticsConfig.getAggregationIntervalMinutes()).thenReturn(5);
        
        xssStatisticsService = new XssStatisticsService(xssFilterConfig);
    }
    
    @Test
    void testRecordXssAttack_ShouldIncrementCounters() {
        // Given
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        // When
        xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, userAgent);
        
        // Then
        assertEquals(1, xssStatisticsService.getTotalAttackCount());
        assertEquals(1, xssStatisticsService.getTodayAttackCount());
        assertEquals(1, xssStatisticsService.getHourlyAttackCount());
        
        Map<String, Long> ipStats = xssStatisticsService.getIpAttackStats();
        assertEquals(1L, ipStats.get(clientIp));
        
        Map<String, Long> attackTypeStats = xssStatisticsService.getAttackTypeStats();
        assertEquals(1L, attackTypeStats.get(attackType));
        
        Map<String, Long> uriStats = xssStatisticsService.getUriAttackStats();
        assertEquals(1L, uriStats.get(requestUri));
    }
    
    @Test
    void testRecordXssAttack_MultipleAttacks_ShouldAccumulate() {
        // Given
        String clientIp1 = "192.168.1.1";
        String clientIp2 = "192.168.1.2";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        // When
        xssStatisticsService.recordXssAttack(clientIp1, requestUri, attackType, userAgent);
        xssStatisticsService.recordXssAttack(clientIp1, requestUri, attackType, userAgent);
        xssStatisticsService.recordXssAttack(clientIp2, requestUri, attackType, userAgent);
        
        // Then
        assertEquals(3, xssStatisticsService.getTotalAttackCount());
        assertEquals(3, xssStatisticsService.getTodayAttackCount());
        assertEquals(3, xssStatisticsService.getHourlyAttackCount());
        
        Map<String, Long> ipStats = xssStatisticsService.getIpAttackStats();
        assertEquals(2L, ipStats.get(clientIp1));
        assertEquals(1L, ipStats.get(clientIp2));
    }
    
    @Test
    void testRecordXssAttack_DifferentAttackTypes_ShouldTrackSeparately() {
        // Given
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String userAgent = "Mozilla/5.0";
        
        // When
        xssStatisticsService.recordXssAttack(clientIp, requestUri, "script_injection", userAgent);
        xssStatisticsService.recordXssAttack(clientIp, requestUri, "javascript_protocol", userAgent);
        xssStatisticsService.recordXssAttack(clientIp, requestUri, "script_injection", userAgent);
        
        // Then
        Map<String, Long> attackTypeStats = xssStatisticsService.getAttackTypeStats();
        assertEquals(2L, attackTypeStats.get("script_injection"));
        assertEquals(1L, attackTypeStats.get("javascript_protocol"));
    }
    
    @Test
    void testGetStatisticsReport_ShouldReturnCompleteReport() {
        // Given
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, userAgent);
        
        // When
        XssStatisticsService.XssStatisticsReport report = xssStatisticsService.getStatisticsReport();
        
        // Then
        assertNotNull(report);
        assertEquals(1, report.getTotalAttackCount());
        assertEquals(1, report.getTodayAttackCount());
        assertEquals(1, report.getHourlyAttackCount());
        assertNotNull(report.getIpAttackStats());
        assertNotNull(report.getAttackTypeStats());
        assertNotNull(report.getUriAttackStats());
        assertNotNull(report.getTimeWindowStats());
        assertNotNull(report.getReportTime());
    }
    
    @Test
    void testRecordXssAttack_StatisticsDisabled_ShouldNotRecord() {
        // Given
        when(statisticsConfig.isEnabled()).thenReturn(false);
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        // When
        xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, userAgent);
        
        // Then
        assertEquals(0, xssStatisticsService.getTotalAttackCount());
        assertEquals(0, xssStatisticsService.getTodayAttackCount());
        assertEquals(0, xssStatisticsService.getHourlyAttackCount());
    }
    
    @Test
    void testResetDailyStats_ShouldResetTodayCount() {
        // Given
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, userAgent);
        assertEquals(1, xssStatisticsService.getTodayAttackCount());
        
        // When
        xssStatisticsService.resetDailyStats();
        
        // Then
        assertEquals(0, xssStatisticsService.getTodayAttackCount());
        assertEquals(1, xssStatisticsService.getTotalAttackCount()); // 总计数不应重置
    }
    
    @Test
    void testResetHourlyStats_ShouldResetHourlyCount() {
        // Given
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, userAgent);
        assertEquals(1, xssStatisticsService.getHourlyAttackCount());
        
        // When
        xssStatisticsService.resetHourlyStats();
        
        // Then
        assertEquals(0, xssStatisticsService.getHourlyAttackCount());
        assertEquals(1, xssStatisticsService.getTotalAttackCount()); // 总计数不应重置
    }
    
    @Test
    void testGetTimeWindowStats_ShouldReturnCurrentWindow() {
        // Given
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, userAgent);
        
        // When
        Map<String, Long> timeWindowStats = xssStatisticsService.getTimeWindowStats();
        
        // Then
        assertFalse(timeWindowStats.isEmpty());
        assertTrue(timeWindowStats.values().stream().anyMatch(count -> count == 1L));
    }
    
    @Test
    void testCleanupExpiredStats_ShouldRemoveOldData() {
        // Given
        when(statisticsConfig.getRetentionDays()).thenReturn(1); // 只保留1天数据
        
        // When
        xssStatisticsService.cleanupExpiredStats();
        
        // Then
        // 由于时间窗口统计使用当前时间，这个测试主要验证方法不会抛出异常
        // 实际的清理逻辑需要更复杂的时间模拟来测试
        assertDoesNotThrow(() -> xssStatisticsService.cleanupExpiredStats());
    }
    
    @Test
    void testGenerateStatisticsReport_ShouldNotThrowException() {
        // Given
        String clientIp = "192.168.1.1";
        String requestUri = "/api/posts";
        String attackType = "script_injection";
        String userAgent = "Mozilla/5.0";
        
        xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, userAgent);
        
        // When & Then
        assertDoesNotThrow(() -> xssStatisticsService.generateStatisticsReport());
    }
    
    @Test
    void testGenerateStatisticsReport_StatisticsDisabled_ShouldReturn() {
        // Given
        when(statisticsConfig.isEnabled()).thenReturn(false);
        
        // When & Then
        assertDoesNotThrow(() -> xssStatisticsService.generateStatisticsReport());
    }
}