package com.myweb.website_core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

/**
 * 交互消息基础DTO
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "messageType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LikeMessageDto.class, name = "LIKE"),
    @JsonSubTypes.Type(value = BookmarkMessageDto.class, name = "BOOKMARK"),
    @JsonSubTypes.Type(value = CommentMessageDto.class, name = "COMMENT"),
    @JsonSubTypes.Type(value = StatsUpdateMessageDto.class, name = "STATS_UPDATE")
})
public abstract class InteractionMessageDto {
    
    private String messageId;
    private Long userId;
    private String username;
    private Long postId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String messageType;
    private int retryCount = 0;

    public InteractionMessageDto() {
        this.timestamp = LocalDateTime.now();
    }

    public InteractionMessageDto(String messageId, Long userId, String username, Long postId, String messageType) {
        this();
        this.messageId = messageId;
        this.userId = userId;
        this.username = username;
        this.postId = postId;
        this.messageType = messageType;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "InteractionMessageDto{" +
                "messageId='" + messageId + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", postId=" + postId +
                ", timestamp=" + timestamp +
                ", messageType='" + messageType + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}