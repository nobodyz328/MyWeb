package com.myweb.website_core.common.util;

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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 安全SQL构建器测试类
 * 
 * 测试安全的动态SQL构建功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("安全SQL构建器测试")
class SafeSqlBuilderTest {
    
    @Mock
    private SqlInjectionProtectionService sqlInjectionProtectionService;
    
    @InjectMocks
    private SafeSqlBuilder safeSqlBuilder;
    
    @BeforeEach
    void setUp() {
        // 模拟SQL注入检测服务的行为
        doNothing().when(sqlInjectionProtectionService)
            .validateAndSanitizeInput(anyString(), anyString(), anyString());
    }
    
    @Nested
    @DisplayName("WHERE子句构建测试")
    class WhereClauseTest {
        
        @Test
        @DisplayName("应该构建安全的WHERE子句")
        void shouldBuildSafeWhereClause() {
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("username", "testuser");
            conditions.put("email", "test@example.com");
            conditions.put("active", true);
            
            String result = safeSqlBuilder.buildSafeWhereClause(conditions);
            
            assertTrue(result.contains("WHERE"));
            assertTrue(result.contains("username = #{username}"));
            assertTrue(result.contains("email = #{email}"));
            assertTrue(result.contains("active = #{active}"));
            assertTrue(result.contains("AND"));
        }
        
        @Test
        @DisplayName("应该处理LIKE查询")
        void shouldHandleLikeQueries() {
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("title", "%search%");
            
            String result = safeSqlBuilder.buildSafeWhereClause(conditions);
            
            assertTrue(result.contains("title LIKE #{title}"));
        }
        
        @Test
        @DisplayName("应该处理NULL值")
        void shouldHandleNullValues() {
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("deleted_at", null);
            
            String result = safeSqlBuilder.buildSafeWhereClause(conditions);
            
            assertTrue(result.contains("deleted_at IS NULL"));
        }
        
        @Test
        @DisplayName("应该返回空字符串当条件为空时")
        void shouldReturnEmptyStringForEmptyConditions() {
            String result = safeSqlBuilder.buildSafeWhereClause(new HashMap<>());
            assertEquals("", result);
            
            result = safeSqlBuilder.buildSafeWhereClause(null);
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("应该拒绝不安全的字段名")
        void shouldRejectUnsafeFieldNames() {
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("'; DROP TABLE users; --", "value");
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeWhereClause(conditions);
            });
            
            assertTrue(exception.getMessage().contains("Unsafe field name"));
        }
        
