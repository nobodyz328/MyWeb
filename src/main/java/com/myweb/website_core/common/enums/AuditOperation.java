package com.myweb.website_core.common.enums;

import lombok.Getter;

/**
 * 审计操作类型枚举
 * 
 * 定义系统中需要记录审计日志的操作类型
 * 符合GB/T 22239-2019二级等保要求的安全审计机制
 */
@Getter
public enum AuditOperation {
    
    // ========== 用户认证相关操作 ==========
    
    /**
     * 用户注册
     */
    USER_REGISTER("USER_REGISTER", "用户注册", "用户创建新账户"),
    
    /**
     * 用户登录成功
     */
    USER_LOGIN_SUCCESS("USER_LOGIN_SUCCESS", "用户登录成功", "用户成功通过身份验证"),
    
    /**
     * 用户登录失败
     */
    USER_LOGIN_FAILURE("USER_LOGIN_FAILURE", "用户登录失败", "用户身份验证失败"),
    
    /**
     * 用户退出登录
     */
    USER_LOGOUT("USER_LOGOUT", "用户退出登录", "用户主动退出系统"),

    /**
     * 用户删除
     */
    USER_DELETE("USER_DELETE", "用户删除", "用户删除账户"),
    /**
     * 用户会话超时
     */
    USER_SESSION_TIMEOUT("USER_SESSION_TIMEOUT", "用户会话超时", "用户会话因超时被系统清理"),
    
    /**
     * 会话清理
     */
    SESSION_CLEANUP("SESSION_CLEANUP", "会话清理", "系统清理用户会话和相关数据"),
    
    /**
     * 账户被锁定
     */
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账户被锁定", "用户账户因多次登录失败被锁定"),
    
    /**
     * 账户解锁
     */
    ACCOUNT_UNLOCKED("ACCOUNT_UNLOCKED", "账户解锁", "被锁定的用户账户恢复正常"),
    
    /**
     * 密码修改
     */
    PASSWORD_CHANGED("PASSWORD_CHANGED", "密码修改", "用户修改登录密码"),
    
    /**
     * 邮箱验证
     */
    EMAIL_VERIFIED("EMAIL_VERIFIED", "邮箱验证", "用户完成邮箱验证"),
    
    /**
     * TOTP启用
     */
    TOTP_ENABLED("TOTP_ENABLED", "TOTP启用", "用户启用二次验证"),
    
    /**
     * TOTP禁用
     */
    TOTP_DISABLED("TOTP_DISABLED", "TOTP禁用", "用户禁用二次验证"),
    
    /**
     * TOTP设置
     */
    TOTP_SETUP("TOTP_SETUP", "TOTP设置", "用户设置TOTP二次验证"),
    
    /**
     * TOTP启用
     */
    TOTP_ENABLE("TOTP_ENABLE", "TOTP启用", "用户启用TOTP二次验证"),
    
    /**
     * TOTP禁用
     */
    TOTP_DISABLE("TOTP_DISABLE", "TOTP禁用", "用户禁用TOTP二次验证"),
    
    /**
     * TOTP验证
     */
    TOTP_VERIFY("TOTP_VERIFY", "TOTP验证", "用户验证TOTP代码"),
    
    /**
     * 密码修改
     */
    PASSWORD_CHANGE("PASSWORD_CHANGE", "密码修改", "用户修改登录密码"),
    
    /**
     * 管理员访问
     */
    ADMIN_ACCESS("ADMIN_ACCESS", "管理员访问", "管理员访问管理界面"),
    
    // ========== 内容管理相关操作 ==========
    
    /**
     * 创建帖子
     */
    POST_CREATE("POST_CREATE", "创建帖子", "用户发布新帖子"),
    
    /**
     * 编辑帖子
     */
    POST_UPDATE("POST_UPDATE", "编辑帖子", "用户修改帖子内容"),
    
    /**
     * 删除帖子
     */
    POST_DELETE("POST_DELETE", "删除帖子", "用户或管理员删除帖子"),
    
    /**
     * 查看帖子
     */
    POST_VIEW("POST_VIEW", "查看帖子", "用户查看帖子详情"),
    
    /**
     * 点赞帖子
     */
    POST_LIKE("POST_LIKE", "点赞帖子", "用户给帖子点赞"),
    
