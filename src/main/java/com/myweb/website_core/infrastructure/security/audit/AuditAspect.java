package com.myweb.website_core.infrastructure.security.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.AuditLogServiceAdapter;
import com.myweb.website_core.common.constant.SystemConstants;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import static com.myweb.website_core.common.util.SecurityEventUtils.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AOP审计切面
 * <p>
 * 自动记录标记了@Auditable注解的方法调用的审计日志，包括：
 * - 方法调用信息记录
 * - 请求参数和响应结果序列化
 * - 执行时间统计和性能监控
 * - 异常处理，确保不影响业务流程
 * <p>
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    
    private final AuditLogServiceAdapter auditLogServiceAdapter;
    private final ObjectMapper objectMapper;
    
    // 敏感信息关键字，用于参数脱敏
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(SystemConstants.SENSITIVE_PARAM_KEYWORDS);

    /**
     * 环绕通知：记录方法调用的审计日志
     *
     * @param joinPoint 连接点
     * @param auditable 审计注解
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(auditable)")
    public Object auditOperation(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        
        // 获取用户信息
        UserInfo userInfo = getCurrentUserInfo();
        
        // 获取网络信息
        NetworkInfo networkInfo = getNetworkInfo();
        
        // 构建审计日志请求基础信息
        AuditLogRequest.AuditLogRequestBuilder logBuilder = AuditLogRequest.builder()
                .operation(auditable.operation())
                .resourceType(auditable.resourceType())
                .description(auditable.description().isEmpty() ? methodName : auditable.description())
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .ipAddress(networkInfo.getIpAddress())
                .userAgent(networkInfo.getUserAgent())
                .sessionId(networkInfo.getSessionId())
                .requestId(generateRequestId())
                .timestamp(LocalDateTime.now())
                .riskLevel(auditable.riskLevel() > 0 ? auditable.riskLevel() : auditable.operation().getRiskLevel())
                .tags(auditable.tags().isEmpty() ? null : auditable.tags());
        
        // 记录请求参数
        Object requestData;
        if (auditable.logRequest()) {
            try {
                requestData = extractRequestData(joinPoint, auditable);
                logBuilder.requestData(requestData);
            } catch (Exception e) {
                if (!auditable.ignoreAuditException()) {
                    throw e;
                }
                log.warn("提取请求参数失败: method={}, error={}", methodName, e.getMessage());
            }
        }
        
        Object result = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            if (auditable.logExecutionTime()){
                logBuilder.executionTime(executionTime);
            }
            if (auditable.logResponse()){
                logBuilder.responseData(extractResponseData( result,auditable));
            }
            
            // 记录成功的审计日志
            recordSuccessAudit(logBuilder,auditable, methodName);

            
        } catch (Throwable e) {

            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            if (auditable.logExecutionTime()){
                logBuilder.executionTime(executionTime);
            }
            // 记录失败的审计日志
            recordFailureAudit(logBuilder, e, auditable, methodName);

        }
        return result;
    }
    
    /**
     * 记录成功操作的审计日志
     * 
     * @param logBuilder 日志构建器
     * @param auditable 审计注解
     * @param methodName 方法名
     */
    private void recordSuccessAudit(AuditLogRequest.AuditLogRequestBuilder logBuilder,
                                   Auditable auditable, String methodName) {
        try {

            // 设置成功结果
            logBuilder.result("SUCCESS");
            
            // 记录审计日志
            AuditLogRequest auditRequest = logBuilder.build();
            auditLogServiceAdapter.logOperation(auditRequest);
            
            log.debug("审计日志记录成功: operation={}, method={}",
                    auditable.operation(), methodName);
            
        } catch (Exception e) {
            if (!auditable.ignoreAuditException()) {
                throw new RuntimeException("记录成功审计日志失败", e);
            }
            log.error("记录成功审计日志失败: method={}, error={}", methodName, e.getMessage(), e);
        }
    }
    
    /**
     * 记录失败操作的审计日志
     * 
     * @param logBuilder 日志构建器
     * @param exception 异常信息
     * @param auditable 审计注解
     * @param methodName 方法名
     */
    private void recordFailureAudit(AuditLogRequest.AuditLogRequestBuilder logBuilder, 
                                   Throwable exception,
                                   Auditable auditable, String methodName) {
        try {
            // 设置失败结果和错误信息
            logBuilder.result("FAILURE")
                     .errorMessage(getExceptionMessage(exception));


                logBuilder.riskLevel(4);


            // 添加失败标签
            String currentTags = logBuilder.build().getTags();
            String failureTags = currentTags != null ? currentTags + ",failure" : "failure";
            logBuilder.tags(failureTags);
            
            // 记录审计日志
            AuditLogRequest auditRequest = logBuilder.build();
            if (auditable.async()) {
                auditLogServiceAdapter.logOperation(auditRequest);
            } else {
                auditLogServiceAdapter.logOperation(auditRequest).join();
            }
            
            log.debug("失败审计日志记录成功: operation={}, method={}, error={}",
                    auditable.operation(), methodName, exception.getMessage());
            
        } catch (Exception e) {
            if (!auditable.ignoreAuditException()) {
                throw new RuntimeException("记录失败审计日志失败", e);
            }
            log.error("记录失败审计日志失败: method={}, error={}", methodName, e.getMessage(), e);
        }
    }
    
    /**
     * 提取请求数据
     * 
     * @param joinPoint 连接点
     * @param auditable 审计注解
     * @return 请求数据
     */
    private Object extractRequestData(ProceedingJoinPoint joinPoint, Auditable auditable) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        
        try {
            // 处理敏感参数脱敏
            Object[] processedArgs = processSensitiveParams(args, auditable.sensitiveParams());
            
            // 构建参数映射
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            
            Map<String, Object> requestData = new LinkedHashMap<>();
            for (int i = 0; i < processedArgs.length && i < paramNames.length; i++) {
                String paramName = paramNames[i];
                Object paramValue = processedArgs[i];
                
                // 检查参数名是否包含敏感信息
                if (SystemConstants.isSensitiveParam(paramName)) {
                    paramValue = SystemConstants.MASKED_VALUE;
                }
                
                // 限制参数长度
                paramValue = limitDataLength(paramValue, auditable.maxParamLength());
                
                requestData.put(paramName, paramValue);
            }
            
            return requestData;
            
        } catch (Exception e) {
            log.warn("提取请求参数失败: {}", e.getMessage());
            return Map.of("error", "Failed to extract request data: " + e.getMessage());
        }
    }
    
    /**
     * 提取响应数据
     * 
     * @param result 方法执行结果
     * @param auditable 审计注解
     * @return 响应数据
     */
    private Object extractResponseData(Object result, Auditable auditable) {
        if (result == null) {
            return null;
        }
        
        try {
            // 限制响应数据长度
            return limitDataLength(result, auditable.maxResponseLength());
        } catch (Exception e) {
            log.warn("提取响应数据失败: {}", e.getMessage());
            return Map.of("error", "Failed to extract response data: " + e.getMessage());
        }
    }
    
    /**
     * 处理敏感参数脱敏
     * 
     * @param args 原始参数数组
     * @param sensitiveIndexes 敏感参数索引
     * @return 处理后的参数数组
     */
    private Object[] processSensitiveParams(Object[] args, int[] sensitiveIndexes) {
        if (sensitiveIndexes == null || sensitiveIndexes.length == 0) {
            return args;
        }
        
        Object[] processedArgs = Arrays.copyOf(args, args.length);
        
        for (int index : sensitiveIndexes) {
            if (index >= 0 && index < processedArgs.length) {
                processedArgs[index] = SystemConstants.MASKED_VALUE;
            }
        }
        
        return processedArgs;
    }
    

    
    /**
     * 限制数据长度
     * 
     * @param data 原始数据
     * @param maxLength 最大长度
     * @return 限制长度后的数据
     */
    private Object limitDataLength(Object data, int maxLength) {
        if (data == null || maxLength <= 0) {
            return data;
        }
        
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            return SystemConstants.limitStringLength(jsonString, maxLength);
        } catch (JsonProcessingException e) {
            String stringValue = data.toString();
            return SystemConstants.limitStringLength(stringValue, maxLength);
        }
    }
    
    /**
     * 获取当前用户信息
     * 
     * @return 用户信息
     */
    private UserInfo getCurrentUserInfo() {
        String username = getUsername();
        Long userId = getUserId();
        return new UserInfo(userId, username);
    }
    /**
     * 获取网络信息
     *
     * @return 网络信息
     */
    private NetworkInfo getNetworkInfo() {
        String ipAddress = getIpAddress();
        String userAgent = getUserAgent();
        String sessionId = getSessionId();
        return new NetworkInfo(ipAddress, userAgent, sessionId);
    }

    /**
     * 用户信息内部类
     */
    @Getter
    private static class UserInfo {
        private final Long userId;
        private final String username;

        public UserInfo(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

    }
    
    /**
     * 网络信息内部类
     */
    @Getter
    private static class NetworkInfo {
        private final String ipAddress;
        private final String userAgent;
        private final String sessionId;
        
        public NetworkInfo(String ipAddress, String userAgent, String sessionId) {
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.sessionId = sessionId;
        }

    }
}