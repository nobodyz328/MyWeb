package com.myweb.website_core.application.service.file;

import com.myweb.website_core.application.service.security.integeration.FileUploadSecurityService;
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
    
    @Autowired
    public FileUploadService(FileUploadConfig fileUploadConfig, ImageService imageService,
                           FileUploadSecurityService fileUploadSecurityService) {
        this.fileUploadConfig = fileUploadConfig;
        this.imageService = imageService;
        this.fileUploadSecurityService = fileUploadSecurityService;
    }

    /**
     * 上传单个图片文件（安全版本）
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
        
        try {
            // 获取HTTP请求对象用于安全验证
            HttpServletRequest request = getCurrentRequest();
            
            // 使用安全服务进行全面的文件安全验证
            fileUploadSecurityService.validateUploadedFile(file, userId, username, request);
            
            // 创建上传目录
            String uploadPath = createUploadDirectory();
            
            // 生成安全的唯一文件名
            String fileName = fileUploadSecurityService.generateSecureFileName(file.getOriginalFilename(), userId);
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, fileName);
            Files.copy(file.getInputStream(), filePath);
            
            // 计算文件哈希值用于完整性检查
            String fileHash = SecurityUtils.calculateHash(new String(file.getBytes(), "UTF-8"));
            
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
            
            // 记录成功的操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "UPLOAD_IMAGE", username, "FILE:" + image.getId(), "SUCCESS", 
                "文件名: " + file.getOriginalFilename() + ", 大小: " + file.getSize() + " bytes", 
                executionTime);
            logger.info(logMessage);
            
            return fileUrl;
            
        } catch (Exception e) {
            // 记录其他异常
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_UPLOAD_SYSTEM_ERROR", e.getMessage(), username, 
                null, "上传文件: " + (file != null ? file.getOriginalFilename() : "unknown"), null);
            logger.error(errorLog);
            throw new FileUploadException("文件上传失败: " + e.getMessage());
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
}