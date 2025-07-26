package com.myweb.website_core.demos.web.blog;


import lombok.Getter;

@Getter
public class LikeResponse {
    private boolean liked;
    private int likeCount;

    public LikeResponse() {}

    public LikeResponse(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
}