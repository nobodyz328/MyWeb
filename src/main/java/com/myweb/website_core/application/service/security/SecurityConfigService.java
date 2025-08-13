package com.myweb.website_core.application.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.domain.security.dto.AuditLogRequest;
import com.myweb.website_core.infrastructure.config.properties.BackupProperties;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.infrastructure.config.JwtConfig;
import com.myweb.website_core.infrastructure.config.properties.RateLimitProperties;
import com.myweb.website_core.infrastructure.config.properties.SecurityProperties;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.security.dto.SecurityConfigBackupDTO;
import com.myweb.website_core.domain.security.dto.SecurityConfigChangeDTO;
import com.myweb.website_core.domain.security.dto.SecurityConfigDTO;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全配置中心服务
 * <p>
 * 提供统一的安全配置管理功能，包括：
 * - 动态配置管理
 * - 配置变更实时生效
 * - 配置变更审计
 * - 配置备份和恢复
 * - 配置验证和校验
 * <p>
 * 符合GB/T 22239-2019等保要求的配置管理规范
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityConfigService {
    
    private final SecurityProperties securityProperties;
    private final JwtConfig jwtConfig;
    private final RateLimitProperties rateLimitProperties;
    private final BackupProperties backupProperties;
    private final AuditMessageService auditLogService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    // 配置缓存，用于快速访问和变更检测
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    
    // Redis键前缀
    private static final String CONFIG_CACHE_PREFIX = "security:config:";
    private static final String CONFIG_BACKUP_PREFIX = "security:config:backup:";
    private static final String CONFIG_LOCK_PREFIX = "security:config:lock:";
    
    // 配置备份路径
    private static final String CONFIG_BACKUP_PATH = "/var/backups/myweb/config";
    
    /**
     * 获取完整的安全配置
     * 
     * @return 安全配置DTO
     */
    public SecurityConfigDTO getSecurityConfig() {
        log.debug("获取完整安全配置");
        
        SecurityConfigDTO config = new SecurityConfigDTO();
        config.setSecurityProperties(securityProperties);
        config.setJwtProperties(jwtConfig);
        config.setRateLimitProperties(rateLimitProperties);
        config.setBackupProperties(backupProperties);
        config.setLastModified(LocalDateTime.now());
        
        return config;
    }
    
    /**
     * 获取指定类型的配置
     * 
     * @param configType 配置类型
     * @return 配置对象
     */
    public Object getConfig(String configType) {
        log.debug("获取配置类型: {}", configType);
        
        // 先从缓存获取
        Object cachedConfig = configCache.get(configType);
        if (cachedConfig != null) {
            return cachedConfig;
        }
        
        // 从Redis获取
        String redisKey = CONFIG_CACHE_PREFIX + configType;
        Object redisConfig = redisTemplate.opsForValue().get(redisKey);
        if (redisConfig != null) {
            configCache.put(configType, redisConfig);
            return redisConfig;
        }
        
        // 从原始配置获取
        Object originalConfig = getOriginalConfig(configType);
        if (originalConfig != null) {
            configCache.put(configType, originalConfig);
            redisTemplate.opsForValue().set(redisKey, originalConfig);
        }
        
        return originalConfig;
    }
    
    /**
     * 动态更新安全配置
     * 
     * @param configType 配置类型
     * @param newConfig 新配置
     * @param operator 操作者
     * @return 更新结果
     */
    @Transactional
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<Boolean> updateConfig(String configType, Object newConfig, String operator) {
        log.info("更新安全配置 - 类型: {}, 操作者: {}", configType, operator);
        
        try {
            // 获取分布式锁
            String lockKey = CONFIG_LOCK_PREFIX + configType;
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, operator, 
                java.time.Duration.ofMinutes(5));
            
            if (!lockAcquired) {
                log.warn("获取配置锁失败 - 类型: {}, 操作者: {}", configType, operator);
                return CompletableFuture.completedFuture(false);
            }
            
            try {
                // 验证配置
                validateConfig(configType, newConfig);
                
                // 备份当前配置
                Object currentConfig = getConfig(configType);
                backupConfig(configType, currentConfig, operator);
                
                // 更新配置
                boolean updated = applyConfigUpdate(configType, newConfig);
                
                if (updated) {
                    // 更新缓存
                    configCache.put(configType, newConfig);
                    redisTemplate.opsForValue().set(CONFIG_CACHE_PREFIX + configType, newConfig);
                    
                    // 发布配置变更事件
                    publishConfigChangeEvent(configType, currentConfig, newConfig, operator);
                    
                    // 记录审计日志
                    auditConfigChange(configType, currentConfig, newConfig, operator, "SUCCESS");
                    
                    log.info("安全配置更新成功 - 类型: {}", configType);
                } else {
                    log.error("安全配置更新失败 - 类型: {}", configType);
                    auditConfigChange(configType, currentConfig, newConfig, operator, "FAILURE");
                }
                
                return CompletableFuture.completedFuture(updated);
                
            } finally {
                // 释放锁
                redisTemplate.delete(lockKey);
            }
            
        } catch (Exception e) {
            log.error("更新安全配置异常 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            auditConfigChange(configType, null, newConfig, operator, "ERROR");
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 批量更新配置
     * 
     * @param configUpdates 配置更新映射
     * @param operator 操作者
     * @return 更新结果
     */
    @Transactional
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<Map<String, Boolean>> batchUpdateConfig(
            Map<String, Object> configUpdates, String operator) {
        log.info("批量更新安全配置 - 数量: {}, 操作者: {}", configUpdates.size(), operator);
        
        Map<String, Boolean> results = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : configUpdates.entrySet()) {
            String configType = entry.getKey();
            Object newConfig = entry.getValue();
            
            try {
                Boolean result = updateConfig(configType, newConfig, operator).get();
                results.put(configType, result);
            } catch (Exception e) {
                log.error("批量更新配置失败 - 类型: {}, 错误: {}", configType, e.getMessage());
                results.put(configType, false);
            }
        }
        
        return CompletableFuture.completedFuture(results);
    }
    
    /**
     * 重置配置到默认值
     * 
     * @param configType 配置类型
     * @param operator 操作者
     * @return 重置结果
     */
    @Transactional
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<Boolean> resetConfig(String configType, String operator) {
        log.info("重置安全配置 - 类型: {}, 操作者: {}", configType, operator);
        
        try {
            Object defaultConfig = createDefaultConfig(configType);
            if (defaultConfig != null) {
                return updateConfig(configType, defaultConfig, operator);
            } else {
                log.error("无法创建默认配置 - 类型: {}", configType);
                return CompletableFuture.completedFuture(false);
            }
        } catch (Exception e) {
            log.error("重置配置异常 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 备份配置
     * 
     * @param configType 配置类型
     * @param config 配置对象
     * @param operator 操作者
     */
    private void backupConfig(String configType, Object config, String operator) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupKey = CONFIG_BACKUP_PREFIX + configType + ":" + timestamp;
            
            SecurityConfigBackupDTO backup = new SecurityConfigBackupDTO();
            backup.setConfigType(configType);
            backup.setConfigData(objectMapper.writeValueAsString(config));
            backup.setOperator(operator);
            backup.setBackupTime(LocalDateTime.now());
            
            // Redis备份
            redisTemplate.opsForValue().set(backupKey, backup, java.time.Duration.ofDays(30));
            
            // 文件备份
            saveConfigBackupToFile(configType, backup, timestamp);
            
            log.debug("配置备份完成 - 类型: {}, 时间戳: {}", configType, timestamp);
            
        } catch (Exception e) {
            log.error("配置备份失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
        }
    }
    
    /**
     * 恢复配置
     * 
     * @param configType 配置类型
     * @param backupTimestamp 备份时间戳
     * @param operator 操作者
     * @return 恢复结果
     */
    @Transactional
    @Auditable(operation = AuditOperation.SYSTEM_CONFIG_UPDATE, resourceType = "SECURITY_CONFIG")
    public CompletableFuture<Boolean> restoreConfig(String configType, String backupTimestamp, String operator) {
        log.info("恢复安全配置 - 类型: {}, 备份时间: {}, 操作者: {}", configType, backupTimestamp, operator);
        
        try {
            String backupKey = CONFIG_BACKUP_PREFIX + configType + ":" + backupTimestamp;
            SecurityConfigBackupDTO backup = (SecurityConfigBackupDTO) redisTemplate.opsForValue().get(backupKey);
            
            if (backup == null) {
                // 尝试从文件恢复
                backup = loadConfigBackupFromFile(configType, backupTimestamp);
            }
            
            if (backup != null) {
                Object restoredConfig = objectMapper.readValue(backup.getConfigData(), 
                    getConfigClass(configType));
                return updateConfig(configType, restoredConfig, operator);
            } else {
                log.error("未找到配置备份 - 类型: {}, 时间戳: {}", configType, backupTimestamp);
                return CompletableFuture.completedFuture(false);
            }
            
        } catch (Exception e) {
            log.error("恢复配置异常 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 获取配置备份列表
     * 
     * @param configType 配置类型
     * @return 备份列表
     */
    public List<SecurityConfigBackupDTO> getConfigBackups(String configType) {
        log.debug("获取配置备份列表 - 类型: {}", configType);
        
        try {
            String pattern = CONFIG_BACKUP_PREFIX + configType + ":*";
            return redisTemplate.keys(pattern).stream()
                .map(key -> (SecurityConfigBackupDTO) redisTemplate.opsForValue().get(key))
                .filter(backup -> backup != null)
                .sorted((a, b) -> b.getBackupTime().compareTo(a.getBackupTime()))
                .toList();
        } catch (Exception e) {
            log.error("获取配置备份列表失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * 验证配置有效性
     * 
     * @param configType 配置类型
     * @param config 配置对象
     */
    private void validateConfig(String configType, Object config) {
        if (config == null) {
            throw new ValidationException("配置不能为空");
        }
        
        switch (configType) {
            case "security":
                validateSecurityProperties((SecurityProperties) config);
                break;
            case "jwt":
                validateJwtProperties((JwtConfig) config);
                break;
            case "rateLimit":
                validateRateLimitProperties((RateLimitProperties) config);
                break;
            case "backup":
                validateBackupProperties((BackupProperties) config);
                break;
            default:
                log.warn("未知配置类型，跳过验证: {}", configType);
        }
    }
    
    /**
     * 验证安全属性配置
     */
    private void validateSecurityProperties(SecurityProperties config) {
        if (config.getPasswordPolicy().getMinLength() < 8) {
            throw new ValidationException("密码最小长度不能小于8位");
        }
        if (config.getPasswordPolicy().getBcryptStrength() < 10) {
            throw new ValidationException("BCrypt强度不能小于10");
        }
        if (config.getAccountLock().getMaxFailedAttempts() < 3) {
            throw new ValidationException("最大失败次数不能小于3次");
        }
    }
    
    /**
     * 验证JWT属性配置
     */
    private void validateJwtProperties(JwtConfig config) {
        if (config.getSecret() == null || config.getSecret().length() < 32) {
            throw new ValidationException("JWT密钥长度不能小于32位");
        }
        if (config.getAccessTokenExpirationMillis() < 300) {
            throw new ValidationException("访问令牌过期时间不能小于5分钟");
        }
    }
    
    /**
     * 验证访问频率限制配置
     */
    private void validateRateLimitProperties(RateLimitProperties config) {
        if (config.getDefaultLimit().getMaxRequests() < 1) {
            throw new ValidationException("最大请求数不能小于1");
        }
        if (config.getDefaultLimit().getWindowSizeSeconds() < 1) {
            throw new ValidationException("时间窗口不能小于1秒");
        }
    }
    
    /**
     * 验证备份属性配置
     */
    private void validateBackupProperties(BackupProperties config) {
        if (config.getRetentionDays() < 1) {
            throw new ValidationException("备份保留天数不能小于1天");
        }
        if (config.getPath() == null || config.getPath().trim().isEmpty()) {
            throw new ValidationException("备份路径不能为空");
        }
    }
    
    /**
     * 应用配置更新
     */
    private boolean applyConfigUpdate(String configType, Object newConfig) {
        try {
            switch (configType) {
                case "security":
                    BeanUtils.copyProperties(newConfig, securityProperties);
                    break;
                case "jwt":
                    BeanUtils.copyProperties(newConfig, jwtConfig);
                    break;
                case "rateLimit":
                    BeanUtils.copyProperties(newConfig, rateLimitProperties);
                    break;
                case "backup":
                    BeanUtils.copyProperties(newConfig, backupProperties);
                    break;
                default:
                    log.warn("未知配置类型，无法应用更新: {}", configType);
                    return false;
            }
            return true;
        } catch (Exception e) {
            log.error("应用配置更新失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取原始配置
     */
    private Object getOriginalConfig(String configType) {
        return switch (configType) {
            case "security" -> securityProperties;
            case "jwt" -> jwtConfig;
            case "rateLimit" -> rateLimitProperties;
            case "backup" -> backupProperties;
            default -> null;
        };
    }
    
    /**
     * 获取配置类
     */
    private Class<?> getConfigClass(String configType) {
        return switch (configType) {
            case "security" -> SecurityProperties.class;
            case "jwt" -> JwtConfig.class;
            case "rateLimit" -> RateLimitProperties.class;
            case "backup" -> BackupProperties.class;
            default -> Object.class;
        };
    }
    
    /**
     * 创建默认配置
     */
    private Object createDefaultConfig(String configType) {
        return switch (configType) {
            case "security" -> new SecurityProperties();
            case "jwt" -> new JwtConfig();
            case "rateLimit" -> new RateLimitProperties();
            case "backup" -> new BackupProperties();
            default -> null;
        };
    }
    
    /**
     * 发布配置变更事件
     */
    private void publishConfigChangeEvent(String configType, Object oldConfig, Object newConfig, String operator) {
        try {
            SecurityConfigChangeDTO changeEvent = new SecurityConfigChangeDTO();
            changeEvent.setConfigType(configType);
            changeEvent.setOldConfig(objectMapper.writeValueAsString(oldConfig));
            changeEvent.setNewConfig(objectMapper.writeValueAsString(newConfig));
            changeEvent.setOperator(operator);
            changeEvent.setChangeTime(LocalDateTime.now());
            
            eventPublisher.publishEvent(changeEvent);
            log.debug("配置变更事件已发布 - 类型: {}", configType);
            
        } catch (Exception e) {
            log.error("发布配置变更事件失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
        }
    }
    
    /**
     * 审计配置变更
     */
    private void auditConfigChange(String configType, Object oldConfig, Object newConfig, 
                                 String operator, String result) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("configType", configType);
            auditData.put("operator", operator);
            auditData.put("result", result);
            auditData.put("changeTime", LocalDateTime.now());
            
            if (oldConfig != null) {
                auditData.put("oldConfig", objectMapper.writeValueAsString(oldConfig));
            }
            if (newConfig != null) {
                auditData.put("newConfig", objectMapper.writeValueAsString(newConfig));
            }
            
            auditLogService.logOperation(
                    AuditLogRequest.system(
                            AuditOperation.CONFIG_CHANGE,
                            "配置变更"
                    )
            );
            
        } catch (Exception e) {
            log.error("审计配置变更失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
        }
    }
    
    /**
     * 保存配置备份到文件
     */
    private void saveConfigBackupToFile(String configType, SecurityConfigBackupDTO backup, String timestamp) {
        try {
            Path backupDir = Paths.get(CONFIG_BACKUP_PATH);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            
            String fileName = String.format("%s_%s.json", configType, timestamp);
            Path backupFile = backupDir.resolve(fileName);
            
            String backupJson = objectMapper.writeValueAsString(backup);
            Files.write(backupFile, backupJson.getBytes());
            
            log.debug("配置备份文件已保存: {}", backupFile);
            
        } catch (IOException e) {
            log.error("保存配置备份文件失败 - 类型: {}, 错误: {}", configType, e.getMessage(), e);
        }
    }
    
    /**
     * 从文件加载配置备份
     */
    private SecurityConfigBackupDTO loadConfigBackupFromFile(String configType, String timestamp) {
        try {
            String fileName = String.format("%s_%s.json", configType, timestamp);
            Path backupFile = Paths.get(CONFIG_BACKUP_PATH, fileName);
            
            if (Files.exists(backupFile)) {
                String backupJson = Files.readString(backupFile);
                return objectMapper.readValue(backupJson, SecurityConfigBackupDTO.class);
            }
            
        } catch (IOException e) {
            log.error("加载配置备份文件失败 - 类型: {}, 时间戳: {}, 错误: {}", 
                configType, timestamp, e.getMessage(), e);
        }
        
        return null;
    }
}