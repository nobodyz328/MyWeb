package com.myweb.website_core.infrastructure.mapper;

import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.entity.AuditLog;
import com.myweb.website_core.infrastructure.persistence.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志Repository测试类
 * 
 * 测试AuditLogRepository的基本功能，包括：
 * - 基础CRUD操作
 * - 查询方法的正确性
 * - 分页和排序功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@DataJpaTest
class AuditLogRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Test
    void testSaveAndFindById() {
        // 创建审计日志
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(1L);
        auditLog.setUsername("testuser");
        auditLog.setOperation(AuditOperation.USER_LOGIN_SUCCESS);
        auditLog.setIpAddress("192.168.1.1");
        auditLog.setResult("SUCCESS");
        auditLog.setTimestamp(LocalDateTime.now());
        
        // 保存
        AuditLog saved = auditLogRepository.save(auditLog);
        entityManager.flush();
        
        // 验证保存成功
        assertNotNull(saved.getId());
        
        // 查询验证
        AuditLog found = auditLogRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
        assertEquals(AuditOperation.USER_LOGIN_SUCCESS, found.getOperation());
        assertEquals("SUCCESS", found.getResult());
    }
    
    @Test
    void testFindByUserId() {
        // 创建测试数据
        AuditLog auditLog1 = createTestAuditLog(1L, "user1", AuditOperation.USER_LOGIN_SUCCESS);
        AuditLog auditLog2 = createTestAuditLog(1L, "user1", AuditOperation.POST_CREATE);
        AuditLog auditLog3 = createTestAuditLog(2L, "user2", AuditOperation.USER_LOGIN_SUCCESS);
        
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);
        auditLogRepository.save(auditLog3);
        entityManager.flush();
        
        // 查询用户1的日志
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> result = auditLogRepository.findByUserId(1L, pageable);
        
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(log -> log.getUserId().equals(1L)));
    }
    
    @Test
    void testFindByOperation() {
        // 创建测试数据
        AuditLog auditLog1 = createTestAuditLog(1L, "user1", AuditOperation.USER_LOGIN_SUCCESS);
        AuditLog auditLog2 = createTestAuditLog(2L, "user2", AuditOperation.USER_LOGIN_SUCCESS);
        AuditLog auditLog3 = createTestAuditLog(3L, "user3", AuditOperation.POST_CREATE);
        
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);
        auditLogRepository.save(auditLog3);
        entityManager.flush();
        
        // 查询登录操作
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> result = auditLogRepository.findByOperation(AuditOperation.USER_LOGIN_SUCCESS, pageable);
        
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(log -> log.getOperation() == AuditOperation.USER_LOGIN_SUCCESS));
    }
    
    @Test
    void testFindByTimestampBetween() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);
        
        // 创建测试数据
        AuditLog auditLog1 = createTestAuditLog(1L, "user1", AuditOperation.USER_LOGIN_SUCCESS);
        auditLog1.setTimestamp(twoHoursAgo);
        
        AuditLog auditLog2 = createTestAuditLog(2L, "user2", AuditOperation.POST_CREATE);
        auditLog2.setTimestamp(oneHourAgo);
        
        AuditLog auditLog3 = createTestAuditLog(3L, "user3", AuditOperation.COMMENT_CREATE);
        auditLog3.setTimestamp(now);
        
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);
        auditLogRepository.save(auditLog3);
        entityManager.flush();
        
        // 查询最近1.5小时的日志
        LocalDateTime startTime = now.minusMinutes(90);
        LocalDateTime endTime = now.plusMinutes(10);
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> result = auditLogRepository.findByTimestampBetween(startTime, endTime, pageable);
        
        assertEquals(2, result.getTotalElements());
    }
    
    @Test
    void testCountByTimestampBetween() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // 创建测试数据
        AuditLog auditLog1 = createTestAuditLog(1L, "user1", AuditOperation.USER_LOGIN_SUCCESS);
        auditLog1.setTimestamp(oneHourAgo);
        
        AuditLog auditLog2 = createTestAuditLog(2L, "user2", AuditOperation.POST_CREATE);
        auditLog2.setTimestamp(now);
        
        auditLogRepository.save(auditLog1);
        auditLogRepository.save(auditLog2);
        entityManager.flush();
        
        // 统计最近2小时的日志数量
        long count = auditLogRepository.countByTimestampBetween(now.minusHours(2), now.plusMinutes(10));
        
        assertEquals(2, count);
    }
    
    @Test
    void testExistsByUserIdAndTimestampBetween() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // 创建测试数据
        AuditLog auditLog = createTestAuditLog(1L, "user1", AuditOperation.USER_LOGIN_SUCCESS);
        auditLog.setTimestamp(oneHourAgo);
        
        auditLogRepository.save(auditLog);
        entityManager.flush();
        
        // 检查用户1在最近2小时内是否有操作记录
        boolean exists = auditLogRepository.existsByUserIdAndTimestampBetween(
            1L, now.minusHours(2), now.plusMinutes(10));
        
        assertTrue(exists);
        
        // 检查用户2在最近2小时内是否有操作记录
        boolean notExists = auditLogRepository.existsByUserIdAndTimestampBetween(
            2L, now.minusHours(2), now.plusMinutes(10));
        
        assertFalse(notExists);
    }
    
    /**
     * 创建测试用的审计日志
     */
    private AuditLog createTestAuditLog(Long userId, String username, AuditOperation operation) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setOperation(operation);
        auditLog.setIpAddress("192.168.1.1");
        auditLog.setResult("SUCCESS");
        auditLog.setTimestamp(LocalDateTime.now());
        return auditLog;
    }
}