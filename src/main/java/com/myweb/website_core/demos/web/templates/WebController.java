package com.myweb.website_core.demos.web.templates;

import com.myweb.website_core.demos.web.blog.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    @Autowired
    private BlogService blogService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("posts", blogService.getAllPosts());
        return "index";
    }

    @GetMapping("/posts/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        blogService.getPostById(id).ifPresent(post -> model.addAttribute("post", post));
        return "post-detail";
    }
}