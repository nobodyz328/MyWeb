package com.myweb.website_core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 文件上传安全配置
 * 
 * 为任务21提供配置支持：
 * - 文件上传安全检查配置
 * - 病毒扫描配置
 * - 文件完整性验证配置
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security.file-upload")
public class FileUploadSecurityConfig {
    
    /**
     * 是否启用文件上传安全检查
     */
    private boolean enabled = true;
    
    /**
     * 是否启用文件魔数验证
     */
    private boolean magicNumberValidation = true;
    
    /**
     * 是否启用恶意代码检查
     */
    private boolean maliciousCodeCheck = true;
    
    /**
     * 是否启用病毒扫描
     */
    private boolean virusScanEnabled = true;
    
    /**
     * 病毒扫描超时时间（秒）
     */
    private int virusScanTimeoutSeconds = 30;
    
    /**
     * 是否启用文件哈希计算
     */
    private boolean fileHashEnabled = true;
    
    /**
     * 哈希算法
     */
    private String hashAlgorithm = "SHA-256";
    
    /**
     * 是否在下载时验证文件完整性
     */
    private boolean verifyIntegrityOnDownload = true;
    
    /**
     * 最大文件大小（字节）
     */
    private long maxFileSize = 5 * 1024 * 1024; // 5MB
    
    /**
     * 允许的文件扩展名
     */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
    
    /**
     * 允许的MIME类型
     */
    private String[] allowedMimeTypes = {
        "image/jpeg", "image/png", "image/gif", "image/webp"
    };
    
    /**
     * 病毒扫描引擎类型
     */
    private String virusScanEngine = "mock"; // mock, clamav
    
    /**
     * ClamAV配置
     */
    private ClamAVConfig clamav = new ClamAVConfig();
    
    @Data
    public static class ClamAVConfig {
        /**
         * ClamAV服务器主机
         */
        private String host = "localhost";
        
        /**
         * ClamAV服务器端口
         */
        private int port = 3310;
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 5000;
        
        /**
         * 扫描超时时间（毫秒）
         */
        private int scanTimeout = 30000;
    }
    
    /**
     * 审计日志配置
     */
    private AuditConfig audit = new AuditConfig();
    
    @Data
    public static class AuditConfig {
        /**
         * 是否记录文件上传审计日志
         */
        private boolean enabled = true;
        
        /**
         * 是否记录病毒扫描结果
         */
        private boolean logVirusScan = true;
        
        /**
         * 是否记录文件完整性验证结果
         */
        private boolean logIntegrityCheck = true;
        
        /**
         * 审计日志级别
         */
        private String logLevel = "INFO";
    }
    
    /**
     * 告警配置
     */
    private AlertConfig alert = new AlertConfig();
    
    @Data
    public static class AlertConfig {
        /**
         * 是否启用安全告警
         */
        private boolean enabled = true;
        
        /**
         * 病毒检测告警
         */
        private boolean virusDetectionAlert = true;
        
        /**
         * 文件完整性违规告警
         */
        private boolean integrityViolationAlert = true;
        
        /**
         * 恶意文件上传告警
         */
        private boolean maliciousFileAlert = true;
        
        /**
         * 告警通知方式
         */
        private String[] notificationMethods = {"log", "email"};
    }
}