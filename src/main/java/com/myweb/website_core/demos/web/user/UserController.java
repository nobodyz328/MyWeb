package com.myweb.website_core.demos.web.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
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
    public CompletableFuture<UserProfileDTO> getProfile(@PathVariable Long id) {
        System.out.println("Getting profile for user ID: " + id);
        return userService.getProfile(id).whenComplete((result, throwable) -> {
            if (throwable != null) {
                System.err.println("Error getting profile: " + throwable.getMessage());
            } else {
                System.out.println("Profile retrieved successfully for user: " + (result != null ? result.getUsername() : "null"));
            }
        });
    }
}

class RegisterRequest {
    private String username;
    private String password;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
class LoginRequest {
    private String username;
    private String password;
    private String code;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
} 