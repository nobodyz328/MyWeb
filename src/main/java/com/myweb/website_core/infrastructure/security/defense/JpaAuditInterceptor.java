package com.myweb.website_core.infrastructure.security.defense;

import com.myweb.website_core.application.service.security.IPS.virusprotect.SqlInjectionProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA/Hibernate SQL审计拦截器
 * <p>
 * 拦截所有JPA/Hibernate SQL执行，进行安全检查和审计日志记录：
 * 1. 实体操作监控
 * 2. SQL注入检测
 * 3. 数据变更审计
 * 4. 性能监控
 * <p>
 * 符合需求：4.1, 4.4 - 入侵防范机制
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaAuditInterceptor implements 
    PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener,
    PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final SqlInjectionProtectionService sqlInjectionProtectionService;

    // ==================== Pre-Event Listeners ====================

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        long startTime = System.currentTimeMillis();
        
        try {
            String entityName = event.getEntity().getClass().getSimpleName();
            String operation = "INSERT";
            
            // 验证实体数据安全性
            validateEntityData(event.getState(), event.getPersister().getPropertyNames(), 
                             entityName, operation);
            
            log.debug("Pre-insert validation passed for entity: {}", entityName);
            return false; // 不阻止操作
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Pre-insert validation failed: {}, execution time: {}ms", e.getMessage(), executionTime);
            throw new SecurityException("Entity insert validation failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        long startTime = System.currentTimeMillis();
        
        try {
            String entityName = event.getEntity().getClass().getSimpleName();
            String operation = "UPDATE";
            
            // 验证实体数据安全性
            validateEntityData(event.getState(), event.getPersister().getPropertyNames(), 
                             entityName, operation);
            
            log.debug("Pre-update validation passed for entity: {}", entityName);
            return false; // 不阻止操作
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Pre-update validation failed: {}, execution time: {}ms", e.getMessage(), executionTime);
            throw new SecurityException("Entity update validation failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        long startTime = System.currentTimeMillis();
        
        try {
            String entityName = event.getEntity().getClass().getSimpleName();
            Object entityId = event.getId();
            
            log.debug("Pre-delete validation for entity: {}, ID: {}", entityName, entityId);
            
            // 记录删除操作审计
            auditEntityOperation(entityName, "DELETE", entityId, null);
            
            return false; // 不阻止操作
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Pre-delete validation failed: {}, execution time: {}ms", e.getMessage(), executionTime);
            throw new SecurityException("Entity delete validation failed: " + e.getMessage());
        }
    }

    // ==================== Post-Event Listeners ====================

    @Override
    public void onPostInsert(PostInsertEvent event) {
        try {
            String entityName = event.getEntity().getClass().getSimpleName();
            Object entityId = event.getId();
            
            // 记录插入操作审计
            auditEntityOperation(entityName, "INSERT", entityId, event.getState());
            
            log.debug("Post-insert audit completed for entity: {}, ID: {}", entityName, entityId);
            
        } catch (Exception e) {
            log.error("Post-insert audit failed: {}", e.getMessage());
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        try {
            String entityName = event.getEntity().getClass().getSimpleName();
            Object entityId = event.getId();
            
            // 记录更新操作审计
            auditEntityOperation(entityName, "UPDATE", entityId, event.getState());
            
            log.debug("Post-update audit completed for entity: {}, ID: {}", entityName, entityId);
            
        } catch (Exception e) {
            log.error("Post-update audit failed: {}", e.getMessage());
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        try {
            String entityName = event.getEntity().getClass().getSimpleName();
            Object entityId = event.getId();
            
            // 记录删除操作审计
            auditEntityOperation(entityName, "DELETE", entityId, null);
            
            log.debug("Post-delete audit completed for entity: {}, ID: {}", entityName, entityId);
            
        } catch (Exception e) {
            log.error("Post-delete audit failed: {}", e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 验证实体数据安全性
     * 
     * @param state 实体状态数组
     * @param propertyNames 属性名数组
     * @param entityName 实体名称
     * @param operation 操作类型
     */
    private void validateEntityData(Object[] state, String[] propertyNames, 
                                  String entityName, String operation) {
        if (state == null || propertyNames == null) {
            return;
        }
        
        for (int i = 0; i < state.length && i < propertyNames.length; i++) {
            Object value = state[i];
            String propertyName = propertyNames[i];
            
            // 只验证字符串类型的属性
            if (value instanceof String) {
                String stringValue = (String) value;
                
                // 检查SQL注入
                if (sqlInjectionProtectionService.detectSqlInjection(stringValue, "ENTITY_FIELD")) {
                    throw new SecurityException(
                        String.format("SQL injection detected in entity %s, field %s, operation %s", 
                                    entityName, propertyName, operation)
                    );
                }
            }
        }
    }
    
    /**
     * 记录实体操作审计
     * 
     * @param entityName 实体名称
     * @param operation 操作类型
     * @param entityId 实体ID
     * @param state 实体状态
     */
    private void auditEntityOperation(String entityName, String operation, 
                                    Object entityId, Object[] state) {
        try {
            // 这里可以集成到审计日志服务
            log.info("JPA Entity Operation - Entity: {}, Operation: {}, ID: {}", 
                    entityName, operation, entityId);
            
            // 如果需要详细的字段变更记录，可以在这里实现
            if (state != null && log.isDebugEnabled()) {
                log.debug("Entity state length: {}", state.length);
            }
            
        } catch (Exception e) {
            log.error("Failed to audit entity operation: {}", e.getMessage());
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }
}