package com.myweb.website_core.application.service.config;

import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 安全配置验证服务
 * 提供详细的配置验证、默认值处理和配置建议
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityConfigurationValidationService {

    private final Validator validator;
    
    // 常用的正则表达式模式
    private static final Pattern CRON_PATTERN = Pattern.compile(
        "^\\s*($|#|\\w+\\s*=|(\\?|\\*|(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?(?:,(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?)*)\\s+(\\?|\\*|(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?(?:,(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?)*)\\s+(\\?|\\*|(?:[01]?\\d|2[0-3])(?:(?:-|\\/|\\,)(?:[01]?\\d|2[0-3]))?(?:,(?:[01]?\\d|2[0-3])(?:(?:-|\\/|\\,)(?:[01]?\\d|2[0-3]))?)*)\\s+(\\?|\\*|(?:0?[1-9]|[12]\\d|3[01])(?:(?:-|\\/|\\,)(?:0?[1-9]|[12]\\d|3[01]))?(?:,(?:0?[1-9]|[12]\\d|3[01])(?:(?:-|\\/|\\,)(?:0?[1-9]|[12]\\d|3[01]))?)*)\\s+(\\?|\\*|(?:[1-9]|1[012])(?:(?:-|\\/|\\,)(?:[1-9]|1[012]))?(?:L|W)?(?:,(?:[1-9]|1[012])(?:(?:-|\\/|\\,)(?:[1-9]|1[012]))?(?:L|W)?)*|\\?|\\*|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?(?:,(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?)*)\\s+(\\?|\\*|(?:[0-6])(?:(?:-|\\/|\\,|#)(?:[0-6]))?(?:L)?(?:,(?:[0-6])(?:(?:-|\\/|\\,|#)(?:[0-6]))?(?:L)?)*|\\?|\\*|(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?(?:,(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?)*)(|\\s)+(\\?|\\*|(?:|\\d{4})(?:(?:-|\\/|\\,)(?:|\\d{4}))?(?:,(?:|\\d{4})(?:(?:-|\\/|\\,)(?:|\\d{4}))?)*))$"
    );

    /**
     * 验证配置结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final List<String> suggestions;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings, List<String> suggestions) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getSuggestions() { return suggestions; }
    }

    /**
     * 全面验证安全配置
     */
    public ValidationResult validateConfiguration(SecurityProperties properties) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // 基础验证注解验证
        Set<ConstraintViolation<SecurityProperties>> violations = validator.validate(properties);
        for (ConstraintViolation<SecurityProperties> violation : violations) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }

        // 业务逻辑验证
        validatePasswordPolicy(properties.getPasswordPolicy(), errors, warnings, suggestions);
        validateAccountLock(properties.getAccountLock(), errors, warnings, suggestions);
        validateInputValidation(properties.getInputValidation(), errors, warnings, suggestions);
        validateXssProtection(properties.getXssProtection(), errors, warnings, suggestions);
        validateDataIntegrity(properties.getDataIntegrity(), errors, warnings, suggestions);
        validateFileIntegrity(properties.getFileIntegrity(), errors, warnings, suggestions);
        validateSafeQuery(properties.getSafeQuery(), errors, warnings, suggestions);
        validateFileUploadSecurity(properties.getFileUploadSecurity(), errors, warnings, suggestions);
        validateDataMasking(properties.getDataMasking(), errors, warnings, suggestions);
        validateSessionCleanup(properties.getSessionCleanup(), errors, warnings, suggestions);
        validateDataDeletion(properties.getDataDeletion(), errors, warnings, suggestions);
        validateMonitoring(properties.getMonitoring(), errors, warnings, suggestions);

        // 跨模块验证
        validateCrossModuleConsistency(properties, errors, warnings, suggestions);

        return new ValidationResult(errors.isEmpty(), errors, warnings, suggestions);
    }

    /**
     * 验证密码策略
     */
    private void validatePasswordPolicy(SecurityProperties.PasswordPolicy policy, 
                                      List<String> errors, List<String> warnings, List<String> suggestions) {
        if (policy.getMinLength() >= policy.getMaxLength()) {
            errors.add("密码最小长度不能大于等于最大长度");
        }

        if (policy.getMinLength() < 8) {
            warnings.add("密码最小长度建议不少于8位");
        }

        if (policy.getBcryptStrength() < 10) {
            warnings.add("BCrypt强度建议不少于10");
        } else if (policy.getBcryptStrength() > 12) {
            warnings.add("BCrypt强度过高可能影响性能");
        }

        if (!policy.isRequireUppercase() && !policy.isRequireLowercase() && 
            !policy.isRequireDigit() && !policy.isRequireSpecialChar()) {
            warnings.add("建议至少启用一种密码复杂度要求");
        }

        if (policy.getHistoryCount() > 10) {
            suggestions.add("密码历史记录数量过多可能影响存储性能");
        }
    }

    /**
     * 验证账户锁定策略
     */
    private void validateAccountLock(SecurityProperties.AccountLock accountLock, 
                                   List<String> errors, List<String> warnings, List<String> suggestions) {
        if (accountLock.getCaptchaThreshold() >= accountLock.getMaxFailedAttempts()) {
            errors.add("验证码阈值不能大于等于最大失败次数");
        }

        if (accountLock.getMaxFailedAttempts() < 3) {
            warnings.add("最大失败次数过少可能导致正常用户被误锁");
        }

        if (accountLock.getLockDurationMinutes() > 60) {
            suggestions.add("锁定时间过长可能影响用户体验");
        }
    }

    /**
     * 验证输入验证配置
     */
    private void validateInputValidation(SecurityProperties.InputValidation inputValidation, 
                                       List<String> errors, List<String> warnings, List<String> suggestions) {
        if (inputValidation.getMaxContentLength() < inputValidation.getMaxTitleLength()) {
            warnings.add("内容最大长度小于标题最大长度，可能导致配置混乱");
        }

        if (inputValidation.getAllowedFileExtensions().isEmpty()) {
            warnings.add("未配置允许的文件扩展名，可能存在安全风险");
        }

        if (inputValidation.getBlockedPatterns().isEmpty()) {
            suggestions.add("建议配置阻止的危险模式以增强安全性");
        }
    }

    /**
     * 验证XSS防护配置
     */
    private void validateXssProtection(SecurityProperties.XssProtection xssProtection, 
                                     List<String> errors, List<String> warnings, List<String> suggestions) {
        if (xssProtection.getAllowedTags().isEmpty() && !xssProtection.isRemoveUnknownTags()) {
            warnings.add("未配置允许的HTML标签且不移除未知标签，可能存在XSS风险");
        }

        if (xssProtection.getMaxTagDepth() > 15) {
            warnings.add("HTML标签最大深度过大可能影响性能");
        }

        if (!xssProtection.isEncodeSpecialChars()) {
            suggestions.add("建议启用特殊字符编码以增强XSS防护");
        }
    }

    /**
     * 验证数据完整性配置
     */
    private void validateDataIntegrity(SecurityProperties.DataIntegrity dataIntegrity, 
                                     List<String> errors, List<String> warnings, List<String> suggestions) {
        if (!isValidCronExpression(dataIntegrity.getScheduleCheckCron())) {
            errors.add("数据完整性检查的Cron表达式格式无效");
        }

        if (!Arrays.asList("MD5", "SHA-1", "SHA-256", "SHA-512").contains(dataIntegrity.getHashAlgorithm())) {
            warnings.add("使用了非标准的哈希算法: " + dataIntegrity.getHashAlgorithm());
        }

        if ("MD5".equals(dataIntegrity.getHashAlgorithm()) || "SHA-1".equals(dataIntegrity.getHashAlgorithm())) {
            warnings.add("建议使用更安全的哈希算法如SHA-256");
        }

        if (dataIntegrity.getIntegrityCheckBatchSize() > 500) {
            suggestions.add("完整性检查批次大小过大可能影响数据库性能");
        }
    }

    /**
     * 验证文件完整性配置
     */
    private void validateFileIntegrity(SecurityProperties.FileIntegrity fileIntegrity, 
                                     List<String> errors, List<String> warnings, List<String> suggestions) {
        if (fileIntegrity.getCriticalFiles().isEmpty()) {
            warnings.add("未配置关键文件列表，文件完整性检查可能无效");
        }

        if (fileIntegrity.getCheckIntervalMinutes() < 5) {
            warnings.add("文件完整性检查间隔过短可能影响系统性能");
        }

        if (fileIntegrity.isBackupEnabled() && 
            (fileIntegrity.getBackupDirectory() == null || fileIntegrity.getBackupDirectory().trim().isEmpty())) {
            errors.add("启用了文件备份但未配置备份目录");
        }
    }

    /**
     * 验证安全查询配置
     */
    private void validateSafeQuery(SecurityProperties.SafeQuery safeQuery, 
                                 List<String> errors, List<String> warnings, List<String> suggestions) {
        if (safeQuery.getAllowedSortFields().isEmpty()) {
            warnings.add("未配置允许的排序字段，可能影响查询功能");
        }

        if (safeQuery.getMaxResultsPerPage() > 1000) {
            warnings.add("单页最大结果数过大可能影响性能");
        }

        if (safeQuery.getDefaultPageSize() > safeQuery.getMaxResultsPerPage()) {
            errors.add("默认页面大小不能超过最大结果数");
        }
    }

    /**
     * 验证文件上传安全配置
     */
    private void validateFileUploadSecurity(SecurityProperties.FileUploadSecurity fileUploadSecurity, 
                                          List<String> errors, List<String> warnings, List<String> suggestions) {
        if (fileUploadSecurity.getMaxFileSize() > fileUploadSecurity.getMaxTotalSize()) {
            errors.add("单个文件最大大小不能超过总文件大小限制");
        }

        if (fileUploadSecurity.getAllowedMimeTypes().isEmpty()) {
            warnings.add("未配置允许的MIME类型，可能存在安全风险");
        }

        if (fileUploadSecurity.getScanTimeoutSeconds() > 60) {
            warnings.add("病毒扫描超时时间过长可能影响用户体验");
        }

        if (!fileUploadSecurity.isMagicNumberCheck()) {
            suggestions.add("建议启用文件魔数检查以增强安全性");
        }
    }

    /**
     * 验证数据脱敏配置
     */
    private void validateDataMasking(SecurityProperties.DataMasking dataMasking, 
                                   List<String> errors, List<String> warnings, List<String> suggestions) {
        if (dataMasking.getSensitiveFields().isEmpty()) {
            warnings.add("未配置敏感字段列表，数据脱敏可能无效");
        }

        SecurityProperties.DataMasking.MaskLength maskLength = dataMasking.getMaskLength();
        if (maskLength.getEmailPrefixLength() + maskLength.getEmailSuffixLength() > 10) {
            warnings.add("邮箱脱敏保留长度过长可能降低脱敏效果");
        }

        if (dataMasking.getRolePermission().getFullEmailAccessRoles().isEmpty()) {
            suggestions.add("建议配置具有完整邮箱访问权限的角色");
        }
    }

    /**
     * 验证会话清理配置
     */
    private void validateSessionCleanup(SecurityProperties.SessionCleanup sessionCleanup, 
                                      List<String> errors, List<String> warnings, List<String> suggestions) {
        if (!isValidCronExpression(sessionCleanup.getExpiredSessionCleanupCron())) {
            errors.add("过期会话清理的Cron表达式格式无效");
        }

        if (!isValidCronExpression(sessionCleanup.getOrphanedCleanupCron())) {
            errors.add("孤立数据清理的Cron表达式格式无效");
        }

        if (sessionCleanup.getCleanupIntervalMinutes() < 1) {
            warnings.add("会话清理间隔过短可能影响系统性能");
        }

        if (sessionCleanup.getCleanupBatchSize() > 500) {
            suggestions.add("清理批次大小过大可能影响数据库性能");
        }
    }

    /**
     * 验证数据删除配置
     */
    private void validateDataDeletion(SecurityProperties.DataDeletion dataDeletion, 
                                    List<String> errors, List<String> warnings, List<String> suggestions) {
        if (dataDeletion.getPermanentDeleteDelayDays() < 7) {
            warnings.add("永久删除延迟时间过短，建议至少7天");
        }

        if (dataDeletion.getBatchDeletionSize() > 200) {
            warnings.add("批量删除大小过大可能影响数据库性能");
        }

        if (!dataDeletion.isAuditDeletionOperations()) {
            suggestions.add("建议启用删除操作审计以便追踪");
        }
    }

    /**
     * 验证监控配置
     */
    private void validateMonitoring(SecurityProperties.Monitoring monitoring, 
                                  List<String> errors, List<String> warnings, List<String> suggestions) {
        if (monitoring.getEventRetentionDays() < 30) {
            warnings.add("安全事件保留天数过短，建议至少30天");
        }

        SecurityProperties.Monitoring.ThresholdAlerts alerts = monitoring.getThresholdAlerts();
        if (alerts.getFailedLoginThreshold() < 5) {
            warnings.add("登录失败告警阈值过低可能产生过多告警");
        }

        if (alerts.getXssAttackThreshold() < 3) {
            suggestions.add("XSS攻击告警阈值建议不少于3次");
        }
    }

    /**
     * 验证跨模块一致性
     */
    private void validateCrossModuleConsistency(SecurityProperties properties, 
                                              List<String> errors, List<String> warnings, List<String> suggestions) {
        // 会话相关配置一致性
        if (properties.getSessionCleanup().getMaxInactiveMinutes() > properties.getSession().getTimeoutMinutes()) {
            warnings.add("会话清理的最大非活跃时间大于会话超时时间，可能导致配置冲突");
        }

        // 文件上传和输入验证一致性
        if (properties.getInputValidation().getMaxFilenameLength() < 50 && 
            properties.getFileUploadSecurity().isEnabled()) {
            warnings.add("文件名最大长度过短可能影响文件上传功能");
        }

        // 数据完整性和脱敏一致性
        if (properties.getDataIntegrity().isEnabled() && properties.getDataMasking().isEnabled()) {
            suggestions.add("同时启用数据完整性和脱敏时，建议在脱敏前计算哈希值");
        }
    }

    /**
     * 验证Cron表达式格式
     */
    private boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }
        return CRON_PATTERN.matcher(cronExpression.trim()).matches();
    }

    /**
     * 应用默认值
     */
    public SecurityProperties applyDefaults(SecurityProperties properties) {
        if (properties == null) {
            properties = new SecurityProperties();
        }

        // 应用各模块默认值
        applyInputValidationDefaults(properties.getInputValidation());
        applyXssProtectionDefaults(properties.getXssProtection());
        applyDataIntegrityDefaults(properties.getDataIntegrity());
        applyFileIntegrityDefaults(properties.getFileIntegrity());
        applySafeQueryDefaults(properties.getSafeQuery());
        applyFileUploadSecurityDefaults(properties.getFileUploadSecurity());
        applyDataMaskingDefaults(properties.getDataMasking());
        applySessionCleanupDefaults(properties.getSessionCleanup());
        applyDataDeletionDefaults(properties.getDataDeletion());
        applyMonitoringDefaults(properties.getMonitoring());

        return properties;
    }

    // 各模块默认值应用方法
    private void applyInputValidationDefaults(SecurityProperties.InputValidation inputValidation) {
        if (inputValidation.getAllowedFileExtensions().isEmpty()) {
            inputValidation.setAllowedFileExtensions(
                List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "txt")
            );
        }
        if (inputValidation.getBlockedPatterns().isEmpty()) {
            inputValidation.setBlockedPatterns(
                List.of("<script", "javascript:", "vbscript:", "onload=", "onerror=")
            );
        }
    }

    private void applyXssProtectionDefaults(SecurityProperties.XssProtection xssProtection) {
        if (xssProtection.getAllowedTags().isEmpty()) {
            xssProtection.setAllowedTags(
                List.of("b", "i", "u", "strong", "em", "p", "br", "a", "img")
            );
        }
        if (xssProtection.getAllowedAttributes().isEmpty()) {
            xssProtection.setAllowedAttributes(
                List.of("href", "src", "alt", "title", "class")
            );
        }
    }

    private void applyDataIntegrityDefaults(SecurityProperties.DataIntegrity dataIntegrity) {
        if (dataIntegrity.getHashAlgorithm() == null || dataIntegrity.getHashAlgorithm().trim().isEmpty()) {
            dataIntegrity.setHashAlgorithm("SHA-256");
        }
        if (dataIntegrity.getScheduleCheckCron() == null || dataIntegrity.getScheduleCheckCron().trim().isEmpty()) {
            dataIntegrity.setScheduleCheckCron("0 0 4 * * ?");
        }
    }

    private void applyFileIntegrityDefaults(SecurityProperties.FileIntegrity fileIntegrity) {
        if (fileIntegrity.getCriticalFiles().isEmpty()) {
            fileIntegrity.setCriticalFiles(
                List.of("application.yml", "application-security.yml", "keystore.p12")
            );
        }
        if (fileIntegrity.getBackupDirectory() == null || fileIntegrity.getBackupDirectory().trim().isEmpty()) {
            fileIntegrity.setBackupDirectory("backup/config");
        }
    }

    private void applySafeQueryDefaults(SecurityProperties.SafeQuery safeQuery) {
        if (safeQuery.getAllowedSortFields().isEmpty()) {
            Map<String, List<String>> defaultSortFields = new HashMap<>();
            defaultSortFields.put("posts", List.of("id", "title", "created_at", "updated_at", "author_id"));
            defaultSortFields.put("users", List.of("id", "username", "created_at", "email"));
            defaultSortFields.put("comments", List.of("id", "created_at", "post_id", "author_id"));
            safeQuery.setAllowedSortFields(defaultSortFields);
        }
    }

    private void applyFileUploadSecurityDefaults(SecurityProperties.FileUploadSecurity fileUploadSecurity) {
        if (fileUploadSecurity.getAllowedMimeTypes().isEmpty()) {
            fileUploadSecurity.setAllowedMimeTypes(
                List.of("image/jpeg", "image/png", "image/gif", "application/pdf", "text/plain")
            );
        }
        if (fileUploadSecurity.getQuarantineDirectory() == null || 
            fileUploadSecurity.getQuarantineDirectory().trim().isEmpty()) {
            fileUploadSecurity.setQuarantineDirectory("quarantine");
        }
    }

    private void applyDataMaskingDefaults(SecurityProperties.DataMasking dataMasking) {
        if (dataMasking.getSensitiveFields().isEmpty()) {
            dataMasking.setSensitiveFields(
                List.of("email", "phone", "mobile", "idCard", "password", "token", "secret")
            );
        }
    }

    private void applySessionCleanupDefaults(SecurityProperties.SessionCleanup sessionCleanup) {
        if (sessionCleanup.getExpiredSessionCleanupCron() == null || 
            sessionCleanup.getExpiredSessionCleanupCron().trim().isEmpty()) {
            sessionCleanup.setExpiredSessionCleanupCron("0 */5 * * * ?");
        }
        if (sessionCleanup.getOrphanedCleanupCron() == null || 
            sessionCleanup.getOrphanedCleanupCron().trim().isEmpty()) {
            sessionCleanup.setOrphanedCleanupCron("0 0 2 * * ?");
        }
    }

    private void applyDataDeletionDefaults(SecurityProperties.DataDeletion dataDeletion) {
        // 数据删除配置已有合理默认值
    }

    private void applyMonitoringDefaults(SecurityProperties.Monitoring monitoring) {
        // 监控配置已有合理默认值
    }
}