    /**
     * 取消点赞帖子
     */
    POST_UNLIKE("POST_UNLIKE", "取消点赞帖子", "用户取消对帖子的点赞"),
    
    /**
     * 收藏帖子
     */
    POST_COLLECT("POST_COLLECT", "收藏帖子", "用户收藏帖子"),
    
    /**
     * 取消收藏帖子
     */
    POST_UNCOLLECT("POST_UNCOLLECT", "取消收藏帖子", "用户取消对帖子的收藏"),
    
    /**
     * 帖子审核
     */
    POST_REVIEW("POST_REVIEW", "帖子审核", "管理员审核帖子内容"),
    
    /**
     * 帖子置顶
     */
    POST_PIN("POST_PIN", "帖子置顶", "管理员置顶帖子"),
    
    /**
     * 取消置顶
     */
    POST_UNPIN("POST_UNPIN", "取消置顶", "管理员取消帖子置顶"),
    
    /**
     * 帖子加精
     */
    POST_HIGHLIGHT("POST_HIGHLIGHT", "帖子加精", "管理员将帖子设为精华"),
    
    /**
     * 取消加精
     */
    POST_UNHIGHLIGHT("POST_UNHIGHLIGHT", "取消加精", "管理员取消帖子精华状态"),
    
    /**
     * 创建评论
     */
    COMMENT_CREATE("COMMENT_CREATE", "创建评论", "用户发表评论"),
    
    /**
     * 编辑评论
     */
    COMMENT_UPDATE("COMMENT_UPDATE", "编辑评论", "用户修改评论内容"),
    
    /**
     * 删除评论
     */
    COMMENT_DELETE("COMMENT_DELETE", "删除评论", "用户或管理员删除评论"),
    
    /**
     * 评论审核
     */
    COMMENT_REVIEW("COMMENT_REVIEW", "评论审核", "管理员审核评论内容"),
    
    /**
     * 评论点赞
     */
    COMMENT_LIKE("COMMENT_LIKE", "评论点赞", "用户给评论点赞"),
    
    /**
     * 取消评论点赞
     */
    COMMENT_UNLIKE("COMMENT_UNLIKE", "取消评论点赞", "用户取消对评论的点赞"),
    
    // ========== 用户管理相关操作 ==========
    
    /**
     * 更新个人资料
     */
    PROFILE_UPDATE("PROFILE_UPDATE", "更新个人资料", "用户修改个人资料信息"),
    
    /**
     * 上传头像
     */
    AVATAR_UPLOAD("AVATAR_UPLOAD", "上传头像", "用户上传或更换头像"),
    
    /**
     * 关注用户
     */
    USER_FOLLOW("USER_FOLLOW", "关注用户", "用户关注其他用户"),
    
    /**
     * 取消关注用户
     */
    USER_UNFOLLOW("USER_UNFOLLOW", "取消关注用户", "用户取消关注其他用户"),
    
    /**
     * 用户注销
     */
    USER_DEACTIVATE("USER_DEACTIVATE", "用户注销", "用户注销账户"),
    
    // ========== 管理员操作 ==========
    
    /**
     * 管理员登录
     */
    ADMIN_LOGIN("ADMIN_LOGIN", "管理员登录", "管理员登录系统"),
    
    /**
     * 用户管理
     */
    USER_MANAGEMENT("USER_MANAGEMENT", "用户管理", "管理员对用户进行管理操作"),
    
    /**
     * 角色分配
     */
    ROLE_ASSIGNMENT("ROLE_ASSIGNMENT", "角色分配", "管理员为用户分配角色"),
    
    /**
     * 权限管理
     */
    PERMISSION_MANAGEMENT("PERMISSION_MANAGEMENT", "权限管理", "管理员管理系统权限"),
    
    /**
     * 系统配置修改
     */
    SYSTEM_CONFIG_UPDATE("SYSTEM_CONFIG_UPDATE", "系统配置修改", "管理员修改系统配置"),
    
    /**
     * 配置变更
     */
    CONFIG_CHANGE("CONFIG_CHANGE", "配置变更", "系统配置发生变更"),
    
    /**
     * 审计日志查看
     */
    AUDIT_LOG_VIEW("AUDIT_LOG_VIEW", "审计日志查看", "管理员查看审计日志"),
    
