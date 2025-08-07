package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.application.service.security.audit.SqlSecurityAuditAdapter;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SQL审计拦截器集成测试
 * 
 * 测试MyBatis拦截器在实际环境中的工作情况
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SQL审计拦截器集成测试")
class SqlAuditInterceptorIntegrationTest {
    
    @Autowired
    private UserMapper userMapper;
    
    @MockBean
    private SqlInjectionProtectionService sqlInjectionProtectionService;
    
    @MockBean
    private SqlSecurityAuditAdapter auditLogService;
    
    @Test
    @DisplayName("应该拦截并审计SELECT查询")
    void shouldInterceptAndAuditSelectQuery() {
        // 模拟SQL安全验证通过
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(true);
        
        // 执行查询
        User user = userMapper.selectUserById(1L);
        
        // 验证SQL安全检查被调用
        verify(sqlInjectionProtectionService).validateMybatisStatement(anyString(), contains("selectUserById"));
        
        // 验证审计日志被记录
        verify(auditLogService).logDatabaseOperation(
            eq("SELECT"),
            contains("selectUserById"),
            anyString(),
            eq("SUCCESS")
        );
    }
    
    @Test
    @DisplayName("应该拦截并审计INSERT操作")
    void shouldInterceptAndAuditInsertOperation() {
        // 模拟SQL安全验证通过
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(true);
        
        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        
        // 执行插入
        int result = userMapper.insertUser(testUser);
        
        // 验证插入成功
        assertTrue(result > 0);
        
        // 验证SQL安全检查被调用
        verify(sqlInjectionProtectionService).validateMybatisStatement(anyString(), contains("insertUser"));
        
        // 验证审计日志被记录
        verify(auditLogService).logDatabaseOperation(
            eq("INSERT"),
            contains("insertUser"),
            anyString(),
            eq("SUCCESS")
        );
    }
    
    @Test
    @DisplayName("应该拦截并审计UPDATE操作")
    void shouldInterceptAndAuditUpdateOperation() {
        // 模拟SQL安全验证通过
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(true);
        
        // 创建并插入测试用户
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        userMapper.insertUser(testUser);
        
        // 更新用户信息
        testUser.setEmail("updated@example.com");
        int result = userMapper.updateUser(testUser);
        
        // 验证更新成功
        assertTrue(result > 0);
        
        // 验证SQL安全检查被调用（包括INSERT和UPDATE）
        verify(sqlInjectionProtectionService, atLeast(2))
            .validateMybatisStatement(anyString(), anyString());
        
        // 验证审计日志被记录（包括INSERT和UPDATE）
        verify(auditLogService, atLeast(2))
            .logDatabaseOperation(anyString(), anyString(), anyString(), eq("SUCCESS"));
    }
    
    @Test
    @DisplayName("应该拦截并审计DELETE操作")
    void shouldInterceptAndAuditDeleteOperation() {
        // 模拟SQL安全验证通过
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(true);
        
        // 创建并插入测试用户
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        userMapper.insertUser(testUser);
        
        // 删除用户
        int result = userMapper.deleteUser(testUser.getId());
        
        // 验证删除成功
        assertTrue(result > 0);
        
        // 验证SQL安全检查被调用（包括INSERT和DELETE）
        verify(sqlInjectionProtectionService, atLeast(2))
            .validateMybatisStatement(anyString(), anyString());
        
        // 验证审计日志被记录（包括INSERT和DELETE）
        verify(auditLogService, atLeast(2))
            .logDatabaseOperation(anyString(), anyString(), anyString(), eq("SUCCESS"));
    }
    
    @Test
    @DisplayName("应该阻止不安全的SQL语句执行")
    void shouldBlockUnsafeSqlExecution() {
        // 模拟SQL安全验证失败
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(false);
        
        // 尝试执行查询应该抛出安全异常
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            userMapper.selectUserById(1L);
        });
        
        assertEquals("SQL security validation failed", exception.getMessage());
        
        // 验证安全事件被记录
        verify(auditLogService).logSecurityEvent(
            eq("SQL_SECURITY_VIOLATION"),
            contains("SQL security validation failed"),
            anyString(),
            eq("HIGH")
        );
    }
    
    @Test
    @DisplayName("应该记录SQL执行异常")
    void shouldLogSqlExecutionException() {
        // 模拟SQL安全验证通过
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(true);
        
        // 尝试查询不存在的用户（这里假设会抛出异常，实际可能返回null）
        try {
            userMapper.selectUserById(-1L); // 使用无效ID
        } catch (Exception e) {
            // 忽略异常，我们主要测试审计日志
        }
        
        // 验证SQL安全检查被调用
        verify(sqlInjectionProtectionService).validateMybatisStatement(anyString(), anyString());
        
        // 验证审计日志被记录（成功或失败都会记录）
        verify(auditLogService, atLeastOnce())
            .logDatabaseOperation(anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("应该监控慢查询")
    void shouldMonitorSlowQueries() {
        // 模拟SQL安全验证通过
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(true);
        
        // 执行一个可能较慢的查询（搜索所有用户）
        userMapper.selectAllUsers();
        
        // 验证SQL安全检查被调用
        verify(sqlInjectionProtectionService).validateMybatisStatement(anyString(), anyString());
        
        // 验证审计日志被记录
        verify(auditLogService).logDatabaseOperation(
            eq("SELECT"),
            contains("selectAllUsers"),
            anyString(),
            eq("SUCCESS")
        );
        
        // 注意：慢查询检测需要查询实际执行时间超过阈值，这里只是验证基本功能
    }
    
    @Test
    @DisplayName("应该处理复杂查询的审计")
    void shouldHandleComplexQueryAudit() {
        // 模拟SQL安全验证通过
        when(sqlInjectionProtectionService.validateMybatisStatement(anyString(), anyString()))
            .thenReturn(true);
        
        // 执行复杂的搜索查询
        userMapper.searchUsersWithPagination("test", "RELEVANCE", 0, 10);
        
        // 验证SQL安全检查被调用
        verify(sqlInjectionProtectionService).validateMybatisStatement(anyString(), contains("searchUsersWithPagination"));
        
        // 验证审计日志被记录
        verify(auditLogService).logDatabaseOperation(
            eq("SELECT"),
            contains("searchUsersWithPagination"),
            anyString(),
            eq("SUCCESS")
        );
    }
}