package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.PostService;
import com.myweb.website_core.application.service.security.authorization.AccessControlService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.dto.*;
//import com.myweb.website_core.domain.dto.*;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.application.service.business.CommentService;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;
    private final CommentService commentService;
    private final AccessControlService accessControlService;

    @PostMapping(value = "")
    @Auditable(operation = AuditOperation.POST_CREATE, resourceType = "POST", description = "创建帖子")
    public ResponseEntity<PostDTO> createPost(@RequestBody CreatePostRequest request) {
        try {
            Post post = new Post();
            post.setTitle(request.getTitle());
            post.setContent(request.getContent());
            Long userId = request.getAuthor().getId();
            post.setAuthor(userRepository.findById(userId).get());

            Post createdPost = postService.createPost(post);
            // 优先使用imageIds，如果没有则使用imageUrls
            if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
                postService.associateImagesByIds(createdPost.getId(), request.getImageIds());
            } else if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
                postService.associateImagesToPost(createdPost.getId(), request.getImageUrls());
            }
            PostDTO postDTO = postService.convertToDTO(createdPost);
            return ResponseEntity.ok(postDTO);
        } catch (Exception e) {
            log.error("创建帖子时发生错误：" + e.getMessage()); // 添加异常打印用于调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PutMapping("/{id}")
    @Auditable(operation = AuditOperation.POST_UPDATE, resourceType = "POST",  description = "更新帖子")
    public ResponseEntity<PostDTO> editPost(@PathVariable Long id, @RequestBody Post post) {
        try {
            Optional<Post> existingPost = postService.getPostById(id);
            if (existingPost.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查编辑权限
            if (!accessControlService.canEditPost(userRepository.findById(post.getAuthor().getId()).get(), existingPost.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Post updatedPost = postService.editPost(id, post);
            PostDTO postDTO = postService.convertToDTO(updatedPost);
            return ResponseEntity.ok(postDTO);
        } catch (Exception e) {
            log.error("更新帖子时发生错误：" + e.getMessage()); // 添加异常打印用于调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    @Auditable(operation = AuditOperation.POST_LIKE, resourceType = "POST", description = "点赞帖子")
    public ResponseEntity<ApiResponse<LikeResponse>> likePost(@PathVariable Long id, @RequestParam Long userId) {
        try {
            // 检查点赞权限
            if (!accessControlService.canLikePost(userRepository.findById(userId).get(), postService.getPostById(id).get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("无权限点赞"));
            }
            
            LikeResponse likeResponse = postService.likePost(id, userId);
            return ResponseEntity.ok(ApiResponse.success(likeResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("点赞操作失败"));
        }
    }

    @PostMapping("/{id}/collect")
    @Auditable(operation = AuditOperation.POST_COLLECT, resourceType = "POST", description = "收藏帖子")
    public ResponseEntity<ApiResponse<CollectResponse>> collectPost(@PathVariable Long id, @RequestParam Long userId) {
        try {
            // 检查收藏权限
            if (!accessControlService.canCollectPost(userRepository.findById(userId).get(), postService.getPostById(id).get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("无权限收藏"));
            }
            
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
            log.error("获取收藏的帖子时发生错误：" + e.getMessage());
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
            log.error("获取点赞的帖子时发生错误：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    
    @GetMapping("/{id}/like-status")
    public ResponseEntity<ApiResponse<Boolean>> getLikeStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId
            ) {
        try {

            boolean isLiked = postService.isPostLikedByUser(id, userId);
            return ResponseEntity.ok(ApiResponse.success(isLiked));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取点赞状态失败"));
        }
    }
    
    @GetMapping("/{id}/collect-status")
    public ResponseEntity<ApiResponse<Boolean>> getCollectStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            Authentication  authentication) {
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
            log.error("获取所有帖子时发生错误：" + e.getMessage()); // 添加异常打印用于调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Auditable(operation = AuditOperation.POST_VIEW, resourceType = "POST",description = "查看帖子")
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
    @Auditable(operation = AuditOperation.POST_DELETE, resourceType = "POST", description = "删除帖子")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, @RequestParam Long userId) {
        try {
            Optional<Post> post = postService.getPostById(id);
            if (post.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查删除权限
            if (!accessControlService.canDeletePost(userRepository.findById(userId).get(), post.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
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
            // 检查创建评论权限
            if (!accessControlService.canCreateComment(userRepository.findById(request.getUserId()).get(), postService.getPostById(id).get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("无权限创建评论"));
            }
            
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
            log.error("获取评论时发生错误：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{postId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentDTO>> createReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        try {
            // 检查创建评论权限
            if (!accessControlService.canCreateComment(userRepository.findById(request.getUserId()).get(), postService.getPostById(postId).get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("无权限评论"));
            }
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
            // 检查删除评论权限
            Comment comment = commentService.getCommentById(commentId);
            if (!accessControlService.canDeleteComment(userRepository.findById(userId).get(), comment)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("无权限删除评论"));
            }
            
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
