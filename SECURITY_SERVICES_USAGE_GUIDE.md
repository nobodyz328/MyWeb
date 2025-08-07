# MyWeb博客系统安全服务使用指南

## 概述

本文档介绍MyWeb博客系统中已实现并可投入使用的安全服务，这些服务符合GB/T 22239-2019二级等保要求，为系统提供全面的安全保护。

## 已投入使用的安全服务

### 1. 自动备份服务 (BackupService)

**功能描述**: 提供数据库和关键文件的自动备份功能

**主要特性**:
- ✅ 定时自动备份（每日凌晨2点）
- ✅ 备份文件AES-256加密存储
- ✅ 备份完整性SHA-256校验
- ✅ 过期备份自动清理
- ✅ 备份失败告警机制

**使用方式**:
```bash
# 查看可用备份
GET /api/admin/data-recovery/backups

# 手动触发备份
POST /api/admin/security-services/backup/trigger

# 验证备份完整性
# 系统会自动验证，也可通过日志查看验证结果
```

**配置参数**:
```yaml
app:
  backup:
    path: ${java.io.tmpdir}/myweb/backups
    retention-days: 30
    encryption-key: ${BACKUP_ENCRYPTION_KEY}
    enabled: true
```

### 2. 数据恢复服务 (DataRecoveryService)

**功能描述**: 提供数据库和文件的恢复功能

**主要特性**:
- ✅ 完全恢复（恢复整个数据库）
- ✅ 时间点恢复（恢复到指定时间点）
- ✅ 选择性恢复（恢复指定表）
- ✅ 恢复前提条件验证
- ✅ 恢复操作审计记录

**使用方式**:
```bash
# 验证恢复前提条件
POST /api/admin/data-recovery/validate
{
  "recoveryType": "FULL",
  "backupFilePath": "/path/to/backup.enc"
}

# 执行完全恢复
POST /api/admin/data-recovery/full
{
  "backupFilePath": "/path/to/backup.enc"
}

# 执行时间点恢复
POST /api/admin/data-recovery/point-in-time
{
  "targetDateTime": "2025-01-06T10:30:00"
}

# 执行选择性恢复
POST /api/admin/data-recovery/selective
{
  "backupFilePath": "/path/to/backup.enc",
  "tablesToRestore": ["users", "posts"]
}
```

### 3. 文件完整性服务 (FileIntegrityService)

**功能描述**: 监控关键文件完整性，检测篡改

**主要特性**:
- ✅ 定时完整性检查（每小时）
- ✅ 关键配置文件监控
- ✅ 文件篡改自动检测
- ✅ 文件备份和恢复
- ✅ 完整性告警通知

**使用方式**:
```bash
# 查看完整性统计
GET /api/admin/file-integrity/statistics

# 手动触发完整性检查
POST /api/admin/file-integrity/check

# 检查特定文件
POST /api/admin/file-integrity/check-file?filePath=/path/to/file

# 创建文件备份
POST /api/admin/file-integrity/backup?filePath=/path/to/file

# 尝试文件恢复
POST /api/admin/file-integrity/recover?filePath=/path/to/file
```

**监控的关键文件**:
- `application.yml` - 主配置文件
- `application-security.yml` - 安全配置文件
- `logback-spring.xml` - 日志配置文件
- `keystore.p12` - 密钥库文件

### 4. 会话清理服务 (SessionCleanupService)

**功能描述**: 清理用户会话和临时数据

**主要特性**:
- ✅ 用户退出时会话清理
- ✅ 会话超时自动清理（每5分钟）
- ✅ Redis缓存数据清理
- ✅ 临时数据清理
- ✅ 系统重启时数据清理

**使用方式**:
```bash
# 查看会话统计
GET /api/admin/security-services/statistics

# 手动清理过期会话
POST /api/admin/security-services/session/cleanup

# 强制清理用户会话（管理员操作）
# 通过代码调用: sessionCleanupService.forceCleanupUserSessions(userId, operatorUserId)
```

**自动清理的数据类型**:
- Spring Session会话数据
- 认证令牌（JWT、OAuth等）
- CSRF令牌
- 临时上传文件
- 验证码缓存
- 用户权限缓存

### 5. 数据彻底删除服务 (DataDeletionService)

**功能描述**: 提供数据的彻底删除功能，确保删除的数据无法恢复

**主要特性**:
- ✅ 用户注销时数据完全清理
- ✅ 帖子删除的级联清理
- ✅ 评论删除的级联清理
- ✅ 数据库记录物理删除
- ✅ 缓存数据清理
- ✅ 删除确认机制

**使用方式**:
```bash
# 生成删除确认令牌
# 通过代码调用: dataDeletionService.generateDeletionConfirmationToken(userId, operationType, resourceId)

# 发送删除确认邮件
# 通过代码调用: dataDeletionService.sendDeletionConfirmationEmail(userId, operationType, resourceId)

# 彻底删除用户数据
# 通过代码调用: dataDeletionService.deleteUserCompletely(userId, confirmationToken, operatorUserId)

# 彻底删除帖子数据
# 通过代码调用: dataDeletionService.deletePostCompletely(postId, confirmationToken, operatorUserId)

# 用户账户注销
# 通过代码调用: dataDeletionService.deactivateUserAccount(userId, confirmationToken)
```

### 6. 用户数据管理服务 (UserDataManagementService)

**功能描述**: 管理用户个人数据的查看、导出、修改和删除

