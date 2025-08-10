package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志MyBatis映射器
 * 
 * 处理复杂的审计日志查询，特别是动态SQL查询
 * 解决PostgreSQL中null参数类型推断问题
 */
@Mapper
public interface AuditLogMapper {
    
    /**
     * 查询指定时间范围内的失败登录记录
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 失败登录记录
     */
    List<AuditLog> findFailedLoginAttempts(@Param("operation") AuditOperation operation,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);
    
    /**
     * 统计失败登录记录总数
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 失败登录记录总数
     */
    long countFailedLoginAttempts(@Param("operation") AuditOperation operation,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定IP地址的可疑活动
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param minAttempts 最小尝试次数
     * @return 可疑活动记录
     */
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
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 高风险操作记录
     */
    List<AuditLog> findHighRiskOperations(@Param("minRiskLevel") int minRiskLevel,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);
    
    /**
     * 统计高风险操作记录总数
     * 
     * @param minRiskLevel 最小风险级别
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 高风险操作记录总数
     */
    long countHighRiskOperations(@Param("minRiskLevel") int minRiskLevel,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询未处理的安全事件
     * 
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 未处理的安全事件
     */
    List<AuditLog> findUnprocessedSecurityEvents(@Param("offset") int offset,
                                                 @Param("limit") int limit);
    
    /**
     * 统计未处理的安全事件总数
     * 
     * @return 未处理的安全事件总数
     */
    long countUnprocessedSecurityEvents();
    
    /**
     * 查询异常登录记录（不同地理位置）
     * 
     * @param userId 用户ID
     * @param currentLocation 当前位置
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常登录记录
     */
    List<AuditLog> findAbnormalLoginsByLocation(@Param("userId") Long userId,
                                               @Param("currentLocation") String currentLocation,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按操作类型统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作统计结果
     */
    List<Map<String, Object>> countOperationsByType(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按用户统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回结果数量限制
     * @return 用户操作统计结果
     */
    List<Map<String, Object>> countOperationsByUser(@Param("startTime") LocalDateTime startTime,
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
    List<Map<String, Object>> countOperationsByIp(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  @Param("limit") int limit);
    
    /**
     * 按小时统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时操作统计结果
     */
    List<Map<String, Object>> countOperationsByHour(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按天统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日操作统计结果
     */
    List<Map<String, Object>> countOperationsByDay(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询最近的用户登录记录
     * 
     * @param userId 用户ID
     * @return 最近的登录记录
     */
    AuditLog findLatestLoginByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户的登录历史
     * 
     * @param userId 用户ID
     * @param limit 返回记录数限制
     * @return 登录历史记录
     */
    List<AuditLog> findLoginHistoryByUserId(@Param("userId") Long userId, 
                                           @Param("limit") int limit);
    
    /**
     * 查询指定资源的操作历史
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 资源操作历史
     */
    List<AuditLog> findResourceHistory(@Param("resourceType") String resourceType,
                                      @Param("resourceId") Long resourceId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);
    
    /**
     * 统计指定资源的操作历史总数
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 资源操作历史总数
     */
    long countResourceHistory(@Param("resourceType") String resourceType,
                             @Param("resourceId") Long resourceId);
    
    /**
     * 查询包含指定标签的审计日志
     * 
     * @param tag 标签
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 包含指定标签的审计日志
     */
    List<AuditLog> findByTag(@Param("tag") String tag,
                            @Param("offset") int offset,
                            @Param("limit") int limit);
    
    /**
     * 统计包含指定标签的审计日志总数
     * 
     * @param tag 标签
     * @return 包含指定标签的审计日志总数
     */
    long countByTag(@Param("tag") String tag);
    
    /**
     * 查询执行时间超过阈值的慢操作
     * 
     * @param minExecutionTime 最小执行时间（毫秒）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 慢操作记录
     */
    List<AuditLog> findSlowOperations(@Param("minExecutionTime") long minExecutionTime,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);
    
    /**
     * 统计执行时间超过阈值的慢操作总数
     * 
     * @param minExecutionTime 最小执行时间（毫秒）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 慢操作记录总数
     */
    long countSlowOperations(@Param("minExecutionTime") long minExecutionTime,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计活跃用户数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃用户数
     */
    long countActiveUsers(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计活跃IP数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃IP数
     */
    long countActiveIPs(@Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 批量标记审计日志为已处理
     * 
     * @param ids 审计日志ID列表
     * @param processedBy 处理人
     * @param processNotes 处理备注
     * @return 更新的记录数
     */
    int markAsProcessed(@Param("ids") List<Long> ids,
                       @Param("processedBy") String processedBy,
                       @Param("processNotes") String processNotes);
}