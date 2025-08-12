package com.myweb.website_core.common.util;

import java.util.regex.Pattern;

/**
 * 验证工具类
 * <p>
 * 提供常用的验证方法，包括：
 * - 邮箱格式验证
 * - 用户名格式验证
 * - 密码强度验证
 * - 手机号格式验证
 * - URL格式验证
 * - IP地址验证
 * - 身份证号验证
 * - 数字验证
 * <p>
 * 符合需求：10.2 - 提供常用验证方法
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public class ValidationUtils {
    
    // ==================== 正则表达式模式 ====================
    
    /**
     * 邮箱格式正则表达式
     */
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._+%-]*[a-zA-Z0-9]@[a-zA-Z0-9][a-zA-Z0-9.-]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}$|^[a-zA-Z0-9]@[a-zA-Z0-9][a-zA-Z0-9.-]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}$");
    
    /**
     * 用户名格式正则表达式（3-50位字母、数字、下划线、连字符）
     */
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_\\-]{3,50}$");
    
    /**
     * 强密码正则表达式（至少8位，包含大小写字母、数字、特殊字符中的至少3种）
     */
    private static final Pattern STRONG_PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$");
    
    /**
     * 中国大陆手机号正则表达式
     */
    private static final Pattern CHINA_MOBILE_PATTERN = 
        Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * URL格式正则表达式
     */
    private static final Pattern URL_PATTERN = 
        Pattern.compile("^https?://[a-zA-Z0-9.-]+(:[0-9]+)?(?:/[^\\s]*)?$");
    
    /**
     * IPv4地址正则表达式
     */
    private static final Pattern IPV4_PATTERN = 
        Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    
    /**
     * IPv6地址正则表达式
     */
    private static final Pattern IPV6_PATTERN = 
        Pattern.compile("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$");
    
    /**
     * 中国大陆身份证号正则表达式（18位）
     */
    private static final Pattern CHINA_ID_CARD_PATTERN = 
        Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    
    /**
     * 正整数正则表达式
     */
    private static final Pattern POSITIVE_INTEGER_PATTERN = 
        Pattern.compile("^[1-9]\\d*$");
    
    /**
     * 非负整数正则表达式
     */
    private static final Pattern NON_NEGATIVE_INTEGER_PATTERN = 
        Pattern.compile("^(0|[1-9]\\d*)$");
    
    /**
     * 数字（包含小数）正则表达式
     */
    private static final Pattern NUMBER_PATTERN = 
        Pattern.compile("^-?\\d+(\\.\\d+)?$");
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private ValidationUtils() {
        // 工具类不允许实例化
    }
    
    // ==================== 邮箱验证 ====================
    
    /**
     * 验证邮箱格式是否正确
     * 
     * @param email 邮箱地址
     * @return 如果格式正确返回true
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // 长度检查
        if (email.length() > 100) {
            return false;
        }
        
        // 不允许连续的点
        if (email.contains("..")) {
            return false;
        }
        
        // 正则表达式验证
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * 验证邮箱格式（严格模式）
     * 
     * @param email 邮箱地址
     * @return 如果格式正确返回true
     */
    public static boolean isValidEmailStrict(String email) {
        if (!isValidEmail(email)) {
            return false;
        }
        
        // 额外的严格检查
        String trimmedEmail = email.trim().toLowerCase();
        
        // 检查是否以点开头或结尾
        if (trimmedEmail.startsWith(".") || trimmedEmail.endsWith(".")) {
            return false;
        }
        
        // 检查@符号前后是否有点
        String[] parts = trimmedEmail.split("@");
        if (parts.length != 2) {
            return false;
        }
        
        String localPart = parts[0];
        String domainPart = parts[1];
        
        // 本地部分不能以点开头或结尾
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            return false;
        }
        
        // 域名部分不能以点开头或结尾
        if (domainPart.startsWith(".") || domainPart.endsWith(".")) {
            return false;
        }
        
        return true;
    }
    
    // ==================== 用户名验证 ====================
    
    /**
     * 验证用户名格式是否正确
     * 
     * @param username 用户名
     * @return 如果格式正确返回true
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }
    
    /**
     * 检查用户名是否为保留名称
     * 
     * @param username 用户名
     * @return 如果是保留名称返回true
     */
    public static boolean isReservedUsername(String username) {
        if (username == null) {
            return false;
        }
        
        String lowerUsername = username.toLowerCase().trim();
        String[] reservedNames = {
            "admin", "administrator", "root", "system", "user", "guest",
            "test", "demo", "api", "www", "mail", "ftp", "blog", "forum",
            "support", "help", "info", "contact", "about", "news", "service",
            "null", "undefined", "anonymous", "public", "private", "default",
            "config", "settings", "profile", "account", "login", "register",
            "signup", "signin", "logout", "password", "security", "privacy"
        };
        
        for (String reserved : reservedNames) {
            if (reserved.equals(lowerUsername)) {
                return true;
            }
        }
        
        return false;
    }
    
    // ==================== 密码验证 ====================
    
    /**
     * 验证密码强度是否符合要求
     * 
     * @param password 密码
     * @return 如果密码强度符合要求返回true
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        // 长度检查
        if (password.length() < 8 || password.length() > 128) {
            return false;
        }
        
        // 复杂度检查
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?".indexOf(c) >= 0);
        
        // 至少包含3种字符类型
        int complexity = 0;
        if (hasLower) complexity++;
        if (hasUpper) complexity++;
        if (hasDigit) complexity++;
        if (hasSpecial) complexity++;
        
        if (complexity < 3) {
            return false;
        }
        
        // 检查是否为常见弱密码
        return !isCommonPassword(password);
    }
    
    /**
     * 检查是否为常见弱密码
     * 
     * @param password 密码
     * @return 如果是常见弱密码返回true
     */
    public static boolean isCommonPassword(String password) {
        if (password == null) {
            return false;
        }
        
        String lowerPassword = password.toLowerCase();
        String[] commonPasswords = {
            "password", "123456", "12345678", "qwerty", "abc123",
            "password123", "admin", "letmein", "welcome", "monkey",
            "1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm",
            "password1", "123456789", "welcome123", "admin123",
            "iloveyou", "princess", "dragon", "sunshine", "master",
            "123123", "football", "baseball", "superman", "trustno1"
        };
        
        for (String common : commonPasswords) {
            if (common.equals(lowerPassword)) {
                return true;
            }
        }
        
        // 检查简单的重复模式
        if (password.matches("(.)\\1{2,}")) { // 3个或更多相同字符
            return true;
        }
        
        // 检查简单的递增或递减数字
        if (password.matches("(012|123|234|345|456|567|678|789|890|987|876|765|654|543|432|321|210)+")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 计算密码强度分数（0-100）
     * 
     * @param password 密码
     * @return 密码强度分数
     */
    public static int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // 长度分数（最多30分）
        int length = password.length();
        if (length >= 8) score += 10;
        if (length >= 12) score += 10;
        if (length >= 16) score += 10;
        
        // 字符类型分数（最多40分）
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?".indexOf(c) >= 0);
        
        if (hasLower) score += 10;
        if (hasUpper) score += 10;
        if (hasDigit) score += 10;
        if (hasSpecial) score += 10;
        
        // 复杂度奖励分数（最多20分）
        int complexity = 0;
        if (hasLower) complexity++;
        if (hasUpper) complexity++;
        if (hasDigit) complexity++;
        if (hasSpecial) complexity++;
        
        if (complexity >= 3) score += 10;
        if (complexity == 4) score += 10;
        
        // 唯一字符分数（最多10分）
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars >= length * 0.7) score += 10;
        
        // 扣分项
        if (isCommonPassword(password)) score -= 30;
        if (password.matches("(.)\\1{2,}")) score -= 20; // 重复字符
        
        return Math.max(0, Math.min(100, score));
    }
    
    // ==================== 手机号验证 ====================
    
    /**
     * 验证中国大陆手机号格式
     * 
     * @param mobile 手机号
     * @return 如果格式正确返回true
     */
    public static boolean isValidChinaMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return false;
        }
        
        return CHINA_MOBILE_PATTERN.matcher(mobile.trim()).matches();
    }
    
    /**
     * 验证手机号格式（支持国际格式）
     * 
     * @param mobile 手机号
     * @return 如果格式正确返回true
     */
    public static boolean isValidMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return false;
        }
        
        String cleanMobile = mobile.trim().replaceAll("[\\s\\-\\(\\)]", "");
        
        // 支持+86开头的中国手机号
        if (cleanMobile.startsWith("+86")) {
            cleanMobile = cleanMobile.substring(3);
            return isValidChinaMobile(cleanMobile);
        }
        
        // 支持86开头的中国手机号
        if (cleanMobile.startsWith("86") && cleanMobile.length() == 13) {
            cleanMobile = cleanMobile.substring(2);
            return isValidChinaMobile(cleanMobile);
        }
        
        // 直接验证中国手机号
        return isValidChinaMobile(cleanMobile);
    }
    
    // ==================== URL验证 ====================
    
    /**
     * 验证URL格式是否正确
     * 
     * @param url URL地址
     * @return 如果格式正确返回true
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // 长度检查
        if (url.length() > 2048) {
            return false;
        }
        
        return URL_PATTERN.matcher(url.trim()).matches();
    }
    
    /**
     * 验证是否为HTTPS URL
     * 
     * @param url URL地址
     * @return 如果是HTTPS URL返回true
     */
    public static boolean isHttpsUrl(String url) {
        return isValidUrl(url) && url.trim().toLowerCase().startsWith("https://");
    }
    
    // ==================== IP地址验证 ====================
    
    /**
     * 验证IPv4地址格式
     * 
     * @param ip IP地址
     * @return 如果格式正确返回true
     */
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }
    
    /**
     * 验证IPv6地址格式
     * 
     * @param ip IP地址
     * @return 如果格式正确返回true
     */
    public static boolean isValidIPv6(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        return IPV6_PATTERN.matcher(ip.trim()).matches();
    }
    
    /**
     * 验证IP地址格式（IPv4或IPv6）
     * 
     * @param ip IP地址
     * @return 如果格式正确返回true
     */
    public static boolean isValidIP(String ip) {
        return isValidIPv4(ip) || isValidIPv6(ip);
    }
    
    // ==================== 身份证号验证 ====================
    
    /**
     * 验证中国大陆身份证号格式
     * 
     * @param idCard 身份证号
     * @return 如果格式正确返回true
     */
    public static boolean isValidChinaIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return false;
        }
        
        String cleanIdCard = idCard.trim().toUpperCase();
        
        // 基本格式检查
        if (!CHINA_ID_CARD_PATTERN.matcher(cleanIdCard).matches()) {
            return false;
        }
        
        // 校验码验证
        return validateIdCardChecksum(cleanIdCard);
    }
    
    /**
     * 验证身份证号校验码
     * 
     * @param idCard 身份证号
     * @return 如果校验码正确返回true
     */
    private static boolean validateIdCardChecksum(String idCard) {
        if (idCard.length() != 18) {
            return false;
        }
        
        // 权重因子
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        // 校验码对应表
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = idCard.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            sum += (c - '0') * weights[i];
        }
        
        int checkIndex = sum % 11;
        char expectedCheckCode = checkCodes[checkIndex];
        char actualCheckCode = idCard.charAt(17);
        
        return expectedCheckCode == actualCheckCode;
    }
    
    // ==================== 数字验证 ====================
    
    /**
     * 验证是否为正整数
     * 
     * @param str 字符串
     * @return 如果是正整数返回true
     */
    public static boolean isPositiveInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        return POSITIVE_INTEGER_PATTERN.matcher(str.trim()).matches();
    }
    
    /**
     * 验证是否为非负整数（包括0）
     * 
     * @param str 字符串
     * @return 如果是非负整数返回true
     */
    public static boolean isNonNegativeInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        return NON_NEGATIVE_INTEGER_PATTERN.matcher(str.trim()).matches();
    }
    
    /**
     * 验证是否为数字（包含小数）
     * 
     * @param str 字符串
     * @return 如果是数字返回true
     */
    public static boolean isNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        return NUMBER_PATTERN.matcher(str.trim()).matches();
    }
    
    /**
     * 验证数字是否在指定范围内
     * 
     * @param str 字符串
     * @param min 最小值
     * @param max 最大值
     * @return 如果在范围内返回true
     */
    public static boolean isNumberInRange(String str, double min, double max) {
        if (!isNumber(str)) {
            return false;
        }
        
        try {
            double value = Double.parseDouble(str.trim());
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // ==================== 长度验证 ====================
    
    /**
     * 验证字符串长度是否在指定范围内
     * 
     * @param str 字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 如果长度在范围内返回true
     */
    public static boolean isLengthInRange(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength <= 0;
        }
        
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * 验证字符串是否为空或仅包含空白字符
     * 
     * @param str 字符串
     * @return 如果为空或仅包含空白字符返回true
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 验证字符串是否不为空且不仅包含空白字符
     * 
     * @param str 字符串
     * @return 如果不为空且不仅包含空白字符返回true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}