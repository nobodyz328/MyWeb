package com.myweb.website_core.application.task;

import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusQuarantineService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.VirusScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 病毒扫描维护任务
 * 
 * 定期执行病毒扫描相关的维护任务：
 * - 清理过期的隔离文件
 * - 更新病毒库
 * - 检查扫描引擎状态
 * - 生成维护报告
 * 
 * 符合GB/T 22239-2019二级等保要求的恶意代码防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.virus-scan.enabled", havingValue = "true", matchIfMissing = true)
public class VirusScanMaintenanceTask {
    
    private final VirusQuarantineService quarantineService;
    private final VirusScanService virusScanService;
    
    /**
     * 清理过期隔离文件
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredQuarantineFiles() {
        log.info("开始执行隔离文件清理任务...");
        
        try {
            CompletableFuture<Integer> cleanupFuture = quarantineService.cleanupExpiredQuarantineFiles();
            
            cleanupFuture.thenAccept(cleanedCount -> {
                if (cleanedCount > 0) {
                    log.info("隔离文件清理完成，清理数量: {}", cleanedCount);
                } else {
                    log.debug("隔离文件清理完成，无过期文件");
                }
            }).exceptionally(throwable -> {
                log.error("隔离文件清理任务异常: {}", throwable.getMessage(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("执行隔离文件清理任务失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新病毒库
     * 每天凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void updateVirusDatabase() {
        log.info("开始执行病毒库更新任务...");
        
        try {
            CompletableFuture<Boolean> updateFuture = virusScanService.updateVirusDatabase();
            
            updateFuture.thenAccept(success -> {
                if (success) {
                    log.info("病毒库更新完成");
                } else {
                    log.warn("病毒库更新失败");
                }
            }).exceptionally(throwable -> {
                log.error("病毒库更新任务异常: {}", throwable.getMessage(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("执行病毒库更新任务失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查扫描引擎状态
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkScanEngineStatus() {
        log.debug("检查病毒扫描引擎状态...");
        
        try {
            boolean available = virusScanService.isAvailable();
            String engineInfo = virusScanService.getEngineInfo();
            
            if (available) {
                log.debug("病毒扫描引擎状态正常: {}", engineInfo);
            } else {
                log.warn("病毒扫描引擎不可用: {}", engineInfo);
                // 这里可以添加告警逻辑
            }
            
        } catch (Exception e) {
            log.error("检查病毒扫描引擎状态失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 生成隔离统计报告
     * 每天上午9点执行
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void generateQuarantineReport() {
        log.info("生成隔离统计报告...");
        
        try {
            var statistics = quarantineService.getQuarantineStatistics();
            
            log.info("隔离统计报告:");
            log.info("- 隔离文件总数: {}", statistics.getTotalFiles());
            log.info("- 隔离文件总大小: {}", statistics.getFormattedSize());
            log.info("- 隔离目录: {}", statistics.getQuarantinePath());
            log.info("- 保留天数: {}", statistics.getRetentionDays());
            
            // 检查隔离空间使用情况
            if (statistics.getTotalFiles() > 1000) {
                log.warn("隔离文件数量较多，建议检查清理策略");
            }
            
            if (statistics.getTotalSizeBytes() > 1024 * 1024 * 1024) { // 1GB
                log.warn("隔离空间使用量较大，建议检查存储容量");
            }
            
        } catch (Exception e) {
            log.error("生成隔离统计报告失败: {}", e.getMessage(), e);
        }
    }
}