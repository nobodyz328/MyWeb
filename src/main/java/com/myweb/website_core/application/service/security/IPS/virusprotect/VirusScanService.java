package com.myweb.website_core.application.service.security.IPS.virusprotect;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * 病毒扫描服务接口
 * 
 * 提供文件病毒扫描功能，支持多种扫描引擎：
 * - ClamAV 扫描引擎
 * - 模拟扫描引擎（用于开发和测试）
 * - 可扩展支持其他扫描引擎
 * 
 * 符合GB/T 22239-2019二级等保要求的恶意代码防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public interface VirusScanService {
    
    /**
     * 扫描上传的文件
     * 
     * @param file 要扫描的文件
     * @param userId 用户ID
     * @param username 用户名
     * @return 扫描结果
     */
    CompletableFuture<VirusScanResult> scanFile(MultipartFile file, Long userId, String username);
    
    /**
     * 扫描输入流
     * 
     * @param inputStream 要扫描的输入流
     * @param filename 文件名
     * @param userId 用户ID
     * @param username 用户名
     * @return 扫描结果
     */
    CompletableFuture<VirusScanResult> scanInputStream(InputStream inputStream, String filename, 
                                                      Long userId, String username);
    
    /**
     * 扫描字节数组
     * 
     * @param fileBytes 要扫描的字节数组
     * @param filename 文件名
     * @param userId 用户ID
     * @param username 用户名
     * @return 扫描结果
     */
    CompletableFuture<VirusScanResult> scanBytes(byte[] fileBytes, String filename, 
                                                Long userId, String username);
    
    /**
     * 检查扫描引擎是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取扫描引擎信息
     * 
     * @return 引擎信息
     */
    String getEngineInfo();
    
    /**
     * 更新病毒库
     * 
     * @return 更新结果
     */
    CompletableFuture<Boolean> updateVirusDatabase();
}