package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.common.constant.SecurityConstants;
import com.myweb.website_core.infrastructure.security.*;
import com.myweb.website_core.infrastructure.security.filter.ContentCachingFilter;
import com.myweb.website_core.infrastructure.security.filter.JwtAuthenticationFilter;
import com.myweb.website_core.infrastructure.security.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;



@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;
    private final UnifiedAccessDeniedHandler unifiedAccessDeniedHandler;
    private final CustomPermissionEvaluator permissionEvaluator;
    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    //private final ContentCachingFilter contentCachingFilter;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 配置会话管理为无状态（JWT模式）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                // 公开访问的资源
                .requestMatchers("/login", "/register", "/static/**", "/css/**", "/js/**", "/images/**", 
                               "/", "/view/**", "/users/register", "/users/login", "/users/register/code", 
                               "/users/refresh-token", "/users/check-username", "/users/check-email",
                               "/post/*", "/api/posts", "/api/posts/*", "/api/images/*", "/posts/top-liked", 
                               "/search", "/announcements", "/posts/*/comments", "/error/**").permitAll()
                
                // 需要认证的基本功能
                .requestMatchers("/posts/new", "/posts/create").hasAuthority("POST_CREATE")
                .requestMatchers("/posts/edit/**").hasAuthority("POST_UPDATE")
                .requestMatchers("/posts/*/delete").hasAuthority("POST_DELETE")
                
                // 管理功能 - 需要管理权限
                .requestMatchers("/admin/**").hasAuthority("SYSTEM_MANAGE")
                .requestMatchers("/api/admin/**").hasAuthority("SYSTEM_MANAGE")
                
                // 用户管理功能
                .requestMatchers("/users/manage/**").hasAuthority("USER_MANAGE")
                .requestMatchers("/api/users/manage/**").hasAuthority("USER_MANAGE")
                
                // 评论管理功能
                .requestMatchers("/comments/manage/**").hasAuthority("COMMENT_MANAGE")
                .requestMatchers("/api/comments/manage/**").hasAuthority("COMMENT_MANAGE")
                
                // 审计日志查看
                .requestMatchers("/audit/**").hasAuthority("AUDIT_READ")
                .requestMatchers("/api/audit/**").hasAuthority("AUDIT_READ")
                
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            .userDetailsService(userDetailsService)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(unifiedAccessDeniedHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/users/logout")
                .logoutSuccessUrl("/view")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .ignoringRequestMatchers(
                    // API接口不需要CSRF保护（使用JWT）
                    "/api/**",
                    // 公开访问的资源不需要CSRF保护
                    "/login", "/register", "/static/**", "/css/**", "/js/**", "/images/**",
                    "/", "/view/**", "/users/register", "/users/login", "/users/register/code",
                    "/users/refresh-token", "/users/logout", "/users/check-username", "/users/check-email",
                    "/post/*", "/api/posts", "/api/posts/*", "/api/images/*", "/posts/top-liked",
                    "/search", "/announcements", "/posts/*/comments", "/error/**",
                    // CSRF令牌获取接口
                    "/api/csrf/token"
                )
            )
            // 添加过滤器
                //.addFilterBefore(contentCachingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    
    /**
     * 配置BCrypt密码编码器
     * 使用强度12，符合GB/T 22239-2019身份鉴别要求
     * 
     * @return BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(SecurityConstants.BCRYPT_STRENGTH);
    }
    
    /**
     * 配置BCrypt密码编码器Bean
     * 
     * @return BCrypt密码编码器
     */
    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder(SecurityConstants.BCRYPT_STRENGTH);
    }
    

    
    /**
     * 配置认证管理器
     * 
     * @param authConfig 认证配置
     * @return 认证管理器
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * 配置DAO认证提供者
     * 
     * @return DAO认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * 配置方法级安全表达式处理器
     * 支持在@PreAuthorize注解中使用hasPermission()表达式
     * 
     * @return 方法安全表达式处理器
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }
    
    /**
     * 配置CSRF令牌存储库
     * 使用Cookie存储CSRF令牌
     * 
     * @return CSRF令牌存储库
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setParameterName("_csrf");
        repository.setCookiePath("/blog");
        repository.setSecure(true); // HTTPS环境下设置为true
        repository.setCookieMaxAge(7200); // 2小时
        return repository;
    }
}