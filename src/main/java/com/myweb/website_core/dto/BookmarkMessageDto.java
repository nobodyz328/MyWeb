package com.myweb.website_core.dto;

/**
 * 书签操作消息DTO
 */
public class BookmarkMessageDto extends InteractionMessageDto {
    
    private boolean isBookmark; // true表示添加书签，false表示取消书签
    private String postTitle;
    private Long postAuthorId;
    private String postAuthorName;

    public BookmarkMessageDto() {
        super();
        setMessageType("BOOKMARK");
    }

    public BookmarkMessageDto(String messageId, Long userId, String username, Long postId, 
                             boolean isBookmark, String postTitle, Long postAuthorId, String postAuthorName) {
        super(messageId, userId, username, postId, "BOOKMARK");
        this.isBookmark = isBookmark;
        this.postTitle = postTitle;
        this.postAuthorId = postAuthorId;
        this.postAuthorName = postAuthorName;
    }

    public boolean isBookmark() {
        return isBookmark;
    }

    public void setBookmark(boolean bookmark) {
        isBookmark = bookmark;
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

    @Override
    public String toString() {
        return "BookmarkMessageDto{" +
                "isBookmark=" + isBookmark +
                ", postTitle='" + postTitle + '\'' +
                ", postAuthorId=" + postAuthorId +
                ", postAuthorName='" + postAuthorName + '\'' +
                ", " + super.toString() +
                '}';
    }
}