package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.file.ImageService;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.domain.business.dto.PostDTO;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.dto.CollectResponse;
import com.myweb.website_core.domain.business.dto.LikeResponse;
import com.myweb.website_core.domain.business.entity.PostCollect;
import com.myweb.website_core.infrastructure.persistence.repository.PostCollectRepository;
import com.myweb.website_core.infrastructure.persistence.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import com.myweb.website_core.infrastructure.persistence.mapper.PostMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MessageProducerService messageProducerService;
    private final PostMapper postMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostCollectRepository postCollectRepository;
    private final PostLikeService postLikeService;
    private final ImageService imageService;


    //@Async
    //@CacheEvict(value = "posts", allEntries = true)
    public Post createPost(Post post) {
        // 只允许已登录用户发帖，author.id必须存在
        if (post.getAuthor() == null || post.getAuthor().getId() == null) {
            throw new RuntimeException("未指定作者");
        }
        User author = userRepository.findById(post.getAuthor().getId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        post.setAuthor(author);
        post.setCreatedAt(java.time.LocalDateTime.now());

        // postMapper.insertPost(post);
        postRepository.save(post);

        // 清除Redis缓存
//        redisTemplate.delete("posts:all");

        // 发送消息到RabbitMQ
//        messageProducerService.sendPostCreatedMessage(post);


        return post;
    }

    // @Async
    public Post editPost(Long id, Post updatedPost) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        // 只允许作者本人编辑
        if (updatedPost.getAuthor() == null || !post.getAuthor().getId().equals(updatedPost.getAuthor().getId())) {
            throw new RuntimeException("无权限编辑");
        }
        post.setTitle(updatedPost.getTitle());
        post.setContent(updatedPost.getContent());
        // post.setImageIds(updatedPost.getImageIds());
        return postRepository.save(post);
    }

    // @Async
    public LikeResponse likePost(Long postId, Long userId) {
        return postLikeService.toggleLike(postId, userId);
    }

    // @Async
    @Transactional
    public CollectResponse collectPost(Long postId, Long userId) {
       return postLikeService.toggleCollect(postId, userId);
    }

    @Async
    public CompletableFuture<List<Post>> getTopLikedPosts() {
        // 获赞前20逻辑
        return CompletableFuture.completedFuture(null);
    }

    // @Async
    @CachePut(value = "posts", key = "'posts:all'")
    public List<Post> getAllPosts() {
        try {
            return postRepository.findAll();
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error in getAllPosts: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // @Async
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    public List<Post> findPostsByUserId(Long userId) {
        return postRepository.findByAuthorId(userId);
    }

    public List<Post> findCollectedPostsByUserId(Long userId) {
        List<PostCollect> collects = postCollectRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return collects.stream()
                .map(PostCollect::getPost)
                .toList();
    }

    public List<Post> findLikedPostsByUserId(Long userId) {
        return postLikeService.getUserLikedPosts(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
    }

    public boolean isPostLikedByUser(Long postId, Long userId) {
        return postLikeService.isLikedByUser(postId, userId);
    }
    
    public boolean isPostCollectedByUser(Long postId, Long userId) {
      return postLikeService.isPostCollectedByUser(postId, userId);
    }

    /**
     * 将Post转换为PostDTO，包含图片URL
     */
    public PostDTO convertToDTO(Post post) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            // 获取图片ID列表（现在从关联的Image实体获取）
            List<Long> imageIds = post.getImageIds();
            if (imageIds != null && !imageIds.isEmpty()) {
                imageUrls = imageIds.stream()
                        .filter(Objects::nonNull)
                        .map(imageId -> "/blog/api/images/" + imageId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("转换图片URL时出错: " + e.getMessage());
            // 如果出错，返回空的图片列表
            imageUrls = new ArrayList<>();
        }
        
        return new PostDTO(post, imageUrls);
    }

    /**
     * 将Post列表转换为PostDTO列表
     */
    public List<PostDTO> convertToDTOList(List<Post> posts) {
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 通过图片ID列表关联图片到帖子（推荐方式）
     */
    public void associateImagesByIds(Long postId, List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            System.out.println("没有图片ID需要关联到帖子 " + postId);
            return;
        }

        System.out.println("开始通过ID关联图片到帖子 " + postId + ", 图片IDs: " + imageIds);

        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));

            for (Long imageId : imageIds) {
                try {
                    imageService.associateImageToPost(imageId, post);
                    System.out.println("成功关联图片 " + imageId + " 到帖子 " + postId);
                } catch (Exception e) {
                    System.err.println("关联图片失败: imageId=" + imageId + ", error=" + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "通过ID关联图片到帖子失败: postId=" + postId + ", imageIds=" + imageIds + ", error=" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将未关联的图片关联到指定帖子（通过URL，向后兼容）
     */
    public void associateImagesToPost(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            System.out.println("没有图片需要关联到帖子 " + postId);
            return;
        }

        System.out.println("开始关联图片到帖子 " + postId + ", 图片URLs: " + imageUrls);

        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));

            for (String imageUrl : imageUrls) {
                System.out.println("处理图片URL: '" + imageUrl + "'");
                String imageIdStr;
                if (imageUrl.startsWith("/blog/api/images/")) {
                    imageIdStr = imageUrl.substring("/blog/api/images/".length());
                } else if (imageUrl.contains("/blog/api/images/")) {
                    int index = imageUrl.indexOf("/blog/api/images/");
                    imageIdStr = imageUrl.substring(index + "/blog/api/images/".length());
                } else {
                    System.err.println("图片URL格式不匹配: '" + imageUrl);
                    continue;
                }

                if (!imageIdStr.isEmpty()) {
                    try {
                        Long imageId = Long.parseLong(imageIdStr);
                        System.out.println("解析的图片ID: " + imageId);

                        imageService.associateImageToPost(imageId, post);
                        System.out.println("成功关联图片 " + imageId + " 到帖子 " + postId);

                    } catch (NumberFormatException e) {
                        System.err.println(
                                "无效的图片ID格式: '" + imageIdStr + "', 完整URL: '" + imageUrl + "', 错误: " + e.getMessage());
                    }
                } else {
                    System.err.println("无法提取图片ID: '" + imageUrl + "'");
                }
            }

            System.out.println("关联完成: 成功关联 " + imageUrls.size() + " 张图片到帖子 " + postId);

        } catch (Exception e) {
            System.err
                    .println("关联图片到帖子失败: postId=" + postId + ", imageUrls=" + imageUrls + ", error=" + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响帖子创建
        }
    }

    @Async
    @CacheEvict(value = "posts", allEntries = true)
    public CompletableFuture<Void> deletePost(Long id, Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Post post = postRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("帖子不存在"));

                // 只允许作者本人删除
                if (!post.getAuthor().getId().equals(userId)) {
                    throw new RuntimeException("无权限删除");
                }

                // 删除相关的点赞记录
                postLikeService.deleteAllLikesForPost(id);

                // 删除帖子
                postRepository.deleteById(id);

                // 发送审计日志
                userRepository.findById(userId).ifPresent(user -> messageProducerService.sendAuditLogMessage(
                        userId.toString(),
                        user.getUsername(),
                        "DELETE_POST",
                        "删除帖子: " + post.getTitle()));
            } catch (Exception ex) {
                System.err.println("Error in deletePost: " + ex.getMessage());
                throw new RuntimeException("删除帖子过程中发生异常", ex);
            }
        }).exceptionally(ex -> {
            // 记录异常并返回null作为Void类型
            System.err.println("Error in deletePost: " + ex.getMessage());
            return null;
        });
    }
}
