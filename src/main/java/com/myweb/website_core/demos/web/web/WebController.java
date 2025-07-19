package com.myweb.website_core.demos.web.web;

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
    @GetMapping("/posts/{id}")
    public String postDetail(@PathVariable Long id) {
        return "post-detail";
    }
    @GetMapping("/announcements")
    public String announcements() {
        return "announcements";
    }
}