package com.myweb.website_core.dto;

/**
 * 评论操作消息DTO
 */
public class CommentMessageDto extends InteractionMessageDto {
    
    private String content;
    private Long parentCommentId; // null表示顶级评论，非null表示回复
    private String postTitle;
    private Long postAuthorId;
    private String postAuthorName;
    private Long commentId; // 创建后的评论ID

    public CommentMessageDto() {
        super();
        setMessageType("COMMENT");
    }

    public CommentMessageDto(String messageId, Long userId, String username, Long postId, 
                            String content, Long parentCommentId, String postTitle, 
                            Long postAuthorId, String postAuthorName) {
        super(messageId, userId, username, postId, "COMMENT");
        this.content = content;
        this.parentCommentId = parentCommentId;
        this.postTitle = postTitle;
        this.postAuthorId = postAuthorId;
        this.postAuthorName = postAuthorName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
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

    public String getPostAuthorName() {
        return postAuthorName;
    }

    public void setPostAuthorName(String postAuthorName) {
        this.postAuthorName = postAuthorName;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isReply() {
        return parentCommentId != null;
    }

    @Override
    public String toString() {
        return "CommentMessageDto{" +
                "content='" + content + '\'' +
                ", parentCommentId=" + parentCommentId +
                ", postTitle='" + postTitle + '\'' +
                ", postAuthorId=" + postAuthorId +
                ", postAuthorName='" + postAuthorName + '\'' +
                ", commentId=" + commentId +
                ", " + super.toString() +
                '}';
    }
}