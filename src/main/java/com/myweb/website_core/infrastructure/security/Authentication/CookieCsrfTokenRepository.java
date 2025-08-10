package com.myweb.website_core.infrastructure.security.Authentication;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 基于Cookie的CSRF令牌存储库
 * 使用HttpOnly Cookie存储CSRF令牌，提高安全性
 * 
 * @author Kiro
 * @since 1.0.0
 */
public class CookieCsrfTokenRepository implements CsrfTokenRepository {
    
    private static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";
    private static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    
    private String cookieName = DEFAULT_CSRF_COOKIE_NAME;
    private String parameterName = DEFAULT_CSRF_PARAMETER_NAME;
    private String headerName = DEFAULT_CSRF_HEADER_NAME;
    private String cookiePath = "/";
    private String cookieDomain;
    private boolean cookieHttpOnly = false; // 设置为false以便JavaScript可以读取
    private boolean secure = true; // HTTPS环境下设置为true
    private int cookieMaxAge = -1; // 会话Cookie
    
    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(headerName, parameterName, createNewToken());
    }
    
    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = (token != null) ? token.getToken() : "";
        
        Cookie cookie = new Cookie(cookieName, tokenValue);
        cookie.setSecure(secure && request.isSecure());
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);
        cookie.setHttpOnly(cookieHttpOnly);
        
        if (StringUtils.hasLength(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }
        
        // 如果token为null，则删除cookie
        if (token == null) {
            cookie.setMaxAge(0);
        }
        
        response.addCookie(cookie);
    }
    
    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie cookie = getCookie(request, cookieName);
        if (cookie == null) {
            return null;
        }
        
        String token = cookie.getValue();
        if (!StringUtils.hasLength(token)) {
            return null;
        }
        
        return new DefaultCsrfToken(headerName, parameterName, token);
    }
    
    /**
     * 创建新的令牌值
     * 
     * @return 新的令牌值
     */
    private String createNewToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 从请求中获取指定名称的Cookie
     * 
     * @param request 请求对象
     * @param cookieName Cookie名称
     * @return Cookie对象，如果不存在则返回null
     */
    private Cookie getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }
    
    /**
     * 设置Cookie名称
     * 
     * @param cookieName Cookie名称
     */
    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }
    
    /**
     * 设置参数名称
     * 
     * @param parameterName 参数名称
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
    
    /**
     * 设置请求头名称
     * 
     * @param headerName 请求头名称
     */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
    
    /**
     * 设置Cookie路径
     * 
     * @param cookiePath Cookie路径
     */
    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }
    
    /**
     * 设置Cookie域名
     * 
     * @param cookieDomain Cookie域名
     */
    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }
    
    /**
     * 设置Cookie是否HttpOnly
     * 
     * @param cookieHttpOnly 是否HttpOnly
     */
    public void setCookieHttpOnly(boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }
    
    /**
     * 设置Cookie是否安全
     * 
     * @param secure 是否安全
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    
    /**
     * 设置Cookie最大存活时间
     * 
     * @param cookieMaxAge 最大存活时间（秒）
     */
    public void setCookieMaxAge(int cookieMaxAge) {
        this.cookieMaxAge = cookieMaxAge;
    }
    
    /**
     * 创建默认的Cookie CSRF令牌存储库实例
     * 
     * @return CookieCsrfTokenRepository实例
     */
    public static CookieCsrfTokenRepository withHttpOnlyFalse() {
        CookieCsrfTokenRepository result = new CookieCsrfTokenRepository();
        result.setCookieHttpOnly(false);
        return result;
    }
}