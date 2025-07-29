package com.myweb.website_core.demos.web.blog;

import com.myweb.website_core.demos.web.comment.Comment;
import com.myweb.website_core.demos.web.comment.CommentDTO;
import com.myweb.website_core.demos.web.comment.CommentService;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.ApiResponse;
import com.myweb.website_core.dto.CollectResponse;
import com.myweb.website_core.dto.LikeResponse;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;
    private final CommentService commentService;
    @Autowired
    public PostController(PostService postService, UserRepository userRepository, CommentService commentService) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.commentService = commentService;
    }

    @PostMapping(value = "", consumes = "application/json", produces = "application/json")
    public ResponseEntity<PostDTO> createPost(@RequestBody CreatePostRequest request) {
        try {
            Post post = new Post();
            post.setTitle(request.getTitle());
            post.setContent(request.getContent());
            Long userId = request.getAuthor().getId();
            post.setAuthor(userRepository.findById(userId).get());

            Post createdPost = postService.createPost(post).get();
            // 优先使用imageIds，如果没有则使用imageUrls
            if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
                postService.associateImagesByIds(createdPost.getId(), request.getImageIds());
            } else if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
                postService.associateImagesToPost(createdPost.getId(), request.getImageUrls());
            }
            PostDTO postDTO = postService.convertToDTO(createdPost);
            return ResponseEntity.ok(postDTO);
        } catch (Exception e) {
            e.printStackTrace(); // 添加异常打印用于调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PutMapping("/{id}")
    public ResponseEntity<PostDTO> editPost(@PathVariable Long id, @RequestBody Post post) {
        try {
            Post updatedPost = postService.editPost(id, post);
            PostDTO postDTO = postService.convertToDTO(updatedPost);
            return ResponseEntity.ok(postDTO);
        } catch (Exception e) {
            e.printStackTrace(); // 添加异常打印用于调试
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
    public CompletableFuture<List<PostDTO>> getMyPosts(@RequestParam Long userId) {
        List<Post> posts = postService.findPostsByUserId(userId);
        List<PostDTO> postDTOs = postService.convertToDTOList(posts);
        return CompletableFuture.completedFuture(postDTOs);
    }

    @GetMapping("/collected")
    public ResponseEntity<List<PostDTO>> getCollectedPosts(@RequestParam Long userId) {
        try {
            List<Post> collectedPosts = postService.findCollectedPostsByUserId(userId);
            List<PostDTO> postDTOs = postService.convertToDTOList(collectedPosts);
            return ResponseEntity.ok(postDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/liked")
    public ResponseEntity<List<PostDTO>> getLikedPosts(@RequestParam Long userId) {
        try {
            List<Post> likedPosts = postService.findLikedPostsByUserId(userId);
            List<PostDTO> postDTOs = postService.convertToDTOList(likedPosts);
            return ResponseEntity.ok(postDTOs);
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
    
    @GetMapping("/{id}/collect-status")
    public ResponseEntity<ApiResponse<Boolean>> getCollectStatus(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            boolean isCollected = postService.isPostCollectedByUser(id, userId);
            return ResponseEntity.ok(ApiResponse.success(isCollected));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取收藏状态失败"));
        }
    }

    @GetMapping("")
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        try {
            List<Post> posts = postService.getAllPosts();
            List<PostDTO> postDTOs = postService.convertToDTOList(posts);
            return ResponseEntity.ok(postDTOs);
        } catch (Exception e) {
            e.printStackTrace(); // 添加异常打印用于调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        try {
            Optional<Post> post = postService.getPostById(id);
            if (post.isPresent()) {
                PostDTO postDTO = postService.convertToDTO(post.get());
                return ResponseEntity.ok(postDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
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

    
    // 创建帖子请求DTO
    @Getter
    public static class CreatePostRequest {
        private String title;
        private String content;
        private AuthorInfo author;
        private List<Long> imageIds; // 优先使用：图片ID列表
        private List<String> imageUrls; // 备用：图片URL列表（向后兼容）

        public void setTitle(String title) { this.title = title; }
        public void setContent(String content) { this.content = content; }
        public void setAuthor(AuthorInfo author) { this.author = author; }
        public void setImageIds(List<Long> imageIds) { this.imageIds = imageIds; }
        public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

        @Getter
        public static class AuthorInfo {
            private Long id;
            public void setId(Long id) { this.id = id; }
        }
    }

    // 评论请求DTO
    @Getter
    public static class CommentRequest {
        private String content;
        private Long userId;
        private Long postId;
        private Long parentCommentId;

        public void setContent(String content) { this.content = content; }

        public void setUserId(Long userId) { this.userId = userId; }

        public void setPostId(Long postId) { this.postId = postId; }

        public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
    }
}
