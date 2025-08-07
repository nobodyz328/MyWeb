package com.myweb.website_core.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.Cookie;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CSRF防护集成测试
 * 测试CSRF令牌的生成、验证和异常处理
 * 
 * @author Kiro
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class CsrfProtectionIntegrationTest {
    
    @Autowired
    private WebApplicationContext context;
    
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
    void testGetCsrfToken() throws Exception {
        // 获取CSRF令牌
        MvcResult result = mockMvc.perform(get("/api/csrf/token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.parameterName").value("_csrf"))
                .andReturn();
        
        // 验证Cookie是否设置
        Cookie[] cookies = result.getResponse().getCookies();
        boolean csrfCookieFound = false;
        for (Cookie cookie : cookies) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                csrfCookieFound = true;
                assertNotNull(cookie.getValue());
                assertFalse(cookie.getValue().isEmpty());
                assertEquals("/blog", cookie.getPath());
                assertTrue(cookie.getSecure());
                assertEquals(7200, cookie.getMaxAge());
                break;
            }
        }
        assertTrue(csrfCookieFound, "CSRF cookie should be set");
    }
    
    @Test
    @WithMockUser
    void testCsrfProtectionWithValidToken() throws Exception {
        // 首先获取CSRF令牌
        MvcResult tokenResult = mockMvc.perform(get("/api/csrf/token"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseBody = tokenResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> tokenData = (Map<String, String>) response.get("data");
        String csrfToken = tokenData.get("token");
        
        // 获取CSRF Cookie
        Cookie csrfCookie = null;
        for (Cookie cookie : tokenResult.getResponse().getCookies()) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                csrfCookie = cookie;
                break;
            }
        }
        assertNotNull(csrfCookie);
        
        // 使用有效的CSRF令牌发送POST请求
        mockMvc.perform(post("/posts/create")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Post\",\"content\":\"Test Content\"}"))
                .andExpect(status().isNotFound()); // 404因为端点不存在，但CSRF验证通过
    }
    
    @Test
    @WithMockUser
    void testCsrfProtectionWithInvalidToken() throws Exception {
        // 使用无效的CSRF令牌发送POST请求
        mockMvc.perform(post("/posts/create")
                .header("X-XSRF-TOKEN", "invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Post\",\"content\":\"Test Content\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("csrf_token_invalid"))
                .andExpect(jsonPath("$.message").value("CSRF令牌无效或已过期，请刷新页面后重试"));
    }
    
    @Test
    @WithMockUser
    void testCsrfProtectionWithMissingToken() throws Exception {
        // 不提供CSRF令牌发送POST请求
        mockMvc.perform(post("/posts/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Post\",\"content\":\"Test Content\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("csrf_token_invalid"));
    }
    
    @Test
    void testCsrfProtectionIgnoredPaths() throws Exception {
        // 测试忽略CSRF保护的路径
        String[] ignoredPaths = {
            "/login",
            "/register", 
            "/static/test.css",
            "/css/test.css",
            "/js/test.js",
            "/images/test.jpg",
            "/",
            "/view/test",
            "/users/register",
            "/users/login",
            "/api/csrf/token"
        };
        
        for (String path : ignoredPaths) {
            mockMvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isNotFound()); // 404因为端点不存在，但CSRF验证被忽略
        }
    }
    
    @Test
    void testRefreshCsrfToken() throws Exception {
        // 首先获取初始令牌
        MvcResult initialResult = mockMvc.perform(get("/api/csrf/token"))
                .andExpect(status().isOk())
                .andReturn();
        
        String initialResponseBody = initialResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> initialResponse = objectMapper.readValue(initialResponseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> initialTokenData = (Map<String, String>) initialResponse.get("data");
        String initialToken = initialTokenData.get("token");
        
        // 获取初始Cookie
        Cookie initialCookie = null;
        for (Cookie cookie : initialResult.getResponse().getCookies()) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                initialCookie = cookie;
                break;
            }
        }
        assertNotNull(initialCookie);
        
        // 刷新令牌
        MvcResult refreshResult = mockMvc.perform(post("/api/csrf/refresh")
                .cookie(initialCookie)
                .header("X-XSRF-TOKEN", initialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        
        String refreshResponseBody = refreshResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> refreshResponse = objectMapper.readValue(refreshResponseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> refreshTokenData = (Map<String, String>) refreshResponse.get("data");
        String refreshedToken = refreshTokenData.get("token");
        
        // 验证新令牌与旧令牌不同
        assertNotEquals(initialToken, refreshedToken);
        
        // 验证新Cookie被设置
        Cookie[] refreshCookies = refreshResult.getResponse().getCookies();
        boolean newCsrfCookieFound = false;
        for (Cookie cookie : refreshCookies) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                newCsrfCookieFound = true;
                assertEquals(refreshedToken, cookie.getValue());
                break;
            }
        }
        assertTrue(newCsrfCookieFound, "New CSRF cookie should be set after refresh");
    }
    
    @Test
    void testValidateCsrfToken() throws Exception {
        // 获取CSRF令牌
        MvcResult tokenResult = mockMvc.perform(get("/api/csrf/token"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseBody = tokenResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> tokenData = (Map<String, String>) response.get("data");
        String csrfToken = tokenData.get("token");
        
        // 获取Cookie
        Cookie csrfCookie = null;
        for (Cookie cookie : tokenResult.getResponse().getCookies()) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                csrfCookie = cookie;
                break;
            }
        }
        assertNotNull(csrfCookie);
        
        // 验证有效令牌
        mockMvc.perform(post("/api/csrf/validate")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + csrfToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.valid").value(true));
        
        // 验证无效令牌
        mockMvc.perform(post("/api/csrf/validate")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"invalid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.valid").value(false));
    }
    
    @Test
    void testClearCsrfToken() throws Exception {
        // 获取CSRF令牌
        MvcResult tokenResult = mockMvc.perform(get("/api/csrf/token"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseBody = tokenResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> tokenData = (Map<String, String>) response.get("data");
        String csrfToken = tokenData.get("token");
        
        // 获取Cookie
        Cookie csrfCookie = null;
        for (Cookie cookie : tokenResult.getResponse().getCookies()) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                csrfCookie = cookie;
                break;
            }
        }
        assertNotNull(csrfCookie);
        
        // 清除令牌
        MvcResult clearResult = mockMvc.perform(delete("/api/csrf/token")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        
        // 验证Cookie被清除（maxAge设置为0）
        Cookie[] clearCookies = clearResult.getResponse().getCookies();
        boolean clearedCookieFound = false;
        for (Cookie cookie : clearCookies) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                clearedCookieFound = true;
                assertEquals(0, cookie.getMaxAge());
                break;
            }
        }
        assertTrue(clearedCookieFound, "CSRF cookie should be cleared");
    }
    
    @Test
    void testAjaxCsrfErrorResponse() throws Exception {
        // 模拟Ajax请求（带有X-Requested-With头）
        mockMvc.perform(post("/posts/create")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("X-XSRF-TOKEN", "invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Post\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("csrf_token_invalid"))
                .andExpect(jsonPath("$.action").value("refresh_token"))
                .andExpect(jsonPath("$.refresh_url").value("/api/csrf/token"));
    }
}