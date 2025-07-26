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
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("")
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        try {
            Post createdPost = postService.createPost(post).get();
            return ResponseEntity.ok(createdPost);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Post> editPost(@PathVariable Long id, @RequestBody Post post) {
        try {
            Post updatedPost = postService.editPost(id, post);
            return ResponseEntity.ok(updatedPost);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> likePost(@PathVariable Long id, @RequestParam Long userId) {
        try {
            LikeResponse likeResponse = postService.likePost(id, userId).get();
            return ResponseEntity.ok(ApiResponse.success(likeResponse));
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.ok(ApiResponse.error("点赞操作失败"));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/collect")
    public ResponseEntity<ApiResponse<CollectResponse>> collectPost(@PathVariable Long id, @RequestParam Long userId) {
        try {
            CollectResponse collectResponse = postService.collectPost(id, userId).get();
            return ResponseEntity.ok(ApiResponse.success(collectResponse));
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.ok(ApiResponse.error("收藏操作失败"));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
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
    public ResponseEntity<List<Post>> getAllPosts() {
        try {
            List<Post> posts = postService.getAllPosts();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            e.printStackTrace(); // 添加异常打印用于调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        try {
            Optional<Post> post = postService.getPostById(id);
            return post.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, @RequestParam Long userId) {
        try {
            postService.deletePost(id, userId).get();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
