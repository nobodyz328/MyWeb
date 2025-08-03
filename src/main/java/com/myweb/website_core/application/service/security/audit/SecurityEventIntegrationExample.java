package com.myweb.website_core.application.service.security.audit;

import com.myweb.website_core.application.service.security.SecurityEventService;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.common.util.SecurityEventUtils;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 安全事件集成示例服务
 * 
 * 展示如何在其他业务服务中集成安全事件监控功能
 * 这个类提供了在各种业务场景中记录安全事件的示例
 */
@Slf4j
@Service
public class SecurityEventIntegrationExample {
    
    private final SecurityEventService securityEventService;
    
    public SecurityEventIntegrationExample(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }
    
    /**
     * 示例：在用户登录失败时记录安全事件
     * 
     * 这个方法应该在AuthenticationService中的登录失败处理逻辑中调用
     */
    public void handleLoginFailure(String username, String reason, int attemptCount) {
        try {
            // 根据失败次数决定事件类型
            SecurityEventRequest eventRequest;
            
            if (attemptCount >= 5) {
                // 多次失败，可能是暴力破解攻击
                eventRequest = SecurityEventUtils.createBruteForceAttackEvent(username, attemptCount);
            } else {
                // 普通登录失败
                eventRequest = SecurityEventUtils.createLoginFailureEvent(username, reason);
            }
            
            // 异步记录安全事件
            securityEventService.recordEvent(eventRequest);
            
            log.info("记录登录失败安全事件: username={}, attemptCount={}", username, attemptCount);
            
        } catch (Exception e) {
            log.error("记录登录失败安全事件失败: username={}, error={}", username, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到未授权访问时记录安全事件
     * 
     * 这个方法应该在权限检查失败时调用
     */
    public void handleUnauthorizedAccess(String resource, String action, String username) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createUnauthorizedAccessEvent(resource, action);
            securityEventService.recordEvent(eventRequest);
            
            log.warn("记录未授权访问安全事件: username={}, resource={}, action={}", username, resource, action);
            
        } catch (Exception e) {
            log.error("记录未授权访问安全事件失败: username={}, error={}", username, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到SQL注入尝试时记录安全事件
     * 
     * 这个方法应该在输入验证过滤器中调用
     */
    public void handleSqlInjectionAttempt(String suspiciousInput, String parameter) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createSqlInjectionEvent(suspiciousInput, parameter);
            securityEventService.recordEvent(eventRequest);
            
            log.warn("记录SQL注入尝试安全事件: parameter={}", parameter);
            
        } catch (Exception e) {
            log.error("记录SQL注入尝试安全事件失败: parameter={}, error={}", parameter, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到XSS攻击尝试时记录安全事件
     * 
     * 这个方法应该在XSS防护过滤器中调用
     */
    public void handleXssAttackAttempt(String suspiciousInput, String parameter) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createXssAttackEvent(suspiciousInput, parameter);
            securityEventService.recordEvent(eventRequest);
            
            log.warn("记录XSS攻击尝试安全事件: parameter={}", parameter);
            
        } catch (Exception e) {
            log.error("记录XSS攻击尝试安全事件失败: parameter={}, error={}", parameter, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到恶意文件上传时记录安全事件
     * 
     * 这个方法应该在文件上传安全检查中调用
     */
    public void handleMaliciousFileUpload(String filename, String reason) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createMaliciousFileUploadEvent(filename, reason);
            securityEventService.recordEvent(eventRequest);
            
            log.warn("记录恶意文件上传安全事件: filename={}, reason={}", filename, reason);
            
        } catch (Exception e) {
            log.error("记录恶意文件上传安全事件失败: filename={}, error={}", filename, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到访问频率异常时记录安全事件
     * 
     * 这个方法应该在访问频率限制过滤器中调用
     */
    public void handleAbnormalAccessFrequency(String clientIp, int requestCount, int timeWindowMinutes) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createAbnormalAccessFrequencyEvent(
                    requestCount, timeWindowMinutes);
            securityEventService.recordEvent(eventRequest);
            
            log.warn("记录访问频率异常安全事件: clientIp={}, requestCount={}, timeWindow={}min", 
                    clientIp, requestCount, timeWindowMinutes);
            
        } catch (Exception e) {
            log.error("记录访问频率异常安全事件失败: clientIp={}, error={}", clientIp, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到权限提升尝试时记录安全事件
     * 
     * 这个方法应该在角色权限管理中调用
     */
    public void handlePrivilegeEscalationAttempt(String username, String currentRole, String targetRole) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createPrivilegeEscalationEvent(targetRole, currentRole);
            securityEventService.recordEvent(eventRequest);
            
            log.warn("记录权限提升尝试安全事件: username={}, currentRole={}, targetRole={}", 
                    username, currentRole, targetRole);
            
        } catch (Exception e) {
            log.error("记录权限提升尝试安全事件失败: username={}, error={}", username, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到数据完整性异常时记录安全事件
     * 
     * 这个方法应该在数据完整性检查中调用
     */
    public void handleDataIntegrityViolation(String dataType, String dataId, String details) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createDataIntegrityViolationEvent(
                    dataType, dataId, details);
            securityEventService.recordEvent(eventRequest);
            
            log.error("记录数据完整性异常安全事件: dataType={}, dataId={}", dataType, dataId);
            
        } catch (Exception e) {
            log.error("记录数据完整性异常安全事件失败: dataType={}, error={}", dataType, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：在检测到可疑IP访问时记录安全事件
     * 
     * 这个方法应该在IP黑名单检查中调用
     */
    public void handleSuspiciousIpAccess(String clientIp, String reason) {
        try {
            SecurityEventRequest eventRequest = SecurityEventUtils.createSuspiciousIpAccessEvent(reason);
            securityEventService.recordEvent(eventRequest);
            
            log.warn("记录可疑IP访问安全事件: clientIp={}, reason={}", clientIp, reason);
            
        } catch (Exception e) {
            log.error("记录可疑IP访问安全事件失败: clientIp={}, error={}", clientIp, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：批量记录安全事件
     * 
     * 当需要同时记录多个相关安全事件时使用
     */
    public void handleBatchSecurityEvents(String username, String clientIp) {
        try {
            // 创建多个相关的安全事件
            SecurityEventRequest loginFailure = SecurityEventUtils.createLoginFailureEvent(username, "密码错误");
            SecurityEventRequest suspiciousIp = SecurityEventUtils.createSuspiciousIpAccessEvent("多次登录失败");
            SecurityEventRequest abnormalFrequency = SecurityEventUtils.createAbnormalAccessFrequencyEvent(10, 5);
            
            // 批量记录
            securityEventService.recordEvents(java.util.List.of(loginFailure, suspiciousIp, abnormalFrequency));
            
            log.warn("批量记录安全事件: username={}, clientIp={}, eventCount=3", username, clientIp);
            
        } catch (Exception e) {
            log.error("批量记录安全事件失败: username={}, error={}", username, e.getMessage(), e);
        }
    }
    
    /**
     * 示例：检查异常模式并记录事件
     * 
     * 这个方法展示如何结合异常检测功能
     */
    public void checkAndRecordAnomalousPattern(Long userId, String clientIp, SecurityEventType eventType) {
        try {
            // 检查是否存在异常模式
            boolean isAnomalous = securityEventService.detectAnomalousPattern(userId, clientIp, eventType);
            
            if (isAnomalous) {
                // 如果检测到异常模式，记录相应的安全事件
                SecurityEventRequest eventRequest = SecurityEventRequest.builder()
                        .eventType(SecurityEventType.ACCOUNT_ABNORMAL_ACTIVITY)
                        .title("账户异常活动检测")
                        .description(String.format("检测到用户ID %d 的异常活动模式，事件类型：%s", userId, eventType))
                        .userId(userId)
                        .sourceIp(clientIp)
                        .riskScore(85)
                        .build();
                
                securityEventService.recordEvent(eventRequest);
                
                log.warn("检测到异常模式并记录安全事件: userId={}, clientIp={}, eventType={}", 
                        userId, clientIp, eventType);
            }
            
        } catch (Exception e) {
            log.error("检查异常模式失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}