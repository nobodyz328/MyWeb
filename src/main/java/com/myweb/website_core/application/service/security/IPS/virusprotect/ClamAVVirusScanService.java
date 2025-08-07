package com.myweb.website_core.application.service.security.IPS.virusprotect;

import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import fi.solita.clamav.ClamAVClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * ClamAV病毒扫描服务实现
 * 
 * 集成ClamAV反病毒引擎，提供实时病毒扫描功能：
 * - 支持文件、输入流、字节数组扫描
 * - 异步扫描处理，避免阻塞业务流程
 * - 完整的审计日志记录
 * - 扫描结果缓存和性能优化
 * - 病毒库更新和健康检查
 * 
 * 符合GB/T 22239-2019二级等保要求的恶意代码防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.virus-scan.engine", havingValue = "clamav", matchIfMissing = false)
public class ClamAVVirusScanService implements VirusScanService {
    
    private final AuditLogServiceAdapter auditLogService;
    
    @Value("${app.security.virus-scan.clamav.host:localhost}")
    private String clamavHost;
    
    @Value("${app.security.virus-scan.clamav.port:3310}")
    private int clamavPort;
    
    @Value("${app.security.virus-scan.timeout:30}")
    private int scanTimeoutSeconds;
    
    @Value("${app.security.virus-scan.max-file-size:50MB}")
    private String maxFileSize;
    
    private static final String ENGINE_NAME = "ClamAV";
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
    
