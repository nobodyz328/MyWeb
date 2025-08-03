package com.myweb.website_core.domain.entity;

import com.myweb.website_core.common.enums.UserRole;
import com.myweb.website_core.domain.business.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户安全功能测试类
 * 
 * 测试User实体类的安全相关功能，确保符合GB/T 22239-2019二级等保要求
 */
class UserSecurityTest {
    
    private User user;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setRole(UserRole.USER);
    }
    
    @Test
    void testAccountLockingMechanism() {
        // 初始状态：账户未锁定
        assertFalse(user.isAccountLocked());
        assertEquals(0, user.getLoginAttempts());
        
        // 模拟4次登录失败
        for (int i = 0; i < 4; i++) {
            user.incrementLoginAttempts();
        }
        assertEquals(4, user.getLoginAttempts());
        assertFalse(user.isAccountLocked()); // 还未达到锁定阈值
        
        // 第5次失败，触发账户锁定
        user.incrementLoginAttempts();
        assertEquals(5, user.getLoginAttempts());
        assertTrue(user.isAccountLocked());
        assertNotNull(user.getAccountLockedUntil());
        assertTrue(user.getAccountLockedUntil().isAfter(LocalDateTime.now()));
    }
    
    @Test
    void testLoginAttemptsReset() {
        // 设置失败次数
        user.setLoginAttempts(3);
        user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
        
        // 重置登录失败次数
        user.resetLoginAttempts();
        
        assertEquals(0, user.getLoginAttempts());
        assertNull(user.getAccountLockedUntil());
        assertFalse(user.isAccountLocked());
    }
    
    @Test
    void testLastLoginInfoUpdate() {
        LocalDateTime loginTime = LocalDateTime.now();
        String loginIp = "192.168.1.100";
        
        user.updateLastLoginInfo(loginTime, loginIp);
        
        assertEquals(loginTime, user.getLastLoginTime());
        assertEquals(loginIp, user.getLastLoginIp());
    }
    
    @Test
    void testRoleBasedPermissions() {
        // 测试普通用户权限
        user.setRole(UserRole.USER);
        assertTrue(user.hasRole(UserRole.USER));
        assertFalse(user.hasManagementPermission());
        assertFalse(user.hasSystemAdminPermission());
        
        // 测试版主权限
        user.setRole(UserRole.MODERATOR);
        assertTrue(user.hasRole(UserRole.MODERATOR));
        assertTrue(user.hasManagementPermission());
        assertFalse(user.hasSystemAdminPermission());
        
        // 测试管理员权限
        user.setRole(UserRole.ADMIN);
        assertTrue(user.hasRole(UserRole.ADMIN));
        assertTrue(user.hasManagementPermission());
        assertTrue(user.hasSystemAdminPermission());
    }
    
    @Test
    void testTwoFactorAuthRequirement() {
        // 普通用户不需要二次验证
        user.setRole(UserRole.USER);
        user.setTotpEnabled(false);
        assertFalse(user.requiresTwoFactorAuth());
        
        // 管理员启用TOTP后需要二次验证
        user.setRole(UserRole.ADMIN);
        user.setTotpEnabled(true);
        assertTrue(user.requiresTwoFactorAuth());
        
        // 管理员未启用TOTP不需要二次验证
        user.setTotpEnabled(false);
        assertFalse(user.requiresTwoFactorAuth());
    }
    
    @Test
    void testDefaultValues() {
        User newUser = new User();
        
        // 验证默认值
        assertFalse(newUser.getEmailVerified());
        assertEquals(0, newUser.getLoginAttempts());
        assertFalse(newUser.getTotpEnabled());
        assertEquals(UserRole.USER, newUser.getRole());
        assertEquals(0, newUser.getLikedCount());
    }
    
    @Test
    void testJpaLifecycleCallbacks() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");
        
        // 使用反射调用protected方法
        try {
            java.lang.reflect.Method onCreateMethod = User.class.getDeclaredMethod("onCreate");
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(newUser);
        } catch (Exception e) {
            fail("Failed to invoke onCreate method: " + e.getMessage());
        }
        
        assertNotNull(newUser.getCreatedAt());
        assertNotNull(newUser.getUpdatedAt());
        assertEquals(newUser.getCreatedAt(), newUser.getUpdatedAt());
        
        // 等待一毫秒确保时间不同
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 使用反射调用protected方法
        LocalDateTime originalUpdatedAt = newUser.getUpdatedAt();
        try {
            java.lang.reflect.Method onUpdateMethod = User.class.getDeclaredMethod("onUpdate");
            onUpdateMethod.setAccessible(true);
            onUpdateMethod.invoke(newUser);
        } catch (Exception e) {
            fail("Failed to invoke onUpdate method: " + e.getMessage());
        }
        
        assertTrue(newUser.getUpdatedAt().isAfter(originalUpdatedAt));
    }
}