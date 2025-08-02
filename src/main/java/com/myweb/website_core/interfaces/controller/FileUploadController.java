package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.domain.dto.ApiResponse;
import com.myweb.website_core.common.exception.FileUploadException;
import com.myweb.website_core.application.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * 提供图片上传相关的API接口：
 * - 单图片上传
 * - 多图片批量上传
 * - 图片删除
 */
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    
    private final FileUploadService fileUploadService;
    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 上传单张图片
     * 
     * @param file 图片文件
     * @param postId 关联的帖子ID（可选）
     * @return 上传结果，包含图片URL
     */
    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId",required = false) Long postId) {
        try {
            String imageUrl = fileUploadService.uploadImage(file, postId);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("filename", file.getOriginalFilename());
            if (postId != null) {
                result.put("postId", postId.toString());
            }
            
            return ResponseEntity.ok(
                ApiResponse.success(result)
            );
            
        } catch (FileUploadException e) {
            logger.warn("图片上传失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
            
        } catch (Exception e) {
            logger.error("图片上传异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("服务器内部错误")
            );
        }
    }
    
    /**
     * 批量上传多张图片
     * 
     * @param files 图片文件数组
     * @param postId 关联的帖子ID（可选）
     * @return 上传结果，包含所有图片URL列表
     */
    @PostMapping("/images")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "postId") Long postId) {
        
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("请选择要上传的文件")
            );
        }
        
        if (files.length > 6) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("最多只能上传6张图片")
            );
        }
        
        List<Map<String, String>> successList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
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
                logger.warn("图片上传失败: {} - {}", file.getOriginalFilename(), e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", successList);
        result.put("errors", errorList);
        result.put("successCount", successList.size());
        result.put("errorCount", errorList.size());
        
        if (successList.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("所有图片上传失败")
            );
        } else if (!errorList.isEmpty()) {
            return ResponseEntity.ok(
                ApiResponse.error("部分图片上传成功")
            );
        } else {
            return ResponseEntity.ok(
                ApiResponse.success(result)
            );
        }
    }
    
    /**
     * 删除图片
     * 
     * @param imageUrl 图片URL
     * @return 删除结果
     */
    @DeleteMapping("/image")
    public ResponseEntity<ApiResponse<String>> deleteImage(@RequestParam("url") String imageUrl) {
        try {
            boolean deleted = fileUploadService.deleteFile(imageUrl);
            
            if (deleted) {
                return ResponseEntity.ok(
                    ApiResponse.success("图片删除成功")
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("图片删除失败，文件不存在")
                );
            }
            
        } catch (Exception e) {
            logger.error("图片删除异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("服务器内部错误")
            );
        }
    }
}