package com.myweb.website_core.demos.web.comment;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    @Async
    public CompletableFuture<Comment> addComment(Comment comment) {
        // 评论逻辑
        return CompletableFuture.completedFuture(null);
    }
    @Async
    public CompletableFuture<List<Comment>> getCommentsByPost(Long postId) {
        // 查询评论逻辑
        return CompletableFuture.completedFuture(null);
    }
} 