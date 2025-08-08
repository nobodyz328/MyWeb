package com.myweb.website_core.common.constant;

/**
 * 系统常量统一管理类
 * 
 * 统一管理系统中的各种常量，避免硬编码
 * 包括：时间格式、文件路径、消息模板、业务规则等
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public class SystemConstants {
    
    // ========== 时间格式常量 ==========
    
    /**
     * 标准日期时间格式
     */
    public static final String STANDARD_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 标准日期格式
     */
    public static final String STANDARD_DATE_FORMAT = "yyyy-MM-dd";
    
    /**
     * 标准时间格式
     */
    public static final String STANDARD_TIME_FORMAT = "HH:mm:ss";
    
    /**
     * ISO日期时间格式
     */
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    /**
     * 文件名时间戳格式
     */
    public static final String FILENAME_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
    
    // ========== 文件路径常量 ==========
    
    /**
     * 上传文件根路径
     */
    public static final String UPLOAD_ROOT_PATH = "uploads/";
    
    /**
     * 头像上传路径
     */
    public static final String AVATAR_UPLOAD_PATH = UPLOAD_ROOT_PATH + "avatars/";
    
    /**
     * 帖子图片上传路径
     */
    public static final String POST_IMAGE_UPLOAD_PATH = UPLOAD_ROOT_PATH + "posts/";
    
    /**
     * 临时文件路径
     */
    public static final String TEMP_FILE_PATH = UPLOAD_ROOT_PATH + "temp/";
    
    /**
     * 备份文件路径
     */
    public static final String BACKUP_FILE_PATH = "backups/";
    
    /**
     * 日志文件路径
     */
    public static final String LOG_FILE_PATH = "logs/";
    
    // ========== 消息模板常量 ==========
    
    /**
     * 操作成功消息模板
     */
    public static final String SUCCESS_MESSAGE_TEMPLATE = "%s成功";
    
    /**
     * 操作失败消息模板
     */
    public static final String FAILURE_MESSAGE_TEMPLATE = "%s失败: %s";
    
    /**
     * 参数验证失败消息模板
     */
    public static final String VALIDATION_FAILURE_TEMPLATE = "参数验证失败: %s";
    
    /**
     * 权限不足消息模板
     */
    public static final String PERMISSION_DENIED_TEMPLATE = "权限不足，无法执行%s操作";
    
    /**
     * 资源不存在消息模板
     */
    public static final String RESOURCE_NOT_FOUND_TEMPLATE = "%s不存在";
    
    /**
     * 服务异常消息模板
     */
    public static final String SERVICE_ERROR_TEMPLATE = "%s异常";
    
    // ========== 业务规则常量 ==========
    
    /**
     * 默认页面大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * 最大页面大小
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * 默认搜索结果数量
     */
    public static final int DEFAULT_SEARCH_SIZE = 10;
    
    /**
     * 最大搜索结果数量
     */
    public static final int MAX_SEARCH_SIZE = 50;
    
    /**
     * 热门关键词数量
     */
    public static final int HOT_KEYWORDS_COUNT = 10;
    
    /**
     * 搜索历史保存数量
     */
    public static final int SEARCH_HISTORY_COUNT = 20;
    
    // ========== 缓存过期时间常量（秒） ==========
    
    /**
     * 短期缓存过期时间（5分钟）
     */
    public static final long SHORT_CACHE_EXPIRE = 300L;
    
    /**
     * 中期缓存过期时间（30分钟）
     */
    public static final long MEDIUM_CACHE_EXPIRE = 1800L;
    
    /**
     * 长期缓存过期时间（2小时）
     */
    public static final long LONG_CACHE_EXPIRE = 7200L;
    
    /**
     * 超长期缓存过期时间（24小时）
     */
    public static final long EXTRA_LONG_CACHE_EXPIRE = 86400L;
    
    // ========== 安全相关常量 ==========
    
    /**
     * 默认密码最小长度
     */
    public static final int DEFAULT_PASSWORD_MIN_LENGTH = 8;
    
    /**
     * 默认密码最大长度
     */
    public static final int DEFAULT_PASSWORD_MAX_LENGTH = 128;
    
    /**
     * 用户名最小长度
     */
    public static final int USERNAME_MIN_LENGTH = 3;
    
    /**
     * 用户名最大长度
     */
    public static final int USERNAME_MAX_LENGTH = 50;
    
    /**
     * 邮箱最大长度
     */
    public static final int EMAIL_MAX_LENGTH = 255;
    
    /**
     * 验证码长度
     */
    public static final int VERIFICATION_CODE_LENGTH = 6;
    
    /**
     * 验证码有效期（分钟）
     */
    public static final int VERIFICATION_CODE_EXPIRE_MINUTES = 10;
    
    /**
     * 登录失败最大尝试次数
     */
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    
    /**
     * 账户锁定时间（分钟）
     */
    public static final int ACCOUNT_LOCK_MINUTES = 30;
    
    // ========== JWT相关常量 ==========
    
    /**
     * JWT访问令牌默认过期时间（秒）- 1小时
     */
    public static final long JWT_ACCESS_TOKEN_EXPIRATION = 3600L;
    
    /**
     * JWT刷新令牌默认过期时间（秒）- 7天
     */
    public static final long JWT_REFRESH_TOKEN_EXPIRATION = 604800L;
    
    /**
     * JWT令牌类型
     */
    public static final String JWT_TOKEN_TYPE = "Bearer";
    
    /**
     * JWT访问令牌类型标识
     */
    public static final String JWT_ACCESS_TOKEN_TYPE = "access";
    
    /**
     * JWT刷新令牌类型标识
     */
    public static final String JWT_REFRESH_TOKEN_TYPE = "refresh";
    
    /**
     * Authorization头名称
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    
    /**
     * Bearer令牌前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";
    
    // ========== 文件上传限制常量 ==========
    
    /**
     * 头像文件最大大小（字节）- 2MB
     */
    public static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;
    
    /**
     * 帖子图片最大大小（字节）- 10MB
     */
    public static final long POST_IMAGE_MAX_SIZE = 10 * 1024 * 1024L;
    
    /**
     * 允许的图片文件扩展名
     */
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "webp", "bmp"
    };
    
    /**
     * 允许的头像文件MIME类型
     */
    public static final String[] ALLOWED_AVATAR_MIME_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp"
    };
    
    // ========== 审计日志常量 ==========
    
    /**
     * 审计日志最大参数长度
     */
    public static final int AUDIT_MAX_PARAM_LENGTH = 1000;
    
    /**
     * 审计日志最大响应长度
     */
    public static final int AUDIT_MAX_RESPONSE_LENGTH = 2000;
    
    /**
     * 审计日志最大错误信息长度
     */
    public static final int AUDIT_MAX_ERROR_LENGTH = 500;
    
    /**
     * 审计日志批量处理大小
     */
    public static final int AUDIT_BATCH_SIZE = 100;
    
    // ========== 系统配置常量 ==========
    
    /**
     * 系统名称
     */
    public static final String SYSTEM_NAME = "MyWeb";
    
    /**
     * 系统版本
     */
    public static final String SYSTEM_VERSION = "2.0.0";
    
    /**
     * 系统描述
     */
    public static final String SYSTEM_DESCRIPTION = "MyWeb社区系统";
    
    /**
     * 默认时区
     */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    
    /**
     * 默认语言
     */
    public static final String DEFAULT_LANGUAGE = "zh-CN";
    
    // ========== HTTP相关常量 ==========
    
    /**
     * 请求ID头名称
     */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    
    /**
     * 用户代理头名称
     */
    public static final String USER_AGENT_HEADER = "User-Agent";
    
    /**
     * 真实IP头名称列表
     */
    public static final String[] REAL_IP_HEADERS = {
        "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
        "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };
    
    // ========== 正则表达式常量 ==========
    
    /**
     * 邮箱格式正则表达式
     */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    
    /**
     * 用户名格式正则表达式（字母、数字、下划线）
     */
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,50}$";
    
    /**
     * 手机号格式正则表达式
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    
    /**
     * IP地址格式正则表达式
     */
    public static final String IP_REGEX = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
    
    // ========== 敏感信息关键字 ==========
    
    /**
     * 敏感参数关键字
     */
    public static final String[] SENSITIVE_PARAM_KEYWORDS = {
        "password", "pwd", "secret", "token", "key", "credential", 
        "auth", "authorization", "session", "cookie", "captcha",
        "totp", "otp", "verification", "code"
    };
    
    /**
     * 脱敏替换值
     */
    public static final String MASKED_VALUE = "*********";
    
    // ========== 错误代码常量 ==========
    
    /**
     * 成功代码
     */
    public static final String SUCCESS_CODE = "SUCCESS";
    
    /**
     * 失败代码
     */
    public static final String FAILURE_CODE = "FAILURE";
    
    /**
     * 参数错误代码
     */
    public static final String PARAM_ERROR_CODE = "PARAM_ERROR";
    
    /**
     * 权限错误代码
     */
    public static final String PERMISSION_ERROR_CODE = "PERMISSION_ERROR";
    
    /**
     * 系统错误代码
     */
    public static final String SYSTEM_ERROR_CODE = "SYSTEM_ERROR";
    
    // ========== 操作类型常量 ==========
    
    /**
     * 创建操作
     */
    public static final String OPERATION_CREATE = "CREATE";
    
    /**
     * 更新操作
     */
    public static final String OPERATION_UPDATE = "UPDATE";
    
    /**
     * 删除操作
     */
    public static final String OPERATION_DELETE = "DELETE";
    
    /**
     * 查询操作
     */
    public static final String OPERATION_QUERY = "QUERY";
    
    /**
     * 导出操作
     */
    public static final String OPERATION_EXPORT = "EXPORT";
    
    /**
     * 导入操作
     */
    public static final String OPERATION_IMPORT = "IMPORT";
    
    // ========== 工具方法 ==========
    
    /**
     * 检查字符串是否为敏感参数
     */
    public static boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        
        String lowerParamName = paramName.toLowerCase();
        for (String keyword : SENSITIVE_PARAM_KEYWORDS) {
            if (lowerParamName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 格式化成功消息
     */
    public static String formatSuccessMessage(String operation) {
        return String.format(SUCCESS_MESSAGE_TEMPLATE, operation);
    }
    
    /**
     * 格式化失败消息
     */
    public static String formatFailureMessage(String operation, String reason) {
        return String.format(FAILURE_MESSAGE_TEMPLATE, operation, reason);
    }
    
    /**
     * 格式化权限不足消息
     */
    public static String formatPermissionDeniedMessage(String operation) {
        return String.format(PERMISSION_DENIED_TEMPLATE, operation);
    }
    
    /**
     * 格式化资源不存在消息
     */
    public static String formatResourceNotFoundMessage(String resource) {
        return String.format(RESOURCE_NOT_FOUND_TEMPLATE, resource);
    }
    
    /**
     * 格式化服务异常消息
     */
    public static String formatServiceErrorMessage(String service) {
        return String.format(SERVICE_ERROR_TEMPLATE, service);
    }
    
    /**
     * 生成请求ID
     */
    public static String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    /**
     * 限制字符串长度
     */
    public static String limitStringLength(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...[TRUNCATED]";
    }
    
    /**
     * 脱敏处理
     */
    public static String maskSensitiveData(String data) {
        return data == null ? null : MASKED_VALUE;
    }
}