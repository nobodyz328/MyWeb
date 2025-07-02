package com.myweb.website_core.demos.web.templates;

import com.myweb.website_core.demos.web.blog.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view") // 基础路径
public class WebController {

    @Autowired
    private BlogService blogService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("posts", blogService.getAllPosts());
        return "index";
    }

    @GetMapping("/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        blogService.getPostById(id).ifPresent(post -> model.addAttribute("post", post));
        return "post-detail";
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }
}