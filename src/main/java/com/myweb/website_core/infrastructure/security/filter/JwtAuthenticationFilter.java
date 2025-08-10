package com.myweb.website_core.infrastructure.security.filter;

import com.myweb.website_core.application.service.security.authentication.JWT.JwtService;
import com.myweb.website_core.application.service.security.authentication.JWT.JwtTokenService;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.infrastructure.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * <p>
 * 拦截HTTP请求，验证JWT令牌并设置Spring Security上下文
 * 支持Bearer令牌格式：Authorization: Bearer <token>
 * <p>
 * 符合GB/T 22239-2019身份鉴别要求
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 提取JWT令牌
            String jwt = extractJwtFromRequest(request);
            
            log.debug("处理请求: {}, JWT令牌: {}", request.getRequestURI(), jwt != null ? "存在" : "不存在");
            
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 验证令牌
                boolean isValidToken = jwtTokenService.validateToken(jwt);
                boolean isAccessToken = jwtService.isAccessToken(jwt);
                
                log.debug("JWT令牌验证结果: valid={}, isAccessToken={}", isValidToken, isAccessToken);
                
                if (isValidToken && isAccessToken) {
                    // 从令牌中获取用户信息
                    String username = jwtService.getUsernameFromToken(jwt);
                    Long userId = jwtService.getUserIdFromToken(jwt);
                    
                    log.debug("从JWT令牌提取用户信息: username={}, userId={}", username, userId);
                    
                    if (StringUtils.hasText(username) && userId != null) {
                        // 加载用户详情
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        if (userDetails != null) {
                            // 创建认证对象
                            UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());
                            
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            // 设置安全上下文
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            log.debug("JWT认证成功: username={}, userId={}, authorities={}", 
                                    username, userId, userDetails.getAuthorities());
                        } else {
                            log.debug("无法加载用户详情: username={}", username);
                        }
                    } else {
                        log.debug("JWT令牌中缺少用户信息: username={}, userId={}", username, userId);
                    }
                } else {
                    log.debug("JWT令牌验证失败或非访问令牌: valid={}, isAccessToken={}", isValidToken, isAccessToken);
                }
            } else if (!StringUtils.hasText(jwt)) {
                log.debug("请求中未找到JWT令牌");
            } else {
                log.debug("安全上下文中已存在认证信息");
            }
            
        } catch (Exception e) {
            log.error("JWT认证过滤器处理失败", e);
            // 清除可能的部分认证状态
            SecurityContextHolder.clearContext();
        }
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求中提取JWT令牌
     * 
     * @param request HTTP请求
     * @return JWT令牌，如果不存在则返回null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTHORIZATION_HEADER.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        return jwtService.extractTokenFromBearer(bearerToken);

    }
    
    /**
     * 判断是否应该跳过JWT认证
     * 可以在这里添加不需要认证的路径
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 移除context path前缀进行匹配
//        String contextPath = request.getContextPath();
//        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
//            path = path.substring(contextPath.length());
//        }
        
        // 跳过登录、注册等公开接口
        return path.startsWith("/users/login") ||
                path.startsWith("/users/register") ||
                path.startsWith("/users/check-") ||
                path.startsWith("/public/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger-") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars/") ||
                path.equals("/") ||
                path.startsWith("/view") ||
                path.startsWith("/static/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/api/csrf/")||
                path.startsWith("/error/");
    }
}