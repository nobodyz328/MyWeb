package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.IPS.virusprotect.CsrfTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CSRF令牌控制器
 * 提供CSRF令牌的获取和刷新接口
 *
 * @author Kiro
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/csrf")
public class CsrfController {

    private static final Logger logger = LoggerFactory.getLogger(CsrfController.class);

    @Autowired
    private CsrfTokenService csrfTokenService;

    /**
     * 获取CSRF令牌
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @return CSRF令牌信息
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // 尝试获取当前令牌
            Map<String, String> tokenInfo = csrfTokenService.getCurrentTokenInfo(request);

            // 如果没有令牌，生成新的
            if (tokenInfo == null) {
                tokenInfo = csrfTokenService.generateToken(request, response);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", tokenInfo);
            result.put("message", "CSRF令牌获取成功");

            logger.debug("CSRF token retrieved successfully for request: {}", request.getRequestURI());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get CSRF token", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "获取CSRF令牌失败");
            errorResult.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 刷新CSRF令牌
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @return 新的CSRF令牌信息
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            Map<String, String> tokenInfo = csrfTokenService.refreshToken(request, response);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", tokenInfo);
            result.put("message", "CSRF令牌刷新成功");

            logger.debug("CSRF token refreshed successfully for request: {}", request.getRequestURI());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to refresh CSRF token", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "刷新CSRF令牌失败");
            errorResult.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 验证CSRF令牌
     *
     * @param request HTTP请求
     * @param tokenRequest 令牌验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCsrfToken(
            HttpServletRequest request,
            @RequestBody TokenValidationRequest tokenRequest) {

        try {
            boolean isValid = csrfTokenService.validateToken(request, tokenRequest.getToken());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("valid", isValid);
            result.put("message", isValid ? "CSRF令牌验证成功" : "CSRF令牌验证失败");

            logger.debug("CSRF token validation result: {} for request: {}", isValid, request.getRequestURI());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to validate CSRF token", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("valid", false);
            errorResult.put("message", "CSRF令牌验证失败");
            errorResult.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 清除CSRF令牌
     *
     * @param request HTTP请求
     * @return 操作结果
     */
    @DeleteMapping("/token")
    public ResponseEntity<Map<String, Object>> clearCsrfToken(HttpServletRequest request) {

        try {
            csrfTokenService.clearToken(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "CSRF令牌清除成功");

            logger.debug("CSRF token cleared successfully for request: {}", request.getRequestURI());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to clear CSRF token", e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "清除CSRF令牌失败");
            errorResult.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 令牌验证请求DTO
     */
    @Getter
    public static class TokenValidationRequest {
        private String token;

        public void setToken(String token) {
            this.token = token;
        }
    }
}