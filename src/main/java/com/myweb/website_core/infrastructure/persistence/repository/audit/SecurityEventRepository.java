package com.myweb.website_core.infrastructure.persistence.repository.audit;

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
    
    // 复杂统计查询已迁移到MyBatis SecurityEventMapper
    // 以避免PostgreSQL null参数类型推断问题
    

    
    /**
     * 删除指定时间之前的事件（用于数据清理）
     */
    @Query("DELETE FROM SecurityEvent e WHERE e.eventTime < :cutoffTime")
    void deleteEventsBeforeTime(@Param("cutoffTime") LocalDateTime cutoffTime);
}