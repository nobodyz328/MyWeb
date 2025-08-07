package com.myweb.website_core.application.service.security.fileProtect;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.dataprotect.DataIntegrityService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.FileIntegrityException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件完整性服务
 * 
 * 提供关键文件完整性验证功能，包括：
 * - 关键配置文件的完整性检查
 * - 配置文件变更检测和告警
 * - 系统启动时的完整性验证
 * - 关键文件哈希值管理
 * - 文件篡改恢复机制
 * 
 * 符合GB/T 22239-2019可信验证机制要求 6.1, 6.2, 6.4, 6.5
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Slf4j
@Service
public class FileIntegrityService {
    
    private final AuditLogService auditLogService;
    private final DataIntegrityService dataIntegrityService;
    
    // 关键文件哈希值存储
    private final Map<String, FileIntegrityRecord> fileIntegrityMap = new ConcurrentHashMap<>();
    
    // 配置参数
    @Value("${app.file-integrity.enabled:true}")
    private boolean integrityCheckEnabled;
    
    @Value("${app.file-integrity.critical-files-path:src/main/resources}")
    private String criticalFilesPath;
    
    @Value("${app.file-integrity.hash-storage-path:${java.io.tmpdir}/myweb/file-hashes}")
    private String hashStoragePath;
    
    @Value("${app.file-integrity.backup-path:${java.io.tmpdir}/myweb/file-backups}")
    private String backupPath;
    
    @Value("${app.file-integrity.alert-enabled:true}")
    private boolean alertEnabled;
    
    // 关键文件列表
    private static final Set<String> CRITICAL_FILES = Set.of(
        "application.yml",
        "application-security.yml", 
        "application-security-events.yml",
        "logback-spring.xml",
        "keystore.p12"
    );
    
    // 关键目录列表
    private static final Set<String> CRITICAL_DIRECTORIES = Set.of(
        "src/main/resources",
        "src/main/java/com/myweb/website_core/infrastructure/config"
    );
    
    @Autowired
    public FileIntegrityService(AuditLogService auditLogService, DataIntegrityService dataIntegrityService) {
        this.auditLogService = auditLogService;
        this.dataIntegrityService = dataIntegrityService;
    }
    
    @PostConstruct
    public void init() {
        if (!integrityCheckEnabled) {
            log.info("文件完整性检查已禁用");
            return;
        }
        
        try {
            // 创建必要的目录
            createDirectories();
            log.info("文件完整性服务初始化完成");
        } catch (Exception e) {
            log.error("文件完整性服务初始化失败", e);
        }
    }
    