    /**
     * 审计日志查询
     */
    AUDIT_LOG_QUERY("AUDIT_LOG_QUERY", "审计日志查询", "管理员查询审计日志"),
    
    /**
     * 审计日志导出
     */
    AUDIT_LOG_EXPORT("AUDIT_LOG_EXPORT", "审计日志导出", "管理员导出审计日志"),
    
    /**
     * 安全事件查询
     */
    SECURITY_EVENT_QUERY("SECURITY_EVENT_QUERY", "安全事件查询", "管理员查询安全事件"),
    
    /**
     * 失败日志查询
     */
    FAILURE_LOG_QUERY("FAILURE_LOG_QUERY", "失败日志查询", "管理员查询失败操作日志"),
    
    /**
     * 用户审计查询
     */
    USER_AUDIT_QUERY("USER_AUDIT_QUERY", "用户审计查询", "管理员查询用户操作历史"),
    
    /**
     * IP审计查询
     */
    IP_AUDIT_QUERY("IP_AUDIT_QUERY", "IP审计查询", "管理员查询IP操作记录"),
    
    /**
     * 审计统计查询
     */
    AUDIT_STATISTICS_QUERY("AUDIT_STATISTICS_QUERY", "审计统计查询", "管理员查询审计统计数据"),
    
    /**
     * 实时审计查询
     */
    REALTIME_AUDIT_QUERY("REALTIME_AUDIT_QUERY", "实时审计查询", "管理员查询实时审计数据"),
    
    /**
     * 数据备份
     */
    DATA_BACKUP("DATA_BACKUP", "数据备份", "系统执行数据备份操作"),
    
    /**
     * 数据恢复
     */
    DATA_RESTORE("DATA_RESTORE", "数据恢复", "管理员执行数据恢复操作"),
    
    /**
     * 数据导出
     */
    DATA_EXPORT("DATA_EXPORT", "数据导出", "用户或管理员导出个人数据"),
    
    // ========== 安全相关操作 ==========
    
    /**
     * 安全策略更新
     */
    SECURITY_POLICY_UPDATE("SECURITY_POLICY_UPDATE", "安全策略更新", "管理员更新安全策略配置"),
    
    /**
     * 访问被拒绝
     */
    ACCESS_DENIED("ACCESS_DENIED", "访问被拒绝", "用户访问被权限控制拒绝"),
    
    /**
     * 可疑活动检测
     */
    SUSPICIOUS_ACTIVITY("SUSPICIOUS_ACTIVITY", "可疑活动检测", "系统检测到可疑用户活动"),
    
    /**
     * 文件上传
     */
    FILE_UPLOAD("FILE_UPLOAD", "文件上传", "用户上传文件"),
    
    /**
     * 文件下载
     */
    FILE_DOWNLOAD("FILE_DOWNLOAD", "文件下载", "用户下载文件"),
    
    /**
     * 文件删除
     */
    FILE_DELETE("FILE_DELETE", "文件删除", "用户或管理员删除文件"),
    
    /**
     * 头像删除
     */
    AVATAR_DELETE("AVATAR_DELETE", "头像删除", "用户删除头像"),
    
    /**
     * 文件审核
     */
    FILE_REVIEW("FILE_REVIEW", "文件审核", "管理员审核上传文件"),
    
    /**
     * 恶意文件检测
     */
    MALICIOUS_FILE_DETECTED("MALICIOUS_FILE_DETECTED", "恶意文件检测", "系统检测到恶意文件上传"),
    
    /**
     * 搜索操作
     */
    SEARCH_OPERATION("SEARCH_OPERATION", "搜索操作", "用户执行搜索操作"),
    
    /**
     * 高级搜索
     */
    ADVANCED_SEARCH("ADVANCED_SEARCH", "高级搜索", "用户执行高级搜索操作"),
    
    /**
     * 搜索缓存清理
     */
    SEARCH_CACHE_CLEAR("SEARCH_CACHE_CLEAR", "搜索缓存清理", "管理员清理搜索缓存"),
    
    // ========== 系统操作 ==========
    
    /**
     * 系统启动
     */
    SYSTEM_START("SYSTEM_START", "系统启动", "系统服务启动"),
    
    /**
     * 系统关闭
     */
    SYSTEM_SHUTDOWN("SYSTEM_SHUTDOWN", "系统关闭", "系统服务关闭"),
    
