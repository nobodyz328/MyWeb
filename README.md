# 我的博客系统

基于Spring Boot的现代化博客系统

## 🛠️ 环境要求

- Java 8+
- Maven 3.6+
- PostgreSQL 12+
- IDE (推荐 IntelliJ IDEA 或 Eclipse)

## 📦 依赖说明

### 核心依赖
- **Spring Boot 2.6.13** - 主框架
- **Spring Data JPA** - 数据持久化
- **Spring Security** - 安全认证
- **PostgreSQL** - 数据库
- **Thymeleaf** - 模板引擎
- **Lombok** - 代码简化

## 🚀 快速开始

### 1. 数据库配置
```sql
-- 创建数据库
CREATE DATABASE myblog;

-- 创建用户（可选）
CREATE USER myblog_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE myblog TO myblog_user;
```

### 2. 配置文件修改
编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myblog
    username: your_username
    password: your_password
```

### 3. 启动项目
```bash
# 方式1：使用Maven
mvn spring-boot:run

# 方式2：IDE中运行
# 直接运行 WebsiteCoreApplication.java 的 main 方法
```

### 4. 访问应用
- 首页：http://localhost:8080/api/
- API文档：http://localhost:8080/api/posts

## 📁 项目结构

```
src/main/java/com/myweb/website_core/
├── WebsiteCoreApplication.java          # 启动类
└── demos/web/
    ├── templates/
    │   ├── WebController.java          # 页面控制器
    │   └── index.html                  # 首页模板
    └── blog/
        ├── BlogPost.java               # 博客实体类
        ├── BlogPostRepository.java     # 数据访问层
        ├── BlogService.java            # 业务逻辑层
        └── BlogController.java         # REST API控制器
```

## 🔧 开发指南

### 添加新功能
1. 创建实体类（Entity）
2. 创建Repository接口
3. 创建Service类
4. 创建Controller类
5. 创建前端页面（如需要）

### API接口
- `GET /api/posts` - 获取所有文章
- `GET /api/posts/{id}` - 获取指定文章
- `POST /api/posts` - 创建新文章
- `PUT /api/posts/{id}` - 更新文章
- `DELETE /api/posts/{id}` - 删除文章

## 🎯 下一步开发计划

1. ✅ 基础博客功能
2. 🔄 用户认证系统
3. 📝 文章编辑器
4. 🏷️ 标签和分类
5. 💬 评论系统
6. 🔍 搜索功能
7. 📱 移动端优化

## 🐛 常见问题

### 数据库连接失败
- 检查PostgreSQL服务是否启动
- 验证数据库连接信息
- 确认数据库用户权限

### 端口占用
- 修改 `application.yml` 中的 `server.port`
- 或杀死占用8080端口的进程

## 📞 技术支持

如有问题，请查看日志文件或联系开发团队。 