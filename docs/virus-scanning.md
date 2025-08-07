# 病毒扫描系统文档

## 概述

MyWeb博客系统的病毒扫描功能实现了符合GB/T 22239-2019二级等保要求的恶意代码防范机制。该系统提供实时文件病毒扫描、可疑文件隔离、安全事件告警等功能。

## 功能特性

### 1. 多引擎支持
- **ClamAV引擎**: 集成开源ClamAV反病毒引擎，提供专业级病毒检测
- **模拟引擎**: 用于开发和测试环境的模拟扫描引擎
- **可扩展架构**: 支持集成其他病毒扫描引擎

### 2. 实时扫描
- 文件上传时自动触发病毒扫描
- 支持多种文件格式的扫描
- 异步扫描处理，不阻塞用户操作
- 可配置的扫描超时和文件大小限制

### 3. 威胁检测
- 支持多种威胁级别：NONE, LOW, MEDIUM, HIGH, CRITICAL
- 检测已知病毒、木马、蠕虫等恶意代码
- 识别可疑文件模式和行为
- EICAR测试文件支持

### 4. 文件隔离
- 自动隔离检测到的恶意文件
- 安全的隔离存储机制
- 隔离文件元数据管理
- 定期清理过期隔离文件

### 5. 安全告警
- 实时安全事件告警
- 邮件通知管理员
- 可配置的告警级别和策略
- 告警历史记录和统计

### 6. 审计日志
- 完整的扫描操作审计记录
- 病毒检测事件日志
- 隔离操作审计
- 告警发送记录

## 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   文件上传      │───▶│   病毒扫描      │───▶│   扫描结果      │
│   File Upload   │    │   Virus Scan    │    │   Scan Result   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   扫描引擎      │    │   结果处理      │
                       │   Scan Engine   │    │   Result Handle │
                       └─────────────────┘    └─────────────────┘
                                                       │
                       ┌─────────────────┐    ┌─────────────────┐
                       │   文件隔离      │◀───│   安全告警      │
                       │   Quarantine    │    │   Alert Service │
                       └─────────────────┘    └─────────────────┘
```

## 配置说明

### 基本配置

```yaml
app:
  security:
    virus-scan:
      # 扫描引擎类型
      engine: mock  # mock | clamav
      
      # 是否启用病毒扫描
      enabled: true
      
      # 扫描超时时间（秒）
      timeout: 30
      
      # 最大扫描文件大小
      max-file-size: 50MB
```

### ClamAV配置

```yaml
app:
  security:
    virus-scan:
      clamav:
        host: localhost
        port: 3310
        connection-timeout: 5000
        read-timeout: 30000
```

### 隔离配置

```yaml
app:
  security:
    virus-scan:
      quarantine:
        path: ${java.io.tmpdir}/myweb-quarantine
        retention-days: 30
        max-size: 100MB
        enabled: true
```

### 告警配置

```yaml
app:
  security:
    virus-scan:
      alert:
        enabled: true
        email-enabled: true
        admin-email: admin@myweb.com
        min-threat-level: MEDIUM
```

## 部署指南

### 1. ClamAV安装（生产环境）

#### Ubuntu/Debian
```bash
sudo apt-get update
sudo apt-get install clamav clamav-daemon
sudo systemctl start clamav-daemon
sudo systemctl enable clamav-daemon
```

#### CentOS/RHEL
```bash
sudo yum install epel-release
sudo yum install clamav clamav-update clamav-daemon
sudo systemctl start clamd
sudo systemctl enable clamd
```

#### Docker部署
```bash
docker run -d --name clamav \
  -p 3310:3310 \
  -v /var/lib/clamav:/var/lib/clamav \
  clamav/clamav:latest
```

### 2. 病毒库更新

```bash
# 手动更新病毒库
sudo freshclam

# 设置自动更新（crontab）
0 2 * * * /usr/bin/freshclam --quiet
```

### 3. 应用配置

在`application.yml`中启用病毒扫描：

```yaml
spring:
  profiles:
    include: virus-scan

app:
  security:
    virus-scan:
      engine: clamav  # 生产环境使用ClamAV
      enabled: true
```

## 使用示例

### 1. 文件上传扫描

```java
@RestController
public class FileUploadController {
    
