# 开发者指南：常量管理和审计系统

## 概述

本指南介绍如何正确使用新的常量管理系统和审计注解，帮助开发者编写更规范、更安全的代码。

## 1. Redis键管理

### 1.1 基本使用

**❌ 错误方式 - 硬编码**
```java
// 不要这样做
String userKey = "user:profile:" + userId;
String sessionKey = "session:" + sessionId;
redisTemplate.opsForValue().set(userKey, userProfile);
```

**✅ 正确方式 - 使用RedisKey工具类**
```java
// 推荐做法
String userKey = RedisKey.userProfileCacheKey(userId);
String sessionKey = RedisKey.sessionKey(sessionId);
redisTemplate.opsForValue().set(userKey, userProfile);
```

### 1.2 常用Redis键方法

#### 用户相关
```java
// 用户资料缓存
String profileKey = RedisKey.userProfileCacheKey(userId);

// 用户点赞帖子
String likeKey = RedisKey.likeKey(postId, userId);

// 用户关注关系
String followKey = RedisKey.userFollowKey(userId, targetUserId);

// 用户统计信息
String statsKey = RedisKey.userStatsKey(userId);
```

#### 安全相关
```java
// 登录尝试次数
String attemptKey = RedisKey.loginAttemptKey(username);

// 账户锁定
String lockKey = RedisKey.accountLockKey(username);

// 频率限制
String rateLimitKey = RedisKey.rateLimitKey("api", userId.toString());

// 可疑IP
String suspiciousKey = RedisKey.suspiciousIpKey(ipAddress);
```

#### 搜索相关
```java
// 搜索缓存
String searchKey = RedisKey.searchCacheKey(query, "POST", params);

// 用户搜索历史
String historyKey = RedisKey.searchHistoryKey(userId);

// 热门搜索关键词
String hotKeywordsKey = RedisKey.HOT_SEARCH_KEYWORDS_KEY;
```

### 1.3 批量操作
```java
// 获取键的模式（用于批量删除等操作）
String pattern = RedisKey.getKeyPattern(RedisKey.USER_PROFILE_CACHE);
Set<String> keys = redisTemplate.keys(pattern);

// 构建复合键
String complexKey = RedisKey.buildKey("custom", "module", "feature", identifier);
```

## 2. 系统常量使用

### 2.1 时间格式
```java
// ❌ 错误方式
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

// ✅ 正确方式
DateTimeFormatter formatter = DateTimeFormatter.ofPattern(SystemConstants.STANDARD_DATETIME_FORMAT);
```

### 2.2 文件路径
```java
// ❌ 错误方式
String avatarPath = "uploads/avatars/" + fileName;

// ✅ 正确方式
String avatarPath = SystemConstants.AVATAR_UPLOAD_PATH + fileName;
```

### 2.3 消息格式化
```java
// ❌ 错误方式
String message = operation + "成功";

// ✅ 正确方式
String message = SystemConstants.formatSuccessMessage(operation);

// 失败消息
String errorMessage = SystemConstants.formatFailureMessage("用户注册", "邮箱已存在");

// 权限错误
String permissionError = SystemConstants.formatPermissionDeniedMessage("删除帖子");
```

### 2.4 敏感信息处理
```java
// 检查敏感参数
if (SystemConstants.isSensitiveParam(paramName)) {
    paramValue = SystemConstants.MASKED_VALUE;
}

// 限制字符串长度
String limitedText = SystemConstants.limitStringLength(longText, 100);

// 生成请求ID
String requestId = SystemConstants.generateRequestId();
```

## 3. 审计注解使用

### 3.1 基本用法

**✅ 推荐做法 - 使用@Auditable注解**
```java
@PostMapping("/posts")
@Auditable(
    operation = AuditOperation.POST_CREATE,
    resourceType = "POST",
    description = "创建新帖子"
)
public ResponseEntity<?> createPost(@RequestBody PostCreateRequest request) {
    // 业务逻辑
    Post post = postService.createPost(request);
    return ResponseEntity.ok(post);
}
```

