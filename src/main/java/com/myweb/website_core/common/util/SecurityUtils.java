package com.myweb.website_core.common.util;

import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.application.service.security.authentication.SessionCleanupService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataDeletionService;
import com.myweb.website_core.application.service.security.SafeQueryService;
import com.myweb.website_core.common.exception.security.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 统一安全工具类
 * <p>
 * 提供统一的安全功能访问接口，包括：
 * - 输入验证
 * - 数据完整性检查
 * - 会话清理
 * - 数据删除
 * - 安全查询
 * - 随机数生成
 * - 令牌生成
 * <p>
 * 符合需求：10.1, 10.2, 10.3, 10.4, 10.5 - 统一安全工具类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Component
public class SecurityUtils {
    
    private static InputValidationService inputValidationService;
    private static DataIntegrityService dataIntegrityService;
    private static SessionCleanupService sessionCleanupService;
    private static DataDeletionService dataDeletionService;
    private static SafeQueryService safeQueryService;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 注入安全服务依赖
     */
    @Autowired
    public void setSecurityServices(
            InputValidationService inputValidationService,
            DataIntegrityService dataIntegrityService,
            SessionCleanupService sessionCleanupService,
            DataDeletionService dataDeletionService,
            SafeQueryService safeQueryService) {
        
        SecurityUtils.inputValidationService = inputValidationService;
        SecurityUtils.dataIntegrityService = dataIntegrityService;
        SecurityUtils.sessionCleanupService = sessionCleanupService;
        SecurityUtils.dataDeletionService = dataDeletionService;
        SecurityUtils.safeQueryService = safeQueryService;
    }
    
    // ==================== 输入验证方法 ====================
    
    /**
     * 验证字符串输入
     * 
     * @param input 输入字符串
     * @param fieldName 字段名称
     * @throws ValidationException 验证失败时抛出
     */
    public static void validateInput(String input, String fieldName) {
        if (inputValidationService != null) {
            inputValidationService.validateStringInput(input, fieldName);
        } else {
            log.warn("InputValidationService not initialized, skipping validation for field: {}", fieldName);
        }
    }
    
    /**
     * 验证字符串输入（指定最大长度）
     * 
     * @param input 输入字符串
     * @param fieldName 字段名称
     * @param maxLength 最大长度
     * @throws ValidationException 验证失败时抛出
     */
    public static void validateInput(String input, String fieldName, int maxLength) {
        if (inputValidationService != null) {
            inputValidationService.validateStringInput(input, fieldName, maxLength);
        } else {
            log.warn("InputValidationService not initialized, skipping validation for field: {}", fieldName);
        }
    }
    
    /**
     * 验证用户名
     * 
     * @param username 用户名
     * @throws ValidationException 验证失败时抛出
     */
    public static void validateUsername(String username) {
        if (inputValidationService != null) {
            inputValidationService.validateUsername(username);
        } else {
            log.warn("InputValidationService not initialized, skipping username validation");
        }
    }
    
    /**
     * 验证邮箱地址
     * 
     * @param email 邮箱地址
     * @throws ValidationException 验证失败时抛出
     */
    public static void validateEmail(String email) {
        if (inputValidationService != null) {
            inputValidationService.validateEmail(email);
        } else {
            log.warn("InputValidationService not initialized, skipping email validation");
        }
    }
    
    /**
     * 验证密码强度
     * 
     * @param password 密码
     * @throws ValidationException 验证失败时抛出
     */
    public static void validatePassword(String password) {
        if (inputValidationService != null) {
            inputValidationService.validatePassword(password);
        } else {
            log.warn("InputValidationService not initialized, skipping password validation");
        }
    }
    
    /**
     * 验证帖子标题
     * 
     * @param title 标题
     * @throws ValidationException 验证失败时抛出
     */
    public static void validatePostTitle(String title) {
        if (inputValidationService != null) {
            inputValidationService.validatePostTitle(title);
        } else {
            log.warn("InputValidationService not initialized, skipping title validation");
        }
    }
    
    /**
     * 验证帖子内容
     * 
     * @param content 内容
     * @throws ValidationException 验证失败时抛出
     */
    public static void validatePostContent(String content) {
        if (inputValidationService != null) {
            inputValidationService.validatePostContent(content);
        } else {
            log.warn("InputValidationService not initialized, skipping content validation");
        }
    }
    
    /**
     * 验证评论内容
     * 
     * @param content 评论内容
     * @throws ValidationException 验证失败时抛出
     */
    public static void validateCommentContent(String content) {
        if (inputValidationService != null) {
            inputValidationService.validateCommentContent(content);
        } else {
            log.warn("InputValidationService not initialized, skipping comment validation");
        }
    }
    
    /**
     * 验证文件名
     * 
     * @param filename 文件名
     * @throws ValidationException 验证失败时抛出
     */
    public static void validateFilename(String filename) {
        if (inputValidationService != null) {
            inputValidationService.validateFilename(filename);
        } else {
            log.warn("InputValidationService not initialized, skipping filename validation");
        }
    }
    
    // ==================== 数据完整性方法 ====================
    
    /**
     * 计算数据哈希值
     * 
     * @param data 要计算哈希的数据
     * @return Base64编码的哈希值
     */
    public static String calculateHash(String data) {
        if (dataIntegrityService != null) {
            return dataIntegrityService.calculateHash(data);
        } else {
            log.warn("DataIntegrityService not initialized, returning null hash");
            return null;
        }
    }
    
