# 图片存储机制详解

## 概述

图片存储采用 **"数据库存储元数据 + 文件系统存储实际内容"** 的分离架构。

## 存储流程

### 1. 用户上传图片

```
用户选择图片 → 前端发送到 /blog/api/upload/image → FileUploadService 处理
```

### 2. 文件系统存储

**存储位置**：
```
项目根目录/uploads/images/yyyy/MM/dd/
```

**目录结构示例**：
```
website_core/
├── uploads/
│   └── images/
│       ├── 2025/
│       │   └── 07/
│       │       └── 29/
│       │           ├── a1b2c3d4e5f6.jpg
│       │           ├── f6e5d4c3b2a1.png
│       │           └── ...
│       └── 2025/
│           └── 08/
│               └── 01/
│                   └── ...
```

**文件命名规则**：
```java
// 生成唯一文件名
String uuid = UUID.randomUUID().toString().replace("-", "");
String fileName = uuid + "." + extension;
// 例如：a1b2c3d4e5f67890123456789012.jpg
```

### 3. 数据库存储元数据

**post_images 表结构**：
```sql
CREATE TABLE post_images (
    id BIGSERIAL PRIMARY KEY,                    -- 图片ID
    original_filename VARCHAR(255) NOT NULL,    -- 原始文件名：如 "我的照片.jpg"
    stored_filename VARCHAR(255) NOT NULL,      -- 存储文件名：如 "a1b2c3d4e5f6.jpg"
    file_path VARCHAR(500) NOT NULL,            -- 完整文件路径
    content_type VARCHAR(100) NOT NULL,         -- MIME类型：如 "image/jpeg"
    file_size BIGINT NOT NULL,                  -- 文件大小（字节）
    upload_time TIMESTAMP NOT NULL,             -- 上传时间
    post_id BIGINT REFERENCES posts(id)         -- 关联的帖子ID
);
```

**存储示例数据**：
```sql
INSERT INTO post_images VALUES (
    123,                                                    -- id
    '我的照片.jpg',                                          -- original_filename
    'a1b2c3d4e5f67890123456789012.jpg',                    -- stored_filename
    'uploads/images/2025/07/29/a1b2c3d4e5f67890123456789012.jpg', -- file_path
    'image/jpeg',                                           -- content_type
    1048576,                                                -- file_size (1MB)
    '2025-07-29 20:30:00',                                 -- upload_time
    456                                                     -- post_id
);
```

## 关键代码解析

### 1. 文件存储 (FileUploadService.java)

```java
public String uploadImage(MultipartFile file, Long postId) {
    // 1. 创建按日期分组的目录
    String uploadPath = createUploadDirectory(); 
    // 返回：uploads/images/2025/07/29/
    
    // 2. 生成唯一文件名
    String fileName = generateUniqueFileName(file.getOriginalFilename());
    // 返回：a1b2c3d4e5f67890123456789012.jpg
    
    // 3. 保存文件到文件系统
    Path filePath = Paths.get(uploadPath, fileName);
    Files.copy(file.getInputStream(), filePath);
    
    // 4. 保存元数据到数据库
    Image image = imageService.saveImage(
        file.getOriginalFilename(),  // "我的照片.jpg"
        fileName,                    // "a1b2c3d4e5f67890123456789012.jpg"
        filePath.toString(),         // "uploads/images/2025/07/29/a1b2c3d4e5f67890123456789012.jpg"
        file.getContentType(),       // "image/jpeg"
        file.getSize(),              // 1048576
        postId                       // 456
    );
    
    // 5. 返回访问URL
    return "/blog/api/images/" + image.getId(); // "/blog/api/images/123"
}
```

### 2. 图片访问 (ImageController.java)

```java
@GetMapping("/{id}")
public ResponseEntity<Resource> getImage(@PathVariable Long id) {
    // 1. 从数据库获取图片元数据
    Image image = imageService.getImageById(id);
    
    // 2. 根据 file_path 读取文件系统中的实际文件
    Resource resource = imageService.getImageResource(id);
    
    // 3. 设置正确的响应头
    headers.setContentType(MediaType.parseMediaType(image.getContentType()));
    headers.setContentLength(image.getFileSize());
    
    // 4. 返回文件内容
    return ResponseEntity.ok().headers(headers).body(resource);
}
```

### 3. 文件读取 (ImageService.java)

```java
public Resource getImageResource(Long id) throws IOException {
    // 1. 获取图片元数据
    Image image = imageRepository.findById(id).orElseThrow();
    
    // 2. 根据 file_path 构建文件路径
    Path filePath = Paths.get(image.getFilePath());
    // 例如：uploads/images/2025/07/29/a1b2c3d4e5f67890123456789012.jpg
    
    // 3. 检查文件是否存在
    if (!Files.exists(filePath)) {
        throw new IOException("图片文件不存在");
    }
    
    // 4. 返回文件资源
    return new UrlResource(filePath.toUri());
}
```

## 访问流程

### 1. 前端请求图片

```html
<img src="/blog/api/images/123" alt="图片">
```

### 2. 后端处理流程

```
1. 接收请求：GET /blog/api/images/123
2. ImageController.getImage(123)
3. 查询数据库：SELECT * FROM post_images WHERE id = 123
4. 获取 file_path：uploads/images/2025/07/29/a1b2c3d4e5f67890123456789012.jpg
5. 读取文件系统中的实际文件
6. 设置响应头：Content-Type: image/jpeg, Content-Length: 1048576
7. 返回文件内容给浏览器
```

## 配置参数

### application.properties

```properties
# 文件上传配置
app.upload.upload-dir=uploads/images/
app.upload.max-file-size=5242880      # 5MB
app.upload.max-request-size=20971520  # 20MB
app.upload.allowed-types=image/jpeg,image/png,image/gif,image/webp
```

## 优势

### 1. 性能优势
- **数据库轻量**：只存储元数据，不存储大文件
- **文件系统优化**：操作系统针对文件I/O优化
- **缓存友好**：可以设置HTTP缓存头

### 2. 扩展性
- **分布式存储**：可以轻松迁移到云存储（如AWS S3）
- **CDN集成**：可以配置CDN加速图片访问
- **负载均衡**：文件服务器可以独立扩展

### 3. 管理便利
- **备份简单**：文件系统备份 + 数据库备份
- **清理方便**：可以批量删除过期文件
- **监控容易**：可以监控磁盘使用情况

## 注意事项

### 1. 文件路径
- `file_path` 存储的是相对路径
- 实际访问时需要结合项目根目录
- 跨平台兼容性需要注意路径分隔符

### 2. 安全考虑
- 文件类型验证
- 文件大小限制
- 防止路径遍历攻击
- 访问权限控制

### 3. 维护建议
- 定期清理未关联的图片文件
- 监控磁盘空间使用
- 备份策略制定
- 日志记录和监控

## 查看实际存储

### 1. 查看数据库记录
```sql
SELECT id, original_filename, stored_filename, file_path, file_size 
FROM post_images 
ORDER BY upload_time DESC 
LIMIT 10;
```

### 2. 查看文件系统
```bash
# 进入项目目录
cd website_core

# 查看上传目录结构
find uploads/images -type f -name "*.jpg" -o -name "*.png" | head -10

# 查看文件详情
ls -la uploads/images/2025/07/29/
```

### 3. 测试图片访问
```bash
# 直接访问图片URL
curl -I http://localhost:8080/blog/api/images/123

# 应该返回类似：
# HTTP/1.1 200 OK
# Content-Type: image/jpeg
# Content-Length: 1048576
# Cache-Control: max-age=3600
```