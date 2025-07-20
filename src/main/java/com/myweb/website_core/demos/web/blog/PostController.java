package com.myweb.website_core.demos.web.blog;

import com.myweb.website_core.demos.web.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;
    public PostController(PostService postService) {
        this.postService = postService;
    }
    @PostMapping
    public CompletableFuture<Post> createPost(@RequestBody Post post) {
        return postService.createPost(post);
    }
    @PutMapping("/{id}")
    public CompletableFuture<Post> editPost(@PathVariable Long id, @RequestBody Post post) {
        return postService.editPost(id, post);
    }
    @PostMapping("/{id}/like")
    public CompletableFuture<Void> likePost(@PathVariable Long id, @RequestParam Long userId) {
        return postService.likePost(id, userId);
    }
    @PostMapping("/{id}/collect")
    public CompletableFuture<Void> collectPost(@PathVariable Long id, @RequestParam Long userId) {
        return postService.collectPost(id, userId);
    }
    @GetMapping("/top-liked")
    public CompletableFuture<List<Post>> getTopLikedPosts() {
        return postService.getTopLikedPosts();
    }
    @GetMapping("/search")
    public CompletableFuture<List<Post>> searchPosts(@RequestParam String keyword) {
        return postService.searchPosts(keyword);
    }
    @GetMapping("/mine")
    public CompletableFuture<List<Post>> getMyPosts(@RequestParam Long userId) {
        return CompletableFuture.completedFuture(postService.findPostsByUserId(userId));
    }
    @GetMapping("")
    public CompletableFuture<List<Post>> getAllPosts() {
        return postService.getAllPosts();
    }
}
