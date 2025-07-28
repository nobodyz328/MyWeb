package com.myweb.website_core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Request DTO for replying to an existing comment
 */
@Getter
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

    public void setContent(String content) {
        this.content = content;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
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