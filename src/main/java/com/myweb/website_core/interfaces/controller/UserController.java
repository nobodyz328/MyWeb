package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.UserService;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.application.service.security.IPS.virusprotect.InputValidationService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.application.service.security.authentication.EmailVerificationService;
import com.myweb.website_core.application.service.security.authentication.JWT.TokenPair;
import com.myweb.website_core.application.service.security.authentication.UserRegistrationService;
import com.myweb.website_core.application.service.security.authorization.AccessControlService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.common.enums.SecurityEventType;
import com.myweb.website_core.common.exception.security.ValidationException;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.SecurityEventUtils;
import com.myweb.website_core.common.validation.ValidateInput;
import com.myweb.website_core.domain.business.dto.UserLoginResponse;
import com.myweb.website_core.domain.business.dto.UserProfileDTO;
import com.myweb.website_core.domain.business.dto.UserRegistrationDTO;
import com.myweb.website_core.domain.business.dto.UserRegistrationResult;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import com.myweb.website_core.domain.business.dto.FollowResponse;
import org.springframework.http.HttpStatus;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.security.dto.SecurityEventRequest;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
    private final InputValidationService inputValidationService;

    @PostMapping("/register/code")
    @Auditable(operation = AuditOperation.EMAIL_VERIFIED, resourceType = "USER", description = "发送注册验证码")
    @ValidateInput(fieldNames = {"email"}, validationTypes = {"email"}, maxLength = 100)
    public ResponseEntity<?> sendRegisterCode(@RequestParam String email, HttpServletRequest request) {
            // 手动验证邮箱格式（作为双重保障）
        inputValidationService.validateEmail(email);
            // 检查邮箱是否已被注册
        if (!userRegistrationService.isEmailAvailable(email)) {
            return ResponseEntity.badRequest().body("邮箱已被注册，请使用其他邮箱");
        }
        emailVerificationService.sendRegistrationVerificationCode(email);
        return ResponseEntity.ok("验证码已发送到您的邮箱，请查收");

    }

    @PostMapping("/register")
    @Auditable(operation = AuditOperation.USER_REGISTER, resourceType = "USER", description = "用户注册")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletRequest request) {
        String ipAddress = getClientIpAddress();
            // 手动验证注册请求的所有字段
            validateRegistrationRequest(req);
            
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
        UserRegistrationResult result = null;
        try {
            result = userRegistrationService.registerUser(registrationDTO).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (result.isSuccess()) {
                log.info("用户注册成功: username={}, email={}, ip={}", 
                        req.getUsername(), req.getEmail(), ipAddress);
                return ResponseEntity.ok(result.getUser());
            } else {
                log.warn("用户注册失败: username={}, email={}, ip={}, error={}", 
                        req.getUsername(), req.getEmail(), ipAddress, result.getErrorMessage());
                return ResponseEntity.badRequest().body("注册失败: " + result.getErrorMessage());
            }

    }
    
    @GetMapping("/check-username")
    @ValidateInput(fieldNames = {"username"}, validationTypes = {"username"}, maxLength = 50)
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        try {
            // 手动验证用户名格式（作为双重保障）
            inputValidationService.validateUsername(username);
            
            boolean available = userRegistrationService.isUsernameAvailable(username);
            return ResponseEntity.ok(Map.of("available", available));
        } catch (ValidationException e) {
            // 记录验证失败的安全事件
            recordValidationFailureEvent("checkUsername", "username", e);
            log.warn("检查用户名输入验证失败: username={}, error={}", username, e.getMessage());
            return ResponseEntity.badRequest().body("输入验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("检查用户名可用性失败: username={}", username, e);
            return ResponseEntity.badRequest().body("检查失败");
        }
    }
    
    @GetMapping("/check-email")
    @ValidateInput(fieldNames = {"email"}, validationTypes = {"email"}, maxLength = 100)
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        try {
            // 手动验证邮箱格式（作为双重保障）
            inputValidationService.validateEmail(email);
            
            boolean available = userRegistrationService.isEmailAvailable(email);
            return ResponseEntity.ok(Map.of("available", available));
        } catch (ValidationException e) {
            // 记录验证失败的安全事件
            recordValidationFailureEvent("checkEmail", "email", e);
            log.warn("检查邮箱输入验证失败: email={}, error={}", email, e.getMessage());
            return ResponseEntity.badRequest().body("输入验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("检查邮箱可用性失败: email={}", email, e);
            return ResponseEntity.badRequest().body("检查失败");
        }
    }
    
    @GetMapping("/check-admin")
    @ValidateInput(fieldNames = {"username"}, validationTypes = {"default"}, maxLength = 100)
    public ResponseEntity<?> checkAdmin(@RequestParam String username) {
        try {
            // 手动验证输入（用户名或邮箱）
            inputValidationService.validateStringInput(username, "username", 100);
            
            User user = userRepository.findByUsernameOrEmail(username, username).orElse(null);
            boolean isAdmin = user != null && user.getRole() != null && user.getRole().isAdmin();
            return ResponseEntity.ok(Map.of("isAdmin", isAdmin));
        } catch (ValidationException e) {
            // 记录验证失败的安全事件
            recordValidationFailureEvent("checkAdmin", "username", e);
            log.warn("检查管理员账户输入验证失败: username={}, error={}", username, e.getMessage());
            return ResponseEntity.badRequest().body("输入验证失败: " + e.getMessage());
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
            // 手动验证登录请求的所有字段
            validateLoginRequest(req);
            
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
        } catch (ValidationException e) {
            // 记录验证失败的安全事件
            recordValidationFailureEvent("login", "login_data", e);
            log.warn("用户登录输入验证失败: username={}, ip={}, error={}", 
                    req.getUsername(), ipAddress, e.getMessage());
            return ResponseEntity.badRequest().body("输入验证失败: " + e.getMessage());
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
    public ResponseEntity<ApiResponse<FollowResponse>> followUser(@PathVariable Long id, @RequestParam Long userId) {
        try {
            // 检查关注权限
            User currentUser = userRepository.findById(userId).orElse(null);
            User targetUser = userRepository.findById(id).orElse(null);
            
            if (currentUser == null || targetUser == null) {
                return ResponseEntity.ok(ApiResponse.error("用户不存在"));
            }
            
            if (userId.equals(id)) {
                return ResponseEntity.ok(ApiResponse.error("不能关注自己"));
            }
            
            FollowResponse followResponse = userService.followUser(userId, id);
            return ResponseEntity.ok(ApiResponse.success(followResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("关注操作失败"));
        }
    }

    @GetMapping("/{id}/follow-status")
    public ResponseEntity<ApiResponse<Boolean>> getFollowStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId
            ) {
        try {
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.success(false));
            }
            boolean isFollowed = userService.isUserFollowedByUser(id, userId);
            return ResponseEntity.ok(ApiResponse.success(isFollowed));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取关注状态失败"));
        }
    }
    
    @GetMapping("/{id}/followed-users")
    public ResponseEntity<List<UserProfileDTO>> getFollowedUsers(@PathVariable Long id) {
        try {
            List<UserProfileDTO> followedUsers = userService.getFollowedUsers(id);
            return ResponseEntity.ok(followedUsers);
        } catch (Exception e) {
            log.error("获取关注用户列表时发生错误：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/profile")
    @Auditable(operation = AuditOperation.PROFILE_UPDATE, resourceType = "USER",  description = "查看用户资料")
    public ResponseEntity<?> getProfile(@PathVariable Long id, HttpServletRequest request) {
        
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
    @ValidateInput(fieldNames = {"email"}, validationTypes = {"email"}, maxLength = 100)
    public ResponseEntity<?> sendBindEmailCode(@PathVariable Long id, @RequestParam String email, HttpServletRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        String ipAddress = getClientIpAddress();
        
        try {
            // 手动验证邮箱格式（作为双重保障）
            inputValidationService.validateEmail(email);
            
            // 检查访问权限
            if (!accessControlService.canBindEmail(currentUser, id)) {
                return ResponseEntity.status(403).body("无权限执行此操作");
            }
            
            emailVerificationService.sendEmailBindingVerificationCode(email);
            return ResponseEntity.ok("验证码已发送到您的邮箱，请查收");
            
        } catch (ValidationException e) {
            // 记录验证失败的安全事件
            recordValidationFailureEvent("sendBindEmailCode", "email", e);
            log.warn("发送邮箱绑定验证码输入验证失败: email={}, ip={}, error={}", email, ipAddress, e.getMessage());
            return ResponseEntity.badRequest().body("输入验证失败: " + e.getMessage());
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
            // 手动验证邮箱绑定请求
            validateBindEmailRequest(req);
            
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
            
        } catch (ValidationException e) {
            // 记录验证失败的安全事件
            recordValidationFailureEvent("bindEmail", "bind_email_data", e);
            log.warn("绑定邮箱输入验证失败: email={}, ip={}, error={}", req.getEmail(), ipAddress, e.getMessage());
            return ResponseEntity.badRequest().body("输入验证失败: " + e.getMessage());
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
       return SecurityEventUtils.getIpAddress();
    }
    
    /**
     * 验证用户注册请求
     * 
     * @param req 注册请求
     * @throws ValidationException 验证失败时抛出
     */
    private void validateRegistrationRequest(RegisterRequest req) throws ValidationException {
        if (req == null) {
            throw new ValidationException("注册请求不能为空", "request", "REQUIRED");
        }
        
        // 验证用户名
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new ValidationException("用户名不能为空", "username", "REQUIRED");
        }
        inputValidationService.validateUsername(req.getUsername());
        
        // 验证密码
        if (req.getPassword() == null || req.getPassword().isEmpty()) {
            throw new ValidationException("密码不能为空", "password", "REQUIRED");
        }
        inputValidationService.validatePassword(req.getPassword());
        
        // 验证邮箱
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new ValidationException("邮箱不能为空", "email", "REQUIRED");
        }
        inputValidationService.validateEmail(req.getEmail());
        
        // 验证验证码
        if (req.getCode() == null || req.getCode().trim().isEmpty()) {
            throw new ValidationException("验证码不能为空", "code", "REQUIRED");
        }
        inputValidationService.validateStringInput(req.getCode(), "验证码", 10);
    }
    
    /**
     * 验证用户登录请求
     * 
     * @param req 登录请求
     * @throws ValidationException 验证失败时抛出
     */
    private void validateLoginRequest(LoginRequest req) throws ValidationException {
        if (req == null) {
            throw new ValidationException("登录请求不能为空", "request", "REQUIRED");
        }
        
        // 验证用户名（可以是用户名或邮箱）
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new ValidationException("用户名不能为空", "username", "REQUIRED");
        }
        inputValidationService.validateStringInput(req.getUsername(), "用户名", 100);
        
        // 验证密码
        if (req.getPassword() == null || req.getPassword().isEmpty()) {
            throw new ValidationException("密码不能为空", "password", "REQUIRED");
        }
        inputValidationService.validateStringInput(req.getPassword(), "密码", 128);
        
        // 验证验证码（可选）
        if (req.getCode() != null && !req.getCode().trim().isEmpty()) {
            inputValidationService.validateStringInput(req.getCode(), "验证码", 10);
        }
    }
    
    /**
     * 验证邮箱绑定请求
     * 
     * @param req 邮箱绑定请求
     * @throws ValidationException 验证失败时抛出
     */
    private void validateBindEmailRequest(SimpleBindEmailRequest req) throws ValidationException {
        if (req == null) {
            throw new ValidationException("邮箱绑定请求不能为空", "request", "REQUIRED");
        }
        
        // 验证邮箱
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new ValidationException("邮箱不能为空", "email", "REQUIRED");
        }
        inputValidationService.validateEmail(req.getEmail());
        
        // 验证验证码
        if (req.getCode() == null || req.getCode().trim().isEmpty()) {
            throw new ValidationException("验证码不能为空", "code", "REQUIRED");
        }
        inputValidationService.validateStringInput(req.getCode(), "验证码", 10);
    }
    
    /**
     * 记录验证失败的安全事件
     * 
     * @param methodName 方法名
     * @param fieldName 字段名
     * @param exception 验证异常
     */
    private void recordValidationFailureEvent(String methodName, String fieldName, ValidationException exception) {
        try {
            // 确定安全事件类型
            SecurityEventType eventType = determineSecurityEventType(exception);
            
            // 创建事件数据
            Map<String, Object> eventData = Map.of(
                "method", "UserController." + methodName,
                "fieldName", fieldName,
                "errorCode", exception.getErrorCode() != null ? exception.getErrorCode() : "UNKNOWN",
                "errorMessage", exception.getMessage(),
                "validationFailure", true
            );
            
            // 创建安全事件请求
            SecurityEventRequest eventRequest = SecurityEventUtils.createCustomEvent(
                eventType,
                "用户控制器输入验证失败",
                String.format("方法 %s 的参数 %s 验证失败：%s", methodName, fieldName, exception.getMessage()),
                eventData
            );
            
            // 记录安全日志
            String securityLog = LoggingUtils.formatSecurityEvent(
                eventType,
                SecurityEventUtils.getUsername(),
                SecurityEventUtils.getIpAddress(),
                eventRequest.getDescription(),
                SecurityEventUtils.getUserAgent(),
                eventData
            );
            
            log.warn("安全事件: {}", securityLog);
            
        } catch (Exception e) {
            log.error("记录验证失败安全事件时发生错误", e);
        }
    }
    
    /**
     * 根据验证异常确定安全事件类型
     * 
     * @param exception 验证异常
     * @return 安全事件类型
     */
    private SecurityEventType determineSecurityEventType(ValidationException exception) {
        String errorCode = exception.getErrorCode();
        
        if (errorCode == null) {
            return SecurityEventType.INPUT_VALIDATION_FAILURE;
        }
        
        return switch (errorCode.toUpperCase()) {
            case "XSS_DETECTED" -> SecurityEventType.XSS_ATTACK_ATTEMPT;
            case "SQL_INJECTION_DETECTED" -> SecurityEventType.SQL_INJECTION_ATTEMPT;
            case "DANGEROUS_EXTENSION" -> SecurityEventType.MALICIOUS_FILE_UPLOAD;
            case "ILLEGAL_CHARACTERS", "LENGTH_EXCEEDED", "INVALID_FORMAT" -> SecurityEventType.DANGEROUS_INPUT_CONTENT;
            default -> SecurityEventType.INPUT_VALIDATION_FAILURE;
        };
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

 