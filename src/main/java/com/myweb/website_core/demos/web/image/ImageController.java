package com.myweb.website_core.demos.web.image;

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
    
    @Autowired
    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }
    
    /**
     * 根据ID获取图片文件
     * 
     * @param id 图片ID
     * @return 图片文件资源
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        try {
            Optional<Image> imageOpt = imageService.getImageById(id);
            if (imageOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Image image = imageOpt.get();
            Resource resource = imageService.getImageResource(id);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            headers.setContentLength(image.getFileSize());
            headers.setCacheControl("max-age=3600"); // 缓存1小时
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (IOException e) {
            logger.error("获取图片失败: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}