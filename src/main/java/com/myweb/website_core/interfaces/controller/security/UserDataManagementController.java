package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.UserDataManagementService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.dto.UserDataExportDTO;
import com.myweb.website_core.domain.business.dto.UserDataUpdateDTO;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 用户数据管理控制器
 * 
 * 提供用户个人数据的查看、导出、修改和删除功能的REST API
 * 符合GB/T 22239-2019二级等保要求的个人信息保护机制
 * 
 * 功能包括：
 * - 查看用户数据（带权限控制和脱敏）
 * - 导出用户数据（JSON/CSV格式）
 * - 修改用户数据（带验证和审计）
 * - 删除用户数据（物理删除）
 * - 完整的操作审计记录
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/security/user-data")
@RequiredArgsConstructor
public class UserDataManagementController {

    private final UserDataManagementService userDataManagementService;

    // ==================== 数据查看接口 ====================

    /**
     * 查看用户个人数据
     * 
     * @param userId 用户ID
     * @param request HTTP请求对象
     * @return 用户数据（脱敏处理）
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    @Auditable(operation = AuditOperation.PROFILE_UPDATE, resourceType = "USER", description = "查看用户数据")
    public ResponseEntity<?> viewUserData(@PathVariable Long userId, HttpServletRequest request) {
        try {
            String clientIp = getClientIpAddress(request);
            log.info("接收查看用户数据请求: userId={}, clientIp={}", userId, clientIp);
            
            CompletableFuture<UserDataExportDTO> future = userDataManagementService.viewUserData(userId, clientIp);
            UserDataExportDTO userData = future.get();
            
            log.info("用户数据查看成功: userId={}", userId);
            return ResponseEntity.ok(userData);
            
        } catch (Exception e) {
            log.error("查看用户数据失败: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("查看用户数据失败: " + e.getMessage());
        }
    }

    // ==================== 数据导出接口 ====================

    /**
     * 导出用户数据（JSON格式）
     * 
     * @param userId 用户ID
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     */
    @GetMapping("/{userId}/export/json")
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    @Auditable(operation = AuditOperation.DATA_EXPORT, resourceType = "USER", description = "导出用户数据(JSON)")
    public void exportUserDataAsJson(@PathVariable Long userId, 
                                   HttpServletRequest request, 
                                   HttpServletResponse response) {
        try {
            String clientIp = getClientIpAddress(request);
            log.info("接收导出用户数据请求(JSON): userId={}, clientIp={}", userId, clientIp);
            
            CompletableFuture<byte[]> future = userDataManagementService.exportUserDataAsJson(userId, clientIp);
            byte[] jsonData = future.get();
            
            // 设置响应头
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("user_data_%d_%s.json", userId, timestamp);
            
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            response.setContentLength(jsonData.length);
            
            // 写入响应
            response.getOutputStream().write(jsonData);
            response.getOutputStream().flush();
            
            log.info("用户数据导出成功(JSON): userId={}, filename={}, size={} bytes", 
                    userId, filename, jsonData.length);
            
        } catch (Exception e) {
            log.error("导出用户数据失败(JSON): userId={}", userId, e);
            try {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.getWriter().write("导出用户数据失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 导出用户数据（CSV格式）
     * 
     * @param userId 用户ID
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     */
    @GetMapping("/{userId}/export/csv")
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    @Auditable(operation = AuditOperation.DATA_EXPORT, resourceType = "USER", description = "导出用户数据(CSV)")
    public void exportUserDataAsCsv(@PathVariable Long userId, 
                                  HttpServletRequest request, 
                                  HttpServletResponse response) {
        try {
            String clientIp = getClientIpAddress(request);
            log.info("接收导出用户数据请求(CSV): userId={}, clientIp={}", userId, clientIp);
            
            CompletableFuture<byte[]> future = userDataManagementService.exportUserDataAsCsv(userId, clientIp);
            byte[] csvData = future.get();
            
            // 设置响应头
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("user_data_%d_%s.csv", userId, timestamp);
            
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            response.setContentLength(csvData.length);
            
            // 添加BOM以支持Excel正确显示中文
            response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            response.getOutputStream().write(csvData);
            response.getOutputStream().flush();
            
            log.info("用户数据导出成功(CSV): userId={}, filename={}, size={} bytes", 
                    userId, filename, csvData.length);
            
        } catch (Exception e) {
            log.error("导出用户数据失败(CSV): userId={}", userId, e);
            try {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.getWriter().write("导出用户数据失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    // ==================== 数据修改接口 ====================

    /**
     * 修改用户数据
     * 
     * @param userId 用户ID
     * @param updateDTO 更新数据
     * @param request HTTP请求对象
     * @return 更新后的用户数据
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    @Auditable(operation = AuditOperation.PROFILE_UPDATE, resourceType = "USER", description = "修改用户数据")
    public ResponseEntity<?> updateUserData(@PathVariable Long userId, 
                                          @Valid @RequestBody UserDataUpdateDTO updateDTO,
                                          HttpServletRequest request) {
        try {
            String clientIp = getClientIpAddress(request);
            log.info("接收修改用户数据请求: userId={}, clientIp={}, updates={}", 
                    userId, clientIp, updateDTO.getUpdateSummary());
            
            // 验证更新数据
            if (!updateDTO.hasAnyUpdate()) {
                return ResponseEntity.badRequest().body("没有提供任何需要更新的字段");
            }
            
            updateDTO.validateFields();
            
            CompletableFuture<UserDataExportDTO> future = userDataManagementService.updateUserData(userId, updateDTO, clientIp);
            UserDataExportDTO updatedData = future.get();
            
            log.info("用户数据修改成功: userId={}, updates={}", userId, updateDTO.getUpdateSummary());
            return ResponseEntity.ok(updatedData);
            
        } catch (IllegalArgumentException e) {
            log.warn("修改用户数据参数错误: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("修改用户数据失败: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("修改用户数据失败: " + e.getMessage());
        }
    }

    // ==================== 数据删除接口 ====================

    /**
     * 删除用户数据（物理删除）
     * 
     * @param userId 用户ID
     * @param request HTTP请求对象
     * @return 删除结果
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    @Auditable(operation = AuditOperation.USER_DELETE, resourceType = "USER", description = "删除用户数据")
    public ResponseEntity<?> deleteUserData(@PathVariable Long userId, HttpServletRequest request) {
        try {
            String clientIp = getClientIpAddress(request);
            log.info("接收删除用户数据请求: userId={}, clientIp={}", userId, clientIp);
            
            CompletableFuture<Boolean> future = userDataManagementService.deleteUserData(userId, clientIp);
            Boolean result = future.get();
            
            if (result) {
                log.info("用户数据删除成功: userId={}", userId);
                return ResponseEntity.ok("用户数据删除成功");
            } else {
                log.warn("用户数据删除失败: userId={}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("用户数据删除失败");
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("删除用户数据参数错误: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("删除用户数据失败: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("删除用户数据失败: " + e.getMessage());
        }
    }

    // ==================== 数据管理信息接口 ====================

    /**
     * 获取用户数据管理信息
     * 
     * @param userId 用户ID
     * @param request HTTP请求对象
     * @return 数据管理信息
     */
    @GetMapping("/{userId}/info")
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    public ResponseEntity<?> getUserDataInfo(@PathVariable Long userId, HttpServletRequest request) {
        try {
            String clientIp = getClientIpAddress(request);
            log.info("接收获取用户数据管理信息请求: userId={}, clientIp={}", userId, clientIp);
            
            // 构建数据管理信息
            var dataInfo = new java.util.HashMap<String, Object>();
            dataInfo.put("userId", userId);
            dataInfo.put("dataProtectionLevel", "GB/T 22239-2019 二级等保");
            dataInfo.put("supportedExportFormats", new String[]{"JSON", "CSV"});
            dataInfo.put("dataRetentionPolicy", "用户数据永久保存，直到用户主动删除");
            dataInfo.put("privacyRights", new String[]{
                "数据查看权", "数据导出权", "数据修改权", "数据删除权"
            });
            dataInfo.put("contactInfo", "如有数据保护相关问题，请联系系统管理员");
            dataInfo.put("lastUpdated", LocalDateTime.now());
            
            log.info("用户数据管理信息获取成功: userId={}", userId);
            return ResponseEntity.ok(dataInfo);
            
        } catch (Exception e) {
            log.error("获取用户数据管理信息失败: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("获取数据管理信息失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}