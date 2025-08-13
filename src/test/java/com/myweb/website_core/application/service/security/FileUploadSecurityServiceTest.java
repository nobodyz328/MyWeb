package com.myweb.website_core.application.service.security;

import com.myweb.website_core.application.service.security.audit.AuditMessageService;
import com.myweb.website_core.application.service.security.integeration.FileUploadSecurityService;
import com.myweb.website_core.common.exception.FileValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * 文件上传安全服务测试类
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileUploadSecurityServiceTest {
    
    @Mock
    private AuditMessageService auditLogService;
    
    @Mock
    private HttpServletRequest request;
    
    private FileUploadSecurityService fileUploadSecurityService;
    
    @BeforeEach
    void setUp() {
        fileUploadSecurityService = new FileUploadSecurityService(auditLogService,null,null,null);
        
        // 设置默认的请求模拟 - 使用lenient stubbing
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
    }
    
    @Test
    void testValidateUploadedFile_ValidJpegFile_ShouldPass() {
        // 创建有效的JPEG文件（简化的JPEG头部）
        byte[] jpegBytes = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, // JPEG header
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, // JFIF
            (byte) 0xFF, (byte) 0xD9 // JPEG end
        };
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", jpegBytes);
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> 
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
    }
    
    @Test
    void testValidateUploadedFile_ValidPngFile_ShouldPass() {
        // 创建有效的PNG文件头部
        byte[] pngBytes = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52 // IHDR chunk start
        };
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", pngBytes);
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> 
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
    }
    
    @Test
    void testValidateUploadedFile_EmptyFile_ShouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", new byte[0]);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertEquals("文件不能为空", exception.getMessage());
    }
    
    @Test
    void testValidateUploadedFile_NullFile_ShouldThrowException() {
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(null, 1L, "testuser", request));
        
        assertEquals("文件不能为空", exception.getMessage());
    }
    
    @Test
    void testValidateUploadedFile_FileTooLarge_ShouldThrowException() {
        // 创建超过5MB的文件
        byte[] largeFileBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
            "file", "large.jpg", "image/jpeg", largeFileBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("文件大小超过限制"));
    }
    
    @Test
    void testValidateUploadedFile_InvalidExtension_ShouldThrowException() {
        byte[] fileBytes = {0x01, 0x02, 0x03, 0x04};
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.exe", "application/octet-stream", fileBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("文件名包含禁止的字符或扩展名"));
    }
    
    @Test
    void testValidateUploadedFile_InvalidMimeType_ShouldThrowException() {
        byte[] fileBytes = {0x01, 0x02, 0x03, 0x04};
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "application/octet-stream", fileBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("不支持的MIME类型"));
    }
    
    @Test
    void testValidateUploadedFile_MagicNumberMismatch_ShouldThrowException() {
        // 文件扩展名是jpg但内容不是JPEG
        byte[] fileBytes = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", fileBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("文件内容与扩展名不匹配"));
    }
    
    @Test
    void testValidateUploadedFile_MaliciousScript_ShouldThrowException() {
        // 创建包含恶意脚本的SVG文件
        String maliciousSvg = "<svg><script>alert('xss')</script></svg>";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.svg", "image/svg+xml", maliciousSvg.getBytes());
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("SVG文件包含危险元素"));
    }
    
    @Test
    void testValidateUploadedFile_ExecutableFile_ShouldThrowException() {
        // 创建PE文件头（Windows可执行文件）
        byte[] peBytes = {0x4D, 0x5A, (byte) 0x90, 0x00}; // MZ header
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", peBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("文件内容与扩展名不匹配") || 
                   exception.getMessage().contains("检测到可执行文件特征"));
    }
    
    @Test
    void testGenerateSecureStoragePath_ShouldReturnValidPath() {
        String path = fileUploadSecurityService.generateSecureStoragePath("test.jpg", 1L);
        
        assertNotNull(path);
        assertTrue(path.startsWith("uploads/images/"));
        assertTrue(path.contains("/"));
        assertTrue(path.endsWith(".jpg"));
    }
    
    @Test
    void testGenerateSecureFileName_ShouldReturnUniqueFileName() {
        String filename1 = fileUploadSecurityService.generateSecureFileName("test.jpg", 1L);
        String filename2 = fileUploadSecurityService.generateSecureFileName("test.jpg", 1L);
        
        assertNotNull(filename1);
        assertNotNull(filename2);
        assertNotEquals(filename1, filename2); // 应该生成不同的文件名
        assertTrue(filename1.endsWith(".jpg"));
        assertTrue(filename2.endsWith(".jpg"));
    }
    
    @Test
    void testGenerateSecureFileName_NullOriginalFilename_ShouldHandleGracefully() {
        String filename = fileUploadSecurityService.generateSecureFileName(null, 1L);
        
        assertNotNull(filename);
        assertTrue(filename.contains("_"));
    }
    
    @Test
    void testValidateUploadedFile_ValidGifFile_ShouldPass() {
        // 创建有效的GIF文件头部
        String gifHeader = "GIF89a";
        byte[] gifBytes = new byte[gifHeader.length() + 10];
        System.arraycopy(gifHeader.getBytes(), 0, gifBytes, 0, gifHeader.length());
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.gif", "image/gif", gifBytes);
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> 
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
    }
    
    @Test
    void testValidateUploadedFile_InvalidGifHeader_ShouldThrowException() {
        // 创建无效的GIF文件头部
        String invalidHeader = "NOTGIF";
        byte[] invalidBytes = new byte[invalidHeader.length() + 10];
        System.arraycopy(invalidHeader.getBytes(), 0, invalidBytes, 0, invalidHeader.length());
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.gif", "image/gif", invalidBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("无效的GIF文件头") ||
                   exception.getMessage().contains("文件内容与扩展名不匹配"));
    }
    
    @Test
    void testValidateUploadedFile_FilenameWithSpecialCharacters_ShouldThrowException() {
        byte[] fileBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile(
            "file", "test<script>.jpg", "image/jpeg", fileBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("文件名包含禁止的字符或扩展名"));
    }
    
    @Test
    void testValidateUploadedFile_FilenameTooLong_ShouldThrowException() {
        // 创建超长文件名
        String longFilename = "a".repeat(250) + ".jpg";
        byte[] fileBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile(
            "file", longFilename, "image/jpeg", fileBytes);
        
        FileValidationException exception = assertThrows(FileValidationException.class, () ->
            fileUploadSecurityService.validateUploadedFile(file, 1L, "testuser", request));
        
        assertTrue(exception.getMessage().contains("文件名长度不能超过255个字符") ||
                   exception.getMessage().contains("JPEG文件结构不完整"));
    }
}