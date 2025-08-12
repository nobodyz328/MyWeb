package com.myweb.website_core.presentation.controller.admin;

import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import com.myweb.website_core.infrastructure.config.SecurityConfigurationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 安全配置管理控制器
 * 提供安全配置的查看、验证和热更新功能
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/security/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SecurityConfigurationController {

    private final SecurityConfigurationManager configurationManager;
    private final SecurityProperties securityProperties;
   // private final ContextRefresher;

    /**
     * 获取当前安全配置摘要
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getConfigurationSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            
            // 全局配置
            summary.put("globalEnabled", securityProperties.isEnabled());
            
            // 各功能模块状态
            Map<String, Boolean> features = new HashMap<>();
            features.put("inputValidation", securityProperties.getInputValidation().isEnabled());
            features.put("xssProtection", securityProperties.getXssProtection().isEnabled());
            features.put("dataIntegrity", securityProperties.getDataIntegrity().isEnabled());
            features.put("fileIntegrity", securityProperties.getFileIntegrity().isEnabled());
            features.put("safeQuery", securityProperties.getSafeQuery().isEnabled());
            features.put("fileUploadSecurity", securityProperties.getFileUploadSecurity().isEnabled());
            features.put("dataMasking", securityProperties.getDataMasking().isEnabled());
            features.put("sessionCleanup", securityProperties.getSessionCleanup().isEnabled());
            features.put("dataDeletion", securityProperties.getDataDeletion().isEnabled());
            features.put("monitoring", securityProperties.getMonitoring().isEnabled());
            features.put("csrf", securityProperties.getCsrf().isEnabled());
            
            summary.put("features", features);
            
            // 关键配置参数
            Map<String, Object> keySettings = new HashMap<>();
            keySettings.put("passwordMinLength", securityProperties.getPasswordPolicy().getMinLength());
            keySettings.put("maxFailedAttempts", securityProperties.getAccountLock().getMaxFailedAttempts());
            keySettings.put("sessionTimeoutMinutes", securityProperties.getSession().getTimeoutMinutes());
            keySettings.put("maxFileSize", securityProperties.getFileUploadSecurity().getMaxFileSize());
            keySettings.put("dataIntegrityHashAlgorithm", securityProperties.getDataIntegrity().getHashAlgorithm());
            
            summary.put("keySettings", keySettings);
            
            return ApiResponse.success(summary);
        } catch (Exception e) {
            log.error("获取安全配置摘要失败", e);
            return ApiResponse.error("获取配置摘要失败: " + e.getMessage());
        }
    }

    /**
     * 验证当前安全配置
     */
    @PostMapping("/validate")
    public ApiResponse<String> validateConfiguration() {
        try {
            configurationManager.validateSecurityConfiguration();
            return ApiResponse.success("安全配置验证通过");
        } catch (Exception e) {
            log.error("安全配置验证失败", e);
            return ApiResponse.error("配置验证失败: " + e.getMessage());
        }
    }

    /**
     * 刷新安全配置（热更新）
     */
    @PostMapping("/refresh")
    public ApiResponse<String> refreshConfiguration() {
        try {
            log.info("开始刷新安全配置...");
            
            // 刷新Spring Cloud配置
            //Set<String> refreshedKeys = contextRefresher.refresh();
            
            // 验证新配置
            configurationManager.onConfigurationRefresh();
            
            //log.info("安全配置刷新完成，更新了 {} 个配置项", refreshedKeys.size());
            
            return ApiResponse.success("配置刷新成功，更新了 " + "refreshedKeys.size()" + " 个配置项");
        } catch (Exception e) {
            log.error("安全配置刷新失败", e);
            return ApiResponse.error("配置刷新失败: " + e.getMessage());
        }
    }

    /**
     * 检查特定安全功能状态
     */
    @GetMapping("/feature/{featureName}/status")
    public ApiResponse<Map<String, Object>> getFeatureStatus(@PathVariable String featureName) {
        try {
            boolean enabled = configurationManager.isSecurityFeatureEnabled(featureName);
            
            Map<String, Object> status = new HashMap<>();
            status.put("featureName", featureName);
            status.put("enabled", enabled);
            status.put("globalEnabled", securityProperties.isEnabled());
            
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取安全功能状态失败: {}", featureName, e);
            return ApiResponse.error("获取功能状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取详细的安全配置信息
     */
    @GetMapping("/details")
    public ApiResponse<SecurityProperties> getDetailedConfiguration() {
        try {
            return ApiResponse.success(securityProperties);
        } catch (Exception e) {
            log.error("获取详细安全配置失败", e);
            return ApiResponse.error("获取详细配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取配置健康检查信息
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> getConfigurationHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // 检查配置有效性
            boolean configValid = true;
            String validationMessage = "配置正常";
            
            try {
                configurationManager.validateSecurityConfiguration();
            } catch (Exception e) {
                configValid = false;
                validationMessage = e.getMessage();
            }
            
            health.put("configurationValid", configValid);
            health.put("validationMessage", validationMessage);
            health.put("globalSecurityEnabled", securityProperties.isEnabled());
            
            // 统计启用的功能数量
            int enabledFeatures = 0;
            if (securityProperties.getInputValidation().isEnabled()) enabledFeatures++;
            if (securityProperties.getXssProtection().isEnabled()) enabledFeatures++;
            if (securityProperties.getDataIntegrity().isEnabled()) enabledFeatures++;
            if (securityProperties.getFileIntegrity().isEnabled()) enabledFeatures++;
            if (securityProperties.getSafeQuery().isEnabled()) enabledFeatures++;
            if (securityProperties.getFileUploadSecurity().isEnabled()) enabledFeatures++;
            if (securityProperties.getDataMasking().isEnabled()) enabledFeatures++;
            if (securityProperties.getSessionCleanup().isEnabled()) enabledFeatures++;
            if (securityProperties.getDataDeletion().isEnabled()) enabledFeatures++;
            if (securityProperties.getMonitoring().isEnabled()) enabledFeatures++;
            
            health.put("enabledFeaturesCount", enabledFeatures);
            health.put("totalFeaturesCount", 10);
            
            return ApiResponse.success(health);
        } catch (Exception e) {
            log.error("获取配置健康检查信息失败", e);
            return ApiResponse.error("获取健康检查信息失败: " + e.getMessage());
        }
    }
}