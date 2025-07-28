package com.myweb.website_core.dto;

import lombok.Getter;

/**
 * 交互操作响应DTO
 */
@Getter
public class InteractionResponse {

    // Getters and Setters
    private boolean success;
    private String message;
    private String operation; // "like", "unlike", "bookmark", "unbookmark"
    private Long postId;
    private Long userId;
    private long newCount; // 新的计数（点赞数或书签数）
    private boolean currentStatus; // 当前状态（是否已点赞/书签）

    public InteractionResponse() {}

    public InteractionResponse(boolean success, String message, String operation, 
                             Long postId, Long userId, long newCount, boolean currentStatus) {
        this.success = success;
        this.message = message;
        this.operation = operation;
        this.postId = postId;
        this.userId = userId;
        this.newCount = newCount;
        this.currentStatus = currentStatus;
    }

    // Static factory methods for common responses
    public static InteractionResponse success(String operation, Long postId, Long userId, 
                                            long newCount, boolean currentStatus) {
        return new InteractionResponse(true, "Operation successful", operation, 
                                     postId, userId, newCount, currentStatus);
    }

    public static InteractionResponse failure(String operation, Long postId, Long userId, String message) {
        return new InteractionResponse(false, message, operation, postId, userId, 0, false);
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setNewCount(long newCount) {
        this.newCount = newCount;
    }

    public void setCurrentStatus(boolean currentStatus) {
        this.currentStatus = currentStatus;
    }

    @Override
    public String toString() {
        return "InteractionResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", operation='" + operation + '\'' +
                ", postId=" + postId +
                ", userId=" + userId +
                ", newCount=" + newCount +
                ", currentStatus=" + currentStatus +
                '}';
    }
}