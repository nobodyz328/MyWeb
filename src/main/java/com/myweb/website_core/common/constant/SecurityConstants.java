package com.myweb.website_core.common.constant;

/**
 * 安全相关常量
 * <p>
 * 定义系统安全功能中使用的各种常量，包括：
 * - JWT相关常量
 * - 会话管理常量
 * - 密码策略常量
 * - 认证相关常量
 * - 安全配置常量
 * <p>
 * 符合GB/T 22239-2019二级等保要求
 */
public class SecurityConstants {
    
    // ========== JWT相关常量 ==========
    
    /**
     * JWT密钥（生产环境应从配置文件读取）
     */
    public static final String JWT_SECRET = "MyWebBlogSecurityJwtSecretKey2024ForGB22239Compliance";
    
    /**
     * JWT Token前缀
     */
    public static final String JWT_TOKEN_PREFIX = "Bearer ";
    
    /**
     * JWT Token请求头名称
     */
    public static final String JWT_HEADER_NAME = "Authorization";
    
    /**
     * JWT Token过期时间（毫秒）- 24小时
     */
    public static final long JWT_EXPIRATION_TIME = 24 * 60 * 60 * 1000L;
    
    /**
     * JWT刷新Token过期时间（毫秒）- 7天
     */
    public static final long JWT_REFRESH_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L;
    
    /**
     * JWT签发者
     */
    public static final String JWT_ISSUER = "MyWeb-Blog-System";
    
    // ========== 会话管理常量 ==========
    
    /**
     * 会话超时时间（分钟）- 30分钟无操作自动退出
     */
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * 会话超时时间（毫秒）
     */
    public static final long SESSION_TIMEOUT_MILLIS = SESSION_TIMEOUT_MINUTES * 60 * 1000L;
    
    /**
     * 最大并发会话数 - 单用户单会话
     */
    public static final int MAX_CONCURRENT_SESSIONS = 1;
    
    /**
     * 会话清理间隔（分钟）
     */
    public static final int SESSION_CLEANUP_INTERVAL_MINUTES = 10;
    
    /**
     * 会话缓存前缀
     */
    public static final String SESSION_CACHE_PREFIX = "security:session:";
    
    // ========== 密码策略常量 ==========
    
    /**
     * 密码最小长度
     */
    public static final int PASSWORD_MIN_LENGTH = 8;
    
    /**
     * 密码最大长度
     */
    public static final int PASSWORD_MAX_LENGTH = 128;
    
    /**
     * BCrypt加密强度
     */
    public static final int BCRYPT_STRENGTH = 12;
    
    /**
     * 密码历史记录数量 - 防止重复使用最近的密码
     */
    public static final int PASSWORD_HISTORY_COUNT = 5;
    
    /**
     * 密码必须包含数字
     */
    public static final boolean PASSWORD_REQUIRE_DIGITS = true;
    
    /**
     * 密码必须包含小写字母
     */
    public static final boolean PASSWORD_REQUIRE_LOWERCASE = true;
    
    /**
     * 密码必须包含大写字母
     */
    public static final boolean PASSWORD_REQUIRE_UPPERCASE = true;
    
    /**
     * 密码必须包含特殊字符
     */
    public static final boolean PASSWORD_REQUIRE_SPECIAL_CHARS = true;
    
    /**
     * 密码特殊字符集合
     */
    public static final String PASSWORD_SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    
    // ========== 认证相关常量 ==========
    
    /**
     * 最大登录失败次数 - 超过后锁定账户
     */
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    
    /**
     * 需要验证码的登录失败次数阈值
     */
    public static final int CAPTCHA_REQUIRED_ATTEMPTS = 3;
    
    /**
     * 账户锁定时间（分钟）
     */
    public static final int ACCOUNT_LOCK_DURATION_MINUTES = 15;
    
    /**
     * 账户锁定时间（毫秒）
     */
    public static final long ACCOUNT_LOCK_DURATION_MILLIS = ACCOUNT_LOCK_DURATION_MINUTES * 60 * 1000L;
    
    /**
     * 邮箱验证码长度
     */
    public static final int EMAIL_VERIFICATION_CODE_LENGTH = 6;
    
    /**
     * 邮箱验证码过期时间（分钟）
     */
    public static final int EMAIL_VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    
    /**
     * TOTP密钥长度
     */
    public static final int TOTP_SECRET_LENGTH = 32;
    
    /**
     * TOTP时间窗口（秒）
     */
    public static final int TOTP_TIME_WINDOW_SECONDS = 30;
    
    /**
     * TOTP容错窗口数量 - 允许前后各1个时间窗口
     */
    public static final int TOTP_TOLERANCE_WINDOWS = 1;
    
    // ========== 访问控制常量 ==========
    
    /**
     * 默认用户角色
     */
    public static final String DEFAULT_USER_ROLE = "USER";
    
    /**
     * 管理员角色
     */
    public static final String ADMIN_ROLE = "ADMIN";
    
    /**
     * 版主角色
     */
    public static final String MODERATOR_ROLE = "MODERATOR";
    
    /**
     * 权限缓存前缀
     */
    public static final String PERMISSION_CACHE_PREFIX = "security:permission:";
    
