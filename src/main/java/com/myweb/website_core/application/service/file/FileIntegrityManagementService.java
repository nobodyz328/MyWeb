package com.myweb.website_core.application.service.file;

import com.myweb.website_core.application.service.file.ImageService;
import com.myweb.website_core.application.service.file.FileUploadService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.PermissionUtils;
import com.myweb.website_core.domain.business.entity.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 文件完整性管理服务
 * <p>
 * 提供文件完整性相关的高级管理功能：
 * - 定期文件完整性检查
 * - 文件完整性报告生成
 * - 损坏文件处理流程
 * - 文件篡改检测和告警
 * - 批量文件完整性验证
 * <p>
 * 符合需求 6.5, 6.6 的要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileIntegrityManagementService {
    
    private final ImageService imageService;
    private final FileUploadService fileUploadService;
    private final DataIntegrityService dataIntegrityService;
    private final AuditLogService auditLogService;
    
    @Value("${app.file-integrity.periodic-check.enabled:true}")
    private boolean periodicCheckEnabled;
    
    @Value("${app.file-integrity.periodic-check.batch-size:50}")
    private int batchSize;
    
    @Value("${app.file-integrity.corrupted-file.quarantine-enabled:true}")
    private boolean quarantineEnabled;
    
    @Value("${app.file-integrity.corrupted-file.quarantine-path:${java.io.tmpdir}/myweb/quarantine}")
    private String quarantinePath;
    
    @Value("${app.file-integrity.report.retention-days:30}")
    private int reportRetentionDays;
    
    // 内存中的完整性检查统计
    private final Map<String, Object> integrityStatistics = new HashMap<>();
    
    /**
     * 定期文件完整性检查任务
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void performScheduledIntegrityCheck() {
        if (!periodicCheckEnabled) {
            log.debug("定期文件完整性检查已禁用");
            return;
        }
        
        log.info("开始执行定期文件完整性检查");
        performFullIntegrityCheckAsync();
    }
    
    /**
     * 异步执行完整文件完整性检查
     */
    @Async
    public CompletableFuture<FileIntegrityReport> performFullIntegrityCheckAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                log.info("开始全量文件完整性检查");
                
                // 获取所有需要检查的图片
                List<Image> allImages = getAllImagesForIntegrityCheck();
                log.info("需要检查的文件总数: {}", allImages.size());
                
                if (allImages.isEmpty()) {
                    log.info("没有需要检查的文件");
                    return createEmptyReport();
                }
                
                // 分批处理文件
                List<FileIntegrityCheckResult> allResults = new ArrayList<>();
                int totalBatches = (int) Math.ceil((double) allImages.size() / batchSize);
                
                for (int i = 0; i < totalBatches; i++) {
                    int startIndex = i * batchSize;
                    int endIndex = Math.min(startIndex + batchSize, allImages.size());
                    List<Image> batch = allImages.subList(startIndex, endIndex);
                    
                    log.debug("处理批次 {}/{}, 文件数: {}", i + 1, totalBatches, batch.size());
                    
                    List<FileIntegrityCheckResult> batchResults = processBatch(batch);
                    allResults.addAll(batchResults);
                    
                    // 批次间短暂休息，避免系统负载过高
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                // 生成完整性报告
                FileIntegrityReport report = generateIntegrityReport(allResults, startTime);
                
                // 处理损坏的文件
                List<FileIntegrityCheckResult> corruptedFiles = allResults.stream()
                    .filter(result -> !result.isValid())
                    .collect(Collectors.toList());
                
                if (!corruptedFiles.isEmpty()) {
                    handleCorruptedFiles(corruptedFiles);
                }
                
                // 更新统计信息
                updateIntegrityStatistics(report);
                
                // 记录审计日志
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "SYSTEM",
                    String.format("定期文件完整性检查完成: 总计=%d, 有效=%d, 损坏=%d, 耗时=%dms",
                                 report.getTotalFiles(), report.getValidFiles(), 
                                 report.getCorruptedFiles(), report.getExecutionTimeMs())
                );
                
                log.info("全量文件完整性检查完成: 总计={}, 有效={}, 损坏={}, 耗时={}ms",
                        report.getTotalFiles(), report.getValidFiles(), 
                        report.getCorruptedFiles(), report.getExecutionTimeMs());
                
                return report;
                
            } catch (Exception e) {
                log.error("全量文件完整性检查失败", e);
                
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "SYSTEM",
                    "定期文件完整性检查失败: " + e.getMessage()
                );
                
                throw new RuntimeException("文件完整性检查失败", e);
            }
        });
    }
    
    /**
     * 获取所有需要进行完整性检查的图片
     */
    private List<Image> getAllImagesForIntegrityCheck() {
        try {
            // 获取所有有文件哈希的图片
            return imageService.getAllImages().stream()
                .filter(image -> image.getFileHash() != null && !image.getFileHash().trim().isEmpty())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取需要完整性检查的文件列表失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 处理一批文件的完整性检查
     */
    private List<FileIntegrityCheckResult> processBatch(List<Image> batch) {
        List<FileIntegrityCheckResult> results = new ArrayList<>();
        
        for (Image image : batch) {
            try {
                FileIntegrityCheckResult result = checkSingleFileIntegrity(image);
                results.add(result);
            } catch (Exception e) {
                log.error("检查文件完整性失败: imageId={}, error={}", image.getId(), e.getMessage());
                
                FileIntegrityCheckResult errorResult = FileIntegrityCheckResult.builder()
                    .imageId(image.getId())
                    .filename(image.getOriginalFilename())
                    .filePath(image.getFilePath())
                    .valid(false)
                    .errorMessage("检查异常: " + e.getMessage())
                    .checkTime(LocalDateTime.now())
                    .build();
                results.add(errorResult);
            }
        }
        
        return results;
    }
    
    /**
     * 检查单个文件的完整性
     */
    private FileIntegrityCheckResult checkSingleFileIntegrity(Image image) {
        long startTime = System.currentTimeMillis();
        
        try {
            Path filePath = Paths.get(image.getFilePath());
            boolean fileExists = Files.exists(filePath);
            
            if (!fileExists) {
                return FileIntegrityCheckResult.builder()
                    .imageId(image.getId())
                    .filename(image.getOriginalFilename())
                    .filePath(image.getFilePath())
                    .valid(false)
                    .fileExists(false)
                    .errorMessage("文件不存在")
                    .checkTime(LocalDateTime.now())
                    .checkDurationMs(System.currentTimeMillis() - startTime)
                    .build();
            }
            
            // 验证文件完整性
            boolean integrityValid = fileUploadService.verifyFileIntegrity(image.getId());
            
            // 获取文件信息
            long fileSize = Files.size(filePath);
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(filePath).toInstant(),
                java.time.ZoneId.systemDefault()
            );
            
            return FileIntegrityCheckResult.builder()
                .imageId(image.getId())
                .filename(image.getOriginalFilename())
                .filePath(image.getFilePath())
                .valid(integrityValid)
                .fileExists(true)
                .fileSize(fileSize)
                .storedHash(image.getFileHash())
                .lastModified(lastModified)
                .checkTime(LocalDateTime.now())
                .checkDurationMs(System.currentTimeMillis() - startTime)
                .errorMessage(integrityValid ? null : "文件哈希值不匹配，可能已被篡改")
                .build();
                
        } catch (Exception e) {
            return FileIntegrityCheckResult.builder()
                .imageId(image.getId())
                .filename(image.getOriginalFilename())
                .filePath(image.getFilePath())
                .valid(false)
                .fileExists(Files.exists(Paths.get(image.getFilePath())))
                .errorMessage("检查异常: " + e.getMessage())
                .checkTime(LocalDateTime.now())
                .checkDurationMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }
    
    /**
     * 生成文件完整性报告
     */
    private FileIntegrityReport generateIntegrityReport(List<FileIntegrityCheckResult> results, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        int totalFiles = results.size();
        int validFiles = (int) results.stream().filter(FileIntegrityCheckResult::isValid).count();
        int corruptedFiles = totalFiles - validFiles;
        int missingFiles = (int) results.stream().filter(r -> !r.isFileExists()).count();
        
        // 按错误类型分组
        Map<String, Long> errorTypes = results.stream()
            .filter(r -> !r.isValid())
            .collect(Collectors.groupingBy(
                r -> r.getErrorMessage() != null ? r.getErrorMessage() : "未知错误",
                Collectors.counting()
            ));
        
        // 计算平均检查时间
        double avgCheckTime = results.stream()
            .mapToLong(FileIntegrityCheckResult::getCheckDurationMs)
            .average()
            .orElse(0.0);
        
        return FileIntegrityReport.builder()
            .reportId(UUID.randomUUID().toString())
            .checkTime(LocalDateTime.now())
            .totalFiles(totalFiles)
            .validFiles(validFiles)
            .corruptedFiles(corruptedFiles)
            .missingFiles(missingFiles)
            .executionTimeMs(executionTime)
            .averageCheckTimeMs((long) avgCheckTime)
            .errorTypes(errorTypes)
            .checkResults(results)
            .reportType("SCHEDULED_FULL_CHECK")
            .build();
    }
    
    /**
     * 创建空报告（当没有文件需要检查时）
     */
    private FileIntegrityReport createEmptyReport() {
        return FileIntegrityReport.builder()
            .reportId(UUID.randomUUID().toString())
            .checkTime(LocalDateTime.now())
            .totalFiles(0)
            .validFiles(0)
            .corruptedFiles(0)
            .missingFiles(0)
            .executionTimeMs(0L)
            .averageCheckTimeMs(0L)
            .errorTypes(new HashMap<>())
            .checkResults(new ArrayList<>())
            .reportType("EMPTY_CHECK")
            .build();
    }
    
    /**
     * 处理损坏的文件
     */
    private void handleCorruptedFiles(List<FileIntegrityCheckResult> corruptedFiles) {
        log.warn("发现 {} 个损坏的文件，开始处理", corruptedFiles.size());
        
        for (FileIntegrityCheckResult result : corruptedFiles) {
            try {
                handleSingleCorruptedFile(result);
            } catch (Exception e) {
                log.error("处理损坏文件失败: imageId={}, error={}", 
                         result.getImageId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * 处理单个损坏的文件
     */
    private void handleSingleCorruptedFile(FileIntegrityCheckResult result) {
        log.warn("处理损坏文件: imageId={}, filename={}, error={}", 
                result.getImageId(), result.getFilename(), result.getErrorMessage());
        
        try {
            // 1. 记录安全事件
            auditLogService.logSecurityEvent(
                AuditOperation.SECURITY_ALERT,
                "SYSTEM",
                String.format("检测到文件完整性问题: ID=%d, 文件=%s, 错误=%s",
                             result.getImageId(), result.getFilename(), result.getErrorMessage())
            );
            
            // 2. 如果启用了隔离功能，将损坏的文件移动到隔离区
            if (quarantineEnabled && result.isFileExists()) {
                quarantineCorruptedFile(result);
            }
            
            // 3. 尝试重新计算文件哈希（可能是哈希值错误而非文件损坏）
            if (result.isFileExists()) {
                boolean recalculated = fileUploadService.recalculateFileHash(result.getImageId());
                if (recalculated) {
                    log.info("重新计算文件哈希成功: imageId={}", result.getImageId());
                    
                    // 重新验证完整性
                    boolean newIntegrityCheck = fileUploadService.verifyFileIntegrity(result.getImageId());
                    if (newIntegrityCheck) {
                        log.info("重新计算哈希后文件完整性验证通过: imageId={}", result.getImageId());
                        return; // 文件已修复，无需进一步处理
                    }
                }
            }
            
            // 4. 发送告警通知（这里可以集成邮件或其他通知系统）
            sendCorruptedFileAlert(result);
            
        } catch (Exception e) {
            log.error("处理损坏文件异常: imageId={}, error={}", result.getImageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 将损坏的文件移动到隔离区
     */
    private void quarantineCorruptedFile(FileIntegrityCheckResult result) {
        try {
            Path originalFile = Paths.get(result.getFilePath());
            if (!Files.exists(originalFile)) {
                return;
            }
            
            // 创建隔离目录
            Path quarantineDir = Paths.get(quarantinePath);
            Files.createDirectories(quarantineDir);
            
            // 生成隔离文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String quarantineFileName = String.format("%d_%s_%s", 
                result.getImageId(), timestamp, result.getFilename());
            Path quarantineFile = quarantineDir.resolve(quarantineFileName);
            
            // 移动文件到隔离区
            Files.move(originalFile, quarantineFile);
            
            log.info("损坏文件已移动到隔离区: {} -> {}", originalFile, quarantineFile);
            
            // 记录隔离操作
            auditLogService.logSecurityEvent(
                AuditOperation.FILE_QUARANTINE,
                "SYSTEM",
                String.format("损坏文件已隔离: ID=%d, 原路径=%s, 隔离路径=%s",
                             result.getImageId(), result.getFilePath(), quarantineFile.toString())
            );
            
        } catch (Exception e) {
            log.error("隔离损坏文件失败: imageId={}, error={}", result.getImageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送损坏文件告警
     */
    private void sendCorruptedFileAlert(FileIntegrityCheckResult result) {
        try {
            String alertMessage = String.format(
                "文件完整性告警:\n" +
                "- 文件ID: %d\n" +
                "- 文件名: %s\n" +
                "- 文件路径: %s\n" +
                "- 错误信息: %s\n" +
                "- 检查时间: %s\n" +
                "请立即检查文件安全性！",
                result.getImageId(),
                result.getFilename(),
                result.getFilePath(),
                result.getErrorMessage(),
                result.getCheckTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            log.warn("发送文件完整性告警: {}", alertMessage);
            
            // 记录告警到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SECURITY_ALERT,
                "SYSTEM",
                "文件完整性告警: " + alertMessage
            );
            
            // TODO: 实际实现时可以集成邮件、短信或其他告警服务
            
        } catch (Exception e) {
            log.error("发送文件完整性告警失败", e);
        }
    }
    
    /**
     * 更新完整性统计信息
     */
    private void updateIntegrityStatistics(FileIntegrityReport report) {
        synchronized (integrityStatistics) {
            integrityStatistics.put("lastCheckTime", report.getCheckTime());
            integrityStatistics.put("totalFiles", report.getTotalFiles());
            integrityStatistics.put("validFiles", report.getValidFiles());
            integrityStatistics.put("corruptedFiles", report.getCorruptedFiles());
            integrityStatistics.put("missingFiles", report.getMissingFiles());
            integrityStatistics.put("lastExecutionTimeMs", report.getExecutionTimeMs());
            integrityStatistics.put("averageCheckTimeMs", report.getAverageCheckTimeMs());
        }
    }
    
    /**
     * 获取文件完整性统计信息
     */
    public Map<String, Object> getIntegrityStatistics() {
        synchronized (integrityStatistics) {
            return new HashMap<>(integrityStatistics);
        }
    }
    
    /**
     * 手动触发文件完整性检查
     */
    public CompletableFuture<FileIntegrityReport> triggerManualIntegrityCheck() {
        log.info("手动触发文件完整性检查");
        
        auditLogService.logSecurityEvent(
            AuditOperation.INTEGRITY_CHECK,
            PermissionUtils.getCurrentUsername() != null ? PermissionUtils.getCurrentUsername() : "SYSTEM",
            "手动触发文件完整性检查"
        );
        
        return performFullIntegrityCheckAsync();
    }
    
    /**
     * 获取最近的完整性检查报告
     */
    public FileIntegrityReport getLatestIntegrityReport() {
        // 这里可以从数据库或文件系统中获取最近的报告
        // 为了简化实现，这里返回当前统计信息构建的报告
        Map<String, Object> stats = getIntegrityStatistics();
        
        return FileIntegrityReport.builder()
            .reportId("LATEST_STATS")
            .checkTime((LocalDateTime) stats.get("lastCheckTime"))
            .totalFiles((Integer) stats.getOrDefault("totalFiles", 0))
            .validFiles((Integer) stats.getOrDefault("validFiles", 0))
            .corruptedFiles((Integer) stats.getOrDefault("corruptedFiles", 0))
            .missingFiles((Integer) stats.getOrDefault("missingFiles", 0))
            .executionTimeMs((Long) stats.getOrDefault("lastExecutionTimeMs", 0L))
            .averageCheckTimeMs((Long) stats.getOrDefault("averageCheckTimeMs", 0L))
            .errorTypes(new HashMap<>())
            .checkResults(new ArrayList<>())
            .reportType("STATISTICS_SUMMARY")
            .build();
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 文件完整性检查结果
     */
    public static class FileIntegrityCheckResult {
        private Long imageId;
        private String filename;
        private String filePath;
        private boolean valid;
        private boolean fileExists;
        private Long fileSize;
        private String storedHash;
        private String currentHash;
        private LocalDateTime lastModified;
        private LocalDateTime checkTime;
        private Long checkDurationMs;
        private String errorMessage;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private FileIntegrityCheckResult result = new FileIntegrityCheckResult();
            
            public Builder imageId(Long imageId) {
                result.imageId = imageId;
                return this;
            }
            
            public Builder filename(String filename) {
                result.filename = filename;
                return this;
            }
            
            public Builder filePath(String filePath) {
                result.filePath = filePath;
                return this;
            }
            
            public Builder valid(boolean valid) {
                result.valid = valid;
                return this;
            }
            
            public Builder fileExists(boolean fileExists) {
                result.fileExists = fileExists;
                return this;
            }
            
            public Builder fileSize(Long fileSize) {
                result.fileSize = fileSize;
                return this;
            }
            
            public Builder storedHash(String storedHash) {
                result.storedHash = storedHash;
                return this;
            }
            
            public Builder currentHash(String currentHash) {
                result.currentHash = currentHash;
                return this;
            }
            
            public Builder lastModified(LocalDateTime lastModified) {
                result.lastModified = lastModified;
                return this;
            }
            
            public Builder checkTime(LocalDateTime checkTime) {
                result.checkTime = checkTime;
                return this;
            }
            
            public Builder checkDurationMs(Long checkDurationMs) {
                result.checkDurationMs = checkDurationMs;
                return this;
            }
            
            public Builder errorMessage(String errorMessage) {
                result.errorMessage = errorMessage;
                return this;
            }
            
            public FileIntegrityCheckResult build() {
                return result;
            }
        }
        
        // Getters
        public Long getImageId() { return imageId; }
        public String getFilename() { return filename; }
        public String getFilePath() { return filePath; }
        public boolean isValid() { return valid; }
        public boolean isFileExists() { return fileExists; }
        public Long getFileSize() { return fileSize; }
        public String getStoredHash() { return storedHash; }
        public String getCurrentHash() { return currentHash; }
        public LocalDateTime getLastModified() { return lastModified; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public Long getCheckDurationMs() { return checkDurationMs; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 文件完整性报告
     */
    public static class FileIntegrityReport {
        private String reportId;
        private LocalDateTime checkTime;
        private int totalFiles;
        private int validFiles;
        private int corruptedFiles;
        private int missingFiles;
        private Long executionTimeMs;
        private Long averageCheckTimeMs;
        private Map<String, Long> errorTypes;
        private List<FileIntegrityCheckResult> checkResults;
        private String reportType;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private FileIntegrityReport report = new FileIntegrityReport();
            
            public Builder reportId(String reportId) {
                report.reportId = reportId;
                return this;
            }
            
            public Builder checkTime(LocalDateTime checkTime) {
                report.checkTime = checkTime;
                return this;
            }
            
            public Builder totalFiles(int totalFiles) {
                report.totalFiles = totalFiles;
                return this;
            }
            
            public Builder validFiles(int validFiles) {
                report.validFiles = validFiles;
                return this;
            }
            
            public Builder corruptedFiles(int corruptedFiles) {
                report.corruptedFiles = corruptedFiles;
                return this;
            }
            
            public Builder missingFiles(int missingFiles) {
                report.missingFiles = missingFiles;
                return this;
            }
            
            public Builder executionTimeMs(Long executionTimeMs) {
                report.executionTimeMs = executionTimeMs;
                return this;
            }
            
            public Builder averageCheckTimeMs(Long averageCheckTimeMs) {
                report.averageCheckTimeMs = averageCheckTimeMs;
                return this;
            }
            
            public Builder errorTypes(Map<String, Long> errorTypes) {
                report.errorTypes = errorTypes;
                return this;
            }
            
            public Builder checkResults(List<FileIntegrityCheckResult> checkResults) {
                report.checkResults = checkResults;
                return this;
            }
            
            public Builder reportType(String reportType) {
                report.reportType = reportType;
                return this;
            }
            
            public FileIntegrityReport build() {
                return report;
            }
        }
        
        // Getters
        public String getReportId() { return reportId; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public int getTotalFiles() { return totalFiles; }
        public int getValidFiles() { return validFiles; }
        public int getCorruptedFiles() { return corruptedFiles; }
        public int getMissingFiles() { return missingFiles; }
        public Long getExecutionTimeMs() { return executionTimeMs; }
        public Long getAverageCheckTimeMs() { return averageCheckTimeMs; }
        public Map<String, Long> getErrorTypes() { return errorTypes; }
        public List<FileIntegrityCheckResult> getCheckResults() { return checkResults; }
        public String getReportType() { return reportType; }
    }
}