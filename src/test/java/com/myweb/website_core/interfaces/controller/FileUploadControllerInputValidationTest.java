package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.file.FileUploadService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.common.util.PermissionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileUploadController输入验证集成测试
 * 
 * 测试文件上传控制器的输入验证功能，包括：
 * - 文件名验证
 * - 文件类型和大小验证
 * - 上传参数验证
 * - 详细的验证日志记录
 * <p>
 * 符合需求：1.4, 6.1, 6.2, 6.7 - 文件上传输入验证
 */
@ExtendWith(MockitoExtension.class)
class FileUploadControllerInputValidationTest {

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private InputValidationService inputValidationService;

    @InjectMocks
    private FileUploadController fileUploadController;

    private MockMultipartFile validImageFile;
    private MockMultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        // 创建有效的图片文件
        validImageFile = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            "fake image content".getBytes()
        );

        // 创建无效的文件
        invalidFile = new MockMultipartFile(
            "file",
            "malicious.exe",
            "application/octet-stream",
            "malicious content".getBytes()
        );
    }

    @Test
    void testUploadImage_ValidFile_Success() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            when(fileUploadService.uploadImage(any(MultipartFile.class), any()))
                .thenReturn("/blog/api/images/123");

            // Act
            ResponseEntity<?> response = fileUploadController.uploadImage(validImageFile, 1L);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(inputValidationService).validateFilename("test-image.jpg");
            verify(fileUploadService).uploadImage(validImageFile, 1L);
        }
    }

    @Test
    void testUploadImage_InvalidFilename_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            doThrow(new ValidationException("文件名包含非法字符", "filename", "ILLEGAL_CHARACTERS"))
                .when(inputValidationService).validateFilename("malicious.exe");

            // Act
            ResponseEntity<?> response = fileUploadController.uploadImage(invalidFile, 1L);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(inputValidationService).validateFilename("malicious.exe");
            verify(fileUploadService, never()).uploadImage(any(), any());
        }
    }

    @Test
    void testUploadImage_InvalidPostId_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");

            // Act
            ResponseEntity<?> response = fileUploadController.uploadImage(validImageFile, -1L);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(fileUploadService, never()).uploadImage(any(), any());
        }
    }

    @Test
    void testUploadImage_FileTooLarge_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            // 创建超大文件（11MB）
            byte[] largeContent = new byte[11 * 1024 * 1024];
            MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent);

            // Act
            ResponseEntity<?> response = fileUploadController.uploadImage(largeFile, 1L);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(fileUploadService, never()).uploadImage(any(), any());
        }
    }

    @Test
    void testUploadImage_UnsupportedFileType_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            MockMultipartFile textFile = new MockMultipartFile(
                "file", "document.txt", "text/plain", "text content".getBytes());

            // Act
            ResponseEntity<?> response = fileUploadController.uploadImage(textFile, 1L);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(fileUploadService, never()).uploadImage(any(), any());
        }
    }

    @Test
    void testUploadImages_ValidFiles_Success() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            MultipartFile[] files = {validImageFile};
            when(fileUploadService.uploadImage(any(MultipartFile.class), any()))
                .thenReturn("/blog/api/images/123");

            // Act
            ResponseEntity<?> response = fileUploadController.uploadImages(files, 1L);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(inputValidationService).validateFilename("test-image.jpg");
            verify(fileUploadService).uploadImage(validImageFile, 1L);
        }
    }

    @Test
    void testUploadImages_TooManyFiles_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            // 创建7个文件（超过限制的6个）
            MultipartFile[] files = new MultipartFile[7];
            for (int i = 0; i < 7; i++) {
                files[i] = new MockMultipartFile("file", "image" + i + ".jpg", "image/jpeg", "content".getBytes());
            }

            // Act
            ResponseEntity<?> response = fileUploadController.uploadImages(files, 1L);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(fileUploadService, never()).uploadImage(any(), any());
        }
    }

    @Test
    void testDeleteImage_ValidUrl_Success() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            String validUrl = "/blog/api/images/123";
            when(fileUploadService.deleteFile(validUrl)).thenReturn(true);

            // Act
            ResponseEntity<?> response = fileUploadController.deleteImage(validUrl);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(fileUploadService).deleteFile(validUrl);
        }
    }

    @Test
    void testDeleteImage_InvalidUrl_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            
            String invalidUrl = "http://malicious.com/file.jpg";

            // Act
            ResponseEntity<?> response = fileUploadController.deleteImage(invalidUrl);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(fileUploadService, never()).deleteFile(any());
        }
    }

    @Test
    void testDeleteImage_EmptyUrl_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");

            // Act
            ResponseEntity<?> response = fileUploadController.deleteImage("");

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(fileUploadService, never()).deleteFile(any());
        }
    }

    @Test
    void testValidateFile_ValidFile_Success() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            mockedPermissionUtils.when(PermissionUtils::getCurrentUserId).thenReturn(1L);
            
            when(fileUploadService.validateFileSecurity(any(), any(), any())).thenReturn(true);

            // Act
            ResponseEntity<?> response = fileUploadController.validateFile(validImageFile);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(inputValidationService).validateFilename("test-image.jpg");
            verify(fileUploadService).validateFileSecurity(validImageFile, 1L, "testuser");
        }
    }

    @Test
    void testValidateFile_InvalidFile_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("testuser");
            mockedPermissionUtils.when(PermissionUtils::getCurrentUserId).thenReturn(1L);
            
            doThrow(new ValidationException("文件名包含非法字符", "filename", "ILLEGAL_CHARACTERS"))
                .when(inputValidationService).validateFilename("malicious.exe");

            // Act
            ResponseEntity<?> response = fileUploadController.validateFile(invalidFile);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(inputValidationService).validateFilename("malicious.exe");
            verify(fileUploadService, never()).validateFileSecurity(any(), any(), any());
        }
    }

    @Test
    void testGetUploadStatistics_ValidDays_Success() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("admin");
            mockedPermissionUtils.when(PermissionUtils::isAdmin).thenReturn(true);

            // Act
            ResponseEntity<?> response = fileUploadController.getUploadStatistics(7, null);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    void testGetUploadStatistics_InvalidDays_ValidationException() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("admin");
            mockedPermissionUtils.when(PermissionUtils::isAdmin).thenReturn(true);

            // Act
            ResponseEntity<?> response = fileUploadController.getUploadStatistics(400, null);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Test
    void testGetUploadStatistics_NonAdmin_Forbidden() throws Exception {
        // Arrange
        try (MockedStatic<PermissionUtils> mockedPermissionUtils = mockStatic(PermissionUtils.class)) {
            mockedPermissionUtils.when(PermissionUtils::getCurrentUsername).thenReturn("user");
            mockedPermissionUtils.when(PermissionUtils::isAdmin).thenReturn(false);

            // Act
            ResponseEntity<?> response = fileUploadController.getUploadStatistics(7, null);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }
}