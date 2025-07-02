# 故障排除指南

## 常见问题及解决方案

### 1. 数据库连接错误

**错误信息**: `Connection refused` 或 `Authentication failed`

**解决方案**:
1. 确保PostgreSQL服务正在运行
2. 检查数据库连接信息是否正确
3. 确认数据库用户权限

```bash
# 检查PostgreSQL服务状态
# Windows
services.msc  # 查找 PostgreSQL 服务

# 或者使用命令行
pg_ctl status -D "C:\Program Files\PostgreSQL\14\data"
```

### 2. SQL语法错误

**错误信息**: `syntax error at or near "."`

**解决方案**:
1. 检查 `data.sql` 文件中的SQL语法
2. 确保没有特殊字符或格式问题
3. 使用简化的测试数据

### 3. 端口占用错误

**错误信息**: `Port 8080 is already in use`

**解决方案**:
1. 修改 `application.yml` 中的端口配置
2. 或者杀死占用端口的进程

```yaml
server:
  port: 8081  # 修改为其他端口
```

### 4. Spring Security 问题

**错误信息**: `Access Denied` 或重定向到登录页面

**解决方案**:
1. 检查 `SecurityConfig.java` 配置
2. 确保所有路径都已允许访问

### 5. Thymeleaf 模板错误

**错误信息**: `Template not found` 或 `Could not parse template`

**解决方案**:
1. 确保模板文件在正确的位置 (`src/main/resources/templates/`)
2. 检查模板语法是否正确
3. 确保Thymeleaf依赖已添加

### 6. JPA/Hibernate 错误

**错误信息**: `Table not found` 或 `Column not found`

**解决方案**:
1. 检查实体类注解是否正确
2. 确认表名映射正确
3. 检查数据库表是否已创建

## 调试步骤

### 1. 启用详细日志

在 `application.yml` 中添加：

```yaml
logging:
  level:
    root: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 2. 检查数据库表

连接到PostgreSQL数据库：

```sql
-- 查看所有表
\dt

-- 查看blog_post表结构
\d blog_post

-- 查看数据
SELECT * FROM blog_post;
```

### 3. 测试API接口

使用curl或Postman测试：

```bash
# 获取所有文章
curl http://localhost:8080/api/posts

# 获取指定文章
curl http://localhost:8080/api/posts/1
```

## 环境检查清单

- [ ] Java 8+ 已安装
- [ ] Maven 3.6+ 已安装
- [ ] PostgreSQL 12+ 已安装并运行
- [ ] 数据库已创建
- [ ] 数据库用户权限正确
- [ ] 防火墙允许8080端口
- [ ] 项目依赖已下载

## 获取帮助

如果问题仍然存在，请：

1. 查看完整的错误日志
2. 检查环境配置
3. 尝试使用简化的测试数据
4. 联系技术支持 