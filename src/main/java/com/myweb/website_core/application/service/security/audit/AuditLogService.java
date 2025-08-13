package com.myweb.website_core.application.service.security.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogQuery;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.infrastructure.persistence.mapper.AuditLogMapperService;
import com.myweb.website_core.infrastructure.persistence.repository.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 审计日志服务
 * <p>
 * 提供审计日志的记录、查询和管理功能，包括：
 * - 异步日志记录
 * - 日志查询和过滤
 * - 日志导出功能
 * <p>
 * 符合GB/T 22239-2019安全审计要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapperService auditLogMapperService;
    private final ObjectMapper objectMapper;
    private final AuditMessageService auditMessageService;
    
    // 审计日志保留天数配置
    @Value("${app.audit.retention-days:90}")
    private int retentionDays;
    
    // 批量处理大小配置
    @Value("${app.audit.batch-size:1000}")
    private int batchSize;
    
    // 导出记录数限制配置
    @Value("${app.audit.export-limit:10000}")
    private int exportLimit;

    /**
     * 异步记录审计日志
     * 
     * @param request 审计日志请求
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> logOperation(AuditLogRequest request) {
            if (request == null || !request.isValid()) {
                log.warn("审计日志请求无效: {}", request);
                return CompletableFuture.completedFuture(null);
            }
            auditMessageService.logOperation(request);
        return CompletableFuture.completedFuture(null);
    }
    

    /**
     * 记录安全事件审计日志
     * 
     * @param eventType 事件类型
     * @param username 用户名
     * @param description 事件描述
     */
    public void logSecurityEvent(AuditOperation eventType, String username, String description) {
        AuditLogRequest request = AuditLogRequest.builder()
                .operation(eventType)
                .username(username)
                .resourceType("SECURITY")
                .result("SECURITY_EVENT")
                .errorMessage(description)
                .timestamp(LocalDateTime.now())
                .build();
        logOperation(request);
    }
    
    /**
     * 查询审计日志 - 支持复杂条件查询和分页
     * 
     * @param query 查询条件
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    public Page<AuditLog> findLogs(AuditLogQuery query, Pageable pageable) {
        try {
            if (query == null || query.isEmpty()) {
                // 如果没有查询条件，返回最近的记录
                Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
                Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(), 
                    pageable.getPageSize(), 
                    sort
                );
                return auditLogRepository.findAll(sortedPageable);
            }
            
            // 构建动态查询条件
            Specification<AuditLog> specification = buildSpecification(query);
            
            // 应用排序
            Pageable sortedPageable = applySorting(query, pageable);
            
            return auditLogRepository.findAll(specification, sortedPageable);
            
        } catch (Exception e) {
            log.error("查询审计日志失败: query={}", query, e);
            throw new RuntimeException("查询审计日志失败", e);
        }
    }
    
    /**
     * 导出审计日志 - 支持CSV和Excel格式
     * 
     * @param query 查询条件
     * @param format 导出格式 (CSV/EXCEL)
     * @return 导出文件的字节数组
     */
    public byte[] exportLogs(AuditLogQuery query, String format) {
        try {
            // 设置导出模式和限制
            if (query != null) {
                query.setExportMode(true);
                if (query.getSize() == null || query.getSize() > exportLimit) {
                    query.setSize(exportLimit);
                }
            }
            
            // 查询数据
            List<AuditLog> logs = findLogsForExport(query);
            
            // 根据格式生成文件
            if ("EXCEL".equalsIgnoreCase(format)) {
                return generateExcelReport(logs, query);
            } else {
                return generateCsvReport(logs, query);
            }
            
        } catch (Exception e) {
            log.error("导出审计日志失败: query={}, format={}", query, format, e);
            throw new RuntimeException("导出审计日志失败", e);
        }
    }
    
    /**
     * 导出审计日志 - 默认CSV格式
     * 
     * @param query 查询条件
     * @return CSV格式的日志数据
     */
    public byte[] exportLogs(AuditLogQuery query) {
        return exportLogs(query, "CSV");
    }
    
    /**
     * 将审计日志请求转换为实体
     * 
     * @param request 审计日志请求
     * @return 审计日志实体
     */
    private AuditLog convertToEntity(AuditLogRequest request) {
        AuditLog auditLog = new AuditLog();
        
        auditLog.setUserId(request.getUserId());
        auditLog.setUsername(request.getUsername());
        auditLog.setOperation(request.getOperation());
        auditLog.setResourceType(request.getResourceType());
        auditLog.setResourceId(request.getResourceId());
        auditLog.setIpAddress(request.getIpAddress());
        auditLog.setUserAgent(request.getUserAgent());
        auditLog.setResult(request.getResult());
        auditLog.setErrorMessage(request.getErrorMessage());
        auditLog.setExecutionTime(request.getExecutionTime());
        auditLog.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        auditLog.setSessionId(request.getSessionId());
        
        // 序列化请求和响应数据
        try {
            if (request.getRequestData() != null) {
                auditLog.setRequestData(objectMapper.writeValueAsString(request.getRequestData()));
            }
            if (request.getResponseData() != null) {
                auditLog.setResponseData(objectMapper.writeValueAsString(request.getResponseData()));
            }
        } catch (JsonProcessingException e) {
            log.warn("序列化审计数据失败: {}", e.getMessage());
            auditLog.setErrorMessage("Failed to serialize audit data: " + e.getMessage());
        }
        
        return auditLog;
    }
    
    // ==================== 自动清理机制 ====================
    
    /**
     * 自动清理过期审计日志
     * 每天凌晨3点执行，清理超过保留期的审计日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            log.info("开始清理{}天前的审计日志，截止时间: {}", retentionDays, cutoffTime);
            
            // 分批删除，避免长时间锁表
            int totalDeleted = 0;
            int batchDeleted;
            
            do {
                // 只删除成功的操作日志，保留失败和错误日志更长时间
                batchDeleted = auditLogRepository.deleteSuccessLogsByTimestampBefore(cutoffTime);
                totalDeleted += batchDeleted;
                
                if (batchDeleted > 0) {
                    log.debug("本批次删除了{}条审计日志", batchDeleted);
                    // 短暂休眠，减少数据库压力
                    Thread.sleep(100);
                }
            } while (batchDeleted > 0);
            
            log.info("审计日志清理完成，共删除{}条记录", totalDeleted);
            
            // 记录清理操作的审计日志
            logSystemOperation(AuditOperation.CACHE_CLEANUP, 
                "审计日志自动清理", "删除了" + totalDeleted + "条过期日志");
            
        } catch (Exception e) {
            log.error("清理过期审计日志失败", e);
            // 记录清理失败的审计日志
            logSystemOperation(AuditOperation.CACHE_CLEANUP, 
                "审计日志清理失败", "清理过程中发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 手动清理指定时间之前的审计日志
     * 
     * @param beforeTime 时间阈值
     * @param onlySuccess 是否只删除成功的操作日志
     * @return 删除的记录数
     */
    @Transactional
    public int cleanupLogsBefore(LocalDateTime beforeTime, boolean onlySuccess) {
        try {
            int deletedCount;
            if (onlySuccess) {
                deletedCount = auditLogRepository.deleteSuccessLogsByTimestampBefore(beforeTime);
            } else {
                deletedCount = auditLogRepository.deleteByTimestampBefore(beforeTime);
            }
            
            log.info("手动清理审计日志完成，删除{}条记录", deletedCount);
            
            // 记录清理操作的审计日志
            logSystemOperation(AuditOperation.CACHE_CLEANUP, 
                "手动清理审计日志", "删除了" + deletedCount + "条日志");
            
            return deletedCount;
        } catch (Exception e) {
            log.error("手动清理审计日志失败", e);
            throw new RuntimeException("清理审计日志失败", e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建动态查询条件
     * 
     * @param query 查询条件
     * @return JPA Specification
     */
    private Specification<AuditLog> buildSpecification(AuditLogQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 用户相关条件
            if (query.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), query.getUserId()));
            }
            
            if (query.getUsername() != null && !query.getUsername().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("username")), 
                    "%" + query.getUsername().toLowerCase() + "%"
                ));
            }
            
            // 操作相关条件
            if (query.getOperation() != null) {
                predicates.add(criteriaBuilder.equal(root.get("operation"), query.getOperation()));
            }
            
            if (query.getOperations() != null && !query.getOperations().isEmpty()) {
                predicates.add(root.get("operation").in(query.getOperations()));
            }
            
            // 资源相关条件
            if (query.getResourceType() != null && !query.getResourceType().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceType"), query.getResourceType()));
            }
            
            if (query.getResourceId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("resourceId"), query.getResourceId()));
            }
            
            // 结果相关条件
            if (query.getResult() != null && !query.getResult().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("result"), query.getResult()));
            }
            
            if (query.getResults() != null && !query.getResults().isEmpty()) {
                predicates.add(root.get("result").in(query.getResults()));
            }
            
            // 网络相关条件
            if (query.getIpAddress() != null && !query.getIpAddress().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("ipAddress"), "%" + query.getIpAddress() + "%"));
            }
            
            if (query.getIpAddresses() != null && !query.getIpAddresses().isEmpty()) {
                predicates.add(root.get("ipAddress").in(query.getIpAddresses()));
            }
            
            if (query.getUserAgent() != null && !query.getUserAgent().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("userAgent")), 
                    "%" + query.getUserAgent().toLowerCase() + "%"
                ));
            }
            
            if (query.getSessionId() != null && !query.getSessionId().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("sessionId"), query.getSessionId()));
            }
            
            // 时间范围条件
            LocalDateTime effectiveStartTime = query.getEffectiveStartTime();
            LocalDateTime effectiveEndTime = query.getEffectiveEndTime();
            
            if (effectiveStartTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), effectiveStartTime));
            }
            
            if (effectiveEndTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), effectiveEndTime));
            }
            
            // 安全相关条件
            if (query.getMinRiskLevel() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("riskLevel"), query.getMinRiskLevel()));
            }
            
            if (query.getMaxRiskLevel() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("riskLevel"), query.getMaxRiskLevel()));
            }
            
            if (query.getRiskLevels() != null && !query.getRiskLevels().isEmpty()) {
                predicates.add(root.get("riskLevel").in(query.getRiskLevels()));
            }
            
            // 性能相关条件
            if (query.getMinExecutionTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("executionTime"), query.getMinExecutionTime()));
            }
            
            if (query.getMaxExecutionTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("executionTime"), query.getMaxExecutionTime()));
            }
            
            // 内容相关条件
            if (query.getErrorKeyword() != null && !query.getErrorKeyword().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("errorMessage")), 
                    "%" + query.getErrorKeyword().toLowerCase() + "%"
                ));
            }
            
            if (query.getDescriptionKeyword() != null && !query.getDescriptionKeyword().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), 
                    "%" + query.getDescriptionKeyword().toLowerCase() + "%"
                ));
            }
            
            if (query.getTag() != null && !query.getTag().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("tags"), "%" + query.getTag() + "%"));
            }
            
            // 处理状态相关条件
            if (query.getProcessed() != null) {
                predicates.add(criteriaBuilder.equal(root.get("processed"), query.getProcessed()));
            }
            
            if (query.getProcessedBy() != null && !query.getProcessedBy().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("processedBy"), query.getProcessedBy()));
            }
            
            // 特殊查询标识
            if (Boolean.TRUE.equals(query.getSecurityEventsOnly())) {
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("result"), "FAILURE"),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("riskLevel"), 4)
                ));
            }
            
            if (Boolean.TRUE.equals(query.getFailuresOnly())) {
                predicates.add(criteriaBuilder.equal(root.get("result"), "FAILURE"));
            }
            
            if (Boolean.TRUE.equals(query.getHighRiskOnly())) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("riskLevel"), 4));
            }
            
            if (Boolean.TRUE.equals(query.getUnprocessedOnly())) {
                predicates.add(criteriaBuilder.equal(root.get("processed"), false));
            }
            
            if (Boolean.TRUE.equals(query.getSlowOperationsOnly())) {
                // 默认认为执行时间超过5秒的为慢操作
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("executionTime"), 5000L));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 应用排序条件
     * 
     * @param query 查询条件
     * @param pageable 原始分页参数
     * @return 应用排序后的分页参数
     */
    private Pageable applySorting(AuditLogQuery query, Pageable pageable) {
        String sortBy = query.getEffectiveSortBy();
        String sortDirection = query.getEffectiveSortDirection();
        
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        
        Sort sort = Sort.by(direction, sortBy);
        
        return PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            sort
        );
    }
    
    /**
     * 查询用于导出的审计日志
     * 
     * @param query 查询条件
     * @return 审计日志列表
     */
    private List<AuditLog> findLogsForExport(AuditLogQuery query) {
        if (query == null || query.isEmpty()) {
            // 如果没有查询条件，返回最近的记录
            Pageable pageable = PageRequest.of(0, exportLimit, Sort.by(Sort.Direction.DESC, "timestamp"));
            return auditLogRepository.findAll(pageable).getContent();
        }
        
        // 构建动态查询条件
        Specification<AuditLog> specification = buildSpecification(query);
        
        // 创建分页参数
        Pageable pageable = PageRequest.of(
            0, 
            query.getEffectiveSize(), 
            Sort.by(Sort.Direction.valueOf(query.getEffectiveSortDirection()), query.getEffectiveSortBy())
        );
        
        return auditLogRepository.findAll(specification, pageable).getContent();
    }
    
    /**
     * 生成CSV报告
     * 
     * @param logs 审计日志列表
     * @param query 查询条件（用于确定导出字段）
     * @return CSV格式的字节数组
     */
    private byte[] generateCsvReport(List<AuditLog> logs, AuditLogQuery query) {
        StringBuilder csv = new StringBuilder();
        
        // 确定导出字段
        List<String> fields = getExportFields(query);
        
        // CSV头部
        csv.append(String.join(",", getFieldHeaders(fields))).append("\n");
        
        // CSV数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (AuditLog log : logs) {
            List<String> values = new ArrayList<>();
            
            for (String field : fields) {
                String value = getFieldValue(log, field, formatter);
                // 处理CSV中的特殊字符
                if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                    value = "\"" + value.replace("\"", "\"\"") + "\"";
                }
                values.add(value);
            }
            
            csv.append(String.join(",", values)).append("\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * 生成Excel报告
     * 
     * @param logs 审计日志列表
     * @param query 查询条件（用于确定导出字段）
     * @return Excel格式的字节数组
     */
    private byte[] generateExcelReport(List<AuditLog> logs, AuditLogQuery query) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("审计日志");
            
            // 确定导出字段
            List<String> fields = getExportFields(query);
            List<String> headers = getFieldHeaders(fields);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            // 创建数据行
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (int i = 0; i < logs.size(); i++) {
                Row row = sheet.createRow(i + 1);
                AuditLog log = logs.get(i);
                
                for (int j = 0; j < fields.size(); j++) {
                    Cell cell = row.createCell(j);
                    String value = getFieldValue(log, fields.get(j), formatter);
                    cell.setCellValue(value);
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("生成Excel报告失败", e);
            throw new RuntimeException("生成Excel报告失败", e);
        }
    }
    
    /**
     * 获取导出字段列表
     * 
     * @param query 查询条件
     * @return 导出字段列表
     */
    private List<String> getExportFields(AuditLogQuery query) {
        if (query != null && query.getExportFields() != null && !query.getExportFields().isEmpty()) {
            return query.getExportFields();
        }
        
        // 默认导出字段
        return List.of("timestamp", "username", "operation", "resourceType", "resourceId", 
                      "ipAddress", "result", "errorMessage", "executionTime", "riskLevel");
    }
    

    
    /**
     * 统计活跃IP数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃IP数
     */
    public long countActiveIPs(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return auditLogMapperService.countActiveIPs(startTime, endTime);
        } catch (Exception e) {
            log.error("统计活跃IP数失败: startTime={}, endTime={}", startTime, endTime, e);
            return 0L;
        }
    }
    
    // ==================== 新增的MyBatis复杂查询方法 ====================
    
    /**
     * 查询失败登录记录
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 失败登录记录分页结果
     */
    public Page<AuditLog> findFailedLoginAttempts(AuditOperation operation, LocalDateTime startTime, 
                                                 LocalDateTime endTime, Pageable pageable) {
        try {
            return auditLogMapperService.findFailedLoginAttempts(operation, startTime, endTime, pageable);
        } catch (Exception e) {
            log.error("查询失败登录记录失败: operation={}, startTime={}, endTime={}", 
                    operation, startTime, endTime, e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * 查询可疑IP活动
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param minAttempts 最小尝试次数
     * @return 可疑活动记录
     */
    public List<AuditLog> findSuspiciousActivitiesByIp(String ipAddress, LocalDateTime startTime, 
                                                       LocalDateTime endTime, int minAttempts) {
        try {
            return auditLogMapperService.findSuspiciousActivitiesByIp(ipAddress, startTime, endTime, minAttempts);
        } catch (Exception e) {
            log.error("查询可疑IP活动失败: ipAddress={}, startTime={}, endTime={}, minAttempts={}", 
                    ipAddress, startTime, endTime, minAttempts, e);
            return List.of();
        }
    }
    
    /**
     * 查询高风险操作记录
     * 
     * @param minRiskLevel 最小风险级别
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 高风险操作记录分页结果
     */
    public Page<AuditLog> findHighRiskOperations(int minRiskLevel, LocalDateTime startTime, 
                                                LocalDateTime endTime, Pageable pageable) {
        try {
            return auditLogMapperService.findHighRiskOperations(minRiskLevel, startTime, endTime, pageable);
        } catch (Exception e) {
            log.error("查询高风险操作记录失败: minRiskLevel={}, startTime={}, endTime={}", 
                    minRiskLevel, startTime, endTime, e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * 查询未处理的安全事件
     * 
     * @param pageable 分页参数
     * @return 未处理的安全事件分页结果
     */
    public Page<AuditLog> findUnprocessedSecurityEvents(Pageable pageable) {
        try {
            return auditLogMapperService.findUnprocessedSecurityEvents(pageable);
        } catch (Exception e) {
            log.error("查询未处理安全事件失败", e);
            return Page.empty(pageable);
        }
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
        try {
            return auditLogMapperService.findAbnormalLoginsByLocation(userId, currentLocation, startTime, endTime);
        } catch (Exception e) {
            log.error("查询异常登录记录失败: userId={}, currentLocation={}, startTime={}, endTime={}", 
                    userId, currentLocation, startTime, endTime, e);
            return List.of();
        }
    }
    
    /**
     * 查询最近的用户登录记录
     * 
     * @param userId 用户ID
     * @return 最近的登录记录
     */
    public Optional<AuditLog> findLatestLoginByUserId(Long userId) {
        try {
            return auditLogMapperService.findLatestLoginByUserId(userId);
        } catch (Exception e) {
            log.error("查询最近登录记录失败: userId={}", userId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 查询用户的登录历史
     * 
     * @param userId 用户ID
     * @param limit 返回记录数限制
     * @return 登录历史记录
     */
    public List<AuditLog> findLoginHistoryByUserId(Long userId, int limit) {
        try {
            return auditLogMapperService.findLoginHistoryByUserId(userId, limit);
        } catch (Exception e) {
            log.error("查询用户登录历史失败: userId={}, limit={}", userId, limit, e);
            return List.of();
        }
    }
    
    /**
     * 查询指定资源的操作历史
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param pageable 分页参数
     * @return 资源操作历史分页结果
     */
    public Page<AuditLog> findResourceHistory(String resourceType, Long resourceId, Pageable pageable) {
        try {
            return auditLogMapperService.findResourceHistory(resourceType, resourceId, pageable);
        } catch (Exception e) {
            log.error("查询资源操作历史失败: resourceType={}, resourceId={}", resourceType, resourceId, e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * 查询包含指定标签的审计日志
     * 
     * @param tag 标签
     * @param pageable 分页参数
     * @return 包含指定标签的审计日志分页结果
     */
    public Page<AuditLog> findByTag(String tag, Pageable pageable) {
        try {
            return auditLogMapperService.findByTag(tag, pageable);
        } catch (Exception e) {
            log.error("按标签查询审计日志失败: tag={}", tag, e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * 查询执行时间超过阈值的慢操作
     * 
     * @param minExecutionTime 最小执行时间（毫秒）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 慢操作记录分页结果
     */
    public Page<AuditLog> findSlowOperations(long minExecutionTime, LocalDateTime startTime, 
                                            LocalDateTime endTime, Pageable pageable) {
        try {
            return auditLogMapperService.findSlowOperations(minExecutionTime, startTime, endTime, pageable);
        } catch (Exception e) {
            log.error("查询慢操作失败: minExecutionTime={}, startTime={}, endTime={}", 
                    minExecutionTime, startTime, endTime, e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * 批量标记审计日志为已处理
     * 
     * @param ids 审计日志ID列表
     * @param processedBy 处理人
     * @param processNotes 处理备注
     * @return 更新的记录数
     */
    @Transactional
    public int markAsProcessed(List<Long> ids, String processedBy, String processNotes) {
        try {
            int updatedCount = auditLogMapperService.markAsProcessed(ids, processedBy, processNotes);
            log.info("批量标记审计日志为已处理: count={}, processedBy={}", updatedCount, processedBy);
            
            // 记录处理操作的审计日志
            logSystemOperation(AuditOperation.AUDIT_LOG_QUERY, 
                "批量处理审计日志", "标记了" + updatedCount + "条日志为已处理");
            
            return updatedCount;
        } catch (Exception e) {
            log.error("批量标记审计日志为已处理失败: ids={}, processedBy={}", ids, processedBy, e);
            throw new RuntimeException("批量处理审计日志失败", e);
        }
    }
    





    
    /**
     * 获取字段标题
     * 
     * @param fields 字段列表
     * @return 标题列表
     */
    private List<String> getFieldHeaders(List<String> fields) {
        List<String> headers = new ArrayList<>();
        
        for (String field : fields) {
            switch (field) {
                case "timestamp": headers.add("时间"); break;
                case "username": headers.add("用户名"); break;
                case "operation": headers.add("操作"); break;
                case "resourceType": headers.add("资源类型"); break;
                case "resourceId": headers.add("资源ID"); break;
                case "ipAddress": headers.add("IP地址"); break;
                case "userAgent": headers.add("用户代理"); break;
                case "result": headers.add("结果"); break;
                case "errorMessage": headers.add("错误信息"); break;
                case "executionTime": headers.add("执行时间(ms)"); break;
                case "riskLevel": headers.add("风险级别"); break;
                case "location": headers.add("地理位置"); break;
                case "sessionId": headers.add("会话ID"); break;
                case "description": headers.add("描述"); break;
                case "tags": headers.add("标签"); break;
                case "processed": headers.add("已处理"); break;
                case "processedBy": headers.add("处理人"); break;
                default: headers.add(field); break;
            }
        }
        
        return headers;
    }
    
    /**
     * 获取字段值
     * 
     * @param log 审计日志
     * @param field 字段名
     * @param formatter 时间格式化器
     * @return 字段值
     */
    private String getFieldValue(AuditLog log, String field, DateTimeFormatter formatter) {
        switch (field) {
            case "timestamp": 
                return log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "";
            case "username": 
                return log.getUsername() != null ? log.getUsername() : "";
            case "operation": 
                return log.getOperation() != null ? log.getOperationDisplayName() : "";
            case "resourceType": 
                return log.getResourceType() != null ? log.getResourceType() : "";
            case "resourceId": 
                return log.getResourceId() != null ? log.getResourceId().toString() : "";
            case "ipAddress": 
                return log.getIpAddress() != null ? log.getIpAddress() : "";
            case "userAgent": 
                return log.getUserAgent() != null ? log.getUserAgent() : "";
            case "result": 
                return log.getResult() != null ? log.getResult() : "";
            case "errorMessage": 
                return log.getErrorMessage() != null ? log.getErrorMessage() : "";
            case "executionTime": 
                return log.getExecutionTime() != null ? log.getExecutionTime().toString() : "";
            case "riskLevel": 
                return log.getRiskLevel() != null ? log.getRiskLevelDisplay() : "";
            case "location": 
                return log.getLocation() != null ? log.getLocation() : "";
            case "sessionId": 
                return log.getSessionId() != null ? log.getSessionId() : "";
            case "description": 
                return log.getDescription() != null ? log.getDescription() : "";
            case "tags": 
                return log.getTags() != null ? log.getTags() : "";
            case "processed": 
                return log.getProcessed() != null ? (log.getProcessed() ? "是" : "否") : "否";
            case "processedBy": 
                return log.getProcessedBy() != null ? log.getProcessedBy() : "";
            default: 
                return "";
        }
    }
    
    /**
     * 记录系统操作审计日志
     * 
     * @param operation 操作类型
     * @param description 操作描述
     * @param details 详细信息
     */
    private void logSystemOperation(AuditOperation operation, String description, String details) {
        AuditLogRequest request = AuditLogRequest.system(operation, description);
        request.setErrorMessage(details);
        logOperation(request);
    }
    
    // ==================== 统计查询方法 ====================
    
    /**
     * 统计审计日志总数
     * 
     * @param query 查询条件
     * @return 审计日志总数
     */
    public long countLogs(AuditLogQuery query) {
        try {
            if (query == null || query.isEmpty()) {
                return auditLogRepository.count();
            }
            
            Specification<AuditLog> specification = buildSpecification(query);
            return auditLogRepository.count(specification);
            
        } catch (Exception e) {
            log.error("统计审计日志总数失败: query={}", query, e);
            return 0L;
        }
    }
    
    /**
     * 根据ID查询审计日志
     * 
     * @param id 审计日志ID
     * @return 审计日志
     */
    public Optional<AuditLog> findById(Long id) {
        try {
            return auditLogRepository.findById(id);
        } catch (Exception e) {
            log.error("根据ID查询审计日志失败: id={}", id, e);
            return Optional.empty();
        }
    }
    
    /**
     * 按操作类型统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作类型统计结果
     */
    public List<Map<String, Object>> getOperationStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Map<String, Long> results = auditLogMapperService.countOperationsByType(startTime, endTime);
            return results.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("operation", entry.getKey());
                        map.put("count", entry.getValue());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取操作类型统计失败: startTime={}, endTime={}", startTime, endTime, e);
            return List.of();
        }
    }
    
    /**
     * 按用户统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回结果数量限制
     * @return 用户统计结果
     */
    public List<Map<String, Object>> getUserStatistics(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        try {
            Map<String, Long> results = auditLogMapperService.countOperationsByUser(startTime, endTime, limit);
            return results.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("username", entry.getKey());
                        map.put("count", entry.getValue());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户统计失败: startTime={}, endTime={}, limit={}", startTime, endTime, limit, e);
            return List.of();
        }
    }
    
    /**
     * 按IP地址统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回结果数量限制
     * @return IP统计结果
     */
    public List<Map<String, Object>> getIpStatistics(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        try {
            Map<String, Long> results = auditLogMapperService.countOperationsByIp(startTime, endTime, limit);
            return results.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("ipAddress", entry.getKey());
                        map.put("count", entry.getValue());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取IP统计失败: startTime={}, endTime={}, limit={}", startTime, endTime, limit, e);
            return List.of();
        }
    }
    
    /**
     * 按小时统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时统计结果
     */
    public List<Map<String, Object>> getHourlyStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Map<Integer, Long> results = auditLogMapperService.countOperationsByHour(startTime, endTime);
            return results.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("hour", entry.getKey().toString());
                        map.put("count", entry.getValue());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取小时统计失败: startTime={}, endTime={}", startTime, endTime, e);
            return List.of();
        }
    }
    
    /**
     * 按天统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日统计结果
     */
    public List<Map<String, Object>> getDailyStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Map<String, Long> results = auditLogMapperService.countOperationsByDay(startTime, endTime);
            return results.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("date", entry.getKey());
                        map.put("count", entry.getValue());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取日统计失败: startTime={}, endTime={}", startTime, endTime, e);
            return List.of();
        }
    }
    
    /**
     * 统计活跃用户数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃用户数
     */
    public long countActiveUsers(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return auditLogMapperService.countActiveUsers(startTime, endTime);
        } catch (Exception e) {
            log.error("统计活跃用户数失败: startTime={}, endTime={}", startTime, endTime, e);
            return 0L;
        }
    }
    

}