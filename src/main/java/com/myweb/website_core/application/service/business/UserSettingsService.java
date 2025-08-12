package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import com.myweb.website_core.application.service.security.authentication.TOTPManagementService;
import com.myweb.website_core.common.constant.SecurityConstants;
import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户设置服务
 * 
 * 提供用户个人设置相关功能，包括：
 * - 个人信息修改
 * - 密码修改
 * - 邮箱绑定/修改
 * - 头像上传
 * - 安全设置
 * 
 * 符合GB/T 22239-2019身份鉴别和访问控制要求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsService {
    
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final TOTPManagementService totpManagementService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    /**
     * 获取用户设置信息
     * 
     * @param userId 用户ID
     * @return 用户设置信息
     */
    @Transactional(readOnly = true)
    public UserSettingsInfo getUserSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return UserSettingsInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .emailVerified(user.getEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .totpEnabled(user.getTotpEnabled())
                .totpRequired(user.getRole() == UserRole.ADMIN)
                .createdAt(user.getCreatedAt())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .build();
    }
    
    /**
     * 更新用户基本信息
     * 
     * @param userId 用户ID
     * @param bio 个人简介
     * @param avatarUrl 头像URL
     */
    @Transactional
    public void updateBasicInfo(Long userId, String bio, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (bio != null) {
            if (bio.length() > 500) {
                throw new RuntimeException("个人简介不能超过500个字符");
            }
            user.setBio(bio);
        }
        
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        
        userRepository.save(user);
        log.info("用户 {} 更新基本信息成功", user.getUsername());
    }
    
    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param currentPassword 当前密码
     * @param newPassword 新密码
     * @param totpCode TOTP验证码（如果启用了TOTP）
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证当前密码
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("当前密码不正确");
        }
        
        // 如果用户启用了TOTP，需要验证TOTP代码
        if (user.getTotpEnabled() && user.getTotpSecret() != null) {
            if (totpCode == null || totpCode.trim().isEmpty()) {
                throw new RuntimeException("请提供TOTP验证码");
            }
            if (!totpManagementService.verifyTOTP(userId, totpCode)) {
                throw new RuntimeException("TOTP验证码不正确");
            }
        }
        
        // 验证新密码强度
        if (!SecurityConstants.isValidPasswordStrength(newPassword)) {
            throw new RuntimeException("新密码不符合安全策略要求");
        }
        
        // 检查新密码是否与当前密码相同
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new RuntimeException("新密码不能与当前密码相同");
        }
        
        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("用户 {} 修改密码成功", user.getUsername());
    }
    
    /**
     * 发送邮箱绑定验证码
     * 
     * @param userId 用户ID
     * @param email 新邮箱地址
     */
    @Transactional
    public void sendEmailBindingCode(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查邮箱是否已被其他用户使用
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new RuntimeException("该邮箱已被其他用户使用");
        }
        
        // 发送验证码
        emailVerificationService.sendEmailBindingVerificationCode(email);
        log.info("为用户 {} 发送邮箱绑定验证码到 {}", user.getUsername(), email);
    }
    
    /**
     * 绑定邮箱
     * 
     * @param userId 用户ID
     * @param email 邮箱地址
     * @param verificationCode 验证码
     * @param totpCode TOTP验证码（如果启用了TOTP）
     */
    @Transactional
    public void bindEmail(Long userId, String email, String verificationCode, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 如果用户启用了TOTP，需要验证TOTP代码
        if (user.getTotpEnabled() && user.getTotpSecret() != null) {
            if (totpCode == null || totpCode.trim().isEmpty()) {
                throw new RuntimeException("请提供TOTP验证码");
            }
            if (!totpManagementService.verifyTOTP(userId, totpCode)) {
                throw new RuntimeException("TOTP验证码不正确");
            }
        }
        
        // 验证邮箱验证码
        if (!emailVerificationService.verifyEmailBindingCode(email, verificationCode)) {
            throw new RuntimeException("邮箱验证码不正确或已过期");
        }
        
        // 检查邮箱是否已被其他用户使用
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new RuntimeException("该邮箱已被其他用户使用");
        }
        
        // 绑定邮箱
        user.setEmail(email);
        user.setEmailVerified(true);
        userRepository.save(user);
        
        log.info("用户 {} 绑定邮箱 {} 成功", user.getUsername(), email);
    }
    
    /**
     * 检查是否可以访问管理界面
     * 
     * @param userId 用户ID
     * @param totpCode TOTP验证码
     * @return 是否可以访问
     */
    @Transactional(readOnly = true)
    public AdminAccessInfo checkAdminAccess(Long userId, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查是否为管理员
        if (!user.hasManagementPermission()) {
            throw new RuntimeException("无管理权限");
        }
        
        // 管理员必须启用TOTP
        if (user.getRole() == UserRole.ADMIN) {
            if (!user.getTotpEnabled() || user.getTotpSecret() == null) {
                return AdminAccessInfo.builder()
                        .canAccess(false)
                        .requiresTOTP(true)
                        .totpEnabled(false)
                        .message("管理员账户必须启用TOTP才能访问管理界面")
                        .build();
            }
            
            // 验证TOTP代码
            if (totpCode == null || totpCode.trim().isEmpty()) {
                return AdminAccessInfo.builder()
                        .canAccess(false)
                        .requiresTOTP(true)
                        .totpEnabled(true)
                        .message("请提供TOTP验证码")
                        .build();
            }
            
            if (!totpManagementService.verifyTOTP(userId, totpCode)) {
                return AdminAccessInfo.builder()
                        .canAccess(false)
                        .requiresTOTP(true)
                        .totpEnabled(true)
                        .message("TOTP验证码不正确")
                        .build();
            }
        }
        
        return AdminAccessInfo.builder()
                .canAccess(true)
                .requiresTOTP(user.getRole() == UserRole.ADMIN)
                .totpEnabled(user.getTotpEnabled())
                .message("验证成功")
                .build();
    }
    
    /**
     * 用户设置信息
     */
    public static class UserSettingsInfo {
        private Long id;
        private String username;
        private String email;
        private Boolean emailVerified;
        private String avatarUrl;
        private String bio;
        private UserRole role;
        private Boolean totpEnabled;
        private Boolean totpRequired;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime lastLoginTime;
        private String lastLoginIp;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private UserSettingsInfo info = new UserSettingsInfo();
            
            public Builder id(Long id) { info.id = id; return this; }
            public Builder username(String username) { info.username = username; return this; }
            public Builder email(String email) { info.email = email; return this; }
            public Builder emailVerified(Boolean emailVerified) { info.emailVerified = emailVerified; return this; }
            public Builder avatarUrl(String avatarUrl) { info.avatarUrl = avatarUrl; return this; }
            public Builder bio(String bio) { info.bio = bio; return this; }
            public Builder role(UserRole role) { info.role = role; return this; }
            public Builder totpEnabled(Boolean totpEnabled) { info.totpEnabled = totpEnabled; return this; }
            public Builder totpRequired(Boolean totpRequired) { info.totpRequired = totpRequired; return this; }
            public Builder createdAt(java.time.LocalDateTime createdAt) { info.createdAt = createdAt; return this; }
            public Builder lastLoginTime(java.time.LocalDateTime lastLoginTime) { info.lastLoginTime = lastLoginTime; return this; }
            public Builder lastLoginIp(String lastLoginIp) { info.lastLoginIp = lastLoginIp; return this; }
            
            public UserSettingsInfo build() { return info; }
        }
        
        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public Boolean getEmailVerified() { return emailVerified; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getBio() { return bio; }
        public UserRole getRole() { return role; }
        public Boolean getTotpEnabled() { return totpEnabled; }
        public Boolean getTotpRequired() { return totpRequired; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public java.time.LocalDateTime getLastLoginTime() { return lastLoginTime; }
        public String getLastLoginIp() { return lastLoginIp; }
    }
    
    /**
     * 管理员访问信息
     */
    public static class AdminAccessInfo {
        private Boolean canAccess;
        private Boolean requiresTOTP;
        private Boolean totpEnabled;
        private String message;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private AdminAccessInfo info = new AdminAccessInfo();
            
            public Builder canAccess(Boolean canAccess) { info.canAccess = canAccess; return this; }
            public Builder requiresTOTP(Boolean requiresTOTP) { info.requiresTOTP = requiresTOTP; return this; }
            public Builder totpEnabled(Boolean totpEnabled) { info.totpEnabled = totpEnabled; return this; }
            public Builder message(String message) { info.message = message; return this; }
            
            public AdminAccessInfo build() { return info; }
        }
        
        // Getters
        public Boolean getCanAccess() { return canAccess; }
        public Boolean getRequiresTOTP() { return requiresTOTP; }
        public Boolean getTotpEnabled() { return totpEnabled; }
        public String getMessage() { return message; }
    }
}