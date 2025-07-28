package com.myweb.website_core.demos.web.like;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.like.PostLike;
import com.myweb.website_core.demos.web.like.PostLikeRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.LikeResponse;
import com.myweb.website_core.service.MessageProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class PostLikeService {
    
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageProducerService messageProducerService;
    
    @Autowired
    public PostLikeService(PostLikeRepository postLikeRepository, 
                          PostRepository postRepository,
                          UserRepository userRepository,
                          RedisTemplate<String, Object> redisTemplate,
                          MessageProducerService messageProducerService) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.messageProducerService = messageProducerService;
    }
    
    /**
     * 切换点赞状态
     */
    @Transactional
    public LikeResponse toggleLike(Long postId, Long userId) {
        System.out.println("开始处理点赞操作: postId=" + postId + ", userId=" + userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);
        
        if (existingLike.isPresent()) {
            // 取消点赞
            postLikeRepository.deleteByUserIdAndPostId(userId, postId);
            
            // 更新点赞数
            long newLikeCount = postLikeRepository.countByPostId(postId);
            post.setLikeCount((int) newLikeCount);
            postRepository.save(post);
            
            // 清除Redis缓存
            String likeKey = "user:like:" + userId + ":" + postId;
            redisTemplate.delete(likeKey);
            
            // 控制台注解：记录取消点赞操作
            System.out.println("User " + userId + " unliked post " + postId + " at " + java.time.LocalDateTime.now());
            
            return new LikeResponse(false, (int) newLikeCount);
        } else {
            // 添加点赞
            PostLike postLike = new PostLike(user, post);
            postLikeRepository.save(postLike);
            
            // 更新点赞数
            long newLikeCount = postLikeRepository.countByPostId(postId);
            post.setLikeCount((int) newLikeCount);
            postRepository.save(post);
            
            // 记录到Redis缓存
            String likeKey = "user:like:" + userId + ":" + postId;
            redisTemplate.opsForValue().set(likeKey, System.currentTimeMillis(), 30, TimeUnit.DAYS);
            
            // 发送消息到RabbitMQ（异步，不影响主业务）
            try {
                messageProducerService.sendPostLikedMessage(postId, userId, user.getUsername());
            } catch (Exception e) {
                System.err.println("Failed to send like message, but like operation succeeded: " + e.getMessage());
            }
            
            // 控制台注解：记录点赞操作
            System.out.println("User " + userId + " liked post " + postId + " at " + java.time.LocalDateTime.now());
            
            return new LikeResponse(true, (int) newLikeCount);
        }
    }
    
    /**
     * 检查用户是否点赞了帖子
     */
    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }
    
    /**
     * 获取帖子的点赞数
     */
    public long getLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    /**
     * 获取用户点赞的帖子列表（分页）
     */
    public Page<Post> getUserLikedPosts(Long userId, Pageable pageable) {
        List<PostLike> likes = postLikeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Post> posts = likes.stream()
                .map(PostLike::getPost)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();
        
        return new PageImpl<>(posts, pageable, likes.size());
    }
    
    /**
     * 获取帖子的点赞用户列表
     */
    public List<User> getPostLikedUsers(Long postId) {
        List<PostLike> likes = postLikeRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return likes.stream()
                .map(PostLike::getUser)
                .toList();
    }
    
    /**
     * 删除帖子的所有点赞记录
     */
    @Transactional
    public void deleteAllLikesForPost(Long postId) {
        postLikeRepository.deleteByPostId(postId);
    }
}