package com.myweb.website_core.application.service.file;

import com.myweb.website_core.application.service.security.integeration.FileUploadSecurityService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusScanService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusScanResult;
import com.myweb.website_core.application.service.security.FileSecurityMonitoringService;
import com.myweb.website_core.common.util.SecurityUtils;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.PermissionUtils;
import com.myweb.website_core.infrastructure.config.properties.FileUploadConfig;
import com.myweb.website_core.domain.business.entity.Image;
import com.myweb.website_core.common.exception.FileUploadException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传服务类
 * 
 * 提供文件上传的核心功能：
 * - 文件类型验证
 * - 文件大小验证
 * - 文件存储
 * - 生成访问URL
 */
@Service
public class FileUploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    
    private final FileUploadConfig fileUploadConfig;
    private final ImageService imageService;
    private final FileUploadSecurityService fileUploadSecurityService;
    private final DataIntegrityService dataIntegrityService;
    private final VirusScanService virusScanService;
    private final FileSecurityMonitoringService fileSecurityMonitoringService;
    
    @Autowired
    public FileUploadService(FileUploadConfig fileUploadConfig, ImageService imageService,
                           FileUploadSecurityService fileUploadSecurityService,
                           DataIntegrityService dataIntegrityService,
                           VirusScanService virusScanService,
                           FileSecurityMonitoringService fileSecurityMonitoringService) {
        this.fileUploadConfig = fileUploadConfig;
        this.imageService = imageService;
        this.fileUploadSecurityService = fileUploadSecurityService;
        this.dataIntegrityService = dataIntegrityService;
        this.virusScanService = virusScanService;
        this.fileSecurityMonitoringService = fileSecurityMonitoringService;
    }

    /**
     * 上传单个图片文件（增强安全版本）
     * 
     * 集成了以下安全功能：
     * - FileUploadSecurityService：文件类型、大小、魔数验证
     * - 恶意代码检查：扫描文件内容中的恶意模式
     * - 病毒扫描：使用VirusScanService进行病毒检测
     * - 文件哈希计算：使用DataIntegrityService计算并存储文件哈希
     * - 完整的审计日志记录
     * 
     * @param file 上传的文件
     * @param postId 关联的帖子ID（可选）
     * @return 文件访问URL
     * @throws FileUploadException 上传失败时抛出异常
     */
    public String uploadImage(MultipartFile file, Long postId) throws FileUploadException {
        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        Long userId = PermissionUtils.getCurrentUserId();
        String originalFilename = file != null ? file.getOriginalFilename() : "unknown";
        
        logger.info("开始增强安全文件上传: user={}, filename={}, size={}, postId={}", 
                   username, originalFilename, file != null ? file.getSize() : 0, postId);
        
        try {
            // ==================== 第1步：基础安全验证 ====================
            
            // 获取HTTP请求对象用于安全验证
            HttpServletRequest request = getCurrentRequest();
            
            // 使用FileUploadSecurityService进行全面的文件安全验证
            // 包括：文件类型验证、大小验证、魔数验证、恶意代码扫描
            logger.debug("执行文件安全验证: {}", originalFilename);
            fileUploadSecurityService.validateUploadedFile(file, userId, username, request);
            logger.debug("文件安全验证通过: {}", originalFilename);
            
            // ==================== 第2步：病毒扫描 ====================
            
            // 执行病毒扫描
            logger.debug("开始病毒扫描: {}", originalFilename);
            CompletableFuture<VirusScanResult> scanFuture = virusScanService.scanFile(file, userId, username);
            
            // 等待扫描结果（设置超时时间30秒）
            VirusScanResult scanResult;
            try {
                scanResult = scanFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                logger.warn("病毒扫描超时: user={}, filename={}", username, originalFilename);
                throw new FileUploadException("文件安全扫描超时，请稍后重试");
            }
            
            // 检查扫描结果
            if (scanResult.shouldBlockUpload()) {
                if (scanResult.isVirusFound()) {
                    logger.warn("检测到病毒文件: user={}, filename={}, virus={}", 
                               username, originalFilename, scanResult.getVirusName());
                    throw new FileUploadException(
                        String.format("检测到恶意文件: %s (威胁级别: %s)", 
                                     scanResult.getVirusName(), scanResult.getThreatLevel()));
                } else {
                    logger.warn("病毒扫描失败: user={}, filename={}, error={}", 
                               username, originalFilename, scanResult.getErrorMessage());
                    throw new FileUploadException("文件安全扫描失败: " + scanResult.getErrorMessage());
                }
            }
            
            logger.debug("病毒扫描通过: user={}, filename={}, duration={}ms", 
                        username, originalFilename, scanResult.getScanDurationMs());
            
            // ==================== 第3步：文件存储 ====================
            
            // 创建上传目录
            String uploadPath = createUploadDirectory();
            
            // 生成安全的唯一文件名
            String fileName = fileUploadSecurityService.generateSecureFileName(originalFilename, userId);
            
            // 保存文件到磁盘
            Path filePath = Paths.get(uploadPath, fileName);
            Files.copy(file.getInputStream(), filePath);
            logger.debug("文件保存成功: {}", filePath.toString());
            
            // ==================== 第4步：文件哈希计算 ====================
            
            // 使用DataIntegrityService计算文件哈希值用于完整性检查
            logger.debug("计算文件哈希: {}", originalFilename);
            byte[] fileBytes = file.getBytes();
            String fileContent = new String(fileBytes, "UTF-8");
            String fileHash = dataIntegrityService.calculateHash(fileContent);
            logger.debug("文件哈希计算完成: hash={}", fileHash.substring(0, Math.min(fileHash.length(), 16)) + "...");
            
            // ==================== 第5步：数据库存储 ====================
            
            // 保存图片信息到数据库（包含文件哈希）
            Image image = imageService.saveImageWithHash(
                originalFilename,
                fileName,
                filePath.toString(),
                file.getContentType(),
                file.getSize(),
                fileHash,
                postId
            );
            
            // ==================== 第6步：文件安全监控 ====================
            
            // 获取客户端IP地址
            String sourceIp = getClientIpAddress(request);
            
            // 异步执行文件安全监控
            logger.debug("启动文件安全监控: imageId={}, filename={}", image.getId(), originalFilename);
            CompletableFuture<FileSecurityMonitoringService.FileSecurityCheckResult> monitoringFuture = 
                fileSecurityMonitoringService.monitorFileUpload(
                    image.getId(),
                    originalFilename,
                    file.getSize(),
                    file.getContentType(),
                    filePath.toString(),
                    username,
                    sourceIp
                );
            
            // 异步处理监控结果（不阻塞上传流程）
            monitoringFuture.whenComplete((monitoringResult, throwable) -> {
                if (throwable != null) {
                    logger.warn("文件安全监控异常: imageId={}, error={}", image.getId(), throwable.getMessage());
                } else if (monitoringResult != null) {
                    logger.debug("文件安全监控完成: imageId={}, securityLevel={}, riskScore={}", 
                               image.getId(), monitoringResult.getSecurityLevel(), monitoringResult.getRiskScore());
                    
                    if (monitoringResult.isThreatDetected()) {
                        logger.warn("文件安全监控检测到威胁: imageId={}, threats={}", 
                                   image.getId(), monitoringResult.getThreats());
                    }
                }
            });
            
            // 返回基于ID的访问URL
            String fileUrl = "/blog/api/images/" + image.getId();
            
            // ==================== 第6步：成功日志记录 ====================
            
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "UPLOAD_IMAGE_ENHANCED", username, "FILE:" + image.getId(), "SUCCESS", 
                String.format("文件名: %s, 大小: %d bytes, 哈希: %s, 病毒扫描: %s", 
                             originalFilename, file.getSize(), 
                             fileHash.substring(0, Math.min(fileHash.length(), 8)),
                             scanResult.getSummary()), 
                executionTime);
            logger.info(logMessage);
            
            logger.info("增强安全文件上传成功: user={}, filename={}, imageId={}, url={}, duration={}ms", 
                       username, originalFilename, image.getId(), fileUrl, executionTime);
            
            return fileUrl;
            
        } catch (FileUploadException e) {
            // 重新抛出文件上传异常
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_UPLOAD_SECURITY_ERROR", e.getMessage(), username, 
                null, "上传文件: " + originalFilename, null);
            logger.error(errorLog);
            throw e;
            
        } catch (Exception e) {
            // 记录其他系统异常
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_UPLOAD_SYSTEM_ERROR", e.getMessage(), username, 
                null, "上传文件: " + originalFilename, null);
            logger.error(errorLog, e);
            throw new FileUploadException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传单个图片文件（基础版本，仅基本验证）
     * 
     * @param file 上传的文件
     * @param postId 关联的帖子ID（可选）
     * @return 文件访问URL
     * @throws FileUploadException 上传失败时抛出异常
     */
    public String uploadImageBasic(MultipartFile file, Long postId) throws FileUploadException {
        // 基本文件验证
        validateFile(file);
        
        try {
            // 创建上传目录
            String uploadPath = createUploadDirectory();
            
            // 生成唯一文件名
            String fileName = generateUniqueFileName(file.getOriginalFilename());
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, fileName);
            Files.copy(file.getInputStream(), filePath);
            
            // 保存图片信息到数据库
            Image image = imageService.saveImage(
                file.getOriginalFilename(),
                fileName,
                filePath.toString(),
                file.getContentType(),
                file.getSize(),
                postId
            );
            
            // 返回基于ID的访问URL
            String fileUrl = "/blog/api/images/" + image.getId();
            logger.info("文件上传成功: {} (ID: {}, PostID: {})", fileUrl, image.getId(), postId);
            
            return fileUrl;
            
        } catch (IOException e) {
            logger.error("文件上传失败", e);
            throw new FileUploadException("文件上传失败: " + e.getMessage());
        }
    }
    


    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) throws FileUploadException {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("文件不能为空");
        }
        
        // 验证文件大小
        if (file.getSize() > fileUploadConfig.getMaxFileSize()) {
            throw new FileUploadException("文件大小不能超过 " + 
                (fileUploadConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(fileUploadConfig.getAllowedTypes()).contains(contentType)) {
            throw new FileUploadException("不支持的文件类型，仅支持图片格式");
        }
        
        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new FileUploadException("文件名不能为空");
        }
        
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
            throw new FileUploadException("不支持的文件扩展名");
        }
    }
    
    /**
     * 创建上传目录
     */
    private String createUploadDirectory() throws IOException {
        String datePath = getDatePath();
        String fullPath = fileUploadConfig.getUploadDir() + datePath;
        
        Path uploadPath = Paths.get(fullPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        return fullPath;
    }
    
    /**
     * 获取日期路径 (yyyy/MM/dd)
     */
    private String getDatePath() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }
    
    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + "." + extension;
    }
    
    /**
     * 删除文件
     * 
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || !fileUrl.startsWith("/uploads/images/")) {
                return false;
            }
            
            // 从URL中提取文件路径
            String relativePath = fileUrl.substring("/uploads/images/".length());
            Path filePath = Paths.get(fileUploadConfig.getUploadDir(), relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("文件删除成功: {}", fileUrl);
                return true;
            }
            
        } catch (IOException e) {
            logger.error("文件删除失败: {}", fileUrl, e);
        }
        
        return false;
    }
    
    /**
     * 生成安全的文件存储路径
     * 
     * @param originalFilename 原始文件名
     * @param userId 用户ID
     * @return 安全的存储路径
     */
    public String generateSecureStoragePath(String originalFilename, Long userId) {
        return fileUploadSecurityService.generateSecureStoragePath(originalFilename, userId);
    }
    
    /**
     * 生成安全的文件名
     * 
     * @param originalFilename 原始文件名
     * @param userId 用户ID
     * @return 安全的文件名
     */
    public String generateSecureFileName(String originalFilename, Long userId) {
        return fileUploadSecurityService.generateSecureFileName(originalFilename, userId);
    }
    
    /**
     * 验证文件安全性（不上传）
     * 
     * @param file 要验证的文件
     * @param userId 用户ID
     * @param username 用户名
     * @return 验证是否通过
     */
    public boolean validateFileSecurity(MultipartFile file, Long userId, String username) {
        try {
            HttpServletRequest request = getCurrentRequest();
            fileUploadSecurityService.validateUploadedFile(file, userId, username, request);
            return true;
        } catch (Exception e) {
            logger.debug("文件安全验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证文件完整性
     * 
     * 在文件下载或访问时验证文件是否被篡改
     * 
     * @param imageId 图片ID
     * @return 完整性验证结果
     */
    public boolean verifyFileIntegrity(Long imageId) {
        try {
            // 获取图片信息
            var imageOpt = imageService.getImageById(imageId);
            if (imageOpt.isEmpty()) {
                logger.warn("文件完整性验证失败: 图片不存在, imageId={}", imageId);
                return false;
            }
            
            Image image = imageOpt.get();
            String storedHash = image.getFileHash();
            
            // 如果没有存储哈希值，跳过验证
            if (storedHash == null || storedHash.trim().isEmpty()) {
                logger.debug("跳过文件完整性验证: 未存储哈希值, imageId={}", imageId);
                return true;
            }
            
            // 读取当前文件内容
            Path filePath = Paths.get(image.getFilePath());
            if (!Files.exists(filePath)) {
                logger.warn("文件完整性验证失败: 文件不存在, path={}", filePath);
                return false;
            }
            
            // 读取文件内容并计算哈希
            byte[] fileBytes = Files.readAllBytes(filePath);
            String fileContent = new String(fileBytes, "UTF-8");
            String currentHash = dataIntegrityService.calculateHash(fileContent);
            
            // 验证完整性
            boolean isValid = dataIntegrityService.verifyIntegrity(fileContent, storedHash);
            
            if (!isValid) {
                logger.warn("文件完整性验证失败: 哈希值不匹配, imageId={}, stored={}, current={}", 
                           imageId, storedHash.substring(0, 8), currentHash.substring(0, 8));
                
                // 记录安全事件
                String logMessage = LoggingUtils.formatErrorLog(
                    "FILE_INTEGRITY_VIOLATION", 
                    "文件完整性验证失败，可能已被篡改",
                    PermissionUtils.getCurrentUsername(),
                    null,
                    "FILE:" + imageId,
                    null
                );
                logger.error(logMessage);
            } else {
                logger.debug("文件完整性验证通过: imageId={}", imageId);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("文件完整性验证异常: imageId={}, error={}", imageId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 批量验证文件完整性
     * 
     * @param imageIds 图片ID列表
     * @return 验证结果映射（imageId -> 是否通过验证）
     */
    public java.util.Map<Long, Boolean> batchVerifyFileIntegrity(java.util.List<Long> imageIds) {
        java.util.Map<Long, Boolean> results = new java.util.HashMap<>();
        
        for (Long imageId : imageIds) {
            try {
                boolean isValid = verifyFileIntegrity(imageId);
                results.put(imageId, isValid);
            } catch (Exception e) {
                logger.error("批量文件完整性验证异常: imageId={}, error={}", imageId, e.getMessage());
                results.put(imageId, false);
            }
        }
        
        return results;
    }
    
    /**
     * 重新计算并更新文件哈希
     * 
     * 用于修复或更新已存在文件的哈希值
     * 
     * @param imageId 图片ID
     * @return 是否更新成功
     */
    public boolean recalculateFileHash(Long imageId) {
        try {
            // 获取图片信息
            var imageOpt = imageService.getImageById(imageId);
            if (imageOpt.isEmpty()) {
                logger.warn("重新计算文件哈希失败: 图片不存在, imageId={}", imageId);
                return false;
            }
            
            Image image = imageOpt.get();
            Path filePath = Paths.get(image.getFilePath());
            
            if (!Files.exists(filePath)) {
                logger.warn("重新计算文件哈希失败: 文件不存在, path={}", filePath);
                return false;
            }
            
            // 读取文件内容并计算新哈希
            byte[] fileBytes = Files.readAllBytes(filePath);
            String fileContent = new String(fileBytes, "UTF-8");
            String newHash = dataIntegrityService.calculateHash(fileContent);
            
            // 更新数据库中的哈希值
            image.setFileHash(newHash);
            image.setHashCalculatedAt(LocalDateTime.now());
            imageService.saveImageWithHash(
                image.getOriginalFilename(),
                image.getStoredFilename(),
                image.getFilePath(),
                image.getContentType(),
                image.getFileSize(),
                newHash,
                image.getPost() != null ? image.getPost().getId() : null
            );
            
            logger.info("文件哈希重新计算成功: imageId={}, newHash={}", 
                       imageId, newHash.substring(0, 8) + "...");
            
            return true;
            
        } catch (Exception e) {
            logger.error("重新计算文件哈希异常: imageId={}, error={}", imageId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取当前HTTP请求对象
     * 
     * @return HttpServletRequest对象，如果获取失败则返回null
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            logger.debug("获取当前HTTP请求失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }
}