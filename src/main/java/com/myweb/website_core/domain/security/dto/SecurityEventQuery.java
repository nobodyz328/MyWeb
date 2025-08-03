package com.myweb.website_core.domain.security.dto;

import com.myweb.website_core.common.enums.SecurityEventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全事件查询DTO
 * 
 * 用于查询安全事件的条件参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEventQuery {
    
    /**
     * 事件类型列表
     */
    private List<SecurityEventType> eventTypes;
    
    /**
     * 严重级别列表
     */
    private List<Integer> severities;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 源IP地址
     */
    private String sourceIp;
    
    /**
     * 事件状态列表
     */
    private List<String> statuses;
    
    /**
     * 是否已告警
     */
    private Boolean alerted;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 最小风险评分
     */
    private Integer minRiskScore;
    
    /**
     * 最大风险评分
     */
    private Integer maxRiskScore;
    
    /**
     * 关键词搜索（标题或描述）
     */
    private String keyword;
    
    /**
     * 排序字段
     */
    private String sortBy;
    
    /**
     * 排序方向（ASC, DESC）
     */
    private String sortDirection;
    
    /**
     * 创建查询高危事件的条件
     */
    public static SecurityEventQuery highRiskEvents() {
        return SecurityEventQuery.builder()
                .severities(List.of(4, 5))
                .build();
    }
    
    /**
     * 创建查询未处理事件的条件
     */
    public static SecurityEventQuery unhandledEvents() {
        return SecurityEventQuery.builder()
                .statuses(List.of("NEW", "PROCESSING"))
                .build();
    }
    
    /**
     * 创建查询最近24小时事件的条件
     */
    public static SecurityEventQuery recentEvents() {
        LocalDateTime now = LocalDateTime.now();
        return SecurityEventQuery.builder()
                .startTime(now.minusHours(24))
                .endTime(now)
                .sortBy("eventTime")
                .sortDirection("DESC")
                .build();
    }
    
    /**
     * 创建查询特定用户事件的条件
     */
    public static SecurityEventQuery userEvents(Long userId) {
        return SecurityEventQuery.builder()
                .userId(userId)
                .sortBy("eventTime")
                .sortDirection("DESC")
                .build();
    }
    
    /**
     * 创建查询特定IP事件的条件
     */
    public static SecurityEventQuery ipEvents(String sourceIp) {
        return SecurityEventQuery.builder()
                .sourceIp(sourceIp)
                .sortBy("eventTime")
                .sortDirection("DESC")
                .build();
    }
}