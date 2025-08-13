package com.myweb.website_core.application.service.security.IPS.virusprotect;

import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.application.service.security.quarantine.QuarantineMetadata;
import com.myweb.website_core.application.service.security.quarantine.QuarantineResult;
import com.myweb.website_core.application.service.security.quarantine.QuarantineStatistics;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 病毒隔离服务
 * <p>
 * 负责处理被病毒扫描检测为恶意的文件：
 * - 安全隔离可疑文件
 * - 生成隔离报告和元数据
 * - 提供隔离文件的管理功能
 * - 支持隔离文件的恢复和删除
 * - 完整的审计日志记录
 * <p>
 * 符合GB/T 22239-2019二级等保要求的恶意代码防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirusQuarantineService {
    
    private final AuditMessageService auditLogService;
    
    @Value("${app.security.quarantine.path:${java.io.tmpdir}/quarantine}")
    private String quarantinePath;
    
    @Value("${app.security.quarantine.retention-days:30}")
    private int retentionDays;
    
    @Value("${app.security.quarantine.max-size:100MB}")
    private String maxQuarantineSize;
    
    private static final String QUARANTINE_EXTENSION = ".quarantine";
    private static final String METADATA_EXTENSION = ".metadata";
    
    /**
     * 隔离可疑文件
     * 
     * @param file 要隔离的文件
     * @param scanResult 病毒扫描结果
     * @return 隔离结果
     */
    @Async
    public CompletableFuture<QuarantineResult> quarantineFile(MultipartFile file, VirusScanResult scanResult) {
        long startTime = System.currentTimeMillis();
        String originalFilename = file.getOriginalFilename();
        
        log.info("开始隔离可疑文件: filename={}, virus={}, user={}", 
                originalFilename, scanResult.getVirusName(), scanResult.getUsername());
        
        try {
            // 确保隔离目录存在
            ensureQuarantineDirectoryExists();
            
            // 生成隔离文件名
            String quarantineFilename = generateQuarantineFilename(originalFilename, scanResult);
            Path quarantineFilePath = Paths.get(quarantinePath, quarantineFilename + QUARANTINE_EXTENSION);
            Path metadataFilePath = Paths.get(quarantinePath, quarantineFilename + METADATA_EXTENSION);
            
            // 复制文件到隔离区
            Files.copy(file.getInputStream(), quarantineFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成文件哈希
            String fileHash = calculateFileHash(file.getBytes());
            
            // 创建隔离元数据
            QuarantineMetadata metadata = createQuarantineMetadata(file, scanResult, fileHash, quarantineFilename);
            
            // 保存元数据
            saveQuarantineMetadata(metadataFilePath, metadata);
            
            // 创建隔离结果
            long duration = System.currentTimeMillis() - startTime;
            QuarantineResult result = QuarantineResult.builder()
                .success(true)
                .quarantineId(quarantineFilename)
                .quarantinePath(quarantineFilePath.toString())
                .metadataPath(metadataFilePath.toString())
                .fileHash(fileHash)
                .quarantineTime(LocalDateTime.now())
                .processingTimeMs(duration)
                .message("文件已成功隔离")
                .build();
            
            // 记录审计日志
            recordQuarantineAuditLog(scanResult, result, "FILE_QUARANTINED");
            
            log.info("文件隔离完成: filename={}, quarantineId={}, duration={}ms", 
                    originalFilename, quarantineFilename, duration);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            QuarantineResult result = QuarantineResult.builder()
                .success(false)
                .errorMessage("隔离文件失败: " + e.getMessage())
                .processingTimeMs(duration)
                .build();
            
            recordQuarantineAuditLog(scanResult, result, "QUARANTINE_FAILED");
            
            log.error("文件隔离失败: filename={}, error={}", originalFilename, e.getMessage(), e);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            QuarantineResult result = QuarantineResult.builder()
                .success(false)
                .errorMessage("隔离处理异常: " + e.getMessage())
                .processingTimeMs(duration)
                .build();
            
            recordQuarantineAuditLog(scanResult, result, "QUARANTINE_ERROR");
            
            log.error("文件隔离异常: filename={}, error={}", originalFilename, e.getMessage(), e);
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * 删除隔离文件
     * 
     * @param quarantineId 隔离ID
     * @param userId 操作用户ID
     * @param username 操作用户名
     * @return 删除结果
     */
    @Async
    public CompletableFuture<Boolean> deleteQuarantinedFile(String quarantineId, Long userId, String username) {
        log.info("开始删除隔离文件: quarantineId={}, user={}", quarantineId, username);
        
        try {
            Path quarantineFilePath = Paths.get(quarantinePath, quarantineId + QUARANTINE_EXTENSION);
            Path metadataFilePath = Paths.get(quarantinePath, quarantineId + METADATA_EXTENSION);
            
            boolean fileDeleted = false;
            boolean metadataDeleted = false;
            
            // 删除隔离文件
            if (Files.exists(quarantineFilePath)) {
                Files.delete(quarantineFilePath);
                fileDeleted = true;
                log.debug("隔离文件已删除: {}", quarantineFilePath);
            }
            
            // 删除元数据文件
            if (Files.exists(metadataFilePath)) {
                Files.delete(metadataFilePath);
                metadataDeleted = true;
                log.debug("隔离元数据已删除: {}", metadataFilePath);
            }
            
            boolean success = fileDeleted || metadataDeleted;
            
            // 记录审计日志
            recordDeletionAuditLog(quarantineId, userId, username, success);
            
            log.info("隔离文件删除完成: quarantineId={}, success={}", quarantineId, success);
            return CompletableFuture.completedFuture(success);
            
        } catch (IOException e) {
            recordDeletionAuditLog(quarantineId, userId, username, false);
            log.error("删除隔离文件失败: quarantineId={}, error={}", quarantineId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 清理过期的隔离文件
     * 
     * @return 清理的文件数量
     */
    @Async
    public CompletableFuture<Integer> cleanupExpiredQuarantineFiles() {
        log.info("开始清理过期隔离文件，保留期限: {} 天", retentionDays);
        
        try {
            Path quarantineDir = Paths.get(quarantinePath);
            if (!Files.exists(quarantineDir)) {
                log.debug("隔离目录不存在，跳过清理");
                return CompletableFuture.completedFuture(0);
            }
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            int cleanedCount = 0;
            
            // 遍历隔离目录
            Files.list(quarantineDir)
                .filter(path -> path.toString().endsWith(QUARANTINE_EXTENSION))
                .forEach(quarantineFile -> {
                    try {
                        // 检查文件修改时间
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(quarantineFile).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        
                        if (fileTime.isBefore(cutoffTime)) {
                            // 删除隔离文件和对应的元数据文件
                            String filename = quarantineFile.getFileName().toString();
                            String baseName = filename.substring(0, filename.lastIndexOf(QUARANTINE_EXTENSION));
                            Path metadataFile = quarantineFile.getParent().resolve(baseName + METADATA_EXTENSION);
                            
                            Files.deleteIfExists(quarantineFile);
                            Files.deleteIfExists(metadataFile);
                            
                            log.debug("清理过期隔离文件: {}", quarantineFile);
                        }
                    } catch (IOException e) {
                        log.warn("清理隔离文件失败: {}, error={}", quarantineFile, e.getMessage());
                    }
                });
            
            log.info("隔离文件清理完成，清理数量: {}", cleanedCount);
            return CompletableFuture.completedFuture(cleanedCount);
            
        } catch (IOException e) {
            log.error("清理过期隔离文件异常: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(0);
        }
    }
    
    /**
     * 获取隔离统计信息
     * 
     * @return 统计信息
     */
    public QuarantineStatistics getQuarantineStatistics() {
        try {
            Path quarantineDir = Paths.get(quarantinePath);
            if (!Files.exists(quarantineDir)) {
                return QuarantineStatistics.empty();
            }
            
            long totalFiles = Files.list(quarantineDir)
                .filter(path -> path.toString().endsWith(QUARANTINE_EXTENSION))
                .count();
            
            long totalSize = Files.list(quarantineDir)
                .filter(path -> path.toString().endsWith(QUARANTINE_EXTENSION))
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
            
            return QuarantineStatistics.builder()
                .totalFiles(totalFiles)
                .totalSizeBytes(totalSize)
                .quarantinePath(quarantinePath)
                .retentionDays(retentionDays)
                .lastUpdated(LocalDateTime.now())
                .build();
            
        } catch (IOException e) {
            log.error("获取隔离统计信息失败: {}", e.getMessage(), e);
            return QuarantineStatistics.empty();
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 确保隔离目录存在
     */
    private void ensureQuarantineDirectoryExists() throws IOException {
        Path quarantineDir = Paths.get(quarantinePath);
        if (!Files.exists(quarantineDir)) {
            Files.createDirectories(quarantineDir);
            log.info("创建隔离目录: {}", quarantineDir);
        }
    }
    
    /**
     * 生成隔离文件名
     */
    private String generateQuarantineFilename(String originalFilename, VirusScanResult scanResult) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String userInfo = scanResult.getUsername() != null ? scanResult.getUsername() : "unknown";
        String virusInfo = scanResult.getVirusName() != null ? 
            scanResult.getVirusName().replaceAll("[^a-zA-Z0-9]", "_") : "unknown";
        
        return String.format("quarantine_%s_%s_%s_%s", 
                           timestamp, userInfo, virusInfo, 
                           originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.]", "_") : "unknown");
    }
    
    /**
     * 创建隔离元数据
     */
    private QuarantineMetadata createQuarantineMetadata(MultipartFile file, VirusScanResult scanResult, 
                                                       String fileHash, String quarantineFilename) {
        return QuarantineMetadata.builder()
            .quarantineId(quarantineFilename)
            .originalFilename(file.getOriginalFilename())
            .fileSize(file.getSize())
            .contentType(file.getContentType())
            .fileHash(fileHash)
            .virusName(scanResult.getVirusName())
            .threatLevel(scanResult.getThreatLevel())
            .scanEngine(scanResult.getEngineName())
            .scanTime(scanResult.getScanEndTime())
            .quarantineTime(LocalDateTime.now())
            .userId(scanResult.getUserId())
            .username(scanResult.getUsername())
            .scanDetails(scanResult.getDetails())
            .recommendedAction(scanResult.getRecommendedAction())
            .build();
    }
    
    /**
     * 保存隔离元数据
     */
    private void saveQuarantineMetadata(Path metadataFilePath, QuarantineMetadata metadata) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# 隔离文件元数据\n");
        sb.append("quarantine_id=").append(metadata.getQuarantineId()).append("\n");
        sb.append("original_filename=").append(metadata.getOriginalFilename()).append("\n");
        sb.append("file_size=").append(metadata.getFileSize()).append("\n");
        sb.append("content_type=").append(metadata.getContentType()).append("\n");
        sb.append("file_hash=").append(metadata.getFileHash()).append("\n");
        sb.append("virus_name=").append(metadata.getVirusName()).append("\n");
        sb.append("threat_level=").append(metadata.getThreatLevel()).append("\n");
        sb.append("scan_engine=").append(metadata.getScanEngine()).append("\n");
        sb.append("scan_time=").append(metadata.getScanTime()).append("\n");
        sb.append("quarantine_time=").append(metadata.getQuarantineTime()).append("\n");
        sb.append("user_id=").append(metadata.getUserId()).append("\n");
        sb.append("username=").append(metadata.getUsername()).append("\n");
        sb.append("scan_details=").append(metadata.getScanDetails()).append("\n");
        sb.append("recommended_action=").append(metadata.getRecommendedAction()).append("\n");
        
        Files.write(metadataFilePath, sb.toString().getBytes());
    }
    
    /**
     * 计算文件哈希
     */
    private String calculateFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256算法不可用，使用简单哈希: {}", e.getMessage());
            return String.valueOf(fileBytes.hashCode());
        }
    }
    
    /**
     * 记录隔离审计日志
     */
    private void recordQuarantineAuditLog(VirusScanResult scanResult, QuarantineResult quarantineResult, String eventType) {
        try {
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .userId(scanResult.getUserId())
                .username(scanResult.getUsername())
                .operation(AuditOperation.FILE_QUARANTINE)
                .resourceType("FILE")
                .result(quarantineResult.isSuccess() ? "SUCCESS" : "FAILURE")
                .errorMessage(quarantineResult.getErrorMessage())
                .executionTime(quarantineResult.getProcessingTimeMs())
                .timestamp(LocalDateTime.now())
                .description(String.format("文件隔离: %s - 病毒: %s", 
                           scanResult.getFilename(), scanResult.getVirusName()))
                .riskLevel(5) // 文件隔离为高风险操作
                .build();
            
            // 异步记录审计日志
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录隔离审计日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录删除审计日志
     */
    private void recordDeletionAuditLog(String quarantineId, Long userId, String username, boolean success) {
        try {
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .userId(userId)
                .username(username)
                .operation(AuditOperation.POST_DELETE)
                .resourceType("QUARANTINE_FILE")
                .result(success ? "SUCCESS" : "FAILURE")
                .timestamp(LocalDateTime.now())
                .description("删除隔离文件: " + quarantineId)
                .riskLevel(4) // 删除隔离文件为中高风险操作
                .build();
            
            // 异步记录审计日志
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录删除隔离文件审计日志失败: {}", e.getMessage(), e);
        }
    }
}