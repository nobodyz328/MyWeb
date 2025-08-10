package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.application.service.security.integeration.FileIntegrityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件完整性服务测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@ExtendWith(MockitoExtension.class)
class FileIntegrityServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DataIntegrityService dataIntegrityService;

    private FileIntegrityService fileIntegrityService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileIntegrityService = new FileIntegrityService(auditLogService, dataIntegrityService);

        // 设置测试配置
        ReflectionTestUtils.setField(fileIntegrityService, "integrityCheckEnabled", true);
        ReflectionTestUtils.setField(fileIntegrityService, "criticalFilesPath", tempDir.toString());
        ReflectionTestUtils.setField(fileIntegrityService, "hashStoragePath", tempDir.resolve("hashes").toString());
        ReflectionTestUtils.setField(fileIntegrityService, "backupPath", tempDir.resolve("backups").toString());
        ReflectionTestUtils.setField(fileIntegrityService, "alertEnabled", true);

        // 初始化服务
        fileIntegrityService.init();
    }

    @Test
    void testInit_ShouldCreateDirectories() {
        // 验证目录创建
        assertTrue(Files.exists(tempDir.resolve("hashes")));
        assertTrue(Files.exists(tempDir.resolve("backups")));
    }

    @Test
    void testCheckFileIntegrity_WithExistingFile_ShouldReturnValidResult() throws IOException {
        // 准备测试数据
        Path testFile = tempDir.resolve("test.yml");
        String testContent = "test: content";
        Files.write(testFile, testContent.getBytes());

        // 执行测试
    }
}