package com.myweb.website_core.application.service.security.audit;

import com.myweb.website_core.application.service.security.SecurityAlertService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.LogStorageStatistics;
import com.myweb.website_core.domain.business.dto.StorageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

/**
 * 日志存储管理服务
 * 
 * 提供日志存储空间监控、告警、备份归档和完整性校验功能，包括：
 * - 磁盘使用情况监控
 * - 存储空间不足告警
 * - 日志备份和归档机制
 * - 日志压缩存储
 * - 日志完整性校验
 * 
 * 符合GB/T 22239-2019安全审计要求 3.4, 3.7
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Slf4j
@Service
public class LogStorageManagementService {
    
    private final AuditLogService auditLogService;
    private final SecurityAlertService securityAlertService;
    
    // 日志存储路径配置
    @Value("${app.audit.storage.path:/var/log/myweb}")
    private String logStoragePath;
    
    // 备份存储路径配置
    @Value("${app.audit.backup.path:/var/backup/myweb/logs}")
    private String backupStoragePath;
    
    // 存储空间告警阈值（百分比）
    @Value("${app.audit.storage.warning-threshold:50}")
    private int warningThreshold;
    
    // 存储空间严重告警阈值（百分比）
    @Value("${app.audit.storage.critical-threshold:70}")
    private int criticalThreshold;
    
    // 日志文件保留天数
    @Value("${app.audit.storage.retention-days:90}")
    private int retentionDays;
    
    // 备份文件保留天数
    @Value("${app.audit.backup.retention-days:365}")
    private int backupRetentionDays;
    
    // 压缩阈值（MB）
    @Value("${app.audit.storage.compression-threshold:100}")
    private long compressionThreshold;
    
    // 是否启用自动压缩
    @Value("${app.audit.storage.auto-compression:true}")
    private boolean autoCompressionEnabled;
    
    // 是否启用完整性校验
    @Value("${app.audit.storage.integrity-check:true}")
    private boolean integrityCheckEnabled;
    
    @Autowired
    public LogStorageManagementService(AuditLogService auditLogService, 
                                     SecurityAlertService securityAlertService) {
        this.auditLogService = auditLogService;
        this.securityAlertService = securityAlertService;
    }
    
    // ==================== 存储空间监控 ====================
    
    /**
     * 监控日志存储空间使用情况
     * 每小时执行一次检查
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void monitorStorageUsage() {
        try {
            log.info("开始监控日志存储空间使用情况");
            
            StorageInfo storageInfo = getStorageInfo(logStoragePath);
            
            // 记录存储使用情况
            log.info("日志存储空间使用情况: 总空间={}MB, 已用={}MB, 可用={}MB, 使用率={}%",
                    storageInfo.getTotalSpaceMB(),
                    storageInfo.getUsedSpaceMB(),
                    storageInfo.getFreeSpaceMB(),
                    storageInfo.getUsagePercentage());
            
            // 检查是否需要告警
            checkStorageAlert(storageInfo);
            
            // 记录监控审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SYSTEM_MONITOR,
                "SYSTEM",
                String.format("存储空间监控: 使用率%.1f%%, 可用空间%dMB", 
                    storageInfo.getUsagePercentage(), storageInfo.getFreeSpaceMB())
            );
            
        } catch (Exception e) {
            log.error("监控日志存储空间失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.SYSTEM_MONITOR,
                "SYSTEM",
                "存储空间监控失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 获取存储空间信息
     * 
     * @param path 存储路径
     * @return 存储空间信息
     */
    public StorageInfo getStorageInfo(String path) {
        try {
            Path storagePath = Paths.get(path);
            
            // 确保目录存在
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            // 获取文件系统信息
            FileStore fileStore = Files.getFileStore(storagePath);
            
            long totalSpace = fileStore.getTotalSpace();
            long freeSpace = fileStore.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;
            
            return StorageInfo.builder()
                    .path(path)
                    .totalSpace(totalSpace)
                    .freeSpace(freeSpace)
                    .usedSpace(usedSpace)
                    .build();
            
        } catch (IOException e) {
            log.error("获取存储空间信息失败: path={}", path, e);
            throw new RuntimeException("获取存储空间信息失败", e);
        }
    }
    
    /**
     * 检查存储空间告警
     * 
     * @param storageInfo 存储空间信息
     */
    private void checkStorageAlert(StorageInfo storageInfo) {
        double usagePercentage = storageInfo.getUsagePercentage();
        
        if (usagePercentage >= criticalThreshold) {
            // 严重告警
            sendStorageAlert("CRITICAL", storageInfo, 
                "日志存储空间严重不足，使用率已达到" + String.format("%.1f%%", usagePercentage));
        } else if (usagePercentage >= warningThreshold) {
            // 警告告警
            sendStorageAlert("WARNING", storageInfo,
                "日志存储空间不足，使用率已达到" + String.format("%.1f%%", usagePercentage));
        }
    }
    
    /**
     * 发送存储空间告警
     * 
     * @param level 告警级别
     * @param storageInfo 存储信息
     * @param message 告警消息
     */
    @Async
    public void sendStorageAlert(String level, StorageInfo storageInfo, String message) {
        try {
            // 记录告警到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SYSTEM_MONITOR,
                "SYSTEM",
                "存储空间告警: " + level + " - " + message
            );
            
            log.warn("发送存储空间告警: level={}, message={}", level, message);
            
        } catch (Exception e) {
            log.error("发送存储空间告警失败", e);
        }
    }
    
    // ==================== 日志备份和归档 ====================
    
    /**
     * 执行日志备份和归档
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void performLogBackupAndArchive() {
        try {
            log.info("开始执行日志备份和归档");
            
            // 创建备份目录
            Path backupPath = Paths.get(backupStoragePath);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }
            
            // 备份当前日志文件
            CompletableFuture<Void> backupFuture = backupCurrentLogs();
            
            // 归档旧日志文件
            CompletableFuture<Void> archiveFuture = archiveOldLogs();
            
            // 等待备份和归档完成
            CompletableFuture.allOf(backupFuture, archiveFuture).get();
            
            // 清理过期备份
            cleanupExpiredBackups();
            
            log.info("日志备份和归档完成");
            
            // 记录备份审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.BACKUP_OPERATION,
                "SYSTEM",
                "日志备份和归档操作完成"
            );
            
        } catch (Exception e) {
            log.error("执行日志备份和归档失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.BACKUP_OPERATION,
                "SYSTEM",
                "日志备份和归档操作失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 备份当前日志文件
     * 
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> backupCurrentLogs() {
        try {
            Path logPath = Paths.get(logStoragePath);
            Path backupPath = Paths.get(backupStoragePath);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // 遍历日志文件进行备份
            Files.walk(logPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .forEach(logFile -> {
                        try {
                            String fileName = logFile.getFileName().toString();
                            String backupFileName = timestamp + "_" + fileName + ".backup";
                            Path backupFile = backupPath.resolve(backupFileName);
                            
                            // 复制文件到备份目录
                            Files.copy(logFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                            
                            // 如果启用压缩，压缩备份文件
                            if (autoCompressionEnabled) {
                                compressFile(backupFile);
                            }
                            
                            // 如果启用完整性校验，计算文件哈希
                            if (integrityCheckEnabled) {
                                String hash = calculateFileHash(backupFile);
                                saveFileHash(backupFile, hash);
                            }
                            
                            log.debug("备份日志文件: {} -> {}", logFile, backupFile);
                            
                        } catch (Exception e) {
                            log.error("备份日志文件失败: {}", logFile, e);
                        }
                    });
            
            log.info("当前日志文件备份完成");
            
        } catch (Exception e) {
            log.error("备份当前日志文件失败", e);
            throw new RuntimeException("备份当前日志文件失败", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 归档旧日志文件
     * 
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> archiveOldLogs() {
        try {
            Path logPath = Paths.get(logStoragePath);
            Path archivePath = logPath.resolve("archive");
            
            if (!Files.exists(archivePath)) {
                Files.createDirectories(archivePath);
            }
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            // 查找需要归档的旧日志文件
            Files.walk(logPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path)
                                    .toInstant()
                                    .isBefore(cutoffTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(oldLogFile -> {
                        try {
                            String fileName = oldLogFile.getFileName().toString();
                            String archiveFileName = "archived_" + fileName;
                            Path archiveFile = archivePath.resolve(archiveFileName);
                            
                            // 移动文件到归档目录
                            Files.move(oldLogFile, archiveFile, StandardCopyOption.REPLACE_EXISTING);
                            
                            // 压缩归档文件
                            if (autoCompressionEnabled) {
                                compressFile(archiveFile);
                            }
                            
                            log.debug("归档日志文件: {} -> {}", oldLogFile, archiveFile);
                            
                        } catch (Exception e) {
                            log.error("归档日志文件失败: {}", oldLogFile, e);
                        }
                    });
            
            log.info("旧日志文件归档完成");
            
        } catch (Exception e) {
            log.error("归档旧日志文件失败", e);
            throw new RuntimeException("归档旧日志文件失败", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 清理过期备份文件
     */
    private void cleanupExpiredBackups() {
        try {
            Path backupPath = Paths.get(backupStoragePath);
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(backupRetentionDays);
            
            int deletedCount = 0;
            
            // 删除过期备份文件
            Files.walk(backupPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path)
                                    .toInstant()
                                    .isBefore(cutoffTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(expiredFile -> {
                        try {
                            Files.delete(expiredFile);
                            log.debug("删除过期备份文件: {}", expiredFile);
                        } catch (IOException e) {
                            log.error("删除过期备份文件失败: {}", expiredFile, e);
                        }
                    });
            
            if (deletedCount > 0) {
                log.info("清理过期备份文件完成，删除{}个文件", deletedCount);
            }
            
        } catch (Exception e) {
            log.error("清理过期备份文件失败", e);
        }
    }
    
    // ==================== 日志压缩存储 ====================
    
    /**
     * 压缩日志文件
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void compressLogFiles() {
        if (!autoCompressionEnabled) {
            return;
        }
        
        try {
            log.info("开始压缩日志文件");
            
            Path logPath = Paths.get(logStoragePath);
            int compressedCount = 0;
            
            // 查找需要压缩的文件
            Files.walk(logPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toString().endsWith(".gz"))
                    .filter(path -> {
                        try {
                            long fileSizeMB = Files.size(path) / (1024 * 1024);
                            return fileSizeMB >= compressionThreshold;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(file -> {
                        try {
                            compressFile(file);
                            log.debug("压缩文件: {}", file);
                        } catch (Exception e) {
                            log.error("压缩文件失败: {}", file, e);
                        }
                    });
            
            log.info("日志文件压缩完成，压缩了{}个文件", compressedCount);
            
            // 记录压缩审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SYSTEM_MAINTENANCE,
                "SYSTEM",
                "日志文件压缩完成，压缩了" + compressedCount + "个文件"
            );
            
        } catch (Exception e) {
            log.error("压缩日志文件失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.SYSTEM_MAINTENANCE,
                "SYSTEM",
                "日志文件压缩失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 压缩单个文件
     * 
     * @param file 要压缩的文件
     * @throws IOException 压缩失败
     */
    private void compressFile(Path file) throws IOException {
        Path compressedFile = Paths.get(file.toString() + ".gz");
        
        try (FileInputStream fis = new FileInputStream(file.toFile());
             FileOutputStream fos = new FileOutputStream(compressedFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, length);
            }
        }
        
        // 删除原文件
        Files.delete(file);
        
        log.debug("文件压缩完成: {} -> {}", file, compressedFile);
    }
    
    // ==================== 日志完整性校验 ====================
    
    /**
     * 执行日志完整性校验
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void performIntegrityCheck() {
        if (!integrityCheckEnabled) {
            return;
        }
        
        try {
            log.info("开始执行日志完整性校验");
            
            int checkedCount = 0;
            int corruptedCount = 0;
            
            // 检查日志文件完整性
            Path logPath = Paths.get(logStoragePath);
            Files.walk(logPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            if (verifyFileIntegrity(file)) {
                                log.debug("文件完整性校验通过: {}", file);
                            } else {
                                log.warn("文件完整性校验失败: {}", file);
                                // 发送完整性告警
                                sendIntegrityAlert(file);
                            }
                        } catch (Exception e) {
                            log.error("校验文件完整性失败: {}", file, e);
                        }
                    });
            
            // 检查备份文件完整性
            Path backupPath = Paths.get(backupStoragePath);
            if (Files.exists(backupPath)) {
                Files.walk(backupPath)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                if (verifyFileIntegrity(file)) {
                                    log.debug("备份文件完整性校验通过: {}", file);
                                } else {
                                    log.warn("备份文件完整性校验失败: {}", file);
                                    sendIntegrityAlert(file);
                                }
                            } catch (Exception e) {
                                log.error("校验备份文件完整性失败: {}", file, e);
                            }
                        });
            }
            
            log.info("日志完整性校验完成，检查了{}个文件，发现{}个损坏文件", checkedCount, corruptedCount);
            
            // 记录校验审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                String.format("日志完整性校验完成，检查了%d个文件，发现%d个损坏文件", checkedCount, corruptedCount)
            );
            
        } catch (Exception e) {
            log.error("执行日志完整性校验失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                "日志完整性校验失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 计算文件哈希值
     * 
     * @param file 文件路径
     * @return SHA-256哈希值
     * @throws IOException 读取文件失败
     * @throws NoSuchAlgorithmException 算法不支持
     */
    private String calculateFileHash(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) != -1) {
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
     * 保存文件哈希值
     * 
     * @param file 文件路径
     * @param hash 哈希值
     * @throws IOException 写入失败
     */
    private void saveFileHash(Path file, String hash) throws IOException {
        Path hashFile = Paths.get(file.toString() + ".sha256");
        Files.write(hashFile, hash.getBytes());
    }
    
    /**
     * 验证文件完整性
     * 
     * @param file 文件路径
     * @return 是否完整
     */
    private boolean verifyFileIntegrity(Path file) {
        try {
            Path hashFile = Paths.get(file.toString() + ".sha256");
            
            // 如果没有哈希文件，计算并保存哈希值
            if (!Files.exists(hashFile)) {
                String hash = calculateFileHash(file);
                saveFileHash(file, hash);
                return true;
            }
            
            // 读取保存的哈希值
            String savedHash = Files.readString(hashFile).trim();
            
            // 计算当前文件哈希值
            String currentHash = calculateFileHash(file);
            
            // 比较哈希值
            return savedHash.equals(currentHash);
            
        } catch (Exception e) {
            log.error("验证文件完整性失败: {}", file, e);
            return false;
        }
    }
    
    /**
     * 发送完整性告警
     * 
     * @param file 损坏的文件
     */
    @Async
    public void sendIntegrityAlert(Path file) {
        try {
            // 记录完整性告警到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                "日志文件完整性告警: 检测到文件可能已被篡改或损坏 - " + file.toString()
            );
            
            log.warn("发送日志完整性告警: file={}", file);
            
        } catch (Exception e) {
            log.error("发送完整性告警失败", e);
        }
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 手动触发存储空间检查
     * 
     * @return 存储空间信息
     */
    public StorageInfo checkStorageSpace() {
        StorageInfo storageInfo = getStorageInfo(logStoragePath);
        checkStorageAlert(storageInfo);
        return storageInfo;
    }
    
    /**
     * 手动触发日志备份
     * 
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> triggerLogBackup() {
        return backupCurrentLogs();
    }
    
    /**
     * 手动触发完整性检查
     * 
     * @param filePath 文件路径（可选，为空则检查所有文件）
     * @return 检查结果
     */
    public boolean triggerIntegrityCheck(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            Path file = Paths.get(filePath);
            return verifyFileIntegrity(file);
        } else {
            performIntegrityCheck();
            return true;
        }
    }
    
    /**
     * 获取日志存储统计信息
     * 
     * @return 存储统计信息
     */
    public LogStorageStatistics getStorageStatistics() {
        try {
            StorageInfo logStorage = getStorageInfo(logStoragePath);
            StorageInfo backupStorage = getStorageInfo(backupStoragePath);
            
            // 统计文件数量
            long logFileCount = Files.walk(Paths.get(logStoragePath))
                    .filter(Files::isRegularFile)
                    .count();
            
            long backupFileCount = Files.exists(Paths.get(backupStoragePath)) ?
                    Files.walk(Paths.get(backupStoragePath))
                            .filter(Files::isRegularFile)
                            .count() : 0;
            
            return LogStorageStatistics.builder()
                    .logStorage(logStorage)
                    .backupStorage(backupStorage)
                    .logFileCount(logFileCount)
                    .backupFileCount(backupFileCount)
                    .retentionDays(retentionDays)
                    .backupRetentionDays(backupRetentionDays)
                    .compressionEnabled(autoCompressionEnabled)
                    .integrityCheckEnabled(integrityCheckEnabled)
                    .build();
            
        } catch (Exception e) {
            log.error("获取日志存储统计信息失败", e);
            throw new RuntimeException("获取日志存储统计信息失败", e);
        }
    }
}