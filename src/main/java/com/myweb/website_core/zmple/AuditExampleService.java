package com.myweb.website_core.zmple;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审计示例服务
 * 
 * 演示如何使用@Auditable注解进行方法审计
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class AuditExampleService {
    
    /**
     * 基本审计示例
     * 记录用户登录操作
     */
    @Auditable(
        operation = AuditOperation.USER_LOGIN_SUCCESS,
        resourceType = "USER",
        description = "用户登录操作"
    )
    public String userLogin(String username, String password) {
        log.info("用户 {} 尝试登录", username);
        // 实际的登录逻辑...
        return "登录成功";
    }
    
    /**
     * 敏感参数审计示例
     * 密码参数将被脱敏处理
     */
    @Auditable(
        operation = AuditOperation.PASSWORD_CHANGED,
        resourceType = "USER",
        description = "用户修改密码",
        sensitiveParams = {1, 2}, // 旧密码和新密码参数索引
        riskLevel = 4,
        tags = "security,password"
    )
    public String changePassword(String username, String oldPassword, String newPassword) {
        log.info("用户 {} 修改密码", username);
        // 实际的密码修改逻辑...
        return "密码修改成功";
    }
    
    /**
     * 高风险操作审计示例
     * 管理员删除用户操作
     */
    @Auditable(
        operation = AuditOperation.USER_MANAGEMENT,
        resourceType = "USER",
        description = "管理员删除用户",
        riskLevel = 5,
        tags = "admin,delete,critical"
    )
    public String deleteUser(Long userId, String reason) {
        log.warn("管理员删除用户: userId={}, reason={}", userId, reason);
        // 实际的用户删除逻辑...
        return "用户删除成功";
    }
    
    /**
     * 内容操作审计示例
     * 用户创建帖子
     */
    @Auditable(
        operation = AuditOperation.POST_CREATE,
        resourceType = "POST",
        description = "用户创建帖子",
        maxParamLength = 500, // 限制参数长度
        maxResponseLength = 200 // 限制响应长度
    )
    public String createPost(String title, String content, String[] tags) {
        log.info("用户创建帖子: title={}", title);
        // 实际的帖子创建逻辑...
        return "帖子创建成功，ID: 12345";
    }
    
    /**
     * 文件操作审计示例
     * 用户上传文件
     */
    @Auditable(
        operation = AuditOperation.FILE_UPLOAD,
        resourceType = "FILE",
        description = "用户上传文件",
        riskLevel = 3,
        tags = "file,upload"
    )
    public String uploadFile(String fileName, byte[] fileContent, String fileType) {
        log.info("用户上传文件: fileName={}, fileType={}, size={}", 
                fileName, fileType, fileContent.length);
        // 实际的文件上传逻辑...
        return "文件上传成功";
    }
    
    /**
     * 搜索操作审计示例
     * 用户搜索内容
     */
    @Auditable(
        operation = AuditOperation.SEARCH_OPERATION,
        resourceType = "SEARCH",
        description = "用户搜索操作",
        logResponse = false, // 不记录搜索结果
        riskLevel = 1
    )
    public String searchContent(String keyword, String category, int page, int size) {
        log.info("用户搜索: keyword={}, category={}", keyword, category);
        // 实际的搜索逻辑...
        return "搜索完成，找到10条结果";
    }
    
    /**
     * 系统配置审计示例
     * 管理员修改系统配置
     */
    @Auditable(
        operation = AuditOperation.SYSTEM_CONFIG_UPDATE,
        resourceType = "CONFIG",
        description = "管理员修改系统配置",
        riskLevel = 5,
        tags = "admin,config,system",
        async = false // 同步记录审计日志
    )
    public String updateSystemConfig(String configKey, String configValue) {
        log.warn("管理员修改系统配置: key={}, value={}", configKey, configValue);
        // 实际的配置修改逻辑...
        return "配置修改成功";
    }
    
    /**
     * 异常处理审计示例
     * 可能失败的操作
     */
    @Auditable(
        operation = AuditOperation.DATA_BACKUP,
        resourceType = "SYSTEM",
        description = "系统数据备份",
        ignoreAuditException = true // 忽略审计异常，确保不影响业务
    )
    public String backupData(String backupPath) {
        log.info("开始数据备份: path={}", backupPath);
        
        // 模拟可能的失败情况
        if (backupPath == null || backupPath.isEmpty()) {
            throw new IllegalArgumentException("备份路径不能为空");
        }
        
        // 实际的备份逻辑...
        return "数据备份成功";
    }
    
    /**
     * 批量操作审计示例
     * 批量删除帖子
     */
    @Auditable(
        operation = AuditOperation.POST_DELETE,
        resourceType = "POST",
        description = "批量删除帖子",
        riskLevel = 4,
        tags = "batch,delete"
    )
    public String batchDeletePosts(Long[] postIds, String reason) {
        log.warn("批量删除帖子: count={}, reason={}", postIds.length, reason);
        // 实际的批量删除逻辑...
        return String.format("成功删除 %d 个帖子", postIds.length);
    }
    
    /**
     * 权限操作审计示例
     * 管理员分配角色
     */
    @Auditable(
        operation = AuditOperation.ROLE_ASSIGNMENT,
        resourceType = "ROLE",
        description = "管理员分配用户角色",
        riskLevel = 4,
        tags = "admin,role,permission"
    )
    public String assignRole(Long userId, String roleName, String reason) {
        log.info("管理员分配角色: userId={}, role={}, reason={}", userId, roleName, reason);
        // 实际的角色分配逻辑...
        return "角色分配成功";
    }
}