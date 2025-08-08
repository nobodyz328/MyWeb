package com.myweb.website_core.infrastructure.config.properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 病毒扫描配置类
 * 
 * 配置病毒扫描相关的参数和策略：
 * - 扫描引擎选择和配置
 * - 扫描超时和性能参数
 * - 隔离和告警策略
 * - 扫描结果处理策略
 * 
 * 符合GB/T 22239-2019二级等保要求的恶意代码防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Getter
@Slf4j
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "app.security.virus-scan")
public class VirusScanConfig {
    
    /**
     * 病毒扫描引擎类型
     * 可选值: mock, clamav
     */
    private String engine = "mock";
    
    /**
     * 是否启用病毒扫描
     */
    private boolean enabled = true;
    
    /**
     * 扫描超时时间（秒）
     */
    private int timeout = 30;
    
    /**
     * 最大扫描文件大小
     */
    private String maxFileSize = "50MB";
    
    /**
     * 扫描失败时是否阻止上传
     */
    private boolean blockOnScanFailure = false;
    
    /**
     * 扫描引擎不可用时是否阻止上传
     */
    private boolean blockOnEngineUnavailable = false;
    
    /**
     * ClamAV配置
     */
    private ClamAVConfig clamav = new ClamAVConfig();
    
    /**
     * 隔离配置
     */
    private QuarantineConfig quarantine = new QuarantineConfig();
    
    /**
     * 告警配置
     */
    private AlertConfig alert = new AlertConfig();
    
    // ==================== Getter/Setter ====================

    public void setEngine(String engine) {
        this.engine = engine;
        log.info("病毒扫描引擎设置为: {}", engine);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("病毒扫描功能{}", enabled ? "已启用" : "已禁用");
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void setBlockOnScanFailure(boolean blockOnScanFailure) {
        this.blockOnScanFailure = blockOnScanFailure;
    }

    public void setBlockOnEngineUnavailable(boolean blockOnEngineUnavailable) {
        this.blockOnEngineUnavailable = blockOnEngineUnavailable;
    }

    public void setClamav(ClamAVConfig clamav) {
        this.clamav = clamav;
    }

    public void setQuarantine(QuarantineConfig quarantine) {
        this.quarantine = quarantine;
    }

    public void setAlert(AlertConfig alert) {
        this.alert = alert;
    }
    
    // ==================== 内部配置类 ====================
    
    /**
     * ClamAV配置
     */
    public static class ClamAVConfig {
        private String host = "localhost";
        private int port = 3310;
        private int connectionTimeout = 5000;
        private int readTimeout = 30000;
        
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public int getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public int getReadTimeout() {
            return readTimeout;
        }
        
        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
    
    /**
     * 隔离配置
     */
    public static class QuarantineConfig {
        private String path = "${java.io.tmpdir}/quarantine";
        private int retentionDays = 30;
        private String maxSize = "100MB";
        private boolean enabled = true;
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public int getRetentionDays() {
            return retentionDays;
        }
        
        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
        
        public String getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(String maxSize) {
            this.maxSize = maxSize;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * 告警配置
     */
    public static class AlertConfig {
        private boolean enabled = true;
        private boolean emailEnabled = true;
        private String adminEmail = "admin@myweb.com";
        private String minThreatLevel = "MEDIUM";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isEmailEnabled() {
            return emailEnabled;
        }
        
        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }
        
        public String getAdminEmail() {
            return adminEmail;
        }
        
        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }
        
        public String getMinThreatLevel() {
            return minThreatLevel;
        }
        
        public void setMinThreatLevel(String minThreatLevel) {
            this.minThreatLevel = minThreatLevel;
        }
    }
}