package com.myweb.website_core.demos.web.blog;

import lombok.Getter;

@Getter
public class CollectResponse {
    private boolean bookmarked;
    private int bookmarkCount;

    public CollectResponse() {}

    public CollectResponse(boolean bookmarked, int bookmarkCount) {
        this.bookmarked = bookmarked;
        this.bookmarkCount = bookmarkCount;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public void setBookmarkCount(int bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }
}