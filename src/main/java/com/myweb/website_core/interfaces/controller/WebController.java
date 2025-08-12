package com.myweb.website_core.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {
    @GetMapping({"/view"})
    public String index() {
        return "index";
    }
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    @GetMapping("/register")
    public String register() {
        return "login";
    }
    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
    
    @GetMapping("/{id}/profile")
    public String userProfile(@PathVariable Long id) {
        return "user-profile";
    }
    @GetMapping("/posts/new")
    public String postEdit() {
        return "post-edit";
    }
    @GetMapping("/posts/edit/{id}")
    public String postEditWithId(@PathVariable Long id) {
        return "post-edit";
    }
    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id) {
        return "post-detail";
    }
    @GetMapping("/announcements")
    public String announcements() {
        return "announcements";
    }
    
    @GetMapping("/search")
    public String search() {
        return "search";
    }
    
    @GetMapping("/debug/authorities")
    @ResponseBody
    public Object debugAuthorities() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            return java.util.Map.of(
                "principal", auth.getPrincipal().getClass().getSimpleName(),
                "authorities", auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(java.util.stream.Collectors.toList()),
                "authenticated", auth.isAuthenticated()
            );
        }
        return "No authentication found";
    }
    
    @GetMapping("/debug/user-roles")
    @ResponseBody
    public Object debugUserRoles() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getPrincipal() instanceof com.myweb.website_core.infrastructure.security.CustomUserDetailsService.CustomUserPrincipal) {
            com.myweb.website_core.infrastructure.security.CustomUserDetailsService.CustomUserPrincipal principal = 
                (com.myweb.website_core.infrastructure.security.CustomUserDetailsService.CustomUserPrincipal) auth.getPrincipal();
            
            com.myweb.website_core.domain.business.entity.User user = principal.getUser();
            
            return java.util.Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "traditionalRole", user.getRole() != null ? user.getRole().name() : "null",
                "rbacRoles", user.getRoles() != null ? 
                    user.getRoles().stream()
                        .map(r -> r.getName())
                        .collect(java.util.stream.Collectors.toList()) : 
                    java.util.List.of(),
                "authorities", auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(java.util.stream.Collectors.toList())
            );
        }
        return "No user principal found";
    }

}