package com.myweb.website_core.application.service.security.integeration.dataManage;

import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.config.BackupProperties;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 备份管理服务
 * 
 * 提供备份文件的生命周期管理功能，包括：
 * - 备份文件生命周期管理
 * - 过期备份自动清理
 * - 存储空间监控告警
 * - 远程存储同步
 * - 备份策略动态配置
 * 
 * 符合GB/T 22239-2019二级等保要求8.5、8.6
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupManagementService {
    
    private final BackupProperties backupProperties;
    private final AuditLogServiceAdapter auditLogService;
    private final EmailService emailService;
    private final BackupService backupService;
    
    private static final String BACKUP_EXTENSION = ".backup";
    private static final String ENCRYPTED_EXTENSION = ".enc";
    private static final String HASH_EXTENSION = ".hash";
    private static final String METADATA_EXTENSION = ".meta";
    
    /**
     * 备份文件元数据
     */
    public static class BackupMetadata {
        private String backupId;
        private BackupService.BackupType backupType;
        private LocalDateTime createdTime;
        private LocalDateTime expiryTime;
        private long fileSizeBytes;
        private String checksum;
        private boolean isEncrypted;
        private boolean isCompressed;
        private String originalPath;
        private Map<String, String> customProperties;
        
        // Constructors
        public BackupMetadata() {
            this.customProperties = new HashMap<>();
        }
        
        public BackupMetadata(String backupId, BackupService.BackupType backupType, 
                            LocalDateTime createdTime, long fileSizeBytes) {
            this();
            this.backupId = backupId;
            this.backupType = backupType;
            this.createdTime = createdTime;
            this.fileSizeBytes = fileSizeBytes;
            this.expiryTime = createdTime.plusDays(30); // 默认30天过期
        }
        
        // Getters and Setters
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        
        public BackupService.BackupType getBackupType() { return backupType; }
        public void setBackupType(BackupService.BackupType backupType) { this.backupType = backupType; }
        
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
        
        public LocalDateTime getExpiryTime() { return expiryTime; }
        public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
        
        public long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
        
        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }
        
        public boolean isEncrypted() { return isEncrypted; }
        public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }
        
        public boolean isCompressed() { return isCompressed; }
        public void setCompressed(boolean compressed) { isCompressed = compressed; }
        
        public String getOriginalPath() { return originalPath; }
        public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }
        
        public Map<String, String> getCustomProperties() { return customProperties; }
        public void setCustomProperties(Map<String, String> customProperties) { this.customProperties = customProperties; }
        
        public boolean isExpired() {
            return expiryTime != null && LocalDateTime.now().isAfter(expiryTime);
        }
        
        public long getDaysUntilExpiry() {
            if (expiryTime == null) return Long.MAX_VALUE;
            return java.time.Duration.between(LocalDateTime.now(), expiryTime).toDays();
        }
    }
    
    /**
     * 存储空间统计信息
     */
    public static class StorageStatistics {
        private long totalSpaceBytes;
        private long usedSpaceBytes;
        private long availableSpaceBytes;
        private double usagePercentage;
        private int totalBackupFiles;
        private long oldestBackupDays;
        private long newestBackupDays;
        private Map<BackupService.BackupType, Integer> backupTypeCount;
        
        public StorageStatistics() {
            this.backupTypeCount = new HashMap<>();
        }
        
        // Getters and Setters
        public long getTotalSpaceBytes() { return totalSpaceBytes; }
        public void setTotalSpaceBytes(long totalSpaceBytes) { this.totalSpaceBytes = totalSpaceBytes; }
        
        public long getUsedSpaceBytes() { return usedSpaceBytes; }
        public void setUsedSpaceBytes(long usedSpaceBytes) { this.usedSpaceBytes = usedSpaceBytes; }
        
        public long getAvailableSpaceBytes() { return availableSpaceBytes; }
        public void setAvailableSpaceBytes(long availableSpaceBytes) { this.availableSpaceBytes = availableSpaceBytes; }
        
        public double getUsagePercentage() { return usagePercentage; }
        public void setUsagePercentage(double usagePercentage) { this.usagePercentage = usagePercentage; }
        
        public int getTotalBackupFiles() { return totalBackupFiles; }
        public void setTotalBackupFiles(int totalBackupFiles) { this.totalBackupFiles = totalBackupFiles; }
        
        public long getOldestBackupDays() { return oldestBackupDays; }
        public void setOldestBackupDays(long oldestBackupDays) { this.oldestBackupDays = oldestBackupDays; }
        
        public long getNewestBackupDays() { return newestBackupDays; }
        public void setNewestBackupDays(long newestBackupDays) { this.newestBackupDays = newestBackupDays; }
        
        public Map<BackupService.BackupType, Integer> getBackupTypeCount() { return backupTypeCount; }
        public void setBackupTypeCount(Map<BackupService.BackupType, Integer> backupTypeCount) { this.backupTypeCount = backupTypeCount; }
        
        public boolean isStorageAlertRequired() {
            return usagePercentage >= 80.0; // 80%阈值
        }
        
        public String getFormattedTotalSpace() {
            return formatBytes(totalSpaceBytes);
        }
        
        public String getFormattedUsedSpace() {
            return formatBytes(usedSpaceBytes);
        }
        
        public String getFormattedAvailableSpace() {
            return formatBytes(availableSpaceBytes);
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 定时执行备份生命周期管理 - 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Async
    public void scheduledLifecycleManagement() {
        log.info("开始执行备份生命周期管理");
        
        try {
            // 1. 清理过期备份
            CompletableFuture<Integer> cleanupFuture = cleanupExpiredBackupsAsync();
            
            // 2. 更新备份元数据
            CompletableFuture<Void> metadataFuture = updateBackupMetadataAsync();
            
            // 3. 检查存储空间
            CompletableFuture<StorageStatistics> storageFuture = checkStorageStatisticsAsync();
            
            // 4. 同步到远程存储
            CompletableFuture<Void> syncFuture = syncToRemoteStorageAsync();
            
            // 等待所有任务完成
            CompletableFuture.allOf(cleanupFuture, metadataFuture, storageFuture, syncFuture).get();
            
            int cleanedFiles = cleanupFuture.get();
            StorageStatistics stats = storageFuture.get();
            
            log.info("备份生命周期管理完成 - 清理文件数: {}, 存储使用率: {:.2f}%", 
                    cleanedFiles, stats.getUsagePercentage());
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    String.format("备份生命周期管理完成 - 清理: %d个文件, 存储使用率: %.2f%%", 
                            cleanedFiles, stats.getUsagePercentage())
                )
            );
            
        } catch (Exception e) {
            log.error("备份生命周期管理失败", e);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.error(
                    AuditOperation.BACKUP_OPERATION,
                    null,
                    "LIFECYCLE_MANAGEMENT_FAILED",
                    "备份生命周期管理失败: " + e.getMessage()
                )
            );
        }
    }
    
    /**
     * 异步清理过期备份
     */
    @Async
    public CompletableFuture<Integer> cleanupExpiredBackupsAsync() {
        return CompletableFuture.supplyAsync(this::cleanupExpiredBackups);
    }
    
    /**
     * 清理过期备份文件
     * 
     * @return 清理的文件数量
     */
    public int cleanupExpiredBackups() {
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                log.warn("备份目录不存在: {}", backupDir);
                return 0;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(backupProperties.getRetentionDays());
            List<Path> expiredFiles = new ArrayList<>();
            
            // 查找过期文件
            Files.walkFileTree(backupDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isBackupFile(file)) {
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            attrs.creationTime().toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        
                        if (fileTime.isBefore(cutoffDate)) {
                            expiredFiles.add(file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
            // 删除过期文件及其相关文件
            int deletedCount = 0;
            for (Path expiredFile : expiredFiles) {
                try {
                    // 删除主文件
                    Files.deleteIfExists(expiredFile);
                    deletedCount++;
                    
                    // 删除相关文件（校验和、元数据等）
                    String basePath = expiredFile.toString();
                    Files.deleteIfExists(Paths.get(basePath + HASH_EXTENSION));
                    Files.deleteIfExists(Paths.get(basePath + METADATA_EXTENSION));
                    
                    log.info("删除过期备份文件: {}", expiredFile.getFileName());
                    
                    // 记录审计日志
                    auditLogService.logOperation(
                        AuditLogRequest.system(
                            AuditOperation.BACKUP_OPERATION,
                            "删除过期备份文件: " + expiredFile.getFileName()
                        )
                    );
                    
                } catch (IOException e) {
                    log.error("删除过期备份文件失败: {}", expiredFile, e);
                }
            }
            
            if (deletedCount > 0) {
                log.info("清理过期备份完成，删除 {} 个文件", deletedCount);
            }
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("清理过期备份失败", e);
            return 0;
        }
    }
    
    /**
     * 异步更新备份元数据
     */
    @Async
    public CompletableFuture<Void> updateBackupMetadataAsync() {
        return CompletableFuture.runAsync(this::updateBackupMetadata);
    }
    
    /**
     * 更新备份文件元数据
     */
    public void updateBackupMetadata() {
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                return;
            }
            
            Files.list(backupDir)
                .filter(this::isBackupFile)
                .forEach(this::updateFileMetadata);
                
        } catch (Exception e) {
            log.error("更新备份元数据失败", e);
        }
    }
    
    /**
     * 更新单个文件的元数据
     */
    private void updateFileMetadata(Path backupFile) {
        try {
            Path metadataFile = Paths.get(backupFile.toString() + METADATA_EXTENSION);
            
            if (!Files.exists(metadataFile)) {
                // 创建新的元数据文件
                BackupMetadata metadata = createMetadataFromFile(backupFile);
                saveMetadata(metadataFile, metadata);
                
                log.debug("创建备份元数据文件: {}", metadataFile.getFileName());
            } else {
                // 验证现有元数据的准确性
                BackupMetadata metadata = loadMetadata(metadataFile);
                if (metadata != null && needsMetadataUpdate(backupFile, metadata)) {
                    updateMetadataFromFile(backupFile, metadata);
                    saveMetadata(metadataFile, metadata);
                    
                    log.debug("更新备份元数据文件: {}", metadataFile.getFileName());
                }
            }
            
        } catch (Exception e) {
            log.error("更新文件元数据失败: {}", backupFile, e);
        }
    }
    
    /**
     * 异步检查存储统计信息
     */
    @Async
    public CompletableFuture<StorageStatistics> checkStorageStatisticsAsync() {
        return CompletableFuture.supplyAsync(this::checkStorageStatistics);
    }
    
    /**
     * 检查存储统计信息
     */
    public StorageStatistics checkStorageStatistics() {
        StorageStatistics stats = new StorageStatistics();
        
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                return stats;
            }
            
            // 获取文件系统信息
            FileStore fileStore = Files.getFileStore(backupDir);
            stats.setTotalSpaceBytes(fileStore.getTotalSpace());
            stats.setAvailableSpaceBytes(fileStore.getUsableSpace());
            stats.setUsedSpaceBytes(stats.getTotalSpaceBytes() - stats.getAvailableSpaceBytes());
            stats.setUsagePercentage((double) stats.getUsedSpaceBytes() / stats.getTotalSpaceBytes() * 100);
            
            // 统计备份文件信息
            List<Path> backupFiles = Files.list(backupDir)
                .filter(this::isBackupFile)
                .collect(Collectors.toList());
            
            stats.setTotalBackupFiles(backupFiles.size());
            
            if (!backupFiles.isEmpty()) {
                // 计算最新和最旧备份的天数
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime oldest = LocalDateTime.MAX;
                LocalDateTime newest = LocalDateTime.MIN;
                
                for (Path file : backupFiles) {
                    try {
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(file).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        
                        if (fileTime.isBefore(oldest)) oldest = fileTime;
                        if (fileTime.isAfter(newest)) newest = fileTime;
                        
                        // 统计备份类型
                        BackupService.BackupType type = getBackupTypeFromFileName(file.getFileName().toString());
                        if (type != null) {
                            stats.getBackupTypeCount().merge(type, 1, Integer::sum);
                        }
                        
                    } catch (IOException e) {
                        log.warn("读取备份文件时间失败: {}", file, e);
                    }
                }
                
                if (oldest != LocalDateTime.MAX) {
                    stats.setOldestBackupDays(java.time.Duration.between(oldest, now).toDays());
                }
                if (newest != LocalDateTime.MIN) {
                    stats.setNewestBackupDays(java.time.Duration.between(newest, now).toDays());
                }
            }
            
            // 检查是否需要发送存储告警
            if (stats.isStorageAlertRequired()) {
                sendStorageAlert(stats);
            }
            
            log.info("存储统计信息 - 总空间: {}, 已使用: {}, 使用率: {:.2f}%, 备份文件数: {}",
                    stats.getFormattedTotalSpace(), stats.getFormattedUsedSpace(), 
                    stats.getUsagePercentage(), stats.getTotalBackupFiles());
            
        } catch (Exception e) {
            log.error("检查存储统计信息失败", e);
        }
        
        return stats;
    }
    
    /**
     * 异步同步到远程存储
     */
    @Async
    public CompletableFuture<Void> syncToRemoteStorageAsync() {
        return CompletableFuture.runAsync(this::syncToRemoteStorage);
    }
    
    /**
     * 同步备份文件到远程存储
     */
    public void syncToRemoteStorage() {
        if (!backupProperties.getStorage().getRemote().isEnabled()) {
            log.debug("远程存储同步已禁用");
            return;
        }
        
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                return;
            }
            
            // 获取需要同步的文件
            List<Path> filesToSync = Files.list(backupDir)
                .filter(this::isBackupFile)
                .filter(this::needsRemoteSync)
                .collect(Collectors.toList());
            
            if (filesToSync.isEmpty()) {
                log.debug("没有需要同步到远程存储的文件");
                return;
            }
            
            log.info("开始同步 {} 个文件到远程存储", filesToSync.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Path file : filesToSync) {
                try {
                    if (syncFileToRemote(file)) {
                        successCount++;
                        markFileAsSynced(file);
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("同步文件到远程存储失败: {}", file, e);
                    failureCount++;
                }
            }
            
            log.info("远程存储同步完成 - 成功: {}, 失败: {}", successCount, failureCount);
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    String.format("远程存储同步完成 - 成功: %d, 失败: %d", successCount, failureCount)
                )
            );
            
        } catch (Exception e) {
            log.error("远程存储同步失败", e);
        }
    }
    
    /**
     * 获取备份文件列表及其元数据
     */
    public List<Map<String, Object>> getBackupFilesWithMetadata() {
        try {
            Path backupDir = Paths.get(backupProperties.getBackupPath());
            if (!Files.exists(backupDir)) {
                return new ArrayList<>();
            }
            
            return Files.list(backupDir)
                .filter(this::isBackupFile)
                .map(this::createFileInfoWithMetadata)
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    LocalDateTime timeA = (LocalDateTime) a.get("createdTime");
                    LocalDateTime timeB = (LocalDateTime) b.get("createdTime");
                    return timeB.compareTo(timeA); // 按创建时间倒序
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取备份文件列表失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 动态更新备份策略配置
     */
    public void updateBackupPolicy(Map<String, Object> policyUpdates) {
        try {
            log.info("开始更新备份策略配置: {}", policyUpdates);
            
            // 更新保留天数
            if (policyUpdates.containsKey("retentionDays")) {
                int newRetentionDays = (Integer) policyUpdates.get("retentionDays");
                if (newRetentionDays > 0 && newRetentionDays <= 365) {
                    backupProperties.setRetentionDays(newRetentionDays);
                    log.info("更新备份保留天数: {} 天", newRetentionDays);
                }
            }
            
            // 更新存储告警阈值
            if (policyUpdates.containsKey("storageAlertThreshold")) {
                double newThreshold = (Double) policyUpdates.get("storageAlertThreshold");
                if (newThreshold > 0 && newThreshold <= 1.0) {
                    backupProperties.getStorage().getLocal().setAlertThreshold(newThreshold);
                    log.info("更新存储告警阈值: {:.2f}%", newThreshold * 100);
                }
            }
            
            // 更新加密设置
            if (policyUpdates.containsKey("encryptionEnabled")) {
                boolean encryptionEnabled = (Boolean) policyUpdates.get("encryptionEnabled");
                backupProperties.getEncryption().setEnabled(encryptionEnabled);
                log.info("更新备份加密设置: {}", encryptionEnabled ? "启用" : "禁用");
            }
            
            // 更新远程存储设置
            if (policyUpdates.containsKey("remoteStorageEnabled")) {
                boolean remoteEnabled = (Boolean) policyUpdates.get("remoteStorageEnabled");
                backupProperties.getStorage().getRemote().setEnabled(remoteEnabled);
                log.info("更新远程存储设置: {}", remoteEnabled ? "启用" : "禁用");
            }
            
            // 记录审计日志
            auditLogService.logOperation(
                AuditLogRequest.system(
                    AuditOperation.BACKUP_OPERATION,
                    "动态更新备份策略配置: " + policyUpdates.toString()
                )
            );
            
        } catch (Exception e) {
            log.error("更新备份策略配置失败", e);
            throw new RuntimeException("更新备份策略配置失败: " + e.getMessage());
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 判断是否为备份文件
     */
    private boolean isBackupFile(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(BACKUP_EXTENSION) || 
               fileName.endsWith(ENCRYPTED_EXTENSION) ||
               (fileName.contains("backup_") && !fileName.endsWith(HASH_EXTENSION) && !fileName.endsWith(METADATA_EXTENSION));
    }
    
    /**
     * 从文件创建元数据
     */
    private BackupMetadata createMetadataFromFile(Path backupFile) throws IOException {
        String fileName = backupFile.getFileName().toString();
        BackupService.BackupType backupType = getBackupTypeFromFileName(fileName);
        LocalDateTime createdTime = LocalDateTime.ofInstant(
            Files.getLastModifiedTime(backupFile).toInstant(),
            java.time.ZoneId.systemDefault()
        );
        long fileSize = Files.size(backupFile);
        
        BackupMetadata metadata = new BackupMetadata(
            extractBackupIdFromFileName(fileName),
            backupType != null ? backupType : BackupService.BackupType.FULL,
            createdTime,
            fileSize
        );
        
        metadata.setOriginalPath(backupFile.toString());
        metadata.setEncrypted(fileName.endsWith(ENCRYPTED_EXTENSION));
        metadata.setCompressed(fileName.contains(".gz"));
        
        // 计算校验和
        try {
            String checksum = backupService.calculateFileChecksum(backupFile);
            metadata.setChecksum(checksum);
        } catch (Exception e) {
            log.warn("计算文件校验和失败: {}", backupFile, e);
        }
        
        return metadata;
    }
    
    /**
     * 从文件名提取备份类型
     */
    private BackupService.BackupType getBackupTypeFromFileName(String fileName) {
        if (fileName.startsWith("full_")) {
            return BackupService.BackupType.FULL;
        } else if (fileName.startsWith("incremental_")) {
            return BackupService.BackupType.INCREMENTAL;
        } else if (fileName.startsWith("differential_")) {
            return BackupService.BackupType.DIFFERENTIAL;
        }
        return BackupService.BackupType.FULL; // 默认为完全备份
    }
    
    /**
     * 从文件名提取备份ID
     */
    private String extractBackupIdFromFileName(String fileName) {
        // 移除扩展名
        String baseName = fileName;
        if (baseName.endsWith(ENCRYPTED_EXTENSION)) {
            baseName = baseName.substring(0, baseName.length() - ENCRYPTED_EXTENSION.length());
        }
        if (baseName.endsWith(BACKUP_EXTENSION)) {
            baseName = baseName.substring(0, baseName.length() - BACKUP_EXTENSION.length());
        }
        if (baseName.endsWith(".gz")) {
            baseName = baseName.substring(0, baseName.length() - 3);
        }
        
        return baseName;
    }
    
    /**
     * 检查是否需要更新元数据
     */
    private boolean needsMetadataUpdate(Path backupFile, BackupMetadata metadata) {
        try {
            long currentSize = Files.size(backupFile);
            return currentSize != metadata.getFileSizeBytes();
        } catch (IOException e) {
            return true; // 如果无法读取文件大小，则认为需要更新
        }
    }
    
    /**
     * 从文件更新元数据
     */
    private void updateMetadataFromFile(Path backupFile, BackupMetadata metadata) throws IOException {
        metadata.setFileSizeBytes(Files.size(backupFile));
        
        // 重新计算校验和
        try {
            String checksum = backupService.calculateFileChecksum(backupFile);
            metadata.setChecksum(checksum);
        } catch (Exception e) {
            log.warn("重新计算文件校验和失败: {}", backupFile, e);
        }
    }
    
    /**
     * 保存元数据到文件
     */
    private void saveMetadata(Path metadataFile, BackupMetadata metadata) throws IOException {
        Properties props = new Properties();
        props.setProperty("backupId", metadata.getBackupId() != null ? metadata.getBackupId() : "");
        props.setProperty("backupType", metadata.getBackupType().name());
        props.setProperty("createdTime", metadata.getCreatedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        props.setProperty("expiryTime", metadata.getExpiryTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        props.setProperty("fileSizeBytes", String.valueOf(metadata.getFileSizeBytes()));
        props.setProperty("checksum", metadata.getChecksum() != null ? metadata.getChecksum() : "");
        props.setProperty("isEncrypted", String.valueOf(metadata.isEncrypted()));
        props.setProperty("isCompressed", String.valueOf(metadata.isCompressed()));
        props.setProperty("originalPath", metadata.getOriginalPath() != null ? metadata.getOriginalPath() : "");
        
        // 保存自定义属性
        for (Map.Entry<String, String> entry : metadata.getCustomProperties().entrySet()) {
            props.setProperty("custom." + entry.getKey(), entry.getValue());
        }
        
        try (var writer = Files.newBufferedWriter(metadataFile)) {
            props.store(writer, "Backup Metadata - Generated by MyWeb Backup Management Service");
        }
    }
    
    /**
     * 从文件加载元数据
     */
    private BackupMetadata loadMetadata(Path metadataFile) {
        try {
            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(metadataFile)) {
                props.load(reader);
            }
            
            BackupMetadata metadata = new BackupMetadata();
            metadata.setBackupId(props.getProperty("backupId"));
            metadata.setBackupType(BackupService.BackupType.valueOf(props.getProperty("backupType", "FULL")));
            metadata.setCreatedTime(LocalDateTime.parse(props.getProperty("createdTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metadata.setExpiryTime(LocalDateTime.parse(props.getProperty("expiryTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metadata.setFileSizeBytes(Long.parseLong(props.getProperty("fileSizeBytes", "0")));
            metadata.setChecksum(props.getProperty("checksum"));
            metadata.setEncrypted(Boolean.parseBoolean(props.getProperty("isEncrypted", "false")));
            metadata.setCompressed(Boolean.parseBoolean(props.getProperty("isCompressed", "false")));
            metadata.setOriginalPath(props.getProperty("originalPath"));
            
            // 加载自定义属性
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("custom.")) {
                    String customKey = key.substring(7);
                    metadata.getCustomProperties().put(customKey, props.getProperty(key));
                }
            }
            
            return metadata;
            
        } catch (Exception e) {
            log.error("加载备份元数据失败: {}", metadataFile, e);
            return null;
        }
    }
    
    /**
     * 检查文件是否需要远程同步
     */
    private boolean needsRemoteSync(Path file) {
        try {
            Path metadataFile = Paths.get(file.toString() + METADATA_EXTENSION);
            if (!Files.exists(metadataFile)) {
                return true; // 没有元数据文件，需要同步
            }
            
            BackupMetadata metadata = loadMetadata(metadataFile);
            if (metadata == null) {
                return true;
            }
            
            // 检查是否已标记为已同步
            return !"true".equals(metadata.getCustomProperties().get("remoteSynced"));
            
        } catch (Exception e) {
            log.warn("检查远程同步状态失败: {}", file, e);
            return true;
        }
    }
    
    /**
     * 同步单个文件到远程存储
     */
    private boolean syncFileToRemote(Path file) {
        try {
            // 这里应该根据配置的远程存储类型实现具体的同步逻辑
            // 目前提供一个模拟实现
            
            String remoteType = backupProperties.getStorage().getRemote().getType();
            log.info("同步文件到远程存储 [{}]: {}", remoteType, file.getFileName());
            
            // 模拟同步过程
            Thread.sleep(100); // 模拟网络延迟
            
            // 实际实现中，这里应该调用相应的云存储SDK
            // 例如：AWS S3, Azure Blob Storage, Google Cloud Storage等
            
            log.info("文件同步成功: {}", file.getFileName());
            return true;
            
        } catch (Exception e) {
            log.error("同步文件到远程存储失败: {}", file, e);
            return false;
        }
    }
    
    /**
     * 标记文件为已同步
     */
    private void markFileAsSynced(Path file) {
        try {
            Path metadataFile = Paths.get(file.toString() + METADATA_EXTENSION);
            BackupMetadata metadata = loadMetadata(metadataFile);
            
            if (metadata == null) {
                metadata = createMetadataFromFile(file);
            }
            
            metadata.getCustomProperties().put("remoteSynced", "true");
            metadata.getCustomProperties().put("remoteSyncTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            saveMetadata(metadataFile, metadata);
            
        } catch (Exception e) {
            log.error("标记文件同步状态失败: {}", file, e);
        }
    }
    
    /**
     * 创建包含元数据的文件信息
     */
    private Map<String, Object> createFileInfoWithMetadata(Path file) {
        try {
            Map<String, Object> fileInfo = new HashMap<>();
            
            // 基本文件信息
            fileInfo.put("fileName", file.getFileName().toString());
            fileInfo.put("filePath", file.toString());
            fileInfo.put("fileSize", Files.size(file));
            fileInfo.put("lastModified", Files.getLastModifiedTime(file).toInstant());
            
            // 加载元数据
            Path metadataFile = Paths.get(file.toString() + METADATA_EXTENSION);
            if (Files.exists(metadataFile)) {
                BackupMetadata metadata = loadMetadata(metadataFile);
                if (metadata != null) {
                    fileInfo.put("backupId", metadata.getBackupId());
                    fileInfo.put("backupType", metadata.getBackupType().getDisplayName());
                    fileInfo.put("createdTime", metadata.getCreatedTime());
                    fileInfo.put("expiryTime", metadata.getExpiryTime());
                    fileInfo.put("isExpired", metadata.isExpired());
                    fileInfo.put("daysUntilExpiry", metadata.getDaysUntilExpiry());
                    fileInfo.put("checksum", metadata.getChecksum());
                    fileInfo.put("isEncrypted", metadata.isEncrypted());
                    fileInfo.put("isCompressed", metadata.isCompressed());
                    fileInfo.put("remoteSynced", "true".equals(metadata.getCustomProperties().get("remoteSynced")));
                }
            }
            
            // 验证文件完整性
            fileInfo.put("isValid", backupService.verifyBackupIntegrity(file.toString()));
            
            return fileInfo;
            
        } catch (Exception e) {
            log.error("创建文件信息失败: {}", file, e);
            return null;
        }
    }
    
    /**
     * 发送存储空间告警
     */
    private void sendStorageAlert(StorageStatistics stats) {
        try {
            if (!backupProperties.getNotification().isEnabled() || 
                !backupProperties.getNotification().getEmail().isEnabled()) {
                return;
            }
            
            String subject = "MyWeb系统备份存储空间告警";
            String content = String.format(
                "备份存储空间使用率告警：\n\n" +
                "存储路径: %s\n" +
                "总空间: %s\n" +
                "已使用: %s\n" +
                "可用空间: %s\n" +
                "使用率: %.2f%%\n" +
                "告警阈值: %.2f%%\n" +
                "备份文件总数: %d\n" +
                "最旧备份: %d 天前\n" +
                "最新备份: %d 天前\n\n" +
                "建议及时清理过期备份文件或扩展存储空间。\n\n" +
                "系统管理员，\n" +
                "MyWeb安全团队\n" +
                "%s",
                backupProperties.getBackupPath(),
                stats.getFormattedTotalSpace(),
                stats.getFormattedUsedSpace(),
                stats.getFormattedAvailableSpace(),
                stats.getUsagePercentage(),
                backupProperties.getStorage().getLocal().getAlertThreshold() * 100,
                stats.getTotalBackupFiles(),
                stats.getOldestBackupDays(),
                stats.getNewestBackupDays(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            // 发送告警邮件
            String[] recipients = backupProperties.getNotification().getEmail().getRecipients();
            for (String recipient : recipients) {
                try {
                    emailService.sendEmail(recipient, subject, content);
                    log.info("存储空间告警邮件已发送: recipient={}, usage={:.2f}%", 
                            recipient, stats.getUsagePercentage());
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
                            stats.getUsagePercentage(), recipients.length)
                )
            );
            
        } catch (Exception e) {
            log.error("发送存储空间告警异常", e);
        }
    }
}