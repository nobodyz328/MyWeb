package com.myweb.website_core.application.service.security.integeration.dataManage;

import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.infrastructure.config.properties.BackupProperties;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

/**
 * 自动备份服务
 * 
 * 提供数据库和关键文件的自动备份功能，包括：
 * - 定时自动备份
 * - 备份文件加密存储
 * - 备份完整性验证
 * - 备份生命周期管理
 * - 备份状态监控和告警
 * 
 * 符合GB/T 22239-2019二级等保要求的数据备份机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {
    
    private final BackupProperties backupProperties;
    private final AuditLogServiceAdapter auditLogService;
    private final EmailService emailService;
    
    private static final String BACKUP_FILE_EXTENSION = ".backup";
    private static final String ENCRYPTED_EXTENSION = ".enc";
    private static final String HASH_EXTENSION = ".hash";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 备份类型枚举
     */
    public enum BackupType {
        FULL("完全备份", "备份所有数据"),
        INCREMENTAL("增量备份", "备份自上次备份以来的变更数据"),
        DIFFERENTIAL("差异备份", "备份自上次完全备份以来的变更数据");
        
        private final String displayName;
        private final String description;
        
        BackupType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * 备份结果
     */
    public static class BackupResult {
        private final boolean success;
        private final String backupId;
        private final BackupType backupType;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final long fileSizeBytes;
        private final String filePath;
        private final String checksum;
        private final String errorMessage;
        
        public BackupResult(boolean success, String backupId, BackupType backupType,
                          LocalDateTime startTime, LocalDateTime endTime, long fileSizeBytes,
                          String filePath, String checksum, String errorMessage) {
            this.success = success;
            this.backupId = backupId;
            this.backupType = backupType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.fileSizeBytes = fileSizeBytes;
            this.filePath = filePath;
            this.checksum = checksum;
            this.errorMessage = errorMessage;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getBackupId() { return backupId; }
        public BackupType getBackupType() { return backupType; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public long getFileSizeBytes() { return fileSizeBytes; }
        public String getFilePath() { return filePath; }
        public String getChecksum() { return checksum; }
        public String getErrorMessage() { return errorMessage; }
        
        public long getDurationMillis() {
            return endTime != null && startTime != null ? 
                java.time.Duration.between(startTime, endTime).toMillis() : 0;
        }
    }
    
    /**
     * 定时自动备份 - 每日凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Async
    public void scheduledBackup() {
        log.info("开始执行定时自动备份");
        
        try {
            // 执行完全备份
            CompletableFuture<BackupResult> backupFuture = performBackupAsync(BackupType.FULL);
            BackupResult result = backupFuture.get();
            
            if (result.isSuccess()) {
                log.info("定时自动备份成功 - 备份ID: {}, 文件大小: {} bytes, 耗时: {} ms",
                        result.getBackupId(), result.getFileSizeBytes(), result.getDurationMillis());
                
                // 发送成功通知
                sendBackupSuccessNotification(result);
                
                // 清理过期备份
                cleanupExpiredBackups();
                
                // 检查存储空间
                checkStorageSpaceAndAlert();
                
            } else {
                log.error("定时自动备份失败: {}", result.getErrorMessage());
                
                // 发送告警通知
                sendBackupFailureAlert(result);
            }
            
        } catch (Exception e) {
            log.error("定时自动备份异常", e);
            
            // 记录审计日志
//            auditLogService.logOperation(
//                    AuditLogRequest.error(
//                        AuditOperation.BACKUP_OPERATION,
//                        null,
//                            "SCHEDULED_BACKUP_FAILED",
//                            "定时自动备份失败: " + e.getMessage()
//                        )
//            );
        }
    }
    
    /**
     * 执行备份（异步）
     * 
     * @param backupType 备份类型
     * @return 备份结果
     */
    @Async
    public CompletableFuture<BackupResult> performBackupAsync(BackupType backupType) {
        return CompletableFuture.supplyAsync(() -> performBackup(backupType));
    }
    
    /**
     * 执行备份（同步）
     * 
     * @param backupType 备份类型
     * @return 备份结果
     */
    public BackupResult performBackup(BackupType backupType) {
        LocalDateTime startTime = LocalDateTime.now();
        String backupId = generateBackupId(backupType, startTime);
        
        log.info("开始执行备份 - 备份ID: {}, 类型: {}", backupId, backupType.getDisplayName());
        
        try {
            // 创建备份目录
            Path backupDir = createBackupDirectory();
            
            // 生成备份文件路径
            String backupFileName = backupId + BACKUP_FILE_EXTENSION;
            Path backupFilePath = backupDir.resolve(backupFileName);
            
            // 执行数据库备份
            boolean dbBackupSuccess = performDatabaseBackup(backupFilePath, backupType);
            if (!dbBackupSuccess) {
                throw new RuntimeException("数据库备份失败");
            }
            
            // 压缩备份文件
            Path compressedPath = compressBackupFile(backupFilePath);
            
            // 加密备份文件
            Path finalPath = encryptBackupFile(compressedPath);
            
            // 计算文件校验和
            String checksum = calculateFileChecksum(finalPath);
            
            // 保存校验和文件
            saveChecksumFile(finalPath, checksum);
            
            // 删除临时文件
            Files.deleteIfExists(backupFilePath);
            if (!finalPath.equals(compressedPath)) {
                Files.deleteIfExists(compressedPath);
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            long fileSize = Files.size(finalPath);
            
            // 记录审计日志
            auditLogService.logOperation(
                    AuditLogRequest.system(
                            AuditOperation.BACKUP_OPERATION,
                            String.format("备份完成 - ID: %s, 类型: %s, 大小: %d bytes",
                                    backupId, backupType.getDisplayName(), fileSize)
                    )
            );
            
            BackupResult result = new BackupResult(
                true, backupId, backupType, startTime, endTime,
                fileSize, finalPath.toString(), checksum, null
            );
            
            log.info("备份执行成功 - 备份ID: {}, 文件: {}, 大小: {} bytes, 耗时: {} ms",
                    backupId, finalPath, fileSize, result.getDurationMillis());
            
            return result;
            
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            String errorMessage = "备份执行失败: " + e.getMessage();
            
            log.error("备份执行失败 - 备份ID: {}", backupId, e);
            
            // 记录审计日志
//            auditLogService.logOperation(
//                AuditLogRequest.error(
//                        AuditOperation.BACKUP_OPERATION,
//                        null,
//                        null,
//                        "BACKUP_FAILED"
//                ).withResource("BACKUP_FILE",null )
//            );
            
            return new BackupResult(
                false, backupId, backupType, startTime, endTime,
                0, null, null, errorMessage
            );
        }
    }
    
    /**
     * 验证备份文件完整性
     * 
     * @param backupFilePath 备份文件路径
     * @return 验证结果
     */
    public boolean verifyBackupIntegrity(String backupFilePath) {
        try {
            Path backupPath = Paths.get(backupFilePath);
            Path checksumPath = Paths.get(backupFilePath + HASH_EXTENSION);
            
            if (!Files.exists(backupPath) || !Files.exists(checksumPath)) {
                log.warn("备份文件或校验和文件不存在: {}", backupFilePath);
                return false;
            }
            
            // 读取保存的校验和
            String savedChecksum = Files.readString(checksumPath).trim();
            
            // 计算当前文件校验和
            String currentChecksum = calculateFileChecksum(backupPath);
            
            boolean isValid = savedChecksum.equals(currentChecksum);
            
            if (isValid) {
                log.info("备份文件完整性验证成功: {}", backupFilePath);
            } else {
                log.error("备份文件完整性验证失败 - 文件: {}, 保存的校验和: {}, 当前校验和: {}",
                         backupFilePath, savedChecksum, currentChecksum);
            }
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                        AuditOperation.INTEGRITY_CHECK,
                        String.format("备份文件完整性验证%s: %s", isValid ? "成功" : "失败", backupFilePath)
                )
            );
            
            return isValid;
            
        } catch (Exception e) {
            log.error("验证备份文件完整性失败: {}", backupFilePath, e);
            return false;
        }
    }
    
    /**
     * 获取备份列表
     * 
     * @return 备份文件列表
     */
    public List<Map<String, Object>> getBackupList() {
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> backupList = new ArrayList<>();
            
            Files.list(backupDir)
                .filter(path -> path.toString().endsWith(ENCRYPTED_EXTENSION))
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .forEach(path -> {
                    try {
                        Map<String, Object> backupInfo = new HashMap<>();
                        backupInfo.put("fileName", path.getFileName().toString());
                        backupInfo.put("filePath", path.toString());
                        backupInfo.put("fileSize", Files.size(path));
                        backupInfo.put("lastModified", Files.getLastModifiedTime(path).toInstant());
                        backupInfo.put("isValid", verifyBackupIntegrity(path.toString()));
                        
                        backupList.add(backupInfo);
                    } catch (IOException e) {
                        log.warn("读取备份文件信息失败: {}", path, e);
                    }
                });
            
            return backupList;
            
        } catch (Exception e) {
            log.error("获取备份列表失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查存储空间并发送告警
     */
    public void checkStorageSpaceAndAlert() {
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                return;
            }
            
            // 获取存储空间信息
            java.nio.file.FileStore fileStore = Files.getFileStore(backupDir);
            long totalSpace = fileStore.getTotalSpace();
            long usableSpace = fileStore.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            double usageRatio = (double) usedSpace / totalSpace;
            double alertThreshold = backupProperties.getStorage().getLocal().getAlertThreshold();
            
            log.info("备份存储空间使用情况: 已使用 {:.2f}%, 阈值 {:.2f}%", 
                    usageRatio * 100, alertThreshold * 100);
            
            if (usageRatio >= alertThreshold) {
                sendStorageSpaceAlert(usedSpace, totalSpace, usageRatio);
            }
            
        } catch (Exception e) {
            log.error("检查存储空间失败", e);
        }
    }
    
    /**
     * 发送存储空间告警
     */
    private void sendStorageSpaceAlert(long usedSpace, long totalSpace, double usageRatio) {
        try {
            if (backupProperties.getNotification().isEnabled() && 
                backupProperties.getNotification().getEmail().isEnabled()) {
                
                String subject = "MyWeb系统备份存储空间告警";
                String content = String.format(
                    "备份存储空间使用率告警：\n\n" +
                    "存储路径: %s\n" +
                    "总空间: %.2f GB\n" +
                    "已使用: %.2f GB\n" +
                    "使用率: %.2f%%\n" +
                    "告警阈值: %.2f%%\n\n" +
                    "建议及时清理过期备份文件或扩展存储空间。\n\n" +
                    "系统管理员，\n" +
                    "MyWeb安全团队\n" +
                    "%s",
                    backupProperties.getBackupPath(),
                    totalSpace / 1024.0 / 1024.0 / 1024.0,
                    usedSpace / 1024.0 / 1024.0 / 1024.0,
                    usageRatio * 100,
                    backupProperties.getStorage().getLocal().getAlertThreshold() * 100,
                    java.time.LocalDateTime.now()
                );
                
                // 发送告警邮件
                String[] recipients = backupProperties.getNotification().getEmail().getRecipients();
                for (String recipient : recipients) {
                    try {
                        emailService.sendEmail(recipient, subject, content);
                        log.info("存储空间告警邮件已发送: recipient={}, usage={:.2f}%", 
                                recipient, usageRatio * 100);
                    } catch (Exception e) {
                        log.error("发送存储空间告警邮件失败: recipient={}, error={}", 
                                recipient, e.getMessage());
                    }
                }
                
                // 记录审计日志
                auditLogService.logOperation(
                    AuditLogRequest.system(
                        AuditOperation.BACKUP_OPERATION,
                        String.format("发送存储空间告警 - 使用率: %.2f%%, 接收者数量: %d", 
                                usageRatio * 100, recipients.length)
                    )
                );
            }
        } catch (Exception e) {
            log.error("发送存储空间告警异常", e);
        }
    }
    
    /**
     * 清理过期备份
     */
    public void cleanupExpiredBackups() {
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                return;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(backupProperties.getRetentionDays());
            
            Files.list(backupDir)
                .filter(path -> path.toString().endsWith(ENCRYPTED_EXTENSION))
                .forEach(path -> {
                    try {
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        
                        if (fileTime.isBefore(cutoffDate)) {
                            // 删除备份文件和校验和文件
                            Files.deleteIfExists(path);
                            Files.deleteIfExists(Paths.get(path.toString() + HASH_EXTENSION));
                            
                            log.info("清理过期备份文件: {}", path);
                            
                            // 记录审计日志
                            auditLogService.logOperation(
                               AuditLogRequest.system(
                                       AuditOperation.BACKUP_OPERATION,
                                       "清理过期备份文件: " + path.getFileName()
                               )
                            );
                        }
                    } catch (IOException e) {
                        log.warn("清理备份文件失败: {}", path, e);
                    }
                });
                
        } catch (Exception e) {
            log.error("清理过期备份失败", e);
        }
    }
    
    /**
     * 生成备份ID
     */
    private String generateBackupId(BackupType backupType, LocalDateTime timestamp) {
        String typePrefix = backupType.name().toLowerCase();
        String timeString = timestamp.format(BACKUP_DATE_FORMAT);
        return String.format("%s_%s", typePrefix, timeString);
    }
    
    /**
     * 创建备份目录
     */
    private Path createBackupDirectory() throws IOException {
        Path backupDir = Paths.get(backupProperties.getBackupPath());
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }
        return backupDir;
    }
    
    /**
     * 执行数据库备份
     */
    protected boolean performDatabaseBackup(Path backupFilePath, BackupType backupType) {
        try {
            log.info("开始执行数据库备份: type={}, file={}", backupType, backupFilePath);
            
            // 构建pg_dump命令
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(
                "pg_dump",
                "-h", "localhost",
                "-p", "5432",
                "-U", "postgres",
                "-d", "postgres",
                "-f", backupFilePath.toString(),
                "--verbose",
                "--no-password"
            );
            
            // 设置环境变量
            processBuilder.environment().put("PGPASSWORD", "123456");
            
            // 设置工作目录
            processBuilder.directory(new java.io.File(System.getProperty("user.home")));
            
            // 重定向错误输出
            processBuilder.redirectErrorStream(true);
            
            // 启动进程
            Process process = processBuilder.start();
            
            // 读取输出
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("pg_dump output: {}", line);
                }
            }
            
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(
                backupProperties.getSchedule().getTimeoutMinutes(), 
                java.util.concurrent.TimeUnit.MINUTES
            );
            
            if (!finished) {
                log.error("数据库备份超时，强制终止进程");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode == 0) {
                // 验证备份文件是否创建成功且不为空
                if (Files.exists(backupFilePath) && Files.size(backupFilePath) > 0) {
                    log.info("数据库备份成功: file={}, size={} bytes", 
                            backupFilePath, Files.size(backupFilePath));
                    return true;
                } else {
                    log.error("数据库备份文件为空或不存在: {}", backupFilePath);
                    return false;
                }
            } else {
                log.error("数据库备份失败，退出码: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("执行数据库备份异常: file={}", backupFilePath, e);
            return false;
        }
    }
    
    /**
     * 压缩备份文件
     */
    private Path compressBackupFile(Path backupFilePath) throws IOException {
        Path compressedPath = Paths.get(backupFilePath.toString() + ".gz");
        
        try (FileInputStream fis = new FileInputStream(backupFilePath.toFile());
             FileOutputStream fos = new FileOutputStream(compressedPath.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, length);
            }
        }
        
        log.info("备份文件压缩完成: {} -> {}", backupFilePath, compressedPath);
        return compressedPath;
    }
    
    /**
     * 加密备份文件
     */
    private Path encryptBackupFile(Path backupFilePath) throws Exception {
        if (!backupProperties.getEncryption().isEnabled()) {
            log.info("备份加密已禁用，跳过加密步骤");
            return backupFilePath;
        }
        
        Path encryptedPath = Paths.get(backupFilePath.toString() + ENCRYPTED_EXTENSION);
        
        // 生成或获取加密密钥
        SecretKey secretKey = getOrGenerateEncryptionKey();
        
        // 使用AES-GCM模式进行加密
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        
        // 生成随机IV
        byte[] iv = new byte[12]; // GCM推荐使用12字节IV
        new SecureRandom().nextBytes(iv);
        javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        
        try (FileInputStream fis = new FileInputStream(backupFilePath.toFile());
             FileOutputStream fos = new FileOutputStream(encryptedPath.toFile())) {
            
            // 首先写入IV
            fos.write(iv);
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                byte[] encrypted = cipher.update(buffer, 0, length);
                if (encrypted != null) {
                    fos.write(encrypted);
                }
            }
            
            // 写入最终的加密数据和认证标签
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                fos.write(finalBytes);
            }
        }
        
        log.info("备份文件加密完成: {} -> {}, 算法: AES-GCM", backupFilePath, encryptedPath);
        return encryptedPath;
    }
    
    /**
     * 获取或生成加密密钥
     */
    private SecretKey getOrGenerateEncryptionKey() throws Exception {
        String keyString = backupProperties.getEncryption().getKey();
        
        if (keyString == null || keyString.isEmpty()) {
            // 生成新的AES-256密钥
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(256);
            SecretKey newKey = keyGen.generateKey();
            
            log.warn("未配置备份加密密钥，已生成新密钥。建议在配置文件中设置固定密钥以确保备份文件的可恢复性。");
            return newKey;
        } else {
            try {
                // 尝试将配置的密钥作为Base64解码
                byte[] keyBytes = Base64.getDecoder().decode(keyString);
                if (keyBytes.length == 32) { // 256位 = 32字节
                    return new SecretKeySpec(keyBytes, AES_ALGORITHM);
                }
            } catch (IllegalArgumentException e) {
                // 如果不是有效的Base64，则使用字符串直接生成密钥
            }
            
            // 使用配置的字符串生成固定长度的密钥
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, AES_ALGORITHM);
        }
    }
    
    /**
     * 计算文件校验和
     */
    public String calculateFileChecksum(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, length);
            }
        }
        
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * 保存校验和文件
     */
    private void saveChecksumFile(Path backupFilePath, String checksum) throws IOException {
        Path checksumPath = Paths.get(backupFilePath.toString() + HASH_EXTENSION);
        Files.writeString(checksumPath, checksum);
    }
    
    /**
     * 发送备份失败告警
     */
    private void sendBackupFailureAlert(BackupResult result) {
        try {
            if (backupProperties.getNotification().isEnabled() && 
                backupProperties.getNotification().getEmail().isEnabled() &&
                backupProperties.getNotification().getEmail().isNotifyOnFailure()) {
                
                String subject = "MyWeb系统备份失败告警";
                String content = String.format(
                    "备份执行失败详情：\n\n" +
                    "备份ID: %s\n" +
                    "备份类型: %s\n" +
                    "开始时间: %s\n" +
                    "结束时间: %s\n" +
                    "错误信息: %s\n\n" +
                    "请及时检查系统状态并处理备份问题。\n\n" +
                    "系统管理员，\n" +
                    "MyWeb安全团队\n" +
                    "%s",
                    result.getBackupId(),
                    result.getBackupType().getDisplayName(),
                    result.getStartTime(),
                    result.getEndTime(),
                    result.getErrorMessage(),
                    java.time.LocalDateTime.now()
                );
                
                // 发送告警邮件给所有配置的接收者
                String[] recipients = backupProperties.getNotification().getEmail().getRecipients();
                for (String recipient : recipients) {
                    try {
                        emailService.sendEmail(recipient, subject, content);
                        log.info("备份失败告警邮件已发送: recipient={}, backupId={}", recipient, result.getBackupId());
                    } catch (Exception e) {
                        log.error("发送备份失败告警邮件失败: recipient={}, backupId={}, error={}", 
                                recipient, result.getBackupId(), e.getMessage());
                    }
                }
                
                // 记录审计日志
                auditLogService.logOperation(
                    AuditLogRequest.system(
                        AuditOperation.BACKUP_OPERATION,
                        String.format("发送备份失败告警 - 备份ID: %s, 接收者数量: %d", 
                                result.getBackupId(), recipients.length)
                    )
                );
            }
        } catch (Exception e) {
            log.error("发送备份失败告警异常", e);
        }
    }
    
    /**
     * 发送备份成功通知
     */
    private void sendBackupSuccessNotification(BackupResult result) {
        try {
            if (backupProperties.getNotification().isEnabled() && 
                backupProperties.getNotification().getEmail().isEnabled() &&
                backupProperties.getNotification().getEmail().isNotifyOnSuccess()) {
                
                String subject = "MyWeb系统备份成功通知";
                String content = String.format(
                    "备份执行成功详情：\n\n" +
                    "备份ID: %s\n" +
                    "备份类型: %s\n" +
                    "开始时间: %s\n" +
                    "结束时间: %s\n" +
                    "文件大小: %.2f MB\n" +
                    "执行耗时: %d 毫秒\n" +
                    "文件路径: %s\n" +
                    "校验和: %s\n\n" +
                    "备份文件已安全存储并加密。\n\n" +
                    "系统管理员，\n" +
                    "MyWeb安全团队\n" +
                    "%s",
                    result.getBackupId(),
                    result.getBackupType().getDisplayName(),
                    result.getStartTime(),
                    result.getEndTime(),
                    result.getFileSizeBytes() / 1024.0 / 1024.0,
                    result.getDurationMillis(),
                    result.getFilePath(),
                    result.getChecksum(),
                    java.time.LocalDateTime.now()
                );
                
                // 发送成功通知邮件给所有配置的接收者
                String[] recipients = backupProperties.getNotification().getEmail().getRecipients();
                for (String recipient : recipients) {
                    try {
                        emailService.sendEmail(recipient, subject, content);
                        log.info("备份成功通知邮件已发送: recipient={}, backupId={}", recipient, result.getBackupId());
                    } catch (Exception e) {
                        log.error("发送备份成功通知邮件失败: recipient={}, backupId={}, error={}", 
                                recipient, result.getBackupId(), e.getMessage());
                    }
                }
                
                // 记录审计日志
                auditLogService.logOperation(
                    AuditLogRequest.system(
                        AuditOperation.BACKUP_OPERATION,
                        String.format("发送备份成功通知 - 备份ID: %s, 接收者数量: %d", 
                                result.getBackupId(), recipients.length)
                    )
                );
            }
        } catch (Exception e) {
            log.error("发送备份成功通知异常", e);
        }
    }
}