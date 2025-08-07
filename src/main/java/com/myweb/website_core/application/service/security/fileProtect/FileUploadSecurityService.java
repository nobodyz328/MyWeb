package com.myweb.website_core.application.service.security.fileProtect;

import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusAlertService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusQuarantineService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusScanResult;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusScanService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.exception.FileValidationException;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.infrastructure.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 文件上传安全服务
 * 
 * 提供文件上传的安全验证功能：
 * - 文件类型、大小、魔数验证
 * - 恶意代码扫描
 * - 安全的文件存储路径和命名策略
 * - 审计日志记录
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
public class FileUploadSecurityService {
    
    private final AuditLogServiceAdapter auditLogService;
    private final VirusScanService virusScanService;
    private final VirusQuarantineService quarantineService;
    private final VirusAlertService alertService;
    
    // ==================== 安全配置常量 ====================
    
    /**
     * 允许的文件扩展名
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg"
    );
    
    /**
     * 允许的MIME类型
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", 
        "image/bmp", "image/svg+xml"
    );
    
    /**
     * 文件魔数映射表
     */
    private static final Map<String, List<String>> MAGIC_NUMBERS = Map.of(
        "jpg", Arrays.asList("FFD8FF"),
        "jpeg", Arrays.asList("FFD8FF"),
        "png", Arrays.asList("89504E47"),
        "gif", Arrays.asList("47494638", "47494639"),
        "webp", Arrays.asList("52494646"),
        "bmp", Arrays.asList("424D"),
        "svg", Arrays.asList("3C3F786D6C", "3C737667")
    );
    
