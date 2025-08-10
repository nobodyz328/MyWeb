package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.UserSettingsService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.application.service.security.authentication.TOTPManagementService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户设置控制器
 * <p>
 * 提供用户设置相关的API接口，包括：
 * - 个人信息管理
 * - 密码修改
 * - 邮箱绑定
 * - TOTP设置
 * - 管理员访问验证
 * <p>
 * 符合GB/T 22239-2019身份鉴别和访问控制要求
 */
@Slf4j
@RestController
@RequestMapping("/api/users/{userId}/settings")
@RequiredArgsConstructor
public class UserSettingsController {
    
    private final UserSettingsService userSettingsService;
    private final TOTPManagementService totpManagementService;
    private final AuthenticationService authenticationService;
    
    /**
     * 获取用户设置信息
     */
    @GetMapping
    @Auditable(operation = AuditOperation.PROFILE_UPDATE, resourceType = "USER", description = "查看用户设置")
    public ResponseEntity<?> getUserSettings(@PathVariable Long userId, HttpServletRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限访问此用户设置");
            }
            
            UserSettingsService.UserSettingsInfo settings = userSettingsService.getUserSettings(userId);
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("获取用户设置失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("获取用户设置失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户基本信息
     */
    @PutMapping("/basic")
    @Auditable(operation = AuditOperation.PROFILE_UPDATE, resourceType = "USER", description = "更新用户基本信息")
    public ResponseEntity<?> updateBasicInfo(@PathVariable Long userId, 
                                           @RequestBody UpdateBasicInfoRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限修改此用户信息");
            }
            
            userSettingsService.updateBasicInfo(userId, request.getBio(), request.getAvatarUrl());
            return ResponseEntity.ok(Map.of("message", "基本信息更新成功"));
            
        } catch (Exception e) {
            log.error("更新用户基本信息失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Auditable(operation = AuditOperation.PASSWORD_CHANGE, resourceType = "USER", description = "修改密码")
    public ResponseEntity<?> changePassword(@PathVariable Long userId, 
                                          @RequestBody ChangePasswordRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限修改此用户密码");
            }
            
            userSettingsService.changePassword(userId, request.getCurrentPassword(), 
                    request.getNewPassword(), request.getTotpCode());
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
            
        } catch (Exception e) {
            log.error("修改密码失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("修改密码失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送邮箱绑定验证码
     */
    @PostMapping("/email/send-code")
    @Auditable(operation = AuditOperation.EMAIL_VERIFIED, resourceType = "USER", description = "发送邮箱绑定验证码")
    public ResponseEntity<?> sendEmailBindingCode(@PathVariable Long userId, 
                                                @RequestParam String email) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            userSettingsService.sendEmailBindingCode(userId, email);
            return ResponseEntity.ok(Map.of("message", "验证码已发送到邮箱"));
            
        } catch (Exception e) {
            log.error("发送邮箱绑定验证码失败: userId={}, email={}", userId, email, e);
            return ResponseEntity.badRequest().body("发送验证码失败: " + e.getMessage());
        }
    }
    
    /**
     * 绑定邮箱
     */
    @PostMapping("/email/bind")
    @Auditable(operation = AuditOperation.EMAIL_VERIFIED, resourceType = "USER", description = "绑定邮箱")
    public ResponseEntity<?> bindEmail(@PathVariable Long userId, 
                                     @RequestBody BindEmailRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            userSettingsService.bindEmail(userId, request.getEmail(), 
                    request.getVerificationCode(), request.getTotpCode());
            return ResponseEntity.ok(Map.of("message", "邮箱绑定成功"));
            
        } catch (Exception e) {
            log.error("绑定邮箱失败: userId={}, email={}", userId, request.getEmail(), e);
            return ResponseEntity.badRequest().body("绑定邮箱失败: " + e.getMessage());
        }
    }
    
    // ==================== TOTP相关接口 ====================
    
