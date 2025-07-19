package com.myweb.website_core.demos.web.blog;

import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import org.springframework.stereotype.Service;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.service.MessageProducerService;
import com.myweb.website_core.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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
    
    @Autowired
    public PostService(PostRepository postRepository, UserRepository userRepository, 
                      MessageProducerService messageProducerService, PostMapper postMapper,
                      RedisTemplate<String, Object> redisTemplate) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.messageProducerService = messageProducerService;
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplate;
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
        
        // 发送消息到RabbitMQ
        messageProducerService.sendPostCreatedMessage(post);
        
        // 发送审计日志
        messageProducerService.sendAuditLogMessage(
            author.getId().toString(), 
            author.getUsername(), 
            "CREATE_POST", 
            "创建帖子: " + post.getTitle()
        );
        
        return CompletableFuture.completedFuture(post);
    }
    @Async
    public CompletableFuture<Post> editPost(Long id, Post updatedPost) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
        // 只允许作者本人编辑
        if (updatedPost.getAuthor() == null || !post.getAuthor().getId().equals(updatedPost.getAuthor().getId())) {
            throw new RuntimeException("无权限编辑");
        }
        post.setTitle(updatedPost.getTitle());
        post.setContent(updatedPost.getContent());
        post.setImages(updatedPost.getImages());
        Post saved = postRepository.save(post);
        return CompletableFuture.completedFuture(saved);
    }
    @Async
    public CompletableFuture<Void> likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
        
        // 检查用户是否已点赞
        String likeKey = "user:like:" + userId + ":" + postId;
        if (redisTemplate.hasKey(likeKey)) {
            throw new RuntimeException("已经点赞过了");
        }
        
        // 更新点赞数
        Integer newLikeCount = post.getLikeCount() == null ? 1 : post.getLikeCount() + 1;
        postMapper.updateLikeCount(postId, newLikeCount);
        
        // 记录用户点赞行为到Redis
        redisTemplate.opsForValue().set(likeKey, java.time.LocalDateTime.now(), 30, TimeUnit.DAYS);
        
        // 发送点赞消息到RabbitMQ
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            messageProducerService.sendPostLikedMessage(postId, userId, user.getUsername());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    @Async
    public CompletableFuture<Void> collectPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
        // 简单计数，实际可用Set<Long>存已收藏用户避免重复
        post.setCollectCount(post.getCollectCount() == null ? 1 : post.getCollectCount() + 1);
        postRepository.save(post);
        return CompletableFuture.completedFuture(null);
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
    @Async
    @Cacheable(value = "posts", key = "'all'")
    public CompletableFuture<List<Post>> getAllPosts() {
        // 优先从Redis缓存获取
        String cacheKey = "posts:all";
        List<Post> cachedPosts = (List<Post>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedPosts != null) {
            return CompletableFuture.completedFuture(cachedPosts);
        }
        
        // 缓存未命中，从数据库查询
        List<Post> posts = postMapper.selectAllPosts();
        
        // 存入Redis缓存
        redisTemplate.opsForValue().set(cacheKey, posts, 1, TimeUnit.HOURS);
        
        return CompletableFuture.completedFuture(posts);
    }
    @Async
    public CompletableFuture<java.util.Optional<Post>> getPostById(Long id) {
        return CompletableFuture.completedFuture(postRepository.findById(id));
    }
    public List<Post> findPostsByUserId(Long userId) {
        return postRepository.findByAuthorId(userId);
    }
}
