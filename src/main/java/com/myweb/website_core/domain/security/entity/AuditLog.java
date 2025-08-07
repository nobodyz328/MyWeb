package com.myweb.website_core.domain.security.entity;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.SecurityAuditMessage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * <p>
 * 记录系统中所有重要操作的审计信息，包括：
 * - 用户操作（登录、发帖、评论等）
 * - 管理员操作（用户管理、权限分配等）
 * - 系统操作（备份、配置变更等）
 * - 安全事件（访问拒绝、可疑活动等）
 * <p>
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs", indexes = {
    // 用户ID索引 - 用于按用户查询审计日志
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    
    // 操作类型索引 - 用于按操作类型查询
    @Index(name = "idx_audit_operation", columnList = "operation"),
    
    // 时间戳索引 - 用于按时间范围查询（最常用的查询条件）
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    
    // 资源类型和ID复合索引 - 用于查询特定资源的操作记录
    @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id"),
    
    // IP地址索引 - 用于安全分析和异常检测
    @Index(name = "idx_audit_ip_address", columnList = "ip_address"),
    
    // 操作结果索引 - 用于查询失败的操作
    @Index(name = "idx_audit_result", columnList = "result"),
    
    // 用户名索引 - 用于按用户名查询（支持已删除用户的审计）
    @Index(name = "idx_audit_username", columnList = "username"),
    
    // 会话ID索引 - 用于追踪会话相关的所有操作
    @Index(name = "idx_audit_session_id", columnList = "session_id"),
    
    // 复合索引：用户+时间 - 用于用户操作历史查询
    @Index(name = "idx_audit_user_time", columnList = "user_id, timestamp"),
    
    // 复合索引：操作+时间 - 用于操作统计分析
    @Index(name = "idx_audit_operation_time", columnList = "operation, timestamp"),
    
    // 复合索引：结果+时间 - 用于失败操作分析
    @Index(name = "idx_audit_result_time", columnList = "result, timestamp")
})
public class AuditLog {
    
    /**
     * 主键ID
     * 使用自增长策略，确保审计日志的唯一性和顺序性
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     * 执行操作的用户ID，可能为空（系统操作）
     * 使用Long类型支持大量用户
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * 用户名
     * 执行操作的用户名，冗余存储以支持用户删除后的审计查询
     * 最大长度50字符，支持中英文用户名
     */
    @Column(name = "username", length = 50)
    private String username;
    
    /**
     * 操作类型
     * 使用枚举类型确保操作类型的标准化
     * 存储为字符串便于数据库查询和扩展
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 50)
    private AuditOperation operation;
    
    /**
     * 资源类型
     * 操作涉及的资源类型（如：USER、POST、COMMENT、SYSTEM等）
     * 最大长度30字符，支持未来扩展新的资源类型
     */
    @Column(name = "resource_type", length = 30)
    private String resourceType;
    
    /**
     * 资源ID
     * 操作涉及的具体资源ID，可能为空（如登录操作）
     * 使用Long类型支持大量资源
     */
    @Column(name = "resource_id")
    private Long resourceId;
    
    /**
     * 客户端IP地址
     * 执行操作的客户端IP地址，用于安全分析和异常检测
     * 支持IPv4和IPv6地址格式，最大长度45字符
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * 用户代理字符串
     * 客户端的User-Agent信息，用于设备和浏览器识别
     * 最大长度500字符，存储完整的User-Agent信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 操作结果
     * 操作执行的结果状态：SUCCESS（成功）、FAILURE（失败）、ERROR（错误）
     * 最大长度20字符，支持未来扩展新的结果状态
     */
    @Column(name = "result", nullable = false, length = 20)
    private String result;
    
    /**
     * 错误信息
     * 当操作失败或出错时的详细错误信息
     * 使用TEXT类型支持长错误信息，便于问题诊断
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 请求数据
     * 操作的请求参数，以JSON格式存储
     * 用于详细的操作追踪和问题重现
     * 敏感信息（如密码）应在记录前进行脱敏处理
     */
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;
    
    /**
     * 响应数据
     * 操作的响应结果，以JSON格式存储
     * 用于操作结果的详细记录和分析
     * 敏感信息应在记录前进行脱敏处理
     */
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
    
    /**
     * 执行时间
     * 操作的执行耗时，单位为毫秒
     * 用于性能监控和异常检测
     */
    @Column(name = "execution_time")
    private Long executionTime;
    
