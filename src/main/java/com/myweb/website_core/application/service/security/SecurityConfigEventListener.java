package com.myweb.website_core.application.service.security;

import com.myweb.website_core.domain.security.dto.SecurityConfigChangeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 安全配置事件监听器
 * <p>
 * 监听配置变更事件，实现配置变更的实时生效机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityConfigEventListener {
    
    /**
     * 处理配置变更事件
     * 
     * @param changeEvent 配置变更事件
     */
    @Async
    @EventListener
    public void handleConfigChange(SecurityConfigChangeDTO changeEvent) {
        log.info("处理配置变更事件 - 类型: {}, 操作者: {}", 
            changeEvent.getConfigType(), changeEvent.getOperator());
        
        try {
            switch (changeEvent.getConfigType()) {
                case "security":
                    handleSecurityConfigChange(changeEvent);
                    break;
                case "jwt":
                    handleJwtConfigChange(changeEvent);
                    break;
                case "rateLimit":
                    handleRateLimitConfigChange(changeEvent);
                    break;
                case "backup":
                    handleBackupConfigChange(changeEvent);
                    break;
                default:
                    log.warn("未知配置类型变更事件: {}", changeEvent.getConfigType());
            }
            
            log.info("配置变更事件处理完成 - 类型: {}", changeEvent.getConfigType());
            
        } catch (Exception e) {
            log.error("处理配置变更事件失败 - 类型: {}, 错误: {}", 
                changeEvent.getConfigType(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理安全配置变更
     * 
     * @param changeEvent 变更事件
     */
    private void handleSecurityConfigChange(SecurityConfigChangeDTO changeEvent) {
        log.debug("处理安全配置变更");
        
        // 这里可以添加安全配置变更后的特殊处理逻辑
        // 例如：刷新密码策略缓存、更新会话超时设置等
        
        // 发送配置变更通知
        sendConfigChangeNotification(changeEvent, "安全配置已更新");
    }
    
    /**
     * 处理JWT配置变更
     * 
     * @param changeEvent 变更事件
     */
    private void handleJwtConfigChange(SecurityConfigChangeDTO changeEvent) {
        log.debug("处理JWT配置变更");
        
        // JWT配置变更后的处理逻辑
        // 例如：清理JWT黑名单缓存、更新令牌验证器等
        
        // 发送配置变更通知
        sendConfigChangeNotification(changeEvent, "JWT配置已更新，新的令牌将使用新配置");
    }
    
    /**
     * 处理访问频率限制配置变更
     * 
     * @param changeEvent 变更事件
     */
    private void handleRateLimitConfigChange(SecurityConfigChangeDTO changeEvent) {
        log.debug("处理访问频率限制配置变更");
        
        // 访问频率限制配置变更后的处理逻辑
        // 例如：清理频率限制缓存、更新限制规则等
        
        // 发送配置变更通知
        sendConfigChangeNotification(changeEvent, "访问频率限制配置已更新");
    }
    
    /**
     * 处理备份配置变更
     * 
     * @param changeEvent 变更事件
     */
    private void handleBackupConfigChange(SecurityConfigChangeDTO changeEvent) {
        log.debug("处理备份配置变更");
        
        // 备份配置变更后的处理逻辑
        // 例如：更新备份调度任务、验证备份路径等
        
        // 发送配置变更通知
        sendConfigChangeNotification(changeEvent, "备份配置已更新");
    }
    
    /**
     * 发送配置变更通知
     * 
     * @param changeEvent 变更事件
     * @param message 通知消息
     */
    private void sendConfigChangeNotification(SecurityConfigChangeDTO changeEvent, String message) {
        try {
            // 这里可以集成邮件、短信或其他通知方式
            // 目前只记录日志
            log.info("配置变更通知 - 类型: {}, 操作者: {}, 消息: {}", 
                changeEvent.getConfigType(), changeEvent.getOperator(), message);
            
            // 可以在这里添加实际的通知发送逻辑
            // 例如：发送邮件给管理员、推送到监控系统等
            
        } catch (Exception e) {
            log.error("发送配置变更通知失败 - 类型: {}, 错误: {}", 
                changeEvent.getConfigType(), e.getMessage(), e);
        }
    }
}