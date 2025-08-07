# 安全配置中心使用指南

## 概述

安全配置中心是MyWeb博客系统的核心安全管理组件，提供统一的安全配置管理功能。该系统符合GB/T 22239-2019等保二级要求，支持动态配置管理、实时变更生效、配置备份恢复等功能。

## 功能特性

### 1. 统一配置管理
- **集中管理**：统一管理所有安全相关配置
- **分类管理**：按功能模块分类管理配置（安全、JWT、访问频率限制、备份等）
- **版本控制**：支持配置版本管理和历史追踪

### 2. 动态配置更新
- **实时生效**：配置变更无需重启系统即可生效
- **批量更新**：支持多个配置项的批量更新操作
- **配置验证**：更新前自动验证配置的有效性和安全性

### 3. 配置备份恢复
- **自动备份**：配置变更前自动创建备份
- **手动备份**：支持管理员手动创建配置备份
- **快速恢复**：支持从备份快速恢复配置
- **多重存储**：Redis缓存 + 文件系统双重备份

### 4. 安全审计
- **变更审计**：记录所有配置变更操作
- **操作追踪**：追踪配置变更的操作者和时间
- **事件通知**：配置变更时发送通知给相关人员

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    安全配置中心架构                          │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer                                           │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         SecurityConfigController                    │    │
│  │  - REST API接口                                     │    │
│  │  - 权限控制                                         │    │
│  │  - 参数验证                                         │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         SecurityConfigService                       │    │
│  │  - 配置管理逻辑                                     │    │
│  │  - 配置验证                                         │    │
│  │  - 备份恢复                                         │    │
│  │  - 事件发布                                         │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │      SecurityConfigEventListener                    │    │
│  │  - 配置变更监听                                     │    │
│  │  - 实时生效处理                                     │    │
│  │  - 通知发送                                         │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  Storage Layer                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │   Redis Cache   │  │  File System    │  │  Database   │  │
│  │  - 配置缓存     │  │  - 备份文件     │  │  - 审计日志 │  │
│  │  - 分布式锁     │  │  - 历史记录     │  │  - 变更记录 │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 配置类型

### 1. 安全配置 (security)
```yaml
app:
  security:
    password-policy:
      min-length: 8
      max-length: 128
      require-uppercase: true
      require-lowercase: true
      require-digit: true
      require-special-char: true
      bcrypt-strength: 12
    account-lock:
      max-failed-attempts: 5
      lock-duration-minutes: 15
      captcha-threshold: 3
    captcha:
      length: 6
      expiration-minutes: 5
      type: mixed
    session:
      timeout-minutes: 30
      single-session: true
      cleanup-interval-minutes: 10
    totp:
      issuer: MyWeb
      time-step-seconds: 30
      allowed-time-skew: 1
      secret-length: 32
```

### 2. JWT配置 (jwt)
```yaml
app:
  jwt:
    secret: "your-jwt-secret-key-here"
    access-token-expiration-seconds: 3600
    refresh-token-expiration-seconds: 604800
    issuer: MyWeb
    audience: MyWeb-Users
    token-prefix: "Bearer "
    header-name: Authorization
    enable-refresh-token: true
    blacklist:
      enabled: true
      cleanup-interval-minutes: 60
      redis-key-prefix: "jwt:blacklist:"
```

### 3. 访问频率限制配置 (rateLimit)
```yaml
app:
  rate-limit:
    enabled: true
    default-limit:
      window-size-seconds: 60
      max-requests: 100
      limit-type: IP
    endpoints:
      "/api/auth/login":
        window-size-seconds: 300
        max-requests: 5
        limit-type: IP
        enabled: true
        description: "登录接口限制"
    redis:
      key-prefix: "rate_limit:"
      key-expiration-seconds: 3600
    alert:
      enabled: true
      threshold: 0.8
      interval-minutes: 5
      recipients: ["admin@myweb.com"]
```

### 4. 备份配置 (backup)
```yaml
app:
  backup:
    enabled: true
    path: "/var/backups/myweb"
    retention-days: 30
    schedule:
      full-backup-cron: "0 0 2 * * ?"
      incremental-backup-cron: "0 0 */4 * * ?"
      enable-incremental: false
      timeout-minutes: 60
    encryption:
      enabled: true
      algorithm: "AES-256-GCM"
      key: "your-backup-encryption-key"
    compression:
      enabled: true
      algorithm: "gzip"
      level: 6
    notification:
      enabled: true
      email:
        enabled: true
        recipients: ["admin@myweb.com"]
        notify-on-success: false
        notify-on-failure: true
```

## API接口

### 1. 获取配置

#### 获取完整安全配置
```http
GET /api/security/config
Authorization: Bearer <token>
```

#### 获取指定类型配置
```http
GET /api/security/config/{configType}
Authorization: Bearer <token>
```

### 2. 更新配置

#### 更新指定类型配置
```http
PUT /api/security/config/{configType}
Authorization: Bearer <token>
Content-Type: application/json

{
  "passwordPolicy": {
    "minLength": 10,
    "bcryptStrength": 12
  }
}
```

