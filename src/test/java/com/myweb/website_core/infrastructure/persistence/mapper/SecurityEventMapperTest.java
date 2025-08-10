package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.SecurityEventQuery;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityEventMapper测试类
 * 
 * 测试MyBatis映射器的功能，特别是复杂查询的null参数处理
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityEventMapperTest {
    
    @Autowired
    private SecurityEventMapper securityEventMapper;
    
    @Autowired
    private SecurityEventMapperService securityEventMapperService;
    
    /**
     * 测试复杂查询 - 所有参数为null的情况
     */
    @Test
    void testFindByComplexQueryWithNullParameters() {
        // 创建查询条件，所有参数都为null
        SecurityEventQuery query = SecurityEventQuery.builder().build();
        
        // 执行查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            List<SecurityEvent> events = securityEventMapper.findByComplexQuery(query, 0, 10);
            assertNotNull(events);
        });
    }
    
    /**
     * 测试复杂查询 - 部分参数为null的情况
     */
    @Test
    void testFindByComplexQueryWithPartialNullParameters() {
        // 创建查询条件，部分参数为null
        SecurityEventQuery query = SecurityEventQuery.builder()
                //.eventTypes(List.of(SecurityEventType.LOGIN_FAILED))
                .userId(null) // null参数
                .username(null) // null参数
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(null) // null参数
                .build();
        
        // 执行查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            List<SecurityEvent> events = securityEventMapper.findByComplexQuery(query, 0, 10);
            assertNotNull(events);
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
            Long totalEvents = securityEventMapperService.countByTimeRange(startTime, endTime);
            assertNotNull(totalEvents);
            
            Long highRiskEvents = securityEventMapperService.countHighRiskByTimeRange(startTime, endTime);
            assertNotNull(highRiskEvents);
            
            var eventTypeStats = securityEventMapperService.countByEventTypeInTimeRange(startTime, endTime);
            assertNotNull(eventTypeStats);
            
            var severityStats = securityEventMapperService.countBySeverityInTimeRange(startTime, endTime);
            assertNotNull(severityStats);
        });
    }
    
    /**
     * 测试用户和IP相关查询
     */
    @Test
    void testUserAndIpQueries() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试用户和IP相关查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Long userEventCount = securityEventMapper.countByUserAndTypeInTimeWindow(
                    1L, SecurityEventType.CONTINUOUS_LOGIN_FAILURE, startTime, endTime);
            assertNotNull(userEventCount);
            
            Long ipEventCount = securityEventMapper.countByIpInTimeWindow(
                    "192.168.1.1", startTime, endTime);
            assertNotNull(ipEventCount);
        });
    }
    
    /**
     * 测试风险评分查询
     */
    @Test
    void testRiskScoreQueries() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 测试风险评分查询，不应该抛出异常
        assertDoesNotThrow(() -> {
            Double avgRiskScore = securityEventMapperService.getAverageRiskScoreInTimeRange(startTime, endTime);
            // 可能为null，如果没有数据
            
            Integer maxRiskScore = securityEventMapperService.getMaxRiskScoreInTimeRange(startTime, endTime);
            // 可能为null，如果没有数据
        });
    }
}