**❌ 错误方式 - 手动记录审计日志**
```java
@PostMapping("/posts")
public ResponseEntity<?> createPost(@RequestBody PostCreateRequest request) {
    try {
        Post post = postService.createPost(request);
        
        // 不要手动记录审计日志
        auditLogService.logOperation(AuditLogRequest.builder()
            .operation(AuditOperation.POST_CREATE)
            .resourceType("POST")
            .description("创建新帖子")
            .build());
            
        return ResponseEntity.ok(post);
    } catch (Exception e) {
        // 手动记录失败日志也是不推荐的
        auditLogService.logOperation(AuditLogRequest.builder()
            .operation(AuditOperation.POST_CREATE)
            .result("FAILURE")
            .errorMessage(e.getMessage())
            .build());
        throw e;
    }
}
```

### 3.2 注解参数详解

```java
@Auditable(
    operation = AuditOperation.POST_UPDATE,           // 必需：操作类型
    resourceType = "POST",                            // 资源类型
    description = "编辑帖子内容",                      // 操作描述
    logRequest = true,                                // 是否记录请求参数（默认true）
    logResponse = true,                               // 是否记录响应结果（默认true）
    logExecutionTime = true,                          // 是否记录执行时间（默认true）
    riskLevel = 2,                                    // 风险级别（0使用默认值）
    tags = "content,user_generated",                  // 标签
    sensitiveParams = {1, 2},                         // 敏感参数索引（从0开始）
    async = true,                                     // 是否异步记录（默认true）
    maxParamLength = 500,                             // 最大参数长度
    maxResponseLength = 1000,                         // 最大响应长度
    ignoreAuditException = true                       // 是否忽略审计异常（默认true）
)
```

### 3.3 常见场景示例

#### 用户认证操作
```java
@PostMapping("/login")
@Auditable(
    operation = AuditOperation.USER_LOGIN_SUCCESS,
    resourceType = "USER",
    description = "用户登录",
    sensitiveParams = {0},  // 假设第一个参数包含密码
    riskLevel = 3
)
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // 登录逻辑
}
```

#### 文件上传操作
```java
@PostMapping("/upload")
@Auditable(
    operation = AuditOperation.FILE_UPLOAD,
    resourceType = "FILE",
    description = "上传文件",
    logResponse = false,  // 文件上传响应可能很大，不记录
    maxParamLength = 200  // 限制参数长度
)
public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file) {
    // 文件上传逻辑
}
```

#### 管理员操作
```java
@DeleteMapping("/admin/users/{id}")
@Auditable(
    operation = AuditOperation.USER_DELETE,
    resourceType = "USER",
    description = "管理员删除用户",
    riskLevel = 5,  // 高风险操作
    tags = "admin,user_management",
    async = false   // 重要操作同步记录
)
public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestParam String reason) {
    // 删除用户逻辑
}
```

#### 搜索操作
```java
@GetMapping("/search")
@Auditable(
    operation = AuditOperation.SEARCH_OPERATION,
    resourceType = "SEARCH",
    description = "搜索内容",
    riskLevel = 1,  // 低风险操作
    logResponse = false  // 搜索结果可能很大
)
public ResponseEntity<?> search(@RequestParam String query, @RequestParam String type) {
    // 搜索逻辑
}
```

## 4. 最佳实践

### 4.1 选择合适的AuditOperation
```java
// 根据具体操作选择最合适的枚举值
AuditOperation.POST_CREATE     // 创建帖子
AuditOperation.POST_UPDATE     // 编辑帖子
AuditOperation.POST_DELETE     // 删除帖子
AuditOperation.POST_LIKE       // 点赞帖子
AuditOperation.POST_REVIEW     // 审核帖子（管理员）

AuditOperation.FILE_UPLOAD     // 文件上传
AuditOperation.AVATAR_UPLOAD   // 头像上传（更具体）

AuditOperation.SEARCH_OPERATION     // 普通搜索
AuditOperation.ADVANCED_SEARCH      // 高级搜索
```

