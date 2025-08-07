package com.myweb.website_core.infrastructure.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * 文件上传配置类
 * 
 * 配置文件上传的相关参数：
 * - 最大文件大小
 * - 最大请求大小
 * - 上传目录路径
 * - 允许的文件类型
 */
@Getter
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadConfig {

    // Getters and Setters
    private String uploadDir = "uploads/images/";
    private long maxFileSize = 5 * 1024 * 1024; // 5MB
    private long maxRequestSize = 20 * 1024 * 1024; // 20MB
    private String[] allowedTypes = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public void setAllowedTypes(String[] allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
}