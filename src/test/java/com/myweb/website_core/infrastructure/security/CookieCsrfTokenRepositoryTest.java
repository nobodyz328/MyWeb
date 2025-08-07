package com.myweb.website_core.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.csrf.CsrfToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Cookie CSRF令牌存储库单元测试
 * 
 * @author Kiro
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CookieCsrfTokenRepositoryTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private CookieCsrfTokenRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new CookieCsrfTokenRepository();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setParameterName("_csrf");
        repository.setCookiePath("/blog");
        repository.setSecure(true);
        repository.setCookieMaxAge(7200);
        repository.setCookieHttpOnly(false);
    }
    
    @Test
    void testGenerateToken() {
        // 生成令牌
        CsrfToken token = repository.generateToken(request);
        
        // 验证令牌属性
        assertNotNull(token);
        assertEquals("X-XSRF-TOKEN", token.getHeaderName());
        assertEquals("_csrf", token.getParameterName());
        assertNotNull(token.getToken());
        assertFalse(token.getToken().isEmpty());
        
        // 验证令牌格式（UUID格式）
        assertTrue(token.getToken().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }
    
    @Test
    void testSaveToken() {
        // 准备令牌
        CsrfToken token = repository.generateToken(request);
        when(request.isSecure()).thenReturn(true);
        
        // 保存令牌
        repository.saveToken(token, request, response);
        
        // 验证Cookie被添加
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        
        Cookie savedCookie = cookieCaptor.getValue();
        assertEquals("XSRF-TOKEN", savedCookie.getName());
        assertEquals(token.getToken(), savedCookie.getValue());
        assertEquals("/blog", savedCookie.getPath());
        assertEquals(7200, savedCookie.getMaxAge());
        assertTrue(savedCookie.getSecure());
        assertFalse(savedCookie.isHttpOnly());
    }
    
    @Test
    void testSaveTokenWithInsecureRequest() {
        // 准备令牌
        CsrfToken token = repository.generateToken(request);
        when(request.isSecure()).thenReturn(false);
        
        // 保存令牌
        repository.saveToken(token, request, response);
        
        // 验证Cookie安全属性
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        
        Cookie savedCookie = cookieCaptor.getValue();
        assertFalse(savedCookie.getSecure()); // 应该为false，因为请求不安全
    }
    
    @Test
    void testSaveNullToken() {
        // 保存null令牌（删除令牌）
        repository.saveToken(null, request, response);
        
        // 验证Cookie被设置为删除
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        
        Cookie savedCookie = cookieCaptor.getValue();
        assertEquals("XSRF-TOKEN", savedCookie.getName());
        assertEquals("", savedCookie.getValue());
        assertEquals(0, savedCookie.getMaxAge()); // 删除Cookie
    }
    
    @Test
    void testLoadToken() {
        // 准备Cookie
        String tokenValue = "test-csrf-token-12345";
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", tokenValue);
        Cookie[] cookies = {csrfCookie};
        
        when(request.getCookies()).thenReturn(cookies);
        
        // 加载令牌
        CsrfToken loadedToken = repository.loadToken(request);
        
        // 验证令牌
        assertNotNull(loadedToken);
        assertEquals("X-XSRF-TOKEN", loadedToken.getHeaderName());
        assertEquals("_csrf", loadedToken.getParameterName());
        assertEquals(tokenValue, loadedToken.getToken());
    }
    
    @Test
    void testLoadTokenWithNoCookies() {
        // 没有Cookie
        when(request.getCookies()).thenReturn(null);
        
        // 加载令牌
        CsrfToken loadedToken = repository.loadToken(request);
        
        // 验证返回null
        assertNull(loadedToken);
    }
    
    @Test
    void testLoadTokenWithEmptyCookies() {
        // 空Cookie数组
        when(request.getCookies()).thenReturn(new Cookie[0]);
        
        // 加载令牌
        CsrfToken loadedToken = repository.loadToken(request);
        
        // 验证返回null
        assertNull(loadedToken);
    }
    
    @Test
    void testLoadTokenWithWrongCookieName() {
        // 准备错误名称的Cookie
        Cookie wrongCookie = new Cookie("WRONG-TOKEN", "test-value");
        Cookie[] cookies = {wrongCookie};
        
        when(request.getCookies()).thenReturn(cookies);
        
        // 加载令牌
        CsrfToken loadedToken = repository.loadToken(request);
        
        // 验证返回null
        assertNull(loadedToken);
    }
    
    @Test
    void testLoadTokenWithEmptyValue() {
        // 准备空值Cookie
        Cookie emptyCookie = new Cookie("XSRF-TOKEN", "");
        Cookie[] cookies = {emptyCookie};
        
        when(request.getCookies()).thenReturn(cookies);
        
        // 加载令牌
        CsrfToken loadedToken = repository.loadToken(request);
        
        // 验证返回null
        assertNull(loadedToken);
    }
    
    @Test
    void testLoadTokenWithNullValue() {
        // 准备null值Cookie
        Cookie nullCookie = new Cookie("XSRF-TOKEN", null);
        Cookie[] cookies = {nullCookie};
        
        when(request.getCookies()).thenReturn(cookies);
        
        // 加载令牌
        CsrfToken loadedToken = repository.loadToken(request);
        
        // 验证返回null
        assertNull(loadedToken);
    }
    
    @Test
    void testLoadTokenWithMultipleCookies() {
        // 准备多个Cookie，包括CSRF Cookie
        String tokenValue = "test-csrf-token-12345";
        Cookie otherCookie = new Cookie("OTHER-COOKIE", "other-value");
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", tokenValue);
        Cookie anotherCookie = new Cookie("ANOTHER-COOKIE", "another-value");
        Cookie[] cookies = {otherCookie, csrfCookie, anotherCookie};
        
        when(request.getCookies()).thenReturn(cookies);
        
        // 加载令牌
        CsrfToken loadedToken = repository.loadToken(request);
        
        // 验证令牌
        assertNotNull(loadedToken);
        assertEquals(tokenValue, loadedToken.getToken());
    }
    
    @Test
    void testWithHttpOnlyFalse() {
        // 使用静态工厂方法创建实例
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        
        // 生成并保存令牌
        CsrfToken token = repository.generateToken(request);
        when(request.isSecure()).thenReturn(true);
        repository.saveToken(token, request, response);
        
        // 验证HttpOnly设置
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        
        Cookie savedCookie = cookieCaptor.getValue();
        assertFalse(savedCookie.isHttpOnly());
    }
    
    @Test
    void testSetCookieDomain() {
        // 设置Cookie域名
        repository.setCookieDomain("example.com");
        
        CsrfToken token = repository.generateToken(request);
        when(request.isSecure()).thenReturn(true);
        repository.saveToken(token, request, response);
        
        // 验证域名设置
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        
        Cookie savedCookie = cookieCaptor.getValue();
        assertEquals("example.com", savedCookie.getDomain());
    }
    
    @Test
    void testCustomConfiguration() {
        // 自定义配置
        repository.setCookieName("CUSTOM-CSRF");
        repository.setHeaderName("X-CUSTOM-CSRF");
        repository.setParameterName("custom_csrf");
        repository.setCookiePath("/custom");
        repository.setCookieMaxAge(3600);
        repository.setCookieHttpOnly(true);
        
        // 生成令牌
        CsrfToken token = repository.generateToken(request);
        
        // 验证令牌属性
        assertEquals("X-CUSTOM-CSRF", token.getHeaderName());
        assertEquals("custom_csrf", token.getParameterName());
        
        // 保存令牌
        when(request.isSecure()).thenReturn(true);
        repository.saveToken(token, request, response);
        
        // 验证Cookie属性
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        
        Cookie savedCookie = cookieCaptor.getValue();
        assertEquals("CUSTOM-CSRF", savedCookie.getName());
        assertEquals("/custom", savedCookie.getPath());
        assertEquals(3600, savedCookie.getMaxAge());
        assertTrue(savedCookie.isHttpOnly());
    }
}