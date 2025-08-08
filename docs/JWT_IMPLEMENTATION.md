# JWT令牌服务实现文档

## 概述

本文档描述了MyWeb系统中JWT（JSON Web Token）令牌服务的实现，用于替代传统的JSESSIONID会话管理机制。

## 架构设计

### 核心组件

1. **JwtService** - JWT令牌的基础操作服务
2. **JwtTokenService** - JWT令牌的高级管理服务
3. **JwtAuthenticationFilter** - JWT认证过滤器
4. **AuthenticationService** - 集成JWT的认证服务

### 令牌类型

- **访问令牌（Access Token）**: 用于API访问，默认有效期1小时
- **刷新令牌（Refresh Token）**: 用于刷新访问令牌，默认有效期7天

## 主要功能

### 1. 令牌生成

```java
// 生成令牌对
TokenPair tokenPair = jwtTokenService.generateTokenPair(user);

// 访问令牌
String accessToken = tokenPair.getAccessToken();

// 刷新令牌  
String refreshToken = tokenPair.getRefreshToken();
```

### 2. 令牌验证

```java
// 验证令牌有效性
boolean isValid = jwtTokenService.validateToken(token);

// 检查令牌类型
boolean isAccessToken = jwtService.isAccessToken(token);
boolean isRefreshToken = jwtService.isRefreshToken(token);
```

### 3. 令牌刷新

```java
// 刷新访问令牌
TokenPair newTokenPair = jwtTokenService.refreshToken(refreshToken, user);
```

### 4. 令牌撤销

```java
// 撤销用户所有令牌
jwtTokenService.revokeAllUserTokens(userId);

// 撤销特定令牌
jwtTokenService.revokeToken(token);
```

## API接口

### 用户登录

```http
POST /users/login
Content-Type: application/json

{
    "username": "user@example.com",
    "password": "password123",
    "code": "123456"
}
```

**响应:**
```json
{
    "id": 1,
    "username": "testuser",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

### 刷新令牌

```http
POST /users/refresh-token
Content-Type: application/json

{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**响应:**
```json
{
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

### 用户登出

```http
POST /users/logout
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

## 客户端使用

### 1. 存储令牌

```javascript
// 登录成功后存储令牌
const loginResponse = await fetch('/users/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        username: 'user@example.com',
        password: 'password123'
    })
});

const data = await loginResponse.json();
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);
```

### 2. 发送请求

```javascript
// 在请求头中包含访问令牌
const response = await fetch('/api/protected-resource', {
    headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
});
```

### 3. 自动刷新令牌

```javascript
// 拦截器示例（axios）
axios.interceptors.response.use(
    response => response,
    async error => {
        if (error.response?.status === 401) {
            try {
                const refreshToken = localStorage.getItem('refreshToken');
                const refreshResponse = await axios.post('/users/refresh-token', {
                    refreshToken
                });
                
                const { accessToken } = refreshResponse.data;
                localStorage.setItem('accessToken', accessToken);
                
                // 重试原请求
                error.config.headers.Authorization = `Bearer ${accessToken}`;
                return axios.request(error.config);
            } catch (refreshError) {
                // 刷新失败，跳转到登录页
                localStorage.clear();
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);
```

## 安全特性

### 1. 令牌黑名单

- 用户登出时，令牌被加入黑名单
- 黑名单令牌存储在Redis中，自动过期清理

### 2. 用户会话管理

- 每个用户的活跃令牌存储在Redis中
- 支持强制下线功能

### 3. 令牌签名

- 使用HS512算法签名
- 密钥可通过环境变量配置

### 4. 令牌内容

```json
{
    "sub": "username",
    "userId": 123,
    "username": "testuser",
    "role": "USER",
    "tokenType": "access",
    "email": "user@example.com",
    "iss": "MyWeb",
    "iat": 1640995200,
    "exp": 1640998800
}
```

## 配置说明

### application.yml

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
    access-token-expiration: 3600  # 1小时
    refresh-token-expiration: 604800  # 7天
    issuer: MyWeb
    enabled: true
```

### 环境变量

- `JWT_SECRET`: JWT签名密钥
- `JWT_ACCESS_TOKEN_EXPIRATION`: 访问令牌过期时间（秒）
- `JWT_REFRESH_TOKEN_EXPIRATION`: 刷新令牌过期时间（秒）

## Redis键结构

```
# 用户活跃令牌
auth:user:tokens:{userId}

# 黑名单令牌
auth:jwt:blacklist:{tokenHash}
```

## 错误处理

### 常见错误码

- `401 Unauthorized`: 令牌无效或已过期
- `403 Forbidden`: 权限不足
- `400 Bad Request`: 请求参数错误

### 错误响应格式

```json
{
    "error": "Unauthorized",
    "message": "访问此资源需要身份认证",
    "path": "/api/protected-resource",
    "timestamp": 1640995200000
}
```

## 性能优化

### 1. Redis缓存

- 令牌验证结果缓存
- 用户信息缓存
- 黑名单快速查询

### 2. 异步处理

- 令牌生成异步化
- 审计日志异步记录

### 3. 批量操作

- 批量撤销令牌
- 批量清理过期数据

## 监控和审计

### 1. 日志记录

- 令牌生成/验证日志
- 认证成功/失败日志
- 安全事件日志

### 2. 指标监控

- 令牌生成速率
- 认证成功率
- 令牌刷新频率

## 最佳实践

### 1. 安全建议

- 使用HTTPS传输令牌
- 定期轮换JWT密钥
- 设置合理的令牌过期时间
- 实施令牌泄露检测

### 2. 开发建议

- 统一错误处理
- 实现自动令牌刷新
- 添加令牌预刷新机制
- 实现优雅的登出流程

## 故障排查

### 1. 常见问题

**问题**: 令牌验证失败
**解决**: 检查令牌格式、签名密钥、过期时间

**问题**: 刷新令牌失败
**解决**: 检查刷新令牌有效性、用户状态

**问题**: Redis连接失败
**解决**: 检查Redis配置、网络连接

### 2. 调试工具

- JWT.io - 在线令牌解析
- Redis CLI - 查看缓存数据
- 应用日志 - 查看详细错误信息

## 升级指南

### 从Session到JWT

1. 更新前端代码，使用Bearer令牌
2. 配置JWT相关参数
3. 部署新版本应用
4. 验证功能正常
5. 清理旧的Session数据

### 版本兼容性

- 支持渐进式迁移
- 保持API向后兼容
- 提供配置开关