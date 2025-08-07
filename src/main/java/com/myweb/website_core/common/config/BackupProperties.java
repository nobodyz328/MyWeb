package com.myweb.website_core.common.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 备份配置属性类
 * 管理备份相关配置
 * 
 * @author MyWeb
 * @since 1.0.0
 */
@Getter
@Component
@ConfigurationProperties(prefix = "app.backup")
public class BackupProperties {

    // Getters and Setters
    /**
     * 是否启用自动备份
     */
    private boolean enabled = true;
    
    /**
     * 备份路径
     */
    private String path = "/var/backups/myweb";
    
    /**
     * 备份保留天数
     */
    private int retentionDays = 30;
    
    /**
     * 备份调度配置
     */
    private Schedule schedule = new Schedule();
    
    /**
     * 加密配置
     */
    private Encryption encryption = new Encryption();
    
    /**
     * 压缩配置
     */
    private Compression compression = new Compression();
    
    /**
     * 存储配置
     */
    private Storage storage = new Storage();
    
    /**
     * 通知配置
     */
    private Notification notification = new Notification();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public void setCompression(Compression compression) {
        this.compression = compression;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }
    
    /**
     * 备份调度配置
     */
    public static class Schedule {
        /**
         * 全量备份Cron表达式（默认每天凌晨2点）
         */
        private String fullBackupCron = "0 0 2 * * ?";
        
        /**
         * 增量备份Cron表达式（默认每4小时）
         */
        private String incrementalBackupCron = "0 0 */4 * * ?";
        
        /**
         * 是否启用增量备份
         */
        private boolean enableIncremental = false;
        
        /**
         * 备份超时时间（分钟）
         */
        private int timeoutMinutes = 60;
        
        // Getters and Setters
        public String getFullBackupCron() {
            return fullBackupCron;
        }
        
        public void setFullBackupCron(String fullBackupCron) {
            this.fullBackupCron = fullBackupCron;
        }
        
        public String getIncrementalBackupCron() {
            return incrementalBackupCron;
        }
        
        public void setIncrementalBackupCron(String incrementalBackupCron) {
            this.incrementalBackupCron = incrementalBackupCron;
        }
        
        public boolean isEnableIncremental() {
            return enableIncremental;
        }
        