    /**
     * 操作时间戳
     * 操作发生的精确时间，使用LocalDateTime类型
     * 不能为空，用于时间范围查询和日志排序
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * 会话ID
     * 用户会话的唯一标识，用于关联同一会话的所有操作
     * 最大长度100字符，支持各种会话ID格式
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    /**
     * 请求ID
     * 单次请求的唯一标识，用于关联同一请求的多个操作
     * 最大长度50字符，通常使用UUID格式
     */
    @Column(name = "request_id", length = 50)
    private String requestId;
    
    /**
     * 操作描述
     * 操作的详细描述信息，便于审计人员理解
     * 最大长度500字符，支持中英文描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 风险级别
     * 操作的风险级别（1-5），用于安全分析和告警
     * 1=最低风险，5=最高风险
     */
    @Column(name = "risk_level")
    private Integer riskLevel;
    
    /**
     * 地理位置信息
     * 基于IP地址解析的地理位置信息（如：北京市 海淀区）
     * 最大长度100字符，用于异常登录检测
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * 标签
     * 用于分类和标记的标签信息，以逗号分隔
     * 如：security,login,failure 等
     * 最大长度200字符
     */
    @Column(name = "tags", length = 200)
    private String tags;
    
    /**
     * 是否已处理
     * 标识该审计日志是否已被处理（如安全事件是否已响应）
     * 默认为false，用于安全事件管理
     */
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
    
    /**
     * 处理时间
     * 审计日志被处理的时间
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    /**
     * 处理人
     * 处理该审计日志的管理员用户名
     * 最大长度50字符
     */
    @Column(name = "processed_by", length = 50)
    private String processedBy;
    
    /**
     * 处理备注
     * 处理该审计日志时的备注信息
     * 最大长度500字符
     */
    @Column(name = "process_notes", length = 500)
    private String processNotes;
    
    // ==================== JPA生命周期回调 ====================
    
    /**
     * 实体持久化前的回调
     * 自动设置时间戳和风险级别
     */
    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        
        // 根据操作类型自动设置风险级别
        if (this.riskLevel == null && this.operation != null) {
            this.riskLevel = this.operation.getRiskLevel();
        }
        
