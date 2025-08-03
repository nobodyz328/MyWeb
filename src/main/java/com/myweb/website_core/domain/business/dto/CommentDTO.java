package com.myweb.website_core.domain.business.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private AuthorInfo author;
    private List<CommentDTO> replies;
    
    @Getter
    @Setter
    public static class AuthorInfo {
        private Long id;
        private String username;
        private String avatarUrl;
    }
}