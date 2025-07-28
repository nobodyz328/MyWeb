package com.myweb.website_core.demos.web.web;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostService;
import com.myweb.website_core.demos.web.comment.Comment;
import com.myweb.website_core.demos.web.comment.CommentDTO;
import com.myweb.website_core.demos.web.comment.CommentService;
import com.myweb.website_core.dto.ApiResponse;
import com.myweb.website_core.dto.CollectResponse;
import com.myweb.website_core.dto.LikeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
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
            LikeResponse likeResponse = postService.likePost(id, userId);
            return ResponseEntity.ok(ApiResponse.success(likeResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("点赞操作失败"));
        }
    }

    @PostMapping("/{id}/collect")
    public ResponseEntity<ApiResponse<CollectResponse>> collectPost(@PathVariable Long id, @RequestParam Long userId) {
        try {
            CollectResponse collectResponse = postService.collectPost(id, userId);
            return ResponseEntity.ok(ApiResponse.success(collectResponse));
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

    @GetMapping("/collected")
    public ResponseEntity<List<Post>> getCollectedPosts(@RequestParam Long userId) {
        try {
            List<Post> collectedPosts = postService.findCollectedPostsByUserId(userId);
            return ResponseEntity.ok(collectedPosts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/liked")
    public ResponseEntity<List<Post>> getLikedPosts(@RequestParam Long userId) {
        try {
            List<Post> likedPosts = postService.findLikedPostsByUserId(userId);
            return ResponseEntity.ok(likedPosts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    
    @GetMapping("/{id}/like-status")
    public ResponseEntity<ApiResponse<Boolean>> getLikeStatus(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            boolean isLiked = postService.isPostLikedByUser(id, userId);
            return ResponseEntity.ok(ApiResponse.success(isLiked));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取点赞状态失败"));
        }
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
    
    // 评论相关API
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(
            @PathVariable Long id, 
            @RequestBody CommentRequest request) {
        try {
            Comment comment = commentService.createComment(id, request.getUserId(), request.getContent());
            CommentDTO commentDTO = convertCommentToDTO(comment);
            return ResponseEntity.ok(ApiResponse.success(commentDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建评论失败"));
        }
    }
    
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long id) {
        try {
            List<CommentDTO> comments = commentService.getCommentsByPostId(id);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{postId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentDTO>> createReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        try {
            Comment reply = commentService.createReply(postId, commentId, request.getUserId(), request.getContent());
            CommentDTO replyDTO = convertCommentToDTO(reply);
            return ResponseEntity.ok(ApiResponse.success(replyDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建回复失败"));
        }
    }
    
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除评论失败"));
        }
    }
    
    // 辅助方法
    private CommentDTO convertCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        
        CommentDTO.AuthorInfo authorInfo = new CommentDTO.AuthorInfo();
        authorInfo.setId(comment.getAuthor().getId());
        authorInfo.setUsername(comment.getAuthor().getUsername());
        authorInfo.setAvatarUrl(comment.getAuthor().getAvatarUrl());
        dto.setAuthor(authorInfo);
        
        return dto;
    }
    
    // 评论请求DTO
    public static class CommentRequest {
        private String content;
        private Long userId;
        private Long postId;
        private Long parentCommentId;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }
        
        public Long getParentCommentId() { return parentCommentId; }
        public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
    }
}