    /**
     * 验证数据完整性
     * 
     * @param data 当前数据
     * @param expectedHash 期望的哈希值
     * @return 验证是否通过
     */
    public static boolean verifyIntegrity(String data, String expectedHash) {
        if (dataIntegrityService != null) {
            return dataIntegrityService.verifyIntegrity(data, expectedHash);
        } else {
            log.warn("DataIntegrityService not initialized, returning false for integrity check");
            return false;
        }
    }
    
    /**
     * 检查帖子内容完整性
     * 
     * @param postId 帖子ID
     * @param content 帖子内容
     * @param storedHash 存储的哈希值
     * @return 检查结果
     */
    public static DataIntegrityService.IntegrityCheckResult checkPostIntegrity(Long postId, String content, String storedHash) {
        if (dataIntegrityService != null) {
            return dataIntegrityService.checkPostIntegrity(postId, content, storedHash);
        } else {
            log.warn("DataIntegrityService not initialized, returning null for post integrity check");
            return null;
        }
    }
    
    // ==================== 会话清理方法 ====================
    
    /**
     * 清理用户会话数据
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @return 清理统计信息的Future
     */
    public static CompletableFuture<SessionCleanupService.CleanupStatistics> cleanupUserSession(Long userId, String sessionId) {
        if (sessionCleanupService != null) {
            return sessionCleanupService.cleanupUserSession(userId, sessionId);
        } else {
            log.warn("SessionCleanupService not initialized, returning empty cleanup statistics");
            return CompletableFuture.completedFuture(new SessionCleanupService.CleanupStatistics());
        }
    }
    
    /**
     * 用户退出时的完整数据清理
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress 客户端IP地址
     * @param reason 清理原因
     * @return 清理是否成功的Future
     */
    public static CompletableFuture<Boolean> performUserLogoutCleanup(String sessionId, Long userId, 
                                                                     String username, String ipAddress, 
                                                                     String reason) {
        if (sessionCleanupService != null) {
            return sessionCleanupService.performUserLogoutCleanup(sessionId, userId, username, ipAddress, reason);
        } else {
            log.warn("SessionCleanupService not initialized, returning false for logout cleanup");
            return CompletableFuture.completedFuture(false);
        }
    }
    
    // ==================== 数据删除方法 ====================
    
    /**
     * 彻底删除实体数据
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param userId 用户ID
     * @param cascadeDelete 是否级联删除
     * @return 删除是否成功的Future
     */
    public static CompletableFuture<Boolean> deleteEntityCompletely(String entityType, Long entityId, 
                                                                   Long userId, boolean cascadeDelete) {
        log.warn("DataDeletionService not yet implemented, returning false for entity deletion");
        return CompletableFuture.completedFuture(false);
    }
    
    // ==================== 安全查询方法 ====================
    
    /**
     * 验证用户输入是否安全
     * 
     * @param input 用户输入
     * @param inputType 输入类型
     * @param fieldName 字段名称
     */
    public static void validateUserInput(String input, String inputType, String fieldName) {
        if (safeQueryService != null) {
            safeQueryService.validateUserInputSafety(input, inputType, fieldName);
        } else {
            log.warn("SafeQueryService not initialized, skipping user input validation");
        }
    }
    
    /**
     * 获取允许的排序字段
     * 
     * @param tableName 表名
     * @return 允许的字段列表
     */
    public static java.util.List<String> getAllowedSortFields(String tableName) {
        if (safeQueryService != null) {
            return safeQueryService.getAllowedSortFields(tableName);
        } else {
            log.warn("SafeQueryService not initialized, returning empty list for allowed sort fields");
            return java.util.Collections.emptyList();
        }
    }
    
    // ==================== 随机数和令牌生成方法 ====================
    
    /**
     * 生成安全的随机字符串
     * 
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateSecureRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return result.toString();
    }
    
    /**
     * 生成安全的令牌
     * 
     * @return 安全令牌
     */
    public static String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               System.currentTimeMillis() + 
               generateSecureRandomString(8);
    }
    
    /**
     * 生成安全的数字令牌
     * 
     * @param length 令牌长度
     * @return 数字令牌
     */
    public static String generateSecureNumericToken(int length) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            result.append(secureRandom.nextInt(10));
        }
        
        return result.toString();
    }
    
    /**
     * 生成安全的UUID
     * 
     * @return UUID字符串
     */
    public static String generateSecureUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成安全的会话ID
     * 
     * @return 会话ID
     */
    public static String generateSecureSessionId() {
        return generateSecureUUID().replace("-", "") + 
               Long.toHexString(System.currentTimeMillis()) +
               generateSecureRandomString(16);
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 检查服务是否已初始化
     * 
     * @return 如果所有服务都已初始化返回true
     */
    public static boolean isInitialized() {
        return inputValidationService != null && 
               dataIntegrityService != null && 
               sessionCleanupService != null && 
               dataDeletionService != null && 
               safeQueryService != null;
    }
    
    /**
     * 获取初始化状态信息
     * 
     * @return 初始化状态信息
     */
    public static String getInitializationStatus() {
        return String.format(
            "SecurityUtils初始化状态: InputValidation=%s, DataIntegrity=%s, SessionCleanup=%s, DataDeletion=%s, SafeQuery=%s",
            inputValidationService != null ? "已初始化" : "未初始化",
            dataIntegrityService != null ? "已初始化" : "未初始化",
            sessionCleanupService != null ? "已初始化" : "未初始化",
            dataDeletionService != null ? "已初始化" : "未初始化",
            safeQueryService != null ? "已初始化" : "未初始化"
        );
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private SecurityUtils() {
        // 工具类不允许实例化
    }
}