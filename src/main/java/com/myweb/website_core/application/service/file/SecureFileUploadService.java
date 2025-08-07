package com.myweb.website_core.application.service.file;

import com.myweb.website_core.application.service.security.fileProtect.FileUploadSecurityService;
import com.myweb.website_core.common.exception.FileUploadException;
import com.myweb.website_core.common.exception.FileValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 安全文件上传服务
 * <p>
 * 集成FileUploadSecurityService和FileUploadService，
 * 提供完整的安全文件上传功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecureFileUploadService {
    
    private final FileUploadSecurityService fileUploadSecurityService;
    private final FileUploadService fileUploadService;
    
    /**
     * 安全上传图片文件
     * 
     * @param file 上传的文件
     * @param postId 关联的帖子ID（可选）
     * @param userId 用户ID
     * @param username 用户名
     * @param request HTTP请求对象
     * @return 文件访问URL
     * @throws FileUploadException 上传失败时抛出异常
     * @throws FileValidationException 安全验证失败时抛出异常
     */
    public String uploadImageSecurely(MultipartFile file, Long postId, Long userId, 
                                    String username, HttpServletRequest request) 
            throws FileUploadException, FileValidationException {
        
        log.info("开始安全文件上传: user={}, filename={}, postId={}", 
                username, file.getOriginalFilename(), postId);
        
        try {
            // 1. 安全验证
            fileUploadSecurityService.validateUploadedFile(file, userId, username, request);
            
            // 2. 执行上传
            String fileUrl = fileUploadService.uploadImage(file, postId);
            
            log.info("安全文件上传成功: user={}, filename={}, url={}", 
                    username, file.getOriginalFilename(), fileUrl);
            
            return fileUrl;
            
        } catch (FileValidationException e) {
            log.warn("文件安全验证失败: user={}, filename={}, error={}", 
                    username, file.getOriginalFilename(), e.getMessage());
            throw e;
            
        } catch (FileUploadException e) {
            log.error("文件上传失败: user={}, filename={}, error={}", 
                     username, file.getOriginalFilename(), e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("安全文件上传异常: user={}, filename={}, error={}", 
                     username, file.getOriginalFilename(), e.getMessage(), e);
            throw new FileUploadException("文件上传失败: " + e.getMessage());
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
}