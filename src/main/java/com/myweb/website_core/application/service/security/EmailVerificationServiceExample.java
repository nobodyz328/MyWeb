package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import org.springframework.stereotype.Component;

/**
 * EmailVerificationService使用示例
 * 
 * 这个类展示了如何使用EmailVerificationService进行邮箱验证
 */
@Component
public class EmailVerificationServiceExample {
    
    private final EmailVerificationService emailVerificationService;
    
    public EmailVerificationServiceExample(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }
    
    /**
     * 用户注册时发送验证码的示例
     */
    public void handleUserRegistration(String email) {
        try {
            // 发送注册验证码
            emailVerificationService.sendRegistrationVerificationCode(email)
                .thenRun(() -> {
                    System.out.println("注册验证码已发送到: " + email);
                })
                .exceptionally(throwable -> {
                    System.err.println("发送验证码失败: " + throwable.getMessage());
                    return null;
                });
        } catch (Exception e) {
            System.err.println("发送验证码异常: " + e.getMessage());
        }
    }
    
    /**
     * 验证用户输入的验证码示例
     */
    public boolean verifyRegistrationCode(String email, String code) {
        try {
            return emailVerificationService.verifyCode(email, code, 
                EmailVerificationService.VerificationType.REGISTRATION);
        } catch (Exception e) {
            System.err.println("验证码验证异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 密码重置时发送验证码的示例
     */
    public void handlePasswordReset(String email) {
        try {
            // 发送密码重置验证码
            emailVerificationService.sendPasswordResetVerificationCode(email)
                .thenRun(() -> {
                    System.out.println("密码重置验证码已发送到: " + email);
                })
                .exceptionally(throwable -> {
                    System.err.println("发送验证码失败: " + throwable.getMessage());
                    return null;
                });
        } catch (Exception e) {
            System.err.println("发送验证码异常: " + e.getMessage());
        }
    }
    
    /**
     * 验证密码重置验证码示例
     */
    public boolean verifyPasswordResetCode(String email, String code) {
        try {
            return emailVerificationService.verifyCode(email, code, 
                EmailVerificationService.VerificationType.PASSWORD_RESET);
        } catch (Exception e) {
            System.err.println("验证码验证异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查验证码是否存在的示例
     */
    public boolean checkCodeExists(String email) {
        try {
            return emailVerificationService.hasValidCode(email, 
                EmailVerificationService.VerificationType.REGISTRATION);
        } catch (Exception e) {
            System.err.println("检查验证码异常: " + e.getMessage());
            return false;
        }
    }
}