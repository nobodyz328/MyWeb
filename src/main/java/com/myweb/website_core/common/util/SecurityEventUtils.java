package com.myweb.website_core.common.util;

import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全事件工具类
 * 
 * 提供创建安全事件的便捷方法
 */
@Component
public class SecurityEventUtils {
    
    /**
     * 创建登录失败事件
     */
    public static SecurityEventRequest createLoginFailureEvent(String username, String reason) {
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.CONTINUOUS_LOGIN_FAILURE)
                .title("用户登录失败")
                .description(String.format("用户 %s 登录失败：%s", username, reason))
                .username(username)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("reason", reason, "username", username))
                .build();
    }
    
    /**
     * 创建暴力破解攻击事件
     */
    public static SecurityEventRequest createBruteForceAttackEvent(String username, int attemptCount) {
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.BRUTE_FORCE_ATTACK)
                .title("暴力破解攻击检测")
                .description(String.format("检测到针对用户 %s 的暴力破解攻击，尝试次数：%d", username, attemptCount))
                .username(username)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("attemptCount", attemptCount, "username", username))
                .riskScore(80 + Math.min(attemptCount * 2, 20)) // 基础80分，每次尝试加2分，最多100分
                .build();
    }
    
    /**
     * 创建未授权访问事件
     */
    public static SecurityEventRequest createUnauthorizedAccessEvent(String resource, String action) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.UNAUTHORIZED_ACCESS)
                .title("未授权访问尝试")
                .description(String.format("用户 %s 尝试未授权访问资源：%s，操作：%s", currentUser, resource, action))
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("resource", resource, "action", action))
                .build();
    }
    
    /**
     * 创建SQL注入尝试事件
     */
    public static SecurityEventRequest createSqlInjectionEvent(String input, String parameter) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.SQL_INJECTION_ATTEMPT)
                .title("SQL注入攻击尝试")
                .description(String.format("检测到SQL注入攻击尝试，参数：%s", parameter))
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("parameter", parameter, "suspiciousInput", input))
                .riskScore(90) // SQL注入是高危攻击
                .build();
    }
    
    /**
     * 创建XSS攻击尝试事件
     */
    public static SecurityEventRequest createXssAttackEvent(String input, String parameter) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.XSS_ATTACK_ATTEMPT)
                .title("XSS攻击尝试")
                .description(String.format("检测到XSS攻击尝试，参数：%s", parameter))
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("parameter", parameter, "suspiciousInput", input))
                .riskScore(75) // XSS攻击中等风险
                .build();
    }
    
    /**
     * 创建恶意文件上传事件
     */
    public static SecurityEventRequest createMaliciousFileUploadEvent(String filename, String reason) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.MALICIOUS_FILE_UPLOAD)
                .title("恶意文件上传尝试")
                .description(String.format("用户 %s 尝试上传恶意文件：%s，原因：%s", currentUser, filename, reason))
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("filename", filename, "reason", reason))
                .riskScore(85) // 恶意文件上传高风险
                .build();
    }
    
    /**
     * 创建访问频率异常事件
     */
    public static SecurityEventRequest createAccessFrequencyEvent(int requestCount, int timeWindowMinutes) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.ABNORMAL_ACCESS_FREQUENCY)
                .title("访问频率异常")
                .description(String.format("检测到异常访问频率：%d分钟内请求%d次", timeWindowMinutes, requestCount))
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("requestCount", requestCount, "timeWindowMinutes", timeWindowMinutes))
                .riskScore(Math.min(50 + requestCount, 100)) // 基础50分，每个请求加1分
                .build();
    }
    
    /**
     * 创建权限提升尝试事件
     */
    public static SecurityEventRequest createPrivilegeEscalationEvent(String targetRole, String currentRole) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.PRIVILEGE_ESCALATION)
                .title("权限提升尝试")
                .description(String.format("用户 %s 尝试从角色 %s 提升到角色 %s", currentUser, currentRole, targetRole))
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("targetRole", targetRole, "currentRole", currentRole))
                .riskScore(95) // 权限提升是严重安全事件
                .build();
    }
    
    /**
     * 创建数据完整性异常事件
     */
    public static SecurityEventRequest createDataIntegrityViolationEvent(String dataType, String dataId, String details) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.DATA_INTEGRITY_VIOLATION)
                .title("数据完整性异常")
                .description(String.format("检测到数据完整性异常：%s (ID: %s)，详情：%s", dataType, dataId, details))
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("dataType", dataType, "dataId", dataId, "details", details))
                .riskScore(90) // 数据完整性异常是严重事件
                .build();
    }
    
    /**
     * 创建可疑IP访问事件
     */
    public static SecurityEventRequest createSuspiciousIpAccessEvent(String reason) {
        String currentUser = getCurrentUsername();
        String sourceIp = getClientIpAddress();
        return SecurityEventRequest.builder()
                .eventType(SecurityEventType.SUSPICIOUS_IP_ACCESS)
                .title("可疑IP访问")
                .description(String.format("检测到来自可疑IP %s 的访问，原因：%s", sourceIp, reason))
                .username(currentUser)
                .sourceIp(sourceIp)
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(Map.of("reason", reason, "sourceIp", sourceIp))
                .riskScore(70) // 可疑IP访问中高风险
                .build();
    }
    
    /**
     * 创建自定义安全事件
     */
    public static SecurityEventRequest createCustomEvent(SecurityEventType eventType, String title, 
                                                        String description, Map<String, Object> eventData) {
        String currentUser = getCurrentUsername();
        return SecurityEventRequest.builder()
                .eventType(eventType)
                .title(title)
                .description(description)
                .username(currentUser)
                .sourceIp(getClientIpAddress())
                .userAgent(getUserAgent())
                .requestUri(getRequestUri())
                .requestMethod(getRequestMethod())
                .sessionId(getSessionId())
                .eventTime(LocalDateTime.now())
                .eventData(eventData != null ? eventData : new HashMap<>())
                .build();
    }
    
    /**
     * 获取当前用户名
     */
    private static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "anonymous";
    }
    
    /**
     * 获取客户端IP地址
     */
    private static String getClientIpAddress() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }
    
    /**
     * 获取用户代理
     */
    private static String getUserAgent() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }
    
    /**
     * 获取请求URI
     */
    private static String getRequestUri() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                return request.getRequestURI();
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }
    
    /**
     * 获取请求方法
     */
    private static String getRequestMethod() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                return request.getMethod();
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }
    
    /**
     * 获取会话ID
     */
    private static String getSessionId() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null && request.getSession(false) != null) {
                return request.getSession().getId();
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }
    
    /**
     * 获取当前HTTP请求
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        return null;
    }
}