**主要特性**:
- ✅ 用户数据查看（带权限控制）
- ✅ 数据脱敏显示
- ✅ 数据导出（JSON/CSV格式）
- ✅ 数据修改（带验证和审计）
- ✅ 数据删除（物理删除）
- ✅ 完整的操作审计记录

**使用方式**:
```bash
# 查看用户数据
GET /api/security/user-data/{userId}

# 导出用户数据（JSON格式）
GET /api/security/user-data/{userId}/export/json

# 导出用户数据（CSV格式）
GET /api/security/user-data/{userId}/export/csv

# 修改用户数据
PUT /api/security/user-data/{userId}
{
  "avatarUrl": "https://example.com/avatar.jpg",
  "bio": "新的个人简介",
  "email": "new@example.com",  // 仅管理员可修改
  "role": "ADMIN"              // 仅管理员可修改
}

# 删除用户数据
DELETE /api/security/user-data/{userId}

# 获取数据管理信息
GET /api/security/user-data/{userId}/info
```

## 统一管理界面

### 安全服务管理控制器 (SecurityServicesController)

提供统一的安全服务管理界面：

```bash
# 查看所有服务状态
GET /api/admin/security-services/status

# 执行健康检查
POST /api/admin/security-services/health-check

# 查看服务统计信息
GET /api/admin/security-services/statistics

# 手动触发备份
POST /api/admin/security-services/backup/trigger

# 手动触发文件完整性检查
POST /api/admin/security-services/integrity/check

# 清理过期会话
POST /api/admin/security-services/session/cleanup
```

## 系统启动检查

系统启动时会自动执行安全服务检查，确保所有服务正常运行：

```
=== 开始安全服务启动检查 ===
✓ 备份服务启动成功，当前备份数量: 5
✓ 数据恢复服务启动成功，可用备份数量: 5
✓ 文件完整性服务启动成功，监控文件数量: 12, 启用状态: true
✓ 会话清理服务启动成功，当前活跃会话数量: 3
✓ 数据删除服务启动成功
✓ 用户数据管理服务启动成功
=== 安全服务启动检查完成 ===
🎉 所有安全服务启动成功！系统已准备好投入使用。
```

## 权限要求

所有安全管理API都需要管理员权限：
- 用户必须具有 `ROLE_ADMIN` 角色
- 用户数据管理API支持用户本人访问自己的数据

## 审计日志

所有安全操作都会记录详细的审计日志：
- 操作用户
- 操作时间
- 操作类型
- 操作结果
- 详细描述
- IP地址

## 配置建议

### 生产环境配置

```yaml
# 备份配置
app:
  backup:
    path: /var/myweb/backups
    retention-days: 90
    encryption-key: ${BACKUP_ENCRYPTION_KEY}  # 使用环境变量
    enabled: true

# 文件完整性配置
app:
  file-integrity:
    enabled: true
    critical-files-path: src/main/resources
    hash-storage-path: /var/myweb/file-hashes
    backup-path: /var/myweb/file-backups
    alert-enabled: true

# 会话配置
spring:
  session:
    timeout: 30m
    redis:
      cleanup-cron: "0 */5 * * * *"  # 每5分钟清理一次
```

### 安全建议

1. **备份加密密钥管理**:
   - 使用强随机密钥
   - 定期轮换密钥
   - 安全存储密钥（如使用密钥管理服务）

2. **文件完整性监控**:
   - 定期检查关键文件
   - 及时响应完整性告警
   - 保持文件备份的完整性

3. **会话管理**:
   - 设置合理的会话超时时间
   - 定期清理过期会话
   - 监控异常会话活动

4. **数据删除**:
   - 确保重要操作有确认机制
   - 记录所有删除操作的审计日志
   - 定期检查删除操作的合规性

## 故障排除

### 常见问题

1. **备份失败**:
   - 检查磁盘空间
   - 验证数据库连接
   - 检查备份目录权限

2. **文件完整性检查失败**:
   - 检查文件路径配置
   - 验证文件权限
   - 检查哈希存储目录

3. **会话清理异常**:
   - 检查Redis连接
   - 验证会话配置
   - 检查清理任务调度

### 日志查看

```bash
# 查看安全服务日志
tail -f logs/application.log | grep -E "(BackupService|DataRecoveryService|FileIntegrityService|SessionCleanupService|DataDeletionService|UserDataManagementService)"

# 查看审计日志
tail -f logs/audit.log

# 查看错误日志
tail -f logs/error.log | grep -i security
```

## 合规性说明

本系统的安全服务完全符合GB/T 22239-2019二级等保要求：

- ✅ **身份鉴别**: 用户认证和权限管理
- ✅ **访问控制**: 基于角色的访问控制
- ✅ **安全审计**: 完整的操作审计记录
- ✅ **入侵防范**: XSS、CSRF、SQL注入防护
- ✅ **恶意代码防范**: 文件上传安全检查
- ✅ **数据完整性**: 文件完整性监控
- ✅ **数据保密性**: 数据加密存储
- ✅ **数据备份恢复**: 自动备份和恢复机制
- ✅ **剩余信息保护**: 数据彻底删除和会话清理

## 联系支持

如有任何问题或需要技术支持，请联系：
- 系统管理员
- 安全团队
- 技术支持团队

---

**版本**: v1.0  
**更新日期**: 2025年1月6日  
**适用系统**: MyWeb博客系统 v2.0+