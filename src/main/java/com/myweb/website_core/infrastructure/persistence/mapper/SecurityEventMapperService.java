package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.domain.security.dto.SecurityEventQuery;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 安全事件MyBatis映射器服务
 * <p>
 * 提供MyBatis查询的包装服务，处理分页和数据转换
 */
@Service
@RequiredArgsConstructor
public class SecurityEventMapperService {
    
    private final SecurityEventMapper securityEventMapper;
    
    /**
     * 复杂条件查询安全事件（分页）
     * 
     * @param query 查询条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<SecurityEvent> findByComplexQuery(SecurityEventQuery query, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        // 查询数据
        List<SecurityEvent> events = securityEventMapper.findByComplexQuery(query, offset, limit);
        
        // 查询总数
        long total = securityEventMapper.countByComplexQuery(query);
        
        return new PageImpl<>(events, pageable, total);
    }
    
    /**
     * 按事件类型统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    public Map<String, Long> countByEventTypeInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = securityEventMapper.countByEventTypeInTimeRange(startTime, endTime);
        return convertToStringLongMap(results, "eventType", "count");
    }
    
    /**
     * 按严重级别统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    public Map<Integer, Long> countBySeverityInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = securityEventMapper.countBySeverityInTimeRange(startTime, endTime);
        return convertToIntegerLongMap(results, "severity", "count");
    }
    
    /**
     * 按状态统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    public Map<String, Long> countByStatusInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = securityEventMapper.countByStatusInTimeRange(startTime, endTime);
        return convertToStringLongMap(results, "status", "count");
    }
    
    /**
     * 按小时统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    public Map<Integer, Long> countByHourInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = securityEventMapper.countByHourInTimeRange(startTime, endTime);
        return convertToIntegerLongMap(results, "hour", "count");
    }
    
    /**
     * 按IP地址统计（时间范围内，Top N）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 统计结果
     */
    public Map<String, Long> countByIpInTimeRange(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        List<Map<String, Object>> results = securityEventMapper.countByIpInTimeRange(startTime, endTime, limit);
        return convertToStringLongMap(results, "sourceIp", "count");
    }
    
    /**
     * 按用户统计（时间范围内，Top N）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 统计结果
     */
    public Map<String, Long> countByUserInTimeRange(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        List<Map<String, Object>> results = securityEventMapper.countByUserInTimeRange(startTime, endTime, limit);
        return convertToStringLongMap(results, "username", "count");
    }
    
    /**
     * 获取指定时间范围内的平均风险评分
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均风险评分
     */
    public Double getAverageRiskScoreInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.getAverageRiskScoreInTimeRange(startTime, endTime);
    }
    
    /**
     * 获取指定时间范围内的最高风险评分
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 最高风险评分
     */
    public Integer getMaxRiskScoreInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.getMaxRiskScoreInTimeRange(startTime, endTime);
    }
    
    /**
     * 统计指定时间范围内的事件总数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 事件总数
     */
    public Long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.countByTimeRange(startTime, endTime);
    }
    
    /**
     * 统计指定时间范围内的高危事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 高危事件数量
     */
    public Long countHighRiskByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.countHighRiskByTimeRange(startTime, endTime);
    }
    
    /**
     * 统计指定时间范围内的中等风险事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 中等风险事件数量
     */
    public Long countMediumRiskByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.countMediumRiskByTimeRange(startTime, endTime);
    }
    
    /**
     * 统计指定时间范围内的低风险事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 低风险事件数量
     */
    public Long countLowRiskByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.countLowRiskByTimeRange(startTime, endTime);
    }
    
    /**
     * 统计指定时间范围内未处理的事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 未处理事件数量
     */
    public Long countUnhandledByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.countUnhandledByTimeRange(startTime, endTime);
    }
    
    /**
     * 统计指定时间范围内已告警的事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 已告警事件数量
     */
    public Long countAlertedByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return securityEventMapper.countAlertedByTimeRange(startTime, endTime);
    }
    
    /**
     * 转换查询结果为String-Long Map
     */
    private Map<String, Long> convertToStringLongMap(List<Map<String, Object>> results, String keyField, String valueField) {
        return results.stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get(keyField)),
                        row -> ((Number) row.get(valueField)).longValue(),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }
    
    /**
     * 转换查询结果为Integer-Long Map
     */
    private Map<Integer, Long> convertToIntegerLongMap(List<Map<String, Object>> results, String keyField, String valueField) {
        return results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get(keyField)).intValue(),
                        row -> ((Number) row.get(valueField)).longValue(),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }
}