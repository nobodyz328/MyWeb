# 用户数据管理功能文档

## 概述

用户数据管理功能是MyWeb博客系统安全增强项目的一部分，符合GB/T 22239-2019二级等保要求的个人信息保护机制。该功能提供了完整的用户个人数据管理能力，包括数据查看、导出、修改和删除功能。

## 功能特性

### 1. 数据查看功能
- 用户可以查看自己的完整个人数据
- 管理员可以查看任何用户的数据
- 自动进行数据脱敏处理（非本人或管理员查看时）
- 完整的权限控制和审计记录

### 2. 数据导出功能
- 支持JSON和CSV两种格式导出
- 用户可以导出自己的完整数据
- 管理员可以导出任何用户的数据
- 导出文件包含时间戳和完整性标识
- 自动添加UTF-8 BOM以支持Excel正确显示中文

### 3. 数据修改功能
- 用户可以修改基本信息（头像、个人简介）
- 管理员可以修改敏感信息（邮箱、角色）
- 完整的数据验证和格式检查
- 修改前后数据对比和审计记录

### 4. 数据删除功能
- 支持用户数据的物理删除
- 用户可以删除自己的账户数据
- 管理员可以删除任何用户的数据
- 删除前记录完整的用户信息用于审计

### 5. 安全特性
- 基于Spring Security的权限控制
- 完整的操作审计日志记录
- 敏感信息自动脱敏处理
- 数据完整性验证
- 异步处理提高性能

## API接口

### 基础路径
```
/api/security/user-data
```

### 1. 查看用户数据
```http
GET /api/security/user-data/{userId}
```

**权限要求**: 用户本人或管理员

**响应示例**:
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "avatarUrl": "http://example.com/avatar.jpg",
  "bio": "用户个人简介",
  "likedCount": 10,
  "followersCount": 5,
  "followingCount": 3,
  "emailVerified": true,
  "role": "USER",
  "createdAt": "2024-12-01 10:00:00",
  "updatedAt": "2025-01-01 15:30:00",
  "exportTime": "2025-01-06 21:00:00",
  "dataVersion": "1.0"
}
```

### 2. 导出用户数据（JSON格式）
```http
GET /api/security/user-data/{userId}/export/json
```

**权限要求**: 用户本人或管理员

**响应**: 下载JSON文件

### 3. 导出用户数据（CSV格式）
```http
GET /api/security/user-data/{userId}/export/csv
```

**权限要求**: 用户本人或管理员

**响应**: 下载CSV文件

### 4. 修改用户数据
```http
PUT /api/security/user-data/{userId}
```

**权限要求**: 用户本人或管理员

**请求体示例**:
```json
{
  "avatarUrl": "http://example.com/new-avatar.jpg",
  "bio": "更新后的个人简介",
  "email": "newemail@example.com",
  "role": "MODERATOR",
  "updateReason": "用户自主更新"
}
```

**注意**: 
- 普通用户只能修改 `avatarUrl` 和 `bio`
- 管理员可以修改所有字段包括 `email` 和 `role`

### 5. 删除用户数据
```http
DELETE /api/security/user-data/{userId}
```

**权限要求**: 用户本人或管理员

**响应**: 删除成功确认消息

### 6. 获取数据管理信息
```http
GET /api/security/user-data/{userId}/info
```

**权限要求**: 用户本人或管理员

**响应示例**:
```json
{
  "userId": 1,
  "dataProtectionLevel": "GB/T 22239-2019 二级等保",
  "supportedExportFormats": ["JSON", "CSV"],
  "dataRetentionPolicy": "用户数据永久保存，直到用户主动删除",
  "privacyRights": [
    "数据查看权",
    "数据导出权", 
    "数据修改权",
    "数据删除权"
  ],
  "contactInfo": "如有数据保护相关问题，请联系系统管理员"
}
```

## 数据脱敏规则

### 邮箱脱敏
- 原始邮箱: `user@example.com`
- 脱敏后: `u***r@example.com`
- 规则: 保留首尾字符，中间用`***`替代

### 敏感字段处理
非本人或管理员查看时，以下字段将被隐藏或脱敏：
- `emailVerified`: 设为 `null`
- `loginAttempts`: 设为 `null`
- `lastLoginTime`: 设为 `null`
- `lastLoginIp`: 设为 `null`
- `accountLocked`: 设为 `null`
- `role`: 显示为 `"USER"`

## 审计日志

所有用户数据管理操作都会记录详细的审计日志，包括：

### 记录的操作类型
- `PROFILE_UPDATE`: 查看/修改用户数据
- `DATA_EXPORT`: 导出用户数据
- `USER_DELETE`: 删除用户数据

### 审计日志内容
- 操作用户ID和用户名
- 目标用户ID
- 操作类型和描述
- 客户端IP地址
- 操作结果（成功/失败）
- 错误信息（如有）
- 操作时间戳
- 修改前后数据对比（修改操作）

## 安全考虑

### 权限控制
- 使用Spring Security的`@PreAuthorize`注解进行方法级权限控制
- 用户只能操作自己的数据
- 管理员可以操作任何用户的数据
- 未认证用户无法访问任何接口

### 数据验证
- 所有输入数据都经过严格验证
- 邮箱格式验证
- 字段长度限制
- 角色枚举值验证
- XSS和SQL注入防护

### 异步处理
- 所有数据操作都使用`CompletableFuture`异步处理
- 避免阻塞主线程
- 提高系统响应性能

### 错误处理
- 完善的异常处理机制
- 用户友好的错误消息
- 详细的错误日志记录
- 不暴露敏感系统信息

## 使用示例

### 前端JavaScript调用示例

```javascript
// 查看用户数据
async function viewUserData(userId) {
    try {
        const response = await fetch(`/api/security/user-data/${userId}`, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken(),
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            const userData = await response.json();
            console.log('用户数据:', userData);
            return userData;
        } else {
            throw new Error('获取用户数据失败');
        }
    } catch (error) {
        console.error('错误:', error);
    }
}