### 4.2 合理设置风险级别
```java
// 风险级别指导原则：
// 1 - 低风险：查看、搜索等只读操作
// 2 - 中低风险：创建内容、点赞等用户操作
// 3 - 中等风险：修改个人信息、文件上传等
// 4 - 中高风险：删除内容、账户操作等
// 5 - 高风险：管理员操作、系统配置等

@Auditable(operation = AuditOperation.POST_VIEW, riskLevel = 1)        // 查看帖子
@Auditable(operation = AuditOperation.POST_CREATE, riskLevel = 2)      // 创建帖子
@Auditable(operation = AuditOperation.FILE_UPLOAD, riskLevel = 3)      // 文件上传
@Auditable(operation = AuditOperation.POST_DELETE, riskLevel = 4)      // 删除帖子
@Auditable(operation = AuditOperation.USER_MANAGEMENT, riskLevel = 5)  // 用户管理
```

### 4.3 敏感参数处理
```java
// 标记包含敏感信息的参数索引
@Auditable(
    operation = AuditOperation.PASSWORD_CHANGED,
    sensitiveParams = {1, 2},  // 第2和第3个参数是密码
    description = "修改密码"
)
public ResponseEntity<?> changePassword(String username, String oldPassword, String newPassword) {
    // 修改密码逻辑
}
```

### 4.4 异步vs同步记录
```java
// 一般操作使用异步记录（默认）
@Auditable(operation = AuditOperation.POST_CREATE, async = true)

// 重要操作使用同步记录
@Auditable(
    operation = AuditOperation.USER_DELETE,
    async = false,  // 确保审计日志记录成功
    ignoreAuditException = false  // 审计失败时抛出异常
)
```

## 5. 迁移指南

### 5.1 替换硬编码Redis键
```java
// 查找项目中的硬编码Redis键
grep -r "\"[^\"]*:[^\"]*\"" --include="*.java" src/

// 逐步替换
// 旧代码
String key = "user:session:" + userId;

// 新代码
String key = RedisKey.userSessionKey(userId);
```

### 5.2 移除手动审计日志
```java
// 旧代码 - 手动记录
@PostMapping("/api/posts")
public ResponseEntity<?> createPost(@RequestBody PostRequest request) {
    try {
        Post post = postService.createPost(request);
        
        // 移除这些手动审计代码
        auditLogService.logOperation(AuditLogRequest.builder()
            .operation(AuditOperation.POST_CREATE)
            .userId(getCurrentUserId())
            .result("SUCCESS")
            .build());
            
        return ResponseEntity.ok(post);
    } catch (Exception e) {
        auditLogService.logOperation(AuditLogRequest.builder()
            .operation(AuditOperation.POST_CREATE)
            .result("FAILURE")
            .errorMessage(e.getMessage())
            .build());
        throw e;
    }
}

// 新代码 - 使用注解
@PostMapping("/api/posts")
@Auditable(operation = AuditOperation.POST_CREATE, resourceType = "POST")
public ResponseEntity<?> createPost(@RequestBody PostRequest request) {
    Post post = postService.createPost(request);
    return ResponseEntity.ok(post);
}
```

## 6. 常见问题

### Q1: 什么时候使用@Auditable注解？
A: 所有需要记录审计日志的Controller方法都应该使用@Auditable注解，而不是手动调用审计服务。

### Q2: 如何处理批量操作的审计？
A: 对于批量操作，可以在注解的description中说明批量数量，或者使用tags标记：
```java
@Auditable(
    operation = AuditOperation.POST_DELETE,
    description = "批量删除帖子",
    tags = "batch_operation"
)
```

### Q3: 敏感参数索引如何计算？
A: 从0开始计算方法参数的位置，包括路径参数：
```java
public ResponseEntity<?> method(@PathVariable Long id,           // 索引0
                               @RequestParam String username,    // 索引1
                               @RequestBody PasswordRequest req) // 索引2
```

### Q4: 如何自定义Redis键？
A: 如果现有的RedisKey方法不满足需求，可以使用buildKey方法：
```java
String customKey = RedisKey.buildKey("custom", "module", "feature", identifier);
```

## 7. 总结

通过使用统一的常量管理和审计注解系统：

1. **提高代码质量**：减少硬编码，提高可维护性
2. **增强安全性**：统一的敏感信息处理和审计记录
3. **简化开发**：开发者只需添加注解，无需手动编写审计代码
4. **保证一致性**：所有审计日志格式统一，便于分析和监控

请在开发新功能时严格遵循本指南，并逐步将现有代码迁移到新的规范中。