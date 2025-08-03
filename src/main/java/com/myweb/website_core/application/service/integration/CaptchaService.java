package com.myweb.website_core.application.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

/**
 * 验证码服务
 * 
 * 提供验证码生成、验证和管理功能，包括：
 * - 图形验证码生成
 * - 验证码验证
 * - Redis缓存管理
 * 
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class CaptchaService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final Random random = new Random();
    
    /**
     * 验证码有效期（分钟）
     */
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    
    /**
     * 验证码长度
     */
    private static final int CAPTCHA_LENGTH = 4;
    
    @Autowired
    public CaptchaService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 生成验证码
     * 
     * @param sessionId 会话ID
     * @return 验证码字符串
     */
    public String generateCaptcha(String sessionId) {
        try {
            // 生成4位数字验证码
            StringBuilder captcha = new StringBuilder();
            for (int i = 0; i < CAPTCHA_LENGTH; i++) {
                captcha.append(random.nextInt(10));
            }
            
            String captchaCode = captcha.toString();
            String key = "captcha:" + sessionId;
            
            // 存储到Redis，5分钟过期
            redisTemplate.opsForValue().set(key, captchaCode, Duration.ofMinutes(CAPTCHA_EXPIRE_MINUTES));
            
            log.debug("为会话 {} 生成验证码", sessionId);
            return captchaCode;
            
        } catch (Exception e) {
            log.error("生成验证码失败: sessionId={}", sessionId, e);
            throw new RuntimeException("生成验证码失败", e);
        }
    }
    
    /**
     * 验证验证码
     * 
     * @param sessionId 会话ID
     * @param inputCode 用户输入的验证码
     * @return 验证是否成功
     */
    public boolean validateCaptcha(String sessionId, String inputCode) {
        try {
            if (sessionId == null || inputCode == null) {
                log.warn("验证码验证失败: 参数为空");
                return false;
            }
            
            String key = "captcha:" + sessionId;
            String storedCode = redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                log.warn("验证码验证失败: 验证码不存在或已过期, sessionId={}", sessionId);
                return false;
            }
            
            boolean isValid = storedCode.equals(inputCode.trim());
            
            if (isValid) {
                // 验证成功后删除验证码
                redisTemplate.delete(key);
                log.debug("验证码验证成功: sessionId={}", sessionId);
            } else {
                log.warn("验证码验证失败: 验证码不匹配, sessionId={}, input={}", sessionId, inputCode);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("验证码验证过程中发生异常: sessionId={}, input={}", sessionId, inputCode, e);
            return false;
        }
    }
    
    /**
     * 验证验证码（重载方法，支持LoginRequest中的captcha字段）
     * 
     * @param captcha 验证码字符串，格式为 "sessionId:code"
     * @return 验证是否成功
     */
    public boolean validateCaptcha(String captcha) {
        try {
            if (captcha == null || !captcha.contains(":")) {
                log.warn("验证码格式错误: {}", captcha);
                return false;
            }
            
            String[] parts = captcha.split(":", 2);
            if (parts.length != 2) {
                log.warn("验证码格式错误: {}", captcha);
                return false;
            }
            
            String sessionId = parts[0];
            String code = parts[1];
            
            return validateCaptcha(sessionId, code);
            
        } catch (Exception e) {
            log.error("验证码验证失败: captcha={}", captcha, e);
            return false;
        }
    }
    
    /**
     * 清除验证码
     * 
     * @param sessionId 会话ID
     */
    public void clearCaptcha(String sessionId) {
        try {
            if (sessionId != null) {
                String key = "captcha:" + sessionId;
                redisTemplate.delete(key);
                log.debug("清除验证码: sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("清除验证码失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 检查是否存在验证码
     * 
     * @param sessionId 会话ID
     * @return 是否存在验证码
     */
    public boolean hasCaptcha(String sessionId) {
        try {
            if (sessionId == null) {
                return false;
            }
            
            String key = "captcha:" + sessionId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            
        } catch (Exception e) {
            log.error("检查验证码存在性失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 获取验证码剩余有效时间（秒）
     * 
     * @param sessionId 会话ID
     * @return 剩余有效时间，如果不存在则返回-1
     */
    public long getCaptchaExpireTime(String sessionId) {
        try {
            if (sessionId == null) {
                return -1;
            }
            
            String key = "captcha:" + sessionId;
            Long expire = redisTemplate.getExpire(key);
            return expire != null ? expire : -1;
            
        } catch (Exception e) {
            log.error("获取验证码过期时间失败: sessionId={}", sessionId, e);
            return -1;
        }
    }
}