    /**
     * 定时任务执行
     */
    SCHEDULED_TASK("SCHEDULED_TASK", "定时任务执行", "系统定时任务执行"),
    
    /**
     * 缓存清理
     */
    CACHE_CLEANUP("CACHE_CLEANUP", "缓存清理", "系统清理过期缓存数据"),
    
    /**
     * 系统监控
     */
    SYSTEM_MONITOR("SYSTEM_MONITOR", "系统监控", "系统执行监控检查操作"),
    
    /**
     * 备份操作
     */
    BACKUP_OPERATION("BACKUP_OPERATION", "备份操作", "系统执行备份相关操作"),
    
    /**
     * 系统维护
     */
    SYSTEM_MAINTENANCE("SYSTEM_MAINTENANCE", "系统维护", "系统执行维护相关操作"),
    
    /**
     * 完整性检查
     */
    INTEGRITY_CHECK("INTEGRITY_CHECK", "完整性检查", "系统执行文件完整性检查"),
    
    // ========== SQL安全相关操作 ==========
    
    /**
     * 数据库操作
     */
    DATABASE_OPERATION("DATABASE_OPERATION", "数据库操作", "系统执行数据库操作"),
    
    /**
     * SQL注入检查
     */
    SQL_INJECTION_CHECK("SQL_INJECTION_CHECK", "SQL注入检查", "系统执行SQL注入安全检查"),
    
    /**
     * 安全事件
     */
    SECURITY_EVENT("SECURITY_EVENT", "安全事件", "系统记录安全相关事件"),
    
    /**
     * 病毒扫描
     */
    VIRUS_SCAN("VIRUS_SCAN", "病毒扫描", "系统执行文件病毒扫描"),
    
    /**
     * 文件隔离
     */
    FILE_QUARANTINE("FILE_QUARANTINE", "文件隔离", "系统隔离可疑文件"),
    
    /**
     * 安全告警
     */
    SECURITY_ALERT("SECURITY_ALERT", "安全告警", "系统发送安全告警通知"),
    
    /**
     * 文件恢复
     */
    FILE_RECOVERY("FILE_RECOVERY", "文件恢复", "系统恢复被篡改的关键文件"),
    
    /**
     * 文件备份
     */
    FILE_BACKUP("FILE_BACKUP", "文件备份", "系统创建关键文件备份"),
    
    // ========== 安全监控面板相关操作 ==========
    
    /**
     * 安全概览查看
     */
    SECURITY_DASHBOARD_VIEW("SECURITY_DASHBOARD_VIEW", "安全概览查看", "管理员查看安全监控面板概览"),
    
    /**
     * 安全统计查看
     */
    SECURITY_STATISTICS_VIEW("SECURITY_STATISTICS_VIEW", "安全统计查看", "管理员查看安全事件统计"),
    
    /**
     * 用户行为分析
     */
    USER_BEHAVIOR_ANALYSIS("USER_BEHAVIOR_ANALYSIS", "用户行为分析", "管理员分析用户行为模式"),
    
    /**
     * 系统状态查看
     */
    SYSTEM_STATUS_VIEW("SYSTEM_STATUS_VIEW", "系统状态查看", "管理员查看系统安全状态"),
    
    /**
     * 威胁检测查看
     */
    THREAT_DETECTION_VIEW("THREAT_DETECTION_VIEW", "威胁检测查看", "管理员查看威胁检测结果"),
    
    /**
     * 审计统计查看
     */
    AUDIT_STATISTICS_VIEW("AUDIT_STATISTICS_VIEW", "审计统计查看", "管理员查看审计日志统计"),
    
    /**
     * 实时监控
     */
    REALTIME_MONITORING("REALTIME_MONITORING", "实时监控", "管理员查看实时监控数据"),
    
    /**
     * 安全报告导出
     */
    SECURITY_REPORT_EXPORT("SECURITY_REPORT_EXPORT", "安全报告导出", "管理员导出安全分析报告");
    
    /**
     * 操作代码
     * -- GETTER --
     *  获取操作代码
     *

     */
    private final String code;
    
    /**
     * 操作名称
     * -- GETTER --
     *  获取操作名称
     *

     */
    private final String name;
    
