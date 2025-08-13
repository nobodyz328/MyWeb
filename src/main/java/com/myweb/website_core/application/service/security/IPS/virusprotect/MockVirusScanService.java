package com.myweb.website_core.application.service.security.IPS.virusprotect;

import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟病毒扫描服务实现
 * <p>
 * 用于开发和测试环境的模拟病毒扫描服务：
 * - 模拟真实的扫描过程和结果
 * - 支持测试病毒文件检测（EICAR测试文件）
 * - 可配置的扫描延迟和结果
 * - 完整的审计日志记录
 * - 不依赖外部病毒扫描引擎
 * <p>
 * 符合GB/T 22239-2019二级等保要求的恶意代码防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.virus-scan.engine", havingValue = "mock", matchIfMissing = true)
public class MockVirusScanService implements VirusScanService {
    
    private final AuditMessageService auditLogService;
    
    private static final String ENGINE_NAME = "Mock Virus Scanner";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String DATABASE_VERSION = "2025.01.01";
    
    // EICAR测试病毒文件标准字符串
    private static final String EICAR_TEST_STRING = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    
    // 模拟病毒签名库
    private static final String[] VIRUS_SIGNATURES = {
        "TestVirus.Generic",
        "Trojan.Test.Mock",
        "Worm.Simulation.A",
        "Malware.Sample.B",
        "Backdoor.Mock.C"
    };
    
