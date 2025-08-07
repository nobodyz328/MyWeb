package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.SqlSecurityAuditAdapter;
import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import com.myweb.website_core.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SQL注入防护服务测试类
 * 
 * 测试SQL注入检测和防护功能的各种场景
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SQL注入防护服务测试")
class SqlInjectionProtectionServiceTest {
    
    @Mock
    private SqlSecurityAuditAdapter auditLogService;
    
    @InjectMocks
    private SqlInjectionProtectionService sqlInjectionProtectionService;
    
    @BeforeEach
    void setUp() {
        // 初始化测试环境
    }
    
    @Nested
    @DisplayName("SQL注入检测测试")
    class SqlInjectionDetectionTest {
        
        @Test
        @DisplayName("应该检测到Union注入攻击")
        void shouldDetectUnionInjection() {
            // 测试各种Union注入变体
            String[] unionAttacks = {
                "' UNION SELECT * FROM users --",
                "1' UNION ALL SELECT username, password FROM users --",
                "test' union select 1,2,3 --",
                "' OR 1=1 UNION SELECT NULL, username, password FROM users --"
            };
            
            for (String attack : unionAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "SEARCH"),
                    "应该检测到Union注入: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到条件注入攻击")
        void shouldDetectConditionalInjection() {
            String[] conditionalAttacks = {
                "' OR 1=1 --",
                "' OR '1'='1",
                "admin' AND 1=1 --",
                "' OR 'a'='a",
                "1' OR 1=1 #",
                "test' AND 1=2 UNION SELECT * FROM users --"
            };
            
            for (String attack : conditionalAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "LOGIN"),
                    "应该检测到条件注入: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到注释注入攻击")
        void shouldDetectCommentInjection() {
            String[] commentAttacks = {
                "admin' --",
                "test'; --",
                "user'/*comment*/",
                "admin'#",
                "' OR 1=1 /* bypass */"
            };
            
            for (String attack : commentAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "USERNAME"),
                    "应该检测到注释注入: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到堆叠查询攻击")
        void shouldDetectStackedQueries() {
            String[] stackedAttacks = {
                "'; DROP TABLE users; --",
                "test'; INSERT INTO users VALUES ('hacker', 'pass'); --",
                "1; UPDATE users SET password='hacked' WHERE id=1; --",
                "'; DELETE FROM posts; --"
            };
            
            for (String attack : stackedAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "PARAMETER"),
                    "应该检测到堆叠查询: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到函数注入攻击")
        void shouldDetectFunctionInjection() {
            String[] functionAttacks = {
                "'; SELECT SLEEP(5); --",
                "1' AND BENCHMARK(1000000, MD5(1)) --",
                "'; WAITFOR DELAY '00:00:05'; --",
                "' OR pg_sleep(5) --",
                "'; SELECT load_file('/etc/passwd'); --"
            };
            
            for (String attack : functionAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "SEARCH"),
                    "应该检测到函数注入: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到系统信息获取攻击")
        void shouldDetectSystemInfoExtraction() {
            String[] systemAttacks = {
                "' UNION SELECT @@version --",
                "1' AND user() LIKE 'root%' --",
                "'; SELECT database(); --",
                "' OR @@user='root' --",
                "1' UNION SELECT current_user --"
            };
            
            for (String attack : systemAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "FILTER"),
                    "应该检测到系统信息获取: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到盲注攻击")
        void shouldDetectBlindInjection() {
            String[] blindAttacks = {
                "1' AND ASCII(SUBSTRING((SELECT password FROM users WHERE id=1),1,1))>65 --",
                "' AND LENGTH(database())>5 --",
                "1' AND (SELECT COUNT(*) FROM users)>0 --",
                "' AND SUBSTRING(user(),1,1)='r' --"
            };
            
            for (String attack : blindAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "SEARCH"),
                    "应该检测到盲注攻击: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到时间盲注攻击")
        void shouldDetectTimeBasedBlindInjection() {
            String[] timeAttacks = {
                "1' AND IF(1=1, SLEEP(5), 0) --",
                "' OR IF((SELECT COUNT(*) FROM users)>0, BENCHMARK(1000000,MD5(1)), 0) --",
                "1'; IF(1=1) WAITFOR DELAY '00:00:05' --"
            };
            
            for (String attack : timeAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "PARAMETER"),
                    "应该检测到时间盲注: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到错误注入攻击")
        void shouldDetectErrorBasedInjection() {
            String[] errorAttacks = {
                "' AND EXTRACTVALUE(1, CONCAT(0x7e, (SELECT user()), 0x7e)) --",
                "1' AND UPDATEXML(1, CONCAT(0x7e, (SELECT version()), 0x7e), 1) --",
                "' OR EXP(~(SELECT * FROM (SELECT COUNT(*),CONCAT(version(),FLOOR(RAND(0)*2))x FROM information_schema.tables GROUP BY x)a)) --"
            };
            
            for (String attack : errorAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "INPUT"),
                    "应该检测到错误注入: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该检测到十六进制编码攻击")
        void shouldDetectHexEncodedAttacks() {
            String[] hexAttacks = {
                "0x61646D696E", // 'admin' in hex
                "' OR username=0x61646D696E --",
                "1' UNION SELECT 0x48656C6C6F --" // 'Hello' in hex
            };
            
            for (String attack : hexAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "SEARCH"),
                    "应该检测到十六进制编码攻击: " + attack);
            }
        }
        
        @Test
        @DisplayName("不应该将正常输入误判为SQL注入")
        void shouldNotDetectNormalInput() {
            String[] normalInputs = {
                "正常的搜索关键词",
                "user123",
                "test@example.com",
                "Hello World!",
                "这是一个正常的帖子标题",
                "包含特殊字符的内容：@#$%^&*()",
                "normal password123",
                "文件名.jpg",
                "https://example.com/path?param=value"
            };
            
            for (String input : normalInputs) {
                assertFalse(sqlInjectionProtectionService.detectSqlInjection(input, "NORMAL"),
                    "不应该将正常输入误判为SQL注入: " + input);
            }
        }
        
        @Test
        @DisplayName("应该正确处理空值和空字符串")
        void shouldHandleNullAndEmptyInput() {
            assertFalse(sqlInjectionProtectionService.detectSqlInjection(null, "TEST"));
            assertFalse(sqlInjectionProtectionService.detectSqlInjection("", "TEST"));
            assertFalse(sqlInjectionProtectionService.detectSqlInjection("   ", "TEST"));
        }
        
        @Test
        @DisplayName("应该在安全上下文中允许某些关键词")
        void shouldAllowSafeKeywordsInContext() {
            // 在排序上下文中，ORDER BY应该是安全的
            assertFalse(sqlInjectionProtectionService.detectSqlInjection("order", "SORT"));
            assertFalse(sqlInjectionProtectionService.detectSqlInjection("by", "ORDER"));
            
            // 但在其他上下文中应该被检测
            assertTrue(sqlInjectionProtectionService.detectSqlInjection("' ORDER BY 1 --", "SEARCH"));
        }
    }
    
    @Nested
    @DisplayName("输入验证和清理测试")
    class InputValidationTest {
        
        @Test
        @DisplayName("应该阻止SQL注入输入并抛出异常")
        void shouldBlockSqlInjectionAndThrowException() {
            String maliciousInput = "' OR 1=1 --";
            
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                sqlInjectionProtectionService.validateAndSanitizeInput(
                    maliciousInput, "SEARCH", "keyword"
                );
            });
            
            assertEquals("keyword包含潜在的SQL注入代码", exception.getMessage());
            assertEquals("keyword", exception);
            assertEquals("SQL_INJECTION_DETECTED", exception);
            
            // 验证审计日志被记录
            verify(auditLogService).logSecurityEvent(
                eq("SQL_INJECTION_BLOCKED"),
                contains("SQL injection attempt blocked"),
                eq(maliciousInput),
                eq("HIGH")
            );
            
            verify(auditLogService).logSqlInjectionCheck(
                eq(maliciousInput),
                eq("SEARCH"),
                eq("keyword"),
                eq("BLOCKED")
            );
        }
        
        @Test
        @DisplayName("应该允许安全输入通过验证")
        void shouldAllowSafeInput() {
            String safeInput = "正常的搜索关键词";
            
            assertDoesNotThrow(() -> {
                sqlInjectionProtectionService.validateAndSanitizeInput(
                    safeInput, "SEARCH", "keyword"
                );
            });
            
            // 验证没有记录安全事件，但记录了通过的检查
            verify(auditLogService, never()).logSecurityEvent(anyString(), anyString(), anyString(), anyString());
            verify(auditLogService).logSqlInjectionCheck(
                eq(safeInput),
                eq("SEARCH"),
                eq("keyword"),
                eq("PASSED")
            );
        }
    }
    
    @Nested
    @DisplayName("动态SQL构建测试")
    class DynamicSqlBuildingTest {
        
        @Test
        @DisplayName("应该构建安全的动态查询")
        void shouldBuildSafeDynamicQuery() {
            String baseQuery = "SELECT * FROM users WHERE active = true";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("username", "testuser");
            parameters.put("email", "test@example.com");
            
            String result = sqlInjectionProtectionService.buildSafeDynamicQuery(baseQuery, parameters);
            
            assertEquals(baseQuery, result);
            
            // 验证审计日志被记录
            verify(auditLogService).logDynamicSqlBuild(
                eq(baseQuery),
                anyString(),
                eq("SUCCESS")
            );
        }
        
        @Test
        @DisplayName("应该拒绝包含SQL注入的基础查询")
        void shouldRejectMaliciousBaseQuery() {
            String maliciousQuery = "SELECT * FROM users WHERE id = 1; DROP TABLE users; --";
            Map<String, Object> parameters = new HashMap<>();
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                sqlInjectionProtectionService.buildSafeDynamicQuery(maliciousQuery, parameters);
            });
            
            assertEquals("Base query contains potential SQL injection", exception.getMessage());
        }
        
        @Test
        @DisplayName("应该拒绝包含SQL注入的参数")
        void shouldRejectMaliciousParameters() {
            String baseQuery = "SELECT * FROM users WHERE username = ?";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("username", "admin' OR 1=1 --");
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                sqlInjectionProtectionService.buildSafeDynamicQuery(baseQuery, parameters);
            });
            
            assertTrue(exception.getMessage().contains("Parameter 'username' contains potential SQL injection"));
        }
    }
    
    @Nested
    @DisplayName("MyBatis语句验证测试")
    class MybatisStatementValidationTest {
        
        @Test
        @DisplayName("应该验证参数化查询为安全")
        void shouldValidateParameterizedQuery() {
            String parameterizedSql = "SELECT * FROM users WHERE username = #{username} AND email = #{email}";
            
            boolean result = sqlInjectionProtectionService.validateMybatisStatement(parameterizedSql, "UserMapper.selectUser");
            
            assertTrue(result);
        }
        
        @Test
        @DisplayName("应该拒绝非参数化查询")
        void shouldRejectNonParameterizedQuery() {
            String nonParameterizedSql = "SELECT * FROM users WHERE username = '${username}'";
            
            boolean result = sqlInjectionProtectionService.validateMybatisStatement(nonParameterizedSql, "UserMapper.selectUser");
            
            assertFalse(result);
            
            // 验证MyBatis验证被记录
            verify(auditLogService).logMybatisValidation(
                eq("UserMapper.selectUser"),
                eq(false),
                contains("Non-parameterized query detected")
            );
        }
        
        @Test
        @DisplayName("应该拒绝包含危险动态SQL的语句")
        void shouldRejectDangerousDynamicSql() {
            String dangerousSql = "SELECT * FROM users WHERE 1=1 UNION SELECT * FROM admin_users";
            
            boolean result = sqlInjectionProtectionService.validateMybatisStatement(dangerousSql, "UserMapper.dangerousQuery");
            
            assertFalse(result);
            
            // 验证MyBatis验证被记录
            verify(auditLogService).logMybatisValidation(
                eq("UserMapper.dangerousQuery"),
                eq(false),
                contains("Dangerous dynamic SQL patterns detected")
            );
        }
    }
    
    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTest {
        
        @Test
        @DisplayName("应该处理大小写混合的SQL注入")
        void shouldDetectMixedCaseInjection() {
            String[] mixedCaseAttacks = {
                "' UnIoN SeLeCt * FrOm users --",
                "' Or 1=1 --",
                "'; DrOp TaBlE users; --"
            };
            
            for (String attack : mixedCaseAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "TEST"),
                    "应该检测到大小写混合的SQL注入: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该处理包含空格和制表符的SQL注入")
        void shouldDetectWhitespaceVariations() {
            String[] whitespaceAttacks = {
                "'   UNION   SELECT   *   FROM   users   --",
                "'\tOR\t1=1\t--",
                "'\nUNION\nSELECT\n*\nFROM\nusers\n--"
            };
            
            for (String attack : whitespaceAttacks) {
                assertTrue(sqlInjectionProtectionService.detectSqlInjection(attack, "TEST"),
                    "应该检测到包含空格变体的SQL注入: " + attack);
            }
        }
        
        @Test
        @DisplayName("应该处理长字符串中的SQL注入")
        void shouldDetectInjectionInLongStrings() {
            String longString = "这是一个很长的字符串，包含了很多正常的内容，但是在中间隐藏了 ' OR 1=1 -- 这样的SQL注入攻击代码，应该被检测出来";
            
            assertTrue(sqlInjectionProtectionService.detectSqlInjection(longString, "CONTENT"));
        }
        
        @Test
        @DisplayName("应该处理Unicode字符")
        void shouldHandleUnicodeCharacters() {
            String unicodeInput = "用户名包含中文字符和emoji😀";
            
            assertFalse(sqlInjectionProtectionService.detectSqlInjection(unicodeInput, "USERNAME"));
        }
    }
}