    /**
     * 系统启动时执行完整性检查
     */
    @EventListener(ApplicationReadyEvent.class)
    public void performStartupIntegrityCheck() {
        if (!integrityCheckEnabled) {
            return;
        }
        
        log.info("开始执行系统启动完整性检查");
        
        CompletableFuture.runAsync(() -> {
            try {
                // 加载已存储的文件哈希值
                loadStoredHashes();
                
                // 检查关键文件完整性
                List<FileIntegrityResult> results = checkCriticalFilesIntegrity();
                
                // 处理检查结果
                processIntegrityResults(results, "STARTUP_CHECK");
                
                log.info("系统启动完整性检查完成，检查了{}个文件", results.size());
                
            } catch (Exception e) {
                log.error("系统启动完整性检查失败", e);
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "SYSTEM",
                    "系统启动完整性检查失败: " + e.getMessage()
                );
            }
        });
    }
    
    /**
     * 定时执行文件完整性检查
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void performScheduledIntegrityCheck() {
        if (!integrityCheckEnabled) {
            return;
        }
        
        // 异步执行完整性检查
        performIntegrityCheckAsync();
    }
    
    /**
     * 异步执行文件完整性检查的具体实现
     */
    @Async
    public CompletableFuture<Void> performIntegrityCheckAsync() {
        try {
            log.info("开始执行定时文件完整性检查");
            
            // 检查关键文件完整性
            List<FileIntegrityResult> results = checkCriticalFilesIntegrity();
            
            // 处理检查结果
            processIntegrityResults(results, "SCHEDULED_CHECK");
            
            // 更新文件哈希值
            updateFileHashes(results);
            
            log.info("定时文件完整性检查完成，检查了{}个文件", results.size());
            
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                String.format("定时文件完整性检查完成，检查了%d个文件", results.size())
            );
            
        } catch (Exception e) {
            log.error("定时文件完整性检查失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                "定时文件完整性检查失败: " + e.getMessage()
            );
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 检查关键文件完整性
     */
    public List<FileIntegrityResult> checkCriticalFilesIntegrity() {
        List<FileIntegrityResult> results = new ArrayList<>();
        
        try {
            // 检查关键文件
            for (String fileName : CRITICAL_FILES) {
                Path filePath = findCriticalFile(fileName);
                if (filePath != null && Files.exists(filePath)) {
                    FileIntegrityResult result = checkFileIntegrity(filePath);
                    results.add(result);
                } else {
                    // 文件不存在
                    FileIntegrityResult result = FileIntegrityResult.builder()
                        .filePath(fileName)
                        .exists(false)
                        .isValid(false)
                        .errorMessage("关键文件不存在: " + fileName)
                        .checkTime(LocalDateTime.now())
                        .build();
                    results.add(result);
                    
                    log.warn("关键文件不存在: {}", fileName);
                }
            }
            
            // 检查关键目录下的配置文件
            for (String dirPath : CRITICAL_DIRECTORIES) {
                Path directory = Paths.get(dirPath);
                if (Files.exists(directory) && Files.isDirectory(directory)) {
                    results.addAll(checkDirectoryIntegrity(directory));
                }
            }
            
        } catch (Exception e) {
            log.error("检查关键文件完整性失败", e);
            throw new FileIntegrityException("检查关键文件完整性失败", e);
        }
        
        return results;
    }
    
    /**
     * 检查单个文件完整性
     */
    public FileIntegrityResult checkFileIntegrity(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return FileIntegrityResult.builder()
                    .filePath(filePath.toString())
                    .exists(false)
                    .isValid(false)
                    .errorMessage("文件不存在")
                    .checkTime(LocalDateTime.now())
                    .build();
            }
            
            // 计算当前文件哈希值
            String currentHash = calculateFileHash(filePath);
            
            // 获取存储的哈希值
            FileIntegrityRecord storedRecord = fileIntegrityMap.get(filePath.toString());
            
            boolean isValid = true;
            String errorMessage = null;
            
            if (storedRecord != null) {
                // 验证哈希值
                isValid = storedRecord.getHashValue().equals(currentHash);
                if (!isValid) {
                    errorMessage = "文件哈希值不匹配，可能已被篡改";
                    log.warn("文件完整性验证失败: {} - 期望哈希: {}, 实际哈希: {}", 
                            filePath, storedRecord.getHashValue(), currentHash);
                }
            } else {
                // 首次检查，记录哈希值
                storedRecord = new FileIntegrityRecord(
                    filePath.toString(),
                    currentHash,
                    Files.getLastModifiedTime(filePath).toInstant(),
                    LocalDateTime.now()
                );
                fileIntegrityMap.put(filePath.toString(), storedRecord);
                log.info("首次记录文件哈希值: {}", filePath);
            }
            
            return FileIntegrityResult.builder()
                .filePath(filePath.toString())
                .exists(true)
                .isValid(isValid)
                .currentHash(currentHash)
                .expectedHash(storedRecord.getHashValue())
                .lastModified(Files.getLastModifiedTime(filePath).toInstant())
                .errorMessage(errorMessage)
                .checkTime(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("检查文件完整性失败: {}", filePath, e);
            return FileIntegrityResult.builder()
                .filePath(filePath.toString())
                .exists(Files.exists(filePath))
                .isValid(false)
                .errorMessage("完整性检查异常: " + e.getMessage())
                .checkTime(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * 检查目录下文件的完整性
     */
    private List<FileIntegrityResult> checkDirectoryIntegrity(Path directory) {
        List<FileIntegrityResult> results = new ArrayList<>();
        
        try {
            Files.walk(directory, 2) // 最多遍历2层
                .filter(Files::isRegularFile)
                .filter(path -> isConfigurationFile(path))
                .forEach(path -> {
                    try {
                        FileIntegrityResult result = checkFileIntegrity(path);
                        results.add(result);
                    } catch (Exception e) {
                        log.error("检查目录文件完整性失败: {}", path, e);
                    }
                });
                
        } catch (IOException e) {
            log.error("遍历目录失败: {}", directory, e);
        }
        
        return results;
    }
    
    /**
     * 计算文件哈希值
     */
    private String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);
        return Base64.getEncoder().encodeToString(hashBytes);
    }
    
    /**
     * 查找关键文件
     */
    private Path findCriticalFile(String fileName) {
        // 在多个可能的位置查找文件
        String[] searchPaths = {
            "src/main/resources/" + fileName,
            "website_core/src/main/resources/" + fileName,
            fileName
        };
        
        for (String searchPath : searchPaths) {
            Path path = Paths.get(searchPath);
            if (Files.exists(path)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * 判断是否为配置文件
     */
    private boolean isConfigurationFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".yml") || 
               fileName.endsWith(".yaml") || 
               fileName.endsWith(".properties") ||
               fileName.endsWith(".xml") ||
               fileName.endsWith(".json") ||
               fileName.endsWith(".p12") ||
               fileName.endsWith(".jks");
    }
    
    /**
     * 处理完整性检查结果
     */
    private void processIntegrityResults(List<FileIntegrityResult> results, String checkType) {
        int totalFiles = results.size();
        int validFiles = 0;
        int invalidFiles = 0;
        int missingFiles = 0;
        
        List<FileIntegrityResult> failedResults = new ArrayList<>();
        
        for (FileIntegrityResult result : results) {
            if (!result.isExists()) {
                missingFiles++;
                failedResults.add(result);
            } else if (result.isValid()) {
                validFiles++;
            } else {
                invalidFiles++;
                failedResults.add(result);
            }
        }
        
        log.info("文件完整性检查结果 [{}]: 总计={}, 有效={}, 无效={}, 缺失={}", 
                checkType, totalFiles, validFiles, invalidFiles, missingFiles);
        
        // 记录审计日志
        auditLogService.logSecurityEvent(
            AuditOperation.INTEGRITY_CHECK,
            "SYSTEM",
            String.format("文件完整性检查 [%s]: 总计=%d, 有效=%d, 无效=%d, 缺失=%d", 
                    checkType, totalFiles, validFiles, invalidFiles, missingFiles)
        );
        
        // 处理失败的结果
        if (!failedResults.isEmpty()) {
            handleIntegrityFailures(failedResults, checkType);
        }
    }
    
    /**
     * 处理完整性检查失败
     */
    private void handleIntegrityFailures(List<FileIntegrityResult> failedResults, String checkType) {
        for (FileIntegrityResult result : failedResults) {
            log.error("文件完整性检查失败: {} - {}", result.getFilePath(), result.getErrorMessage());
            
            // 记录安全事件
            auditLogService.logSecurityEvent(
                AuditOperation.SECURITY_ALERT,
                "SYSTEM",
                String.format("文件完整性检查失败 [%s]: %s - %s", 
                        checkType, result.getFilePath(), result.getErrorMessage())
            );
            
            // 尝试恢复文件
            if (result.isExists() && !result.isValid()) {
                attemptFileRecovery(result.getFilePath());
            }
        }
        
        // 发送告警
        if (alertEnabled && !failedResults.isEmpty()) {
            sendIntegrityAlert(failedResults, checkType);
        }
    }
    
    /**
     * 尝试文件恢复
     */
    @Async
    public CompletableFuture<Boolean> attemptFileRecovery(String filePath) {
        try {
            log.info("尝试恢复文件: {}", filePath);
            
            Path originalFile = Paths.get(filePath);
            Path backupFile = getBackupFilePath(filePath);
            
            if (Files.exists(backupFile)) {
                // 验证备份文件完整性
                FileIntegrityResult backupResult = checkFileIntegrity(backupFile);
                
                if (backupResult.isValid()) {
                    // 备份原文件
                    Path corruptedBackup = Paths.get(filePath + ".corrupted." + System.currentTimeMillis());
                    Files.move(originalFile, corruptedBackup);
                    
                    // 恢复备份文件
                    Files.copy(backupFile, originalFile);
                    
                    log.info("文件恢复成功: {} <- {}", filePath, backupFile);
                    
                    auditLogService.logSecurityEvent(
                        AuditOperation.FILE_RECOVERY,
                        "SYSTEM",
                        String.format("文件恢复成功: %s <- %s", filePath, backupFile)
                    );
                    
                    return CompletableFuture.completedFuture(true);
                } else {
                    log.warn("备份文件也已损坏，无法恢复: {}", backupFile);
                }
            } else {
                log.warn("备份文件不存在，无法恢复: {}", backupFile);
            }
            
        } catch (Exception e) {
            log.error("文件恢复失败: {}", filePath, e);
            auditLogService.logSecurityEvent(
                AuditOperation.FILE_RECOVERY,
                "SYSTEM",
                String.format("文件恢复失败: %s - %s", filePath, e.getMessage())
            );
        }
        
        return CompletableFuture.completedFuture(false);
    }
    
    /**
     * 创建文件备份
     */
    public CompletableFuture<Boolean> createFileBackup(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path originalFile = Paths.get(filePath);
                if (!Files.exists(originalFile)) {
                    log.warn("要备份的文件不存在: {}", filePath);
                    return false;
                }
                
                Path backupFile = getBackupFilePath(filePath);
                Files.createDirectories(backupFile.getParent());
                
                // 创建备份
                Files.copy(originalFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                
                // 计算并存储备份文件哈希值
                String backupHash = calculateFileHash(backupFile);
                FileIntegrityRecord backupRecord = new FileIntegrityRecord(
                    backupFile.toString(),
                    backupHash,
                    Files.getLastModifiedTime(backupFile).toInstant(),
                    LocalDateTime.now()
                );
                fileIntegrityMap.put(backupFile.toString(), backupRecord);
                
                log.info("文件备份创建成功: {} -> {}", filePath, backupFile);
                
                auditLogService.logSecurityEvent(
                    AuditOperation.FILE_BACKUP,
                    "SYSTEM",
                    String.format("文件备份创建成功: %s -> %s", filePath, backupFile)
                );
                
                return true;
                
            } catch (Exception e) {
                log.error("创建文件备份失败: {}", filePath, e);
                return false;
            }
        });
    }
    
    /**
     * 发送完整性告警
     */
    @Async
    public void sendIntegrityAlert(List<FileIntegrityResult> failedResults, String checkType) {
        try {
            StringBuilder alertMessage = new StringBuilder();
            alertMessage.append(String.format("文件完整性检查告警 [%s]:\n", checkType));
            alertMessage.append(String.format("发现 %d 个文件完整性问题:\n", failedResults.size()));
            
            for (FileIntegrityResult result : failedResults) {
                alertMessage.append(String.format("- %s: %s\n", 
                        result.getFilePath(), result.getErrorMessage()));
            }
            
            alertMessage.append("\n请立即检查系统安全性！");
            
            log.warn("发送文件完整性告警: {}", alertMessage.toString());
            
            // 记录告警到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SECURITY_ALERT,
                "SYSTEM",
                "文件完整性告警: " + alertMessage.toString()
            );
            
            // TODO: 实际实现时可以集成邮件或短信告警服务
            
        } catch (Exception e) {
            log.error("发送文件完整性告警失败", e);
        }
    }
    
    /**
     * 手动触发完整性检查
     */
    public CompletableFuture<List<FileIntegrityResult>> triggerManualIntegrityCheck() {
        log.info("手动触发文件完整性检查");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<FileIntegrityResult> results = checkCriticalFilesIntegrity();
                processIntegrityResults(results, "MANUAL_CHECK");
                return results;
            } catch (Exception e) {
                log.error("手动完整性检查失败", e);
                throw new RuntimeException("手动完整性检查失败", e);
            }
        });
    }
    
    /**
     * 更新文件哈希值
     */
    private void updateFileHashes(List<FileIntegrityResult> results) {
        for (FileIntegrityResult result : results) {
            if (result.isExists() && result.isValid()) {
                try {
                    Path filePath = Paths.get(result.getFilePath());
                    FileIntegrityRecord record = new FileIntegrityRecord(
                        result.getFilePath(),
                        result.getCurrentHash(),
                        result.getLastModified(),
                        LocalDateTime.now()
                    );
                    fileIntegrityMap.put(result.getFilePath(), record);
                    
                    // 持久化哈希值
                    saveHashToFile(record);
                    
                } catch (Exception e) {
                    log.error("更新文件哈希值失败: {}", result.getFilePath(), e);
                }
            }
        }
    }
    
    /**
     * 加载已存储的哈希值
     */
    private void loadStoredHashes() {
        try {
            Path hashDir = Paths.get(hashStoragePath);
            if (!Files.exists(hashDir)) {
                return;
            }
            
            Files.walk(hashDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".hash"))
                .forEach(this::loadHashFromFile);
                
            log.info("加载了 {} 个文件哈希记录", fileIntegrityMap.size());
            
        } catch (Exception e) {
            log.error("加载文件哈希值失败", e);
        }
    }
    
    /**
     * 从文件加载哈希值
     */
    private void loadHashFromFile(Path hashFile) {
        try {
            List<String> lines = Files.readAllLines(hashFile, StandardCharsets.UTF_8);
            if (lines.size() >= 4) {
                String filePath = lines.get(0);
                String hashValue = lines.get(1);
                String lastModified = lines.get(2);
                String recordTime = lines.get(3);
                
                FileIntegrityRecord record = new FileIntegrityRecord(
                    filePath,
                    hashValue,
                    java.time.Instant.parse(lastModified),
                    LocalDateTime.parse(recordTime)
                );
                
                fileIntegrityMap.put(filePath, record);
            }
        } catch (Exception e) {
            log.error("加载哈希文件失败: {}", hashFile, e);
        }
    }
    
    /**
     * 保存哈希值到文件
     */
    private void saveHashToFile(FileIntegrityRecord record) {
        try {
            Path hashDir = Paths.get(hashStoragePath);
            Files.createDirectories(hashDir);
            
            String fileName = record.getFilePath().replaceAll("[/\\\\:]", "_") + ".hash";
            Path hashFile = hashDir.resolve(fileName);
            
            List<String> lines = Arrays.asList(
                record.getFilePath(),
                record.getHashValue(),
                record.getLastModified().toString(),
                record.getRecordTime().toString()
            );
            
            Files.write(hashFile, lines, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("保存哈希文件失败: {}", record.getFilePath(), e);
        }
    }
    
    /**
     * 获取备份文件路径
     */
    private Path getBackupFilePath(String originalFilePath) {
        String fileName = Paths.get(originalFilePath).getFileName().toString();
        return Paths.get(backupPath, fileName + ".backup");
    }
    
    /**
     * 创建必要的目录
     */
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(hashStoragePath));
        Files.createDirectories(Paths.get(backupPath));
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 获取文件完整性统计信息
     */
    public FileIntegrityStatistics getIntegrityStatistics() {
        int totalFiles = fileIntegrityMap.size();
        int validFiles = 0;
        int needsCheck = 0;
        
        for (FileIntegrityRecord record : fileIntegrityMap.values()) {
            Path filePath = Paths.get(record.getFilePath());
            if (Files.exists(filePath)) {
                try {
                    String currentHash = calculateFileHash(filePath);
                    if (record.getHashValue().equals(currentHash)) {
                        validFiles++;
                    } else {
                        needsCheck++;
                    }
                } catch (Exception e) {
                    needsCheck++;
                }
            } else {
                needsCheck++;
            }
        }
        
        return FileIntegrityStatistics.builder()
            .totalFiles(totalFiles)
            .validFiles(validFiles)
            .invalidFiles(needsCheck)
            .lastCheckTime(LocalDateTime.now())
            .integrityCheckEnabled(integrityCheckEnabled)
            .alertEnabled(alertEnabled)
            .build();
    }
    
    /**
     * 检查指定文件的完整性
     */
    public FileIntegrityResult checkSpecificFile(String filePath) {
        Path path = Paths.get(filePath);
        return checkFileIntegrity(path);
    }
    
    /**
     * 获取支持的哈希算法
     */
    public String[] getSupportedHashAlgorithms() {
        return new String[]{"SHA-256", "SHA-1", "MD5"};
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 文件完整性记录
     */
    public static class FileIntegrityRecord {
        private final String filePath;
        private final String hashValue;
        private final java.time.Instant lastModified;
        private final LocalDateTime recordTime;
        
        public FileIntegrityRecord(String filePath, String hashValue, 
                                 java.time.Instant lastModified, LocalDateTime recordTime) {
            this.filePath = filePath;
            this.hashValue = hashValue;
            this.lastModified = lastModified;
            this.recordTime = recordTime;
        }
        
        // Getters
        public String getFilePath() { return filePath; }
        public String getHashValue() { return hashValue; }
        public java.time.Instant getLastModified() { return lastModified; }
        public LocalDateTime getRecordTime() { return recordTime; }
    }
    
    /**
     * 文件完整性检查结果
     */
    public static class FileIntegrityResult {
        private String filePath;
        private boolean exists;
        private boolean isValid;
        private String currentHash;
        private String expectedHash;
        private java.time.Instant lastModified;
        private String errorMessage;
        private LocalDateTime checkTime;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private FileIntegrityResult result = new FileIntegrityResult();
            
            public Builder filePath(String filePath) {
                result.filePath = filePath;
                return this;
            }
            
            public Builder exists(boolean exists) {
                result.exists = exists;
                return this;
            }
            
            public Builder isValid(boolean isValid) {
                result.isValid = isValid;
                return this;
            }
            
            public Builder currentHash(String currentHash) {
                result.currentHash = currentHash;
                return this;
            }
            
            public Builder expectedHash(String expectedHash) {
                result.expectedHash = expectedHash;
                return this;
            }
            
            public Builder lastModified(java.time.Instant lastModified) {
                result.lastModified = lastModified;
                return this;
            }
            
            public Builder errorMessage(String errorMessage) {
                result.errorMessage = errorMessage;
                return this;
            }
            
            public Builder checkTime(LocalDateTime checkTime) {
                result.checkTime = checkTime;
                return this;
            }
            
            public FileIntegrityResult build() {
                return result;
            }
        }
        
        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }
        
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { isValid = valid; }
        
        public String getCurrentHash() { return currentHash; }
        public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }
        
        public String getExpectedHash() { return expectedHash; }
        public void setExpectedHash(String expectedHash) { this.expectedHash = expectedHash; }
        
        public java.time.Instant getLastModified() { return lastModified; }
        public void setLastModified(java.time.Instant lastModified) { this.lastModified = lastModified; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        
        @Override
        public String toString() {
            return String.format(
                "FileIntegrityResult{filePath='%s', exists=%s, isValid=%s, currentHash='%s', expectedHash='%s', lastModified=%s, errorMessage='%s', checkTime=%s}",
                filePath, exists, isValid, currentHash, expectedHash, lastModified, errorMessage, checkTime
            );
        }
    }
    
    /**
     * 文件完整性统计信息
     */
    @Getter
    public static class FileIntegrityStatistics {
        private int totalFiles;
        private int validFiles;
        private int invalidFiles;
        private LocalDateTime lastCheckTime;
        private boolean integrityCheckEnabled;
        private boolean alertEnabled;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private FileIntegrityStatistics statistics = new FileIntegrityStatistics();
            
            public Builder totalFiles(int totalFiles) {
                statistics.totalFiles = totalFiles;
                return this;
            }
            
            public Builder validFiles(int validFiles) {
                statistics.validFiles = validFiles;
                return this;
            }
            
            public Builder invalidFiles(int invalidFiles) {
                statistics.invalidFiles = invalidFiles;
                return this;
            }
            
            public Builder lastCheckTime(LocalDateTime lastCheckTime) {
                statistics.lastCheckTime = lastCheckTime;
                return this;
            }
            
            public Builder integrityCheckEnabled(boolean integrityCheckEnabled) {
                statistics.integrityCheckEnabled = integrityCheckEnabled;
                return this;
            }
            
            public Builder alertEnabled(boolean alertEnabled) {
                statistics.alertEnabled = alertEnabled;
                return this;
            }
            
            public FileIntegrityStatistics build() {
                return statistics;
            }
        }

        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

        public void setValidFiles(int validFiles) { this.validFiles = validFiles; }

        public void setInvalidFiles(int invalidFiles) { this.invalidFiles = invalidFiles; }

        public void setLastCheckTime(LocalDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }

        public void setIntegrityCheckEnabled(boolean integrityCheckEnabled) { this.integrityCheckEnabled = integrityCheckEnabled; }

        public void setAlertEnabled(boolean alertEnabled) { this.alertEnabled = alertEnabled; }
        
        @Override
        public String toString() {
            return String.format(
                "FileIntegrityStatistics{totalFiles=%d, validFiles=%d, invalidFiles=%d, lastCheckTime=%s, integrityCheckEnabled=%s, alertEnabled=%s}",
                totalFiles, validFiles, invalidFiles, lastCheckTime, integrityCheckEnabled, alertEnabled
            );
        }
    }
}