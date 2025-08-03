package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.common.constant.SecurityConstants;
import com.myweb.website_core.common.security.exception.ValidationException;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码加密和验证服务
 * 
 * 提供密码相关的安全功能，包括：
 * - 密码加密和验证
 * - 密码强度检查
 * - 密码策略验证
 * - 历史密码检查
 * 
 * 符合GB/T 22239-2019 7.1.4.1 身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    // 密码强度验证正则表达式
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[" + Pattern.quote(SecurityConstants.PASSWORD_SPECIAL_CHARS) + "].*");
    
    /**
     * 加密密码
     * 使用BCrypt算法，强度为12
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码哈希
     * @throws ValidationException 如果密码不符合策略要求
     */
    public String encodePassword(String rawPassword) {
        log.debug("开始加密密码");
        
        // 验证密码策略
        validatePasswordPolicy(rawPassword);
        
        // 使用BCrypt加密，强度为12
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        log.debug("密码加密完成");
        return encodedPassword;
    }
    
    /**
     * 验证密码
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码哈希
     * @return 密码是否匹配
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            log.warn("密码验证失败：密码或哈希值为空");
            return false;
        }
        
        try {
            boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
            log.debug("密码验证结果: {}", matches ? "成功" : "失败");
            return matches;
        } catch (Exception e) {
            log.error("密码验证过程中发生异常", e);
            return false;
        }
    }
    
    /**
     * 验证密码策略
     * 检查密码是否符合安全策略要求
     * 
     * @param password 待验证的密码
     * @throws ValidationException 如果密码不符合策略要求
     */
    public void validatePasswordPolicy(String password) {
        List<String> errors = new ArrayList<>();
        
        // 检查密码是否为空
        if (password == null || password.trim().isEmpty()) {
            throw ValidationException.requiredFieldEmpty("password");
        }
        
        // 检查密码长度
        if (password.length() < SecurityConstants.PASSWORD_MIN_LENGTH) {
            errors.add(String.format("密码长度不能少于%d位", SecurityConstants.PASSWORD_MIN_LENGTH));
        }
        
        if (password.length() > SecurityConstants.PASSWORD_MAX_LENGTH) {
            errors.add(String.format("密码长度不能超过%d位", SecurityConstants.PASSWORD_MAX_LENGTH));
        }
        
        // 检查密码复杂度
        if (SecurityConstants.PASSWORD_REQUIRE_DIGITS && !DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个数字");
        }
        
        if (SecurityConstants.PASSWORD_REQUIRE_LOWERCASE && !LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个小写字母");
        }
        
        if (SecurityConstants.PASSWORD_REQUIRE_UPPERCASE && !UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个大写字母");
        }
        
        if (SecurityConstants.PASSWORD_REQUIRE_SPECIAL_CHARS && !SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个特殊字符 (" + SecurityConstants.PASSWORD_SPECIAL_CHARS + ")");
        }
        
        // 检查密码是否包含常见弱密码模式
        validatePasswordStrength(password, errors);
        
        // 如果有错误，抛出异常
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            log.warn("密码策略验证失败: {}", errorMessage);
            throw new ValidationException("password", password, "POLICY_CHECK", errorMessage);
        }
        
        log.debug("密码策略验证通过");
    }
    
    /**
     * 检查密码强度
     * 验证密码是否为常见弱密码
     * 
     * @param password 待检查的密码
     * @param errors 错误列表
     */
    private void validatePasswordStrength(String password, List<String> errors) {
        // 检查是否为纯数字
        if (password.matches("^\\d+$")) {
            errors.add("密码不能为纯数字");
        }
        
        // 检查是否为纯字母
        if (password.matches("^[a-zA-Z]+$")) {
            errors.add("密码不能为纯字母");
        }
        
        // 检查是否为键盘序列
        if (isKeyboardSequence(password)) {
            errors.add("密码不能为键盘序列");
        }
        
        // 检查是否为重复字符
        if (isRepeatingCharacters(password)) {
            errors.add("密码不能为重复字符");
        }
        
        // 检查是否为常见弱密码
        if (isCommonWeakPassword(password)) {
            errors.add("密码过于简单，请使用更复杂的密码");
        }
    }
    
    /**
     * 检查是否为键盘序列
     * 
     * @param password 密码
     * @return 是否为键盘序列
     */
    private boolean isKeyboardSequence(String password) {
        String[] sequences = {
            "123456789", "987654321", "qwertyuiop", "poiuytrewq",
            "asdfghjkl", "lkjhgfdsa", "zxcvbnm", "mnbvcxz",
            "abcdefghijklmnopqrstuvwxyz", "zyxwvutsrqponmlkjihgfedcba"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String sequence : sequences) {
            if (lowerPassword.contains(sequence.substring(0, Math.min(4, sequence.length())))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否为重复字符
     * 
     * @param password 密码
     * @return 是否为重复字符
     */
    private boolean isRepeatingCharacters(String password) {
        if (password.length() < 3) {
            return false;
        }
        
        // 检查是否有连续3个或以上相同字符
        for (int i = 0; i <= password.length() - 3; i++) {
            char c = password.charAt(i);
            if (password.charAt(i + 1) == c && password.charAt(i + 2) == c) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否为常见弱密码
     * 
     * @param password 密码
     * @return 是否为常见弱密码
     */
    private boolean isCommonWeakPassword(String password) {
        String[] commonPasswords = {
            "password", "123456", "123456789", "12345678", "12345",
            "1234567", "admin", "administrator", "root", "user",
            "guest", "test", "demo", "qwerty", "abc123",
            "password123", "admin123", "123123", "111111", "000000"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common) || lowerPassword.contains(common)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查历史密码
     * 验证新密码是否与最近使用的密码重复
     * 
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否与历史密码重复
     */
    public boolean isPasswordInHistory(Long userId, String newPassword) {
        if (userId == null || newPassword == null) {
            return false;
        }
        
        try {
            // 获取用户信息
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("用户不存在，无法检查历史密码: userId={}", userId);
                return false;
            }
            
            // 检查当前密码
            if (verifyPassword(newPassword, user.getPasswordHash())) {
                log.debug("新密码与当前密码相同: userId={}", userId);
                return true;
            }
            
            // TODO: 实现历史密码存储和检查机制
            // 这里需要一个单独的表来存储用户的历史密码哈希
            // 由于当前数据库结构中没有历史密码表，暂时只检查当前密码
            
            log.debug("历史密码检查通过: userId={}", userId);
            return false;
            
        } catch (Exception e) {
            log.error("检查历史密码时发生异常: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 验证并加密新密码
     * 综合验证密码策略、历史密码检查，然后加密
     * 
     * @param userId 用户ID（用于历史密码检查，可为null）
     * @param newPassword 新密码
     * @return 加密后的密码哈希
     * @throws ValidationException 如果密码不符合要求
     */
    public String validateAndEncodePassword(Long userId, String newPassword) {
        log.debug("开始验证并加密新密码: userId={}", userId);
        
        // 验证密码策略
        validatePasswordPolicy(newPassword);
        
        // 检查历史密码（如果提供了用户ID）
        if (userId != null && isPasswordInHistory(userId, newPassword)) {
            throw new ValidationException("password", newPassword, "HISTORY_CHECK", 
                "新密码不能与最近使用的密码相同");
        }
        
        // 加密密码
        String encodedPassword = encodePassword(newPassword);
        
        log.debug("密码验证和加密完成: userId={}", userId);
        return encodedPassword;
    }
    
    /**
     * 生成密码强度评分
     * 
     * @param password 密码
     * @return 密码强度评分（0-100）
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // 长度评分（最多30分）
        int length = password.length();
        if (length >= 8) score += 10;
        if (length >= 12) score += 10;
        if (length >= 16) score += 10;
        
        // 字符类型评分（每种类型10分，最多40分）
        if (DIGIT_PATTERN.matcher(password).matches()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).matches()) score += 10;
        if (UPPERCASE_PATTERN.matcher(password).matches()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score += 10;
        
        // 复杂度评分（最多30分）
        if (!isRepeatingCharacters(password)) score += 10;
        if (!isKeyboardSequence(password)) score += 10;
        if (!isCommonWeakPassword(password)) score += 10;
        
        return Math.min(score, 100);
    }
    
    /**
     * 获取密码强度描述
     * 
     * @param password 密码
     * @return 密码强度描述
     */
    public String getPasswordStrengthDescription(String password) {
        int strength = calculatePasswordStrength(password);
        
        if (strength >= 80) {
            return "强";
        } else if (strength >= 60) {
            return "中等";
        } else if (strength >= 40) {
            return "较弱";
        } else {
            return "弱";
        }
    }
    
    /**
     * 验证密码是否需要更新
     * 基于密码创建时间和安全策略判断
     * 
     * @param user 用户对象
     * @return 是否需要更新密码
     */
    public boolean isPasswordUpdateRequired(User user) {
        if (user == null || user.getUpdatedAt() == null) {
            return false;
        }
        
        // TODO: 实现密码过期策略
        // 例如：密码超过90天需要更新
        // LocalDateTime passwordExpiry = user.getUpdatedAt().plusDays(90);
        // return LocalDateTime.now().isAfter(passwordExpiry);
        
        return false;
    }
}