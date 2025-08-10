package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.UserService;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import com.myweb.website_core.application.service.security.authentication.JWT.TokenPair;
import com.myweb.website_core.application.service.security.authentication.UserRegistrationService;
import com.myweb.website_core.application.service.security.authorization.AccessControlService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.common.util.SecurityEventUtils;
import com.myweb.website_core.domain.business.dto.UserLoginResponse;
import com.myweb.website_core.domain.business.dto.UserProfileDTO;
import com.myweb.website_core.domain.business.dto.UserRegistrationDTO;
import com.myweb.website_core.domain.business.dto.UserRegistrationResult;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final MessageProducerService messageProducerService;
    private final AccessControlService accessControlService;
    private final AuthenticationService authenticationService;
    private final UserRegistrationService userRegistrationService;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;

    @PostMapping("/register/code")
    @Auditable(operation = AuditOperation.EMAIL_VERIFIED, resourceType = "USER", description = "发送注册验证码")
    public ResponseEntity<?> sendRegisterCode(@RequestParam String email, HttpServletRequest request) {
        String ipAddress = getClientIpAddress();
        
        try {
            // 检查邮箱是否已被注册
            if (!userRegistrationService.isEmailAvailable(email)) {
                return ResponseEntity.badRequest().body("邮箱已被注册，请使用其他邮箱");
            }
            
            emailVerificationService.sendRegistrationVerificationCode(email);
            return ResponseEntity.ok("验证码已发送到您的邮箱，请查收");
            
        } catch (Exception e) {
            log.error("发送注册验证码失败: email={}, ip={}", email, ipAddress, e);
            return ResponseEntity.badRequest().body("发送验证码失败: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    @Auditable(operation = AuditOperation.USER_REGISTER, resourceType = "USER", description = "用户注册")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletRequest request) {
        String ipAddress = getClientIpAddress();
        
        try {
            // 构建注册DTO
            UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                    .username(req.getUsername())
                    .password(req.getPassword())
                    .email(req.getEmail())
                    .verificationCode(req.getCode())
                    .clientIp(ipAddress)
                    .userAgent(request.getHeader("User-Agent"))
                    .build();
            
            // 使用统一的注册服务
            UserRegistrationResult result = userRegistrationService.registerUser(registrationDTO).get();
            
            if (result.isSuccess()) {
                log.info("用户注册成功: username={}, email={}, ip={}", 
                        req.getUsername(), req.getEmail(), ipAddress);
                return ResponseEntity.ok(result.getUser());
            } else {
                log.warn("用户注册失败: username={}, email={}, ip={}, error={}", 
                        req.getUsername(), req.getEmail(), ipAddress, result.getErrorMessage());
                return ResponseEntity.badRequest().body("注册失败: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("用户注册失败: username={}, email={}, ip={}", 
                    req.getUsername(), req.getEmail(), ipAddress, e);
            return ResponseEntity.badRequest().body("注册失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        try {
            boolean available = userRegistrationService.isUsernameAvailable(username);
            return ResponseEntity.ok(Map.of("available", available));
        } catch (Exception e) {
            log.error("检查用户名可用性失败: username={}", username, e);
            return ResponseEntity.badRequest().body("检查失败");
        }
    }
    
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        try {
            boolean available = userRegistrationService.isEmailAvailable(email);
            return ResponseEntity.ok(Map.of("available", available));
        } catch (Exception e) {
            log.error("检查邮箱可用性失败: email={}", email, e);
            return ResponseEntity.badRequest().body("检查失败");
        }
    }
    
    @GetMapping("/check-admin")
    public ResponseEntity<?> checkAdmin(@RequestParam String username) {
        try {
            User user = userRepository.findByUsernameOrEmail(username, username).orElse(null);
            boolean isAdmin = user != null && user.getRole() != null && user.getRole().isAdmin();
            return ResponseEntity.ok(Map.of("isAdmin", isAdmin));
        } catch (Exception e) {
            log.error("检查管理员账户失败: username={}", username, e);
            return ResponseEntity.ok(Map.of("isAdmin", false));
        }
    }

    @PostMapping("/login")
    @Auditable(operation = AuditOperation.USER_LOGIN, resourceType = "USER", description = "用户登录")
    public ResponseEntity<?> login(@RequestBody LoginRequest req,
                                   HttpServletResponse resp,
                                    HttpServletRequest request) {
        String ipAddress = getClientIpAddress();
        //String sessionId = request.getSession().getId();
        
        try {
            UserLoginResponse response = authenticationService.login(req.getUsername(), 
                    req.getPassword(), req.getCode(), ipAddress);

            log.info("用户登录成功: {}", req.getUsername());
            // 设置 HttpOnly Cookie，自动随请求发送
            String jwt = response.getAccessToken();
            Cookie cookie = new Cookie("Authorization", jwt);
            cookie.setPath("/blog");
            cookie.setHttpOnly(true);
            cookie.setSecure(true); // HTTPS
            cookie.setMaxAge(3600); // 1小时
            resp.addCookie(cookie);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("用户登录失败: ", e);
            
            return ResponseEntity.badRequest().body("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Auditable(operation = AuditOperation.USER_LOGOUT, resourceType = "USER", description = "用户退出登录")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String username = authenticationService.getCurrentUsername();
        
        try {
            String result = authenticationService.logout();
            // JWT模式下不需要invalidate session
            // request.getSession().invalidate();

            log.info("用户退出登录成功: {}", username);
            return ResponseEntity.ok(Map.of("message", result));
            
        } catch (Exception e) {
            log.error("用户退出登录失败: ", e);
            return ResponseEntity.badRequest().body("退出登录失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/refresh-token")
    @Auditable(operation = AuditOperation.USER_LOGIN_SUCCESS, resourceType = "USER", description = "刷新访问令牌")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            TokenPair tokenPair = authenticationService.refreshAccessToken(request.getRefreshToken());
            
            log.info("刷新令牌成功");
            return ResponseEntity.ok(tokenPair);
            
        } catch (Exception e) {
            log.error("刷新令牌失败: ", e);
            return ResponseEntity.badRequest().body("刷新令牌失败: " + e.getMessage());
        }
    }



    @PostMapping("/{id}/follow")
    @Auditable(operation = AuditOperation.USER_FOLLOW, resourceType = "USER", description = "关注用户")
    public ResponseEntity<?> follow(@PathVariable Long id, @RequestParam Long targetId, HttpServletRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        String ipAddress = getClientIpAddress();
        
        try {
            // 检查关注权限
            if (!accessControlService.canAccessAdmin(currentUser)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            CompletableFuture<Void> result = userService.follow(id, targetId);
            
            return ResponseEntity.ok("关注成功");
            
        } catch (Exception e) {
            log.error("关注用户失败: ", e);
            
            return ResponseEntity.badRequest().body("关注失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/unfollow")
    @Auditable(operation = AuditOperation.USER_UNFOLLOW, resourceType = "USER", description = "取消关注用户")
    public ResponseEntity<?> unfollow(@PathVariable Long id, @RequestParam Long targetId, HttpServletRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        //String ipAddress = getClientIpAddress();
        
        try {
            // 检查取消关注权限
            if (!accessControlService.canAccessAdmin(currentUser)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            CompletableFuture<Void> result = userService.unfollow(id, targetId);

            return ResponseEntity.ok("取消关注成功");
            
        } catch (Exception e) {
            log.error("取消关注用户失败: ", e);

            return ResponseEntity.badRequest().body("取消关注失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/profile")
    @Auditable(operation = AuditOperation.PROFILE_UPDATE, resourceType = "USER",  description = "查看用户资料")
    public ResponseEntity<?> getProfile(@PathVariable Long id, HttpServletRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        String ipAddress = getClientIpAddress();
        
        try {
            System.out.println("Getting profile for user ID: " + id);
            UserProfileDTO profile = userService.getProfile(id);
            
            return ResponseEntity.ok(profile);
            
        } catch (Exception e) {
            log.error("获取用户资料失败: ", e);
            
            return ResponseEntity.badRequest().body("获取用户资料失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/bind-email/code")
    @Auditable(operation = AuditOperation.EMAIL_VERIFIED, resourceType = "USER", description = "发送邮箱绑定验证码")
    public ResponseEntity<?> sendBindEmailCode(@PathVariable Long id, @RequestParam String email, HttpServletRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        String ipAddress = getClientIpAddress();
        
        try {
            // 检查访问权限
            if (!accessControlService.canBindEmail(currentUser, id)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            emailVerificationService.sendEmailBindingVerificationCode(email);
            return ResponseEntity.ok("验证码已发送到您的邮箱，请查收");
            
        } catch (Exception e) {
            log.error("发送邮箱绑定验证码失败: email={}, ip={}", email, ipAddress, e);
            return ResponseEntity.badRequest().body("发送验证码失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/bind-email")
    @Auditable(operation = AuditOperation.EMAIL_VERIFIED, resourceType = "USER", description = "绑定邮箱")
    public ResponseEntity<?> bindEmail(@PathVariable Long id, @RequestBody SimpleBindEmailRequest req, HttpServletRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        String ipAddress = getClientIpAddress();
        
        try {
            // 检查访问权限
            if (!accessControlService.canBindEmail(currentUser, id)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            // 验证邮箱验证码
            if (!emailVerificationService.verifyEmailBindingCode(req.getEmail(), req.getCode())) {
                return ResponseEntity.badRequest().body("邮箱验证码不正确或已过期");
            }
            
            // 更新用户邮箱
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }
            
            user.setEmail(req.getEmail());
            user.setEmailVerified(true);
            userService.save(user);
            
            log.info("用户 {} 绑定邮箱 {} 成功", user.getUsername(), req.getEmail());
            return ResponseEntity.ok("邮箱绑定成功");
            
        } catch (Exception e) {
            log.error("绑定邮箱失败: email={}, ip={}", req.getEmail(), ipAddress, e);
            return ResponseEntity.badRequest().body("绑定邮箱失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
       return SecurityEventUtils.getClientIpAddress();
    }
    

}

@Getter
class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String code;

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setCode(String code) { this.code = code; }
}
@Getter@Setter
class LoginRequest {
    private String username;
    private String password;
    private String code;

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

    public void setCode(String code) { this.code = code; }
}

@Getter
class RefreshTokenRequest {
    private String refreshToken;

    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}

@Getter
class SimpleBindEmailRequest {
    private String email;
    private String code;

    public void setEmail(String email) { this.email = email; }
    public void setCode(String code) { this.code = code; }
}

 