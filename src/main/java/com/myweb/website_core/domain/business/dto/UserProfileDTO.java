package com.myweb.website_core.domain.business.dto;

import com.myweb.website_core.domain.business.entity.Post;
import lombok.Getter;

@Getter
public class UserProfileDTO {
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