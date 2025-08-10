package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogQuery;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审计日志查询控制器
 * 
 * 提供审计日志的REST API接口，包括：
 * - 审计日志的分页查询和条件过滤
 * - 审计日志的统计分析功能
 * - 审计日志的导出功能（CSV/Excel）
 * - 安全事件的专项查询
 * 
 * 安全要求：
 * - 只有管理员可以访问审计日志
 * - 所有操作都会记录审计日志
 * - 支持细粒度的权限控制
 * 
 * 符合GB/T 22239-2019二级等保要求 3.2, 3.6
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    
    private final AuditLogService auditLogService;
    
    @Autowired
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
    
    // ==================== 基础查询接口 ====================
    
    /**
     * 分页查询审计日志
     * 
     * @param query 查询条件
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sortBy 排序字段
     * @param sortDirection 排序方向
     * @param request HTTP请求对象
     * @return 审计日志分页结果
     */
    @GetMapping
    @Auditable(operation = AuditOperation.AUDIT_LOG_QUERY, resourceType = "AUDIT_LOG")
    public ResponseEntity<Page<AuditLog>> queryAuditLogs(
            @ModelAttribute @Valid AuditLogQuery query,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            HttpServletRequest request) {
        
        try {
            // 参数验证和限制
            size = Math.min(size, 100); // 限制每页最大100条
            
            // 设置查询参数
            if (query == null) {
                query = new AuditLogQuery();
            }
            query.setPage(page);
            query.setSize(size);
            query.setSortBy(sortBy);
            query.setSortDirection(sortDirection);
            
            // 创建分页参数
            Pageable pageable = PageRequest.of(page, size);
            
            // 执行查询
            Page<AuditLog> result = auditLogService.findLogs(query, pageable);
            
            log.info("审计日志查询成功: page={}, size={}, total={}, conditions={}", 
                    page, size, result.getTotalElements(), query);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("审计日志查询失败: query={}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据ID查询单个审计日志详情
     * 
     * @param id 审计日志ID
     * @return 审计日志详情
     */
    @GetMapping("/{id}")
    @Auditable(operation = AuditOperation.AUDIT_LOG_VIEW, resourceType = "AUDIT_LOG")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable Long id) {
        
        try {
            Optional<AuditLog> auditLog = auditLogService.findById(id);
            
            if (auditLog.isPresent()) {
                log.info("查询审计日志详情成功: id={}", id);
                return ResponseEntity.ok(auditLog.get());
            } else {
                log.warn("审计日志不存在: id={}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("查询审计日志详情失败: id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== 专项查询接口 ====================
    
    /**
     * 查询安全事件日志
     * 
     * @param page 页码
     * @param size 每页大小
     * @param minRiskLevel 最小风险级别
     * @param hours 最近小时数
     * @return 安全事件日志
     */
    @GetMapping("/security-events")
    @Auditable(operation = AuditOperation.SECURITY_EVENT_QUERY, resourceType = "AUDIT_LOG")
    public ResponseEntity<Page<AuditLog>> querySecurityEvents(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "3") Integer minRiskLevel,
            @RequestParam(defaultValue = "24") Integer hours) {
        
        try {
            AuditLogQuery query = AuditLogQuery.builder()
                    .securityEventsOnly(true)
                    .minRiskLevel(minRiskLevel)
                    .lastHours(hours)
                    .sortBy("riskLevel")
                    .sortDirection("DESC")
                    .build();
            
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<AuditLog> result = auditLogService.findLogs(query, pageable);
            
            log.info("安全事件查询成功: page={}, size={}, total={}, minRiskLevel={}, hours={}", 
                    page, size, result.getTotalElements(), minRiskLevel, hours);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("安全事件查询失败: minRiskLevel={}, hours={}", minRiskLevel, hours, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 查询失败操作日志
     * 
     * @param page 页码
     * @param size 每页大小
     * @param hours 最近小时数
     * @return 失败操作日志
     */
    @GetMapping("/failures")
    @Auditable(operation = AuditOperation.FAILURE_LOG_QUERY, resourceType = "AUDIT_LOG")
    public ResponseEntity<Page<AuditLog>> queryFailures(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "24") Integer hours) {
        
        try {
            AuditLogQuery query = AuditLogQuery.builder()
                    .failuresOnly(true)
                    .result("FAILURE")
                    .lastHours(hours)
                    .sortBy("timestamp")
                    .sortDirection("DESC")
                    .build();
            
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<AuditLog> result = auditLogService.findLogs(query, pageable);
            
            log.info("失败操作查询成功: page={}, size={}, total={}, hours={}", 
                    page, size, result.getTotalElements(), hours);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("失败操作查询失败: hours={}", hours, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 查询用户操作历史
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @param days 最近天数
     * @return 用户操作历史
     */
    @GetMapping("/user/{userId}")
    @Auditable(operation = AuditOperation.USER_AUDIT_QUERY, resourceType = "AUDIT_LOG")
    public ResponseEntity<Page<AuditLog>> queryUserAuditLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "30") Integer days) {
        
        try {
            AuditLogQuery query = AuditLogQuery.forUserActivity(userId);
            query.setLastDays(days);
            query.setPage(page);
            query.setSize(size);
            
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<AuditLog> result = auditLogService.findLogs(query, pageable);
            
            log.info("用户操作历史查询成功: userId={}, page={}, size={}, total={}, days={}", 
                    userId, page, size, result.getTotalElements(), days);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("用户操作历史查询失败: userId={}, days={}", userId, days, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 查询IP地址操作记录
     * 
     * @param ipAddress IP地址
     * @param page 页码
     * @param size 每页大小
     * @param hours 最近小时数
     * @return IP操作记录
     */
    @GetMapping("/ip/{ipAddress}")
    @Auditable(operation = AuditOperation.IP_AUDIT_QUERY, resourceType = "AUDIT_LOG")
    public ResponseEntity<Page<AuditLog>> queryIpAuditLogs(
            @PathVariable String ipAddress,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "24") Integer hours) {
        
        try {
            AuditLogQuery query = AuditLogQuery.builder()
                    .ipAddress(ipAddress)
                    .lastHours(hours)
                    .sortBy("timestamp")
                    .sortDirection("DESC")
                    .build();
            
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            Page<AuditLog> result = auditLogService.findLogs(query, pageable);
            
            log.info("IP操作记录查询成功: ipAddress={}, page={}, size={}, total={}, hours={}", 
                    ipAddress, page, size, result.getTotalElements(), hours);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("IP操作记录查询失败: ipAddress={}, hours={}", ipAddress, hours, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== 统计分析接口 ====================
    
    /**
     * 获取审计日志统计报表
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param groupBy 分组字段
     * @return 统计报表数据
     */
    @GetMapping("/statistics")
    @Auditable(operation = AuditOperation.AUDIT_STATISTICS_QUERY, resourceType = "AUDIT_LOG")
    public ResponseEntity<Map<String, Object>> getAuditStatistics(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "operation") String groupBy) {
        
        try {
            // 设置默认时间范围（最近7天）
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(7);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("timeRange", Map.of(
                "startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
            
            // 基础统计
            AuditLogQuery baseQuery = AuditLogQuery.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
            
            // 总操作数统计
            statistics.put("totalOperations", getTotalOperationsCount(baseQuery));
            
            // 成功/失败操作统计
            statistics.put("successOperations", getOperationsByResult(baseQuery, "SUCCESS"));
            statistics.put("failureOperations", getOperationsByResult(baseQuery, "FAILURE"));
            
            // 安全事件统计
            statistics.put("securityEvents", getSecurityEventsCount(baseQuery));
            
            // 高风险操作统计
            statistics.put("highRiskOperations", getHighRiskOperationsCount(baseQuery));
            
            // 根据分组字段获取详细统计
            switch (groupBy.toLowerCase()) {
                case "operation":
                    statistics.put("operationStats", getOperationStatistics(startTime, endTime));
                    break;
                case "user":
                    statistics.put("userStats", getUserStatistics(startTime, endTime));
                    break;
                case "ip":
                    statistics.put("ipStats", getIpStatistics(startTime, endTime));
                    break;
                case "hour":
                    statistics.put("hourlyStats", getHourlyStatistics(startTime, endTime));
                    break;
                case "day":
                    statistics.put("dailyStats", getDailyStatistics(startTime, endTime));
                    break;
                default:
                    statistics.put("operationStats", getOperationStatistics(startTime, endTime));
            }
            
            log.info("审计日志统计查询成功: startTime={}, endTime={}, groupBy={}", 
                    startTime, endTime, groupBy);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("审计日志统计查询失败: startTime={}, endTime={}, groupBy={}", 
                    startTime, endTime, groupBy, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取实时监控数据
     * 
     * @return 实时监控数据
     */
    @GetMapping("/realtime")
    @Auditable(operation = AuditOperation.REALTIME_AUDIT_QUERY, resourceType = "AUDIT_LOG")
    public ResponseEntity<Map<String, Object>> getRealtimeData() {
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            
            Map<String, Object> realtimeData = new HashMap<>();
            
            // 最近1小时的操作统计
            AuditLogQuery query = AuditLogQuery.builder()
                    .startTime(oneHourAgo)
                    .endTime(now)
                    .build();
            
            realtimeData.put("lastHourOperations", getTotalOperationsCount(query));
            realtimeData.put("lastHourFailures", getOperationsByResult(query, "FAILURE"));
            realtimeData.put("lastHourSecurityEvents", getSecurityEventsCount(query));
            
            // 最近10分钟的操作统计
            LocalDateTime tenMinutesAgo = now.minusMinutes(10);
            AuditLogQuery recentQuery = AuditLogQuery.builder()
                    .startTime(tenMinutesAgo)
                    .endTime(now)
                    .build();
            
            realtimeData.put("last10MinutesOperations", getTotalOperationsCount(recentQuery));
            realtimeData.put("last10MinutesFailures", getOperationsByResult(recentQuery, "FAILURE"));
            
            // 活跃用户统计（最近1小时）
            realtimeData.put("activeUsers", getActiveUsersCount(oneHourAgo, now));
            
            // 活跃IP统计（最近1小时）
            realtimeData.put("activeIPs", getActiveIPsCount(oneHourAgo, now));
            
            realtimeData.put("timestamp", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.debug("实时监控数据查询成功");
            
            return ResponseEntity.ok(realtimeData);
            
        } catch (Exception e) {
            log.error("实时监控数据查询失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== 导出功能接口 ====================
    
    /**
     * 导出审计日志
     * 
     * @param query 查询条件
     * @param format 导出格式（CSV/EXCEL）
     * @return 导出文件
     */
    @PostMapping("/export")
    @Auditable(operation = AuditOperation.AUDIT_LOG_EXPORT, resourceType = "AUDIT_LOG")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestBody @Valid AuditLogQuery query,
            @RequestParam(defaultValue = "CSV") String format) {
        
        try {
            // 设置导出模式
            if (query == null) {
                query = new AuditLogQuery();
            }
            query.setExportMode(true);
            query.setExportFormat(format);
            
            // 执行导出
            byte[] exportData = auditLogService.exportLogs(query, format);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "audit_logs_" + timestamp;
            
            if ("EXCEL".equalsIgnoreCase(format)) {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", filename + ".xlsx");
            } else {
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData("attachment", filename + ".csv");
            }
            
            log.info("审计日志导出成功: format={}, size={} bytes, conditions={}", 
                    format, exportData.length, query);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportData);
            
        } catch (Exception e) {
            log.error("审计日志导出失败: format={}, query={}", format, query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 获取总操作数
     */
    private long getTotalOperationsCount(AuditLogQuery query) {
        try {
            return auditLogService.countLogs(query);
        } catch (Exception e) {
            log.warn("获取总操作数失败", e);
            return 0L;
        }
    }
    
    /**
     * 根据结果获取操作数
     */
    private long getOperationsByResult(AuditLogQuery query, String result) {
        try {
            AuditLogQuery resultQuery = AuditLogQuery.builder()
                    .startTime(query.getStartTime())
                    .endTime(query.getEndTime())
                    .result(result)
                    .build();
            return getTotalOperationsCount(resultQuery);
        } catch (Exception e) {
            log.warn("获取{}操作数失败", result, e);
            return 0L;
        }
    }
    
    /**
     * 获取安全事件数
     */
    private long getSecurityEventsCount(AuditLogQuery query) {
        try {
            AuditLogQuery securityQuery = AuditLogQuery.builder()
                    .startTime(query.getStartTime())
                    .endTime(query.getEndTime())
                    .securityEventsOnly(true)
                    .build();
            return getTotalOperationsCount(securityQuery);
        } catch (Exception e) {
            log.warn("获取安全事件数失败", e);
            return 0L;
        }
    }
    
    /**
     * 获取高风险操作数
     */
    private long getHighRiskOperationsCount(AuditLogQuery query) {
        try {
            AuditLogQuery highRiskQuery = AuditLogQuery.builder()
                    .startTime(query.getStartTime())
                    .endTime(query.getEndTime())
                    .highRiskOnly(true)
                    .build();
            return getTotalOperationsCount(highRiskQuery);
        } catch (Exception e) {
            log.warn("获取高风险操作数失败", e);
            return 0L;
        }
    }
    
    /**
     * 获取操作类型统计
     */
    private List<Map<String, Object>> getOperationStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogService.getOperationStatistics(startTime, endTime);
    }
    
    /**
     * 获取用户统计
     */
    private List<Map<String, Object>> getUserStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogService.getUserStatistics(startTime, endTime, 20);
    }
    
    /**
     * 获取IP统计
     */
    private List<Map<String, Object>> getIpStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogService.getIpStatistics(startTime, endTime, 20);
    }
    
    /**
     * 获取小时统计
     */
    private List<Map<String, Object>> getHourlyStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogService.getHourlyStatistics(startTime, endTime);
    }
    
    /**
     * 获取日统计
     */
    private List<Map<String, Object>> getDailyStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogService.getDailyStatistics(startTime, endTime);
    }
    
    /**
     * 获取活跃用户数
     */
    private long getActiveUsersCount(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogService.countActiveUsers(startTime, endTime);
    }
    
    /**
     * 获取活跃IP数
     */
    private long getActiveIPsCount(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogService.countActiveIPs(startTime, endTime);
    }
}