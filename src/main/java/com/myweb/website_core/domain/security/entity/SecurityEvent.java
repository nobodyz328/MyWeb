package com.myweb.website_core.domain.security.entity;

import com.myweb.website_core.common.enums.SecurityEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 安全事件实体
 * 
 * 记录系统中发生的各种安全事件，用于安全监控和告警
 * 符合GB/T 22239-2019二级等保要求的安全审计功能
 */
@Entity
@Table(name = "security_events", indexes = {
    @Index(name = "idx_security_event_type", columnList = "event_type"),
    @Index(name = "idx_security_event_user", columnList = "user_id"),
    @Index(name = "idx_security_event_ip", columnList = "source_ip"),
    @Index(name = "idx_security_event_time", columnList = "event_time"),
    @Index(name = "idx_security_event_severity", columnList = "severity"),
    @Index(name = "idx_security_event_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 事件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SecurityEventType eventType;
    
    /**
     * 事件标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    /**
     * 事件描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 严重级别（1-5）
     */
    @Column(name = "severity", nullable = false)
    private Integer severity;
    
    /**
     * 相关用户ID
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * 相关用户名
     */
    @Column(name = "username", length = 50)
    private String username;
    
    /**
     * 源IP地址
     */
    @Column(name = "source_ip", length = 45)
    private String sourceIp;
    
    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 请求URI
     */
    @Column(name = "request_uri", length = 500)
    private String requestUri;
    
    /**
     * 请求方法
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    /**
     * 事件详细数据（JSON格式）
     */
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;
    
    /**
     * 事件发生时间
     */
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
    
    /**
     * 事件状态（NEW, PROCESSING, RESOLVED, IGNORED）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    /**
     * 是否已告警
     */
    @Column(name = "alerted", nullable = false)
    private Boolean alerted;
    
    /**
     * 告警时间
     */
    @Column(name = "alert_time")
    private LocalDateTime alertTime;
    
    /**
     * 处理人员
     */
    @Column(name = "handled_by", length = 50)
    private String handledBy;
    
    /**
     * 处理时间
     */
    @Column(name = "handled_time")
    private LocalDateTime handledTime;
    
    /**
     * 处理备注
     */
    @Column(name = "handle_notes", columnDefinition = "TEXT")
    private String handleNotes;
    
    /**
     * 风险评分（0-100）
     */
    @Column(name = "risk_score")
    private Integer riskScore;
    
    /**
     * 相关事件数量（同类型事件在时间窗口内的数量）
     */
    @Column(name = "related_event_count")
    private Integer relatedEventCount;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 事件状态枚举
     */
    @Getter
    public enum Status {
        NEW("NEW", "新建"),
        PROCESSING("PROCESSING", "处理中"),
        RESOLVED("RESOLVED", "已解决"),
        IGNORED("IGNORED", "已忽略");
        
        private final String code;
        private final String description;
        
        Status(String code, String description) {
            this.code = code;
            this.description = description;
        }

    }
    
    /**
     * 检查是否为高危事件
     */
    public boolean isHighRisk() {
        return severity != null && severity >= 4;
    }
    
    /**
     * 检查是否需要立即告警
     */
    public boolean requiresImmediateAlert() {
        return severity != null && severity == 5;
    }
    
    /**
     * 检查事件是否已处理
     */
    public boolean isHandled() {
        return Status.RESOLVED.getCode().equals(status) || Status.IGNORED.getCode().equals(status);
    }
    
    /**
     * 设置为已告警状态
     */
    public void markAsAlerted() {
        this.alerted = true;
        this.alertTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 设置处理信息
     */
    public void setHandleInfo(String handledBy, String handleNotes, Status status) {
        this.handledBy = handledBy;
        this.handleNotes = handleNotes;
        this.status = status.getCode();
        this.handledTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (eventTime == null) {
            eventTime = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.NEW.getCode();
        }
        if (alerted == null) {
            alerted = false;
        }
        if (severity == null && eventType != null) {
            severity = eventType.getSeverity();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}