    @Autowired
    private FileUploadSecurityService securityService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        try {
            // 文件安全验证（包含病毒扫描）
            securityService.validateUploadedFile(file, userId, username, request);
            
            // 处理文件上传
            // ...
            
            return ResponseEntity.ok("文件上传成功");
            
        } catch (FileValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
```

### 2. 手动病毒扫描

```java
@Service
public class FileProcessingService {
    
    @Autowired
    private VirusScanService virusScanService;
    
    public void processFile(MultipartFile file, Long userId, String username) {
        // 执行病毒扫描
        CompletableFuture<VirusScanResult> scanFuture = 
            virusScanService.scanFile(file, userId, username);
        
        scanFuture.thenAccept(result -> {
            if (result.isVirusFound()) {
                log.warn("检测到病毒: {}", result.getVirusName());
                // 处理病毒文件
            } else {
                log.info("文件安全，继续处理");
                // 继续文件处理
            }
        });
    }
}
```

### 3. 隔离文件管理

```java
@Service
public class QuarantineManagementService {
    
    @Autowired
    private VirusQuarantineService quarantineService;
    
    public void cleanupQuarantine() {
        // 清理过期隔离文件
        CompletableFuture<Integer> cleanupFuture = 
            quarantineService.cleanupExpiredQuarantineFiles();
        
        cleanupFuture.thenAccept(count -> {
            log.info("清理了 {} 个过期隔离文件", count);
        });
    }
    
    public QuarantineStatistics getStatistics() {
        // 获取隔离统计信息
        return quarantineService.getQuarantineStatistics();
    }
}
```

## 监控和维护

### 1. 健康检查

系统提供健康检查端点：

```bash
# 检查病毒扫描服务状态
curl http://localhost:8080/actuator/health/virus-scan

# 检查隔离服务状态
curl http://localhost:8080/actuator/health/quarantine
```

### 2. 指标监控

支持Prometheus指标：

- `virus_scan_total`: 总扫描次数
- `virus_scan_duration`: 扫描耗时
- `virus_detected_total`: 检测到的病毒数量
- `quarantine_files_total`: 隔离文件数量

### 3. 定时维护任务

系统自动执行以下维护任务：

- **每天3:00**: 清理过期隔离文件
- **每天4:00**: 更新病毒库
- **每小时**: 检查扫描引擎状态
- **每天9:00**: 生成隔离统计报告

## 故障排除

### 1. ClamAV连接失败

**问题**: 无法连接到ClamAV服务

**解决方案**:
```bash
# 检查ClamAV服务状态
sudo systemctl status clamav-daemon

# 检查端口监听
netstat -tlnp | grep 3310

# 检查配置文件
sudo nano /etc/clamav/clamd.conf
```

### 2. 扫描超时

**问题**: 大文件扫描超时

**解决方案**:
```yaml
app:
  security:
    virus-scan:
      timeout: 60  # 增加超时时间
      max-file-size: 100MB  # 调整文件大小限制
```

### 3. 隔离空间不足

**问题**: 隔离目录空间不足

**解决方案**:
```bash
# 手动清理过期文件
find /tmp/myweb-quarantine -name "*.quarantine" -mtime +30 -delete

# 调整保留策略
app:
  security:
    virus-scan:
      quarantine:
        retention-days: 7  # 减少保留天数
```

### 4. 告警邮件发送失败

**问题**: 安全告警邮件无法发送

**解决方案**:
```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-email@example.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## 安全建议

### 1. 生产环境配置

- 使用ClamAV引擎而非模拟引擎
- 启用文件隔离功能
- 配置适当的告警级别
- 定期更新病毒库

### 2. 性能优化

- 合理设置扫描超时时间
- 限制扫描文件大小
- 使用异步扫描避免阻塞
- 监控扫描性能指标

### 3. 安全策略

- 对高威胁级别文件立即隔离
- 启用实时安全告警
- 定期审查隔离文件
- 保持审计日志完整性

## 版本历史

- **v1.0.0** (2025-01-01): 初始版本，支持基本病毒扫描功能
- 支持ClamAV和模拟扫描引擎
- 实现文件隔离和安全告警
- 完整的审计日志记录

## 技术支持

如有问题或建议，请联系：

- 邮箱: security@myweb.com
- 文档: https://docs.myweb.com/security/virus-scan
- 问题跟踪: https://github.com/myweb/issues