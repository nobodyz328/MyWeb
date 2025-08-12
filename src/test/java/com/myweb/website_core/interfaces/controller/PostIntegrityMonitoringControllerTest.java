package com.myweb.website_core.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.business.PostService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PostIntegrityMonitoringController测试类
 * 
 * 测试帖子完整性监控控制器的各项功能，包括：
 * - 单个帖子完整性验证
 * - 批量帖子完整性检查
 * - 用户帖子完整性统计
 * - 系统帖子完整性概览
 * 
 * @author MyWeb Security Team
 * @version 1.0
 */
@DisplayName("PostIntegrityMonitoringController测试")
public class PostIntegrityMonitoringControllerTest {

    private MockMvc mockMvc;
    
    @Mock
    private PostService postService;
    
    private PostIntegrityMonitoringController controller;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PostIntegrityMonitoringController(postService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    @DisplayName("验证单个帖子完整性 - 成功")
    void testVerifyPostIntegrity_Success() throws Exception {
        // Arrange
        Long postId = 1L;
        DataIntegrityService.IntegrityCheckResult validResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(true)
                .actualHash("test_hash")
                .expectedHash("test_hash")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(postService.verifyPostIntegrity(postId)).thenReturn(validResult);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/verify/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("帖子完整性验证通过"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.entityType").value("POST"))
                .andExpect(jsonPath("$.data.entityId").value(postId));
        
        verify(postService).verifyPostIntegrity(postId);
    }
    
    @Test
    @DisplayName("验证单个帖子完整性 - 失败")
    void testVerifyPostIntegrity_Failed() throws Exception {
        // Arrange
        Long postId = 1L;
        DataIntegrityService.IntegrityCheckResult invalidResult = 
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(false)
                .actualHash("tampered_hash")
                .expectedHash("original_hash")
                .errorMessage("帖子内容哈希值不匹配，可能已被篡改")
                .checkTime(LocalDateTime.now())
                .build();
        
        when(postService.verifyPostIntegrity(postId)).thenReturn(invalidResult);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/verify/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("帖子完整性验证失败"))
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errorMessage").value("帖子内容哈希值不匹配，可能已被篡改"));
        
        verify(postService).verifyPostIntegrity(postId);
    }
    
    @Test
    @DisplayName("批量验证帖子完整性")
    void testVerifyPostsIntegrity() throws Exception {
        // Arrange
        List<Long> postIds = Arrays.asList(1L, 2L, 3L);
        
        List<DataIntegrityService.IntegrityCheckResult> results = Arrays.asList(
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST").entityId(1L).isValid(true).checkTime(LocalDateTime.now()).build(),
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST").entityId(2L).isValid(true).checkTime(LocalDateTime.now()).build(),
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST").entityId(3L).isValid(false).errorMessage("完整性验证失败").checkTime(LocalDateTime.now()).build()
        );
        
        when(postService.verifyPostsIntegrity(postIds)).thenReturn(results);
        
        // Act & Assert
        mockMvc.perform(post("/api/admin/monitoring/post-integrity/verify/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("批量验证完成: 总数=3, 失败数=1"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
        
        verify(postService).verifyPostsIntegrity(postIds);
    }
    
    @Test
    @DisplayName("验证用户帖子完整性")
    void testVerifyUserPostsIntegrity() throws Exception {
        // Arrange
        Long userId = 1L;
        
        List<DataIntegrityService.IntegrityCheckResult> results = Arrays.asList(
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST").entityId(1L).isValid(true).checkTime(LocalDateTime.now()).build(),
            DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST").entityId(2L).isValid(true).checkTime(LocalDateTime.now()).build()
        );
        
        when(postService.verifyUserPostsIntegrity(userId)).thenReturn(results);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/verify/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户帖子验证完成: 总数=2, 失败数=0"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
        
        verify(postService).verifyUserPostsIntegrity(userId);
    }
    
    @Test
    @DisplayName("获取帖子完整性统计信息")
    void testGetPostIntegrityStats() throws Exception {
        // Arrange
        PostService.PostIntegrityStats stats = PostService.PostIntegrityStats.builder()
                .totalPosts(100)
                .postsWithHash(90)
                .validPosts(85)
                .invalidPosts(5)
                .integrityRate(0.944) // 85/90
                .build();
        
        when(postService.getPostIntegrityStats()).thenReturn(stats);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("帖子完整性统计获取成功"))
                .andExpect(jsonPath("$.data.totalPosts").value(100))
                .andExpect(jsonPath("$.data.postsWithHash").value(90))
                .andExpect(jsonPath("$.data.validPosts").value(85))
                .andExpect(jsonPath("$.data.invalidPosts").value(5))
                .andExpect(jsonPath("$.data.integrityRate").value(0.944));
        
        verify(postService).getPostIntegrityStats();
    }
    
    @Test
    @DisplayName("获取帖子完整性监控概览")
    void testGetPostIntegrityOverview() throws Exception {
        // Arrange
        PostService.PostIntegrityStats stats = PostService.PostIntegrityStats.builder()
                .totalPosts(100)
                .postsWithHash(90)
                .validPosts(85)
                .invalidPosts(5)
                .integrityRate(0.944)
                .build();
        
        when(postService.getPostIntegrityStats()).thenReturn(stats);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("帖子完整性监控概览获取成功"))
                .andExpect(jsonPath("$.data.totalPosts").value(100))
                .andExpect(jsonPath("$.data.postsWithHash").value(90))
                .andExpect(jsonPath("$.data.validPosts").value(85))
                .andExpect(jsonPath("$.data.invalidPosts").value(5))
                .andExpect(jsonPath("$.data.integrityRate").value(0.944))
                .andExpect(jsonPath("$.data.integrityRatePercentage").value("94.40%"))
                .andExpect(jsonPath("$.data.hasIntegrityIssues").value(true))
                .andExpect(jsonPath("$.data.needsAttention").value(true))
                .andExpect(jsonPath("$.data.postsWithoutHash").value(10))
                .andExpect(jsonPath("$.data.hashCoverage").value(0.9))
                .andExpect(jsonPath("$.data.hashCoveragePercentage").value("90.00%"));
        
        verify(postService).getPostIntegrityStats();
    }
    
    @Test
    @DisplayName("手动触发完整性检查")
    void testTriggerIntegrityCheck() throws Exception {
        // Arrange
        PostService.PostIntegrityStats stats = PostService.PostIntegrityStats.builder()
                .totalPosts(100)
                .postsWithHash(95)
                .validPosts(93)
                .invalidPosts(2)
                .integrityRate(0.979)
                .build();
        
        when(postService.getPostIntegrityStats()).thenReturn(stats);
        
        // Act & Assert
        mockMvc.perform(post("/api/admin/monitoring/post-integrity/check/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("帖子完整性检查完成"))
                .andExpect(jsonPath("$.data.stats").exists())
                .andExpect(jsonPath("$.data.summary").exists())
                .andExpect(jsonPath("$.data.checkTime").exists())
                .andExpect(jsonPath("$.data.executionTimeMs").exists());
        
        verify(postService).getPostIntegrityStats();
    }
    
    @Test
    @DisplayName("获取完整性趋势数据")
    void testGetIntegrityTrend() throws Exception {
        // Arrange
        PostService.PostIntegrityStats stats = PostService.PostIntegrityStats.builder()
                .totalPosts(100)
                .postsWithHash(98)
                .validPosts(96)
                .invalidPosts(2)
                .integrityRate(0.98)
                .build();
        
        when(postService.getPostIntegrityStats()).thenReturn(stats);
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("帖子完整性趋势数据获取成功"))
                .andExpect(jsonPath("$.data.currentTime").exists())
                .andExpect(jsonPath("$.data.currentStats").exists())
                .andExpect(jsonPath("$.data.trend").value("stable"))
                .andExpect(jsonPath("$.data.recommendation").value("完整性状况良好"))
                .andExpect(jsonPath("$.data.nextCheckTime").exists());
        
        verify(postService).getPostIntegrityStats();
    }
    
    @Test
    @DisplayName("验证帖子完整性异常处理")
    void testVerifyPostIntegrity_Exception() throws Exception {
        // Arrange
        Long postId = 1L;
        when(postService.verifyPostIntegrity(postId))
            .thenThrow(new RuntimeException("数据库连接失败"));
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/verify/{postId}", postId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("验证帖子完整性失败: 数据库连接失败"));
        
        verify(postService).verifyPostIntegrity(postId);
    }
    
    @Test
    @DisplayName("获取统计信息异常处理")
    void testGetPostIntegrityStats_Exception() throws Exception {
        // Arrange
        when(postService.getPostIntegrityStats())
            .thenThrow(new RuntimeException("服务不可用"));
        
        // Act & Assert
        mockMvc.perform(get("/api/admin/monitoring/post-integrity/stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取帖子完整性统计失败: 服务不可用"));
        
        verify(postService).getPostIntegrityStats();
    }
}