    /**
     * 最大文件大小（5MB）
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    /**
     * 恶意代码检测模式
     */
    private static final List<Pattern> MALICIOUS_PATTERNS = Arrays.asList(
        // 脚本标签
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        
        // 事件处理器
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        
        // PHP代码
        Pattern.compile("<\\?php", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<\\?=", Pattern.CASE_INSENSITIVE),
        
        // JSP代码
        Pattern.compile("<%", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<jsp:", Pattern.CASE_INSENSITIVE),
        
        // Shell脚本
        Pattern.compile("#!/bin/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("#!/usr/bin/", Pattern.CASE_INSENSITIVE),
        
        // 可执行文件头
        Pattern.compile("MZ", Pattern.CASE_INSENSITIVE), // Windows PE
        Pattern.compile("\\x7fELF", Pattern.CASE_INSENSITIVE), // Linux ELF
        
        // 危险函数调用
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("exec\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("shell_exec\\s*\\(", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 禁止的文件名模式
     */
    private static final List<Pattern> FORBIDDEN_FILENAME_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\.(exe|bat|cmd|com|pif|scr|vbs|js|jar|war|ear)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.(php|jsp|asp|aspx|pl|py|rb|sh)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.(htaccess|htpasswd|web\\.config)$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\.", Pattern.CASE_INSENSITIVE), // 隐藏文件
        Pattern.compile(".*[<>:\"|?*].*", Pattern.CASE_INSENSITIVE) // 特殊字符
    );
    
    // ==================== 主要验证方法 ====================
    
    /**
     * 验证上传文件的安全性
     * 
     * @param file 上传的文件
     * @param userId 用户ID
     * @param username 用户名
     * @param request HTTP请求对象
     * @throws FileValidationException 验证失败时抛出异常
     */
    @Auditable(operation = AuditOperation.FILE_UPLOAD, resourceType = "FILE")
    public void validateUploadedFile(MultipartFile file, Long userId, String username, 
                                   HttpServletRequest request) throws FileValidationException {
        
        long startTime = System.currentTimeMillis();
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String originalFilename = file != null ? file.getOriginalFilename() : "null";
        
        try {
            log.info("开始文件安全验证: user={}, filename={}, size={}, ip={}", 
                    username, originalFilename, file != null ? file.getSize() : 0, clientIp);
            
            // 1. 基本验证
            validateBasicProperties(file);
            
            // 2. 文件名验证
            validateFileName(originalFilename);
            
            // 3. 文件类型验证
            validateFileType(file);
            
            // 4. 文件魔数验证
            validateFileMagicNumber(file);
            
            // 5. 恶意代码扫描
            scanForMaliciousContent(file);
            
            // 6. 病毒扫描
            performVirusScan(file, userId, username);
            
            // 7. 记录成功的审计日志
            recordAuditLog(userId, username, originalFilename, file, clientIp, userAgent, 
                          "SUCCESS", null, System.currentTimeMillis() - startTime);
            
            log.info("文件安全验证通过: user={}, filename={}, size={}", 
                    username, originalFilename, file.getSize());
            
        } catch (FileValidationException e) {
            // 记录失败的审计日志
            recordAuditLog(userId, username, originalFilename, file, clientIp, userAgent, 
                          "FAILURE", e.getMessage(), System.currentTimeMillis() - startTime);
            
            log.warn("文件安全验证失败: user={}, filename={}, error={}", 
                    username, originalFilename, e.getMessage());
            throw e;
            
        } catch (Exception e) {
            // 记录错误的审计日志
            recordAuditLog(userId, username, originalFilename, file, clientIp, userAgent, 
                          "ERROR", e.getMessage(), System.currentTimeMillis() - startTime);
            
            log.error("文件安全验证异常: user={}, filename={}, error={}", 
                     username, originalFilename, e.getMessage(), e);
            throw new FileValidationException("文件安全验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成安全的文件存储路径
     * 
     * @param originalFilename 原始文件名
     * @param userId 用户ID
     * @return 安全的存储路径
     */
    public String generateSecureStoragePath(String originalFilename, Long userId) {
        // 生成基于日期的目录结构
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 生成安全的文件名
        String secureFilename = generateSecureFileName(originalFilename, userId);
        
        return "uploads/images/" + datePath + "/" + secureFilename;
    }
    
    /**
     * 生成安全的文件名
     * 
     * @param originalFilename 原始文件名
     * @param userId 用户ID
     * @return 安全的文件名
     */
    public String generateSecureFileName(String originalFilename, Long userId) {
        if (originalFilename == null) {
            originalFilename = "unknown";
        }
        
        // 提取文件扩展名
        String extension = getFileExtension(originalFilename);
        
        // 生成UUID作为文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // 添加时间戳和用户ID增强唯一性
        String timestamp = String.valueOf(System.currentTimeMillis());
        String userIdStr = userId != null ? userId.toString() : "0";
        
        // 计算哈希值确保唯一性
        String hash = calculateHash(uuid + timestamp + userIdStr).substring(0, 8);
        
        return String.format("%s_%s_%s.%s", uuid, timestamp, hash, extension);
    }
    
    // ==================== 验证方法实现 ====================
    
    /**
     * 验证文件基本属性
     */
    private void validateBasicProperties(MultipartFile file) throws FileValidationException {
        // 检查文件是否为空
        if (file == null) {
            throw new FileValidationException("文件不能为空");
        }
        
        if (file.isEmpty()) {
            throw new FileValidationException("文件不能为空");
        }
        
        // 检查文件大小是否为0
        if (file.getSize() == 0) {
            throw new FileValidationException("文件大小不能为0");
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException(
                String.format("文件大小超过限制，最大允许 %d MB，当前文件 %.2f MB", 
                             MAX_FILE_SIZE / 1024 / 1024, 
                             file.getSize() / 1024.0 / 1024.0));
        }
    }
    
    /**
     * 验证文件名
     */
    private void validateFileName(String filename) throws FileValidationException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileValidationException("文件名不能为空");
        }
        
        // 检查文件名长度
        if (filename.length() > 255) {
            throw new FileValidationException("文件名长度不能超过255个字符");
        }
        
        // 检查禁止的文件名模式
        for (Pattern pattern : FORBIDDEN_FILENAME_PATTERNS) {
            if (pattern.matcher(filename).matches()) {
                throw new FileValidationException("文件名包含禁止的字符或扩展名: " + filename);
            }
        }
        
        // 检查文件扩展名
        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileValidationException(
                String.format("不支持的文件类型: %s，仅支持: %s", 
                             extension, String.join(", ", ALLOWED_EXTENSIONS)));
        }
    }
    
    /**
     * 验证文件类型
     */
    private void validateFileType(MultipartFile file) throws FileValidationException {
        String contentType = file.getContentType();
        
        if (contentType == null) {
            throw new FileValidationException("无法确定文件类型");
        }
        
        if (!ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new FileValidationException(
                String.format("不支持的MIME类型: %s，仅支持: %s", 
                             contentType, String.join(", ", ALLOWED_MIME_TYPES)));
        }
    }
    
    /**
     * 验证文件魔数
     */
    private void validateFileMagicNumber(MultipartFile file) throws FileValidationException {
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4) {
                throw new FileValidationException("文件内容过短，无法验证文件类型");
            }
            
            String filename = file.getOriginalFilename();
            String extension = getFileExtension(filename).toLowerCase();
            
            List<String> expectedMagicNumbers = MAGIC_NUMBERS.get(extension);
            if (expectedMagicNumbers == null) {
                throw new FileValidationException("不支持的文件扩展名: " + extension);
            }
            
            // 获取文件头部字节
            String actualMagic = bytesToHex(Arrays.copyOf(fileBytes, Math.min(fileBytes.length, 12)));
            
            // 检查是否匹配任何一个预期的魔数
            boolean magicMatches = expectedMagicNumbers.stream()
                .anyMatch(expected -> actualMagic.toUpperCase().startsWith(expected.toUpperCase()));
            
            if (!magicMatches) {
                throw new FileValidationException(
                    String.format("文件内容与扩展名不匹配，扩展名: %s，文件头: %s", 
                                 extension, actualMagic.substring(0, Math.min(actualMagic.length(), 16))));
            }
            
        } catch (IOException e) {
            throw new FileValidationException("读取文件内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 扫描恶意代码
     */
    private void scanForMaliciousContent(MultipartFile file) throws FileValidationException {
        try {
            byte[] fileBytes = file.getBytes();
            
            // 1. 二进制内容检查
            scanBinaryContent(fileBytes);
            
            // 2. 文本内容检查（对于可能包含文本的文件）
            scanTextContent(fileBytes);
            
            // 3. 文件结构检查
            validateFileStructure(fileBytes, file.getOriginalFilename());
            
        } catch (IOException e) {
            throw new FileValidationException("扫描文件内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 扫描二进制内容
     */
    private void scanBinaryContent(byte[] fileBytes) throws FileValidationException {
        // 检查是否包含可执行文件特征
        String hexContent = bytesToHex(fileBytes);
        
        // 检查PE文件头 (Windows可执行文件)
        if (hexContent.startsWith("4D5A")) { // MZ header
            throw new FileValidationException("检测到可执行文件特征，禁止上传");
        }
        
        // 检查ELF文件头 (Linux可执行文件)
        if (hexContent.startsWith("7F454C46")) { // ELF header
            throw new FileValidationException("检测到可执行文件特征，禁止上传");
        }
        
        // 检查其他可疑的二进制模式
        if (containsSuspiciousBinaryPatterns(fileBytes)) {
            throw new FileValidationException("检测到可疑的二进制内容");
        }
    }
    
    /**
     * 扫描文本内容
     */
    private void scanTextContent(byte[] fileBytes) throws FileValidationException {
        try {
            // 尝试将文件内容解析为文本
            String content = new String(fileBytes, StandardCharsets.UTF_8);
            String lowerContent = content.toLowerCase();
            
            // 检查恶意代码模式
            for (Pattern pattern : MALICIOUS_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    throw new FileValidationException("检测到潜在的恶意代码: " + pattern.pattern());
                }
            }
            
            // 检查可疑的字符串
            String[] suspiciousStrings = {
                "eval(", "exec(", "system(", "shell_exec(", "passthru(",
                "file_get_contents(", "file_put_contents(", "fopen(", "fwrite(",
                "include(", "require(", "include_once(", "require_once(",
                "base64_decode(", "gzinflate(", "str_rot13(",
                "create_function(", "call_user_func(", "preg_replace("
            };
            
            for (String suspicious : suspiciousStrings) {
                if (lowerContent.contains(suspicious.toLowerCase())) {
                    throw new FileValidationException("检测到可疑的函数调用: " + suspicious);
                }
            }
            
        } catch (Exception e) {
            // 如果不是文本文件，忽略文本扫描错误
            log.debug("文本内容扫描跳过（可能是二进制文件）: {}", e.getMessage());
        }
    }
    
    /**
     * 验证文件结构
     */
    private void validateFileStructure(byte[] fileBytes, String filename) throws FileValidationException {
        String extension = getFileExtension(filename).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                validateJpegStructure(fileBytes);
                break;
            case "png":
                validatePngStructure(fileBytes);
                break;
            case "gif":
                validateGifStructure(fileBytes);
                break;
            case "svg":
                validateSvgStructure(fileBytes);
                break;
            default:
                // 对于其他格式，进行基本的结构检查
                validateBasicStructure(fileBytes);
        }
    }
    
    // ==================== 文件结构验证方法 ====================
    
    /**
     * 验证JPEG文件结构
     */
    private void validateJpegStructure(byte[] fileBytes) throws FileValidationException {
        if (fileBytes.length < 10) {
            throw new FileValidationException("JPEG文件结构不完整");
        }
        
        // 检查JPEG文件头和尾
        if (!(fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8)) {
            throw new FileValidationException("无效的JPEG文件头");
        }
        
        // 检查JPEG文件尾
        if (fileBytes.length >= 2) {
            int len = fileBytes.length;
            if (!(fileBytes[len - 2] == (byte) 0xFF && fileBytes[len - 1] == (byte) 0xD9)) {
                log.warn("JPEG文件可能缺少正确的文件尾标记");
            }
        }
    }
    
    /**
     * 验证PNG文件结构
     */
    private void validatePngStructure(byte[] fileBytes) throws FileValidationException {
        if (fileBytes.length < 8) {
            throw new FileValidationException("PNG文件结构不完整");
        }
        
        // 检查PNG文件签名
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < 8; i++) {
            if (fileBytes[i] != pngSignature[i]) {
                throw new FileValidationException("无效的PNG文件签名");
            }
        }
    }
    
    /**
     * 验证GIF文件结构
     */
    private void validateGifStructure(byte[] fileBytes) throws FileValidationException {
        if (fileBytes.length < 6) {
            throw new FileValidationException("GIF文件结构不完整");
        }
        
        // 检查GIF文件头
        String header = new String(fileBytes, 0, 6, StandardCharsets.US_ASCII);
        if (!header.equals("GIF87a") && !header.equals("GIF89a")) {
            throw new FileValidationException("无效的GIF文件头: " + header);
        }
    }
    
    /**
     * 验证SVG文件结构
     */
    private void validateSvgStructure(byte[] fileBytes) throws FileValidationException {
        try {
            String content = new String(fileBytes, StandardCharsets.UTF_8);
            
            // SVG必须包含<svg>标签
            if (!content.contains("<svg")) {
                throw new FileValidationException("SVG文件必须包含<svg>标签");
            }
            
            // 检查是否包含危险的SVG元素
            String[] dangerousElements = {
                "<script", "<object", "<embed", "<iframe", "<link", "<meta",
                "<foreignObject", "javascript:", "data:text/html"
            };
            
            String lowerContent = content.toLowerCase();
            for (String dangerous : dangerousElements) {
                if (lowerContent.contains(dangerous.toLowerCase())) {
                    throw new FileValidationException("SVG文件包含危险元素: " + dangerous);
                }
            }
            
        } catch (Exception e) {
            throw new FileValidationException("SVG文件结构验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证基本文件结构
     */
    private void validateBasicStructure(byte[] fileBytes) throws FileValidationException {
        // 检查文件是否过小
        if (fileBytes.length < 10) {
            throw new FileValidationException("文件内容过小，可能不是有效的图片文件");
        }
        
        // 检查是否包含过多的空字节
        int nullByteCount = 0;
        for (byte b : fileBytes) {
            if (b == 0) {
                nullByteCount++;
            }
        }
        
        // 如果空字节超过50%，可能是可疑文件
        if (nullByteCount > fileBytes.length * 0.5) {
            throw new FileValidationException("文件包含过多空字节，可能是可疑文件");
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查是否包含可疑的二进制模式
     */
    private boolean containsSuspiciousBinaryPatterns(byte[] fileBytes) {
        // 检查是否包含大量重复的字节模式（可能是填充攻击）
        Map<Byte, Integer> byteFrequency = new HashMap<>();
        for (byte b : fileBytes) {
            byteFrequency.put(b, byteFrequency.getOrDefault(b, 0) + 1);
        }
        
        // 如果某个字节出现频率超过80%，可能是可疑的
        for (int count : byteFrequency.values()) {
            if (count > fileBytes.length * 0.8) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
            "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一个IP地址
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 计算字符串的SHA-256哈希值
     */
    private String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // 如果SHA-256不可用，使用简单的哈希
            return String.valueOf(input.hashCode());
        }
    }
    
    /**
     * 执行病毒扫描
     */
    private void performVirusScan(MultipartFile file, Long userId, String username) throws FileValidationException {
        try {
            // 检查病毒扫描服务是否可用
            if (!virusScanService.isAvailable()) {
                log.warn("病毒扫描服务不可用，跳过扫描: user={}, filename={}", 
                        username, file.getOriginalFilename());
                
                // 发送引擎不可用告警
                alertService.sendEngineUnavailableAlert(
                    virusScanService.getClass().getSimpleName(), 
                    "病毒扫描引擎连接失败或未安装"
                );
                
                // 根据配置决定是否允许上传
                // 这里采用保守策略：扫描服务不可用时仍允许上传，但记录警告
                return;
            }
            
            // 执行异步病毒扫描
            CompletableFuture<VirusScanResult> scanFuture = virusScanService.scanFile(file, userId, username);
            
            // 等待扫描结果（设置超时时间）
            VirusScanResult scanResult;
            try {
                scanResult = scanFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("病毒扫描超时: user={}, filename={}", username, file.getOriginalFilename());
                throw new FileValidationException("文件安全扫描超时，请稍后重试");
            }
            
            // 处理扫描结果
            if (scanResult.shouldBlockUpload()) {
                // 发现病毒或扫描失败，阻止上传
                if (scanResult.isVirusFound()) {
                    log.warn("检测到病毒文件: user={}, filename={}, virus={}", 
                            username, file.getOriginalFilename(), scanResult.getVirusName());
                    
                    // 隔离可疑文件
                    if (scanResult.isRequiresQuarantine()) {
                        quarantineService.quarantineFile(file, scanResult);
                    }
                    
                    // 发送安全告警
                    if (scanResult.isRequiresAlert()) {
                        alertService.sendVirusAlert(scanResult);
                    }
                    
                    throw new FileValidationException(
                        String.format("检测到恶意文件: %s (威胁级别: %s)", 
                                     scanResult.getVirusName(), scanResult.getThreatLevel()));
                } else {
                    // 扫描失败
                    throw new FileValidationException("文件安全扫描失败: " + scanResult.getErrorMessage());
                }
            }
            
            log.debug("病毒扫描通过: user={}, filename={}, duration={}ms", 
                     username, file.getOriginalFilename(), scanResult.getScanDurationMs());
            
        } catch (FileValidationException e) {
            // 重新抛出文件验证异常
            throw e;
        } catch (Exception e) {
            log.error("病毒扫描异常: user={}, filename={}, error={}", 
                     username, file.getOriginalFilename(), e.getMessage(), e);
            
            // 根据配置决定是否在扫描异常时阻止上传
            // 这里采用保守策略：扫描异常时仍允许上传，但记录错误
            log.warn("病毒扫描异常，允许上传但建议人工审查: {}", e.getMessage());
        }
    }
    
    /**
     * 记录审计日志
     */
    private void recordAuditLog(Long userId, String username, String filename, MultipartFile file,
                               String clientIp, String userAgent, String result, String errorMessage,
                               long executionTime) {
        try {
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                .userId(userId)
                .username(username)
                .operation(AuditOperation.FILE_UPLOAD)
                .resourceType("FILE")
                .ipAddress(clientIp)
                .userAgent(userAgent)
                .result(result)
                .errorMessage(errorMessage)
                .executionTime(executionTime)
                .timestamp(LocalDateTime.now())
                .description(String.format("文件上传: %s (%s, %d bytes)", 
                           filename, 
                           file != null ? file.getContentType() : "unknown", 
                           file != null ? file.getSize() : 0))
                .riskLevel(3) // 文件上传为中等风险操作
                .build();
            
            // 异步记录审计日志
            auditLogService.logOperation(auditRequest);
            
        } catch (Exception e) {
            log.error("记录文件上传审计日志失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }
}