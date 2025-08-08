# TOTP和用户设置功能实现文档

## 概述

本文档描述了在MySteam社区系统中实现的TOTP二次验证和用户设置功能，包括个人信息管理、密码修改、邮箱绑定以及管理员访问控制等功能。

## 功能特性

### 1. 用户设置管理

#### 个人信息设置
- 修改个人简介（bio）
- 更新头像URL
- 实时保存和验证

#### 密码修改
- 当前密码验证
- 新密码强度检查
- TOTP验证（如果已启用）
- 符合GB/T 22239-2019密码策略要求

#### 邮箱绑定
- 邮箱验证码发送
- 邮箱唯一性检查
- TOTP验证（如果已启用）
- 邮箱验证状态管理

### 2. TOTP二次验证

#### TOTP设置功能
- 生成TOTP密钥
- 二维码生成（Google Authenticator兼容）
- 手动密钥输入支持
- 验证码测试功能

#### TOTP管理功能
- 启用/禁用TOTP
- 重置TOTP密钥
- 验证码验证
- 时间窗口管理

#### 管理员强制策略
- 管理员账户必须启用TOTP
- 访问管理界面需要TOTP验证
- 修改敏感信息需要TOTP验证

### 3. 管理员访问控制

#### 访问验证
- 角色权限检查
- TOTP验证要求
- 访问状态反馈

#### 管理界面
- 简单的管理面板
- 功能模块占位
- 安全提醒和操作指南

## 技术实现

### 后端架构

#### 服务层
1. **TOTPManagementService** - TOTP管理服务
   - 密钥生成和管理
   - 二维码生成
   - 验证码验证
   - 状态管理

2. **UserSettingsService** - 用户设置服务
   - 个人信息管理
   - 密码修改
   - 邮箱绑定
   - 管理员访问验证

3. **EmailVerificationService** - 邮箱验证服务（扩展）
   - 添加邮箱绑定验证码功能
   - 支持多种验证场景

#### 控制器层
1. **UserSettingsController** - 用户设置控制器
   - RESTful API设计
   - 权限验证
   - 审计日志记录

2. **AdminController** - 管理界面控制器
   - 管理界面访问控制
   - 权限验证

#### 数据层
- 扩展UserRepository支持邮箱唯一性检查
- 利用现有User实体的TOTP字段

### 前端实现

#### 用户界面
- 响应式设计
- Steam风格界面
- 实时状态反馈
- 表单验证

#### JavaScript功能
- 异步API调用
- 状态管理
- 错误处理
- 用户体验优化

#### CSS样式
- 现代化界面设计
- 动画效果
- 响应式布局
- 高对比度支持

## 安全特性

### 1. 身份验证
- TOTP时间窗口验证
- 容错机制（±30秒）
- 验证码格式检查
- 防重放攻击

### 2. 访问控制
- 用户权限验证
- 管理员强制TOTP
- 敏感操作二次验证
- 会话管理

### 3. 数据保护
- 密码哈希存储
- TOTP密钥安全存储
- 邮箱验证码时效性
- 审计日志记录

### 4. 合规性
- 符合GB/T 22239-2019二级等保要求
- 身份鉴别机制
- 访问控制机制
- 安全审计机制

## API接口

### 用户设置接口

```
GET    /users/{userId}/settings              - 获取用户设置
PUT    /users/{userId}/settings/basic        - 更新基本信息
PUT    /users/{userId}/settings/password     - 修改密码
POST   /users/{userId}/settings/email/send-code - 发送邮箱验证码
POST   /users/{userId}/settings/email/bind   - 绑定邮箱
```

### TOTP接口

```
GET    /users/{userId}/settings/totp         - 获取TOTP设置
GET    /users/{userId}/settings/totp/status  - 获取TOTP状态
GET    /users/{userId}/settings/totp/qrcode  - 获取TOTP二维码
POST   /users/{userId}/settings/totp/enable  - 启用TOTP
POST   /users/{userId}/settings/totp/disable - 禁用TOTP
POST   /users/{userId}/settings/totp/reset   - 重置TOTP
POST   /users/{userId}/settings/totp/verify  - 验证TOTP
```

### 管理员接口

```
POST   /users/{userId}/settings/admin/check-access - 检查管理员访问
GET    /admin                                      - 管理界面
```

## 使用流程

### 1. 普通用户设置TOTP

1. 访问个人设置页面
2. 点击"设置二次验证"
3. 扫描二维码或手动输入密钥
4. 输入验证码启用TOTP
5. 可选择禁用（非管理员）

### 2. 管理员访问管理界面

1. 必须先启用TOTP
2. 在设置页面点击"进入管理界面"
3. 输入TOTP验证码
4. 验证成功后跳转到管理界面

### 3. 修改敏感信息

1. 修改密码或绑定邮箱
2. 如果启用了TOTP，需要输入验证码
3. 验证通过后执行操作

## 测试

### 单元测试
- UserSettingsControllerTest
- TOTPManagementServiceTest
- UserSettingsServiceTest

### 集成测试
- API接口测试
- 权限验证测试
- TOTP验证流程测试

### 安全测试
- 权限绕过测试
- TOTP时间窗口测试
- 会话管理测试

## 部署注意事项

### 1. 配置要求
- Redis缓存（用于验证码存储）
- 邮件服务配置
- TOTP时间同步

### 2. 安全配置
- HTTPS强制使用
- 会话安全配置
- CORS策略配置

### 3. 监控告警
- TOTP验证失败监控
- 管理员访问监控
- 异常操作告警

## 后续扩展

### 1. 功能扩展
- 备用验证码
- 硬件密钥支持
- 生物识别集成

### 2. 管理功能
- 用户管理界面
- 内容审核功能
- 系统监控面板

### 3. 安全增强
- 风险评估
- 行为分析
- 威胁检测

## 总结

本实现提供了完整的TOTP二次验证和用户设置管理功能，满足了现代Web应用的安全要求，特别是符合GB/T 22239-2019二级等保的相关要求。通过合理的架构设计和安全机制，为用户提供了安全、易用的设置管理体验。