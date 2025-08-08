# JWT前端集成实现

## 概述

本文档描述了将前端认证从localStorage userId模式迁移到JWT令牌模式的实现。

## 问题描述

原有系统存在以下问题：
1. 前端只存储用户ID在localStorage中
2. 应用重启后，localStorage中的userId仍然存在，但后端JWT令牌已失效
3. 导致前端认为用户已登录，但后端API调用失败

## 解决方案

### 1. 创建JWT认证工具类

创建了 `auth-utils.js` 工具类，提供统一的JWT令牌管理：

```javascript
class AuthUtils {
    // 获取访问令牌
    static getAccessToken()
    
    // 检查用户是否已登录
    static isLoggedIn()
    
    // 自动刷新令牌的认证请求
    static authenticatedFetch(url, options)
    
    // 退出登录
    static logout()
    
    // 清除认证信息
    static clearAuth()
}
```

### 2. 更新登录流程

修改 `login.html` 中的登录成功处理：

```javascript
// 原来只存储userId
localStorage.setItem('userId', data.id);

// 现在存储完整的JWT信息
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);
localStorage.setItem('userId', data.id);
localStorage.setItem('username', data.username);
localStorage.setItem('tokenType', data.tokenType || 'Bearer');
localStorage.setItem('expiresIn', data.expiresIn);
```

### 3. 更新所有JavaScript文件

将所有API调用从普通fetch改为使用JWT认证：

```javascript
// 原来
const response = await fetch('/api/endpoint', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
});

// 现在
const response = await AuthUtils.authenticatedFetch('/api/endpoint', {
    method: 'POST',
    body: JSON.stringify(data)
});
```

### 4. 自动令牌刷新

`AuthUtils.authenticatedFetch()` 自动处理令牌刷新：

1. 检查令牌是否过期
2. 如果过期，自动调用刷新接口
3. 更新localStorage中的令牌
4. 重试原始请求
5. 如果刷新失败，跳转到登录页

### 5. 兼容性处理

在登录页面添加了旧数据清理逻辑：

```javascript
// 清理旧的认证信息（如果只有userId而没有JWT令牌）
const userId = localStorage.getItem('userId');
const accessToken = localStorage.getItem('accessToken');

if (userId && !accessToken) {
    console.log('检测到旧的认证信息，清理中...');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
}
```

## 修改的文件

### JavaScript文件
- `website_core/src/main/resources/static/js/auth-utils.js` (新建)
- `website_core/src/main/resources/static/js/index.js`
- `website_core/src/main/resources/static/js/profile.js`
- `website_core/src/main/resources/static/js/post-edit.js`
- `website_core/src/main/resources/static/js/post-detail.js`
- `website_core/src/main/resources/static/js/search.js`

### HTML模板
- `website_core/src/main/resources/templates/login.html`
- `website_core/src/main/resources/templates/index.html`
- `website_core/src/main/resources/templates/profile.html`
- `website_core/src/main/resources/templates/post-edit.html`
- `website_core/src/main/resources/templates/post-detail.html`
- `website_core/src/main/resources/templates/search.html`

## 主要特性

### 1. 统一认证管理
- 所有JWT相关操作集中在AuthUtils类中
- 统一的错误处理和重试机制

### 2. 自动令牌刷新
- 检测到401错误时自动刷新令牌
- 刷新失败时自动跳转到登录页

### 3. 安全性增强
- 令牌过期检查
- 自动清理无效认证信息
- 防止未认证的API调用

### 4. 向后兼容
- 保持userId的使用以兼容现有代码
- 自动清理旧的认证数据

## 使用方法

### 1. 检查登录状态
```javascript
if (AuthUtils.isLoggedIn()) {
    // 用户已登录
}
```

### 2. 发送认证请求
```javascript
try {
    const response = await AuthUtils.authenticatedFetch('/api/protected', {
        method: 'POST',
        body: JSON.stringify(data)
    });
    const result = await response.json();
} catch (error) {
    console.error('请求失败:', error);
}
```

### 3. 退出登录
```javascript
AuthUtils.logout(); // 自动清理本地数据并跳转
```

### 4. 页面初始化
```javascript
document.addEventListener('DOMContentLoaded', async function() {
    // 初始化认证状态
    await AuthUtils.initAuth();
    
    // 其他初始化代码...
});
```

## 测试验证

1. 登录后检查localStorage是否包含所有JWT相关数据
2. 重启应用后访问需要认证的页面，验证是否正常工作
3. 令牌过期时验证自动刷新机制
4. 刷新令牌过期时验证自动跳转到登录页

## 注意事项

1. 所有需要认证的API调用都应使用`AuthUtils.authenticatedFetch()`
2. 公开API（如获取帖子列表、搜索等）仍使用普通fetch
3. FormData上传时需要特殊处理，不设置Content-Type头
4. 页面加载时应调用`AuthUtils.initAuth()`进行认证状态检查