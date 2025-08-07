package com.myweb.website_core.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.security.IPS.ratelimit.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 访问频率限制过滤器集成测试
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class RateLimitingFilterIntegrationTest {
    
    @Autowired
    private WebApplicationContext context;
    
    @MockBean
    private RateLimitingService rateLimitingService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }
    
    @Test
    void testRateLimitingFilter_WhenAllowed_ShouldProcessRequest() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(anyString(), anyString(), any()))
            .thenReturn(true);
        
        RateLimitingService.RateLimitStatus status = 
            new RateLimitingService.RateLimitStatus(true, 5, 0, 10);
        when(rateLimitingService.getRateLimitStatus(anyString(), anyString(), any()))
            .thenReturn(status);
        
        // When & Then
        mockMvc.perform(get("/view")
                .header("X-Forwarded-For", "192.168.1.1"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "10"))
            .andExpect(header().string("X-RateLimit-Remaining", "5"))
            .andExpect(header().exists("X-RateLimit-Reset"));
    }
    
    @Test
    void testRateLimitingFilter_WhenBlocked_ShouldReturnTooManyRequests() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(anyString(), anyString(), any()))
            .thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/test")
                .header("X-Forwarded-For", "192.168.1.1")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"))
            .andExpect(jsonPath("$.message").value("访问频率过高，请稍后再试"))
            .andExpect(header().string("Retry-After", "60"));
    }
    
    @Test
    void testRateLimitingFilter_WithStaticResources_ShouldSkip() throws Exception {
        // Given - 静态资源不应该被频率限制
        
        // When & Then
        mockMvc.perform(get("/static/css/style.css")
                .header("X-Forwarded-For", "192.168.1.1"))
            .andExpect(status().isNotFound()); // 404 because file doesn't exist, but not rate limited
    }
    
    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void testRateLimitingFilter_WithAuthenticatedUser_ShouldIncludeUsername() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(eq("192.168.1.1"), anyString(), eq("testuser")))
            .thenReturn(true);
        
        RateLimitingService.RateLimitStatus status = 
            new RateLimitingService.RateLimitStatus(true, 3, 2, 10);
        when(rateLimitingService.getRateLimitStatus(eq("192.168.1.1"), anyString(), eq("testuser")))
            .thenReturn(status);
        
        // When & Then
        mockMvc.perform(get("/view")
                .header("X-Forwarded-For", "192.168.1.1"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-IP-Count", "3"))
            .andExpect(header().string("X-RateLimit-User-Count", "2"));
    }
    
    @Test
    void testRateLimitingFilter_WithMultipleIpHeaders_ShouldUseFirstValid() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(eq("10.0.0.1"), anyString(), any()))
            .thenReturn(true);
        
        RateLimitingService.RateLimitStatus status = 
            new RateLimitingService.RateLimitStatus(true, 1, 0, 10);
        when(rateLimitingService.getRateLimitStatus(eq("10.0.0.1"), anyString(), any()))
            .thenReturn(status);
        
        // When & Then
        mockMvc.perform(get("/view")
                .header("X-Forwarded-For", "10.0.0.1, 192.168.1.1")
                .header("X-Real-IP", "172.16.0.1"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testRateLimitingFilter_WhenServiceThrowsException_ShouldAllowRequest() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When & Then - Should allow request when rate limiting service fails
        mockMvc.perform(get("/view")
                .header("X-Forwarded-For", "192.168.1.1"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testRateLimitingFilter_NonAjaxRequest_ShouldRedirectToErrorPage() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(anyString(), anyString(), any()))
            .thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/test")
                .header("X-Forwarded-For", "192.168.1.1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "/error/rate-limit"));
    }
    
    @Test
    void testRateLimitingFilter_GetRequest_ShouldCheckIfReadOnly() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(anyString(), anyString(), any()))
            .thenReturn(true);
        
        RateLimitingService.RateLimitStatus status = 
            new RateLimitingService.RateLimitStatus(true, 0, 0, 10);
        when(rateLimitingService.getRateLimitStatus(anyString(), anyString(), any()))
            .thenReturn(status);
        
        // When & Then - GET requests to read-only endpoints should be processed
        mockMvc.perform(get("/api/posts")
                .header("X-Forwarded-For", "192.168.1.1"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testRateLimitingFilter_InvalidIpAddress_ShouldUseRemoteAddr() throws Exception {
        // Given
        when(rateLimitingService.isAllowed(anyString(), anyString(), any()))
            .thenReturn(true);
        
        RateLimitingService.RateLimitStatus status = 
            new RateLimitingService.RateLimitStatus(true, 0, 0, 10);
        when(rateLimitingService.getRateLimitStatus(anyString(), anyString(), any()))
            .thenReturn(status);
        
        // When & Then
        mockMvc.perform(get("/view")
                .header("X-Forwarded-For", "unknown")
                .header("X-Real-IP", "invalid-ip"))
            .andExpect(status().isOk());
    }
}