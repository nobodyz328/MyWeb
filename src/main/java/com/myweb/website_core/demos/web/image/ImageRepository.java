package com.myweb.website_core.demos.web.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 图片数据访问接口
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    /**
     * 根据存储文件名查找图片
     */
    Optional<Image> findByStoredFilename(String storedFilename);
    
    /**
     * 根据帖子ID查找所有图片
     */
    List<Image> findByPostId(Long postId);
    
    /**
     * 根据文件路径查找图片
     */
    Optional<Image> findByFilePath(String filePath);
    
    /**
     * 查找所有未关联帖子的图片
     */
    List<Image> findByPostIsNull();
}