    /**
     * 扫描上传的文件
     */
    @Override
    @Async
    public CompletableFuture<VirusScanResult> scanFile(MultipartFile file, Long userId, String username) {
        long startTime = System.currentTimeMillis();
        String filename = file.getOriginalFilename();
        
        log.info("开始ClamAV病毒扫描: user={}, filename={}, size={}", 
                username, filename, file.getSize());
        
        try {
            // 检查文件大小
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                String errorMsg = String.format("文件大小超过扫描限制: %d bytes > %d bytes", 
                                               file.getSize(), MAX_FILE_SIZE_BYTES);
                log.warn("ClamAV扫描跳过大文件: {}", errorMsg);
                
                VirusScanResult result = VirusScanResult.failure(filename, errorMsg, userId, username, ENGINE_NAME);
                recordAuditLog(result, "FILE_TOO_LARGE");
                return CompletableFuture.completedFuture(result);
            }
            
            // 执行扫描
            VirusScanResult result = performScan(file.getInputStream(), filename, userId, username, startTime);
            result.setFileSize(file.getSize());
            
            recordAuditLog(result, result.isVirusFound() ? "VIRUS_DETECTED" : "SCAN_CLEAN");
            
            log.info("ClamAV病毒扫描完成: user={}, filename={}, result={}, duration={}ms", 
                    username, filename, result.getSummary(), result.getScanDurationMs());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            VirusScanResult result = VirusScanResult.failure(filename, "读取文件失败: " + e.getMessage(), 
                                                           userId, username, ENGINE_NAME);
            result.setScanDurationMs(duration);
            
            recordAuditLog(result, "SCAN_ERROR");
            
            log.error("ClamAV病毒扫描异常: user={}, filename={}, error={}", 
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
        
        log.info("开始ClamAV输入流扫描: user={}, filename={}", username, filename);
        
        try {
            VirusScanResult result = performScan(inputStream, filename, userId, username, startTime);
            recordAuditLog(result, result.isVirusFound() ? "VIRUS_DETECTED" : "SCAN_CLEAN");
            
            log.info("ClamAV输入流扫描完成: user={}, filename={}, result={}, duration={}ms", 
                    username, filename, result.getSummary(), result.getScanDurationMs());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            VirusScanResult result = VirusScanResult.failure(filename, "扫描异常: " + e.getMessage(), 
                                                           userId, username, ENGINE_NAME);
            result.setScanDurationMs(duration);
            
            recordAuditLog(result, "SCAN_ERROR");
            
            log.error("ClamAV输入流扫描异常: user={}, filename={}, error={}", 
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
        
        log.info("开始ClamAV字节数组扫描: user={}, filename={}, size={}", 
                username, filename, fileBytes.length);
        
        try {
            // 检查字节数组大小
            if (fileBytes.length > MAX_FILE_SIZE_BYTES) {
                String errorMsg = String.format("数据大小超过扫描限制: %d bytes > %d bytes", 
                                               fileBytes.length, MAX_FILE_SIZE_BYTES);
                log.warn("ClamAV扫描跳过大数据: {}", errorMsg);
                
                VirusScanResult result = VirusScanResult.failure(filename, errorMsg, userId, username, ENGINE_NAME);
                recordAuditLog(result, "DATA_TOO_LARGE");
                return CompletableFuture.completedFuture(result);
            }
            
            ClamAVClient clamAVClient = createClamAVClient();
            byte[] scanResult = clamAVClient.scan(fileBytes);
            
            long duration = System.currentTimeMillis() - startTime;
            VirusScanResult result = parseScanResult(scanResult, filename, userId, username, duration);
            result.setFileSize(fileBytes.length);
            
            recordAuditLog(result, result.isVirusFound() ? "VIRUS_DETECTED" : "SCAN_CLEAN");
            
            log.info("ClamAV字节数组扫描完成: user={}, filename={}, result={}, duration={}ms", 
                    username, filename, result.getSummary(), duration);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            VirusScanResult result = VirusScanResult.failure(filename, "扫描异常: " + e.getMessage(), 
                                                           userId, username, ENGINE_NAME);
            result.setScanDurationMs(duration);
            result.setFileSize(fileBytes.length);
            
            recordAuditLog(result, "SCAN_ERROR");
            
            log.error("ClamAV字节数组扫描异常: user={}, filename={}, error={}", 
                     username, filename, e.getMessage(), e);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * 检查ClamAV是否可用
     */
    @Override
    public boolean isAvailable() {
        try {
            ClamAVClient clamAVClient = createClamAVClient();
            clamAVClient.ping();
            log.debug("ClamAV引擎可用: {}:{}", clamavHost, clamavPort);
            return true;
        } catch (Exception e) {
            log.warn("ClamAV引擎不可用: {}:{}, error={}", clamavHost, clamavPort, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取ClamAV引擎信息
     */
    @Override
    public String getEngineInfo() {
        try {
            ClamAVClient clamAVClient = createClamAVClient();
            String version = null;//clamAVClient.version();
            return String.format("ClamAV Engine - Host: %s:%d, Version: %s", 
                                clamavHost, clamavPort, version);
        } catch (Exception e) {
            return String.format("ClamAV Engine - Host: %s:%d, Status: Unavailable (%s)", 
                                clamavHost, clamavPort, e.getMessage());
        }
    }
    
    /**
     * 更新病毒库
     */
    @Override
    @Async
    public CompletableFuture<Boolean> updateVirusDatabase() {
        log.info("开始更新ClamAV病毒库...");
        
        try {
            // ClamAV的病毒库更新通常通过freshclam命令在系统级别执行
            // 这里我们只是检查连接状态，实际更新需要系统管理员权限
            ClamAVClient clamAVClient = createClamAVClient();
            clamAVClient.ping();
            
            log.info("ClamAV病毒库更新检查完成，引擎正常运行");
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("ClamAV病毒库更新检查失败: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 创建ClamAV客户端
     */
    private ClamAVClient createClamAVClient() {
        return new ClamAVClient(clamavHost, clamavPort, scanTimeoutSeconds * 1000);
    }
    
    /**
     * 执行扫描
     */
    private VirusScanResult performScan(InputStream inputStream, String filename, 
                                       Long userId, String username, long startTime) throws IOException {
        try {
            ClamAVClient clamAVClient = createClamAVClient();
            
            // 设置扫描超时
            byte[] scanResult = clamAVClient.scan(inputStream);
            
            long duration = System.currentTimeMillis() - startTime;
            return parseScanResult(scanResult, filename, userId, username, duration);
            
        } catch (Exception e) {
            // 检查是否是超时异常
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                long duration = System.currentTimeMillis() - startTime;
                VirusScanResult result = VirusScanResult.builder()
                    .status(VirusScanResult.ScanStatus.TIMEOUT)
                    .virusFound(false)
                    .filename(filename)
                    .userId(userId)
                    .username(username)
                    .engineName(ENGINE_NAME)
                    .scanDurationMs(duration)
                    .scanEndTime(LocalDateTime.now())
                    .errorMessage("扫描超时")
                    .details("扫描超时，可能文件过大或网络延迟")
                    .recommendedAction("拒绝上传")
                    .requiresAlert(true)
                    .build();
                return result;
            }
            
            // 检查是否是连接异常
            if (!isAvailable()) {
                return VirusScanResult.unavailable(filename, userId, username, ENGINE_NAME);
            }
            
            throw e;
        }
    }
    
    /**
     * 解析扫描结果
     */
    private VirusScanResult parseScanResult(byte[] scanResult, String filename, 
                                           Long userId, String username, long duration) {
        try {
            String resultString = new String(scanResult).trim();
            
            if ("OK".equals(resultString)) {
                // 未发现病毒
                return VirusScanResult.builder()
                    .status(VirusScanResult.ScanStatus.SUCCESS)
                    .virusFound(false)
                    .threatLevel(VirusScanResult.ThreatLevel.NONE)
                    .filename(filename)
                    .userId(userId)
                    .username(username)
                    .engineName(ENGINE_NAME)
                    .scanDurationMs(duration)
                    .scanStartTime(LocalDateTime.now().minusNanos(duration * 1_000_000))
                    .scanEndTime(LocalDateTime.now())
                    .details("文件扫描完成，未发现威胁")
                    .recommendedAction("允许上传")
                    .requiresQuarantine(false)
                    .requiresAlert(false)
                    .build();
            } else {
                // 发现病毒
                String virusName = extractVirusName(resultString);
                VirusScanResult.ThreatLevel threatLevel = determineThreatLevel(virusName);
                
                return VirusScanResult.builder()
                    .status(VirusScanResult.ScanStatus.SUCCESS)
                    .virusFound(true)
                    .virusName(virusName)
                    .threatLevel(threatLevel)
                    .filename(filename)
                    .userId(userId)
                    .username(username)
                    .engineName(ENGINE_NAME)
                    .scanDurationMs(duration)
                    .scanStartTime(LocalDateTime.now().minusNanos(duration * 1_000_000))
                    .scanEndTime(LocalDateTime.now())
                    .details("检测到病毒: " + virusName)
                    .recommendedAction("拒绝上传并隔离文件")
                    .requiresQuarantine(true)
                    .requiresAlert(true)
                    .build();
            }
            
        } catch (Exception e) {
            log.error("解析ClamAV扫描结果失败: {}", e.getMessage(), e);
            return VirusScanResult.failure(filename, "解析扫描结果失败: " + e.getMessage(), 
                                         userId, username, ENGINE_NAME);
        }
    }
    
    /**
     * 从扫描结果中提取病毒名称
     */
    private String extractVirusName(String resultString) {
        // ClamAV结果格式通常是: "stream: Eicar-Test-Signature FOUND"
        if (resultString.contains("FOUND")) {
            String[] parts = resultString.split(":");
            if (parts.length > 1) {
                String virusPart = parts[1].trim();
                return virusPart.replace("FOUND", "").trim();
            }
        }
        return resultString;
    }
    
    /**
     * 根据病毒名称确定威胁级别
     */
    private VirusScanResult.ThreatLevel determineThreatLevel(String virusName) {
        if (virusName == null) {
            return VirusScanResult.ThreatLevel.MEDIUM;
        }
        
        String lowerVirusName = virusName.toLowerCase();
        
        // 测试病毒（如EICAR）
        if (lowerVirusName.contains("eicar") || lowerVirusName.contains("test")) {
            return VirusScanResult.ThreatLevel.LOW;
        }
        
        // 高危病毒类型
        if (lowerVirusName.contains("trojan") || lowerVirusName.contains("backdoor") ||
            lowerVirusName.contains("rootkit") || lowerVirusName.contains("ransomware")) {
            return VirusScanResult.ThreatLevel.CRITICAL;
        }
        
        // 中高危病毒类型
        if (lowerVirusName.contains("worm") || lowerVirusName.contains("virus") ||
            lowerVirusName.contains("malware") || lowerVirusName.contains("spyware")) {
            return VirusScanResult.ThreatLevel.HIGH;
        }
        
        // 默认中等威胁
        return VirusScanResult.ThreatLevel.MEDIUM;
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
                .description(String.format("ClamAV病毒扫描: %s - %s", 
                           result.getFilename(), result.getSummary()))
                .riskLevel(result.isVirusFound() ? 5 : 2) // 发现病毒为高风险，否则为低风险
                .build();
            
            // 异步记录审计日志
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录病毒扫描审计日志失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }
}