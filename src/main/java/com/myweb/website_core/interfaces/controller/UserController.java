package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.UserService;
import com.myweb.website_core.domain.dto.UserProfileDTO;
import com.myweb.website_core.domain.entity.User;
import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register/code")
    public CompletableFuture<Void> sendRegisterCode(@RequestParam String email) {
        return userService.sendRegisterCode(email);
    }

    @PostMapping("/register")
    public CompletableFuture<User> register(@RequestBody RegisterRequest req) {
        try{
            return userService.register(req.getUsername(), req.getPassword());
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/login")
    public CompletableFuture<User> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            User user = userService.login(req.getUsername(), req.getPassword(), req.getCode()).join();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return CompletableFuture.completedFuture(user);
        } catch (Exception e) {
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/follow")
    public CompletableFuture<Void> follow(@PathVariable Long id, @RequestParam Long targetId) {
        return userService.follow(id, targetId);
    }

    @PostMapping("/{id}/unfollow")
    public CompletableFuture<Void> unfollow(@PathVariable Long id, @RequestParam Long targetId) {
        return userService.unfollow(id, targetId);
    }

    @GetMapping("/{id}/profile")
    public UserProfileDTO getProfile(@PathVariable Long id) {
        System.out.println("Getting profile for user ID: " + id);
        return userService.getProfile(id);

    }
}

@Getter
class RegisterRequest {
    private String username;
    private String password;

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }
}
@Getter
class LoginRequest {
    private String username;
    private String password;
    private String code;

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

    public void setCode(String code) { this.code = code; }
} 