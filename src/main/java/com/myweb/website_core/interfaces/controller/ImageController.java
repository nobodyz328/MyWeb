package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.file.ImageService;
import com.myweb.website_core.application.service.file.FileUploadService;
import com.myweb.website_core.domain.business.entity.Image;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.PermissionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

/**
 * 图片访问控制器
 * 
 * 提供图片访问的API接口：
 * - 根据ID获取图片
 * - 图片信息查询
 */
@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    
    private final ImageService imageService;
    private final FileUploadService fileUploadService;
    
    @Autowired
    public ImageController(ImageService imageService, FileUploadService fileUploadService) {
        this.imageService = imageService;
        this.fileUploadService = fileUploadService;
    }
    
    /**
     * 根据ID获取图片文件（增强版本 - 包含文件完整性验证）
     * 
     * @param id 图片ID
     * @return 图片文件资源
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            Optional<Image> imageOpt = imageService.getImageById(id);
            if (imageOpt.isEmpty()) {
                logger.warn("图片不存在: id={}, user={}", id, username);
                return ResponseEntity.notFound().build();
            }
            
            Image image = imageOpt.get();
            
            // ==================== 文件完整性验证 ====================
            logger.debug("开始文件完整性验证: imageId={}, user={}", id, username);
            boolean integrityValid = fileUploadService.verifyFileIntegrity(id);
            
            if (!integrityValid) {
                // 文件完整性验证失败，记录安全事件并返回错误
                long executionTime = System.currentTimeMillis() - startTime;
                String errorLog = LoggingUtils.formatErrorLog(
                    "FILE_INTEGRITY_VIOLATION_DOWNLOAD", 
                    "文件下载时完整性验证失败，可能已被篡改",
                    username,
                    null,
                    "FILE:" + id,
                    null
                );
                logger.error(errorLog);
                
                // 返回410 Gone状态码，表示文件已损坏
                return ResponseEntity.status(HttpStatus.GONE)
                    .header("X-Integrity-Status", "FAILED")
                    .header("X-Error-Message", "文件完整性验证失败")
                    .build();
            }
            
            logger.debug("文件完整性验证通过: imageId={}, user={}", id, username);
            
            // ==================== 获取文件资源 ====================
            Resource resource = imageService.getImageResource(id);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            headers.setContentLength(image.getFileSize());
            headers.setCacheControl("max-age=3600"); // 缓存1小时
            headers.set("X-Integrity-Status", "VERIFIED"); // 添加完整性验证状态头
            headers.set("X-File-Hash", image.getFileHash() != null ? 
                       image.getFileHash().substring(0, 8) + "..." : "N/A");
            
            // 记录成功访问日志
            long executionTime = System.currentTimeMillis() - startTime;
            String successLog = LoggingUtils.formatOperationLog(
                "FILE_DOWNLOAD_WITH_INTEGRITY_CHECK", username, "FILE:" + id, "SUCCESS",
                String.format("文件: %s, 大小: %d bytes, 完整性: 已验证", 
                             image.getOriginalFilename(), image.getFileSize()),
                executionTime
            );
            logger.info(successLog);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (IOException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_DOWNLOAD_IO_ERROR", e.getMessage(), username, 
                null, "FILE:" + id, null);
            logger.error(errorLog, e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Type", "IO_ERROR")
                .build();
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_DOWNLOAD_SYSTEM_ERROR", e.getMessage(), username, 
                null, "FILE:" + id, null);
            logger.error(errorLog, e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Type", "SYSTEM_ERROR")
                .build();
        }
    }
    
    /**
     * 获取图片信息
     * 
     * @param id 图片ID
     * @return 图片信息
     */
    @GetMapping("/{id}/info")
    public ResponseEntity<Image> getImageInfo(@PathVariable Long id) {
        Optional<Image> imageOpt = imageService.getImageById(id);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(imageOpt.get());
    }
    
    /**
     * 验证单个文件的完整性
     * 
     * @param id 图片ID
     * @return 完整性验证结果
     */
    @GetMapping("/{id}/integrity")
    public ResponseEntity<java.util.Map<String, Object>> verifyFileIntegrity(@PathVariable Long id) {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            Optional<Image> imageOpt = imageService.getImageById(id);
            if (imageOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Image image = imageOpt.get();
            boolean isValid = fileUploadService.verifyFileIntegrity(id);
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("imageId", id);
            result.put("filename", image.getOriginalFilename());
            result.put("integrityValid", isValid);
            result.put("fileHash", image.getFileHash());
            result.put("hashCalculatedAt", image.getHashCalculatedAt());
            result.put("checkTime", java.time.LocalDateTime.now());
            result.put("status", isValid ? "VALID" : "CORRUPTED");
            
            // 记录验证操作
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_INTEGRITY_MANUAL_CHECK", username, "FILE:" + id, 
                isValid ? "SUCCESS" : "FAILED",
                String.format("文件: %s, 完整性: %s", image.getOriginalFilename(), 
                             isValid ? "有效" : "已损坏"),
                executionTime
            );
            logger.info(logMessage);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("文件完整性验证失败: imageId={}, user={}, error={}", id, username, e.getMessage(), e);
            
            java.util.Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("imageId", id);
            errorResult.put("integrityValid", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("status", "ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * 批量验证文件完整性
     * 
     * @param ids 图片ID列表（逗号分隔）
     * @return 批量验证结果
     */
    @GetMapping("/integrity/batch")
    public ResponseEntity<java.util.Map<String, Object>> batchVerifyFileIntegrity(
            @RequestParam("ids") String ids) {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            // 解析ID列表
            java.util.List<Long> imageIds = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
            
            if (imageIds.size() > 100) {
                return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "批量验证最多支持100个文件"));
            }
            
            // 执行批量验证
            java.util.Map<Long, Boolean> verificationResults = 
                fileUploadService.batchVerifyFileIntegrity(imageIds);
            
            // 统计结果
            long validCount = verificationResults.values().stream()
                .mapToLong(valid -> valid ? 1 : 0).sum();
            long invalidCount = verificationResults.size() - validCount;
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("totalFiles", verificationResults.size());
            result.put("validFiles", validCount);
            result.put("invalidFiles", invalidCount);
            result.put("results", verificationResults);
            result.put("checkTime", java.time.LocalDateTime.now());
            result.put("overallStatus", invalidCount == 0 ? "ALL_VALID" : "SOME_CORRUPTED");
            
            // 记录批量验证操作
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_INTEGRITY_BATCH_CHECK", username, "FILES:" + verificationResults.size(), 
                "COMPLETED",
                String.format("总计: %d, 有效: %d, 损坏: %d", 
                             verificationResults.size(), validCount, invalidCount),
                executionTime
            );
            logger.info(logMessage);
            
            return ResponseEntity.ok(result);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                java.util.Map.of("error", "无效的ID格式"));
        } catch (Exception e) {
            logger.error("批量文件完整性验证失败: user={}, error={}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                java.util.Map.of("error", "批量验证失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重新计算文件哈希值
     * 
     * @param id 图片ID
     * @return 重新计算结果
     */
    @PostMapping("/{id}/recalculate-hash")
    public ResponseEntity<java.util.Map<String, Object>> recalculateFileHash(@PathVariable Long id) {
        String username = PermissionUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();
        
        try {
            boolean success = fileUploadService.recalculateFileHash(id);
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("imageId", id);
            result.put("success", success);
            result.put("recalculatedAt", java.time.LocalDateTime.now());
            
            if (success) {
                // 获取更新后的图片信息
                Optional<Image> imageOpt = imageService.getImageById(id);
                if (imageOpt.isPresent()) {
                    Image image = imageOpt.get();
                    result.put("newHash", image.getFileHash());
                    result.put("filename", image.getOriginalFilename());
                }
            }
            
            // 记录重新计算操作
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FILE_HASH_RECALCULATION", username, "FILE:" + id, 
                success ? "SUCCESS" : "FAILED",
                "重新计算文件哈希值",
                executionTime
            );
            logger.info(logMessage);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("重新计算文件哈希失败: imageId={}, user={}, error={}", id, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                java.util.Map.of("imageId", id, "success", false, "error", e.getMessage()));
        }
    }
}