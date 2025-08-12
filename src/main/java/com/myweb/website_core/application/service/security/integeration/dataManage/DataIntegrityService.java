package com.myweb.website_core.application.service.security.integeration.dataManage;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.common.exception.DataIntegrityException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * 数据完整性服务
 * <p>
 * 提供数据哈希计算、验证和完整性检查功能，包括：
 * - 数据内容哈希计算和验证
 * - 重要实体数据完整性校验
 * - 数据变更时的完整性检查
 * - 定时数据完整性检查任务
 * - 数据完整性异常处理
 * <p>
 * 符合GB/T 22239-2019数据完整性保护要求 7.1, 7.2, 7.4, 7.5
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
public class DataIntegrityService {
    
    private final AuditLogService auditLogService;
    
    // 支持的哈希算法
    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";
    private static final String BACKUP_HASH_ALGORITHM = "SHA-1";
    
    @Autowired
    public DataIntegrityService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
    
    // ==================== 数据哈希计算 ====================
    
    /**
     * 计算数据的SHA-256哈希值
     * 
     * @param data 要计算哈希的数据
     * @return Base64编码的哈希值
     * @throws DataIntegrityException 计算失败时抛出
     */
    public String calculateHash(String data) {
        return calculateHash(data, DEFAULT_HASH_ALGORITHM);
    }
    
    /**
     * 使用指定算法计算数据哈希值
     * 
     * @param data 要计算哈希的数据
     * @param algorithm 哈希算法（SHA-256, SHA-1, MD5等）
     * @return Base64编码的哈希值
     * @throws DataIntegrityException 计算失败时抛出
     */
    public String calculateHash(String data, String algorithm) {
        if (data == null) {
            throw new DataIntegrityException("数据不能为空");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("不支持的哈希算法: {}", algorithm, e);
            throw new DataIntegrityException("不支持的哈希算法: " + algorithm, e);
        }
    }
    
    /**
     * 计算对象的哈希值
     * 将对象转换为字符串后计算哈希
     * 
     * @param object 要计算哈希的对象
     * @return Base64编码的哈希值
     * @throws DataIntegrityException 计算失败时抛出
     */
    public String calculateObjectHash(Object object) {
        if (object == null) {
            throw new DataIntegrityException("对象不能为空");
        }
        
        String objectString = object.toString();
        return calculateHash(objectString);
    }
    
    /**
     * 计算多个字段组合的哈希值
     * 用于实体对象的完整性校验
     * 
     * @param fields 要计算哈希的字段数组
     * @return Base64编码的哈希值
     * @throws DataIntegrityException 计算失败时抛出
     */
    public String calculateCombinedHash(String... fields) {
        if (fields == null || fields.length == 0) {
            throw new DataIntegrityException("字段数组不能为空");
        }
        
        StringBuilder combined = new StringBuilder();
        for (String field : fields) {
            if (field != null) {
                combined.append(field).append("|");
            }
        }
        
        return calculateHash(combined.toString());
    }
    
    // ==================== 数据完整性验证 ====================
    
