package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.common.security.exception.TOTPValidationException;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TOTP管理服务
 * 
 * 提供TOTP相关的管理功能，包括：
 * - 启用/禁用TOTP
 * - 生成TOTP密钥和二维码
 * - TOTP验证
 * - 管理员强制TOTP策略
 * 
 * 符合GB/T 22239-2019身份鉴别要求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TOTPManagementService {
    
    private final TOTPService totpService;
    private final UserRepository userRepository;
    
    /**
     * 为用户生成TOTP设置信息
     * 
     * @param userId 用户ID
     * @return TOTP设置信息
     */
    @Transactional(readOnly = true)
    public TOTPSetupInfo generateTOTPSetup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 如果用户已经有TOTP密钥，返回现有信息
        if (user.getTotpSecret() != null && !user.getTotpSecret().isEmpty()) {
            return TOTPSetupInfo.builder()
                    .secret(user.getTotpSecret())
                    .qrCodeUri(totpService.getTOTPUri(user.getUsername(), user.getTotpSecret()))
                    .enabled(user.getTotpEnabled())
                    .required(isRequiredForUser(user))
                    .build();
        }
        
        // 生成新的TOTP密钥
        String secret = totpService.generateSecret();
        String qrCodeUri = totpService.getTOTPUri(user.getUsername(), secret);
        
        return TOTPSetupInfo.builder()
                .secret(secret)
                .qrCodeUri(qrCodeUri)
                .enabled(false)
                .required(isRequiredForUser(user))
                .build();
    }
    
    /**
     * 生成TOTP二维码图片
     * 
     * @param userId 用户ID
     * @param width 二维码宽度
     * @param height 二维码高度
     * @return 二维码图片字节数组
     */
    @Transactional(readOnly = true)
    public byte[] generateTOTPQRCode(Long userId, int width, int height) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        String secret = user.getTotpSecret();
        if (secret == null || secret.isEmpty()) {
            throw new RuntimeException("用户尚未设置TOTP密钥");
        }
        
        return totpService.generateQRCode(user.getUsername(), secret, width, height);
    }
    
    /**
     * 启用TOTP
     * 
     * @param userId 用户ID
     * @param secret TOTP密钥
     * @param verificationCode 验证码
     */
    @Transactional
    public void enableTOTP(Long userId, String secret, String verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证TOTP代码
        if (!totpService.validateTOTP(secret, verificationCode, user.getUsername())) {
            throw new TOTPValidationException("TOTP验证码不正确");
        }
        
        // 保存TOTP密钥并启用
        user.setTotpSecret(secret);
        user.setTotpEnabled(true);
        userRepository.save(user);
        
        log.info("用户 {} 启用TOTP成功", user.getUsername());
    }
    
    /**
     * 禁用TOTP
     * 
     * @param userId 用户ID
     * @param verificationCode 当前TOTP验证码
     */
    @Transactional
    public void disableTOTP(Long userId, String verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查是否为管理员（管理员不能禁用TOTP）
        if (isRequiredForUser(user)) {
            throw new RuntimeException("管理员账户必须启用TOTP，无法禁用");
        }
        
        // 验证当前TOTP代码
        if (user.getTotpSecret() != null && user.getTotpEnabled()) {
            if (!totpService.validateTOTP(user.getTotpSecret(), verificationCode, user.getUsername())) {
                throw new TOTPValidationException("TOTP验证码不正确");
            }
        }
        
        // 禁用TOTP
        user.setTotpEnabled(false);
        userRepository.save(user);
        
        log.info("用户 {} 禁用TOTP成功", user.getUsername());
    }
    
    /**
     * 重置TOTP密钥
     * 
     * @param userId 用户ID
     * @param currentVerificationCode 当前TOTP验证码
     * @return 新的TOTP设置信息
     */
    @Transactional
    public TOTPSetupInfo resetTOTPSecret(Long userId, String currentVerificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 如果已启用TOTP，需要验证当前代码
        if (user.getTotpEnabled() && user.getTotpSecret() != null) {
            if (!totpService.validateTOTP(user.getTotpSecret(), currentVerificationCode, user.getUsername())) {
                throw new TOTPValidationException("当前TOTP验证码不正确");
            }
        }
        
        // 生成新的TOTP密钥
        String newSecret = totpService.generateSecret();
        String qrCodeUri = totpService.getTOTPUri(user.getUsername(), newSecret);
        
        // 更新用户信息（重置后需要重新启用）
        user.setTotpSecret(newSecret);
        user.setTotpEnabled(false);
        userRepository.save(user);
        
        log.info("用户 {} 重置TOTP密钥成功", user.getUsername());
        
        return TOTPSetupInfo.builder()
                .secret(newSecret)
                .qrCodeUri(qrCodeUri)
                .enabled(false)
                .required(isRequiredForUser(user))
                .build();
    }
    
    /**
     * 验证用户TOTP代码
     * 
     * @param userId 用户ID
     * @param verificationCode 验证码
     * @return 验证是否成功
     */
    @Transactional(readOnly = true)
    public boolean verifyTOTP(Long userId, String verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (!user.getTotpEnabled() || user.getTotpSecret() == null) {
            throw new RuntimeException("用户未启用TOTP");
        }
        
        return totpService.validateTOTP(user.getTotpSecret(), verificationCode, user.getUsername());
    }
    
    /**
     * 获取用户TOTP状态
     * 
     * @param userId 用户ID
     * @return TOTP状态信息
     */
    @Transactional(readOnly = true)
    public TOTPStatusInfo getTOTPStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return TOTPStatusInfo.builder()
                .enabled(user.getTotpEnabled())
                .configured(user.getTotpSecret() != null && !user.getTotpSecret().isEmpty())
                .required(isRequiredForUser(user))
                .remainingTime(totpService.getRemainingTimeInWindow())
                .build();
    }
    
    /**
     * 检查用户是否需要强制启用TOTP
     * 
     * @param user 用户对象
     * @return 是否需要强制启用
     */
    private boolean isRequiredForUser(User user) {
        return user.getRole() == UserRole.ADMIN || user.hasSystemAdminPermission();
    }
    
    /**
     * TOTP设置信息
     */
    public static class TOTPSetupInfo {
        private String secret;
        private String qrCodeUri;
        private Boolean enabled;
        private Boolean required;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private TOTPSetupInfo info = new TOTPSetupInfo();
            
            public Builder secret(String secret) {
                info.secret = secret;
                return this;
            }
            
            public Builder qrCodeUri(String qrCodeUri) {
                info.qrCodeUri = qrCodeUri;
                return this;
            }
            
            public Builder enabled(Boolean enabled) {
                info.enabled = enabled;
                return this;
            }
            
            public Builder required(Boolean required) {
                info.required = required;
                return this;
            }
            
            public TOTPSetupInfo build() {
                return info;
            }
        }
        
        // Getters
        public String getSecret() { return secret; }
        public String getQrCodeUri() { return qrCodeUri; }
        public Boolean getEnabled() { return enabled; }
        public Boolean getRequired() { return required; }
    }
    
    /**
     * TOTP状态信息
     */
    public static class TOTPStatusInfo {
        private Boolean enabled;
        private Boolean configured;
        private Boolean required;
        private Integer remainingTime;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private TOTPStatusInfo info = new TOTPStatusInfo();
            
            public Builder enabled(Boolean enabled) {
                info.enabled = enabled;
                return this;
            }
            
            public Builder configured(Boolean configured) {
                info.configured = configured;
                return this;
            }
            
            public Builder required(Boolean required) {
                info.required = required;
                return this;
            }
            
            public Builder remainingTime(Integer remainingTime) {
                info.remainingTime = remainingTime;
                return this;
            }
            
            public TOTPStatusInfo build() {
                return info;
            }
        }
        
        // Getters
        public Boolean getEnabled() { return enabled; }
        public Boolean getConfigured() { return configured; }
        public Boolean getRequired() { return required; }
        public Integer getRemainingTime() { return remainingTime; }
    }
}