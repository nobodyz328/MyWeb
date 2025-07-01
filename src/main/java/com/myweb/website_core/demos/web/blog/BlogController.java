package com.myweb.website_core.demos.web.blog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts") // 基础路径
public class BlogController {
    private final BlogService blogService;
    @Autowired
    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }
    // 创建新文章
    @PostMapping
    public ResponseEntity<BlogPost> createPost(@RequestBody BlogPost post) {
        BlogPost newPost = blogService.createPost(post);
        return new ResponseEntity<>(newPost, HttpStatus.CREATED);
    }
    @GetMapping("/{id}")
    public ResponseEntity<BlogPost> getPostById(@PathVariable Long id) {
        Optional<BlogPost> post = blogService.getPostById(id);
        return post.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // 更新文章
    @PutMapping("/{id}")
    public ResponseEntity<BlogPost> updatePost(@PathVariable Long id, @RequestBody BlogPost updatedPost) {
        BlogPost post = blogService.updatePost(id, updatedPost);
        return new ResponseEntity<>(post, HttpStatus.OK);
    }
    // 删除文章
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        blogService.deletePost(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    // BlogController.java
    @GetMapping
    public List<BlogPost> getAllPosts() {
        return blogService.getAllPosts();
    }
}