// 导出用户数据
async function exportUserData(userId, format = 'json') {
    try {
        const response = await fetch(`/api/security/user-data/${userId}/export/${format}`, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken()
            }
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `user_data_${userId}_${new Date().getTime()}.${format}`;
            a.click();
            window.URL.revokeObjectURL(url);
        } else {
            throw new Error('导出用户数据失败');
        }
    } catch (error) {
        console.error('错误:', error);
    }
}

// 修改用户数据
async function updateUserData(userId, updateData) {
    try {
        const response = await fetch(`/api/security/user-data/${userId}`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken(),
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify(updateData)
        });
        
        if (response.ok) {
            const updatedData = await response.json();
            console.log('用户数据更新成功:', updatedData);
            return updatedData;
        } else {
            const errorText = await response.text();
            throw new Error(errorText);
        }
    } catch (error) {
        console.error('错误:', error);
    }
}

// 删除用户数据
async function deleteUserData(userId) {
    if (!confirm('确定要删除用户数据吗？此操作不可恢复！')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/security/user-data/${userId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken(),
                'X-CSRF-TOKEN': getCsrfToken()
            }
        });
        
        if (response.ok) {
            const message = await response.text();
            console.log('删除成功:', message);
            return true;
        } else {
            const errorText = await response.text();
            throw new Error(errorText);
        }
    } catch (error) {
        console.error('错误:', error);
        return false;
    }
}
```

## 配置说明

### 必要的依赖
确保项目中包含以下依赖：
- Spring Boot Security
- Spring Boot Web
- Spring Boot Data JPA
- Jackson (JSON处理)
- Validation API

### 权限配置
在Spring Security配置中添加相应的权限规则：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/security/user-data/**").authenticated()
            // 其他配置...
        );
        return http.build();
    }
}
```

## 监控和维护

### 性能监控
- 监控API响应时间
- 监控数据导出文件大小
- 监控并发用户数量

### 日志监控
- 监控审计日志记录情况
- 监控错误日志和异常
- 监控权限拒绝事件

### 定期维护
- 定期清理过期的临时文件
- 定期检查审计日志完整性
- 定期更新安全策略

## 故障排除

### 常见问题

1. **权限被拒绝**
   - 检查用户是否已登录
   - 检查用户是否有相应权限
   - 检查Spring Security配置

2. **数据导出失败**
   - 检查文件系统权限
   - 检查内存使用情况
   - 检查网络连接

3. **数据修改失败**
   - 检查输入数据格式
   - 检查数据验证规则
   - 检查数据库连接

### 日志级别配置
```yaml
logging:
  level:
    com.myweb.website_core.application.service.security.UserDataManagementService: INFO
    com.myweb.website_core.interfaces.controller.security.UserDataManagementController: INFO
```

## 版本历史

- **v1.0** (2025-01-06): 初始版本，实现基本的用户数据管理功能
  - 数据查看、导出、修改、删除功能
  - 权限控制和审计日志
  - 数据脱敏和安全防护

---

**注意**: 本功能涉及用户隐私数据，请严格按照相关法律法规和公司政策使用。