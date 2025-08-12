package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * XSS监控控制器单元测试
 * <p>
 * 测试XssMonitoringController的各种功能：
 * 1. 统计数据查询接口
 * 2. 监控数据查询接口
 * 3. 配置管理接口
 * 4. 权限控制
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - XSS监控接口测试
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-01-01
 */
@WebMvcTest(XssMonitoringController.class)
class XssMonitoringControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private XssStatisticsService xssStatisticsService;
    
    @MockBean
    private XssMonitoringService xssMonitoringService;
    
    @MockBean
    private XssFilterConfig xssFilterConfig;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetStatistics_ShouldReturnStatisticsReport() throws Exception {
        // Given
        XssStatisticsService.XssStatisticsReport report = XssStatisticsService.XssStatisticsReport.builder()
                .totalAttackCount(100L)
                .todayAttackCount(10L)
                .hourlyAttackCount(2L)
                .ipAttackStats(Map.of("192.168.1.1", 5L))
                .attackTypeStats(Map.of("script_injection", 8L))
                .uriAttackStats(Map.of("/api/posts", 6L))
                .timeWindowStats(Map.of("2025-01-11-14", 3L))
                .reportTime(LocalDateTime.now())
                .build();
        
        when(xssStatisticsService.getStatisticsReport()).thenReturn(report);
        
        // When & Then
        mockMvc.perform(get("/api/security/xss/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttackCount").value(100))
                .andExpect(jsonPath("$.todayAttackCount").value(10))
                .andExpect(jsonPath("$.hourlyAttackCount").value(2));
        
        verify(xssStatisticsService).getStatisticsReport();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetEffectiveness_ShouldReturnEffectivenessData() throws Exception {
        // Given
        XssMonitoringService.XssProtectionEffectiveness effectiveness = 
                XssMonitoringService.XssProtectionEffectiveness.builder()
                        .totalRequests(1000L)
                        .blockedAttacks(50L)
                        .allowedRequests(950L)
                        .blockRate(5.0)
                        .averageProcessingTimeMs(15.5)
                        .currentTimeWindow("2025-01-11-14")
                        .timeWindowAttacks(3L)
                        .build();
        
        when(xssMonitoringService.getProtectionEffectiveness()).thenReturn(effectiveness);
        
        // When & Then
        mockMvc.perform(get("/api/security/xss/effectiveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(1000))
                .andExpect(jsonPath("$.blockedAttacks").value(50))
                .andExpect(jsonPath("$.blockRate").value(5.0))
                .andExpect(jsonPath("$.averageProcessingTimeMs").value(15.5));
        
        verify(xssMonitoringService).getProtectionEffectiveness();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetOverview_ShouldReturnOverviewData() throws Exception {
        // Given
        when(xssStatisticsService.getTotalAttackCount()).thenReturn(100L);
        when(xssStatisticsService.getTodayAttackCount()).thenReturn(10L);
        when(xssStatisticsService.getHourlyAttackCount()).thenReturn(2L);
        
        XssMonitoringService.XssProtectionEffectiveness effectiveness = 
                XssMonitoringService.XssProtectionEffectiveness.builder()
                        .blockRate(5.0)
                        .averageProcessingTimeMs(15.5)
                        .build();
        when(xssMonitoringService.getProtectionEffectiveness()).thenReturn(effectiveness);
        
        when(xssFilterConfig.isEnabled()).thenReturn(true);
        when(xssFilterConfig.isStrictMode()).thenReturn(false);
        
        XssFilterConfig.PerformanceConfig performanceConfig = new XssFilterConfig.PerformanceConfig();
        performanceConfig.setCacheEnabled(true);
        when(xssFilterConfig.getPerformance()).thenReturn(performanceConfig);
        
        XssFilterConfig.MonitoringConfig monitoringConfig = new XssFilterConfig.MonitoringConfig();
        monitoringConfig.setEnabled(true);
        when(xssFilterConfig.getMonitoring()).thenReturn(monitoringConfig);
        
        // When & Then
        mockMvc.perform(get("/api/security/xss/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttacks").value(100))
                .andExpect(jsonPath("$.todayAttacks").value(10))
                .andExpect(jsonPath("$.hourlyAttacks").value(2))
                .andExpect(jsonPath("$.blockRate").value(5.0))
                .andExpect(jsonPath("$.avgProcessingTime").value(15.5))
                .andExpect(jsonPath("$.xssProtectionEnabled").value(true))
                .andExpect(jsonPath("$.strictMode").value(false))
                .andExpect(jsonPath("$.cacheEnabled").value(true))
                .andExpect(jsonPath("$.monitoringEnabled").value(true));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetIpStats_ShouldReturnIpStatistics() throws Exception {
        // Given
        Map<String, Long> ipStats = Map.of(
                "192.168.1.1", 10L,
                "192.168.1.2", 5L
        );
        when(xssStatisticsService.getIpAttackStats()).thenReturn(ipStats);
        
        // When & Then
        mockMvc.perform(get("/api/security/xss/ip-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['192.168.1.1']").value(10))
                .andExpect(jsonPath("$['192.168.1.2']").value(5));
        
        verify(xssStatisticsService).getIpAttackStats();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAttackTypes_ShouldReturnAttackTypeStatistics() throws Exception {
        // Given
        Map<String, Long> attackTypes = Map.of(
                "script_injection", 15L,
                "javascript_protocol", 8L
        );
        when(xssStatisticsService.getAttackTypeStats()).thenReturn(attackTypes);
        
        // When & Then
        mockMvc.perform(get("/api/security/xss/attack-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.script_injection").value(15))
                .andExpect(jsonPath("$.javascript_protocol").value(8));
        
        verify(xssStatisticsService).getAttackTypeStats();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetConfig_ShouldReturnConfiguration() throws Exception {
        // Given
        when(xssFilterConfig.isEnabled()).thenReturn(true);
        when(xssFilterConfig.isStrictMode()).thenReturn(false);
        when(xssFilterConfig.getAllowedTags()).thenReturn(Set.of("b", "i", "u"));
        when(xssFilterConfig.getAllowedAttributes()).thenReturn(Set.of("href", "src"));
        when(xssFilterConfig.getTagSpecificAttributes()).thenReturn(Map.of("a", Set.of("href")));
        when(xssFilterConfig.isRemoveUnknownTags()).thenReturn(true);
        when(xssFilterConfig.isEncodeSpecialChars()).thenReturn(true);
        when(xssFilterConfig.getMaxTagDepth()).thenReturn(10);
        when(xssFilterConfig.getMaxContentLength()).thenReturn(50000);
        when(xssFilterConfig.getWhitelistUrlPatterns()).thenReturn(java.util.List.of("/api/admin/**"));
        
        XssFilterConfig.PerformanceConfig performanceConfig = new XssFilterConfig.PerformanceConfig();
        XssFilterConfig.StatisticsConfig statisticsConfig = new XssFilterConfig.StatisticsConfig();
        XssFilterConfig.MonitoringConfig monitoringConfig = new XssFilterConfig.MonitoringConfig();
        
        when(xssFilterConfig.getPerformance()).thenReturn(performanceConfig);
        when(xssFilterConfig.getStatistics()).thenReturn(statisticsConfig);
        when(xssFilterConfig.getMonitoring()).thenReturn(monitoringConfig);
        
        // When & Then
        mockMvc.perform(get("/api/security/xss/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.strictMode").value(false))
                .andExpect(jsonPath("$.removeUnknownTags").value(true))
                .andExpect(jsonPath("$.encodeSpecialChars").value(true))
                .andExpect(jsonPath("$.maxTagDepth").value(10))
                .andExpect(jsonPath("$.maxContentLength").value(50000));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateConfig_ShouldAcceptConfigUpdates() throws Exception {
        // Given
        Map<String, Object> configUpdates = new HashMap<>();
        configUpdates.put("strictMode", true);
        configUpdates.put("maxContentLength", 60000);
        
        // When & Then
        mockMvc.perform(put("/api/security/xss/config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configUpdates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.receivedUpdates").exists());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testClearStatistics_ShouldReturnSuccessMessage() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/security/xss/statistics")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void testGetStatistics_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/xss/statistics"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testGetStatistics_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/security/xss/statistics"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetStatistics_ServiceException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(xssStatisticsService.getStatisticsReport()).thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        mockMvc.perform(get("/api/security/xss/statistics"))
                .andExpect(status().isInternalServerError());
    }
}