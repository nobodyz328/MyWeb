package com.myweb.website_core.infrastructure.config;

import com.myweb.website_core.common.constant.SecurityConstants;
import com.myweb.website_core.infrastructure.security.CustomAccessDecisionVoter;
import com.myweb.website_core.infrastructure.security.CustomAccessDeniedHandler;
import com.myweb.website_core.infrastructure.security.CustomPermissionEvaluator;
import com.myweb.website_core.infrastructure.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;



@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;
    
    @Autowired
    private CustomPermissionEvaluator permissionEvaluator;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // 公开访问的资源
                .requestMatchers("/login", "/register", "/static/**", "/css/**", "/js/**", "/images/**", 
                               "/", "/view/**", "/users/register", "/users/login", "/users/register/code", 
                               "/post/*", "/api/posts", "/api/posts/*", "/api/images/*", "/posts/top-liked", 
                               "/search", "/announcements", "/posts/*/comments").permitAll()
                
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
                .accessDeniedHandler(accessDeniedHandler)
            )
            .logout(logout -> logout
                    .logoutUrl("/user/logout")
                .logoutSuccessUrl("/view")
                .permitAll()
            )
            .csrf(AbstractHttpConfigurer::disable);
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
     * 配置BCrypt密码编码器Bean（用于依赖注入）
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
}