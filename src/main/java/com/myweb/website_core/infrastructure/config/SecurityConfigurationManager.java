package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * 安全配置管理器
 * 负责安全配置的加载、验证、热更新和默认值处理
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@Slf4j
@Configuration
//@RefreshScope
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityProperties.class)
@Validated
public class SecurityConfigurationManager {

    private final SecurityProperties securityProperties;
    private final Validator validator;

    /**
     * 应用启动完成后验证配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfigurationOnStartup() {
        log.info("开始验证安全配置...");
        
        try {
            validateSecurityConfiguration();
            logConfigurationSummary();
            log.info("安全配置验证完成，所有配置项均有效");
        } catch (Exception e) {
            log.error("安全配置验证失败: {}", e.getMessage(), e);
            throw new IllegalStateException("安全配置验证失败，应用无法启动", e);
        }
    }

    /**
     * 验证安全配置
     */
    public void validateSecurityConfiguration() {
        Set<ConstraintViolation<SecurityProperties>> violations = validator.validate(securityProperties);
        
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("安全配置验证失败:\n");
            for (ConstraintViolation<SecurityProperties> violation : violations) {
                errorMessage.append("- ").append(violation.getPropertyPath())
                          .append(": ").append(violation.getMessage()).append("\n");
            }
            throw new IllegalArgumentException(errorMessage.toString());
        }
        
        // 自定义业务逻辑验证
        validateBusinessRules();
    }

    /**
     * 验证业务规则
     */
    private void validateBusinessRules() {
        // 验证密码策略
        SecurityProperties.PasswordPolicy passwordPolicy = securityProperties.getPasswordPolicy();
        if (passwordPolicy.getMinLength() >= passwordPolicy.getMaxLength()) {
            throw new IllegalArgumentException("密码最小长度不能大于等于最大长度");
        }

        // 验证账户锁定策略
        SecurityProperties.AccountLock accountLock = securityProperties.getAccountLock();
        if (accountLock.getCaptchaThreshold() >= accountLock.getMaxFailedAttempts()) {
            throw new IllegalArgumentException("验证码阈值不能大于等于最大失败次数");
        }

        // 验证文件上传大小
        SecurityProperties.FileUploadSecurity fileUpload = securityProperties.getFileUploadSecurity();
        if (fileUpload.getMaxFileSize() > fileUpload.getMaxTotalSize()) {
            throw new IllegalArgumentException("单个文件最大大小不能超过总文件大小限制");
        }

        // 验证会话配置
        SecurityProperties.Session session = securityProperties.getSession();
        SecurityProperties.SessionCleanup sessionCleanup = securityProperties.getSessionCleanup();
        if (sessionCleanup.getMaxInactiveMinutes() > session.getTimeoutMinutes()) {
            log.warn("会话清理的最大非活跃时间({})大于会话超时时间({}), 可能导致配置冲突", 
                    sessionCleanup.getMaxInactiveMinutes(), session.getTimeoutMinutes());
        }
    }

    /**
     * 记录配置摘要
     */
    private void logConfigurationSummary() {
        log.info("=== 安全配置摘要 ===");
        log.info("全局安全功能: {}", securityProperties.isEnabled() ? "启用" : "禁用");
        log.info("输入验证: {}", securityProperties.getInputValidation().isEnabled() ? "启用" : "禁用");
        log.info("XSS防护: {}", securityProperties.getXssProtection().isEnabled() ? "启用" : "禁用");
        log.info("数据完整性: {}", securityProperties.getDataIntegrity().isEnabled() ? "启用" : "禁用");
        log.info("文件完整性: {}", securityProperties.getFileIntegrity().isEnabled() ? "启用" : "禁用");
        log.info("安全查询: {}", securityProperties.getSafeQuery().isEnabled() ? "启用" : "禁用");
        log.info("文件上传安全: {}", securityProperties.getFileUploadSecurity().isEnabled() ? "启用" : "禁用");
        log.info("数据脱敏: {}", securityProperties.getDataMasking().isEnabled() ? "启用" : "禁用");
        log.info("会话清理: {}", securityProperties.getSessionCleanup().isEnabled() ? "启用" : "禁用");
        log.info("数据删除: {}", securityProperties.getDataDeletion().isEnabled() ? "启用" : "禁用");
        log.info("安全监控: {}", securityProperties.getMonitoring().isEnabled() ? "启用" : "禁用");
        log.info("==================");
    }

    /**
     * 获取安全配置属性
     */
    @Bean
    //@RefreshScope
    public SecurityProperties getSecurityProperties() {
        return securityProperties;
    }

    /**
     * 检查特定安全功能是否启用
     */
    public boolean isSecurityFeatureEnabled(String featureName) {
        if (!securityProperties.isEnabled()) {
            return false;
        }

        return switch (featureName.toLowerCase()) {
            case "input-validation" -> securityProperties.getInputValidation().isEnabled();
            case "xss-protection" -> securityProperties.getXssProtection().isEnabled();
            case "data-integrity" -> securityProperties.getDataIntegrity().isEnabled();
            case "file-integrity" -> securityProperties.getFileIntegrity().isEnabled();
            case "safe-query" -> securityProperties.getSafeQuery().isEnabled();
            case "file-upload-security" -> securityProperties.getFileUploadSecurity().isEnabled();
            case "data-masking" -> securityProperties.getDataMasking().isEnabled();
            case "session-cleanup" -> securityProperties.getSessionCleanup().isEnabled();
            case "data-deletion" -> securityProperties.getDataDeletion().isEnabled();
            case "monitoring" -> securityProperties.getMonitoring().isEnabled();
            case "csrf" -> securityProperties.getCsrf().isEnabled();
            default -> false;
        };
    }

    /**
     * 获取配置的默认值处理
     */
    public <T> T getConfigValueWithDefault(T configValue, T defaultValue) {
        return configValue != null ? configValue : defaultValue;
    }

    /**
     * 刷新配置后的回调
     */
    public void onConfigurationRefresh() {
        log.info("检测到配置更新，重新验证安全配置...");
        try {
            validateSecurityConfiguration();
            logConfigurationSummary();
            log.info("配置热更新完成");
        } catch (Exception e) {
            log.error("配置热更新失败: {}", e.getMessage(), e);
            throw new IllegalStateException("配置热更新失败", e);
        }
    }
}