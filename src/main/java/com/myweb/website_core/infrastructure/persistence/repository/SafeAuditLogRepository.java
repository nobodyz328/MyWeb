package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.security.entity.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Component;

/**
 * 安全的审计日志Repository实现
 * <p>
 * 继承SafeRepositoryBase，提供安全的审计日志数据访问方法
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Component
public class SafeAuditLogRepository extends SafeRepositoryBase<AuditLog, Long> {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Override
    protected JpaRepository<AuditLog, Long> getRepository() {
        return auditLogRepository;
    }
    
    @Override
    protected JpaSpecificationExecutor<AuditLog> getSpecificationExecutor() {
        return auditLogRepository;
    }
    
    @Override
    protected String getTableName() {
        return "audit_logs";
    }
}