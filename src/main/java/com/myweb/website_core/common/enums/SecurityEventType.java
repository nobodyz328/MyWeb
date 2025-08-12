package com.myweb.website_core.common.enums;

import lombok.Getter;

/**
 * 安全事件类型枚举
 * <p>
 * 定义系统中各种安全事件的类型和严重级别
 * 符合GB/T 22239-2019二级等保要求的安全事件分类
 * 
 * @author MyWeb
 * @version 1.0
 */
@Getter
public enum SecurityEventType {
    
    // ========== 认证相关安全事件 ==========
    
    /**
     * 连续登录失败
     */
    CONTINUOUS_LOGIN_FAILURE("CONTINUOUS_LOGIN_FAILURE", "连续登录失败", "检测到连续多次登录失败", 4),
    
    /**
     * 账户被锁定
     */
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账户锁定", "用户账户因多次失败尝试被锁定", 3),
    
    /**
     * 异常登录地点
     */
    UNUSUAL_LOGIN_LOCATION("UNUSUAL_LOGIN_LOCATION", "异常登录地点", "检测到来自异常地理位置的登录", 3),
    
    /**
     * 异常登录时间
     */
    UNUSUAL_LOGIN_TIME("UNUSUAL_LOGIN_TIME", "异常登录时间", "检测到异常时间段的登录行为", 2),
    
    /**
     * 暴力破解攻击
     */
    BRUTE_FORCE_ATTACK("BRUTE_FORCE_ATTACK", "暴力破解攻击", "检测到暴力破解登录攻击", 5),
    
    // ========== 访问控制相关安全事件 ==========
    
    /**
     * 权限提升尝试
     */
    PRIVILEGE_ESCALATION("PRIVILEGE_ESCALATION", "权限提升尝试", "检测到尝试提升权限的行为", 4),
    
    /**
     * 未授权访问尝试
     */
    ACCESS_DENIED("UNAUTHORIZED_ACCESS", "未授权访问", "检测到未授权的资源访问尝试", 4),
    
    /**
     * 敏感操作异常
     */
    SENSITIVE_OPERATION_ANOMALY("SENSITIVE_OPERATION_ANOMALY", "敏感操作异常", "检测到异常的敏感操作行为", 4),

    SENSITIVE_DATA_ACCESS("SENSITIVE_DATA_ACCESS", "敏感数据访问", "用户访问敏感数据", 3),
    
    /**
     * 批量数据访问
     */
    BULK_DATA_ACCESS("BULK_DATA_ACCESS", "批量数据访问", "用户在短时间内访问大量数据", 3),
    
    /**
     * 跨域访问异常
     */
    CROSS_DOMAIN_ANOMALY("CROSS_DOMAIN_ANOMALY", "跨域访问异常", "检测到异常的跨域访问行为", 3),
    
    // ========== 输入安全事件 ==========
    
    /**
     * SQL注入尝试
     */
    SQL_INJECTION_ATTEMPT("SQL_INJECTION_ATTEMPT", "SQL注入尝试", "检测到SQL注入攻击尝试", 5),
    
    /**
     * XSS攻击尝试
     */
    XSS_ATTACK_ATTEMPT("XSS_ATTACK_ATTEMPT", "XSS攻击尝试", "检测到跨站脚本攻击尝试", 4),
    
    /**
     * CSRF攻击尝试
     */
    CSRF_ATTACK_ATTEMPT("CSRF_ATTACK_ATTEMPT", "CSRF攻击尝试", "检测到跨站请求伪造攻击", 4),
    
    /**
     * 恶意文件上传
     */
    MALICIOUS_FILE_UPLOAD("MALICIOUS_FILE_UPLOAD", "恶意文件上传", "检测到恶意文件上传尝试", 5),
    
    /**
     * 危险输入内容
     */
    DANGEROUS_INPUT_CONTENT("DANGEROUS_INPUT_CONTENT", "危险输入内容", "用户输入包含危险内容", 3),
    
    /**
     * 输入验证失败
     */
    INPUT_VALIDATION_FAILURE("INPUT_VALIDATION_FAILURE", "输入验证失败", "用户输入未通过安全验证", 2),
    
    // ========== 网络安全事件 ==========
    
    /**
     * DDoS攻击
     */
    DDOS_ATTACK("DDOS_ATTACK", "DDoS攻击", "检测到分布式拒绝服务攻击", 5),
    
    /**
     * 访问频率异常
     */
    ABNORMAL_ACCESS_FREQUENCY("ABNORMAL_ACCESS_FREQUENCY", "访问频率异常", "用户访问频率超过正常阈值", 3),
    
    /**
     * 可疑IP访问
     */
    SUSPICIOUS_IP_ACCESS("SUSPICIOUS_IP_ACCESS", "可疑IP访问", "来自可疑IP地址的访问", 4),
    
    /**
     * 端口扫描
     */
    PORT_SCANNING("PORT_SCANNING", "端口扫描", "检测到针对系统的端口扫描", 4),
    
