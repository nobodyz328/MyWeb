package com.myweb.website_core.dto;

/**
 * 点赞操作消息DTO
 */
public class LikeMessageDto extends InteractionMessageDto {
    
    private boolean isLike; // true表示点赞，false表示取消点赞
    private String postTitle;
    private Long postAuthorId;

    public LikeMessageDto() {
        super();
        setMessageType("LIKE");
    }

    public LikeMessageDto(String messageId, Long userId, String username, Long postId, 
                         boolean isLike, String postTitle, Long postAuthorId) {
        super(messageId, userId, username, postId, "LIKE");
        this.isLike = isLike;
        this.postTitle = postTitle;
        this.postAuthorId = postAuthorId;
    }

    public boolean isLike() {
        return isLike;
    }

    public void setLike(boolean like) {
        isLike = like;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public Long getPostAuthorId() {
        return postAuthorId;
    }

    public void setPostAuthorId(Long postAuthorId) {
        this.postAuthorId = postAuthorId;
    }

    @Override
    public String toString() {
        return "LikeMessageDto{" +
                "isLike=" + isLike +
                ", postTitle='" + postTitle + '\'' +
                ", postAuthorId=" + postAuthorId +
                ", " + super.toString() +
                '}';
    }
}