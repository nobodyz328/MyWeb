package com.myweb.website_core.demos.web.user;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PostRepository postRepository;
    private final ConcurrentHashMap<String, String> emailCodeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> loginFailCount = new ConcurrentHashMap<>();

    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.postRepository = postRepository;
    }

    @Async
    public CompletableFuture<Void> sendRegisterCode(String email) {
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
        emailCodeMap.put(email, code);
        emailService.sendVerificationCode(email, code);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<User> register(String username, String password) {
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        // For now, store password as plain text for testing - should be encrypted in production
        user.setPassword(password);
        user.setEmail("");
        user.setAvatarUrl("https://static.hdslb.com/images/member/noface.gif"); 
        user.setBio("这个人很懒，什么都没有留下");       
        user.setLikedCount(0);
        userRepository.save(user);
        return CompletableFuture.completedFuture(user);
    }

    @Async
    public CompletableFuture<User> login(String username, String password, String code) {
        int failCount = loginFailCount.getOrDefault(username, 0);
        if (failCount >= 3 && (code == null || code.isEmpty())) {
            throw new RuntimeException("需要验证码");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            loginFailCount.put(username, failCount + 1);
            throw new RuntimeException("用户不存在");
        }
        if (!user.getPassword().equals(password)) {
            loginFailCount.put(username, failCount + 1);
            throw new RuntimeException("密码错误");
        }
        loginFailCount.remove(username);
        return CompletableFuture.completedFuture(user);
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
            dto.setUsername(user.getUsername() != null ? user.getUsername() : "用户");
            dto.setAvatarUrl(user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty() ? 
                             user.getAvatarUrl() : "https://static.hdslb.com/images/member/noface.gif");
            dto.setBio(user.getBio() != null && !user.getBio().isEmpty() ? 
                      user.getBio() : "这个人很懒，什么都没有留下");
            
            // Safely handle potentially null collections
            if (user.getFollowers() != null) {
                dto.setFollowersCount(user.getFollowers().size());
            } else {
                dto.setFollowersCount(0);
            }
            
            if (user.getFollowing() != null) {
                dto.setFollowingCount(user.getFollowing().size());
            } else {
                dto.setFollowingCount(0);
            }
            
            dto.setLikedCount(user.getLikedCount() != null ? user.getLikedCount() : 0);
            
            // Safely handle posts
            try {
                dto.setPosts(postRepository.findAll().stream()
                        .filter(p -> p != null && p.getAuthor() != null && p.getAuthor().getId() != null && p.getAuthor().getId().equals(userId))
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                System.err.println("Error retrieving posts: " + e.getMessage());
                dto.setPosts(Collections.emptyList());
            }
            
            System.out.println("UserService: Profile DTO created - Username: " + dto.getUsername() + 
                              ", Bio: " + dto.getBio() + ", Followers: " + dto.getFollowersCount());
            return dto;
        } catch (Exception e) {
            System.err.println("Error in getProfile: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取用户资料失败: " + e.getMessage());
        }
    }
}

@Getter
class UserProfileDTO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    private int followersCount;
    private int followingCount;
    private int likedCount;
    private java.util.List<Post> posts;

    public void setId(Long id) { this.id = id; }

    public void setUsername(String username) { this.username = username; }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public void setBio(String bio) { this.bio = bio; }

    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public void setLikedCount(int likedCount) { this.likedCount = likedCount; }

    public void setPosts(java.util.List<Post> posts) { this.posts = posts; }
}