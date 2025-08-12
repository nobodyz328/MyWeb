package com.myweb.website_core.common.exception.handle;

import com.myweb.website_core.application.service.security.SecurityExceptionMaskingService;
import com.myweb.website_core.application.service.security.SecurityExceptionStatisticsService;
import com.myweb.website_core.common.exception.SecurityErrorResponse;
import com.myweb.website_core.common.util.SecurityEventUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@ControllerAdvice
@Order(2) // 优先级低于SecurityExceptionHandler
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    private final SecurityExceptionMaskingService maskingService;
    private final SecurityExceptionStatisticsService statisticsService;

    /**
     * 处理通用系统异常
     * 注意：安全相关异常已由SecurityExceptionHandler处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<SecurityErrorResponse> handlerExceptions(Exception e, WebRequest request) {
        String path = getRequestPath(request);
        String username = SecurityEventUtils.getUsername();
        String ipAddress = SecurityEventUtils.getIpAddress();
        
        // 创建通用错误响应
        SecurityErrorResponse errorResponse = SecurityErrorResponse.genericSecurityError(
                "SYSTEM_ERROR",
                "系统繁忙，请稍后再试",
                SecurityErrorResponse.SecurityErrorCategory.OTHER,
                SecurityErrorResponse.SecurityErrorSeverity.MEDIUM,
                path,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        ).withRequestId(SecurityEventUtils.generateRequestId())
         .withSuggestedAction("请稍后重试，如问题持续请联系技术支持");
        
        // 脱敏处理
        SecurityErrorResponse maskedResponse = maskingService.maskByCurrentUserPermission(errorResponse);
        
        // 记录统计
        statisticsService.recordSecurityException(errorResponse, e, username, ipAddress);
        
        // 记录日志
        log.error("GlobalExceptionHandler: 系统异常 - 用户: {}, IP: {}, 异常: {},",
                username, ipAddress, e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(maskedResponse);
    }
    
    /**
     * 获取请求路径
     * 
     * @param request Web请求
     * @return 请求路径
     */
    private String getRequestPath(WebRequest request) {
        try {
            return request.getDescription(false).replace("uri=", "");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
