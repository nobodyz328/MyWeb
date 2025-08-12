package com.myweb.website_core.application.service.security.authentication;

import com.myweb.website_core.application.service.security.authentication.JWT.JwtTokenService;
import com.myweb.website_core.application.service.security.authentication.JWT.TokenPair;
import com.myweb.website_core.common.exception.security.AccountLockedException;
import com.myweb.website_core.common.exception.security.AuthenticationException;
import com.myweb.website_core.common.exception.security.TokenException;
import com.myweb.website_core.common.util.SecurityEventUtils;
import com.myweb.website_core.domain.business.dto.UserLoginResponse;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 * 
 * 使用JWT令牌和Spring Security标准流程处理用户认证
 * 符合GB/T 22239-2019二级等保要求的身份鉴别机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final TOTPService totpService;
    

    
    /**
     * 用户登录
     * 
     * @param username 用户名或邮箱
     * @param password 密码
     * @param code 验证码（管理员必须提供TOTP验证码）
     * @param ipAddress 客户端IP地址
     * @return 登录响应
     */
    @Transactional
    public UserLoginResponse login(String username, String password, String code, String ipAddress) {
        try {
            // 查找用户
            User user = userRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
            
            // 检查账户锁定状态
            if (user.isAccountLocked()) {
                throw new AccountLockedException(user.getUsername(),user.getAccountLockedUntil());
            }
            
            // 检查邮箱
//            if (!user.getEmailVerified()) {
//                throw new DisabledException("账户未激活，请先验证邮箱");
//            }
            
            // 管理员登录必须提供TOTP验证码
            if (user.getRole() != null && user.getRole().isAdmin()) {
                if (!user.getTotpEnabled()) {
                    throw new AuthenticationException("管理员账户必须启用二次验证才能登录");
                }
                
                if (code == null || code.trim().isEmpty()) {
                    throw new AuthenticationException("管理员登录需要提供TOTP验证码");
                }
                
                // 验证TOTP代码
                if (!validateTOTPCode(user, code.trim())) {
                    throw new AuthenticationException("TOTP验证码不正确");
                }
                
                log.info("管理员 {} TOTP验证成功", username);
            }
            
            // 使用Spring Security进行认证
            UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(username, password);
            
            Authentication authentication = authenticationManager.authenticate(authToken);
            
            // 认证成功，设置安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 生成JWT令牌对
            TokenPair tokenPair = jwtTokenService.generateTokenPair(user);
            
            // 更新用户登录信息
            user.resetLoginAttempts();
            user.updateLastLoginInfo(LocalDateTime.now(), ipAddress);
            userRepository.save(user);
            
            log.info("用户登录成功: {}, IP: {}", username, ipAddress);
            
            // 返回登录响应（包含JWT令牌）
            return createLoginResponse(user, tokenPair);
            
        } catch (AuthenticationException e) {
            // 认证失败，增加失败次数
            userRepository.findByUsernameOrEmail(username, username)
                    .ifPresent(user -> {
                        user.incrementLoginAttempts();
                        userRepository.save(user);
                    });
            throw new AuthenticationException("用户登录失败:"+username+"; 原因: "+e.getMessage());
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
            
            // 获取当前用户并撤销所有令牌
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                jwtTokenService.revokeAllUserTokens(currentUser.getId());
            }
            
            // 清除安全上下文
            SecurityContextHolder.clearContext();
            
            log.info("用户退出登录成功: {}", username);
            return "退出登录成功";
            
        } catch (Exception e) {
            throw new AuthenticationException("退出登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前认证用户
     * 
     * @return 当前用户，如果未认证则返回null
     */
    public User getCurrentUser() {
       return SecurityEventUtils.getCurrentUser();
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
     * 刷新访问令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的令牌对
     */
    public TokenPair refreshAccessToken(String refreshToken) {
        try {
            // 从刷新令牌中获取用户信息
            String username = jwtTokenService.getUsernameFromToken(refreshToken);
            if (username == null) {
                throw new TokenException("无效的JWT刷新令牌");
            }
            
            // 查找用户
            User user = userRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new AuthenticationException("用户不存在"));
            
            // 刷新令牌
            TokenPair tokenPair = jwtTokenService.refreshToken(refreshToken, user);
            
            log.info("刷新访问令牌成功: {}", username);
            return tokenPair;
            
        } catch (Exception e) {
            throw new TokenException("刷新令牌失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证用户的TOTP代码
     * 
     * @param user 用户对象
     * @param code TOTP验证码
     * @return 验证是否成功
     */
    private boolean validateTOTPCode(User user, String code) {
        try {
            if (user.getTotpSecret() == null || user.getTotpSecret().trim().isEmpty()) {
                log.warn("用户 {} 未设置TOTP密钥", user.getUsername());
                return false;
            }
            
            return totpService.validateTOTP(user.getTotpSecret(), code, user.getUsername());
            
        } catch (Exception e) {
            log.error("验证用户 {} 的TOTP代码失败", user.getUsername(), e);
            return false;
        }
    }
    
    /**
     * 创建登录响应对象
     * 
     * @param user 用户对象
     * @param tokenPair JWT令牌对
     * @return 登录响应
     */
    private UserLoginResponse createLoginResponse(User user, TokenPair tokenPair) {
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
        
        // 设置JWT令牌信息
        response.setAccessToken(tokenPair.getAccessToken());
        //response.setRefreshToken(tokenPair.getRefreshToken());
        response.setTokenType(tokenPair.getTokenType());
        response.setExpiresIn(tokenPair.getExpiresIn());
        
        return response;
    }
}