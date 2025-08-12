package com.myweb.website_core.infrastructure.config.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

/**
 * 安全配置属性类
 * 绑定application-security.yml中的安全相关配置
 * 支持配置验证和默认值处理
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@Getter
@Component
@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * 全局安全功能开关
     */
    private boolean enabled = true;

    /**
     * 输入验证配置
     */
    @Valid
    private InputValidation inputValidation = new InputValidation();
    
    /**
     * XSS防护配置
     */
    @Valid
    private XssProtection xssProtection = new XssProtection();
    
    /**
     * 数据完整性配置
     */
    @Valid
    private DataIntegrity dataIntegrity = new DataIntegrity();
    
    /**
     * 文件完整性配置
     */
    @Valid
    private FileIntegrity fileIntegrity = new FileIntegrity();
    
    /**
     * 安全查询配置
     */
    @Valid
    private SafeQuery safeQuery = new SafeQuery();
    
    /**
     * 文件上传安全配置
     */
    @Valid
    private FileUploadSecurity fileUploadSecurity = new FileUploadSecurity();
    
    /**
     * 数据脱敏配置
     */
    @Valid
    private DataMasking dataMasking = new DataMasking();
    
    /**
     * 会话清理配置
     */
    @Valid
    private SessionCleanup sessionCleanup = new SessionCleanup();
    
    /**
     * 数据删除配置
     */
    @Valid
    private DataDeletion dataDeletion = new DataDeletion();
    
    /**
     * 安全监控配置
     */
    @Valid
    private Monitoring monitoring = new Monitoring();
    
    /**
     * 密码策略配置
     */
    @Valid
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    
    /**
     * 账户锁定配置
     */
    @Valid
    private AccountLock accountLock = new AccountLock();
    
    /**
     * 验证码配置
     */
    @Valid
    private Captcha captcha = new Captcha();
    
    /**
     * 会话管理配置
     */
    @Valid
    private Session session = new Session();
    
    /**
     * TOTP动态口令配置
     */
    @Valid
    private Totp totp = new Totp();
    
    /**
     * CSRF配置
     */
    @Valid
    private Csrf csrf = new Csrf();

    // Setters for all properties
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setInputValidation(InputValidation inputValidation) {
        this.inputValidation = inputValidation;
    }

    public void setXssProtection(XssProtection xssProtection) {
        this.xssProtection = xssProtection;
    }

    public void setDataIntegrity(DataIntegrity dataIntegrity) {
        this.dataIntegrity = dataIntegrity;
    }

    public void setFileIntegrity(FileIntegrity fileIntegrity) {
        this.fileIntegrity = fileIntegrity;
    }

    public void setSafeQuery(SafeQuery safeQuery) {
        this.safeQuery = safeQuery;
    }

    public void setFileUploadSecurity(FileUploadSecurity fileUploadSecurity) {
        this.fileUploadSecurity = fileUploadSecurity;
    }

    public void setDataMasking(DataMasking dataMasking) {
        this.dataMasking = dataMasking;
    }

    public void setSessionCleanup(SessionCleanup sessionCleanup) {
        this.sessionCleanup = sessionCleanup;
    }

    public void setDataDeletion(DataDeletion dataDeletion) {
        this.dataDeletion = dataDeletion;
    }

    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public void setAccountLock(AccountLock accountLock) {
        this.accountLock = accountLock;
    }

    public void setCaptcha(Captcha captcha) {
        this.captcha = captcha;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setTotp(Totp totp) {
        this.totp = totp;
    }

    public void setCsrf(Csrf csrf) {
        this.csrf = csrf;
    }
    
    /**
     * 输入验证配置
     */
    @Getter
    public static class InputValidation {
        private boolean enabled = true;
        
        @Min(100)
        @Max(100000)
        private int maxStringLength = 10000;
        
        @Min(10)
        @Max(500)
        private int maxTitleLength = 200;
        
        @Min(100)
        @Max(100000)
        private int maxContentLength = 50000;
        
        @Min(10)
        @Max(5000)
        private int maxCommentLength = 1000;
        
        @Min(3)
        @Max(100)
        private int maxUsernameLength = 50;
        
        @Min(5)
        @Max(200)
        private int maxEmailLength = 100;
        
        @Min(10)
        @Max(500)
        private int maxFilenameLength = 255;
        
        private List<String> allowedFileExtensions = List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "txt");
        private List<String> blockedPatterns = List.of("<script", "javascript:", "vbscript:", "onload=", "onerror=");
        private List<String> sqlInjectionPatterns = List.of("union", "select", "insert", "update", "delete", "drop", "exec", "script");

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setMaxStringLength(int maxStringLength) { this.maxStringLength = maxStringLength; }
        public void setMaxTitleLength(int maxTitleLength) { this.maxTitleLength = maxTitleLength; }
        public void setMaxContentLength(int maxContentLength) { this.maxContentLength = maxContentLength; }
        public void setMaxCommentLength(int maxCommentLength) { this.maxCommentLength = maxCommentLength; }
        public void setMaxUsernameLength(int maxUsernameLength) { this.maxUsernameLength = maxUsernameLength; }
        public void setMaxEmailLength(int maxEmailLength) { this.maxEmailLength = maxEmailLength; }
        public void setMaxFilenameLength(int maxFilenameLength) { this.maxFilenameLength = maxFilenameLength; }
        public void setAllowedFileExtensions(List<String> allowedFileExtensions) { this.allowedFileExtensions = allowedFileExtensions; }
        public void setBlockedPatterns(List<String> blockedPatterns) { this.blockedPatterns = blockedPatterns; }
        public void setSqlInjectionPatterns(List<String> sqlInjectionPatterns) { this.sqlInjectionPatterns = sqlInjectionPatterns; }
    }
    
    /**
     * XSS防护配置
     */
    @Getter
    public static class XssProtection {
        private boolean enabled = true;
        private boolean strictMode = false;
        private List<String> allowedTags = List.of("b", "i", "u", "strong", "em", "p", "br", "a", "img");
        private List<String> allowedAttributes = List.of("href", "src", "alt", "title", "class");
        private boolean removeUnknownTags = true;
        private boolean encodeSpecialChars = true;
        
        @Min(1)
        @Max(20)
        private int maxTagDepth = 10;

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }
        public void setAllowedTags(List<String> allowedTags) { this.allowedTags = allowedTags; }
        public void setAllowedAttributes(List<String> allowedAttributes) { this.allowedAttributes = allowedAttributes; }
        public void setRemoveUnknownTags(boolean removeUnknownTags) { this.removeUnknownTags = removeUnknownTags; }
        public void setEncodeSpecialChars(boolean encodeSpecialChars) { this.encodeSpecialChars = encodeSpecialChars; }
        public void setMaxTagDepth(int maxTagDepth) { this.maxTagDepth = maxTagDepth; }
    }
    
    /**
     * 数据完整性配置
     */
    @Getter
    public static class DataIntegrity {
        private boolean enabled = true;
        
        @NotBlank
        private String hashAlgorithm = "SHA-256";
        
        private boolean autoVerify = true;
        private String scheduleCheckCron = "0 0 4 * * ?";
        private boolean verifyOnRead = false;
        private boolean storeHashSeparately = true;
        
        @Min(10)
        @Max(1000)
        private int integrityCheckBatchSize = 100;

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setHashAlgorithm(String hashAlgorithm) { this.hashAlgorithm = hashAlgorithm; }
        public void setAutoVerify(boolean autoVerify) { this.autoVerify = autoVerify; }
        public void setScheduleCheckCron(String scheduleCheckCron) { this.scheduleCheckCron = scheduleCheckCron; }
        public void setVerifyOnRead(boolean verifyOnRead) { this.verifyOnRead = verifyOnRead; }
        public void setStoreHashSeparately(boolean storeHashSeparately) { this.storeHashSeparately = storeHashSeparately; }
        public void setIntegrityCheckBatchSize(int integrityCheckBatchSize) { this.integrityCheckBatchSize = integrityCheckBatchSize; }
    }
    
    /**
     * 文件完整性配置
     */
    @Getter
    public static class FileIntegrity {
        private boolean enabled = true;
        private List<String> criticalFiles = List.of("application.yml", "application-security.yml", "keystore.p12");
        private boolean backupEnabled = true;
        private boolean alertEnabled = true;
        
        @Min(1)
        @Max(1440)
        private int checkIntervalMinutes = 60;
        
        private String backupDirectory = "backup/config";
        private String hashAlgorithm = "SHA-256";

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setCriticalFiles(List<String> criticalFiles) { this.criticalFiles = criticalFiles; }
        public void setBackupEnabled(boolean backupEnabled) { this.backupEnabled = backupEnabled; }
        public void setAlertEnabled(boolean alertEnabled) { this.alertEnabled = alertEnabled; }
        public void setCheckIntervalMinutes(int checkIntervalMinutes) { this.checkIntervalMinutes = checkIntervalMinutes; }
        public void setBackupDirectory(String backupDirectory) { this.backupDirectory = backupDirectory; }
        public void setHashAlgorithm(String hashAlgorithm) { this.hashAlgorithm = hashAlgorithm; }
    }
    
    /**
     * 安全查询配置
     */
    @Getter
    public static class SafeQuery {
        private boolean enabled = true;
        private boolean sqlInjectionCheck = true;
        private boolean parameterValidation = true;
        
        @Min(100)
        @Max(50000)
        private int maxQueryLength = 5000;
        
        private Map<String, List<String>> allowedSortFields = Map.of(
            "posts", List.of("id", "title", "created_at", "updated_at", "author_id"),
            "users", List.of("id", "username", "created_at", "email"),
            "comments", List.of("id", "created_at", "post_id", "author_id")
        );
        
        @Min(10)
        @Max(1000)
        private int maxResultsPerPage = 100;
        
        @Min(5)
        @Max(100)
        private int defaultPageSize = 20;

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setSqlInjectionCheck(boolean sqlInjectionCheck) { this.sqlInjectionCheck = sqlInjectionCheck; }
        public void setParameterValidation(boolean parameterValidation) { this.parameterValidation = parameterValidation; }
        public void setMaxQueryLength(int maxQueryLength) { this.maxQueryLength = maxQueryLength; }
        public void setAllowedSortFields(Map<String, List<String>> allowedSortFields) { this.allowedSortFields = allowedSortFields; }
        public void setMaxResultsPerPage(int maxResultsPerPage) { this.maxResultsPerPage = maxResultsPerPage; }
        public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
    }
    
    /**
     * 文件上传安全配置
     */
    @Getter
    public static class FileUploadSecurity {
        private boolean enabled = true;
        
        @Min(1024)
        @Max(104857600) // 100MB
        private long maxFileSize = 10485760L; // 10MB
        
        @Min(1024)
        @Max(1073741824) // 1GB
        private long maxTotalSize = 52428800L; // 50MB
        
        private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/gif", "application/pdf", "text/plain");
        private boolean virusScanEnabled = true;
        private boolean magicNumberCheck = true;
        private boolean contentScanEnabled = true;
        private String quarantineDirectory = "quarantine";
        
        @Min(5)
        @Max(300)
        private int scanTimeoutSeconds = 30;

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
        public void setMaxTotalSize(long maxTotalSize) { this.maxTotalSize = maxTotalSize; }
        public void setAllowedMimeTypes(List<String> allowedMimeTypes) { this.allowedMimeTypes = allowedMimeTypes; }
        public void setVirusScanEnabled(boolean virusScanEnabled) { this.virusScanEnabled = virusScanEnabled; }
        public void setMagicNumberCheck(boolean magicNumberCheck) { this.magicNumberCheck = magicNumberCheck; }
        public void setContentScanEnabled(boolean contentScanEnabled) { this.contentScanEnabled = contentScanEnabled; }
        public void setQuarantineDirectory(String quarantineDirectory) { this.quarantineDirectory = quarantineDirectory; }
        public void setScanTimeoutSeconds(int scanTimeoutSeconds) { this.scanTimeoutSeconds = scanTimeoutSeconds; }
    }
    
    /**
     * 数据脱敏配置
     */
    @Getter
    public static class DataMasking {
        private boolean enabled = true;
        private String emailMaskLevel = "partial";
        private String phoneMaskLevel = "partial";
        private String usernameMaskLevel = "partial";
        private String idCardMaskLevel = "partial";
        private String maskChar = "*";
        private boolean adminFullAccess = true;
        private boolean selfFullAccess = true;
        private List<String> sensitiveFields = List.of("email", "phone", "mobile", "idCard", "password", "token", "secret");
        private MaskLength maskLength = new MaskLength();
        private RolePermission rolePermission = new RolePermission();

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setEmailMaskLevel(String emailMaskLevel) { this.emailMaskLevel = emailMaskLevel; }
        public void setPhoneMaskLevel(String phoneMaskLevel) { this.phoneMaskLevel = phoneMaskLevel; }
        public void setUsernameMaskLevel(String usernameMaskLevel) { this.usernameMaskLevel = usernameMaskLevel; }
        public void setIdCardMaskLevel(String idCardMaskLevel) { this.idCardMaskLevel = idCardMaskLevel; }
        public void setMaskChar(String maskChar) { this.maskChar = maskChar; }
        public void setAdminFullAccess(boolean adminFullAccess) { this.adminFullAccess = adminFullAccess; }
        public void setSelfFullAccess(boolean selfFullAccess) { this.selfFullAccess = selfFullAccess; }
        public void setSensitiveFields(List<String> sensitiveFields) { this.sensitiveFields = sensitiveFields; }
        public void setMaskLength(MaskLength maskLength) { this.maskLength = maskLength; }
        public void setRolePermission(RolePermission rolePermission) { this.rolePermission = rolePermission; }

        @Getter
        public static class MaskLength {
            private int emailPrefixLength = 2;
            private int emailSuffixLength = 2;
            private int phonePrefixLength = 3;
            private int phoneSuffixLength = 4;
            private int usernamePrefixLength = 2;
            private int usernameSuffixLength = 2;
            private int idCardPrefixLength = 4;
            private int idCardSuffixLength = 4;

            // Setters
            public void setEmailPrefixLength(int emailPrefixLength) { this.emailPrefixLength = emailPrefixLength; }
            public void setEmailSuffixLength(int emailSuffixLength) { this.emailSuffixLength = emailSuffixLength; }
            public void setPhonePrefixLength(int phonePrefixLength) { this.phonePrefixLength = phonePrefixLength; }
            public void setPhoneSuffixLength(int phoneSuffixLength) { this.phoneSuffixLength = phoneSuffixLength; }
            public void setUsernamePrefixLength(int usernamePrefixLength) { this.usernamePrefixLength = usernamePrefixLength; }
            public void setUsernameSuffixLength(int usernameSuffixLength) { this.usernameSuffixLength = usernameSuffixLength; }
            public void setIdCardPrefixLength(int idCardPrefixLength) { this.idCardPrefixLength = idCardPrefixLength; }
            public void setIdCardSuffixLength(int idCardSuffixLength) { this.idCardSuffixLength = idCardSuffixLength; }
        }

        @Getter
        public static class RolePermission {
            private List<String> fullEmailAccessRoles = List.of("ADMIN", "SUPER_ADMIN");
            private List<String> fullPhoneAccessRoles = List.of("ADMIN", "SUPER_ADMIN");
            private List<String> fullUserAccessRoles = List.of("SUPER_ADMIN");
            private List<String> dataExportRoles = List.of("ADMIN", "SUPER_ADMIN");

            // Setters
            public void setFullEmailAccessRoles(List<String> fullEmailAccessRoles) { this.fullEmailAccessRoles = fullEmailAccessRoles; }
            public void setFullPhoneAccessRoles(List<String> fullPhoneAccessRoles) { this.fullPhoneAccessRoles = fullPhoneAccessRoles; }
            public void setFullUserAccessRoles(List<String> fullUserAccessRoles) { this.fullUserAccessRoles = fullUserAccessRoles; }
            public void setDataExportRoles(List<String> dataExportRoles) { this.dataExportRoles = dataExportRoles; }
        }
    }
    
    /**
     * 会话清理配置
     */
    @Getter
    public static class SessionCleanup {
        private boolean enabled = true;
        
        @Min(1)
        @Max(60)
        private int cleanupIntervalMinutes = 5;
        
        private String expiredSessionCleanupCron = "0 */5 * * * ?";
        private String orphanedCleanupCron = "0 0 2 * * ?";
        
        @Min(5)
        @Max(1440)
        private int maxInactiveMinutes = 30;
        
        @Min(10)
        @Max(1000)
        private int cleanupBatchSize = 100;
        
        private boolean forceCleanupOnLogout = true;

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) { this.cleanupIntervalMinutes = cleanupIntervalMinutes; }
        public void setExpiredSessionCleanupCron(String expiredSessionCleanupCron) { this.expiredSessionCleanupCron = expiredSessionCleanupCron; }
        public void setOrphanedCleanupCron(String orphanedCleanupCron) { this.orphanedCleanupCron = orphanedCleanupCron; }
        public void setMaxInactiveMinutes(int maxInactiveMinutes) { this.maxInactiveMinutes = maxInactiveMinutes; }
        public void setCleanupBatchSize(int cleanupBatchSize) { this.cleanupBatchSize = cleanupBatchSize; }
        public void setForceCleanupOnLogout(boolean forceCleanupOnLogout) { this.forceCleanupOnLogout = forceCleanupOnLogout; }
    }
    
    /**
     * 数据删除配置
     */
    @Getter
    public static class DataDeletion {
        private boolean enabled = true;
        private boolean softDeleteEnabled = true;
        
        @Min(1)
        @Max(365)
        private int permanentDeleteDelayDays = 30;
        
        private boolean cascadeDeleteEnabled = true;
        private boolean deletionConfirmationRequired = true;
        private boolean auditDeletionOperations = true;
        
        @Min(10)
        @Max(500)
        private int batchDeletionSize = 50;
        
        @Min(1)
        @Max(60)
        private int deletionTimeoutMinutes = 10;

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setSoftDeleteEnabled(boolean softDeleteEnabled) { this.softDeleteEnabled = softDeleteEnabled; }
        public void setPermanentDeleteDelayDays(int permanentDeleteDelayDays) { this.permanentDeleteDelayDays = permanentDeleteDelayDays; }
        public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) { this.cascadeDeleteEnabled = cascadeDeleteEnabled; }
        public void setDeletionConfirmationRequired(boolean deletionConfirmationRequired) { this.deletionConfirmationRequired = deletionConfirmationRequired; }
        public void setAuditDeletionOperations(boolean auditDeletionOperations) { this.auditDeletionOperations = auditDeletionOperations; }
        public void setBatchDeletionSize(int batchDeletionSize) { this.batchDeletionSize = batchDeletionSize; }
        public void setDeletionTimeoutMinutes(int deletionTimeoutMinutes) { this.deletionTimeoutMinutes = deletionTimeoutMinutes; }
    }
    
    /**
     * 安全监控配置
     */
    @Getter
    public static class Monitoring {
        private boolean enabled = true;
        private boolean metricsEnabled = true;
        private boolean alertEnabled = true;
        private boolean logSecurityEvents = true;
        
        @Min(1)
        @Max(365)
        private int eventRetentionDays = 90;
        
        private boolean performanceMonitoring = true;
        private ThresholdAlerts thresholdAlerts = new ThresholdAlerts();

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }
        public void setAlertEnabled(boolean alertEnabled) { this.alertEnabled = alertEnabled; }
        public void setLogSecurityEvents(boolean logSecurityEvents) { this.logSecurityEvents = logSecurityEvents; }
        public void setEventRetentionDays(int eventRetentionDays) { this.eventRetentionDays = eventRetentionDays; }
        public void setPerformanceMonitoring(boolean performanceMonitoring) { this.performanceMonitoring = performanceMonitoring; }
        public void setThresholdAlerts(ThresholdAlerts thresholdAlerts) { this.thresholdAlerts = thresholdAlerts; }

        @Getter
        public static class ThresholdAlerts {
            private int failedLoginThreshold = 10;
            private int xssAttackThreshold = 5;
            private int sqlInjectionThreshold = 3;
            private int fileUploadFailureThreshold = 20;

            // Setters
            public void setFailedLoginThreshold(int failedLoginThreshold) { this.failedLoginThreshold = failedLoginThreshold; }
            public void setXssAttackThreshold(int xssAttackThreshold) { this.xssAttackThreshold = xssAttackThreshold; }
            public void setSqlInjectionThreshold(int sqlInjectionThreshold) { this.sqlInjectionThreshold = sqlInjectionThreshold; }
            public void setFileUploadFailureThreshold(int fileUploadFailureThreshold) { this.fileUploadFailureThreshold = fileUploadFailureThreshold; }
        }
    }
    
    /**
     * CSRF配置
     */
    @Getter
    public static class Csrf {
        private boolean enabled = true;
        private String cookieName = "XSRF-TOKEN";
        private String headerName = "X-XSRF-TOKEN";
        private String parameterName = "_csrf";
        private String cookiePath = "/blog";
        private int cookieMaxAge = 7200;
        private boolean cookieHttpOnly = false;
        private boolean cookieSecure = true;
        private int tokenCacheExpirationHours = 2;

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setCookieName(String cookieName) { this.cookieName = cookieName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public void setParameterName(String parameterName) { this.parameterName = parameterName; }
        public void setCookiePath(String cookiePath) { this.cookiePath = cookiePath; }
        public void setCookieMaxAge(int cookieMaxAge) { this.cookieMaxAge = cookieMaxAge; }
        public void setCookieHttpOnly(boolean cookieHttpOnly) { this.cookieHttpOnly = cookieHttpOnly; }
        public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
        public void setTokenCacheExpirationHours(int tokenCacheExpirationHours) { this.tokenCacheExpirationHours = tokenCacheExpirationHours; }
    }

    /**
     * 密码策略配置
     */
    @Getter
    public static class PasswordPolicy {
        /**
         * 最小长度
         */
        @Min(6)
        @Max(20)
        private int minLength = 8;
        
        /**
         * 最大长度
         */
        @Min(20)
        @Max(256)
        private int maxLength = 128;
        
        /**
         * 是否需要大写字母
         */
        private boolean requireUppercase = true;
        
        /**
         * 是否需要小写字母
         */
        private boolean requireLowercase = true;
        
        /**
         * 是否需要数字
         */
        private boolean requireDigit = true;
        
        /**
         * 是否需要特殊字符
         */
        private boolean requireSpecialChar = true;
        
        /**
         * BCrypt加密强度
         */
        @Min(4)
        @Max(15)
        private int bcryptStrength = 12;
        
        /**
         * 特殊字符集合
         */
        @NotBlank
        private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        
        /**
         * 密码历史记录数量
         */
        @Min(0)
        @Max(20)
        private int historyCount = 5;

        // Setters
        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public void setRequireUppercase(boolean requireUppercase) {
            this.requireUppercase = requireUppercase;
        }

        public void setRequireLowercase(boolean requireLowercase) {
            this.requireLowercase = requireLowercase;
        }

        public void setRequireDigit(boolean requireDigit) {
            this.requireDigit = requireDigit;
        }

        public void setRequireSpecialChar(boolean requireSpecialChar) {
            this.requireSpecialChar = requireSpecialChar;
        }

        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }

        public void setSpecialChars(String specialChars) {
            this.specialChars = specialChars;
        }

        public void setHistoryCount(int historyCount) {
            this.historyCount = historyCount;
        }
    }
    
    /**
     * 账户锁定配置
     */
    @Getter
    public static class AccountLock {
        /**
         * 最大登录失败次数
         */
        @Min(3)
        @Max(20)
        private int maxFailedAttempts = 5;
        
        /**
         * 锁定时间（分钟）
         */
        @Min(1)
        @Max(1440)
        private int lockDurationMinutes = 15;
        
        /**
         * 需要验证码的失败次数阈值
         */
        @Min(1)
        @Max(10)
        private int captchaThreshold = 3;

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public void setLockDurationMinutes(int lockDurationMinutes) {
            this.lockDurationMinutes = lockDurationMinutes;
        }

        public void setCaptchaThreshold(int captchaThreshold) {
            this.captchaThreshold = captchaThreshold;
        }
    }
    
    /**
     * 验证码配置
     */
    @Getter
    public static class Captcha {
        /**
         * 验证码长度
         */
        @Min(4)
        @Max(10)
        private int length = 6;
        
        /**
         * 验证码有效期（分钟）
         */
        @Min(1)
        @Max(30)
        private int expirationMinutes = 5;
        
        /**
         * 验证码类型（数字、字母、混合）
         */
        @NotBlank
        private String type = "mixed";

        public void setLength(int length) {
            this.length = length;
        }

        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
    
    /**
     * 会话管理配置
     */
    @Getter
    public static class Session {
        /**
         * 会话超时时间（分钟）
         */
        @Min(5)
        @Max(1440)
        private int timeoutMinutes = 30;
        
        /**
         * 是否启用单用户单会话
         */
        private boolean singleSession = true;
        
        /**
         * 会话清理间隔（分钟）
         */
        @Min(1)
        @Max(60)
        private int cleanupIntervalMinutes = 10;
        
        /**
         * 最大并发会话数
         */
        @Min(1)
        @Max(10)
        private int maxConcurrentSessions = 1;

        // Setters
        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }

        public void setSingleSession(boolean singleSession) {
            this.singleSession = singleSession;
        }

        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
            this.cleanupIntervalMinutes = cleanupIntervalMinutes;
        }

        public void setMaxConcurrentSessions(int maxConcurrentSessions) {
            this.maxConcurrentSessions = maxConcurrentSessions;
        }
    }
    
    /**
     * TOTP动态口令配置
     */
    @Getter
    public static class Totp {
        /**
         * 发行者名称
         */
        @NotBlank
        private String issuer = "MyWeb";
        
        /**
         * 时间窗口大小（秒）
         */
        @Min(15)
        @Max(120)
        private int timeStepSeconds = 30;
        
        /**
         * 允许的时间偏差窗口数
         */
        @Min(0)
        @Max(5)
        private int allowedTimeSkew = 1;
        
        /**
         * 密钥长度
         */
        @Min(16)
        @Max(64)
        private int secretLength = 32;

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public void setTimeStepSeconds(int timeStepSeconds) {
            this.timeStepSeconds = timeStepSeconds;
        }

        public void setAllowedTimeSkew(int allowedTimeSkew) {
            this.allowedTimeSkew = allowedTimeSkew;
        }

        public void setSecretLength(int secretLength) {
            this.secretLength = secretLength;
        }
    }
}