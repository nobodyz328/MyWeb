package com.myweb.website_core.application.service.security.IPS.virusprotect;

import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.infrastructure.security.XssPatternLibrary;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入验证服务
 * <p>
 * 提供统一的输入验证功能，包括长度检查、字符集验证、XSS检测、SQL注入检测等。
 * <p>
 * 主要功能：
 * 1. 字符串长度和字符集验证
 * 2. XSS攻击模式检测
 * 3. SQL注入模式检测
 * 4. 邮箱格式验证
 * 5. 用户名格式验证
 * 6. 密码强度验证
 * 7. 文件名安全验证
 * 8. URL格式验证
 * <p>
 * 符合需求：4.4, 4.5 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Service
public class InputValidationService {
    
    // 安全字符集定义
    private static final int MAX_STRING_LENGTH = 10000;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 50000;
    private static final int MAX_COMMENT_LENGTH = 2000;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_URL_LENGTH = 2048;
    
    // 安全字符集模式 - 允许更多常用字符包括换行符
    private static final Pattern SAFE_STRING_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9\\u4e00-\\u9fa5\\s.,!?;:()\\-_@#$%^&*+={}\\[\\]|\\\\\"'<>/~`\\r\\n\\t]*$", Pattern.DOTALL);
    
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_\\-]{3,50}$");
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._+%-]*[a-zA-Z0-9]@[a-zA-Z0-9][a-zA-Z0-9.-]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}$|^[a-zA-Z0-9]@[a-zA-Z0-9][a-zA-Z0-9.-]*[a-zA-Z0-9]\\.[a-zA-Z]{2,}$");
    
    private static final Pattern FILENAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9._\\-\\u4e00-\\u9fa5]+$");
    
    private static final Pattern URL_PATTERN = 
        Pattern.compile("^https?://[a-zA-Z0-9.-]+(:[0-9]+)?(?:/[^\\s]*)?$");
    
    // SQL注入攻击模式 - 增强版本
    private static final List<String> SQL_INJECTION_PATTERNS = Arrays.asList(
        // 基本SQL命令
        "union", "select", "insert", "update", "delete", "drop", "create",
        "alter", "truncate", "exec", "execute", "sp_", "xp_", "fn_",
        
        // 注释符号
        "--", "/*", "*/", "#",
        
        // 系统函数和变量
        "@@", "@@version", "@@user", "@@database", "user(", "version(",
        "database(", "schema(", "current_user", "session_user", "system_user",
        
        // 字符串函数
        "char(", "nchar(", "varchar(", "nvarchar(", "cast(", "convert(",
        "ascii(", "substring(", "len(", "length(", "concat(", "concat_ws(",
        
        // 时间延迟函数
        "sleep(", "benchmark(", "waitfor", "delay", "pg_sleep(",
        
        // 系统表和视图
        "sysobjects", "syscolumns", "sysusers", "systables", "pg_tables",
        "pg_user", "pg_database", "mysql.user", "information_schema",
        "master", "msdb", "tempdb", "model", "northwind", "pubs",
        
        // 文件操作
        "load_file(", "into outfile", "into dumpfile", "load data infile",
        "bulk insert", "openrowset", "opendatasource",
        
        // 联合查询和排序
        "union all select", "union select", "order by", "group by", "having",
        "limit", "offset", "waitfor delay",
        
        // 堆叠查询
        ";select", ";insert", ";update", ";delete", ";drop", ";create",
        
        // 盲注相关
        "and 1=1", "and 1=2", "or 1=1", "or 1=2", "and '1'='1", "and '1'='2",
        "or '1'='1", "or '1'='2"
    );
    
    // 危险文件扩展名
    private static final List<String> DANGEROUS_FILE_EXTENSIONS = Arrays.asList(
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar",
        "php", "asp", "aspx", "jsp", "py", "rb", "pl", "sh", "ps1"
    );
    
    /**
     * 验证通用字符串输入
     * 
     * @param input     输入字符串
     * @param fieldName 字段名称
     * @throws ValidationException 验证失败时抛出
     */
    public void validateStringInput(String input, String fieldName) {
        validateStringInput(input, fieldName, MAX_STRING_LENGTH);
    }
    
    /**
     * 验证字符串输入（指定最大长度）
     * 
     * @param input     输入字符串
     * @param fieldName 字段名称
     * @param maxLength 最大长度
     * @throws ValidationException 验证失败时抛出
     */
    public void validateStringInput(String input, String fieldName, int maxLength) {
        if (input == null) {
            return; // null值由@NotNull等注解处理
        }
        
        // 长度检查
        if (input.length() > maxLength) {
            throw new ValidationException(
                String.format("%s长度超过限制（最大%d字符）", fieldName, maxLength), 
                fieldName, 
                "LENGTH_EXCEEDED"
            );
        }
        
        // 字符集检查 - 检查是否包含控制字符（除了常见的换行符、制表符等）
        if (containsIllegalCharacters(input)) {
            throw new ValidationException(
                String.format("%s包含非法字符", fieldName), 
                fieldName, 
                "ILLEGAL_CHARACTERS"
            );
        }
        
        // XSS检查
        if (containsXSS(input)) {
            throw new ValidationException(
                String.format("%s包含潜在的XSS攻击代码", fieldName), 
                fieldName, 
                "XSS_DETECTED"
            );
        }
        
        // SQL注入检查
        if (containsSQLInjection(input)) {
            throw new ValidationException(
                String.format("%s包含潜在的SQL注入代码", fieldName), 
                fieldName, 
                "SQL_INJECTION_DETECTED"
            );
        }
    }
    
    /**
     * 验证用户名
     * 
     * @param username 用户名
     * @throws ValidationException 验证失败时抛出
     */
    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("用户名不能为空", "username", "REQUIRED");
        }
        
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new ValidationException(
                String.format("用户名长度超过限制（最大%d字符）", MAX_USERNAME_LENGTH), 
                "username", 
                "LENGTH_EXCEEDED"
            );
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException(
                "用户名只能包含字母、数字、下划线和连字符，长度3-50字符", 
                "username", 
                "INVALID_FORMAT"
            );
        }
        
        // 检查保留用户名
        if (isReservedUsername(username)) {
            throw new ValidationException(
                "该用户名为系统保留，请选择其他用户名", 
                "username", 
                "RESERVED_NAME"
            );
        }
    }
    
    /**
     * 验证邮箱地址
     * 
     * @param email 邮箱地址
     * @throws ValidationException 验证失败时抛出
     */
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("邮箱地址不能为空", "email", "REQUIRED");
        }
        
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException(
                String.format("邮箱地址长度超过限制（最大%d字符）", MAX_EMAIL_LENGTH), 
                "email", 
                "LENGTH_EXCEEDED"
            );
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches() || email.contains("..")) {
            throw new ValidationException(
                "邮箱地址格式不正确", 
                "email", 
                "INVALID_FORMAT"
            );
        }
    }
    
    /**
     * 验证密码强度
     * 
     * @param password 密码
     * @throws ValidationException 验证失败时抛出
     */
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("密码不能为空", "password", "REQUIRED");
        }
        
        if (password.length() < 8) {
            throw new ValidationException(
                "密码长度至少8个字符", 
                "password", 
                "TOO_SHORT"
            );
        }
        
        if (password.length() > 128) {
            throw new ValidationException(
                "密码长度不能超过128个字符", 
                "password", 
                "TOO_LONG"
            );
        }
        
        // 检查密码复杂度
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) >= 0);
        
        int complexity = 0;
        if (hasLower) complexity++;
        if (hasUpper) complexity++;
        if (hasDigit) complexity++;
        if (hasSpecial) complexity++;
        
        if (complexity < 3) {
            throw new ValidationException(
                "密码必须包含以下至少3种字符类型：小写字母、大写字母、数字、特殊字符", 
                "password", 
                "WEAK_PASSWORD"
            );
        }
        
        // 检查常见弱密码
        if (isCommonPassword(password)) {
            throw new ValidationException(
                "密码过于简单，请使用更复杂的密码", 
                "password", 
                "COMMON_PASSWORD"
            );
        }
    }
    
    /**
     * 验证帖子标题
     * 
     * @param title 标题
     * @throws ValidationException 验证失败时抛出
     */
    public void validatePostTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("标题不能为空", "title", "REQUIRED");
        }
        
        validateStringInput(title.trim(), "标题", MAX_TITLE_LENGTH);
    }
    
    /**
     * 验证帖子内容
     * 
     * @param content 内容
     * @throws ValidationException 验证失败时抛出
     */
    public void validatePostContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("内容不能为空", "content", "REQUIRED");
        }
        
        validateStringInput(content.trim(), "内容", MAX_CONTENT_LENGTH);
    }
    
    /**
     * 验证评论内容
     * 
     * @param content 评论内容
     * @throws ValidationException 验证失败时抛出
     */
    public void validateCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("评论内容不能为空", "content", "REQUIRED");
        }
        
        validateStringInput(content.trim(), "评论内容", MAX_COMMENT_LENGTH);
    }
    
    /**
     * 验证文件名
     * 
     * @param filename 文件名
     * @throws ValidationException 验证失败时抛出
     */
    public void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new ValidationException("文件名不能为空", "filename", "REQUIRED");
        }
        
        if (filename.length() > MAX_FILENAME_LENGTH) {
            throw new ValidationException(
                String.format("文件名长度超过限制（最大%d字符）", MAX_FILENAME_LENGTH), 
                "filename", 
                "LENGTH_EXCEEDED"
            );
        }
        
        if (!FILENAME_PATTERN.matcher(filename).matches()) {
            throw new ValidationException(
                "文件名包含非法字符", 
                "filename", 
                "ILLEGAL_CHARACTERS"
            );
        }
        
        // 检查危险文件扩展名
        String extension = getFileExtension(filename).toLowerCase();
        if (DANGEROUS_FILE_EXTENSIONS.contains(extension)) {
            throw new ValidationException(
                "不允许上传该类型的文件", 
                "filename", 
                "DANGEROUS_EXTENSION"
            );
        }
    }
    
    /**
     * 验证URL
     * 
     * @param url URL地址
     * @throws ValidationException 验证失败时抛出
     */
    public void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return; // URL可以为空
        }
        
        if (url.length() > MAX_URL_LENGTH) {
            throw new ValidationException(
                String.format("URL长度超过限制（最大%d字符）", MAX_URL_LENGTH), 
                "url", 
                "LENGTH_EXCEEDED"
            );
        }
        
        if (!URL_PATTERN.matcher(url).matches()) {
            throw new ValidationException(
                "URL格式不正确", 
                "url", 
                "INVALID_FORMAT"
            );
        }
    }
    
    /**
     * 检查是否包含XSS攻击代码
     * 
     * @param input 输入字符串
     * @return 如果包含XSS攻击代码返回true
     */
    private boolean containsXSS(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // 使用现有的XSS模式库进行检测
        return XssPatternLibrary.containsXssPattern(input);
    }
    
    /**
     * 检查是否包含SQL注入代码 - 增强版本
     * 
     * @param input 输入字符串
     * @return 如果包含SQL注入代码返回true
     */
    private boolean containsSQLInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase().trim();
        
        // 1. 检查SQL注入关键词
        for (String pattern : SQL_INJECTION_PATTERNS) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        // 2. 检查SQL注入特征模式
        // Union注入
        if (lowerInput.matches(".*\\bunion\\s+(all\\s+)?select\\b.*")) {
            return true;
        }
        
        // 条件注入
        if (lowerInput.matches(".*\\s+(or|and)\\s+\\d+\\s*=\\s*\\d+.*") ||
            lowerInput.matches(".*\\s+(or|and)\\s+'[^']*'\\s*=\\s*'[^']*'.*") ||
            lowerInput.matches(".*\\s+(or|and)\\s+\\w+\\s*=\\s*\\w+.*")) {
            return true;
        }
        
        // 注释注入
        if (lowerInput.matches(".*'\\s*(--|/\\*|#).*")) {
            return true;
        }
        
        // 堆叠查询
        if (lowerInput.matches(".*;\\s*(select|insert|update|delete|drop|create|alter)\\b.*")) {
            return true;
        }
        
        // 函数注入
        if (lowerInput.matches(".*\\b(sleep|benchmark|waitfor|pg_sleep)\\s*\\(.*") ||
            lowerInput.matches(".*\\b(load_file|into\\s+outfile|into\\s+dumpfile)\\b.*")) {
            return true;
        }
        
        // 系统信息获取
        if (lowerInput.matches(".*\\b(@@version|@@user|@@database|user\\(\\)|version\\(\\)|database\\(\\)).*")) {
            return true;
        }
        
        // 盲注特征
        if (lowerInput.matches(".*\\b(ascii|substring|length|len)\\s*\\(.*\\bfrom\\b.*") ||
            lowerInput.matches(".*\\bcast\\s*\\(.*\\bas\\s+.*") ||
            lowerInput.matches(".*\\bconvert\\s*\\(.*")) {
            return true;
        }
        
        // 时间盲注
        if (lowerInput.matches(".*\\bif\\s*\\(.*,\\s*(sleep|benchmark|waitfor)\\s*\\(.*")) {
            return true;
        }
        
        // 错误注入
        if (lowerInput.matches(".*\\b(extractvalue|updatexml|exp)\\s*\\(.*")) {
            return true;
        }
        
        // 检查多个连续的特殊字符组合
        if (lowerInput.matches(".*[';\"\\-]{2,}.*") ||
            lowerInput.matches(".*\\*{2,}.*") ||
            lowerInput.matches(".*/{2,}.*")) {
            return true;
        }
        
        // 检查十六进制编码
        if (lowerInput.matches(".*0x[0-9a-f]+.*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否为保留用户名
     * 
     * @param username 用户名
     * @return 如果是保留用户名返回true
     */
    private boolean isReservedUsername(String username) {
        List<String> reservedNames = Arrays.asList(
            "admin", "administrator", "root", "system", "user", "guest",
            "test", "demo", "api", "www", "mail", "ftp", "blog", "forum",
            "support", "help", "info", "contact", "about", "news", "service",
            "null", "undefined", "anonymous", "public", "private", "default"
        );
        
        return reservedNames.contains(username.toLowerCase());
    }
    
    /**
     * 检查是否为常见弱密码
     * 
     * @param password 密码
     * @return 如果是常见弱密码返回true
     */
    private boolean isCommonPassword(String password) {
        List<String> commonPasswords = Arrays.asList(
            "password", "123456", "12345678", "qwerty", "abc123",
            "password123", "admin", "letmein", "welcome", "monkey",
            "1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm",
            "password1", "123456789", "welcome123", "admin123"
        );
        
        return commonPasswords.contains(password.toLowerCase());
    }
    
    /**
     * 检查是否包含非法字符
     * 
     * @param input 输入字符串
     * @return 如果包含非法字符返回true
     */
    private boolean containsIllegalCharacters(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // 检查是否包含危险的控制字符（保留常见的空白字符）
        for (char c : input.toCharArray()) {
            // 允许常见的空白字符
            if (c == '\n' || c == '\r' || c == '\t' || c == ' ') {
                continue;
            }
            
            // 检查其他控制字符
            if (Character.isISOControl(c)) {
                return true;
            }
            
            // 检查一些特殊的危险字符
            if (c == '\0' || c == '\u0001' || c == '\u0002' || c == '\u0003' || 
                c == '\u0004' || c == '\u0005' || c == '\u0006' || c == '\u0007' ||
                c == '\u0008' || c == '\u000B' || c == '\u000C' || c == '\u000E' ||
                c == '\u000F' || c == '\u0010' || c == '\u0011' || c == '\u0012' ||
                c == '\u0013' || c == '\u0014' || c == '\u0015' || c == '\u0016' ||
                c == '\u0017' || c == '\u0018' || c == '\u0019' || c == '\u001A' ||
                c == '\u001B' || c == '\u001C' || c == '\u001D' || c == '\u001E' ||
                c == '\u001F' || c == '\u007F') {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1);
    }
}