package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.file.ImageService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.audit.SecurityAlertService;
import com.myweb.website_core.application.service.security.audit.SecurityEventService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.common.enums.SecurityLevel;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.PermissionUtils;
import com.myweb.website_core.domain.business.entity.Image;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文件安全监控服务
 * <p>
 * 提供文件上传安全监控功能：
 * - 文件上传安全统计
 * - 恶意文件告警机制
 * - 文件安全事件记录
 * - 文件安全可视化监控
 * - 文件安全定期报告
 * <p>
 * 符合需求 6.6, 6.7 的要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSecurityMonitoringService {
    
    private final ImageService imageService;
    private final AuditLogService auditLogService;
    private final SecurityEventService securityEventService;
    private final SecurityAlertService securityAlertService;
    
    @Value("${app.file-security.monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    @Value("${app.file-security.monitoring.max-file-size:10485760}") // 10MB
    private long maxFileSize;
    
    @Value("${app.file-security.monitoring.allowed-extensions:jpg,jpeg,png,gif,bmp,webp}")
    private String allowedExtensions;
    
    @Value("${app.file-security.monitoring.scan-enabled:true}")
    private boolean virusScanEnabled;
    
    @Value("${app.file-security.monitoring.quarantine-path:${java.io.tmpdir}/myweb/quarantine}")
    private String quarantinePath;
    
    // 内存中的文件安全统计
    private final Map<String, Object> fileSecurityStatistics = new ConcurrentHashMap<>();
    
    // 可疑文件类型模式
    private final Set<String> suspiciousExtensions = Set.of(
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", 
        "php", "asp", "jsp", "sh", "py", "pl", "rb"
    );
    
    // 恶意文件签名（简化版本）
    private final Map<String, String> maliciousSignatures = Map.of(
        "4D5A", "PE Executable",
        "7F454C46", "ELF Executable", 
        "504B0304", "ZIP Archive (potential)",
        "52617221", "RAR Archive (potential)"
    );
    
    /**
     * 监控文件上传安全性
     * 
     * @param imageId 图片ID
     * @param originalFilename 原始文件名
     * @param fileSize 文件大小
     * @param contentType 内容类型
     * @param filePath 文件路径
     * @param username 上传用户
     * @param sourceIp 来源IP
     * @return 安全检查结果
     */
    @Async
    public CompletableFuture<FileSecurityCheckResult> monitorFileUpload(
            Long imageId, String originalFilename, long fileSize, 
            String contentType, String filePath, String username, String sourceIp) {
        
        return CompletableFuture.supplyAsync(() -> {
            if (!monitoringEnabled) {
                return FileSecurityCheckResult.safe("文件安全监控已禁用");
            }
            
            long startTime = System.currentTimeMillis();
            
            try {
                log.debug("开始文件安全监控: imageId={}, filename={}, size={}, user={}", 
                         imageId, originalFilename, fileSize, username);
                
                FileSecurityCheckResult result = performSecurityCheck(
                    imageId, originalFilename, fileSize, contentType, filePath, username, sourceIp);
                
                // 记录监控结果
                recordSecurityCheckResult(result, username, sourceIp, startTime);
                
                // 更新统计信息
                updateFileSecurityStatistics(result);
                
                // 如果检测到威胁，发送告警
                if (result.isThreatDetected()) {
                    handleSecurityThreat(result, username, sourceIp);
                }
                
                log.debug("文件安全监控完成: imageId={}, result={}, duration={}ms", 
                         imageId, result.getSecurityLevel(), System.currentTimeMillis() - startTime);
                
                return result;
                
            } catch (Exception e) {
                log.error("文件安全监控异常: imageId={}, error={}", imageId, e.getMessage(), e);
                
                FileSecurityCheckResult errorResult = FileSecurityCheckResult.error(
                    "文件安全监控异常: " + e.getMessage());
                
                recordSecurityCheckResult(errorResult, username, sourceIp, startTime);
                return errorResult;
            }
        });
    }
    
    /**
     * 执行文件安全检查
     */
    private FileSecurityCheckResult performSecurityCheck(
            Long imageId, String originalFilename, long fileSize, 
            String contentType, String filePath, String username, String sourceIp) {
        
        FileSecurityCheckResult.Builder resultBuilder = FileSecurityCheckResult.builder()
            .imageId(imageId)
            .filename(originalFilename)
            .fileSize(fileSize)
            .contentType(contentType)
            .filePath(filePath)
            .username(username)
            .sourceIp(sourceIp)
            .checkTime(LocalDateTime.now());
        
        List<String> threats = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int riskScore = 0;
        
        // 1. 文件大小检查
        if (fileSize > maxFileSize) {
            threats.add("文件大小超过限制: " + fileSize + " > " + maxFileSize);
            riskScore += 30;
        }
        
        // 2. 文件扩展名检查
        String extension = getFileExtension(originalFilename);
        if (suspiciousExtensions.contains(extension.toLowerCase())) {
            threats.add("可疑文件扩展名: " + extension);
            riskScore += 50;
        } else if (!isAllowedExtension(extension)) {
            warnings.add("不在允许的扩展名列表中: " + extension);
            riskScore += 20;
        }
        
        // 3. 内容类型检查
        if (!isValidContentType(contentType, extension)) {
            threats.add("内容类型与文件扩展名不匹配: " + contentType + " vs " + extension);
            riskScore += 40;
        }
        
        // 4. 文件内容检查
        try {
            FileContentCheckResult contentCheck = checkFileContent(filePath);
            if (contentCheck.isMalicious()) {
                threats.addAll(contentCheck.getThreats());
                riskScore += contentCheck.getRiskScore();
            }
            if (!contentCheck.getWarnings().isEmpty()) {
                warnings.addAll(contentCheck.getWarnings());
                riskScore += 10;
            }
        } catch (Exception e) {
            warnings.add("文件内容检查失败: " + e.getMessage());
            riskScore += 15;
        }
        
        // 5. 用户行为分析
        UserBehaviorAnalysis behaviorAnalysis = analyzeUserBehavior(username, sourceIp);
        if (behaviorAnalysis.isSuspicious()) {
            warnings.add("用户行为异常: " + behaviorAnalysis.getDescription());
            riskScore += behaviorAnalysis.getRiskScore();
        }
        
        // 确定安全级别
        SecurityLevel securityLevel;
        if (riskScore >= 70) {
            securityLevel = SecurityLevel.HIGH_RISK;
        } else if (riskScore >= 40) {
            securityLevel = SecurityLevel.MEDIUM_RISK;
        } else if (riskScore >= 20) {
            securityLevel = SecurityLevel.LOW_RISK;
        } else {
            securityLevel = SecurityLevel.SAFE;
        }
        
        return resultBuilder
            .threats(threats)
            .warnings(warnings)
            .riskScore(riskScore)
            .securityLevel(securityLevel)
            .threatDetected(!threats.isEmpty())
            .build();
    }
    
    /**
     * 检查文件内容
     */
    private FileContentCheckResult checkFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return FileContentCheckResult.safe("文件不存在");
        }
        
        List<String> threats = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int riskScore = 0;
        
        // 读取文件头部字节
        byte[] header = Files.readAllBytes(path);
        if (header.length > 16) {
            header = Arrays.copyOf(header, 16);
        }
        
        String hexHeader = bytesToHex(header);
        
        // 检查恶意文件签名
        for (Map.Entry<String, String> signature : maliciousSignatures.entrySet()) {
            if (hexHeader.startsWith(signature.getKey())) {
                threats.add("检测到恶意文件签名: " + signature.getValue());
                riskScore += 60;
            }
        }
        
        // 检查文件大小异常
        long fileSize = Files.size(path);
        if (fileSize == 0) {
            warnings.add("文件大小为0");
            riskScore += 10;
        } else if (fileSize < 100) {
            warnings.add("文件大小异常小: " + fileSize + " bytes");
            riskScore += 5;
        }
        
        // 简单的内容扫描（检查可疑字符串）
        if (fileSize < 1024 * 1024) { // 只扫描小于1MB的文件
            String content = new String(Files.readAllBytes(path));
            if (containsSuspiciousContent(content)) {
                threats.add("文件内容包含可疑脚本或代码");
                riskScore += 40;
            }
        }
        
        return FileContentCheckResult.builder()
            .threats(threats)
            .warnings(warnings)
            .riskScore(riskScore)
            .malicious(!threats.isEmpty())
            .build();
    }
    
    /**
     * 分析用户行为
     */
    private UserBehaviorAnalysis analyzeUserBehavior(String username, String sourceIp) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime windowStart = now.minusHours(1);
            
            // 统计用户在过去1小时内的上传次数
            // 注意：这里需要ImageService提供相应的查询方法
            // List<Image> recentUploads = imageService.findByUsernameAndTimeRange(username, windowStart, now);
            
            int uploadCount = 0; // 暂时设为0，实际应该查询数据库
            boolean suspicious = false;
            String description = "";
            int riskScore = 0;
            
            // 检查上传频率异常
            if (uploadCount > 20) {
                suspicious = true;
                description = "1小时内上传文件过多: " + uploadCount + " 个";
                riskScore = 30;
            } else if (uploadCount > 10) {
                description = "1小时内上传文件较多: " + uploadCount + " 个";
                riskScore = 15;
            }
            
            // 检查IP地址异常
            if (sourceIp != null) {
                // 检查是否为内网IP
                if (!isInternalIp(sourceIp)) {
                    // 检查该IP的上传历史
                    // List<Image> ipUploads = imageService.findBySourceIpAndTimeRange(sourceIp, windowStart, now);
                    int ipUploadCount = 0; // 暂时设为0
                    if (ipUploadCount > 15) {
                        suspicious = true;
                        description += (description.isEmpty() ? "" : "; ") + 
                                      "IP地址上传频率异常: " + ipUploadCount + " 个";
                        riskScore += 25;
                    }
                }
            }
            
            return UserBehaviorAnalysis.builder()
                .username(username)
                .sourceIp(sourceIp)
                .uploadCount(uploadCount)
                .suspicious(suspicious)
                .description(description)
                .riskScore(riskScore)
                .build();
                
        } catch (Exception e) {
            log.warn("用户行为分析失败: username={}, error={}", username, e.getMessage());
            return UserBehaviorAnalysis.safe(username, sourceIp);
        }
    }
    
    /**
     * 记录安全检查结果
     */
    private void recordSecurityCheckResult(FileSecurityCheckResult result, String username, 
                                         String sourceIp, long startTime) {
        try {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录审计日志
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_SECURITY_CHECK", 
                username != null ? username : "SYSTEM", 
                "FILE:" + result.getImageId(),
                result.isThreatDetected() ? "THREAT_DETECTED" : "SAFE",
                String.format("文件: %s, 安全级别: %s, 风险评分: %d, 威胁: %d, 警告: %d",
                             result.getFilename(), result.getSecurityLevel(), result.getRiskScore(),
                             result.getThreats().size(), result.getWarnings().size()),
                executionTime
            );
            
            if (result.isThreatDetected()) {
                log.warn(logMessage);
            } else {
                log.info(logMessage);
            }
            
            // 记录到审计日志服务
            auditLogService.logSecurityEvent(
                AuditOperation.FILE_SECURITY_CHECK,
                username != null ? username : "SYSTEM",
                String.format("文件安全检查: %s, 结果: %s, 风险评分: %d",
                             result.getFilename(), result.getSecurityLevel(), result.getRiskScore())
            );
            
        } catch (Exception e) {
            log.error("记录文件安全检查结果失败", e);
        }
    }
    
    /**
     * 处理安全威胁
     */
    private void handleSecurityThreat(FileSecurityCheckResult result, String username, String sourceIp) {
        try {
            log.warn("检测到文件安全威胁: imageId={}, filename={}, threats={}", 
                    result.getImageId(), result.getFilename(), result.getThreats());
            
            // 如果是高风险文件，立即隔离
            if (result.getSecurityLevel() == SecurityLevel.HIGH_RISK) {
                quarantineMaliciousFile(result);
            }
            
            // 发送告警
            if (result.getRiskScore() >= 50) {
                sendSecurityAlert(result, username, sourceIp);
            }
            
        } catch (Exception e) {
            log.error("处理文件安全威胁失败: imageId={}, error={}", 
                     result.getImageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 隔离恶意文件
     */
    private void quarantineMaliciousFile(FileSecurityCheckResult result) {
        try {
            Path originalFile = Paths.get(result.getFilePath());
            if (!Files.exists(originalFile)) {
                log.warn("要隔离的文件不存在: {}", result.getFilePath());
                return;
            }
            
            // 创建隔离目录
            Path quarantineDir = Paths.get(quarantinePath);
            Files.createDirectories(quarantineDir);
            
            // 生成隔离文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String quarantineFileName = String.format("MALICIOUS_%d_%s_%s", 
                result.getImageId(), timestamp, result.getFilename());
            Path quarantineFile = quarantineDir.resolve(quarantineFileName);
            
            // 移动文件到隔离区
            Files.move(originalFile, quarantineFile);
            
            log.warn("恶意文件已隔离: {} -> {}", originalFile, quarantineFile);
            
            // 记录隔离操作
            auditLogService.logSecurityEvent(
                AuditOperation.FILE_QUARANTINE,
                result.getUsername() != null ? result.getUsername() : "SYSTEM",
                String.format("恶意文件已隔离: ID=%d, 原路径=%s, 隔离路径=%s, 威胁=%s",
                             result.getImageId(), result.getFilePath(), quarantineFile.toString(),
                             String.join("; ", result.getThreats()))
            );
            
        } catch (Exception e) {
            log.error("隔离恶意文件失败: imageId={}, error={}", result.getImageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送安全告警
     */
    private void sendSecurityAlert(FileSecurityCheckResult result, String username, String sourceIp) {
        try {
            String alertMessage = String.format(
                "文件安全告警:\n" +
                "- 文件ID: %d\n" +
                "- 文件名: %s\n" +
                "- 上传用户: %s\n" +
                "- 来源IP: %s\n" +
                "- 安全级别: %s\n" +
                "- 风险评分: %d\n" +
                "- 检测威胁: %s\n" +
                "- 检测时间: %s\n" +
                "请立即检查文件安全性！",
                result.getImageId(),
                result.getFilename(),
                username != null ? username : "未知",
                sourceIp != null ? sourceIp : "未知",
                result.getSecurityLevel(),
                result.getRiskScore(),
                String.join("; ", result.getThreats()),
                result.getCheckTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            log.warn("发送文件安全告警: {}", alertMessage);
            
            // 记录告警到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SECURITY_ALERT,
                username != null ? username : "SYSTEM",
                "文件安全告警: " + alertMessage
            );
            
        } catch (Exception e) {
            log.error("发送文件安全告警失败", e);
        }
    }
    
    /**
     * 更新文件安全统计信息
     */
    private void updateFileSecurityStatistics(FileSecurityCheckResult result) {
        try {
            synchronized (fileSecurityStatistics) {
                // 更新总体统计
                fileSecurityStatistics.put("lastCheckTime", result.getCheckTime());
                fileSecurityStatistics.put("totalChecks", 
                    ((Number) fileSecurityStatistics.getOrDefault("totalChecks", 0)).longValue() + 1);
                
                // 按安全级别统计
                String levelKey = result.getSecurityLevel().name().toLowerCase() + "Count";
                fileSecurityStatistics.put(levelKey,
                    ((Number) fileSecurityStatistics.getOrDefault(levelKey, 0)).longValue() + 1);
                
                // 威胁统计
                if (result.isThreatDetected()) {
                    fileSecurityStatistics.put("threatsDetected",
                        ((Number) fileSecurityStatistics.getOrDefault("threatsDetected", 0)).longValue() + 1);
                }
                
                // 按文件类型统计
                String extension = getFileExtension(result.getFilename());
                String extKey = "ext_" + extension.toLowerCase();
                fileSecurityStatistics.put(extKey,
                    ((Number) fileSecurityStatistics.getOrDefault(extKey, 0)).longValue() + 1);
            }
        } catch (Exception e) {
            log.error("更新文件安全统计失败", e);
        }
    }
    
    /**
     * 定期生成文件安全报告
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailySecurityReport() {
        try {
            log.info("开始生成每日文件安全报告");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);
            
            FileSecurityReport report = generateSecurityReport(yesterday, now);
            
            // 保存报告（这里可以保存到数据库或文件）
            saveSecurityReport(report);
            
            // 如果有高风险事件，发送报告
            if (report.getHighRiskCount() > 0 || report.getThreatCount() > 0) {
                sendSecurityReport(report);
            }
            
            log.info("每日文件安全报告生成完成: 总检查={}, 威胁={}, 高风险={}", 
                    report.getTotalChecks(), report.getThreatCount(), report.getHighRiskCount());
            
        } catch (Exception e) {
            log.error("生成每日文件安全报告失败", e);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    private boolean isAllowedExtension(String extension) {
        if (allowedExtensions == null || allowedExtensions.trim().isEmpty()) {
            return true;
        }
        return Arrays.stream(allowedExtensions.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .anyMatch(ext -> ext.equals(extension.toLowerCase()));
    }
    
    private boolean isValidContentType(String contentType, String extension) {
        if (contentType == null || extension == null) {
            return false;
        }
        
        // 简单的内容类型验证
        String lowerContentType = contentType.toLowerCase();
        String lowerExtension = extension.toLowerCase();
        
        return switch (lowerExtension) {
            case "jpg", "jpeg" -> lowerContentType.contains("jpeg");
            case "png" -> lowerContentType.contains("png");
            case "gif" -> lowerContentType.contains("gif");
            case "bmp" -> lowerContentType.contains("bmp");
            case "webp" -> lowerContentType.contains("webp");
            default -> true; // 其他类型暂时允许
        };
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
    
    private boolean containsSuspiciousContent(String content) {
        String lowerContent = content.toLowerCase();
        String[] suspiciousPatterns = {
            "<script", "javascript:", "eval(", "document.cookie",
            "<?php", "<%", "exec(", "system(", "shell_exec(",
            "base64_decode", "file_get_contents", "fopen("
        };
        
        return Arrays.stream(suspiciousPatterns)
            .anyMatch(lowerContent::contains);
    }
    
    private boolean isInternalIp(String ip) {
        if (ip == null) return false;
        return ip.startsWith("192.168.") || ip.startsWith("10.") || 
               ip.startsWith("172.") || ip.equals("127.0.0.1") || 
               ip.equals("localhost");
    }
    
    private Long getUserIdByUsername(String username) {
        // 这里应该通过UserService获取用户ID
        // 为了简化，返回null
        return null;
    }
    
    /**
     * 获取文件安全统计信息
     */
    public Map<String, Object> getFileSecurityStatistics() {
        synchronized (fileSecurityStatistics) {
            return new HashMap<>(fileSecurityStatistics);
        }
    }
    
    /**
     * 生成文件安全报告
     */
    private FileSecurityReport generateSecurityReport(LocalDateTime startTime, LocalDateTime endTime) {
        // 这里应该从数据库查询统计数据
        // 为了简化，使用内存统计
        Map<String, Object> stats = getFileSecurityStatistics();
        
        return FileSecurityReport.builder()
            .reportId(UUID.randomUUID().toString())
            .startTime(startTime)
            .endTime(endTime)
            .totalChecks(((Number) stats.getOrDefault("totalChecks", 0)).longValue())
            .safeCount(((Number) stats.getOrDefault("safeCount", 0)).longValue())
            .lowRiskCount(((Number) stats.getOrDefault("low_riskCount", 0)).longValue())
            .mediumRiskCount(((Number) stats.getOrDefault("medium_riskCount", 0)).longValue())
            .highRiskCount(((Number) stats.getOrDefault("high_riskCount", 0)).longValue())
            .threatCount(((Number) stats.getOrDefault("threatsDetected", 0)).longValue())
            .generatedTime(LocalDateTime.now())
            .build();
    }
    
    /**
     * 保存安全报告
     */
    private void saveSecurityReport(FileSecurityReport report) {
        // 这里可以保存到数据库或文件系统
        log.info("保存文件安全报告: reportId={}, period={} to {}", 
                report.getReportId(), report.getStartTime(), report.getEndTime());
    }
    
    /**
     * 发送安全报告
     */
    private void sendSecurityReport(FileSecurityReport report) {
        // 这里可以通过邮件或其他方式发送报告
        log.info("发送文件安全报告: reportId={}, threats={}, highRisk={}", 
                report.getReportId(), report.getThreatCount(), report.getHighRiskCount());
    }
}    

    // ==================== 内部类 ====================
    
    /**
     * 文件安全检查结果
     */
    public static class FileSecurityCheckResult {
        private Long imageId;
        private String filename;
        private long fileSize;
        private String contentType;
        private String filePath;
        private String username;
        private String sourceIp;
        private LocalDateTime checkTime;
        private List<String> threats;
        private List<String> warnings;
        private int riskScore;
        private SecurityLevel securityLevel;
        private boolean threatDetected;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static FileSecurityCheckResult safe(String message) {
            return builder()
                .threats(new ArrayList<>())
                .warnings(List.of(message))
                .riskScore(0)
                .securityLevel(SecurityLevel.SAFE)
                .threatDetected(false)
                .checkTime(LocalDateTime.now())
                .build();
        }
        
        public static FileSecurityCheckResult error(String message) {
            return builder()
                .threats(List.of(message))
                .warnings(new ArrayList<>())
                .riskScore(100)
                .securityLevel(SecurityLevel.HIGH_RISK)
                .threatDetected(true)
                .checkTime(LocalDateTime.now())
                .build();
        }
        
        public static class Builder {
            private FileSecurityCheckResult result = new FileSecurityCheckResult();
            
            public Builder imageId(Long imageId) {
                result.imageId = imageId;
                return this;
            }
            
            public Builder filename(String filename) {
                result.filename = filename;
                return this;
            }
            
            public Builder fileSize(long fileSize) {
                result.fileSize = fileSize;
                return this;
            }
            
            public Builder contentType(String contentType) {
                result.contentType = contentType;
                return this;
            }
            
            public Builder filePath(String filePath) {
                result.filePath = filePath;
                return this;
            }
            
            public Builder username(String username) {
                result.username = username;
                return this;
            }
            
            public Builder sourceIp(String sourceIp) {
                result.sourceIp = sourceIp;
                return this;
            }
            
            public Builder checkTime(LocalDateTime checkTime) {
                result.checkTime = checkTime;
                return this;
            }
            
            public Builder threats(List<String> threats) {
                result.threats = threats;
                return this;
            }
            
            public Builder warnings(List<String> warnings) {
                result.warnings = warnings;
                return this;
            }
            
            public Builder riskScore(int riskScore) {
                result.riskScore = riskScore;
                return this;
            }
            
            public Builder securityLevel(SecurityLevel securityLevel) {
                result.securityLevel = securityLevel;
                return this;
            }
            
            public Builder threatDetected(boolean threatDetected) {
                result.threatDetected = threatDetected;
                return this;
            }
            
            public FileSecurityCheckResult build() {
                return result;
            }
        }
        
        // Getters
        public Long getImageId() { return imageId; }
        public String getFilename() { return filename; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public String getFilePath() { return filePath; }
        public String getUsername() { return username; }
        public String getSourceIp() { return sourceIp; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public List<String> getThreats() { return threats; }
        public List<String> getWarnings() { return warnings; }
        public int getRiskScore() { return riskScore; }
        public SecurityLevel getSecurityLevel() { return securityLevel; }
        public boolean isThreatDetected() { return threatDetected; }
    }
    
    /**
     * 文件内容检查结果
     */
    public static class FileContentCheckResult {
        private List<String> threats;
        private List<String> warnings;
        private int riskScore;
        private boolean malicious;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static FileContentCheckResult safe(String message) {
            return builder()
                .threats(new ArrayList<>())
                .warnings(List.of(message))
                .riskScore(0)
                .malicious(false)
                .build();
        }
        
        public static class Builder {
            private FileContentCheckResult result = new FileContentCheckResult();
            
            public Builder threats(List<String> threats) {
                result.threats = threats;
                return this;
            }
            
            public Builder warnings(List<String> warnings) {
                result.warnings = warnings;
                return this;
            }
            
            public Builder riskScore(int riskScore) {
                result.riskScore = riskScore;
                return this;
            }
            
            public Builder malicious(boolean malicious) {
                result.malicious = malicious;
                return this;
            }
            
            public FileContentCheckResult build() {
                return result;
            }
        }
        
        // Getters
        public List<String> getThreats() { return threats; }
        public List<String> getWarnings() { return warnings; }
        public int getRiskScore() { return riskScore; }
        public boolean isMalicious() { return malicious; }
    }
    
    /**
     * 用户行为分析结果
     */
    public static class UserBehaviorAnalysis {
        private String username;
        private String sourceIp;
        private int uploadCount;
        private boolean suspicious;
        private String description;
        private int riskScore;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static UserBehaviorAnalysis safe(String username, String sourceIp) {
            return builder()
                .username(username)
                .sourceIp(sourceIp)
                .uploadCount(0)
                .suspicious(false)
                .description("正常行为")
                .riskScore(0)
                .build();
        }
        
        public static class Builder {
            private UserBehaviorAnalysis result = new UserBehaviorAnalysis();
            
            public Builder username(String username) {
                result.username = username;
                return this;
            }
            
            public Builder sourceIp(String sourceIp) {
                result.sourceIp = sourceIp;
                return this;
            }
            
            public Builder uploadCount(int uploadCount) {
                result.uploadCount = uploadCount;
                return this;
            }
            
            public Builder suspicious(boolean suspicious) {
                result.suspicious = suspicious;
                return this;
            }
            
            public Builder description(String description) {
                result.description = description;
                return this;
            }
            
            public Builder riskScore(int riskScore) {
                result.riskScore = riskScore;
                return this;
            }
            
            public UserBehaviorAnalysis build() {
                return result;
            }
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getSourceIp() { return sourceIp; }
        public int getUploadCount() { return uploadCount; }
        public boolean isSuspicious() { return suspicious; }
        public String getDescription() { return description; }
        public int getRiskScore() { return riskScore; }
    }
    
    /**
     * 文件安全报告
     */
    public static class FileSecurityReport {
        private String reportId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long totalChecks;
        private long safeCount;
        private long lowRiskCount;
        private long mediumRiskCount;
        private long highRiskCount;
        private long threatCount;
        private LocalDateTime generatedTime;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private FileSecurityReport report = new FileSecurityReport();
            
            public Builder reportId(String reportId) {
                report.reportId = reportId;
                return this;
            }
            
            public Builder startTime(LocalDateTime startTime) {
                report.startTime = startTime;
                return this;
            }
            
            public Builder endTime(LocalDateTime endTime) {
                report.endTime = endTime;
                return this;
            }
            
            public Builder totalChecks(long totalChecks) {
                report.totalChecks = totalChecks;
                return this;
            }
            
            public Builder safeCount(long safeCount) {
                report.safeCount = safeCount;
                return this;
            }
            
            public Builder lowRiskCount(long lowRiskCount) {
                report.lowRiskCount = lowRiskCount;
                return this;
            }
            
            public Builder mediumRiskCount(long mediumRiskCount) {
                report.mediumRiskCount = mediumRiskCount;
                return this;
            }
            
            public Builder highRiskCount(long highRiskCount) {
                report.highRiskCount = highRiskCount;
                return this;
            }
            
            public Builder threatCount(long threatCount) {
                report.threatCount = threatCount;
                return this;
            }
            
            public Builder generatedTime(LocalDateTime generatedTime) {
                report.generatedTime = generatedTime;
                return this;
            }
            
            public FileSecurityReport build() {
                return report;
            }
        }
        
        // Getters
        public String getReportId() { return reportId; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public long getTotalChecks() { return totalChecks; }
        public long getSafeCount() { return safeCount; }
        public long getLowRiskCount() { return lowRiskCount; }
        public long getMediumRiskCount() { return mediumRiskCount; }
        public long getHighRiskCount() { return highRiskCount; }
        public long getThreatCount() { return threatCount; }
        public LocalDateTime getGeneratedTime() { return generatedTime; }
    }
}