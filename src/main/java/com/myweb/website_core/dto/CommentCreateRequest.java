package com.myweb.website_core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Request DTO for creating a new comment
 */
@Getter
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

    public void setContent(String content) {
        this.content = content;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
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