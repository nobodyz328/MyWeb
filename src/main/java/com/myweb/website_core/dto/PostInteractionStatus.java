package com.myweb.website_core.dto;

/**
 * 帖子交互状态DTO
 */
public class PostInteractionStatus {
    
    private Long postId;
    private Long userId;
    private boolean isLiked;
    private boolean isBookmarked;
    private long likeCount;
    private long bookmarkCount;
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

    // Getters and Setters
    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getBookmarkCount() {
        return bookmarkCount;
    }

    public void setBookmarkCount(long bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
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