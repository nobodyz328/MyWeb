package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.dataprotect.DataIntegrityService;
import com.myweb.website_core.common.exception.DataIntegrityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 数据完整性服务测试类
 * 
 * 测试数据完整性服务的各项功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@ExtendWith(MockitoExtension.class)
class DataIntegrityServiceTest {
    
    @Mock
    private AuditLogService auditLogService;
    
    private DataIntegrityService dataIntegrityService;
    
    @BeforeEach
    void setUp() {
        dataIntegrityService = new DataIntegrityService(auditLogService);
    }
    
    // ==================== 哈希计算测试 ====================
    
    @Test
    void testCalculateHash_WithValidData_ShouldReturnHash() {
        // 准备测试数据
        String testData = "这是一个测试内容";
        
        // 执行测试
        String hash = dataIntegrityService.calculateHash(testData);
        
        // 验证结果
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertTrue(hash.length() > 0);
        
        // 验证相同数据产生相同哈希
        String hash2 = dataIntegrityService.calculateHash(testData);
        assertEquals(hash, hash2);
    }
    
    @Test
    void testCalculateHash_WithNullData_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(DataIntegrityException.class, () -> {
            dataIntegrityService.calculateHash(null);
        });
    }
    
    @Test
    void testCalculateHash_WithEmptyData_ShouldReturnHash() {
        // 准备测试数据
        String testData = "";
        
        // 执行测试
        String hash = dataIntegrityService.calculateHash(testData);
        
        // 验证结果
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }
    
    @Test
    void testCalculateHash_WithDifferentAlgorithm_ShouldReturnDifferentHash() {
        // 准备测试数据
        String testData = "测试数据";
        
        // 执行测试
        String sha256Hash = dataIntegrityService.calculateHash(testData, "SHA-256");
        String sha1Hash = dataIntegrityService.calculateHash(testData, "SHA-1");
        
        // 验证结果
        assertNotNull(sha256Hash);
        assertNotNull(sha1Hash);
        assertNotEquals(sha256Hash, sha1Hash);
    }
    
    @Test
    void testCalculateHash_WithUnsupportedAlgorithm_ShouldThrowException() {
        // 准备测试数据
        String testData = "测试数据";
        
        // 执行测试并验证异常
        assertThrows(DataIntegrityException.class, () -> {
            dataIntegrityService.calculateHash(testData, "UNSUPPORTED_ALGORITHM");
        });
    }
    
    @Test
    void testCalculateObjectHash_WithValidObject_ShouldReturnHash() {
        // 准备测试数据
        Object testObject = "测试对象";
        
        // 执行测试
        String hash = dataIntegrityService.calculateObjectHash(testObject);
        
        // 验证结果
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }
    
    @Test
    void testCalculateObjectHash_WithNullObject_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(DataIntegrityException.class, () -> {
            dataIntegrityService.calculateObjectHash(null);
        });
    }
    
    @Test
    void testCalculateCombinedHash_WithValidFields_ShouldReturnHash() {
        // 准备测试数据
        String[] fields = {"field1", "field2", "field3"};
        
        // 执行测试
        String hash = dataIntegrityService.calculateCombinedHash(fields);
        
        // 验证结果
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        
        // 验证相同字段产生相同哈希
        String hash2 = dataIntegrityService.calculateCombinedHash(fields);
        assertEquals(hash, hash2);
    }
    
    @Test
    void testCalculateCombinedHash_WithNullFields_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(DataIntegrityException.class, () -> {
            dataIntegrityService.calculateCombinedHash((String[]) null);
        });
    }
    
    @Test
    void testCalculateCombinedHash_WithEmptyFields_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(DataIntegrityException.class, () -> {
            dataIntegrityService.calculateCombinedHash();
        });
    }
    
    // ==================== 完整性验证测试 ====================
    
    @Test
    void testVerifyIntegrity_WithMatchingHash_ShouldReturnTrue() {
        // 准备测试数据
        String testData = "测试数据";
        String expectedHash = dataIntegrityService.calculateHash(testData);
        
        // 执行测试
        boolean result = dataIntegrityService.verifyIntegrity(testData, expectedHash);
        
        // 验证结果
        assertTrue(result);
    }
    
    @Test
    void testVerifyIntegrity_WithNonMatchingHash_ShouldReturnFalse() {
        // 准备测试数据
        String testData = "测试数据";
        String wrongHash = "错误的哈希值";
        
        // 执行测试
        boolean result = dataIntegrityService.verifyIntegrity(testData, wrongHash);
        
        // 验证结果
        assertFalse(result);
        
        // 验证审计日志被调用
        verify(auditLogService).logSecurityEvent(any(), eq("SYSTEM"), contains("数据完整性验证失败"));
    }
    
    @Test
    void testVerifyIntegrity_WithNullData_ShouldReturnFalse() {
        // 准备测试数据
        String expectedHash = "someHash";
        
        // 执行测试
        boolean result = dataIntegrityService.verifyIntegrity(null, expectedHash);
        
        // 验证结果
        assertFalse(result);
    }
    
    @Test
    void testVerifyIntegrity_WithNullHash_ShouldReturnFalse() {
        // 准备测试数据
        String testData = "测试数据";
        
        // 执行测试
        boolean result = dataIntegrityService.verifyIntegrity(testData, null);
        
        // 验证结果
        assertFalse(result);
    }
    
    @Test
    void testVerifyObjectIntegrity_WithValidObject_ShouldReturnTrue() {
        // 准备测试数据
        String testObject = "测试对象";
        String expectedHash = dataIntegrityService.calculateObjectHash(testObject);
        
        // 执行测试
        boolean result = dataIntegrityService.verifyObjectIntegrity(testObject, expectedHash);
        
        // 验证结果
        assertTrue(result);
    }
    
    @Test
    void testVerifyObjectIntegrity_WithNullObject_ShouldReturnTrueIfHashIsNull() {
        // 执行测试
        boolean result = dataIntegrityService.verifyObjectIntegrity(null, null);
        
        // 验证结果
        assertTrue(result);
    }
    
    @Test
    void testVerifyObjectIntegrity_WithNullObject_ShouldReturnFalseIfHashIsNotNull() {
        // 执行测试
        boolean result = dataIntegrityService.verifyObjectIntegrity(null, "someHash");
        
        // 验证结果
        assertFalse(result);
    }
    
    // ==================== 实体完整性检查测试 ====================
    
    @Test
    void testCheckPostIntegrity_WithValidData_ShouldReturnValidResult() {
        // 准备测试数据
        Long postId = 1L;
        String content = "这是帖子内容";
        String storedHash = dataIntegrityService.calculateHash(content);
        
        // 执行测试
        DataIntegrityService.IntegrityCheckResult result = 
            dataIntegrityService.checkPostIntegrity(postId, content, storedHash);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("POST", result.getEntityType());
        assertEquals(postId, result.getEntityId());
        assertTrue(result.isValid());
        assertNotNull(result.getCheckTime());
    }
    
    @Test
    void testCheckPostIntegrity_WithInvalidHash_ShouldReturnInvalidResult() {
        // 准备测试数据
        Long postId = 1L;
        String content = "这是帖子内容";
        String wrongHash = "错误的哈希值";
        
        // 执行测试
        DataIntegrityService.IntegrityCheckResult result = 
            dataIntegrityService.checkPostIntegrity(postId, content, wrongHash);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("POST", result.getEntityType());
        assertEquals(postId, result.getEntityId());
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("哈希值不匹配"));
        
        // 验证审计日志被调用
        verify(auditLogService).logSecurityEvent(any(), eq("SYSTEM"), contains("帖子完整性检查失败"));
    }
    
    @Test
    void testCheckPostIntegrity_WithNullContent_ShouldReturnInvalidResult() {
        // 准备测试数据
        Long postId = 1L;
        String storedHash = "someHash";
        
        // 执行测试
        DataIntegrityService.IntegrityCheckResult result = 
            dataIntegrityService.checkPostIntegrity(postId, null, storedHash);
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("内容或哈希值为空"));
    }
    
    @Test
    void testCheckCommentIntegrity_WithValidData_ShouldReturnValidResult() {
        // 准备测试数据
        Long commentId = 1L;
        String content = "这是评论内容";
        String storedHash = dataIntegrityService.calculateHash(content);
        
        // 执行测试
        DataIntegrityService.IntegrityCheckResult result = 
            dataIntegrityService.checkCommentIntegrity(commentId, content, storedHash);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("COMMENT", result.getEntityType());
        assertEquals(commentId, result.getEntityId());
        assertTrue(result.isValid());
        assertNotNull(result.getCheckTime());
    }
    
    @Test
    void testCheckCommentIntegrity_WithInvalidHash_ShouldReturnInvalidResult() {
        // 准备测试数据
        Long commentId = 1L;
        String content = "这是评论内容";
        String wrongHash = "错误的哈希值";
        
        // 执行测试
        DataIntegrityService.IntegrityCheckResult result = 
            dataIntegrityService.checkCommentIntegrity(commentId, content, wrongHash);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("COMMENT", result.getEntityType());
        assertEquals(commentId, result.getEntityId());
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("哈希值不匹配"));
        
        // 验证审计日志被调用
        verify(auditLogService).logSecurityEvent(any(), eq("SYSTEM"), contains("评论完整性检查失败"));
    }
    
    // ==================== 定时检查测试 ====================
    
    @Test
    void testCheckAllPostsIntegrity_ShouldReturnCompletableFuture() {
        // 执行测试
        CompletableFuture<Integer> future = dataIntegrityService.checkAllPostsIntegrity();
        
        // 验证结果
        assertNotNull(future);
        assertDoesNotThrow(() -> {
            Integer result = future.get();
            assertNotNull(result);
            assertTrue(result >= 0);
        });
    }
    
    @Test
    void testCheckAllCommentsIntegrity_ShouldReturnCompletableFuture() {
        // 执行测试
        CompletableFuture<Integer> future = dataIntegrityService.checkAllCommentsIntegrity();
        
        // 验证结果
        assertNotNull(future);
        assertDoesNotThrow(() -> {
            Integer result = future.get();
            assertNotNull(result);
            assertTrue(result >= 0);
        });
    }
    
    @Test
    void testTriggerManualIntegrityCheck_ShouldReturnCompletableFuture() {
        // 执行测试
        CompletableFuture<Void> future = dataIntegrityService.triggerManualIntegrityCheck();
        
        // 验证结果
        assertNotNull(future);
        assertDoesNotThrow(() -> future.get());
    }
    
    // ==================== 工具方法测试 ====================
    
    @Test
    void testGetSupportedHashAlgorithms_ShouldReturnAlgorithms() {
        // 执行测试
        String[] algorithms = dataIntegrityService.getSupportedHashAlgorithms();
        
        // 验证结果
        assertNotNull(algorithms);
        assertTrue(algorithms.length > 0);
        assertTrue(java.util.Arrays.asList(algorithms).contains("SHA-256"));
    }
    
    @Test
    void testIsHashAlgorithmSupported_WithSupportedAlgorithm_ShouldReturnTrue() {
        // 执行测试
        boolean result = dataIntegrityService.isHashAlgorithmSupported("SHA-256");
        
        // 验证结果
        assertTrue(result);
    }
    
    @Test
    void testIsHashAlgorithmSupported_WithUnsupportedAlgorithm_ShouldReturnFalse() {
        // 执行测试
        boolean result = dataIntegrityService.isHashAlgorithmSupported("UNSUPPORTED_ALGORITHM");
        
        // 验证结果
        assertFalse(result);
    }
    
    @Test
    void testSendIntegrityAlert_ShouldNotThrowException() {
        // 执行测试
        assertDoesNotThrow(() -> {
            dataIntegrityService.sendIntegrityAlert(5, 3, 2);
        });
        
        // 验证审计日志被调用
        verify(auditLogService).logSecurityEvent(any(), eq("SYSTEM"), contains("数据完整性告警"));
    }
}