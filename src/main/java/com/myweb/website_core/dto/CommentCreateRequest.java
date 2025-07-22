package com.myweb.website_core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new comment
 */
public class CommentCreateRequest {
    
    @NotBlank(message = "Comment content cannot be empty")
    @Size(max = 2000, message = "Comment content cannot exceed 2000 characters")
    private String content;
    
    @NotNull(message = "Post ID is required")
    private Long postId;
    
    @NotNull(message = "Author ID is required")
    private Long authorId;

    public CommentCreateRequest() {}

    public CommentCreateRequest(String content, Long postId, Long authorId) {
        this.content = content;
        this.postId = postId;
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

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    @Override
    public String toString() {
        return "CommentCreateRequest{" +
                "content='" + content + '\'' +
                ", postId=" + postId +
                ", authorId=" + authorId +
                '}';
    }
}