package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.application.service.integration.EmailService;
import com.myweb.website_core.domain.business.dto.UserLoginResponse;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.infrastructure.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务
 * 
 * 使用Spring Security标准流程处理用户认证
 * 符合GB/T 22239-2019二级等保要求的身份鉴别机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    

    
    /**
     * 用户登录
     * 
     * @param username 用户名或邮箱
     * @param password 密码
     * @param code 验证码
     * @param ipAddress 客户端IP地址
     * @return 登录响应
     */
    @Transactional
    public UserLoginResponse login(String username, String password, String code, String ipAddress) {
        try {
            // 查找用户
            User user = userRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));
            
            // 检查账户锁定状态
            if (user.isAccountLocked()) {
                throw new LockedException("账户已被锁定，请稍后再试");
            }
            
            // 检查邮箱
//            if (!user.getEmailVerified()) {
//                throw new DisabledException("账户未激活，请先验证邮箱");
//            }
            
            // 使用Spring Security进行认证
            UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(username, password);
            
            Authentication authentication = authenticationManager.authenticate(authToken);
            
            // 认证成功，设置安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 更新用户登录信息
            user.resetLoginAttempts();
            user.updateLastLoginInfo(LocalDateTime.now(), ipAddress);
            userRepository.save(user);
            
            log.info("用户登录成功: {}, IP: {}", username, ipAddress);
            
            // 返回登录响应
            return createLoginResponse(user);
            
        } catch (AuthenticationException e) {
            // 认证失败，增加失败次数
            userRepository.findByUsernameOrEmail(username, username)
                    .ifPresent(user -> {
                        user.incrementLoginAttempts();
                        userRepository.save(user);
                    });
            
            log.warn("用户登录失败: {}, IP: {}, 原因: {}", username, ipAddress, e.getMessage());
            throw new RuntimeException("用户名或密码错误");
        }
    }
    
    /**
     * 用户登出
     * 
     * @return 登出结果
     */
    public String logout() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "unknown";
            
            // 清除安全上下文
            SecurityContextHolder.clearContext();
            
            log.info("用户退出登录成功: {}", username);
            return "退出登录成功";
            
        } catch (Exception e) {
            log.error("用户退出登录失败: ", e);
            throw new RuntimeException("退出登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前认证用户
     * 
     * @return 当前用户，如果未认证则返回null
     */
    public User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
                
                if (authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal) {
                    CustomUserDetailsService.CustomUserPrincipal principal = 
                            (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
                    return principal.getUser();
                }
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 当前用户名，如果未认证则返回"anonymous"
     */
    public String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : "anonymous";
    }
    
    /**
     * 创建登录响应对象
     * 
     * @param user 用户对象
     * @return 登录响应
     */
    private UserLoginResponse createLoginResponse(User user) {
        UserLoginResponse response = new UserLoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setBio(user.getBio());
        response.setLikedCount(user.getLikedCount());
        response.setRole(user.getRole());
        response.setEmailVerified(user.getEmailVerified());
        response.setTotpEnabled(user.getTotpEnabled());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}