package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.common.enums.AuditOperation;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志查询条件DTO
 * 
 * 用于封装审计日志的复杂查询条件，支持：
 * - 多维度条件组合查询
 * - 时间范围查询
 * - 模糊匹配查询
 * - 排序和分页参数
 * 
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogQuery {
    
    // ==================== 基础查询条件 ====================
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名（支持模糊匹配）
     */
    private String username;
    
    /**
     * 操作类型
     */
    private AuditOperation operation;
    
    /**
     * 操作类型列表（多选）
     */
    private List<AuditOperation> operations;
    
    /**
     * 资源类型
     */
    private String resourceType;
    
    /**
     * 资源ID
     */
    private Long resourceId;
    
    /**
     * 操作结果
     */
    private String result;
    
    /**
     * 操作结果列表（多选）
     */
    private List<String> results;
    
    // ==================== 网络相关查询条件 ====================
    
    /**
     * IP地址（支持模糊匹配）
     */
    private String ipAddress;
    
    /**
     * IP地址列表（多选）
     */
    private List<String> ipAddresses;
    
    /**
     * 用户代理字符串（支持模糊匹配）
     */
    private String userAgent;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    // ==================== 时间范围查询条件 ====================
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 最近N小时内的记录
     */
    private Integer lastHours;
    
    /**
     * 最近N天内的记录
     */
    private Integer lastDays;
    
    // ==================== 安全相关查询条件 ====================
    
    /**
     * 最小风险级别
     */
    private Integer minRiskLevel;
    
    /**
     * 最大风险级别
     */
    private Integer maxRiskLevel;
    
    /**
     * 风险级别列表（多选）
     */
    private List<Integer> riskLevels;
    
    /**
     * 地理位置（支持模糊匹配）
     */
    private String location;
    
    /**
     * 设备指纹
     */
    private String deviceFingerprint;
    
    // ==================== 性能相关查询条件 ====================
    
    /**
     * 最小执行时间（毫秒）
     */
    private Long minExecutionTime;
    
    /**
     * 最大执行时间（毫秒）
     */
    private Long maxExecutionTime;
    
    // ==================== 内容相关查询条件 ====================
    
    /**
     * 错误信息关键词（支持模糊匹配）
     */
    private String errorKeyword;
    
    /**
     * 描述关键词（支持模糊匹配）
     */
    private String descriptionKeyword;
    
    /**
     * 标签
     */
    private String tag;
    
    /**
     * 标签列表（多选）
     */
    private List<String> tags;
    
    // ==================== 处理状态查询条件 ====================
    
    /**
     * 是否已处理
     */
    private Boolean processed;
    
    /**
     * 处理人
     */
    private String processedBy;
    
    /**
     * 处理开始时间
     */
    private LocalDateTime processedStartTime;
    
    /**
     * 处理结束时间
     */
    private LocalDateTime processedEndTime;
    
    // ==================== 排序和分页参数 ====================
    
    /**
     * 排序字段
     */
    private String sortBy;
    
    /**
     * 排序方向（ASC/DESC）
     */
    private String sortDirection;
    
    /**
     * 页码（从0开始）
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    // ==================== 特殊查询标识 ====================
    
    /**
     * 是否只查询安全事件
     */
    private Boolean securityEventsOnly;
    
    /**
     * 是否只查询失败操作
     */
    private Boolean failuresOnly;
    
    /**
     * 是否只查询高风险操作
     */
    private Boolean highRiskOnly;
    
    /**
     * 是否只查询管理员操作
     */
    private Boolean adminOperationsOnly;
    
    /**
     * 是否只查询未处理事件
     */
    private Boolean unprocessedOnly;
    
    /**
     * 是否只查询慢操作
     */
    private Boolean slowOperationsOnly;
    
    // ==================== 统计查询参数 ====================
    
    /**
     * 是否进行统计查询
     */
    private Boolean statisticsMode;
    
    /**
     * 统计分组字段
     */
    private String groupBy;
    
    /**
     * 统计结果限制数量
     */
    private Integer statisticsLimit;
    
    // ==================== 导出相关参数 ====================
    
    /**
     * 是否为导出查询
     */
    private Boolean exportMode;
    
    /**
     * 导出格式（CSV/EXCEL/JSON）
     */
    private String exportFormat;
    
    /**
     * 导出字段列表
     */
    private List<String> exportFields;
    
    // ==================== 业务方法 ====================
    
    /**
     * 检查是否有时间范围条件
     * 
     * @return 是否有时间范围条件
     */
    public boolean hasTimeRange() {
        return startTime != null || endTime != null || lastHours != null || lastDays != null;
    }
    
    /**
     * 检查是否有用户相关条件
     * 
     * @return 是否有用户相关条件
     */
    public boolean hasUserConditions() {
        return userId != null || (username != null && !username.trim().isEmpty());
    }
    
    /**
     * 检查是否有操作相关条件
     * 
     * @return 是否有操作相关条件
     */
    public boolean hasOperationConditions() {
        return operation != null || (operations != null && !operations.isEmpty());
    }
    
    /**
     * 检查是否有网络相关条件
     * 
     * @return 是否有网络相关条件
     */
    public boolean hasNetworkConditions() {
        return (ipAddress != null && !ipAddress.trim().isEmpty()) ||
               (ipAddresses != null && !ipAddresses.isEmpty()) ||
               (userAgent != null && !userAgent.trim().isEmpty()) ||
               (sessionId != null && !sessionId.trim().isEmpty());
    }
    
    /**
     * 检查是否有安全相关条件
     * 
     * @return 是否有安全相关条件
     */
    public boolean hasSecurityConditions() {
        return minRiskLevel != null || maxRiskLevel != null ||
               (riskLevels != null && !riskLevels.isEmpty()) ||
               Boolean.TRUE.equals(securityEventsOnly) ||
               Boolean.TRUE.equals(highRiskOnly);
    }
    
    /**
     * 检查是否有性能相关条件
     * 
     * @return 是否有性能相关条件
     */
    public boolean hasPerformanceConditions() {
        return minExecutionTime != null || maxExecutionTime != null ||
               Boolean.TRUE.equals(slowOperationsOnly);
    }
    
    /**
     * 检查是否有内容相关条件
     * 
     * @return 是否有内容相关条件
     */
    public boolean hasContentConditions() {
        return (errorKeyword != null && !errorKeyword.trim().isEmpty()) ||
               (descriptionKeyword != null && !descriptionKeyword.trim().isEmpty()) ||
               (tag != null && !tag.trim().isEmpty()) ||
               (tags != null && !tags.isEmpty());
    }
    
    /**
     * 检查是否有处理状态相关条件
     * 
     * @return 是否有处理状态相关条件
     */
    public boolean hasProcessingConditions() {
        return processed != null ||
               (processedBy != null && !processedBy.trim().isEmpty()) ||
               processedStartTime != null || processedEndTime != null ||
               Boolean.TRUE.equals(unprocessedOnly);
    }
    
    /**
     * 获取实际的开始时间
     * 优先使用明确指定的时间，其次使用相对时间
     * 
     * @return 实际的开始时间
     */
    public LocalDateTime getEffectiveStartTime() {
        if (startTime != null) {
            return startTime;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (lastHours != null) {
            return now.minusHours(lastHours);
        }
        
        if (lastDays != null) {
            return now.minusDays(lastDays);
        }
        
        return null;
    }
    
    /**
     * 获取实际的结束时间
     * 
     * @return 实际的结束时间
     */
    public LocalDateTime getEffectiveEndTime() {
        return endTime != null ? endTime : LocalDateTime.now();
    }
    
    /**
     * 获取默认的排序字段
     * 
     * @return 默认的排序字段
     */
    public String getEffectiveSortBy() {
        return sortBy != null ? sortBy : "timestamp";
    }
    
    /**
     * 获取默认的排序方向
     * 
     * @return 默认的排序方向
     */
    public String getEffectiveSortDirection() {
        return sortDirection != null ? sortDirection : "DESC";
    }
    
    /**
     * 获取默认的页码
     * 
     * @return 默认的页码
     */
    public Integer getEffectivePage() {
        return page != null ? page : 0;
    }
    
    /**
     * 获取默认的每页大小
     * 
     * @return 默认的每页大小
     */
    public Integer getEffectiveSize() {
        if (Boolean.TRUE.equals(exportMode)) {
            return size != null ? size : 10000; // 导出模式下默认更大的页面大小
        }
        return size != null ? size : 20;
    }
    
    /**
     * 检查查询条件是否为空
     * 
     * @return 查询条件是否为空
     */
    public boolean isEmpty() {
        return !hasTimeRange() && !hasUserConditions() && !hasOperationConditions() &&
               !hasNetworkConditions() && !hasSecurityConditions() && !hasPerformanceConditions() &&
               !hasContentConditions() && !hasProcessingConditions() &&
               (result == null || result.trim().isEmpty()) &&
               (resourceType == null || resourceType.trim().isEmpty()) &&
               resourceId == null;
    }
    
    /**
     * 创建用于安全事件查询的查询条件
     * 
     * @return 安全事件查询条件
     */
    public static AuditLogQuery forSecurityEvents() {
        return AuditLogQuery.builder()
                .securityEventsOnly(true)
                .sortBy("riskLevel")
                .sortDirection("DESC")
                .build();
    }
    
    /**
     * 创建用于失败操作查询的查询条件
     * 
     * @return 失败操作查询条件
     */
    public static AuditLogQuery forFailures() {
        return AuditLogQuery.builder()
                .failuresOnly(true)
                .result("FAILURE")
                .sortBy("timestamp")
                .sortDirection("DESC")
                .build();
    }
    
    /**
     * 创建用于用户活动查询的查询条件
     * 
     * @param userId 用户ID
     * @return 用户活动查询条件
     */
    public static AuditLogQuery forUserActivity(Long userId) {
        return AuditLogQuery.builder()
                .userId(userId)
                .sortBy("timestamp")
                .sortDirection("DESC")
                .build();
    }
    
    /**
     * 创建用于最近活动查询的查询条件
     * 
     * @param hours 最近小时数
     * @return 最近活动查询条件
     */
    public static AuditLogQuery forRecentActivity(int hours) {
        return AuditLogQuery.builder()
                .lastHours(hours)
                .sortBy("timestamp")
                .sortDirection("DESC")
                .build();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AuditLogQuery{");
        
        if (userId != null) sb.append("userId=").append(userId).append(", ");
        if (username != null) sb.append("username='").append(username).append("', ");
        if (operation != null) sb.append("operation=").append(operation).append(", ");
        if (result != null) sb.append("result='").append(result).append("', ");
        if (ipAddress != null) sb.append("ipAddress='").append(ipAddress).append("', ");
        if (startTime != null) sb.append("startTime=").append(startTime).append(", ");
        if (endTime != null) sb.append("endTime=").append(endTime).append(", ");
        if (minRiskLevel != null) sb.append("minRiskLevel=").append(minRiskLevel).append(", ");
        
        if (sb.length() > 15) {
            sb.setLength(sb.length() - 2); // 移除最后的 ", "
        }
        sb.append("}");
        
        return sb.toString();
    }
}