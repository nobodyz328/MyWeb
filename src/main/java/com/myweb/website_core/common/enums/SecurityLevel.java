package com.myweb.website_core.common.enums;

/**
 * 安全级别枚举
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-13
 */
public enum SecurityLevel {
    
    SAFE("安全", 0),
    LOW_RISK("低风险", 1),
    MEDIUM_RISK("中风险", 2),
    HIGH_RISK("高风险", 3);
    
    private final String description;
    private final int level;
    
    SecurityLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(SecurityLevel other) {
        return this.level > other.level;
    }
    
    public boolean isRisky() {
        return this.level > 0;
    }
    
    public boolean isHighRisk() {
        return this.level >= 3;
    }
}