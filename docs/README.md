# MyWeb 博客系统

## 项目简介

MyWeb是一个基于Spring Boot的现代异步博客社区系统，采用前后端分离架构，支持用户注册、发帖、评论、点赞、收藏、关注等功能。

## 技术栈

- **后端框架**: Spring Boot 3.2.0
- **Java版本**: Java 21 (LTS)
- **数据库**: PostgreSQL 15+
- **消息队列**: RabbitMQ
- **缓存**: Redis
- **ORM框架**: Spring Data JPA + MyBatis 3.0.3
- **安全框架**: Spring Security
- **模板引擎**: Thymeleaf
- **构建工具**: Maven 3.11.0

## 主要功能

### 用户模块
- 用户注册/登录（支持邮箱验证）
- 用户个人主页
- 用户关注/取关
- 邮箱绑定设置

### 帖子模块
- 帖子发布/编辑/删除
- 帖子点赞/取消点赞
- 帖子收藏/取消收藏
- 帖子搜索和排序

### 互动模块
- 评论系统（支持异步加载）
- 用户关注关系
- 个人动态展示

### 管理模块
- 公告发布和管理
- 用户管理
- 内容审核

## 快速开始

### 环境要求
- Java 21
- Maven 3.6+
- PostgreSQL 15+
- Redis 6+
- RabbitMQ 3.8+



## 项目结构

```
website_core/
├── src/main/java/com/myweb/website_core/
│   ├── App.java                          # 主应用类
│   ├── config/                           # 配置类
│   │   ├── SecurityConfig.java          # 安全配置
│   │   ├── WebConfig.java               # Web配置
│   │   ├── RabbitMQConfig.java          # RabbitMQ配置
│   │   └── RedisConfig.java             # Redis配置
│   ├── demos/web/                        # 业务模块
│   │   ├── user/                         # 用户模块
│   │   ├── blog/                         # 博客模块
│   │   ├── comment/                      # 评论模块
│   │   └── announcement/                 # 公告模块
│   ├── service/                          # 服务层
│   └── mapper/                           # MyBatis映射器
├── src/main/resources/
│   ├── templates/                        # Thymeleaf模板
│   ├── static/                           # 静态资源
│   └── application.yml                   # 应用配置
└── pom.xml                               # Maven配置
```

## 安全特性

项目实现了符合GB/T 22239-2019 Level 2要求的安全措施：

- 身份鉴别（密码加密、验证码、会话管理）
- 访问控制（RBAC权限模型）
- 安全审计（操作日志记录）
- 入侵防范（SQL注入、XSS、CSRF防护）
- 数据完整性（数据校验和备份）

详细安全设计请参考：`SECURITY.md`

## 开发指南

### 代码规范
- 遵循Java编码规范
- 使用Lombok简化代码
- 异步方法使用@Async注解
- 统一异常处理

### 数据库设计
- 使用JPA注解定义实体
- 复杂查询使用MyBatis
- 支持数据库迁移

### 前端开发
- 使用Thymeleaf模板引擎
- 异步数据加载
- Steam风格UI设计

## 部署说明

### 开发环境
```bash
mvn spring-boot:run
```

### 生产环境
```bash
mvn clean package
java -jar target/website_core-0.0.1-SNAPSHOT.jar
```

## 常见问题

### Java版本问题
- 确保使用Java 21
- 运行`verify-java21.bat`验证

### 编译错误
- 清理项目：`mvn clean`
- 重新编译：`mvn compile`

### 数据库连接
- 检查PostgreSQL服务状态
- 验证数据库配置

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 发起Pull Request

## 许可证

MIT License

## 联系方式