    /**
     * 操作描述
     * -- GETTER --
     *  获取操作描述
     *

     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param code 操作代码
     * @param name 操作名称
     * @param description 操作描述
     */
    AuditOperation(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据代码获取审计操作枚举
     * 
     * @param code 操作代码
     * @return 审计操作枚举，如果不存在则返回null
     */
    public static AuditOperation fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (AuditOperation operation : AuditOperation.values()) {
            if (operation.getCode().equals(code)) {
                return operation;
            }
        }
        return null;
    }
    
    /**
     * 检查是否为用户认证相关操作
     * 
     * @return 是否为用户认证相关操作
     */
    public boolean isAuthenticationOperation() {
        return this == USER_REGISTER || this == USER_LOGIN_SUCCESS || this == USER_LOGIN_FAILURE ||
               this == USER_LOGOUT || this == USER_SESSION_TIMEOUT || this == ACCOUNT_LOCKED ||
               this == ACCOUNT_UNLOCKED || this == PASSWORD_CHANGED || this == EMAIL_VERIFIED ||
               this == TOTP_ENABLED || this == TOTP_DISABLED;
    }
    
    /**
     * 检查是否为内容管理相关操作
     * 
     * @return 是否为内容管理相关操作
     */
    public boolean isContentOperation() {
        return this == POST_CREATE || this == POST_UPDATE || this == POST_DELETE ||
               this == POST_VIEW || this == POST_LIKE || this == POST_UNLIKE ||
               this == POST_COLLECT || this == POST_UNCOLLECT || this == COMMENT_CREATE ||
               this == COMMENT_UPDATE || this == COMMENT_DELETE;
    }
    
    /**
     * 检查是否为管理员操作
     * 
     * @return 是否为管理员操作
     */
    public boolean isAdminOperation() {
        return this == ADMIN_LOGIN || this == USER_MANAGEMENT || this == ROLE_ASSIGNMENT ||
               this == PERMISSION_MANAGEMENT || this == SYSTEM_CONFIG_UPDATE ||
               this == AUDIT_LOG_VIEW || this == AUDIT_LOG_EXPORT || this == DATA_BACKUP ||
               this == DATA_RESTORE || this == SECURITY_POLICY_UPDATE ||
               this == SECURITY_DASHBOARD_VIEW || this == SECURITY_STATISTICS_VIEW ||
               this == USER_BEHAVIOR_ANALYSIS || this == SYSTEM_STATUS_VIEW ||
               this == THREAT_DETECTION_VIEW || this == AUDIT_STATISTICS_VIEW ||
               this == REALTIME_MONITORING || this == SECURITY_REPORT_EXPORT;
    }
    
    /**
     * 检查是否为安全相关操作
     * 
     * @return 是否为安全相关操作
     */
    public boolean isSecurityOperation() {
        return this == ACCESS_DENIED || this == SUSPICIOUS_ACTIVITY ||
               this == SECURITY_POLICY_UPDATE || isAuthenticationOperation();
    }
    
    /**
     * 检查是否为系统操作
     * 
     * @return 是否为系统操作
     */
    public boolean isSystemOperation() {
        return this == SYSTEM_START || this == SYSTEM_SHUTDOWN ||
               this == SCHEDULED_TASK || this == CACHE_CLEANUP;
    }
    
    /**
     * 获取操作的风险级别（1-5，数值越大风险越高）
     * 
     * @return 风险级别
     */
    public int getRiskLevel() {
        if (isAdminOperation() || this == DATA_RESTORE || this == SECURITY_POLICY_UPDATE) {
            return 5; // 高风险
        } else if (this == USER_REGISTER || this == PASSWORD_CHANGED || this == ACCOUNT_LOCKED ||
                   this == POST_DELETE || this == COMMENT_DELETE || this == USER_DEACTIVATE) {
            return 4; // 中高风险
        } else if (this == USER_LOGIN_FAILURE || this == ACCESS_DENIED || this == SUSPICIOUS_ACTIVITY ||
                   this == FILE_UPLOAD) {
            return 3; // 中等风险
        } else if (isContentOperation() || this == PROFILE_UPDATE || this == USER_FOLLOW) {
            return 2; // 低风险
        } else {
            return 1; // 最低风险
        }
    }
    
    @Override
    public String toString() {
        return String.format("AuditOperation{code='%s', name='%s', description='%s'}", 
                           code, name, description);
    }
}