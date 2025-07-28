package com.myweb.website_core.dto;

import lombok.Getter;

/**
 * 帖子交互状态DTO
 */
public class PostInteractionStatus {

    // Getters and Setters
    @Getter
    private Long postId;
    @Getter
    private Long userId;
    private boolean isLiked;
    private boolean isBookmarked;
    @Getter
    private long likeCount;
    @Getter
    private long bookmarkCount;
    @Getter
    private long commentCount;

    public PostInteractionStatus() {}

    public PostInteractionStatus(Long postId, Long userId, boolean isLiked, boolean isBookmarked,
                               long likeCount, long bookmarkCount, long commentCount) {
        this.postId = postId;
        this.userId = userId;
        this.isLiked = isLiked;
        this.isBookmarked = isBookmarked;
        this.likeCount = likeCount;
        this.bookmarkCount = bookmarkCount;
        this.commentCount = commentCount;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    @Override
    public String toString() {
        return "PostInteractionStatus{" +
                "postId=" + postId +
                ", userId=" + userId +
                ", isLiked=" + isLiked +
                ", isBookmarked=" + isBookmarked +
                ", likeCount=" + likeCount +
                ", bookmarkCount=" + bookmarkCount +
                ", commentCount=" + commentCount +
                '}';
    }
}