    /**
     * 权限缓存过期时间（分钟）
     */
    public static final int PERMISSION_CACHE_EXPIRE_MINUTES = 30;
    
    // ========== 安全事件常量 ==========
    
    /**
     * 安全事件缓存前缀
     */
    public static final String SECURITY_EVENT_CACHE_PREFIX = "security:event:";
    
    /**
     * 安全事件告警阈值 - 单用户单小时内的可疑操作次数
     */
    public static final int SECURITY_EVENT_ALERT_THRESHOLD = 10;
    
    /**
     * 安全事件统计时间窗口（小时）
     */
    public static final int SECURITY_EVENT_WINDOW_HOURS = 1;
    
    // ========== 审计日志常量 ==========
    
    /**
     * 审计日志保留天数
     */
    public static final int AUDIT_LOG_RETENTION_DAYS = 90;
    
    /**
     * 审计日志批量处理大小
     */
    public static final int AUDIT_LOG_BATCH_SIZE = 100;
    
    /**
     * 审计日志异步队列大小
     */
    public static final int AUDIT_LOG_QUEUE_SIZE = 1000;
    
    // ========== 访问频率限制常量 ==========
    
    /**
     * 默认访问频率限制 - 每分钟请求数
     */
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 60;
    
    /**
     * 登录接口访问频率限制 - 每分钟请求数
     */
    public static final int LOGIN_RATE_LIMIT_PER_MINUTE = 5;
    
    /**
     * 注册接口访问频率限制 - 每分钟请求数
     */
    public static final int REGISTER_RATE_LIMIT_PER_MINUTE = 3;
    
    /**
     * 发帖接口访问频率限制 - 每分钟请求数
     */
    public static final int POST_CREATE_RATE_LIMIT_PER_MINUTE = 10;
    
    /**
     * 访问频率限制缓存前缀
     */
    public static final String RATE_LIMIT_CACHE_PREFIX = "security:rate_limit:";
    
    // ========== 文件上传安全常量 ==========
    
    /**
     * 允许的图片文件扩展名
     */
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};
    
    /**
     * 最大文件上传大小（字节）- 5MB
     */
    public static final long MAX_FILE_UPLOAD_SIZE = 5 * 1024 * 1024L;
    
    /**
     * 文件上传临时目录
     */
    public static final String FILE_UPLOAD_TEMP_DIR = "/tmp/uploads";
    
    // ========== 数据保护常量 ==========
    
    /**
     * 数据完整性哈希算法
     */
    public static final String DATA_INTEGRITY_HASH_ALGORITHM = "SHA-256";
    
    /**
     * 备份加密算法
     */
    public static final String BACKUP_ENCRYPTION_ALGORITHM = "AES";
    
    /**
     * 备份加密密钥长度
     */
    public static final int BACKUP_ENCRYPTION_KEY_LENGTH = 256;
    
    /**
     * 备份保留天数
     */
    public static final int BACKUP_RETENTION_DAYS = 30;
    
    // ========== 私有构造函数 ==========
    
    private SecurityConstants() {
        // 防止实例化
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 验证密码强度是否符合策略
     * 
     * @param password 密码
     * @return 是否符合策略
     */
    public static boolean isValidPasswordStrength(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            return false;
        }
        
        boolean hasDigit = false;
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (PASSWORD_SPECIAL_CHARS.indexOf(c) >= 0) {
                hasSpecial = true;
            }
        }
        
        return (!PASSWORD_REQUIRE_DIGITS || hasDigit) &&
               (!PASSWORD_REQUIRE_LOWERCASE || hasLower) &&
               (!PASSWORD_REQUIRE_UPPERCASE || hasUpper) &&
               (!PASSWORD_REQUIRE_SPECIAL_CHARS || hasSpecial);
    }
    
    /**
     * 验证用户角色是否有效
     * 
     * @param role 用户角色
     * @return 是否有效
     */
    public static boolean isValidUserRole(String role) {
        return DEFAULT_USER_ROLE.equals(role) || 
               ADMIN_ROLE.equals(role) || 
               MODERATOR_ROLE.equals(role);
    }
    
    /**
     * 验证文件扩展名是否允许
     * 
     * @param extension 文件扩展名
     * @return 是否允许
     */
    public static boolean isAllowedFileExtension(String extension) {
        if (extension == null) {
            return false;
        }
        
        String lowerExtension = extension.toLowerCase();
        for (String allowed : ALLOWED_IMAGE_EXTENSIONS) {
            if (allowed.equals(lowerExtension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取会话缓存键
     * 
     * @param sessionId 会话ID
     * @return 缓存键
     */
    public static String getSessionCacheKey(String sessionId) {
        return SESSION_CACHE_PREFIX + sessionId;
    }
    
    /**
     * 获取权限缓存键
     * 
     * @param username 用户名
     * @return 缓存键
     */
    public static String getPermissionCacheKey(String username) {
        return PERMISSION_CACHE_PREFIX + username;
    }
    
    /**
     * 获取访问频率限制缓存键
     * 
     * @param clientIp 客户端IP
     * @param endpoint 接口端点
     * @return 缓存键
     */
    public static String getRateLimitCacheKey(String clientIp, String endpoint) {
        return RATE_LIMIT_CACHE_PREFIX + clientIp + ":" + endpoint;
    }
}