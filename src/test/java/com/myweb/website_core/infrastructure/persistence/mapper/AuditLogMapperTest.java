package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuditLogMapper测试类
 * 
 * 测试MyBatis映射器的功能，特别是复杂查询的null参数处理
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogMapperTest {
    
    @Autowired
    private AuditLogMapper auditLogMapper;
    
    @Autowired
    private AuditLogMapperService auditLogMapperService;
    
    /**
     * 测试失败登录查询
     */
    @Test
    void testFindFailedLoginAttempts() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Page<AuditLog> result = auditLogMapperService.findFailedLoginAttempts(
                    AuditOperation.USER_LOGIN_FAILURE, startTime, endTime, PageRequest.of(0, 10));
            assertNotNull(result);
        });
    }
    
    /**
     * 测试可疑活动查询
     */
    @Test
    void testFindSuspiciousActivitiesByIp() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            List<AuditLog> result = auditLogMapperService.findSuspiciousActivitiesByIp(
                    "192.168.1.1", startTime, endTime, 3);
            assertNotNull(result);
        });
    }
    
    /**
     * 测试高风险操作查询
     */
    @Test
    void testFindHighRiskOperations() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Page<AuditLog> result = auditLogMapperService.findHighRiskOperations(
                    4, startTime, endTime, PageRequest.of(0, 10));
            assertNotNull(result);
        });
    }
    
    /**
     * 测试未处理安全事件查询
     */
    @Test
    void testFindUnprocessedSecurityEvents() {
        // 测试查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Page<AuditLog> result = auditLogMapperService.findUnprocessedSecurityEvents(
                    PageRequest.of(0, 10));
            assertNotNull(result);
        });
    }
    
    /**
     * 测试统计查询
     */
    @Test
    void testStatisticalQueries() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试各种统计查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Map<String, Long> operationStats = auditLogMapperService.countOperationsByType(startTime, endTime);
            assertNotNull(operationStats);
            
            Map<String, Long> userStats = auditLogMapperService.countOperationsByUser(startTime, endTime, 10);
            assertNotNull(userStats);
            
            Map<String, Long> ipStats = auditLogMapperService.countOperationsByIp(startTime, endTime, 10);
            assertNotNull(ipStats);
            
            Map<Integer, Long> hourlyStats = auditLogMapperService.countOperationsByHour(startTime, endTime);
            assertNotNull(hourlyStats);
        });
    }
    
    /**
     * 测试用户相关查询
     */
    @Test
    void testUserRelatedQueries() {
        // 测试用户相关查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            var latestLogin = auditLogMapperService.findLatestLoginByUserId(1L);
            // 可能为空，如果没有数据
            
            List<AuditLog> loginHistory = auditLogMapperService.findLoginHistoryByUserId(1L, 10);
            assertNotNull(loginHistory);
        });
    }
    
    /**
     * 测试资源历史查询
     */
    @Test
    void testResourceHistoryQuery() {
        // 测试资源历史查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Page<AuditLog> result = auditLogMapperService.findResourceHistory(
                    "POST", 1L, PageRequest.of(0, 10));
            assertNotNull(result);
        });
    }
    
    /**
     * 测试标签查询
     */
    @Test
    void testFindByTag() {
        // 测试标签查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Page<AuditLog> result = auditLogMapperService.findByTag(
                    "security", PageRequest.of(0, 10));
            assertNotNull(result);
        });
    }
    
    /**
     * 测试慢操作查询
     */
    @Test
    void testFindSlowOperations() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试慢操作查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Page<AuditLog> result = auditLogMapperService.findSlowOperations(
                    1000L, startTime, endTime, PageRequest.of(0, 10));
            assertNotNull(result);
        });
    }
    
    /**
     * 测试活跃统计查询
     */
    @Test
    void testActiveStatistics() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试活跃统计查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            long activeUsers = auditLogMapperService.countActiveUsers(startTime, endTime);
            assertTrue(activeUsers >= 0);
            
            long activeIPs = auditLogMapperService.countActiveIPs(startTime, endTime);
            assertTrue(activeIPs >= 0);
        });
    }
}