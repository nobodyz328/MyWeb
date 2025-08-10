package com.myweb.website_core.infrastructure.persistence.mapper;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 安全的审计日志MyBatis映射器服务
 * <p>
 * 继承SafeMapperBase，提供安全的MyBatis查询包装服务
 * 处理分页和数据转换，集成SQL注入防护
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class AuditLogMapperService extends SafeMapperBase {
    
    private final AuditLogMapper auditLogMapper;
    
    @Override
    protected String getTableName() {
        return "audit_logs";
    }
    
    /**
     * 查询指定时间范围内的失败登录记录（分页）
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 失败登录记录分页结果
     */
    public Page<AuditLog> findFailedLoginAttempts(AuditOperation operation, LocalDateTime startTime, 
                                                 LocalDateTime endTime, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<AuditLog> logs = auditLogMapper.findFailedLoginAttempts(operation, startTime, endTime, offset, limit);
        long total = auditLogMapper.countFailedLoginAttempts(operation, startTime, endTime);
        
        return new PageImpl<>(logs, pageable, total);
    }
    
    /**
     * 安全查询指定IP地址的可疑活动
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param minAttempts 最小尝试次数
     * @return 可疑活动记录
     */
    public List<AuditLog> findSuspiciousActivitiesByIp(String ipAddress, LocalDateTime startTime, 
                                                       LocalDateTime endTime, int minAttempts) {
        
        // 验证IP地址参数安全性
        if (ipAddress != null) {
            sqlInjectionProtectionService.validateAndSanitizeInput(ipAddress, "IP_QUERY", "ipAddress");
        }
        
        logSafeQuery("SUSPICIOUS_ACTIVITIES", Map.of(
            "ipAddress", ipAddress,
            "startTime", startTime,
            "endTime", endTime,
            "minAttempts", minAttempts
        ));
        
        return auditLogMapper.findSuspiciousActivitiesByIp(ipAddress, startTime, endTime, minAttempts);
    }
    
    /**
     * 安全的分页查询审计日志
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<AuditLog> findSafePaginated(Map<String, Object> conditions, String sortField, 
                                          String sortDirection, Pageable pageable) {
        
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        // 构建安全的查询参数
        Map<String, Object> safeParams = buildSafePaginatedParams(conditions, sortField, sortDirection, limit, offset);
        
        logSafeQuery("PAGINATED", safeParams);
        
        // 这里需要在AuditLogMapper中添加对应的安全查询方法
        // List<AuditLog> logs = auditLogMapper.findSafePaginated(safeParams);
        // long total = auditLogMapper.countSafe(conditions);
        
        // 临时使用现有方法，实际应该使用安全的动态查询
        List<AuditLog> logs = auditLogMapper.findFailedLoginAttempts(null, null, null, offset, limit);
        long total = auditLogMapper.countFailedLoginAttempts(null, null, null);
        
        return new PageImpl<>(logs, pageable, total);
    }
    
    /**
     * 安全的搜索查询
     * 
     * @param searchFields 搜索字段列表
     * @param keyword 搜索关键词
     * @param additionalConditions 额外查询条件
     * @param pageable 分页参数
     * @return 搜索结果
     */
    public Page<AuditLog> findSafeSearch(List<String> searchFields, String keyword,
                                       Map<String, Object> additionalConditions, Pageable pageable) {
        
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        // 构建安全的搜索参数
        Map<String, Object> safeParams = buildSafeSearchParams(searchFields, keyword, additionalConditions);
        safeParams.put("offset", offset);
        safeParams.put("limit", limit);
        
        logSafeQuery("SEARCH", safeParams);
        
        // 这里需要在AuditLogMapper中添加对应的安全搜索方法
        // List<AuditLog> logs = auditLogMapper.findSafeSearch(safeParams);
        // long total = auditLogMapper.countSafeSearch(safeParams);
        
        // 临时返回空结果，实际应该实现安全的搜索查询
        return new PageImpl<>(List.of(), pageable, 0);
    }
    
    /**
     * 查询高风险操作记录（分页）
     * 
     * @param minRiskLevel 最小风险级别
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 高风险操作记录分页结果
     */
    public Page<AuditLog> findHighRiskOperations(int minRiskLevel, LocalDateTime startTime, 
                                                LocalDateTime endTime, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<AuditLog> logs = auditLogMapper.findHighRiskOperations(minRiskLevel, startTime, endTime, offset, limit);
        long total = auditLogMapper.countHighRiskOperations(minRiskLevel, startTime, endTime);
        
        return new PageImpl<>(logs, pageable, total);
    }
    
    /**
     * 查询未处理的安全事件（分页）
     * 
     * @param pageable 分页参数
     * @return 未处理的安全事件分页结果
     */
    public Page<AuditLog> findUnprocessedSecurityEvents(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<AuditLog> logs = auditLogMapper.findUnprocessedSecurityEvents(offset, limit);
        long total = auditLogMapper.countUnprocessedSecurityEvents();
        
        return new PageImpl<>(logs, pageable, total);
    }
    
    /**
     * 查询异常登录记录（不同地理位置）
     * 
     * @param userId 用户ID
     * @param currentLocation 当前位置
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常登录记录
     */
    public List<AuditLog> findAbnormalLoginsByLocation(Long userId, String currentLocation, 
                                                      LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.findAbnormalLoginsByLocation(userId, currentLocation, startTime, endTime);
    }
    
    /**
     * 按操作类型统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作统计结果
     */
    public Map<String, Long> countOperationsByType(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = auditLogMapper.countOperationsByType(startTime, endTime);
        return convertToStringLongMap(results, "operation", "count");
    }
    
    /**
     * 按用户统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回结果数量限制
     * @return 用户操作统计结果
     */
    public Map<String, Long> countOperationsByUser(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        List<Map<String, Object>> results = auditLogMapper.countOperationsByUser(startTime, endTime, limit);
        return convertToStringLongMap(results, "username", "count");
    }
    
    /**
     * 按IP地址统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回结果数量限制
     * @return IP操作统计结果
     */
    public Map<String, Long> countOperationsByIp(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        List<Map<String, Object>> results = auditLogMapper.countOperationsByIp(startTime, endTime, limit);
        return convertToStringLongMap(results, "ipAddress", "count");
    }
    
    /**
     * 按小时统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时操作统计结果
     */
    public Map<Integer, Long> countOperationsByHour(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = auditLogMapper.countOperationsByHour(startTime, endTime);
        return convertToIntegerLongMap(results, "hour", "count");
    }
    
    /**
     * 按天统计指定时间范围内的操作次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日操作统计结果
     */
    public Map<String, Long> countOperationsByDay(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = auditLogMapper.countOperationsByDay(startTime, endTime);
        return convertToStringLongMap(results, "day", "count");
    }
    
    /**
     * 查询最近的用户登录记录
     * 
     * @param userId 用户ID
     * @return 最近的登录记录
     */
    public Optional<AuditLog> findLatestLoginByUserId(Long userId) {
        AuditLog log = auditLogMapper.findLatestLoginByUserId(userId);
        return Optional.ofNullable(log);
    }
    
    /**
     * 查询用户的登录历史
     * 
     * @param userId 用户ID
     * @param limit 返回记录数限制
     * @return 登录历史记录
     */
    public List<AuditLog> findLoginHistoryByUserId(Long userId, int limit) {
        return auditLogMapper.findLoginHistoryByUserId(userId, limit);
    }
    
    /**
     * 查询指定资源的操作历史（分页）
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param pageable 分页参数
     * @return 资源操作历史分页结果
     */
    public Page<AuditLog> findResourceHistory(String resourceType, Long resourceId, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<AuditLog> logs = auditLogMapper.findResourceHistory(resourceType, resourceId, offset, limit);
        long total = auditLogMapper.countResourceHistory(resourceType, resourceId);
        
        return new PageImpl<>(logs, pageable, total);
    }
    
    /**
     * 查询包含指定标签的审计日志（分页）
     * 
     * @param tag 标签
     * @param pageable 分页参数
     * @return 包含指定标签的审计日志分页结果
     */
    public Page<AuditLog> findByTag(String tag, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<AuditLog> logs = auditLogMapper.findByTag(tag, offset, limit);
        long total = auditLogMapper.countByTag(tag);
        
        return new PageImpl<>(logs, pageable, total);
    }
    
    /**
     * 查询执行时间超过阈值的慢操作（分页）
     * 
     * @param minExecutionTime 最小执行时间（毫秒）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 慢操作记录分页结果
     */
    public Page<AuditLog> findSlowOperations(long minExecutionTime, LocalDateTime startTime, 
                                            LocalDateTime endTime, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        
        List<AuditLog> logs = auditLogMapper.findSlowOperations(minExecutionTime, startTime, endTime, offset, limit);
        long total = auditLogMapper.countSlowOperations(minExecutionTime, startTime, endTime);
        
        return new PageImpl<>(logs, pageable, total);
    }
    
    /**
     * 统计活跃用户数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃用户数
     */
    public long countActiveUsers(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.countActiveUsers(startTime, endTime);
    }
    
    /**
     * 统计活跃IP数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃IP数
     */
    public long countActiveIPs(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.countActiveIPs(startTime, endTime);
    }
    
    /**
     * 批量标记审计日志为已处理
     * 
     * @param ids 审计日志ID列表
     * @param processedBy 处理人
     * @param processNotes 处理备注
     * @return 更新的记录数
     */
    public int markAsProcessed(List<Long> ids, String processedBy, String processNotes) {
        return auditLogMapper.markAsProcessed(ids, processedBy, processNotes);
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