    /**
     * 网络探测
     */
    NETWORK_PROBING("NETWORK_PROBING", "网络探测", "检测到网络探测行为", 3),
    
    // ========== 系统安全事件 ==========
    
    /**
     * 系统配置被篡改
     */
    SYSTEM_CONFIG_TAMPERED("SYSTEM_CONFIG_TAMPERED", "系统配置被篡改", "检测到系统配置文件被非法修改", 5),
    
    /**
     * 关键文件被修改
     */
    CRITICAL_FILE_MODIFIED("CRITICAL_FILE_MODIFIED", "关键文件被修改", "系统关键文件被修改", 5),
    
    /**
     * 异常进程活动
     */
    ABNORMAL_PROCESS_ACTIVITY("ABNORMAL_PROCESS_ACTIVITY", "异常进程活动", "检测到异常的进程活动", 4),
    
    /**
     * 资源使用异常
     */
    RESOURCE_USAGE_ANOMALY("RESOURCE_USAGE_ANOMALY", "资源使用异常", "系统资源使用出现异常", 3),
    
    /**
     * 日志被篡改
     */
    LOG_TAMPERING("LOG_TAMPERING", "日志被篡改", "检测到审计日志被篡改", 5),
    
    /**
     * 备份失败
     */
    BACKUP_FAILURE("BACKUP_FAILURE", "备份失败", "数据备份操作失败", 4),
    
    // ========== 数据安全事件 ==========
    
    /**
     * 数据完整性异常
     */
    DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION", "数据完整性异常", "检测到数据完整性被破坏", 5),
    
    /**
     * 敏感数据泄露
     */
    SENSITIVE_DATA_LEAK("SENSITIVE_DATA_LEAK", "敏感数据泄露", "检测到敏感数据可能泄露", 5),
    
    /**
     * 数据异常删除
     */
    ABNORMAL_DATA_DELETION("ABNORMAL_DATA_DELETION", "数据异常删除", "检测到异常的数据删除操作", 4),
    
    /**
     * 数据导出异常
     */
    ABNORMAL_DATA_EXPORT("ABNORMAL_DATA_EXPORT", "数据导出异常", "检测到异常的数据导出行为", 4),
    
    // ========== 恶意代码事件 ==========
    
    /**
     * 病毒检测
     */
    VIRUS_DETECTED("VIRUS_DETECTED", "病毒检测", "在上传文件中检测到病毒", 5),
    
    /**
     * 恶意脚本检测
     */
    MALICIOUS_SCRIPT_DETECTED("MALICIOUS_SCRIPT_DETECTED", "恶意脚本检测", "检测到恶意脚本代码", 5),
    
    /**
     * 依赖包漏洞
     */
    DEPENDENCY_VULNERABILITY("DEPENDENCY_VULNERABILITY", "依赖包漏洞", "检测到依赖包存在安全漏洞", 4),
    
    /**
     * 代码注入尝试
     */
    CODE_INJECTION_ATTEMPT("CODE_INJECTION_ATTEMPT", "代码注入尝试", "检测到代码注入攻击尝试", 5),
    
    // ========== 业务安全事件 ==========
    
    /**
     * 异常业务操作
     */
    ABNORMAL_BUSINESS_OPERATION("ABNORMAL_BUSINESS_OPERATION", "异常业务操作", "检测到异常的业务操作模式", 3),
    
    /**
     * 垃圾内容发布
     */
    SPAM_CONTENT_POSTED("SPAM_CONTENT_POSTED", "垃圾内容发布", "用户发布垃圾或恶意内容", 2),
    
    /**
     * 批量操作异常
     */
    BULK_OPERATION_ANOMALY("BULK_OPERATION_ANOMALY", "批量操作异常", "检测到异常的批量操作行为", 3),
    
    /**
     * 账户异常活动
     */
    ACCOUNT_ABNORMAL_ACTIVITY("ACCOUNT_ABNORMAL_ACTIVITY", "账户异常活动", "用户账户出现异常活动模式", 3),
    
    // ========== XSS监控相关安全事件 ==========
    
    /**
     * XSS攻击阈值超限
     */
    XSS_ATTACK_THRESHOLD_EXCEEDED("XSS_ATTACK_THRESHOLD_EXCEEDED", "XSS攻击阈值超限", "XSS攻击次数超过设定阈值", 4),
    
    /**
     * 可疑IP活动
     */
    SUSPICIOUS_IP_ACTIVITY("SUSPICIOUS_IP_ACTIVITY", "可疑IP活动", "检测到来自可疑IP的异常活动", 4),
    
    /**
     * 性能降级
     */
    PERFORMANCE_DEGRADATION("PERFORMANCE_DEGRADATION", "性能降级", "系统性能出现降级", 3);
    
    /**
     * 事件代码
     * -- GETTER --
     *  获取事件代码
     *

     */
    private final String code;
    
    /**
     * 事件名称
     * -- GETTER --
     *  获取事件名称
     *

     */
    private final String name;
    
    /**
     * 事件描述
     * -- GETTER --
     *  获取事件描述
     *

     */
    private final String description;
    
