package com.myweb.website_core.demos.web.image;

import com.myweb.website_core.config.FileUploadConfig;
import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 图片服务类
 * 
 * 提供图片相关的业务逻辑：
 * - 保存图片信息到数据库
 * - 根据ID获取图片
 * - 获取图片文件资源
 */
@Service
public class ImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    
    private final ImageRepository imageRepository;
    private final FileUploadConfig fileUploadConfig;
    private final PostRepository postRepository;
    
    @Autowired
    public ImageService(ImageRepository imageRepository, FileUploadConfig fileUploadConfig, PostRepository postRepository) {
        this.imageRepository = imageRepository;
        this.fileUploadConfig = fileUploadConfig;
        this.postRepository = postRepository;
    }
    
    /**
     * 保存图片信息到数据库
     */
    public Image saveImage(String originalFilename, String storedFilename, String filePath, 
                          String contentType, Long fileSize, Long postId) {
        Image image = new Image(originalFilename, storedFilename, filePath, contentType, fileSize);
        
        // 如果提供了postId，验证帖子是否存在并设置关联
        if (postId != null) {
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isPresent()) {
                image.setPost(postOpt.get());
                logger.info("图片关联到帖子: postId={}", postId);
            } else {
                logger.warn("指定的帖子不存在: postId={}", postId);
                // 可以选择抛出异常或者继续保存图片但不关联帖子
                // 这里选择继续保存但记录警告
            }
        }
        
        return imageRepository.save(image);
    }
    
    /**
     * 保存图片信息到数据库（兼容旧版本，不关联帖子）
     */
    public Image saveImage(String originalFilename, String storedFilename, String filePath, 
                          String contentType, Long fileSize) {
        return saveImage(originalFilename, storedFilename, filePath, contentType, fileSize, null);
    }
    
    /**
     * 根据ID获取图片信息
     */
    public Optional<Image> getImageById(Long id) {
        return imageRepository.findById(id);
    }
    
    /**
     * 根据ID获取图片文件资源
     */
    public Resource getImageResource(Long id) throws IOException {
        Optional<Image> imageOpt = imageRepository.findById(id);
        if (imageOpt.isEmpty()) {
            throw new IOException("图片不存在");
        }
        
        Image image = imageOpt.get();
        Path filePath = Paths.get(image.getFilePath());
        
        if (!Files.exists(filePath)) {
            throw new IOException("图片文件不存在");
        }
        
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("无法读取图片文件");
        }
    }
    
    /**
     * 根据存储文件名获取图片
     */
    public Optional<Image> getImageByStoredFilename(String storedFilename) {
        return imageRepository.findByStoredFilename(storedFilename);
    }
    
    /**
     * 根据帖子ID获取所有关联的图片
     */
    public List<Image> getImagesByPostId(Long postId) {
        return imageRepository.findByPostId(postId);
    }
    
    /**
     * 将已存在的图片关联到帖子
     */
    public void associateImageToPost(Long imageId, Post post) {
        try {
            logger.info("开始关联图片 {} 到帖子 {}", imageId, post.getId());
            
            Optional<Image> imageOpt = imageRepository.findById(imageId);
            if (imageOpt.isPresent()) {
                Image image = imageOpt.get();
                
                // 打印图片信息用于调试
                logger.info("找到图片: id={}, originalFilename={}, contentType={}, fileSize={}", 
                    image.getId(), image.getOriginalFilename(), image.getContentType(), image.getFileSize());
                
                // 设置关联
                image.setPost(post);
                
                // 保存更新
                Image savedImage = imageRepository.save(image);
                logger.info("成功关联图片 {} 到帖子 {}, 保存后的图片ID: {}", 
                    imageId, post.getId(), savedImage.getId());
                
            } else {
                logger.error("图片不存在: imageId={}", imageId);
                throw new RuntimeException("图片不存在: " + imageId);
            }
            
        } catch (Exception e) {
            logger.error("关联图片到帖子失败: imageId={}, postId={}, error={}", 
                imageId, post.getId(), e.getMessage(), e);
            throw new RuntimeException("关联图片失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有未关联帖子的图片（用于清理）
     */
    public List<Image> getUnassociatedImages() {
        return imageRepository.findByPostIsNull();
    }
    
    /**
     * 清理超过指定时间的未关联图片
     */
    public void cleanupUnassociatedImages(int hoursOld) {
        List<Image> unassociatedImages = imageRepository.findByPostIsNull();
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursOld);
        
        for (Image image : unassociatedImages) {
            if (image.getUploadTime().isBefore(cutoffTime)) {
                try {
                    // 删除文件
                    Path filePath = Paths.get(image.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                    
                    // 删除数据库记录
                    imageRepository.delete(image);
                    logger.info("清理未关联图片: {}", image.getId());
                    
                } catch (Exception e) {
                    logger.error("清理图片失败: {}", image.getId(), e);
                }
            }
        }
    }
    
    /**
     * 删除图片
     */
    public boolean deleteImage(Long id) {
        try {
            Optional<Image> imageOpt = imageRepository.findById(id);
            if (imageOpt.isEmpty()) {
                return false;
            }
            
            Image image = imageOpt.get();
            Path filePath = Paths.get(image.getFilePath());
            
            // 删除文件
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // 删除数据库记录
            imageRepository.delete(image);
            
            logger.info("图片删除成功: {}", id);
            return true;
            
        } catch (Exception e) {
            logger.error("删除图片失败: {}", id, e);
            return false;
        }
    }
}