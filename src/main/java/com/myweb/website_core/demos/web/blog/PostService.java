package com.myweb.website_core.demos.web.blog;

import com.myweb.website_core.dto.CollectResponse;
import com.myweb.website_core.dto.LikeResponse;
import com.myweb.website_core.demos.web.collect.PostCollect;
import com.myweb.website_core.demos.web.collect.PostCollectRepository;
import com.myweb.website_core.demos.web.like.PostLikeRepository;
import com.myweb.website_core.demos.web.like.PostLikeService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import org.springframework.stereotype.Service;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.service.MessageProducerService;
import com.myweb.website_core.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MessageProducerService messageProducerService;
    private final PostMapper postMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostCollectRepository postCollectRepository;

    private final PostLikeService postLikeService;

    @Autowired
    public PostService(PostRepository postRepository, UserRepository userRepository,
            MessageProducerService messageProducerService, PostMapper postMapper,
            RedisTemplate<String, Object> redisTemplate, PostCollectRepository postCollectRepository,
            PostLikeRepository postLikeRepository, PostLikeService postLikeService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.messageProducerService = messageProducerService;
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplate;
        this.postCollectRepository = postCollectRepository;
        this.postLikeService = postLikeService;
    }

    @Async
    @CacheEvict(value = "posts", allEntries = true)
    public CompletableFuture<Post> createPost(Post post) {
        // 只允许已登录用户发帖，author.id必须存在
        if (post.getAuthor() == null || post.getAuthor().getId() == null) {
            throw new RuntimeException("未指定作者");
        }
        User author = userRepository.findById(post.getAuthor().getId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        post.setAuthor(author);
        post.setCreatedAt(java.time.LocalDateTime.now());

        // 使用MyBatis插入
        postMapper.insertPost(post);

        // 清除Redis缓存
        redisTemplate.delete("posts:all");

        // 发送消息到RabbitMQ
        messageProducerService.sendPostCreatedMessage(post);

        // 发送审计日志
        messageProducerService.sendAuditLogMessage(
                author.getId().toString(),
                author.getUsername(),
                "CREATE_POST",
                "创建帖子: " + post.getTitle());

        return CompletableFuture.completedFuture(post);
    }

    //@Async
    public Post editPost(Long id, Post updatedPost) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        // 只允许作者本人编辑
        if (updatedPost.getAuthor() == null || !post.getAuthor().getId().equals(updatedPost.getAuthor().getId())) {
            throw new RuntimeException("无权限编辑");
        }
        post.setTitle(updatedPost.getTitle());
        post.setContent(updatedPost.getContent());
        post.setImages(updatedPost.getImages());
        return postRepository.save(post);
    }

    //@Async
    public LikeResponse likePost(Long postId, Long userId) {
        return postLikeService.toggleLike(postId, userId);
    }

    //@Async
    @Transactional
    public CollectResponse collectPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查用户是否已收藏
        Optional<PostCollect> existingCollect = postCollectRepository.findByUserIdAndPostId(userId, postId);
        
        if (existingCollect.isPresent()) {
            // 用户已收藏，取消收藏
            postCollectRepository.deleteByUserIdAndPostId(userId, postId);
            
            // 更新收藏数
            long newCollectCount = postCollectRepository.countByPostId(postId);
            post.setCollectCount((int) newCollectCount);
            postRepository.save(post);
            
            // 清除Redis缓存
            String collectKey = "user:collect:" + userId + ":" + postId;
            redisTemplate.delete(collectKey);
            
            return new CollectResponse(false, (int) newCollectCount);
        } else {
            // 用户未收藏，添加收藏
            PostCollect postCollect = new PostCollect(user, post);
            postCollectRepository.save(postCollect);
            
            // 更新收藏数
            long newCollectCount = postCollectRepository.countByPostId(postId);
            post.setCollectCount((int) newCollectCount);
            postRepository.save(post);
            
            // 记录用户收藏行为到Redis
            String collectKey = "user:collect:" + userId + ":" + postId;
            redisTemplate.opsForValue().set(collectKey, java.time.LocalDateTime.now(), 30, TimeUnit.DAYS);
            
            return new CollectResponse(true, (int) newCollectCount);
        }
    }

    @Async
    public CompletableFuture<List<Post>> getTopLikedPosts() {
        // 获赞前20逻辑
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<List<Post>> searchPosts(String keyword) {
        // 搜索逻辑
        return CompletableFuture.completedFuture(null);
    }

    //@Async
    @CachePut(value = "posts", key = "'posts:all'")
    public List<Post> getAllPosts() {
        try {
            // 优先从Redis缓存获取
            // String cacheKey = "posts:all";
            // List<Post> cachedPosts = (List<Post>)
            // redisTemplate.opsForValue().get(cacheKey);
            // if (cachedPosts != null) {
            // return CompletableFuture.completedFuture(cachedPosts);
            // }
            return postRepository.findAll();
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error in getAllPosts: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    //@Async
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
        return postLikeService.getUserLikedPosts(userId, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }
    

    
    public boolean isPostLikedByUser(Long postId, Long userId) {
        return postLikeService.isLikedByUser(postId, userId);
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
