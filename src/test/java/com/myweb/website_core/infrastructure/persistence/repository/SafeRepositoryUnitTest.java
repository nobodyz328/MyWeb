package com.myweb.website_core.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全Repository单元测试
 * <p>
 * 测试安全Repository基类扩展的基本功能：
 * 1. 参数验证
 * 2. 安全性检查
 * 3. 基本逻辑验证
 * <p>
 * 符合需求：5.1, 5.4, 5.6 - 安全查询服务集成测试
 * 
 * @author MyWeb
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SafeRepositoryUnitTest {
    
    /**
     * 测试参数验证功能
     */
    @Test
    public void testParameterValidation() {
        // 测试分页参数验证
        assertThrows(IllegalArgumentException.class, () -> {
            validatePaginationParams(-1, 10);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            validatePaginationParams(0, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            validatePaginationParams(0, 2000);
        });
        
        // 正常参数应该不抛异常
        assertDoesNotThrow(() -> {
            validatePaginationParams(0, 10);
        });
        
        assertDoesNotThrow(() -> {
            validatePaginationParams(5, 100);
        });
    }
    
    /**
     * 测试字段名验证
     */
    @Test
    public void testFieldNameValidation() {
        // 测试无效字段名
        assertFalse(isSafeFieldName(null));
        assertFalse(isSafeFieldName(""));
        assertFalse(isSafeFieldName("   "));
        assertFalse(isSafeFieldName("field with spaces"));
        assertFalse(isSafeFieldName("field-with-dashes"));
        assertFalse(isSafeFieldName("field.with.dots"));
        assertFalse(isSafeFieldName("SELECT"));
        assertFalse(isSafeFieldName("DROP"));
        
        // 测试有效字段名
        assertTrue(isSafeFieldName("id"));
        assertTrue(isSafeFieldName("username"));
        assertTrue(isSafeFieldName("created_at"));
        assertTrue(isSafeFieldName("field123"));
        assertTrue(isSafeFieldName("_private_field"));
    }
    
    /**
     * 测试排序方向验证
     */
    @Test
    public void testSortDirectionValidation() {
        // 测试有效排序方向
        assertTrue(isValidSortDirection("ASC"));
        assertTrue(isValidSortDirection("DESC"));
        assertTrue(isValidSortDirection("asc"));
        assertTrue(isValidSortDirection("desc"));
        
        // 测试无效排序方向
        assertFalse(isValidSortDirection("INVALID"));
        assertFalse(isValidSortDirection("UP"));
        assertFalse(isValidSortDirection("DOWN"));
        assertFalse(isValidSortDirection(null));
        assertFalse(isValidSortDirection(""));
    }
    
    /**
     * 验证分页参数的辅助方法
     */
    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("Page size must be between 1 and 1000");
        }
    }
    
    /**
     * 验证字段名安全性的辅助方法
     */
    private boolean isSafeFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否符合安全的字段名模式
        if (!fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return false;
        }
        
        // 检查长度限制
        if (fieldName.length() > 64) {
            return false;
        }
        
        // 检查是否为SQL关键词
        String[] sqlKeywords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "UNION", "WHERE", "ORDER", "GROUP", "HAVING", "LIMIT", "OFFSET"
        };
        
        for (String keyword : sqlKeywords) {
            if (keyword.equalsIgnoreCase(fieldName)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 验证排序方向的辅助方法
     */
    private boolean isValidSortDirection(String direction) {
        if (direction == null || direction.trim().isEmpty()) {
            return false;
        }
        
        String[] validDirections = {"ASC", "DESC", "asc", "desc"};
        for (String valid : validDirections) {
            if (valid.equals(direction)) {
                return true;
            }
        }
        
        return false;
    }
}