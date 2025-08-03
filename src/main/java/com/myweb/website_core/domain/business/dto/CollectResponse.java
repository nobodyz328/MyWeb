package com.myweb.website_core.domain.business.dto;

public class CollectResponse {
    private boolean bookmarked;
    private int bookmarkCount;
    
    public CollectResponse() {}
    
    public CollectResponse(boolean bookmarked, int bookmarkCount) {
        this.bookmarked = bookmarked;
        this.bookmarkCount = bookmarkCount;
    }
    
    public boolean isBookmarked() {
        return bookmarked;
    }
    
    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }
    
    public int getBookmarkCount() {
        return bookmarkCount;
    }
    
    public void setBookmarkCount(int bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }
}