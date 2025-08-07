package com.myweb.website_core.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据完整性配置类
 * 
 * 配置数据完整性检查相关的参数和功能
 * 符合GB/T 22239-2019数据完整性保护要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@ConfigurationProperties(prefix = "app.data-integrity")
public class DataIntegrityConfig {
    
    /**
     * 是否启用数据完整性检查
     */
    private boolean enabled = true;
    
    /**
     * 默认哈希算法
     */
    private String defaultHashAlgorithm = "SHA-256";
    
    /**
     * 是否启用定时完整性检查
     */
    private boolean scheduledCheckEnabled = true;
    
    /**
     * 完整性检查的cron表达式
     */
    private String checkCronExpression = "0 0 4 * * ?";
    
    /**
     * 深度检查的cron表达式
     */
    private String deepCheckCronExpression = "0 0 3 ? * SUN";
    
    /**
     * 是否启用完整性告警
     */
    private boolean alertEnabled = true;
    
    /**
     * 告警阈值（发现问题数量超过此值时发送告警）
     */
    private int alertThreshold = 10;
    
    /**
     * 哈希重新计算的间隔天数
     */
    private int hashRecalculationDays = 30;
    
    /**
     * 是否在实体保存时自动计算哈希
     */
    private boolean autoCalculateHash = true;
    
    /**
     * 是否启用实体完整性验证
     */
    private boolean entityVerificationEnabled = true;
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("数据完整性检查功能{}", enabled ? "已启用" : "已禁用");
    }
    
    public String getDefaultHashAlgorithm() {
        return defaultHashAlgorithm;
    }
    
    public void setDefaultHashAlgorithm(String defaultHashAlgorithm) {
        this.defaultHashAlgorithm = defaultHashAlgorithm;
        log.info("默认哈希算法设置为: {}", defaultHashAlgorithm);
    }
    
    public boolean isScheduledCheckEnabled() {
        return scheduledCheckEnabled;
    }
    
    public void setScheduledCheckEnabled(boolean scheduledCheckEnabled) {
        this.scheduledCheckEnabled = scheduledCheckEnabled;
        log.info("定时完整性检查功能{}", scheduledCheckEnabled ? "已启用" : "已禁用");
    }
    
    public String getCheckCronExpression() {
        return checkCronExpression;
    }
    
    public void setCheckCronExpression(String checkCronExpression) {
        this.checkCronExpression = checkCronExpression;
        log.info("完整性检查定时任务设置为: {}", checkCronExpression);
    }
    
    public String getDeepCheckCronExpression() {
        return deepCheckCronExpression;
    }
    
    public void setDeepCheckCronExpression(String deepCheckCronExpression) {
        this.deepCheckCronExpression = deepCheckCronExpression;
        log.info("深度完整性检查定时任务设置为: {}", deepCheckCronExpression);
    }
    
    public boolean isAlertEnabled() {
        return alertEnabled;
    }
    
    public void setAlertEnabled(boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
        log.info("完整性告警功能{}", alertEnabled ? "已启用" : "已禁用");
    }
    
    public int getAlertThreshold() {
        return alertThreshold;
    }
    
    public void setAlertThreshold(int alertThreshold) {
        this.alertThreshold = alertThreshold;
        log.info("完整性告警阈值设置为: {}", alertThreshold);
    }
    
    public int getHashRecalculationDays() {
        return hashRecalculationDays;
    }
    
    public void setHashRecalculationDays(int hashRecalculationDays) {
        this.hashRecalculationDays = hashRecalculationDays;
        log.info("哈希重新计算间隔设置为: {}天", hashRecalculationDays);
    }
    
    public boolean isAutoCalculateHash() {
        return autoCalculateHash;
    }
    
    public void setAutoCalculateHash(boolean autoCalculateHash) {
        this.autoCalculateHash = autoCalculateHash;
        log.info("自动计算哈希功能{}", autoCalculateHash ? "已启用" : "已禁用");
    }
    
    public boolean isEntityVerificationEnabled() {
        return entityVerificationEnabled;
    }
    
    public void setEntityVerificationEnabled(boolean entityVerificationEnabled) {
        this.entityVerificationEnabled = entityVerificationEnabled;
        log.info("实体完整性验证功能{}", entityVerificationEnabled ? "已启用" : "已禁用");
    }
    
    /**
     * 获取配置摘要信息
     * 
     * @return 配置摘要
     */
    public String getConfigSummary() {
        return String.format(
            "DataIntegrityConfig{enabled=%s, algorithm=%s, scheduledCheck=%s, alert=%s, threshold=%d}",
            enabled, defaultHashAlgorithm, scheduledCheckEnabled, alertEnabled, alertThreshold
        );
    }
}