    /**
     * 扫描上传的文件
     */
    @Override
    @Async
    public CompletableFuture<VirusScanResult> scanFile(MultipartFile file, Long userId, String username) {
        long startTime = System.currentTimeMillis();
        String filename = file.getOriginalFilename();
        
        log.info("开始模拟病毒扫描: user={}, filename={}, size={}", 
                username, filename, file.getSize());
        
        try {
            // 模拟扫描延迟
            simulateScanDelay();
            
            // 读取文件内容进行检测
            byte[] fileBytes = file.getBytes();
            VirusScanResult result = performMockScan(fileBytes, filename, userId, username, startTime);
            result.setFileSize(file.getSize());
            
            recordAuditLog(result, result.isVirusFound() ? "VIRUS_DETECTED" : "SCAN_CLEAN");
            
            log.info("模拟病毒扫描完成: user={}, filename={}, result={}, duration={}ms", 
                    username, filename, result.getSummary(), result.getScanDurationMs());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            VirusScanResult result = VirusScanResult.failure(filename, "读取文件失败: " + e.getMessage(), 
                                                           userId, username, ENGINE_NAME);
            result.setScanDurationMs(duration);
            
            recordAuditLog(result, "SCAN_ERROR");
            
            log.error("模拟病毒扫描异常: user={}, filename={}, error={}", 
                     username, filename, e.getMessage(), e);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * 扫描输入流
     */
    @Override
    @Async
    public CompletableFuture<VirusScanResult> scanInputStream(InputStream inputStream, String filename, 
                                                             Long userId, String username) {
        long startTime = System.currentTimeMillis();
        
        log.info("开始模拟输入流扫描: user={}, filename={}", username, filename);
        
        try {
            // 模拟扫描延迟
            simulateScanDelay();
            
            // 读取输入流内容
            byte[] fileBytes = inputStream.readAllBytes();
            VirusScanResult result = performMockScan(fileBytes, filename, userId, username, startTime);
            result.setFileSize(fileBytes.length);
            
            recordAuditLog(result, result.isVirusFound() ? "VIRUS_DETECTED" : "SCAN_CLEAN");
            
            log.info("模拟输入流扫描完成: user={}, filename={}, result={}, duration={}ms", 
                    username, filename, result.getSummary(), result.getScanDurationMs());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            VirusScanResult result = VirusScanResult.failure(filename, "读取输入流失败: " + e.getMessage(), 
                                                           userId, username, ENGINE_NAME);
            result.setScanDurationMs(duration);
            
            recordAuditLog(result, "SCAN_ERROR");
            
            log.error("模拟输入流扫描异常: user={}, filename={}, error={}", 
                     username, filename, e.getMessage(), e);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * 扫描字节数组
     */
    @Override
    @Async
    public CompletableFuture<VirusScanResult> scanBytes(byte[] fileBytes, String filename, 
                                                       Long userId, String username) {
        long startTime = System.currentTimeMillis();
        
        log.info("开始模拟字节数组扫描: user={}, filename={}, size={}", 
                username, filename, fileBytes.length);
        
        try {
            // 模拟扫描延迟
            simulateScanDelay();
            
            VirusScanResult result = performMockScan(fileBytes, filename, userId, username, startTime);
            result.setFileSize(fileBytes.length);
            
            recordAuditLog(result, result.isVirusFound() ? "VIRUS_DETECTED" : "SCAN_CLEAN");
            
            log.info("模拟字节数组扫描完成: user={}, filename={}, result={}, duration={}ms", 
                    username, filename, result.getSummary(), result.getScanDurationMs());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            VirusScanResult result = VirusScanResult.failure(filename, "扫描异常: " + e.getMessage(), 
                                                           userId, username, ENGINE_NAME);
            result.setScanDurationMs(duration);
            result.setFileSize(fileBytes.length);
            
            recordAuditLog(result, "SCAN_ERROR");
            
            log.error("模拟字节数组扫描异常: user={}, filename={}, error={}", 
                     username, filename, e.getMessage(), e);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * 检查模拟扫描引擎是否可用（始终可用）
     */
    @Override
    public boolean isAvailable() {
        log.debug("模拟病毒扫描引擎始终可用");
        return true;
    }
    
    /**
     * 获取模拟引擎信息
     */
    @Override
    public String getEngineInfo() {
        return String.format("%s - Version: %s, Database: %s, Status: Available", 
                           ENGINE_NAME, ENGINE_VERSION, DATABASE_VERSION);
    }
    
    /**
     * 模拟更新病毒库
     */
    @Override
    @Async
    public CompletableFuture<Boolean> updateVirusDatabase() {
        log.info("开始模拟病毒库更新...");
        
        try {
            // 模拟更新延迟
            Thread.sleep(2000);
            
            log.info("模拟病毒库更新完成，当前版本: {}", DATABASE_VERSION);
            return CompletableFuture.completedFuture(true);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("模拟病毒库更新被中断: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 执行模拟扫描
     */
    private VirusScanResult performMockScan(byte[] fileBytes, String filename, 
                                           Long userId, String username, long startTime) {
        
        // 检测EICAR测试文件
        String content = new String(fileBytes, StandardCharsets.UTF_8);
        if (content.contains(EICAR_TEST_STRING)) {
            long duration = System.currentTimeMillis() - startTime;
            return createVirusDetectedResult("EICAR-Test-Signature", 
                                           VirusScanResult.ThreatLevel.LOW,
                                           filename, userId, username, duration);
        }
        
        // 检测文件名中的测试病毒标识
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.contains("virus") || lowerFilename.contains("malware") ||
                lowerFilename.contains("trojan") || lowerFilename.contains("test-virus")) {
                
                String virusName = selectRandomVirus();
                VirusScanResult.ThreatLevel threatLevel = determineThreatLevel(virusName);
                long duration = System.currentTimeMillis() - startTime;
                
                return createVirusDetectedResult(virusName, threatLevel, filename, userId, username, duration);
            }
        }
        
        // 检测文件内容中的可疑模式
        if (containsSuspiciousPatterns(content)) {
            String virusName = "Suspicious.Pattern.Detected";
            long duration = System.currentTimeMillis() - startTime;
            return createVirusDetectedResult(virusName, VirusScanResult.ThreatLevel.MEDIUM,
                                           filename, userId, username, duration);
        }
        
        // 模拟随机病毒检测（1%概率）
        if (ThreadLocalRandom.current().nextInt(100) < 1) {
            String virusName = selectRandomVirus();
            VirusScanResult.ThreatLevel threatLevel = determineThreatLevel(virusName);
            long duration = System.currentTimeMillis() - startTime;
            
            return createVirusDetectedResult(virusName, threatLevel, filename, userId, username, duration);
        }
        
        // 默认返回清洁结果
        long duration = System.currentTimeMillis() - startTime;
        return createCleanResult(filename, userId, username, duration);
    }
    
    /**
     * 创建病毒检测结果
     */
    private VirusScanResult createVirusDetectedResult(String virusName, VirusScanResult.ThreatLevel threatLevel,
                                                     String filename, Long userId, String username, long duration) {
        return VirusScanResult.builder()
            .status(VirusScanResult.ScanStatus.SUCCESS)
            .virusFound(true)
            .virusName(virusName)
            .threatLevel(threatLevel)
            .filename(filename)
            .userId(userId)
            .username(username)
            .engineName(ENGINE_NAME)
            .engineVersion(ENGINE_VERSION)
            .databaseVersion(DATABASE_VERSION)
            .scanDurationMs(duration)
            .scanStartTime(LocalDateTime.now().minusNanos(duration * 1_000_000))
            .scanEndTime(LocalDateTime.now())
            .details("模拟检测到病毒: " + virusName)
            .recommendedAction("拒绝上传并隔离文件")
            .requiresQuarantine(true)
            .requiresAlert(true)
            .build();
    }
    
    /**
     * 创建清洁扫描结果
     */
    private VirusScanResult createCleanResult(String filename, Long userId, String username, long duration) {
        return VirusScanResult.builder()
            .status(VirusScanResult.ScanStatus.SUCCESS)
            .virusFound(false)
            .threatLevel(VirusScanResult.ThreatLevel.NONE)
            .filename(filename)
            .userId(userId)
            .username(username)
            .engineName(ENGINE_NAME)
            .engineVersion(ENGINE_VERSION)
            .databaseVersion(DATABASE_VERSION)
            .scanDurationMs(duration)
            .scanStartTime(LocalDateTime.now().minusNanos(duration * 1_000_000))
            .scanEndTime(LocalDateTime.now())
            .details("模拟扫描完成，未发现威胁")
            .recommendedAction("允许上传")
            .requiresQuarantine(false)
            .requiresAlert(false)
            .build();
    }
    
    /**
     * 检测可疑模式
     */
    private boolean containsSuspiciousPatterns(String content) {
        String lowerContent = content.toLowerCase();
        
        // 检测可疑的脚本内容
        String[] suspiciousPatterns = {
            "<script>alert('virus')</script>",
            "eval(atob(",
            "document.write(unescape(",
            "javascript:void(0)",
            "onload=\"malicious()",
            "<?php system($_GET",
            "exec($_POST",
            "shell_exec(",
            "#!/bin/sh"
        };
        
        for (String pattern : suspiciousPatterns) {
            if (lowerContent.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 随机选择一个病毒名称
     */
    private String selectRandomVirus() {
        int index = ThreadLocalRandom.current().nextInt(VIRUS_SIGNATURES.length);
        return VIRUS_SIGNATURES[index];
    }
    
    /**
     * 根据病毒名称确定威胁级别
     */
    private VirusScanResult.ThreatLevel determineThreatLevel(String virusName) {
        if (virusName == null) {
            return VirusScanResult.ThreatLevel.MEDIUM;
        }
        
        String lowerVirusName = virusName.toLowerCase();
        
        // 测试病毒
        if (lowerVirusName.contains("test") || lowerVirusName.contains("eicar")) {
            return VirusScanResult.ThreatLevel.LOW;
        }
        
        // 高危病毒
        if (lowerVirusName.contains("trojan") || lowerVirusName.contains("backdoor")) {
            return VirusScanResult.ThreatLevel.CRITICAL;
        }
        
        // 中高危病毒
        if (lowerVirusName.contains("worm") || lowerVirusName.contains("malware")) {
            return VirusScanResult.ThreatLevel.HIGH;
        }
        
        // 默认中等威胁
        return VirusScanResult.ThreatLevel.MEDIUM;
    }
    
    /**
     * 模拟扫描延迟
     */
    private void simulateScanDelay() {
        try {
            // 随机延迟100-500毫秒，模拟真实扫描时间
            int delay = ThreadLocalRandom.current().nextInt(100, 501);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("模拟扫描延迟被中断");
        }
    }
    
    /**
     * 记录审计日志
     */
    private void recordAuditLog(VirusScanResult result, String eventType) {
        try {
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .userId(result.getUserId())
                .username(result.getUsername())
                .operation(AuditOperation.VIRUS_SCAN)
                .resourceType("FILE")
                .result(result.getStatus().name())
                .errorMessage(result.getErrorMessage())
                .executionTime(result.getScanDurationMs())
                .timestamp(LocalDateTime.now())
                .description(String.format("模拟病毒扫描: %s - %s", 
                           result.getFilename(), result.getSummary()))
                .riskLevel(result.isVirusFound() ? 5 : 2) // 发现病毒为高风险，否则为低风险
                .build();
            
            // 异步记录审计日志
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录模拟病毒扫描审计日志失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }
}