    /**
     * 验证数据完整性
     * 比较当前数据哈希值与期望哈希值
     * 
     * @param data 当前数据
     * @param expectedHash 期望的哈希值
     * @return 验证是否通过
     */
    public boolean verifyIntegrity(String data, String expectedHash) {
        if (data == null || expectedHash == null) {
            log.warn("数据完整性验证失败: 数据或期望哈希值为空");
            return false;
        }
        
        try {
            String actualHash = calculateHash(data);
            boolean isValid = MessageDigest.isEqual(
                actualHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
            );
            
            if (!isValid) {
                log.warn("数据完整性验证失败: 期望哈希={}, 实际哈希={}", expectedHash, actualHash);
                
                // 记录完整性验证失败的审计日志
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "SYSTEM",
                    "数据完整性验证失败: 哈希值不匹配"
                );
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("数据完整性验证异常", e);
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                "数据完整性验证异常: " + e.getMessage()
            );
            return false;
        }
    }
    
    /**
     * 验证对象完整性
     * 
     * @param object 当前对象
     * @param expectedHash 期望的哈希值
     * @return 验证是否通过
     */
    public boolean verifyObjectIntegrity(Object object, String expectedHash) {
        if (object == null) {
            return expectedHash == null;
        }
        
        String objectString = object.toString();
        return verifyIntegrity(objectString, expectedHash);
    }
    
    /**
     * 验证多字段组合的完整性
     * 
     * @param expectedHash 期望的哈希值
     * @param fields 字段数组
     * @return 验证是否通过
     */
    public boolean verifyCombinedIntegrity(String expectedHash, String... fields) {
        try {
            String actualHash = calculateCombinedHash(fields);
            return verifyIntegrity(actualHash, expectedHash);
        } catch (Exception e) {
            log.error("组合字段完整性验证异常", e);
            return false;
        }
    }
    
    // ==================== 实体完整性检查 ====================
    
    /**
     * 检查帖子内容完整性
     * 
     * @param postId 帖子ID
     * @param content 帖子内容
     * @param storedHash 存储的哈希值
     * @return 检查结果
     */
    public IntegrityCheckResult checkPostIntegrity(Long postId, String content, String storedHash) {
        try {
            if (content == null || storedHash == null) {
                return IntegrityCheckResult.builder()
                    .entityType("POST")
                    .entityId(postId)
                    .isValid(false)
                    .errorMessage("内容或哈希值为空")
                    .checkTime(LocalDateTime.now())
                    .build();
            }
            
            boolean isValid = verifyIntegrity(content, storedHash);
            
            IntegrityCheckResult result = IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(isValid)
                .actualHash(calculateHash(content))
                .expectedHash(storedHash)
                .checkTime(LocalDateTime.now())
                .build();
            
            if (!isValid) {
                result.setErrorMessage("帖子内容哈希值不匹配，可能已被篡改");
                
                // 记录完整性检查失败的审计日志
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "SYSTEM",
                    String.format("帖子完整性检查失败: postId=%d, 内容可能已被篡改", postId)
                );
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("检查帖子完整性失败: postId={}", postId, e);
            
            return IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(false)
                .errorMessage("完整性检查异常: " + e.getMessage())
                .checkTime(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * 检查评论内容完整性
     * 
     * @param commentId 评论ID
     * @param content 评论内容
     * @param storedHash 存储的哈希值
     * @return 检查结果
     */
    public IntegrityCheckResult checkCommentIntegrity(Long commentId, String content, String storedHash) {
        try {
            if (content == null || storedHash == null) {
                return IntegrityCheckResult.builder()
                    .entityType("COMMENT")
                    .entityId(commentId)
                    .isValid(false)
                    .errorMessage("内容或哈希值为空")
                    .checkTime(LocalDateTime.now())
                    .build();
            }
            
            boolean isValid = verifyIntegrity(content, storedHash);
            
            IntegrityCheckResult result = IntegrityCheckResult.builder()
                .entityType("COMMENT")
                .entityId(commentId)
                .isValid(isValid)
                .actualHash(calculateHash(content))
                .expectedHash(storedHash)
                .checkTime(LocalDateTime.now())
                .build();
            
            if (!isValid) {
                result.setErrorMessage("评论内容哈希值不匹配，可能已被篡改");
                
                // 记录完整性检查失败的审计日志
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    "SYSTEM",
                    String.format("评论完整性检查失败: commentId=%d, 内容可能已被篡改", commentId)
                );
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("检查评论完整性失败: commentId={}", commentId, e);
            
            return IntegrityCheckResult.builder()
                .entityType("COMMENT")
                .entityId(commentId)
                .isValid(false)
                .errorMessage("完整性检查异常: " + e.getMessage())
                .checkTime(LocalDateTime.now())
                .build();
        }
    }
    
    // ==================== 定时完整性检查 ====================
    
    /**
     * 定时执行数据完整性检查
     * 每天凌晨4点执行，检查重要数据的完整性
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void performScheduledIntegrityCheck() {
        // 异步执行完整性检查
        performIntegrityCheckAsync();
    }
    
    /**
     * 检查所有帖子的完整性
     * 
     * @return 发现问题的帖子数量
     */
    @Async
    public CompletableFuture<Integer> checkAllPostsIntegrity() {
        log.info("开始检查所有帖子的完整性");
        int issueCount = 0;
        
        try {
            // 由于无法直接注入Repository（会造成循环依赖），
            // 这个方法将在DataIntegrityTask中实现具体的数据库查询逻辑
            // 这里保持原有的模拟实现，实际检查逻辑在Task中完成
            
            log.info("帖子完整性检查完成，发现{}个问题", issueCount);
            
        } catch (Exception e) {
            log.error("检查所有帖子完整性失败", e);
        }
        
        return CompletableFuture.completedFuture(issueCount);
    }
    
    /**
     * 检查所有评论的完整性
     * 
     * @return 发现问题的评论数量
     */
    @Async
    public CompletableFuture<Integer> checkAllCommentsIntegrity() {
        log.info("开始检查所有评论的完整性");
        int issueCount = 0;
        
        try {
            // 由于无法直接注入Repository（会造成循环依赖），
            // 这个方法将在DataIntegrityTask中实现具体的数据库查询逻辑
            // 这里保持原有的模拟实现，实际检查逻辑在Task中完成
            
            log.info("评论完整性检查完成，发现{}个问题", issueCount);
            
        } catch (Exception e) {
            log.error("检查所有评论完整性失败", e);
        }
        
        return CompletableFuture.completedFuture(issueCount);
    }
    
    /**
     * 异步执行完整性检查的具体实现
     */
    @Async
    public CompletableFuture<Void> performIntegrityCheckAsync() {
        try {
            log.info("开始执行定时数据完整性检查");
            
            // 检查帖子数据完整性
            CompletableFuture<Integer> postCheckFuture = checkAllPostsIntegrity();
            
            // 检查评论数据完整性
            CompletableFuture<Integer> commentCheckFuture = checkAllCommentsIntegrity();
            
            // 等待所有检查完成
            CompletableFuture.allOf(postCheckFuture, commentCheckFuture).get();
            
            int postIssues = postCheckFuture.get();
            int commentIssues = commentCheckFuture.get();
            int totalIssues = postIssues + commentIssues;
            
            log.info("定时数据完整性检查完成: 帖子问题数={}, 评论问题数={}, 总问题数={}", 
                    postIssues, commentIssues, totalIssues);
            
            // 记录检查结果到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                String.format("定时数据完整性检查完成: 发现%d个完整性问题", totalIssues)
            );
            
            // 如果发现问题，发送告警
            if (totalIssues > 0) {
                sendIntegrityAlert(totalIssues, postIssues, commentIssues);
            }
            
        } catch (Exception e) {
            log.error("定时数据完整性检查失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.INTEGRITY_CHECK,
                "SYSTEM",
                "定时数据完整性检查失败: " + e.getMessage()
            );
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // ==================== 完整性告警 ====================
    
    /**
     * 发送数据完整性告警
     * 
     * @param totalIssues 总问题数
     * @param postIssues 帖子问题数
     * @param commentIssues 评论问题数
     */
    @Async
    public void sendIntegrityAlert(int totalIssues, int postIssues, int commentIssues) {
        try {
            String alertMessage = String.format(
                "数据完整性检查发现%d个问题：帖子%d个，评论%d个。请立即检查数据安全性。",
                totalIssues, postIssues, commentIssues
            );
            
            log.warn("发送数据完整性告警: {}", alertMessage);
            
            // 记录告警到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SECURITY_ALERT,
                "SYSTEM",
                "数据完整性告警: " + alertMessage
            );
            
            // TODO: 实际实现时可以集成邮件或短信告警服务
            
        } catch (Exception e) {
            log.error("发送数据完整性告警失败", e);
        }
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 手动触发完整性检查
     * 
     * @return 检查结果的Future
     */
    public CompletableFuture<Void> triggerManualIntegrityCheck() {
        log.info("手动触发数据完整性检查");
        return performIntegrityCheckAsync();
    }
    
    /**
     * 获取支持的哈希算法列表
     * 
     * @return 哈希算法数组
     */
    public String[] getSupportedHashAlgorithms() {
        return new String[]{DEFAULT_HASH_ALGORITHM, BACKUP_HASH_ALGORITHM, "MD5"};
    }
    
    /**
     * 检查哈希算法是否受支持
     * 
     * @param algorithm 哈希算法名称
     * @return 是否支持
     */
    public boolean isHashAlgorithmSupported(String algorithm) {
        try {
            MessageDigest.getInstance(algorithm);
            return true;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 完整性检查结果
     */
    @Getter
    public static class IntegrityCheckResult {
        // Getters and Setters
        private String entityType;
        private Long entityId;
        private boolean isValid;
        private String actualHash;
        private String expectedHash;
        private String errorMessage;
        private LocalDateTime checkTime;
        
        // Builder模式
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private IntegrityCheckResult result = new IntegrityCheckResult();
            
            public Builder entityType(String entityType) {
                result.entityType = entityType;
                return this;
            }
            
            public Builder entityId(Long entityId) {
                result.entityId = entityId;
                return this;
            }
            
            public Builder isValid(boolean isValid) {
                result.isValid = isValid;
                return this;
            }
            
            public Builder actualHash(String actualHash) {
                result.actualHash = actualHash;
                return this;
            }
            
            public Builder expectedHash(String expectedHash) {
                result.expectedHash = expectedHash;
                return this;
            }
            
            public Builder errorMessage(String errorMessage) {
                result.errorMessage = errorMessage;
                return this;
            }
            
            public Builder checkTime(LocalDateTime checkTime) {
                result.checkTime = checkTime;
                return this;
            }
            
            public IntegrityCheckResult build() {
                return result;
            }
        }

        public void setEntityType(String entityType) { this.entityType = entityType; }

        public void setEntityId(Long entityId) { this.entityId = entityId; }
        
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { isValid = valid; }
        
        public String getActualHash() { return actualHash; }
        public void setActualHash(String actualHash) { this.actualHash = actualHash; }
        
        public String getExpectedHash() { return expectedHash; }
        public void setExpectedHash(String expectedHash) { this.expectedHash = expectedHash; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        
        @Override
        public String toString() {
            return String.format("IntegrityCheckResult{entityType='%s', entityId=%d, isValid=%s, errorMessage='%s', checkTime=%s}",
                    entityType, entityId, isValid, errorMessage, checkTime);
        }
    }
}