    /**
     * 严重级别（1-5，数值越大越严重）
     * -- GETTER --
     *  获取严重级别
     *

     */
    private final int severity;
    
    /**
     * 构造函数
     * 
     * @param code 事件代码
     * @param name 事件名称
     * @param description 事件描述
     * @param severity 严重级别
     */
    SecurityEventType(String code, String name, String description, int severity) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.severity = severity;
    }

    /**
     * 根据代码获取安全事件类型枚举
     * 
     * @param code 事件代码
     * @return 安全事件类型枚举，如果不存在则返回null
     */
    public static SecurityEventType fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (SecurityEventType eventType : SecurityEventType.values()) {
            if (eventType.getCode().equals(code)) {
                return eventType;
            }
        }
        return null;
    }
    
    /**
     * 检查是否为高危安全事件（严重级别4-5）
     * 
     * @return 是否为高危事件
     */
    public boolean isHighRisk() {
        return severity >= 4;
    }
    
    /**
     * 检查是否为中等风险安全事件（严重级别3）
     * 
     * @return 是否为中等风险事件
     */
    public boolean isMediumRisk() {
        return severity == 3;
    }
    
    /**
     * 检查是否为低风险安全事件（严重级别1-2）
     * 
     * @return 是否为低风险事件
     */
    public boolean isLowRisk() {
        return severity <= 2;
    }
    
    /**
     * 检查是否为认证相关安全事件
     * 
     * @return 是否为认证相关事件
     */
    public boolean isAuthenticationEvent() {
        return this == BRUTE_FORCE_ATTACK || this == CONTINUOUS_LOGIN_FAILURE ||
               this == UNUSUAL_LOGIN_LOCATION || this == UNUSUAL_LOGIN_TIME ||
               this == ACCOUNT_LOCKED;
    }
    
    /**
     * 检查是否为访问控制相关安全事件
     * 
     * @return 是否为访问控制相关事件
     */
    public boolean isAccessControlEvent() {
        return this == PRIVILEGE_ESCALATION || this == ACCESS_DENIED ||
               this == SENSITIVE_DATA_ACCESS || this == BULK_DATA_ACCESS ||
               this == CROSS_DOMAIN_ANOMALY;
    }
    
    /**
     * 检查是否为输入安全相关事件
     * 
     * @return 是否为输入安全相关事件
     */
    public boolean isInputSecurityEvent() {
        return this == SQL_INJECTION_ATTEMPT || this == XSS_ATTACK_ATTEMPT ||
               this == CSRF_ATTACK_ATTEMPT || this == MALICIOUS_FILE_UPLOAD ||
               this == DANGEROUS_INPUT_CONTENT || this == INPUT_VALIDATION_FAILURE;
    }
    
    /**
     * 检查是否为网络安全相关事件
     * 
     * @return 是否为网络安全相关事件
     */
    public boolean isNetworkSecurityEvent() {
        return this == DDOS_ATTACK || this == ABNORMAL_ACCESS_FREQUENCY ||
               this == SUSPICIOUS_IP_ACCESS || this == PORT_SCANNING ||
               this == NETWORK_PROBING;
    }
    
    /**
     * 检查是否为系统安全相关事件
     * 
     * @return 是否为系统安全相关事件
     */
    public boolean isSystemSecurityEvent() {
        return this == SYSTEM_CONFIG_TAMPERED || this == CRITICAL_FILE_MODIFIED ||
               this == ABNORMAL_PROCESS_ACTIVITY || this == RESOURCE_USAGE_ANOMALY ||
               this == LOG_TAMPERING || this == BACKUP_FAILURE;
    }
    
    /**
     * 检查是否为数据安全相关事件
     * 
     * @return 是否为数据安全相关事件
     */
    public boolean isDataSecurityEvent() {
        return this == DATA_INTEGRITY_VIOLATION || this == SENSITIVE_DATA_LEAK ||
               this == ABNORMAL_DATA_DELETION || this == ABNORMAL_DATA_EXPORT;
    }
    
    /**
     * 检查是否为恶意代码相关事件
     * 
     * @return 是否为恶意代码相关事件
     */
    public boolean isMalwareEvent() {
        return this == VIRUS_DETECTED || this == MALICIOUS_SCRIPT_DETECTED ||
               this == DEPENDENCY_VULNERABILITY || this == CODE_INJECTION_ATTEMPT;
    }
    
    /**
     * 检查是否需要立即告警（高危事件）
     * 
     * @return 是否需要立即告警
     */
    public boolean requiresImmediateAlert() {
        return severity == 5;
    }
    
    /**
     * 获取严重级别描述
     * 
     * @return 严重级别描述
     */
    public String getSeverityDescription() {
        switch (severity) {
            case 5:
                return "严重";
            case 4:
                return "高";
            case 3:
                return "中";
            case 2:
                return "低";
            case 1:
                return "信息";
            default:
                return "未知";
        }
    }
    
    @Override
    public String toString() {
        return String.format("SecurityEventType{code='%s', name='%s', description='%s', severity=%d}", 
                           code, name, description, severity);
    }
}