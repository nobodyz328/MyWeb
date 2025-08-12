package com.myweb.website_core.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

}