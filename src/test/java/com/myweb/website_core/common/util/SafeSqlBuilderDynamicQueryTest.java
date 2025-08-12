package com.myweb.website_core.common.util;

import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.quality.Strictness.LENIENT;

/**
 * SafeSqlBuilder动态查询构建测试
 * <p>
 * 测试SafeSqlBuilder的动态查询构建功能：
 * 1. 复杂动态查询构建
 * 2. 参数化查询处理
 * 3. 子查询和UNION查询
 * 4. 安全验证和SQL注入防护
 * <p>
 * 符合需求：5.1, 5.6, 5.7 - 动态查询安全构建
 * 
 * @author MyWeb
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
@DisplayName("SafeSqlBuilder动态查询构建测试")
class SafeSqlBuilderDynamicQueryTest {
    
    @Mock
    private SqlInjectionProtectionService sqlInjectionProtectionService;
    
    private SafeSqlBuilder safeSqlBuilder;
    
    @BeforeEach
    void setUp() {
        safeSqlBuilder = new SafeSqlBuilder(sqlInjectionProtectionService);
        
        // 设置SQL注入保护服务的默认行为
        doNothing().when(sqlInjectionProtectionService)
            .validateAndSanitizeInput(anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("测试复杂动态查询构建 - 基本查询")
    void testBuildComplexDynamicQuery_BasicQuery() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .select("id", "title", "content")
            .where("author_id", 1L)
            .where("status", "PUBLISHED")
            .orderBy("created_at", "DESC")
            .limit(10)
            .offset(0);
        
        // When
        String result = safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("SELECT id, title, content"));
        assertTrue(result.contains("FROM posts"));
        assertTrue(result.contains("WHERE"));
        assertTrue(result.contains("author_id = #{author_id}"));
        assertTrue(result.contains("status = #{status}"));
        assertTrue(result.contains("ORDER BY created_at DESC"));
        assertTrue(result.contains("LIMIT 10"));
        assertTrue(result.contains("OFFSET 0"));
        
        // 验证SQL注入保护被调用
        verify(sqlInjectionProtectionService, atLeastOnce())
            .validateAndSanitizeInput(anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("测试复杂动态查询构建 - 带JOIN的查询")
    void testBuildComplexDynamicQuery_WithJoins() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .alias("p")
            .select("p.id", "p.title", "u.username")
            .join("INNER", "users", "u", "p.author_id = u.id")
            .join("LEFT", "comments", "c", "p.id = c.post_id")
            .where("status", "PUBLISHED")
            .orderBy("created_at", "DESC");
        
        // When
        String result = safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("SELECT p.id, p.title, u.username"));
        assertTrue(result.contains("FROM posts p"));
        assertTrue(result.contains("INNER JOIN users u ON p.author_id = u.id"));
        assertTrue(result.contains("LEFT JOIN comments c ON p.id = c.post_id"));
        assertTrue(result.contains("WHERE status = #{status}"));
        assertTrue(result.contains("ORDER BY created_at DESC"));
    }
    
    @Test
    @DisplayName("测试复杂动态查询构建 - 带聚合和分组的查询")
    void testBuildComplexDynamicQuery_WithAggregationAndGroupBy() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .select("author_id")
            .aggregate("COUNT(*) as post_count", "AVG(like_count) as avg_likes")
            .where("status", "PUBLISHED")
            .groupBy("author_id")
            .having("COUNT(*)", 5)
            .orderBy("created_at", "DESC")
            .limit(20);
        
        // When
        String result = safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        
        // 打印实际生成的SQL以便调试
        System.out.println("Generated SQL: " + result);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("SELECT author_id, COUNT(*) as post_count, AVG(like_count) as avg_likes"), 
                  "Expected SELECT clause not found in: " + result);
        assertTrue(result.contains("FROM posts"), 
                  "Expected FROM clause not found in: " + result);
        assertTrue(result.contains("WHERE status = #{status}"), 
                  "Expected WHERE clause not found in: " + result);
        assertTrue(result.contains("GROUP BY author_id"), 
                  "Expected GROUP BY clause not found in: " + result);
        assertTrue(result.contains("HAVING COUNT(*) = #{HAVING_COUNT___}"), 
                  "Expected HAVING clause not found in: " + result);
        assertTrue(result.contains("ORDER BY created_at DESC"), 
                  "Expected ORDER BY clause not found in: " + result);
        assertTrue(result.contains("LIMIT 20"), 
                  "Expected LIMIT clause not found in: " + result);
    }
    
    @Test
    @DisplayName("测试参数化查询构建")
    void testBuildParameterizedQuery() {
        // Given
        String baseQuery = "SELECT * FROM posts WHERE author_id = #{authorId} AND created_at > #{startDate}";
        Map<String, Object> parameters = Map.of(
            "authorId", 1L,
            "startDate", "2024-01-01"
        );
        
        // When
        SafeSqlBuilder.ParameterizedQuery result = safeSqlBuilder.buildParameterizedQuery(baseQuery, parameters);
        
        // Then
        assertNotNull(result);
        assertEquals(baseQuery, result.getQuery());
        assertEquals(parameters, result.getParameters());
        
        // 验证SQL注入保护被调用
        verify(sqlInjectionProtectionService).validateAndSanitizeInput(baseQuery, "QUERY_TEMPLATE", "baseQuery");
        verify(sqlInjectionProtectionService).validateAndSanitizeInput("2024-01-01", "PARAMETER", "startDate");
    }
    
    @Test
    @DisplayName("测试安全子查询构建")
    void testBuildSafeSubQuery() {
        // Given
        String subQuery = "SELECT author_id FROM posts WHERE like_count > 100";
        String alias = "popular_authors";
        
        // When
        String result = safeSqlBuilder.buildSafeSubQuery(subQuery, alias);
        
        // Then
        assertNotNull(result);
        assertEquals("(" + subQuery + ") AS " + alias, result);
        
        // 验证SQL注入保护被调用
        verify(sqlInjectionProtectionService).validateAndSanitizeInput(subQuery, "SUBQUERY", "subQuery");
    }
    
    @Test
    @DisplayName("测试安全UNION查询构建")
    void testBuildSafeUnionQuery() {
        // Given
        List<String> queries = List.of(
            "SELECT id, title FROM posts WHERE status = 'PUBLISHED'",
            "SELECT id, title FROM drafts WHERE author_id = 1"
        );
        String unionType = "UNION";
        
        // When
        String result = safeSqlBuilder.buildSafeUnionQuery(queries, unionType);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("UNION"));
        assertTrue(result.contains("SELECT id, title FROM posts WHERE status = 'PUBLISHED'"));
        assertTrue(result.contains("SELECT id, title FROM drafts WHERE author_id = 1"));
        
        // 验证SQL注入保护被调用
        verify(sqlInjectionProtectionService, times(2))
            .validateAndSanitizeInput(anyString(), eq("UNION_QUERY"), anyString());
    }
    
    @Test
    @DisplayName("测试动态查询构建器 - 链式调用")
    void testDynamicQueryBuilder_FluentInterface() {
        // Given & When
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .alias("p")
            .select("id", "title")
            .aggregate("COUNT(*)")
            .join("INNER", "users", "u", "p.author_id = u.id")
            .where("status", "PUBLISHED")
            .where(Map.of("like_count", 10))
            .groupBy("author_id")
            .having("COUNT(*)", 5)
            .orderBy("created_at", "DESC")
            .limit(20)
            .offset(10);
        
        // Then
        assertEquals("posts", queryBuilder.getMainTable());
        assertEquals("p", queryBuilder.getMainTableAlias());
        assertEquals(List.of("id", "title"), queryBuilder.getSelectFields());
        assertEquals(List.of("COUNT(*)"), queryBuilder.getAggregations());
        assertEquals(1, queryBuilder.getJoins().size());
        assertEquals(2, queryBuilder.getConditions().size());
        assertEquals(List.of("author_id"), queryBuilder.getGroupByFields());
        assertEquals(1, queryBuilder.getHavingConditions().size());
        assertEquals("created_at", queryBuilder.getSortField());
        assertEquals("DESC", queryBuilder.getSortDirection());
        assertEquals(Integer.valueOf(20), queryBuilder.getLimit());
        assertEquals(Integer.valueOf(10), queryBuilder.getOffset());
    }
    
    @Test
    @DisplayName("测试安全验证 - 不安全的表名")
    void testSecurityValidation_UnsafeTableName() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts; DROP TABLE users; --");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        });
        
        assertTrue(exception.getMessage().contains("Unsafe main table name"));
    }
    
    @Test
    @DisplayName("测试安全验证 - 不安全的字段名")
    void testSecurityValidation_UnsafeFieldName() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .select("id; DROP TABLE users; --");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        });
        
        assertTrue(exception.getMessage().contains("Unsafe select field"));
    }
    
    @Test
    @DisplayName("测试安全验证 - 不允许的排序字段")
    void testSecurityValidation_DisallowedSortField() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .select("id", "title")
            .orderBy("malicious_field", "DESC");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        });
        
        assertTrue(exception.getMessage().contains("Sort field 'malicious_field' not allowed"));
    }
    
    @Test
    @DisplayName("测试安全验证 - 无效的JOIN类型")
    void testSecurityValidation_InvalidJoinType() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .join("MALICIOUS_JOIN", "users", "u", "posts.author_id = u.id");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        });
        
        assertTrue(exception.getMessage().contains("Invalid join type"));
    }
    
    @Test
    @DisplayName("测试安全验证 - 无效的聚合函数")
    void testSecurityValidation_InvalidAggregationFunction() {
        // Given
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .aggregate("MALICIOUS_FUNCTION(id)");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder);
        });
        
        assertTrue(exception.getMessage().contains("Invalid aggregation function"));
    }
    
    @Test
    @DisplayName("测试参数化查询 - SQL注入防护")
    void testParameterizedQuery_SqlInjectionProtection() {
        // Given
        String baseQuery = "SELECT * FROM posts WHERE title = #{title}";
        Map<String, Object> parameters = Map.of("title", "'; DROP TABLE posts; --");
        
        // 模拟SQL注入检测
        doThrow(new RuntimeException("SQL injection detected"))
            .when(sqlInjectionProtectionService)
            .validateAndSanitizeInput(eq("'; DROP TABLE posts; --"), eq("PARAMETER"), eq("title"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            safeSqlBuilder.buildParameterizedQuery(baseQuery, parameters);
        });
        
        assertEquals("SQL injection detected", exception.getMessage());
    }
    
    @Test
    @DisplayName("测试LIMIT和OFFSET验证")
    void testLimitOffsetValidation() {
        // Given - 测试无效的LIMIT值
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder1 = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .limit(-1);
        
        // When & Then
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder1);
        });
        assertTrue(exception1.getMessage().contains("Invalid limit value"));
        
        // Given - 测试过大的LIMIT值
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder2 = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .limit(2000);
        
        // When & Then
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder2);
        });
        assertTrue(exception2.getMessage().contains("Invalid limit value"));
        
        // Given - 测试无效的OFFSET值
        SafeSqlBuilder.DynamicQueryBuilder queryBuilder3 = new SafeSqlBuilder.DynamicQueryBuilder("posts")
            .offset(-1);
        
        // When & Then
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            safeSqlBuilder.buildComplexDynamicQuery(queryBuilder3);
        });
        assertTrue(exception3.getMessage().contains("Invalid offset value"));
    }
}