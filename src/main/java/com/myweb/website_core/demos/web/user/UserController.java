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

import javax.servlet.http.HttpSession;

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
        return userService.register(req.getUsername(), req.getEmail(), req.getPassword(), req.getCode());
    }

    @PostMapping("/login")
    public CompletableFuture<User> login(@RequestBody LoginRequest req) {
        return userService.login(req.getUsernameOrEmail(), req.getPassword(), req.getCode());
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
    public CompletableFuture<User> getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }
}

class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String code;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
class LoginRequest {
    private String usernameOrEmail;
    private String password;
    private String code;
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
} 