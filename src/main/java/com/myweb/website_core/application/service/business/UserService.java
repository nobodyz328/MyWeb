package com.myweb.website_core.application.service.business;


import com.myweb.website_core.domain.business.dto.UserProfileDTO;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

import com.myweb.website_core.infrastructure.persistence.repository.PostRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Autowired
    public UserService(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }



    @Async
    public CompletableFuture<Void> follow(Long userId, Long targetId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        User target = userRepository.findById(targetId).orElseThrow(() -> new RuntimeException("目标用户不存在"));
        user.getFollowing().add(target);
        target.getFollowers().add(user);
        userRepository.save(user);
        userRepository.save(target);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> unfollow(Long userId, Long targetId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        User target = userRepository.findById(targetId).orElseThrow(() -> new RuntimeException("目标用户不存在"));
        user.getFollowing().remove(target);
        target.getFollowers().remove(user);
        userRepository.save(user);
        userRepository.save(target);
        return CompletableFuture.completedFuture(null);
    }

    //@Async
    public UserProfileDTO getProfile(Long userId) {
        try {
            System.out.println("UserService: Getting profile for user ID: " + userId);
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
            System.out.println("UserService: Found user: " + user.getUsername());
            
            UserProfileDTO dto = new UserProfileDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setAvatarUrl(user.getAvatarUrl());
            dto.setBio(user.getBio());
            dto.setLikedCount(user.getLikedCount());
            dto.setFollowersCount(user.getFollowers().size());
            dto.setFollowingCount(user.getFollowing().size());
            dto.setPosts(postRepository.findByAuthorId(userId));

            System.out.println("UserService: Profile DTO created - Username: " + dto.getUsername() + 
                              ", Bio: " + dto.getBio() + ", Followers: " + dto.getFollowersCount());
            return dto;
        } catch (Exception e) {
            System.err.println("Error in getProfile: " + e.getMessage());
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

