# 访问频率限制与审计系统集成

## 概述

本文档描述了访问频率限制功能与现有审计系统（AuditAspect）的集成实现。

## 集成架构

### 核心组件

1. **RateLimitingService** - 访问频率限制核心服务
2. **RateLimitingFilter** - 访问频率限制过滤器
3. **RateLimitAlertService** - 访问频率告警服务
4. **AuditLogService** - 现有的审计日志服务

### 集成方式

访问频率限制功能通过以下方式与审计系统集成：

#### 1. 安全事件记录

当检测到访问频率超限时，系统会通过 `AuditLogService.logSecurityEvent()` 方法记录安全事件：

```java
auditLogService.logSecurityEvent(
    AuditOperation.SUSPICIOUS_ACTIVITY,
    username != null ? username : "anonymous",
    String.format("访问频率超限: IP=%s, URI=%s, 限制=%d/%ds", 
                clientIp, uri, limit.getMaxRequests(), limit.getWindowSizeSeconds())
);
```

#### 2. 告警事件记录

当访问频率接近限制阈值时，系统会记录预警事件：

```java
auditLogService.logSecurityEvent(
    AuditOperation.SUSPICIOUS_ACTIVITY,
    username != null ? username : "anonymous",
    String.format("访问频率预警: IP=%s, URI=%s, 当前=%d, 限制=%d, 比例=%.2f%%",
                clientIp, uri, currentCount, maxRequests, ratio * 100)
);
```

## 事件类型

所有访问频率相关的安全事件都使用 `AuditOperation.SUSPICIOUS_ACTIVITY` 类型，通过事件描述来区分具体的事件类型：

- **访问频率超限** - 当用户访问频率超过配置限制时触发
- **访问频率预警** - 当用户访问频率接近限制阈值时触发
- **访问频率告警** - 当系统检测到异常访问模式时触发

## 消息队列集成

通过现有的 `AuditLogService`，访问频率限制事件会自动：

1. 记录到数据库中的审计日志表
2. 通过 RabbitMQ 发送到安全事件队列
3. 触发相应的安全事件处理流程

## 配置说明

### 访问频率限制配置

```yaml
app:
  rate-limit:
    enabled: true
    default-limit:
      window-size-seconds: 60
      max-requests: 100
      limit-type: IP
    
    endpoints:
      "/users/login":
        window-size-seconds: 300
        max-requests: 5
        limit-type: IP
        enabled: true
        description: "登录接口限制"
    
    alert:
      enabled: true
      threshold: 0.8
      interval-minutes: 5
```

### 审计事件配置

访问频率限制事件会自动使用现有的审计系统配置，包括：

- 数据库存储配置
- RabbitMQ 消息队列配置
- 安全事件处理配置

## 监控和告警

### 实时监控

通过 `/api/admin/rate-limit/status` 接口可以实时查看访问频率限制状态。

### 统计信息

通过 `/api/admin/rate-limit/stats` 接口可以查看访问频率限制统计信息。

### 告警趋势

通过 `/api/admin/rate-limit/alerts/trend` 接口可以查看告警趋势数据。

## 安全考虑

1. **异常处理** - 当 Redis 或审计系统异常时，访问频率限制不会阻止正常业务流程
2. **性能优化** - 安全事件记录采用异步方式，不影响请求响应时间
3. **数据脱敏** - 敏感信息在审计日志中会被适当脱敏处理

## 测试

系统包含完整的单元测试和集成测试：

- `RateLimitingServiceTest` - 核心服务单元测试
- `RateLimitingServiceIntegrationTest` - 与审计系统的集成测试
- `RateLimitingFilterIntegrationTest` - 过滤器集成测试

## 部署注意事项

1. 确保 Redis 服务正常运行
2. 确保 RabbitMQ 服务正常运行
3. 确保数据库中有相应的审计日志表
4. 根据实际业务需求调整访问频率限制配置