        @Test
        @DisplayName("应该验证字符串值的安全性")
        void shouldValidateStringValues() {
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("username", "malicious' OR 1=1 --");
            
            // 模拟SQL注入检测抛出异常
            doThrow(new ValidationException("SQL injection detected", "username", "SQL_INJECTION"))
                .when(sqlInjectionProtectionService)
                .validateAndSanitizeInput(eq("malicious' OR 1=1 --"), eq("WHERE_CONDITION"), eq("username"));
            
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                safeSqlBuilder.buildSafeWhereClause(conditions);
            });
            
            assertEquals("SQL injection detected", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("ORDER BY子句构建测试")
    class OrderByClauseTest {
        
        @Test
        @DisplayName("应该构建安全的ORDER BY子句")
        void shouldBuildSafeOrderByClause() {
            String result = safeSqlBuilder.buildSafeOrderByClause("users", "username", "ASC");
            
            assertEquals(" ORDER BY username ASC", result);
        }
        
        @Test
        @DisplayName("应该处理默认排序方向")
        void shouldHandleDefaultSortDirection() {
            String result = safeSqlBuilder.buildSafeOrderByClause("users", "id", null);
            
            assertEquals(" ORDER BY id ASC", result);
        }
        
        @Test
        @DisplayName("应该处理大小写不敏感的排序方向")
        void shouldHandleCaseInsensitiveSortDirection() {
            String result = safeSqlBuilder.buildSafeOrderByClause("users", "created_at", "desc");
            
            assertEquals(" ORDER BY created_at DESC", result);
        }
        
        @Test
        @DisplayName("应该返回空字符串当排序字段为空时")
        void shouldReturnEmptyStringForEmptySortField() {
            String result = safeSqlBuilder.buildSafeOrderByClause("users", null, "ASC");
            assertEquals("", result);
            
            result = safeSqlBuilder.buildSafeOrderByClause("users", "", "ASC");
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("应该拒绝不在白名单中的排序字段")
        void shouldRejectUnallowedSortFields() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeOrderByClause("users", "password", "ASC");
            });
            
            assertTrue(exception.getMessage().contains("Sort field 'password' not allowed"));
        }
        
        @Test
        @DisplayName("应该拒绝无效的排序方向")
        void shouldRejectInvalidSortDirection() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeOrderByClause("users", "username", "INVALID");
            });
            
            assertTrue(exception.getMessage().contains("Invalid sort direction"));
        }
        
        @Test
        @DisplayName("应该拒绝不安全的表名")
        void shouldRejectUnsafeTableName() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeOrderByClause("users; DROP TABLE posts; --", "id", "ASC");
            });
            
            assertTrue(exception.getMessage().contains("Unsafe table name"));
        }
    }
    
    @Nested
    @DisplayName("LIMIT子句构建测试")
    class LimitClauseTest {
        
        @Test
        @DisplayName("应该构建安全的LIMIT子句")
        void shouldBuildSafeLimitClause() {
            String result = safeSqlBuilder.buildSafeLimitClause(10, 20);
            
            assertEquals(" LIMIT 10 OFFSET 20", result);
        }
        
        @Test
        @DisplayName("应该处理只有LIMIT的情况")
        void shouldHandleLimitOnly() {
            String result = safeSqlBuilder.buildSafeLimitClause(10, null);
            
            assertEquals(" LIMIT 10", result);
        }
        
        @Test
        @DisplayName("应该处理只有OFFSET的情况")
        void shouldHandleOffsetOnly() {
            String result = safeSqlBuilder.buildSafeLimitClause(null, 20);
            
            assertEquals(" OFFSET 20", result);
        }
        
        @Test
        @DisplayName("应该返回空字符串当参数都为空时")
        void shouldReturnEmptyStringForNullParameters() {
            String result = safeSqlBuilder.buildSafeLimitClause(null, null);
            
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("应该拒绝负数的LIMIT值")
        void shouldRejectNegativeLimit() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeLimitClause(-1, 0);
            });
            
            assertTrue(exception.getMessage().contains("Invalid limit value"));
        }
        
        @Test
        @DisplayName("应该拒绝过大的LIMIT值")
        void shouldRejectTooLargeLimit() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeLimitClause(1001, 0);
            });
            
            assertTrue(exception.getMessage().contains("Invalid limit value"));
        }
        
        @Test
        @DisplayName("应该拒绝负数的OFFSET值")
        void shouldRejectNegativeOffset() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeLimitClause(10, -1);
            });
            
            assertTrue(exception.getMessage().contains("Invalid offset value"));
        }
    }
    
    @Nested
    @DisplayName("搜索条件构建测试")
    class SearchConditionTest {
        
        @Test
        @DisplayName("应该构建安全的搜索条件")
        void shouldBuildSafeSearchCondition() {
            List<String> searchFields = List.of("username", "email");
            String keyword = "test";
            
            String result = safeSqlBuilder.buildSafeSearchCondition(searchFields, keyword, "users");
            
            assertTrue(result.contains("username LIKE CONCAT('%', #{keyword}, '%')"));
            assertTrue(result.contains("email LIKE CONCAT('%', #{keyword}, '%')"));
            assertTrue(result.contains("OR"));
            assertTrue(result.startsWith("("));
            assertTrue(result.endsWith(")"));
        }
        
        @Test
        @DisplayName("应该返回空字符串当参数无效时")
        void shouldReturnEmptyStringForInvalidParameters() {
            String result = safeSqlBuilder.buildSafeSearchCondition(null, "test", "users");
            assertEquals("", result);
            
            result = safeSqlBuilder.buildSafeSearchCondition(List.of(), "test", "users");
            assertEquals("", result);
            
            result = safeSqlBuilder.buildSafeSearchCondition(List.of("username"), null, "users");
            assertEquals("", result);
            
            result = safeSqlBuilder.buildSafeSearchCondition(List.of("username"), "", "users");
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("应该验证搜索关键词的安全性")
        void shouldValidateSearchKeyword() {
            List<String> searchFields = List.of("username");
            String maliciousKeyword = "'; DROP TABLE users; --";
            
            // 模拟SQL注入检测抛出异常
            doThrow(new ValidationException("SQL injection detected", "keyword", "SQL_INJECTION"))
                .when(sqlInjectionProtectionService)
                .validateAndSanitizeInput(eq(maliciousKeyword), eq("SEARCH"), eq("keyword"));
            
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                safeSqlBuilder.buildSafeSearchCondition(searchFields, maliciousKeyword, "users");
            });
            
            assertEquals("SQL injection detected", exception.getMessage());
        }
        
        @Test
        @DisplayName("应该拒绝不在白名单中的搜索字段")
        void shouldRejectUnallowedSearchFields() {
            List<String> searchFields = List.of("password");
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeSearchCondition(searchFields, "test", "users");
            });
            
            assertTrue(exception.getMessage().contains("Search field 'password' not allowed"));
        }
    }
    
    @Nested
    @DisplayName("JOIN子句构建测试")
    class JoinClauseTest {
        
        @Test
        @DisplayName("应该构建安全的JOIN子句")
        void shouldBuildSafeJoinClause() {
            String result = safeSqlBuilder.buildSafeJoinClause("INNER", "posts", "p", "u.id = p.author_id");
            
            assertEquals(" INNER JOIN posts p ON u.id = p.author_id", result);
        }
        
        @Test
        @DisplayName("应该处理没有别名的JOIN")
        void shouldHandleJoinWithoutAlias() {
            String result = safeSqlBuilder.buildSafeJoinClause("LEFT", "posts", null, "users.id = posts.author_id");
            
            assertEquals(" LEFT JOIN posts ON users.id = posts.author_id", result);
        }
        
        @Test
        @DisplayName("应该处理没有ON条件的JOIN")
        void shouldHandleJoinWithoutCondition() {
            String result = safeSqlBuilder.buildSafeJoinClause("RIGHT", "posts", "p", null);
            
            assertEquals(" RIGHT JOIN posts p", result);
        }
        
        @Test
        @DisplayName("应该拒绝无效的JOIN类型")
        void shouldRejectInvalidJoinType() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeJoinClause("INVALID", "posts", "p", "u.id = p.author_id");
            });
            
            assertTrue(exception.getMessage().contains("Invalid join type"));
        }
        
        @Test
        @DisplayName("应该拒绝不安全的表名")
        void shouldRejectUnsafeTableNameInJoin() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeJoinClause("INNER", "posts; DROP TABLE users; --", "p", "u.id = p.author_id");
            });
            
            assertTrue(exception.getMessage().contains("Unsafe table name"));
        }
        
        @Test
        @DisplayName("应该拒绝不安全的表别名")
        void shouldRejectUnsafeTableAlias() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                safeSqlBuilder.buildSafeJoinClause("INNER", "posts", "p'; DROP TABLE users; --", "u.id = p.author_id");
            });
            
            assertTrue(exception.getMessage().contains("Unsafe table alias"));
        }
    }
    
    @Nested
    @DisplayName("分页查询构建测试")
    class PaginatedQueryTest {
        
        @Test
        @DisplayName("应该构建完整的安全分页查询")
        void shouldBuildCompleteSafePaginatedQuery() {
            String baseQuery = "SELECT * FROM users";
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("active", true);
            
            String result = safeSqlBuilder.buildSafePaginatedQuery(
                baseQuery, conditions, "users", "username", "ASC", 10, 0
            );
            
            assertTrue(result.contains("SELECT * FROM users"));
            assertTrue(result.contains("WHERE"));
            assertTrue(result.contains("active = #{active}"));
            assertTrue(result.contains("ORDER BY username ASC"));
            assertTrue(result.contains("LIMIT 10 OFFSET 0"));
        }
        
        @Test
        @DisplayName("应该处理没有条件的分页查询")
        void shouldHandlePaginatedQueryWithoutConditions() {
            String baseQuery = "SELECT * FROM users";
            
            String result = safeSqlBuilder.buildSafePaginatedQuery(
                baseQuery, null, "users", "id", "DESC", 20, 10
            );
            
            assertTrue(result.contains("SELECT * FROM users"));
            assertFalse(result.contains("WHERE"));
            assertTrue(result.contains("ORDER BY id DESC"));
            assertTrue(result.contains("LIMIT 20 OFFSET 10"));
        }
        
        @Test
        @DisplayName("应该验证基础查询的安全性")
        void shouldValidateBaseQuerySafety() {
            String maliciousQuery = "SELECT * FROM users; DROP TABLE posts; --";
            
            // 模拟SQL注入检测抛出异常
            doThrow(new ValidationException("SQL injection detected", "baseQuery", "SQL_INJECTION"))
                .when(sqlInjectionProtectionService)
                .validateAndSanitizeInput(eq(maliciousQuery), eq("BASE_QUERY"), eq("baseQuery"));
            
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                safeSqlBuilder.buildSafePaginatedQuery(
                    maliciousQuery, null, "users", "id", "ASC", 10, 0
                );
            });
            
            assertEquals("SQL injection detected", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("白名单管理测试")
    class WhitelistManagementTest {
        
        @Test
        @DisplayName("应该能够添加允许的排序字段")
        void shouldAddAllowedSortFields() {
            List<String> newFields = List.of("custom_field1", "custom_field2");
            
            safeSqlBuilder.addAllowedSortFields("custom_table", newFields);
            
            List<String> allowedFields = safeSqlBuilder.getAllowedSortFields("custom_table");
            assertTrue(allowedFields.contains("custom_field1"));
            assertTrue(allowedFields.contains("custom_field2"));
        }
        
        @Test
        @DisplayName("应该返回空列表对于未知表")
        void shouldReturnEmptyListForUnknownTable() {
            List<String> allowedFields = safeSqlBuilder.getAllowedSortFields("unknown_table");
            
            assertTrue(allowedFields.isEmpty());
        }
        
        @Test
        @DisplayName("应该返回预定义表的允许字段")
        void shouldReturnPredefinedAllowedFields() {
            List<String> userFields = safeSqlBuilder.getAllowedSortFields("users");
            
            assertTrue(userFields.contains("id"));
            assertTrue(userFields.contains("username"));
            assertTrue(userFields.contains("email"));
        }
    }
}