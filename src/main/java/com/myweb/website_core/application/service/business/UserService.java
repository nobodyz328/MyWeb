package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.security.DataMaskingService;
import com.myweb.website_core.common.util.DTOConverter;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.util.PermissionUtils;
import com.myweb.website_core.domain.business.dto.UserProfileDTO;
import com.myweb.website_core.domain.business.dto.FollowResponse;
import com.myweb.website_core.domain.business.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final DataMaskingService dataMaskingService;
    private final DTOConverter dtoConverter;

    @Autowired
    public UserService(UserRepository userRepository, PostRepository postRepository, 
                      DataMaskingService dataMaskingService, DTOConverter dtoConverter,
                      UserFollowService userFollowService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.dataMaskingService = dataMaskingService;
        this.dtoConverter = dtoConverter;
        this.userFollowService = userFollowService;
    }



    private final UserFollowService userFollowService;

    @Async
    public CompletableFuture<Void> follow(Long userId, Long targetId) {
        userFollowService.toggleFollow(targetId, userId);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> unfollow(Long userId, Long targetId) {
        userFollowService.toggleFollow(targetId, userId);
        return CompletableFuture.completedFuture(null);
    }
    
    public FollowResponse followUser(Long userId, Long targetId) {
        return userFollowService.toggleFollow(targetId, userId);
    }
    
    public boolean isUserFollowedByUser(Long targetId, Long userId) {
        return userFollowService.isFollowedByUser(targetId, userId);
    }
    
    public List<UserProfileDTO> getFollowedUsers(Long userId) {
        List<User> followingUsers = userFollowService.getFollowingUsers(userId);
        
        return followingUsers.stream()
                .map(followedUser -> dtoConverter.convertToUserProfileDTO(followedUser, userId))
                .collect(Collectors.toList());
    }

    //@Async
    public UserProfileDTO getProfile(Long userId) {
        long startTime = System.currentTimeMillis();
        String currentUsername = PermissionUtils.getCurrentUsername();
        
        try {
            log.debug("UserService: Getting profile for user ID: {}", userId);
            
            // 检查访问权限
//            if (!PermissionUtils.canAccessUserResource(userId)) {
//                throw new RuntimeException("无权限访问该用户资料");
//            }
            
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
            log.debug("UserService: Found user: {}", user.getUsername());
            
            // 进行转换和脱敏
            UserProfileDTO dto = dtoConverter.convertToUserProfileDTO(user, userId);
            
            // 设置帖子信息
            dto.setPosts(postRepository.findByAuthorId(userId));
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "GET_PROFILE", currentUsername, "USER:" + userId, "SUCCESS", 
                "查看用户资料", executionTime);
            log.info(logMessage);

            log.debug("UserService: Profile DTO created - Username: {}, Bio: {}, Followers: {}", 
                     dto.getUsername(), dto.getBio(), dto.getFollowersCount());
            return dto;
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "GET_PROFILE_ERROR", e.getMessage(), currentUsername, 
                null, "获取用户资料ID:" + userId, null);
            log.error(errorLog);
            throw new RuntimeException("获取用户资料失败: " + e.getMessage());
        }
    }
    
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    public User save(User user) {
        return userRepository.save(user);
    }
}

