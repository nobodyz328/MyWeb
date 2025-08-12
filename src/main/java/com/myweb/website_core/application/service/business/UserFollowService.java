package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.common.util.RedisKey;
import com.myweb.website_core.domain.business.dto.FollowResponse;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.business.entity.UserFollow;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.UserFollowRepository;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowService {
    
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageProducerService messageProducerService;

    /**
     * 切换关注状态
     */
    @Transactional
    public FollowResponse toggleFollow(Long targetUserId, Long userId) {
        System.out.println("开始处理关注操作: targetUserId=" + targetUserId + ", userId=" + userId);
        
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("不能关注自己");
        }
        
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Optional<UserFollow> existingFollow = userFollowRepository.findByFollowerIdAndFollowingId(userId, targetUserId);
        boolean isFollowed = existingFollow.isPresent();
        
        // 计算关注者数量变化
        int followersCount = isFollowed ? 
            (int) userFollowRepository.countByFollowingId(targetUserId) - 1 : 
            (int) userFollowRepository.countByFollowingId(targetUserId) + 1;

        // 发送消息到RabbitMQ（异步，不影响主业务）
        try {
            messageProducerService.sendUserFollowMessage(targetUserId, userId, !isFollowed);
        } catch (Exception e) {
            System.err.println("Failed to send follow message, but follow operation succeeded: " + e.getMessage());
        }

        // 记录到Redis缓存
        String followKey = RedisKey.followKey(targetUserId, userId);
        redisTemplate.opsForValue().set(followKey, !isFollowed, 5, TimeUnit.MINUTES);
        
        // 控制台日志：记录关注操作
        System.out.println("User " + userId + (isFollowed ? " unfollowed " : " followed ") + 
                          "user " + targetUserId + " at " + java.time.LocalDateTime.now());

        return new FollowResponse(!isFollowed, followersCount);
    }
    
    /**
     * 检查用户是否关注了目标用户
     */
    public boolean isFollowedByUser(Long targetUserId, Long userId) {
        if (userId == null || targetUserId == null) return false;
        
        String followKey = RedisKey.followKey(targetUserId, userId);
        Object cachedResult = redisTemplate.opsForValue().get(followKey);
        
        if (cachedResult == null) {
            return userFollowRepository.existsByFollowerIdAndFollowingId(userId, targetUserId);
        } else {
            return (boolean) cachedResult;
        }
    }
    
    /**
     * 获取用户的关注者数量
     */
    public long getFollowersCount(Long userId) {
        return userFollowRepository.countByFollowingId(userId);
    }
    
    /**
     * 获取用户的关注数量
     */
    public long getFollowingCount(Long userId) {
        return userFollowRepository.countByFollowerId(userId);
    }
    
    /**
     * 获取用户关注的用户列表
     */
    public List<User> getFollowingUsers(Long userId) {
        List<UserFollow> follows = userFollowRepository.findByFollowerIdOrderByCreatedAtDesc(userId);
        return follows.stream()
                .map(UserFollow::getFollowing)
                .toList();
    }
    
    /**
     * 获取用户的关注者列表
     */
    public List<User> getFollowers(Long userId) {
        List<UserFollow> follows = userFollowRepository.findByFollowingIdOrderByCreatedAtDesc(userId);
        return follows.stream()
                .map(UserFollow::getFollower)
                .toList();
    }
    
    /**
     * 删除用户的所有关注关系
     */
    @Transactional
    public void deleteAllFollowsForUser(Long userId) {
        userFollowRepository.deleteByFollowerIdOrFollowingId(userId, userId);
    }
}