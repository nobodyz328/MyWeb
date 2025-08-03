package com.myweb.website_core.application.service.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 图片清理服务
 * 
 * 定期清理未关联到帖子的图片文件
 */
@Service
public class ImageCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageCleanupService.class);
    
    private final ImageService imageService;
    
    @Autowired
    public ImageCleanupService(ImageService imageService) {
        this.imageService = imageService;
    }
    
    /**
     * 每天凌晨2点执行清理任务
     * 清理超过24小时未关联的图片
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupUnassociatedImages() {
        logger.info("开始清理未关联的图片...");
        
        try {
            // 清理超过24小时未关联的图片
            imageService.cleanupUnassociatedImages(24);
            logger.info("图片清理任务完成");
            
        } catch (Exception e) {
            logger.error("图片清理任务失败", e);
        }
    }
    
    /**
     * 手动触发清理任务（用于测试）
     */
    public void manualCleanup() {
        logger.info("手动触发图片清理任务...");
        cleanupUnassociatedImages();
    }
}