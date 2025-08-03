package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.business.dto.CollectResponse;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.PostCollect;
import com.myweb.website_core.infrastructure.persistence.repository.PostCollectRepository;
import com.myweb.website_core.infrastructure.persistence.repository.PostLikeRepository;
import com.myweb.website_core.infrastructure.persistence.repository.PostRepository;
import com.myweb.website_core.domain.business.entity.PostLike;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.dto.LikeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {
    
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
   // private final UserRepository userRepository;
    private final PostCollectRepository postCollectRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageProducerService messageProducerService;

    
    /**
     * 切换点赞状态
     */
    @Transactional
    public LikeResponse toggleLike(Long postId, Long userId) {
        System.out.println("开始处理点赞操作: postId=" + postId + ", userId=" + userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);
        //判断是否已点赞
        boolean isLiked = existingLike.isPresent();
        int likeCount = isLiked ? post.getLikeCount() - 1 : post.getLikeCount() + 1;

        // 发送消息到RabbitMQ（异步，不影响主业务）
        try {
            messageProducerService.sendPostLikeMessage(postId, userId,!isLiked);
        } catch (Exception e) {
            System.err.println("Failed to send like message, but like operation succeeded: " + e.getMessage());
        }

            // 记录到Redis缓存
            String likeKey = RedisKey.likeKey(postId, userId);
            redisTemplate.opsForValue().set(likeKey, !isLiked, 5, TimeUnit.MINUTES);
            
            // 控制台注解：记录取消点赞操作
            System.out.println("User " + userId + " unliked post " + postId + " at " + java.time.LocalDateTime.now());

            return new LikeResponse(!isLiked, likeCount);
    }
    @Transactional
    public CollectResponse toggleCollect(Long postId, Long userId) {
        System.out.println("开始处理收藏操作: postId=" + postId + ", userId=" + userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        Optional<PostCollect> existingCollect = postCollectRepository.findByUserIdAndPostId(userId, postId);
        //判断是否已点赞
        boolean isCollect = existingCollect.isPresent();
        int CollectCount = isCollect ? post.getCollectCount() - 1 : post.getCollectCount() + 1;

        // 发送消息到RabbitMQ（异步，不影响主业务）
        try {
            messageProducerService.sendPostCollectMessage(postId, userId,!isCollect);
        } catch (Exception e) {
            System.err.println("Failed to send like message, but like operation succeeded: " + e.getMessage());
        }

        // 记录到Redis缓存
        String CollectKey = RedisKey.collectKey(postId, userId);
        redisTemplate.opsForValue().set(CollectKey, !isCollect, 5, TimeUnit.MINUTES);

        // 控制台注解：记录取消点赞操作
        System.out.println("User " + userId + " unCollectd post " + postId + " at " + java.time.LocalDateTime.now());

        return new CollectResponse(false,CollectCount);
    }
    /**
     * 检查用户是否点赞了帖子
     */
    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        String likeKey = RedisKey.likeKey(postId, userId);
        if(redisTemplate.opsForValue().get(likeKey) == null){
            return postLikeRepository.existsByUserIdAndPostId(userId, postId);
        }else{
            return (boolean) redisTemplate.opsForValue().get(likeKey);
        }
    }
    public boolean isPostCollectedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        String collectKey = RedisKey.collectKey(postId, userId);
        if(redisTemplate.opsForValue().get(collectKey) == null){
            return postCollectRepository.findByUserIdAndPostId(userId, postId).isPresent();
        }else{
            return (boolean) redisTemplate.opsForValue().get(collectKey);
        }
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