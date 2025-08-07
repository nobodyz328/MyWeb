package com.myweb.website_core.application.service.security.IPS.virusprotect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 病毒扫描结果
 * <p>
 * 封装病毒扫描的结果信息，包括：
 * - 扫描状态（成功、失败、错误）
 * - 是否发现病毒
 * - 病毒名称和详细信息
 * - 扫描耗时和引擎信息
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirusScanResult {
    
    /**
     * 扫描状态
     */
    public enum ScanStatus {
        SUCCESS,    // 扫描成功
        FAILURE,    // 扫描失败
        ERROR,      // 扫描错误
        TIMEOUT,    // 扫描超时
        UNAVAILABLE // 扫描引擎不可用
    }
    
    /**
     * 威胁级别
     */
    public enum ThreatLevel {
        NONE,       // 无威胁
        LOW,        // 低威胁
        MEDIUM,     // 中等威胁
        HIGH,       // 高威胁
        CRITICAL    // 严重威胁
    }
    
    /**
     * 扫描状态
     */
    private ScanStatus status;
    
    /**
     * 是否发现病毒
     */
    private boolean virusFound;
    
    /**
     * 病毒名称
     */
    private String virusName;
    
    /**
     * 威胁级别
     */
    private ThreatLevel threatLevel;
    
    /**
     * 扫描详细信息
     */
    private String details;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 扫描引擎名称
     */
    private String engineName;
    
    /**
     * 扫描引擎版本
     */
    private String engineVersion;
    
    /**
     * 病毒库版本
     */
    private String databaseVersion;
    
    /**
     * 扫描开始时间
     */
    private LocalDateTime scanStartTime;
    
    /**
     * 扫描结束时间
     */
    private LocalDateTime scanEndTime;
    
    /**
     * 扫描耗时（毫秒）
     */
    private long scanDurationMs;
    
    /**
     * 文件大小（字节）
     */
    private long fileSize;
    
    /**
     * 文件名
     */
    private String filename;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 建议的处理动作
     */
    private String recommendedAction;
    
    /**
     * 是否需要隔离文件
     */
    private boolean requiresQuarantine;
    
    /**
     * 是否需要发送告警
     */
    private boolean requiresAlert;
    
    /**
     * 创建成功的扫描结果
     */
    public static VirusScanResult success(String filename, Long userId, String username, 
                                         String engineName, long scanDurationMs) {
        return VirusScanResult.builder()
            .status(ScanStatus.SUCCESS)
            .virusFound(false)
            .threatLevel(ThreatLevel.NONE)
            .filename(filename)
            .userId(userId)
            .username(username)
            .engineName(engineName)
            .scanDurationMs(scanDurationMs)
            .scanEndTime(LocalDateTime.now())
            .details("文件扫描完成，未发现威胁")
            .recommendedAction("允许上传")
            .requiresQuarantine(false)
            .requiresAlert(false)
            .build();
    }
    
    /**
     * 创建发现病毒的扫描结果
     */
    public static VirusScanResult virusDetected(String filename, String virusName, 
                                               ThreatLevel threatLevel, Long userId, String username,
                                               String engineName, long scanDurationMs) {
        return VirusScanResult.builder()
            .status(ScanStatus.SUCCESS)
            .virusFound(true)
            .virusName(virusName)
            .threatLevel(threatLevel)
            .filename(filename)
            .userId(userId)
            .username(username)
            .engineName(engineName)
            .scanDurationMs(scanDurationMs)
            .scanEndTime(LocalDateTime.now())
            .details("检测到病毒: " + virusName)
            .recommendedAction("拒绝上传并隔离文件")
            .requiresQuarantine(true)
            .requiresAlert(true)
            .build();
    }
    
    /**
     * 创建扫描失败的结果
     */
    public static VirusScanResult failure(String filename, String errorMessage, 
                                         Long userId, String username, String engineName) {
        return VirusScanResult.builder()
            .status(ScanStatus.FAILURE)
            .virusFound(false)
            .filename(filename)
            .userId(userId)
            .username(username)
            .engineName(engineName)
            .errorMessage(errorMessage)
            .scanEndTime(LocalDateTime.now())
            .details("扫描失败: " + errorMessage)
            .recommendedAction("拒绝上传")
            .requiresQuarantine(false)
            .requiresAlert(true)
            .build();
    }
    
    /**
     * 创建扫描引擎不可用的结果
     */
    public static VirusScanResult unavailable(String filename, Long userId, String username, 
                                             String engineName) {
        return VirusScanResult.builder()
            .status(ScanStatus.UNAVAILABLE)
            .virusFound(false)
            .filename(filename)
            .userId(userId)
            .username(username)
            .engineName(engineName)
            .scanEndTime(LocalDateTime.now())
            .details("病毒扫描引擎不可用")
            .errorMessage("扫描引擎连接失败或未安装")
            .recommendedAction("使用备用验证方式")
            .requiresQuarantine(false)
            .requiresAlert(true)
            .build();
    }
    
    /**
     * 获取扫描结果摘要
     */
    public String getSummary() {
        if (virusFound) {
            return String.format("发现病毒: %s (威胁级别: %s)", virusName, threatLevel);
        } else if (status == ScanStatus.SUCCESS) {
            return "文件安全，未发现威胁";
        } else {
            return String.format("扫描%s: %s", 
                status == ScanStatus.FAILURE ? "失败" : "异常", 
                errorMessage != null ? errorMessage : "未知错误");
        }
    }
    
    /**
     * 是否应该阻止文件上传
     */
    public boolean shouldBlockUpload() {
        return virusFound || status == ScanStatus.FAILURE || status == ScanStatus.ERROR;
    }
}