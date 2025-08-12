package com.myweb.website_core.infrastructure.persistence.repository.audit;

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

/**
 * 审计日志数据访问接口
 * <p>
 * 提供审计日志的数据访问方法，包括：
 * - 基本的CRUD操作
 * - 简单的查询方法
 * - 数据清理功能
 * <p>
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 *
 * @author MyWeb
 * @version 1.0
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
     * @param result 操作结果
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 失败操作次数
     */
    long countByResultAndTimestampBetween(String result, LocalDateTime startTime, LocalDateTime endTime);

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

    // ==================== 特殊查询方法 ====================

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
     * @param result 操作结果
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否有失败操作
     */
    boolean existsByIpAddressAndResultAndTimestampBetween(String ipAddress, String result,
                                                          LocalDateTime startTime, LocalDateTime endTime);

}