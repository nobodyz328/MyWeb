package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.common.exception.RateLimitExceededException;
import com.myweb.website_core.common.security.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 邮箱验证服务
 * 提供验证码生成、发送和验证功能
 * 支持注册验证和密码重置场景
 */
@Slf4j
@Service
public class EmailVerificationService {
    
    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    
    // Redis键前缀
    private static final String VERIFICATION_CODE_PREFIX = "email:verification:";
    private static final String RATE_LIMIT_PREFIX = "email:rate_limit:";
    
    // 验证码配置
    private static final int CODE_LENGTH = 6;
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5);
    
    // 频率限制配置
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final int MAX_REQUESTS_PER_MINUTE = 1;
    private static final Duration DAILY_LIMIT_WINDOW = Duration.ofHours(24);
    private static final int MAX_REQUESTS_PER_DAY = 10;
    
    // 验证码字符集
    private static final String CODE_CHARACTERS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    public EmailVerificationService(JavaMailSender mailSender, RedisTemplate<String, String> redisTemplate) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 发送注册验证码
     * 
     * @param email 邮箱地址
     * @return 异步执行结果
     */
    @Async
    public CompletableFuture<Void> sendRegistrationVerificationCode(String email) {
        log.info("发送注册验证码到邮箱: {}", email);
        
        // 检查频率限制
        checkRateLimit(email);
        
        // 生成验证码
        String code = generateVerificationCode();
        
        // 存储验证码到Redis
        storeVerificationCode(email, code, VerificationType.REGISTRATION);
        
        // 发送邮件
        sendRegistrationEmail(email, code);
        
        log.info("注册验证码已发送到邮箱: {}", email);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 发送密码重置验证码
     * 
     * @param email 邮箱地址
     * @return 异步执行结果
     */
    @Async
    public CompletableFuture<Void> sendPasswordResetVerificationCode(String email) {
        log.info("发送密码重置验证码到邮箱: {}", email);
        
        // 检查频率限制
        checkRateLimit(email);
        
        // 生成验证码
        String code = generateVerificationCode();
        
        // 存储验证码到Redis
        storeVerificationCode(email, code, VerificationType.PASSWORD_RESET);
        
        // 发送邮件
        sendPasswordResetEmail(email, code);
        
        log.info("密码重置验证码已发送到邮箱: {}", email);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 验证邮箱验证码
     * 
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证类型
     * @return 验证是否成功
     */
    public boolean verifyCode(String email, String code, VerificationType type) {
        if (email == null || code == null || type == null) {
            log.warn("验证码验证失败: 参数不能为空");
            return false;
        }
        
        String key = getVerificationKey(email, type);
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            log.warn("验证码验证失败: 验证码不存在或已过期, email: {}, type: {}", email, type);
            return false;
        }
        
        boolean isValid = storedCode.equals(code);
        
        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.delete(key);
            log.info("验证码验证成功, email: {}, type: {}", email, type);
        } else {
            log.warn("验证码验证失败: 验证码错误, email: {}, type: {}", email, type);
        }
        
        return isValid;
    }
    
    /**
     * 检查验证码是否存在且未过期
     * 
     * @param email 邮箱地址
     * @param type 验证类型
     * @return 验证码是否存在
     */
    public boolean hasValidCode(String email, VerificationType type) {
        String key = getVerificationKey(email, type);
        return redisTemplate.hasKey(key);
    }
    
    /**
     * 生成验证码
     * 
     * @return 6位数字验证码
     */
    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARACTERS.charAt(RANDOM.nextInt(CODE_CHARACTERS.length())));
        }
        return code.toString();
    }
    
    /**
     * 存储验证码到Redis
     * 
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证类型
     */
    private void storeVerificationCode(String email, String code, VerificationType type) {
        String key = getVerificationKey(email, type);
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRATION);
        log.debug("验证码已存储到Redis, key: {}, expiration: {}", key, CODE_EXPIRATION);
    }
    
    /**
     * 获取验证码Redis键
     * 
     * @param email 邮箱地址
     * @param type 验证类型
     * @return Redis键
     */
    private String getVerificationKey(String email, VerificationType type) {
        return VERIFICATION_CODE_PREFIX + type.name().toLowerCase() + ":" + email;
    }
    
    /**
     * 检查发送频率限制
     * 
     * @param email 邮箱地址
     */
    private void checkRateLimit(String email) {
        // 检查每分钟限制
        String minuteKey = RATE_LIMIT_PREFIX + "minute:" + email;
        String minuteCount = redisTemplate.opsForValue().get(minuteKey);
        
        if (minuteCount != null && Integer.parseInt(minuteCount) >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("邮箱验证码发送频率超限(每分钟): {}", email);
            throw new RateLimitExceededException("发送验证码过于频繁，请稍后再试");
        }
        
        // 检查每日限制
        String dailyKey = RATE_LIMIT_PREFIX + "daily:" + email;
        String dailyCount = redisTemplate.opsForValue().get(dailyKey);
        
        if (dailyCount != null && Integer.parseInt(dailyCount) >= MAX_REQUESTS_PER_DAY) {
            log.warn("邮箱验证码发送频率超限(每日): {}", email);
            throw new RateLimitExceededException("今日验证码发送次数已达上限，请明日再试");
        }
        
        // 更新计数器
        if (minuteCount == null) {
            redisTemplate.opsForValue().set(minuteKey, "1", RATE_LIMIT_WINDOW);
        } else {
            redisTemplate.opsForValue().increment(minuteKey);
        }
        
        if (dailyCount == null) {
            redisTemplate.opsForValue().set(dailyKey, "1", DAILY_LIMIT_WINDOW);
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
        }
    }
    
    /**
     * 发送注册验证邮件
     * 
     * @param email 邮箱地址
     * @param code 验证码
     */
    private void sendRegistrationEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("MyWeb - 注册验证码");
            message.setText(buildRegistrationEmailContent(code));
            
            mailSender.send(message);
            log.debug("注册验证邮件发送成功: {}", email);
        } catch (Exception e) {
            log.error("注册验证邮件发送失败: {}", email, e);
            throw new ValidationException("验证码发送失败，请稍后重试");
        }
    }
    
    /**
     * 发送密码重置验证邮件
     * 
     * @param email 邮箱地址
     * @param code 验证码
     */
    private void sendPasswordResetEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("MyWeb - 密码重置验证码");
            message.setText(buildPasswordResetEmailContent(code));
            
            mailSender.send(message);
            log.debug("密码重置验证邮件发送成功: {}", email);
        } catch (Exception e) {
            log.error("密码重置验证邮件发送失败: {}", email, e);
            throw new ValidationException("验证码发送失败，请稍后重试");
        }
    }
    
    /**
     * 构建注册验证邮件内容
     * 
     * @param code 验证码
     * @return 邮件内容
     */
    private String buildRegistrationEmailContent(String code) {
        return String.format("""
            亲爱的用户，
            
            欢迎注册MyWeb博客系统！
            
            您的注册验证码是：%s
            
            验证码有效期为5分钟，请及时使用。
            如果您没有进行注册操作，请忽略此邮件。
            
            感谢您的使用！
            
            MyWeb团队
            """, code);
    }
    
    /**
     * 构建密码重置验证邮件内容
     * 
     * @param code 验证码
     * @return 邮件内容
     */
    private String buildPasswordResetEmailContent(String code) {
        return String.format("""
            亲爱的用户，
            
            您正在进行密码重置操作。
            
            您的验证码是：%s
            
            验证码有效期为5分钟，请及时使用。
            如果您没有进行密码重置操作，请立即联系我们的客服。
            
            为了您的账户安全，请不要将验证码告诉他人。
            
            MyWeb团队
            """, code);
    }
    
    /**
     * 验证类型枚举
     */
    public enum VerificationType {
        REGISTRATION,    // 注册验证
        PASSWORD_RESET   // 密码重置
    }
}