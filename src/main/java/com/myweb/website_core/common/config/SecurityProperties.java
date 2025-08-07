package com.myweb.website_core.common.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全配置属性类
 * 绑定application.yml中的安全相关配置
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@Getter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    // Getters and Setters
    /**
     * 密码策略配置
     */
    private PasswordPolicy passwordPolicy = new PasswordPolicy();
    
    /**
     * 账户锁定配置
     */
    private AccountLock accountLock = new AccountLock();
    
    /**
     * 验证码配置
     */
    private Captcha captcha = new Captcha();
    
    /**
     * 会话管理配置
     */
    private Session session = new Session();
    
    /**
     * TOTP动态口令配置
     */
    private Totp totp = new Totp();

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
    
    /**
     * 密码策略配置
     */
    public static class PasswordPolicy {
        /**
         * 最小长度
         */
        private int minLength = 8;
        
        /**
         * 最大长度
         */
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
        private int bcryptStrength = 12;
        
        // Getters and Setters
        public int getMinLength() {
            return minLength;
        }
        
        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }
        
        public int getMaxLength() {
            return maxLength;
        }
        
        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
        
        public boolean isRequireUppercase() {
            return requireUppercase;
        }
        
        public void setRequireUppercase(boolean requireUppercase) {
            this.requireUppercase = requireUppercase;
        }
        
        public boolean isRequireLowercase() {
            return requireLowercase;
        }
        
        public void setRequireLowercase(boolean requireLowercase) {
            this.requireLowercase = requireLowercase;
        }
        
        public boolean isRequireDigit() {
            return requireDigit;
        }
        
        public void setRequireDigit(boolean requireDigit) {
            this.requireDigit = requireDigit;
        }
        
        public boolean isRequireSpecialChar() {
            return requireSpecialChar;
        }
        
        public void setRequireSpecialChar(boolean requireSpecialChar) {
            this.requireSpecialChar = requireSpecialChar;
        }
        
        public int getBcryptStrength() {
            return bcryptStrength;
        }
        
        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }
    }
    
    /**
     * 账户锁定配置
     */
    public static class AccountLock {
        /**
         * 最大登录失败次数
         */
        private int maxFailedAttempts = 5;
        
        /**
         * 锁定时间（分钟）
         */
        private int lockDurationMinutes = 15;
        
        /**
         * 需要验证码的失败次数阈值
         */
        private int captchaThreshold = 3;
        
        // Getters and Setters
        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }
        
        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }
        
        public int getLockDurationMinutes() {
            return lockDurationMinutes;
        }
        
        public void setLockDurationMinutes(int lockDurationMinutes) {
            this.lockDurationMinutes = lockDurationMinutes;
        }
        
        public int getCaptchaThreshold() {
            return captchaThreshold;
        }
        
        public void setCaptchaThreshold(int captchaThreshold) {
            this.captchaThreshold = captchaThreshold;
        }
    }
    
    /**
     * 验证码配置
     */
    public static class Captcha {
        /**
         * 验证码长度
         */
        private int length = 6;
        
        /**
         * 验证码有效期（分钟）
         */
        private int expirationMinutes = 5;
        
        /**
         * 验证码类型（数字、字母、混合）
         */
        private String type = "mixed";
        
        // Getters and Setters
        public int getLength() {
            return length;
        }
        
        public void setLength(int length) {
            this.length = length;
        }
        
        public int getExpirationMinutes() {
            return expirationMinutes;
        }
        
        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
    
    /**
     * 会话管理配置
     */
    public static class Session {
        /**
         * 会话超时时间（分钟）
         */
        private int timeoutMinutes = 30;
        
        /**
         * 是否启用单用户单会话
         */
        private boolean singleSession = true;
        
        /**
         * 会话清理间隔（分钟）
         */
        private int cleanupIntervalMinutes = 10;
        
        // Getters and Setters
        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }
        
        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }
        
        public boolean isSingleSession() {
            return singleSession;
        }
        
        public void setSingleSession(boolean singleSession) {
            this.singleSession = singleSession;
        }
        
        public int getCleanupIntervalMinutes() {
            return cleanupIntervalMinutes;
        }
        
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
            this.cleanupIntervalMinutes = cleanupIntervalMinutes;
        }
    }
    
    /**
     * TOTP动态口令配置
     */
    public static class Totp {
        /**
         * 发行者名称
         */
        private String issuer = "MyWeb";
        
        /**
         * 时间窗口大小（秒）
         */
        private int timeStepSeconds = 30;
        
        /**
         * 允许的时间偏差窗口数
         */
        private int allowedTimeSkew = 1;
        
        /**
         * 密钥长度
         */
        private int secretLength = 32;
        
        // Getters and Setters
        public String getIssuer() {
            return issuer;
        }
        
        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
        
        public int getTimeStepSeconds() {
            return timeStepSeconds;
        }
        
        public void setTimeStepSeconds(int timeStepSeconds) {
            this.timeStepSeconds = timeStepSeconds;
        }
        
        public int getAllowedTimeSkew() {
            return allowedTimeSkew;
        }
        
        public void setAllowedTimeSkew(int allowedTimeSkew) {
            this.allowedTimeSkew = allowedTimeSkew;
        }
        
        public int getSecretLength() {
            return secretLength;
        }
        
        public void setSecretLength(int secretLength) {
            this.secretLength = secretLength;
        }
    }
}