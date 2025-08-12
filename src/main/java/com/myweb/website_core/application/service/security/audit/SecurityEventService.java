package com.myweb.website_core.application.service.security.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.common.constant.SecurityConstants;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.SecurityEventQuery;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.domain.security.dto.SecurityEventStatistics;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import com.myweb.website_core.infrastructure.persistence.repository.SecurityEventRepository;
import com.myweb.website_core.infrastructure.persistence.mapper.SecurityEventMapper;
import com.myweb.website_core.infrastructure.persistence.mapper.SecurityEventMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 安全事件监控服务
 * <p>
 * 负责安全事件的记录、监控、告警和统计分析
 * 符合GB/T 22239-2019二级等保要求的安全审计和入侵防范机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventService {
    
    private final SecurityEventRepository securityEventRepository;
    private final SecurityEventMapper securityEventMapper;
    private final SecurityEventMapperService securityEventMapperService;
    private final SecurityAlertService securityAlertService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 查询安全事件
     * 
     * @param query 查询条件
     * @param pageable 分页参数
     * @return 安全事件分页结果
     */
    public Page<SecurityEvent> findEvents(SecurityEventQuery query, Pageable pageable) {
        log.debug("查询安全事件: query={}", query);
        
        return securityEventMapperService.findByComplexQuery(query, pageable);
    }
    
    /**
     * 获取安全事件统计信息
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    public SecurityEventStatistics getEventStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("获取安全事件统计: startTime={}, endTime={}", startTime, endTime);
        
        // 基础统计 - 使用MyBatis避免null参数问题
        Long totalEvents = securityEventMapperService.countByTimeRange(startTime, endTime);
        Long highRiskEvents = securityEventMapperService.countHighRiskByTimeRange(startTime, endTime);
        Long mediumRiskEvents = securityEventMapperService.countMediumRiskByTimeRange(startTime, endTime);
        Long lowRiskEvents = securityEventMapperService.countLowRiskByTimeRange(startTime, endTime);
        Long unhandledEvents = securityEventMapperService.countUnhandledByTimeRange(startTime, endTime);
        Long alertedEvents = securityEventMapperService.countAlertedByTimeRange(startTime, endTime);
        
        // 分类统计 - 使用MyBatis处理复杂统计查询
        Map<String, Long> eventTypeStats = securityEventMapperService.countByEventTypeInTimeRange(startTime, endTime);
        Map<Integer, Long> severityStats = securityEventMapperService.countBySeverityInTimeRange(startTime, endTime);
        Map<String, Long> statusStats = securityEventMapperService.countByStatusInTimeRange(startTime, endTime);
        Map<Integer, Long> hourlyStats = securityEventMapperService.countByHourInTimeRange(startTime, endTime);
        
        // Top 10统计
        Map<String, Long> ipStats = securityEventMapperService.countByIpInTimeRange(startTime, endTime, 10);
        Map<String, Long> userStats = securityEventMapperService.countByUserInTimeRange(startTime, endTime, 10);
        
        // 风险评分统计
        Double averageRiskScore = securityEventMapperService.getAverageRiskScoreInTimeRange(startTime, endTime);
        Integer maxRiskScore = securityEventMapperService.getMaxRiskScoreInTimeRange(startTime, endTime);
        
        // 计算比率
        Double handlingRate = totalEvents > 0 ? ((totalEvents - unhandledEvents) * 100.0) / totalEvents : 0.0;
        Double alertRate = totalEvents > 0 ? (alertedEvents * 100.0) / totalEvents : 0.0;
        
        // 趋势分析（与上一周期对比）
        Duration period = Duration.between(startTime, endTime);
        LocalDateTime prevStartTime = startTime.minus(period);
        Long prevTotalEvents = securityEventMapperService.countByTimeRange(prevStartTime, startTime);
        Double trendPercentage = prevTotalEvents > 0 ? 
                ((totalEvents - prevTotalEvents) * 100.0) / prevTotalEvents : 0.0;
        
        return SecurityEventStatistics.builder()
                .startTime(startTime)
                .endTime(endTime)
                .totalEvents(totalEvents)
                .highRiskEvents(highRiskEvents)
                .mediumRiskEvents(mediumRiskEvents)
                .lowRiskEvents(lowRiskEvents)
                .unhandledEvents(unhandledEvents)
                .alertedEvents(alertedEvents)
                .eventTypeStats(eventTypeStats)
                .severityStats(severityStats)
                .statusStats(statusStats)
                .hourlyStats(hourlyStats)
                .ipStats(ipStats)
                .userStats(userStats)
                .averageRiskScore(averageRiskScore)
                .maxRiskScore(maxRiskScore)
                .handlingRate(handlingRate)
                .alertRate(alertRate)
                .trendPercentage(trendPercentage)
                .build();
    }
    
    /**
     * 处理安全事件
     * 
     * @param eventId 事件ID
     * @param handledBy 处理人
     * @param handleNotes 处理备注
     * @param status 处理状态
     */
    @Transactional
    public void handleEvent(Long eventId, String handledBy, String handleNotes, SecurityEvent.Status status) {
        SecurityEvent event = securityEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("安全事件不存在: " + eventId));
        
        event.setHandleInfo(handledBy, handleNotes, status);
        securityEventRepository.save(event);
        
        log.info("安全事件处理完成: eventId={}, handledBy={}, status={}", 
                eventId, handledBy, status);
    }
    
    /**
     * 检测异常模式
     * 
     * @param userId 用户ID
     * @param sourceIp 源IP
     * @param eventType 事件类型
     * @return 是否检测到异常模式
     */
    public boolean detectAnomalousPattern(Long userId, String sourceIp, SecurityEventType eventType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(SecurityConstants.SECURITY_EVENT_WINDOW_HOURS);
        
        // 检查用户异常模式
        if (userId != null) {
            Long userEventCount = securityEventMapper.countByUserAndTypeInTimeWindow(
                    userId, eventType, windowStart, now);
            if (userEventCount >= SecurityConstants.SECURITY_EVENT_ALERT_THRESHOLD) {
                log.warn("检测到用户异常模式: userId={}, eventType={}, count={}", 
                        userId, eventType, userEventCount);
                return true;
            }
        }
        
        // 检查IP异常模式
        if (sourceIp != null) {
            Long ipEventCount = securityEventMapper.countByIpInTimeWindow(
                    sourceIp, windowStart, now);
            if (ipEventCount >= SecurityConstants.SECURITY_EVENT_ALERT_THRESHOLD) {
                log.warn("检测到IP异常模式: sourceIp={}, count={}", sourceIp, ipEventCount);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 定时清理过期事件
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void cleanupExpiredEvents() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(SecurityConstants.AUDIT_LOG_RETENTION_DAYS);
            securityEventRepository.deleteEventsBeforeTime(cutoffTime);
            log.info("清理过期安全事件完成: cutoffTime={}", cutoffTime);
        } catch (Exception e) {
            log.error("清理过期安全事件失败: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 定时检查未告警的高危事件
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟检查一次
    public void checkUnalertedHighRiskEvents() {
        try {
            List<SecurityEvent> unalertedEvents = securityEventMapper.findUnalertedHighRiskEvents();
            if (!unalertedEvents.isEmpty()) {
                log.warn("发现未告警的高危事件: count={}", unalertedEvents.size());
                securityAlertService.sendBatchAlert(unalertedEvents);
                
                // 标记为已告警
                unalertedEvents.forEach(SecurityEvent::markAsAlerted);
                securityEventRepository.saveAll(unalertedEvents);
            }
        } catch (Exception e) {
            log.error("检查未告警高危事件失败: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 构建安全事件实体
     */
    private SecurityEvent buildSecurityEvent(SecurityEventRequest request) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(request.getEventType())
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getEventType().getSeverity())
                .userId(request.getUserId())
                .username(request.getUsername())
                .sourceIp(request.getSourceIp())
                .userAgent(request.getUserAgent())
                .requestUri(request.getRequestUri())
                .requestMethod(request.getRequestMethod())
                .sessionId(request.getSessionId())
                .eventTime(request.getEventTime() != null ? request.getEventTime() : LocalDateTime.now())
                .status(SecurityEvent.Status.NEW.getCode())
                .alerted(false)
                .riskScore(request.getRiskScore())
                .build();
        
        // 序列化事件数据
        if (request.getEventData() != null) {
            try {
                event.setEventData(objectMapper.writeValueAsString(request.getEventData()));
            } catch (JsonProcessingException e) {
                log.warn("序列化事件数据失败: error={}", e.getMessage());
            }
        }
        
        return event;
    }
    
    /**
     * 计算事件指标
     */
    private void calculateEventMetrics(SecurityEvent event) {
        // 计算相关事件数量
        if (event.getUserId() != null && event.getEventType() != null) {
            LocalDateTime windowStart = event.getEventTime().minusHours(SecurityConstants.SECURITY_EVENT_WINDOW_HOURS);
            Long relatedCount = securityEventMapper.countByUserAndTypeInTimeWindow(
                    event.getUserId(), event.getEventType(), windowStart, event.getEventTime());
            event.setRelatedEventCount(relatedCount.intValue());
        }
        
        // 计算风险评分（如果未设置）
        if (event.getRiskScore() == null) {
            int riskScore = calculateRiskScore(event);
            event.setRiskScore(riskScore);
        }
    }
    
    /**
     * 计算风险评分
     */
    private int calculateRiskScore(SecurityEvent event) {
        int baseScore = event.getSeverity() * 20; // 基础分数：严重级别 * 20
        
        // 根据相关事件数量调整分数
        if (event.getRelatedEventCount() != null && event.getRelatedEventCount() > 1) {
            baseScore += Math.min(event.getRelatedEventCount() * 5, 30); // 最多加30分
        }
        
        // 根据事件类型调整分数
        if (event.getEventType().requiresImmediateAlert()) {
            baseScore += 10;
        }
        
        return Math.min(baseScore, 100); // 最高100分
    }
    
    /**
     * 检查并发送告警
     */
    private void checkAndSendAlert(SecurityEvent event) {
        if (event.isHighRisk()) {
            securityAlertService.sendAlert(event);
            event.markAsAlerted();
            securityEventRepository.save(event);
        }
    }
    
    /**
     * 更新事件统计缓存
     */
    private void updateEventStatistics(SecurityEvent event) {
        try {
            String cacheKey = SecurityConstants.SECURITY_EVENT_CACHE_PREFIX + "stats:today";
            String currentStats = redisTemplate.opsForValue().get(cacheKey);
            
            Map<String, Object> stats = new HashMap<>();
            if (currentStats != null) {
                stats = objectMapper.readValue(currentStats, Map.class);
            }
            
            // 更新统计数据
            stats.put("totalEvents", ((Number) stats.getOrDefault("totalEvents", 0)).longValue() + 1);
            if (event.isHighRisk()) {
                stats.put("highRiskEvents", ((Number) stats.getOrDefault("highRiskEvents", 0)).longValue() + 1);
            }
            
            // 缓存1天
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(stats), Duration.ofDays(1));
            
        } catch (Exception e) {
            log.warn("更新事件统计缓存失败: error={}", e.getMessage());
        }
    }
    

}