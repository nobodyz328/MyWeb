package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.common.exception.security.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 密码服务
 * 
 * 提供密码验证、加密和安全检查功能
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {
    
    private final PasswordEncoder passwordEncoder;
    
    // 密码强度正则表达式
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,50}$"
    );
    
    // 弱密码列表（更全面的弱密码检测）
    private static final String[] WEAK_PASSWORDS = {
            // 纯数字序列
            "12345678", "123456789", "1234567890", "87654321", "11111111", "22222222", "33333333",
            "44444444", "55555555", "66666666", "77777777", "88888888", "99999999", "00000000",
            
            // 常见密码
            "password", "password123", "password1", "password12", "passw0rd", "p@ssword", "p@ssw0rd",
            "admin123", "administrator", "admin1234", "admin888", "admin666", "root123", "root1234",
            "user1234", "user123", "test1234", "test123", "welcome123", "welcome1", "welcome12",
            
            // 键盘序列
            "qwerty123", "qwerty12", "qwerty1", "qwertyui", "asdfgh123", "asdfghjk", "zxcvbn123",
            "qazwsx123", "qazwsxedc", "123qwe", "123qwer", "qwe123", "abc123", "abc1234",
            
            // 日期相关
            "20240101", "20230101", "20220101", "20210101", "19900101", "19800101", "birthday",
            
            // 中文拼音
            "woaini123", "nihao123", "zhongguo", "beijing123", "shanghai", "guangzhou",
            
            // 公司/网站相关
            "company123", "website123", "internet", "computer", "laptop123", "mobile123",
            
            // 其他常见弱密码
            "letmein123", "changeme", "secret123", "private123", "master123", "superman",
            "batman123", "dragon123", "monkey123", "shadow123", "sunshine", "princess",
            "football", "baseball", "basketball", "soccer123", "love1234", "family123"
    };
    
    // 弱密码模式（正则表达式）
    private static final Pattern[] WEAK_PATTERNS = {
            // 重复字符（3个或以上相同字符）
            Pattern.compile("(.)\\1{2,}"),
            // 连续数字（4个或以上）
            Pattern.compile("(0123|1234|2345|3456|4567|5678|6789|9876|8765|7654|6543|5432|4321|3210)"),
            // 连续字母（4个或以上）
            Pattern.compile("(?i)(abcd|bcde|cdef|defg|efgh|fghi|ghij|hijk|ijkl|jklm|klmn|lmno|mnop|nopq|opqr|pqrs|qrst|rstu|stuv|tuvw|uvwx|vwxy|wxyz|zyxw|yxwv|xwvu|wvut|vuts|utsr|tsrq|srqp|rqpo|qpon|ponm|onml|nmlk|mlkj|lkji|kjih|jihg|ihgf|hgfe|gfed|fedc|edcb|dcba)"),
            // 键盘序列
            Pattern.compile("(?i)(qwer|wert|erty|rtyu|tyui|yuio|uiop|asdf|sdfg|dfgh|fghj|ghjk|hjkl|zxcv|xcvb|cvbn|vbnm)"),
            // 纯数字后跟简单字符
            Pattern.compile("^\\d{6,}[a-zA-Z]{1,2}$"),
            // 简单字符后跟纯数字
            Pattern.compile("^[a-zA-Z]{1,4}\\d{4,}$")
    };
    
    /**
     * 验证并加密密码
     * 
     * @param currentPassword 当前密码（用于密码修改，新注册时为null）
     * @param newPassword 新密码
     * @return 加密后的密码
     * @throws ValidationException 如果密码不符合要求
     */
    public String validateAndEncodePassword(String currentPassword, String newPassword) {
        log.debug("开始密码验证和加密");
        
        // 1. 基本格式验证
        validatePasswordFormat(newPassword);
        
        // 2. 密码强度验证
        validatePasswordStrength(newPassword);
        
        // 3. 弱密码检查
        validateWeakPassword(newPassword);
        
        // 4. 如果是密码修改，检查新旧密码不能相同
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new ValidationException("password", newPassword, "SAME_AS_CURRENT", 
                    "新密码不能与当前密码相同");
        }
        
        // 5. 加密密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        log.debug("密码验证和加密完成");
        
        return encodedPassword;
    }
    
    /**
     * 验证密码格式
     * 
     * @param password 密码
     * @throws ValidationException 如果格式不正确
     */
    private void validatePasswordFormat(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("password", password, "EMPTY", "密码不能为空");
        }
        
        if (password.length() < 8) {
            throw new ValidationException("password", password, "TOO_SHORT", 
                    "密码长度不能少于8个字符");
        }
        
        if (password.length() > 50) {
            throw new ValidationException("password", password, "TOO_LONG", 
                    "密码长度不能超过50个字符");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("password", password, "INVALID_FORMAT", 
                    "密码必须包含至少一个字母和一个数字，可包含特殊字符@$!%*#?&");
        }
    }
    
    /**
     * 验证密码强度
     * 
     * @param password 密码
     * @throws ValidationException 如果密码强度不够
     */
    private void validatePasswordStrength(String password) {
        int score = calculatePasswordStrength(password);
        
        if (score < 3) {
            throw new ValidationException("password", password, "WEAK_PASSWORD", 
                    "密码强度不够，请使用更复杂的密码");
        }
    }
    
    /**
     * 计算密码强度分数
     * 
     * @param password 密码
     * @return 强度分数（0-5）
     */
    private int calculatePasswordStrength(String password) {
        int score = 0;
        
        // 长度加分
        if (password.length() >= 12) score++;
        
        // 包含小写字母
        if (password.matches(".*[a-z].*")) score++;
        
        // 包含大写字母
        if (password.matches(".*[A-Z].*")) score++;
        
        // 包含数字
        if (password.matches(".*\\d.*")) score++;
        
        // 包含特殊字符
        if (password.matches(".*[@$!%*#?&].*")) score++;
        
        return score;
    }
    
    /**
     * 检查是否为弱密码
     * 
     * @param password 密码
     * @throws ValidationException 如果是弱密码
     */
    private void validateWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        
        // 检查弱密码列表
        for (String weakPassword : WEAK_PASSWORDS) {
            if (lowerPassword.equals(weakPassword) || lowerPassword.contains(weakPassword)) {
                throw new ValidationException("password", password, "WEAK_PASSWORD", 
                        "密码包含常见弱密码，请使用更安全的密码");
            }
        }
        
        // 检查弱密码模式
        for (Pattern pattern : WEAK_PATTERNS) {
            if (pattern.matcher(password).find()) {
                throw new ValidationException("password", password, "WEAK_PASSWORD", 
                        "密码包含不安全的字符序列，请使用更复杂的密码");
            }
        }
        
        // 检查是否只包含用户名相关信息（如果有的话）
        if (containsPersonalInfo(password)) {
            throw new ValidationException("password", password, "WEAK_PASSWORD", 
                    "密码不能包含个人信息，请使用更安全的密码");
        }
    }
    
    /**
     * 检查密码是否包含个人信息
     * 
     * @param password 密码
     * @return 是否包含个人信息
     */
    private boolean containsPersonalInfo(String password) {
        String lowerPassword = password.toLowerCase();
        
        // 检查是否包含常见的个人信息模式
        String[] personalPatterns = {
                "name", "user", "admin", "root", "guest", "test", "demo",
                "birthday", "birth", "age", "year", "month", "day",
                "phone", "mobile", "email", "mail", "address", "home",
                "work", "office", "company", "school", "university"
        };
        
        for (String pattern : personalPatterns) {
            if (lowerPassword.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 验证密码是否匹配
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 加密密码
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * 密码验证规则类
     */
    public static class PasswordValidationRules {
        public final int minLength = 8;
        public final int maxLength = 50;
        public final String pattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,50}$";
        public final String[] weakPasswords = WEAK_PASSWORDS;
        public final String[] weakPatterns = {
                "(.)\\1{2,}", // 重复字符
                "(0123|1234|2345|3456|4567|5678|6789|9876|8765|7654|6543|5432|4321|3210)", // 连续数字
                "(?i)(abcd|bcde|cdef|defg|efgh|fghi|ghij|hijk|ijkl|jklm|klmn|lmno|mnop|nopq|opqr|pqrs|qrst|rstu|stuv|tuvw|uvwx|vwxy|wxyz)", // 连续字母
                "(?i)(qwer|wert|erty|rtyu|tyui|yuio|uiop|asdf|sdfg|dfgh|fghj|ghjk|hjkl|zxcv|xcvb|cvbn|vbnm)" // 键盘序列
        };
        
        public String getRequirements() {
            return "密码必须：8-50个字符，包含字母和数字，可包含特殊字符@$!%*#?&，不能使用常见弱密码";
        }
    }
}