package com.myweb.website_core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.SecurityAlertService;
import com.myweb.website_core.application.service.security.audit.SecurityEventService;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.SecurityEventQuery;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.domain.security.dto.SecurityEventStatistics;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import com.myweb.website_core.infrastructure.persistence.repository.SecurityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 安全事件服务测试类
 */
@ExtendWith(MockitoExtension.class)
class SecurityEventServiceTest {
    
    @Mock
    private SecurityEventRepository securityEventRepository;
    
    @Mock
    private SecurityAlertService securityAlertService;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private SecurityEventService securityEventService;
    
    private SecurityEventRequest testRequest;
    private SecurityEvent testEvent;
    
    @BeforeEach
    void setUp() {
//        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        testRequest = SecurityEventRequest.builder()
                .eventType(SecurityEventType.CONTINUOUS_LOGIN_FAILURE)
                .title("登录失败测试")
                .description("测试用户登录失败")
                .userId(1L)
                .username("testuser")
                .sourceIp("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .requestUri("/api/auth/login")
                .requestMethod("POST")
                .sessionId("test-session-id")
                .eventTime(LocalDateTime.now())
                .build();
        
        testEvent = SecurityEvent.builder()
                .id(1L)
                .eventType(SecurityEventType.CONTINUOUS_LOGIN_FAILURE)
                .title("登录失败测试")
                .description("测试用户登录失败")
                .severity(4)
                .userId(1L)
                .username("testuser")
                .sourceIp("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .requestUri("/api/auth/login")
                .requestMethod("POST")
                .sessionId("test-session-id")
                .eventTime(LocalDateTime.now())
                .status("NEW")
                .alerted(false)
                .riskScore(80)
                .relatedEventCount(1)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testRecordEvent_Success() throws Exception {
        // Given
        when(securityEventRepository.countByUserAndTypeInTimeWindow(any(), any(), any(), any()))
                .thenReturn(1L);
        when(securityEventRepository.save(any(SecurityEvent.class))).thenReturn(testEvent);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // When
        CompletableFuture<SecurityEvent> result = securityEventService.recordEvent(testRequest);
        SecurityEvent savedEvent = result.get();
        
        // Then
        assertNotNull(savedEvent);
        assertEquals(testEvent.getId(), savedEvent.getId());
        assertEquals(testEvent.getEventType(), savedEvent.getEventType());
        assertEquals(testEvent.getTitle(), savedEvent.getTitle());
        
        verify(securityEventRepository).save(any(SecurityEvent.class));
        verify(securityAlertService).sendAlert(any(SecurityEvent.class));
    }
    
    @Test
    void testRecordEvents_BatchSuccess() throws Exception {
        // Given
        List<SecurityEventRequest> requests = Arrays.asList(testRequest, testRequest);
        List<SecurityEvent> events = Arrays.asList(testEvent, testEvent);
        
        when(securityEventRepository.countByUserAndTypeInTimeWindow(any(), any(), any(), any()))
                .thenReturn(1L);
        when(securityEventRepository.saveAll(anyList())).thenReturn(events);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // When
        CompletableFuture<List<SecurityEvent>> result = securityEventService.recordEvents(requests);
        List<SecurityEvent> savedEvents = result.get();
        
        // Then
        assertNotNull(savedEvents);
        assertEquals(2, savedEvents.size());
        
        verify(securityEventRepository).saveAll(anyList());
        verify(securityAlertService).sendBatchAlert(anyList());
    }
    
    @Test
    void testFindEvents_WithQuery() {
        // Given
        SecurityEventQuery query = SecurityEventQuery.builder()
                .eventTypes(List.of(SecurityEventType.CONTINUOUS_LOGIN_FAILURE))
                .severities(List.of(4, 5))
                .userId(1L)
                .build();
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<SecurityEvent> expectedPage = new PageImpl<>(List.of(testEvent));
        
        when(securityEventRepository.findByComplexQuery(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expectedPage);
        
        // When
        Page<SecurityEvent> result = securityEventService.findEvents(query, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testEvent.getId(), result.getContent().get(0).getId());
        
        verify(securityEventRepository).findByComplexQuery(
                eq(query.getEventTypes()),
                eq(query.getSeverities()),
                eq(query.getUserId()),
                eq(query.getUsername()),
                eq(query.getSourceIp()),
                eq(query.getStatuses()),
                eq(query.getAlerted()),
                eq(query.getStartTime()),
                eq(query.getEndTime()),
                eq(query.getMinRiskScore()),
                eq(query.getMaxRiskScore()),
                eq(query.getKeyword()),
                eq(pageable)
        );
    }
    
    @Test
    void testGetEventStatistics() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        
        when(securityEventRepository.countByTimeRange(startTime, endTime)).thenReturn(100L);
        when(securityEventRepository.countHighRiskByTimeRange(startTime, endTime)).thenReturn(20L);
        when(securityEventRepository.countMediumRiskByTimeRange(startTime, endTime)).thenReturn(30L);
        when(securityEventRepository.countLowRiskByTimeRange(startTime, endTime)).thenReturn(50L);
        when(securityEventRepository.countUnhandledByTimeRange(startTime, endTime)).thenReturn(15L);
        when(securityEventRepository.countAlertedByTimeRange(startTime, endTime)).thenReturn(25L);
        
        // 修复: 明确指定泛型类型
        List<Object[]> eventTypeStats = List.<Object[]>of(new Object[]{SecurityEventType.CONTINUOUS_LOGIN_FAILURE, 50L});
        List<Object[]> severityStats = List.<Object[]>of(new Object[]{4, 20L});
        List<Object[]> statusStats = List.<Object[]>of(new Object[]{"NEW", 15L});
        List<Object[]> hourlyStats = List.<Object[]>of(new Object[]{10, 5L});
        List<Object[]> ipStats = List.<Object[]>of(new Object[]{"192.168.1.100", 10L});
        List<Object[]> userStats = List.<Object[]>of(new Object[]{"testuser", 5L});
        
        when(securityEventRepository.countByEventTypeInTimeRange(startTime, endTime))
                .thenReturn(eventTypeStats);
        when(securityEventRepository.countBySeverityInTimeRange(startTime, endTime))
                .thenReturn(severityStats);
        when(securityEventRepository.countByStatusInTimeRange(startTime, endTime))
                .thenReturn(statusStats);
        when(securityEventRepository.countByHourInTimeRange(startTime, endTime))
                .thenReturn(hourlyStats);
        
        when(securityEventRepository.countByIpInTimeRange(eq(startTime), eq(endTime), any(Pageable.class)))
                .thenReturn(ipStats);
        when(securityEventRepository.countByUserInTimeRange(eq(startTime), eq(endTime), any(Pageable.class)))
                .thenReturn(userStats);
        
        when(securityEventRepository.getAverageRiskScoreInTimeRange(startTime, endTime)).thenReturn(75.5);
        when(securityEventRepository.getMaxRiskScoreInTimeRange(startTime, endTime)).thenReturn(95);
        
        // 上一周期数据
        when(securityEventRepository.countByTimeRange(any(LocalDateTime.class), eq(startTime))).thenReturn(80L);
        
        // When
        SecurityEventStatistics statistics = securityEventService.getEventStatistics(startTime, endTime);
        
        // Then
        assertNotNull(statistics);
        assertEquals(100L, statistics.getTotalEvents());
        assertEquals(20L, statistics.getHighRiskEvents());
        assertEquals(30L, statistics.getMediumRiskEvents());
        assertEquals(50L, statistics.getLowRiskEvents());
        assertEquals(15L, statistics.getUnhandledEvents());
        assertEquals(25L, statistics.getAlertedEvents());
        assertEquals(75.5, statistics.getAverageRiskScore());
        assertEquals(95, statistics.getMaxRiskScore());
        assertEquals(85.0, statistics.getHandlingRate()); // (100-15)/100*100
        assertEquals(25.0, statistics.getAlertRate()); // 25/100*100
        assertEquals(25.0, statistics.getTrendPercentage()); // (100-80)/80*100
        
        assertNotNull(statistics.getEventTypeStats());
        assertNotNull(statistics.getSeverityStats());
        assertNotNull(statistics.getStatusStats());
        assertNotNull(statistics.getHourlyStats());
        assertNotNull(statistics.getIpStats());
        assertNotNull(statistics.getUserStats());
    }
    
    @Test
    void testHandleEvent_Success() {
        // Given
        Long eventId = 1L;
        String handledBy = "admin";
        String handleNotes = "已处理";
        SecurityEvent.Status status = SecurityEvent.Status.RESOLVED;
        
        when(securityEventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(securityEventRepository.save(any(SecurityEvent.class))).thenReturn(testEvent);
        
        // When
        securityEventService.handleEvent(eventId, handledBy, handleNotes, status);
        
        // Then
        verify(securityEventRepository).findById(eventId);
        verify(securityEventRepository).save(any(SecurityEvent.class));
    }
    
    @Test
    void testHandleEvent_EventNotFound() {
        // Given
        Long eventId = 999L;
        when(securityEventRepository.findById(eventId)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            securityEventService.handleEvent(eventId, "admin", "test", SecurityEvent.Status.RESOLVED);
        });
        
        assertTrue(exception.getMessage().contains("安全事件不存在"));
        verify(securityEventRepository).findById(eventId);
        verify(securityEventRepository, never()).save(any());
    }
    
    @Test
    void testDetectAnomalousPattern_UserAnomaly() {
        // Given
        Long userId = 1L;
        String sourceIp = "192.168.1.100";
        SecurityEventType eventType = SecurityEventType.CONTINUOUS_LOGIN_FAILURE;
        
        when(securityEventRepository.countByUserAndTypeInTimeWindow(
                eq(userId), eq(eventType), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15L); // 超过阈值10
        
        // When
        boolean result = securityEventService.detectAnomalousPattern(userId, sourceIp, eventType);
        
        // Then
        assertTrue(result);
        verify(securityEventRepository).countByUserAndTypeInTimeWindow(
                eq(userId), eq(eventType), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void testDetectAnomalousPattern_IpAnomaly() {
        // Given
        Long userId = 1L;
        String sourceIp = "192.168.1.100";
        SecurityEventType eventType = SecurityEventType.CONTINUOUS_LOGIN_FAILURE;
        
        when(securityEventRepository.countByUserAndTypeInTimeWindow(
                eq(userId), eq(eventType), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L); // 未超过用户阈值
        
        when(securityEventRepository.countByIpInTimeWindow(
                eq(sourceIp), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(25L); // 超过IP阈值10
        
        // When
        boolean result = securityEventService.detectAnomalousPattern(userId, sourceIp, eventType);
        
        // Then
        assertTrue(result);
        verify(securityEventRepository).countByIpInTimeWindow(
                eq(sourceIp), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void testDetectAnomalousPattern_NoAnomaly() {
        // Given
        Long userId = 1L;
        String sourceIp = "192.168.1.100";
        SecurityEventType eventType = SecurityEventType.CONTINUOUS_LOGIN_FAILURE;
        
        when(securityEventRepository.countByUserAndTypeInTimeWindow(
                eq(userId), eq(eventType), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L); // 未超过阈值
        
        when(securityEventRepository.countByIpInTimeWindow(
                eq(sourceIp), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L); // 未超过阈值
        
        // When
        boolean result = securityEventService.detectAnomalousPattern(userId, sourceIp, eventType);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testCleanupExpiredEvents() {
        // Given
        doNothing().when(securityEventRepository).deleteEventsBeforeTime(any(LocalDateTime.class));
        
        // When
        securityEventService.cleanupExpiredEvents();
        
        // Then
        verify(securityEventRepository).deleteEventsBeforeTime(any(LocalDateTime.class));
    }
    
    @Test
    void testCheckUnalertedHighRiskEvents() {
        // Given
        List<SecurityEvent> unalertedEvents = List.of(testEvent);
        when(securityEventRepository.findUnalertedHighRiskEvents()).thenReturn(unalertedEvents);
        when(securityEventRepository.saveAll(anyList())).thenReturn(unalertedEvents);
        
        // When
        securityEventService.checkUnalertedHighRiskEvents();
        
        // Then
        verify(securityEventRepository).findUnalertedHighRiskEvents();
        verify(securityAlertService).sendBatchAlert(unalertedEvents);
        verify(securityEventRepository).saveAll(anyList());
    }
    
    @Test
    void testCheckUnalertedHighRiskEvents_NoEvents() {
        // Given
        // 修复: 明确指定泛型类型
        List<SecurityEvent> emptyList = List.of();
        when(securityEventRepository.findUnalertedHighRiskEvents()).thenReturn(emptyList);
        
        // When
        securityEventService.checkUnalertedHighRiskEvents();
        
        // Then
        verify(securityEventRepository).findUnalertedHighRiskEvents();
        verify(securityAlertService, never()).sendBatchAlert(anyList());
        verify(securityEventRepository, never()).saveAll(anyList());
    }
}