#### 批量更新配置
```http
PUT /api/security/config/batch
Authorization: Bearer <token>
Content-Type: application/json

{
  "security": {
    "passwordPolicy": {
      "minLength": 10
    }
  },
  "jwt": {
    "accessTokenExpirationSeconds": 7200
  }
}
```

### 3. 配置管理

#### 重置配置
```http
POST /api/security/config/{configType}/reset
Authorization: Bearer <token>
```

#### 验证配置
```http
POST /api/security/config/{configType}/validate
Authorization: Bearer <token>
Content-Type: application/json

{
  "passwordPolicy": {
    "minLength": 8
  }
}
```

### 4. 备份恢复

#### 获取备份列表
```http
GET /api/security/config/{configType}/backups
Authorization: Bearer <token>
```

#### 恢复配置
```http
POST /api/security/config/{configType}/restore/{backupTimestamp}
Authorization: Bearer <token>
```

#### 获取变更历史
```http
GET /api/security/config/changes?configType=security&limit=50
Authorization: Bearer <token>
```

## 使用示例

### 1. 更新密码策略

```bash
curl -X PUT "http://localhost:8080/api/security/config/security" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "passwordPolicy": {
      "minLength": 12,
      "requireUppercase": true,
      "requireLowercase": true,
      "requireDigit": true,
      "requireSpecialChar": true,
      "bcryptStrength": 14
    }
  }'
```

### 2. 更新JWT配置

```bash
curl -X PUT "http://localhost:8080/api/security/config/jwt" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "accessTokenExpirationSeconds": 7200,
    "refreshTokenExpirationSeconds": 1209600,
    "enableRefreshToken": true
  }'
```

### 3. 批量更新配置

```bash
curl -X PUT "http://localhost:8080/api/security/config/batch" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "security": {
      "passwordPolicy": {
        "minLength": 10
      }
    },
    "rateLimit": {
      "enabled": true,
      "defaultLimit": {
        "maxRequests": 200
      }
    }
  }'
```

### 4. 恢复配置备份

```bash
curl -X POST "http://localhost:8080/api/security/config/security/restore/20250106_120000" \
  -H "Authorization: Bearer your-token"
```

## 安全考虑

### 1. 权限控制
- 只有具有 `SYSTEM_MANAGE` 权限的用户才能访问配置管理接口
- 所有配置变更操作都会记录操作者信息
- 支持基于角色的细粒度权限控制

### 2. 配置验证
- 更新前自动验证配置的有效性
- 防止无效配置导致系统异常
- 支持自定义验证规则

### 3. 审计日志
- 记录所有配置变更操作
- 包含操作者、时间、变更内容等信息
- 支持审计日志查询和导出

### 4. 备份保护
- 配置变更前自动创建备份
- 支持加密存储备份文件
- 提供完整性校验机制

## 监控告警

### 1. 配置变更监控
- 实时监控配置变更事件
- 异常配置变更自动告警
- 支持邮件、短信等多种告警方式

### 2. 系统健康检查
- 定期检查配置的有效性
- 监控配置服务的运行状态
- 提供健康检查接口

### 3. 性能监控
- 监控配置更新的响应时间
- 统计配置访问频率
- 提供性能优化建议

## 故障排除

### 1. 常见问题

#### 配置更新失败
- 检查配置格式是否正确
- 验证配置值是否在有效范围内
- 确认操作者是否有足够权限

#### 配置不生效
- 检查配置是否成功更新到缓存
- 验证相关服务是否正确监听配置变更事件
- 查看系统日志确认是否有异常

#### 备份恢复失败
- 检查备份文件是否存在且完整
- 验证备份文件的格式和版本兼容性
- 确认有足够的权限进行恢复操作

### 2. 日志分析

#### 查看配置变更日志
```bash
grep "配置变更" /var/log/myweb/application.log
```

#### 查看配置验证日志
```bash
grep "配置验证" /var/log/myweb/application.log
```

#### 查看备份恢复日志
```bash
grep "配置备份\|配置恢复" /var/log/myweb/application.log
```

## 最佳实践

### 1. 配置管理
- 定期备份重要配置
- 在生产环境变更前先在测试环境验证
- 保持配置文档的及时更新

### 2. 安全配置
- 使用强密码策略
- 定期更新JWT密钥
- 合理设置访问频率限制

### 3. 监控运维
- 设置合适的告警阈值
- 定期检查配置备份的完整性
- 建立配置变更的审批流程

## 版本历史

- **v1.0** (2025-01-06): 初始版本，支持基本的配置管理功能
  - 统一配置管理
  - 动态配置更新
  - 配置备份恢复
  - 安全审计功能

## 技术支持

如有问题或建议，请联系：
- 邮箱：security@myweb.com
- 文档：查看系统内置帮助文档
- 日志：查看 `/var/log/myweb/` 目录下的相关日志文件