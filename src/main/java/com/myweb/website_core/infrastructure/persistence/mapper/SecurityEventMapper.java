package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.SecurityEventQuery;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 安全事件MyBatis映射器
 * 
 * 处理复杂的安全事件查询，特别是动态SQL查询
 * 解决PostgreSQL中null参数类型推断问题
 */
@Mapper
public interface SecurityEventMapper {
    
    /**
     * 复杂条件查询安全事件
     * 
     * @param query 查询条件
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 安全事件列表
     */
    List<SecurityEvent> findByComplexQuery(@Param("query") SecurityEventQuery query,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);
    
    /**
     * 复杂条件查询安全事件总数
     * 
     * @param query 查询条件
     * @return 总数
     */
    long countByComplexQuery(@Param("query") SecurityEventQuery query);
    
    /**
     * 按事件类型统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> countByEventTypeInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按严重级别统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> countBySeverityInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按状态统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> countByStatusInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按小时统计（时间范围内）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> countByHourInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按IP地址统计（时间范围内，Top N）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 统计结果
     */
    List<Map<String, Object>> countByIpInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   @Param("limit") int limit);
    
    /**
     * 按用户统计（时间范围内，Top N）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 统计结果
     */
    List<Map<String, Object>> countByUserInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime,
                                                     @Param("limit") int limit);
    
    /**
     * 获取指定时间范围内的平均风险评分
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均风险评分
     */
    Double getAverageRiskScoreInTimeRange(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取指定时间范围内的最高风险评分
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 最高风险评分
     */
    Integer getMaxRiskScoreInTimeRange(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的事件总数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 事件总数
     */
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的高危事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 高危事件数量
     */
    Long countHighRiskByTimeRange(@Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的中等风险事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 中等风险事件数量
     */
    Long countMediumRiskByTimeRange(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的低风险事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 低风险事件数量
     */
    Long countLowRiskByTimeRange(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内未处理的事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 未处理事件数量
     */
    Long countUnhandledByTimeRange(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内已告警的事件数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 已告警事件数量
     */
    Long countAlertedByTimeRange(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定用户在时间窗口内的同类型事件数量
     * 
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 事件数量
     */
    Long countByUserAndTypeInTimeWindow(@Param("userId") Long userId,
                                       @Param("eventType") SecurityEventType eventType,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定IP在时间窗口内的事件数量
     * 
     * @param sourceIp 源IP
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 事件数量
     */
    Long countByIpInTimeWindow(@Param("sourceIp") String sourceIp,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询未告警的高危事件
     * 
     * @return 未告警的高危事件列表
     */
    List<SecurityEvent> findUnalertedHighRiskEvents();
}