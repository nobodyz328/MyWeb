package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.util.PermissionUtils;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.validation.ValidateInput;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.common.exception.FileUploadException;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.application.service.file.FileUploadService;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 文件上传控制器
 * 提供图片上传相关的API接口：
 * - 单图片上传
 * - 多图片批量上传
 * - 图片删除
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class FileUploadController {
    private final FileUploadService fileUploadService;
    private final InputValidationService inputValidationService;

    /**
     * 上传单张图片（安全版本）
     * 
     * @param file   图片文件
     * @param postId 关联的帖子ID（可选）
     * @return 上传结果，包含图片URL
     */
    @PostMapping("/image")
    @Auditable(operation = AuditOperation.FILE_UPLOAD, resourceType = "FILE", description = "安全上传图片")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId", required = false) Long postId) {
        
        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            // 1. 文件名验证
            if (file != null && file.getOriginalFilename() != null) {
                inputValidationService.validateFilename(file.getOriginalFilename());
                
                // 记录文件验证日志
                String validationLog = LoggingUtils.formatOperationLog(
                    "FILE_VALIDATION", username, "FILE:" + file.getOriginalFilename(), 
                    "SUCCESS", "文件名验证通过", System.currentTimeMillis() - startTime);
                log.debug(validationLog);
            }
            
            // 2. 文件类型和大小的预验证
            validateFilePreUpload(file, username);
            
            // 3. 上传参数验证
            if (postId != null && postId <= 0) {
                throw new ValidationException("帖子ID必须为正数", "postId", "INVALID_VALUE");
            }
            
            // 使用安全版本的上传方法（包含文件内容安全检查）
            String imageUrl = fileUploadService.uploadImage(file, postId);

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("filename", file.getOriginalFilename());
            if (postId != null) {
                result.put("postId", postId.toString());
            }

            // 记录成功的文件上传日志
            long executionTime = System.currentTimeMillis() - startTime;
            String successLog = LoggingUtils.formatOperationLog(
                "FILE_UPLOAD", username, "FILE:" + file.getOriginalFilename(), 
                "SUCCESS", "文件上传成功，URL: " + imageUrl, executionTime);
            log.info(successLog);
            
            return ResponseEntity.ok(
                    ApiResponse.success(result));

        } catch (ValidationException e) {
            // 记录验证失败的详细日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_VALIDATION_FAILED", e.getMessage(), username, 
                null, "文件: " + (file != null ? file.getOriginalFilename() : "unknown"), null);
            log.warn(errorLog);
            
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("文件验证失败: " + e.getMessage()));
                    
        } catch (FileUploadException e) {
            // 记录文件上传失败日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_UPLOAD_FAILED", e.getMessage(), username, 
                null, "文件: " + (file != null ? file.getOriginalFilename() : "unknown"), null);
            log.warn(errorLog);
            
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            // 记录系统异常日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_UPLOAD_SYSTEM_ERROR", e.getMessage(), username, 
                null, "文件: " + (file != null ? file.getOriginalFilename() : "unknown"), null);
            log.error(errorLog);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("服务器内部错误"));
        }
    }

    /**
     * 上传单张图片（基础版本，仅用于内部或特殊场景）
     * 
     * @param file   图片文件
     * @param postId 关联的帖子ID（可选）
     * @return 上传结果，包含图片URL
     */
    @PostMapping("/image/basic")
    @Auditable(operation = AuditOperation.FILE_UPLOAD, resourceType = "FILE", description = "基础上传图片")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImageBasic(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId", required = false) Long postId) {
        
        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            // 基础文件名验证
            if (file != null && file.getOriginalFilename() != null) {
                inputValidationService.validateFilename(file.getOriginalFilename());
            }
            
            // 基础参数验证
            if (postId != null && postId <= 0) {
                throw new ValidationException("帖子ID必须为正数", "postId", "INVALID_VALUE");
            }
            
            // 使用基础版本的上传方法（仅基本验证）
            String imageUrl = fileUploadService.uploadImageBasic(file, postId);

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("filename", file.getOriginalFilename());
            if (postId != null) {
                result.put("postId", postId.toString());
            }

            return ResponseEntity.ok(
                    ApiResponse.success(result));

        } catch (FileUploadException e) {
            log.warn("图片上传失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("图片上传异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("服务器内部错误"));
        }
    }

    /**
     * 批量上传多张图片
     * 
     * @param files  图片文件数组
     * @param postId 关联的帖子ID（可选）
     * @return 上传结果，包含所有图片URL列表
     */
    @PostMapping("/images")
    @Auditable(operation = AuditOperation.FILE_UPLOAD, resourceType = "FILE", description = "批量上传图片")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "postId") Long postId) {

        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            // 1. 基础参数验证
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("请选择要上传的文件"));
            }

            if (files.length > 6) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("最多只能上传6张图片"));
            }
            
            // 2. 上传参数验证
            if (postId != null && postId <= 0) {
                throw new ValidationException("帖子ID必须为正数", "postId", "INVALID_VALUE");
            }

            // 3. 预验证所有文件名
            for (MultipartFile file : files) {
                if (file != null && file.getOriginalFilename() != null) {
                    inputValidationService.validateFilename(file.getOriginalFilename());
                    validateFilePreUpload(file, username);
                }
            }

            List<Map<String, String>> successList = new ArrayList<>();
            List<String> errorList = new ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    // 使用安全版本的上传方法（包含文件内容安全检查）
                    String imageUrl = fileUploadService.uploadImage(file, postId);

                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("url", imageUrl);
                    fileInfo.put("filename", file.getOriginalFilename());
                    if (postId != null) {
                        fileInfo.put("postId", postId.toString());
                    }
                    successList.add(fileInfo);

                } catch (FileUploadException e) {
                    errorList.add(file.getOriginalFilename() + ": " + e.getMessage());
                    log.warn("图片上传失败: {} - {}", file.getOriginalFilename(), e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", successList);
            result.put("errors", errorList);
            result.put("successCount", successList.size());
            result.put("errorCount", errorList.size());

            // 记录批量上传结果日志
            long executionTime = System.currentTimeMillis() - startTime;
            String batchLog = LoggingUtils.formatOperationLog(
                "BATCH_FILE_UPLOAD", username, "FILES:" + files.length, 
                "COMPLETED", String.format("成功: %d, 失败: %d", successList.size(), errorList.size()), 
                executionTime);
            log.info(batchLog);

            if (successList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("所有图片上传失败"));
            } else if (!errorList.isEmpty()) {
                return ResponseEntity.ok(
                        ApiResponse.error("部分图片上传成功"));
            } else {
                return ResponseEntity.ok(
                        ApiResponse.success(result));
            }
            
        } catch (ValidationException e) {
            // 记录验证失败的详细日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "BATCH_FILE_VALIDATION_FAILED", e.getMessage(), username, 
                null, "文件数量: " + (files != null ? files.length : 0), null);
            log.warn(errorLog);
            
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("文件验证失败: " + e.getMessage()));
                    
        } catch (Exception e) {
            // 记录系统异常日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "BATCH_FILE_UPLOAD_SYSTEM_ERROR", e.getMessage(), username, 
                null, "文件数量: " + (files != null ? files.length : 0), null);
            log.error(errorLog);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("服务器内部错误"));
        }
    }

    /**
     * 删除图片
     * 
     * @param imageUrl 图片URL
     * @return 删除结果
     */
    @DeleteMapping("/image")
    @Auditable(operation = AuditOperation.FILE_UPLOAD, resourceType = "FILE", description = "删除图片")
    @ValidateInput(fieldNames = {"imageUrl"}, validationTypes = {"url"}, maxLength = 2048)
    public ResponseEntity<ApiResponse<String>> deleteImage(@RequestParam("url") String imageUrl) {
        
        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            // 额外的URL格式验证
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                throw new ValidationException("图片URL不能为空", "imageUrl", "REQUIRED");
            }
            
            // 验证URL格式和安全性
            if (!imageUrl.startsWith("/blog/api/images/") && !imageUrl.startsWith("/uploads/images/")) {
                throw new ValidationException("无效的图片URL格式", "imageUrl", "INVALID_FORMAT");
            }
            
            boolean deleted = fileUploadService.deleteFile(imageUrl);

            // 记录删除操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String operationLog = LoggingUtils.formatOperationLog(
                "FILE_DELETE", username, "FILE:" + imageUrl, 
                deleted ? "SUCCESS" : "FAILED", 
                deleted ? "图片删除成功" : "图片不存在", executionTime);
            log.info(operationLog);

            if (deleted) {
                return ResponseEntity.ok(
                        ApiResponse.success("图片删除成功"));
            } else {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("图片删除失败，文件不存在"));
            }

        } catch (ValidationException e) {
            // 记录验证失败日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_DELETE_VALIDATION_FAILED", e.getMessage(), username, 
                null, "URL: " + imageUrl, null);
            log.warn(errorLog);
            
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("URL验证失败: " + e.getMessage()));
                    
        } catch (Exception e) {
            // 记录系统异常日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_DELETE_SYSTEM_ERROR", e.getMessage(), username, 
                null, "URL: " + imageUrl, null);
            log.error(errorLog);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("服务器内部错误"));
        }
    }

    /**
     * 验证文件安全性
     * 
     * @param file 要验证的文件
     * @return 验证结果
     */
    @PostMapping("/validate")
    @Auditable(operation = AuditOperation.FILE_UPLOAD, resourceType = "FILE", description = "验证文件安全性")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateFile(
            @RequestParam("file") MultipartFile file) {
        
        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        Long userId = PermissionUtils.getCurrentUserId();
        
        try {
            // 1. 文件名验证
            if (file != null && file.getOriginalFilename() != null) {
                inputValidationService.validateFilename(file.getOriginalFilename());
            }
            
            // 2. 文件类型和大小的预验证
            validateFilePreUpload(file, username);
            
            // 3. 使用文件上传服务进行安全验证
            boolean isValid = fileUploadService.validateFileSecurity(file, userId, username);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("filename", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("contentType", file.getContentType());

            if (isValid) {
                result.put("message", "文件验证通过");
            } else {
                result.put("message", "文件验证失败，存在安全风险");
            }
            
            // 记录文件验证日志
            long executionTime = System.currentTimeMillis() - startTime;
            String validationLog = LoggingUtils.formatOperationLog(
                "FILE_SECURITY_VALIDATION", username, "FILE:" + file.getOriginalFilename(), 
                isValid ? "SUCCESS" : "FAILED", 
                isValid ? "文件安全验证通过" : "文件存在安全风险", executionTime);
            log.info(validationLog);

            return ResponseEntity.ok(
                    ApiResponse.success(result));

        } catch (ValidationException e) {
            // 记录验证失败的详细日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_VALIDATION_FAILED", e.getMessage(), username, 
                null, "文件: " + (file != null ? file.getOriginalFilename() : "unknown"), null);
            log.warn(errorLog);
            
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("文件验证失败: " + e.getMessage()));
                    
        } catch (Exception e) {
            // 记录系统异常日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "FILE_VALIDATION_SYSTEM_ERROR", e.getMessage(), username, 
                null, "文件: " + (file != null ? file.getOriginalFilename() : "unknown"), null);
            log.error(errorLog);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("文件验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取上传统计信息（管理员功能）
     */
    @GetMapping("/statistics")
    @Auditable(operation = AuditOperation.AUDIT_STATISTICS_QUERY, resourceType = "FILE", description = "查看上传统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUploadStatistics(
            @RequestParam(defaultValue = "7") Integer days,
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String username = PermissionUtils.getCurrentUsername();
        
        try {
            // 参数验证
            if (days == null || days <= 0 || days > 365) {
                throw new ValidationException("天数参数必须在1-365之间", "days", "INVALID_RANGE");
            }
            
            // 检查管理员权限
            if (!PermissionUtils.isAdmin()) {
                // 记录权限不足日志
                String permissionLog = LoggingUtils.formatErrorLog(
                    "INSUFFICIENT_PERMISSION", "非管理员尝试访问上传统计", username, 
                    null, "统计天数: " + days, null);
                log.warn(permissionLog);
                
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        ApiResponse.error("需要管理员权限"));
            }

            // 获取上传统计（这里需要实现统计服务）
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUploads", 0);
            statistics.put("totalSize", 0);
            statistics.put("successRate", 0.0);
            statistics.put("days", days);

            // 记录统计查询日志
            long executionTime = System.currentTimeMillis() - startTime;
            String statisticsLog = LoggingUtils.formatOperationLog(
                "UPLOAD_STATISTICS_QUERY", username, "STATISTICS", 
                "SUCCESS", "查询" + days + "天的上传统计", executionTime);
            log.info(statisticsLog);

            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (ValidationException e) {
            // 记录参数验证失败日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "STATISTICS_VALIDATION_FAILED", e.getMessage(), username, 
                null, "天数参数: " + days, null);
            log.warn(errorLog);
            
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("参数验证失败: " + e.getMessage()));
                    
        } catch (Exception e) {
            // 记录系统异常日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "STATISTICS_SYSTEM_ERROR", e.getMessage(), username, 
                null, "天数参数: " + days, null);
            log.error(errorLog);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("获取上传统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 文件上传前的预验证
     * 包含文件类型、大小、基本安全检查
     * 
     * @param file 要验证的文件
     * @param username 当前用户名
     * @throws ValidationException 验证失败时抛出
     */
    private void validateFilePreUpload(MultipartFile file, String username) throws ValidationException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("文件不能为空", "file", "REQUIRED");
        }
        
        // 1. 文件大小验证
        long maxFileSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxFileSize) {
            String errorMsg = String.format("文件大小不能超过 %dMB，当前文件大小: %.2fMB", 
                maxFileSize / 1024 / 1024, file.getSize() / 1024.0 / 1024.0);
            
            // 记录文件大小超限日志
            String sizeLog = LoggingUtils.formatErrorLog(
                "FILE_SIZE_EXCEEDED", errorMsg, username, 
                null, "文件: " + file.getOriginalFilename(), null);
            log.warn(sizeLog);
            
            throw new ValidationException(errorMsg, "fileSize", "SIZE_EXCEEDED");
        }
        
        // 2. 文件类型验证
        String contentType = file.getContentType();
        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        
        if (contentType == null || !Arrays.asList(allowedTypes).contains(contentType.toLowerCase())) {
            String errorMsg = "不支持的文件类型，仅支持图片格式 (JPEG, PNG, GIF, WebP)";
            
            // 记录不支持的文件类型日志
            String typeLog = LoggingUtils.formatErrorLog(
                "UNSUPPORTED_FILE_TYPE", errorMsg, username, 
                null, "文件: " + file.getOriginalFilename() + ", 类型: " + contentType, null);
            log.warn(typeLog);
            
            throw new ValidationException(errorMsg, "contentType", "UNSUPPORTED_TYPE");
        }
        
        // 3. 文件扩展名验证
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                extension = originalFilename.substring(lastDotIndex + 1).toLowerCase();
            }
            
            String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
            if (!Arrays.asList(allowedExtensions).contains(extension)) {
                String errorMsg = "不支持的文件扩展名，仅支持: " + String.join(", ", allowedExtensions);
                
                // 记录不支持的扩展名日志
                String extLog = LoggingUtils.formatErrorLog(
                    "UNSUPPORTED_FILE_EXTENSION", errorMsg, username, 
                    null, "文件: " + originalFilename + ", 扩展名: " + extension, null);
                log.warn(extLog);
                
                throw new ValidationException(errorMsg, "fileExtension", "UNSUPPORTED_EXTENSION");
            }
        }
        
        // 4. 文件内容基本检查（检查是否为空文件）
        if (file.getSize() == 0) {
            throw new ValidationException("文件内容为空", "fileContent", "EMPTY_FILE");
        }
        
        // 记录预验证成功日志
        String preValidationLog = LoggingUtils.formatOperationLog(
            "FILE_PRE_VALIDATION", username, "FILE:" + originalFilename, 
            "SUCCESS", String.format("文件预验证通过 - 大小: %.2fKB, 类型: %s", 
                file.getSize() / 1024.0, contentType), 0L);
        log.debug(preValidationLog);
    }
}