    /**
     * 获取TOTP设置信息
     */
    @GetMapping("/totp/setup")
    @Auditable(operation = AuditOperation.TOTP_SETUP, resourceType = "USER", description = "查看TOTP设置")
    public ResponseEntity<?> getTOTPSetup(@PathVariable Long userId) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "无权限访问此用户TOTP设置"
                ));
            }
            
            TOTPManagementService.TOTPSetupInfo setupInfo = totpManagementService.generateTOTPSetup(userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", setupInfo,
                "message", "TOTP设置信息获取成功"
            ));
            
        } catch (Exception e) {
            log.error("获取TOTP设置失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取TOTP设置失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取TOTP状态
     */
    @GetMapping("/totp/status")
    @Auditable(operation = AuditOperation.TOTP_SETUP, resourceType = "USER", description = "查看TOTP状态")
    public ResponseEntity<?> getTOTPStatus(@PathVariable Long userId) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "无权限访问此用户TOTP状态"
                ));
            }
            
            TOTPManagementService.TOTPStatusInfo statusInfo = totpManagementService.getTOTPStatus(userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", statusInfo,
                "message", "TOTP状态获取成功"
            ));
            
        } catch (Exception e) {
            log.error("获取TOTP状态失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取TOTP状态失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 生成TOTP二维码
     */
    @GetMapping("/totp/qrcode")
    @Auditable(operation = AuditOperation.TOTP_SETUP, resourceType = "USER", description = "生成TOTP二维码")
    public ResponseEntity<?> getTOTPQRCode(@PathVariable Long userId,
                                         @RequestParam(defaultValue = "200") int width,
                                         @RequestParam(defaultValue = "200") int height,
                                         HttpServletResponse response) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限访问此用户TOTP二维码");
            }
            
            byte[] qrCodeImage = totpManagementService.generateTOTPQRCode(userId, width, height);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
            
        } catch (Exception e) {
            log.error("生成TOTP二维码失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("生成二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用TOTP
     */
    @PostMapping("/totp/enable")
    @Auditable(operation = AuditOperation.TOTP_ENABLE, resourceType = "USER", description = "启用TOTP")
    public ResponseEntity<?> enableTOTP(@PathVariable Long userId, 
                                      @RequestBody EnableTOTPRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            totpManagementService.enableTOTP(userId, request.getSecret(), request.getVerificationCode());
            return ResponseEntity.ok(Map.of("message", "TOTP启用成功"));
            
        } catch (Exception e) {
            log.error("启用TOTP失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("启用TOTP失败: " + e.getMessage());
        }
    }
    
    /**
     * 禁用TOTP
     */
    @PostMapping("/totp/disable")
    @Auditable(operation = AuditOperation.TOTP_DISABLED, resourceType = "USER", description = "禁用TOTP")
    public ResponseEntity<?> disableTOTP(@PathVariable Long userId, 
                                       @RequestBody DisableTOTPRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            totpManagementService.disableTOTP(userId, request.getVerificationCode());
            return ResponseEntity.ok(Map.of("message", "TOTP禁用成功"));
            
        } catch (Exception e) {
            log.error("禁用TOTP失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("禁用TOTP失败: " + e.getMessage());
        }
    }
    
    /**
     * 重置TOTP密钥
     */
    @PostMapping("/totp/reset")
    @Auditable(operation = AuditOperation.TOTP_SETUP, resourceType = "USER", description = "重置TOTP密钥")
    public ResponseEntity<?> resetTOTPSecret(@PathVariable Long userId, 
                                           @RequestBody ResetTOTPRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            TOTPManagementService.TOTPSetupInfo setupInfo = totpManagementService.resetTOTPSecret(
                    userId, request.getCurrentVerificationCode());
            return ResponseEntity.ok(setupInfo);
            
        } catch (Exception e) {
            log.error("重置TOTP密钥失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("重置TOTP密钥失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证TOTP代码
     */
    @PostMapping("/totp/verify")
    @Auditable(operation = AuditOperation.TOTP_VERIFY, resourceType = "USER", description = "验证TOTP代码")
    public ResponseEntity<?> verifyTOTP(@PathVariable Long userId, 
                                      @RequestBody VerifyTOTPRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            boolean isValid = totpManagementService.verifyTOTP(userId, request.getVerificationCode());
            return ResponseEntity.ok(Map.of("valid", isValid));
            
        } catch (Exception e) {
            log.error("验证TOTP代码失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("验证TOTP代码失败: " + e.getMessage());
        }
    }
    
    // ==================== 管理员访问相关接口 ====================
    
    /**
     * 检查管理员访问权限
     */
    @PostMapping("/admin/check-access")
    @Auditable(operation = AuditOperation.ADMIN_ACCESS, resourceType = "USER", description = "检查管理员访问权限")
    public ResponseEntity<?> checkAdminAccess(@PathVariable Long userId, 
                                            @RequestBody CheckAdminAccessRequest request) {
        try {
            // 验证用户权限
            if (!validateUserAccess(userId)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            UserSettingsService.AdminAccessInfo accessInfo = userSettingsService.checkAdminAccess(
                    userId, request.getTotpCode());
            return ResponseEntity.ok(accessInfo);
            
        } catch (Exception e) {
            log.error("检查管理员访问权限失败: userId={}", userId, e);
            return ResponseEntity.badRequest().body("检查访问权限失败: " + e.getMessage());
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 验证用户访问权限
     * 
     * @param userId 用户ID
     * @return 是否有权限
     */
    private boolean validateUserAccess(Long userId) {
        User currentUser = authenticationService.getCurrentUser();
        return currentUser != null && currentUser.getId().equals(userId);
    }


// ==================== 请求DTO类 ====================


    @Getter@Setter
    private static class UpdateBasicInfoRequest {
        private String bio;
        private String avatarUrl;
    }

    @Getter@Setter
    private static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String totpCode;
    }

    @Getter@Setter
    private static class BindEmailRequest {
        private String email;
        private String verificationCode;
        private String totpCode;
    }

    @Getter@Setter
    private static class EnableTOTPRequest {
        private String secret;
        private String verificationCode;
    }

    @Getter@Setter
    private static class DisableTOTPRequest {
        private String verificationCode;
    }

    @Getter@Setter
    private static  class ResetTOTPRequest {
        private String currentVerificationCode;

    }

    @Getter@Setter
    private static class VerifyTOTPRequest {
        private String verificationCode;
    }

    @Getter@Setter
    private static class CheckAdminAccessRequest {
        private String totpCode;
    }
}

