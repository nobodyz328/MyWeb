package com.myweb.website_core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for replying to an existing comment
 */
public class CommentReplyRequest {
    
    @NotBlank(message = "Reply content cannot be empty")
    @Size(max = 2000, message = "Reply content cannot exceed 2000 characters")
    private String content;
    
    @NotNull(message = "Post ID is required")
    private Long postId;
    
    @NotNull(message = "Parent comment ID is required")
    private Long parentCommentId;
    
    @NotNull(message = "Author ID is required")
    private Long authorId;

    public CommentReplyRequest() {}

    public CommentReplyRequest(String content, Long postId, Long parentCommentId, Long authorId) {
        this.content = content;
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    @Override
    public String toString() {
        return "CommentReplyRequest{" +
                "content='" + content + '\'' +
                ", postId=" + postId +
                ", parentCommentId=" + parentCommentId +
                ", authorId=" + authorId +
                '}';
    }
}