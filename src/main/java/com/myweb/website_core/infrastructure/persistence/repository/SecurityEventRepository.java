package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全事件数据访问接口
 * 
 * 提供安全事件的数据库操作方法
 */
@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    
    /**
     * 根据事件类型查询
     */
    List<SecurityEvent> findByEventType(SecurityEventType eventType);
    
    /**
     * 根据用户ID查询
     */
    List<SecurityEvent> findByUserId(Long userId);
    
    /**
     * 根据源IP查询
     */
    List<SecurityEvent> findBySourceIp(String sourceIp);
    
    /**
     * 根据严重级别查询
     */
    List<SecurityEvent> findBySeverity(Integer severity);
    
    /**
     * 根据状态查询
     */
    List<SecurityEvent> findByStatus(String status);
    
    /**
     * 查询未告警的高危事件
     */
    @Query("SELECT e FROM SecurityEvent e WHERE e.severity >= 4 AND e.alerted = false ORDER BY e.eventTime DESC")
    List<SecurityEvent> findUnalertedHighRiskEvents();
    
    /**
     * 查询指定时间范围内的事件
     */
    @Query("SELECT e FROM SecurityEvent e WHERE e.eventTime BETWEEN :startTime AND :endTime ORDER BY e.eventTime DESC")
    List<SecurityEvent> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定用户在时间窗口内的同类型事件数量
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.userId = :userId AND e.eventType = :eventType " +
           "AND e.eventTime BETWEEN :startTime AND :endTime")
    Long countByUserAndTypeInTimeWindow(@Param("userId") Long userId, 
                                       @Param("eventType") SecurityEventType eventType,
                                       @Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定IP在时间窗口内的事件数量
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.sourceIp = :sourceIp " +
           "AND e.eventTime BETWEEN :startTime AND :endTime")
    Long countByIpInTimeWindow(@Param("sourceIp") String sourceIp,
                              @Param("startTime") LocalDateTime startTime, 
                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的事件总数
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.eventTime BETWEEN :startTime AND :endTime")
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime, 
                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的高危事件数量
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.severity >= 4 " +
           "AND e.eventTime BETWEEN :startTime AND :endTime")
    Long countHighRiskByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的中等风险事件数量
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.severity = 3 " +
           "AND e.eventTime BETWEEN :startTime AND :endTime")
    Long countMediumRiskByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的低风险事件数量
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.severity <= 2 " +
           "AND e.eventTime BETWEEN :startTime AND :endTime")
    Long countLowRiskByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内未处理的事件数量
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.status IN ('NEW', 'PROCESSING') " +
           "AND e.eventTime BETWEEN :startTime AND :endTime")
    Long countUnhandledByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内已告警的事件数量
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.alerted = true " +
           "AND e.eventTime BETWEEN :startTime AND :endTime")
    Long countAlertedByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按事件类型统计
     */
    @Query("SELECT e.eventType, COUNT(e) FROM SecurityEvent e " +
           "WHERE e.eventTime BETWEEN :startTime AND :endTime " +
           "GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> countByEventTypeInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按严重级别统计
     */
    @Query("SELECT e.severity, COUNT(e) FROM SecurityEvent e " +
           "WHERE e.eventTime BETWEEN :startTime AND :endTime " +
           "GROUP BY e.severity ORDER BY e.severity DESC")
    List<Object[]> countBySeverityInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                             @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按状态统计
     */
    @Query("SELECT e.status, COUNT(e) FROM SecurityEvent e " +
           "WHERE e.eventTime BETWEEN :startTime AND :endTime " +
           "GROUP BY e.status ORDER BY COUNT(e) DESC")
    List<Object[]> countByStatusInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按小时统计（24小时）
     */
    @Query("SELECT HOUR(e.eventTime), COUNT(e) FROM SecurityEvent e " +
           "WHERE e.eventTime BETWEEN :startTime AND :endTime " +
           "GROUP BY HOUR(e.eventTime) ORDER BY HOUR(e.eventTime)")
    List<Object[]> countByHourInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按IP地址统计（Top 10）
     */
    @Query("SELECT e.sourceIp, COUNT(e) FROM SecurityEvent e " +
           "WHERE e.sourceIp IS NOT NULL AND e.eventTime BETWEEN :startTime AND :endTime " +
           "GROUP BY e.sourceIp ORDER BY COUNT(e) DESC")
    List<Object[]> countByIpInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime, 
                                       Pageable pageable);
    
    /**
     * 按用户统计（Top 10）
     */
    @Query("SELECT e.username, COUNT(e) FROM SecurityEvent e " +
           "WHERE e.username IS NOT NULL AND e.eventTime BETWEEN :startTime AND :endTime " +
           "GROUP BY e.username ORDER BY COUNT(e) DESC")
    List<Object[]> countByUserInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime, 
                                         Pageable pageable);
    
    /**
     * 计算指定时间范围内的平均风险评分
     */
    @Query("SELECT AVG(e.riskScore) FROM SecurityEvent e " +
           "WHERE e.riskScore IS NOT NULL AND e.eventTime BETWEEN :startTime AND :endTime")
    Double getAverageRiskScoreInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取指定时间范围内的最高风险评分
     */
    @Query("SELECT MAX(e.riskScore) FROM SecurityEvent e " +
           "WHERE e.riskScore IS NOT NULL AND e.eventTime BETWEEN :startTime AND :endTime")
    Integer getMaxRiskScoreInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 复杂条件查询
     */
    @Query("SELECT e FROM SecurityEvent e WHERE " +
           "(:eventTypes IS NULL OR e.eventType IN :eventTypes) AND " +
           "(:severities IS NULL OR e.severity IN :severities) AND " +
           "(:userId IS NULL OR e.userId = :userId) AND " +
           "(:username IS NULL OR e.username LIKE %:username%) AND " +
           "(:sourceIp IS NULL OR e.sourceIp = :sourceIp) AND " +
           "(:statuses IS NULL OR e.status IN :statuses) AND " +
           "(:alerted IS NULL OR e.alerted = :alerted) AND " +
           "(:startTime IS NULL OR e.eventTime >= :startTime) AND " +
           "(:endTime IS NULL OR e.eventTime <= :endTime) AND " +
           "(:minRiskScore IS NULL OR e.riskScore >= :minRiskScore) AND " +
           "(:maxRiskScore IS NULL OR e.riskScore <= :maxRiskScore) AND " +
           "(:keyword IS NULL OR e.title LIKE %:keyword% OR e.description LIKE %:keyword%) " +
           "ORDER BY e.eventTime DESC")
    Page<SecurityEvent> findByComplexQuery(@Param("eventTypes") List<SecurityEventType> eventTypes,
                                          @Param("severities") List<Integer> severities,
                                          @Param("userId") Long userId,
                                          @Param("username") String username,
                                          @Param("sourceIp") String sourceIp,
                                          @Param("statuses") List<String> statuses,
                                          @Param("alerted") Boolean alerted,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("minRiskScore") Integer minRiskScore,
                                          @Param("maxRiskScore") Integer maxRiskScore,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);
    
    /**
     * 删除指定时间之前的事件（用于数据清理）
     */
    @Query("DELETE FROM SecurityEvent e WHERE e.eventTime < :cutoffTime")
    void deleteEventsBeforeTime(@Param("cutoffTime") LocalDateTime cutoffTime);
}