        // 设置默认结果状态
        if (this.result == null) {
            this.result = "SUCCESS";
        }
    }
    
    // ==================== 业务方法 ====================
    
    /**
     * 检查操作是否成功
     * 
     * @return 操作是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.result);
    }
    
    /**
     * 检查操作是否失败
     * 
     * @return 操作是否失败
     */
    public boolean isFailure() {
        return "FAILURE".equals(this.result);
    }
    
    /**
     * 检查操作是否出错
     * 
     * @return 操作是否出错
     */
    public boolean isError() {
        return "ERROR".equals(this.result);
    }
    
    /**
     * 检查是否为高风险操作
     * 
     * @return 是否为高风险操作（风险级别>=4）
     */
    public boolean isHighRisk() {
        return this.riskLevel != null && this.riskLevel >= 4;
    }
    
    /**
     * 检查是否为安全相关操作
     * 
     * @return 是否为安全相关操作
     */
    public boolean isSecurityOperation() {
        return this.operation != null && this.operation.isSecurityOperation();
    }
    
    /**
     * 检查是否为管理员操作
     * 
     * @return 是否为管理员操作
     */
    public boolean isAdminOperation() {
        return this.operation != null && this.operation.isAdminOperation();
    }
    
    /**
     * 添加标签
     * 
     * @param tag 要添加的标签
     */
    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        if (this.tags == null || this.tags.isEmpty()) {
            this.tags = tag.trim();
        } else if (!this.tags.contains(tag.trim())) {
            this.tags += "," + tag.trim();
        }
    }
    
    /**
     * 移除标签
     * 
     * @param tag 要移除的标签
     */
    public void removeTag(String tag) {
        if (tag == null || this.tags == null) {
            return;
        }
        
        String[] tagArray = this.tags.split(",");
        StringBuilder newTags = new StringBuilder();
        
        for (String existingTag : tagArray) {
            if (!existingTag.trim().equals(tag.trim())) {
                if (newTags.length() > 0) {
                    newTags.append(",");
                }
                newTags.append(existingTag.trim());
            }
        }
        
        this.tags = newTags.toString();
    }
    
    /**
     * 检查是否包含指定标签
     * 
     * @param tag 要检查的标签
     * @return 是否包含指定标签
     */
    public boolean hasTag(String tag) {
        if (tag == null || this.tags == null) {
            return false;
        }
        
        return this.tags.contains(tag.trim());
    }
    
    /**
     * 标记为已处理
     * 
     * @param processedBy 处理人
     * @param processNotes 处理备注
     */
    public void markAsProcessed(String processedBy, String processNotes) {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
        this.processNotes = processNotes;
    }
    
    /**
     * 标记为未处理
     */
    public void markAsUnprocessed() {
        this.processed = false;
        this.processedAt = null;
        this.processedBy = null;
        this.processNotes = null;
    }
    
    /**
     * 获取操作的显示名称
     * 
     * @return 操作的显示名称
     */
    public String getOperationDisplayName() {
        return this.operation != null ? this.operation.getName() : "未知操作";
    }
    
    /**
     * 获取操作的描述信息
     * 
     * @return 操作的描述信息
     */
    public String getOperationDescription() {
        return this.operation != null ? this.operation.getDescription() : "未知操作";
    }
    
    /**
     * 获取格式化的时间戳字符串
     * 
     * @return 格式化的时间戳字符串
     */
    public String getFormattedTimestamp() {
        if (this.timestamp == null) {
            return "";
        }
        
        return this.timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 获取执行时间的显示字符串
     * 
     * @return 执行时间的显示字符串
     */
    public String getExecutionTimeDisplay() {
        if (this.executionTime == null) {
            return "未知";
        }
        
        if (this.executionTime < 1000) {
            return this.executionTime + "ms";
        } else {
            return String.format("%.2fs", this.executionTime / 1000.0);
        }
    }
    
    /**
     * 获取风险级别的显示字符串
     * 
     * @return 风险级别的显示字符串
     */
    public String getRiskLevelDisplay() {
        if (this.riskLevel == null) {
            return "未知";
        }
        
        switch (this.riskLevel) {
            case 1: return "最低";
            case 2: return "低";
            case 3: return "中";
            case 4: return "高";
            case 5: return "最高";
            default: return "未知";
        }
    }
    
    @Override
    public String toString() {
        return String.format("AuditLog{id=%d, operation=%s, username='%s', result='%s', timestamp=%s}", 
                           id, operation, username, result, 
                           timestamp != null ? timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "null");
    }

    public AuditLog(){

    }
    
    public AuditLog(SecurityAuditMessage  message){
        this.setUserId(message.getUserId());
        this.setUsername(message.getUsername());
        this.setOperation(message.getOperation());
        this.setResourceType(message.getResourceType());
        this.setResourceId(message.getResourceId());
        this.setIpAddress(message.getIpAddress());
        this.setUserAgent(message.getUserAgent());
        this.setResult(message.getResult());
        this.setErrorMessage(message.getErrorMessage());
        this.setRequestData(message.getRequestData());
        this.setResponseData(message.getResponseData());
        this.setExecutionTime(message.getExecutionTime());
        this.setTimestamp(message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now());
        this.setSessionId(message.getSessionId());
        this.setRequestId(message.getRequestId());
        this.setDescription(message.getDescription());
        this.setRiskLevel(message.getRiskLevel());
        this.setLocation(message.getLocation());
        this.setTags(message.getTags());
    }
    
    public AuditLog(com.myweb.website_core.domain.security.dto.UnifiedSecurityMessage message){
        this.setUserId(message.getUserId());
        this.setUsername(message.getUsername());
        this.setOperation(message.getOperation());
        this.setResourceType(message.getResourceType());
        this.setResourceId(message.getResourceId());
        this.setIpAddress(message.getIpAddress());
        this.setUserAgent(message.getUserAgent());
        this.setResult(message.getResult());
        this.setErrorMessage(message.getErrorMessage());
        this.setRequestData(message.getRequestData());
        this.setResponseData(message.getResponseData());
        this.setExecutionTime(message.getExecutionTime());
        this.setTimestamp(message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now());
        this.setSessionId(message.getSessionId());
        this.setRequestId(message.getRequestId());
        this.setDescription(message.getDescription());
        this.setRiskLevel(message.getRiskLevel());
        this.setLocation(message.getLocation());
        this.setTags(message.getTags());
    }
}