package com.myweb.website_core.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping("/admin_users")
    public String adminUsers() {
        return "admin/users";
    }
    @GetMapping("/admin_settings")
    public String adminSettings() {
        return "admin/settings";
    }
    @GetMapping("/admin_security_events")
    public String adminSecurityEvents() {
        return "admin/security-events";
    }
    @GetMapping("/admin_audit_logs")
    public String adminAuditLogs() {
        return "admin/audit-logs";
    }
    @GetMapping("/admin_roles")
    public String adminRoles() {
        return "admin/roles";
    }
    @GetMapping("/admin_dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }
}