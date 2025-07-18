package com.myweb.website_core.demos.web.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Set;

import java.util.Collections;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, String> emailCodeMap = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Async
    public CompletableFuture<Void> sendRegisterCode(String email) {
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
        emailCodeMap.put(email, code);
        emailService.sendVerificationCode(email, code);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<User> register(String username, String email, String password, String code) {
        if (!code.equals(emailCodeMap.get(email))) {
            throw new RuntimeException("验证码错误");
        }
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("邮箱已注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // 实际应加密
        userRepository.save(user);
        emailCodeMap.remove(email);
        return CompletableFuture.completedFuture(user);
    }

    @Async
    public CompletableFuture<User> login(String usernameOrEmail, String password, String code) {
        // 校验验证码、密码、返回用户
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> follow(Long userId, Long targetId) {
        // 关注逻辑
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> unfollow(Long userId, Long targetId) {
        // 取关逻辑
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<User> getProfile(Long userId) {
        // 查询个人主页信息
        return CompletableFuture.completedFuture(null);
    }
}