package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审计日志数据访问接口
 * 
 * 提供审计日志的数据访问方法，包括：
 * - 基本的CRUD操作
 * - 复杂的查询和统计功能
 * - 批量操作和清理功能
 * - 安全分析相关查询
 * 
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    
    // ==================== 基础查询方法 ====================
    
    /**
     * 根据用户ID查询审计日志
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据用户名查询审计日志
     * 
     * @param username 用户名
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByUsername(String username, Pageable pageable);
    
    /**
     * 根据操作类型查询审计日志
     * 
     * @param operation 操作类型
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByOperation(AuditOperation operation, Pageable pageable);
    
    /**
     * 根据操作结果查询审计日志
     * 
     * @param result 操作结果
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByResult(String result, Pageable pageable);
    
    /**
     * 根据IP地址查询审计日志
     * 
     * @param ipAddress IP地址
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByIpAddress(String ipAddress, Pageable pageable);
    
    /**
     * 根据会话ID查询审计日志
     * 
     * @param sessionId 会话ID
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findBySessionId(String sessionId, Pageable pageable);
    
    /**
     * 根据资源类型和资源ID查询审计日志
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, Long resourceId, Pageable pageable);
    
    // ==================== 时间范围查询方法 ====================
    
    /**
     * 根据时间范围查询审计日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据用户ID和时间范围查询审计日志
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByUserIdAndTimestampBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据操作类型和时间范围查询审计日志
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByOperationAndTimestampBetween(AuditOperation operation, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据操作结果和时间范围查询审计日志
     * 
     * @param result 操作结果
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByResultAndTimestampBetween(String result, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    // ==================== 复合条件查询方法 ====================
    
    /**
     * 根据用户ID、操作类型和时间范围查询审计日志
     * 
     * @param userId 用户ID
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByUserIdAndOperationAndTimestampBetween(Long userId, AuditOperation operation, 
                                                               LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据IP地址和时间范围查询审计日志
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<AuditLog> findByIpAddressAndTimestampBetween(String ipAddress, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    // ==================== 统计查询方法 ====================
    
    /**
     * 统计指定时间范围内的审计日志总数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计日志总数
     */
    long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定用户在指定时间范围内的操作次数
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作次数
     */
    long countByUserIdAndTimestampBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定操作类型在指定时间范围内的次数
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作次数
     */
    long countByOperationAndTimestampBetween(AuditOperation operation, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定IP地址在指定时间范围内的操作次数
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作次数
     */
    long countByIpAddressAndTimestampBetween(String ipAddress, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计失败操作的次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 失败操作次数
     */
    long countByResultAndTimestampBetween(String result, LocalDateTime startTime, LocalDateTime endTime);
    
    // ==================== 安全分析查询方法 ====================
    
    /**
     * 查询指定时间范围内的失败登录记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 失败登录记录
     */
    @Query("SELECT al FROM AuditLog al WHERE al.operation = :operation AND al.result = 'FAILURE' " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.timestamp DESC")
    Page<AuditLog> findFailedLoginAttempts(@Param("operation") AuditOperation operation,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          Pageable pageable);
    
    /**
     * 查询指定IP地址的可疑活动
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param minAttempts 最小尝试次数
     * @return 可疑活动记录
     */
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress " +
           "AND al.timestamp BETWEEN :startTime AND :endTime " +
           "AND al.result = 'FAILURE' " +
           "GROUP BY al.ipAddress HAVING COUNT(*) >= :minAttempts")
    List<AuditLog> findSuspiciousActivitiesByIp(@Param("ipAddress") String ipAddress,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("minAttempts") int minAttempts);
    
    /**
     * 查询高风险操作记录
     * 
     * @param minRiskLevel 最小风险级别
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 高风险操作记录
     */
    @Query("SELECT al FROM AuditLog al WHERE al.riskLevel >= :minRiskLevel " +
           "AND al.timestamp BETWEEN :startTime AND :endTime ORDER BY al.riskLevel DESC, al.timestamp DESC")
    Page<AuditLog> findHighRiskOperations(@Param("minRiskLevel") int minRiskLevel,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         Pageable pageable);
    
    /**
     * 查询未处理的安全事件
     * 
     * @param pageable 分页参数
     * @return 未处理的安全事件
     */
    @Query("SELECT al FROM AuditLog al WHERE al.processed = false " +
           "AND (al.result = 'FAILURE' OR al.riskLevel >= 4) " +
           "ORDER BY al.riskLevel DESC, al.timestamp DESC")
    Page<AuditLog> findUnprocessedSecurityEvents(Pageable pageable);
    
    /**
     * 查询异常登录记录（不同地理位置）
     * 
     * @param userId 用户ID
     * @param currentLocation 当前位置
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常登录记录
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.operation IN ('USER_LOGIN_SUCCESS', 'USER_LOGIN_FAILURE') " +
           "AND al.location != :currentLocation " +
           "AND al.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findAbnormalLoginsByLocation(@Param("userId") Long userId,
                                               @Param("currentLocation") String currentLocation,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    // ==================== 统计分析查询方法 ====================
    
    /**
     * 按操作类型统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作统计结果
     */
    @Query("SELECT al.operation, COUNT(*) FROM AuditLog al " +
           "WHERE al.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY al.operation ORDER BY COUNT(*) DESC")
    List<Object[]> countOperationsByType(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按用户统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回结果数量限制
     * @return 用户操作统计结果
     */
    @Query(value = "SELECT username, COUNT(*) as count FROM audit_logs " +
                   "WHERE timestamp BETWEEN :startTime AND :endTime " +
                   "GROUP BY username ORDER BY count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> countOperationsByUser(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("limit") int limit);
    
    /**
     * 按IP地址统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回结果数量限制
     * @return IP操作统计结果
     */
    @Query(value = "SELECT ip_address, COUNT(*) as count FROM audit_logs " +
                   "WHERE timestamp BETWEEN :startTime AND :endTime " +
                   "GROUP BY ip_address ORDER BY count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> countOperationsByIp(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("limit") int limit);
    
    /**
     * 按小时统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时操作统计结果
     */
    @Query(value = "SELECT DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00') as hour, COUNT(*) as count " +
                   "FROM audit_logs WHERE timestamp BETWEEN :startTime AND :endTime " +
                   "GROUP BY hour ORDER BY hour", nativeQuery = true)
    List<Object[]> countOperationsByHour(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按天统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日操作统计结果
     */
    @Query(value = "SELECT DATE(timestamp) as day, COUNT(*) as count " +
                   "FROM audit_logs WHERE timestamp BETWEEN :startTime AND :endTime " +
                   "GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> countOperationsByDay(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    // ==================== 数据清理方法 ====================
    
    /**
     * 删除指定时间之前的审计日志
     * 
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.timestamp < :beforeTime")
    int deleteByTimestampBefore(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 删除指定时间之前的成功操作日志（保留失败和错误日志）
     * 
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.timestamp < :beforeTime AND al.result = 'SUCCESS'")
    int deleteSuccessLogsByTimestampBefore(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 批量标记审计日志为已处理
     * 
     * @param ids 审计日志ID列表
     * @param processedBy 处理人
     * @param processNotes 处理备注
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE AuditLog al SET al.processed = true, al.processedAt = CURRENT_TIMESTAMP, " +
           "al.processedBy = :processedBy, al.processNotes = :processNotes WHERE al.id IN :ids")
    int markAsProcessed(@Param("ids") List<Long> ids,
                       @Param("processedBy") String processedBy,
                       @Param("processNotes") String processNotes);
    
    // ==================== 特殊查询方法 ====================
    
    /**
     * 查询最近的用户登录记录
     * 
     * @param userId 用户ID
     * @return 最近的登录记录
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.operation = 'USER_LOGIN_SUCCESS' " +
           "ORDER BY al.timestamp DESC LIMIT 1")
    Optional<AuditLog> findLatestLoginByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户的登录历史
     * 
     * @param userId 用户ID
     * @param limit 返回记录数限制
     * @return 登录历史记录
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.operation IN ('USER_LOGIN_SUCCESS', 'USER_LOGIN_FAILURE') " +
           "ORDER BY al.timestamp DESC LIMIT :limit")
    List<AuditLog> findLoginHistoryByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 查询指定资源的操作历史
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param pageable 分页参数
     * @return 资源操作历史
     */
    @Query("SELECT al FROM AuditLog al WHERE al.resourceType = :resourceType " +
           "AND al.resourceId = :resourceId ORDER BY al.timestamp DESC")
    Page<AuditLog> findResourceHistory(@Param("resourceType") String resourceType,
                                      @Param("resourceId") Long resourceId,
                                      Pageable pageable);
    
    /**
     * 查询包含指定标签的审计日志
     * 
     * @param tag 标签
     * @param pageable 分页参数
     * @return 包含指定标签的审计日志
     */
    @Query("SELECT al FROM AuditLog al WHERE al.tags LIKE CONCAT('%', :tag, '%') " +
           "ORDER BY al.timestamp DESC")
    Page<AuditLog> findByTag(@Param("tag") String tag, Pageable pageable);
    
    /**
     * 查询执行时间超过阈值的慢操作
     * 
     * @param minExecutionTime 最小执行时间（毫秒）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 慢操作记录
     */
    @Query("SELECT al FROM AuditLog al WHERE al.executionTime >= :minExecutionTime " +
           "AND al.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY al.executionTime DESC")
    Page<AuditLog> findSlowOperations(@Param("minExecutionTime") long minExecutionTime,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     Pageable pageable);
    
    /**
     * 检查指定时间范围内是否存在指定用户的操作记录
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否存在操作记录
     */
    boolean existsByUserIdAndTimestampBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 检查指定IP地址在指定时间范围内是否有失败操作
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否有失败操作
     */
    boolean existsByIpAddressAndResultAndTimestampBetween(String ipAddress, String result, 
                                                         LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计活跃用户数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃用户数
     */
    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM audit_logs " +
                   "WHERE timestamp BETWEEN :startTime AND :endTime AND user_id IS NOT NULL", 
           nativeQuery = true)
    long countActiveUsers(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计活跃IP数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃IP数
     */
    @Query(value = "SELECT COUNT(DISTINCT ip_address) FROM audit_logs " +
                   "WHERE timestamp BETWEEN :startTime AND :endTime AND ip_address IS NOT NULL", 
           nativeQuery = true)
    long countActiveIPs(@Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);
}