        public void setEnableIncremental(boolean enableIncremental) {
            this.enableIncremental = enableIncremental;
        }
        
        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }
        
        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }
    }
    
    /**
     * 加密配置
     */
    public static class Encryption {
        /**
         * 是否启用加密
         */
        private boolean enabled = true;
        
        /**
         * 加密算法
         */
        private String algorithm = "AES-256-GCM";
        
        /**
         * 加密密钥
         */
        private String key = "myWebBackupEncryptionKey2024!@#$%^&*";
        
        /**
         * 密钥文件路径
         */
        private String keyFilePath;
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getAlgorithm() {
            return algorithm;
        }
        
        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getKeyFilePath() {
            return keyFilePath;
        }
        
        public void setKeyFilePath(String keyFilePath) {
            this.keyFilePath = keyFilePath;
        }
    }
    
    /**
     * 压缩配置
     */
    public static class Compression {
        /**
         * 是否启用压缩
         */
        private boolean enabled = true;
        
        /**
         * 压缩算法
         */
        private String algorithm = "gzip";
        
        /**
         * 压缩级别（1-9）
         */
        private int level = 6;
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getAlgorithm() {
            return algorithm;
        }
        
        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
        
        public int getLevel() {
            return level;
        }
        
        public void setLevel(int level) {
            this.level = level;
        }
    }
    
    /**
     * 存储配置
     */
    public static class Storage {
        /**
         * 本地存储配置
         */
        private Local local = new Local();
        
        /**
         * 远程存储配置
         */
        private Remote remote = new Remote();
        
        // Getters and Setters
        public Local getLocal() {
            return local;
        }
        
        public void setLocal(Local local) {
            this.local = local;
        }
        
        public Remote getRemote() {
            return remote;
        }
        
        public void setRemote(Remote remote) {
            this.remote = remote;
        }
        
        /**
         * 本地存储配置
         */
        public static class Local {
            /**
             * 最大存储空间（GB）
             */
            private int maxSizeGb = 100;
            
            /**
             * 存储空间告警阈值（百分比）
             */
            private double alertThreshold = 0.8;
            
            // Getters and Setters
            public int getMaxSizeGb() {
                return maxSizeGb;
            }
            
            public void setMaxSizeGb(int maxSizeGb) {
                this.maxSizeGb = maxSizeGb;
            }
            
            public double getAlertThreshold() {
                return alertThreshold;
            }
            
            public void setAlertThreshold(double alertThreshold) {
                this.alertThreshold = alertThreshold;
            }
        }
        
        /**
         * 远程存储配置
         */
        public static class Remote {
            /**
             * 是否启用远程存储
             */
            private boolean enabled = false;
            
            /**
             * 存储类型（S3、FTP、SFTP等）
             */
            private String type = "S3";
            
            /**
             * 存储配置
             */
            private String endpoint;
            private String accessKey;
            private String secretKey;
            private String bucket;
            private String region;
            
            // Getters and Setters
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
            
            public String getType() {
                return type;
            }
            
            public void setType(String type) {
                this.type = type;
            }
            
            public String getEndpoint() {
                return endpoint;
            }
            
            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }
            
            public String getAccessKey() {
                return accessKey;
            }
            
            public void setAccessKey(String accessKey) {
                this.accessKey = accessKey;
            }
            
            public String getSecretKey() {
                return secretKey;
            }
            
            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }
            
            public String getBucket() {
                return bucket;
            }
            
            public void setBucket(String bucket) {
                this.bucket = bucket;
            }
            
            public String getRegion() {
                return region;
            }
            
            public void setRegion(String region) {
                this.region = region;
            }
        }
    }
    
    /**
     * 通知配置
     */
    public static class Notification {
        /**
         * 是否启用通知
         */
        private boolean enabled = true;
        
        /**
         * 邮件通知配置
         */
        private Email email = new Email();
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public Email getEmail() {
            return email;
        }
        
        public void setEmail(Email email) {
            this.email = email;
        }
        
        /**
         * 邮件通知配置
         */
        public static class Email {
            /**
             * 是否启用邮件通知
             */
            private boolean enabled = true;
            
            /**
             * 通知接收者
             */
            private String[] recipients = {"admin@myweb.com"};
            
            /**
             * 是否在备份成功时发送通知
             */
            private boolean notifyOnSuccess = false;
            
            /**
             * 是否在备份失败时发送通知
             */
            private boolean notifyOnFailure = true;
            
            // Getters and Setters
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
            
            public String[] getRecipients() {
                return recipients;
            }
            
            public void setRecipients(String[] recipients) {
                this.recipients = recipients;
            }
            
            public boolean isNotifyOnSuccess() {
                return notifyOnSuccess;
            }
            
            public void setNotifyOnSuccess(boolean notifyOnSuccess) {
                this.notifyOnSuccess = notifyOnSuccess;
            }
            
            public boolean isNotifyOnFailure() {
                return notifyOnFailure;
            }
            
            public void setNotifyOnFailure(boolean notifyOnFailure) {
                this.notifyOnFailure = notifyOnFailure;
            }
        }
    }
    
    /**
     * 获取备份文件完整路径
     */
    public String getFullBackupPath(String filename) {
        return path.endsWith("/") ? path + filename : path + "/" + filename;
    }
    
    /**
     * 检查是否需要存储空间告警
     */
    public boolean shouldAlertStorage(long usedSpaceBytes, long totalSpaceBytes) {
        double usageRatio = (double) usedSpaceBytes / totalSpaceBytes;
        return usageRatio >= storage.getLocal().getAlertThreshold();
    }
    
    /**
     * 获取备份路径
     */
    public String getBackupPath() {
        return path;
    }
    
    /**
     * 获取加密密钥
     */
    public String getEncryptionKey() {
        return encryption.getKey();
    }
}