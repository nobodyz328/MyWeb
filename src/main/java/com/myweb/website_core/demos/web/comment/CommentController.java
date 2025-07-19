package com.myweb.website_core.demos.web.comment;

import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;
import java.util.List;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }
    @PostMapping
    public CompletableFuture<Comment> addComment(@PathVariable Long postId, @RequestBody Comment comment) {
        // 需设置comment.post
        return commentService.addComment(comment);
    }
    @GetMapping
    public CompletableFuture<List<Comment>> getComments(@PathVariable Long postId) {
        return commentService.getCommentsByPost(postId);
    }
} 